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

package meesters.wifip2p.app;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import meesters.wifip2p.connect.IP2PMessageReceiver;
import meesters.wifip2p.connect.P2PMessage;
import meesters.wifip2p.connect.PongP2PMessage;
import meesters.wifip2p.deps.AbstractP2PFrontendActivity;
import meesters.wifip2p.deps.IP2PMessage;
import meesters.wifip2p.deps.P2PServiceDescriptor;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.deps.WifiP2PReceiver;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class WifiP2PApp extends AbstractP2PFrontendActivity implements
		IP2PMessageReceiver {

	private static final String TAG = "WifiP2PApp";
	public static final String ACTION_P2PAPP = "meesters.wifip2p.app.WifiP2PApp";
	public static final String APP_ID = "WifiP2PApp";
	public static final String APP_UUID = "33ae1dc5-28c9-4a93-9e3a-2aa9fd471dd7";

	// private IP2PConnectorService mService = null;
	private GenericP2PBridge mServiceBridge = null;

	private int mDeviceRole = AbstractP2PFrontendActivity.PROVIDER;
	private boolean mWifiState = true;
	private String mConsumerLabel = "Provider";
	private String mWifiStateLabel;

	private TextView mIPAddressView = null;
	private TextView mConnStateView = null;
	private String mConnState = "";
	private TextView mActionStateView = null;
	private String mActionState = "";
	private TextView mDiscStateView = null;
	private String mDiscState = "";
	private TextView mGroupOwnerStateView = null;
	private boolean mGroupOwner = false;
	private TextView mDevicesView = null;
	private TextView mServicesView = null;

	private IntentFilter mFilter = null;
	private WifiP2PReceiver mReceiver = null;

	private ArrayList<WifiP2pDevice> mDevices = null;
	private HashSet<String> mDevicesMAC = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_p2_pbridge);
		init();
		Log.v(TAG, "onCreate");

		registerReceiver(mReceiver, mFilter);
		mServiceBridge.init();

	}

	private void init() {

		mIPAddressView = (TextView) findViewById(R.id.ip_address);
		mConnStateView = (TextView) findViewById(R.id.conn_state);
		mActionStateView = (TextView) findViewById(R.id.action_state);
		mDiscStateView = (TextView) findViewById(R.id.discovery_state);
		mGroupOwnerStateView = (TextView) findViewById(R.id.group_owner);

		mDevicesView = (TextView) findViewById(R.id.device_list);
		mServicesView = (TextView) findViewById(R.id.service_list);

		mDevices = new ArrayList<WifiP2pDevice>();
		mDevicesMAC = new HashSet<String>();

		mFilter = new IntentFilter();
		mReceiver = new WifiP2PReceiver(this);
		setUpIntentFilter();

		updateGroupOwner(false);
		mServiceBridge = new GenericP2PBridge(getApplicationContext());

		mWifiStateLabel = getString(R.string.wifi_enabled);

		Log.v(TAG, "init");
	}

	public void setUpIntentFilter() {
		Log.v(TAG, "setUpIntentFilter");
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_CONNECTED);
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_DISCONNECTED);
		mFilter.addAction(Router.P2P_ReceiverActions.DISCOVERY_STARTED);
		mFilter.addAction(Router.P2P_ReceiverActions.DISCOVERY_STOPPED);
		mFilter.addAction(Router.P2P_ReceiverActions.SCAN_START);
		mFilter.addAction(Router.P2P_ReceiverActions.SCAN_STOP);
		mFilter.addAction(Router.P2P_ReceiverActions.CONNECTING);
		mFilter.addAction(Router.P2P_ReceiverActions.DISCONNECTING);
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_CONNECTED);
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_DISCONNECTED);
		mFilter.addAction(Router.P2P_ReceiverActions.DISCOVERY_STARTED);
		mFilter.addAction(Router.P2P_ReceiverActions.DISCOVERY_STOPPED);
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_LIST_AVAILABLE);
		mFilter.addAction(Router.P2P_ReceiverActions.GROUP_OWNER);
		mFilter.addAction(Router.P2P_ReceiverActions.CONSUMER_STATE);
		mFilter.addAction(Router.P2P_ReceiverActions.STOP_ALL);
		mFilter.addAction(Router.P2P_ReceiverActions.UPDATE_SERVICES_VIEW);
		mFilter.addAction(Router.P2P_ReceiverActions.P2P_SERVICE_DESCRIPTOR);
		mFilter.addAction(Router.P2P_ReceiverActions.CLEAR_SERVICES_VIEW);
		mFilter.addAction(Router.P2P_ReceiverActions.BYTES_RECEIVED);
		mFilter.addAction(Router.P2P_ReceiverActions.GET_LOCALP2PADDRESS);

	}

	@Override
	public void updateIPAddress(InetAddress address) {
		Log.v(TAG, "updateConnState");
		if (address != null) {
			mIPAddressView.setText("IP Address: " + address.toString());
		} else {
			Log.e(TAG, "IP Address was null");
			mIPAddressView.setText("IP Address: ");
		}

	}

	@Override
	public void updateConsumerState(int deviceRole) {
		mDeviceRole = deviceRole;

		Button mConsumerButton = (Button) findViewById(R.id.set_consumer);
		mConsumerButton
				.setText(AbstractP2PFrontendActivity.deviceRoleState[mDeviceRole]);
	}

	@Override
	public void updateConnState(String text) {
		Log.v(TAG, "updateConnState");
		mConnState = text;
		mConnStateView.setText("Connection Status: " + text);
	}

	@Override
	public void updateActionState(String text) {
		Log.v(TAG, "updateActionState");
		mActionState = text;
		mActionStateView.setText("Action: " + text);
	}

	@Override
	public void updateDiscoverState(String text) {
		Log.v(TAG, "updateDiscoverState");
		mDiscState = text;
		mDiscStateView.setText("Discover: " + text);
	}

	@Override
	public void updateDevicesView(WifiP2pDeviceList list) {
		Log.v(TAG, "updateDevicesView");
		String devicesText = "Devices: ";

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
		for (WifiP2pDevice device : mDevices) {
			devicesText += device.deviceName + " " + device.deviceAddress
					+ "\n";
		}
		mDevicesView.setText(devicesText);
	}

	@Override
	public void updateGroupOwner(boolean owner) {
		Log.v(TAG, "updateGroupOwner");
		mGroupOwner = owner;
		mGroupOwnerStateView.setText(getString(R.string.group_owner)
				+ Boolean.toString(mGroupOwner));
	}

	@Override
	public void clearServicesView() {
		Log.v(TAG, "clearServicesView");
		mServicesView.setText("Services: ");
	}

	@Override
	public void updateServicesView(String text) {
		Log.v(TAG, "updateServicesView");
		String servicesText = mServicesView.getText().toString();
		mServicesView.setText(servicesText + "\n" + text);
	}

	@Override
	public void updateServicesView(P2PServiceDescriptor descriptor) {
		Log.v(TAG, "updateServicesView with P2PServiceDescriptor");

		String servicesString = mServicesView.getText().toString();

		servicesString += descriptor.fullDomainName + "\nOn "
				+ descriptor.srcDevice.deviceName + "\nExtras:"
				+ descriptor.extras.toString() + "\n\n";

		mServicesView.setText(servicesString);
	}

	@Override
	public void doUpdateServicesView(final String text) {
		Log.v(TAG, "doUpdateServicesView");
		mServicesView.getHandler().post(new Runnable() {
			@Override
			public void run() {
				String servicesText = mServicesView.getText().toString();
				mServicesView.setText(servicesText + "\n" + text);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "onPause");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "onDestroy");
		unregisterReceiver(mReceiver);
		mServiceBridge.disconnect();
	}

	public void onDoneButton(View v) {
		Log.v(TAG, "onDoneButton");
		setResult(Router.SUCCESS_WIFIP2PAPP);
		finish();
	}

	public void onConsumerButton(View v) {
		Button button = (Button) v;
		if (mDeviceRole == AbstractP2PFrontendActivity.CONSUMER) {
			// Toggle to Provider
			mConsumerLabel = getString(R.string.provider_state);
			mDeviceRole = AbstractP2PFrontendActivity.PROVIDER;
		} else if (mDeviceRole == AbstractP2PFrontendActivity.PROVIDER) {
			// Toggle to consumer
			mConsumerLabel = getString(R.string.consumer_state);
			mDeviceRole = AbstractP2PFrontendActivity.CONSUMER;
		}
		button.setText(mConsumerLabel);
		mServiceBridge.setDeviceRole(mDeviceRole);

	}

	public void onScanButton(View v) {

		mServiceBridge.getPeerList();
		Log.v(TAG, "onScanButton");
	}

	public void onConnectButton(View v) {

		mServiceBridge.connectPeer(0);
		Log.v(TAG, "onConnectButton");
	}

	public void onRemoveButton(View v) {

		Log.v(TAG, "onRemoveButton");
		// if (mConnState.equals(connStates[CONNECTED])) {
		mServiceBridge.resetConnection();
		// }
	}

	public void onStopButton(View v) {
		Log.v(TAG, "onStopButton");
		mServiceBridge.stopScanning();
	}

	public void onDiscoverButton(View v) {
		mServiceBridge.startDiscovery();
		Log.v(TAG, "onDiscoverButton");

	}

	public void onRegisterButton(View v) {
		Log.v(TAG, "onRegisterButton");
		mServiceBridge.registerService("app_service", "_app_p2pservice._tcp",
				9999);
	}

	public void onClearButton(View v) {
		clearServicesView();
		mServiceBridge.resetServices();
		Log.v(TAG, "onClearButton");
	}

	public void onCheckCacheButton(View v) {
		Log.v(TAG, "onCheckCacheButton");
		P2PServiceDescriptor[] descriptors = mServiceBridge
				.getCurrentP2PServices();

		if (descriptors != null) {
			for (P2PServiceDescriptor descriptor : descriptors) {
				updateServicesView(descriptor);
			}
		}

	}

	public void onStopServiceButton(View v) {
		Log.v(TAG, "onStopServiceButton");
		mServiceBridge.killService();
	}

	public void onStartServiceButton(View v) {
		Log.v(TAG, "onStartServiceButton");
		mServiceBridge.init();
	}

	public void onWifiToggle(View v) {
		Button button = (Button) v;
		if (!mWifiState) {
			// Toggle to Active
			mWifiStateLabel = getString(R.string.wifi_enabled);
			mWifiState = true;
		} else if (mWifiState) {
			// Toggle to Inactive
			mWifiStateLabel = getString(R.string.wifi_disabled);
			mWifiState = false;
		}
		button.setText(mWifiStateLabel);
		mServiceBridge.toggleWifi(mWifiState);
	}

	public String getActionState() {
		Log.v(TAG, "getActionState");
		return mActionState;
	}

	public String getConnState() {
		Log.v(TAG, "getConnState");
		return mConnState;
	}

	public String getDiscState() {
		Log.v(TAG, "getDiscState");
		return mDiscState;
	}

	@Override
	public void receiveP2PMessage(IP2PMessage msg) {
		if (msg != null) {
			if (msg instanceof P2PMessage) {

				P2PMessage p2pmsg = (P2PMessage) msg;
				updateServicesView("Message received: " + p2pmsg.getFromDev()
						+ ": " + new String(p2pmsg.getData()));
			} else if (msg instanceof PongP2PMessage) {
				PongP2PMessage p2pmsg = (PongP2PMessage) msg;
				updateServicesView("Message received: " + p2pmsg.getFromDev()
						+ ": " + new String(p2pmsg.getData()));
			} else {
				Log.d(TAG, "Message received was not of type P2PMessage");
			}
		} else {
			Log.e(TAG, "Message received was null");
			updateServicesView("Message received: null");
		}

	}
}
