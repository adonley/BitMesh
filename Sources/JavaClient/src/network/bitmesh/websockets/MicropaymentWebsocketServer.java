package network.bitmesh.websockets;

import fi.iki.elonen.NanoWebSocketServer;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import org.bitcoinj.net.StreamParser;
import org.bitcoinj.net.StreamParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by c on 4/15/15.
 */
public class MicropaymentWebsocketServer extends NanoWebSocketServer
{
    private static final Logger log = LoggerFactory.getLogger(MicropaymentWebsocketServer.class);
    StreamParserFactory protobufParserFactory;
    private HashSet<String> connectedIps;

    public MicropaymentWebsocketServer(int port, StreamParserFactory factory)
    {
        super(port);
        this.protobufParserFactory = factory;
        this.connectedIps = new HashSet<String>();
    }

    @Override
    public WebSocket openWebSocket(IHTTPSession handshake) {
        String remoteAddr = handshake.getHeaders().get("http-client-ip");
        try {
            // Get InetAddress from string remoteAddr
            InetAddress ipAddress = InetAddress.getByName(remoteAddr);

            // Get the stream parser that will parse the incoming protobuf messages
            StreamParser parser = protobufParserFactory.getNewParser(ipAddress, 0);

            // Build the MicropaymentWebsocket
            final MicropaymentWebsocket websocket = new MicropaymentWebsocket(handshake, parser, this, remoteAddr);

            // Tell the parser to write to the websocket
            parser.setWriteTarget(websocket);

            // Set the parser in the opened state
            parser.connectionOpened();


            if (connectedIps.contains(remoteAddr)) {
                log.warn("Tried to open multiple websockets with same client");
                // Set parser to null so we don't respond to messages and confuse the user
                websocket.ephemeral = true;

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            websocket.close(WebSocketFrame.CloseCode.PolicyViolation, "Multiple sockets open for same client");
                        }
                        catch (IOException e)
                        {
                            log.warn("Was not able to close websocket for having a twin open on same client.");
                        }
                    }
                }, 100);

            }
            else {

                connectedIps.add(remoteAddr);

                websocket.keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            //log.info("Sending ping");
                            websocket.ping(new byte[]{});
                        } catch (Exception e) {
                            log.error("Failed to send ping");
                            //e.printStackTrace();
                        }
                    }
                }, 0, (NanoHTTPD.SOCKET_READ_TIMEOUT / 2));
            }
            return websocket;
        } catch (UnknownHostException e) {
            log.warn("Websocket connection attempted to unknown host");
            return null;
        }

    }

    public void removeWebsocket(String ipAddress)
    {
        if (!connectedIps.remove(ipAddress))
        {
            log.warn("Tried to remove ipAddress {} from MicropaymentWebsocketServer, but we don't have a record of that one. {}", ipAddress, connectedIps);
        }

    }

}
