package network.bitmesh.Updater;

public class UpdaterConfig
{
    /**
     * If we are testing locally or not
     */
    public static boolean test = true;

    /**
     * URL to check the latest version of package
     */
    public static final String VERSION_URL = test ?
            "http://127.0.0.1:8082/latestversion" :
            "https://www.bitmesh.network/latestversion";

    /**
     * The update url for bitmesh. We should never change this becasue it will break
     * already deployed nodes.
     */
    public static String UPDATE_URL = test ?
            "http://127.0.0.1:8082/download/update" :
            "https://www.bitmesh.network/download/update";

    /**
     * The installation directory for bitmesh. When we make windows a target we will
     * have to change this.
     */
    public static String INSTALLATION_DIRECTORY = test ?
            "./out" :
            "/opt/bitmesh";

    public static final String VERSION_LOCATION = "./version.txt";
}
