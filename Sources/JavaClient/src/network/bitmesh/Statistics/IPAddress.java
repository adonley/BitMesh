package network.bitmesh.Statistics;

import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.io.Serializable;

@Entity

@Table(name = "IP_ADDRESS")
public class IPAddress implements Serializable
{
    @Expose
    @Id @GeneratedValue
    @Column(name = "IP_ID", nullable = false, unique = true)
    private long ipId;

    @Expose
    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @ManyToOne
    @JoinColumn(name = "VENDOR_ID", nullable = false)
    private Vendor vendor;

    public IPAddress() { }

    public IPAddress(String ipAddress, Vendor vendor)
    {
        this.ipAddress = ipAddress;
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

    public long getIpId()
    {
        return ipId;
    }

    public void setIpId(long ipId)
    {
        this.ipId = ipId;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString()
    {
        return "IPAddress{" +
                "ipId=" + ipId +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
