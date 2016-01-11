package network.bitmesh.Updater;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import network.bitmesh.utilities.Versioning.PackageInfo;
import network.bitmesh.utilities.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unpackager
{
    private static final Logger log = LoggerFactory.getLogger(Unpackager.class.getName());

    private String packageLocation;
    private PackageInfo packageInfo;
    private Crypto crypto;

    public Unpackager(PackageInfo packageInfo)
    {
        this.crypto = Crypto.getInstance();
        this.packageInfo = packageInfo;
        this.packageLocation = UpdaterConfig.INSTALLATION_DIRECTORY
                + File.separator + packageInfo.getPackageName();
    }

    /**
     * Download the bitmesh jars
     */
    public boolean fetchBitMesh()
    {
        boolean success = false;

        try
        {
            // Use Https to fetch package
            URL website = new URL(UpdaterConfig.UPDATE_URL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            // Make the parent directories if they don't exist
            new File(new File(packageLocation).getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(packageLocation);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            success = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return success;
    }

    /**
     * Unzips a zipped archive.
     * @param outputDirectory - directory to unzip it to
     */
    public void unZip(String outputDirectory)
    {
        byte[] buffer = new byte[1024];

        try
        {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(packageLocation));

            //get the zipped file list entry
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while(zipEntry != null)
            {
                String fileName = zipEntry.getName();
                File newFile = new File(outputDirectory + File.separator + fileName);

                // Create all non exists folders
                File parent = new File(newFile.getParent());

                // Delete the file being decompressed if it exists
                if(newFile.exists())
                {
                    log.info("Replacing {}.", newFile.getName());
                    newFile.delete();
                }

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zipInputStream.read(buffer)) > 0)
                    fos.write(buffer, 0, len);

                fos.close();
                zipEntry = zipInputStream.getNextEntry();
            }

            zipInputStream.closeEntry();
            zipInputStream.close();

        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public boolean validateBitmesh()
    {
        log.info("Signature: {}", packageInfo.getSignature());
        return Crypto.verifyFile(Crypto.digestFile(packageLocation), packageInfo.getSignature());
    }

    public void removeZip()
    {
        File zipPackage = new File(packageLocation);
        if(zipPackage.exists())
            zipPackage.delete();
        else
            log.error("Package doesn't exist and this doesn't make any sense.");
    }
}
