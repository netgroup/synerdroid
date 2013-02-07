package it.uniroma2.mobilecollaborationplatform;


import java.util.List;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * A class to keep the information about the applications using the internet
 */

public class NetApp {
	public String name;
	public String uid;
	public int port;
	
	public NetApp(String name, int port) {
		this.name = name;
		this.port = port;
		uid = getAppUid(name);
	}
	
	public NetApp(String name, int port, String uid) {
		this.name = name;
		this.port = port;
		this.uid = uid;
	}
	
	public String getAppUid(String app) {
		Context context = ProxyService.context;
		int uid = 0;
		PackageManager pm = context.getPackageManager();
		List<ApplicationInfo> listApplication = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		for (ApplicationInfo packageInfo : listApplication) {
			if (packageInfo.packageName.equals(app)) {
				uid = packageInfo.uid;
			}
		}
		return uid+"";
	}
}
