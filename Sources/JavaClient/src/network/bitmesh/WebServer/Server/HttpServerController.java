package network.bitmesh.WebServer.Server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.ConnectivitySentry;
import network.bitmesh.TransactionServer.Server.BitmeshServer;
import network.bitmesh.Units.TimeUnit;
import network.bitmesh.WebServer.HttpController;
import org.apache.commons.validator.EmailValidator;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HttpServerController extends HttpController
{
    private static final Logger log = LoggerFactory.getLogger(HttpServerController.class);
    private BitmeshServer server = BitmeshServer.getInstance();

    public HttpServerController()
    {
        super(BitmeshConfiguration.defaultConfiguration().getPort());
    }

    public static HttpController getInstance()
    {
        if (instance == null)
        {
            instance = new HttpServerController();
        }
        return instance;
    }

    public boolean isLocalHost(String remote)
    {
        if (!remote.equals("localhost") && !remote.equals("127.0.0.1"))
        {
            return false;
        }
        return true;
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        String remote = session.getHeaders().get("http-client-ip");
        boolean isLocalHost = isLocalHost(remote);
        String uri = session.getUri();

        Map<String, String> headers = session.getHeaders();
        Map<String, String> params  = session.getParms();

        StringBuilder responseBuilder = new StringBuilder();
        InputStream buffer = null;

        log.info("Received {} request for uri {} from {} with user-agent {}", session.getMethod(), uri, remote, headers.get("user-agent"));

        if (uri != null) {
            int index = uri.lastIndexOf('.');
            if (index > -1)
            {
                String suffix = uri.substring(index);
                if (suffix.equals(".js") || suffix.equals(".css") ||
                        suffix.equals(".png") || suffix.equals(".svg")) {
                    buffer = getClass().getResourceAsStream(uri.substring(1));
                    return new NanoHTTPD.Response(Response.Status.OK, MIME_TYPES.get(suffix), buffer);
                }
            }
        }

        if (clientAttemptingViewDashboard(session))
        {
            if (clientAuthorizedViewDashboard(session))
            {
                return serveDashboard(session);
            }
            else
            {
                return forbidden();
            }
        }
        else
        {
            ConnectedClient connectedClient = ConnectivitySentry.getInstance().getClient(session);
            return connectedClient.getWisprSequence().serve(session);
        }


/*
        if (connectedClient.getPlatform() == ConnectedClient.Platform.NOT_SET
                && headers.containsKey("user-agent"))
        {
            // Small optimization
            String userAgent = headers.get("user-agent");

            if (userAgent.contains("Android"))
                connectedClient.setPlatform(ConnectedClient.Platform.ANDROID);
            else if (userAgent.contains("Microsoft") || userAgent.contains("Skype WISPr"))
                connectedClient.setPlatform(ConnectedClient.Platform.WINDOWS);
            else if (userAgent.contains("iPhone"))
                connectedClient.setPlatform(ConnectedClient.Platform.IPHONE);
            else if (userAgent.contains("Macintosh"))
                connectedClient.setPlatform(ConnectedClient.Platform.OSX);
            // Linux but not Android
            else if (userAgent.contains("Linux"))
                connectedClient.setPlatform(ConnectedClient.Platform.LINUX);
        }
*/


    }

    private boolean clientAuthorizedViewDashboard(IHTTPSession session)
    {
        return isLocalHost(session.getHeaders().get("http-client-ip"));
    }

    private boolean clientAttemptingViewDashboard(IHTTPSession session)
    {
        String uri = session.getUri();
        boolean uriForDashboard = uri.substring(0).equals("/save") ||
                                  uri.substring(0).equals("/cost") ||
                                  uri.substring(0).equals("/email") ||
                                  uri.substring(0).equals("/address");
        return uriForDashboard;
    }

    private Response serveDashboard(IHTTPSession session)
    {
        InputStream buffer = null;
        String uri = session.getUri();

        // TODO: make this more secure, I think if they URL encode periods this is circumventable
        // Prohibit getting out of current directory
        if (uri.contains("../"))
        {
            return forbidden();
        }

        try
        {
            if (uri != null) {
                 if (uri.substring(0).equals("/save")) {

                    Map<String, String> parsedBody = new HashMap<String, String>();
                    session.parseBody(parsedBody);

                    JsonParser parser = new JsonParser();

                    // This is a nanohttpd thing for post requests
                    JsonObject jsonObject = (JsonObject) parser.parse(parsedBody.get("postData"));
                    jsonObject.toString();

                    // Get the info about the configuration
                    long minPrice = jsonObject.get("min_price").getAsLong();
                    long maxPrice = jsonObject.get("max_price").getAsLong();
                    long refundThreshold = jsonObject.get("refund_threshold").getAsLong();
                    String meterUnit = jsonObject.get("meter_unit").getAsString();

                    // TODO: Bring in the change in the change from the data type
                    TimeUnit unit = new TimeUnit();

                    BitmeshConfiguration configuration = new BitmeshConfiguration(unit, maxPrice, minPrice);
                    configuration.setRefundThreshold(refundThreshold);
                    server.setConfiguration(configuration);

                    Gson gson = new GsonBuilder().create();
                    HashMap<String, String> jsonMap = new HashMap<String, String>();
                    jsonMap.put("Message", "ok");
                    return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                }
                // TODO: This is the old way of getting the cost. Let's change this to websockets.
                else if (uri.substring(0).equals("/cost")) {
                    Gson gson = new GsonBuilder().create();
                    HashMap<String, String> jsonMap = new HashMap<String, String>();

                    try {
                        BitmeshConfiguration configuration = BitmeshServer.getInstance().getConfig();
                        // TODO: Figure out which information needs to be transfered, negotiate.
                        jsonMap.put("price_scalar", new Long(configuration.getMinPrice()).toString());
                        //log.info("Min price per base unit: {}", configuration.getMinPricePerUnit());
                        jsonMap.put("meter_unit", configuration.getPurchaseUnit().getType());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    NanoHTTPD.Response response = new Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return response;
                } else if (uri.substring(0).equals("/email")) {

                    Map<String, String> parsedBody = new HashMap<String, String>();
                    session.parseBody(parsedBody);

                    JsonParser parser = new JsonParser();

                    // This is a nanohttpd thing for post requests
                    JsonObject jsonObject = (JsonObject) parser.parse(parsedBody.get("postData"));
                    jsonObject.toString();

                    String tempEmail = jsonObject.get("email_address").getAsString();
                    Gson gson = new GsonBuilder().create();
                    HashMap<String, String> jsonMap = new HashMap<String, String>();

                     // TODO: de-deprecate
                    EmailValidator validator = EmailValidator.getInstance();
                    if (validator.isValid(tempEmail)) {
                        // Get the email address and save it
                        server.getConfig().setEmailAddress(tempEmail);
                        server.getConfig().saveCurrentConfiguration(server.getPreferences());
                        jsonMap.put("Message", "ok");
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    } else {
                        jsonMap.put("Email", "Invalid email address.");
                        return new NanoHTTPD.Response(Response.Status.METHOD_NOT_ALLOWED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                } else if (uri.substring(0).equals("/address")) {

                    Map<String, String> parsedBody = new HashMap<String, String>();
                    session.parseBody(parsedBody);

                    JsonParser parser = new JsonParser();

                    // This is a nanohttpd thing for post requests
                    JsonObject jsonObject = (JsonObject) parser.parse(parsedBody.get("postData"));
                    jsonObject.toString();

                    String tempAddress = jsonObject.get("btc_address").getAsString();
                    Gson gson = new GsonBuilder().create();
                    HashMap<String, String> jsonMap = new HashMap<String, String>();

                    try {
                        Address updatedAddress = new Address(BitmeshConfiguration.params, tempAddress);
                        // set the return btc address and save it
                        server.getConfig().setBtcAddressString(tempAddress);
                        server.getConfig().saveCurrentConfiguration(server.getPreferences());
                        jsonMap.put("Message", "ok");
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    } catch (AddressFormatException e) {
                        jsonMap.put("Error", "Invalid bitcoin address.");
                        return new NanoHTTPD.Response(Response.Status.METHOD_NOT_ALLOWED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                }
                else
                {
                    return forbidden();
                }
            }
            else
            {
                return new NanoHTTPD.Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "No URI provided.");
            }
        }
        catch (ResponseException e)
        {
            e.printStackTrace();
            return new NanoHTTPD.Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "Server error.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return new NanoHTTPD.Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "Server error.");
        }

    }

/*    private Response serveWisprAuthenticationReply(ConnectedClient connectedClient)
    {
        InputStream wispr = getClass().getResourceAsStream("auth_reply.html");
        Scanner scanner = new Scanner(wispr);
        StringBuilder responseBuilder = new StringBuilder();

        while (scanner.hasNextLine())
        {
            responseBuilder.append(scanner.nextLine() + "\n");
        }

        // Set iptables to let all requests thru for a moment
        if (connectedClient.getPlatform() == ConnectedClient.Platform.ANDROID)
           //     connectedClient.getPlatform() == ConnectedClient.Platform.WINDOWS)
        {
            connectedClient.setPaymentState(ConnectedClient.PaymentState.CAPTIVE_PORTAL);
        }
        else
        {
            connectedClient.setPaymentState(ConnectedClient.PaymentState.WISPR_SUBMITTED);
        }
        return new NanoHTTPD.Response(Response.Status.OK, NanoHTTPD.MIME_HTML, responseBuilder.toString());
    }

    private Response serveWisprInitialPage(ConnectedClient connectedClient)
    {
        InputStream wispr = getClass().getResourceAsStream("wispr.html");
        Scanner scanner = new Scanner(wispr);
        StringBuilder responseBuilder = new StringBuilder();

        while (scanner.hasNextLine())
        {
            responseBuilder.append(scanner.nextLine() + "\n");
        }

        // Make their next request route to the next else if
        connectedClient.setPaymentState(ConnectedClient.PaymentState.FIRST_WISPR_SERVED);
        return new NanoHTTPD.Response(Response.Status.OK, NanoHTTPD.MIME_HTML, responseBuilder.toString());
    }

    private Response serveWisprMobilePage(ConnectedClient connectedClient)
    {
        InputStream wispr = getClass().getResourceAsStream("wispr.html");
        Scanner scanner = new Scanner(wispr);
        StringBuilder responseBuilder = new StringBuilder();

        while (scanner.hasNextLine())
        {
            responseBuilder.append(scanner.nextLine() + "\n");
        }

        // Make their next request route to the next else if
        connectedClient.setPaymentState(ConnectedClient.PaymentState.FIRST_WISPR_SERVED);
        return new NanoHTTPD.Response(Response.Status.OK, NanoHTTPD.MIME_HTML, responseBuilder.toString());
    }

    private Response serveWindowsWisprPage(ConnectedClient connectedClient)
    {
        InputStream wispr = getClass().getResourceAsStream("windows_7_wispr.html");
        Scanner scanner = new Scanner(wispr);
        responseBuilder = new StringBuilder();

        while (scanner.hasNextLine())
        {
            responseBuilder.append(scanner.nextLine() + "\n");
        }

        connectedClient.setPaymentState(ConnectedClient.PaymentState.CAPTIVE_PORTAL);
        return new NanoHTTPD.Response(Response.Status.OK, NanoHTTPD.MIME_HTML, responseBuilder.toString());
    }*/
}
