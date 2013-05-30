package it.uniroma2.backgroundfiletransfer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	final static String LOGTAG="MainActivity";
	public static Context context;
	static TextView ipTextView;
	static TextView currentStatusTV;
	static TextView currentActivityTV;
	public static String appDir;
	private FileTransferManager incRM;
	private QueryManager sendRM;
	public static Handler handler;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		context=this;
		handler=new Handler();
		appDir=getFilesDir().getPath();
		ipTextView=(TextView) findViewById(R.id.IPTextView);
		currentStatusTV=(TextView) findViewById(R.id.CurrentStatusTV);
		currentActivityTV=(TextView) findViewById(R.id.CurrentActivityTV);
		
		currentStatusTV.setMovementMethod(new ScrollingMovementMethod());
		currentActivityTV.setMovementMethod(new ScrollingMovementMethod());
		
		currentStatusTV.setBackgroundColor(Color.parseColor("#000000"));
		currentStatusTV.setTextColor(Color.parseColor("#FFFFFF"));
		
		currentActivityTV.setBackgroundColor(Color.parseColor("#000000"));
		currentActivityTV.setTextColor(Color.parseColor("#FFFFFF"));
		
		Database dbHelper=new Database(MainActivity.context);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String sqll = "SELECT COUNT(*) FROM "
				+ dbHelper.TABLE_RESULTS_NAME;
		SQLiteStatement statement = db.compileStatement(sqll);
		long count = statement.simpleQueryForLong();
		statement.close();
		String text="Results Table has :"+count+" entries.\n";
		
		sqll= "SELECT COUNT(*) FROM "
				+ dbHelper.TABLE_PUBLICATIONS_NAME;
		statement = db.compileStatement(sqll);
		count=statement.simpleQueryForLong();
		statement.close();
		text+="There are "+count+" published files.\n";
		
		sqll= "SELECT COUNT(*) FROM "
				+ dbHelper.TABLE_QUERIES_NAME;
		statement = db.compileStatement(sqll);
		count=statement.simpleQueryForLong();
		statement.close();
		text+="There are "+count+" queries registered.\n";
		currentStatusTV.setText(text);
		db.close();
		dbHelper.close();
		
		incRM=FileTransferManager.getInstance();
		sendRM=QueryManager.getInstance();
		
		ipTextView.setText("Local IP "+Utils.getLocalIpAddress());
		
		// Gets root permissions
        try {
			Runtime.getRuntime().exec("su");
			Log.i(LOGTAG, "Root permissions acquired.");
		} catch (IOException e) {
			Log.e(LOGTAG, "Couldn't get root permissions.");
		}
		
		// Copies required executables to the application directory
        if (CommunicationManager.wifiLock==null){
       	copyFileFromAssets("iptables", false);
		copyFileFromAssets("wifiloader", false);
		copyFileFromAssets("iwconfig", false);
		copyFileFromAssets("ifconfig", true);
		copyFileFromAssets("ip", false);
		copyFileFromAssets("busybox", false);}
		
		//Start Client Service
		Intent intent=new Intent(this, SharingFileService.class);
		startService(intent);	
		
		Button startButton=(Button)findViewById(R.id.StartListening);
		startButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!isWifiDisabled() ) {
					AlertDialog.Builder alert_box = new AlertDialog.Builder(MainActivity.context);
					alert_box.setMessage("Switch wifi off first.");
					alert_box.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
					alert_box.show();
					return;
				}
				
				CommunicationManager.getInstance().changeState(false);
				new Thread(CommunicationManager.getInstance()).start();
				
				Log.i(LOGTAG,"Started listening. (Button clicked).");
			}
		});
		
		Button stopButton=(Button)findViewById(R.id.StopListening);	
		stopButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CommunicationManager.getInstance().changeState(true);
				CommunicationManager.getInstance().stop();
				
			}
		});

		Button refreshLogs = (Button) findViewById(R.id.RefreshLogs);
		refreshLogs.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				currentActivityTV.setText("");
				currentStatusTV.setText("");
				Database dbHelper=new Database(MainActivity.context);
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				String sqll = "SELECT COUNT(*) FROM "
						+ dbHelper.TABLE_RESULTS_NAME;
				SQLiteStatement statement = db.compileStatement(sqll);
				long count = statement.simpleQueryForLong();
				statement.close();
				String text="Results Table has :"+count+" entries.\n";
				
				sqll= "SELECT COUNT(*) FROM "
						+ dbHelper.TABLE_PUBLICATIONS_NAME;
				statement = db.compileStatement(sqll);
				count=statement.simpleQueryForLong();
				statement.close();
				text+="There are "+count+" published files.\n";
				
				sqll= "SELECT COUNT(*) FROM "
						+ dbHelper.TABLE_QUERIES_NAME;
				statement = db.compileStatement(sqll);
				count=statement.simpleQueryForLong();
				statement.close();
				text+="There are "+count+" queries registered.\n";
				currentStatusTV.setText(text);
				db.close();
				dbHelper.close();
			}
		});
	}
	
	/**
	 * Copies binary files from assets to the application directory
	 * @param name								Name of the executable
	 * @param differentProcessorExecutables		Whether there are different executables for different processors
	 */
	private void copyFileFromAssets(String name, boolean differentProcessorExecutables) {
		String APPDIR =  appDir+"/";//"/data/data/it.uniroma.mobiletransmissionplatform/";
		AssetManager assetManager = getAssets();
		InputStream in = null;
		OutputStream out = null;
		try {
			if(differentProcessorExecutables) {
				if(Utils.processorIsARMv7()) {
					in = assetManager.open(name+"-armv7");
				} else {
					in = assetManager.open(name+"-armv6");
				}
			} else {
				in = assetManager.open(name);
			}
			out = new FileOutputStream(APPDIR+name);
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch (Exception e) {
			Log.e(LOGTAG, e.getMessage());
		}

		// Changes file permissions
		String command = "chmod 777 "+APPDIR+name+"\n";
		Utils.rootExec(command);
		Log.i(LOGTAG, name+ " file copied in "+ APPDIR);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private boolean isWifiDisabled() {
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		return (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED);
		
	}

}
