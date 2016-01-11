package network.bitmesh.Updater;

import network.bitmesh.Updater.Version.VersionCheck;
import network.bitmesh.utilities.Versioning.PackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static final Logger log = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args)
    {
        VersionCheck versionCheck = new VersionCheck();
        PackageInfo packageInfo = versionCheck.grabVersionInfo();

        // If the version online is newer
        if(versionCheck.compareVersions(packageInfo))
        {
            log.info("New version of BitMesh found.");
            Unpackager unpackager = new Unpackager(packageInfo);
            unpackager.fetchBitMesh();
            if(!unpackager.validateBitmesh())
                log.info("Incorrect signature for bitmesh package.");
            else
            {

                log.info("Good signature for bitmesh package.");
                unpackager.unZip(UpdaterConfig.INSTALLATION_DIRECTORY);
                unpackager.removeZip();
            }
        }
    }
}
