package it.uniroma2.mobilecollaborationplatform;

import java.io.*;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import android.util.*;

public class ProxyService extends Service {
	private final String LOGTAG = "ProxyService";
	
	public static Context context;
	private final static int NOTIFICATION_ID = 1;
	private NotificationManager notificationManager;
	private ServiceThread serviceThread;
	private Notification notificationStart;
	private Notification notificationEnd;
	private PendingIntent pIntent;
	
	private boolean noWifiAdHoc = false;
	private boolean noIptablesRedirect = false;
	private boolean disableFirewall = false;
	
	PowerManager powerManager;
	PowerManager.WakeLock wakeLock;
	WifiManager wifiManager;
	WifiManager.WifiLock wifiLock;
	WifiManager.MulticastLock multicastLock;

	@Override
	public void onCreate() {
		super.onCreate();
		
		// Power wake lock is required to avoid the CPU sleep. Otherwise the service can't answer to requests from other devices
		powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, LOGTAG); // PARTIAL_WAKE_LOCK Only keeps CPU on
		wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
	    //wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, LOGTAG);
		wifiLock = wifiManager.createWifiLock(3, LOGTAG);
		multicastLock = wifiManager.createMulticastLock(LOGTAG);
		
		context = getApplicationContext();
		// Generates the notifications for the status bar
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationStart = new Notification(R.drawable.ic_launcher, getString(R.string.proxyEnabled), System.currentTimeMillis());
		notificationStart.flags |= Notification.FLAG_ONGOING_EVENT;
		notificationEnd = new Notification(R.drawable.ic_launcher, getString(R.string.proxyDisabled), System.currentTimeMillis());
		notificationEnd.flags |= Notification.FLAG_ONGOING_EVENT;
		Intent intent = new Intent(this, MCPActivity.class);
		pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		// Acquires CPU wake lock
		wakeLock.acquire();
		wifiLock.acquire();
		multicastLock.acquire();
		Settings.System.putInt(getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
		
		// Copies required executables to the application directory
		copyFileFromAssets("iptables", false);
		copyFileFromAssets("wifiloader", false);
		copyFileFromAssets("iwconfig", false);
		copyFileFromAssets("ifconfig", true);
		copyFileFromAssets("ip", false);
		copyFileFromAssets("busybox", false);
		
		/*AssetManager assetManager = getAssets();
		File modulesdir = new File(BaseSettings.APPDIR+"zdev/modules");
		modulesdir.mkdirs();
		try {
			String[] zdevfiles = assetManager.list("zdev");
			for(String s : zdevfiles) {
				File f = new File(BaseSettings.APPDIR+"zdev/"+s);
				if(f.isDirectory()) continue;
				copyFileFromAssets("zdev/"+s, false);
			}
			String[] modulesfiles = assetManager.list("zdev/modules");
			for(String s : modulesfiles) {
				copyFileFromAssets("zdev/modules/"+s, false);
			}
		} catch(Exception e) {
			Log.e(LOGTAG, "onStartCommand()", e);
		}*/
		
		// Reads data passed from the GUI
		String signallingTechnology = intent.getStringExtra("signallingTechnology");
		boolean clearCache = intent.getBooleanExtra("clearCache", false);
		BaseSettings.MAXCACHESIZE = intent.getIntExtra("maxCacheSize", 100)*1024*1024;
		noWifiAdHoc = intent.getBooleanExtra("noWifiAdHoc", false);
		noIptablesRedirect = intent.getBooleanExtra("noIptablesRedirect", false);
		disableFirewall = intent.getBooleanExtra("disableFirewall", false);
		BaseSettings.serversListenOnAllInterfaces = intent.getBooleanExtra("serversListenOnAllInterfaces", false);
		
		if(!noIptablesRedirect) setIptablesRedirect();
		if(!noWifiAdHoc) IFManager.startWifiAdHoc();
		if(!disableFirewall) IFManager.enableFirewall();
		
		serviceThread = new ServiceThread(signallingTechnology, clearCache, powerManager);
		serviceThread.start();
		
		// Notifies the user the proxy started
		Log.i(LOGTAG, "Service Started");
		notificationStart.setLatestEventInfo(this, getString(R.string.app_name)+" | "+getString(R.string.proxyEnabled), getString(R.string.runningInBackground), pIntent);
		notificationManager.notify(NOTIFICATION_ID, notificationStart);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		serviceThread.stopServiceThread();
		
		if(!noIptablesRedirect) resetIptablesRedirect();
		if(!noWifiAdHoc) IFManager.stopWifiAdHoc();
		if(!disableFirewall) IFManager.disableFirewall();
		
		// Notifies the user the proxy stopped
		notificationManager.cancel(NOTIFICATION_ID);
		notificationEnd.setLatestEventInfo(this, getString(R.string.app_name)+" | "+getString(R.string.proxyDisabled), "", pIntent);
		notificationManager.notify(NOTIFICATION_ID, notificationEnd);
		notificationManager.cancel(NOTIFICATION_ID);
		
		// Releases wake locks
		multicastLock.release();
		wifiLock.release();
		wakeLock.release();

		super.onDestroy();
		Log.i(LOGTAG, "Service Destroyed");
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	/**
	 * Sets redirects to the proxy port for legacy applications.
	 */
	private void setIptablesRedirect() {
		// Sets TCP iptables rules for every application in the list
		for(NetApp app : BaseSettings.apps) {
			String command = BaseSettings.APPDIR+"iptables -t nat -m owner --uid-owner " + app.uid + " -A OUTPUT -p tcp  ! -d 127.0.0.1 --dport " + app.port + " -j REDIRECT --to " + BaseSettings.PROXYPORT + "\n";
			Log.i(LOGTAG, command);
			Utils.rootExec(command);
		}
	}
	
	/**
	 * Removes the iptables rules set by the application
	 */
	private void resetIptablesRedirect() {
		Utils.rootExec(BaseSettings.APPDIR+"iptables -t nat -F OUTPUT\n");
		Log.i(LOGTAG, "iptables rules resetted");
	}
	
	/**
	 * Copies binary files from assets to the application directory
	 * @param name								Name of the executable
	 * @param differentProcessorExecutables		Whether there are different executables for different processors
	 */
	private void copyFileFromAssets(String name, boolean differentProcessorExecutables) {
		AssetManager assetManager = getAssets();
		InputStream in = null;
		OutputStream out = null;
		try {
			if(differentProcessorExecutables) {
				if(Utils.processorIsARMv7()) {
					in = assetManager.open(name+"-armv7");
				} else {
					in = assetManager.open(name+"-armv6");
				}
			} else {
				in = assetManager.open(name);
			}
			out = new FileOutputStream(BaseSettings.APPDIR+name);
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch (Exception e) {
			Log.e(LOGTAG, e.getMessage());
		}

		// Changes file permissions
		String command = "chmod 777 "+BaseSettings.APPDIR+name+"\n";
		Utils.rootExec(command);
	}
}
