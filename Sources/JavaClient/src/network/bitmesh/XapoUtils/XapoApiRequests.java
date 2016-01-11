package network.bitmesh.XapoUtils;

import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.TransactionServer.Server.BitmeshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;

public class XapoApiRequests
{
    private static final Logger log = LoggerFactory.getLogger(XapoApiRequests.class);

    private static String xapoAppId = "a66781edeb729287";
    private static String xapoAppSecret = "5c4826a2430867b582578cd745d2c7d2";

    public static void thresholdPayout()
    {
        BitmeshConfiguration configuration = BitmeshServer.getInstance().getConfig();

        // TODO: Possibility of leaving the connection open here
        try
        {
            // id_customer is the mac address for the server
            URL url = new URL("https://v2.api.xapo.com/customers?email=" + configuration.getEmailAddress()
                    + "&id_customer=" + configuration.getMac());

            // Open connection to host
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set headers
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization","Bearer " + xapoAppSecret);

            String message = connection.getResponseMessage();
            int responseCode = connection.getResponseCode();
            log.info("Response Message: " + message + " Response code: " + responseCode);
            connection.disconnect();
        }
        catch (Exception e)
        {
            log.error("Could not pay the user out after a threshold event.");
            e.printStackTrace();
        }
    }

}
