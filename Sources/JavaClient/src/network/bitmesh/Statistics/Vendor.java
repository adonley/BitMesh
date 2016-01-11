package network.bitmesh.Statistics;

import com.google.gson.annotations.Expose;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity

@Table(name = "VENDOR")
public class Vendor implements Serializable
{
    @Expose
    @Id @GeneratedValue
    @Column(name = "VENDOR_ID", nullable=false, unique=true)
    private long vendorId;

    @Expose
    @Column(name = "MAC", nullable = false, unique = true)
    private String MAC;

    @Expose
    @Column(name = "xLoc")
    private String xLocation;

    @Expose
    @Column(name = "yLoc")
    private String yLocation;

    @Expose
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "vendor")
    private Set<Sell> sells;

    @Expose
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "vendor")
    private Set<IPAddress> ips;

    @Expose
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "vendor")
    private Set<VendorCashOut> cashOuts;

    public Vendor() { }

    public Vendor(String MAC)
    {
        this.MAC = MAC;
    }

    public Vendor(String MAC, String xLocation, String yLocation)
    {
        this.MAC = MAC;
        this.xLocation = xLocation;
        this.yLocation = yLocation;
    }

    public long getVendorId()
    {
        return vendorId;
    }

    public void setVendorId(long vendorId)
    {
        this.vendorId = vendorId;
    }

    public String getMAC()
    {
        return MAC;
    }

    public void setMAC(String MAC)
    {
        this.MAC = MAC;
    }

    public String getxLocation()
    {
        return xLocation;
    }

    public void setxLocation(String xLocation)
    {
        this.xLocation = xLocation;
    }

    public String getyLocation()
    {
        return yLocation;
    }

    public void setyLocation(String yLocation)
    {
        this.yLocation = yLocation;
    }

    public Set<IPAddress> getIps()
    {
        return ips;
    }

    public void setIps(Set<IPAddress> ips)
    {
        this.ips = ips;
    }

    public Set<Sell> getSells()
    {
        return sells;
    }

    public void setSells(Set<Sell> sells)
    {
        this.sells = sells;
    }

    public Set<VendorCashOut> getCashOuts()
    {
        return cashOuts;
    }

    public void setCashOuts(Set<VendorCashOut> cashOuts)
    {
        this.cashOuts = cashOuts;
    }

    @Override
    public String toString()
    {
        return "Vendor{" +
                "vendorId=" + vendorId +
                ", MAC='" + MAC + '\'' +
                ", xLocation='" + xLocation + '\'' +
                ", yLocation='" + yLocation + '\'' +
                ", sells=" + sells +
                ", ips=" + ips +
                ", cashOuts=" + cashOuts +
                '}';
    }
}
