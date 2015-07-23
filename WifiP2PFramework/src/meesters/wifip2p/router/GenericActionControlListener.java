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

package meesters.wifip2p.router;

import meesters.wifip2p.deps.GenericActionListener;
import android.util.Log;

public class GenericActionControlListener extends GenericActionListener implements IControlChannel {

	private static final String TAG = "ActionListener";
	private String mMessage = "";
	private IControlChannel mControlChannel = null;
	private int mState = 0;
	
	public GenericActionControlListener(String message) {
		super(message);
	}
	
	public GenericActionControlListener(String message, IControlChannel channel) {
		super(message);
		mMessage = message;
		mControlChannel = channel;
	}
	
	@Override
	public void onFailure(int reason) {
		Log.e(TAG, mMessage + " failed with reason: " + onErrorCode(reason));
		//Pass it on to the correct handler
		status(mMessage, reason);
	}

	@Override
	public void onSuccess() {
		Log.d(TAG, mMessage + " successful");
		//Status -1 signifying no error has occured
		status(mMessage, -1);
	}

	@Override
	public void status(String message, int state) {
		//Pass this control message to the correct handler
		mControlChannel.status(message, state);		
	}

	@Override
	public int getStatus() {
		return mState;
	}

}
