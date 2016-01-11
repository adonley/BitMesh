package network.bitmesh.cloudserver.Servlets;

import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.junit.Test;

import java.io.File;

/**
 * Created by andrew on 6/21/15.
 */
public class UnspentTransactionServletTest
{
    private WalletAppKit appKit;
    private Wallet wallet;
    private File walletLocation = new File("wallet");

    @Test
    public void testDoPost() throws Exception
    {

    }
}
