package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.net.*;
import android.util.Log;

/**
 * The TCP server to send content to other devices
 */
public class DataExchanger extends Thread {
	private String LOGTAG = "DataExchanger";
	private boolean running = false;
	private CacheManager cacheManager;
	private ServerSocket serverSocket;
	
	
	public DataExchanger(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
	
	/**
	 * Downloads a file from a nearby device
	 * @param url
	 * @param friendIP	IP address of the other device
	 * @return	Downloaded file
	 */
	public File getFile(String urlOrDigest, String app, String deviceIP) {
		Log.i(LOGTAG, "Starting download from: "+deviceIP);
		try {
			File file = CacheManager.newCacheFile();
			FileOutputStream fileOS = new FileOutputStream(file);
			
			Socket socket = new Socket(deviceIP, BaseSettings.TCPPORT);
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			output.writeUTF(urlOrDigest);
			if(app==null) output.writeUTF("");
			else output.writeUTF(app);
			InputStream input = socket.getInputStream();
	        
	        int read;
			byte[] buffer = new byte[8192];
			while((read=input.read(buffer))!=-1) {
				fileOS.write(buffer, 0, read);
				fileOS.flush();
			}
	        
	        input.close();
	        output.close();
	        fileOS.close();
	        socket.close();
	        return file;
		} catch(Exception e) {
			Log.e(LOGTAG, "getFileFromWifi()", e);
			return null;
		}
	}
	
	public void run() {
		Log.i(LOGTAG, "Starting server TCP");
		running = true;
		try {
			if(BaseSettings.serversListenOnAllInterfaces) {
				serverSocket = new ServerSocket(BaseSettings.TCPPORT);
			} else {
				serverSocket = new ServerSocket(BaseSettings.TCPPORT, 50, Utils.getLocalInetAddress());
			}
			while(running) {
				Socket socket = serverSocket.accept();
				new ServerTCPConnection(socket).start();
			}
		} catch(Exception e) {
			Log.e(LOGTAG, "run()", e);
		}
	}
	
	public void stopServer() {
		running = false;
		try {
			serverSocket.close();
		} catch(Exception e) {}
	}
	
	private class ServerTCPConnection extends Thread {
		private String LOGTAG = "ServerTCPConnection";
		private Socket socket;
		
		public ServerTCPConnection(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			try {
				Log.i(LOGTAG, "ServerTCP: Client connected: "+socket.getRemoteSocketAddress().toString());
				
				DataInputStream input = new DataInputStream(socket.getInputStream());
				OutputStream output = socket.getOutputStream();
				
				String req = input.readUTF();
				String app = input.readUTF();
				if(app.equals("")) app=null;
				Log.i(LOGTAG, "TCP request: "+req+", APP: "+app);
				
				File file = cacheManager.getFile(req, app);
				if(file!=null) {
					FileInputStream fileInput = new FileInputStream(file);
					
					int read;
					byte[] buffer = new byte[8192];
					while((read=fileInput.read(buffer))!=-1) {
						output.write(buffer, 0, read);
						output.flush();
					}
				}
				
				input.close();
				output.close();
			} catch(Exception e) {
				Log.e(LOGTAG, "run()", e);
			} finally {
				try {
					socket.close();
				} catch(Exception e) {}
			}
		}
	}
}
