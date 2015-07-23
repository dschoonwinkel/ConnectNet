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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import meesters.wifip2p.deps.Router;

import android.util.Log;

public class SocketReceiveDelegate {

	private DataInputStream mInStream = null;
	private IP2PMessageReceiver mP2PMessageReceiver = null;
	byte[] buffer = null;
	int byteCounter = 0;
	private final static String TAG = "SocketReceiveDelegate";
	private String mName = "";

	private boolean mInterrupted = false;

	public SocketReceiveDelegate(InputStream in,
			IP2PMessageReceiver mP2PMessageReceiver2, String name) {
		mName = name + "." + TAG;
		Log.v(mName, "SocketReceiverDelegate constructor");

		mInStream = new DataInputStream(new BufferedInputStream(in));
		mP2PMessageReceiver = mP2PMessageReceiver2;
	}

	public void receiveLoop() {
		while (!mInterrupted) {
			Log.v(mName, "receiveLoop");
			try {

				// Receive the message type first, then the message
				int messageType = mInStream.readInt();
				Log.v(TAG, "Message type: " + Router.MsgType.MsgTypeToString(messageType));

				switch (messageType) {
				case Router.MsgType.DATA_MESSAGE:
					Log.v(TAG, "Receiving data message");
					P2PMessage msg = new P2PMessage();
					msg.readFromDataStream(mInStream);
					mP2PMessageReceiver.receiveP2PMessage(msg);
					break;

				case Router.MsgType.CONTROL_MESSAGE:
					Log.v(TAG, "Receiving data message");
					ControlP2PMessage control_msg = new ControlP2PMessage();
					control_msg.readFromDataStream(mInStream);
					mP2PMessageReceiver.receiveP2PMessage(control_msg);
					break;

				case Router.MsgType.PONG_MESSAGE:
					Log.v(TAG, "Receivng Pong message");
					PongP2PMessage pong_msg = new PongP2PMessage();
					pong_msg.readFromDataStream(mInStream);
					mP2PMessageReceiver.receiveP2PMessage(pong_msg);
					break;
					
				case Router.MsgType.PONG_ANSW_MESSAGE:
					Log.v(TAG, "Receiving a Pong answer message");
					PongP2PMessage pong2_msg = new PongP2PMessage();
					pong2_msg.readFromDataStream(mInStream);
					mP2PMessageReceiver.receiveP2PMessage(pong2_msg);
					break;
				}

			} catch (IOException ioe) {
				if (ioe instanceof EOFException) {
					// //This stops reading from this socket as soon as there is
					// no more data...
					// //TODO: consider writing to IControlChannel here that
					// something went wrong...
					Log.e(TAG, "breaking receiveLoop() due to EOFException");
					break;
				}
				ioe.printStackTrace();
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setInterrupted(boolean interrupted) {
		Log.v(mName, "setInterrupted");
		mInterrupted = interrupted;
	}

}
