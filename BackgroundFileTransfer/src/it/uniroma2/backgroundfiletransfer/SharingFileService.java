package it.uniroma2.backgroundfiletransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Calendar;

import it.uniroma2.backgroundfiletransfer.aidls.*;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class SharingFileService extends Service {

	public static String LOGTAG = "SharingFileService";
	private String INTENT_ACTION_BIND_MESSAGE_SERVICE = "bind.ClientService";

	private CommunicationManager ComMgr;

	static Context context;

	
	@Override
	public void onCreate() { 
		super.onCreate();
		
		context = getApplicationContext();
		ComMgr = CommunicationManager.getInstance();
		new Thread(ComMgr).start();
		
		Log.i(LOGTAG, "SharingFileService Created");
	}

	/**
	 * Subscribe(),get() - options : 
	 * TIME_5S_5S    - emit a query every second for 5 seconds then sleep 5 seconds
	 * TIME_5S_25S  - emit a query every second for 5 seconds then sleep 25 seconds
	 */

	@Override
	public IBinder onBind(Intent intent) {
		if (INTENT_ACTION_BIND_MESSAGE_SERVICE.equals(intent.getAction())) {
			Log.d(LOGTAG, "The SharingFileService was bound.");

			return new IClientService.Stub() {

				@Override
				public String[] notify(String key) throws RemoteException {

					Log.i(LOGTAG, "getNotifications");
					String[] list = new String[1];
					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(SharingFileService.context);
					database = dbHelper.getReadableDatabase();

					String sql = "SELECT COUNT(*) FROM "
							+ dbHelper.TABLE_RESULTS_NAME + " WHERE "
							+ dbHelper.colKey + " = '" + key + "'";
					SQLiteStatement statement = database.compileStatement(sql);
					long numRows = statement.simpleQueryForLong();
					statement.close();
					if (numRows > 0) {
						Cursor cursor = database.rawQuery("SELECT * FROM "
								+ dbHelper.TABLE_RESULTS_NAME + " WHERE "
								+ dbHelper.colKey + " = '" + key + "'", null);

						list = new String[(int) numRows];
						int i = 0;
						cursor.moveToFirst();
						while (!cursor.isAfterLast()) {
							list[i] = cursor.getString(2);
							i++;
							cursor.moveToNext();
						}
						cursor.close();
						cursor=null;
						
					} else 
						{
						
						sql = "SELECT COUNT(*) FROM "
								+ dbHelper.TABLE_AUTO_DOWNLOAD + " WHERE "
								+ dbHelper.colQueryKey + " = '" + key + "'";
						statement = database.compileStatement(sql);
						numRows = statement.simpleQueryForLong();
						statement.close();
						if (numRows!=0)
						{
							Cursor cursor = database.rawQuery("SELECT * FROM "
									+ dbHelper.TABLE_AUTO_DOWNLOAD + " WHERE "
									+ dbHelper.colQueryKey + " = '" + key + "'", null);

							list = new String[(int) numRows];
							int i = 0;
							cursor.moveToFirst();
							while (!cursor.isAfterLast()) {
								list[i] = cursor.getString(3);
								i++;
								cursor.moveToNext();
							}

							
						}
						else
						
						{Log.i(LOGTAG, "Platform does not have any results for "
								+ key);
						list = new String[1];
						list[0] = "";
						}
				}
					database.close();
					dbHelper.close();
					return list;

				}

				@Override
				public String subscribe(String metadata, String options) {

					int onTime = 0;
					int offTime = 0;
					if (options.equals("TIME_5S_5S")) {
						onTime = 5;// seconds
						offTime = 5;// seconds
					} else if (options.equals("TIME_5S_25S")) {
						onTime = 5;// seconds
						offTime = 25;// seconds
					} else if (options.equals("TIME_5M_5H")) {
						onTime = 300;// seconds
						offTime = 18000;// seconds
					} else if (options.equals("TIME_30M_24H")) {
						onTime = 1800;// seconds
						offTime = 86400;// seconds
					} else if (options.equals("TIME_1H_48H")) {
						onTime = 3600;// seconds
						offTime = 172800;// seconds
					} else
						return "error";

					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(SharingFileService.context);
					database = dbHelper.getReadableDatabase();

					// check if the key already exists in the table database
					String key = RandomAlphaNumericString(3);
					String sql = "SELECT COUNT(*) FROM "
							+ dbHelper.TABLE_QUERIES_NAME + " WHERE "
							+ dbHelper.colKey + " = '" + key + "'";
					SQLiteStatement statement = database.compileStatement(sql);
					long numRows = statement.simpleQueryForLong();
					
					while (numRows > 0) {
						key = RandomAlphaNumericString(3);
						statement = database.compileStatement(sql);
						numRows = statement.simpleQueryForLong();
					}
					statement.close();
					dbHelper.close();

					// inserts the new entry into the table
					dbHelper = new Database(context);
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
							+ key + "','0','1','" + metadata + "','"
							+ Calendar.getInstance().getTimeInMillis() + "','"
							+ "172800" + "','" + (Calendar.getInstance().getTimeInMillis()-(offTime*1000)) + "','"					
							+ onTime + "','"+offTime+"')";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();
					Log.i(LOGTAG, "Get request on metadata " + metadata
							+ "registered." + " Key is " + key);
					return key;// The metadata is registered for search. Can be
								// unregistered using the returned key.
				}

				@Override
				public String unsubscribe(String key) {

					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(SharingFileService.context);
					database = dbHelper.getWritableDatabase();
					String sql = "DELETE FROM " + dbHelper.TABLE_QUERIES_NAME
							+ " WHERE " + dbHelper.colKey + " = '" + key + "';";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();
					Log.i(LOGTAG, "The request with key :"+key+" was canceled.");
					return "200";// Everything is ok, the app unsubscribed
									// successfully
				}

				@Override
				public String get(String md5digest, String options) {

					int onTime = 0;
					int offTime = 0;
					if (options.equals("TIME_5S_5S")) {
						onTime = 5;// seconds
						offTime = 5;// seconds
					} else if (options.equals("TIME_5S_25S")) {
						onTime = 5;// seconds
						offTime = 25;// seconds
					} else if (options.equals("TIME_5M_5H")) {
						onTime = 300;// seconds
						offTime = 18000;// seconds
					} else if (options.equals("TIME_30M_24H")) {
						onTime = 1800;// seconds
						offTime = 86400;// seconds
					} else if (options.equals("TIME_1H_48H")) {
						onTime = 3600;// seconds
						offTime = 172800;// seconds
					} else
						return "error";

					Log.i(LOGTAG, "Digest received to submit: " + md5digest);
					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(context);
					database = dbHelper.getReadableDatabase();

					String key = RandomAlphaNumericString(3);
					String sql = "SELECT COUNT(*) FROM "
							+ dbHelper.TABLE_QUERIES_NAME + " WHERE "
							+ dbHelper.colKey + " = '" + key + "'";
					SQLiteStatement statement = database.compileStatement(sql);
					long numRows = statement.simpleQueryForLong();

					while (numRows > 0) {
						key = RandomAlphaNumericString(3);
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
							+ key + "','1','0','" + md5digest + "','"
							+ Calendar.getInstance().getTimeInMillis() + "','"
							+ "172800" + "','" + (Calendar.getInstance().getTimeInMillis()-(offTime*1000)) + "','"					
							+ onTime + "','"+offTime+"')";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();

					return key;// The file is registered for download. Can be
								// unregistered using the returned key.
				}

				@Override
				public String deleteGet(String key) throws RemoteException {
					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(SharingFileService.context);
					database = dbHelper.getWritableDatabase();
					String sql = "DELETE FROM " + dbHelper.TABLE_QUERIES_NAME
							+ " WHERE " + dbHelper.colKey + " = '" + key + "';";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();
					Log.i(LOGTAG, "The download with key :"+key+" was canceled.");
					return "200";
				}

				/**
				 * Inserts a file (existent at fileLocation) with the specific metadata in the publications 
				 * table. The file will be shared for the amount of time specified in availableTime parameter.
				 * 
				 * Parameter availableTime has to be of the format "00D:00H:00M:00S" (days:hours:minutes:seconds)
				 */
				@Override
				public String publish(String fileLocation, String metadata, String availableTime) {
					
					// Transform the availableTime parameter in miliseconds
					String[] time = availableTime.split(":");
					if (time.length!=4)
						return "error";
					long availabletime = 0;
					
					// transform the days in miliseconds
					String aux = time[0].substring(0, 2);
					availabletime = Integer.parseInt(aux) * 24 * 3600 * 1000;

					// transform the hours in miliseconds
					aux=time[1].substring(0, 2);
					availabletime += Integer.parseInt(aux) * 3600 * 1000;
					
					// transform the minutes in miliseconds
					aux=time[2].substring(0, 2);
					availabletime += Integer.parseInt(aux) * 1000;
					
					// transform the seconds in miliseconds
					aux=time[3].substring(0, 2);
					availabletime += Integer.parseInt(aux) * 1000;

					File file=new File(fileLocation);
					String md5digest=getMD5Digest(file);
					
					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(context);
					database = dbHelper.getWritableDatabase();
					long insertTime=Calendar.getInstance().getTimeInMillis();
					String sql = "INSERT or replace INTO "
							+ dbHelper.TABLE_PUBLICATIONS_NAME + " ("
							+ dbHelper.colMD5digest + ", " + dbHelper.colFileLocation
							+ ", "+dbHelper.colMetadata+","+dbHelper.colInsertTime+"," + dbHelper.colTimeAvailable +") " 
							+ "VALUES('"
							+ md5digest +"','" +fileLocation + "','" + metadata + "','"+insertTime +"','"
							+ availabletime + "')";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();
					Log.i(LOGTAG, "published: key "+md5digest+" : "+fileLocation+" Metadata: "+ metadata+"Insert Time : "+insertTime+" Available Time: " +availableTime+" = "+availabletime+" miliseconds");
					return md5digest;// The file is published. Can be
										// unpublished using the returned key.
				}

				/**
				 * Deletes a file identified through its digest from the publications table.
				 */
				@Override
				public String unpublish(String md5digest)
						throws RemoteException {

					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(SharingFileService.context);
					database = dbHelper.getWritableDatabase();
					String sql = "DELETE FROM " + dbHelper.TABLE_PUBLICATIONS_NAME
							+ " WHERE " + dbHelper.colMD5digest + " = '" + md5digest + "';";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();
					

					return "200";
				}

				@Override
				public String autoDownload(String metadata, String options)
						throws RemoteException {
					int onTime = 0;
					int offTime = 0;
					if (options.equals("TIME_5S_5S")) {
						onTime = 5;// seconds
						offTime = 5;// seconds
					} else if (options.equals("TIME_5S_25S")) {
						onTime = 5;// seconds
						offTime = 25;// seconds
					} else if (options.equals("TIME_5M_5H")) {
						onTime = 300;// seconds
						offTime = 18000;// seconds
					} else if (options.equals("TIME_30M_24H")) {
						onTime = 1800;// seconds
						offTime = 86400;// seconds
					} else if (options.equals("TIME_1H_48H")) {
						onTime = 3600;// seconds
						offTime = 172800;// seconds
					} else
						return "error";

					Log.i(LOGTAG, "Metadata received to autodownload: " + metadata);
					SQLiteDatabase database;
					Database dbHelper;
					dbHelper = new Database(context);
					database = dbHelper.getReadableDatabase();

					String key = RandomAlphaNumericString(3);
					String sql = "SELECT COUNT(*) FROM "
							+ dbHelper.TABLE_QUERIES_NAME + " WHERE "
							+ dbHelper.colKey + " = '" + key + "'";
					SQLiteStatement statement = database.compileStatement(sql);
					long numRows = statement.simpleQueryForLong();

					while (numRows > 0) {
						key = RandomAlphaNumericString(3);
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
							+ key + "','0','2','" + metadata + "','"
							+ Calendar.getInstance().getTimeInMillis() + "','"
							+ "172800" + "','" + (Calendar.getInstance().getTimeInMillis()-(offTime*1000)) + "','"					
							+ onTime + "','"+offTime+"')";
					database.execSQL(sql);
					database.close();
					database=null;
					dbHelper.close();

					return key;// The file is registered for download. Can be
								// unregistered using the returned key.
					
				}

			};
		}
		return null;
	}

	/**
	 * Generate a random alpha-numeric string of a given size.
	 * 
	 * @param size
	 * @return
	 */
	public static String RandomAlphaNumericString(int size) {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String ret = "";
		int length = chars.length();
		for (int i = 0; i < size; i++) {
			ret += chars.split("")[(int) (Math.random() * (length - 1))];
		}
		return ret;
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
