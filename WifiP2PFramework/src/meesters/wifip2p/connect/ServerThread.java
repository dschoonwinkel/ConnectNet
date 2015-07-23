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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import meesters.wifip2p.deps.IP2PMessage;
import meesters.wifip2p.deps.Router;
import android.util.Log;

public class ServerThread extends Thread implements IP2PMessageSender,
		IP2PMessageReceiver, IP2PMessageThreadedReceiver {

	private static final String TAG = "ServerThread";

	private IServerSocketConnectListener mListener = null;
	private ServerSocket mSSocket = null;
	private IP2PMessageReceiver mP2PMessageReceiver = null;

	private InetAddress mAddress = null;

	private int mThreadCount = 0;
	private List<SocketResponseThread> mThreads = null;

	public ServerThread(IServerSocketConnectListener listener,
			InetAddress address) {
		Log.v(TAG, "ServerThread constructor");
		mListener = listener;
		mAddress = address;
		mThreads = new ArrayList<SocketResponseThread>();
		mThreadCount = 0;
	}

	@Override
	public void run() {
		Log.v(TAG, "run");
		try {

			// TODO: Check if this is the correct way...
			mSSocket = new ServerSocket(0, 50, mAddress);
			Log.e(TAG, mSSocket.getInetAddress().toString());
			// mSSocket = new ServerSocket(0, 50, mAddress);
		} catch (IOException e) {
			Log.e(TAG, "Server Socket creation failed");
			// TODO: Handle this exception more gracefully
			e.printStackTrace();
			return;
		}

		// Callback to mService here
		mListener.onServerSocketConnected();

		while (!this.isInterrupted()) {
			Socket mSocket = null;
			try {
				mSocket = mSSocket.accept();
			} catch (IOException e) {
				if (e instanceof SocketException) {
					Log.e(TAG,
							"Server thread interrupted before accept() completed");
					return;
				}
				Log.e(TAG, "Server listener Socket could not be created");
				// TODO: Handle this Exception more gracefully
				e.printStackTrace();
				return;
			}

			// This thread should not continue if it is interrupted. Possible
			// racing
			// condition
			if (this.isInterrupted()) {
				Log.i(TAG, "Server Thread thread finished");
				return;
			}

			// Start the handler thread for this socket
			if (mSocket != null) {
				SocketResponseThread resp_thread = new SocketResponseThread(
						mSocket, mListener, mThreadCount);
				resp_thread.setP2PMessageReceiver(this);
				mThreads.add(resp_thread);
				mThreads.get(mThreadCount).start();
				mThreadCount++;

			} else {
				Log.e(TAG, "Accepted socket was null");
			}
		}
		Log.i(TAG, "Server Thread thread finished");

	}

	public ServerSocket getSSocket() {
		Log.v(TAG, "getSSocket");
		return mSSocket;
	}

	public IP2PMessageSender getP2PMessageSender() {
		return this;
	}

	public void setP2PMessageReceiver(IP2PMessageReceiver receiver) {
		mP2PMessageReceiver = receiver;
	}

	@Override
	public void sendP2PMessage(IP2PMessage msg) {
		Log.v(TAG, "sendP2PMessage" + msg.toString());

		// Send this message to all connected devices
		for (SocketResponseThread thread : mThreads) {
			thread.sendP2PMessage(msg);
		}
	}

	public void receiveP2PMessageFromThread(IP2PMessage msg, int threadNumber) {
		Log.v(TAG, "receiveP2PMessageFromThread nr" + threadNumber);

		if (msg.getMsgType() != Router.MsgType.PONG_MESSAGE) {
			// Do the multipexing to other clients here
			for (SocketResponseThread thread : mThreads) {
				if (thread.getThreadNumber() != threadNumber) {
					thread.sendP2PMessage(msg);
				}
			}
		}
		// Distribute the received message on this device
		receiveP2PMessage(msg);
	}

	@Override
	public void receiveP2PMessage(IP2PMessage msg) {
		if (mP2PMessageReceiver != null) {
			mP2PMessageReceiver.receiveP2PMessage(msg);
		} else {
			Log.e(TAG,
					"mP2PMessageReceiver was null, i.e. not ready for receiving messages");
		}
	}

	@Override
	public void interrupt() {
		Log.v(TAG, "interrupt()");
		try {
			if (mSSocket != null) {
				mSSocket.close();
			}
		} catch (IOException e) {
			Log.e(TAG, "Closing sockets in interrupt failed");
			e.printStackTrace();
		}
		for (SocketResponseThread thread : mThreads) {
			thread.interrupt();
		}
	}

}
