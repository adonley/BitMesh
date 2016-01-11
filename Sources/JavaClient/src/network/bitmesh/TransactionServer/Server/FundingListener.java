package network.bitmesh.TransactionServer.Server;

import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.Database.PersistenceHelper;
import network.bitmesh.Statistics.VendorCashOut;
import network.bitmesh.channels.PaymentChannelServer;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;

public class FundingListener implements WalletEventListener
{
    private static final Logger log = LoggerFactory.getLogger(FundingListener.class);

    private LinkedHashMap<Address, PaymentChannelServer> addressConnectionMap;

    public FundingListener(@Nonnull LinkedHashMap<Address, PaymentChannelServer> addressConnectionMap)
    {
        this.addressConnectionMap = addressConnectionMap;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance)
    {
        for (TransactionOutput output : tx.getWalletOutputs(wallet))
        {
            PaymentChannelServer connection =
                        this.addressConnectionMap.get(output.getAddressFromP2PKHScript(wallet.getNetworkParameters()));

            // if this output is one we are watching for our clients
            if (connection != null)
            {
                connection.sendFunding(tx);
            }
        }

        BitmeshConfiguration configuration = BitmeshServer.getInstance().getConfig();
        if (wallet.getBalance().value > configuration.getRefundThreshold())
        {
            VendorCashOut cashOut = null;
            try
            {
                long tempBalance = wallet.getBalance().value - 1000;
                Wallet.SendResult result = wallet.sendCoins(BitmeshServer.getInstance().getAppKit().peerGroup(), configuration.getBtcAddress(), Coin.valueOf(tempBalance));

                log.info("Sent refund of " + tempBalance + " bits to:" + configuration.getBtcAddress().toString());

                // Creates a Cashout statistic to send up to the server

                cashOut = new VendorCashOut(result.tx.getHashAsString(),
                        wallet.getBalance().longValue() - 1000,
                        true,
                        null,
                        PersistenceHelper.getVendor());
                PersistenceHelper.addVendorCashOut(cashOut);
            }
            catch(InsufficientMoneyException e)
            {
                // Creates a Cashout statistic to send up to the server

                cashOut = new VendorCashOut(null,
                        wallet.getBalance().longValue()-1000,
                        false,
                        "Not enough money to make a refund send, balance: " + wallet.getBalance().toString(),
                        PersistenceHelper.getVendor());

                log.error("Not enough money to make a refund send, balance: " + wallet.getBalance().toString());
                e.printStackTrace();
            }
            catch(NullPointerException n)
            {
                log.error("Refund wallet was not set. Attempting to send via Xapo email instead.");
                // TODO: Send via XAPO.
            }
        }
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance)
    {

    }

    @Override
    public void onReorganize(Wallet wallet)
    {

    }

    @Override
    public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx)
    {

    }

    @Override
    public void onWalletChanged(Wallet wallet)
    {

    }

    @Override
    public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts)
    {

    }

    @Override
    public void onKeysAdded(List<ECKey> keys)
    {

    }
}
