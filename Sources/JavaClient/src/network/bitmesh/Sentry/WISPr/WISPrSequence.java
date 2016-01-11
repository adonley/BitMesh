package network.bitmesh.Sentry.WISPr;

import fi.iki.elonen.NanoHTTPD;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.ConnectivitySentry;
import network.bitmesh.Utils.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Scanner;

public abstract class WISPrSequence
{
    protected static final Logger log = LoggerFactory.getLogger(WISPrSequence.class);
    protected ConnectivitySentry sentry = ConnectivitySentry.getInstance();

    public StateMachine stateMachine;
    public ConnectedClient client;

    public boolean wisprComplete = false;

    public boolean isWisprComplete() { return wisprComplete; }

    public abstract NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session);

    public String buildStringResponseFromFile(String fileName)
    {
        InputStream wispr = getClass().getResourceAsStream(fileName);
        Scanner scanner = new Scanner(wispr);
        StringBuilder responseBuilder = new StringBuilder();

        while (scanner.hasNextLine())
        {
            responseBuilder.append(scanner.nextLine() + "\n");
        }

        return responseBuilder.toString();
    }

    public NanoHTTPD.Response servePage(String page)
    {
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, page);
    }


}
