package org.oep.pong;

import static org.oep.pong.PongP2PBridge.LOCALP2P_IPADDRESS_KEY;
import static org.oep.pong.PongP2PBridge.PORTNUM_KEY;
import static org.oep.pong.PongP2PBridge.SERVERSTATE_KEY;

import java.util.ArrayList;
import java.util.Map;

import meesters.wifip2p.deps.AbstractP2PFrontendActivity;
import meesters.wifip2p.deps.P2PServiceDescriptor;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.deps.WifiP2PReceiver;

import org.oep.pong.ConnectDialog.ConnectDialogListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class Pong extends AbstractP2PFrontendActivity implements
		ConnectDialogListener {
	/** Called when the activity is first created. */
	private PongView mPongView;
	private AlertDialog mAboutBox;
	private RefreshHandler mRefresher;
	protected PowerManager.WakeLock mWakeLock;
	private ConnectDialog mConnectDialog = null;
	private String mInetAddressString = "";
	private String mPort = "";
	private boolean mIsServer = false;

	private WifiP2PReceiver mReceiver = null;
	private IntentFilter mFilter = null;
	private ServicesAvailableDialogFragment mServicesFragment = null;
	private static final String SERVICES_FRAGMENT = "Services Fragment";

	private ArrayList<P2PServiceDescriptor> mDescriptors = null;

	// WifiP2P Variables
	private PongP2PBridge mBridge = null;

	private static final String TAG = "Pong";

	// Blue player is Player 1

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Pong.this.hideAboutBox();
		}

		public void sleep(long delay) {
			this.removeMessages(0);
			this.sendMessageDelayed(obtainMessage(0), delay);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Logger.logMessage(TAG, "Pong starting up");

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.pong_view);
		mPongView = (PongView) findViewById(R.id.pong);
		mPongView.update();
		mRefresher = new RefreshHandler();

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		final PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Pong");
		mWakeLock.acquire();

		mBridge = new PongP2PBridge(getApplicationContext());
		mBridge.init();

		mReceiver = new WifiP2PReceiver(this);
		setUpIntentFilter();

		// mBridge.registerPongService(); -- This is done by user when ready to
		// host server
		// mBridge.startDiscovery();

		mDescriptors = new ArrayList<P2PServiceDescriptor>();

		// mServicesFragment = ServicesAvailableDialogFragment
		// .newInstance(new P2PServiceDescriptor[0]);
		// mServicesFragment.show(getFragmentManager(), SERVICES_FRAGMENT);

		Intent startIntent = getIntent();
		
		if (startIntent.getAction().equals("org.oep.pong.Pong_TestRunner")) {
			Log.v(TAG, "Started with test runner");
			
			String ipaddress = startIntent.getStringExtra("IPADDRESS");
			String serverBox = startIntent.getStringExtra("ServerBox");
			
			Log.v(TAG, "IPADDRESS: " + ipaddress);
			if (ipaddress != null && serverBox != null) {
				Log.v(TAG, "IPAddress: " + ipaddress + " serverBox: " + serverBox);
				this.onDialogSuccess(ipaddress, "10001", Boolean.parseBoolean(serverBox));
			}
		}
		else {
			mConnectDialog = new ConnectDialog(this, this);
			mConnectDialog.show();
		}
	}

	protected void onStop() {
		super.onStop();
		mPongView.stop();
	}

	protected void onResume() {
		super.onResume();
		mPongView.resume();

		registerReceiver(mReceiver, mFilter);

		mBridge.getCurrentP2PServices();
		mBridge.postOnConnectedStack(new Runnable() {
			public void run() {
				Pong.this.onServiceConnected();
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	public void onServiceConnected() {
		// Starts the Services/Connect dialog here..
	}

	public void onRegisterButton(View v) {
		Log.v(TAG, "onRegisterButton");
		registerService(false);
	}

	public void registerService(boolean server_box) {
		Log.v(TAG, "registerService");
		mBridge.registerPongService(server_box);
	}

	protected void onDestroy() {
		super.onDestroy();
		mPongView.releaseResources();
		mWakeLock.release();

		mBridge.unregisterPongService();
		mBridge.disconnect();

		PongController.onDestroy();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		boolean result = super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_menu, menu);

		return result;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		int id = item.getItemId();
		boolean flag = false;

		switch (id) {
		case R.id.menu_0p:
			flag = true;
			mPongView.setPlayerControl(false, false);
			break;
		case R.id.menu_1p:
			flag = true;
			mPongView.setPlayerControl(false, true);
			break;
		case R.id.menu_2p:
			flag = true;
			mPongView.setPlayerControl(true, true);
			break;
		case R.id.menu_about:
			mAboutBox = new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.about).setMessage(R.string.about_msg)
					.show();
			mPongView.pause();
			mRefresher.sleep(5000);
			break;
		case R.id.quit:
			this.finish();
			return true;

		case R.id.menu_toggle_sound:
			mPongView.toggleMuted();
			break;
		}

		if (flag) {
			mPongView.setShowTitle(false);
			mPongView.newGame();
		}

		return true;
	}

	public void hideAboutBox() {
		if (mAboutBox != null) {
			mAboutBox.hide();
			mAboutBox = null;
		}
	}

	public static final String DB_PREFS = "Pong";
	public static final String PREF_MUTED = "pref_muted";

	@Override
	public void onDialogSuccess(String address, String port, boolean serverBox) {

		//Dismiss the dialog if it is still visible
		if (mConnectDialog != null && mConnectDialog.isShowing()) {
			mConnectDialog.dismiss();
		}
		
		Log.v(TAG, "onDialogSuccess" + " " + address + " " + port + " "
				+ serverBox);
		mPongView.setServerState(serverBox, true);
		PongController.startController(address, port);
		mBridge.stopDiscovery();

		new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (PongController.isReady()) {
//						PongController
//								.sendDebugMessage("This is a debug message", -1, "00:00");
					}
					else {
						Log.e(TAG, "Not ready for sending a debug message");
					}
				}
			}
		}.start();

	}

	public void onTwoPlayer(View v) {
		Log.v(TAG, "onTwoPlayer");

		if (mIsServer) {
			// If this is the server, start a new game, with the blue player
			// this side,
			mPongView.setPlayerControl(true, true);
			mPongView.setShowTitle(false);
			mPongView.newGame();
		} else if (!mIsServer) {
			mPongView.setPlayerControl(true, true);
			mPongView.setShowTitle(false);
			mPongView.newGame();
		}

		findViewById(R.id.register_button).setVisibility(View.INVISIBLE);
		findViewById(R.id.one_player).setVisibility(View.INVISIBLE);

	}

	public void onOpenConnectDialog(View v) {
		mConnectDialog = new ConnectDialog(this, this);
		mConnectDialog.show();
	}

	public void onDiscoverServices(View v) {
		Log.v(TAG, "onDiscoverServices");
		mBridge.startDiscovery();
	}

	@Override
	public void updateServicesView(String text) {
		Log.v(TAG, "updateServicesView:" + text);
		if (mServicesFragment != null) {
			mServicesFragment.updateServicesView(text);
		}
	}

	@Override
	public void updateServicesView(P2PServiceDescriptor descriptor) {
		Log.v(TAG, "updateServices_withDescriptor");
		if (mServicesFragment != null) {
			mServicesFragment.updateServicesView(descriptor);
		}
		mDescriptors.add(descriptor);
	}

	@Override
	public void updateConnState(String text) {
		if (text.equals(AbstractP2PFrontendActivity.connStates[AbstractP2PFrontendActivity.CONNECTED])) {
			mBridge.unregisterPongService();
			mBridge.registerPongService();
		}
	}

	public void onItemClicked(int position) {
		Log.v(TAG, "onItemClicked " + position);
		if (mDescriptors.size() > position) {
			P2PServiceDescriptor descriptor = mDescriptors.get(position);
			Map<String, String> extras = descriptor.extras;

			if (extras.containsKey(PORTNUM_KEY)
					&& extras.containsKey(LOCALP2P_IPADDRESS_KEY)
					&& extras.containsKey(SERVERSTATE_KEY)) {
				if (extras.get(LOCALP2P_IPADDRESS_KEY).equals("nknown")) {
					Log.i(TAG, "Connecting P2P");
					mBridge.connectToAddress(
							descriptor.srcDevice.deviceAddress,
							!Boolean.parseBoolean(extras.get(SERVERSTATE_KEY)) ? 15
									: 0);
				} else {
					Log.i(TAG, "Opening sockets");
					onDialogSuccess(extras.get(LOCALP2P_IPADDRESS_KEY),
							extras.get(PORTNUM_KEY),
							!Boolean.parseBoolean(extras.get(SERVERSTATE_KEY)));
				}
				//
			}
		}
	}

	@Override
	public void clearServicesView() {
		Log.v(TAG, "clearServicesView");
		mDescriptors.clear();
		if (mServicesFragment != null) {
			mServicesFragment.clearServicesView();
		}
	}

	@Override
	public void setUpIntentFilter() {
		Log.v(TAG, "setUpIntentFilter");
		mFilter = new IntentFilter();
		mFilter.addAction(Router.P2P_ReceiverActions.UPDATE_SERVICES_VIEW);
		mFilter.addAction(Router.P2P_ReceiverActions.P2P_SERVICE_DESCRIPTOR);
		mFilter.addAction(Router.P2P_ReceiverActions.CLEAR_SERVICES_VIEW);

	}
}