package it.uniroma2.mobilecollaborationplatform;


public class BaseSettings {
	// Name for preferences
	public static final String PREFS_NAME = "Settings";
	
	public final static String APPDIR = "/data/data/it.uniroma2.mobilecollaborationplatform/";

	// Proxy port
	public static final int PROXYPORT = 8080;
	
	// Maximum cache size in Byte
	//public final static int MAXCACHESIZE = 100000000;
	public static int MAXCACHESIZE;
	
	// TCP server port
	public final static int TCPPORT = 6000;
	
	// UDP server port
	public final static int UDPPORT = 5000;
	
	public final static int UDPREQTIMEOUT = 7000;
	
	public final static long QUERYCACHINGTIMEOUT = 10000;
	
	// Applications which need to use proxy
	public final static NetApp[] apps = {
		new NetApp("com.android.browser", 80),
		//new NetApp("com.opera.browser", 80),
		//new NetApp("android.media", 5228, "media"),
		new NetApp("android.media", 80, "media")
	};
	
	public static boolean serversListenOnAllInterfaces;
	
	public final static String WIFI_ESSID = "mcp";
	public final static int WIFI_CHANNEL = 1; // On Galaxy SII seems only to work with channel 1
	
	public final static int ZIGBEEPORT = 11111;
	public final static int MAXZIGBEEPAYLOAD = 20;
}
