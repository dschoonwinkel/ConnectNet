/*
 * Copyright 2015 Daniel Schoonwinkel

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package meesters.wifip2p.router;

import static meesters.wifip2p.deps.AbstractP2PFrontendActivity.CONSUMER;
import static meesters.wifip2p.deps.AbstractP2PFrontendActivity.PROVIDER;
import static meesters.wifip2p.deps.AbstractP2PFrontendActivity.deviceRoleState;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import meesters.wifip2p.connect.ClientThread;
import meesters.wifip2p.connect.ControlP2PMessage;
import meesters.wifip2p.connect.IClientConnectListener;
import meesters.wifip2p.connect.IP2PMessageReceiver;
import meesters.wifip2p.connect.IP2PMessageSender;
import meesters.wifip2p.connect.IServerSocketConnectListener;
import meesters.wifip2p.connect.P2PMessage;
import meesters.wifip2p.connect.PongP2PMessage;
import meesters.wifip2p.connect.ServerThread;
import meesters.wifip2p.connect.ServiceAdvertiserThread;
import meesters.wifip2p.connect.SocketResponseThread;
import meesters.wifip2p.deps.BaseP2PMessage;
import meesters.wifip2p.deps.GenericActionListener;
import meesters.wifip2p.deps.IP2PConnectorService;
import meesters.wifip2p.deps.IP2PMessage;
import meesters.wifip2p.deps.P2PServiceDescriptor;
import meesters.wifip2p.deps.Router;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * P2PConnector Service is the workhorse class of the P2P Framework. It is
 * outline by the IP2PConnectorService.aidl file, providing limited access to
 * the Service functionality. This service runs stand-alone until it is stopped
 * explicitly. This ensures that the Wifi P2P state is preserved between apps.
 */
public class P2PConnectorService extends Service implements PeerListListener,
		GroupInfoListener, ConnectionInfoListener, DnsSdTxtRecordListener,
		DnsSdServiceResponseListener, IServerSocketConnectListener,
		IClientConnectListener, IP2PMessageReceiver, IControlChannel {

	public static boolean isJUnitTesting = false;

	private static boolean continuallyScanP2P = false;

	// Static keys Service Advertising
	public static final String CONSUMERSTATE_KEY = "CONSUMER_STATE";
	public static final String CONNECTEDSTATE_KEY = "CONNECTED_STATE";
	public static final String P2PGENERIC_SERVICE_NAME = "GenericP2PService";
	public static final String P2PGENERIC_SERVICE_TYPE = "_genericp2p._tcp";

	/**
	 * P2PConnector Service starting Action String
	 */
	public static final String P2PCONNECTOR_SERVICE = "meesters.wifip2p.router.P2PConnectorService";

	/**
	 * ID used by the Notification Manager to create and remove Notifications of
	 * this Service
	 */
	public static final int P2PCONNECTORSERVICE_NOTIFICATION_ID = 111111;
	public static final int P2PCONNECTORSERVICE_NOTIFICATION_CONNECTED_ID = 111112;

	// Low-level WifiP2p structures: organizes hardware connections and service
	// discovery
	private WifiP2pManager mManager = null;
	private Channel mChannel = null;
	private P2PConnectorReceiver mReceiver = null;
	private IntentFilter mFilter = null;
	// Use this one if you are interested in receiving peer device updates
	private PeerReceiver mPeerReceiver = null;
	private IntentFilter mPeerFilter = null;

	private WifiP2pServiceRequest mServiceRequest = null;
	private ArrayList<WifiP2pDevice> mDevices = null;
	private HashSet<String> mDevicesMAC = null;
	private WifiP2pDevice thisDevice = null;

	// Transport layer connection variables: server and client socket threads
	// and P2P Service Advertising threads
	private int mRemotePort = -1;
	private boolean mReady = false;
	private InetAddress mRemoteAddress = null;
	private boolean mGroupOwner = false;
	private boolean mGroupFormed = false;
	private String mActionState = "";
	private int mDeviceRole = PROVIDER;

	private ServerThread mSThread = null;
	private ClientThread mCThread = null;
	private IP2PMessageSender mP2PMessageSender = null;

	private InetAddress mLocalWifiP2PIp = null;

	private ArrayList<ServiceAdvertiserThread> mServiceAdThreads = null;
	private int mGenericServiceAdNumber = -1;
	private ArrayList<P2PServiceDescriptor> mDiscoveredServices = null;

	private P2PWatchDogThread mWatchDogThread = null;
	private P2PConnectorService mThis = this;
	private WifiManager mWifiManager;
	private WifiLock mWifiLock;
	private NotificationManager mNotificationManager = null;

	private final static String TAG = "P2PConnectorService";

	@Override
	public void onCreate() {
		// onCreate is called only the first time the service is created,
		// even if there are more calls to startService
		Log.v(TAG, "onCreate");
		super.onCreate();

		mWatchDogThread = new P2PWatchDogThread(this, this);

		init();

	};

	/**
	 * Main setup of P2P Service. Setups up necessary objects, takes Wi-Fi lock,
	 * initializes WifiP2PManager, sets up P2PReceiver and Notification
	 */
	protected void init() {
		Log.v(TAG, "init");

		mServiceAdThreads = new ArrayList<ServiceAdvertiserThread>();
		mDiscoveredServices = new ArrayList<P2PServiceDescriptor>();

		mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		mWifiLock = mWifiManager.createWifiLock("Wifi Lock");
		mWifiLock.acquire();

		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(getApplicationContext(),
				getMainLooper(), new ChannelListener() {

					@Override
					public void onChannelDisconnected() {
						Log.e(TAG, "WifiP2P Channel disconnected");

					}
				});

		mFilter = new IntentFilter();
		mPeerFilter = new IntentFilter();

		setUpP2PIntentFilter();
		mReceiver = new P2PConnectorReceiver(mManager, mChannel, mThis);
		mPeerReceiver = new PeerReceiver(mManager, mChannel, mThis);

		mManager.setDnsSdResponseListeners(mChannel, mThis, mThis);

		mDevices = new ArrayList<WifiP2pDevice>();
		mDevicesMAC = new HashSet<String>();

		registerReceiver(mReceiver, mFilter);
		registerReceiver(mPeerReceiver, mPeerFilter);

		// Initial device role change, sets up everything needed for the
		// Provider/Consumer state
		onRoleChange(mDeviceRole);

		setUpNotification();

		deletePersistentGroup();

		// If you want this device to scan every X time-period. See
		// P2PScannerThread
		if (continuallyScanP2P && mWatchDogThread != null) {
			mWatchDogThread.startScannerThread();
		} else if (mWatchDogThread == null) {
			Log.e(TAG, "mWatchDogThread was null, this should not happen");
		}

	}

	private void setUpP2PIntentFilter() {
		Log.v(TAG, "setUpP2PIntentFilter");
		mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);

		mFilter.addAction(Router.P2P_ReceiverActions.BYTES_SEND);
		mFilter.addAction(Router.P2P_ReceiverActions.DEVICE_ROLE);
		mFilter.addAction(Router.P2P_ReceiverActions.TOGGLE_WIFI);
		mFilter.addAction(Router.P2P_ReceiverActions.REQUEST_WIFIP2P_ADDRESS);

		mFilter.addAction(Router.P2P_ServiceDebugActions.DEBUG_RESET_CONNECTION);
		mFilter.addAction(Router.P2P_ServiceDebugActions.DEBUG_START_DISCOVERY);
		mFilter.addAction(Router.P2P_ServiceDebugActions.DEBUG_STOP_DISCOVERY);

		mPeerFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// This is called many times, every time that the startService is called
		Log.v(TAG, "onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.v(TAG, "onBind");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}

	/**
	 * Kills the service: removes the notification, unregisters the receiver,
	 * resets threads, connection and services and releases Wi-Fi lock
	 */
	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		killService();
		super.onDestroy();

		// Stop the WatchdogThread
		if (mWatchDogThread != null) {
			mWatchDogThread.interrupt();
			mWatchDogThread = null;
		}
	}

	protected void killService() {
		// This function will be called when the service is stopped. It
		// unregisters the receiver and removes the notification

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// Cancel the started notification
		if (mNotificationManager != null) {
			mNotificationManager.cancel(P2PCONNECTORSERVICE_NOTIFICATION_ID);
		} else {
			Log.e(TAG, "Notification Manager was null");
		}

		// Unregister the receiver
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}

		if (mPeerReceiver != null) {
			unregisterReceiver(mPeerReceiver);
		}

		if (mWifiLock != null) {
			mWifiLock.release();
		}

		resetThreads();
		resetP2PConnection();
		resetP2PServices();

		deletePersistentGroup();
	}

	protected void startServiceAsRole(int deviceRole) {
		mDeviceRole = deviceRole;
		init();
	}

	private IBinder mBinder = new IP2PConnectorService.Stub() {

		/**
		 * Starts the peer discovery process. Will update the action state of
		 * all Action state receivers, then immediately return. The peerlist is
		 * returned when ready.
		 */
		@Override
		public void getPeerList() throws RemoteException {
			Log.v(TAG, "Stub: getPeerList");
			mManager.discoverPeers(mChannel, new GenericActionListener(
					"Peer discovery started"));
			Intent intent = new Intent(Router.P2P_ReceiverActions.SCAN_START);
			sendBroadcast(intent);
			mActionState = Router.P2P_ReceiverActions.SCAN_START;

			Log.i(TAG, "getPeerList()");

		}

		/**
		 * Manually connects a peer. This device takes responsibility to be
		 * Group Owner
		 * 
		 * @param peer
		 *            The peer number to connect to. Checks if the given number
		 *            is a valid peer in the peer list
		 */
		@Override
		public void connectPeer(int peer) throws RemoteException {
			Log.v(TAG, "Stub: connectPeer");
			if (mDevices.size() > peer) {
				String address = mDevices.get(peer).deviceAddress;
				connectToDevice(address, 15);
			}

		}

		@Override
		public void connectToAddress(String deviceAddress, int GOIntent)
				throws RemoteException {
			Log.v(TAG, "Stub: connectToAddress");
			connectToAddress(deviceAddress, GOIntent);

		}

		/**
		 * Starts service discovery. Results are returned in
		 * onDnsSdServiceAvailable and onDnsSdTxtRecordAvailable
		 */
		@Override
		public void startDiscovery() throws RemoteException {
			Log.v(TAG, "Stub: startDiscovery");
			discoverServices();
		}

		@Override
		public void stopDiscovery() throws RemoteException {
			Log.v(TAG, "Stub: stopDiscovery");
			mManager.stopPeerDiscovery(mChannel, new GenericActionListener(
					"Peer discovery stop"));
		}

		/**
		 * Resets the connection, i.e. disconnects from the Wi-Fi Direct GO and
		 * stops Server and Client Threads
		 */
		@Override
		public void resetConnection() throws RemoteException {
			Log.v(TAG, "Stub: resetConnection");

			resetP2PConnection();
			resetThreads();

		}

		/**
		 * Resets all the advertised P2P Services
		 */
		@Override
		public void resetServices() throws RemoteException {
			Log.v(TAG, "Stub: resetServices");
			resetP2PServices();

		}

		/**
		 * Resets the connection, communication threads and services
		 */
		@Override
		public void resetAll() throws RemoteException {
			Log.v(TAG, "Stub: resetAll");
			resetThreads();
			resetP2PConnection();
			resetP2PServices();
		}

		/**
		 * Sends a P2PMessage subclass over the communication channel
		 */
		@Override
		public int send(BaseP2PMessage msg) throws RemoteException {
			Log.v(TAG, "Stub: send");
			sendP2PMsg(msg);
			return msg.getMsgType();
		}

		/**
		 * Broadcasts the states of the P2PConnectorService. This includes Group
		 * Owner, Action, Connection, Discovery and Consumer/Provider states
		 */
		@Override
		public void getStates() throws RemoteException {
			Log.v(TAG, "getStates");
			// Broadcast states for a client late to the party

			Intent groupOwner = new Intent(
					Router.P2P_ReceiverActions.GROUP_OWNER);
			groupOwner.putExtra(Router.P2P_ReceiverActions.EXTRA_GROUP_OWNER,
					mGroupOwner);
			sendBroadcast(groupOwner);
			if (mActionState != "") {
				Intent actionState = new Intent(mActionState);
				sendBroadcast(actionState);
			}

			Intent consumerState = new Intent(
					Router.P2P_ReceiverActions.CONSUMER_STATE);
			consumerState.putExtra(
					Router.P2P_ReceiverActions.EXTRA_CONSUMER_STATE,
					mDeviceRole);
			sendBroadcast(consumerState);

			mReceiver.getStates();
		}

		/**
		 * Broadcasts details of this device
		 */
		@Override
		public void getThisP2pDevice() throws RemoteException {
			Log.v(TAG, "getThisP2pDevice");
			getThisDevice();
		}

		/**
		 * Fetches the Local P2P InetAddress of this device, assuming that it
		 * has been assigned. Returns a String representation of the
		 * InetAddress, and broadcasts the full InetAddress object.
		 * 
		 * @return ipaddress The String representation of the InetAddress.
		 *         Returns "" if the InetAddress is null
		 */
		@Override
		public String getLocalP2PAddress() throws RemoteException {
			Log.v(TAG, "getLocalP2PAddress");
			if (mLocalWifiP2PIp != null) {
				Intent ipAddress = new Intent(
						Router.P2P_ReceiverActions.GET_LOCALP2PADDRESS);
				ipAddress.putExtra(
						Router.P2P_ReceiverActions.EXTRA_GET_LOCALP2PADDRESS,
						mLocalWifiP2PIp);
				// Discard leading "/"
				return mLocalWifiP2PIp.toString().substring(1);
			} else {
				Log.e(TAG, "Local WifiP2P InetAddress was null");
				return "";
			}

		}

		/**
		 * Starts an {@link meesters.wifip2p.connect.ServiceAdvertiserThread}
		 * with the given parameters
		 * 
		 * @param service_name
		 *            Name of the WifiP2P Service
		 * @param service_type
		 *            Type and protocol of service eg. _generic_p2p._tcp
		 * @param portnum
		 *            The port number of the service
		 * @param extras
		 *            A map containing all of the information to be transmitted
		 *            in the DnsSdTxtRecord
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public int AdvertiseP2PService(String service_name,
				String service_type, int portnum, Map extras)
				throws RemoteException {
			// Assuming that the passed Map is correct
			return startServiceAdvertising(service_name, service_type, extras);

		}

		/**
		 * Remove a specific Service Advertiser Thread
		 * 
		 * @param service_name
		 *            Name of service to be removed. Currently only the
		 *            service_ad_number is used.
		 * @param service_ad_number
		 *            Index of service ad thread to be removed.
		 */
		@Override
		public boolean unAdvertiseP2PService(String service_name,
				int ServiceAdNumber) throws RemoteException {
			return removeServiceAdThread(ServiceAdNumber, service_name);
		}

		@Override
		public P2PServiceDescriptor[] getCurrentP2PServices()
				throws RemoteException {
			return mDiscoveredServices.toArray(new P2PServiceDescriptor[0]);
		}

	};

	// This sets up the general Service notification, needed to make the service
	// foreground
	public void setUpNotification() {
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotificationManager == null) {
			throw new RuntimeException("Notification manager is null!!");
		}
		mNotificationManager.cancel(P2PCONNECTORSERVICE_NOTIFICATION_ID);

		Notification.Builder notificationBuilder = null;

		Intent startWifiP2PApp = new Intent(Router.ACTION_P2PAPP);
		PendingIntent pendingIntent = PendingIntent.getActivity(
				getApplicationContext(), Router.REQUEST_WIFIP2PAPP,
				startWifiP2PApp, 0);

		notificationBuilder = new Notification.Builder(getApplicationContext())
				.setTicker(TAG).setSmallIcon(android.R.drawable.arrow_up_float)
				.setContentTitle(TAG).setContentText(TAG + " running")
				.setContentIntent(pendingIntent).setOngoing(true);

		Notification notification = notificationBuilder.build();
		if (notification == null) {
			throw new RuntimeException("Notificatin was null!!");
		}
		if (!isJUnitTesting)
			startForeground(P2PCONNECTORSERVICE_NOTIFICATION_ID, notification);
	}

	// This sets up the connected/not connected notification
	public void setUpNotification(boolean connected) {

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotificationManager == null) {
			throw new RuntimeException("Notification manager is null!!");
		}
		mNotificationManager
				.cancel(P2PCONNECTORSERVICE_NOTIFICATION_CONNECTED_ID);

		Notification.Builder notificationBuilder = null;

		if (connected) {
			notificationBuilder = new Notification.Builder(
					getApplicationContext()).setTicker(TAG)
					.setSmallIcon(android.R.drawable.ic_menu_mapmode)
					.setContentTitle(TAG).setContentText(TAG + " started")
					.setOngoing(true);
		} else {
			notificationBuilder = new Notification.Builder(
					getApplicationContext()).setTicker(TAG)
					.setSmallIcon(android.R.drawable.ic_menu_directions)
					.setContentTitle(TAG).setContentText(TAG + " started")
					.setOngoing(true);
		}

		mNotificationManager.notify(P2PCONNECTORSERVICE_NOTIFICATION_ID,
				notificationBuilder.build());
	}

	public void connectToDevice(String deviceAddress, int GOIntent) {
		Log.v(TAG, "connectToDevice: " + deviceAddress + " " + GOIntent);

		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		config.groupOwnerIntent = GOIntent;
		mManager.connect(mChannel, config, new GenericActionListener(
				"Connect to peer start"));
		Intent intent = new Intent(Router.P2P_ReceiverActions.CONNECTING);
		sendBroadcast(intent);
	}

	public void discoverServices() {

		if (mManager == null || mChannel == null || mDiscoveredServices == null) {
			Log.e(TAG, "Service is not ready to discover");
			return;
		}

		// Service discovery helper function, consider putting this in a thread

		// Clear the discovered services cache
		mDiscoveredServices.clear();

		// Clear services view before new discovery
		Intent clearServicesIntent = new Intent(
				Router.P2P_ReceiverActions.CLEAR_SERVICES_VIEW);
		sendBroadcast(clearServicesIntent);

		mManager.stopPeerDiscovery(mChannel, new GenericActionListener(
				"Stopping service discovery before retry"));
		// mManager.clearServiceRequests(mChannel, new GenericActionListener(
		// "Clear Service requests before discovery"));
		if (mServiceRequest != null) {
			mManager.removeServiceRequest(mChannel, mServiceRequest,
					new GenericActionListener("Remove mServiceRequest"));
		}
		mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
		if (mServiceRequest == null) {
			Log.e(TAG, "Service request was null");
		}
		mManager.addServiceRequest(mChannel, mServiceRequest,
				new GenericActionListener("Service request added"));
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// makeToastToUI("Discovery starting");
		mManager.discoverServices(mChannel, new GenericActionControlListener(
				"Discovery started", this));
	}

	public void stopDiscovery() {
		Log.v(TAG, "stopDiscovery");
		mManager.stopPeerDiscovery(mChannel, new GenericActionControlListener(
				"Peer discovery stop", this));
		// Clear service requests to stop service discovery
		mManager.clearServiceRequests(mChannel, new GenericActionListener(
				"Clear local service requests"));
	}

	public int startServiceAdvertising(String serviceName, String serviceType,
			Map<String, String> extras) {
		Log.v(TAG, "startServiceAdvertising with Extras: " + serviceName
				+ extras.toString());
		ServiceAdvertiserThread thread = new ServiceAdvertiserThread(mManager,
				mChannel, serviceName, serviceType, extras);
		thread.start();

		try {
			mServiceAdThreads.add(thread);
		} catch (ClassCastException | IllegalArgumentException e) {
			Log.e(TAG, "ServiceAd could not be added to List");
			return -1;
		}

		return mServiceAdThreads.size() - 1;
	}

	public int startServiceAdvertising(String serviceName, String serviceType,
			int port, String consumerState) {
		Log.v(TAG, "startServiceAdvertising with ConsumerState");
		ServiceAdvertiserThread thread = new ServiceAdvertiserThread(mManager,
				mChannel, serviceName, serviceType, port, consumerState);
		thread.start();

		try {
			mServiceAdThreads.add(thread);
		} catch (ClassCastException | IllegalArgumentException e) {
			Log.e(TAG, "ServiceAd could not be added to List");
			return -1;
		}

		return mServiceAdThreads.size() - 1;
	}

	public boolean removeServiceAdThread(int ServiceAdNumber,
			String ServiceAdName) {
		Log.v(TAG, "removeServiceAdThread: " + ServiceAdName);
		if (ServiceAdNumber == -1) {
			Log.e(TAG, "Invalid service ad number");
			return false;
		}

		if (mServiceAdThreads.size() > ServiceAdNumber) {

			try {
				ServiceAdvertiserThread thread = mServiceAdThreads
						.remove(ServiceAdNumber);
				if (thread == null) {
					Log.e(TAG, "ServiceAdThread was null");
					return false;
				}
				// Stop the thread
				thread.interrupt();
				return true;

			} catch (IndexOutOfBoundsException e) {
				Log.e(TAG, "Requested thread was out of bounds");
				return false;
			} catch (NullPointerException e) {
				Log.e(TAG, "Thread was null");
			}
		}

		return false;
	}

	public boolean removeAllServiceAds() {
		Log.v(TAG, "removeAllServiceAds");
		boolean success = true;

		if (mServiceAdThreads != null) {
			for (int i = 0; i < mServiceAdThreads.size(); i++) {
				success = success && removeServiceAdThread(i, "");
			}
		}
		return success;
	}

	public void resetThreads() {
		if (mSThread != null) {
			mSThread.interrupt();
		}

		if (mCThread != null) {
			mCThread.interrupt();
		}

		mSThread = null;
		mCThread = null;
	}

	public void resetP2PConnection() {
		if (mManager != null) {
			mManager.cancelConnect(mChannel, new GenericActionControlListener(
					"Cancel connect", this));

			mManager.stopPeerDiscovery(mChannel, new GenericActionListener(
					"Stopping peer discovery"));

			if (mGroupFormed) {
				mManager.removeGroup(mChannel, new GenericActionListener(
						"Leave group"));
			}
		}
	}

	public void resetP2PServices() {
		Log.v(TAG, "resetP2PServices");
		// if (mServiceAdThread != null) {
		// mServiceAdThread.interrupt();
		// }
		if (removeServiceAdThread(mGenericServiceAdNumber, "Generic service"))
			mGenericServiceAdNumber = -1;

		if (!removeAllServiceAds()) {
			Log.e(TAG, "An error occured removing all of the service ads");
		}

		if (mManager != null) {
			// mServiceAdThread = null;
			mManager.clearLocalServices(mChannel, new GenericActionListener(
					"Clearing local services"));
			mManager.removeServiceRequest(mChannel, mServiceRequest,
					new GenericActionListener("Remove mServiceRequeset"));
			mManager.clearServiceRequests(mChannel, new GenericActionListener(
					"Clearing local service requests"));
		}
	}

	@Override
	public void onDnsSdServiceAvailable(String instanceName,
			String registrationType, WifiP2pDevice srcDevice) {
		Log.v(TAG, "onDnsSdServiceAvailable");
		// Log.d(TAG, "Service " + instanceName + " available from "
		// + srcDevice.deviceName + " with " + registrationType.toString());
		//
		// Intent updateIntent = new Intent(
		// Router.P2P_ReceiverActions.UPDATE_SERVICES_VIEW);
		// updateIntent.putExtra(Router.P2P_ReceiverActions.EXTRA_SERVICES_VIEW,
		// "onDnsSdServiceAvailable\n" + "Service " + instanceName
		// + " available from " + srcDevice.deviceName + " with "
		// + registrationType.toString());
		// sendBroadcast(updateIntent);

	}

	@Override
	public void onDnsSdTxtRecordAvailable(String fullDomain,
			Map<String, String> record, WifiP2pDevice device) {
		Log.v(TAG, "onDnsSdTxtRecordAvailable");
		Log.d(TAG, "Service " + fullDomain + " available from "
				+ device.deviceName + " with " + record.toString());

		// P2PServiceDescriptor update services view
		P2PServiceDescriptor descriptor = new P2PServiceDescriptor(fullDomain,
				record, device);
		sendBroadcast(descriptor.getThisIntent());

		// Add the discovered service to the cache
		mDiscoveredServices.add(descriptor);

		// if (record.containsKey(Router.P2P_EXTRA_KEYS.PORTNUM_KEY)
		if (fullDomain.contains(P2PGENERIC_SERVICE_NAME.toLowerCase())
				&& record.containsKey(CONSUMERSTATE_KEY)) {

			// If this device is a Provider, connect to the Consumer
			if (mDeviceRole == PROVIDER
					&& record.get(CONSUMERSTATE_KEY).equals(
							deviceRoleState[CONSUMER])) {

				// If not connected to the device yet, connect now
				if (mReceiver.getConnState() != Router.P2P_ReceiverActions.PEER_CONNECTED) {
					// Connect to this device
					WifiP2pConfig config = new WifiP2pConfig();
					config.deviceAddress = device.deviceAddress;
					config.groupOwnerIntent = 0;

					mManager.connect(mChannel, config,
							new GenericActionControlListener("Connect start",
									this));
					Intent connectingIntent = new Intent(
							Router.P2P_ReceiverActions.CONNECTING);
					sendBroadcast(connectingIntent);

				}
				// If already connected to the device, open sockets to it
				else if (mReceiver.getConnState() == Router.P2P_ReceiverActions.PEER_CONNECTED) {
					int portnum = Integer.parseInt(record
							.get(Router.P2P_EXTRA_KEYS.PORTNUM_KEY));
					if (portnum > 1023) {
						if (mCThread == null || !mCThread.isAlive()
								|| mCThread.getSocket() == null
								|| !mCThread.getSocket().isConnected()) {
							Log.d(TAG, "Starting a new client thread");
							mCThread = new ClientThread(mThis, mRemoteAddress,
									portnum);
							mCThread.start();
							mRemotePort = portnum;
						} else {
							Log.d(TAG,
									"Client thread has already been started, and is connected");
						}
					} else {
						Log.e(TAG, "Could not open socket on advertised port: "
								+ Integer.toString(portnum));
					}
				}
			}

			// If this device is the Consumer and the Group Owner, add other
			// devices to the group : This almost never happens... I wonder if I
			// will ever use it
			// else if (mDeviceRole == CONSUMER && mGroupFormed && mGroupOwner)
			// {
			//
			// // Connect to this device
			// WifiP2pConfig config = new WifiP2pConfig();
			// config.deviceAddress = device.deviceAddress;
			// config.groupOwnerIntent = 15;
			//
			// mManager.connect(mChannel, config, new GenericActionListener(
			// "Connect"));
			// }
		}

	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		Log.v(TAG, "onConnectionInfoAvailable");
		if (info.groupFormed && info.isGroupOwner) {
			// Do server actions here
			updateGroupOwner(info.isGroupOwner);
			mGroupFormed = info.groupFormed;

			// When connected, stop listening for peer devices, only listen for
			// services
			if (mPeerReceiver != null) {
				unregisterReceiver(mPeerReceiver);
				mPeerReceiver = null;
			}

			Log.d(TAG, "Starting server Thread as GO");

			// Stop this triggering an automatic connect to server and change to
			// CONSUMER state. Only CONSUMERs should host a ServerThread
			// This odd case only happens if the device was previously the group
			// owner, but is not anymore
			if (mDeviceRole == CONSUMER) {

				if (mSThread == null || !mSThread.isAlive()) {
					Log.i(TAG, "Starting new Server Thread");

					mSThread = new ServerThread(this, info.groupOwnerAddress);
					mSThread.start();
				} else {
					Log.i(TAG,
							"Server Thread was started, client should connected automatically");
				}

			}

		} else if (info.groupFormed) {
			// // Do client actions here
			updateGroupOwner(info.isGroupOwner);
			mGroupFormed = info.groupFormed;
			mRemoteAddress = info.groupOwnerAddress;

			// When connected, stop listening for peer devices, only listen for
			// services
			if (mPeerReceiver != null) {
				unregisterReceiver(mPeerReceiver);
				mPeerReceiver = null;
			}

			// Start discovery again...
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					discoverServices();
				}

			}).start();
		}

		Log.i(TAG, info.toString());

	}

	@Override
	public void onServerSocketConnected() {
		Log.v(TAG, "onServerSocketConnected");
		// Advertise your service here(in a thread), with the correct port
		// number
		mRemotePort = mSThread.getSSocket().getLocalPort();
		mReady = true;

		if (removeServiceAdThread(mGenericServiceAdNumber, "Generic service"))
			mGenericServiceAdNumber = -1;
		mGenericServiceAdNumber = startServiceAdvertising(
				P2PGENERIC_SERVICE_NAME, P2PGENERIC_SERVICE_TYPE, mRemotePort,
				deviceRoleState[CONSUMER]);
		// }

	}

	@Override
	public void onSocketConnected(SocketResponseThread handler_thread) {
		Log.v(TAG, "onSocketConnected");

		mSThread.setP2PMessageReceiver(this);
		mP2PMessageSender = mSThread.getP2PMessageSender();
		if (getLocalWifiP2PIPAddress() != null) {
			Log.d(TAG, "Local Wifi P2P IP: "
					+ getLocalWifiP2PIPAddress().toString());
			mLocalWifiP2PIp = getLocalWifiP2PIPAddress();
			broadcastLocalP2PAddress();
		} else {
			Log.e(TAG, "Could not parse local IP");
		}
		Log.i(TAG, "onSocketConnected");
	}

	@Override
	public void onClientConnected() {
		Log.v(TAG, "onClientConnected");
		// Start the P2P Chat client here (client side)
		mCThread.setP2PMessageReceiver(this);
		mP2PMessageSender = mCThread.getP2PMessageSender();
		mReady = true;
		if (getLocalWifiP2PIPAddress() != null) {
			Log.d(TAG, "Local Wifi P2P IP:"
					+ getLocalWifiP2PIPAddress().toString());
			mLocalWifiP2PIp = getLocalWifiP2PIPAddress();
			broadcastLocalP2PAddress();
		} else {
			Log.e(TAG, "Could not parse local IP");
		}
		Log.i(TAG, "onClientConnected");
	}

	public void onRoleChange(int role) {
		Log.v(TAG, "onRoleChange");
		mDeviceRole = role;
		if (mDeviceRole == CONSUMER) {
			if (removeServiceAdThread(mGenericServiceAdNumber,
					"Generic Service"))
				mGenericServiceAdNumber = -1;

			mGenericServiceAdNumber = startServiceAdvertising(
					P2PGENERIC_SERVICE_NAME, P2PGENERIC_SERVICE_TYPE, -2,
					deviceRoleState[CONSUMER]);

			// Requestion Connection Info, which will check if it is connected.
			// If so, restart server thread
			// mManager.requestConnectionInfo(mChannel, mThis);

		} else if (mDeviceRole == PROVIDER) {
			// TODO: Stop server thread
			if (mSThread != null) {
				mSThread.interrupt();
				mSThread = null;
			}

			if (removeServiceAdThread(mGenericServiceAdNumber,
					"Generic Service"))
				mGenericServiceAdNumber = -1;

			mGenericServiceAdNumber = startServiceAdvertising(
					P2PGENERIC_SERVICE_NAME, P2PGENERIC_SERVICE_TYPE, -2,
					deviceRoleState[PROVIDER]);
			Log.e(TAG, "Get ready to connect to a different device");
			discoverServices();
		}
	}

	@Override
	public void receiveP2PMessage(IP2PMessage msg) {
		Log.v(TAG, "receiveP2PMessage");

		// First time pong message, to check if everything is working
		if (msg.getMsgType() == Router.MsgType.PONG_MESSAGE) {
			if (msg instanceof PongP2PMessage) {
				msg.setMsgType(Router.MsgType.PONG_ANSW_MESSAGE);
				sendP2PMsg(msg);

//				Intent received = new Intent(
//						Router.P2P_ReceiverActions.BYTES_RECEIVED);
//				received.putExtra(
//						Router.P2P_ReceiverActions.EXTRA_BYTES_RECEIVED,
//						(PongP2PMessage) msg);
//				sendBroadcast(received);
			}
		} else if (msg.getMsgType() == Router.MsgType.PONG_ANSW_MESSAGE) {
//			Intent received = new Intent(
//					Router.P2P_ReceiverActions.BYTES_RECEIVED);
//			received.putExtra(Router.P2P_ReceiverActions.EXTRA_BYTES_RECEIVED,
//					(PongP2PMessage) msg);
//			sendBroadcast(received);

		} else if (msg.getMsgType() == Router.MsgType.CONTROL_MESSAGE) {
			if (msg instanceof ControlP2PMessage) {
				reactToControlMessage(((ControlP2PMessage) msg)
						.getControlCommand());
			}
		} else if (msg instanceof P2PMessage) {
			Intent received = new Intent(
					Router.P2P_ReceiverActions.BYTES_RECEIVED);
			received.putExtra(Router.P2P_ReceiverActions.EXTRA_BYTES_RECEIVED,
					(P2PMessage) msg);
			sendBroadcast(received);
		} else {
			Log.e(TAG,
					"There was a problem with the received msg, incorrect type");
			// TODO: Insert capability for a Callback listener to handle other
			// messages
		}

	}

	public void sendP2PMsg(IP2PMessage msg) {
		if (mReady && mP2PMessageSender != null) {
			mP2PMessageSender.sendP2PMessage(msg);
		} else {
			Log.e(TAG, "Connection was not ready for sending!");
		}
	}

	public void getThisDevice() {
		Intent intent = new Intent(Router.P2P_ReceiverActions.GET_THISP2PDEVICE);
		intent.putExtra(Router.P2P_ReceiverActions.EXTRA_GET_THISP2PDEVICE,
				thisDevice);
		sendBroadcast(intent);
	}
	
	@Override
	public void onPeersAvailable(WifiP2pDeviceList list) {
		Log.v(TAG, "onPeersAvailable");
		mDevicesMAC.clear();
		mDevices.clear();
		Iterator<WifiP2pDevice> it = list.getDeviceList().iterator();
		while (it.hasNext()) {
			WifiP2pDevice device = it.next();
			if (!mDevicesMAC.contains(device.deviceAddress)) {
				mDevices.add(device);
				mDevicesMAC.add(device.deviceAddress);
			}
		}

		// Send the peerlist to all of the interested frontend Apps
		Intent peerListAvailable = new Intent(
				Router.P2P_ReceiverActions.PEER_LIST_AVAILABLE);
		peerListAvailable.putExtra(Router.P2P_ReceiverActions.EXTRA_PEER_LIST,
				list);
		sendBroadcast(peerListAvailable);

	}

	public InetAddress getLocalWifiP2PIPAddress() {

		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				String intfName = intf.getDisplayName();

				// Log.d(TAG, intf.getDisplayName());

				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()) {
						// Log.d(TAG, inetAddress.toString());
						// This is the correct conncted Wi-Fi P2P interface
						if (intfName.contains("p2p")) {
							broadcastLocalP2PAddress();
							return inetAddress;
						}
						// }
						// return inetAddress.getHostAddress().toString(); //
						// Galaxy Nexus returns IPv6
					}
				}
			}
		} catch (SocketException ex) {
			// Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		} catch (NullPointerException ex) {
			// Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		}
		return null;
	}

	/**
	 * This function is called when WifiP2p connectivity is lost
	 */
	public void onP2PDisconnected() {
		mLocalWifiP2PIp = null;
		if (mCThread != null) {
			mCThread.interrupt();
		}

		// Start searching for peers again
		if (mPeerFilter != null) {
			registerReceiver(mPeerReceiver, mPeerFilter);
		}
	}

	
	public void reactToControlMessage(int controlCommand) {

		switch (controlCommand) {
		case Router.ControlCodes.STOP_REMOTE_DISCOVERY:
			stopDiscovery();
			break;
		}
	}

	public void sendControlMessage(int controlCommand) {
		ControlP2PMessage msg = new ControlP2PMessage(controlCommand);
		sendP2PMsg(msg);
	}

	@Override
	public void status(String message, int state) {
		// This function catches errors from the discovery process
		makeToastToUI(message + ": " + GenericActionListener.onErrorCode(state));

		if (state == WifiP2pManager.NO_SERVICE_REQUESTS) {
			Log.e(TAG, "Oh no, it happened again");
			mWatchDogThread.startRestartThread(mDeviceRole);

		}

	}

	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean checkConnectivity() {
		Log.v(TAG, "checkConnectivity");
		if (mReceiver.getConnState() != Router.P2P_ReceiverActions.PEER_CONNECTED) {
			Log.e(TAG, "Wifi Direct not connected");
			return false;
		}
		// This is a client
		if (!mGroupOwner) {
			if (mCThread == null) {
				Log.e(TAG, "Client thread null");
				return false;
			}
			if (mCThread.getSocket() == null) {
				Log.e(TAG, "Socket is null");
				return false;
			}
			if (!mCThread.getSocket().isConnected()) {
				Log.e(TAG, "Socket is not connected");
				return false;
			}
			if (mCThread.getSocket().isClosed()) {
				Log.e(TAG, "Socket is closed");
			}
			return true;
		}
		// This is the server
		else {
			if (mSThread == null) {
				Log.e(TAG, "Server thread null");
				return false;
			}
			// To many client sockets to test them all.
			return true;
		}

	}
	
	public void updateGroupOwner(boolean owner) {
		Log.v(TAG, "updateGroupOwner");
		Intent intent = new Intent(Router.P2P_ReceiverActions.GROUP_OWNER);
		intent.putExtra(Router.P2P_ReceiverActions.EXTRA_GROUP_OWNER, owner);
		mGroupOwner = owner;
		sendBroadcast(intent);
	}

	/* Seldom used or changed functions */

	public void toggleWifi(boolean state) {
		Log.v(TAG, "toggleWifi");
		mWifiManager.setWifiEnabled(state);
	}
	
	public void broadcastLocalP2PAddress() {
		Intent intent = new Intent(
				Router.P2P_ReceiverActions.GET_LOCALP2PADDRESS);
		intent.putExtra(Router.P2P_ReceiverActions.EXTRA_GET_LOCALP2PADDRESS,
				mLocalWifiP2PIp);
		// Send sticky broadcast, so that everyone can access it when they need
		// it : this was decided against, due to ambiguity when connected
		sendBroadcast(intent);
	}
	
	public void updateDeviceInfo(WifiP2pDevice device) {
		// This is the MAC address - not the IP Address. Useful for connecting,
		// but not sending message
		Log.v(TAG, "updateDeviceInfo: Address" + device.deviceAddress);
		thisDevice = device;
	}
	
	@SuppressWarnings("rawtypes")
	public void deletePersistentGroup() {
		Log.v(TAG, "deletePersistentGroup");
		try {

			Class persistentInterface = null;

			// Iterate and get class PersistentGroupInfoListener
			for (Class<?> classR : WifiP2pManager.class.getDeclaredClasses()) {
				if (classR.getName().contains("PersistentGroupInfoListener")) {
					persistentInterface = classR;
					break;
				}

			}

			final Method deletePersistentGroupMethod = WifiP2pManager.class
					.getDeclaredMethod("deletePersistentGroup", new Class[] {
							Channel.class, int.class, ActionListener.class });

			// anonymous class to implement PersistentGroupInfoListener which
			// has a method, onPersistentGroupInfoAvailable
			Object persitentInterfaceObject = java.lang.reflect.Proxy
					.newProxyInstance(persistentInterface.getClassLoader(),
							new java.lang.Class[] { persistentInterface },
							new java.lang.reflect.InvocationHandler() {
								@Override
								public Object invoke(Object proxy,
										java.lang.reflect.Method method,
										Object[] args)
										throws java.lang.Throwable {
									String method_name = method.getName();

									if (method_name
											.equals("onPersistentGroupInfoAvailable")) {
										Class wifiP2pGroupListClass = Class
												.forName("android.net.wifi.p2p.WifiP2pGroupList");
										Object wifiP2pGroupListObject = wifiP2pGroupListClass
												.cast(args[0]);

										@SuppressWarnings("unchecked")
										Collection<WifiP2pGroup> wifiP2pGroupList = (Collection<WifiP2pGroup>) wifiP2pGroupListClass
												.getMethod("getGroupList",
														(Class[]) null).invoke(
														wifiP2pGroupListObject,
														null);
										for (WifiP2pGroup group : wifiP2pGroupList) {
											deletePersistentGroupMethod
													.invoke(mManager,
															mChannel,
															(Integer) WifiP2pGroup.class
																	.getMethod(
																			"getNetworkId")
																	.invoke(group,
																			null),
															new ActionListener() {
																@Override
																public void onSuccess() {
																	// All
																	// groups
																	// deleted
																}

																@Override
																public void onFailure(
																		int i) {

																}
															});
										}
									}

									return null;
								}
							});

			Method requestPersistentGroupMethod = WifiP2pManager.class
					.getDeclaredMethod("requestPersistentGroupInfo",
							new Class[] { Channel.class, persistentInterface });

			requestPersistentGroupMethod.invoke(mManager, mChannel,
					persitentInterfaceObject);

		} catch (Exception ex) {
			ex.printStackTrace();
			Log.e(TAG,
					"Something terrible has happened, except if it was by the Samsung... nobody cares about the Samsung... :P");
		}
	}

	
	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		// Log.v(TAG, "onGroupInfoAvailable"); //Nothing happens here...
		Log.v(TAG, "Passphrase: " + group.getPassphrase());

	}

	public void makeToastToUI(final String text) {

		Handler handler = new Handler(Looper.getMainLooper());

		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), text,
						Toast.LENGTH_SHORT).show();
			}
		});
	}
	
//	public static String MACBytesToString(byte[] mac) {
//	Log.d(TAG, "pre process: " + mac.toString());
//	StringBuilder sb = new StringBuilder(18);
//	for (byte b : mac) {
//		if (sb.length() > 0)
//			sb.append(':');
//		sb.append(String.format("%02x", b));
//	}
//	return sb.toString();
//}
}
