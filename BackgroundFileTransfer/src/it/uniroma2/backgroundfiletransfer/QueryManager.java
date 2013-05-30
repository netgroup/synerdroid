package it.uniroma2.backgroundfiletransfer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This class sends into the network the registered queries
 * 
 * @author Lacra
 * 
 */

public class QueryManager implements Runnable {
	
	private class KEY{
			@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((IP == null) ? 0 : IP.hashCode());
			result = prime * result
					+ ((queryKey == null) ? 0 : queryKey.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KEY other = (KEY) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (IP == null) {
				if (other.IP != null)
					return false;
			} else if (!IP.equals(other.IP))
				return false;
			if (queryKey == null) {
				if (other.queryKey != null)
					return false;
			} else if (!queryKey.equals(other.queryKey))
				return false;
			return true;
		}
			String IP;
			String queryKey;
			KEY(String ip, String key)
			{
				IP=ip;
				queryKey=key;
			}
			private QueryManager getOuterType() {
				return QueryManager.this;
			}
			
			}
	
	
	private QueryManager getOuterType() {
				return QueryManager.this;
			}
			
		
	Hashtable <KEY,String> discoveredDevices;

	private static String LOGTAG = "QueryManager";
	private static QueryManager single_instance = null;
	
	private DatagramSocket clientSocket;

	
	
	public static QueryManager getInstance() {
		if (single_instance == null)
			single_instance = new QueryManager();
		return single_instance;
	}

	private QueryManager() {

		try {
			clientSocket = new DatagramSocket(7000);

			discoveredDevices = new Hashtable<QueryManager.KEY, String>();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		
		startSearching();

	}

	
	/**
	 * Sends broadcast for the download request
	 * 
	 * @param 	  digest
	 *            The files digests concatenated with ","
	 * @return
	 */
	private void downloadFiles(String digests) {

		if (!digests.equals("")) {
			
			final String toTV1= "\nSearching devices nearby for the files with the digests :"
					+ digests;
			if (MainActivity.currentActivityTV!=null)
				MainActivity.handler.post(new Runnable() {
	                public void run() {
	                	MainActivity.currentActivityTV.append(toTV1); }
	            });
			
			Log.d(LOGTAG,
					"Searching devices nearby for the files with the digests :"
							+ digests);
			
			sendUDPMessage("REQ;;GET;;"+ digests + ";;" + Utils.getLocalIpAddress());
	
		}

	}

	/**
	 * Sends broadcast with the query-request to the other devices
	 */
	private void getSubscriptionResponse(String key,String metaData) {
		Log.d(LOGTAG, "REQ;;SUB;;"+key+";;"
				+ metaData + ";;" + Utils.getLocalIpAddress());

		sendUDPMessage("REQ;;SUB;;"+key+";;"
					+ metaData + ";;" + Utils.getLocalIpAddress());

	}

	/**
	 * Sends broadcast 
	 * Port 5000 (UDP)
	 */
	private void sendUDPMessage(String msg) {
		
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(7000); // Milliseconds
			clientSocket.setBroadcast(true);
			InetAddress address = InetAddress.getByName(Utils
					.getBroadcastAddress());

			byte[] sendData;
			
			sendData = msg.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, address, 5000);
			clientSocket.send(sendPacket);
			
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * If canStop returns true then queries are finished - can stop wifi.
	 * If canStop returns false then queries are not finished - can't stop wifi.
	 */
	public boolean canStop()
	{
		return !runListenForResponses;	
	}
	
	
	private boolean runListenForResponses=false;
	
	private void startSearching() {

		runListenForResponses=true;
		new Thread(listenForResponses).start();
		
		if (MainActivity.currentActivityTV != null)
			MainActivity.handler.post(new Runnable() {
				public void run() {
					MainActivity.currentActivityTV
							.append("\nPlatform sending queries into the network.");
				}
			});
		Log.i(LOGTAG, "Queries have begun");

		SQLiteDatabase database;
		Database dbHelper;
		dbHelper = new Database(SharingFileService.context);
		database = dbHelper.getReadableDatabase();
		
		String sqlGets= "SELECT * FROM "+dbHelper.TABLE_QUERIES_NAME + " WHERE "+dbHelper.colGetFlag+" = 1 OR "+dbHelper.colGetFlag+" = 2";
		Cursor cursor=database.rawQuery(sqlGets, null);
		cursor.moveToFirst();
		String digestsMessage="";
		while (!cursor.isAfterLast())
		{
			//check expiration time
			long time = Long.parseLong(cursor.getString(4))
					+ Long.parseLong(cursor.getString(5)) * 1000; 
			if (time > Calendar.getInstance().getTimeInMillis()) //check if the query has expired
				digestsMessage+=cursor.getString(0)+"~"+cursor.getInt(1)+":"+cursor.getString(3)+",";
			else
				{deleteRow(cursor.getString(0));
				Log.i(LOGTAG, "Digest: "+cursor.getString(3)+" was deleted. Time expired.");
				}
			cursor.moveToNext();
		}

		 
		cursor.close();
		cursor=null;

		String[] allColumns = { dbHelper.colKey, dbHelper.colGetFlag,
				dbHelper.colMetadataFlag, dbHelper.colQuery,
				dbHelper.colInsertTime, dbHelper.colExpirationTime,
				dbHelper.colLastRequested, dbHelper.colONTime, dbHelper.colOFFTime };

		cursor = database.query(dbHelper.TABLE_QUERIES_NAME, allColumns,
				null, null, null, null, null);
	
	
		for (int i = 0; i < 5; i++) {
			Log.e(LOGTAG, "Session of sending reqs number "+(i+1));
			downloadFiles(digestsMessage);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {

				//check expiration time
				long time = Long.parseLong(cursor.getString(4))
						+ Long.parseLong(cursor.getString(5)) * 1000; 
				if (time > Calendar.getInstance().getTimeInMillis()) {

					database = dbHelper.getWritableDatabase();

					// Check if it is a download request - (a GET)
					if (cursor.getInt(1) == 1 || cursor.getInt(1)==2) {
						// Updates the LastRequested field in the table if it is the last sending in the ONtime
						if (i==4)
						{String strSQL = "UPDATE " + dbHelper.TABLE_QUERIES_NAME
								+ " SET " + dbHelper.colLastRequested + " = "
								+ Calendar.getInstance().getTimeInMillis()
								+ " WHERE " + dbHelper.colKey + " = '"
								+ cursor.getString(0) + "'";
						database.execSQL(strSQL);}

					} else if (cursor.getInt(2) == 1||cursor.getInt(2)==2) {// Check if it is a
														// metadata
														// request (a search)

						//CHECKS IF THE TIME BETWEEN THE LAST TIME + THE OFF TIME EXCEEDS THE CURRENT TIME (IF YES THEN IT MUST STILL WAIT)
						long timeLastRequested=Long.parseLong(cursor.getString(6));
						long timeOut=Long.parseLong(cursor.getString(8))*1000;
						//Log.i(LOGTAG, "Time Last Requested :"+timeLastRequested+" Timout: "+timeOut+" Current time :"+Calendar.getInstance().getTimeInMillis());
						if (timeLastRequested + timeOut <= Calendar
								.getInstance().getTimeInMillis()) {
							getSubscriptionResponse(cursor.getString(0)+","+cursor.getInt(2),
									cursor.getString(3));

							// Updates the LastRequested field in the table
							if (i == 4) {
								String strSQL = "UPDATE "
										+ dbHelper.TABLE_QUERIES_NAME
										+ " SET "
										+ dbHelper.colLastRequested
										+ " = "
										+ Calendar.getInstance()
												.getTimeInMillis() + " WHERE "
										+ dbHelper.colKey + " = '"
										+ cursor.getString(0) + "'";
								database.execSQL(strSQL);
							}
						}

					}
				

				} else
					deleteRow(cursor.getString(0));// delete the query from the
													// queries table - the time
													// has expired
				cursor.moveToNext();
			}
			try {
				Thread.sleep(1000); //Wait a second before the next session of sending req
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		cursor.close();
		cursor=null;
		database.close();

		runListenForResponses=false; //Stop listening for responses because I finished making queries
		
		// THE BELOW IS DONE JUST FOR SOME PRINTS
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String sqll = "SELECT COUNT(*) FROM " + dbHelper.TABLE_RESULTS_NAME;
		SQLiteStatement statement = db.compileStatement(sqll);
		long count = statement.simpleQueryForLong();
		statement.close();

		dbHelper.close();
		final String toTV = "\nResults list has " + count + " objects";
		if (MainActivity.currentStatusTV != null)
			MainActivity.handler.post(new Runnable() {
				public void run() {
					MainActivity.currentStatusTV.append(toTV);
				}
			});
		Log.i(LOGTAG, "Results list has " + count);

		if (MainActivity.currentActivityTV != null)
			MainActivity.handler.post(new Runnable() {
				public void run() {
					MainActivity.currentActivityTV
							.append("\nPlatform finished sending queries.");
				}
			});
		Log.i(LOGTAG, "Queries finished");
	}

	private void deleteRow(String key) {
		SQLiteDatabase database;
		Database dbHelper;
		dbHelper = new Database(SharingFileService.context);
		database = dbHelper.getWritableDatabase();
		String sql = "DELETE FROM " + dbHelper.TABLE_QUERIES_NAME + " WHERE "
				+ dbHelper.colKey + " = '" + key + "';";
		database.execSQL(sql);
		dbHelper.close();
		Log.i(LOGTAG, "Delete row with key=" + key);
	}

	private Runnable listenForResponses = new Runnable() {

		@Override
		public void run() {
			Log.i(LOGTAG, "Started listening for answers!");
			byte[] receiveData = new byte[1024];

			try {

				
				while (runListenForResponses) {

					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					clientSocket.receive(receivePacket);

					if (MainActivity.currentActivityTV != null)
						MainActivity.handler.post(new Runnable() {
							public void run() {
								MainActivity.currentActivityTV
										.append("\nResponse received!");
							}
						});

					final String res = new String(receivePacket.getData(), 0,
							receivePacket.getLength());
					

					new Thread(new Runnable() {

						@Override
						public void run() {

							String IPdeviceFound = null;
							String metadataDigest = null;
							String portDeviceFound = null;

							if (res != null && res.startsWith("RES;;SUB")) {
								String ID = null;
								boolean canContinue = false;
								Log.i(LOGTAG, "Received response: " + res);
								String parts[] = res.split(";;");
								ID = parts[2];
								metadataDigest = parts[3];
								IPdeviceFound = parts[4];
								portDeviceFound = parts[5];

								//VERIFY IF I HAVE IT IN MY DISCOVERED DEVICES
								
								if (discoveredDevices.containsKey(new KEY(
										IPdeviceFound, ID))) {
									long timestamp = Long
											.parseLong(discoveredDevices
													.get(new KEY(IPdeviceFound,
															ID)));
									if (timestamp < Calendar.getInstance()
											.getTimeInMillis()) {
										canContinue = true;
									} else
										{
											canContinue = false;
											Log.i(LOGTAG, "Response already received from previous sends. Ignore this one!");
										}
								} else
									canContinue = true;
								
								//IF IT CAN CONTINUE, THEN IT DOWNLOADS THE DATA THAT IT RECEIVED
								if (canContinue == true) {
									String[] typeOfSub=ID.split(",");
									if (typeOfSub[1].equals("1")){
									if (IPdeviceFound != null) {
										discoveredDevices.put(new KEY(IPdeviceFound, ID), ""+(Calendar.getInstance().getTimeInMillis()+1800000));
										final String toTV = "\nDevice found :"
												+ IPdeviceFound;
										if (MainActivity.currentActivityTV != null)
											MainActivity.handler
													.post(new Runnable() {
														public void run() {
															MainActivity.currentActivityTV
																	.append(toTV);
														}
													});
										Log.i(LOGTAG, "Device found :"
												+ IPdeviceFound);
										final String toTV1 = "\nStarting download from: "
												+ IPdeviceFound
												+ "/"
												+ portDeviceFound;
										if (MainActivity.currentActivityTV != null)
											MainActivity.handler
													.post(new Runnable() {
														public void run() {
															MainActivity.currentActivityTV
																	.append(toTV1);
														}
													});
										Log.i(LOGTAG,
												"Starting download from: "
														+ IPdeviceFound + "/"
														+ portDeviceFound);
										try {
											File file = File
													.createTempFile(
															"cache",
															"",
															SharingFileService.context
																	.getExternalCacheDir());
											FileOutputStream fileOS = new FileOutputStream(
													file);

											Socket socket = new Socket(
													IPdeviceFound,
													Integer.parseInt(portDeviceFound));
											DataOutputStream output = new DataOutputStream(
													socket.getOutputStream());
											output.writeUTF(metadataDigest);

											InputStream input = socket
													.getInputStream();

											int read;
											byte[] buffer = new byte[8192];
											while ((read = input.read(buffer)) != -1) {
												fileOS.write(buffer, 0, read);
												fileOS.flush();
											}
											final String toTV2 = "\nFinished downloading the file from "
													+ IPdeviceFound
													+ "/"
													+ portDeviceFound;
											if (MainActivity.currentActivityTV != null)
												MainActivity.handler
														.post(new Runnable() {
															public void run() {
																MainActivity.currentActivityTV
																		.append(toTV2);
															}
														});
											Log.i(LOGTAG,
													"Finished downloading the file from "
															+ IPdeviceFound
															+ "/"
															+ portDeviceFound);
											input.close();
											output.close();
											fileOS.close();
											socket.close();

											//INSERT THE RESULT INTO THE DATABASE TABLE OF RESULTS
											SQLiteDatabase database;
											Database dbHelper;
											dbHelper = new Database(SharingFileService.context);
											database = dbHelper.getWritableDatabase();
											String sql = "INSERT or replace INTO "
													+ dbHelper.TABLE_RESULTS_NAME + " ("
													+ dbHelper.colKey + ", "
													+ dbHelper.colFileLocation + ") "
													+ "VALUES('" + typeOfSub[0] + "','" + file.getPath()
													+ "')";
											database.execSQL(sql);
											dbHelper.close();
											String fileContent = "";

											try {
												BufferedReader reader = new BufferedReader(
														new FileReader(file));
												fileContent = reader.readLine();
												reader.close();
												Log.i(LOGTAG, "File Content: "
														+ fileContent);
												if (fileContent == "") {
													Log.d(LOGTAG,
															"Nothing to send. No file content.");
												}
											} catch (Exception e) {

											}
										} catch (Exception e) {
											Log.e(LOGTAG, "getFileFromWifi()",
													e);

										}
									}
								}//typeOfSub
									else
										if (typeOfSub[1].equals("2"))
										{
											if (IPdeviceFound != null) {
												discoveredDevices.put(new KEY(IPdeviceFound, ID), ""+(Calendar.getInstance().getTimeInMillis()+1800000));
												
												Log.i(LOGTAG, "Device found :"
														+ IPdeviceFound);
												final String toTV1 = "\nStarting download from: "
														+ IPdeviceFound
														+ "/"
														+ portDeviceFound;
												if (MainActivity.currentActivityTV != null)
													MainActivity.handler
															.post(new Runnable() {
																public void run() {
																	MainActivity.currentActivityTV
																			.append(toTV1);
																}
															});
												Log.i(LOGTAG,
														"Starting download from: "
																+ IPdeviceFound + "/"
																+ portDeviceFound);
												try {
													File file = File
															.createTempFile(
																	"cache",
																	"",
																	SharingFileService.context
																			.getExternalCacheDir());
													FileOutputStream fileOS = new FileOutputStream(
															file);

													Socket socket = new Socket(
															IPdeviceFound,
															Integer.parseInt(portDeviceFound));
													DataOutputStream output = new DataOutputStream(
															socket.getOutputStream());
													output.writeUTF(metadataDigest);

													InputStream input = socket
															.getInputStream();

													int read;
													byte[] buffer = new byte[8192];
													while ((read = input.read(buffer)) != -1) {
														fileOS.write(buffer, 0, read);
														fileOS.flush();
													}
													final String toTV2 = "\nFinished downloading the file from "
															+ IPdeviceFound
															+ "/"
															+ portDeviceFound;
													if (MainActivity.currentActivityTV != null)
														MainActivity.handler
																.post(new Runnable() {
																	public void run() {
																		MainActivity.currentActivityTV
																				.append(toTV2);
																	}
																});
													Log.i(LOGTAG,
															"Finished downloading the file from "
																	+ IPdeviceFound
																	+ "/"
																	+ portDeviceFound);
													input.close();
													output.close();
													fileOS.close();
													socket.close();
													
													
													//PUT THE FILES FOUND IN THE QUERIES TABLE IN ORDER TO DOWNLOAD THEM
												
														ArrayList<String> linesFromFile = new ArrayList<String>();

														FileReader fr=new FileReader(file);
														BufferedReader br = new BufferedReader(fr);

														//Read File Line By Line
														String line = "";
														while ((line = br.readLine()) != null) {
															String[] filesArrived = line.split("\\*\\*");
															for (String fileArrived : filesArrived) {
																linesFromFile.add(fileArrived);
																Log.i(LOGTAG, fileArrived);
																
																String[] fields = fileArrived.split("\\|\\|");
																
																String md5digest = fields[0];
																String fileName = fields[1];

																SQLiteDatabase database;
																Database dbHelper;
																dbHelper = new Database(SharingFileService.context);
																database = dbHelper.getReadableDatabase();
			
																String key = SharingFileService.RandomAlphaNumericString(3);
																String sql = "SELECT COUNT(*) FROM "
																		+ dbHelper.TABLE_QUERIES_NAME + " WHERE "
																		+ dbHelper.colKey + " = '" + key + "'";
																SQLiteStatement statement = database.compileStatement(sql);
																long numRows = statement.simpleQueryForLong();
			
																while (numRows > 0) {
																	key = SharingFileService.RandomAlphaNumericString(3);
																	statement = database.compileStatement(sql);
																	numRows = statement.simpleQueryForLong();
																}
																statement.close(); 
																dbHelper.close();
			
																database = dbHelper.getWritableDatabase();
																sql = "INSERT or replace INTO "
																		+ dbHelper.TABLE_QUERIES_NAME + " ("
																		+ dbHelper.colKey + ", " + dbHelper.colGetFlag
																		+ ", " + dbHelper.colMetadataFlag + ", "
																		+ dbHelper.colQuery + ", " + dbHelper.colInsertTime
																		+ ", " + dbHelper.colExpirationTime + ", "
																		+ dbHelper.colLastRequested + ", "
																		+ dbHelper.colONTime +","
																		+ dbHelper.colOFFTime +") " + "VALUES('"
																		+ key + "','2','0','" + md5digest + "','"
																		+ Calendar.getInstance().getTimeInMillis() + "','"
																		+ "172800" + "','" + (Calendar.getInstance().getTimeInMillis()-5000) + "','"					
																		+ "5000" + "','"+"5000"+"')";
																database.execSQL(sql);
																sql= "INSERT or replace INTO "
																		+dbHelper.TABLE_AUTO_DOWNLOAD+" ( "
																		+dbHelper.colKey +","+dbHelper.colQueryKey+", "+ dbHelper.colFileName+", "+dbHelper.colFileLocation
																		+") VALUES ('"+key+"', '"+typeOfSub[0]+"', '"+fileName+"','nothing')";
																database.execSQL(sql);	
																database.close();
																database=null;
																dbHelper.close();
															}
														}
														//Close the input stream
														fr.close();
														br.close();
													//FINISHED PUTING IN THE QUERIES TABLE THE DOWNLOAD FILES
													
													String fileContent = "";

													try {
														BufferedReader reader = new BufferedReader(
																new FileReader(file));
														fileContent = reader.readLine();
														reader.close();
														Log.i(LOGTAG, "File Content autoDownload: "
																+ fileContent);
														if (fileContent == "") {
															Log.d(LOGTAG,
																	"Nothing to send. No file content.");
														}
													} catch (Exception e) {

													}
												} catch (Exception e) {
													Log.e(LOGTAG, "getFileFromWifi()",
															e);

												}
											}
										}//typeOfSub
								}

							}
							else
								if (res != null && res.startsWith("RES;;GET")) {
									Log.i(LOGTAG, "GET response received");
									boolean canContinue = false;
									
									String [] parts = res.split(";;");
									IPdeviceFound = parts[3];
									metadataDigest = parts[2];
									portDeviceFound = parts[4];

									if (IPdeviceFound == null || portDeviceFound == null) {
											Log.i(LOGTAG, "IP or ports null from the device that responded");
									} else {
												final String toTV= "\nDevice found :" + IPdeviceFound + "/"
														+ portDeviceFound + " with files: "
														+ metadataDigest;
												if (MainActivity.currentActivityTV!=null)
														MainActivity.handler.post(new Runnable() {
																public void run() {
																		MainActivity.currentActivityTV.append(toTV); }
																});
												Log.i(LOGTAG, "Device found :" + IPdeviceFound + "/"
														+ portDeviceFound + " with files: "
														+ metadataDigest);

								
												String[] digestsFound = metadataDigest.split(",");
												Log.i(LOGTAG, "Digests found="+digestsFound.length);
												
												for (String digestAndKey : digestsFound)
													if (!digestAndKey.equals("")) {
															String[] digest=digestAndKey.split(":");
															String[] typeOfget=digest[0].split("~");
	
															if (discoveredDevices.containsKey(new KEY(IPdeviceFound, typeOfget[0]))) {
																
																long timestamp = Long.parseLong(discoveredDevices.get(new KEY(IPdeviceFound, typeOfget[0])));
																if (timestamp < Calendar.getInstance().getTimeInMillis()) {
																	canContinue = true;
																} else
																		{
																		canContinue = false;
																		Log.i(LOGTAG, "Response already received from previous sends. Ignore this one!");
																		}
															} else
																	canContinue = true;
															
															if (canContinue==true)
															{
																if (typeOfget[1].equals("1")){
																discoveredDevices.put(new KEY(IPdeviceFound, typeOfget[0]), ""+(Calendar.getInstance().getTimeInMillis()+50000));//ignore responses for 5 minutes - we do not want to download the same file multiple times
																	///DOWNLOADING FILE
																	final String toTV2="\nStarting download from: " + IPdeviceFound + "/" + portDeviceFound;
																	if (MainActivity.currentActivityTV!=null)
																			MainActivity.handler.post(new Runnable() {
																					public void run() {
																						MainActivity.currentActivityTV.append(toTV2); }
																				});
																	Log.i(LOGTAG, "Starting download from: " + IPdeviceFound + "/" + portDeviceFound);
																	 
																	
																		try {
																			File	file = File.createTempFile("cache", "", SharingFileService.context.getExternalCacheDir());
																		
																		FileOutputStream fileOS = new FileOutputStream(file);

																		Socket socket = new Socket(IPdeviceFound, Integer.parseInt(portDeviceFound));
																		DataOutputStream output = new DataOutputStream(
																				socket.getOutputStream());
																		output.writeUTF(digest[1]);

																		InputStream input = socket.getInputStream();

																		int read;
																		byte[] buffer = new byte[8192];
																		while ((read = input.read(buffer)) != -1) {
																			fileOS.write(buffer, 0, read);
																			fileOS.flush();
																		}
																		
																		final String toTV1= "\nFinished downloading the file("+file.getPath()+") from " + IPdeviceFound+ "/" + portDeviceFound;
																		if (MainActivity.currentActivityTV!=null)
																			MainActivity.handler.post(new Runnable() {
																				public void run() {
																					MainActivity.currentActivityTV.append(toTV1); }
																			});
																		Log.i(LOGTAG, "Finished downloading the file from " + IPdeviceFound+ "/" + portDeviceFound);
																		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
																		NotificationCompat.Builder builder =  
																		        new NotificationCompat.Builder(SharingFileService.context)  
																		        .setSmallIcon(R.drawable.ic_launcher)  
																		        .setContentTitle("Download finished") 
																		        .setSound(soundUri)
																		        .setContentText("Finished downloading the file from " + IPdeviceFound+ "/" + portDeviceFound);  
																		
																		Intent notificationIntent = new Intent(SharingFileService.context, MainActivity.class);  

															            PendingIntent contentIntent = PendingIntent.getActivity(SharingFileService.context, 0, notificationIntent,   
															                    PendingIntent.FLAG_UPDATE_CURRENT);  

															            builder.setContentIntent(contentIntent);  
															            builder.setAutoCancel(true);
															            builder.setLights(Color.BLUE, 500, 500);
															            long[] pattern = {500,500,500,500,500,500,500,500,500};
															            builder.setVibrate(pattern);
															            builder.setStyle(new NotificationCompat.InboxStyle());
															             Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
															                if(alarmSound == null){
															                    alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
															                    if(alarmSound == null){
															                        alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
															                    }
															                }

															            // Add as notification  
															            NotificationManager manager = (NotificationManager) SharingFileService.context.getSystemService(Context.NOTIFICATION_SERVICE);  
															            builder.setSound(alarmSound);
															            manager.notify(1, builder.build());  
																		
																		//FINISHED DOWNLOADING FILE
																		
																		//PUT THE RESULT IN THE RESULTS DATABASE TABLE
																		if (file != null) {
																			
																			SQLiteDatabase database;
																			Database dbHelper;
																			dbHelper = new Database(
																					SharingFileService.context);
																			database = dbHelper.getReadableDatabase();

																			// Count how many requests are for this digest
																			String sqll = "SELECT COUNT(*) FROM "
																					+ dbHelper.TABLE_QUERIES_NAME
																					+ " WHERE " + dbHelper.colQuery + "='"
																					+ digest[1] + "'";
																			SQLiteStatement statement = database
																					.compileStatement(sqll);
																			long count = statement.simpleQueryForLong();
																			statement.close();
																			statement=null;
																			Log.i(LOGTAG, "There are "+count+" requests for the file");
																			
																			String sql = "SELECT * FROM "
																					+ dbHelper.TABLE_QUERIES_NAME
																					+ " WHERE " + dbHelper.colQuery + "='"
																					+ digest[1] + "'";
																			Cursor cursor = database.rawQuery(sql, null);
																			
																			String[] keys = new String[(int) count];
																			int i = 0;
																			cursor.moveToFirst();
																			while (!cursor.isAfterLast()) {
																				keys[i] = cursor.getString(0);
																				i++;
																				cursor.moveToNext();
																			}
																			
																			cursor.close();
																			cursor=null;
																			dbHelper.close();
																			
																			dbHelper = new Database(SharingFileService.context);
																			database = dbHelper.getWritableDatabase();

																			for (String key : keys) {
																				deleteRow(key);
																				sql = "INSERT or replace INTO "
																						+ dbHelper.TABLE_RESULTS_NAME
																						+ " (" + dbHelper.colKey + ", "
																						+ dbHelper.colFileLocation + ") "
																						+ "VALUES('" + key + "','"
																						+ file.getPath() + "')";
																				database.execSQL(sql);
																			}
																			database.close();
																			database=null;
																			dbHelper.close();
																			
																		}
																		} catch (IOException e) {
																			
																			e.printStackTrace();
																		}
																	}//if TypeOfGet
																else
																{
																	if (typeOfget[1].equals("2"))
																	{
																		discoveredDevices.put(new KEY(IPdeviceFound, typeOfget[0]), ""+(Calendar.getInstance().getTimeInMillis()+50000));//ignore responses for 5 minutes - we do not want to download the same file multiple times
																		///DOWNLOADING FILE
																		final String toTV2="\nStarting download from: " + IPdeviceFound + "/" + portDeviceFound;
																		if (MainActivity.currentActivityTV!=null)
																				MainActivity.handler.post(new Runnable() {
																						public void run() { 
																							MainActivity.currentActivityTV.append(toTV2); }
																					});
																		Log.i(LOGTAG, "Starting autodownload from: " + IPdeviceFound + "/" + portDeviceFound);
																		 
																		
																			try {
																				
																				SQLiteDatabase database;
																				Database dbHelper;
																				dbHelper = new Database(SharingFileService.context);
																				database = dbHelper.getReadableDatabase();

																				String sql = "SELECT * FROM "
																						+ dbHelper.TABLE_AUTO_DOWNLOAD + " WHERE "
																						+ dbHelper.colKey + " = '" + typeOfget[0] + "'";
																				Cursor cursor=database.rawQuery(sql,null);
																				cursor.moveToFirst();	
																			File file = new File (SharingFileService.context.getExternalCacheDir()+"/"+cursor.getString(2)); 
																					//File.createTempFile(cursor.getString(2), "", SharingFileService.context.getExternalCacheDir());
																			Log.i(LOGTAG, "File cache created: name : "+file.getPath());
																			cursor.close();
																			cursor=null;
																			database.close();
																			dbHelper.close();
																			FileOutputStream fileOS = new FileOutputStream(file);

																			Socket socket = new Socket(IPdeviceFound, Integer.parseInt(portDeviceFound));
																			DataOutputStream output = new DataOutputStream(
																					socket.getOutputStream());
																			output.writeUTF(digest[1]);

																			InputStream input = socket.getInputStream();

																			int read;
																			byte[] buffer = new byte[8192];
																			while ((read = input.read(buffer)) != -1) {
																				fileOS.write(buffer, 0, read);
																				fileOS.flush();
																			}
																			
																			final String toTV1= "\nFinished downloading the file("+file.getPath()+") from " + IPdeviceFound+ "/" + portDeviceFound;
																			if (MainActivity.currentActivityTV!=null)
																				MainActivity.handler.post(new Runnable() {
																					public void run() {
																						MainActivity.currentActivityTV.append(toTV1); }
																				});
																			Log.i(LOGTAG, "Finished downloading the file from " + IPdeviceFound+ "/" + portDeviceFound);
																			  
																			
																			//FINISHED DOWNLOADING FILE
																			
																			//PUT THE RESULT IN THE RESULTS DATABASE TABLE
																			if (file != null) {

																				dbHelper = new Database(
																						SharingFileService.context);
																				database = dbHelper.getWritableDatabase();

																				sql = "SELECT * FROM "
																						+ dbHelper.TABLE_AUTO_DOWNLOAD
																						+ " WHERE " + dbHelper.colKey + "='"
																						+ typeOfget[0] + "'";
																				cursor = database.rawQuery(sql, null);

																				cursor.moveToFirst();
																				String strSQL = "UPDATE " + dbHelper.TABLE_AUTO_DOWNLOAD
																						+ " SET " + dbHelper.colFileLocation + " = '"
																						+ file.getPath()
																						+ "' WHERE " + dbHelper.colKey + " = '"
																						+ cursor.getString(0) + "'";
																				database.execSQL(strSQL);
																				
																				cursor.close();
																				cursor=null;
																				database.close();
																				dbHelper.close();	
																				
																			}
																			} catch (IOException e) {
																				
																				e.printStackTrace();
																			}
																		
																	}//if Typeof get
																}
																	}//if CanContinue
													}
									}
								}
						}
					}).run();
				}
			} catch (SocketException e) {

				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

	};

}
