package network.bitmesh;

import network.bitmesh.Units.PurchaseUnit;
import network.bitmesh.Units.TimeUnit;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.prefs.Preferences;

public class BitmeshConfiguration
{
    private static final Logger log = LoggerFactory.getLogger(BitmeshConfiguration.class);

    /**
     * Enum to facilitate accessing the preferences
     */
    private enum ConfigConstants
    {
        ESCROW           ("escrow"),
        MIN_PRICE        ("min_price"),
        MAX_PRICE        ("max_price"),
        PURCHASE_TYPE    ("purchase_type"),
        MAC_ADDRESS      ("mac_address"),
        EMAIL_ADDRESS    ("email_address"),
        BTC_ADDRESS      ("btc_address"),
        REFUND_THRESHOLD ("refund_threshold");

        private final String value;

        ConfigConstants(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }

    // Default values
    // TODO: Make this location absolute. (Think running from commandline different dir)
    public static String BITMESH_LOCATION = "/opt/bitmesh";
    public static String IN_FACE = "";
    public static String OUT_FACE = "";
    public static File WALLET_LOCATION = new File(BITMESH_LOCATION + File.separator + "wallet");
    public static long defaultEscrow = 10000;
    public static long defaultRefundThreshold = 10000;
    public static long defaultMaxPrice = 100;
    public static long defaultMinPrice = 100;
    public static PurchaseUnit defaultPurchaseUnit = new TimeUnit();
    public static final NetworkParameters params = BitmeshConstants.BITMESH_TEST ?
            TestNet3Params.get() : MainNetParams.get();

    private long minPrice;
    private long maxPrice;
    private long negotiatedPrice;
    private long escrow;

    private long refundThreshold;
    private String mac = null;

    private String emailAddress = "";
    private String btcAddress = "";
    private Address address = null;

    private int netTimeout = 60;
    // TODO: is this really necessary? Shouldn't this be somewhere else?
    private int port = 8080;

    public PurchaseUnit purchaseUnit;

    public BitmeshConfiguration(@Nonnull PurchaseUnit purchaseUnit,
                                long maxPrice,
                                long minPrice)
    {
        // TODO: Figure out a better scheme for this
        this.negotiatedPrice = minPrice;
        this.purchaseUnit = purchaseUnit;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.escrow = defaultEscrow;
        this.mac = generateIdentifier();
    }

    public static BitmeshConfiguration defaultConfiguration()
    {
        // TODO: Refactor these into comments?
        return new BitmeshConfiguration(new TimeUnit(), defaultMaxPrice, defaultMinPrice);
    }

    public long getMinPrice()
    {
        return minPrice;
    }

    public void setMinPrice(long minPrice)
    {
        this.minPrice = minPrice;
    }

    public long getMaxPrice()
    {
        return maxPrice;
    }

    public void setMaxPrice(long maxPrice)
    {
        this.maxPrice = maxPrice;
    }

    public long getNegotiatedPrice()
    {
        return negotiatedPrice;
    }

    public void setNegotiatedPrice(long price)
    {
        negotiatedPrice = price;
    }

    public PurchaseUnit getPurchaseUnit()
    {
        return purchaseUnit;
    }

    public void setPurchaseUnit(PurchaseUnit unit)
    {
        purchaseUnit = unit;
    }

    public long getEscrow()
    {
        return escrow;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress)
    {
        this.emailAddress = emailAddress;
    }

    public int getNetTimeout()
    {
        return netTimeout;
    }

    public void setNetTimeout(int timeout)
    {
        netTimeout = timeout;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public long getCoinWidth()
    {
        return this.getNegotiatedPrice();
    }

    public long getPurchaseWidth()
    {
        return 1;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    public String getBtcAddressString()
    {
        return btcAddress;
    }

    public long getRefundThreshold()
    {
        return refundThreshold;
    }

    public void setRefundThreshold(long refundThreshold)
    {
        this.refundThreshold = refundThreshold;
    }

    public Address getBtcAddress()
    {
        if(address == null)
        {
            log.info("Returned a null address from getBtcAddress");
        }
        return address;
    }

    public void setBtcAddressString(String btcAddress)
    {
        try
        {
            address = new Address(BitmeshConfiguration.params, btcAddress);
            this.btcAddress = btcAddress;
        }
        catch(AddressFormatException e)
        {
            log.error("BTC Address was formatted incorrectly: {}.", address);
            e.printStackTrace();
        }
    }

    /**
     * Grabs the mac address or generates a unique identifier to identify clients / servers
     * using BitMesh.
     *
     * @return unique identifier for a computer using BitMesh
     */
    public static String generateIdentifier()
    {
        String id = null;
        try
        {
            // Try to grab the IP address
            InetAddress ip;
            ip = InetAddress.getLocalHost();

            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++)
            {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            id = sb.toString();
        }
        catch (Exception e)
        {
            // If we can't grab the IP address then make a new random
//            e.printStackTrace();
            log.info("Could not get IP address. Generating new random id");
            // Generate a random identifier that is 32 characters in length
            SecureRandom random = new SecureRandom();
            id = new BigInteger(130, random).toString(32);
        }
        return id;
    }

    /**
     * Grabs a configuration out of system java preferences.
     *
     * @param preferences preferences object / location to grab the configuration
     * @return new configuration based on what was saved in the preferences
     */
    public static BitmeshConfiguration getConfigFromPreferences(Preferences preferences)
    {
        BitmeshConfiguration configuration;

        // Getting the values of the configuratoin class out of the preferences with default values
        long escrow = preferences.getLong(ConfigConstants.ESCROW.value(), defaultEscrow);
        long minPrice = preferences.getLong(ConfigConstants.MIN_PRICE.value(), defaultMinPrice);
        long maxPrice = preferences.getLong(ConfigConstants.MAX_PRICE.value(), defaultMaxPrice);

        String type = preferences.get(ConfigConstants.PURCHASE_TYPE.value(), defaultPurchaseUnit.getType());

        PurchaseUnit purchaseUnit;

        try
        {
            Class purchaseUnitClass = Class.forName(type);
            Class[] argumentClasses = {Long.class, Coin.class};
            Constructor purchaseUnitConstructor = purchaseUnitClass.getConstructor(argumentClasses);

            // TODO: Make this more generic, maybe some sort of static reflection method
            Long size = preferences.getLong("size", 5L);
            Coin price = Coin.valueOf(preferences.getLong("price", 1000L));
            Object[] args = {size, price};
            purchaseUnit = (PurchaseUnit) purchaseUnitConstructor.newInstance(args);
            log.info("Successfully initiated a purchaseunit from the preferences.");
        }
        catch (Exception e)
        {
            log.error("Could not initiate a purchaseunit from the preferences.");
            e.printStackTrace();
            // TODO: Make this more generic - fuckin static factory method anus
            purchaseUnit = new TimeUnit();
            purchaseUnit.setPrice(5L, Coin.valueOf(BitmeshConfiguration.defaultMinPrice));
        }

        configuration = new BitmeshConfiguration(purchaseUnit, maxPrice, minPrice);

        // Gets the default send threshold
        configuration.setRefundThreshold(preferences.getLong(ConfigConstants.REFUND_THRESHOLD.value(), defaultRefundThreshold));

        // Get email address, "" if there is nothing saved in preferences
        configuration.setEmailAddress(preferences.get(ConfigConstants.EMAIL_ADDRESS.value(), ""));
        // Attempts to get the btc return address from the preferences file
        configuration.setBtcAddressString(preferences.get(ConfigConstants.BTC_ADDRESS.value(), ""));

        // Create an identifier and save it off if it is the first time this is accessed
        configuration.setMac(preferences.get(ConfigConstants.MAC_ADDRESS.value(), generateIdentifier()));

        log.info("Got config: {}", configuration);

        setConfiguration(configuration,preferences);
        return configuration;
    }

    /**
     * Makes a copy of the current BitMesh Configuration. Used when connected clients might have variant
     * prices on internet.
     * @return A duplicate of the bitmesh configuration
     */
    public BitmeshConfiguration makeCopy()
    {
        // TODO: This is wrong for all the other configurable parts of a BitMesh Config
        return new BitmeshConfiguration(purchaseUnit.makeCopy(), this.minPrice, this.maxPrice);
    }

    /**
     * Puts a Bitmesh configuration object into the preferences when called
     *
     * @param configuration
     */
    public static void setConfiguration(@Nonnull BitmeshConfiguration configuration, @Nonnull Preferences preferences)
    {
        // General Configuration Values //
        preferences.putLong(ConfigConstants.ESCROW.value(), configuration.getEscrow());
        preferences.putLong(ConfigConstants.MIN_PRICE.value(), configuration.getMinPrice());
        preferences.putLong(ConfigConstants.MAX_PRICE.value(), configuration.getMaxPrice());
        preferences.putLong(ConfigConstants.REFUND_THRESHOLD.value(), configuration.getRefundThreshold());
        preferences.put(ConfigConstants.MAC_ADDRESS.value(), configuration.getMac());
        preferences.put(ConfigConstants.EMAIL_ADDRESS.value(), configuration.getEmailAddress());
        preferences.put(ConfigConstants.BTC_ADDRESS.value(), configuration.getBtcAddressString());

        // Purchase Unit Values //
        preferences.put(ConfigConstants.PURCHASE_TYPE.value(), configuration.purchaseUnit.getType());
        preferences.putLong("size", configuration.purchaseUnit.getSize());
        preferences.putLong("price", configuration.purchaseUnit.getPrice().value);

        log.info("Set the config.\nCurrent config: {}", configuration.toString());
    }

    public void saveCurrentConfiguration(@Nonnull Preferences preferences)
    {
        setConfiguration(this, preferences);
    }

    @Override
    public String toString()
    {
        return "BitmeshConfiguration{" +
                "minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", negotiatedPrice=" + negotiatedPrice +
                ", escrow=" + escrow +
                ", refundThreshold=" + refundThreshold +
                ", mac='" + mac + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", btcAddress='" + btcAddress + '\'' +
                ", address=" + address +
                ", netTimeout=" + netTimeout +
                ", port=" + port +
                ", purchaseUnit=" + purchaseUnit +
                '}';
    }
}
