package network.bitmesh.cloudserver.Statistics;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import network.bitmesh.cloudserver.Utils.PersistenceHelper;

import javax.persistence.*;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

@Entity

@Table(name = "SELL")
public class Sell implements Serializable
{
    @Expose
    @Id @GeneratedValue
    @Column(name = "SELL_ID", nullable=false, unique=true)
    private long sellId;

    @Expose
    @Column(name = "TXID")
    private String txId;

    @Expose
    @Column(name = "DATE")
    private Date date;

    @Expose
    @Column(name = "AMOUNT_STATOSHI")
    private long amountStatoshi;

    @Expose
    @Column(name = "METER_TYPE")
    private String type;

    @Expose
    @Column(name = "AMOUNT_INTERNET")
    private long amountInternet;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "VENDOR_ID", nullable = false)
    private Vendor vendor;

    public Sell() { }

    public Sell(long amountStatoshi, String type, long amountInternet, Vendor vendor, String txId)
    {
        this.amountStatoshi = amountStatoshi;
        this.type = type;
        this.amountInternet = amountInternet;
        this.vendor = vendor;
        this.txId = txId;
        this.date = Calendar.getInstance().getTime();
    }

    public Sell(String txId, Date date, long amountStatoshi, String type, long amountInternet, Vendor vendor)
    {
        this.txId = txId;
        this.date = date;
        this.amountStatoshi = amountStatoshi;
        this.type = type;
        this.amountInternet = amountInternet;
        this.vendor = vendor;
    }

    public Sell(JsonObject json, Vendor vendor)
    {
        this.txId = json.get("txId").getAsString();
        String dateString = json.get("date").getAsString();
        try
        {
            this.date = PersistenceHelper.dateFormat.parse(dateString);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        this.amountStatoshi = json.get("amountStatoshi").getAsLong();
        this.type = json.get("type").getAsString();
        this.amountInternet = json.get("amountInternet").getAsLong();
        this.vendor = vendor;
    }

    public Vendor getVendor()
    {
        return vendor;
    }

    public void setVendor(Vendor vendor)
    {
        this.vendor = vendor;
    }

    public long getSellId()
    {
        return sellId;
    }

    public void setSellId(long sellId)
    {
        this.sellId = sellId;
    }

    public String getTxId()
    {
        return txId;
    }

    public void setTxId(String txId)
    {
        this.txId = txId;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public long getAmountStatoshi()
    {
        return amountStatoshi;
    }

    public void setAmountStatoshi(long amountStatoshi)
    {
        this.amountStatoshi = amountStatoshi;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public long getAmountInternet()
    {
        return amountInternet;
    }

    public void setAmountInternet(long amountInternet)
    {
        this.amountInternet = amountInternet;
    }

    @Override
    public String toString()
    {
        return "Sell{" +
                "sellId=" + sellId +
                ", txId='" + txId + '\'' +
                ", date=" + date +
                ", amountStatoshi=" + amountStatoshi +
                ", type='" + type + '\'' +
                ", amountInternet=" + amountInternet +
                '}';
    }
}
