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

import java.util.HashMap;

import meesters.wifip2p.deps.AbstractP2PBridge;
import meesters.wifip2p.deps.AbstractP2PFrontendActivity;
import meesters.wifip2p.deps.IP2PConnectorService;
import meesters.wifip2p.deps.P2PServiceDescriptor;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.router.P2PConnectorService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class GenericP2PBridge extends AbstractP2PBridge {

	private static final String TAG = "GenericP2PBridge";

	private Context mContext = null;
	private boolean mBound = false;

	private int mDeviceRole = AbstractP2PFrontendActivity.CONSUMER;
	private IP2PConnectorService mService = null;

	public GenericP2PBridge(Context context) {
		mContext = context;
	}

	@Override
	public void init() {
		Log.v(TAG, "init");
		Intent p2pconnectorIntent = new Intent(
				P2PConnectorService.P2PCONNECTOR_SERVICE);
		mContext.startService(p2pconnectorIntent);
		mContext.bindService(p2pconnectorIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IP2PConnectorService.Stub.asInterface(service);
			Log.v(TAG, "Service connected");
			mBound = true;
			try {
				mService.getStates();
				mService.getPeerList();
				mService.startDiscovery();
				mService.getLocalP2PAddress();
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

	@Override
	public void disconnect() {
		Log.v(TAG, "disconnect");
		if (mBound) {
			mContext.unbindService(mServiceConnection);
			mBound = false;
		}
	}

	public void killService() {
		Log.v(TAG, "killService");
		if (mBound) {
			mContext.unbindService(mServiceConnection);
			mBound = false;
		}
		Intent p2pconnectorIntent = new Intent(
				P2PConnectorService.P2PCONNECTOR_SERVICE);
		mContext.stopService(p2pconnectorIntent);
	}

	public void getPeerList() {
		Log.v(TAG, "getPeerList");
		if (mService != null && mBound) {
			try {
				mService.getPeerList();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void connectPeer(int i) {
		Log.v(TAG, "connectPeer");
		if (mService != null && mBound) {
			try {
				mService.connectPeer(i);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void resetConnection() {
		Log.v(TAG, "resetConnection");
		if (mService != null && mBound) {
			try {
				mService.resetConnection();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void startDiscovery() {
		Log.v(TAG, "startDiscovery");
		if (mService != null && mBound) {
			try {
				mService.startDiscovery();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	public void stopScanning() {
		Log.v(TAG, "stopScanning");
		if (mService != null && mBound) {
			try {
				mService.stopDiscovery();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public P2PServiceDescriptor[] getCurrentP2PServices() {
		Log.v(TAG, "getCurrentP2PServices");
		if (mService != null && mBound) {
			try {
				return mService.getCurrentP2PServices();
			} catch (RemoteException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	public void registerService(String serviceName, String serviceType,
			int portnum) {
		Log.v(TAG, "registerServices");
		if (mService != null && mBound) {
			try {
				HashMap<String, String> extras = new HashMap<String, String>();
				extras.put("key", "value");
				if (mService.AdvertiseP2PService(serviceName, serviceType,
						portnum, extras) == -1) {
					Log.e(TAG, "Advertise service was unsuccessful");
				} else {
					Log.e(TAG,
							"Advertise service was successful as far as I know...");
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void resetServices() {
		Log.v(TAG, "resetServices");
		if (mService != null && mBound) {
			try {
				mService.resetServices();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void resetAll() {
		Log.v(TAG, "resetAll");
		if (mService != null && mBound) {
			try {
				mService.resetAll();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void setDeviceRole(int role) {
		Log.v(TAG, "setDeviceRole");
		mDeviceRole = role;
		Intent deviceRoleIntent = new Intent(
				Router.P2P_ReceiverActions.DEVICE_ROLE);
		deviceRoleIntent.putExtra(Router.P2P_ReceiverActions.EXTRA_DEVICE_ROLE,
				mDeviceRole);

		mContext.sendBroadcast(deviceRoleIntent);
	}

	public void toggleWifi(boolean wifiState) {
		Log.v(TAG, "toggleWifi");
		Intent toggleWifiIntent = new Intent(
				Router.P2P_ReceiverActions.TOGGLE_WIFI);
		toggleWifiIntent.putExtra(Router.P2P_ReceiverActions.EXTRA_TOGGLE_WIFI,
				wifiState);
		mContext.sendBroadcast(toggleWifiIntent);
	}

}
