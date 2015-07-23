package be.ppareit.swiftp;

import java.net.InetAddress;

import meesters.wifip2p.deps.Router;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

/**
 * WifiP2PReceiver handles broadcasts from the P2PConnector Framework, specifically to 
 * provide the {@link be.ppareit.swiftp.FsService} with the local P2P Wifi InetAddress and 
 * P2P Device details. 
 * 
 * @author schoonwi
 * @version 1.0
 * @since 2015-02-10
 * 
 */
@SuppressWarnings("unused")
public class WifiP2PReceiver extends BroadcastReceiver {

	private static final String TAG = "WifiDirectBroadcastReceiver";
	private WifiP2pManager mManager = null;
	private Channel mChannel = null;
	private FsService mService;

	public WifiP2PReceiver(WifiP2pManager manager, Channel channel,
			FsService service) {
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
		
		case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
			Log.v(TAG, "wifiP2PThisDeviceChanged");
			WifiP2pDevice device = intent
					.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			// Log.i(TAG, "Wifi P2P This device Changed: " + device.toString());
			mService.updateDeviceInfo(device);
			break;
			
			
		case Router.P2P_ReceiverActions.GET_LOCALP2PADDRESS:
			Log.v(TAG, "GetLocalP2PAddress");
			InetAddress address = (InetAddress) intent.getSerializableExtra(Router.P2P_ReceiverActions.EXTRA_GET_LOCALP2PADDRESS);
			mService.updateLocalP2PAddress(address);
		
		}

	}
	
	public static IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		filter.addAction(Router.P2P_ReceiverActions.GET_LOCALP2PADDRESS);
		return filter;
	}
}
