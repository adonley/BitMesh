package network.bitmesh.cloudserver.Utils;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class CommonResponses
{
    /**
     * Common mime-types used in responses
     */
    public static final Map<String, String> MIME_TYPES = new HashMap<String, String>()
    {{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("xml", "text/xml");
        put("java", "text/x-java-source, text/java");
        put("md", "text/plain");
        put("txt", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("ogg", "application/x-ogg");
        put("zip", "application/octet-stream");
        put("exe", "application/octet-stream");
        put("class", "application/octet-stream");
        put("bin", "application/octet-stream");
        put("json","application/json");
        put("ttf","application/octet-stream");
        put("woff","application/octet-stream");
        put("woff2","application/octet-stream");
        put("other", "application/octet-stream");
    }};

    public static void allowCrossOrigin(HttpServletResponse response)
    {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers","Content-Type, x-requested-with, x-forwarded-for");
    }

    public static boolean shouldTimeOut(long startTime)
    {
        double elapsed = (System.nanoTime() - startTime) / 1000000000.0;
        if(elapsed > 30)
            return true;
        return false;
    }
}
