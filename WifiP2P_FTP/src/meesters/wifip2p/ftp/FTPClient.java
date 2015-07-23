/*

Author: Daniel Schoonwinkel
This file uses code written by Pieter Pareit, see below.

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

package meesters.wifip2p.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;
import be.ppareit.swiftp.FsSettings;
import be.ppareit.swiftp.server.CmdCDUP;
import be.ppareit.swiftp.server.CmdCWD;
import be.ppareit.swiftp.server.CmdLIST;
import be.ppareit.swiftp.server.CmdNLST;
import be.ppareit.swiftp.server.CmdPASS;
import be.ppareit.swiftp.server.CmdPASV;
import be.ppareit.swiftp.server.CmdPORT;
import be.ppareit.swiftp.server.CmdPWD;
import be.ppareit.swiftp.server.CmdQUIT;
import be.ppareit.swiftp.server.CmdRETR;
import be.ppareit.swiftp.server.CmdSTOR;
import be.ppareit.swiftp.server.CmdTYPE;
import be.ppareit.swiftp.server.CmdUSER;
import be.ppareit.swiftp.server.NormalDataSocketFactory;
import be.ppareit.swiftp.server.SessionThread;
import be.ppareit.swiftp.server.SessionThread.Source;
import be.ppareit.swiftp.server.StatusListener;

public class FTPClient implements StatusListener {

	private static final String TAG = "FTPClient";
	private SessionThread mSession = null;
	private Socket mCmdSocket = null;
	private BufferedReader mInputReader = null;
	@SuppressWarnings("unused")
	private InetAddress mAddress = null;
	private File ftpDir = null;
	private StatusListener mStatusListener = null;
	private String mStatus = "";

	// Empty constructor, call connect after this call
	public FTPClient() {

	}

	// Constructor and connect call combined
	public FTPClient(InetAddress address, int port,
			StatusListener status_listener) throws IOException {
		connect(address, port, status_listener);
	}

	public void connect(InetAddress address, int port,
			StatusListener status_listener) throws IOException {
		// Completely invalid address, bail out
		if (address == null) {
			Log.e(TAG, "Address was null");
			SocketException e = new SocketException(
					"Ftp address to connect to was null!");
			throw e;
		}

		ftpDir = FsSettings.getChrootDir();
		mStatusListener = status_listener;

		writeReadmeToFTPDir();

		mCmdSocket = new Socket(address, port);
		mCmdSocket.setReuseAddress(true);
		mAddress = address;

		Log.d(TAG, "Receive buffer size: " + mCmdSocket.getReceiveBufferSize());
		mInputReader = new BufferedReader(new InputStreamReader(
				mCmdSocket.getInputStream()));

		NormalDataSocketFactory socketFactory = new NormalDataSocketFactory();
		mSession = new SessionThread(mCmdSocket, socketFactory, Source.LOCAL);

		mSession.setWorkingDir(ftpDir);

		// Read welcoming banner
		readResponse();
	}

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		InetAddress address = InetAddress.getByName("10.10.11.100");
		// InetAddress mAddress = InetAddress.getLocalHost();
		int port = 2121;
		// String username = "admin";
		// String password = "admin";
		String username = "ftp";
		String password = "ftp";

		// Connects and starts the FTP Client
		if (address == null) {
			throw new RuntimeException("Address was null, probably not found");
		}
		FTPClient client = new FTPClient(address, port, null);

		client.logIn(username, password);
		client.getLIST("");
		client.getNLST("");

		client.getFile("user_info.txt");
		// This should give an error
		// client.getFileBinary("Ringtones");

		client.changeDir("Ringtones");
		client.printWorkingDir();
		client.changeDirUp();
		client.endSession();
	}

	public String logIn(String username, String password) {
		mSession.writeString(CmdUSER.buildCommand(username));
		String userResponse = readResponse();
		Log.d(TAG, userResponse);

		mSession.writeString(CmdPASS.buildCommand(password));
		String passResponse = readResponse();
		Log.d(TAG, passResponse);

		String responseCode = getResponseCode(passResponse);
		// Log.d(TAG, responseCode);

		if (responseCode.equals("230"))
			return "User logged in successfully";
		else {
			return responseCode;
		}
	}

	public String getLIST(String dirname) {

		String returnString = "";
		// Start LIST
		mSession.writeString(CmdPASV.buildCommand(""));
		String pasvResponse = readResponse();
		Log.d(TAG, pasvResponse);

		InetAddress pasvAddress = CmdPASV.parseInetAddress(pasvResponse);
		int pasvPort = CmdPASV.parsePortNumber(pasvResponse);

		// Log.d(TAG, "Address:" + pasvAddress.toString());
		// Log.d(TAG, "portnum:" + pasvPort);

		mSession.onPort(pasvAddress, pasvPort);
		if (!mSession.startUsingDataSocket()) {
			throw new RuntimeException("Could not open data socket");
		}

		mSession.writeString(CmdLIST.buildCommand(""));
		readResponse();

		byte[] receivedBytes = new byte[8192];
		int receiveCount = 0;
		while ((receiveCount = mSession.receiveFromDataSocket(receivedBytes)) > 0) {
			String receivedString = new String(receivedBytes).substring(0,
					receiveCount);
			Log.d(TAG, "received Bytes\n" + receivedString);
			returnString += receivedString;
		}

		readResponse();

		return returnString;
	}

	public String getNLST(String dirname) {

		String returnString = "";

		// Start NLST
		mSession.writeString(CmdPASV.buildCommand(""));

		String pasvResponse = readResponse();

		InetAddress pasvAddress = CmdPASV.parseInetAddress(pasvResponse);
		int pasvPort = CmdPASV.parsePortNumber(pasvResponse);

		// Log.d(TAG, "Address:" + pasvAddress.toString());
		// Log.d(TAG, "portnum:" + pasvPort);

		mSession.onPort(pasvAddress, pasvPort);
		if (!mSession.startUsingDataSocket()) {
			throw new RuntimeException("Could not open data socket");
		}

		Log.d(TAG, CmdNLST.buildCommand(""));
		mSession.writeString(CmdNLST.buildCommand(""));
		String nlstResponse = readResponse();
		Log.d(TAG, nlstResponse);

		byte[] receivedBytes = new byte[8192];
		int receiveCount = 0;
		while ((receiveCount = mSession.receiveFromDataSocket(receivedBytes)) > 0) {
			String receivedString = new String(receivedBytes).substring(0,
					receiveCount);
			Log.d(TAG, "received Bytes\n" + receivedString);
			returnString += receivedString;
		}
		if (receiveCount == -2) {
			Log.d(TAG, "A socket error occurred");
		}

		nlstResponse = readResponse();
		Log.d(TAG, nlstResponse);
		return returnString;
	}

	public void getFile(String param) {
		Log.i(TAG, "getFile: " + param);
		
		// Set this side to PASV, so that the RETR side will connect to us.
		int dataport = mSession.onPasv();
		String portCmd = CmdPORT.buildCommand(mSession.getDataSocketPasvIp(),
				dataport);
		Log.d(TAG, "PORT Cmd: " + portCmd);
		mSession.writeString(portCmd);
		readResponse();

		// Start the data transmission
		CmdSTOR storCmd = new CmdSTOR(mSession, "STOR " + param, this);
		mSession.writeString(CmdRETR.buildCommand(param));
		String retrResponse = readResponse();

		if (getResponseCode(retrResponse).equals("550")) {
			throw new RuntimeException("File error");
		}

		readResponse();
		new Thread(storCmd).start();

	}

	public void getFileBinary(String param) {
		Log.i(TAG, "getFileBinary: " + param);
		// Set the TYPE to Binary
		mSession.writeString(CmdTYPE.buildCommand("I"));
		String typeResponse = readResponse();

		if (getResponseCode(typeResponse).equals("503")) {
			throw new RuntimeException("Malformed TYPE command");
		}

		mSession.setBinaryMode(true);

		// Set this side to PASV, so that the RETR side will connect to us.
		int dataport = mSession.onPasv();
		String portCmd = CmdPORT.buildCommand(mSession.getDataSocketPasvIp(),
				dataport);
		Log.d(TAG, "PORT Cmd: " + portCmd);
		mSession.writeString(portCmd);
		// readResponse();

		// Start the data transmission
		CmdSTOR storCmd = new CmdSTOR(mSession, "STOR " + param, this);
		mSession.writeString(CmdRETR.buildCommand(param));
		String retrResponse = readResponse();

		if (getResponseCode(retrResponse).equals("550")) {
			throw new RuntimeException("File error");
		}

		readResponse();
		new Thread(storCmd).start();

	}

	public void changeDir(String dirname) {
		mSession.writeString(CmdCWD.buildCommand(dirname));
		readResponse();
	}

	public void changeDirUp() {
		mSession.writeString(CmdCDUP.buildCommand(""));
		readResponse();
	}

	public void printWorkingDir() {
		mSession.writeString(CmdPWD.buildCommand(""));
		readResponse();
	}

	public void endSession() {
		mSession.writeString(CmdQUIT.buildCommand(""));
		readResponse();
		mSession.quit();
	}

	public static String getResponseCode(String message) {
		String[] words = message.split(" ");

		return words[0];
	}

	public String readResponse() {
		String response = "";
		try {
			response = mInputReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Log.d(TAG, response);
		return response;
	}

	public void writeReadmeToFTPDir() {
		if (ftpDir == null) {
			Log.e(TAG, "Could not open Ftp Dir for writing");
			return;
		}
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new File(ftpDir, "README.txt"));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File could not be found or created");
			e.printStackTrace();
			return;
		}
		writer.println("Welcome to the FTPClient Readme!");
		writer.println("This directory will be used for storing all of the FTP received files\n");
		writer.println(LICENSE);

		writer.close();
	}

	private static final String LICENSE = "Author: Daniel Schoonwinkel\nThis file uses code written by Pieter Pareit, David Revell, see below.\nCopyright 2011-2013 Pieter Pareit\nCopyright 2009 David Revell\nThis file is part of SwiFTP.\nSwiFTP is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\nSwiFTP is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\nYou should have received a copy of the GNU General Public License\nalong with SwiFTP.  If not, see <http://www.gnu.org/licenses/>";

	@Override
	public void updateStatus(String status) {
		// Log.d(TAG, "updateStatus: " + status);
		mStatus = status;
		mStatusListener.updateStatus(status);
	}

	@Override
	public String getStatus(String status) {
		return mStatus;

	}
}
