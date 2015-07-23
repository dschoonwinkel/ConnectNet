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

package meesters.wifip2p.deps;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.os.Parcel;

//Stubbed base class for general P2PMessages

public class BaseP2PMessage implements IP2PMessage {

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeToDataStream(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFromDataStream(DataInputStream in) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMsgType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMsgType(int msgType) {
		// TODO Auto-generated method stub

	}
	
	public void readFromParcel(Parcel source) {
		
	}

}
