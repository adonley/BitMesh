package network.bitmesh;

import network.bitmesh.utilities.Crypto;

public class Main
{
    public static void main(String[] args)
    {
        Crypto crypto = Crypto.getInstance();

        for(String s : args)
        {
            System.out.println("File: " + s);
            String digestedFile = Crypto.digestFile(s);
            System.out.println("Digested File: " + digestedFile);
            String signedHash = Crypto.signHash(digestedFile);
            System.out.println("Signed Hash: " + signedHash);
            if(Crypto.verifyFile(digestedFile, signedHash))
                System.out.println("Signature verified correctly");
            else
                System.out.println("!!!Signature failed to verify!!!");
        }
    }
}
