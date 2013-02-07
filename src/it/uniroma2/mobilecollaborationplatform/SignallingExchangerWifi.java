package it.uniroma2.mobilecollaborationplatform;

import java.net.*;
import java.util.*;
import android.util.Log;

public class SignallingExchangerWifi extends SignallingExchanger {
	private final String LOGTAG = "SignallingExchangerWifi";
	private DatagramSocket serverSocket;
	boolean running;
	
	public SignallingExchangerWifi(CacheManager cacheManager) {
		super(cacheManager);
	}
	
	public String sendSignal(String msg) {
		DatagramPacket resPacket = Utils.UDPSendReceiveMulticast(msg, BaseSettings.UDPPORT, 2000);
		if(resPacket==null) return null;
		String res = new String(resPacket.getData(), 0, resPacket.getLength());
		return res;
	}
	
	public List<String> sendSignalMultipleAnswer(String msg) {
		LinkedList<String> responses = new LinkedList<String>();
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(BaseSettings.UDPREQTIMEOUT); // Milliseconds
			clientSocket.setBroadcast(true);
			InetAddress address = InetAddress.getByName(Utils.getBroadcastAddress());
			byte[] sendData;
			byte[] receiveData = new byte[1024];
			sendData = msg.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, BaseSettings.UDPPORT);
			clientSocket.send(sendPacket);
			boolean run = true;
			while(run) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				responses.add(new String(receivePacket.getData(), 0, receivePacket.getLength()));
			}
			clientSocket.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return responses;
	}
	
	public void run() {
		Log.i(LOGTAG, "Starting server UDP");
		running = true;
		try {
			if(BaseSettings.serversListenOnAllInterfaces) {
				serverSocket = new DatagramSocket(BaseSettings.UDPPORT);
			} else {
				serverSocket = new DatagramSocket(BaseSettings.UDPPORT, Utils.getLocalInetAddress());
			}
	        byte[] receiveData = new byte[1024];
	        byte[] sendData = new byte[1024];
			while(running) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				InetAddress address = receivePacket.getAddress();
				int port = receivePacket.getPort();
				
				if(receivePacket.getAddress().getHostAddress().equals(Utils.getLocalIpAddress())) {
					continue; // I received a request sent from myself, obviously I ignore it.
				}
				
				String req = new String(receivePacket.getData(), 0, receivePacket.getLength());
				Log.d(LOGTAG, "Received req: "+req+" from: "+receivePacket.getAddress().getHostAddress());
				
				String res = manageRequest(req);
				if(res==null) {
					Log.d(LOGTAG, "Requested file NOT found in cache.");
					continue;
				}
				
				Log.d(LOGTAG, "Send res: "+res);
				sendData = res.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
				serverSocket.send(sendPacket);
			}
		} catch(Exception e) {
			Log.e(LOGTAG, "run()", e);
		}
	}
	
	public void stopServer() {
		serverSocket.close();
	}
}
