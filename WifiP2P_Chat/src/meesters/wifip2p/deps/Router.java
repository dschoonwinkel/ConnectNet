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

import java.net.InetAddress;

//This is the class for all of the static variables that will be used by P2PConnectorService and its users

public class Router {
	// service startup intents for starting P2PConnectorService service
	public static final String ACTION_P2P_SERVICE = "meesters.wifip2p.router.P2PConnectorService";
	public static final String ACTION_P2PAPP = "meesters.wifip2p.app.WifiP2PApp";

	public static final int REQUEST_WIFIP2PAPP = 101;
	public static final int SUCCESS_WIFIP2PAPP = -1001;

	public final static class MsgType {
		public final static int DATA_MESSAGE = 1;
		public final static int CONTROL_MESSAGE = 2;
		public final static int PONG_MESSAGE = 3;
		public final static int PONG_ANSW_MESSAGE = 4;

		public static String MsgTypeToString(int msgType) {
			switch (msgType) {
			case DATA_MESSAGE:
				return "DATA_MESSAGE";
			case CONTROL_MESSAGE:
				return "CONTROL_MESSAGE";
			case PONG_MESSAGE:
				return "PONG_MESSAGE";
			case PONG_ANSW_MESSAGE:
				return "PONG_ANSW_MESSAGE"; 
			default:
				return "UNDEFINED";
			}
		}
	}
	
	public final static class ControlCodes {
		public static final int STOP_REMOTE_DISCOVERY = 1;
		public static final int START_REMOTE_DISCOVERY = 2;
	}

	public final static class P2P_ReceiverActions {
		public static final String SCAN_START = "P2PConnectorService_StartScan";
		public static final String SCAN_STOP = "P2PConnectorService_StopScan";

		public static final String CONNECTING = "P2PConnectorService_Connecting";
		public static final String DISCONNECTING = "P2PConnectorService_Disconnecting";

		public static final String PEER_CONNECTED = "P2PConnectorService_Peerconnected";
		public static final String PEER_DISCONNECTED = "P2PConnectorService_Peerdisconnected";

		public static final String PEER_LIST_AVAILABLE = "P2PConnectorService_PeerlistAvailable";
		public static final String EXTRA_PEER_LIST = "P2PConnectorService_Peerlist_EXTRA";

		public static final String GROUP_OWNER = "P2PConnectorService_GroupOwner";
		public static final String EXTRA_GROUP_OWNER = "P2PConnectorService_GroupOwner_EXTRA";

		public static final String CONSUMER_STATE = "P2PConnectorService_ConsumerState";
		public static final String EXTRA_CONSUMER_STATE = "P2PConnectorService_ConsumerState_EXTRA";

		public static final String STOP_ALL = "P2PConnectorService_StopAll";

		// TODO: This is a very explicit broadcast: consider revising
		public static final String UPDATE_SERVICES_VIEW = "P2PConnectorService_UpdateServicesView";
		public static final String EXTRA_SERVICES_VIEW = "P2PConnectorService_UpdateServicesView_EXTRA";

		public static final String P2P_SERVICE_DESCRIPTOR = "P2PConnectorService_P2PServiceDescriptor";
		public static final String EXTRA_P2P_SERVICE_DESCRIPTOR = "P2PConnectorService_ExtraP2PServiceDescriptor";

		public static final String CLEAR_SERVICES_VIEW = "P2PConnectorService_ClearServicesView";

		public static final String DISCOVERY_STARTED = "P2PConnectorService_DiscoveryStarted";
		public static final String DISCOVERY_STOPPED = "P2PConnectorService_DiscoveryStopped";

		public static final String BYTES_RECEIVED = "P2PConnectorService_BytesReceived";
		public static final String EXTRA_BYTES_RECEIVED = "P2PConnectorService_ExtraBytesReceived";
		public static final String BYTES_SEND = "P2PConnectorService_BytesSend";
		public static final String EXTRA_BYTES_SEND = "P2PConnectorService_ExtraBytesSend";

		// Device Roles, ie. Services Consumer/ Provider
		public static final String DEVICE_ROLE = "P2PConnectorService_DeviceRole";
		public static final String EXTRA_DEVICE_ROLE = "P2PConnectorService_ExtraDeviceRole";

		// Toggle Wifi states, used to reset if device is misbehaving
		public static final String TOGGLE_WIFI = "P2PConnectorService_ToggleWifi";
		public static final String EXTRA_TOGGLE_WIFI = "P2PConnectorService_ExtraToggleWifi";

		// Get the local device info
		public static final String GET_THISP2PDEVICE = "P2PConnectorService_GetP2PDevice";
		public static final String EXTRA_GET_THISP2PDEVICE = "P2PConnectorService_ExtraGetP2PDevice";

		// Request for the service to reply with the LocalP2PAddress
		public static final String REQUEST_WIFIP2P_ADDRESS = "P2PConnectorService_RequestLocalP2PAddress";

		// Response to above mentioned request
		public static final String GET_LOCALP2PADDRESS = "P2PConnectorService_GetLocalP2PAddress";
		public static final String EXTRA_GET_LOCALP2PADDRESS = "P2PConnectorService_ExtraGetLocalP2PAddress";

		
	}
	
	public static class P2P_ServiceDebugActions {
		public static final String DEBUG_RESET_CONNECTION = "P2PConnectorService_DebugRemoveGroup";
		public static final String DEBUG_START_DISCOVERY = "P2PConnectorService_DebugStartDiscovery";
		public static final String DEBUG_STOP_DISCOVERY = "P2PConnectorService_DebugStopDiscovery";
	}

	public static class P2P_STATES {
		public static final String CONNECTED = "CONNECTED";
		public static final String DISCONNECTED = "DISCONNECTED";
	}

	public static class P2P_EXTRA_KEYS {
		public static final String PORTNUM_KEY = "PORTNUM";
		public static final String INETADDR_KEY = "INETADDR_KEY";
	}

	// Global InetAddress for the local WifiP2P Address
	public static InetAddress mWifiP2PLocalAddress = null;

	public static void setWifiP2PLocalAddress(InetAddress address) {
		mWifiP2PLocalAddress = address;
	}

	public static InetAddress getWifiP2PLocalAddress() {
		return mWifiP2PLocalAddress;
	}
}
