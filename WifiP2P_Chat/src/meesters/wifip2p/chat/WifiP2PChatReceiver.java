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

package meesters.wifip2p.chat;

import meesters.wifip2p.app.AbstractP2PFrontendActivity;
import meesters.wifip2p.connect.P2PMessage;
import meesters.wifip2p.deps.Router;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WifiP2PChatReceiver extends BroadcastReceiver {

	private WifiP2PChat mActivity = null;
	private static final String TAG = "WifiP2PChatReceiver";

	public WifiP2PChatReceiver(WifiP2PChat activity) {
		mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.v(TAG, "onReceive: " + action);
		switch (action) {
		case Router.P2P_ReceiverActions.BYTES_RECEIVED:
			Object msg_obj = intent
					.getParcelableExtra(Router.P2P_ReceiverActions.EXTRA_BYTES_RECEIVED);
			if (msg_obj instanceof P2PMessage) {
				P2PMessage msg = (P2PMessage) msg_obj;
				byte[] data = msg.getData();
				String fromDev = msg.getFromDev();
				Log.i(TAG, "receivedMsg: " + fromDev + ": " + new String(data));
				mActivity.onReceiveMsg(msg);
			}

			break;

		case Router.P2P_ReceiverActions.PEER_CONNECTED:
			mActivity
					.updateConnState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.CONNECTED]);
			break;

		case Router.P2P_ReceiverActions.PEER_DISCONNECTED:
			mActivity
					.updateConnState(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.DISCONNECTED]);
			break;
		}
	}

}
