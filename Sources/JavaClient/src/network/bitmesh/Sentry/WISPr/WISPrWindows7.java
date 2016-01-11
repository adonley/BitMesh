package network.bitmesh.Sentry.WISPr;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.ConnectivityStateMachine;
import network.bitmesh.Utils.StateMachine;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Scanner;

public class WISPrWindows7 extends WISPrSequence
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(WISPrWindows7.class);

    public enum WISPrStages implements StateMachine.State
    {
        NEW,                  // client is new, redirecting to localhost to initiate wispr sequence
        WISPR_SERVED,          // client has received the first page in WISPR sequence
        COMPLETE              // wispr sequence complete
    }

    protected class WISPrMachine extends StateMachine
    {
        public boolean transitionIsValid(State newState)
        {
            return (currentState == WISPrStages.NEW && newState == WISPrStages.WISPR_SERVED) ||
                    (currentState == WISPrStages.WISPR_SERVED && newState == WISPrStages.COMPLETE);
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

    public WISPrWindows7(ConnectedClient client)
    {
        this.stateMachine = new WISPrMachine(WISPrStages.values(), WISPrStages.NEW);
        this.client = client;
    }

    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
    {
        NanoHTTPD.Response response;

        if (stateMachine.currentState == WISPrStages.NEW)
        {
            response = serveWisprInitialPage();
        }
        else if (stateMachine.currentState == WISPrStages.WISPR_SERVED)
        {
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

        String response = buildStringResponseFromFile("wispr.html");

        stateMachine.setState(WISPrStages.WISPR_SERVED);
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, response);
    }

    private NanoHTTPD.Response serveWisprAuthenticationReply()
    {
        log.info("Serving authentication reply to client {}", client);

        String response = buildStringResponseFromFile("auth_reply.html");

        stateMachine.setState(WISPrStages.COMPLETE);
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, response);
    }

}
