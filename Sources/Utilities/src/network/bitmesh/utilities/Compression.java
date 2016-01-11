package network.bitmesh.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Compression
{
    /**
     * Unzips a zipped archive.
     * @param outputDirectory - directory to unzip it to
     */
    public static void unZip(Path fileLocation, Path outputDirectory)
    {
        byte[] buffer = new byte[1024];

        try
        {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(fileLocation.toFile()));

            //get the zipped file list entry
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while(zipEntry != null)
            {
                String fileName = zipEntry.getName();
                Path outputPath = Paths.get(outputDirectory + File.separator + fileName);
                File newFile = new File(outputPath.toUri());

                // Create all non exists folders
                new File(newFile.getParent()).mkdirs();

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

}
