package network.bitmesh;

import network.bitmesh.utilities.Crypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignTest
{

    private static final Logger log = LoggerFactory.getLogger(SignTest.class.getName());


    @org.junit.Test
    public void testSignHash() throws Exception
    {
        Crypto crypto = Crypto.getInstance();
        crypto.toString();
        for(int i = 0; i < 100000; i++)
        {
            String randoString = Crypto.getInstance().randomBase64String(24);
            assert(Crypto.verifyFile(randoString, Crypto.signHash(randoString)));
            if(i % 100 == 0)
                System.out.print(i + " ");
        }

    }

    @org.junit.Test
    public void testSignHashFile() throws Exception
    {
        Crypto crypto = Crypto.getInstance();

        for(int i = 0; i < 40; i++)
        {
            String digest =
                    Crypto.digestFile("/Users/andrew/Programs/bitmesh/Sources/CloudServer/src/main/webapp/releases/bitmesh-1.0.0.zip");
            String sig = Crypto.signHash(digest);

            log.info("i: {}", i);
            log.info("Digest: {}", digest);
            log.info("Signature Hash {}", sig);

            assert (Crypto.verifyFile(digest, sig) == true);
        }

    }

}