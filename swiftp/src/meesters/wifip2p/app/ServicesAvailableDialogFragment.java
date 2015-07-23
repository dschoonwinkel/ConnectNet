package meesters.wifip2p.app;

import meesters.wifip2p.deps.P2PServiceDescriptor;
import meesters.wifip2p.deps.Router;
import meesters.wifip2p.deps.Router.P2P_EXTRA_KEYS;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import be.ppareit.swiftp.R;

public class ServicesAvailableDialogFragment extends DialogFragment {

	private P2PServiceDescriptor[] mServices;
	private ArrayAdapter<String> mAdapter = null;
	private ListView mServicesList = null;

	private static final String TAG = ServicesAvailableDialogFragment.class
			.getName();

	public static ServicesAvailableDialogFragment newInstance(
			P2PServiceDescriptor[] services) {

		ServicesAvailableDialogFragment fragment = new ServicesAvailableDialogFragment();
		fragment.mServices = services;

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater
				.inflate(R.layout.services_available, container, false);

		mServicesList = (ListView) v.findViewById(R.id.services_list);
		return v;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

	@Override
	public void onResume() {
		super.onResume();

		mServicesList.setAdapter(mAdapter);

		mAdapter.clear();

		if (mServices == null) {
			Log.e(TAG, "P2PServices not ready yet");
		} else {
			for (P2PServiceDescriptor service : mServices) {
				String serviceString = "";
				serviceString += service.fullDomainName
						+ " on port: "
						+ service.extras.get(Router.P2P_EXTRA_KEYS.PORTNUM_KEY)
						+ " at "
						+ service.extras
								.get(Router.P2P_EXTRA_KEYS.INETADDR_KEY);
				mAdapter.add(serviceString);
			}
		}

		mAdapter.notifyDataSetChanged();

		Button mOpenConnectDialogButton = (Button) getView().findViewById(
				R.id.open_connect_dialog);
		mOpenConnectDialogButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				((Pong) getActivity()).onOpenConnectDialog(v);
			}
		});

		Button mDiscover = (Button) getView().findViewById(
				R.id.discover_services);
		mDiscover.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				((WifiP2P_FTP) getActivity()).onDiscoverServices(v);
			}
		});

		Button mRegister = (Button) getView().findViewById(
				R.id.register_service);
		mRegister.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				CheckBox server_box = (CheckBox) getView().findViewById(R.id.server_box);
//				((Pong) getActivity()).registerService(server_box.isChecked());
			}
		});

		ListView mServicesList = (ListView) getView().findViewById(
				R.id.services_list);
		mServicesList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.v(TAG, "Clicked: " + position);
				
				
//				((WifiP2P_FTP)getActivity()).onFTPServiceSelected(position);
				ServicesAvailableDialogFragment.this.dismiss();
			}
		});
	}

	public void clearServicesView() {
		Log.v(TAG, "onClearServicesView");
		mAdapter.clear();
		mAdapter.notifyDataSetChanged();
	}

	public void updateServicesView(String text) {
		Log.v(TAG, "updateServicesView");

		int index = text.indexOf("onDnsSdTxtRecordAvailable");
		if (index != -1) {
			text = text.substring(index);
		}
		mAdapter.add(text);
		mAdapter.notifyDataSetChanged();
	}

	public void updateServicesView(P2PServiceDescriptor descriptor) {
		Log.v(TAG, "updateServicesView_withDescriptor");

		String serviceString = descriptor.fullDomainName + " on "
				+ descriptor.srcDevice.deviceName + " with"
				+ descriptor.extras.toString();
		mAdapter.add(serviceString);
		mAdapter.notifyDataSetChanged();
	}
}
