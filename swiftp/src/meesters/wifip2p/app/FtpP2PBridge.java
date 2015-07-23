package meesters.wifip2p.app;

import java.util.HashMap;

import meesters.wifip2p.deps.AbstractP2PBridge;
import meesters.wifip2p.deps.BaseP2PBridge;
import meesters.wifip2p.deps.IP2PConnectorService;
import meesters.wifip2p.deps.Router;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class FtpP2PBridge extends BaseP2PBridge {

	private IP2PConnectorService mService = null;
	private Context mContext = null;
	private boolean mBound = false;
	private String mLocalWifiP2PAddress = "";
	private int mServiceNumber = -1;

	public static final String FTP_P2P_SERVICE_NAME = "FTPService";
	public static final String FTP_P2P_SERVICE_TYPE = "_ftpservice._tcp";

	private static final String TAG = "FtpP2PBridge";

	public FtpP2PBridge(Context context) {
		mContext = context;
	}

	@Override
	public void init() {
		Log.v(TAG, "init");
		Intent p2pconnectorIntent = new Intent(Router.ACTION_P2P_SERVICE);
		mContext.bindService(p2pconnectorIntent, mServiceConn,
				Context.BIND_AUTO_CREATE);

	}

	private ServiceConnection mServiceConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.v(TAG, "Service disconnected");
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v(TAG, "Service connected");
			mService = IP2PConnectorService.Stub.asInterface(service);
			mBound = true;
			try {
				mService.getStates();
				mLocalWifiP2PAddress = mService.getLocalP2PAddress();
				for (Runnable runnable : mOnConnectedStack) {
					runnable.run();
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	@Override
	public void disconnect() {
		Log.v(TAG, "disconnect");
		if (mBound) {
			mContext.unbindService(mServiceConn);
		}

	}

	public String getLocalP2PAddress() {
		if (mBound) {
			try {
				return mService.getLocalP2PAddress();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, "Service is unbound!");
			return mLocalWifiP2PAddress;
		}
		return "null";
	}

	public void registerFTPService(final int port) {
		Log.v(TAG, "registerFTPService");
		Runnable registerRunnable = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				String localP2PAddress = getLocalP2PAddress();
				if (localP2PAddress == null) {
					localP2PAddress = "null";
				}
				try {
					HashMap<String, String> extras = new HashMap<String, String>();
					extras.put(Router.P2P_EXTRA_KEYS.PORTNUM_KEY,
							Integer.toString(port));
					extras.put(Router.P2P_EXTRA_KEYS.INETADDR_KEY,
							localP2PAddress);

					mServiceNumber = mService.AdvertiseP2PService(
							FTP_P2P_SERVICE_NAME, FTP_P2P_SERVICE_TYPE, port,
							extras);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		};
		if (mBound) {
			registerRunnable.run();
		} else {
			postOnConnectedStack(registerRunnable);
		}
	}

	public void unregisterFTPService() {
		if (mBound) {
			try {
				mService.unAdvertiseP2PService(FTP_P2P_SERVICE_NAME,
						mServiceNumber);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
