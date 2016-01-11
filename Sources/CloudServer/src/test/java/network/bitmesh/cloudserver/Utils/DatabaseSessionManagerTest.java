package network.bitmesh.cloudserver.Utils;

import network.bitmesh.cloudserver.Statistics.IPAddress;
import network.bitmesh.cloudserver.Statistics.Sell;
import network.bitmesh.cloudserver.Statistics.Vendor;
import org.hibernate.Session;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Calendar;

import static org.junit.Assert.*;

public class DatabaseSessionManagerTest
{
    private static final Logger log = LoggerFactory.getLogger(DatabaseSessionManagerTest.class.getName());

    @Test
    public void testVendorSellRelationship()
    {
        Session session = DatabaseSessionManager.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Timestamp timestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
        Vendor vendor = new Vendor("GOOSE");
        Sell sell1 = new Sell("tempTX1", timestamp, 1000, "Type1", 1000, vendor);
        Sell sell2 = new Sell("tempTX2", timestamp, 1000, "Type1", 1000, vendor);
        session.save(vendor);
        session.save(sell1);
        session.save(sell2);
        session.getTransaction().commit();
    }

    @Test
    public void testAllRelationships()
    {
        Session session = DatabaseSessionManager.getSessionFactory().getCurrentSession();
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
    }

}