package be.ppareit.swiftp;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.util.Log;


/**
 * @author Daniel Schoonwinkel
 * 
 * This class provides an implementation of the WifiP2PManager.ActionListener interface. 
 * The constructor takes the Message string as this listeners main action and purpose, 
 * and reports if the action was successful, or parses the error code to String
 *
 */
public class GenericActionListener implements ActionListener {

	private static final String TAG = "ActionListener";
	private String mMessage = "";
	
	/**
	 * @param message: Action that this class listens for. On success or failure, the message + result will be logged 
	 */
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
		}

		return "NOT FOUND";
	}

}
