package network.bitmesh.utilities.Versioning;

public class PackageInfo implements Comparable<PackageInfo>
{
    private String signature;
    private int major;
    private int minor;
    private int release;

    public PackageInfo(String signature, int major, int minor, int release)
    {
        this.signature = signature;
        this.major = major;
        this.minor = minor;
        this.release = release;
    }

    public PackageInfo()
    {
        this.signature = "";
        this.major = 0;
        this.minor = 0;
        this.release = 0;
    }

    public String getSignature()
    {
        return signature;
    }

    public int getMajor()
    {
        return major;
    }

    public int getMinor()
    {
        return minor;
    }

    public int getRelease()
    {
        return release;
    }

    @Override
    public int compareTo(PackageInfo o)
    {
        // Have to weigh the major release differently than the minor
        int majorMagnitude = 100;
        int minorMagnitude = 10;
        int majorDif = (this.getMajor() - o.getMajor()) * majorMagnitude;
        int minorDif = (this.getMinor() - o.getMinor()) * minorMagnitude;
        int releaseDif = (this.getRelease() - o.getRelease());
        return majorDif + minorDif + releaseDif;
    }

    public String getPackageName()
    {
        return "bitmesh-" + getMajor() + "." + getMinor() + "." + getRelease() + ".zip";
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }

    public void setMajor(int major)
    {
        this.major = major;
    }

    public void setMinor(int minor)
    {
        this.minor = minor;
    }

    public void setRelease(int release)
    {
        this.release = release;
    }
}
