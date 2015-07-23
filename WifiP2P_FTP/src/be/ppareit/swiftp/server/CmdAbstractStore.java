/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.ppareit.swiftp.server;

/**
 * CmdAbstractListing is the abstract base class for CmdSTOR, CmdAPPE - both storing the received file
 * named in the parameter. Since STOR and APPE are essentially identical except for append vs truncate,
 * the common code is in this class, and inherited by CmdSTOR and CmdAPPE.
 * 
 * @author David Revell, Pieter Pareit, Daniel Schoonwinkel
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.util.Log;
import be.ppareit.swiftp.Defaults;

abstract public class CmdAbstractStore extends FtpCmd {
	private final static String TAG = "CmdAbstractStore"; // TODO:
															// .class.getSimpleName()
															// from abstract
															// class
	private StatusListener mStatusListener = null;
	public static final String message = "TEMPLATE!!";

	public CmdAbstractStore(SessionThread sessionThread, String input,
			StatusListener status_listener) {
		super(sessionThread);
		mStatusListener = status_listener;
	}

	public void doStorOrAppe(String param, boolean append) {
		long nowtime = 0, prevtime = 0;
		long totalNumBytesRead = 0, prevNumBytesRead = 0;
		int counter = 0;

		Log.d(TAG, "STOR/APPE executing with append=" + append);
		mStatusListener.updateStatus("STOR/APPE executing with append="
				+ append);
		mStatusListener.updateStatus(StatusListener.STARTED_SUCCESSFULLY);
		File storeFile = inputPathToChrootedFile(sessionThread.getWorkingDir(),
				param);

		String errString = null;
		FileOutputStream out = null;
		// DedicatedWriter dedicatedWriter = null;
		// int origPriority = Thread.currentThread().getPriority();
		// myLog.l(Log.DEBUG, "STOR original priority: " + origPriority);
		storing: {

			// Get a normalized absolute path for the desired file
			if (violatesChroot(storeFile)) {
				errString = "550 Invalid name or chroot violation\r\n";
				break storing;
			}
			if (storeFile.isDirectory()) {
				errString = "451 Can't overwrite a directory\r\n";
				break storing;
			}

			try {
				if (storeFile.exists()) {
					if (!append) {
						if (!storeFile.delete()) {
							errString = "451 Couldn't truncate file\r\n";
							break storing;
						}
						// Notify other apps that we just deleted a file
						// MediaUpdater.notifyFileDeleted(storeFile.getPath());
					}
				}
				out = new FileOutputStream(storeFile, append);
			} catch (FileNotFoundException e) {
				try {
					errString = "451 Couldn't open file \"" + param
							+ "\" aka \"" + storeFile.getCanonicalPath()
							+ "\" for writing\r\n";
				} catch (IOException io_e) {
					errString = "451 Couldn't open file, nested exception\r\n";
				}
				break storing;
			}
			if (!sessionThread.startUsingDataSocket()) {
				errString = "425 Couldn't open data socket\r\n";
				break storing;
			}
			Log.d(TAG, "Data socket ready");

			// This should not be written, as we are a CLIENT
			// sessionThread.writeString("150 Data socket ready\r\n");

			byte[] buffer = new byte[Defaults.getDataChunkSize()];
			// dedicatedWriter = new DedicatedWriter(out);
			// dedicatedWriter.start(); // start the writer thread executing
			// myLog.l(Log.DEBUG, "Started DedicatedWriter");
			int numRead;
			// Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			// int newPriority = Thread.currentThread().getPriority();
			// myLog.l(Log.DEBUG, "New STOR prio: " + newPriority);
			if (sessionThread.isBinaryMode()) {
				Log.d(TAG, "Mode is binary");
			} else {
				Log.d(TAG, "Mode is ascii");
			}
			prevtime = System.nanoTime();
			while (true) {
				/*
				 * if(dedicatedWriter.checkErrorFlag()) { errString =
				 * "451 File IO problem\r\n"; break storing; }
				 */
				switch (numRead = sessionThread.receiveFromDataSocket(buffer)) {
				case -1:
					Log.d(TAG, "Returned from final read");
					// We're finished reading
					break storing;
				case 0:
					errString = "426 Couldn't receive data\r\n";
					break storing;
				case -2:
					errString = "425 Could not connect data socket\r\n";
					break storing;
				default:
					totalNumBytesRead += numRead;
					// Sub sampling the speed measurements
					if (counter > 5) {
						
						nowtime = System.nanoTime();
						double time_diff = (nowtime - prevtime);
						time_diff /= 1e9;
						double byte_diff = totalNumBytesRead - prevNumBytesRead;
						double linkspeed = byte_diff/time_diff;
//						Log.v(TAG, "Byte difference " + byte_diff);
//						Log.v(TAG, "Time difference " + time_diff);
						linkspeed /= 1000; // Linkspeed in KB/s
						Log.v("BANDWIDTH", "Linkspeed " + linkspeed);

						// Format num of Bytes in readable units
						DecimalFormatSymbols symbols = new DecimalFormatSymbols();
						symbols.setGroupingSeparator(' ');
						DecimalFormat format = new DecimalFormat("#,###",
								symbols);
						String formatted = format
								.format(totalNumBytesRead / 1000);
						String formatted_linkspeed = format.format(linkspeed);

						mStatusListener
								.updateStatus(StatusListener.BUSY_WORKING
										+ formatted + " KBs" + " Speed: "
										+ formatted_linkspeed + " KB/s");
						// Log.d(TAG, "Enqueueing buffer of " + numRead);
						// dedicatedWriter.enqueueBuffer(buffer, numRead);
						prevtime = System.nanoTime();
						prevNumBytesRead = totalNumBytesRead;
						counter = 0;
					}
					counter++;

					try {

						if (sessionThread.isBinaryMode()) {
							out.write(buffer, 0, numRead);
						} else {
							// ASCII mode, substitute \r\n to \n
							int startPos = 0, endPos;
							for (endPos = 0; endPos < numRead; endPos++) {
								if (buffer[endPos] == '\r') {
									// Our hacky method is to drop all \r
									out.write(buffer, startPos, endPos
											- startPos);
									startPos = endPos + 1;
								}
							}
							// Write last part of buffer as long as there was
							// something
							// left after handling the last \r
							if (startPos < numRead) {
								out.write(buffer, startPos, endPos - startPos);
							}
						}

						// Attempted bugfix for transfer stalls. Reopen file
						// periodically.
						// bytesSinceReopen += numRead;
						// if(bytesSinceReopen >= Defaults.bytes_between_reopen
						// &&
						// Defaults.do_reopen_hack) {
						// Log.d(TAG, "Closing and reopening file: " +
						// storeFile);
						// out.close();
						// out = new FileOutputStream(storeFile,
						// true/*append*/);
						// bytesSinceReopen = 0;
						// }

						// Attempted bugfix for transfer stalls. Flush file
						// periodically.
						// bytesSinceFlush += numRead;
						// if(bytesSinceFlush >= Defaults.bytes_between_flush &&
						// Defaults.do_flush_hack) {
						// Log.d(TAG, "Flushing: " + storeFile);
						// out.flush();
						// bytesSinceFlush = 0;
						// }

						// If this transfer fails, a later APPEND operation
						// might be
						// received. In that case, we will need to have flushed
						// the
						// previous writes in order for the append to work. The
						// filesystem on my G1 doesn't seem to recognized
						// unflushed
						// data when appending.
						out.flush();

					} catch (IOException e) {
						errString = "451 File IO problem. Device might be full.\r\n";
						Log.e(TAG, "Exception while storing: " + e);
						Log.e(TAG, "Message: " + e.getMessage());
						Log.e(TAG, "Stack trace: ");
						StackTraceElement[] traceElems = e.getStackTrace();
						for (StackTraceElement elem : traceElems) {
							Log.e(TAG, elem.toString());
						}
						break storing;
					}
					break;
				}
			}
		}
		// // Clean up the dedicated writer thread
		// if(dedicatedWriter != null) {
		// dedicatedWriter.exit(); // set its exit flag
		// dedicatedWriter.interrupt(); // make sure it wakes up to process the
		// flag
		// }
		// Thread.currentThread().setPriority(origPriority);
		try {
			// if(dedicatedWriter != null) {
			// dedicatedWriter.exit();
			// }
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
		}

		if (errString != null) {
			Log.e(TAG, "STOR error: " + errString.trim());
			mStatusListener.updateStatus("STOR error: " + errString.trim());
			// This should not be written, as we are a CLIENT
			// sessionThread.writeString(errString);
		} else {
			// This should not be written, as we are a CLIENT
			// sessionThread.writeString("226 Transmission complete\r\n");
			// Notify the music player (and possibly others) that a few file has
			// been uploaded.
			// MediaUpdater.notifyFileCreated(storeFile.getPath());
			mStatusListener.updateStatus(StatusListener.FINISHED_SUCCESSFULLY);
		}
		sessionThread.closeDataSocket();
		Log.d(TAG, "STOR finished");
		mStatusListener.updateStatus("STOR finished");
	}
}
