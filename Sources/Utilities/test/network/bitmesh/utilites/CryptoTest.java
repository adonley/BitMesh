package network.bitmesh.utilites;

import network.bitmesh.utilities.Crypto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class CryptoTest
{
    private static final Logger log = LoggerFactory.getLogger(CryptoTest.class.getName());

    @Test
    public void testAuthenticate() throws Exception
    {
        Crypto crypto = Crypto.getInstance();
        String salt = crypto.randomBase64String(6);
        String salt2 = crypto.randomBase64String(6);
        String token = crypto.generateAuthenticationToken(salt);
        String token2 = crypto.generateAuthenticationToken(salt2);
        assert(crypto.authenticate(salt, token));
        assert(crypto.authenticate(salt2, token2));
        assert(!crypto.authenticate(salt, token2));
        assert(!crypto.authenticate(salt2, token));
    }

    @Test
    public void testEncode() throws Exception
    {
        Crypto crypto = Crypto.getInstance();
        String randomString = crypto.randomBase64String(14);
        String randomString2 = crypto.randomBase64String(14);
        String encoded1 = crypto.encode(randomString.getBytes());
        String encoded2 = crypto.encode(randomString.getBytes());
        String encoded3 = crypto.encode(randomString2.getBytes());
        assert(encoded1.equals(encoded2));
        assert(!encoded3.equals(encoded1));
    }

    @Test
    public void generateAuthenticationToken() throws Exception
    {
        Crypto crypto = Crypto.getInstance();
        String salt = crypto.randomBase64String(6);
        String token1 = crypto.generateAuthenticationToken(salt);
        String token2 = crypto.generateAuthenticationToken(salt);
        assert(token1.substring(crypto.getPepperLength()).equals(token2.substring(crypto.getPepperLength())));
        assert(!token1.equals(token2));
    }

    @Test
    public void testRandomBase64String() throws Exception
    {
        Crypto crypto = Crypto.getInstance();
        Set<String> uniqueStrings = new HashSet<String>();
        int length = 5;
        String unique;

        // There's possibility for collision here -> (1/62)^length * 10000
        for(int i = 0; i < 10000; i++)
        {
            unique = crypto.randomBase64String(length);
            assert(!uniqueStrings.contains(unique));
            uniqueStrings.add(unique);
        }
    }

    @Test
    public void testBase64Encoding()
    {
        Crypto crypto = Crypto.getInstance();

        int length = 5;
        String unique;
        System.out.print("i:");

        for(int i = 0; i < 10000; i++)
        {
            unique = crypto.randomBase64String(length);
            assert (unique == Crypto.encode(Crypto.decode(Crypto.encode(Crypto.decode(unique)))));
            if(i % 100 == 0)
                System.out.print(" " + i);
        }
    }

    @org.junit.Test
    public void testSignHash() throws Exception
    {
        Crypto crypto = Crypto.getInstance();
        String previous = null;

        for(int i = 0; i < 100000; i++)
        {
            String randoString = Crypto.getInstance().randomBase64String(24);
            assert(Crypto.verifyFile(randoString, Crypto.signHash(randoString)));
            if(i % 100 == 0)
                System.out.print(i + " ");

            if(previous != null)
                assert (previous != randoString);

            previous = randoString;
        }

    }

    @org.junit.Test
    public void testSignHashFile() throws Exception
    {
        Crypto crypto = Crypto.getInstance();

        for(int i = 0; i < 40; i++)
        {
            String digest =
                    Crypto.digestFile("/Users/andrew/Desktop/bitmesh-1.0.0.zip");
            String sig = Crypto.signHash(digest);

            log.info("i: {}", i);
            log.info("Digest: {}", digest);
            log.info("Signature Hash {}", sig);

            assert (Crypto.verifyFile(digest, sig) == true);
        }

    }
}