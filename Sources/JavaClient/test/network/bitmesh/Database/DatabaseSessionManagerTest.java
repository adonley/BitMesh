package network.bitmesh.Database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.bitmesh.Statistics.IPAddress;
import network.bitmesh.Statistics.Sell;
import network.bitmesh.Statistics.SendStatistics;
import network.bitmesh.Statistics.Vendor;
import org.hibernate.Session;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;

import static org.junit.Assert.*;

public class DatabaseSessionManagerTest
{
    private static final Logger log = LoggerFactory.getLogger(DatabaseSessionManagerTest.class.getName());

    @Test
    public void testDatabase()
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();
        Timestamp timestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
        Vendor vendor = new Vendor("GOOSE");
        Sell sell1 = new Sell("tempTX1", timestamp, 1000, "Type1", 1000, vendor);
        Sell sell2 = new Sell("tempTX2", timestamp, 1000, "Type1", 1000, vendor);
        IPAddress address = new IPAddress("127.0.0.1", vendor);
        session.save(vendor);
        session.save(sell1);
        session.save(sell2);
        session.save(address);
        session.getTransaction().commit();

        try
        {
            session = DatabaseSessionManager.getSessionFactory().openSession();
            Vendor v =  (Vendor) session.get(Vendor.class, 1L);
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z")
                                         .excludeFieldsWithoutExposeAnnotation()
                                         .create();
            String json = gson.toJson(v);
            this.sendStats();
            log.info("Json vendor: " + json);
        }
        catch(Exception e)
        {
            log.error("Was not able to retrieve vendor.");
            e.printStackTrace();
        }
    }

    public void sendStats()
    {
        try
        {
            URL url = new URL("http://127.0.0.1:8082/stats");

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
            assert(responseCode == 200);

            if(responseCode == 200)
                PersistenceHelper.deleteEntries();

        }
        catch(Exception e)
        {
            log.error("Error sending statistics to server.");
            e.printStackTrace();
        }
    }
}