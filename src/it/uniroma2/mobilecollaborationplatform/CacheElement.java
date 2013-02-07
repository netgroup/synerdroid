package it.uniroma2.mobilecollaborationplatform;

import java.io.*;

/**
 *	A class that represents an element in the cache
 */
public class CacheElement implements Serializable {
	private static final long serialVersionUID = 1L;
	public String filename;
	public String url;
	public long insertTime;
	public long lastReqTime;
	public Long timeout;
	public long size;
	public String digest;
	
	public CacheElement(String url, File file, String digest, Long timeout) {
		this.url = url;
		this.digest = digest;
		this.timeout = timeout;
		filename = file.toString();
		insertTime = System.nanoTime();
		lastReqTime = insertTime;
		size = file.length();
	}
	
	public void updateLastReqTime() {
		lastReqTime = System.nanoTime();
	}
	
	public boolean isTimeout() {
		if(timeout==null) return false;
		if(timeout>System.nanoTime()) return true;
		return false;
	}
}
