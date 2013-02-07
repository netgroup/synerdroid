package it.uniroma2.mobilecollaborationplatform;

import it.uniroma2.mobilecollaborationplatform.querymanager.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.widget.*;

/**
 *	A class to manage downloaded files cache
 */
public class CacheManager {
	private final String LOGTAG = "CacheManager";
	private LinkedList<CacheElement> cacheContent;
	private ReentrantLock cacheContentLock;
	
	public CacheManager() {
		cacheContentLock = new ReentrantLock();
		cacheContent = new LinkedList<CacheElement>();
		loadCache();
	}
	
	public void stopCacheManager() {
		saveCache();
	}
	
	/**
	 * Gets a CacheElement from the cache
	 * @param urlOrDigest	The URL or Digest which identifies the file
	 * @return	The CacheElement
	 */
	private CacheElement getCacheElement(String urlOrDigest) {
		urlOrDigest = urlOrDigest.replace("\r", "").replace("\n", "");
		CacheElement element = null;
		boolean digest = !urlOrDigest.startsWith("http");
		cacheContentLock.lock();
		for(CacheElement e : cacheContent) {
			if(digest) {
				if(e.digest.equals(urlOrDigest)) {
					element = e;
					break;
				}
			} else {
				if(e.url.equals(urlOrDigest)) {
					element = e;
					break;
				}
			}
		}
		cacheContentLock.unlock();
		return element;
	}
	
	/**
	 * Checks if a file is in the cache
	 * @param urlOrDigest	URL or digest of the file
	 * @return				True if the file is present, false otherwise
	 */
	public boolean haveFile(String urlOrDigest, String app) {
		return getFile(urlOrDigest, app)!=null;
	}
	
	/**
	 * Gets a file from cache if it's present
	 * @param urlOrDigest	URL or digest of the file
	 * @return				The file, null if it isn't present
	 */
	public File getFile(String urlOrDigest, String app) {
		CacheElement element = getCacheElement(urlOrDigest);
		File file = null;
		
		if(element!=null) {
			if(element.isTimeout()) {
				deleteCacheElement(element);
				return null;
			}
			Log.i(LOGTAG, "Found file in cache: "+urlOrDigest+" "+element.filename);
			file = new File(element.filename);
			if(file.exists()) {
				element.updateLastReqTime();
				return file;
			} else {
				deleteCacheElement(element);
			}
		} else {
			if(!urlOrDigest.startsWith("http")) {
				try {
					Log.i(LOGTAG, "Asking to external app for file");
					OpServiceConnection opServiceConnection = bindOpService(app);
					if(opServiceConnection.queryManager==null) {
						Log.d(LOGTAG, "Application "+app+" not present!");
					} else {
						file = new File(opServiceConnection.queryManager.getFile(urlOrDigest));
					}
					releaseOpService(opServiceConnection);
				} catch(Exception e) {}
			}
		}
		
		return file;
	}
	
	/**
	 * Puts a file in cache
	 * @param url
	 * @param file		The file
	 * @param timeout	The timeout after which the cache deletes the file (in milliseconds)
	 */
	public CacheElement putFile(String url, File file, Long timeout) {
		if(url!=null) url = url.replace("\r", "").replace("\n", "");
		String digest = Utils.getMD5Digest(file);
		CacheElement element = new CacheElement(url, file, digest, timeout);
		cacheContentLock.lock();
		cacheContent.add(element);
		cacheContentLock.unlock();
		checkCacheSize();
		Log.i(LOGTAG, "File put in cache: "+url);
		return element;
	}
	
	/**
	 * Gets a new empty file
	 * @return	The file
	 * @throws IOException
	 */
	public static File newCacheFile() throws IOException {
		return File.createTempFile("cache", "", ProxyService.context.getExternalCacheDir());
	}
	
	/**
	 * Deletes a file from the cache
	 * @param url
	 */
	private void deleteCacheElement(CacheElement ce) {
		File f = new File(ce.filename);
		f.delete();
		cacheContentLock.lock();
		cacheContent.remove(ce);
		cacheContentLock.unlock();
	}
	
	/**
	 * Checks if the cache exceeded maximum size and takes action
	 */
	public void checkCacheSize() {
		int size = 0;
		CacheElement oldest = null;
		for(CacheElement e : cacheContent) {
			size += e.size;
			if(oldest==null) {
				oldest = e;
			} else {
				if(e.lastReqTime<oldest.lastReqTime) oldest = e;
			}
		}
		
		if(size>BaseSettings.MAXCACHESIZE) {
			Log.i(LOGTAG, "Cache full. "+oldest.filename+" will be deleted.");
			deleteCacheElement(oldest);
		}
	}
	
	/**
	 * Clears the cache
	 */
	public void clearCache() {
		File[] files = ProxyService.context.getExternalCacheDir().listFiles();
		for(File f : files) {
			f.delete();
		}
		cacheContent.clear();
		Toast.makeText(ProxyService.context, "Cache cleared!", Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Generates the response to a new query. Returns the digest of the response file put in cache.
	 * @param query
	 * @return
	 */
	public String newQuery(String query, String app) {
		try {
			OpServiceConnection opServiceConnection = bindOpService(app);
			if(opServiceConnection.queryManager==null) return null; // There's no external app which can answer the query.
			
			String res = opServiceConnection.queryManager.getQueryResponse(query);
			if(res==null) {
				Log.i(LOGTAG, "Null response from app.");
				return null;
			}
			
			if(res.contains("\r") || res.contains("\n") || res.contains(";;")) {
				Log.i(LOGTAG, "Query response contains illegal characters. This response will be discarded.");
				return null;
			}
			File file = newCacheFile();
			FileWriter writer = new FileWriter(file);
			writer.write(res);
			writer.flush();
			writer.close();
			
			releaseOpService(opServiceConnection);
			
			CacheElement element = putFile(null, file, BaseSettings.QUERYCACHINGTIMEOUT);
			return element.digest;
		} catch(Exception e) {
			Log.e(LOGTAG, "newQuery()", e);
			return null;
		}
	}
	
	/**
	 * Serializes cache content
	 */
	private void saveCache() {
		CacheElement[] cacheset = cacheContent.toArray(new CacheElement[0]);
		
		File f = new File(ProxyService.context.getFilesDir().toString()+"/cacheContent");
		f.delete();
		try {
			f.createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
			Log.i(LOGTAG, "Cache elements to be written: "+cacheset.length);
			oos.writeInt(cacheset.length);
			for(int i=0; i<cacheset.length; i++) {
				oos.writeObject(cacheset[i]);
				Log.i(LOGTAG, "Written: URL: "+cacheset[i].url+", Digest: "+cacheset[i].digest);
			}
			oos.flush();
			oos.close();
		} catch(Exception e) {
			Log.e(LOGTAG, "saveCache()", e);
		}
	}
	
	/**
	 * Deserializes cache content
	 */
	private void loadCache() {
		cacheContent = new LinkedList<CacheElement>();
		File f = new File(ProxyService.context.getFilesDir().toString()+"/cacheContent");
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
			int size = ois.readInt();
			for(int i=0; i<size; i++) {
				CacheElement e = (CacheElement)ois.readObject();
				cacheContent.add(e);
			}
			ois.close();
		} catch(Exception e) {
			Log.e(LOGTAG, "loadCache()", e);
		}
	}
	
	
	
	
	
	// CODE USED TO BIND TO EXTERNAL APPLICATION
	
	private OpServiceConnection bindOpService(String app) {
    	OpServiceConnection opServiceConnection = new OpServiceConnection();
    	Intent intent = new Intent(app);
		ProxyService.context.bindService(intent, opServiceConnection, Context.BIND_AUTO_CREATE);
		
		int i = 0;
		while(opServiceConnection.queryManager==null && i<15) {
			try { Thread.sleep(100); } catch(Exception e) {}
			i++;
		}
		
		return opServiceConnection;
	}

	private void releaseOpService(OpServiceConnection opServiceConnection) {
		ProxyService.context.unbindService( opServiceConnection );
		opServiceConnection = null;
	}
    
    class OpServiceConnection implements ServiceConnection {
    	private String LOGTAG = "OpServiceConnection";
    	public QueryManagerOp queryManager;
    	
        public void onServiceConnected(ComponentName className, IBinder boundService) {
        	queryManager = QueryManagerOp.Stub.asInterface((IBinder)boundService);
		 	Log.d(LOGTAG,"QueryManager service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
        	queryManager = null;
			Log.d(LOGTAG, "QueryManager service disconnected");
        }
    }
}
