package network.bitmesh.cloudserver.Bitcoin;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This is necessary just in case there is a thrown error. Will interrupt the
 * server otherwise.
 */
public class DaemonThreadFactory implements ThreadFactory
{
    private final ThreadFactory factory;

    public DaemonThreadFactory()
    {
        factory = Executors.defaultThreadFactory();
    }

    public Thread newThread(Runnable r)
    {
        final Thread thread = factory.newThread(r);
        thread.setDaemon(true);
        return thread;
    }
}
