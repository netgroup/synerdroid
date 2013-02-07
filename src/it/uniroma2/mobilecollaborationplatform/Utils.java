package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.net.*;
import java.util.*;

import android.os.Build;
import android.util.*;

import java.security.*;

public class Utils {
	private final static String LOGTAG = "Utils";
	private static String WIFI_INTERFACE_NAME = null;
	private static Boolean ARMV7_PROCESSOR = null;
	
	/**
	 * Executes a command with root permissions
	 * @param command	The command to be executed
	 * @return			The command output
	 */
	public static String[] rootExec(String command) {
		Process process = null;
		DataInputStream input = null;
		DataOutputStream output = null;
		String outputResponse = null;
		try {
			if(Build.VERSION.SDK_INT < 11) {
				process = Runtime.getRuntime().exec("su");
			} else {
				String[] cmd = {"su", "-c", "/system/bin/sh"};
				process = Runtime.getRuntime().exec(cmd);
			}
			input = new DataInputStream(process.getInputStream());
			output = new DataOutputStream(process.getOutputStream());
			output.writeBytes(command);
			output.writeBytes("exit\n");
			output.flush();
			process.waitFor();

			Vector<String> res = new Vector<String>();
			while ((outputResponse = input.readLine()) != null) {
				Log.d(LOGTAG, "Command: "+command+" - Response: "+outputResponse);
				res.add(outputResponse);
			}
			return res.toArray(new String[0]);
		} catch (Exception e) {
			Log.e(LOGTAG, "rootExecGB()", e);
		} finally {
			try {
				if(output!=null) output.close();
				if(input!=null) input.close();
				if(process!=null) process.destroy();
			} catch (Exception e) {
				Log.e(LOGTAG, "rootExecGB()", e);
			}
		}
		return null;
	}
	
	/**
	 * Extracts the path from a URL
	 * @param url	Complete URL
	 * @return		Path-only URL
	 */
	public static String getPathFromUrl(String url) {
		String ret = "";
        if (url.endsWith("/")) {
            return url; // String already contains a path
        }
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            ret += parts[i] + "/";
        }
        return ret;
	}
	
	/**
	 * Retrieves local IPv4 address of the wifi interface
	 * @return Local IPv4 address
	 */
	public static String getLocalIpAddress() {
		InetAddress inetAddress = getLocalInetAddress();
		if(inetAddress==null) return null;
		return inetAddress.getHostAddress().toString();
	}
	
	/**
	 * Retrieves local IPv4 InetAddress of the wifi interface
	 * @return	The InetAddress of the wifi interface
	 */
	public static InetAddress getLocalInetAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if(intf.getName().equals(getWifiInterface()) && inetAddress instanceof Inet4Address) {
	                	Log.i(LOGTAG, "Local inet address: "+inetAddress.getHostAddress().toString());
	                	return inetAddress;
	                }
	            }
	        }
	    } catch (SocketException e) {}
	    return null;
	}
	
	
	/**
	 * Sends an UDP multicast packet and waits for an answer
	 * 
	 * This function requires Android API level 9 or later!
	 * 
	 * @param req		Request message
	 * @param port		Port to which send the packet
	 * @param timeout	Timeout for an answer
	 * @return			Response packet
	 */
	public static DatagramPacket UDPSendReceiveMulticast(String req, int port, int timeout) {
		return UDPSendReceive(req, getBroadcastAddress(), port, timeout, true);
	}
	
	/**
	 * Sends an UDP unicast packet and waits for an answer
	 * @param req		Request message
	 * @param host		Host to which send the packet
	 * @param port		Port to which send the packet
	 * @param timeout	Timeout for an answer
	 * @return			Response message
	 */
	public static String UDPSendReceive(String req, String host, int port, int timeout) {
		DatagramPacket packet = UDPSendReceive(req, host, port, timeout, false);
		if(packet==null) return null;
		return new String(packet.getData(), 0, packet.getLength());
	}
	
	/**
	 * Sends an UDP unicast packet and waits for an answer
	 * @param req			Request message
	 * @param host			Host to which send the packet
	 * @param port			Port to which send the packet
	 * @param timeout		Timeout for an answer
	 * @param broadcast		Send a broadcast package?
	 * @return				Response package
	 */
	private static DatagramPacket UDPSendReceive(String req, String host, int port, int timeout, boolean broadcast) {
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(timeout); // Milliseconds
			if(broadcast) clientSocket.setBroadcast(true);
			InetAddress address = InetAddress.getByName(host);
			byte[] sendData;
			byte[] receiveData = new byte[1024];
			sendData = req.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
			clientSocket.send(sendPacket);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			clientSocket.close();
			return receivePacket;
		} catch(Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets the file MIME type from its extension
	 * @param url	File URL
	 * @return		MIME type
	 */
	public static String getMime(String url) {
		if(url.endsWith(".txt")) return "text/plain";
		else if(url.endsWith(".htm")) return "text/html";
		else if(url.endsWith(".ts")) return "video/mp2t";
		else if(url.endsWith(".m3u") || url.endsWith(".m3u8")) return "audio/x-mpegurl";
		else if(url.endsWith(".mp4")) return "video/mp4";
		return "";
	}
	
	/**
	 * Gets the wifi interface name
	 * @return	The wifi interface
	 */
	public static String getWifiInterface() {
		// The interface name is cached for future requests
		if(WIFI_INTERFACE_NAME==null) {
			String[] out = rootExec("getprop wifi.interface\n");
			if(out.length>0 && out[0]!=null && out[0]!="") {
				WIFI_INTERFACE_NAME = out[0];
				Log.i(LOGTAG, "Found Wifi interface: "+out[0]);
				return out[0];
			}
		} else {
			return WIFI_INTERFACE_NAME;
		}
		
		// Else, property wifi.interface was not set. So I guess it's eth0.
		WIFI_INTERFACE_NAME = "eth0";
		return WIFI_INTERFACE_NAME;
	}
	
	/**
	 * Gets the broadcast address for the wifi interface
	 * @return	The broadcast IPv4 address
	 */
	public static String getBroadcastAddress() {
		try {
			for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
		        NetworkInterface ni = niEnum.nextElement();
		        if (ni.getName().equals(getWifiInterface())) {
		            for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
		            	InetAddress broadcastInetAddress = interfaceAddress.getBroadcast();
		            	if(broadcastInetAddress instanceof Inet4Address) {
		            		return broadcastInetAddress.toString().substring(1);
		            	}
		            }
		        }
		    }
		} catch(Exception e) {
			Log.e(LOGTAG, "getBroadcastAddress()", e);
		}
		return null;
	}
	
	
	/**
	 * Gets a new link-local IPv4 address
	 * @return	Found free IPv4 address
	 */
	public static String getLinkLocalAddress() {
		int numTests = 2;
		
		Random random = new Random(System.nanoTime());
		boolean ok = false;
		while(!ok) {
			int a = random.nextInt(253)+1;
			int b = random.nextInt(253)+1;
			String newAddress = "169.254."+a+"."+b;
			Log.i("getLinkLocalAddress", "Trying address: "+newAddress);
			
			String res[] = rootExec(BaseSettings.APPDIR+"busybox arping -I "+getWifiInterface()+" -D -c "+numTests+" "+newAddress+" \n");
			for(String s : res) {
				if(s.startsWith("Received 0")) {
					ok = true;
					Log.i("getLinkLocalAddress", "Free address found: "+newAddress);
					return newAddress;
				}
			}
		}
		return null;
	}
	
	/**
	 * Calculates the MD5 digest of a file
	 * @param file	The input file
	 * @return		The MD5 digest
	 */
	public static String getMD5Digest(File file) {
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
	
	/**
	 * Checks if the current device has an ARMv7 processor. Otherwise it's supposed there's an ARMv6.
	 * @return		True if device has an ARMv7 processor, false otherwise
	 */
	public static boolean processorIsARMv7() {
		// The information is cached for future requests		
		if(ARMV7_PROCESSOR==null) {
			String cpuinfo[] = Utils.rootExec("cat /proc/cpuinfo\n");
			for(String info : cpuinfo) {
				if(info.contains("ARMv7")) {
					Log.i("LOGTAG", "ARMv7 processor found");
					ARMV7_PROCESSOR = true;
					return ARMV7_PROCESSOR;
				}
			}
		} else {
			return ARMV7_PROCESSOR;
		}
		ARMV7_PROCESSOR = false;
		return ARMV7_PROCESSOR;
	}
}
