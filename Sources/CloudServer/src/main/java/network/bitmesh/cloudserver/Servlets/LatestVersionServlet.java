package network.bitmesh.cloudserver.Servlets;

import com.google.gson.Gson;
import network.bitmesh.cloudserver.ServerConfig;
import network.bitmesh.cloudserver.Utils.Versioning;
import network.bitmesh.utilities.Versioning.PackageInfo;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class LatestVersionServlet extends HttpServlet
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LatestVersionServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        PackageInfo packageInfo = Versioning.latestReleaseInfo();

        HashMap<String, Object> responseMap = new HashMap<String,Object>();
        HashMap<String, Integer> versionMap = new HashMap<String, Integer>();

        versionMap.put("major", packageInfo.getMajor());
        versionMap.put("minor", packageInfo.getMinor());
        versionMap.put("release", packageInfo.getRelease());
        responseMap.put("version", versionMap);
        responseMap.put("signature", packageInfo.getSignature());

        Gson gson = new Gson();

        resp.setStatus(resp.SC_OK);
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print(gson.toJson(responseMap));
        log.info("Version Response: " + gson.toJson(responseMap));

        out.flush();
        out.close();
    }
}
