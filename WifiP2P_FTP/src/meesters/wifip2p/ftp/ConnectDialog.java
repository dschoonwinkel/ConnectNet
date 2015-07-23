package meesters.wifip2p.ftp;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ConnectDialog extends Dialog implements
		android.view.View.OnClickListener {

	public interface ConnectDialogListener {
		public void onConnectDialog(String ipaddress, String port);
	}
	
	private ConnectDialogListener mListener = null;

	public ConnectDialog(Activity activity, ConnectDialogListener listener) {
		super(activity);
		mListener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.log_in_dialog);
		Button cancel = (Button) findViewById(R.id.dialog_cancel_button);
		Button signin = (Button) findViewById(R.id.dialog_sign_in);
		cancel.setOnClickListener(this);
		signin.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.dialog_cancel_button:
			// Do nothing
			this.dismiss();
			break;
		case R.id.dialog_sign_in:
			TextView username = (TextView) this.findViewById(R.id.username);
			TextView password = (TextView) this.findViewById(R.id.password);
			mListener.onConnectDialog(username.getText().toString(), password
					.getText().toString());
			this.dismiss();
		}

	}

}
