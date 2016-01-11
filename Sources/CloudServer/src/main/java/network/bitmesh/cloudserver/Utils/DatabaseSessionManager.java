package network.bitmesh.cloudserver.Utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSessionManager
{
    private static final Logger log = LoggerFactory.getLogger(DatabaseSessionManager.class.getName());
    private static final SessionFactory sessionFactory = buildSessionFactory();

    /**
     * Creates a session factory from the hibernate xml file
     * @return
     */
    private static SessionFactory buildSessionFactory()
    {
        try
        {
            return new Configuration().configure().buildSessionFactory();
        }
        catch(Throwable ex)
        {
            log.error("SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() { return sessionFactory; }

    /**
     * Close connections and connection pools
     */
    public static void shutdown()
    {
        getSessionFactory().close();
    }


}
