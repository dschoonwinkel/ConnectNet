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

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.util.Log;

public class GenericActionListener implements ActionListener {

	private static final String TAG = "ActionListener";
	private String mMessage = "";
	
	public GenericActionListener(String message) {
		mMessage = message;
	}
	
	@Override
	public void onFailure(int reason) {
		Log.e(TAG, mMessage + " failed with reason: " + onErrorCode(reason));

	}

	@Override
	public void onSuccess() {
		Log.d(TAG, mMessage + " successful");
	}
	
	public static String onErrorCode(int code) {

		switch (code) {
		case WifiP2pManager.BUSY:
			return "BUSY";
		case WifiP2pManager.P2P_UNSUPPORTED:
			return "P2P_UNSUPPORTED";
		case WifiP2pManager.ERROR:
			return "ERROR";
		case WifiP2pManager.NO_SERVICE_REQUESTS:
			return "NO SERVICE REQUESTS";
		//If no errors were detected, the code of -1 is returned
		case -1:
			return "SUCCESS";
		}

		return "NOT FOUND";
	}

}
