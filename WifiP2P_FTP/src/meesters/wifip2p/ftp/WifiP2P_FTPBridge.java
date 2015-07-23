package meesters.wifip2p.ftp;

import meesters.wifip2p.deps.BaseP2PBridge;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

public class WifiP2P_FTPBridge extends BaseP2PBridge {

	private static final String TAG = WifiP2P_FTPBridge.class.getName();

	public WifiP2P_FTPBridge(Context context) {
		mContext = context;
	}

	@Override
	public void init() {
		super.init();
	}

	public void startDiscovery() {
		Log.v(TAG, "startDiscovery");
		if (mBound) {
			try {
				mService.startDiscovery();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopDiscovery() {
		Log.v(TAG, "stopDiscovery");
		if (mBound) {
			try {
				mService.stopDiscovery();
			} catch(RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
