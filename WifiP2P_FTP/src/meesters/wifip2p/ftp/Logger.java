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

package meesters.wifip2p.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Time;
import java.util.GregorianCalendar;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

public class Logger {

	private static PrintStream out;
	private static Logger instance = null;
	private static final String TAG = "Logger";

	public static void main(String[] args) {
		Logger.logMessage("MAIN", "Test string");
		out.close();

	}

	protected Logger() {
		try {
			File chrootDir = Environment.getExternalStorageDirectory();

//			Log.v("Logger", "chrootDir: " + chrootDir.getAbsolutePath()
//					+ "/FTPClientLog/ftp_log.txt");
//			File logdir = new File(chrootDir.getAbsolutePath()
//					+ "/FTPClientLog");
//			logdir.mkdirs();
//			File logfile = new File(logdir + "/ftp_log.txt");
//			logfile.delete();
			
			File logdir = new File(chrootDir.getAbsolutePath() + "/FTPClientLog");
			logdir.mkdirs();
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis(System.currentTimeMillis());
			File logfile = new File(logdir + "/ftp_log" + DateFormat.format("MM-dd-HH.mm.ss", cal) + ".txt");
			Log.v(TAG, logdir + "/ftp_log" + DateFormat.format("MM-dd_HH.mm.ss", cal) + ".txt");

			out = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(logfile, true)));
			System.out.println("Created log.txt");
			out.println("Starting logger");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Unable to create ftp_log.txt");
		}
	}

	public static void logMessage(String TAG, String message) {
		Logger.getInstance().log(TAG, message);
	}

	private void log(String TAG, String message) {
		Time time = new Time(System.currentTimeMillis());
		out.println(time.toString() + " " + TAG + " : " + message + " millis: "
				+ ((double) System.nanoTime()) / 1e6);
		// System.out.println("Printed message");
	}

	public static void close() {
		if (instance != null) {
			out.close();
		}
	}

	public static Logger getInstance() {
		if (instance == null) {
			instance = new Logger();
			System.out.println("Created a Logger");
		}
		return instance;
	}

}
