package org.oep.pong;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.zip.CRC32;

import org.oep.pong.PongController.MsgTypes;

import android.util.Log;

public class PongClientThread extends Thread {

	private static final String TAG = "PongClientThread";

	private PongView mPongView = null;
	private DatagramSocket mSocket = null;
	private InetAddress mAddress = null;
	private int mPort = -1;
	private boolean mIsServer = false;

	// private DataInputStream mInputStream = null;

	private int mReceivedMsgType = -1;
	private int[] receivedInts = new int[8];
	private double[] receivedDoubles = new double[8];
	private int mPacketNumber = -1;
	private int mPrevPacketNumber = -1;
	private int mDebugPacketNumber = -1;
	private int mDebugPrevPacketNumber = -1;
	private long mPacketsLost = 0;
	private long mTotalPackets = 0;
	private String mTimestamp = "";
	private double mNanoTime = 0;

	public PongClientThread(PongView view, boolean isServer) {
		mPongView = view;
		mIsServer = isServer;
		Log.v(TAG, "Constructor");
		new Thread() {
			public void run() {
				try {
					mSocket = new DatagramSocket(10001);
					mSocket.setReuseAddress(true);
					// Log.d(TAG, "IP Address" +
					// mSocket.getInetAddress().toString());
					// Start the receiving as soon as the Socket is created
					PongClientThread.this.start();
				} catch (IOException e) {
					Log.e(TAG, "Could not open socket");
				}
			}
		}.start();
	}

	@Override
	public void run() {
		Log.v(TAG, "run");
		byte[] buf = null;
		try {
			buf = new byte[mSocket.getReceiveBufferSize()];
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RuntimeException("Could not create buffer");
		}
		DatagramPacket packet = null;

		while (true) {
			packet = new DatagramPacket(buf, buf.length);
			try {
				mSocket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			DataInputStream mInputStream = new DataInputStream(
					new ByteArrayInputStream(buf));
			
			ByteArrayOutputStream mCRCCheckStream = new ByteArrayOutputStream();
			DataOutputStream mCRCOutputStream = new DataOutputStream(mCRCCheckStream);
			// Log.i(TAG, "Packet received: ");

			try {
				mReceivedMsgType = mInputStream.readInt();
				mCRCOutputStream.writeInt(mReceivedMsgType);
				
				if (mReceivedMsgType == MsgTypes.UPDATE_MSG) {

					if (!mIsServer) {
						
						// Ball_X
						receivedInts[0] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[0]);
						receivedDoubles[0] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[0]);
						// Log.d(TAG, "Received: " + receivedInts[0] + " "
						// + receivedDoubles[0]);
						// Ball_y
						receivedInts[1] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[1]);
						receivedDoubles[1] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[1]);
						// Log.d(TAG, "Received: " +receivedInts[1] + " "
						// +receivedDoubles[1]);
						// Ball_vel_x
						receivedInts[2] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[2]);
						receivedDoubles[2] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[2]);
						// Log.d(TAG, "Received: " +receivedInts[2] + " "
						// +receivedDoubles[2]);
						// Ball_vel_y
						receivedInts[3] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[3]);
						receivedDoubles[3] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[3]);
						// Log.d(TAG, "Received: " +receivedInts[3] + " "
						// +receivedDoubles[3]);

						// Bluepaddle_x
						receivedInts[6] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[6]);
						receivedDoubles[6] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[6]);
						// Log.d(TAG, "Received: " +receivedInts[6] + " "
						// +receivedDoubles[6]);
						// Bluepaddle_y
						receivedInts[7] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[7]);
						receivedDoubles[7] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[7]);
						// Log.d(TAG, "Received: " +receivedInts[7] + " "
						// +receivedDoubles[7]);

						// Get the packet number, for testing purposes
						mPacketNumber = mInputStream.readInt();
						mCRCOutputStream.writeInt(mPacketNumber);
						mNanoTime = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(mNanoTime);
						
						long receivedCRC = mInputStream.readLong();
//						int computedCRC = CRC16.computeCrc16(mCRCCheckStream.toByteArray());
						CRC32 checker = new CRC32();
						checker.update(mCRCCheckStream.toByteArray());
						long computedCRC = checker.getValue();
						checker.reset();
						
//						Log.e(TAG, receivedCRC == computedCRC ? "CRC equal ":"CRC differs " +  "received CRC: " + Long.toHexString(receivedCRC) + " computed CRC: " + Long.toHexString(computedCRC));
						
						//Assume that this packet was gibberish
//						if (mPrevPacketNumber != -1 && (mPacketNumber > 100000 || mPacketNumber <= 0 || Math.abs(mPacketNumber - mPrevPacketNumber) > 1000)) {
//							Log.e(TAG, "Gibberish Packet number: " + mPacketNumber);
//							continue;
//						}
						
						if (receivedCRC != computedCRC) {
							Log.e(TAG, "CRC differs " +  "received CRC: " + Long.toHexString(receivedCRC) + " computed CRC: " + Long.toHexString(computedCRC));
							Log.e(TAG, "Discarding packet");
							continue;
						}
						
						//Assume that this packet was gibberish
						if (mPrevPacketNumber != -1 && (Math.abs(mPacketNumber - mPrevPacketNumber) > 1000)) {
							Log.e(TAG, "Gibberish Packet number: " + mPacketNumber);
							continue;
						}

						mTotalPackets++;
						Log.i(TAG, "Packets: " + mTotalPackets + " errors: "
								+ mPacketsLost + " " + mPacketNumber + ":"
								+ mPrevPacketNumber + " timestamp " + mNanoTime);
						Logger.logMessage(TAG, "Packets: " + mTotalPackets + " errors: "
								+ mPacketsLost + " " + mPacketNumber + ":"
								+ mPrevPacketNumber + " timestamp " + mNanoTime);

						// Update the packets lost counter
						if (mPrevPacketNumber == -1) {
							mPrevPacketNumber = mPacketNumber;
						} else {
							if (mPacketNumber > mPrevPacketNumber + 1) {
								mPacketsLost += mPacketNumber
										- mPrevPacketNumber - 1;
								Log.e(TAG, "Packets lost: " + mPacketsLost);
//								if (mPacketsLost > 100000) {
//									throw new RuntimeException("Packets lost you idiot!");
//								}
								mPrevPacketNumber = mPacketNumber;
							} else {
								mPrevPacketNumber = mPacketNumber;
							}
						}
						PongController.sendDebugMessage(mPacketNumber, mNanoTime);

					} else if (mIsServer) {
						// Redpaddle_x
						receivedInts[4] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[4]);
						receivedDoubles[4] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[4]);
						// Log.i(TAG, "Received: " + receivedInts[4] + " "
						// + receivedDoubles[4]);
						// Redpaddle_y
						receivedInts[5] = mInputStream.readInt();
						mCRCOutputStream.writeInt(receivedInts[5]);
						receivedDoubles[5] = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(receivedDoubles[5]);
						// Log.i(TAG, "Received: " + receivedInts[5] + " "
						// + receivedDoubles[5]);

						// Read the packet number, for testing purposes
						mPacketNumber = mInputStream.readInt();
						mCRCOutputStream.writeInt(mPacketNumber);
						mNanoTime = mInputStream.readDouble();
						mCRCOutputStream.writeDouble(mNanoTime);

						long receivedCRC = mInputStream.readLong();
//						int computedCRC = CRC16.computeCrc16(mCRCCheckStream.toByteArray());
						CRC32 checker = new CRC32();
						checker.update(mCRCCheckStream.toByteArray());
						long computedCRC = checker.getValue();
						checker.reset();
						
//						Log.e(TAG, receivedCRC == computedCRC ? "CRC equal ":"CRC differs " +  "received CRC: " + Long.toHexString(receivedCRC) + " computed CRC: " + Long.toHexString(computedCRC));
						
						if (receivedCRC != computedCRC) {
							Log.e(TAG, "CRC differs " +  "received CRC: " + Long.toHexString(receivedCRC) + " computed CRC: " + Long.toHexString(computedCRC));
							Log.e(TAG, "Discarding packet");
							continue;
						}
						
						//Assume that this packet was gibberish
						if (mPrevPacketNumber != -1 && (Math.abs(mPacketNumber - mPrevPacketNumber) > 1000)) {
							Log.e(TAG, "Gibberish Packet number: " + mPacketNumber);
							continue;
						}
						
						mTotalPackets++;
						Log.i(TAG, "Packets: " + mTotalPackets + " errors: "
								+ mPacketsLost + " " + mPacketNumber + ":"
								+ mPrevPacketNumber + " timestamp " + mNanoTime);
						Logger.logMessage(TAG, "Packets: " + mTotalPackets + " errors: "
								+ mPacketsLost + " " + mPacketNumber + ":"
								+ mPrevPacketNumber + " timestamp " + mNanoTime);

						// Update the packets lost counter
						if (mPrevPacketNumber == -1) {
							mPrevPacketNumber = mPacketNumber;
						} else {
							if (mPacketNumber > mPrevPacketNumber + 1) {
								mPacketsLost += mPacketNumber
										- mPrevPacketNumber - 1;
								Log.e(TAG, "Packets lost: " + mPacketsLost);
//								if (mPacketsLost > 100000 || mPacketNumber < -1) {
//									throw new RuntimeException("Packets lost you idiot!");
//								}
								mPrevPacketNumber = mPacketNumber;
							} else {
								mPrevPacketNumber = mPacketNumber;
							}
						}

						PongController.sendDebugMessage(mPacketNumber, mNanoTime);
					}
				}
				
				if (mReceivedMsgType == MsgTypes.DEBUG_MSG) {
					mDebugPacketNumber = mInputStream.readInt();
					mCRCOutputStream.writeInt(mDebugPacketNumber);
					
					double timestamp = mInputStream.readDouble();
					mCRCOutputStream.writeDouble(timestamp);
					
					long receivedCRC = mInputStream.readLong();
//					int computedCRC = CRC16.computeCrc16(mCRCCheckStream.toByteArray());
					CRC32 checker = new CRC32();
					checker.update(mCRCCheckStream.toByteArray());
					long computedCRC = checker.getValue();
					checker.reset();
					
					
//					Log.e(TAG, receivedCRC == computedCRC ? "Debug CRC equal ":"Debug CRC differs " +  "received CRC: " + Long.toHexString(receivedCRC) + " computed CRC: " + Long.toHexString(computedCRC));
					
					if (receivedCRC != computedCRC) {
						Log.e(TAG, "Debug CRC differs " +  "received CRC: " + Long.toHexString(receivedCRC) + " computed CRC: " + Long.toHexString(computedCRC));
						Log.e(TAG, "Discarding debug packet");
						continue;
					}
					
					//Use the CRC check to discard packets
					if (mDebugPrevPacketNumber != -1 && (Math.abs(mDebugPacketNumber - mDebugPrevPacketNumber) > 1000)) {
						Log.e(TAG, "Gibberish Debug Packet number: " + mDebugPacketNumber);
						continue;
					}
					
//					Log.i("DEBUG_MSG", "Debug packet nr: " +mDebugPacketNumber);
					
					double timenow = System.nanoTime();
					timenow /= 1e6;
					double latency = timenow - timestamp;
					
					
					Log.i("DEBUG_MSG", "Packet timestamp: " +timestamp + " Time now: " + timenow + " Latency: " + latency);
					Logger.logMessage("DEBUG_MSG", "Packet timestamp: " +timestamp + " Time now: " + timenow + " Latency: " + latency);
					
					mDebugPrevPacketNumber = mDebugPacketNumber;
					
				}

			} catch (IOException e) {
				Log.e(TAG, "IOException occurred");
				// Log.e(TAG, e.getMessage());

				// Do not process this packet...
				continue;
			}

			if (mReceivedMsgType == MsgTypes.UPDATE_MSG) {
				// When all was received successfully, process the packet
				mPongView.receiveGameState(receivedDoubles[0],
						receivedDoubles[1], receivedDoubles[2],
						receivedDoubles[3], receivedDoubles[4],
						receivedDoubles[5], receivedDoubles[6],
						receivedDoubles[7]);
			}

		}
	}

	@Override
	public void interrupt() {
		super.interrupt();

		if (mSocket != null) {
			mSocket.close();
		}
	}

	public static final String[] updateKeys = { "ball_x", "ball_y",
			"ball_vel_x", "ball_vel_y", "bluepaddle_x", "bluepaddle_y",
			"redpaddle_x", "redpaddle_y" };

	public static final int BALL_X = 0;
	public static final int BALL_Y = 1;
	public static final int BALL_VEL_X = 2;
	public static final int BALL_VEL_Y = 3;
	public static final int BLUEPADDLE_X = 4;
	public static final int BLUEPADDLE_Y = 5;
	public static final int REDPADDLE_X = 6;
	public static final int REDPADDLE_Y = 7;

}
