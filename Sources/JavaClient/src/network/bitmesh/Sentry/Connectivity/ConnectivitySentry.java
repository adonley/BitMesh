package network.bitmesh.Sentry.Connectivity;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.Sentry.WISPr.*;
import network.bitmesh.TransactionServer.Server.BitmeshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

/**
 * This is a singleton class that handles changing the metered service on and off for various clients.
 * It calls a iptables/... script whenever a client changes state. It is also responsible for creating new ConnectedClients
 */
public class ConnectivitySentry
{
    private static final Logger log = LoggerFactory.getLogger(ConnectivitySentry.class);
    private static int numberOfSentryThreads = 8;
    private static ConnectivitySentry sentryInstance = null;

    // TODO: Fix this dependency
    private String SCRIPT_LOCATION = getClass().getResource("iptablesrouting.sh").getPath();
    private String TEMP_SCRIPT_LOCATION;
    private final String URL_PATTERN =
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private final String IP_ADDRESS_PATTERN_STRING =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private Pattern ipAddressPattern = Pattern.compile(IP_ADDRESS_PATTERN_STRING);

    private LinkedHashMap<String, ConnectedClient> ipList;
    private LinkedHashMap<SocketAddress, ConnectedClient> clientMap;
    private ScheduledExecutorService executorService;
    private BitmeshServer server;

    private ConnectivitySentry()
    {
        File script = new File(SCRIPT_LOCATION);
        try
        {
            File temp = File.createTempFile("routing", ".sh");

            // copy script to temp file
            InputStream is = new FileInputStream(script);
            OutputStream os =  new FileOutputStream(temp);
            try
            {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0)
                    os.write(buffer, 0, length);
            }
            finally
            {
                is.close();
                os.close();
            }
            log.info("temp file at {}", temp.getAbsolutePath());

            TEMP_SCRIPT_LOCATION = temp.getAbsolutePath();
            temp.setExecutable(true);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return;
        }
        ipList = new LinkedHashMap<String, ConnectedClient>();
        executorService = Executors.newScheduledThreadPool(numberOfSentryThreads);
        server = BitmeshServer.getInstance();
        updatePeers();
        //clientMap = new LinkedHashMap<SocketAddress, ConnectedClient>();
    }

    public static ConnectivitySentry getInstance()
    {
        if (sentryInstance == null)
            sentryInstance = new ConnectivitySentry();
        return sentryInstance;
    }

    public synchronized void removeConnectedClient(String ipAddress)
    {
        log.info("Removing client {} from Sentry", ipAddress);
        ipList.remove(ipAddress);
    }

    /**
     * Method to update the list of peers allowed to
     * have internet access. Should only be called in
     * other synchronized methods.
     */
    public synchronized void updatePeers()
    {
        try
        {
            ArrayList<String> scriptArgs = new ArrayList<String>();
            scriptArgs.add(TEMP_SCRIPT_LOCATION);

            scriptArgs.add(BitmeshConfiguration.IN_FACE);
            scriptArgs.add(BitmeshConfiguration.OUT_FACE);

            // Iterator ok, linked hash map
            Iterator<ConnectedClient> ipIter = ipList.values().iterator();
            while (ipIter.hasNext())
            {

                ConnectedClient client = ipIter.next();

                // Only allow the client if they are in the PAYED / GRACED / WISPR_SUBMITTED state
                if (client.getConnectivityState() == ConnectivityStateMachine.ConnectivityState.OPEN)
                {
                    scriptArgs.add("-o " + client.getIpAddress());
                }
                else if (client.getConnectivityState() == ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_CAPTIVE_PORTAL)
                {
                    scriptArgs.add("-c " + client.getIpAddress());
                }
            }
            log.info("Script arguments: " + (scriptArgs.toString().length() < 1 ? "None" : scriptArgs.toString()));
            ProcessBuilder pb = new ProcessBuilder(scriptArgs);

            // TODO: Make this switch to PF on apple/BSD and IPTABLES on Linux
            Process p = pb.start();
        }
        catch (Exception e)
        {
            log.error("Issue executing peer control script.");
            e.printStackTrace();
        }
    }

    public ScheduledExecutorService getExecutor() { return executorService; }

    public int numberOfConnectedClients() { return ipList.size(); }

    /**
     * Synchronized method to get a connection event handler for a SocketAddress. This should only be called by
     * PaymentChannelWebsocketServerListener, so we set wisprSequence to None
     * @param clientAddress
     * @return
     */
    public synchronized ConnectedClient getClient(SocketAddress clientAddress)
    {
        InetSocketAddress inetAddr = (InetSocketAddress)clientAddress;
        String ipAddress = inetAddr.getAddress().getHostAddress();
        ConnectedClient client;
        if (ipList.containsKey(ipAddress))
        {
            // TODO: assert correct state here
            client = ipList.get(ipAddress);
        }
        else
        {
            if (!ipAddressPattern.matcher(ipAddress).matches())
            {
                log.error("Bad ip address {}", ipAddress);
            }

            client = new ConnectedClient(ipAddress, server.getConfig());
            ipList.put(ipAddress, client);
        }

        client.setWisprSequence(new WISPrNone(client));
        client.setChannelOpen(true);
        log.info("Getting client {}", client);
        return client;
    }


    // This should be called only by HttpServerController
    public synchronized ConnectedClient getClient(NanoHTTPD.IHTTPSession session)
    {
        ConnectedClient client;
        Map<String, String> headers = session.getHeaders();
        String ipAddress = headers.get("http-client-ip");
        if (ipList.containsKey(ipAddress))
        {
            client = ipList.get(ipAddress);
        }
        else
        {
            client = new ConnectedClient(ipAddress, server.getConfig());
            ipList.put(ipAddress, client);
        }

        PaymentStateMachine.PaymentState clientPaymentState = client.getPaymentState();
        WISPrSequence wispr = client.getWisprSequence();

        // if this is the first time we have seen the client or the client is returning after having completed
        // the sequence previously, look at user-agent to configure wispr sequnece. This handles the case where the
        // same IP address connects with a different device... TODO
        if (wispr == null || wispr.isWisprComplete())
        {
            String userAgent = headers.get("user-agent");
            // TODO: Can we expect the browser to be making the first request always?
            if (userAgent == null)
            {
                log.warn("Null user-agent for client {}:{}", ipAddress);
                client.setWisprSequence(new WISPrNone(client));
            }
            else
            {
                WISPrSequence currentSequence = client.getWisprSequence();

                if (userAgent.contains("Apple") || userAgent.contains("CaptiveNetworkSupport") ||
                        userAgent.contains("ocspd") || userAgent.contains("Macintosh") || userAgent.contains("OS X"))
                {
                    if (currentSequence == null || !(currentSequence instanceof WISPrApple))
                    {
                        client.setWisprSequence(new WISPrApple(client));
                    }
                }
                else if (userAgent.contains("Skype WISPr"))
                {
                    if (currentSequence == null || !(currentSequence instanceof WISPrWindows7))
                    {
                        client.setWisprSequence(new WISPrWindows7(client));
                    }
                }
                else if(userAgent.contains("Dalvik"))
                {
                    if (currentSequence == null || !(currentSequence instanceof WISPrAndroid))
                    {
                        client.setWisprSequence(new WISPrAndroid(client));
                    }
                }
                else if (userAgent.contains("Microsoft-WNS") || userAgent.contains("Microsoft NCSI") || userAgent.contains("WISPR!Microsoft"))
                {
                    if (currentSequence == null || !(currentSequence instanceof WISPrWindows8))
                    {
                        client.setWisprSequence(new WISPrWindows8(client));
                    }
                }
                else
                {
                    log.warn("Unknown user-agent for client {}:{}", ipAddress, userAgent);
                    client.setWisprSequence(new WISPrNone(client));
                }
            }
        }

        return client;
    }

}
