package network.bitmesh.Sentry.Connectivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.Sentry.Logic.SentryDataLogic;
import network.bitmesh.Sentry.Logic.SentryLogic;
import network.bitmesh.Sentry.Logic.SentryTimeLogic;
import network.bitmesh.Sentry.WISPr.WISPrSequence;
import network.bitmesh.TransactionServer.Server.BitmeshServer;
import network.bitmesh.Units.DataUnit;
import network.bitmesh.Units.TimeUnit;
import network.bitmesh.channels.*;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;

/**
 * This class is meant to be the servers representation of a client that has connected. It has a WISPrSequence object
 * that encodes the clients path thru the wispr protocol. It also encodes the payment state and connectivity state with
 * state machines. It has a timer that regularly checks to see if the client's account has been funded, according to the
 * SentryLogic. It has another timer that will reset the WISPrSequence if the client appears to have gotten lost on the
 * way.
 */
public class ConnectedClient extends ServerConnectionEventHandler
{
    private static final Logger log = LoggerFactory.getLogger(ConnectedClient.class);

    // Identification
    private String ipAddress;

    // Configuration Information
    private BitmeshServer server;
    private BitmeshConfiguration config;
    private SentryLogic sentryLogic;
    private PaymentChannelServer manager;
    private ConnectivitySentry sentry = ConnectivitySentry.getInstance();;

    // TODO: Create a way to reset gracePeriod if client adhere's to good ratio after reopen
    // States and Information
    private PaymentStateMachine paymentStateMachine;
    private ConnectivityStateMachine connectivityStateMachine;
    private WISPrSequence wisprSequence;
    private boolean channelOpen;

    // Controller Threads and Futures
    private SentryTimerRunnable sentryTimerRunnable;
    private WISPrResetRunnable resetRunnable;
    private ScheduledFuture<?> sentryHandler = null;
    private ScheduledFuture<?> resetHandler = null;
    private final int SENTRY_FREQUENCY_MILLIS = 1000;
    private final int RESET_FREQUENCY_SECONDS = 10;

    // TODO: Refactor this with MAC addresses?
    public ConnectedClient(String ipAddress, BitmeshConfiguration config)
    {
        this.server = BitmeshServer.getInstance();
        this.ipAddress = ipAddress;
        this.config = config.makeCopy();
        this.paymentStateMachine = new PaymentStateMachine(this);
        this.connectivityStateMachine = new ConnectivityStateMachine(this);
        this.channelOpen = false;
        //this.sentryHandler = sentry.getExecutor().scheduleAtFixedRate()
        this.resetRunnable = new WISPrResetRunnable();
        this.resetHandler = sentry.getExecutor().scheduleAtFixedRate(resetRunnable, RESET_FREQUENCY_SECONDS,
                                                                    RESET_FREQUENCY_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        if (config.purchaseUnit.getType().equals(DataUnit.class.getName()))
        {
            sentryLogic = new SentryDataLogic(config, ipAddress);
        }
        else if (config.purchaseUnit.getType().equals(TimeUnit.class.getName()))
        {
            // TODO: Make sure to update this if the config changes. Amount payed and things will be different
            sentryLogic = new SentryTimeLogic(config, this);
        }
        else
        {
            log.error("Unknown purchaseUnit type {}", config.purchaseUnit.getType());
        }

        setPaymentState(PaymentStateMachine.PaymentState.NEW);
    }


    /**
     * SentryTimerRunnable checks to see whether or not a connected client is in arrears.
     * The max number of checks was removed from this class since it is controlled by the connected client itself. It
     * should rather be checked for the duration of the grace period.
     */
    private class SentryTimerRunnable implements Runnable
    {
        @Override
        public void run()
        {
            //log.info("Checking whether amount transferred has been paid for");

            if (!sentryLogic.accountFunded()) {
                // set state to disconnected
                setPaymentState(PaymentStateMachine.PaymentState.DISCONNECTED);

//                channelClosed(PaymentChannelCloseException.CloseReason.CLIENT_STOPPED_PAYING);

                if (manager == null)
                {
                    throw new IllegalStateException("PaymentChannelServer needs to be associated with ConnectedClient");
                }
            }
        }
    }


    private class WISPrResetRunnable implements Runnable
    {
        @Override
        public void run()
        {
            if (!isChannelOpen())
            {
                if (getConnectivityState() != ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_LOCALHOST)
                {
                    log.info("Forgetting client {} due to timeout", ipAddress);
                    setConnectivityState(ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_LOCALHOST);
                }
            }
            else
            {
                log.info("resetHandler.cancel() returns {}", resetHandler.cancel(true));
            }
        }
    }


    //================================ ConnectionHandler Methods ====================

    @Override
    public void channelOpen(Sha256Hash channelId)
    {
        log.info("Channel open for {} channelId: {}" + getIpAddress(), channelId);

        // Try to get the state object from the stored state set in our wallet
        PaymentChannelServerState state;
        wisprSequence.wisprComplete = true;
        StoredServerChannel ch = server.getStoredStates().getChannel(channelId);
        if (ch != null)
        {
            try
            {
                state = ch.getOrCreateState(server.getWallet(), server.getAppKit().peerGroup());

                log.info("   with a maximum value of {}, expiring at UNIX timestamp {}.",
                        // The channel's maximum value is the value of the multisig contract which locks in some
                        // amount of money to the channel
                        state.getMultisigContract().getOutput(0).getValue(),
                        // The channel expires at some offset from when the client's refund transaction becomes
                        // spendable.
                        state.getRefundTransactionUnlockTime() + StoredPaymentChannelServerStates.CHANNEL_EXPIRE_OFFSET);

            }
            catch (VerificationException e)
            {
                // This indicates corrupted data, and since the channel was just opened, cannot happen
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    /**
     * Whenever the micropayment is incremented, the server responds with this function.
     * @param by
     * @param to
     * @param info
     */
    public ListenableFuture<ByteString> paymentIncrease(Coin by, Coin to, ByteString info)
    {

        log.info("Client {} paid increased payment by {} for a total of " + to.toString(), getIpAddress(), by);

        sentryLogic.paymentIncreased(to.value);
        PaymentStateMachine.PaymentState state = getPaymentState();
        switch (state)
        {
            case NEW:
                // Change client state to payed and allow to connect
                setPaymentState(PaymentStateMachine.PaymentState.PAYED);

                // Begin monitoring the amount of purchase the client is allowed
                sentryTimerRunnable = new SentryTimerRunnable();

                // TODO: This needs to be called back for the data payed for. (Probably different for data vs time)
                // Maybe create a callback system where a time will check certain things
                sentryHandler = sentry.getExecutor().scheduleAtFixedRate(sentryTimerRunnable, SENTRY_FREQUENCY_MILLIS, SENTRY_FREQUENCY_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);

                break;
            case PAYED:
                // nothing to see here...
                break;
            case GRACED:
                log.info("Ungracing client {}", ipAddress);
                setPaymentState(PaymentStateMachine.PaymentState.PAYED);
                break;
            case DISCONNECTED:
                log.error("Should not receive payment increase in DISCONNECTED state");
                break;
            default:
                log.error("Payment state invalid: {}", getPaymentState());
                break;
        }

        return null;
    }

    @Override
    /**
     * Called whenever the channel closes, by the ProtobufParser.Listener when it closes
     */
    public void channelClosed(PaymentChannelCloseException.CloseReason reason)
    {
        log.info("Closing channel for client {} for reason {}", getIpAddress(), reason);

        // simple flag
        setChannelOpen(false);

        // set this to false so the wispr sequence can restart
        wisprSequence.wisprComplete = false;
        //setPaymentState(PaymentStateMachine.PaymentState.DISCONNECTED);
    }

    // ============================== Getters/Setters, Regular Java Methods ========================

    public String getIpAddress()
    {
        return ipAddress;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ConnectedClient))
            return false;
        if (obj == this)
            return true;
        ConnectedClient client = (ConnectedClient)obj;
        if (ipAddress.equals(client.getIpAddress()))
            return true;
        return false;
    }

    @Override
    public String toString()
    {
        return "ConnectedClient {" +
                " ipAddress='" + ipAddress + '\'' +
                ", paymentState=" + getPaymentState() +
                ", connectivityState=" + getConnectivityState() +
                '}';
    }

    public BitmeshConfiguration getConfig() { return config; }

    public void setManager(PaymentChannelServer manager) { this.manager = manager; }

    public void setReloadedChannel(boolean reloaded) { ((SentryTimeLogic)sentryLogic).setReloadedChannel(reloaded); }

    public WISPrSequence getWisprSequence() { return wisprSequence; }

    public void setWisprSequence(WISPrSequence sequence) { this.wisprSequence = sequence; }

    public ScheduledFuture<?> getSentryHandler() { return sentryHandler; }

    public PaymentChannelServer getManager() { return manager; }

    //public PaymentStateMachine getPaymentStateMachine() { return paymentStateMachine; }

    public PaymentStateMachine.PaymentState getPaymentState() { return (PaymentStateMachine.PaymentState)paymentStateMachine.getState(); }

    public void setPaymentState(PaymentStateMachine.PaymentState state) { paymentStateMachine.setState(state); }

    //public ConnectivityStateMachine getConnectivityStateMachine() { return connectivityStateMachine; }

    public ConnectivityStateMachine.ConnectivityState getConnectivityState() { return (ConnectivityStateMachine.ConnectivityState)connectivityStateMachine.getState(); }

    public void setConnectivityState(ConnectivityStateMachine.ConnectivityState state) { connectivityStateMachine.setState(state); }

    public boolean isChannelOpen() { return channelOpen; }

    public void setChannelOpen(boolean open) { channelOpen = open; }
}
