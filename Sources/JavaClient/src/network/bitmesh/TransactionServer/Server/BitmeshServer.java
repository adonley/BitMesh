/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package network.bitmesh.TransactionServer.Server;

import network.bitmesh.BitmeshConfiguration;
import network.bitmesh.BitmeshConstants;
import network.bitmesh.Sentry.Connectivity.ConnectivitySentry;
import network.bitmesh.TransactionServer.TransactionServer;
import network.bitmesh.WebServer.HttpRunner;
import network.bitmesh.WebServer.Server.HttpServerController;
import network.bitmesh.channels.PaymentChannelServer;
import network.bitmesh.channels.PaymentChannelWebsocketServerListener;
import network.bitmesh.websockets.DashboardWebsocketServer;
import org.bitcoinj.core.*;
import org.bitcoinj.utils.BriefLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BitmeshServer extends TransactionServer
{
    private static final Logger log = LoggerFactory.getLogger(BitmeshServer.class);
    private static BitmeshServer instance = null;
    private PaymentChannelWebsocketServerListener serverListener;
    private LinkedHashMap<Address, PaymentChannelServer> addressConnectionMap;
    public HashSet<String> alreadySentTxs = new HashSet<String>();
    private static final int dashboardWebsocketPort = 48911;

    // TODO: make this dynamic when we start reusing payment channels
    private Coin minAcceptedChannelSize = Transaction.MIN_NONDUST_OUTPUT.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);

    private BitmeshServer()
    {
        super(BitmeshServer.class);
        this.addressConnectionMap = new LinkedHashMap<Address, PaymentChannelServer>();
        this.alreadySentTxs = new HashSet<String>();
    }

    public static BitmeshServer getInstance()
    {
        if(instance == null)
        {
            instance = new BitmeshServer();
        }
        return instance;
    }

    public static void main(String[] args) throws Exception 
    {
        BriefLogFormatter.init();


        if(args.length < 2)
        {
            log.error("Main", "IN_FACE and OUT_FACE not provided.");
            return;
        }
        else
        {
            BitmeshConfiguration.IN_FACE = args[0];
            BitmeshConfiguration.OUT_FACE = args[1];
        }
//        server.getConfig().setMaxPrice(100);
//        server.getConfig().setMinPrice(100);
//        BitmeshConfiguration.setConfiguration(server.getConfig(), server.preferences);
        //server.getConfig().pref

        BitmeshServer server = BitmeshServer.getInstance();

        // we need to listen for transactions that pay the browsers wallet, so we can tell the browser about them
        server.wallet.addEventListener(new FundingListener(instance.addressConnectionMap));
        server.run();

        // Create a thread for the http server to run on
        Thread httpThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                log.info("Starting the HTTP server");
                HttpRunner.run(HttpServerController.class);
            }
        });
        httpThread.start();

        DashboardWebsocketServer dashboardWebsocketServer = new DashboardWebsocketServer(dashboardWebsocketPort);
        dashboardWebsocketServer.start();

        /*
            WTF was going on here? Was this testing?
        log.info("Total received: {}", server.wallet.getBalance(Wallet.BalanceType.TOTAL_RECEIVED));
        server.wallet.sendCoins(server.peerGroup, new Address(server.wallet.getParams(), "n1Hw1nZxLqbr1T35aCYZLtKreHiMVkRuYd"),
                server.wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE).subtract(Coin.valueOf(3000)));
        log.info("Total received: {}", server.wallet.getBalance(Wallet.BalanceType.TOTAL_RECEIVED)); */

    }

    /**
     * Start the micropayment server on the main thread.
     * @throws Exception encapsulates all exceptions thrown by the micropayment wallet
     */
    public void run() throws Exception 
    {
        // setting this to 0 nullifies the timeout, which is necessary if we want to have long running payment channels
        config.setNetTimeout(0);
        serverListener = new PaymentChannelWebsocketServerListener(peerGroup, wallet, config.getNetTimeout(), minAcceptedChannelSize);

        // this triggers a call to updatePeers(), which flushes iptables
        ConnectivitySentry.getInstance();

        serverListener.bindAndStart(BitmeshConstants.bitmeshPort);
    }

    public void watchAddress(Address address, PaymentChannelServer channelServer)
    {
        // TODO: this doesn't exactly do what we want
        if (addressConnectionMap.containsKey(address))
        {
            if (addressConnectionMap.get(address) != channelServer)
            {
                log.warn("Same funding address listed for multiple PaymentChannelServers");
                addressConnectionMap.remove(address);
                addressConnectionMap.put(address, channelServer);
                wallet.addWatchedAddress(address);
            }
            else
            {
                log.warn("Being told to watch same address twice.");
            }
        }
        else
        {
            log.info("Watching address: {}", address.toString());
            wallet.addWatchedAddress(address);
            addressConnectionMap.put(address, channelServer);
        }
    }

    public void stopWatchingAddress(Address address, PaymentChannelServer channelServer)
    {
        if (addressConnectionMap.containsKey(address))
        {
            if (addressConnectionMap.get(address) == channelServer)
            {
                wallet.removeWatchedAddress(address);
                addressConnectionMap.remove(address);
            }
            else
            {
                log.info("Not removing watch address because PaymentChannelServer duplicated the address.");
            }
        }
        else
        {
            log.warn("Not removing watch address because we aren't watching that address.");
        }
    }

    public Set<Transaction> getFundingTransactionsForAddress(Address address) {
        HashSet<Transaction> fundingTxs = new HashSet<Transaction>();
        for (TransactionOutput output : wallet.getWatchedOutputs(true))
        {
            if (output.getAddressFromP2PKHScript(BitmeshConfiguration.params).equals(address))
            {
                // HashSet makes sure we don't add a transaction twice
                fundingTxs.add(output.getParentTransaction());
            }
        }
        return fundingTxs;
    }

}
