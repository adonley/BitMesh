package network.bitmesh;

import network.bitmesh.Units.DataUnit;
import org.bitcoinj.core.Coin;

import java.util.concurrent.TimeUnit;

public class BitmeshConstants 
{
    static public final Boolean BITMESH_TEST = true;
	static public final Coin escrowAmount = Coin.valueOf(10000);
	static public final Coin coinWidth = Coin.valueOf(1000);
	static public final long dataWidth = 1;
    static public final long dataUpFront = 10;
    static public TimeUnit timeUnitServed = TimeUnit.SECONDS;
    static public long timeWidth = 5;
    static public int bitmeshPort = 11984;
    static public final int INITIAL_GRACE_PERIOD = 5000; // Milliseconds right now
    static public boolean test = true;
}
