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

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Parcelable class for sending around P2P Service information
 * 
 * @author schoonwi
 * 
 */
public class P2PServiceDescriptor implements Parcelable {

	public final String fullDomainName;
	public final WifiP2pDevice srcDevice;
	public final Map<String, String> extras;

	public P2PServiceDescriptor(String fullDomainName,
			Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
		this.fullDomainName = fullDomainName;
		this.extras = txtRecordMap;
		this.srcDevice = srcDevice;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(fullDomainName);
		dest.writeParcelable(srcDevice, flags);

		// Write the size of the map
		dest.writeInt(extras.size());

		// Add each key-value pair in the Map to the Parcel
		for (Map.Entry<String, String> entry : extras.entrySet()) {
			dest.writeString(entry.getKey());
			dest.writeString(entry.getValue());
		}
	}

	public static final Creator<P2PServiceDescriptor> CREATOR = new Creator<P2PServiceDescriptor>() {
		public P2PServiceDescriptor createFromParcel(Parcel source) {
			return new P2PServiceDescriptor(source);
		}

		@Override
		public P2PServiceDescriptor[] newArray(int size) {
			return new P2PServiceDescriptor[size];
		}
	};

	// Private constructor used by the CREATOR
	private P2PServiceDescriptor(Parcel source) {
		fullDomainName = source.readString();
		srcDevice = source.readParcelable(WifiP2pDevice.class.getClassLoader());
		int map_size = source.readInt();
		HashMap<String, String> txtRecordMap = new HashMap<String, String>();

		for (int i = 0; i < map_size; i++) {
			txtRecordMap.put(source.readString(), source.readString());
		}
		extras = txtRecordMap;
	}

	@Override
	public String toString() {
		String returnString = "";

		returnString += "fullDomainName: " + fullDomainName + " ";
		returnString += "srcDevice: " + srcDevice.toString() + " ";
		returnString += "extras: " + extras.toString();

		return returnString;
	}

	@Override
	public int hashCode() {
		final int prime = 17;
		int result = 1;
		result = prime * result + ((extras == null) ? 0 : extras.hashCode());
		result = prime * result
				+ ((fullDomainName == null) ? 0 : fullDomainName.hashCode());
		result = prime * result
				+ ((srcDevice == null) ? 0 : srcDevice.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof P2PServiceDescriptor)) {
			return false;
		}
		P2PServiceDescriptor other = (P2PServiceDescriptor) obj;
		if (extras == null) {
			if (other.extras != null) {
				return false;
			}
		} else if (!extras.equals(other.extras)) {
			return false;
		}
		if (fullDomainName == null) {
			if (other.fullDomainName != null) {
				return false;
			}
		} else if (!fullDomainName.equals(other.fullDomainName)) {
			return false;
		}
		if (srcDevice == null) {
			if (other.srcDevice != null) {
				return false;
			}
		} else if (!srcDevice.equals(other.srcDevice)) {
			// This is a particularly strong comparison, consider revising
			return false;
		}
		return true;
	}
	
	public Intent getThisIntent() {
		Intent descriptorIntent = new Intent(
				Router.P2P_ReceiverActions.P2P_SERVICE_DESCRIPTOR);
		descriptorIntent.putExtra(
				Router.P2P_ReceiverActions.EXTRA_P2P_SERVICE_DESCRIPTOR,
				this);
		
		return descriptorIntent;
	}

}
