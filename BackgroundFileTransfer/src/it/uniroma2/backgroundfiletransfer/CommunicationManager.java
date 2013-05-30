package it.uniroma2.backgroundfiletransfer;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

public class CommunicationManager implements Runnable{
	
	static private CommunicationManager single_instance=null;
	final static String LOGTAG="CommunicationManager";
	
	private boolean stopFromButton=false; 
	
	WifiManager wifiManager;
	static WifiManager.WifiLock wifiLock;
	PowerManager powerManager;
	static PowerManager.WakeLock wakeLock;
	static WifiManager.MulticastLock multicastLock;
	

	private FileTransferManager incRM; 
	private QueryManager queryMgr;
	
	public static int minON=0; // timeout between turning on/off wifi
	private static int minOFF=0;
	
	private boolean condWifiAdhoc=false;
	private String localIP="";
	private String wifiInterface;
	
	/**
	 * Singleton class
	 * @return Instance of the CommunicationManager class
	 */
	static public CommunicationManager getInstance()
	{
	 if ( single_instance == null )
		 {
		 	single_instance = new CommunicationManager();
		 	Log.i(LOGTAG, "CommunicationManager instance created.(Constructor called)");
		 }
	 
	 return single_instance;
	}
	
	private CommunicationManager() {
		powerManager = (PowerManager)SharingFileService.context.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , LOGTAG); // PARTIAL_WAKE_LOCK Only keeps CPU on
		wifiManager = (WifiManager)SharingFileService.context.getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock(3, LOGTAG);
		multicastLock = wifiManager.createMulticastLock(LOGTAG);

		changeMinON_OFF();
		incRM=FileTransferManager.getInstance();
		queryMgr=QueryManager.getInstance();
		
		
		}
	public void changeState(boolean val)
	{
		stopFromButton=val;
	}

	public void start()
	{
		startWifiAdhoc();
		}
	
	public void stop()
	{
		stopWifiAdHoc();	
	}

	private void startWifiAdhoc() {

		String command="";
		if (condWifiAdhoc == false) {
			
			condWifiAdhoc=true;
			wifiInterface = Utils.getWifiInterface();
			if (MainActivity.currentActivityTV != null)
				MainActivity.handler.post(new Runnable() {
					public void run() {
						MainActivity.currentActivityTV
								.append("Wifi interface generated");
					}
				});

			Log.i(LOGTAG, "Wifi interface generated");

			// Load wifi driver
			command = MainActivity.context.getFilesDir().getPath()
					+ "/wifiloader start\n";

			if (MainActivity.currentActivityTV != null)
				MainActivity.handler.post(new Runnable() {
					public void run() {
						MainActivity.currentActivityTV.append("\n"
								+ "wifiloader start");
					}
				});

			Log.i(LOGTAG, command);
			Utils.rootExec(command);
			localIP = Utils.getLinkLocalAddress();
		}
		else
		{
			command=SharingFileService.context.getFilesDir().getPath()+"/iwconfig "+wifiInterface+" txpower on\n";
			Log.i(LOGTAG, command);
			Utils.rootExec(command);
		}
			// Set wifi ad-hoc
			command = SharingFileService.context.getFilesDir().getPath()
					+ "/iwconfig " + wifiInterface + " mode ad-hoc essid "
					+ "mcp" + " channel " + "1" + " commit\n";
			if (MainActivity.currentActivityTV != null)
				MainActivity.handler.post(new Runnable() {
					public void run() {
						MainActivity.currentActivityTV.append("\n"
								+ "iwconfig " + wifiInterface
								+ " mode ad-hoc essid " + "mcp" + " channel "
								+ "1" + " commit");
					}
				});
			Log.i(LOGTAG, command);
			Utils.rootExec(command);

			// Wait for interface to be ready
			if (MainActivity.currentActivityTV != null)
				MainActivity.handler.post(new Runnable() {
					public void run() {
						MainActivity.currentActivityTV
								.append("\nWaiting for interface to be ready...");
					}
				});

			Log.i(LOGTAG, "Waiting for interface to be ready...");
			
			
			Log.i(LOGTAG, "Ip address generated");

			if (MainActivity.currentActivityTV != null)
				MainActivity.handler.post(new Runnable() {
					public void run() {
						MainActivity.currentActivityTV
								.append("\nIp address used :" + localIP);
						
						MainActivity.ipTextView.setText("Local IP: "+localIP);
					}
				});

			Log.i(LOGTAG, "Ip address used :" + localIP);
			command = SharingFileService.context.getFilesDir().getPath()
					+ "/ifconfig " + wifiInterface + " " + localIP
					+ " netmask 255.255.0.0 up\n";

			if (MainActivity.currentActivityTV != null)
				MainActivity.handler.post(new Runnable() {
					public void run() {
						MainActivity.currentActivityTV.append("\n"
								+ "ifconfig " + wifiInterface + " " + localIP
								+ " netmask 255.255.0.0 up");
					}
				});

			Log.i(LOGTAG, command);
			Utils.rootExec(command);
		
	}
	
	private void stopWifiAdHoc() {

		if (MainActivity.currentActivityTV!=null)
			MainActivity.handler.post(new Runnable() {
                public void run() {
                	MainActivity.currentActivityTV.append("\n/iwconfig "+wifiInterface+" txpower off");
                	MainActivity.currentActivityTV.append("\nWifi stopped...");}
            });
		
		String command = SharingFileService.context.getFilesDir().getPath()+"/iwconfig "+wifiInterface+" txpower off\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
		
		Log.i(LOGTAG, "Wifi stopped...");
	}

	
	public void changeMinON_OFF()
	{
		SQLiteDatabase db;
		Database dbHelper= new Database(SharingFileService.context);
		db = dbHelper.getReadableDatabase();
		Cursor c = db.query(dbHelper.TABLE_QUERIES_NAME, new String[] { "min("
				+ dbHelper.colONTime + ")" }, null, null, null, null, null);
		c.moveToFirst();
		int minon = c.getInt(0);
		
		c = db.query(dbHelper.TABLE_QUERIES_NAME, new String[] { "min("
				+ dbHelper.colOFFTime + ")" }, null, null, null, null, null);
		c.moveToFirst();
		int minoff = c.getInt(0);
		
		c.close();
		db.close();
		dbHelper.close();
		
		minON=minon;
		minOFF=minoff;
		if (minON==0)
			minON=5;
		if (minOFF==0)
			minOFF=5;
	}

	@Override
	public void run() {
		start();//Turns on for the first time the Wi-fi - this takes up to 5 seconds
		long current_time=System.currentTimeMillis();
		 int seconds = (int) (current_time / 1000) % 60 ;
		 
		 while ((minON+minOFF)!=seconds) //check if the current time has the seconds a multiple of 10, if not, it repeats this step
		 {
		 	 current_time=System.currentTimeMillis();
		 	 seconds = (int) (current_time / 1000) % 60 ;
		 }
		 Log.i(LOGTAG, "Process started at seconds="+seconds);
		turnWifiOn.run();
	}

	private boolean wifi_state=false; //if false then it is not on
	Handler handler=new Handler();
	
	public Runnable turnWifiOn= new Runnable() {
		
		@Override
		public void run() { 

			if (wifi_state==false && stopFromButton==false )
			{
				Log.i(LOGTAG, "Turning wifi ON");
				wakeLock.acquire();
				multicastLock.acquire();
				wifiLock.acquire();
				
				startWifiAdhoc();
				
//				new Thread(new Runnable() {
//					public void run() {
//						changeMinON_OFF(); 
//					}
//				}).run();
				new Thread(incRM).start();
				new Thread(queryMgr).start();
				
				wifi_state=true;
			}
			handler.postDelayed(turnWifiOff, minON*1000);
		}
	};
	
	public Runnable turnWifiOff = new Runnable() {

		@Override
		public void run() {

			if (wifi_state == true) {

				if (queryMgr.canStop()==true)
					if (incRM.getNumThreads()==0)
					{
						Log.i(LOGTAG, "Turning wifi OFF");
						wakeLock.release();
						wifiLock.release();
						multicastLock.release();
						incRM.stopListening();
						stopWifiAdHoc();
						wifi_state = false;
					}
					else
						{
							Log.i(LOGTAG, "Can't turn wifi OFF. Uploads not finished.");
						}
				else
					{
						Log.i(LOGTAG, "Can't turn wifi OFF. Queries not finished.");	
					}
				if (stopFromButton==false)
					handler.postDelayed(turnWifiOn, minOFF*1000);
				changeMinON_OFF();
			}
			
		}
	};
	
	
}
