/*
Copyright 2011-2013 Pieter Pareit
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.ppareit.swiftp;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import meesters.wifip2p.app.FtpP2PBridge;
import meesters.wifip2p.deps.Router;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import be.ppareit.swiftp.server.SessionThread;
import be.ppareit.swiftp.server.TcpListener;

/**
 * FsService is the administrator service: it starts up
 * {@link be.ppareit.swiftp.server.SessionThread}s, keeps the ftp server running
 * and shuts it down. Compatibility for Wi-Fi P2P was added by Daniel Schoonwinkel
 * 
 * @author David Revell, Pieter Pareit, Daniel Schoonwinkel
 * 
 */

public class FsService extends Service implements Runnable,
		ConnectionInfoListener {
	private static final String TAG = FsService.class.getSimpleName();

	// Service will (global) broadcast when server start/stop
	static public final String ACTION_STARTED = "be.ppareit.swiftp.FTPSERVER_STARTED";
	static public final String ACTION_STOPPED = "be.ppareit.swiftp.FTPSERVER_STOPPED";
	static public final String ACTION_FAILEDTOSTART = "be.ppareit.swiftp.FTPSERVER_FAILEDTOSTART";

	// RequestStartStopReceiver listens for these actions to start/stop this
	// server
	static public final String ACTION_START_FTPSERVER = "be.ppareit.swiftp.ACTION_START_FTPSERVER";
	static public final String ACTION_STOP_FTPSERVER = "be.ppareit.swiftp.ACTION_STOP_FTPSERVER";
	static public final String ACTION_UPDATE_FTPSERVER = "be.ppareit.swiftp.ACTION_UPDATE_FTPSERVER";

	protected static Thread serverThread = null;
	protected boolean shouldExit = false;

	protected ServerSocket listenSocket;

	// The server thread will check this often to look for incoming
	// connections. We are forced to use non-blocking accept() and polling
	// because we cannot wait forever in accept() if we want to be able
	// to receive an exit signal and cleanly exit.
	public static final int WAKE_INTERVAL_MS = 1000; // milliseconds

	private TcpListener wifiListener = null;
	private final List<SessionThread> sessionThreads = new ArrayList<SessionThread>();

	private PowerManager.WakeLock wakeLock;
	private WifiLock wifiLock = null;

	private static Channel mChannel = null;
	private static WifiP2pManager mWifiP2PManager = null;
	private static boolean isWifiP2PConnected = false;
	private static InetAddress mWifiP2PAddress = null;

	private WifiP2PReceiver mWifiP2PReceiver = null;
	private IntentFilter mFilter = null;
	private FtpP2PBridge mBridge = null;

	private WifiP2pDevice mThisP2PDevice = null;

	@Override
	public void onCreate() {
		super.onCreate();

		// Starting everything needed for the WifiP2P connection
		mWifiP2PManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
		mChannel = mWifiP2PManager.initialize(getApplicationContext(),
				getMainLooper(), null);
		mWifiP2PManager.requestConnectionInfo(mChannel, this);
		mWifiP2PReceiver = new WifiP2PReceiver(mWifiP2PManager, mChannel, this);
		mFilter = WifiP2PReceiver.getIntentFilter();
		registerReceiver(mWifiP2PReceiver, mFilter);
		mBridge = new FtpP2PBridge(getApplicationContext());
		mBridge.init();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		shouldExit = false;
		int attempts = 10;
		// The previous server thread may still be cleaning up, wait for it to
		// finish.
		while (serverThread != null) {
			Log.w(TAG, "Won't start, server thread exists");
			if (attempts > 0) {
				attempts--;
				Util.sleepIgnoreInterupt(1000);
			} else {
				Log.w(TAG, "Server thread already exists");
				return START_STICKY;
			}
		}
		Log.d(TAG, "Creating server thread");
		serverThread = new Thread(this);
		serverThread.start();
		return START_STICKY;
	}

	public static boolean isRunning() {
		// return true if and only if a server Thread is running
		if (serverThread == null) {
			Log.d(TAG, "Server is not running (null serverThread)");
			return false;
		}
		if (!serverThread.isAlive()) {
			Log.d(TAG, "serverThread non-null but !isAlive()");
		} else {
			Log.d(TAG, "Server is alive");
		}
		return true;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy() Stopping server");
		shouldExit = true;
		if (serverThread == null) {
			Log.w(TAG, "Stopping with null serverThread");
			return;
		}
		serverThread.interrupt();
		try {
			serverThread.join(10000); // wait 10 sec for server thread to finish
		} catch (InterruptedException e) {
		}
		if (serverThread.isAlive()) {
			Log.w(TAG, "Server thread failed to exit");
			// it may still exit eventually if we just leave the shouldExit flag
			// set
		} else {
			Log.d(TAG, "serverThread join()ed ok");
			serverThread = null;
		}
		try {
			if (listenSocket != null) {
				Log.i(TAG, "Closing listenSocket");
				listenSocket.close();
			}
		} catch (IOException e) {
		}

		if (wifiLock != null) {
			Log.d(TAG, "onDestroy: Releasing wifi lock");
			wifiLock.release();
			wifiLock = null;
		}
		if (wakeLock != null) {
			Log.d(TAG, "onDestroy: Releasing wake lock");
			wakeLock.release();
			wakeLock = null;
		}

		if (mBridge != null) {
			mBridge.disconnect();
			unregisterReceiver(mWifiP2PReceiver);
		}

		Log.d(TAG, "FTPServerService.onDestroy() finished");
	}

	// This opens a listening socket on all interfaces.
	void setupListener() throws IOException {
		listenSocket = new ServerSocket();
		listenSocket.setReuseAddress(true);
		listenSocket.bind(new InetSocketAddress(FsSettings.getPortNumber()));
	}

	public void run() {
		Log.d(TAG, "Server thread running");

		//TODO: Replace this check! Otherwise everything may break...
//		if (isConnectedToLocalNetwork() == false) {
//			Log.w(TAG, "run: There is no local network, bailing out");
//			stopSelf();
//			sendBroadcast(new Intent(ACTION_FAILEDTOSTART));
//			return;
//		}

		// Initialization of wifi, set up the socket
		try {
			setupListener();
		} catch (IOException e) {
			Log.w(TAG, "run: Unable to open port, bailing out.");
			stopSelf();
			sendBroadcast(new Intent(ACTION_FAILEDTOSTART));
			return;
		}

		// @TODO: when using ethernet, is it needed to take wifi lock?
		takeWifiLock();
		takeWakeLock();

		// A socket is open now, so the FTP server is started, notify rest of
		// world
		Log.i(TAG, "Ftp Server up and running, broadcasting ACTION_STARTED");
		sendBroadcast(new Intent(ACTION_STARTED));
		mBridge.registerFTPService(listenSocket.getLocalPort());

		while (!shouldExit) {
			if (wifiListener != null) {
				if (!wifiListener.isAlive()) {
					Log.d(TAG, "Joining crashed wifiListener thread");
					try {
						wifiListener.join();
					} catch (InterruptedException e) {
					}
					wifiListener = null;
				}
			}
			if (wifiListener == null) {
				// Either our wifi listener hasn't been created yet, or has
				// crashed,
				// so spawn it
				wifiListener = new TcpListener(listenSocket, this);
				wifiListener.start();
			}
			try {
				// TODO: think about using ServerSocket, and just closing
				// the main socket to send an exit signal
				Thread.sleep(WAKE_INTERVAL_MS);
			} catch (InterruptedException e) {
				Log.d(TAG, "Thread interrupted");
			}
		}

		terminateAllSessions();

		if (wifiListener != null) {
			wifiListener.quit();
			wifiListener = null;
		}
		shouldExit = false; // we handled the exit flag, so reset it to
							// acknowledge
		Log.d(TAG, "Exiting cleanly, returning from run()");

		stopSelf();
		sendBroadcast(new Intent(ACTION_STOPPED));
		mBridge.unregisterFTPService();
	}

	private void terminateAllSessions() {
		Log.i(TAG, "Terminating " + sessionThreads.size()
				+ " session thread(s)");
		synchronized (this) {
			for (SessionThread sessionThread : sessionThreads) {
				if (sessionThread != null) {
					sessionThread.closeDataSocket();
					sessionThread.closeSocket();
				}
			}
		}
	}

	/**
	 * Takes the wake lock
	 * 
	 * Takes a PARTIAL_WAKE_LOCK. FULL_WAKE_LOCK is deprecated
	 */
	private void takeWakeLock() {
		if (wakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			Log.d(TAG, "takeWakeLock: Taking partial wake lock");
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
			wakeLock.setReferenceCounted(true);
		}
		wakeLock.acquire();
	}

	private void takeWifiLock() {
		Log.d(TAG, "takeWifiLock: Taking wifi lock");
		if (wifiLock == null) {
			WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			wifiLock = manager.createWifiLock(TAG);
			wifiLock.setReferenceCounted(true);
		}
		wifiLock.acquire();
	}

	/**
	 * Gets the local ip address
	 * 
	 * @return local ip adress or null if not found
	 */
	public static InetAddress getLocalInetAddress() {
		if (isConnectedToLocalNetwork() == false) {
			Log.e(TAG, "getLocalInetAddress called and no connection");
			return null;
		}
		// TODO: next if block could probably be removed
		if (isConnectedUsingWifi() == true) {
			Context context = FsApp.getAppContext();
			WifiManager wm = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			int ipAddress = wm.getConnectionInfo().getIpAddress();
			if (ipAddress == 0)
				return null;
			return Util.intToInet(ipAddress);
		}
		// This next part should be able to get the local ip address, but in
		// some case
		// I'm receiving the routable address

		if (isConnectedUsingWifiP2P() == true) {
			if (mWifiP2PAddress != null) {
				return mWifiP2PAddress;
			} else {
				Log.e(TAG, "WifiP2P Address was null!!");
				return null;
			}
		}

		// Potentially dangerous: can get all kinds on wrong addresses here....
		// try {
		// Enumeration<NetworkInterface> netinterfaces = NetworkInterface
		// .getNetworkInterfaces();
		// while (netinterfaces.hasMoreElements()) {
		// NetworkInterface netinterface = netinterfaces.nextElement();
		// Enumeration<InetAddress> adresses = netinterface
		// .getInetAddresses();
		// while (adresses.hasMoreElements()) {
		// InetAddress address = adresses.nextElement();
		// // this is the condition that sometimes gives problems
		// if (address.isLoopbackAddress() == false
		// && address.isLinkLocalAddress() == false)
		// return address;
		// }
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		return null;
	}

	/**
	 * Checks to see if we are connected to a local network, for instance wifi,
	 * wifi-direct or ethernet
	 * 
	 * @return true if connected to a local network
	 */
	public static boolean isConnectedToLocalNetwork() {
		boolean connected = false;
		Context context = FsApp.getAppContext();
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		// Is connected to a Wi-Fi AP?
		NetworkInfo ni = cm.getActiveNetworkInfo();
		connected = ni != null
				&& ni.isConnected() == true
				&& (ni.getType() & (ConnectivityManager.TYPE_WIFI | ConnectivityManager.TYPE_ETHERNET)) != 0;

		if (connected == false) {
			// Is a Wi-Fi AP?
			Log.d(TAG, "Device not connected to a network, see if it is an AP");
			WifiManager wm = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			try {
				Method method = wm.getClass().getDeclaredMethod(
						"isWifiApEnabled");
				connected = (Boolean) method.invoke(wm);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// Is connected to a Wi-Fi P2P Group?
		if (connected == false && isWifiP2PConnected == true) {
			Log.d(TAG, "It is connected to a Wifi P2P group");
			connected = true;
		}
		return connected;
	}

	/**
	 * Checks to see if we are connected using wifi
	 * 
	 * @return true if connected using wifi
	 */
	public static boolean isConnectedUsingWifi() {
		Context context = FsApp.getAppContext();
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnected() == true
				&& ni.getType() == ConnectivityManager.TYPE_WIFI;
	}

	public static boolean isConnectedUsingWifiP2P() {
		return isWifiP2PConnected;
	}

	/**
	 * All messages server<->client are also send to this call
	 * 
	 * @param incoming
	 * @param s
	 */
	public static void writeMonitor(boolean incoming, String s) {
	}

	/**
	 * The FTPServerService must know about all running session threads so they
	 * can be terminated on exit. Called when a new session is created.
	 */
	public void registerSessionThread(SessionThread newSession) {
		// Before adding the new session thread, clean up any finished session
		// threads that are present in the list.

		// Since we're not allowed to modify the list while iterating over
		// it, we construct a list in toBeRemoved of threads to remove
		// later from the sessionThreads list.
		synchronized (this) {
			List<SessionThread> toBeRemoved = new ArrayList<SessionThread>();
			for (SessionThread sessionThread : sessionThreads) {
				if (!sessionThread.isAlive()) {
					Log.d(TAG, "Cleaning up finished session...");
					try {
						sessionThread.join();
						Log.d(TAG, "Thread joined");
						toBeRemoved.add(sessionThread);
						sessionThread.closeSocket(); // make sure socket closed
					} catch (InterruptedException e) {
						Log.d(TAG, "Interrupted while joining");
						// We will try again in the next loop iteration
					}
				}
			}
			for (SessionThread removeThread : toBeRemoved) {
				sessionThreads.remove(removeThread);
			}

			// Cleanup is complete. Now actually add the new thread to the list.
			sessionThreads.add(newSession);
		}
		Log.d(TAG, "Registered session thread");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		Log.d(TAG, "user has removed my activity, we got killed! restarting...");
		Intent restartService = new Intent(getApplicationContext(),
				this.getClass());
		restartService.setPackage(getPackageName());
		PendingIntent restartServicePI = PendingIntent.getService(
				getApplicationContext(), 1, restartService,
				PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmService = (AlarmManager) getApplicationContext()
				.getSystemService(Context.ALARM_SERVICE);
		alarmService.set(AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime() + 2000, restartServicePI);
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		isWifiP2PConnected = info.groupFormed;
		if (isWifiP2PConnected) {
			Intent ipRequest = new Intent(
					Router.P2P_ReceiverActions.REQUEST_WIFIP2P_ADDRESS);
			sendBroadcast(ipRequest);
		} else {
			Log.e(TAG, "Wifi P2P is not connected");
		}
	}

	public void updateDeviceInfo(final WifiP2pDevice device) {
		Log.v(TAG, "updateDeviceInfo");
		mThisP2PDevice = device;
	}

	public void updateLocalP2PAddress(InetAddress address) {
		if (address != null) {
			Log.v(TAG, "updateLocalP2PAddress " + address.toString());
		}
		else {
			Log.e(TAG, "Wifi P2P Address was null");
		}
		mWifiP2PAddress = address;
		Intent updateLocalP2PAddressIntent = new Intent(ACTION_UPDATE_FTPSERVER);
		sendBroadcast(updateLocalP2PAddressIntent);
	}

}
