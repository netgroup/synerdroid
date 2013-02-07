package it.uniroma2.mobilecollaborationplatform;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import android.util.Log;

public class SignallingExchangerZigbee extends SignallingExchanger {
	private String LOGTAG = "SignallingExchangerZigbee";
	MulticastSocket socket;
	InetAddress group;
	boolean running;
	private HashMap<InetAddress, PeerRequest> pendingRequests;

	public SignallingExchangerZigbee(CacheManager cacheManager) {
		super(cacheManager);
		
		String myipv6 = getLocalIPv6();
		Log.i(LOGTAG, myipv6);

		String cmd1 = new String("cd /data/local/zdev;nohup ./zdevd -f conf.ini&\n");
		rootExec(cmd1);
		String cmd2 = new String("cd /data/local/zdev;./izconf -m -M adhoc\n");
		rootExec(cmd2);
		String cmd3 = new String("ip6tables -I OUTPUT -d FF01::1/128 ! -s" + myipv6 + " -j ACCEPT\n");
		rootExec(cmd3);
		String cmd4 = new String("ip6tables -A OUTPUT -d FF01::1/128 -j NFQUEUE --queue-num 345\n");
		rootExec(cmd4);
		
		String defaultRoute = null;
		String routeInfo[] = Utils.rootExec(BaseSettings.APPDIR+"busybox ip r\n");
		for(String route : routeInfo) {
			route = route.trim();
			if(route.startsWith("default") && route.endsWith("eth0")) {
				String[] splitted = route.split(" ");
				defaultRoute = splitted[2];
			}
		}
		
		String cmd5 = new String("cd /system/xbin;./ip link set eth0 down\n");
		rootExec(cmd5);
		if (linkLocalDummy0() != null) {
			String linkLocal = linkLocalDummy0();
			Log.d(LOGTAG, "Link local dummy0: "+linkLocal);
			String cmd6 = new String("cd /system/xbin;./ip -6 a del " + linkLocal + " dev dummy0\n");
			rootExec(cmd6);
		}
		String cmd7 = new String("cd /system/xbin;./ip link set eth0 up\n");
		rootExec(cmd7);
		
		if(defaultRoute!=null) {
			String cmd8 = new String(BaseSettings.APPDIR+"busybox ip r add default via "+defaultRoute+"\n");
			rootExec(cmd8);
		}
		
		try {
			socket = new MulticastSocket(BaseSettings.ZIGBEEPORT);
			group = InetAddress.getByName("FF01::1");
			socket.joinGroup(group);
		} catch(Exception e) {
			Log.e(LOGTAG, "Constructor", e);
		}
		
		pendingRequests = new HashMap<InetAddress, PeerRequest>();
	}
	
	

	public void run() {
		try {
			running = true;
			Log.i(LOGTAG, "Server in ascolto");

			while (running) {
				byte[] payload = new byte[65000];
				DatagramPacket receivePacket = new DatagramPacket(payload, payload.length, null, 0);
				socket.receive(receivePacket);
				String part = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				InetAddress address = receivePacket.getAddress();
				int port = receivePacket.getPort();
				
				int partial = Integer.parseInt(part.substring(0, 2));
				int total = Integer.parseInt(part.substring(2, 4));
				String partPayload = part.substring(4);
				Log.i("PACCHETTO", partPayload);
				
				if(partial==0) {
					PeerRequest peerRequest = new PeerRequest();
					peerRequest.lastPart = partial;
					peerRequest.address = address;
					peerRequest.port = port;
					peerRequest.msg += partPayload;
					pendingRequests.put(address, peerRequest);
				} else {
					PeerRequest peerRequest = pendingRequests.get(address);
					if(peerRequest==null) continue;
					peerRequest.lastPart++;
					if(peerRequest.lastPart!=partial) {
						Log.i("DEBUG", "Packet discarded!");
						pendingRequests.remove(address);
						continue;
					}
					peerRequest.msg += partPayload;
					
					if(total==partial+1) {
						sendResponse(peerRequest, socket);
						pendingRequests.remove(address);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendResponse(PeerRequest peerRequest, MulticastSocket socket) throws IOException {
		if (peerRequest.address.getHostAddress().equals(getLocalIPv6())) {
			Log.i(LOGTAG, "Messaggio ricevuto in locale");
			return;
		}
		Log.d(LOGTAG, "Received req: " + peerRequest.msg + " from: "+ peerRequest.address.getHostAddress());

		String res = manageRequest(peerRequest.msg);
		if(res==null) {
			Log.d(LOGTAG, "Requested file NOT found in cache.");
			return;
		}
		Log.d(LOGTAG, "Send res: "+res);
		
		List<String> parts = splitMessage(res);
		int i = 0;
		for(String part : parts) {
			String sendPart = numberString(i)+numberString(parts.size())+part;
			byte sendData[] = sendPart.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, group, peerRequest.port);
			socket.send(sendPacket);
			i++;
		}
		Log.i(LOGTAG, "Sended response to: "+peerRequest.address.getHostAddress().toString() + " Port: "+peerRequest.port);
	}

	public String sendSignal(String msg) {
		List<String> responses = sendSignalMultipleAnswer(msg, true);
		if(responses.size()==0) {
			return null;
		} else {
			return responses.get(0);
		}
	}
	
	// Questo metodo restituisce l' indirizzo ipv6 dell' interfaccia dummy0
	// scritto nel file di configurazione conf.ini

	private String getLocalIPv6() {
		String cmd1 = new String("cd /data/local/zdev;cat conf.ini\n");
		Process process = null;
		DataInputStream input = null;
		DataOutputStream output = null;
		String outputResponse = null;
		String ipv6 = null;
		try {
			process = Runtime.getRuntime().exec("su");
			input = new DataInputStream(process.getInputStream());
			output = new DataOutputStream(process.getOutputStream());
			output.writeBytes(cmd1);
			output.writeBytes("exit\n");
			output.flush();
			process.waitFor();
			while ((outputResponse = input.readLine()) != null) {
				if (outputResponse.startsWith("ipv6addr")) {
					ipv6 = outputResponse.substring(9);
					break;
				}
			}
		} catch (Exception e) {
			Log.e("LOG_TAG", e.getMessage());
		}

		finally {
			try {
				if(output != null) output.close();
				if(input != null) input.close();
				if(process != null) process.destroy();
			} catch (Exception e) {
				Log.e(LOGTAG, "getLocalIPv6()", e);
			}
		}
		return ipv6;
	}

	private String linkLocalDummy0() {
		String cmd = "cd system/xbin;./ip -6 a s\n";
		Process process = null;
		DataInputStream input = null;
		DataOutputStream output = null;
		String outputResponse = null;
		String linklocal = null;
		boolean flag = false;
		try {
			process = Runtime.getRuntime().exec("su");
			input = new DataInputStream(process.getInputStream());
			output = new DataOutputStream(process.getOutputStream());
			output.writeBytes(cmd);
			output.writeBytes("exit\n");
			output.flush();
			process.waitFor();
			while ((outputResponse = input.readLine()) != null) {
				if ((outputResponse.substring(3, 9)).equals("dummy0")) {
					while ((outputResponse = input.readLine()) != null) {
						if (outputResponse.startsWith("    inet6 fe80")) {
							flag = true;
							linklocal = outputResponse.substring(10, 38);
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			Log.e(LOGTAG, "linkLocalDummy0()", e);
		}
		if (flag == false)
			return linklocal = null;
		return linklocal;

	}

	public void stopServer() {
		running = false;
		try {
			socket.close();
		} catch(Exception e) {
			Log.e(LOGTAG, "stopServer()", e);
		}
		String kill = new String("killall zdevd\n");
		rootExec(kill);
	}
	
	@Override
	public List<String> sendSignalMultipleAnswer(String msg) {
		return sendSignalMultipleAnswer(msg, false);
	}

	public List<String> sendSignalMultipleAnswer(String msg, boolean firstOnly) {
		LinkedList<String> responses = new LinkedList<String>();
		MulticastSocket clientSocket = null;
		
		try {
			clientSocket = new MulticastSocket();
			clientSocket.setSoTimeout(BaseSettings.UDPREQTIMEOUT);
			
			List<String> parts = splitMessage(msg);
			int i = 0;
			for(String part : parts) {
				String sendPart = numberString(i)+numberString(parts.size())+part;
				byte[] sendData;
				sendData = sendPart.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, group, BaseSettings.ZIGBEEPORT);
				clientSocket.send(sendPacket);
				i++;
			}
			
			HashMap<InetAddress, PeerRequest> pendingResponses = new HashMap<InetAddress, PeerRequest>();
			while(true) {
				byte[] payload = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(payload, payload.length, null, 0);
				clientSocket.receive(receivePacket);
				String part = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				InetAddress address = receivePacket.getAddress();
				int partial = Integer.parseInt(part.substring(0, 2));
				int total = Integer.parseInt(part.substring(2, 4));
				String partPayload = part.substring(4);
				Log.i("PIECE", partPayload);
				
				if((partial==0 && !firstOnly) || (partial==0 && firstOnly && pendingResponses.size()==0)) {
					PeerRequest peerRequest = new PeerRequest();
					peerRequest.lastPart = partial;
					peerRequest.address = address;
					peerRequest.msg += partPayload;
					pendingResponses.put(address, peerRequest);
				} else {
					PeerRequest peerRequest = pendingResponses.get(address);
					if(peerRequest==null) continue;
					peerRequest.lastPart++;
					if(peerRequest.lastPart!=partial) {
						Log.i("DEBUG", "Packet discarded!");
						pendingResponses.remove(address);
						continue;
					}
					peerRequest.msg += partPayload;
					if(total==partial+1) {
						responses.add(peerRequest.msg);
						Log.i("COMPLETE RESPONSE: ", peerRequest.msg);
						pendingResponses.remove(address);
						if(firstOnly) break;
					}
				}
			}
		} catch(Exception e) {
			Log.d(LOGTAG, "sendSignalMultipleAnswer()", e);
		} finally {
			try {
				clientSocket.close();
			} catch(Exception e) {}
		}
		
		return responses;
	}
	
	private String numberString(int num) {
		if(num<10) return "0"+num;
		else return ""+num;
	}
	
	
	// Metodo per eseguire i comandi da root sul terminale del dispositivo
	public void rootExec(String Command) {
		Process process = null;
		DataInputStream input = null;
		DataOutputStream output = null;
		String outputResponse = null;
		try {
			process = Runtime.getRuntime().exec("su");
			input = new DataInputStream(process.getInputStream());
			output = new DataOutputStream(process.getOutputStream());
			output.writeBytes(Command);
			output.writeBytes("exit\n");
			output.flush();
			process.waitFor();
			int i = 0;
			while ((outputResponse = input.readLine()) != null && i < 20) {
				Log.i(LOGTAG, outputResponse);
				i++;
			}
		} catch (Exception e) {
			Log.e(LOGTAG, "rootExec()", e);
		}

		finally {
			try {
				if (output != null) output.close();
				if (input != null) input.close();
				if (process != null) process.destroy();
			} catch (Exception e) {}
		}
	}
	
	
	private List<String> splitMessage(String msg) {
		LinkedList<String> parts = new LinkedList<String>();
		while(msg.length()>0) {
			int s;
			if(msg.length()>BaseSettings.MAXZIGBEEPAYLOAD) {
				s = BaseSettings.MAXZIGBEEPAYLOAD;
			} else {
				s = msg.length();
			}
			String part = msg.substring(0, s);
			msg = msg.substring(s);
			parts.add(part);
		}
		return parts;
	}
	
	private class PeerRequest {
		public InetAddress address;
		public int port;
		int lastPart = -1;
		String msg = "";
	}
}