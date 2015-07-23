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
import java.util.Arrays;

import meesters.wifip2p.deps.BaseP2PMessage;
import meesters.wifip2p.deps.IP2PMessage;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class P2PMessage extends BaseP2PMessage implements Parcelable, IP2PMessage {

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
		//This needs to be initialized to avoid NPEs later. Overwritten with readFromDataStream
		mData = new byte[0];
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
			try {
			source.readByteArray(data); // This could be dangerous? Expect a
										// null pointer exception here?
			} catch(NullPointerException npe) {
				Log.e(TAG, "createFromParcel read a null byte array, creating new one");
				data = new byte[0];
			}

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
	
	@Override
	public void readFromParcel(Parcel source) {
		mAPP_ID = source.readString();
		mAPP_UUID = source.readString();
		mMsgType = source.readInt();
		mFromDev = source.readString();
		mTargetDev = new WifiP2pDevice();
		mTargetDev.deviceName = source.readString();
		mTargetDev.deviceAddress = source.readString();
		mDataLength = source.readInt();
		mData = new byte[mDataLength];
		source.readByteArray(mData);
	}

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

	@Override
	public void setMsgType(int msgType) {
		mMsgType = msgType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 17;
		int result = 1;
		result = prime * result + ((mAPP_ID == null) ? 0 : mAPP_ID.hashCode());
		result = prime * result
				+ ((mAPP_UUID == null) ? 0 : mAPP_UUID.hashCode());
		result = prime * result + Arrays.hashCode(mData);
		result = prime * result + mDataLength;
		result = prime * result
				+ ((mFromDev == null) ? 0 : mFromDev.hashCode());
		result = prime * result + mMsgType;
		result = prime * result
				+ ((mTargetDev == null) ? 0 : mTargetDev.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		P2PMessage other = (P2PMessage) obj;
		if (mAPP_ID == null) {
			if (other.mAPP_ID != null)
				return false;
		} else if (!mAPP_ID.equals(other.mAPP_ID))
			return false;
		if (mAPP_UUID == null) {
			if (other.mAPP_UUID != null)
				return false;
		} else if (!mAPP_UUID.equals(other.mAPP_UUID))
			return false;
		if (!Arrays.equals(mData, other.mData))
			return false;
		if (mDataLength != other.mDataLength)
			return false;
		if (mFromDev == null) {
			if (other.mFromDev != null)
				return false;
		} else if (!mFromDev.equals(other.mFromDev))
			return false;
		if (mMsgType != other.mMsgType)
			return false;
		if (mTargetDev == null) {
			if (other.mTargetDev != null)
				return false;
		} else if (!mTargetDev.equals(other.mTargetDev))
			return false;
		return true;
	}

}
