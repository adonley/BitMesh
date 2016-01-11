package network.bitmesh.cloudserver.Utils;

import network.bitmesh.cloudserver.ServerConfig;
import network.bitmesh.utilities.Crypto;
import network.bitmesh.utilities.Versioning.PackageInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This class hurts to look at. Assumptions are that we are not going to have
 * many versions hosted online. Every request scans and digests the releases available.
 */
public class Versioning
{
    private static final Logger log = LoggerFactory.getLogger(Versioning.class.getName());

    /**
     * Testing goes on desktop for now.
     */
    private static File releaseLocation = ServerConfig.BITMESH_TEST ?
            new File(System.getProperty("user.home") + File.separator + "Desktop") :
            new File(System.getenv("persistdir") + File.separator + "releases");

    public static String releaseLocation()
    {
        return releaseLocation.getAbsolutePath() + File.separator + latestReleaseInfo().getPackageName();
    }

    public static String latestReleaseName()
    {
        return latestReleaseInfo().getPackageName();
    }

    public static PackageInfo latestReleaseInfo()
    {
        PackageInfo latestVersion = new PackageInfo("", 0, 0, 0);

        if(!releaseLocation.exists())
        {
            log.error("Could not open releases directory: {}", releaseLocation.getAbsolutePath());
            return null;
        }

        for(File f: releaseLocation.listFiles())
        {
            // Get the version number from the name
            if(f.getName().startsWith("bitmesh-"))
            {
                String versionString = f.getName().substring(8);
                String[] versionInfo = versionString.split("\\.");

                // TODO: Probably inefficient to digest the file everytime. Cache this somehow.
                PackageInfo tempPackageInfo = new PackageInfo(
                        Crypto.signHash(Crypto.digestFile(f.getAbsolutePath())),
                        Integer.valueOf(versionInfo[0]),
                        Integer.valueOf(versionInfo[1]),
                        Integer.valueOf(versionInfo[2]));

                // If the current version is greater than the latest, change it
                if(tempPackageInfo.compareTo(latestVersion) >= 0)
                {
                    latestVersion = tempPackageInfo;
                }
            }
        }

        return latestVersion;
    }
}
