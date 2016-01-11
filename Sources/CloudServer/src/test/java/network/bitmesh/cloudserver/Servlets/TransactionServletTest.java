package network.bitmesh.cloudserver.Servlets;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import network.bitmesh.cloudserver.Bitcoin.WalletContextListener;
import network.bitmesh.cloudserver.Utils.CommonResponses;
import network.bitmesh.cloudserver.ServerConfig;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

public class TransactionServletTest
{
    private static final Logger log = LoggerFactory.getLogger(TransactionServletTest.class);

    private WalletAppKit appKit;
    private Wallet wallet;
    private File walletLocation = new File("wallet");
    private int lockTime = 10;

    @Test
    public void testDoPost() throws Exception
    {
        // Wait for funding.
        Coin amount = Coin.valueOf(5000);
        ListenableFuture<Coin> balanceFuture = appKit.wallet().getBalanceFuture(amount, Wallet.BalanceType.ESTIMATED);
        if (!balanceFuture.isDone())
        {
            log.info("Waiting for " + amount.toFriendlyString() + " to " + wallet.currentReceiveAddress());
            Futures.getUnchecked(balanceFuture);
        }

        // Prep transaction (requires running host)
        Address address = new Address(TestNet3Params.get(), TestConfig.testNetAddr);
        Wallet.SendRequest request = Wallet.SendRequest.to(address, Coin.valueOf(10000));
        wallet.completeTx(request);
        Transaction transaction = request.tx;

        // Make sure this is in GMT
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        // This is in seconds not milliseconds
        transaction.setLockTime((now.getTime()/1000L) + lockTime);

        log.info("Transaction will be good at: " + new Date(now.getTime() + lockTime*1000).toString());

        // Need non-zero sequence number on at least one input
        transaction.getInputs().get(0).setSequenceNumber(0);

        wallet.signTransaction(request);

        try
        {
            transaction.verify();
        }
        catch(Exception e)
        {
            log.info("Problems verifying transaction.");
            e.printStackTrace();
            return;
        }

        transaction.bitcoinSerialize();

        URL url = TestConfig.urlBuilder("transaction");

        // Open connection to host
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set headers
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", CommonResponses.MIME_TYPES.get("bin"));
        byte[] serializedTransaction = transaction.bitcoinSerialize();
        connection.setRequestProperty("Content-Length", new Integer(serializedTransaction.length).toString());
        connection.setDoOutput(true);

        InputStream file = new ByteArrayInputStream(serializedTransaction);
        OutputStream output = connection.getOutputStream();

        try
        {
            byte[] buffer = new byte[4096];
            int length;
            while((length = file.read(buffer)) > 0)
            {
                output.write(buffer, 0, length);
            }
            output.flush();
        }
        catch(Exception e)
        {
            log.error("Error opening the streams to send bitcoin transaction");
            e.printStackTrace();
        }
        finally
        {
            output.close();
            file.close();
        }

        String message = connection.getResponseMessage();
        int responseCode = connection.getResponseCode();
        log.info("Response Message: " + message + " Response code: " + responseCode);
        connection.disconnect();
        assert(responseCode == 200);
    }

    @Before
    public void setUp() throws Exception
    {
        appKit = new WalletAppKit(TestNet3Params.get(), walletLocation, TransactionServletTest.class.getName().toString());
        appKit.setAutoSave(true);
        appKit.startAsync();
        appKit.awaitRunning();
        appKit.setBlockingStartup(true);
        wallet = appKit.wallet();
        log.info("Wallet Address " + wallet.currentReceiveAddress());
    }

    @After
    public void tearDown() throws Exception
    {
        appKit.setAutoStop(true);
    }

}