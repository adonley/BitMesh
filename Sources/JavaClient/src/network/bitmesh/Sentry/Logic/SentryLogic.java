package network.bitmesh.Sentry.Logic;

public interface SentryLogic
{

    void paymentIncreased(long increasedTo);
    boolean accountFunded();

}
