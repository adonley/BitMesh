package network.bitmesh.Sentry.Connectivity;

import network.bitmesh.Utils.StateMachine;

/**
 * Created by c on 8/15/15.
 */

public class ConnectivityStateMachine extends StateMachine
{
    private ConnectivitySentry sentry = ConnectivitySentry.getInstance();
    private ConnectedClient client;

    public enum ConnectivityState implements StateMachine.State
    {
        REDIRECT_TO_LOCALHOST,
        REDIRECT_TO_CAPTIVE_PORTAL,
        OPEN
    }

    public boolean transitionIsValid(State newState)
    {
        return true;
    }

    public ConnectivityStateMachine(ConnectedClient client)
    {
        super(ConnectivityState.values(), ConnectivityState.REDIRECT_TO_LOCALHOST);
        this.client = client;
    }

    public void setState(ConnectivityState newState)
    {
        State oldState = currentState;
        super.setState(newState);
        log.info("Client {} changing connectivity state from {} to {}", client, oldState, newState);
        if (oldState != newState)
        {
            ConnectivitySentry.getInstance().updatePeers();
        }
    }
}
