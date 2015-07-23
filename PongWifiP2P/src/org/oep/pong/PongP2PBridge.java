package org.oep.pong;

import java.util.HashMap;
import java.util.Map;

import meesters.wifip2p.deps.BaseP2PBridge;
import meesters.wifip2p.deps.P2PServiceDescriptor;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

public class PongP2PBridge extends BaseP2PBridge {

	private final static String TAG = "PongP2PBridge";

	public final static String P2P_SERVICE_NAME = "pongp2p";
	public final static String P2P_SERVICE_TYPE = "_pongp2p._tcp";
	public final static String P2P_FULL_DOMAIN_NAME = PongP2PBridge.P2P_SERVICE_NAME
			+ "." + PongP2PBridge.P2P_SERVICE_TYPE + ".local.";
	public final static String PORTNUM_KEY = "pong_port";
	public final static String LOCALP2P_IPADDRESS_KEY = "pong_local_ip";
	public final static String SERVERSTATE_KEY = "SERVER_STATE";
	private final static int DEFAULT_PONG_PORT = 10001;

	private boolean mServerState = false;

	private P2PServiceDescriptor[] mP2PServices = null;

	private int mServiceAdNumber = -1;
	private String mLocalIPAddressString = "";

	public PongP2PBridge(Context context) {
		super();
		mContext = context;
	}

	public void connectToAddress(String deviceAddress, int GOIntent) {
		Log.v(TAG, "connectToAddress");
		if (mBound) {
			try {
				mService.connectToAddress(deviceAddress, GOIntent);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, "Service is not bound");
		}
	}

	public void getLocalP2PIpAddress() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, "getLocalP2PIpAddress");
				try {
					mLocalIPAddressString = mService.getLocalP2PAddress();
				} catch (RemoteException e) {
					Log.e(TAG, "Could not get LocalP2PIpAddress via framework");
					e.printStackTrace();
				}
			}
		};

		postOnConnectedStack(runnable);

	}

	/**
	 * A function used to re-register the service in it's previous server state
	 */
	public void registerPongService() {
		Log.v(TAG, "registerPongService with mServerState " + mServerState);
		registerPongService(mServerState);
	}

	public void registerPongService(final boolean server_box) {

		mServerState = server_box;

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, "registerPongService");
				try {
					// Remove the service if it was registered before
					mService.unAdvertiseP2PService(P2P_SERVICE_NAME,
							mServiceAdNumber);

					getLocalP2PIpAddress();

					if (mLocalIPAddressString.equals("")) {
						mLocalIPAddressString = "Unknown";
					}

					Map<String, String> extras = new HashMap<String, String>();
					extras.put(PORTNUM_KEY, Integer.toString(DEFAULT_PONG_PORT));
					extras.put(LOCALP2P_IPADDRESS_KEY,
							mLocalIPAddressString);
					extras.put(SERVERSTATE_KEY, Boolean.toString(server_box));
					mServiceAdNumber = mService.AdvertiseP2PService(
							P2P_SERVICE_NAME, P2P_SERVICE_TYPE,
							DEFAULT_PONG_PORT, extras);
				} catch (RemoteException e) {
					Log.e(TAG, "Could not advertise service via framework");
					e.printStackTrace();
				}

				if (mServiceAdNumber != -1) {
					Log.i(TAG, "Service Advertiser success");
				} else {
					Log.e(TAG, "Something went wrong with Service advertising");
				}
			}
		};

		postOnConnectedStack(runnable);
	}

	public void unregisterPongService() {
		Log.v(TAG, "unregisterPongService");
		if (mBound) {
			try {
				mService.unAdvertiseP2PService(P2P_SERVICE_NAME,
						mServiceAdNumber);
			} catch (RemoteException e) {
				Log.e(TAG, "Could not unAdvertise the service");
			}
		}
	}

	public void getCurrentP2PServices() {

		Runnable run = new Runnable() {
			public void run() {
				Log.v(TAG, "getCurrentP2PServices");
				try {
					mP2PServices = mService.getCurrentP2PServices();
					// for (P2PServiceDescriptor service : mP2PServices) {
					// Log.i(TAG, service.toString());
					// }

				} catch (RemoteException e) {
					Log.e(TAG, "Could not get P2PServices");
					e.printStackTrace();
				}
			}
		};

		postOnConnectedStack(run);
	}

	public void startDiscovery() {
		Log.v(TAG, "startDiscovery");
		if (mBound) {
			try {
				mService.startDiscovery();
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			// Delay the request of new Service availablibilty check
			new Thread() {
				public void run() {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					getCurrentP2PServices();
				}
			}.start();
		}
	}
	
	public void stopDiscovery() {
		Log.v(TAG, "stopDiscovery");
		if (mBound) {
			try {
				mService.stopDiscovery();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public P2PServiceDescriptor[] getP2PServiceDescriptors() {
		return mP2PServices;
	}
}
