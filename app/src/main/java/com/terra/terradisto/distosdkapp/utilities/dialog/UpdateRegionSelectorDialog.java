package com.terra.terradisto.distosdkapp.utilities.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.activities.YetiInformationActivity;
import com.terra.terradisto.distosdkapp.update.UpdateController;


public class UpdateRegionSelectorDialog
		extends Dialog implements
		android.view.View.OnClickListener {

	public interface IUpdateRegion {
		void onSelectUpdateTarget(UpdateController.UpdateRegion updateRegion);
        void onCancel();
    }

	private IUpdateRegion updateTargetListener;

	public Button updateDevice, updateComponent, updateBoth, cancel;

	private TextView messageTxtView;
	private String messageStr;

	private TextView txt_updateView;
	private String txt_updateStr;




	public UpdateRegionSelectorDialog(YetiInformationActivity activity, IUpdateRegion updateTargetListener) {
		super(activity);

		this.updateTargetListener = updateTargetListener;
		this.setCancelable(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.update_region_selector_dialog);
		updateDevice = (Button) findViewById(R.id.updateDevice_btn);
		updateComponent = (Button) findViewById(R.id.updateComponent_btn);
		updateBoth = (Button) findViewById(R.id.updateBoth_btn);
		cancel = (Button) findViewById(R.id.cancel_btn);
		messageTxtView = (TextView) findViewById(R.id.info_txt);
		messageTxtView.setText(messageStr);
		messageTxtView.setMovementMethod(new ScrollingMovementMethod());

		txt_updateView = (TextView) findViewById(R.id.updateMessage_txt);//TODO: check for possible exception related to this field
		txt_updateView.setText(txt_updateStr);

		updateDevice.setOnClickListener(this);
		updateComponent.setOnClickListener(this);
		updateBoth.setOnClickListener(this);
		cancel.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {


		switch (v.getId()) {
			case R.id.updateDevice_btn:
				this.updateTargetListener.onSelectUpdateTarget(UpdateController.UpdateRegion.device);

				break;
			case R.id.updateComponent_btn:
				this.updateTargetListener.onSelectUpdateTarget(UpdateController.UpdateRegion.components);
				break;
			case R.id.updateBoth_btn:
				this.updateTargetListener.onSelectUpdateTarget(UpdateController.UpdateRegion.both);
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

	public void setTxt_updateStr(String txt_updateStr) {
		this.txt_updateStr = txt_updateStr;
	}

}