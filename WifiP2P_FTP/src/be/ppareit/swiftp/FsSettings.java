/*
Copyright 2011-2013 Pieter Pareit
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

package be.ppareit.swiftp;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class FsSettings {

	private final static String TAG = FsSettings.class.getSimpleName();
	private static String mUsername = "ftp";
	private static String mPassword = "ftp";
	private static int mPortNumber = 2121;

	public static String getUserName() {
		return mUsername;
	}

	public static String getPassWord() {
		return mPassword;
	}

	public static File getChrootDir() {

		// final SharedPreferences sp = getSharedPreferences();
		// String dirName = sp.getString("chrootDir", "");
		// File chrootDir = new File(dirName);
		File chrootDir = null;
		// if (dirName.equals("")) {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			chrootDir = Environment.getExternalStorageDirectory();
			File ftpFolder = new File(chrootDir, "FTPClient");
			if (!(ftpFolder.mkdir() || ftpFolder.isDirectory())) {
				Log.e(TAG, "Error creating dir");
				return null;
			}
			return ftpFolder;
		}
		// } else {

		//JAVA implementation, not valid for android
//		String startingDir = System.getProperty("user.dir");
		// System.out.println("Starting directory: " + startingDir);
//		chrootDir = new File(startingDir);
		// }
		// }

		return null;
	}

	public static int getPortNumber() {
		// final SharedPreferences sp = getSharedPreferences();
		// // TODO: port is always an number, so store this accordenly
		// String portString = sp.getString("portNum", "2121");
		// int port = Integer.valueOf(portString);
		// Log.v(TAG, "Using port: " + port);
		return mPortNumber;
	}

	// cleaning up after his
	protected static int inputBufferSize = 256;
	protected static boolean allowOverwrite = false;
	protected static int dataChunkSize = 8192; // do file I/O in 8k chunks
	protected static int sessionMonitorScrollBack = 10;
	protected static int serverLogScrollBack = 10;

	public static int getInputBufferSize() {
		return inputBufferSize;
	}

	public static void setInputBufferSize(int inputBufferSize) {
		FsSettings.inputBufferSize = inputBufferSize;
	}

	public static boolean isAllowOverwrite() {
		return allowOverwrite;
	}

	public static void setAllowOverwrite(boolean allowOverwrite) {
		FsSettings.allowOverwrite = allowOverwrite;
	}

	public static int getDataChunkSize() {
		return dataChunkSize;
	}

	public static void setDataChunkSize(int dataChunkSize) {
		FsSettings.dataChunkSize = dataChunkSize;
	}

	public static int getSessionMonitorScrollBack() {
		return sessionMonitorScrollBack;
	}

	public static void setSessionMonitorScrollBack(int sessionMonitorScrollBack) {
		FsSettings.sessionMonitorScrollBack = sessionMonitorScrollBack;
	}

	public static int getServerLogScrollBack() {
		return serverLogScrollBack;
	}

	public static void setLogScrollBack(int serverLogScrollBack) {
		FsSettings.serverLogScrollBack = serverLogScrollBack;
	}

}
