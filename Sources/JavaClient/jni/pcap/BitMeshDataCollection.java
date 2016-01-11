package jni.pcap;

/**
 * Created by christopher on 4/7/15.
 */

public class BitMeshDataCollection
{
	public native long getDataUpForClient(String client);
	public native long getDataDownForClient(String client);
	public native int addClient(String client);
	public native int forgetClient(String client);
	public native int initPcap();
	public native void pcapLoop();
	public native int stopPcap();

	public static BitMeshDataCollection instance;
    static 
    {
        System.loadLibrary("bitmeshpcap");
    }        

	public static BitMeshDataCollection getInstance()
	{
		if (instance == null)
		{
			instance = new BitMeshDataCollection();
		}

		return instance;
	}

	public BitMeshDataCollection()
	{
		startLoop();
	}

	public void startLoop()
	{
		initPcap();
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				pcapLoop();
			}
		});
		t.start();
	}

    public static void main(String[] args)
    {
    	final BitMeshDataCollection bData = new BitMeshDataCollection();
    	String client = "178.162.209.231";
		bData.initPcap();
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				bData.pcapLoop();
			}
		});
		t.start();
    	bData.addClient(client);

    	try
    	{
	    	for (int i = 0; i < 30; i++)
	    	{
	    		Thread.sleep(4000);
		    	System.out.println(bData.getDataDownForClient(client));
	    	}    		
    	}
    	catch (Exception e)
    	{
    		// ignore
    	}

    	bData.stopPcap();
    }

}
