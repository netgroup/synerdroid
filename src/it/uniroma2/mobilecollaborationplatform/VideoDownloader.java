package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

import android.util.Log;

/**
 * A class which manages the download of videos
 */
public class VideoDownloader {
	private final static String LOGTAG = "VideoDownloader";
	private DownloadManager downloadManager;
	private HashMap<String, Video> videos;
	private Lock videosLock;
	
	public VideoDownloader(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
		videos = new HashMap<String, Video>();
		videosLock = new ReentrantLock();
	}
	
	/**
	 * Gets a video file
	 * @param url	The URL of the M3U8 or of the TS file
	 * @return		The downloaded file
	 */
	public File getFile(String url) {
		Log.i(LOGTAG, "Video URL request: "+url);
		removeTimeoutVideos();
		
		if(url.endsWith(".m3u8")) {
			videosLock.lock();
			Video v = videos.get(url);
			videosLock.unlock();
			if(v==null) { // New video
				File file = downloadManager.getFileFromURL(url, false);
				if(file==null) return null;
				Video video = new Video(url);
				video.parseM3U8(file);
				video.start();
				videosLock.lock();
				videos.put(url, video);
				videosLock.unlock();
				return file;
			} else { // Video already in cache
				//return downloadManager.getFileFromURL(url);
				File file = downloadManager.getFileFromURL(url, false);
				if(file==null) return null;
				v.parseM3U8(file);
				return file;
			}
		} else if(url.endsWith(".ts")) {
			Video v = getVideoFromTS(url);
			if(v==null) return downloadManager.getFileFromURL(url, false);
			v.updateLastRequest();
			VideoChunk chunk = v.getTS(url);
			return v.downloadFile(chunk);
		} else {
			Log.e(LOGTAG, "Error in getFile()");
			return null;
		}
	}
	
	/**
	 * Searches the Video object associated to the TS URL
	 * @param url	The URL of the TS file
	 * @return		The Video object found, null if no object is found
	 */
	private Video getVideoFromTS(String url) {
		Video video = null;
		videosLock.lock();
		for(Video v : videos.values()) {
			if(v.getTS(url)!=null) video = v;
		}
		videosLock.unlock();
		return video;
	}
	
	/**
	 * Looks for videos in timeout and removes them from the list
	 */
	private void removeTimeoutVideos() {
		videosLock.lock();
		Video[] videosArray = videos.values().toArray(new Video[0]);
		for(Video v : videosArray) {
			if(v.timeout()) {
				Log.e(LOGTAG, "Video in timeout: "+v.m3u8url);
				videos.remove(v.m3u8url);
			}
		}
		videosLock.unlock();
	}
	
	/**
	 * A class that represents a video
	 */
	private class Video extends Thread {
		private static final String LOGTAG = "Video";
		private String m3u8url;
		private long lastRequest;
		private List<VideoChunk> videoChunks;
		private long timeoutInterval;
		private String m3u8parentUrl;
		
		public Video(String m3u8url) {
			this.m3u8url = m3u8url;
			videoChunks = new ArrayList<VideoChunk>();
			
			m3u8parentUrl = Utils.getPathFromUrl(m3u8url);
			timeoutInterval = 1000000000*4;
		}
		
		public void parseM3U8(File file) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while((line=reader.readLine())!=null) {
					line = line.trim();
					if(line.startsWith("#EXT-X-TARGETDURATION")) {
						String parts[] = line.split(":");
						String value = parts[1].trim();
						timeoutInterval = Long.parseLong(value)*1000000000*4;
					} else if(line.startsWith("#")) {
						// Comment line
					} else {
						VideoChunk chunk;
						if(line.startsWith("http://")) { // Static url
							chunk = new VideoChunk(line);
						} else { // Relative url
							chunk = new VideoChunk(m3u8parentUrl+line);
						}
						if(!videoChunks.contains(chunk)) videoChunks.add(chunk);
					}
				}
				reader.close();
				
				updateLastRequest();
			} catch(Exception e) {
				Log.e(LOGTAG, "Constructor", e);
			}
		}
		
		/**
		 * Updates last request for a file of this video
		 */
		public void updateLastRequest() {
			lastRequest = System.nanoTime();
		}
		
		/**
		 * Checks if timeout occurred
		 * @return		True if timeout, false otherwise
		 */
		public boolean timeout() {
			if(System.nanoTime()-lastRequest > timeoutInterval) return true;
			else return false;
		}
		
		/**
		 * Gets a VideoChunk object from its URL
		 * @param url	The URL of the video chunk
		 * @return		The VideoChunk
		 */
		public VideoChunk getTS(String url) {
			for(VideoChunk chunk : videoChunks) {
				if(chunk.url.equals(url)) return chunk;
			}
			return null; // The chunk is not part of this video
		}
		
		/**
		 * Downloads a TS file
		 * @param chunk		The chunk to be downloaded
		 * @return			The downloaded file
		 */
		public File downloadFile(VideoChunk chunk) {
			chunk.lock.lock();
			File file = downloadManager.getFileFromURL(chunk.url, true);
			chunk.lock.unlock();
			return file;
		}
		
		/**
		 * Thread which downloads immediately all chunks of the video
		 */
		public void run() {
			videosLock.lock();
			VideoChunk[] chunks = videoChunks.toArray(new VideoChunk[0]);
			videosLock.unlock();
			
			for(VideoChunk chunk : chunks) {
				if(timeout()) {
					return;
				}
				Log.i(LOGTAG, "Starting predownloading chunk: "+chunk.url);
				downloadFile(chunk);
				Log.i(LOGTAG, "Finished predownloading chunk: "+chunk.url);
			}
		}
	}
	
	/**
	 * A class that represents a video chunk
	 */
	private class VideoChunk {
		public String url;
		public Lock lock;
		public VideoChunk(String url) {
			this.url = url;
			lock = new ReentrantLock();
		}
	}
}
