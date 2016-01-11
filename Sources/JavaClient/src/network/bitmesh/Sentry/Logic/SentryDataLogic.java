package network.bitmesh.Sentry.Logic;

import network.bitmesh.BitmeshConfiguration;
//import bitmesh.bitmeshmicropayments.jni.pcap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by c on 4/7/15.
 */
public class SentryDataLogic implements SentryLogic
{
    // TODO: make this work with jni or jpcap
    private static final Logger log = LoggerFactory.getLogger(SentryDataLogic.class);

    private final BitmeshConfiguration config;
    private long bytesPurchased = 0;
    private String client;

    public SentryDataLogic(BitmeshConfiguration config, String client)
    {
        this.config = config;
        this.client = client;
        //dataCollection = BitMeshDataCollection.getInstance();
    }

    public void paymentIncreased(long increasedTo)
    {
        log.info("payment increased to {}", increasedTo);
        bytesPurchased = (increasedTo/config.getCoinWidth()) * config.getPurchaseWidth();
    }

    public boolean accountFunded()
    {
        /*
        log.info("bytesPurchased {} dataCollection.getDataDownForClient(client) {} dataCollection.getDataUpForClient(client) {}", bytesPurchased,
                 dataCollection.getDataDownForClient(client), dataCollection.getDataUpForClient(client));
        return bytesPurchased >= dataCollection.getDataDownForClient(client) + dataCollection.getDataUpForClient(client);
    */
        return true;
    }
}
