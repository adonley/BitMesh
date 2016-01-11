package network.bitmesh.cloudserver.Utils;

import network.bitmesh.cloudserver.Exceptions.DuplicateDatabaseEntry;
import network.bitmesh.cloudserver.Exceptions.NotFoundInDatabase;
import network.bitmesh.cloudserver.Statistics.IPAddress;
import network.bitmesh.cloudserver.Statistics.RefundPost;
import network.bitmesh.cloudserver.Statistics.Vendor;
import network.bitmesh.cloudserver.Statistics.VendorCashOut;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class PersistenceHelper
{
    private static final Logger log = LoggerFactory.getLogger(PersistenceHelper.class.getName());

    public static final String dateFormatString = "yyyy-MM-dd HH:mm:ss.SSS Z";
    public static final DateFormat dateFormat = new SimpleDateFormat(dateFormatString);


    public static void addVendorCashOut(VendorCashOut cashOut)
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();
        session.save(cashOut);
        session.getTransaction().commit();
        session.close();
    }

    public static void addRefund(RefundPost refund)
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();
        session.save(refund);
        session.getTransaction().commit();
        session.close();
    }

    public static synchronized Vendor getVendor(String MAC, Session session) throws NotFoundInDatabase
    {
        Vendor vendor = null;

        Query query = session.createQuery("from Vendor where MAC = :mac");
        query.setParameter("mac", MAC);

        List<? extends Vendor> vendors = query.list();

        if(vendors.size() <= 0)
            throw new NotFoundInDatabase(MAC + ": was not found in the vendor database");
        // This should actually never happens since we have the unique definition in hibernate
        else if(vendors.size() > 1)
            log.error("Vendor with mac " + MAC + ": is in the database " + vendors.size() + " times?");

        vendor = vendors.get(0);
        log.info("Retrieved vendor: " + vendor);

        return vendor;
    }

    public static synchronized IPAddress getIP(String IP, Session session) throws NotFoundInDatabase
    {
        IPAddress ipAddress = null;

        Query query = session.createQuery("from IPAddress where ipAddress = :ip");
        query.setParameter("ip", IP);

        List<? extends IPAddress> ips = query.list();

        if(ips.size() <= 0)
            throw new NotFoundInDatabase(IP + ": was not found in the ip address database");

        ipAddress = ips.get(0);
        log.info("Retrieved IPAddress: " + ipAddress);

        return ipAddress;
    }

    public static synchronized RefundPost getRefundTx(String hash, Session session) throws NotFoundInDatabase
    {
        RefundPost refundPost = null;

        Query query = session.createQuery("from RefundPost where txId = :txid");
        query.setParameter("txid", hash);

        List<? extends RefundPost> refundTxs = query.list();

        if(refundTxs.size() <= 0)
            throw new NotFoundInDatabase(hash + ": was not found in the RefundPost database");

        refundPost = refundTxs.get(0);
        log.info("Retrieved RefundTx from DB: " + refundPost.getTxId());

        return refundPost;
    }

    public static synchronized List<? extends RefundPost> getRefundTxs()
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();

        Query query = session.createQuery("from RefundPost where isMainNet!=null order by id DESC");

        List<? extends RefundPost> refundTxs = query.list();

        session.close();

        return refundTxs;
    }

    public static void updateRefundSuccess(String hash, boolean status)
    {
        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();

        RefundPost refundPost = null;

        try
        {
            refundPost = getRefundTx(hash, session);
            // Update the status of the refund
            refundPost.setSucceeded(status);
            session.update(refundPost);
            session.getTransaction().commit();
        }
        catch(Exception e)
        {
            log.error("Error getting refund from database, this shouldn't happen at this stage...");
            e.printStackTrace();
        }
        session.close();
        session.flush();
    }

    /**
     * Adds a vendor to the database - if the vendor mac already exists it is not
     * added because of the sql unique definition.
     * @param vendor - vendor to add to the database
     */
    public static synchronized void addOrSaveVendor(Vendor vendor, Session session)
    {
        try
        {
            session.beginTransaction();
            session.saveOrUpdate(vendor);
            session.getTransaction().commit();
        }
        catch(Exception e)
        {
            log.error("Error adding vendor to database");
            e.printStackTrace();
        }
    }
}
