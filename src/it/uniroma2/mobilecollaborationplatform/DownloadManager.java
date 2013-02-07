package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.net.*;
import java.util.*;
import android.util.Log;

/**
 *	Class to manage the download of a file
 */
public class DownloadManager extends Thread {
	private final static String LOGTAG = "DownloadManager";
	CacheManager cacheManager;
	boolean running;
	private DataExchanger dataExchanger;
	private SignallingExchanger signallingExchanger;
	
	public DownloadManager(CacheManager cacheManager, DataExchanger dataExchanger, SignallingExchanger signallingExchanger) {
		this.cacheManager = cacheManager;
		this.dataExchanger = dataExchanger;
		this.signallingExchanger = signallingExchanger;
	}
	
	public void stopDownloadManager() {
		running = false;
	}
	
	/**
	 * Downloads a file knowing its URL. If the file is found on a nearby devices it's downloaded from there.
	 * Otherwise it's downloaded from cellular network.
	 * @param url	The URL of the resource
	 * @return		The downloaded file
	 */
	public File getFileFromURL(String url, boolean saveInCache) {
		File file = cacheManager.getFile(url, null);
		if(file!=null) {
			Log.i(LOGTAG, "File already in cache!");
			return file;
		}
		
		Log.i(LOGTAG, "Searching devices nearby...");
		String response = signallingExchanger.sendSignal("REQ;;HTTP;;"+Utils.getLocalIpAddress()+";;"+url);
		
		String deviceFound = null;
		if(response!=null && response.startsWith("RES;;HTTP")) {
			String parts[] = response.split(";;");
			deviceFound = parts[2];
		}
		
		if(deviceFound==null) {
			file = getFileFromHTTP(url, saveInCache);
		} else {
			Log.i(LOGTAG, "Device found!");
			file = dataExchanger.getFile(url, null, deviceFound);
			if(file==null) { // Something went wrong during file transfer
				Log.i(LOGTAG, "Error downloading from device, starting download from HTTP");
				file = getFileFromHTTP(url, saveInCache);
			} else {
				if(saveInCache) cacheManager.putFile(url, file, null);
			}
		}
		//if(file!=null && saveInCache) cacheManager.putFile(url, file);
		return file;
	}
	
	/**
	 * Downloads a file from HTTP and puts it into the cache
	 * @param url
	 * @return	Downloaded file
	 */
	private File getFileFromHTTP(String url, boolean saveInCache) {
		try {
			File file = CacheManager.newCacheFile();
			FileOutputStream fileOS = new FileOutputStream(file);
			URLConnection connection = new URL(url).openConnection();
	        InputStream httpIS = connection.getInputStream();
	        
	        String cacheControl = connection.getHeaderField("cache-control");
	        if(cacheControl!=null && cacheControl.equals("no-cache")) {
	        	saveInCache = false;
	        }
	        
	        long now = connection.getDate();
	        long expire = connection.getHeaderFieldDate("expires", 0);
	        
	        Long timeout = null;
	        if(expire!=0) timeout = expire-now+System.nanoTime();
	        
	        int read;
			byte[] buffer = new byte[8192];
			while((read=httpIS.read(buffer))!=-1) {
				fileOS.write(buffer, 0, read);
				fileOS.flush();
			}
	        
	        httpIS.close();
	        fileOS.close();
	        
	        if(file!=null && saveInCache) cacheManager.putFile(url, file, timeout);
	        
	        return file;
		} catch(Exception e) {
			Log.e(LOGTAG, "getFileFromHTTP()", e);
			return null;
		}
	}
	
	/**
	 * Gets metadata associated to a query from a nearby device
	 * @param query		The query to be sent
	 * @param app		The application which requested the service
	 * @return			Metadata file content
	 */
	public List<String> getQueryResponse(String query, String app) {
		File file = null;
		Log.i(LOGTAG, "Searching devices nearby...");
		
		LinkedList<String> filesContent = new LinkedList<String>();
		
		List<String> responses = signallingExchanger.sendSignalMultipleAnswer("REQ;;MCP/QUERY;;"+Utils.getLocalIpAddress()+";;"+app+";;"+query);
		
		for(String response : responses) {
			String deviceFound = null;
			String metadataDigest = null;
			
			if(response!=null && response.startsWith("RES;;MCP/QUERY")) {
				Log.i(LOGTAG, "Received response: "+response);
				String parts[] = response.split(";;");
				deviceFound = parts[2];
				metadataDigest = parts[3];
			}
			
			if(deviceFound==null) {
				continue;
			} else {
				Log.i(LOGTAG, "Device found!");
				file = dataExchanger.getFile(metadataDigest, app, deviceFound);
			}
			
			String /*line,*/ fileContent="";
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				fileContent = reader.readLine();
				//while((line=reader.readLine())!=null) {
					//fileContent += line+"\n";
				//}
				reader.close();
				filesContent.add(fileContent);
			} catch(Exception e) {
				continue;
			}
		}
		
		return filesContent;
	}
	
	/**
	 * Downloads a file from a nearby device
	 * @param digest	The file digest
	 * @param app		The application which requested the service
	 * @return			The downloaded file
	 */
	public File getFile(String digest, String app) {
		Log.i(LOGTAG, "getFile(): requested digest: "+digest+", app: "+app);
		File file = cacheManager.getFile(digest, app);
		if(file!=null) {
			Log.i(LOGTAG, "File already in cache!");
			return file;
		}
		
		Log.i(LOGTAG, "Searching devices nearby...");
		String response = signallingExchanger.sendSignal("REQ;;MCP/FILE;;"+Utils.getLocalIpAddress()+";;"+app+";;"+digest);
		
		String deviceFound = null;
		if(response!=null && response.startsWith("RES;;MCP/FILE")) {
			String parts[] = response.split(";;");
			deviceFound = parts[2];
		}
		
		if(deviceFound==null) {
			return null;
		} else {
			file = dataExchanger.getFile(digest, app, deviceFound);
		}
		if(file!=null) cacheManager.putFile(null, file, null);
		return file;
	}
}
