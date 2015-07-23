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

package meesters.wifip2p.connect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import meesters.wifip2p.deps.IP2PMessage;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.deps.Router.MsgType;
import android.util.Log;

public class ClientThread extends Thread implements IP2PMessageSender,
		IP2PMessageReceiver {

	private IClientConnectListener mListener = null;
	private Socket mSocket = null;
	private InetAddress mAddress = null;
	private int mPort = -1;
	private static final String TAG = "ClientThread";
	private IP2PMessageReceiver mP2PMessageReceiver = null;
	private SocketSendDelegate mSendDelegate = null;
	private SocketReceiveDelegate mReceiveDelegate = null;

	private static final int MSG_TIMEOUT = 60;
	private static final int SOCKET_TIMEOUT = 60;
	private int mTimeOutCounter = 0;
	private boolean isConnected = false;
	private boolean isWaiting = false;

	public ClientThread(IClientConnectListener listener, InetAddress address,
			int port) {
		Log.v(TAG, "ClientThread constructor");
		mListener = listener;
		mAddress = address;
		mPort = port;
	}

	@Override
	public void run() {
		Log.v(TAG, "run");

		try {
			mSocket = new Socket(mAddress, mPort);
		} catch (IOException e) {
			Log.e(TAG, "Could not open Socket");
			e.printStackTrace();
			// TODO: Send broadcast here that something went wrong....
			mSocket = null;
			return;
		} catch (IllegalArgumentException iae) {
			// Port number was incorrect
			mSocket = null;
			Log.e(TAG, "Could not open Socket on specified port");
			// TODO: Send broadcast here that port number was wrong...
			return;
		} catch (NullPointerException npe) {
			mSocket = null;
			Log.e(TAG, "mAddress was null, could not open port");
			// TODO: Send broadcast here that port number was wrong...
			return;
		}

		Log.i(TAG, "Client connected");

		mListener.onClientConnected();

		if (mSocket == null) {
			Log.e(TAG, "Socket was null, bailing out");
			return;
		}

		try {
			mSendDelegate = new SocketSendDelegate(mSocket.getOutputStream(),
					TAG);
			// Changed receiver to this, so that we can intercept the messages
			mReceiveDelegate = new SocketReceiveDelegate(
					mSocket.getInputStream(), this, TAG);
		} catch (IOException e) {
			Log.e(TAG, ": " + "Could not open Output or Input Streams");
			e.printStackTrace();
		}

		sendPongP2PMessage();
		isConnected = true;
		mTimeOutThread = new TimeOutThread();
		mTimeOutThread.start();
		mReceiveDelegate.receiveLoop();

	}

	public Socket getSocket() {
		Log.v(TAG, "getSocket");
		return mSocket;
	}

	public void setP2PMessageReceiver(IP2PMessageReceiver receiver) {
		Log.v(TAG, "setP2PMessageReceiver");
		mP2PMessageReceiver = receiver;
	}

	public IP2PMessageSender getP2PMessageSender() {
		Log.v(TAG, "getP2PMessageSender");
		return this;
	}

	// @Override
	// public void sendBytes(byte[] data) {
	// Log.v(TAG, "sendBytes" + new String(data));
	// mSendDelegate.sendBytes(data);
	// }

	public void sendPongP2PMessage() {
		// Send a test string
		String testString = new String("Hello world! :)");
		PongP2PMessage msg = new PongP2PMessage("TestMessage", "0",
				MsgType.PONG_MESSAGE, null, null, testString.getBytes().length,
				testString.getBytes());
		mSendDelegate.sendP2PMessage(msg);
	}

	private TimeOutThread mTimeOutThread = null; 
			
	private class TimeOutThread extends Thread {
		public void run() {
			while (true && !isInterrupted() && isConnected) {
				mTimeOutCounter++;

				if (mTimeOutCounter > MSG_TIMEOUT) {
					// Log.e(TAG, "It's been a while since I was refreshed");

					if (!isWaiting) {
						Log.e(TAG, "It's been a while since I was refreshed");
						Log.e(TAG, "Sending a Pong message");
						sendPongP2PMessage();
						isWaiting = true;
					} else {
						if (mTimeOutCounter > MSG_TIMEOUT + SOCKET_TIMEOUT) {
							Log.e(TAG,
									"Something terrible happened, should I retry the connection?");
							if (isConnected) {
								Log.e(TAG, "Let's retry:");
								new Thread() {
									public void run() {
										ClientThread.this.run();
									}
								}.start();
							}
						}
					}
				}

				try {
					sleep(1000);
					if (mTimeOutCounter % 10 == 0)
						Log.e(TAG, "mTimeOutThread sleeping " + mTimeOutCounter);
				} catch (InterruptedException e) {
					Log.e(TAG, "I was interrupted");
				}
			}
		}
	};

	public void refresh(PongP2PMessage msg) {
		Log.e(TAG, "I have been refreshed");
		mTimeOutCounter = 0;
		isWaiting = false;
	}

	@Override
	public void interrupt() {
		// Only close Socket and ReceiveDelegate if Socket was successfully
		// opened
		try {
			if (mSocket != null) {
				mSocket.close();
			}
		} catch (IOException e) {
			Log.e(TAG, "Closing sockets in interrupt failed");
			e.printStackTrace();
		}

		if (mReceiveDelegate != null) {
			mReceiveDelegate.setInterrupted(true);
		}
		if (mTimeOutThread != null) {
			mTimeOutThread.interrupt();
			mTimeOutThread = null;
		}
		isConnected = false;
		// super.interrupt();
	}

	@Override
	public void sendP2PMessage(IP2PMessage msg) {
		Log.v(TAG, "sendP2PMessage" + msg.toString());
		if (mSendDelegate != null) {
			mSendDelegate.sendP2PMessage(msg);
		} else {
			Log.e(TAG, "mSendDelegate was null");
		}
	}

	@Override
	public void receiveP2PMessage(IP2PMessage msg) {
		if (mP2PMessageReceiver != null) {
			mP2PMessageReceiver.receiveP2PMessage(msg);
		}

		if (msg instanceof PongP2PMessage
				&& msg.getMsgType() == Router.MsgType.PONG_ANSW_MESSAGE) {
			PongP2PMessage pong_msg = (PongP2PMessage) msg;
			refresh(pong_msg);
		}

	}

}
