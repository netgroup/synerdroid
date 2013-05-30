package it.uniroma2.backgroundfiletransfer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.Calendar;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import android.util.Log;

/**
 * This class manages all the incoming messages.
 * @author Lacra
 * 
 */
public class FileTransferManager implements Runnable {

	private static String LOGTAG="FileTransferManager"; 
	
	private  boolean runningUDP= false;

	private DatagramSocket serverSocketUDP;
	
	private volatile int numThreads=0;
	
	
	/**
	 * Singleton class
	 * @return Instance of the FileTransfer class
	 */
	private static FileTransferManager single_instance=null;
	public static FileTransferManager getInstance()
	{
	 if ( single_instance == null )
		 {
		 	
		 	single_instance = new FileTransferManager();
		 	Log.i(LOGTAG, "FileTransferManager instance created.(Constructor called)");
		 }
	 
	 return single_instance;
	}
	
	private FileTransferManager() {
	}
	
	/**
	 * Start listening on UDP port.
	 */
	@Override
	public void run() {
		
		
		if (MainActivity.currentActivityTV!=null)
			MainActivity.handler.post(new Runnable() {
                public void run() {
                	MainActivity.currentActivityTV.append("\nStart listening on UDP port."); }
            });
		Log.i(LOGTAG, "Start listening on UDP port.");
		
		runningUDP= true;
		start_UDP();
		
	}

	/**
	 * Stop listening on UDP port.
	 */
	public void stopListening()
	{
			runningUDP= false;
			numThreads=0;
			serverSocketUDP.close();
			
			if (MainActivity.currentActivityTV!=null)
				MainActivity.handler.post(new Runnable() {
	                public void run() {
	                	MainActivity.currentActivityTV.append("\nStop listening on UDP port..."); }
	            });
		
			Log.i(LOGTAG, "UDP socket closed.");
		 
	}

	public int getNumThreads()
	{
		Log.i(LOGTAG, "Number of active threads: "+numThreads);
		return numThreads;
	}
		
	
	/**
	 * Manages an incoming TCP connection
	 */
	private  class ServerTCPConnection extends Thread {
		private String LOGTAG = "ServerTCPConnection";
		private Socket socket;
		
		public ServerTCPConnection(Socket socket) {
			this.socket = socket;
			
		}
		
		public void run() {
			try {
				if (MainActivity.currentActivityTV!=null)
					MainActivity.handler.post(new Runnable() {
		                public void run() {
		                	MainActivity.currentActivityTV.append("\nServerTCP: Client connected: "+socket.getRemoteSocketAddress().toString()+"with "+socket.getLocalSocketAddress().toString()); }
		            });
				Log.i(LOGTAG, "ServerTCP: Client connected: "+socket.getRemoteSocketAddress().toString()+"with "+socket.getLocalSocketAddress().toString());
				
				DataInputStream input = new DataInputStream(socket.getInputStream());
				OutputStream output = socket.getOutputStream();
				
				String digest = input.readUTF();//read the digest of the file that needs to be uploaded

				Log.i(LOGTAG, "Digest: "+digest);
				
				File file = null;
				
				if (file==null)
				{
					if (MainActivity.currentActivityTV!=null)
						MainActivity.handler.post(new Runnable() {
			                public void run() {
			                	MainActivity.currentActivityTV.append("\nStart searching in the published files..."); }
			            });
					Log.i(LOGTAG, "Start searching in the published files...");

					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(SharingFileService.context);
					database = dbHelper.getReadableDatabase();
					String sql = "SELECT * FROM "
							+ dbHelper.TABLE_PUBLICATIONS_NAME
							+ " WHERE " + dbHelper.colMD5digest + "='"
							+ digest + "'";
					Cursor cursor = database.rawQuery(sql, null);
				
					cursor.moveToFirst();
					file = new File(cursor.getString(1));
					if (!file.exists())
						{
							//Check if the file still exists on the SD card. If not, it is deleted from the publications table.
							file=null;
							SQLiteDatabase db;
							Database dBHelper;
							dBHelper = new Database(SharingFileService.context);
							db = dBHelper.getWritableDatabase();
							sql = "DELETE FROM " + dBHelper.TABLE_PUBLICATIONS_NAME
									+ " WHERE " + dBHelper.colMD5digest + " = '" + digest + "';";
							db.execSQL(sql);
							db.close();
							dBHelper.close();
							
							final String toTV="\nFile "+cursor.getString(1)+" no longer exists. Deleted from the published files.";
							if (MainActivity.currentActivityTV!=null)
								MainActivity.handler.post(new Runnable() {
					                public void run() {
					                	MainActivity.currentActivityTV.append(toTV); }
					            });
							Log.i(LOGTAG, "File "+cursor.getString(1)+" no longer exists. Deleted from the published files.");
						}
					cursor.close();
					cursor=null;
					database.close();
					dbHelper.close();
				}
				
				if(file!=null) {
					final String toTV="\nUploading the file "+file.getName();
					if (MainActivity.currentActivityTV!=null)
						MainActivity.handler.post(new Runnable() {
			                public void run() {
			                	MainActivity.currentActivityTV.append(toTV); }
			            });
					Log.i(LOGTAG, "Uploading the file "+ file.getName());
					FileInputStream fileInput = new FileInputStream(file);
					
					int read;
					byte[] buffer = new byte[8192];
					while((read=fileInput.read(buffer))!=-1) {
						output.write(buffer, 0, read);// write data back in the opened socket
						output.flush();
					}
					if (MainActivity.currentActivityTV!=null)
						MainActivity.handler.post(new Runnable() {
			                public void run() {
			                	MainActivity.currentActivityTV.append("\nUpload finished."); }
			            });
				}
				
				input.close();
				output.close();
				
			} catch(Exception e) {
				Log.e(LOGTAG, "runningTCP", e);
			} finally {
				try {
					socket.close();
					numThreads--;
					Log.i(LOGTAG, "Num of active threads decremented:"+numThreads);
				} catch(Exception e) {}
			}
		}
	}

	
	/**
	 * Manages the incoming broadcasts from the other devices
	 */
	private void start_UDP()
	{
		Log.i(LOGTAG, "Starting UDP");
		try {
				serverSocketUDP = new DatagramSocket(5000);
			}
		catch (Exception e) {
			if (MainActivity.currentActivityTV!=null)
				MainActivity.handler.post(new Runnable() {
	                public void run() {
	                	MainActivity.currentActivityTV.append("\nException opening DatagramSocket UDP"); }
	            });
			Log.i(LOGTAG, "Exception opening DatagramSocket UDP");
		}
		
	    final byte[] receiveData = new byte[1024];
		
		
		while(runningUDP) {
			try {
			
			
			if (MainActivity.currentActivityTV!=null)
				MainActivity.handler.post(new Runnable() {
	                public void run() {
	                	MainActivity.currentActivityTV.append("\n\nWaiting for Broadcast request in ServerUDP"); }
	            });
			
			Log.d(LOGTAG, "Waiting for Broadcast request in ServerUDP.");
			
			final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocketUDP.receive(receivePacket);//it blocks until a packet is received
			
			new Thread (new Runnable() {
				
				@Override
				public void run() {
					
					byte[] sendData = new byte[1024];
					InetAddress address = receivePacket.getAddress();
					int port = receivePacket.getPort();
					
					if(!receivePacket.getAddress().getHostAddress().equals(Utils.getLocalIpAddress())) {
						
					
					String req = new String(receivePacket.getData(), 0, receivePacket.getLength());
					final String toTV="\nReceived UDP message: "+req+" from: "+receivePacket.getAddress().getHostAddress();
					if (MainActivity.currentActivityTV!=null)
						MainActivity.handler.post(new Runnable() {
			                public void run() {
			                	MainActivity.currentActivityTV.append(toTV); }
			            });
					Log.d(LOGTAG, "Received UDP message: "+req+" from: "+receivePacket.getAddress().getHostAddress());
					
					String res = manageRequest(req); 

					if (res!=null)
					{ 
						try {
						numThreads++;
						Log.i(LOGTAG, "Num of active threads incremented:"+numThreads);
						final ServerSocket serverSocket= new ServerSocket(0);
						serverSocket.getLocalPort();
						
						Log.i(LOGTAG, "IP : "+Utils.getLocalIpAddress()+" port :"+serverSocket.getLocalPort());
						
						res+=";;"+serverSocket.getLocalPort();
						final String toTV1="\nSend res: "+res+" to "+address+":"+port;
						if (MainActivity.currentActivityTV!=null)
							MainActivity.handler.post(new Runnable() {
				                public void run() {
				                	MainActivity.currentActivityTV.append(toTV1); }
				            });
						Log.d(LOGTAG, "Send res: "+res+" to "+address+":"+port);
						
						sendData = res.getBytes();
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, 7000);//sending res on port 7000 where I know it is listening
						
						serverSocketUDP.send(sendPacket);
						
						
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									serverSocket.setSoTimeout(2000);//lets the other device a 2 seconds timeout to connect and transfer otherwise it closes the socket
									Socket socket=serverSocket.accept();
									new ServerTCPConnection(socket).start();
									
								}catch (SocketTimeoutException e){
									try {
										serverSocket.close();
										numThreads--;
										Log.i(LOGTAG, "Num of active threads decremented:"+numThreads);
										e.printStackTrace();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
								} 
								catch (IOException e) {
									numThreads--;
									Log.i(LOGTAG, "Num of active threads decremented:"+numThreads);
								} 
								
								
							}
						}).start();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
						
						
					}
					
				}
				}
			}).run();
			
			
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
    	
		
	}
	
	
	
	/**
	 * Manages a request - Builds the response back
	 */
	private synchronized String manageRequest(String req)
	{
		String res=null;

		String[] parts = req.split(";;");
		Log.i(LOGTAG, "Type of query:"+ parts[0]+"---");
		if(!parts[0].equals("REQ")) 
				return null;
		
		if(parts[1].equals("SUB")) 
		{
				Log.i(LOGTAG, "Query request arrived");
				if(parts.length<5)
				{
					Log.d(LOGTAG, "Query not valid");
					res= null;
				}
				String query = parts[3];
				if (query.length()>1)
				{String digest = newQuery(query);//checks if the device has files that match the query(subscription)
				if(digest==null) 
					return null;
				
				res= "RES;;SUB;;"+parts[2]+";;"+digest+";;"+Utils.getLocalIpAddress();}
		} 
		else 
			 if (parts[1].equals("GET")) {
						String[] digestsAndKeys = parts[2].split(",");
						String haveFiles = "";
						res = null;
						for (String digestAndKey : digestsAndKeys)
							if (digestAndKey.length() > 1) {
									{
										String[] digest=digestAndKey.split(":");//the key is the first elem and the second is the digest
										try {
											Log.i(LOGTAG,
													"Verifing if the file exists in the shared files.");
											
											if (MainActivity.currentActivityTV!=null)
												MainActivity.handler.post(new Runnable() {
									                public void run() {
									                	MainActivity.currentActivityTV.append("\nVerifing if the file exists in the shared files."); }
									            });
											SQLiteDatabase database;
											Database dbHelper;
											dbHelper = new Database(SharingFileService.context);
											database = dbHelper.getReadableDatabase();
											String sqll = "SELECT COUNT(*) FROM "
													+ dbHelper.TABLE_PUBLICATIONS_NAME
													+ " WHERE " + dbHelper.colMD5digest + "='"
													+ digest[1] + "'";
											SQLiteStatement statement = database
													.compileStatement(sqll);
											long count = statement.simpleQueryForLong();
											statement.close();
											database.close();
											dbHelper.close();
											if (count!=0)
												haveFiles += digest[0]+":"+digest[1] + ",";
										} catch (Exception e) {
										}
									}
							}
						if (!haveFiles.equals(""))
							res = "RES;;GET;;" +haveFiles  + ";;"+ Utils.getLocalIpAddress();
					}
			
				if(res==null) 
				{
					if (MainActivity.currentActivityTV!=null)
						MainActivity.handler.post(new Runnable() {
			                public void run() {
			                	MainActivity.currentActivityTV.append( "\nRequested files NOT found in cache neither in the published files."); }
			            });
					Log.d(LOGTAG, "Requested files NOT found in cache neither in the published files.");
					return null;
				}
				
			Log.i(LOGTAG, "Have some requested files: "+res);
			return res;
	}
	
	
	/**
	 * Generates the response to a new query. Returns the digest of the response file put in cache.
	 * @param query
	 * @return md5digest
	 */
	public synchronized String newQuery(String query) {
		try {
			SQLiteDatabase database;
			Database dbHelper;
			dbHelper = new Database(SharingFileService.context);
			
			String res="";
			String[] query_split=query.split(",");
			
			String list="";
			for (int i=0;i<query_split.length-1;i++)
			{
				list +=  " LIKE '%"+query_split[i]+"%' OR "+dbHelper.colMetadata + " COLLATE NOCASE";				
			}
			list+=" LIKE '%"+query_split[query_split.length-1]+"%'";

			
			database = dbHelper.getReadableDatabase();
			String sqll = "SELECT * FROM "
					+ dbHelper.TABLE_PUBLICATIONS_NAME
					+ " WHERE " + dbHelper.colMetadata +" COLLATE NOCASE"+ list;
			Cursor cursor=database.rawQuery(sqll, null);
			
			
			cursor.moveToFirst();
			while (!cursor.isAfterLast())
			{
				long expirationTime=Long.parseLong(cursor.getString(3))+Long.parseLong(cursor.getString(4));
				if (expirationTime>Calendar.getInstance().getTimeInMillis())
				{
					File file=new File(cursor.getString(1));
					res+=cursor.getString(0)+"||"+file.getName()+"**";
				}
				else
				{
					//Delete the entry from the publications because its time has expired.
					SQLiteDatabase db;
					Database databHelper;
					databHelper = new Database(SharingFileService.context);
					db = databHelper.getWritableDatabase();
					String sql = "DELETE FROM " + databHelper.TABLE_PUBLICATIONS_NAME
							+ " WHERE " + databHelper.colMD5digest + " = '" + cursor.getString(0) + "';";
					db.execSQL(sql);
					db.close();
					databHelper.close();
					
					final String toTV="\nThe file "+ cursor.getString(1)+" was deleted from the database. Available time has expired.";
					if (MainActivity.currentActivityTV!=null)
						MainActivity.handler.post(new Runnable() {
			                public void run() {
			                	MainActivity.currentActivityTV.append( toTV); }
			            });
					Log.i(LOGTAG, "The file "+ cursor.getString(1)+" was deleted from the database. Available time has expired.");
					
				}
				cursor.moveToNext();
			}
			cursor.close();
			cursor=null;
			database.close();
			dbHelper.close();
			Log.i(LOGTAG, "Response to newQuery: "+res);
			
			
			if(res.equals("")) {
				Log.i(LOGTAG, "Null response.");
				return null;
			}
			
			if(res.contains("\r") || res.contains("\n") || res.contains(";;")) {
				Log.i(LOGTAG, "Query response contains illegal characters. This response will be discarded.");
				return null;
			}

			File file = File.createTempFile("cache", "", SharingFileService.context.getExternalCacheDir());
			FileWriter writer = new FileWriter(file);
			writer.write(res);
			writer.flush();
			writer.close();
			
			
			//PUT FILE IN THE PUBLICATIONS TABLE
			String md5digest=getMD5Digest(file);
			
			dbHelper = new Database(SharingFileService.context);
			
			database = dbHelper.getReadableDatabase();

			String sql = "SELECT COUNT(*) FROM "
					+ dbHelper.TABLE_PUBLICATIONS_NAME + " WHERE "
					+ dbHelper.colMD5digest + " = '" + md5digest + "'";
			SQLiteStatement statement = database.compileStatement(sql);
			long numRows = statement.simpleQueryForLong();
			statement.close();
			if (numRows==0)
			{database = dbHelper.getWritableDatabase();
			sql = "INSERT or replace INTO "
					+ dbHelper.TABLE_PUBLICATIONS_NAME + " ("
					+ dbHelper.colMD5digest + ", " + dbHelper.colFileLocation
					+ ", "+dbHelper.colMetadata+","+dbHelper.colInsertTime+"," + dbHelper.colTimeAvailable +") " 
					+ "VALUES('"
					+ md5digest +"','" +file.getPath() + "','" + "" + "','" 
					+ Calendar.getInstance().getTimeInMillis()+"','600000')"; //puts it in the publications files for 10 minutes
			database.execSQL(sql);
			database.close();
			dbHelper.close();}
			else
				{
					database.close();
					dbHelper.close();
					file.delete();
					Log.i(LOGTAG, "This request was previously received and processed. The same result was generated.");
					
				}
			
			
			return md5digest;
			
		} catch(Exception e) {
			Log.e(LOGTAG, "newQuery()", e);
			return null;
		}
	} 
	
	/**
	 * Calculates the MD5 digest of a file
	 * @param file	The input file
	 * @return		The MD5 digest
	 */
	private String getMD5Digest(File file) {
		try {
			String md5 = "";
			InputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			MessageDigest complete = MessageDigest.getInstance("MD5");
			int numRead;
			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
			fis.close();
			byte[] b = complete.digest();
			for (int i = 0; i < b.length; i++) {
				md5 += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
			}
			return md5;
		} catch(Exception e) {
			return null;
		}
	}
}
