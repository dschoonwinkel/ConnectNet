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
import java.net.Socket;

import meesters.wifip2p.deps.IP2PMessage;

import android.util.Log;

/**
 * This class is responsible for handling IO on this socket. Multiple threads
 * like this one will be spawned to respond to every incoming connection
 * 
 * @author Daniel Schoonwinkel
 * 
 */
public class SocketResponseThread extends Thread implements IP2PMessageSender {

	private Socket mSocket = null;
	private String mName = "SocketResponseThread";
	private int mThreadNumber = -1;
	private IServerSocketConnectListener mSocketListener = null;

	private IP2PMessageThreadedReceiver mP2PMessageReceiver = null;
	private SocketSendDelegate mSendDelegate = null;
	private SocketThreadedReceiveDelegate mReceiveDelegate = null;

	public SocketResponseThread(Socket socket,
			IServerSocketConnectListener listener, int thread_nr) {
		mName = mName + thread_nr;
		mThreadNumber = thread_nr;
		mSocket = socket;
		mSocketListener = listener;
	}

	@Override
	public void run() {
		mSocketListener.onSocketConnected(this);

		try {
			mSendDelegate = new SocketSendDelegate(mSocket.getOutputStream(),
					mName);
			mReceiveDelegate = new SocketThreadedReceiveDelegate(
					mSocket.getInputStream(), mP2PMessageReceiver, mName, mThreadNumber);
		} catch (IOException e) {
			System.out.println(mName + ": "
					+ "Could not open Output or Input Streams");
			e.printStackTrace();
			// TODO: Handle this Exception more gracefully
		}

		mReceiveDelegate.receiveLoop();
	}

	@Override
	public void interrupt() {
		if (mReceiveDelegate != null) {
			mReceiveDelegate.setInterrupted(true);
		}
	}

	public Socket getSocket() {
		Log.v(mName, "getSocket");
		return mSocket;
	}

	public void setP2PMessageReceiver(IP2PMessageThreadedReceiver receiver) {
		Log.v(mName, "setP2PMessageReceiver");
		mP2PMessageReceiver = receiver;
	}

	public IP2PMessageSender getP2PMessageSender() {
		Log.v(mName, "getP2PMessageSender");
		return this;
	}

	@Override
	public void sendP2PMessage(IP2PMessage msg) {
		if (mSendDelegate != null) {
			mSendDelegate.sendP2PMessage(msg);
		} else {
			Log.e(mName,
					"Send Delegate is null, i.e. not ready for transmission");
		}

	}
	
	public int getThreadNumber() {
		return mThreadNumber;
	}
}
