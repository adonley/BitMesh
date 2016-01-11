package network.bitmesh.authentication;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by c on 8/22/15. Andrew probably has a better solution for this.
 * Was just wireframing how seller authentication would go.
 */
public class Authenticator
{

    private static Authenticator instance;
    private String password;

    public boolean authorizedForDashboard(NanoHTTPD.IHTTPSession session)
    {
        return true;
    }

    public static Authenticator getInstance()
    {
        if (instance == null)
        {
            instance = new Authenticator();
        }
        return instance;
    }

    public boolean isPasswordCorrect(String password)
    {
        return true;
    }
}
