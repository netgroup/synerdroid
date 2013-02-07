package it.uniroma2.mobilecollaborationplatform;

import java.io.*;

import android.app.*;
import android.content.*;
import android.net.wifi.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;

public class MCPActivity extends Activity implements OnClickListener {
	private final String LOGTAG = "DroidVideoProxyActivity";
	
	public boolean status = false; // Service status: true=running, false=stopped
	private Intent serviceIntent;
	
	private TextView textStatus;
	private Button buttonStart;
	private Button buttonStop;
	private Spinner spinnerSignallingTechnology;
	private CheckBox checkBoxClearCache;
	private EditText editTextMaxCacheSize;
	private CheckBox checkBoxNoWifiAdHoc;
	private CheckBox checkBoxNoIptablesRedirect;
	private CheckBox checkBoxServersListenOnAllInterfaces;
	private CheckBox checkBoxDisableFirewall;
	
	// Labels
	private TextView textViewSignallingTechnology;
	private TextView textViewMaximumCacheSize;
	private TextView textViewDebugOptions;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Gets the interface elements and assignes events to buttons
        textStatus = (TextView)findViewById(R.id.textStatus);
    	buttonStart = (Button)findViewById(R.id.buttonStart);
    	buttonStop = (Button)findViewById(R.id.buttonStop);
    	buttonStart.setOnClickListener(this);
    	buttonStop.setOnClickListener(this);
    	
    	buttonStart.requestFocus();
    	    	
    	spinnerSignallingTechnology = (Spinner)findViewById(R.id.spinnerSignallingTechnology);
    	checkBoxClearCache = (CheckBox)findViewById(R.id.checkBoxClearCache);
    	editTextMaxCacheSize = (EditText)findViewById(R.id.editTextMaxCacheSize);
    	
    	checkBoxNoWifiAdHoc = (CheckBox)findViewById(R.id.checkBoxNoWifiAdHoc);
    	checkBoxNoIptablesRedirect = (CheckBox)findViewById(R.id.checkBoxNoIptablesRedirect);
    	checkBoxServersListenOnAllInterfaces = (CheckBox)findViewById(R.id.checkBoxServersListenOnAllInterfaces);
    	checkBoxDisableFirewall = (CheckBox)findViewById(R.id.checkBoxDisableFirewall);
    	
    	textViewSignallingTechnology = (TextView)findViewById(R.id.textViewSignallingTechnology);
    	textViewMaximumCacheSize = (TextView)findViewById(R.id.textViewMaximumCacheSize);
    	textViewDebugOptions = (TextView)findViewById(R.id.textViewDebugOptions);
    	
    	// Avoids soft keyboard to popup every time the app is launched
    	textStatus.setFocusable(true);
    	textStatus.setFocusableInTouchMode(true);
    	textStatus.requestFocus();
        
        serviceIntent = new Intent(this, ProxyService.class);
        
        loadSettings();
        
        // Gets root permissions
        try {
			Runtime.getRuntime().exec("su");
		} catch (IOException e) {
			Log.e(LOGTAG, "Impossibile ottenere permessi di root");
		}
    }
    
    /**
     * Sets the application status
     * @param s	The status to be set
     */
    private void setStatus(boolean s) {
		if (s == true) {
			status = true;
			String address = Utils.getLocalIpAddress();
			if (address == null) {
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle(getString(R.string.error));
				alertDialog.setMessage(getString(R.string.errorStartingProxy));
				alertDialog.setButton("Close app", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						stopService(serviceIntent);
						setStatus(false);
						finish();
						return;
					}
				});
				alertDialog.show();
			}
			textStatus.setText(getString(R.string.proxyEnabled) + " (" + address + ")");
			buttonStart.setClickable(false);
			buttonStop.setClickable(true);
			spinnerSignallingTechnology.setEnabled(false);
			editTextMaxCacheSize.setEnabled(false);
			checkBoxClearCache.setEnabled(false);
			checkBoxNoWifiAdHoc.setEnabled(false);
			checkBoxNoIptablesRedirect.setEnabled(false);
			checkBoxServersListenOnAllInterfaces.setEnabled(false);
			textViewSignallingTechnology.setEnabled(false);
	    	textViewMaximumCacheSize.setEnabled(false);
	    	textViewDebugOptions.setEnabled(false);
	    	checkBoxDisableFirewall.setEnabled(false);
		} else {
			status = false;
			textStatus.setText(R.string.proxyDisabled);
			buttonStart.setClickable(true);
			buttonStop.setClickable(false);
			spinnerSignallingTechnology.setEnabled(true);
			editTextMaxCacheSize.setEnabled(true);
			checkBoxClearCache.setEnabled(true);
			checkBoxNoWifiAdHoc.setEnabled(true);
			checkBoxNoIptablesRedirect.setEnabled(true);
			checkBoxServersListenOnAllInterfaces.setEnabled(true);
			textViewSignallingTechnology.setEnabled(true);
	    	textViewMaximumCacheSize.setEnabled(true);
	    	textViewDebugOptions.setEnabled(true);
	    	checkBoxDisableFirewall.setEnabled(true);
		}
		saveSettings();
	}
    
    /**
     * Reloads the application status
     */
    public void loadSettings() {
        SharedPreferences settings = getSharedPreferences(BaseSettings.PREFS_NAME, 0);
        boolean s = settings.getBoolean("status", false);
        editTextMaxCacheSize.setText(settings.getString("maxCacheSize", "100"));
        checkBoxNoWifiAdHoc.setChecked(settings.getBoolean("noWifiAdHoc", false));
        checkBoxNoIptablesRedirect.setChecked(settings.getBoolean("noIptablesRedirect", false));
        checkBoxServersListenOnAllInterfaces.setChecked(settings.getBoolean("serversListenOnAllInterfaces", false));
        checkBoxDisableFirewall.setChecked(settings.getBoolean("disableFirewall", false));
        String st = settings.getString("signallingTechnology", null);
        if(st!=null) {
        	for(int i=0; i<spinnerSignallingTechnology.getCount(); i++) {
        		if(spinnerSignallingTechnology.getItemAtPosition(i).toString().equals(st)) {
        			spinnerSignallingTechnology.setSelection(i);
        		}
        	}
        }
        setStatus(s);
    }
    
    /**
     * Saves the current application status
     */
	public void saveSettings() {
		SharedPreferences settings = getSharedPreferences(BaseSettings.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("status", status);
		editor.putString("maxCacheSize", editTextMaxCacheSize.getText().toString());
		editor.putBoolean("noWifiAdHoc", checkBoxNoWifiAdHoc.isChecked());
		editor.putBoolean("noIptablesRedirect", checkBoxNoIptablesRedirect.isChecked());
		editor.putBoolean("serversListenOnAllInterfaces", checkBoxServersListenOnAllInterfaces.isChecked());
		editor.putBoolean("disableFirewall", checkBoxDisableFirewall.isChecked());
		editor.putString("signallingTechnology", spinnerSignallingTechnology.getSelectedItem().toString());
		editor.commit();
	}
	
	private boolean isWifiDisabled() {
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		return (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED);
	}

	/**
	 * Click listener for buttons
	 */
	public void onClick(View v) {
		if(v.equals(buttonStart)) {
			if(!isWifiDisabled() && !checkBoxNoWifiAdHoc.isChecked()) {
				AlertDialog.Builder alert_box = new AlertDialog.Builder(this);
				alert_box.setMessage(R.string.switchWifiOff);
				alert_box.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				alert_box.show();
				return;
			}
			
			textStatus.setText(R.string.starting);
			serviceIntent.putExtra("signallingTechnology", spinnerSignallingTechnology.getSelectedItem().toString());
			serviceIntent.putExtra("clearCache", checkBoxClearCache.isChecked());
			serviceIntent.putExtra("cacheSize", Integer.parseInt(editTextMaxCacheSize.getText().toString()));
			serviceIntent.putExtra("noWifiAdHoc", checkBoxNoWifiAdHoc.isChecked());
			serviceIntent.putExtra("noIptablesRedirect", checkBoxNoIptablesRedirect.isChecked());
			serviceIntent.putExtra("serversListenOnAllInterfaces", checkBoxServersListenOnAllInterfaces.isChecked());
			serviceIntent.putExtra("disableFirewall", checkBoxDisableFirewall.isChecked());
			
			new Thread() {
				public void run() {
					startService(serviceIntent);
					runOnUiThread(new Runnable() {
						public void run() {
							setStatus(true);
						}
					});
			    	
				}
			}.start();

	    	//startService(serviceIntent);
	    	//setStatus(true);
		} else if(v.equals(buttonStop)) {
			textStatus.setText(R.string.stopping);
			stopService(serviceIntent);
	    	setStatus(false);
		}
	}
	
}