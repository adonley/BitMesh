package network.bitmesh.websockets;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import network.bitmesh.authentication.Authenticator;
import org.bitcoin.paymentchannel.Protos;
import org.bitcoinj.core.Utils;
import org.bitcoinj.net.StreamParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by c on 8/22/15.This websocket represents a connection with the seller's dashboard
 */
public class DashboardWebsocket extends WebSocket
{
    private static final Logger log = LoggerFactory.getLogger(DashboardWebsocket.class);

    private String remoteAddress;
    public Timer keepAliveTimer;
    public Timer statisticsTimer;
    private DashboardWebsocketServer server;
    public boolean ephemeral;
    private NanoHTTPD.IHTTPSession session;
    private boolean deliveringStatistics;
    private final int STATISTICS_UPDATE_FREQUENCY_SECONDS = 10;

    public DashboardWebsocket(NanoHTTPD.IHTTPSession handshake, DashboardWebsocketServer server, String ipAddress)
    {
        super(handshake);
        this.session = handshake;
        this.server = server;
        this.remoteAddress = ipAddress;
        this.keepAliveTimer = new Timer();
        this.statisticsTimer = new Timer();
        this.ephemeral = false;
        this.deliveringStatistics = false;
    }

    @Override
    protected void onPong(WebSocketFrame pongFrame)
    {
        //log.info("Pong " + pongFrame.toString());
    }

    @Override
    protected void onMessage(WebSocketFrame messageFrame)
    {
        try
        {
            messageFrame.setUnmasked();
            String message = messageFrame.getTextPayload();
            log.info("received message {}", message);
            if (Authenticator.getInstance().authorizedForDashboard(session) && !ephemeral)
            {
                startStatisticsDelivery();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote)
    {
        System.out.println("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " +
                (code != null ? code : "UnknownCloseCode[" + code + "]") +
                (reason != null && !reason.isEmpty() ? ": " + reason : ""));
        if (!ephemeral)
        {
            keepAliveTimer.cancel();
            statisticsTimer.cancel();
            server.removeWebsocket(remoteAddress);
        }
    }

    @Override
    protected void onException(IOException e) {
        e.printStackTrace();
        server.removeWebsocket(remoteAddress);
    }

    private void startStatisticsDelivery()
    {
        if (!deliveringStatistics)
        {
            deliveringStatistics = true;
            statisticsTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run()
                {
                    try
                    {
                        // TODO: this
                        send("Hello");
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        statisticsTimer.cancel();
                        deliveringStatistics = false;
                    }
                }
            }, 0, STATISTICS_UPDATE_FREQUENCY_SECONDS * 1000);
        }
        else
        {
            log.warn("Should not be told to send statistics twice");
        }

    }

}
