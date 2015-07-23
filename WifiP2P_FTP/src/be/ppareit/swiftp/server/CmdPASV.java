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

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * PASV - Requests the server DTP to listen on a data port and wait for
 * connection. Response include address and port : <h1,h2,h3,h4,p1,p2>
 * 
 * @author David Revell, Pieter Pareit, Daniel Schoonwinkel
 */

public class CmdPASV extends FtpCmd implements Runnable {
	private static final String TAG = CmdPASV.class.getSimpleName();

	public CmdPASV(SessionThread sessionThread, String input) {
		super(sessionThread);
	}

	@Override
	public void run() {
		String cantOpen = "502 Couldn't open a port\r\n";
		Log.d(TAG, "PASV running");
		int port;
		if ((port = sessionThread.onPasv()) == 0) {
			// There was a problem opening a port
			Log.e(TAG, "Couldn't open a port for PASV");
			sessionThread.writeString(cantOpen);
			return;
		}
		InetAddress addr = sessionThread.getDataSocketPasvIp();
		if (addr == null) {
			Log.e(TAG, "PASV IP string invalid");
			sessionThread.writeString(cantOpen);
			return;
		}
		Log.d(TAG, "PASV sending IP: " + addr.getHostAddress());
		if (port < 1) {
			Log.e(TAG, "PASV port number invalid");
			sessionThread.writeString(cantOpen);
			return;
		}
		StringBuilder response = new StringBuilder(
				"227 Entering Passive Mode (");
		// Output our IP address in the format xxx,xxx,xxx,xxx
		response.append(addr.getHostAddress().replace('.', ','));
		response.append(",");
		// Output our port in the format p1,p2 where port=p1*256+p2
		response.append(port / 256);
		response.append(",");
		response.append(port % 256);
		response.append(").\r\n");
		String responseString = response.toString();
		sessionThread.writeString(responseString);
		Log.d(TAG, "PASV completed, sent: " + responseString);
	}

	public static String buildCommand(String params) {
		String cmd = "PASV " + params + FtpCmd.CRLF;
		return cmd;

	}

	public static InetAddress parseInetAddress(String input) {

		Log.v(TAG, "parseInetAddress: " + input);

		String param = null;

		try {
			param = input.substring(input.indexOf("(") + 1);
			param = param.substring(0, param.indexOf(")"));
		} catch (IndexOutOfBoundsException e) {
			Log.e(TAG, "input was incorrect format: " + input);
			return null;
		}

		// Log.d(TAG, param);
		String[] substrs = param.split(",");

		byte[] ipBytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			try {
				// We have to manually convert unsigned to signed
				// byte representation.
				int ipByteAsInt = Integer.parseInt(substrs[i]);
				if (ipByteAsInt >= 128) {
					ipByteAsInt -= 256;
				}
				ipBytes[i] = (byte) ipByteAsInt;
			} catch (Exception e) {
				return null;
			}
		}
		InetAddress inetAddr = null;
		try {
			inetAddr = InetAddress.getByAddress(ipBytes);
		} catch (UnknownHostException e) {
			return null;
		}

		return inetAddr;
	}

	public static int parsePortNumber(String input) {

		Log.v(TAG, "parsePortNumber: " + input);

		String param = null;
		try {
		param = input.substring(input.indexOf("(") + 1);
		param = param.substring(0, param.indexOf(")"));
		} catch(IndexOutOfBoundsException e) {
			Log.e(TAG, "input was incorrect format:" + input);
			return -1;
		}
		
		String[] substrs = param.split(",");

		int port = Integer.parseInt(substrs[4]) * 256
				+ Integer.parseInt(substrs[5]);

		return port;
	}
}
