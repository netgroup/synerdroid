package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.net.*;
import java.util.*;

import android.util.Log;

public class ProxyConnectionThread implements Runnable {
	private final String LOGTAG = "ProxyConnectionThread";
	private Socket socket;
	private DownloadManager downloadManager;
	private VideoDownloader videoDownloader;
	private Vector<String> request;
	
	public ProxyConnectionThread(Socket socket, DownloadManager downloadManager, VideoDownloader videoDownloader) {
		this.socket = socket;
		this.downloadManager = downloadManager;
		this.videoDownloader = videoDownloader;
		request = new Vector<String>();
	}
	
	/**
	 * Main functions. Receives the HTTP request from client and generates the answer.
	 */
	public void run() {		
		Log.d(LOGTAG, "HTTP client " + socket.getInetAddress() + ":" + socket.getPort() + " connected");
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = null;
			//while(in.ready()) {
			do {
				line = in.readLine();
				Log.d(LOGTAG, "HTTP Line: #"+line+"#");
				request.add(line);
			} while(line!=null && !line.equals(""));
			parseHTTP();
			in.close();
			socket.close();
		} catch(Exception e) {
			Log.e(LOGTAG, "run()", e);
		}
	}
	
	/**
	 * Parses an HTTP request
	 */
	private void parseHTTP() {
		if(request.size()==0) {
			generateErrorAnswer(400, "Request message is empty");
			return;
		}
		if(request.get(0).equals("GET //MCP/QUERY HTTP/1.1")) {
			Log.i(LOGTAG, "Extended HTTP found");
			parseExtendedHTTPQuery();
		} else if(request.get(0).equals("GET //MCP/FILE HTTP/1.1")) {
			parseExtendedHTTPFile();
		} else {
			Log.i(LOGTAG, "Legacy HTTP found");
			parseLegacyHTTP();
		}
	}
	
	/**
	 * Parses a legacy HTTP request
	 */
	private void parseLegacyHTTP() {
		String filename=null, host=null;
		int port = 80;
		for(String line : request) {
			if(line.startsWith("GET")) {
				filename = line.replace("GET ", "").replace(" HTTP/1.1", "");
			} else if(line.startsWith("Host")) {
				host = line.replace("Host: ", "");
				if(host.contains(":")) {
					StringTokenizer st = new StringTokenizer(line, ":");
					host = st.nextToken();
					port = Integer.parseInt(st.nextToken());
				} else {
					port = 80;
				}
			}
		}
		
		if(filename==null || host==null) {
			generateErrorAnswer(400, "Filename or host not specified");
		}
		
		String url = "http://"+host+filename;
		
		boolean fromMCP = false;
		if(filename.endsWith(".m3u8") || filename.endsWith(".ts")) {
			fromMCP = true;
		}
		
		if(fromMCP) {
			File file;
			if(filename.endsWith(".m3u8") || filename.endsWith(".ts")) {
				file = videoDownloader.getFile(url);
			} else {
				file = downloadManager.getFileFromURL(url, true);
			}
			String mime = Utils.getMime(url);
			if(file!=null) generateAnswerFromFile(file, mime);
			else generateErrorAnswer(404, "File not found");
		} else {
			simpleHttpGet(host, port);
		}
	}
	
	private void parseExtendedHTTPQuery() {
		String query = null, app = null;
		int ttl = 60000;
		
		for(String line : request) {
			String value = getHttpParameterValue(line);
			if(line.startsWith("x-mcp-query")) {
				query = value;
			} else if(line.startsWith("x-mcp-ttl")) {
				ttl = Integer.parseInt(value);
			} else if(line.startsWith("x-mcp-app")) {
				app = value;
			}
		}
		
		if(query==null || app==null || query.contains("\r") || query.contains("\n") || query.contains(";;")) {
			generateErrorAnswer(400, "Query message not valid. Check if it contains illegal characters");
			return;
		}
		
		try {
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // Output stream to the client
			String msg = "HTTP/1.1 200 OK\r\n";
			msg += "Keep-Alive: timeout=15, max=100\r\n";
			msg += "Connection: Keep-Alive\r\n";
			msg += "Transfer-Encoding: chunked\r\n";
			msg += "Content-Type: text/plain\r\n";
			msg += "\r\n";
			out.writeBytes(msg);
			out.flush();
			
			while(ttl>0) {
				List<String> responses = downloadManager.getQueryResponse(query, app);
				
				for(String response : responses) {
					//msg = response;
					int chunkSize = response.length();
					out.writeBytes(Integer.toHexString(chunkSize)+"\r\n");
					out.writeBytes(response+"\r\n");
					out.flush();
				}
				
				
				Thread.sleep(5000);
				ttl-=5000;
			}
			
			out.writeBytes("0\r\n");
			out.flush();
			out.close();
		} catch(Exception e) {
			Log.e(LOGTAG, "parseExtendedHTTPQuery()", e);
			return;
		}
	}
	
	
	private void parseExtendedHTTPFile() {
		String digest = null, app = null;
		for(String line : request) {
			if(line.startsWith("x-mcp-digest")) {
				digest = getHttpParameterValue(line);
			} else if(line.startsWith("x-mcp-app")) {
				app = getHttpParameterValue(line);
			}
		}
		
		if(digest==null || app==null) {
			generateErrorAnswer(400, "Needed headers not specified");
			return;
		}
		
		File file = downloadManager.getFile(digest, app);
		try {
			if(file!=null) {
				String mime = file.toURL().openConnection().getContentType();
				//String mime = URLConnection.guessContentTypeFromStream(new FileInputStream(file));
				generateAnswerFromFile(file, mime);
			} else {
				generateErrorAnswer(404, "File not found");
			}
		} catch(Exception e) {
			Log.e(LOGTAG, "parseExtendedHTTPFile()", e);
		}
	}
	
	
	private String getHttpParameterValue(String line) {
		return line.substring(line.indexOf(" ")+1);
	}
	
	
	/**
	 * Generates a HTTP response with the content of a file
	 * @param file	The file
	 * @return		true if success, false if an error occurs
	 */
	private boolean generateAnswerFromFile(File file, String mime) {
		try {			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // Output stream to the client
			
			// Generates the HTTP response
			String res = "HTTP/1.1 200 OK\r\n";			
			res += "Content-Type: " + mime + "\r\n";
			res += "Content-Length: "+file.length()+"\r\n";
			res += "Connection: close\r\n";
			res += "\r\n";
			out.writeBytes(res);
			
			// Read data from file and send them to client
			int buffersize = 1024;
	        byte[] buffer = new byte[buffersize];
			FileInputStream tempFileInput = new FileInputStream(file);
			while(tempFileInput.read(buffer)!=-1) {
				out.write(buffer);
			}
			out.flush();
			out.close();
			tempFileInput.close();
			
			return true;
		} catch(Exception e) {
			Log.e(LOGTAG, "generateAnswerFromFile()", e);
			return false;
		}
	}
	
	/**
	 * Generates HTTP error responses
	 * @param code	The error code to be sent
	 */
	private void generateErrorAnswer(int code, String errMsg) {
		String errDescription = "";
		if(code==400) errDescription = "BAD REQUEST";
		else if(code==404) errDescription = "NOT FOUND";
		else if(code==408) errDescription = "REQUEST TIMEOUT";
		
		try {			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // Output stream to the client
			
			String msg = "<html><head><title>Error</title></head><body><h1>Local proxy answer: "+code+" "+errDescription+"</h1><p>"+errMsg+"</p></body></html>";
			
			// Generates the HTTP response
			String res = "HTTP/1.1 "+code+" "+errMsg+"\r\n";			
			res += "Content-Type: text/html\n";
			res += "Content-Length: "+msg.length()+"\r\n";
			res += "Connection: close\r\n";
			res += "\r\n";
			res += msg;
			out.writeBytes(res);
			out.flush();
			out.close();
		} catch(Exception e) {
			Log.e(LOGTAG, "generateErrorAnswer()", e);
		}
	}
	
	
	/**
	 * Sends a HTTP to a host and forwards the response to the client (trasparent proxy)
	 */
	private void simpleHttpGet(String host, int port) {
		try {
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // Output stream to the client
			if(host.equals("")) return;
			Log.d(LOGTAG, "Asking for a page to "+host+":"+port);
			Socket s = new Socket(host, port);
			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream();
			
			PrintWriter pw = new PrintWriter(os);
			for(String r : request) {
				pw.println(r);
			}
			pw.flush();
			
			int read;
			byte[] buffer = new byte[8192];
			while((read=is.read(buffer))!=-1) {
				out.write(buffer, 0, read);
				out.flush();
			}
			is.close();
			os.close();
			s.close();
			out.close();
			Log.d(LOGTAG, "Page from host "+host+" sent to the browser.");
		} catch(Exception e) {
			Log.e(LOGTAG, "simpleHttpGet()", e);
		}
	}
}