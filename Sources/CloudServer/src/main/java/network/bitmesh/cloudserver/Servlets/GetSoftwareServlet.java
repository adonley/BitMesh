package network.bitmesh.cloudserver.Servlets;

import network.bitmesh.cloudserver.Utils.Versioning;
import network.bitmesh.utilities.Versioning.PackageInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;

public class GetSoftwareServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        PackageInfo packageInfo = Versioning.latestReleaseInfo();
        resp.setContentType("application/zip");
        resp.setHeader("Content-Disposition", "filename=\"" + packageInfo.getPackageName() + "\"");
        String release = Versioning.releaseLocation();

        byte[] buffer = new byte[1024];
        File releaseFile = new File(release);
        InputStream input = new FileInputStream(releaseFile);
        OutputStream output = resp.getOutputStream();

        int len;
        while((len = input.read(buffer, 0, 1024)) > 0)
            output.write(buffer, 0, len);

        output.flush();

        resp.setStatus(resp.SC_OK);
    }
}
