package org.oep.pong;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.zip.CRC32;

import android.util.Log;

public class PongController {

	private static ByteArrayOutputStream mBuffer = null;
	private static DataOutputStream mOutput = null;
	private static InetAddress remote_address = null;
	private static int remote_port = -1;
	private static DatagramSocket mSocket = null;
	private static boolean mIsServer = false;

	// Packet number, for testing purposes
	private static int mPacketNumber = 1;

	public static class MsgTypes {
		public static final int UPDATE_MSG = 0;
		public static final int DEBUG_MSG = 1;
	}

	private final static String TAG = "PongController";

	public static void startController(final String address, final String port) {
		Log.e(TAG, "Starting controller");
		new Thread() {
			@Override
			public void run() {

				// Set up byte buffer
				mBuffer = new ByteArrayOutputStream();
				mOutput = new DataOutputStream(mBuffer);

				try {
					remote_address = InetAddress.getByName(address);
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				remote_port = Integer.parseInt(port);

				Log.i(TAG, "Remote address : " + remote_address.toString());
				Log.i(TAG, "Remote port : " + remote_port);

				try {
					// This is the sending socket
					mSocket = new DatagramSocket(10002);
				} catch (NumberFormatException | IOException e) {
					e.printStackTrace();
					return;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}

		}.start();
	}

	public static void updateVariablesServer(final double x, final double y,
			final double vel_x, final double vel_y, final double bluepaddle_x,
			final double bluepaddle_y) {

		// Log.v(TAG, "updateVariables");
		Thread updateVariablesThread = new Thread() {

			public void run() {
				if (mSocket != null && mOutput != null) {
					try {
						mOutput.writeInt(MsgTypes.UPDATE_MSG);
						mOutput.writeInt(BALL_X);
						mOutput.writeDouble(x);
						mOutput.writeInt(BALL_Y);
						mOutput.writeDouble(y);
						mOutput.writeInt(BALL_VEL_X);
						mOutput.writeDouble(vel_x);
						mOutput.writeInt(BALL_VEL_Y);
						mOutput.writeDouble(vel_y);
						mOutput.writeInt(BLUEPADDLE_X);
						mOutput.writeDouble(bluepaddle_x);
						mOutput.writeInt(BLUEPADDLE_Y);
						mOutput.writeDouble(bluepaddle_y);
						mOutput.writeInt(mPacketNumber);

						double nanoTime = System.nanoTime();
						nanoTime /= 1e6;
						mOutput.writeDouble(nanoTime);

						// Compute CRC16 and send
//						int computedCRC = CRC16.computeCrc16(mCRCCheckStream.toByteArray());
						CRC32 checker = new CRC32();
						checker.update(mBuffer.toByteArray());
						long computedCRC = checker.getValue();
						checker.reset();
						mOutput.writeLong(computedCRC);

						mPacketNumber++;
						sendBufferedData();
					} catch (IOException e) {
						Log.e(TAG, "Error occured: " + e.getMessage());
						e.printStackTrace();
					}
				} else {
					// Log.e(TAG, "Not connected...");
				}
			}

		};
		updateVariablesThread.start();
	}

	public static void updateVariablesClient(final double redpaddle_x,
			final double redpaddle_y) {
		Thread updateVariablesThread = new Thread() {
			public void run() {
				try {
					mOutput.writeInt(MsgTypes.UPDATE_MSG);
					mOutput.writeInt(REDPADDLE_X);
					mOutput.writeDouble(redpaddle_x);
					mOutput.writeInt(REDPADDLE_Y);
					mOutput.writeDouble(redpaddle_y);
					mOutput.writeInt(mPacketNumber);

					double nanoTime = System.nanoTime();
					nanoTime /= 1e6;
					mOutput.writeDouble(nanoTime);

					//Compute CRC and send
					CRC32 checker = new CRC32();
					checker.update(mBuffer.toByteArray());
					long computedCRC = checker.getValue();
					checker.reset();
					mOutput.writeLong(computedCRC);

					mPacketNumber++;
					sendBufferedData();
				} catch (IOException e) {
					Log.e(TAG, "Error occured: " + e.getMessage());
					e.printStackTrace();
				}
			}
		};
		updateVariablesThread.start();
	}

	public static void sendDebugMessage(final int packetNumber,
			final double timestamp) {
		Thread sendDebugMessageThread = new Thread() {
			public void run() {
				try {
					mOutput.writeInt(MsgTypes.DEBUG_MSG);
					mOutput.writeInt(packetNumber);
					mOutput.writeDouble(timestamp);

					// Compute CRC32 and send
					CRC32 checker = new CRC32();
					checker.update(mBuffer.toByteArray());
					long computedCRC = checker.getValue();
					checker.reset();
					mOutput.writeLong(computedCRC);

					sendBufferedData();
				} catch (IOException e) {
					Log.e(TAG, "Error occured: " + e.getMessage());
					e.printStackTrace();
				}
			}
		};
		sendDebugMessageThread.start();
	}

	public static void writeInt(int data) {
		try {
			mOutput.writeInt(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeDouble(double data) {
		try {
			mOutput.writeDouble(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isReady() {
		Log.v(TAG, "isReady");
		if (mSocket == null) {
			return false;
		}
		if (mSocket.isBound()) {
			return true;
		}
		return false;
	}

	public static void sendBufferedData() {
		// Log.v(TAG, "sendingBufferedData");
		byte[] buf = mBuffer.toByteArray();
		DatagramPacket packet = new DatagramPacket(buf, buf.length,
				remote_address, remote_port);
		try {
			mSocket.send(packet);
			mBuffer.flush();
			mBuffer.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void onDestroy() {
		new Thread() {
			public void run() {
				if (mSocket != null) {
					mSocket.close();
					try {
						mBuffer.flush();
					} catch (IOException e) {
						Log.e(TAG, "Flusing buffer failed" + e.getMessage());
						e.printStackTrace();
					}
					mBuffer.reset();
				}
			}
		}.start();
	}

	public static final String[] updateKeys = { "ball_x", "ball_y",
			"ball_vel_x", "ball_vel_y", "bluepaddle_x", "bluepaddle_y",
			"redpaddle_x", "redpaddle_y" };

	public static final int BALL_X = 0;
	public static final int BALL_Y = 1;
	public static final int BALL_VEL_X = 2;
	public static final int BALL_VEL_Y = 3;
	public static final int REDPADDLE_X = 4;
	public static final int REDPADDLE_Y = 5;
	public static final int BLUEPADDLE_X = 6;
	public static final int BLUEPADDLE_Y = 7;

}
