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

package meesters.wifip2p.connect;

import java.util.HashMap;
import java.util.Map;

import meesters.wifip2p.deps.GenericActionListener;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.router.P2PConnectorService;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

public class ServiceAdvertiserThread extends Thread {

	private WifiP2pManager mManager = null;
	private Channel mChannel = null;
	private String mServiceName = null;
	private String mServiceType = null;
	private Map<String, String> mExtras = null;
	private WifiP2pDnsSdServiceInfo mServiceInfo = null;
	private static final String TAG = "ServiceAdvertiserThread";

	public ServiceAdvertiserThread(WifiP2pManager manager, Channel channel,
			String serviceName, String serviceType, Map<String, String> extra) {
		Log.v(TAG, "ServiceAdvertiserThread constructor");
		mManager = manager;
		mChannel = channel;
		mServiceName = serviceName;
		mServiceType = serviceType;
		mExtras = extra;
	}

	public ServiceAdvertiserThread(WifiP2pManager manager, Channel channel,
			String serviceName, String serviceType, int portnum, String ConsumerState) {
		Log.v(TAG, "ServiceAdvertiserThread constructor");
		mManager = manager;
		mChannel = channel;
		mServiceName = serviceName;
		mServiceType = serviceType;
		HashMap<String, String> extra = new HashMap<String, String>();
		extra.put(Router.P2P_EXTRA_KEYS.PORTNUM_KEY, Integer.toString(portnum));
		extra.put(P2PConnectorService.CONSUMERSTATE_KEY, ConsumerState);
		mExtras = extra;
	}

	@Override
	public void run() {
		Log.v(TAG, "run");

		mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(mServiceName,
				mServiceType, mExtras);
		mManager.addLocalService(mChannel, mServiceInfo,
				new GenericActionListener("Start Service Ad: " + mServiceName));

		Log.i(TAG, "ServiceAdvertiser finished");

	}

	public WifiP2pDnsSdServiceInfo getServiceInfo() {
		Log.v(TAG, "getServiceInfo");
		return mServiceInfo;
	}

	@Override
	public void interrupt() {
		super.interrupt();
		if (mChannel != null && mServiceInfo != null) {
			mManager.removeLocalService(mChannel, mServiceInfo,
					new GenericActionListener("Remove local service"));
		}
	}
}
