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

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class BaseP2PBridge extends AbstractP2PBridge {

	// These variable are set to protected, so that subclasses can access them
	protected Context mContext = null;
	protected IP2PConnectorService mService = null;
	protected boolean mBound = false;

	protected ArrayList<Runnable> mOnConnectedStack = null;
	private final static String TAG = "BaseP2PBridge";

	public BaseP2PBridge() {
		mOnConnectedStack = new ArrayList<Runnable>();
	}

	@Override
	public void init() {
		Log.v(TAG, "init");
		Intent p2pconnectorIntent = new Intent(Router.ACTION_P2P_SERVICE);
		mContext.bindService(p2pconnectorIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE);

	}

	public void postOnConnectedStack(Runnable runnable) {

		if (mBound) {
			runnable.run();
		} else {
			Log.e(TAG,
					"Service not bound yet, posting to the OnConnected execution stack");
			mOnConnectedStack.add(runnable);
		}
	}

	@Override
	public void disconnect() {
		Log.v(TAG, "disconnect");
		if (mBound) {
			mContext.unbindService(mServiceConnection);
			mBound = false;
		}

	}

	protected ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IP2PConnectorService.Stub.asInterface(service);
			Log.v(TAG, "Service connected");
			mBound = true;

			// Run runnables
			for (Runnable runnable : mOnConnectedStack) {
				runnable.run();
			}
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
			Log.e(TAG, "Service has unexpectedly disconnected");
			mBound = false;
			mService = null;

		}
	};

}
