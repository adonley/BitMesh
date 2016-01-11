package network.bitmesh.cloudserver.Statistics;

import network.bitmesh.cloudserver.Exceptions.NotFoundInDatabase;
import network.bitmesh.cloudserver.Utils.DatabaseSessionManager;
import network.bitmesh.cloudserver.Utils.PersistenceHelper;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;

@Entity

@Table(name = "REFUNDTX")
public class RefundPost
{
    private static final Logger log = LoggerFactory.getLogger(RefundPost.class.getName());

    @Id
    @GeneratedValue
    @Column(name = "REFUND_ID", nullable=false, unique=true)
    private long refundId;

    @Column(name = "TXID")
    private String txId;

    @Column(name = "LOCKDATE")
    private Date lockTime;

    @Column(name = "DATESUBMITTED")
    private Date dateSubmitted;

    @Column(name = "AMOUNTSATOSHI")
    private long amountSatoshi;

    @Column(name = "IS_MAINNET")
    private boolean isMainNet;

    @Column(name = "SUCCEEDED")
    private boolean succeeded;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "IP_ID", nullable = false)
    private IPAddress ip;

    private TransactionConfidence confidence;

    public RefundPost() { }

    public RefundPost(Transaction transaction, String ip)
    {
        this.txId = transaction.getHashAsString();

        // Assuming lock time is default zero
        if(transaction.getLockTime() != 0)
            this.lockTime = new Date(transaction.getLockTime()*1000);
        else
            this.lockTime = null;

        this.dateSubmitted = Calendar.getInstance().getTime();
        // We are assuming that there is only one output, this is not good if
        // we let other people use our server
        this.amountSatoshi = transaction.getOutputs().get(0).getValue().longValue();

        Session session = DatabaseSessionManager.getSessionFactory().openSession();
        session.beginTransaction();

        try
        {
            this.ip = PersistenceHelper.getIP(ip, session);
        }
        catch(NotFoundInDatabase n)
        {
            this.ip = new IPAddress(ip);
        }
        catch(Exception e)
        {
            log.error("Error getting IP from database.");
            e.printStackTrace();
        }

        session.close();
    }

    public RefundPost(Transaction transaction, String ip, boolean isMainNet)
    {
        this(transaction, ip);
        this.succeeded = false;
        this.isMainNet = isMainNet;
    }

    public RefundPost(Transaction transaction, String ip, boolean isMainNet, boolean succeeded)
    {
        this(transaction, ip);
        this.succeeded = succeeded;
        this.isMainNet = isMainNet;
    }

    public long getRefundId()
    {
        return refundId;
    }

    public void setRefundId(long refundId)
    {
        this.refundId = refundId;
    }

    public String getTxId()
    {
        return txId;
    }

    public void setTxId(String txId)
    {
        this.txId = txId;
    }

    public Date getLockTime()
    {
        return lockTime;
    }

    public void setLockTime(Date lockTime)
    {
        this.lockTime = lockTime;
    }

    public Date getDateSubmitted()
    {
        return dateSubmitted;
    }

    public void setDateSubmitted(Date dateSubmitted)
    {
        this.dateSubmitted = dateSubmitted;
    }

    public long getAmountSatoshi()
    {
        return amountSatoshi;
    }

    public void setAmountSatoshi(long amountSatoshi)
    {
        this.amountSatoshi = amountSatoshi;
    }

    public IPAddress getIp()
    {
        return ip;
    }

    public void setIp(IPAddress ip)
    {
        this.ip = ip;
    }

    public boolean isSucceeded()
    {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded)
    {
        this.succeeded = succeeded;
    }

    public boolean isMainNet()
    {
        return isMainNet;
    }

    public void setIsMainNet(boolean isMainNet)
    {
        this.isMainNet = isMainNet;
    }

    public TransactionConfidence getConfidence()
    {
        return confidence;
    }

    public void setConfidence(TransactionConfidence confidence)
    {
        this.confidence = confidence;
    }

    @Override
    public String toString()
    {
        return "RefundPost{" +
                "refundId=" + refundId +
                ", txId='" + txId + '\'' +
                ", lockTime=" + lockTime +
                ", dateSubmitted=" + dateSubmitted +
                ", amountSatoshi=" + amountSatoshi +
                ", isMainNet=" + isMainNet +
                ", succeeded=" + succeeded +
                ", ip=" + ip +
                '}';
    }
}
