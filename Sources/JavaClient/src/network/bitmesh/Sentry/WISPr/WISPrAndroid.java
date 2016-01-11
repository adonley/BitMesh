package network.bitmesh.Sentry.WISPr;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Utils.StateMachine;
import org.slf4j.LoggerFactory;

public class WISPrAndroid extends WISPrSequence
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(WISPrAndroid.class);

    public enum WISPrStages implements StateMachine.State
    {
        NEW,
        WISPR_SERVED,
        WISPR_SUBMITTED,
        PAID,
        DEAUTHENTICATED
    }

    protected class WISPrMachine extends StateMachine
    {
        public boolean transitionIsValid(State newState)
        {
            return (currentState == WISPrStages.NEW && newState == WISPrStages.WISPR_SERVED) ||
                    (currentState == WISPrStages.WISPR_SERVED && newState == WISPrStages.WISPR_SUBMITTED) ||
                    (currentState == WISPrStages.WISPR_SUBMITTED && newState == WISPrStages.PAID);
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
            }
            else if (newState == WISPrStages.PAID)
            {
            }
        }
    }

    public WISPrAndroid(ConnectedClient client)
    {
        this.stateMachine = new WISPrMachine(WISPrStages.values(), WISPrStages.NEW);
        this.client = client;
    }

    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
    {
        NanoHTTPD.Response response = null;

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
            // TODO: Validate payment -> could transistion into validation
        }
        else // if (stateMachine.currentState == WISPrStages.COMPLETE)
        {
        }

        return response;
    }

    private NanoHTTPD.Response serveWisprInitialPage()
    {
        log.info("Serving initial wispr page to client {}", client);

        // TODO: Make this serve the mobile wispr page
        String response = buildStringResponseFromFile("wispr.html");

        stateMachine.setState(WISPrStages.WISPR_SERVED);

        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, response);
    }

    private NanoHTTPD.Response serveWisprAuthenticationReply()
    {
        log.info("Serving wispr authentication page to client {}", client);

        String response = buildStringResponseFromFile("auth_reply.html");

        stateMachine.setState(WISPrStages.WISPR_SUBMITTED);

        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, response);
    }
}
