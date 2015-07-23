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

import android.os.Parcelable;

public interface IP2PMessage extends Parcelable {
	public void writeToDataStream(DataOutputStream out) throws IOException;
	public void readFromDataStream(DataInputStream in) throws IOException;
	public int getMsgType();
	public void setMsgType(int msgType);
}
