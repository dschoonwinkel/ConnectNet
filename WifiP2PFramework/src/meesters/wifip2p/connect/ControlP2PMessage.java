package meesters.wifip2p.connect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import meesters.wifip2p.deps.BaseP2PMessage;
import meesters.wifip2p.deps.IP2PMessage;
import meesters.wifip2p.deps.Router;
import android.os.Parcel;
import android.os.Parcelable;

public class ControlP2PMessage extends BaseP2PMessage implements IP2PMessage {

	private int mControlCommand = -1;

	public ControlP2PMessage() {

	}

	public ControlP2PMessage(int controlCmd) {
		mControlCommand = controlCmd;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mControlCommand);
	}

	public static final Parcelable.Creator<ControlP2PMessage> CREATOR = new Parcelable.Creator<ControlP2PMessage>() {

		@Override
		public ControlP2PMessage createFromParcel(Parcel source) {

			ControlP2PMessage msg = new ControlP2PMessage();
			msg.mControlCommand = source.readInt();

			return msg;
		}

		@Override
		public ControlP2PMessage[] newArray(int size) {
			return new ControlP2PMessage[size];
		}

	};

	@Override
	public void writeToDataStream(DataOutputStream out) throws IOException {
		out.writeInt(mControlCommand);
	}

	@Override
	public void readFromDataStream(DataInputStream in) throws IOException {
		mControlCommand = in.readInt();

	}

	@Override
	public int getMsgType() {
		return Router.MsgType.CONTROL_MESSAGE;
	}

	@Override
	public void setMsgType(int msgType) {
		// Do nothing, this message cannot change type
	}

	public int getControlCommand() {
		return mControlCommand;
	}

	public void readFromParcel(Parcel source) {
		mControlCommand = source.readInt();
	}

}
