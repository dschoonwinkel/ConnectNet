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

package meesters.wifip2p.chat;

import meesters.wifip2p.connect.P2PMessage;
import meesters.wifip2p.deps.Router;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class WifiP2PChat extends Activity {

	public static final String APP_ID = "WifiP2PChat";
	public static final String APP_UUID = "721dfa71-4348-43c3-abff-4e6a735b5138";

	// Debugging
	private static final String TAG = "WifiP2PChat";

	private P2PChatBridge mBridge = null;
	//
	private ArrayAdapter<String> mChatArrayAdapter;
	private ListView mChatView;
	private EditText mOutMsgText;
	private WifiP2PChatReceiver mReceiver = null;
	private IntentFilter mFilter = null;
	private TextView mConnStateView = null;

	int numPeer = 0;

	/**
	 * Standard initialization of this activity. Set up the UI, then wait for
	 * the user to poke it before doing anything.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Initialize the array adapter for the conversation thread
		mChatArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mChatView = (ListView) findViewById(R.id.chat_msgs);
		mChatView.setAdapter(mChatArrayAdapter);

		mConnStateView = (TextView) findViewById(R.id.conn_state);

		// Initialize the compose field with a listener for the return key
		mOutMsgText = (EditText) findViewById(R.id.msg_out);
		mOutMsgText
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					public boolean onEditorAction(TextView view, int actionId,
							KeyEvent event) {
						// If the action is a key-up event on the return key,
						// send the message
						if (actionId == EditorInfo.IME_NULL
								&& event.getAction() == KeyEvent.ACTION_UP) {
							String message = view.getText().toString();
							view.setText("");
							sendMsg(message);
						}
						return true;
					}
				});

		// mSendButton.setEnabled(false);

		mBridge = new P2PChatBridge(getApplicationContext());
		mBridge.init();
		mReceiver = new WifiP2PChatReceiver(this);
		setUpIntentFilter();

		Logger.logMessage(TAG, "Started logger");
	}

	public void setUpIntentFilter() {
		Log.v(TAG, "setUpIntentFilter");
		mFilter = new IntentFilter();
		mFilter.addAction(Router.P2P_ReceiverActions.BYTES_RECEIVED);
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_CONNECTED);
		mFilter.addAction(Router.P2P_ReceiverActions.PEER_DISCONNECTED);
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();
		registerReceiver(mReceiver, mFilter);
		mBridge.getStates();
	}

	@Override
	protected void onPause() {
		Log.v(TAG, "onPause");
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	public void onWifiP2PApp(View view) {
		Log.v(TAG, "onWifiP2PApp");
		Intent p2pconnect = new Intent(Router.ACTION_P2PAPP);
		Log.i(TAG, "Starting activity for result");
		startActivityForResult(p2pconnect, Router.REQUEST_WIFIP2PAPP);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v(TAG, "onActivityResult");
		if (resultCode == Router.SUCCESS_WIFIP2PAPP) {
			Log.i(TAG, "Success");
		} else if (resultCode == RESULT_CANCELED) {
			Log.i(TAG, "Cancelled");
		}
		mBridge.getStates();
	}

	public void updateConnState(String text) {
		Log.v(TAG, "updateConnState");
		mConnStateView.setText("Connection Status: " + text);
	}

	public void onSendButton(View view) {
		Log.v(TAG, "onSendButton");
		String message = mOutMsgText.getText().toString();
		sendMsg(message);
		mOutMsgText.setText("");
	}

	public void onDoneButton(View view) {
		Log.v(TAG, "onDoneButton");
		mBridge.disconnect();
		finish();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		Logger.close();
		super.onDestroy();
	}

	private void sendMsg(String msg_data) {
		// Log.v(TAG, "sendMessage: " + msg_data);
		Log.i(TAG, Build.MODEL + ": " + msg_data);
		// show my msg first
		mChatArrayAdapter.add("Me: " + msg_data);
		// send my msg
		mBridge.sendMessageToPeer(0, APP_ID, APP_UUID,
				Router.MsgType.DATA_MESSAGE, null, msg_data.getBytes());
		Logger.logMessage(Build.MODEL, msg_data);
	}

	public void onReceiveMsg(P2PMessage msg) {
		byte[] data = msg.getData();

		// Log.v(TAG, "onReceiveMsg" + new String(data));
		String fromDev = msg.getFromDev();
		String msg_content = new String(data);
		Log.i(TAG, fromDev + ": " + msg_content);
		mChatArrayAdapter.add(fromDev + ": " + msg_content);
		Logger.logMessage(fromDev, msg_content);
		
	}
}
