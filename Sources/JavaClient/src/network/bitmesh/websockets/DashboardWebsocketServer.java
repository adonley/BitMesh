package network.bitmesh.websockets;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWebSocketServer;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import org.bitcoinj.net.StreamParser;
import org.bitcoinj.net.StreamParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by c on 8/22/15.This server creates websockets for pushing statistics to the dashboard.
 */
public class DashboardWebsocketServer extends NanoWebSocketServer
{
    private static final Logger log = LoggerFactory.getLogger(DashboardWebsocketServer.class);

    // Only one client can be connected at a time
    private boolean clientAlreadyConnected;
    private String sellerIp;

    public DashboardWebsocketServer(int port)
    {
        super(port);
        this.clientAlreadyConnected = false;
        this.sellerIp = "";
    }

    @Override
    public WebSocket openWebSocket(IHTTPSession handshake) {
        String remoteAddr = handshake.getHeaders().get("http-client-ip");
        try {
            // Get InetAddress from string remoteAddr
            InetAddress ipAddress = InetAddress.getByName(remoteAddr);

            // Build the MicropaymentWebsocket
            final DashboardWebsocket websocket = new DashboardWebsocket(handshake, this, remoteAddr);

            if (clientAlreadyConnected) {
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
            else
            {
                clientAlreadyConnected = true;
                sellerIp = remoteAddr;
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
        if (clientAlreadyConnected)
        {
            clientAlreadyConnected = false;
            sellerIp = "";
        }
        else
        {
            log.warn("Tried to remove client, but there is none...");
        }
    }

}
