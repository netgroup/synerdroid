package it.uniroma2.mobilecollaborationplatform;

import android.util.Log;

public class IFManager {
	private static String LOGTAG="IFManager";
	
	public static void startWifiAdHoc() {
		String wifiInterface = Utils.getWifiInterface();
		
		// Load wifi driver
		String command = BaseSettings.APPDIR+"wifiloader start\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
		
		// Set wifi ad-hoc
		command = BaseSettings.APPDIR+"iwconfig "+wifiInterface+" mode ad-hoc essid "+BaseSettings.WIFI_ESSID+" channel "+BaseSettings.WIFI_CHANNEL+" commit\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
		
		// Wait for interface to be ready
		Log.i(LOGTAG, "Waiting for interface to be ready...");
		try { Thread.sleep(5000); } catch(Exception e) {}
		
		// Set IP address
		// NOT NEEDED FOR IPv6
		String address = Utils.getLinkLocalAddress();
		command = BaseSettings.APPDIR+"ifconfig "+wifiInterface+" "+address+" netmask 255.255.0.0 up\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
	}
	
	public static void stopWifiAdHoc() {
		String command = BaseSettings.APPDIR+"ifconfig eth0 down\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
		
		command = BaseSettings.APPDIR+"wifiloader stop\n";
		Log.i(LOGTAG, command);
		Utils.rootExec(command);
	}
	
	/**
	 * Enables the firewall for security reasons
	 */
	public static void enableFirewall() {
		String command = BaseSettings.APPDIR+"iptables -A INPUT -p icmp -i "+Utils.getWifiInterface()+" -j ACCEPT\n";
		Utils.rootExec(command);
		command = BaseSettings.APPDIR+"iptables -A INPUT -p tcp -i "+Utils.getWifiInterface()+" --dport "+BaseSettings.TCPPORT+" -j ACCEPT\n";
		Utils.rootExec(command);
		command = BaseSettings.APPDIR+"iptables -A INPUT -p udp -i "+Utils.getWifiInterface()+" --dport "+BaseSettings.UDPPORT+" -j ACCEPT\n";
		Utils.rootExec(command);
		command = BaseSettings.APPDIR+"iptables -A INPUT -p tcp -i "+Utils.getWifiInterface()+" --sport "+BaseSettings.TCPPORT+" -j ACCEPT\n";
		Utils.rootExec(command);
		command = BaseSettings.APPDIR+"iptables -A INPUT -p udp -i "+Utils.getWifiInterface()+" --sport "+BaseSettings.UDPPORT+" -j ACCEPT\n";
		Utils.rootExec(command);
		command = BaseSettings.APPDIR+"iptables -A INPUT -i "+Utils.getWifiInterface()+" -m state --state ESTABLISHED -j ACCEPT\n";
		Utils.rootExec(command);
		command = BaseSettings.APPDIR+"iptables -A INPUT -i "+Utils.getWifiInterface()+" -j DROP\n";
		Utils.rootExec(command);
	}
	
	/**
	 * Disables the firewall
	 */
	public static void disableFirewall() {
		Utils.rootExec(BaseSettings.APPDIR+"iptables -F\n");
	}
}
