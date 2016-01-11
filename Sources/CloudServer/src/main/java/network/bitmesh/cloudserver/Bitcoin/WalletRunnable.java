package network.bitmesh.cloudserver.Bitcoin;

import network.bitmesh.cloudserver.ServerConfig;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class WalletRunnable implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger(WalletRunnable.class.getName());
    private static WalletAppKit appKit = null;
    private static WalletAppKit testAppKit = null;

    public WalletRunnable()
    {
        super();

        log.info("Starting wallet.");

        // https://stackoverflow.com/questions/5115339/tomcat-opts-environment-variable-and-system-getenv
        File walletLoc = ServerConfig.BITMESH_TEST ?
            new File("./") :
            new File(System.getenv("persistdir"));

        if(!walletLoc.canRead() || !walletLoc.canWrite())
        {
            log.error("Cannot read or write to wallet location.");
            return;
        }

        // Initialize wallet appkit with params, location and class name
        appKit = new WalletAppKit(MainNetParams.get(), walletLoc, "mainnet");
        appKit.setAutoSave(true);

        testAppKit = new WalletAppKit(TestNet3Params.get(), walletLoc, "testnet");
        testAppKit.setAutoSave(true);
    }

    // TODO: implement thread interruptable
    public void run()
    {
        log.info("Beginning run loop");
        // Start the sync in the run method to avoid blocking on assignment
        appKit.startAsync();
        testAppKit.startAsync();

        appKit.awaitRunning();
        testAppKit.awaitRunning();

        appKit.setBlockingStartup(true);
        testAppKit.setBlockingStartup(true);

        appKit.peerGroup().setMinBroadcastConnections(ServerConfig.MIN_PEERS_TO_BROADCAST);
        testAppKit.peerGroup().setMinBroadcastConnections(ServerConfig.MIN_PEERS_TO_BROADCAST);
        appKit.peerGroup().setMaxConnections(ServerConfig.MAX_PEER_CONNECTIONS);
        testAppKit.peerGroup().setMaxConnections(ServerConfig.MAX_PEER_CONNECTIONS);

        // This is for testing - don't want to use localhost as a peer
        appKit.peerGroup().setUseLocalhostPeerWhenPossible(false);
        testAppKit.peerGroup().setUseLocalhostPeerWhenPossible(false);
        appKit.setAutoStop(true);
        testAppKit.setAutoStop(true);
    }

    public static Wallet getWallet()
    {
        if(appKit == null)
            log.error("appKit not initialized when wallet requested.");
        return appKit.wallet();
    }

    public static NetworkParameters getParams()
    {
        if(appKit == null)
            log.error("appKit not initialized when params requested.");
        return appKit.params();
    }

    public static PeerGroup getPeergroup()
    {
        if(appKit == null)
            log.error("appKit not initialized when peergroup requested.");
        return appKit.peerGroup();
    }

    public static WalletAppKit getAppKit()
    {
        if(appKit == null)
            log.error("appKit not initialized when appkit requested.");
        return appKit;
    }

    public static Wallet getTestWallet()
    {
        if(testAppKit == null)
            log.error("appKit not initialized when wallet requested.");
        return testAppKit.wallet();
    }

    public static NetworkParameters getTestParams()
    {
        if(testAppKit == null)
            log.error("appKit not initialized when params requested.");
        return testAppKit.params();
    }

    public static PeerGroup getTestPeergroup()
    {
        if(testAppKit == null)
            log.error("appKit not initialized when peergroup requested.");
        return testAppKit.peerGroup();
    }

    public static WalletAppKit getTestAppKit()
    {
        if(testAppKit == null)
            log.error("appKit not initialized when appkit requested.");
        return testAppKit;
    }

}
