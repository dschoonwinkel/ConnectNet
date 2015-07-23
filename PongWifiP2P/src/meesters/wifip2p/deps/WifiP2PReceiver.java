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

package meesters.wifip2p.deps;

import java.net.InetAddress;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.util.Log;

/**
 * This class implements the Android BroadcastReceiver abstract class, to enable
 * the manipulation of the WifiP2P GUI without the explcit function calling,
 * decoupling the P2PConnector Service from the WifiP2P Activity.
 * 
 * @author schoonwi
 * @version 1.0
 * @since 2014-11-17
 * 
 */
public class WifiP2PReceiver extends BroadcastReceiver {

	private AbstractP2PFrontendActivity mActivity = null;
	private static final String TAG = "WifiP2PReceiver";

	public WifiP2PReceiver(AbstractP2PFrontendActivity activity) {
		Log.v(TAG, "WifiP2PReceiver constructor");
		mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.v(TAG, "onReceive: " + action);

		switch (action) {

		case Router.P2P_ReceiverActions.SCAN_START:
			mActivity
					.updateActionState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.SCANNING]);
			break;

		case Router.P2P_ReceiverActions.SCAN_STOP:
			mActivity
					.updateActionState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.STOPPED_SCAN]);
			break;

		case Router.P2P_ReceiverActions.CONNECTING:
			mActivity
					.updateConnState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.CONNECTING]);
			break;

		case Router.P2P_ReceiverActions.DISCONNECTING:
			mActivity
					.updateConnState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.DISCONNECTING]);
			break;

		case Router.P2P_ReceiverActions.PEER_CONNECTED:
			mActivity
					.updateConnState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.CONNECTED]);
			break;

		case Router.P2P_ReceiverActions.PEER_DISCONNECTED:
			mActivity
					.updateConnState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.DISCONNECTED]);
			break;

		case Router.P2P_ReceiverActions.DISCOVERY_STARTED:
			mActivity
					.updateDiscoverState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.DISCOVERING]);
			break;

		case Router.P2P_ReceiverActions.DISCOVERY_STOPPED:
			mActivity
					.updateDiscoverState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.STOPPED_DISCOVER]);
			break;

		case Router.P2P_ReceiverActions.PEER_LIST_AVAILABLE:
			mActivity
					.updateDevicesView((WifiP2pDeviceList) intent
							.getParcelableExtra(Router.P2P_ReceiverActions.EXTRA_PEER_LIST));
			break;

		case Router.P2P_ReceiverActions.GROUP_OWNER:
			mActivity.updateGroupOwner((boolean) intent.getBooleanExtra(
					Router.P2P_ReceiverActions.EXTRA_GROUP_OWNER, false));
			break;

		case Router.P2P_ReceiverActions.CONSUMER_STATE:
			mActivity.updateConsumerState(intent.getIntExtra(
					Router.P2P_ReceiverActions.EXTRA_CONSUMER_STATE,
					AbstractP2PFrontendActivity.PROVIDER));
			break;

		case Router.P2P_ReceiverActions.STOP_ALL:
			mActivity
					.updateActionState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.IDLE]);
			break;

		case Router.P2P_ReceiverActions.UPDATE_SERVICES_VIEW:
			mActivity
					.updateServicesView(intent
							.getStringExtra(Router.P2P_ReceiverActions.EXTRA_SERVICES_VIEW));

			break;
			
		case Router.P2P_ReceiverActions.P2P_SERVICE_DESCRIPTOR:
			mActivity
					.updateServicesView((P2PServiceDescriptor)intent
							.getParcelableExtra(Router.P2P_ReceiverActions.EXTRA_P2P_SERVICE_DESCRIPTOR));

			break;

		case Router.P2P_ReceiverActions.CLEAR_SERVICES_VIEW:
			mActivity.clearServicesView();

			break;

		case Router.P2P_ReceiverActions.BYTES_RECEIVED:
			IP2PMessage msg = (IP2PMessage) intent
					.getParcelableExtra(Router.P2P_ReceiverActions.EXTRA_BYTES_RECEIVED);
			mActivity.receiveP2PMessage(msg);
			break;

		case Router.P2P_ReceiverActions.GET_LOCALP2PADDRESS:
			InetAddress address = (InetAddress) intent
					.getSerializableExtra(Router.P2P_ReceiverActions.EXTRA_GET_LOCALP2PADDRESS);
			mActivity.updateIPAddress(address);
			break;
		}
	}
}
