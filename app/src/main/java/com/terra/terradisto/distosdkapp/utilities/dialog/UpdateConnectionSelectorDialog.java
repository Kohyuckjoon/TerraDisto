package com.terra.terradisto.distosdkapp.utilities.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.activities.YetiInformationActivity;
import com.terra.terradisto.distosdkapp.update.UpdateController;



public class UpdateConnectionSelectorDialog
		extends Dialog implements
		View.OnClickListener {

	public interface IUpdateConnection {
		void onSelectUpdateConnection(UpdateController.UpdateConn updateConn);
		void onCancel();
	}

	private UpdateConnectionSelectorDialog.IUpdateConnection updateTargetListener;

	private YetiInformationActivity activity;
	private Dialog dialog;
	private Button updateOnline, updateOffline, cancel;

	private TextView messageTxtView;
	private String messageStr;



	public UpdateConnectionSelectorDialog(YetiInformationActivity activity, IUpdateConnection updateTargetListener) {
		super(activity);

		this.activity = activity;
		this.updateTargetListener = updateTargetListener;
		this.setCancelable(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.update_connection_selector_dialog);

		updateOnline = (Button) findViewById(R.id.updateOnline_btn);
		updateOffline = (Button) findViewById(R.id.updateOffline_btn);
		cancel = (Button) findViewById(R.id.cancel_btn);
		messageTxtView = (TextView) findViewById(R.id.info_txt);

		messageTxtView.setText(messageStr);

		updateOnline.setOnClickListener(this);
		updateOffline.setOnClickListener(this);
		cancel.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.updateOnline_btn:
				this.updateTargetListener.onSelectUpdateConnection(UpdateController.UpdateConn.online);
				break;
			case R.id.updateOffline_btn:
				this.updateTargetListener.onSelectUpdateConnection(UpdateController.UpdateConn.offline);
				break;
			case R.id.cancel_btn:
			    this.updateTargetListener.onCancel();
				dismiss();
				break;
			default:
				break;
		}
		dismiss();
	}


	public void setMessage(String message){
		this.messageStr = message;
	}
}