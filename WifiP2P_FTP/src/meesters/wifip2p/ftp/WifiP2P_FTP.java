/*

Author: Daniel Schoonwinkel
This file uses code written by Pieter Pareit, see below.

Copyright 2011-2013 Pieter Pareit
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package meesters.wifip2p.ftp;

import java.util.ArrayList;

import meesters.wifip2p.deps.AbstractP2PFrontendActivity;
import meesters.wifip2p.deps.P2PServiceDescriptor;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.deps.WifiP2PReceiver;
import meesters.wifip2p.ftp.ConnectDialog.ConnectDialogListener;
import meesters.wifip2p.ftp.FTPClientThread.fileListListener;
import meesters.wifip2p.ftp.FTPClientThread.readyForLoginListener;
import meesters.wifip2p.ftp.FTPClientThread.statusUpdateListener;
import meesters.wifip2p.ftp.LoginDialog.LoginDialogListener;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import be.ppareit.swiftp.server.StatusListener;

public class WifiP2P_FTP extends AbstractP2PFrontendActivity implements
		LoginDialogListener, ConnectDialogListener, readyForLoginListener,
		statusUpdateListener, OnItemClickListener, fileListListener {

	FTPClientThread mClient = null;
	private static final String TAG = "WifiP2P_FTP";
	private String mUsername = "";
	private String mPassword = "";
	private ArrayAdapter<String> mAdapter = null;
	private ListView mListView = null;
	private String[] mItems = null;

	private ArrayList<P2PServiceDescriptor> mServices = new ArrayList<P2PServiceDescriptor>();

	private WifiP2P_FTPBridge mBridge = null;

	private ServicesAvailableDialogFragment mServicesAvailable = null;

	private WifiP2PReceiver mReceiver = null;

	private Thread mRunnerThread = null;
	private ProgressBar mTestProgress = null;

	// TODO: Remove: this is for testing purposes only!
	public interface ExternalTrigger {
		public void onExternalTrigger(String message);
	}

	public ExternalTrigger mTrigger = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_p2_p__ftp);
		mListView = (ListView) findViewById(R.id.files_list);
		mAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.list_item);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);

		mTestProgress = (ProgressBar) findViewById(R.id.test_progress);

		Logger.logMessage(TAG, "Starting WifiP2P_FTP");

		// Add later to show the dialog
		// mServicesAvailable = new ServicesAvailableDialogFragment();
		// mServicesAvailable.show(getFragmentManager(), "ServicesAvailable");

		mReceiver = new WifiP2PReceiver(this);
		setUpIntentFilter();

		mBridge = new WifiP2P_FTPBridge(getApplicationContext());
		mBridge.init();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
		mBridge.disconnect();
		Logger.close();
	}

	@Override
	public void updateServicesView(P2PServiceDescriptor descriptor) {
		Log.v(TAG, "updateServicesView");
		if (mServicesAvailable != null) {
			mServicesAvailable.updateServicesView(descriptor);
		}
		mServices.add(descriptor);
	}

	public void onFTPServiceSelected(int position) {
		Log.v(TAG, "onFTPServiceSelected");

		P2PServiceDescriptor descriptor = mServices.get(position);
		if (descriptor.extras.containsKey(Router.P2P_EXTRA_KEYS.INETADDR_KEY)
				&& descriptor.extras
						.containsKey(Router.P2P_EXTRA_KEYS.PORTNUM_KEY)) {
			String ipaddress = descriptor.extras
					.get(Router.P2P_EXTRA_KEYS.INETADDR_KEY);
			String portnum = descriptor.extras
					.get(Router.P2P_EXTRA_KEYS.PORTNUM_KEY);
			Log.v(TAG, "starting Client: " + ipaddress + ":" + portnum);
			startFTPClient(ipaddress, portnum);
		}

	}

	public void onConnectButton(View v) {
		Log.v(TAG, "onConnectButton");

		// DialogFragment loginFrag = new LogInDialogFragment(this);
		// loginFrag.show(getFragmentManager(), "Log In Dialog");

		// LoginDialog dialog = new LoginDialog(this, this);
		// dialog.show();

		mUsername = "ftp";
		mPassword = "ftp";

		ConnectDialog connectDialog = new ConnectDialog(this, this);
		connectDialog.show();

	}

	public void onConnectDialog(String ipaddress, String port) {
		Log.v(TAG, "onConnectDialog");
		startFTPClient(ipaddress, port);
	}

	public void onDiscoverServices(View v) {
		Log.v(TAG, "onDiscoverServices");
		mServices.clear();
		mBridge.startDiscovery();

	}

	public void stopDiscovery() {
		Log.v(TAG, "stopDiscovery");
		// Trying to not stop discovery, to keep Wi-Fi P2P alive maybe....
		mBridge.stopDiscovery();
	}

	public void onRefreshButton(View v) {
		Log.v(TAG, "onRefreshButton");
		mClient.doRefreshList();
	}

	public void onRunTestButton(View v) {
		Log.v(TAG, "onRunTestButton");
		mRunnerThread = new Thread() {
			public void run() {

				for (int j = 0; j < 20; j++) {
					Logger.logMessage(TAG, "100MB files:");
					doExcersise(_100M);

					Logger.logMessage(TAG, "50MB files:");
					for (int i = 0; i < 2; i++) {
						doExcersise(_50M);
					}

					Logger.logMessage(TAG, "10MB files:");
					for (int i = 0; i < 10; i++) {
						doExcersise(_10M);
					}

					Logger.logMessage(TAG, "5MB files:");
					for (int i = 0; i < 20; i++) {
						doExcersise(_5M);
					}

					Logger.logMessage(TAG, "1MB files:");
					for (int i = 0; i < 100; i++) {
						doExcersise(_1M);
					}
					doUpdateProgressBar(j * 5);
				}
				finish();
			}

			public void doExcersise(int position) {
				getItem(position);
				Log.v(TAG, "Starting wait");
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		mRunnerThread.start();
	}

	public void doUpdateProgressBar(final int progress) {
		if (mTestProgress != null && mTestProgress.getHandler() != null) {
			mTestProgress.getHandler().post(new Runnable() {

				@Override
				public void run() {
					mTestProgress.setProgress(progress);
				}
			});
		}
	}

	public void startFTPClient() {
		Log.v(TAG, "startFTPClient - unimplemented...");
		//
		// String ipaddress = ((TextView) findViewById(R.id.ip_edit_text))
		// .getText().toString();
		// Log.d(TAG, ipaddress);
		// String portnum = ((TextView) findViewById(R.id.port_edit_text))
		// .getText().toString();
		// Log.d(TAG, portnum);

		// mClient = new FTPClientThread(ipaddress, portnum, this, this, this);
		// mClient.start();
	}

	public void startFTPClient(String ipaddress, String portnum) {

		mUsername = "ftp";
		mPassword = "ftp";

		mClient = new FTPClientThread(ipaddress, portnum, this, this, this);
		mClient.start();
	}

	@Override
	public void onDialogSuccess(String username, String password) {
		Log.v(TAG, "onDialogSuccess");
		mUsername = username;
		mPassword = password;

		startFTPClient();
		Log.d(TAG, mUsername);
		Log.d(TAG, mPassword);

	}

	@Override
	public void readyLogIn() {
		String status = mClient.logIn(mUsername, mPassword);
		doUpdateStatus(status);
	}

	public void doUpdateStatus(final String status) {
		// Log.v(TAG, "doUpdateStatus");

		final TextView text = (TextView) findViewById(R.id.status_view);
		text.getHandler().post(new Runnable() {
			@Override
			public void run() {
				text.setText(getString(R.string.status_string) + " " + status);
				// Log.v(TAG, ""+ status);
			}
		});

		if (status.equals(StatusListener.STARTED_SUCCESSFULLY)
				|| status.equals(StatusListener.FINISHED_SUCCESSFULLY)) {
			Log.v(TAG, status);
			Logger.logMessage(TAG, status);

		}

		if (status.equals(StatusListener.FINISHED_SUCCESSFULLY)) {
			if (mRunnerThread != null) {
				synchronized (mRunnerThread) {
					mRunnerThread.notify();
				}
			}
		}

		// Log.v(TAG, status);

		if (status.equals(StatusListener.FINISHED_SUCCESSFULLY)) {
			if (mTrigger != null) {
				mTrigger.onExternalTrigger(status);
			}
		}
	}

	public void updateListView() {
		Log.v(TAG, "updateListView");
		if (mItems.length <= 0) {
			Log.e(TAG, "List contains no items...");
		}
		mAdapter.addAll(mItems);
		mAdapter.notifyDataSetChanged();
	}

	public void doUpdateListView(final String[] items) {
		Log.v(TAG, "doUpdateListView");

		mItems = items;
		mListView.getHandler().post(new Runnable() {
			public void run() {
				mAdapter.clear();
				mAdapter.addAll(items);
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		mClient.getItem(position);
	}

	public void getItem(int position) {
		stopDiscovery();
		mClient.getItem(position);
	}

	@Override
	public void setUpIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Router.P2P_ReceiverActions.P2P_SERVICE_DESCRIPTOR);

		registerReceiver(mReceiver, filter);
	}

	/*
	 * 1: random100M.txt 5: random50M.txt 3: random10M.txt 6: random5M.txt 4:
	 * random1M.txt 0: random100K.txt 2: random10K.txt
	 */

	private final int _100M = 1;
	private final int _50M = 4;
	private final int _10M = 2;
	private final int _5M = 5;
	private final int _1M = 3;
	private final int _100K = 0;
	private final String IPADDRESS = "192.168.49.59";
}
