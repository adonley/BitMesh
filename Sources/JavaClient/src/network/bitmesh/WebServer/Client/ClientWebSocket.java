package network.bitmesh.WebServer.Client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by andrew on 4/21/15.
 */
public class ClientWebSocket extends WebSocket
{
    private static final Logger log = LoggerFactory.getLogger(ClientWebSocket.class);
    private Wallet wallet;
    private long ident;

    private enum WebsocketMessageType
    {
        ADDRESS_UPDATE("ADDRESS_UPDATE"),
        BALANCE_UPDATE("BALANCE_UPDATE"),
        PAYMENT_UPDATE("PAYMENT_UPDATE"),
        CHANNEL_CLOSED("CHANNEL_CLOSED"),
        CHANNEL_OPENED("CHANNEL_OPENED"),
        KEEP_ALIVE("KEEP_ALIVE");

        private final String val;

        private WebsocketMessageType(final String val)
        {
            this.val = val;
        }

        @Override
        public String toString()
        {
            return val;
        }
    }

    public ClientWebSocket(NanoHTTPD.IHTTPSession handshakeRequest, Wallet wallet)
    {
        super(handshakeRequest);
        this.wallet = wallet;
        this.ident = new Date().getTime();
        wallet.addEventListener(new WalletEventListener()
        {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance)
            {
                // Update the balance and new receive address when there's money added to the wallet
                ClientWebSocket.this.updateBalance(newBalance.getValue());
                ClientWebSocket.this.updateAddress(wallet.currentReceiveAddress().toString());
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
        });
    }

    @Override
    protected void onPong(WebSocketFrame pongFrame)
    {
        String message = pongFrame.getTextPayload();
        log.info("Pong {}", message);
    }

    @Override
    protected void onMessage(WebSocketFrame messageFrame)
    {
        String message = messageFrame.getTextPayload();
        // Do nothing if it is a keep alive response
        if(message.equals(WebsocketMessageType.KEEP_ALIVE.toString()))
            return;

        log.info("Message {}", message);

        try
        {
            send("OK");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote)
    {
        log.info("Closed connection. Code: {} Reason: {} Initiated By Remote: {}", code, reason, initiatedByRemote);
        ClientWebsocketManager.removeWebSocket(this);
    }

    @Override
    protected void onException(IOException e)
    {
        e.printStackTrace();
    }

    @Override
    protected void handleWebsocketFrame(WebSocketFrame frame) throws IOException
    {
        super.handleWebsocketFrame(frame);
    }

    @Override
    public synchronized void sendFrame(WebSocketFrame frame) throws IOException
    {
        super.sendFrame(frame);
    }

    public synchronized void keepAlive()
    {
        /*try
        {
            Gson gson = new GsonBuilder().create();
            HashMap<String, String> jsonMap = new HashMap<String, String>();
            jsonMap.put("type", WebsocketMessageType.KEEP_ALIVE.toString());
            send(gson.toJson(jsonMap).toString().getBytes("UTF8"));
        }
        catch(Exception e)
        {
            // TODO: Create back off for server.
            log.error("Failed to send keep alive request.");
            e.printStackTrace();
        }*/

        try
        {
            ping(new byte[]{});
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void updateBalance(long balance)
    {
        try
        {
            Gson gson = new GsonBuilder().create();
            HashMap<String, String> jsonMap = new HashMap<String, String>();
            jsonMap.put("type", WebsocketMessageType.BALANCE_UPDATE.toString());
            jsonMap.put("balance", new Long(balance).toString());
            send(gson.toJson(jsonMap).toString().getBytes("UTF8"));
        }
        catch(Exception e)
        {
            log.error("Failed to send balance.");
            e.printStackTrace();
        }
    }

    public synchronized void updateAddress(String address)
    {
        try
        {
            Gson gson = new GsonBuilder().create();
            HashMap<String, String> jsonMap = new HashMap<String, String>();
            jsonMap.put("type", WebsocketMessageType.ADDRESS_UPDATE.toString());
            jsonMap.put("address", address);
            send(gson.toJson(jsonMap).toString().getBytes("UTF8"));
        }
        catch(Exception e)
        {
            log.error("Failed to send balance.");
            e.printStackTrace();
        }
    }

    public synchronized void updatePayment(Coin amount)
    {
        try
        {
            Gson gson = new GsonBuilder().create();
            HashMap<String, String> jsonMap = new HashMap<String, String>();
            jsonMap.put("type", WebsocketMessageType.PAYMENT_UPDATE.toString());
            jsonMap.put("amount", amount.toString());
            send(gson.toJson(jsonMap).toString().getBytes("UTF8"));
        }
        catch(Exception e)
        {
            log.error("Failed to send balance.");
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        final ClientWebSocket other = (ClientWebSocket) obj;

        if(this.ident != other.ident)
        {
            return false;
        }

        return true;
    }
}
