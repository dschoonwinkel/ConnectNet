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

import android.app.Activity;
import android.net.wifi.p2p.WifiP2pDeviceList;

public abstract class AbstractP2PFrontendActivity extends Activity {

	public abstract void setUpIntentFilter();
	public void updateConnState(String text){}
	public void updateActionState(String text){}
	public void updateDiscoverState(String text){}
	public void updateDevicesView(WifiP2pDeviceList list){}
	public void updateGroupOwner(boolean owner){}
	public void clearServicesView(){}
	public void updateServicesView(String text){}
	public void doUpdateServicesView(final String text){}

	public static final String[] connStates = { "IDLE", "SCANNING",
			"STOP_SCAN", "CONNECTING", "CONNECTED", "DISCOVERING",
			"STOP_DISCOVER", "DISCONNECTING", "DISCONNECTED" };
	public static final int IDLE = 0, SCANNING = 1, STOPPED_SCAN = 2,
			CONNECTING = 3, CONNECTED = 4, DISCOVERING = 5,
			STOPPED_DISCOVER = 6, DISCONNECTING = 7, DISCONNECTED = 8;
}
