package network.bitmesh.cloudserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerConfig
{
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class.getName());

    public static final Boolean BITMESH_TEST = true;
    public static final int MAX_PEER_CONNECTIONS = 8;
    public static final int MIN_PEERS_TO_BROADCAST = 1;
}
