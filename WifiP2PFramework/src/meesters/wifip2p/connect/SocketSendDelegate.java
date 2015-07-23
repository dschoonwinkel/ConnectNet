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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import meesters.wifip2p.deps.IP2PMessage;

import android.util.Log;
public class SocketSendDelegate implements IP2PMessageSender {

	private static final String TAG = "SocketSendDelegate";
	private DataOutputStream mOutput = null;
	private String mName = "";

	public SocketSendDelegate(OutputStream out, String name) {
		mName = name + "." + TAG;
		
		Log.v(mName, "SocketSendDelegate constructor");
		mOutput = new DataOutputStream(out);
		
	}

	@Override
	public void sendP2PMessage(IP2PMessage msg) {
		Log.v(mName, "sendObject: " + msg.toString());
		try {
			//Write the message type first, then the message
			mOutput.writeInt(msg.getMsgType());
			
			msg.writeToDataStream(mOutput);
			mOutput.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
