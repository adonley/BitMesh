package network.bitmesh.WebServer.Client;

import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.BitmeshConstants;
import network.bitmesh.TransactionServer.Client.BitmeshClient;
import network.bitmesh.Units.TimeUnit;
import network.bitmesh.WebServer.HttpController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import org.bitcoinj.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.ConnectException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by andrew on 4/17/15.
 */
public class HttpClientController extends HttpController
{

    private static final Logger log = LoggerFactory.getLogger(HttpClientController.class);
    private BitmeshClient client;
    private Timer keepAliveTimer;
    private ClientWebsocketManager websocketManager;

    private HttpClientController()
    {
        super(BitmeshConfiguration.defaultConfiguration().getPort() + 1);
        client = BitmeshClient.getInstance();
        keepAliveTimer = new Timer();
        websocketManager = new ClientWebsocketManager();
        // Keep the socket open at a rate of the timeout minus a second
        keepAliveTimer.scheduleAtFixedRate(websocketManager, 0, (NanoHTTPD.SOCKET_READ_TIMEOUT - 1000));
    }

    public static HttpController getInstance()
    {
        if(instance == null)
        {
            instance = new HttpClientController();
        }
        return instance;
    }

    public ClientWebSocket openWebSocket(IHTTPSession handshake)
    {
        return new ClientWebSocket(handshake, client.getWallet());
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        String remote = session.getHeaders().get("remote-addr");
        String uri = session.getUri();

        // Prohibit getting out of current directory
        if(!remote.equals("localhost") && !remote.equals("127.0.0.1") || uri.contains("../"))
        {
            return forbidden();
        }

        InputStream buffer = null;
        Map<String, String> headers = session.getHeaders();
        Map<String, String> params  = session.getParms();

        StringBuilder responseBuilder = new StringBuilder();

        // Handle the websocket connection
        if (isWebsocketRequested(session))
        {
            if (!HEADER_WEBSOCKET_VERSION_VALUE.equalsIgnoreCase(headers.get(HEADER_WEBSOCKET_VERSION)))
            {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Invalid Websocket-Version " + headers.get(HEADER_WEBSOCKET_VERSION));
            }

            if (!headers.containsKey(HEADER_WEBSOCKET_KEY))
            {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Missing Websocket-Key");
            }

            ClientWebSocket webSocket = openWebSocket(session);
            Response handshakeResponse = webSocket.getHandshakeResponse();
            try
            {
                handshakeResponse.addHeader(HEADER_WEBSOCKET_ACCEPT, makeAcceptKey(headers.get(HEADER_WEBSOCKET_KEY)));
            }
            catch (NoSuchAlgorithmException e)
            {
                return new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                        "The SHA-1 Algorithm required for websockets is not available on the server.");
            }

            if (headers.containsKey(HEADER_WEBSOCKET_PROTOCOL))
            {
                handshakeResponse.addHeader(HEADER_WEBSOCKET_PROTOCOL, headers.get(HEADER_WEBSOCKET_PROTOCOL).split(",")[0]);
            }

            ClientWebsocketManager.addWebSocket(webSocket);

            return handshakeResponse;
        }
        // Handle regular web requests
        else
        {
            try
            {
                if (uri != null)
                {
                    Map<String, String> parsedBody = new HashMap<String, String>();
                    session.parseBody(parsedBody);

                    JsonParser parser = new JsonParser();

                    Gson gson = new GsonBuilder().create();
                    HashMap<String, String> jsonMap = new HashMap<String, String>();

                    if (uri.contains(".js"))
                    {
                        buffer = getClass().getResourceAsStream(uri.substring(1));
                        return new NanoHTTPD.Response(Response.Status.OK, MIME_TYPES.get("js"), buffer);
                    }
                    else if (uri.contains(".css"))
                    {
                        buffer = getClass().getResourceAsStream(uri.substring(1));
                        return new NanoHTTPD.Response(Response.Status.OK, MIME_TYPES.get("css"), buffer);
                    }
                    else if (uri.contains(".png"))
                    {
                        buffer = getClass().getResourceAsStream(uri.substring(1));
                        return new NanoHTTPD.Response(Response.Status.OK, MIME_TYPES.get("png"), buffer);
                    }
                    else if (uri.substring(0).equals("/disconnect"))
                    {
                        client = BitmeshClient.getInstance();
                        client.closeConnection();
                        jsonMap.put("Message", "ok");
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                    else if (uri.substring(0).equals("/connect"))
                    {
                        // This is a nanohttpd thing for post requests
                        JsonObject jsonObject = (JsonObject) parser.parse(parsedBody.get("postData"));

                        // Get the info about the configuration
                        // TODO: this flow can go from client to server
                        long priceScalar = jsonObject.get("price_scalar").getAsLong();
                        //String priceUnit = jsonObject.get("price_unit").getAsString();
                        //int meterScalar = jsonObject.get("meter_scalar").getAsInt();
                        //String meterUnit = jsonObject.get("meter_unit").getAsString();

                        try
                        {
                            // TODO: This is only for testing
                            // TODO: Make this async somehow?
                            if(BitmeshConstants.test)
                                client.run("localhost");
                            else
                                client.run(BitmeshClient.retrieveGateway());
                        }
                        catch (ConnectException e)
                        {
                            e.printStackTrace();
                        }

                        // TODO: Bring in the change in the change from the data type
                        /*BitmeshConfiguration configuration = new BitmeshConfiguration(BitmeshConfiguration.PurchaseType.TIME,
                                TimeUnit.SECONDS, priceScalar, priceScalar);
                        client.setConfiguration(configuration); */

                        jsonMap.put("Message", "ok");
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                    else if (uri.substring(0).equals("/send"))
                    {

                        // This is a nanohttpd thing for post requests
                        JsonObject jsonObject = (JsonObject) parser.parse(parsedBody.get("postData"));

                        // Get the info about address
                        String stringToAddress = jsonObject.get("to_address").getAsString();

                        try
                        {
                            Address toAddress = new Address(BitmeshConfiguration.params, stringToAddress);
                            // Enough for the transaction
                            Coin balance = client.getWallet().getBalance().subtract(Coin.valueOf(1000));
                            Wallet.SendRequest request = Wallet.SendRequest.to(toAddress, balance);
                            client.getWallet().completeTx(request);
                            client.getWallet().commitTx(request.tx);
                            client.getAppKit().peerGroup().broadcastTransaction(request.tx);

                            jsonMap.put("status","ok");
                            jsonMap.put("reason", request.tx.getHash().toString());
                        }
                        catch (WrongNetworkException e)
                        {
                            log.info("Address incorrect.");
                            jsonMap.put("status", "error");
                            jsonMap.put("reason","Address incorrect for this network.");
                        }
                        catch (AddressFormatException a)
                        {
                            jsonMap.put("status","error");
                            jsonMap.put("reason", "Address is incorrect.");
                        }
                        catch (InsufficientMoneyException i)
                        {
                            log.info("Not enough money to be able to send out?");
                            jsonMap.put("status","error");
                            jsonMap.put("reason","Insufficient balance in wallet.");
                        }
                        catch (Exception e)
                        {
                            jsonMap.put("status","error");
                            jsonMap.put("reason","Internal server error.");
                        }
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                    else if (uri.substring(0).equals("/address"))
                    {
                        // Put address in Json map
                        jsonMap.put("address", client.getWallet().currentReceiveAddress().toString());
                        jsonMap.put("balance", client.getWallet().getBalance().toString());
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                    else if (uri.substring(0).equals("/gateway"))
                    {
                        // Put address in Json map
                        jsonMap.put("gateway", BitmeshClient.retrieveGateway());
                        return new NanoHTTPD.Response(Response.Status.CREATED, MIME_TYPES.get("json"), gson.toJson(jsonMap));
                    }
                    else if (uri.contains("woff") || uri.contains("ttf"))
                    {
                        buffer = getClass().getResourceAsStream(uri.substring(1));
                        return new NanoHTTPD.Response(Response.Status.OK, MIME_TYPES.get("other"), buffer);
                    }
                    else if (uri.equals("/wispr.html") || uri.equals("/"))
                    {
                        // Read in the file from the resources
                        File index = new File(getClass().getResource("wispr.html").getFile());
                        Scanner scanner = new Scanner(index);

                        // Read in the file line-by-line
                        while (scanner.hasNextLine())
                        {
                            responseBuilder.append(scanner.next() + "\n");
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return new NanoHTTPD.Response(responseBuilder.toString());
        }
    }

    public void sendPaymentAckValue(Coin value)
    {
        websocketManager.sendPaymentUpdateToClients(value);
    }
}
