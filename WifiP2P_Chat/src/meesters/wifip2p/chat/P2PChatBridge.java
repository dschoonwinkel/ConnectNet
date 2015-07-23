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

import meesters.wifip2p.connect.P2PMessage;
import meesters.wifip2p.deps.IP2PConnectorService;
import meesters.wifip2p.deps.Router;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class P2PChatBridge extends AbstractP2PBridge {

	private Context mContext = null;
	private IP2PConnectorService mService = null;
	private boolean mBound = false;
	private boolean mExpectedDisconnect = false;

	private static final String TAG = "P2PChatBridge";

	public P2PChatBridge(Context context) {
		Log.v(TAG, "P2PChatBridge constructor");
		mContext = context;
	}

	@Override
	public void init() {
		Log.v(TAG, "init");
		Intent p2pconnectorIntent = new Intent(
				Router.ACTION_P2P_SERVICE);
		// startService(p2pconnectorIntent);
		mContext.bindService(p2pconnectorIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void disconnect() {
		Log.v(TAG, "disconnect");
		mContext.unbindService(mServiceConnection);
		mExpectedDisconnect = true;
	}

	public void getStates() {
		Log.v(TAG, "getStates");
		if (mBound) {
			try {
				mService.getStates();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMessageToPeer(int i, String APP_ID, String APP_UUID, int msgType, WifiP2pDevice toDev, byte[] data) {
		Log.v(TAG, "sendMessageToPeer");
		// Complete method stub
		Log.i(TAG, "sendMessageToPeer" + new String(data));
		P2PMessage msg = new P2PMessage(APP_ID, APP_UUID, msgType, Build.MODEL, toDev, data.length, data);
		
		if (mBound) {
			Log.i(TAG, "sending message...");
			Intent intent = new Intent(Router.P2P_ReceiverActions.BYTES_SEND);
			intent.putExtra(Router.P2P_ReceiverActions.EXTRA_BYTES_SEND, msg);
			mContext.sendBroadcast(intent);

			// try {
			// mService.send(msg.getBytes());
			// } catch (RemoteException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
		} else {
			Log.e(TAG, "Not Bound to Service");
		}
	}

	public void requestPeers() {
		Log.v(TAG, "requestPeers");
		try {
			if (mBound) {
				mService.getPeerList();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IP2PConnectorService.Stub.asInterface(service);
			Log.v(TAG, "Service connected");
			mBound = true;
			try {
				mService.getStates();
				Log.i(TAG, "gettingStates");
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (mExpectedDisconnect) {
				Log.v(TAG, "Service has disconnected");
			} else {
				Log.e(TAG, "Service has unexpectedly disconnected");
			}
			mBound = false;
			mService = null;

		}
	};

}
