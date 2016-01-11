package network.bitmesh.Statistics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.bitmesh.BitmeshConstants;
import network.bitmesh.Database.PersistenceHelper;
import network.bitmesh.Sentry.Connectivity.ConnectivitySentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;

public class SendStatistics
{
    private static final Logger log = LoggerFactory.getLogger(SendStatistics.class);
    private static URL url;
    private static ScheduledFuture<?> statsHandler = null;
    private static StatisticsRunnable statisticsRunnable;
    private final long delay = 1000*60*60*8; // -> Delay 1/3 of a day

    {
        try
        {
            // TODO: Make this https when the server problems are fixed.
            if(!BitmeshConstants.BITMESH_TEST)
                url = new URL("http://www.bitmesh.network/stats");
            else
                url = new URL("http://127.0.0.1:8082/stats");
        }
        catch(Exception e)
        {
            log.error("Error initializing SendStatistics.");
            e.printStackTrace();
        }

        // Re-use executor service created by the connectivity sentry
        statisticsRunnable = new StatisticsRunnable();
        ConnectivitySentry.getInstance().getExecutor()
                          .scheduleAtFixedRate(statisticsRunnable, 0, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private class StatisticsRunnable implements Runnable
    {
        @Override
        public void run()
        {
            sendStats();
        }
    }

    /**
     * Sends statistics to the cloud server.
     */
    public void sendStats()
    {
        try
        {
            // Open connection to host
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set headers
            connection.setRequestMethod("POST");

            Vendor vendor = PersistenceHelper.getVendor();
            Gson gson = new GsonBuilder().setDateFormat(PersistenceHelper.dateFormatString)
                                         .excludeFieldsWithoutExposeAnnotation()
                                         .create();

            String payload = gson.toJson(vendor);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", new Integer(payload.getBytes().length).toString());
            connection.setDoOutput(true);

            OutputStream output = connection.getOutputStream();

            try
            {
                // TODO: Encrypt this payload
                output.write(payload.getBytes());
                output.flush();
            }
            catch (Exception e)
            {
                log.error("Error while sending the statistics to the server.");
                e.printStackTrace();
            }
            finally
            {
                output.close();
            }

            int responseCode = connection.getResponseCode();

            connection.disconnect();

            if(responseCode != 200)
                log.error("Response code was not 200. Error sending statistics.");
            else
            {
                // Delete everything from the database because
                // we uploaded it to the server
                PersistenceHelper.deleteEntries();
            }
        }
        catch(Exception e)
        {
            log.error("Error sending statistics to server.");
            e.printStackTrace();
        }
    }
}
