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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class P2PMessage implements Parcelable, IP2PMessage {

	/**
	 * 
	 */
	// private static final long serialVersionUID = 539731704309164538L;
	private static final String TAG = "P2PMessage";

	private String mAPP_ID = "";
	private String mAPP_UUID = "";
	private int mMsgType = -1;
	private String mFromDev = "";
	private WifiP2pDevice mTargetDev = null;
	private int mDataLength = 0;
	private byte[] mData;

	
	//Empty constructor for use with DataInputStreams
	public P2PMessage() {
		
	}
	
	public P2PMessage(String fromAppID, String fromAppUUID, int msgType,
			String fromDev, WifiP2pDevice targetDev, int dataLength, byte[] data) {
		mAPP_ID = fromAppID;
		mAPP_UUID = fromAppUUID;
		mMsgType = msgType;
		mFromDev = fromDev;
		mTargetDev = targetDev;
		mDataLength = dataLength;
		mData = data;
	}

	public static final Parcelable.Creator<P2PMessage> CREATOR = new Parcelable.Creator<P2PMessage>() {

		@Override
		public P2PMessage createFromParcel(Parcel source) {
			String APP_ID = source.readString();
			String APP_UUID = source.readString();
			int msgType = source.readInt();
			String fromDev = source.readString();
			WifiP2pDevice targetDev = source.readParcelable(WifiP2pDevice.class
					.getClassLoader());
			int dataLength = source.readInt();
			byte[] data = new byte[dataLength];
			source.readByteArray(data); // This could be dangerous? Expect a
										// null pointer exception here?

			return new P2PMessage(APP_ID, APP_UUID, msgType, fromDev,
					targetDev, dataLength, data);
		}

		@Override
		public P2PMessage[] newArray(int size) {
			return new P2PMessage[size];
		}
	};

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mAPP_ID);
		dest.writeString(mAPP_UUID);
		dest.writeInt(mMsgType);
		dest.writeString(mFromDev);
		dest.writeParcelable(mTargetDev, flags);
		dest.writeInt(mDataLength);
		dest.writeByteArray(mData);
	}

	// If the object is not Serializable by default means...
	// private void writeObject(ObjectOutputStream out) throws IOException {
	//
	// }
	//
	// private void readObject(ObjectOutputStream in) throws IOException,
	// ClassNotFoundException {
	//
	// }

	public String getAPP_ID() {
		return mAPP_ID;
	}

	public String getAPP_UUID() {
		return mAPP_UUID;
	}

	public int getMsgType() {
		return mMsgType;
	}

	public String getFromDev() {
		return mFromDev;
	}

	public WifiP2pDevice getTargetDev() {
		return mTargetDev;
	}

	public int getDataLength() {
		return mDataLength;
	}

	public byte[] getData() {
		return mData;
	}

	@Override
	public String toString() {
		String string = "";
		string += "APP_ID: " + this.mAPP_ID;
		string += " APP_UUID: " + mAPP_UUID;
		string += " MsgType: " + mMsgType;
		string += " FromDev: " + mFromDev;
		if (mTargetDev != null) {
			string += " TargetDev: " + mTargetDev.deviceName + " "
					+ mTargetDev.deviceAddress;
		} else {
			string += " TargetDev: " + "null" + " " + "null";
		}
		string += " Data Length: " + mDataLength;
		string += " Data: " + new String(mData);
		return string;
	}

	@Override
	public void writeToDataStream(DataOutputStream out) throws IOException {
		// TODO: Change this to a more robust implementation
		try {
			out.writeUTF(mAPP_ID);
			out.writeUTF(mAPP_UUID);
			out.writeInt(mMsgType);
			if (mFromDev == null) {
				out.writeUTF("null");
			} else {
				out.writeUTF(mFromDev);
			}

			if (mTargetDev == null) {
				out.writeUTF("null");
				out.writeUTF("null");
			} else {
				out.writeUTF(mTargetDev.deviceName);
				out.writeUTF(mTargetDev.deviceAddress);
			}

			out.writeInt(mDataLength);
			out.write(mData, 0, mData.length);

		} catch (IOException ioe) {
			Log.e(TAG, "IOException occurred");
			throw ioe;
		}

	}

	@Override
	public void readFromDataStream(DataInputStream in) throws IOException {
		// TODO: Change this to a more robust implementation
		try {
			mAPP_ID = in.readUTF();
			mAPP_UUID = in.readUTF();
			mMsgType = in.readInt();
			mFromDev = in.readUTF();
			mTargetDev = new WifiP2pDevice();
			mTargetDev.deviceName = in.readUTF();
			mTargetDev.deviceAddress = in.readUTF();
			mDataLength = in.readInt();
			mData = new byte[mDataLength];
			in.read(mData, 0, mData.length);

		} catch (IOException ioe) {
			Log.e(TAG, "IOException occurred");
			throw ioe;
		}
	}

}
