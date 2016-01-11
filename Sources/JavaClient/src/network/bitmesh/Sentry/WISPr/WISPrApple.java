package network.bitmesh.Sentry.WISPr;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.ConnectivityStateMachine;
import network.bitmesh.Utils.StateMachine;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Scanner;

public class WISPrApple extends WISPrSequence
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(WISPrApple.class);

    private final int NUM_SECONDS_FOR_WISPR_GAP = 20;
    private static String wisprInitial;
    private static String authReply;

    public enum WISPrStages implements StateMachine.State
    {
        NEW,                  // client is new, redirecting to localhost to initiate wispr sequence
        WISPR_SERVED,         // client has received the first page in WISPR sequence
        WISPR_SUBMITTED,      // client has made it thru the first step in wispr sequence, let them make NUM_SECONDS_FOR_WISPR_GAP's worth of GETs
        COMPLETE              // client wispr software thinks it is online, redirect to captive portal now
    }

    protected class WISPrMachine extends StateMachine
    {
        public boolean transitionIsValid(State newState)
        {
            return (currentState == WISPrStages.NEW && newState == WISPrStages.WISPR_SERVED) ||
                    (currentState == WISPrStages.WISPR_SERVED && newState == WISPrStages.WISPR_SUBMITTED) ||
                    (currentState == WISPrStages.WISPR_SUBMITTED && newState == WISPrStages.COMPLETE);
        }

        public WISPrMachine(StateMachine.State[] states, State initial)
        {
            super(states, initial);
        }

        @Override
        public void setState(State newState) throws IllegalStateException
        {
            super.setState(newState);
            log.info("Setting wispr state to {}", newState);
            if (newState == WISPrStages.WISPR_SUBMITTED)
            {
                // Let client on for a few seconds
                client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.OPEN);
                sentry.getExecutor().schedule(new WISPrGapRunnable(), NUM_SECONDS_FOR_WISPR_GAP, java.util.concurrent.TimeUnit.SECONDS);
            }
            else if (newState == WISPrStages.COMPLETE)
            {
                client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_CAPTIVE_PORTAL);
                wisprComplete = true;
            }
        }
    }

    public WISPrApple(ConnectedClient client)
    {
        this.stateMachine = new WISPrMachine(WISPrStages.values(), WISPrStages.NEW);
        this.client = client;
    }

    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
    {
        NanoHTTPD.Response response;

        if (stateMachine.getState() == WISPrStages.NEW)
        {
            response = serveWisprInitialPage();
        }
        else if (stateMachine.getState() == WISPrStages.WISPR_SERVED)
        {
            response = serveWisprAuthenticationReply();
        }
        else if (stateMachine.getState() == WISPrStages.WISPR_SUBMITTED)
        {
            // TODO: look at this
            response = serveWisprInitialPage();
        }
        else // if (stateMachine.currentState == WISPrStages.COMPLETE)
        {
            // TODO: maybe throw?
            response = serveWisprInitialPage();
        }

        return response;
    }

    private NanoHTTPD.Response serveWisprInitialPage()
    {
        log.info("Serving initial wispr page to client {}", client);

        stateMachine.setState(WISPrStages.WISPR_SERVED);
        return servePage(getWisprInitial());
    }

    private NanoHTTPD.Response serveWisprAuthenticationReply() {
        log.info("Serving wispr authentication page to client {}", client);

        stateMachine.setState(WISPrStages.WISPR_SUBMITTED);
        return servePage(getAuthReply());
    }

    /**
     * CaptivePortalRunnable simply sets the state to captive_portal after NUM_SECONDS_FOR_WISPR_GAP seconds
     */
    private class WISPrGapRunnable implements Runnable
    {
        @Override
        public void run()
        {
            stateMachine.setState(WISPrStages.COMPLETE);
        }
    }

    private String getAuthReply()
    {
        if (authReply == null)
        {
            authReply = buildStringResponseFromFile("wispr_auth_apple.html");
        }
        return authReply;
    }

    private String getWisprInitial()
    {
        if (wisprInitial == null)
        {
            wisprInitial = buildStringResponseFromFile("wispr.html");
        }
        return wisprInitial;
    }

}
