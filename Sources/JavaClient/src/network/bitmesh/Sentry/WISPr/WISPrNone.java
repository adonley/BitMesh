package network.bitmesh.Sentry.WISPr;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.WebServer.HttpController;

/**
 * Created by root on 8/14/15.
 */
public class WISPrNone extends WISPrSequence
{


    public WISPrNone(ConnectedClient client)
    {
        this.client = client;
        this.wisprComplete = true;
    }

    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session)
    {
        log.error("Should not be asked to serve a wispr page to client {}", client);
        return HttpController.forbidden();
    }

}
