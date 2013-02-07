package it.uniroma2.mobilecollaborationplatform;

import java.util.*;
import android.util.Log;

/**
 * Superclass for the Signalling Exchanger
 */
public abstract class SignallingExchanger implements Runnable {
	private final String LOGTAG = "SignallingExchanger";
	CacheManager cacheManager;
	
	public abstract String sendSignal(String msg);
	public abstract List<String> sendSignalMultipleAnswer(String msg);
	public abstract void stopServer();
	
	public SignallingExchanger(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
	
	/**
	 * Manages a new signalling request arrived from another device
	 * @param req	The request string
	 * @return		The response string, null if no response has to be sent back
	 */
	protected String manageRequest(String req) {
		String[] parts = req.split(";;");
		if(!parts[0].equals("REQ")) return null;
		
		if(parts[1].equals("HTTP")) {
			String url = parts[3];
			if(cacheManager.haveFile(url, null)) return "RES;;HTTP;;"+Utils.getLocalIpAddress()+";;"+url;
		} else if(parts[1].equals("MCP/QUERY")) {
			Log.i(LOGTAG, "Query request arrived");
			if(parts.length<5) {
				Log.d(LOGTAG, "Query not valid");
				return null;
			}
			String app = parts[3];
			String query = parts[4];
			String digest = cacheManager.newQuery(query, app);
			if(digest==null) return null;
			return "RES;;MCP/QUERY;;"+Utils.getLocalIpAddress()+";;"+digest;
		} else if(parts[1].equals("MCP/FILE")) {
			String app = parts[3];
			String digest = parts[4];
			if(cacheManager.haveFile(digest, app)) return "RES;;MCP/FILE;;"+Utils.getLocalIpAddress()+";;"+digest;
		}
		return null;
	}
}
