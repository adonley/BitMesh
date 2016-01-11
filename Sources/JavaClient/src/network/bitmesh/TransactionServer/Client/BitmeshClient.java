/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package network.bitmesh.TransactionServer.Client;

import com.google.common.util.concurrent.FutureCallback;
import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.BitmeshConstants;
import network.bitmesh.TransactionServer.TransactionServer;
import network.bitmesh.WebServer.Client.HttpClientController;
import network.bitmesh.WebServer.HttpRunner;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import network.bitmesh.channels.*;
import org.bitcoinj.core.*;
import org.bitcoinj.utils.Threading;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

/**
 * Simple client that connects to the given host, opens a channel, and pays one cent.
 */
public class BitmeshClient extends TransactionServer
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BitmeshClient.class);
    private Preferences clientPrefs = Preferences.userRoot().node(BitmeshClient.class.getName());

    private volatile boolean shouldContinue = false;
    private final ECKey channelKey;
    private boolean serverIsBitmeshNode = true;
    public Object lock = new Object();

    private Timer purchaseTimer;
    private TimerTask purchaseTimerTask;
    private PaymentChannelClientConnection clientConnection;
    private CountDownLatch latch;
    private HttpClientController clientController;
    private Thread micropaymentChannelThread;

    private static BitmeshClient bitmeshClient = null;

    public static void main(String[] args)
    {
        BitmeshClient client = BitmeshClient.getInstance();

        // Start the server
        HttpRunner.run(HttpClientController.class);
    }

    private BitmeshClient()
    {
        super(BitmeshClient.class);
        channelKey = new ECKey();
        clientController = (HttpClientController)HttpClientController.getInstance();
    }

    public static BitmeshClient getInstance()
    {
        if (bitmeshClient == null)
        {
            bitmeshClient = new BitmeshClient();
        }
        return bitmeshClient;
    }

    public void run(final String host) throws ConnectException
    {
        final InetSocketAddress server = new InetSocketAddress(host, 11984);
        final String channelID = host;
        // If the thread isn't running
        if(micropaymentChannelThread == null || !micropaymentChannelThread.isAlive())
        {
            micropaymentChannelThread = new Thread(new MicropaymentRunnable(server, channelID));
            micropaymentChannelThread.start();
        }
    }

    private class MicropaymentRunnable implements Runnable
    {

        protected InetSocketAddress server;
        protected String channelID;

        public MicropaymentRunnable(InetSocketAddress server, String channelID)
        {
            this.channelID = channelID;
            this.server = server;
        }

        @Override
        public void run()
        {
            // TODO: Test for this and use the if statement
            serverIsBitmeshNode = true;

            shouldContinue = true;
            // We now have active network connections and a fully synced wallet.
            // Add a new key which will be used for the multisig contract.
            wallet.importKey(channelKey);
            wallet.allowSpendingUnconfirmedTransactions();

            final int timeoutSecs = 120;

            // try to open a channel. if we fail because it was not a bitmesh node, send that info back to
            // caller via a
            try
            {
                // TODO: Check for internet connectivity
                // While the user is cool paying
                while(shouldContinue)
                    openAndSend(timeoutSecs, this.server, channelID);
            }
            catch (ValueOutOfRangeException ex)
            {
                ex.printStackTrace();
            }
            catch (InterruptedException ex)
            {
                log.warn("Thread interrupted while waiting for micropayment thread");
                ex.printStackTrace();
            }
            catch (IOException ex)
            {
                log.warn("Something thru an IOException");
                ex.printStackTrace();
            }
        }
    }

    private void openAndSend(int timeoutSecs,
                             InetSocketAddress server,
                             String channelID) throws ValueOutOfRangeException, InterruptedException, IOException

    {
        // probably should display a UI message here if waiting
        clientConnection = new PaymentChannelClientConnection(
                server, timeoutSecs, wallet, channelKey, Coin.valueOf(config.getEscrow()), channelID);

        // Opening the channel requires talking to the server, so it's asynchronous.
        latch = new CountDownLatch(1);

        // open the channel
        // client.getChannelOpenFuture().get() throws an InsufficientMoneyException
        // when there are insufficient funds for escrow
        Futures.addCallback(clientConnection.getChannelOpenFuture(), getClientConnectionCallback(), Threading.USER_THREAD);

        // wait here until we need to open a new channel

        latch.await();
        log.info("Client closing out");
    }

    private void waitForSufficientBalance(Coin amount)
    {
        // TODO
        // Not enough money in the wallet.
        Coin amountPlusFee = amount.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB);

        // ESTIMATED because we don't really need to wait for confirmation.
        ListenableFuture<Coin> balanceFuture = wallet.getBalanceFuture(amountPlusFee, Wallet.BalanceType.ESTIMATED);

        if (!balanceFuture.isDone())
        {
            // TODO: Andrew put UI message here
            System.out.println("Please send " + amountPlusFee.toFriendlyString() +
                    " to " + channelKey.toAddress(BitmeshConfiguration.params));
            Futures.getUnchecked(balanceFuture);
        }
    }

    private FutureCallback<PaymentChannelClientConnection> getClientConnectionCallback()
    {
        return new FutureCallback<PaymentChannelClientConnection>()
        {
            @Override
            public void onSuccess(PaymentChannelClientConnection client)
            {
                // By the time we get here, if the channel is new then we already made a micropayment! The reason is,
                // we are not allowed to have payment channels that pay nothing at all.
                log.info("Success! Already paid {} satoshis on this channel",
                        client.state().getValueSpent());
                // TODO: We seem to do this twice
                clientConnection = client;
                /*try
                {
                    HttpClientController.getInstance().setPaymentChannel();
                }
                catch(UninitializedException e)
                {
                    // Can't happen because we are only in this logic because the client started up
                    e.printStackTrace();
                }*/
                purchaseTimer = new Timer(true);
                purchaseTimerTask = new PurchaseTimerTask();
                purchaseTimer.scheduleAtFixedRate(purchaseTimerTask, 1000, 1000);
            }

            @Override
            public void onFailure(Throwable throwable)
            {
                log.error("Failed to open connection", throwable);

                if (throwable instanceof PaymentChannelCloseException)
                {
                    PaymentChannelCloseException exception = (PaymentChannelCloseException) throwable;
                    // if the server wasn't up or something else...
                    if (exception.getCloseReason() == PaymentChannelCloseException.CloseReason.CONNECTION_CLOSED)
                    {
                        serverIsBitmeshNode = false;
                    }
                }
                if (throwable instanceof ExecutionException ||
                        throwable instanceof InsufficientMoneyException)
                {
                    //TODO: Notify user of insufficient funds
                    log.error("Insufficient Funds");
                    // TODO: payment channel code checks for this somewhere else
                    waitForSufficientBalance(BitmeshConstants.coinWidth);
                }

                latch.countDown();
            }
        };
    }

    // this should block until the user has received the data +- error buffer
    // return false on timeout?
    private boolean listenForData() throws InterruptedException
    {
        log.info("Receiving internet for 5 seconds");

        /*
        synchronized(lock)
        {
            lock.wait();
        }
                */
        return true;
    }

    private void turnOffMicropayments()
    {
        log.info("Settling channel.");
        clientConnection.settle();
        latch.countDown();
        purchaseTimerTask.cancel();
    }

    public class PurchaseTimerTask extends TimerTask
    {
        public void run()
        {
            try
            {

                int compare = clientConnection.state().getValueRefunded().compareTo(BitmeshConstants.coinWidth);
                if (compare > 0)
                {
                    log.info("Incrementing payment");
                    try
                    {
                        PaymentIncrementAck ack = Uninterruptibles.getUninterruptibly(clientConnection.incrementPayment(BitmeshConstants.coinWidth));
                        log.info("Payment Ack value {}", ack.getValue());
                        clientController.sendPaymentAckValue(ack.getValue());
                    }
                    catch (Exception e)
                    {
                        log.error("Problem incrementing the payment");
                        e.printStackTrace();
                    }
                    return;
                }
                else if (compare == 0)
                {
                    log.info("Incrementing payment last time for this channel");
                    clientConnection.incrementPayment(BitmeshConstants.coinWidth);
                    turnOffMicropayments();
                }
            }
            catch (ValueOutOfRangeException e)
            {
                // either tried to increment by negative value or by
                // more than the remaining amount in escrow (more likely)
                log.error("Failed to increment payment by {}, remaining value is {}",
                        BitmeshConstants.coinWidth, clientConnection.state().getValueRefunded());
                turnOffMicropayments();
            }
            /*catch (ExecutionException e)
            {
                log.error("Failed to increment payment", e);
                turnOffMicropayments();
            }*/
        }
    }

    /**
     * Get the gateway address of the current connection. I think this only
     * works for linux and mac right now.
     *
     * @return gateway adress of the access point
     * @throws IOException
     */
    public static String retrieveGateway() throws IOException
    {
        Process result = Runtime.getRuntime().exec("netstat -rn");
        BufferedReader output = new BufferedReader(new InputStreamReader(result.getInputStream()));
        String s = "";
        do
        {
            s = output.readLine();
            // Some clients have 0.0.0.0 for their default host
        } while (!s.startsWith("0.0.0.0") && !s.startsWith("default"));
        s = s.replaceAll("\\s+", " ");
        String[] netstatArray = s.split(" |\n|\t");
        return netstatArray[1];
    }

    public void closeConnection()
    {
        shouldContinue = false;
        purchaseTimerTask.cancel();
        clientConnection.settle();
        micropaymentChannelThread = null;
    }
}
