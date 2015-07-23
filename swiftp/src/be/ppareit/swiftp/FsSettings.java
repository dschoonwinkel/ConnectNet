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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * @author David Revell, Pieter Pareit
 * 
 * Shared prefences accessed from across the app. The setter functions are mostly called by the 
 * {@link be.ppareit.swiftp.gui.FsPreferenceActivity}. This class provides settings for the Username, 
 * Password, local FTP root directory, and FTP control socket port number.
 * 
 *
 */
public class FsSettings {

    private final static String TAG = FsSettings.class.getSimpleName();

    public static String getUserName() {
        final SharedPreferences sp = getSharedPreferences();
        return sp.getString("username", "ftp");
    }

    public static String getPassWord() {
        final SharedPreferences sp = getSharedPreferences();
        return sp.getString("password", "ftp");
    }

    public static File getChrootDir() {
    	
    	//Previous implementation, changing it to a constant directory shared
//        final SharedPreferences sp = getSharedPreferences();
//        String dirName = sp.getString("chrootDir", "");
//        File chrootDir = new File(dirName);
//        if (dirName.equals("")) {
//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                chrootDir = Environment.getExternalStorageDirectory();
//            } else {
//                chrootDir = new File("/");
//            }
//        }
//        if (!chrootDir.isDirectory()) {
//            Log.e(TAG, "getChrootDir: not a directory");
//            return null;
//        }
    	
    	File chrootDir = null;
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
        
        return null;
    }

    public static int getPortNumber() {
        final SharedPreferences sp = getSharedPreferences();
		// TODO: port is always an number, so store this accordenly
		String portString = sp.getString("portNum", "2121");
        int port = Integer.valueOf(portString);
        Log.v(TAG, "Using port: " + port);
        return port;
    }

//    public static boolean shouldTakeFullWakeLock() {
//        final SharedPreferences sp = getSharedPreferences();
//        return sp.getBoolean("stayAwake", false);
//    }

    /**
     * @return the SharedPreferences for this application
     */
    private static SharedPreferences getSharedPreferences() {
        final Context context = FsApp.getAppContext();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    
    
    //This is a duplicate of what happens in Defaults. Suspect multiple authors not checking each other's code
    
    // cleaning up after his
//    protected static int inputBufferSize = 256;
//    protected static boolean allowOverwrite = false;
//    protected static int dataChunkSize = 8192; // do file I/O in 8k chunks
//    protected static int sessionMonitorScrollBack = 10;
//    protected static int serverLogScrollBack = 10;

//    public static int getInputBufferSize() {
//        return inputBufferSize;
//    }
//
//    public static void setInputBufferSize(int inputBufferSize) {
//        FsSettings.inputBufferSize = inputBufferSize;
//    }

//    public static boolean isAllowOverwrite() {
//        return allowOverwrite;
//    }
//
//    public static void setAllowOverwrite(boolean allowOverwrite) {
//        FsSettings.allowOverwrite = allowOverwrite;
//    }

//    public static int getDataChunkSize() {
//        return dataChunkSize;
//    }
//
//    public static void setDataChunkSize(int dataChunkSize) {
//        FsSettings.dataChunkSize = dataChunkSize;
//    }

//    public static int getSessionMonitorScrollBack() {
//        return sessionMonitorScrollBack;
//    }

//    public static void setSessionMonitorScrollBack(int sessionMonitorScrollBack) {
//        FsSettings.sessionMonitorScrollBack = sessionMonitorScrollBack;
//    }

//    public static int getServerLogScrollBack() {
//        return serverLogScrollBack;
//    }
//
//    public static void setLogScrollBack(int serverLogScrollBack) {
//        FsSettings.serverLogScrollBack = serverLogScrollBack;
//    }

}
