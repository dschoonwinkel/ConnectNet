package meesters.wifip2p.router;

import meesters.wifip2p.deps.AbstractP2PFrontendActivity;
import android.net.wifi.WifiManager;
import android.util.Log;

public class P2PWatchDogThread extends Thread implements IControlChannel {

	private P2PConnectorService mService = null;
	private IControlChannel mControlChannel = null;
	private final static int SLEEP_TIME = 30000;
	private final static String TAG = "P2PScannerThread";

	private P2PScannerThread mScanner = null;
	private P2PRestarterThread mRestarter = null;
	private int mDeviceRole = AbstractP2PFrontendActivity.PROVIDER;

	public P2PWatchDogThread(P2PConnectorService service,
			IControlChannel control) {
		mService = service;
		mControlChannel = control;
	}

	private class P2PScannerThread extends Thread {
		@Override
		public void run() {
			while (true && !isInterrupted()) {
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
				Log.v(TAG, "Starting scan");
				if (mService != null) {
					mService.discoverServices();
				}
			}
		}
	}

	private class P2PRestarterThread extends Thread {
		@Override
		public void run() {
			Log.e(TAG, "Starting restart of service");
			mService.killService();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			mService.startServiceAsRole(mDeviceRole);
		}
	}

	public void startScannerThread() {
		if (mScanner != null && mScanner.isAlive() && !mScanner.isInterrupted()) {
			Log.e(TAG, "The Scanner is already busy!!");
			return;
		}

		mScanner = new P2PScannerThread();
		mScanner.start();
	}

	public void startRestartThread(int deviceRole) {
		if (mRestarter != null && mRestarter.isAlive()) {
			Log.e(TAG, "The Restarter is still busy!!");
			return;
		}

		// Toggle wifi to reset the stack
		mService.toggleWifi(false);
		mService.toggleWifi(true);

		// Save the device role, and restart the P2PConnectorService
		mDeviceRole = deviceRole;
		mRestarter = new P2PRestarterThread();
		mRestarter.start();
	}

	@Override
	public void interrupt() {
		Log.v(TAG, "interrupt");
		if (mScanner != null) {
			Log.v(TAG, "Interrupting the scanner thread");
			mScanner.interrupt();
			mScanner = null;
		}

		if (mRestarter != null) {
			mRestarter.interrupt();
			mRestarter = null;
		}

		super.interrupt();
	}

	@Override
	public void status(String message, int state) {

	}

	@Override
	public int getStatus() {
		return 0;
	}

}
