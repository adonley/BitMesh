package network.bitmesh.cloudserver.Bitcoin;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class WalletContextListener implements ServletContextListener
{
    private ExecutorService executor;
    private Future<?> walletFuture;
    private WalletRunnable walletRunnable;
    private ThreadFactory factory;

    // TODO: Rethink this? Reference keeping the timer alive here
    private LockedTransactionBroadcasterTimer transactionBroadcaster;
    public static String executorName = "bitcoinexecutor";

    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        ServletContext context = servletContextEvent.getServletContext();
        factory = new DaemonThreadFactory();
        // Create a new executor for the bitcoinj thread
        executor = Executors.newFixedThreadPool(1, factory);
        context.setAttribute(executorName, executor);
        walletRunnable = new WalletRunnable();
        walletFuture = executor.submit(walletRunnable);

        // TODO: load transactionBroadcaster state
        transactionBroadcaster = LockedTransactionBroadcasterTimer.getInstance();
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent)
    {
        // TODO: save transactionBroadcaster state
        servletContextEvent.getServletContext();
        walletFuture.cancel(true);
        executor.shutdown();
    }
}