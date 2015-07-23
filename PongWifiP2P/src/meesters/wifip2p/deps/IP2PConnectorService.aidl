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

import meesters.wifip2p.deps.BaseP2PMessage;
import meesters.wifip2p.deps.P2PServiceDescriptor;

interface IP2PConnectorService {
	void getPeerList();
	void connectPeer(int peer);
	void connectToAddress(String deviceAddress, int GOIntent);
	
	void startDiscovery();
	void stopDiscovery();
	
	void resetConnection();
	void resetServices();
	void resetAll();
	
	void getStates();
	void getThisP2pDevice();
	String getLocalP2PAddress();
	
	int send(out BaseP2PMessage msg);
	
	int AdvertiseP2PService(String service_name, String service_type, int portnum, in Map extras);
	boolean unAdvertiseP2PService(String service_name, int ServiceAdNumber);
	
	P2PServiceDescriptor[] getCurrentP2PServices();
}