package network.bitmesh.WebServer.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by andrew on 4/21/15.
 */
public class ClientKeepAliveTimerTask extends TimerTask
{
    private static final Logger log = LoggerFactory.getLogger(ClientKeepAliveTimerTask.class);
    public static List<ClientWebSocket> clientSocketList = new ArrayList<ClientWebSocket>();

    public synchronized static void addWebSocket(ClientWebSocket socket)
    {
        if(!clientSocketList.contains(socket))
        {
            clientSocketList.add(socket);
            log.info("Added socket: {}", socket);

        }
        else
        {
            log.info("Didn't addSocket - Already exists. Socket: {}", socket);
        }
    }

    public synchronized static void removeWebSocket(ClientWebSocket socket)
    {
        clientSocketList.remove(socket);
    }

    private synchronized void keepAliveClients()
    {
        if(clientSocketList.size() == 0)
            return;

        for(ClientWebSocket c : clientSocketList)
        {
            c.keepAlive();
        }
    }

    @Override
    public void run()
    {
        keepAliveClients();
    }
}
