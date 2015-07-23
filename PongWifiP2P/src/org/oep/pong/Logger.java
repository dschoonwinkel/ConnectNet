package org.oep.pong;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Time;

import android.os.Environment;
import android.util.Log;

public class Logger {

	private static PrintStream out;
	private static Logger instance = null;

	public static void main(String[] args) {
		Logger.logMessage("MAIN", "Test string");
		out.close();

	}

	protected Logger() {
		try {
			File chrootDir = Environment.getExternalStorageDirectory();
			
			Log.v("Logger", "chrootDir: " + chrootDir.getAbsolutePath() + "/Pong/pong_log.txt");
			File logdir = new File(chrootDir.getAbsolutePath() + "/Pong");
			logdir.mkdirs();
			File logfile = new File(logdir + "/pong_log.txt");
			logfile.delete();
			
			out = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(logfile, true)));
			System.out.println("Created log.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("Unable to create pong_log.txt");
		}
	}
	
	public static void logMessage(String TAG, String message) {
		Logger.getInstance().log(TAG, message);
	}
	
	private void log(String TAG, String message) {
		Time time = new Time(System.currentTimeMillis());
			out.println(time.toString() + " " + TAG + " : " + message);
//			System.out.println("Printed message");
	}
	
	public void close() {
		out.close();
	}
	
	public static Logger getInstance() {
		if (instance == null) {
			instance = new Logger();
			System.out.println("Created a Logger");
		}
		return instance;
	}

}
