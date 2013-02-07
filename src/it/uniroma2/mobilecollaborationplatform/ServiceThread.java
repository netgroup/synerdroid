package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.net.*;

import android.os.*;
import android.util.Log;

/**
 * The service main thread
 */
public class ServiceThread extends Thread {
	private final String LOGTAG = "ServiceThread";
	
	private boolean running;
	private CacheManager cacheManager;
	private DownloadManager downloadManager;
	private VideoDownloader videoDownloader;
	private ServerSocket serverSocket;
	private DataExchanger dataExchanger;
	private SignallingExchanger signallingExchanger;
	
	public ServiceThread(String signallingTechnology, boolean clearCache, PowerManager powerManager) {
		cacheManager = new CacheManager();
		if(clearCache) cacheManager.clearCache();
		
		dataExchanger = new DataExchanger(cacheManager);
		if(signallingTechnology.equals("WiFi")) {
			signallingExchanger = new SignallingExchangerWifi(cacheManager);
		} else {
			signallingExchanger = new SignallingExchangerZigbee(cacheManager);
		}
		downloadManager = new DownloadManager(cacheManager, dataExchanger, signallingExchanger);
		videoDownloader = new VideoDownloader(downloadManager);
		dataExchanger.start();
		
		Thread t = new Thread(signallingExchanger);
		t.start();
	}

	public void run() {
		running = true;

		try {
			if(BaseSettings.serversListenOnAllInterfaces) {
				serverSocket = new ServerSocket(BaseSettings.PROXYPORT);
			} else {
				serverSocket = new ServerSocket(BaseSettings.PROXYPORT, 50, InetAddress.getByName("127.0.0.1")); // Listen on localhost only for security reasons
			}
			while(running) {
				// Waits for a connection from a client
				Socket socket = serverSocket.accept();
				// Starts a thread for every accepted connection
				Runnable r = new ProxyConnectionThread(socket, downloadManager, videoDownloader);
				Thread t = new Thread(r);
				t.start();
			}
		} catch (IOException e) {
			Log.e(LOGTAG, e.getMessage());
		}
	}
	
	public void stopServiceThread() {
		running = false;
		downloadManager.stopDownloadManager();
		signallingExchanger.stopServer();
		dataExchanger.stopServer();
		cacheManager.stopCacheManager();
		try {
			if(serverSocket!=null) serverSocket.close();
		} catch (IOException e) {}
	}
}
