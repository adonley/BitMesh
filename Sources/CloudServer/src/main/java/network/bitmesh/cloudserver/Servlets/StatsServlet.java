package network.bitmesh.cloudserver.Servlets;

import com.google.gson.*;
import network.bitmesh.cloudserver.Exceptions.NotFoundInDatabase;
import network.bitmesh.cloudserver.Statistics.IPAddress;
import network.bitmesh.cloudserver.Statistics.Sell;
import network.bitmesh.cloudserver.Statistics.Vendor;
import network.bitmesh.cloudserver.Statistics.VendorCashOut;
import network.bitmesh.cloudserver.Utils.DatabaseSessionManager;
import network.bitmesh.cloudserver.Utils.PersistenceHelper;
import org.hibernate.Session;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class StatsServlet extends HttpServlet
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(StatsServlet.class.getName());

    JsonParser parser = new JsonParser();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        byte[] statsArray = new byte[req.getContentLength()];

        InputStream transactionInputStream = req.getInputStream();

        int alreadyRead = 0;
        // This might block indefinitely - need to have a time out for this
        while(req.getContentLength() > alreadyRead)
        {
            alreadyRead += transactionInputStream.read(statsArray, alreadyRead, transactionInputStream.available());
        }

        String statsString = new String(statsArray);
        statsString = statsString.substring(0, (statsString.length()));

        JsonElement element = parser.parse(statsString);

        if(putStatsInDatabase(element, req) == true)
            resp.setStatus(resp.SC_OK);
        else
            resp.setStatus(resp.SC_BAD_REQUEST);
    }

    protected boolean putStatsInDatabase(JsonElement element, HttpServletRequest request)
    {
        if(element == null || element.isJsonNull())
        {
            log.error("Json object was null.");
            return false;
        }

        Vendor vendor;

        // We are assuming this will be correct since it will have to
        // be have a valid token to get to this point.
        JsonObject jsonObject = element.getAsJsonObject();
        String MAC = jsonObject.get("MAC").getAsString();

        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();

        // Try to get the vendor from the database
        try
        {
            vendor = PersistenceHelper.getVendor(MAC, session);
        }
        // If the vendor doesn't exist then add it
        catch(NotFoundInDatabase d)
        {
            vendor = new Vendor();
            vendor.setMAC(MAC);

            // If x and y location exist then add them to the new vendor
            if(!(jsonObject.get("xLoc") == null) && !(jsonObject.get("yLoc") == null)
                    && !jsonObject.get("xLoc").isJsonNull() && !jsonObject.get("yLoc").isJsonNull())
            {
                vendor.setxLocation(jsonObject.get("xLoc").getAsString());
                vendor.setyLocation(jsonObject.get("yLoc").getAsString());
            }

            session.saveOrUpdate(vendor);
        }
        catch(Exception e)
        {
            log.error("Error decoding json.");
            e.printStackTrace();
            return false;
        }

        JsonArray sellsJsonArray = jsonObject.getAsJsonArray("sells");
        for(JsonElement o : sellsJsonArray)
        {
            if(o.isJsonObject())
                session.save(new Sell((JsonObject) o, vendor));
        }

        // Get the IP address from the request and save it in the DB
        String ip = request.getRemoteAddr();
        if(vendor.getIps() == null || !vendor.getIps().contains(ip))
            session.save(new IPAddress(ip, vendor));

        // Get the cashouts from the array and save them into the database
        JsonArray cashoutsJsonArray = jsonObject.getAsJsonArray("cashOuts");
        for(JsonElement o : cashoutsJsonArray)
        {
            if(o.isJsonObject())
                session.save(new VendorCashOut((JsonObject) o, vendor));
        }

        // Save the pushed stats
        session.getTransaction().commit();

        session.close();

        return true;
    }

}
