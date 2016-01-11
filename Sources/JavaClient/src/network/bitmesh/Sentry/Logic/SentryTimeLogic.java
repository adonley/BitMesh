package network.bitmesh.Sentry.Logic;

import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.BitmeshConstants;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.PaymentStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * Created by christopher on 4/7/15. This class defines the logic for when the server sells time on the internet, as
 * opposed to data or milk.
 */
public class SentryTimeLogic implements SentryLogic
{
    private static final Logger log = LoggerFactory.getLogger(SentryTimeLogic.class);

    private final BitmeshConfiguration config;
    private long timePurchased;
    private long lastTimeChecked;
    private long timeSpent;
    private boolean currentlyPaying = false;
    private long gracePeriodRemaining;
    private long lastTimeGraced;
    private ConnectedClient client;
    private boolean reloadedChannel = false;
    private boolean firstIncrease = true;

    public SentryTimeLogic(BitmeshConfiguration config, ConnectedClient client)
    {
        this.client = client;
        this.config = config;
        this.timePurchased = 0;
        this.timeSpent = 0;
        this.gracePeriodRemaining = BitmeshConstants.INITIAL_GRACE_PERIOD;
    }

    public void paymentIncreased(long increasedTo)
    {
        if (!currentlyPaying || client.getPaymentState() == PaymentStateMachine.PaymentState.GRACED)
        {
            currentlyPaying = true;
            lastTimeChecked = Calendar.getInstance().getTime().getTime();
        }
        //log.info("payment increased to {} coinWidth {}, purchaseWidth {}", increasedTo, config.getCoinWidth(), config.getPurchaseWidth());
        timePurchased = (increasedTo/config.getCoinWidth()) * config.getPurchaseWidth();

        if (firstIncrease)
        {
            firstIncrease = false;
            // TODO: fix this dirty hack, put the info in the stored payment channel
            // this is here to account for accounting differences in the case that the client closes the channel and
            // then tries to reopen it. There's probably an attack vector here.
            if (reloadedChannel)
            {
                timeSpent = timePurchased - 10;
            }
        }

        //lastTimeChecked = cal.getTime().getTime();
    }

    /**
     * Determines whether the amount of time consumed is greater than the time paid for
     * @return Is the account funded?
     */
    public boolean accountFunded()
    {
        long now = Calendar.getInstance().getTime().getTime();

        // don't charge for time the client wasn't online
        if (currentlyPaying)
        {
            timeSpent += (now - lastTimeChecked) / 1000;
        }
        lastTimeChecked = now;
        log.info("timePurchased {} timeSpent {}", timePurchased, timeSpent);
        boolean funded = timePurchased >= timeSpent;
        if (!funded)
        {
            funded = maybeGrace();
        }
        currentlyPaying = funded;
        return funded;
    }

    /**
     * This function will put the client into the grace period if there is any grace period left
     * @return whether or not there is any grace period left
     */
    public boolean maybeGrace()
    {
        long now = Calendar.getInstance().getTime().getTime();

        // Remove some maybeGrace time from the client if they are not paying
        if (gracePeriodRemaining > 0)
        {
            // Don't double maybeGrace a client
            if (client.getPaymentState() != PaymentStateMachine.PaymentState.GRACED)
            {
                //log.info("Graced client {}", ipAddress);
                client.setPaymentState(PaymentStateMachine.PaymentState.GRACED);

                // Save latest maybeGrace time to compare against
                lastTimeGraced = now;
            }
            // Already graced lets update grace period remaining

            gracePeriodRemaining -= now - lastTimeGraced;

            lastTimeGraced = now;

//            log.info("Client {} has grace period of {} seconds", ipAddress, gracePeriodRemaining/1000);

        }

        return gracePeriodRemaining > 0;
    }

    public void setReloadedChannel(boolean reloaded) { this.reloadedChannel = reloaded; }

}
