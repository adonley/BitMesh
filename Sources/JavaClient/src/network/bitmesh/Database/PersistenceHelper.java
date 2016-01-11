package network.bitmesh.Database;

import network.bitmesh.Statistics.Vendor;
import network.bitmesh.Statistics.VendorCashOut;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceHelper
{
    private static final Logger log = LoggerFactory.getLogger(PersistenceHelper.class.getName());

    public static final String dateFormatString = "yyyy-MM-dd HH:mm:ss.SSS Z";

    /**
     * Records a cashout vendor event into the database.
     * @param cashOut - the event to put into the database.
     */
    public static void addVendorCashOut(VendorCashOut cashOut)
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();
        session.save(cashOut);
        session.getTransaction().commit();
        // Not sure if this is necessary here.
        session.flush();
        session.clear();
    }

    /**
     * Gets the vendor from the database. This is a limited version of
     * what is in the configuration and is mean't for sending statistics to the server.
     * @return the single vendor from the database
     */
    public static synchronized Vendor getVendor()
    {
        Session session;
        Vendor vendor = null;
        try
        {
            session = DatabaseSessionManager.getSessionFactory().openSession();
            vendor =  (Vendor)session.get(Vendor.class, 1L);
            log.info("Retrieved vendor: " + vendor);
        }
        catch(Exception e)
        {
            log.error("Was not able to retrieve vendor for this device.");
            e.printStackTrace();
        }
        return vendor;
    }

    /**
     * Adds a vendor to the database if there doesn't exist one. Vendor should never
     * change, so there should really only be one vendor in the database.
     * @param vendor
     */
    public static synchronized void addVendor(Vendor vendor)
    {
        Session session;
        try
        {
            session = DatabaseSessionManager.getSessionFactory().openSession();
            vendor =  (Vendor)session.get(Vendor.class, 1L);
            log.info("Retrieved vendor: " + vendor + " not adding another to the local db.");
        }
        catch(Exception e)
        {
            session = DatabaseSessionManager.getSessionFactory().openSession();
            session.beginTransaction();
            session.save(vendor);
            session.getTransaction().commit();
            // Not sure if this is necessary here.
            session.flush();
            session.clear();
            log.info("Added vendor: " + vendor + " to the local db.");
        }
    }

    /**
     * Deletes all entries from the database after sucessfully sending the statistics
     * to the server.
     */
    public static synchronized void deleteEntries()
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        try
        {
            // Delete VendorCashOuts
            org.hibernate.Query query = session.createQuery("DELETE from VendorCashOut");
            query.executeUpdate();

            // Delete Sells
            query = session.createQuery("DELETE from Sell");
            query.executeUpdate();

            // We are not concerned about IP addresses since those are recorded on the cloud
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            session.close();
        }
    }
}
