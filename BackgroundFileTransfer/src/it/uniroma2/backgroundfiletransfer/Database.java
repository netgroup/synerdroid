package it.uniroma2.backgroundfiletransfer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper{
	
	private static final String DATABASE_NAME = "fileSharingDB.db";
	private static final int DATABASE_VERSION = 1;
	
	//TABLE NAMES
	public String TABLE_QUERIES_NAME = "queriesTable";
	public String TABLE_RESULTS_NAME = "resultsTable";
	public String TABLE_AUTO_DOWNLOAD= "autoDownloadResultsTable";
	public String TABLE_PUBLICATIONS_NAME = "publicationsTable"; 
	
	//AUTODOWNLOAD RESULTSTABLE
	//public String colKey="KEY";
	public String colQueryKey="QueryKey";
	public String colFileName="FileName";
	//public String fileLocation="FileLocation";
	
	//QUERIES TABLE
	public String colGetFlag="GetFlag";
	public String colMetadataFlag="MetadataFlag";
	
	public String colQuery="Query";
	
	public String colInsertTime="InsertTime";
	public String colExpirationTime="ExpirationTime";
	public String colLastRequested="LastRequested";
	public String colONTime= "ONTime";
	public String colOFFTime= "OFFTime";
	
	//RESULTS TABLE
	public String colKey = "KEY";
	public String colFileLocation= "FileLocation";
	//TODO add a TimeAvailable column
	
	//PUBLICATIONS TABLE
	public String colMD5digest = "MD5";
	//public String colFileLocation= "FileLocation";
	public String colMetadata = "Metadata";
	//public String colInsertTime="InsertTime";
	public String colTimeAvailable = "TimeAvailable";
	
	
	// Database creation sql statements
	private final String DATABASE_CREATE_TABLE_QUERIES = "create table "
			+ TABLE_QUERIES_NAME
			+ "( "+colKey+" String primary key, GetFlag INTEGER, MetadataFlag INTEGER,"
			+ colQuery+" String not null, "
			+ colInsertTime + " String not null, " 
			+ colExpirationTime+" String not null, "
			+ colLastRequested+" String not null, "
			+ colONTime +" String not null, "
			+ colOFFTime +" String not null );";
	
	private final String DATABASE_CREATE_TABLE_AUTODOWNLOADS= "create table "
			+ TABLE_AUTO_DOWNLOAD
			+ "("+colKey+" String primary key,"
			+ colQueryKey+" String not null, "
			+ colFileName + " String not null, "
			+ colFileLocation +" String not null);";
	
	private final String DATABASE_CREATE_TABLE_RESULTS = "create table "
			+ TABLE_RESULTS_NAME
			+ "( ID INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ colKey+" String not null, "
			+ colFileLocation +" String not null);";
	
	private final String DATABASE_CREATE_TABLE_PUBLICATIONS = "create table "
			+ TABLE_PUBLICATIONS_NAME 
			+ "( " + colMD5digest + " String primary key, "
			+ colFileLocation + " String not null, " 
			+ colMetadata +" String not null, "
			+ colInsertTime +" String not null, "
			+ colTimeAvailable+" String not null );";

	  public Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE_TABLE_QUERIES);
		database.execSQL(DATABASE_CREATE_TABLE_RESULTS);
		database.execSQL(DATABASE_CREATE_TABLE_PUBLICATIONS);
		database.execSQL(DATABASE_CREATE_TABLE_AUTODOWNLOADS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		 Log.w(Database.class.getName(),
			        "Upgrading database from version " + oldVersion + " to "
			            + newVersion + ", which will destroy all old data");
			    db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUERIES_NAME);
			    db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULTS_NAME);
			    db.execSQL("DROP TABLE IF EXISTS " + TABLE_PUBLICATIONS_NAME);
			    db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTO_DOWNLOAD);
			    onCreate(db);
		
	}

}
