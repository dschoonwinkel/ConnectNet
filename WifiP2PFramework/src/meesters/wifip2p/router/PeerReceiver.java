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

import meesters.wifip2p.deps.WifiP2PReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
public class PeerReceiver extends BroadcastReceiver {

	private static final String TAG = "PeerReceiver";
	private WifiP2pManager mManager = null;
	private Channel mChannel = null;
	private P2PConnectorService mService;

	private Time mPrevPeerUpdateTime = new Time();
	private Time mTimeNow = new Time();

	private String mConnState = "";
	private String mDiscState = "";

	public PeerReceiver(WifiP2pManager manager, Channel channel,
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
		case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
			P2PPeersChanged(context, intent);
			break;
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

		if (timeDifference / 1000 > 10) {
			mManager.requestPeers(mChannel, mService);
		} else {
			Log.d(TAG, "Did not request peers, too soon: " + timeDifference
					/ 1000);
		}
	}
}
