package meesters.wifip2p.ftp;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;
import be.ppareit.swiftp.server.StatusListener;

public class FTPClientThread extends Thread implements StatusListener {

	public interface readyForLoginListener {
		public void readyLogIn();
	}

	public interface statusUpdateListener {
		public void doUpdateStatus(String status);
	}

	public interface fileListListener {
		public void doUpdateListView(String[] items);
	}

	private FTPClient mClient = null;
	private String mIpAddress = "";
	private String mPortnum = "";
	private int mPort = -1;
	private static final String TAG = "FTPClientThread";
	private readyForLoginListener mListener = null;
	private statusUpdateListener mStatusListener = null;
	private fileListListener mFileListener = null;
	private String[] mItems = null;

	public FTPClientThread(String ipaddress, String portnum,
			readyForLoginListener listener,
			statusUpdateListener status_listener, fileListListener file_listener) {
		mIpAddress = ipaddress;
		mPortnum = portnum;
		mListener = listener;
		mStatusListener = status_listener;
		mFileListener = file_listener;

		mIpAddress = mIpAddress.trim();

		if (mIpAddress == "") {
			Log.e(TAG, "Invalid ipAddress, using localhost");
			mIpAddress = "localhost";
		}
		try {
			mPort = Integer.parseInt(mPortnum);
		} catch (NumberFormatException e) {
			Log.e(TAG, "Invalid number format, using default 2121");
			mPort = 2121;
		}
	}

	@Override
	public void run() {
		try {
			mClient = new FTPClient(InetAddress.getByName(mIpAddress), mPort,
					this);
			mListener.readyLogIn();
		} catch (UnknownHostException e) {
			Log.e(TAG, "UnknownHostException");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "IOException");
			if (e instanceof ConnectException) {
				mStatusListener.doUpdateStatus(e.getMessage());
			}

			e.printStackTrace();
		}
	}

	public String logIn(String username, String password) {
		if (mClient != null) {
			mStatusListener.doUpdateStatus(mClient.logIn(username, password));
			getFileList();
			return "Grate Success";
		} else {
			Log.e(TAG, "FTP client null");
			return "FTP client null";
		}
	}

	public void getFileList() {
		String list = mClient.getNLST("");
		
		if (list == null) {
			Log.e(TAG, "Got a null response from FTP server, bailing out");
			return;
		}
		mItems = list.split("\n");
		for (int i = 0; i < mItems.length; i++) {
			Log.d(TAG, Integer.toString(i) + ": " + mItems[i]);
		}
		mFileListener.doUpdateListView(mItems);
	}

	public void doRefreshList() {
		if (mClient != null) {
			new Thread() {
				@Override
				public void run() {
					getFileList();
				}
			}.start();
		}
	}

	public void getItem(final int position) {
		Log.v(TAG, "getItem");

		mStatusListener.doUpdateStatus("Starting download");

		Log.d(TAG, "starting new thread");
		new Thread() {
			@Override
			public void run() {
				if (mClient != null && mItems.length > position) {
					mClient.getFileBinary(mItems[position]);
				} else if (mClient == null){
					Log.e(TAG, "mClient is null, i.e. unconnected");
				} else {
					Log.e(TAG, "mItems is too small, i.e. position is out of bounds");
				}
			}
		}.start();
	}

	public void endSession() {
		Log.v(TAG, "endSession");

		new Thread() {
			public void run() {
				if (mClient != null) {
					mClient.endSession();
				} else {
					Log.e(TAG, "mClient is null, i.e. unconnected");
				}
				mClient = null;
				mStatusListener.doUpdateStatus("End session success");
			}
		}.start();
	}

	@Override
	public void updateStatus(String status) {
		// React to status update
		switch (status) {
		case StatusListener.FINISHED_SUCCESSFULLY:
			mStatusListener.doUpdateStatus(StatusListener.FINISHED_SUCCESSFULLY);
			break;
		case StatusListener.STARTED_SUCCESSFULLY:
			mStatusListener.doUpdateStatus(StatusListener.STARTED_SUCCESSFULLY);
			break;
		}
		if (status.contains(StatusListener.BUSY_WORKING)) {
			mStatusListener.doUpdateStatus(status);
		}
	}

	@Override
	public String getStatus(String status) {
		return "All is well...";

	}

}
