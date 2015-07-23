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

import android.util.Log;

public class Defaults {
	protected static int inputBufferSize = 256;
	public static int dataChunkSize = 65536;  // do file I/O in 64k chunks
	protected static int sessionMonitorScrollBack = 10;
	protected static int serverLogScrollBack = 10;
	protected static int uiLogLevel = Defaults.release ? Log.INFO : Log.DEBUG;
	protected static int consoleLogLevel = Defaults.release ? Log.INFO : Log.DEBUG;
	protected static String settingsName = "SwiFTP";
	public static final int tcpConnectionBacklog = 5;
	public static final String chrootDir = "/";
	public static final int REMOTE_PROXY_PORT = 2222;
	public static final String STRING_ENCODING = "UTF-8";
	public static final int SO_TIMEOUT_MS = 30000; // socket timeout millis
	// FTP control sessions should start out in ASCII, according to the RFC.
	// However, many clients don't turn on UTF-8 even though they support it,
	// so we just turn it on by default.
	public static final String SESSION_ENCODING = "UTF-8";

	// This is a flag that should be true for public builds and false for dev builds
	public static final boolean release = true;

	// Try to fix the transfer stall bug, reopen the destination file periodically
	//public static final boolean do_reopen_hack = false;
	//public static final int bytes_between_reopen = 4000000;

	// Try to fix the transfer stall bug, flush the file periodically
	//public static final boolean do_flush_hack = false;
	//public static final int bytes_between_flush = 500000;

	public static final boolean do_mediascanner_notify = true;
	
	public static final String VERSION = "2.7";


//	public static int getIpRetrievalAttempts() {
//		return ipRetrievalAttempts;
//	}

//	public static void setIpRetrievalAttempts(int ipRetrievalAttempts) {
//		Defaults.ipRetrievalAttempts = ipRetrievalAttempts;
//	}

	public static String getSettingsName() {
		return settingsName;
	}

	public static void setSettingsName(String settingsName) {
		Defaults.settingsName = settingsName;
	}

	public static int getSettingsMode() {
		return settingsMode;
	}

	public static void setSettingsMode(int settingsMode) {
		Defaults.settingsMode = settingsMode;
	}

	public static void setServerLogScrollBack(int serverLogScrollBack) {
		Defaults.serverLogScrollBack = serverLogScrollBack;
	}

	//This mode is deprecated in API 17. Be warned, this file mode will make it readable to everyone...
	protected static int settingsMode = 2;

	public static int getUiLogLevel() {
		return uiLogLevel;
	}

	public static void setUiLogLevel(int uiLogLevel) {
		Defaults.uiLogLevel = uiLogLevel;
	}

	public static int getInputBufferSize() {
		return inputBufferSize;
	}

	public static void setInputBufferSize(int inputBufferSize) {
		Defaults.inputBufferSize = inputBufferSize;
	}

	public static int getDataChunkSize() {
		return dataChunkSize;
	}

	public static void setDataChunkSize(int dataChunkSize) {
		Defaults.dataChunkSize = dataChunkSize;
	}

	public static int getSessionMonitorScrollBack() {
		return sessionMonitorScrollBack;
	}

	public static void setSessionMonitorScrollBack(
			int sessionMonitorScrollBack)
	{
		Defaults.sessionMonitorScrollBack = sessionMonitorScrollBack;
	}

	public static int getServerLogScrollBack() {
		return serverLogScrollBack;
	}

	public static void setLogScrollBack(int serverLogScrollBack) {
		Defaults.serverLogScrollBack = serverLogScrollBack;
	}

	public static int getConsoleLogLevel() {
		return consoleLogLevel;
	}

	public static void setConsoleLogLevel(int consoleLogLevel) {
		Defaults.consoleLogLevel = consoleLogLevel;
	}
	
	public static String getVersion() {
		return Defaults.VERSION;
	}


}
