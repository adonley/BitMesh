package network.bitmesh.Updater.Version;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.bitmesh.Updater.UpdaterConfig;
import network.bitmesh.utilities.Versioning.PackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class VersionCheck
{
    private static final Logger log = LoggerFactory.getLogger(VersionCheck.class.getName());

    /**
     * Grabs the current version of the software
     */
    public PackageInfo grabVersionInfo()
    {
        URL url;
        HttpURLConnection conn;
        String line;
        StringBuilder result = new StringBuilder();

        try
        {
            url = new URL(UpdaterConfig.VERSION_URL);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while ((line = rd.readLine()) != null)
                result.append(line);

            rd.close();
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (Exception c)
        {
            c.printStackTrace();
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = (JsonObject) parser.parse(result.toString());

        JsonObject version = jsonObject.get("version").getAsJsonObject();

        PackageInfo packageInfo = new PackageInfo(jsonObject.get("signature").getAsString(),
                version.get("major").getAsInt(),
                version.get("minor").getAsInt(),
                version.get("release").getAsInt());

        log.info("Signature: {}", packageInfo.getSignature());

        return packageInfo;
    }

    public PackageInfo readVersionFromFile(String fileLocation)
    {
        PackageInfo packageInfo = null;
        try
        {
            FileInputStream inputStream = new FileInputStream(fileLocation);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            packageInfo = new PackageInfo(bufferedReader.readLine(),
                    new Integer(bufferedReader.readLine()).intValue(),
                    new Integer(bufferedReader.readLine()).intValue(),
                    new Integer(bufferedReader.readLine()).intValue());

        }
        catch(Exception e)
        {
            packageInfo = new PackageInfo("", 0, 0, 0);
            File file = new File(fileLocation);
            if(file.exists())
            {
                System.err.println("Weird error getting local version.");
                e.printStackTrace();
            }
        }
        return packageInfo;
    }

    public void writeVersionToFile(String fileLocation, PackageInfo packageInfo)
    {
        try
        {
            PrintWriter writer = new PrintWriter(fileLocation);
            writer.println(packageInfo.getSignature());
            writer.println(packageInfo.getMajor());
            writer.println(packageInfo.getMinor());
            writer.println(packageInfo.getRelease());
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean compareVersions(PackageInfo packageInfo)
    {
        PackageInfo currentInfo = readVersionFromFile(UpdaterConfig.VERSION_LOCATION);

        if(currentInfo.getMajor() < packageInfo.getMajor())
            return true;
        else if (currentInfo.getMajor() == packageInfo.getMajor())
        {
            if(currentInfo.getMinor() < packageInfo.getMinor())
                return true;

            if(currentInfo.getMinor() == packageInfo.getMinor())
            {
                if(currentInfo.getRelease() < packageInfo.getRelease())
                    return true;
            }
        }
        return false;
    }
}
