package network.bitmesh.Sentry.WISPr;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.ConnectivityStateMachine;
import network.bitmesh.Utils.StateMachine;

/**
 * Created by c on 8/19/15.
 */
public class WISPrWindows8 extends WISPrSequence
{

    private static String wisprInitial;
    private static String authReply;

    public enum WISPrStages implements StateMachine.State
    {
        NEW,                  // client is new, redirecting to localhost to initiate wispr sequence
        FIRST_WISPR_SERVED,          // client has received the first page in WISPR sequence
        SECOND_WISPR_SERVED,          // client has received the second page in WISPR sequence
        COMPLETE              // wispr sequence complete
    }

    protected class WISPrMachine extends StateMachine
    {
        public boolean transitionIsValid(State newState)
        {
            return (currentState == WISPrStages.NEW && newState == WISPrStages.FIRST_WISPR_SERVED) ||
                    (currentState == WISPrStages.FIRST_WISPR_SERVED && newState == WISPrStages.COMPLETE);
        }

        public WISPrMachine(StateMachine.State[] states, State initial)
        {
            super(states, initial);
        }

        @Override
        public void setState(State newState) throws IllegalStateException
        {
            super.setState(newState);

            if (newState == WISPrStages.COMPLETE)
            {
                client.setConnectivityState(ConnectivityStateMachine.ConnectivityState.REDIRECT_TO_CAPTIVE_PORTAL);
                wisprComplete = true;

            }
        }
    }

    public WISPrWindows8(ConnectedClient client)
    {
        this.stateMachine = new WISPrMachine(WISPrStages.values(), WISPrStages.NEW);
        this.client = client;
    }

    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
    {
        NanoHTTPD.Response response;
        if (session.getUri().contains("/redirect"))
        {
            response = serveWisprAuthenticationReply();
            stateMachine.setState(WISPrStages.SECOND_WISPR_SERVED);
        }
        else if (stateMachine.currentState == WISPrStages.NEW) {
            response = serveWisprInitialPage();
            stateMachine.setState(WISPrStages.FIRST_WISPR_SERVED);
        } else if (stateMachine.currentState == WISPrStages.FIRST_WISPR_SERVED) {
            response = serveWisprInitialPage();
            stateMachine.setState(WISPrStages.SECOND_WISPR_SERVED);
        } else if (stateMachine.currentState == WISPrStages.SECOND_WISPR_SERVED)
        {
//            response = serveWisprInitialPage();
            response = serveWisprAuthenticationReply();
//            stateMachine.setState(WISPrStages.COMPLETE);
        }
        else // if (stateMachine.currentState == WISPrStages.COMPLETE)
        {
            // TODO: maybe throw?
            response = serveWisprInitialPage();
            stateMachine.setState(WISPrStages.FIRST_WISPR_SERVED);
        }

        return response;
    }

    private String getAuthReply()
    {
        if (authReply == null)
        {
            authReply = buildStringResponseFromFile("wispr_auth_windows.html");
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
    private NanoHTTPD.Response serveWisprInitialPage() {
        log.info("Serving initial wispr page to client {}", client);

        return servePage(getWisprInitial());
    }

    private NanoHTTPD.Response serveWisprAuthenticationReply() {
        log.info("Serving wispr authentication page to client {}", client);
        return servePage(getAuthReply());
    }

}
