package network.bitmesh.WebServer;

import network.bitmesh.WebServer.Client.HttpClientController;

import fi.iki.elonen.NanoHTTPD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;

public class HttpRunner
{
    private static final Logger log = LoggerFactory.getLogger(HttpRunner.class);

    /**
     * Reflective method to invoke the server.
     * @param serverClass
     */
    public static void run(Class serverClass)
    {
        try
        {
            // TODO: Make sure this thing can run correctly without the wallet info?
            if(serverClass.isAssignableFrom(HttpClientController.class))
            {
                Method method = serverClass.getMethod("getInstance");
                executeBitmeshClientInstance((HttpClientController) method.invoke(HttpController.class));
            }
            else
            {
                executeInstance((NanoHTTPD) serverClass.newInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void executeInstance(NanoHTTPD server) {
        try
        {
            server.start();
        }
        catch (IOException ioe)
        {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }
    }

    public static void executeBitmeshClientInstance(HttpClientController client) {
        try
        {
            client.start();
        }
        catch (IOException ioe)
        {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

    }
}
