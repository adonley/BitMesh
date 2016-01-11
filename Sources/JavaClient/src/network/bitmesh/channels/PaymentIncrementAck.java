package network.bitmesh.channels;

import com.google.protobuf.ByteString;
import org.bitcoinj.core.Coin;

import javax.annotation.Nullable;

/**
 * An acknowledgement of a payment increase
 */
public class PaymentIncrementAck {
    private final Coin value;
    @Nullable private final ByteString info;

    public PaymentIncrementAck(Coin value, @Nullable ByteString info) {
        this.value = value;
        this.info = info;
    }

    public Coin getValue() {
        return value;
    }

    @Nullable
    public ByteString getInfo() {
        return info;
    }
}
