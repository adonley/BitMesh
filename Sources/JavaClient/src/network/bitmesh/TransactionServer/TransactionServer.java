package network.bitmesh.TransactionServer;

import com.google.common.collect.ImmutableList;
import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.Database.PersistenceHelper;
import network.bitmesh.Statistics.Vendor;
import network.bitmesh.channels.StoredPaymentChannelServerStates;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.prefs.Preferences;

public abstract class TransactionServer
{
    private static final Logger log = LoggerFactory.getLogger(TransactionServer.class);
    protected Preferences preferences;
    protected BitmeshConfiguration config = null;
    protected WalletAppKit appKit;
    protected PeerGroup peerGroup;
    protected Wallet wallet;
    protected Context context;

    private StoredPaymentChannelServerStates storedStates;

    /**
     * Initializes all the standard variables for the BitMesh server and client
     */
    public TransactionServer(Class cls)
    {
        // Preferences init //
        preferences = Preferences.userRoot().node(cls.getName().toString());
        config = getConfigurationFromPreferences();
        // TODO: Add coordinates if applicable
        Vendor vendor = new Vendor(config.getMac());
        PersistenceHelper.addVendor(vendor);


        // Wallet init //
        appKit =  new WalletAppKit(BitmeshConfiguration.params, BitmeshConfiguration.WALLET_LOCATION,
                cls.getSimpleName().toString())
        {
            @Override
            protected List<WalletExtension> provideWalletExtensions()
            {
                storedStates = new StoredPaymentChannelServerStates(null);
                return ImmutableList.<WalletExtension>of(storedStates);
            }
        };
        // This needs to happen before we start the appKit
        // PeerGroup Configuration //
        /*if (BitmeshConstants.BITMESH_TEST)
        {
            appKit.setPeerNodes(new PeerAddress(InetAddresses.forString("54.153.18.210"),
                    BitmeshConfiguration.params.getPort()));
        }*/

        // TODO: put UI wallet syncing signal
        log.info("Starting and syncing the appKit");
        appKit.startAsync();
        appKit.awaitRunning();
        log.info("Appkit completed.");

        // This needs to happen *after we sync the appkit
        wallet = appKit.wallet();
        peerGroup = appKit.peerGroup();
        peerGroup.setUseLocalhostPeerWhenPossible(false);
        peerGroup.setMaxConnections(8);
        wallet.cleanup();
        log.info("Wallet balance: {}", wallet.getBalance().toFriendlyString());

        // This is needed for an obscure bug that occurs at the end of the payment channel
        storedStates.setTransactionBroadcaster(peerGroup);


        // TODO: Is this still needed?
        //wallet.addOrGetExistingExtension(new StoredPaymentChannelServerStates(null));
    }

    /**
     * Grabs the BitmeshConfiguration for this class
     * @return new or old configuration based upon whether it was in preferences or not
     */
    protected BitmeshConfiguration getConfigurationFromPreferences()
    {
        return BitmeshConfiguration.getConfigFromPreferences(preferences);
    }

    /**
     * Saves the configuration to preferences and changes the current server's configuration
     * to the new one.
     * @param configuration - configuration to change to
     */
    public void setConfiguration(@Nonnull BitmeshConfiguration configuration)
    {
        // Save the configuration to preferences
        BitmeshConfiguration.setConfiguration(configuration, preferences);
        this.config = configuration;
    }

    public BitmeshConfiguration getConfig()
    {
        if(config == null)
            getConfigurationFromPreferences();
        return config;
    }

    public WalletAppKit getAppKit() { return appKit; }

    public Wallet getWallet() { return wallet; }

    public StoredPaymentChannelServerStates getStoredStates() { return storedStates; }

    public Preferences getPreferences() { return preferences; }
}
