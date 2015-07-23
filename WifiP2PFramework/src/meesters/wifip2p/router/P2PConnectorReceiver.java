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

import meesters.wifip2p.deps.IP2PMessage;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.deps.WifiP2PReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.text.format.Time;
import android.util.Log;

/**
 * This class implements the Android BroadcastReceiver abstract class, and
 * handles all of the network related work in conjunction with the
 * P2PConnectorService. It also broadcasts intents for the WifiP2PReceiver to
 * update the WifiP2P Activity frontend.
 * 
 * @see WifiP2PReceiver
 * @author schoonwi
 * @version 1.0
 * @since 2014-11-17
 * 
 */
public class P2PConnectorReceiver extends BroadcastReceiver {

	private static final String TAG = "WifiDirectBroadcastReceiver";
	private WifiP2pManager mManager = null;
	private Channel mChannel = null;
	private P2PConnectorService mService;

	private Time mPrevPeerUpdateTime = new Time();
	private Time mTimeNow = new Time();

	private String mConnState = "";
	private String mDiscState = "";

	public P2PConnectorReceiver(WifiP2pManager manager, Channel channel,
			P2PConnectorService service) {
		Log.v(TAG, "WifiDirectBroadcastReceiver constructor");
		mManager = manager;
		mChannel = channel;
		mService = service;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.v(TAG, "onReceive: " + action);

		switch (action) {
		case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
			P2PStateChanged(context, intent);
			break;
		case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
			P2PPeersChanged(context, intent);
			break;

		case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
			P2PConnectionChanged(context, intent);
			break;
		case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
			P2PDiscoveryChanged(context, intent);
			break;

		case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
			P2PDeviceChanged(context, intent);
			break;

		case Router.P2P_ReceiverActions.BYTES_SEND:
			bytesSend(context, intent);
			break;

		case Router.P2P_ServiceDebugActions.DEBUG_RESET_CONNECTION:
			debugResetConnection(context, intent);
			break;

		case Router.P2P_ServiceDebugActions.DEBUG_START_DISCOVERY:
			debugStartDiscovery();
			break;

		case Router.P2P_ServiceDebugActions.DEBUG_STOP_DISCOVERY:
			debugStopDiscovery();
			break;

		case Router.P2P_ReceiverActions.DEVICE_ROLE:
			deviceRoleChanged(context, intent);
			break;

		case Router.P2P_ReceiverActions.TOGGLE_WIFI:
			toggleWifi(context, intent);
			break;

		case Router.P2P_ReceiverActions.REQUEST_WIFIP2P_ADDRESS:
			requestWifiP2PAddress();
			break;

		}

	}

	private void P2PStateChanged(Context context, Intent intent) {
		int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
		if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
			Log.d(TAG, "Wifi P2P enabled");
		} else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
			Log.d(TAG, "Wifi P2P disabled");
		}
	}

	private void P2PPeersChanged(Context context, Intent intent) {
		Log.i(TAG, "Peers Changed");

		// Check if enough time has passed to warrant a new peerRequest
		mTimeNow.setToNow();
		long timeDifference = mTimeNow.toMillis(true)
				- mPrevPeerUpdateTime.toMillis(true);
		// Log.d(TAG, "Time difference" + timeDifference/1000);
		mPrevPeerUpdateTime.setToNow();

		//Temporarily taking this out
//		if (timeDifference / 1000 > 10) {
//			mManager.requestPeers(mChannel, mService);
//		} else {
//			Log.d(TAG, "Did not request peers, too soon: " + timeDifference
//					/ 1000);
//		}
		
	}

	private void P2PConnectionChanged(Context context, Intent intent) {
		Log.i(TAG, "Connection changed");
		NetworkInfo info = (NetworkInfo) intent
				.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

		if (info.isConnected()) {
			Log.d(TAG, "one or more peers connected");
			mManager.requestConnectionInfo(mChannel, mService);

			Intent peerConnected = new Intent(
					Router.P2P_ReceiverActions.PEER_CONNECTED);
			mService.sendBroadcast(peerConnected);
			mConnState = Router.P2P_ReceiverActions.PEER_CONNECTED;
			// Set the notification to display a connected symbol
			mService.setUpNotification(true);
			// mActivity.updateConnState(WifiP2P.connStates[WifiP2P.CONNECTED]);

		} else {
			Log.d(TAG, "one or more peers disconnected");

			Intent peerDisconnected = new Intent(
					Router.P2P_ReceiverActions.PEER_DISCONNECTED);
			mService.sendBroadcast(peerDisconnected);
			mService.onP2PDisconnected();
			mConnState = Router.P2P_ReceiverActions.PEER_DISCONNECTED;
			// Set the notification to display a disconnected symbol
			mService.setUpNotification(false);
			// mActivity.updateConnState(WifiP2P.connStates[WifiP2P.DISCONNECTED]);
		}
	}

	private void P2PDeviceChanged(Context context, Intent intent) {
		Log.v(TAG, "wifiP2PThisDeviceChanged");
		WifiP2pDevice device = intent
				.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
		// Log.i(TAG, "Wifi P2P This device Changed: " + device.toString());
		mService.updateDeviceInfo(device);
	}

	private void P2PDiscoveryChanged(Context context, Intent intent) {
		Log.i(TAG, "P2P Discovery state changed");
		switch (intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)) {
		case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
			Log.i(TAG, "Discovery started");

			Intent discoveryStarted = new Intent(
					Router.P2P_ReceiverActions.DISCOVERY_STARTED);
			mService.sendBroadcast(discoveryStarted);
			mDiscState = Router.P2P_ReceiverActions.DISCOVERY_STARTED;

			// mActivity.updateDiscoverState(WifiP2P.connStates[WifiP2P.DISCOVERING]);
			break;
		case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
			Log.i(TAG, "Discovery stopped");

			Intent discoveryStopped = new Intent(
					Router.P2P_ReceiverActions.DISCOVERY_STOPPED);
			mService.sendBroadcast(discoveryStopped);
			mDiscState = Router.P2P_ReceiverActions.DISCOVERY_STOPPED;
			// mActivity.updateDiscoverState(WifiP2P.connStates[WifiP2P.STOPPED_DISCOVER]);
			break;
		}
	}

	private void deviceRoleChanged(Context context, Intent intent) {
		Log.v(TAG, "onDeviceRole");

		int deviceRole = intent.getIntExtra(
				Router.P2P_ReceiverActions.EXTRA_DEVICE_ROLE, 0);
		mService.onRoleChange(deviceRole);
	}

	private void bytesSend(Context context, Intent intent) {
		Log.i(TAG, "on BYTES_SEND");
		IP2PMessage msg = (IP2PMessage) intent
				.getParcelableExtra(Router.P2P_ReceiverActions.EXTRA_BYTES_SEND);

		// Change it to a marshalable Parcel, for sending byte[]

		if (msg != null) {
			mService.checkConnectivity();
			mService.sendP2PMsg(msg);
		} else if (msg == null) {
			Log.e(TAG, "Something went terribly wrong...");
		}
	}

	private void toggleWifi(Context context, Intent intent) {
		Log.v(TAG, "onToggleWifi");

		boolean wifiState = intent.getBooleanExtra(
				Router.P2P_ReceiverActions.EXTRA_TOGGLE_WIFI, true);
		mService.toggleWifi(wifiState);
	}

	private void requestWifiP2PAddress() {
		Log.v(TAG, "onRequestLocalP2PAddress");
		mService.broadcastLocalP2PAddress();
	}

	private void debugResetConnection(Context context, Intent intent) {
		Log.v(TAG, "onDebugResetConnection");
		mService.resetP2PConnection();
		mService.resetThreads();
	}

	private void debugStartDiscovery() {
		Log.v(TAG, "onDebugStartDiscovery");
		mService.discoverServices();
	}

	private void debugStopDiscovery() {
		Log.v(TAG, "onDebugStopDiscovery");
		mService.stopDiscovery();
		mService.sendControlMessage(Router.ControlCodes.STOP_REMOTE_DISCOVERY);
	}

	void getStates() {
		Log.i(TAG, "getStates");
		if (mConnState != "") {
			Intent connState = new Intent(mConnState);
			mService.sendBroadcast(connState);
		}
		if (mDiscState != "") {
			Intent discState = new Intent(mDiscState);
			mService.sendBroadcast(discState);
		}
	}

	String getConnState() {
		return mConnState;
	}
}
