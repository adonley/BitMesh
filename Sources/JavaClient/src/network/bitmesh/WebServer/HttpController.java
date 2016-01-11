package network.bitmesh.WebServer;

import network.bitmesh.BitmeshConfiguration;

import fi.iki.elonen.NanoHTTPD;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpController extends NanoHTTPD
{

    protected static HttpController instance;
    protected StringBuilder responseBuilder;

    protected static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("svg", "image/svg+xml");
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
        put("json", "application/json");
        put("ttf", "application/octet-stream");
        put("woff", "application/octet-stream");
        put("woff2", "application/octet-stream");
        put("other", "application/octet-stream");
    }};

    // Websocket vars
    public static final String HEADER_UPGRADE = "upgrade";
    public static final String HEADER_UPGRADE_VALUE = "websocket";
    public static final String HEADER_CONNECTION = "connection";
    public static final String HEADER_CONNECTION_VALUE = "Upgrade";
    public static final String HEADER_WEBSOCKET_VERSION = "sec-websocket-version";
    public static final String HEADER_WEBSOCKET_VERSION_VALUE = "13";
    public static final String HEADER_WEBSOCKET_KEY = "sec-websocket-key";
    public static final String HEADER_WEBSOCKET_ACCEPT = "sec-websocket-accept";
    public static final String HEADER_WEBSOCKET_PROTOCOL = "sec-websocket-protocol";
    public final static String WEBSOCKET_KEY_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    // Need permissions for port 80
    public HttpController()
    {
        super(BitmeshConfiguration.defaultConfiguration().getPort());
    }

    public HttpController(int port)
    {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) { throw new NotImplementedException(); }


    /* protected Response getForbiddenResponse(String s) {
        return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: "
                + s);
    } */

    public Response authenticationPage()
    {
        responseBuilder = new StringBuilder();
        return new NanoHTTPD.Response(responseBuilder.toString());
    }

    protected final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    protected static String encodeBase64(byte[] buf)
    {
        int size = buf.length;
        char[] ar = new char[((size + 2) / 3) * 4];
        int a = 0;
        int i = 0;
        while (i < size)
        {
            byte b0 = buf[i++];
            byte b1 = (i < size) ? buf[i++] : 0;
            byte b2 = (i < size) ? buf[i++] : 0;

            int mask = 0x3F;
            ar[a++] = ALPHABET[(b0 >> 2) & mask];
            ar[a++] = ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & mask];
            ar[a++] = ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & mask];
            ar[a++] = ALPHABET[b2 & mask];
        }
        switch (size % 3)
        {
            case 1:
                ar[--a] = '=';
            case 2:
                ar[--a] = '=';
        }
        return new String(ar);
    }

    protected boolean isWebsocketRequested(IHTTPSession session)
    {
        Map<String, String> headers = session.getHeaders();
        String upgrade = headers.get(HEADER_UPGRADE);
        boolean isCorrectConnection = isWebSocketConnectionHeader(headers);
        boolean isUpgrade = HEADER_UPGRADE_VALUE.equalsIgnoreCase(upgrade);
        return (isUpgrade && isCorrectConnection);
    }

    private boolean isWebSocketConnectionHeader(Map<String, String> headers)
    {
        String connection = headers.get(HEADER_CONNECTION);
        return (connection != null && connection.toLowerCase().contains(HEADER_CONNECTION_VALUE.toLowerCase()));
    }

    public static String makeAcceptKey(String key) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = key + WEBSOCKET_KEY_MAGIC;
        md.update(text.getBytes(), 0, text.length());
        byte[] sha1hash = md.digest();
        return encodeBase64(sha1hash);
    }

    public static Response forbidden()
    {
        return new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: You are not authorized to access this webpage.");
    }
}
