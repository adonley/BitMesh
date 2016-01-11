package network.bitmesh.Sentry.Connectivity;

import com.google.common.util.concurrent.ListenableFuture;
import network.bitmesh.Utils.StateMachine;
import network.bitmesh.channels.PaymentChannelServer;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by c on 8/15/15.
 */
public class PaymentStateMachine extends StateMachine
{
    private ConnectivitySentry sentry = ConnectivitySentry.getInstance();
    private ConnectedClient client;

    public enum PaymentState implements StateMachine.State
    {
        NEW,                  // client is new, redirecting to localhost to initiate wispr sequence
        PAYED,                // client has payed, account not in arrears
        GRACED,               // client has run out of time, but is within the grace period
        DISCONNECTED          // client has been disconnected
    }

    public boolean transitionIsValid(State newState)
    {
        boolean valid = (newState == PaymentState.DISCONNECTED) ||
                (currentState == PaymentState.DISCONNECTED && newState == PaymentState.NEW) ||
                (currentState == null && newState == PaymentState.NEW) ||
                (currentState == PaymentState.NEW && newState == PaymentState.PAYED) ||
                (currentState == PaymentState.PAYED && newState == PaymentState.GRACED) ||
                (currentState == PaymentState.GRACED && newState == PaymentState.PAYED);
        return valid;
    }

    public PaymentStateMachine(ConnectedClient client)
    {
        super(PaymentState.values(), PaymentState.NEW);
        this.client = client;
    }

    public void setState(PaymentState newState)
    {
        log.info("Client {} changing payment state from {} to {}", client, currentState, newState);
        super.setState(newState);
        if (currentState == PaymentState.NEW)
        {
            client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_LOCALHOST);
        }
        else if (currentState == PaymentState.PAYED)
        {
            client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.OPEN);
        }
        else if (currentState == PaymentState.GRACED)
        {
            client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.OPEN);
        }
        else if (currentState == PaymentState.DISCONNECTED)
        {

            // Client stopped paying, so put them back in the portal
            client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_CAPTIVE_PORTAL);

            // TODO: should we ever forget a client? Probably not
            sentry.removeConnectedClient(client.getIpAddress());

            // cancel the timer that checks for payment
            ScheduledFuture<?> handler = client.getSentryHandler();
            if (handler != null)
            {
                handler.cancel(true);
            }

            // close the payment channel
            PaymentChannelServer manager = client.getManager();
            if (manager != null)
            {
                // tell the PaymentChannelServer to send a close message
                manager.close();
            }
        }
    }
}
