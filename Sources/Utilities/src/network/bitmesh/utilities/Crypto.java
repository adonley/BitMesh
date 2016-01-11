package network.bitmesh.utilities;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.encoders.Base64Encoder;

import java.io.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crypto
{
    private static final Logger log = LoggerFactory.getLogger(Crypto.class.getName());

    private final int pepperLength = 12;

    // These are char arrays because strings will never get de-allocated
    private final char[] secret = new char [] { 'm', '6', 'd', 'G',
            'u', 'n', 'Y', 'O', 'b', 'O', '8', 'T', 'g', 'Q', 'b', 's' , 'O', '4', 'p', '9' };
    private final char[] base64Chars = new char [] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '-', '_' };

    /**
     * Initialize java security with the bouncy castle provider
     */
    private Crypto()
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Holder pattern for singleton class, lazy thread-safe initialization
     */
    private static class CryptoHolder
    {
        static final Crypto instance = new Crypto();
    }

    public static Crypto getInstance()
    {
        return CryptoHolder.instance;
    }

    /**
     * Authenticate the token using the salt
     * @param salt - the salt used to hash
     * @param token - token that the client used to authenticate
     * @return - true if the base64 strings match, false if the base64
     */
    public boolean authenticate(String salt, String token)
    {
        // Make sure token does not match the initial condition
        if(token == null || token.length() <= pepperLength)
            return false;

        // Remove the pepper from the token
        String tokenWithoutPepper = token.substring(pepperLength);
        String encoded = null;
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512","BC");
            String concatenated = String.valueOf(secret) + salt;
            byte[] shaBytes = messageDigest.digest(concatenated.getBytes("UTF-8"));
            // Create the encoded string
            encoded = encode(shaBytes);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return (tokenWithoutPepper.equals(encoded));
    }

    /**
     * Generates a base64 token from the salt
     * @param salt - salt to use to use in the hash
     * @return a base64 encoded string that serves as an authentication token
     */
    public String generateAuthenticationToken(String salt)
    {
        String pepper = randomBase64String(pepperLength);
        String concatenated = secret + salt;
        String encoded = "";

        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512","BC");
            byte[] shaBytes = messageDigest.digest(concatenated.getBytes("UTF-8"));
            // Create the encoded string
            encoded = encode(shaBytes);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return (pepper + encoded);
    }

    /**
     * Encodes a byte array into base64 string
     * @param binary - binary array to be base64 encoded
     * @return base64 encoded String of binary array
     */
    public static String encode(byte[] binary)
    {
        Base64Encoder encoder = new Base64Encoder();
        OutputStream encoded = new ByteArrayOutputStream();

        try
        {
            encoder.encode(binary, 0, binary.length, encoded);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // TODO: this could be wrong - might need to force UTF-8?
        return encoded.toString();
    }

    public static byte[] decode(String base64String)
    {
        return Base64.decodeBase64(base64String);
    }

    /**
     * Generates a random string of specified length in base64 encoding
     * @param length - length of the random base64 string to generate
     * @return - a random String of length "length" in base64 encoding
     */
    public String randomBase64String(int length)
    {
        StringBuilder randomStringBuilder = new StringBuilder(length);
        SecureRandom random = new SecureRandom();

        for(int i = 0; i < length; i++)
        {
            // Append base64 char to string from random
            randomStringBuilder.append(base64Chars[random.nextInt(base64Chars.length)]);

            // Reseed after four uses
            if(i % 4 == 0)
                random = new SecureRandom();
        }

        return randomStringBuilder.toString();
    }

    /**
     * Takes a file path and computes the sha512 hash of it contents.
     * @param filePath location of the file to be digested.
     * @return Base64 encoded sha512 of the input file
     */
    public static String digestFile(String filePath)
    {
        Security.addProvider(new BouncyCastleProvider());

        String digest = "";

        try
        {
            InputStream fis =  new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", "BC");
            int numRead;

            do
            {
                numRead = fis.read(buffer);

                if (numRead > 0)
                    messageDigest.update(buffer, 0, numRead);

            } while (numRead != -1);

            fis.close();

            digest = Crypto.encode(messageDigest.digest());

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return digest;
    }

    /**
     * Takes a base64 string and computes a signature given the hash
     * @param hash
     * @return
     */
    public static String signHash(String hash)
    {
        byte[] signature = null;
        network.bitmesh.utilities.Crypto.Keys keys = new network.bitmesh.utilities.Crypto().new Keys();
        KeyPair pair = keys.getKeyPair();

        try
        {
            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaSign.initSign(pair.getPrivate());
            ecdsaSign.update(hash.getBytes("UTF-8"));
            signature = ecdsaSign.sign();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return Crypto.encode(signature);
    }


    /**
     * Verifies a signature over a hash using an elliptical curve.
     * @param fileHash
     * @param signature
     * @return
     */
    public static boolean verifyFile(String fileHash, String signature)
    {
        network.bitmesh.utilities.Crypto.Keys keys = new network.bitmesh.utilities.Crypto().new Keys();
        KeyPair pair = keys.getKeyPair();
        boolean result = false;

        try
        {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaVerify.initVerify(pair.getPublic());
            ecdsaVerify.update(fileHash.getBytes("UTF-8"));
            result = ecdsaVerify.verify(Crypto.decode(signature));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public int getPepperLength() { return  pepperLength; }

    /**
     * Keys class holds all the private and public keys to do the signatures. Needs to be a private inner class
     * because proguard will not otherwise obfuscate this aggressively in a library jar.
     */
    private class Keys
    {

        private KeyPair keyPair;
        //private final String PRIVATE_KEY_LOCATION = getClass().getResource("private.key").getPath();
        //private final String PUBLIC_KEY_LOCATION = getClass().getResource("public.key").getPath();

        public Keys()
        {
            Security.addProvider(new BouncyCastleProvider());

            try
            {
                keyPair = loadKeyPair();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        public KeyPair getKeyPair()
        {
            return keyPair;
        }

        public KeyPair generateKeys()
        {
            KeyPair pair = null;

            try
            {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp384r1");
                keyGen.initialize(ecSpec, new SecureRandom());
                return keyGen.generateKeyPair();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return pair;
        }

        private String getHexString(byte[] b)
        {
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < b.length; i++)
                result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));

            return result.toString();
        }

        /*
        // Fuck this for now, null pointer when trying to have string path in a jar.
        public void saveKeyPair(KeyPair keyPair) throws IOException
        {
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Store Public Key.
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                    publicKey.getEncoded());
            FileOutputStream fos = new FileOutputStream(PUBLIC_KEY_LOCATION);
            fos.write(x509EncodedKeySpec.getEncoded());
            fos.close();

            // Store Private Key.
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                    privateKey.getEncoded());
            fos = new FileOutputStream(PRIVATE_KEY_LOCATION);
            fos.write(pkcs8EncodedKeySpec.getEncoded());
            fos.close();
        } */

        public KeyPair loadKeyPair()
                throws IOException, NoSuchAlgorithmException,
                InvalidKeySpecException
        {
            byte[] encodedPublicKey = { 48, -126, 1, -75, 48, -126, 1, 77, 6, 7, 42, -122, 72, -50, 61, 2, 1, 48, -126, 1, 64, 2, 1, 1, 48, 60, 6, 7, 42, -122, 72, -50, 61, 1, 1, 2, 49, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, 48, 100, 4, 48, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -4, 4, 48, -77, 49, 47, -89, -30, 62, -25, -28, -104, -114, 5, 107, -29, -8, 45, 25, 24, 29, -100, 110, -2, -127, 65, 18, 3, 20, 8, -113, 80, 19, -121, 90, -58, 86, 57, -115, -118, 46, -47, -99, 42, -123, -56, -19, -45, -20, 42, -17, 4, 97, 4, -86, -121, -54, 34, -66, -117, 5, 55, -114, -79, -57, 30, -13, 32, -83, 116, 110, 29, 59, 98, -117, -89, -101, -104, 89, -9, 65, -32, -126, 84, 42, 56, 85, 2, -14, 93, -65, 85, 41, 108, 58, 84, 94, 56, 114, 118, 10, -73, 54, 23, -34, 74, -106, 38, 44, 111, 93, -98, -104, -65, -110, -110, -36, 41, -8, -12, 29, -67, 40, -102, 20, 124, -23, -38, 49, 19, -75, -16, -72, -64, 10, 96, -79, -50, 29, 126, -127, -99, 122, 67, 29, 124, -112, -22, 14, 95, 2, 49, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -57, 99, 77, -127, -12, 55, 45, -33, 88, 26, 13, -78, 72, -80, -89, 122, -20, -20, 25, 106, -52, -59, 41, 115, 2, 1, 1, 3, 98, 0, 4, -93, -1, 49, 19, -84, -65, -61, -22, -65, 68, 52, 12, 96, 27, 28, 50, -108, -94, 109, -84, -42, 94, 47, -11, 14, 59, 69, -3, 50, -101, 101, -90, -94, 26, -68, -34, 57, 94, -55, -65, 123, -23, 85, -51, -94, 63, 6, 82, 8, -96, 82, -32, -88, -118, -84, 17, -61, -83, -107, -114, -94, -40, 69, 87, 61, -116, -73, 8, 99, 127, -34, -32, 71, 94, -49, 68, 66, 13, 57, -105, 99, -72, -86, -46, -45, 53, 75, 95, 12, 7, -90, -64, -102, -42, -25, -34 };
            byte[] encodedPrivateKey = { 48, 87, 2, 1, 0, 48, 16, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 5, 43, -127, 4, 0, 34, 4, 64, 48, 62, 2, 1, 1, 4, 48, -29, 21, 82, -96, 104, 23, -12, -24, 75, 21, 50, 10, -17, -113, -99, 16, -51, 9, 96, -13, 96, 32, -61, -125, -55, 74, 31, -11, 55, -54, 93, -86, 79, 93, -27, -94, -7, 16, 97, -20, 14, 16, -56, -59, 67, -35, -19, -52, -96, 7, 6, 5, 43, -127, 4, 0, 34 };

            PublicKey publicKey = null;
            PrivateKey privateKey = null;

            try
            {
                KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                        encodedPrivateKey);
                privateKey = keyFactory.generatePrivate(privateKeySpec);

                // Recompute public key.
                BCECPrivateKey priv = (BCECPrivateKey)privateKey;
                ECParameterSpec params = priv.getParameters();
                ECPublicKeySpec pubKS = new ECPublicKeySpec(
                        params.getG().multiply(priv.getD()), params);
                publicKey = keyFactory.generatePublic(pubKS);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return new KeyPair(publicKey, privateKey);
        }
    }
}
