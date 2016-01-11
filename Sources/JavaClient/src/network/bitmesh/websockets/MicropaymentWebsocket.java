package network.bitmesh.websockets;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import org.bitcoin.paymentchannel.Protos;
import org.bitcoinj.core.Utils;
import org.bitcoinj.net.MessageWriteTarget;
import org.bitcoinj.net.StreamParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by c on 4/16/15.
 */
public class MicropaymentWebsocket extends WebSocket implements MessageWriteTarget
{
    private static final Logger log = LoggerFactory.getLogger(MicropaymentWebsocket.class);

    //    ProtobufParser<Protos.TwoWayChannelMessage> parser;
    private StreamParser parser;
    private String remoteAddress;
    public Timer keepAliveTimer;
    private MicropaymentWebsocketServer server;
    public boolean ephemeral;

    public MicropaymentWebsocket(NanoHTTPD.IHTTPSession handshake, StreamParser parser, MicropaymentWebsocketServer server, String ipAddress)
    {
        super(handshake);
        this.parser = parser;
        this.server = server;
        this.remoteAddress = ipAddress;
        this.keepAliveTimer = new Timer();
        this.ephemeral = false;
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
            byte[] messageBytes = messageFrame.getBinaryPayload();
            checkState(messageBytes.length <= parser.getMaxMessageSize());
            byte[] messageLength = new byte[4];
            Utils.uint32ToByteArrayBE(messageBytes.length, messageLength, 0);

            ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length + 4);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(messageBytes.length);
            buffer.put(messageBytes);
            buffer.flip();
            Protos.TwoWayChannelMessage message = Protos.TwoWayChannelMessage.parseFrom(messageFrame.getBinaryPayload());
            int bytesConsumed = parser.receiveBytes(buffer);
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
            parser.connectionClosed();
            server.removeWebsocket(remoteAddress);
        }
    }

    @Override
    protected void onException(IOException e) {
        e.printStackTrace();
        server.removeWebsocket(remoteAddress);
    }

    @Override
    protected void handleWebsocketFrame(WebSocketFrame frame) throws IOException
    {
        //log.info("Received frame", frame.toString());
        super.handleWebsocketFrame(frame);
    }

    @Override
    public synchronized void sendFrame(WebSocketFrame frame) throws IOException
    {
        //log.info("Sending frame" +  frame.toString());
        super.sendFrame(frame);
    }


    /**
     * Writes the given bytes to the remote server.
     */
    public void writeBytes(byte[] message) throws IOException
    {
        send(message);
    }

    /**
     * Closes the connection to the server, triggering the {@link StreamParser#connectionClosed()}
     * event on the network-handling thread where all callbacks occur.
     */
    public void closeConnection()
    {
        try
        {
            close(WebSocketFrame.CloseCode.NormalClosure, "");
        }
        catch (IOException e)
        {
            log.error("Failed to close websocket");
            e.printStackTrace();
        }
    }


}
