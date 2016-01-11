package network.bitmesh.channels;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import fi.iki.elonen.NanoWebSocketServer;
import network.bitmesh.Sentry.Connectivity.ConnectedClient;
import network.bitmesh.Sentry.Connectivity.ConnectivitySentry;
import network.bitmesh.websockets.MicropaymentWebsocketServer;

import org.bitcoin.paymentchannel.Protos;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionBroadcaster;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.net.ProtobufParser;
import org.bitcoinj.net.StreamParserFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;

public class PaymentChannelWebsocketServerListener
{
    // The wallet and peergroup which are used to complete/broadcast transactions
    private final Wallet wallet;
    private final TransactionBroadcaster broadcaster;

    // The event handler factory which creates new ServerConnectionEventHandler per connection
    private final Coin minAcceptedChannelSize;
    private NanoWebSocketServer server;
    private final int timeoutSeconds;


    private class ServerHandler {
        public ServerHandler(final SocketAddress address, final int timeoutSeconds) {
            paymentChannelManager = new PaymentChannelServer(broadcaster, wallet, minAcceptedChannelSize,
                    new PaymentChannelServer.ServerConnection()
            {
                @Override public void sendToClient(Protos.TwoWayChannelMessage msg) {
                    socketProtobufHandler.write(msg);
                }

                @Override public void destroyConnection(PaymentChannelCloseException.CloseReason reason) {
                    if (closeReason != null)
                        closeReason = reason;
                    socketProtobufHandler.closeConnection();
                    //paymentChannelManager.connectionClosed();
                    paymentChannelManager.stopWatchingAddress();
                }

                @Override public void channelOpen(Sha256Hash contractHash) {
                    //socketProtobufHandler.setSocketTimeout(0);
                    eventHandler.channelOpen(contractHash);
                }

                @Override public ListenableFuture<ByteString> paymentIncrease(Coin by, Coin to, @Nullable ByteString info) {
                    return eventHandler.paymentIncrease(by, to, info);
                }
            });

            // The ProtobufParser.Listener is what responds to events at the protobuf layer. messageReceived is called
            // ultimately by the websocket that receives the binary message
            protobufHandlerListener = new ProtobufParser.Listener<Protos.TwoWayChannelMessage>() {
                @Override
                public synchronized void messageReceived(ProtobufParser<Protos.TwoWayChannelMessage> handler, Protos.TwoWayChannelMessage msg) {
                    paymentChannelManager.receiveMessage(msg);
                }

                @Override
                public synchronized void connectionClosed(ProtobufParser<Protos.TwoWayChannelMessage> handler) {
                    paymentChannelManager.connectionClosed();
                    if (closeReason != null)
                        eventHandler.channelClosed(closeReason);
                    else
                        eventHandler.channelClosed(PaymentChannelCloseException.CloseReason.CONNECTION_CLOSED);
                    eventHandler.setConnectionChannel(null);
                }

                @Override
                public synchronized void connectionOpen(ProtobufParser<Protos.TwoWayChannelMessage> handler) {
                    //ServerConnectionEventHandler eventHandler = eventHandlerFactory.onNewConnection(address);
                    ConnectedClient eventHandler = ConnectivitySentry.getInstance().getClient(address);
                    if (eventHandler == null)
                        handler.closeConnection();
                    else {
                        ServerHandler.this.eventHandler = eventHandler;
                        paymentChannelManager.client = eventHandler;
                        eventHandler.setManager(paymentChannelManager);
                        paymentChannelManager.connectionOpen();
                    }
                }
            };

            socketProtobufHandler = new ProtobufParser<Protos.TwoWayChannelMessage>
                    (protobufHandlerListener, Protos.TwoWayChannelMessage.getDefaultInstance(), Short.MAX_VALUE, timeoutSeconds*1000);
        }

        private PaymentChannelCloseException.CloseReason closeReason;

        // The user-provided event handler
        private ServerConnectionEventHandler eventHandler;

        // The payment channel server which does the actual payment channel handling
        private final PaymentChannelServer paymentChannelManager;

        // The connection handler which puts/gets protobufs from the TCP socket
        private final ProtobufParser<Protos.TwoWayChannelMessage> socketProtobufHandler;

        // The listener which connects to socketProtobufHandler
        private final ProtobufParser.Listener<Protos.TwoWayChannelMessage> protobufHandlerListener;
    }

    /**
     * Binds to the given port and starts accepting new client connections.
     * @throws Exception If binding to the given port fails (eg SocketException: Permission denied for privileged ports)
     */
    public void bindAndStart(int port) throws Exception {
        server = new MicropaymentWebsocketServer(port, new StreamParserFactory()
        {
            @Override
            public ProtobufParser<Protos.TwoWayChannelMessage> getNewParser(InetAddress inetAddress, int port) {
                return new ServerHandler(new InetSocketAddress(inetAddress, port), timeoutSeconds).socketProtobufHandler;
            }
        });
        server.start();
    }

    /**
     * Sets up a new payment channel server which listens on the given port.
     *
     * @param broadcaster The PeerGroup on which transactions will be broadcast - should have multiple connections.
     * @param wallet The wallet which will be used to complete transactions
     * @param timeoutSeconds The read timeout between messages. This should accommodate latency and client ECDSA
     *                       signature operations.
     * @param minAcceptedChannelSize The minimum amount of coins clients must lock in to create a channel. Clients which
     *                               are unwilling or unable to lock in at least this value will immediately disconnect.
     *                               For this reason, a fairly conservative value (in terms of average value spent on a
     *                               channel) should generally be chosen.
//     * @param eventHandlerFactory A factory which generates event handlers which are created for each new connection
     */
    public PaymentChannelWebsocketServerListener(TransactionBroadcaster broadcaster, Wallet wallet,
                                                final int timeoutSeconds, Coin minAcceptedChannelSize) throws IOException
    {
        this.wallet = checkNotNull(wallet);
        this.broadcaster = checkNotNull(broadcaster);
        this.minAcceptedChannelSize = checkNotNull(minAcceptedChannelSize);
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * <p>Closes all client connections currently connected gracefully.</p>
     *
     * <p>Note that this does <i>not</i> settle the actual payment channels (and broadcast payment transactions), which
     * must be done using the {@link StoredPaymentChannelServerStates} which manages the states for the associated
     * wallet.</p>
     */
    public void close() {
        server.stop();
    }


}
