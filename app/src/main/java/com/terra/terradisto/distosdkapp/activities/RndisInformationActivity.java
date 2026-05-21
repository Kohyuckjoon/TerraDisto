
package com.terra.terradisto.distosdkapp.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.device.WifiDeviceController;
import com.terra.terradisto.distosdkapp.utilities.Logs;
import ch.leica.sdk.Devices.Disto3DDevice;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.Types;
import ch.leica.sdk.Utilities.LiveImagePixelConverter;
import ch.leica.sdk.commands.Image;
import ch.leica.sdk.commands.response.Response;



public class RndisInformationActivity extends WifiInformationActivity{

	/**
	 * ClassName
	 */
	private static final String CLASSTAG = RndisInformationActivity.class.getSimpleName();


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ UI-ELEMENTS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//Textfields present information to user
	/**
	 * Device ID
	 */
	public TextView deviceType_txt;
	public TextView deviceType_label;

	/**
	 * Device operating hours
	 */
	public TextView htime_txt;
	public TextView htime_label;

	/**
	 * Equipment number
	 */
	public TextView equipment_txt;
	public TextView equipment_label;

	/**
	 * Telescope position
	 */
	public TextView face_txt;
	public TextView face_label;


	/**
	 * Motor polling
	 */
	public TextView motwhile_txt;
	public TextView motwhile_label;

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ IMAGE MEMBERS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++
	private SeekBar zoomBar;

	/**
	 * Defines image zoom
	 */
	private TextView zoom_one_view;
	/**
	 * Defines image zoom
	 */
	private TextView zoom_two_view;
	/**
	 * Defines image zoom
	 */
	private TextView zoom_four_view;
	/**
	 * Defines image zoom
	 */
	private TextView zoom_eight_view;


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ BUTTONS implementing functionality of leica device
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * Starts live image transfer
	 */
	private Button startLiveImage_Button;

	/**
	 * Stops live image transfer
	 */
	private Button stopLiveImage_Button;

	/**
	 * Moves the 3D Disto device in the up direction
	 */
	private Button wifiUp_Button;

	/**
	 * Moves the 3D Disto device in the down direction
	 */
	private Button wifiDown_Button;

	/**
	 * Moves the 3D Disto device in the right direction
	 */
	private Button wifiRight_Button;

	/**
	 * Moves the 3D Disto device in the left direction
	 */
	private Button wifiLeft_Button;


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ RELATIVE LAYOUT - IMAGE
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * Moves the 3D Disto device in the up direction
	 */
	private RelativeLayout wifiUpButtonContainer;

	/**
	 * Moves the 3D Disto device in the down direction
	 */
	private RelativeLayout wifiDownButtonContainer;

	/**
	 * Moves the 3D Disto device in the right direction
	 */
	private RelativeLayout wifiRightButtonContainer;

	/**
	 * Moves the 3D Disto device in the left direction
	 */
	private RelativeLayout wifiLeftButtonContainer;


	/////////////////////////////////////////////////////////////////
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	ANDROID LIFECYCLE
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String METHODTAG = ".onCreate";
	}

	@Override
	public void setContentView() {
		setContentView(R.layout.activity_rndis_information);
	}

	@Override
	void initMembers() {
		super.initMembers();

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++ Set UI Listeners
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++

		startLiveImage_Button = (Button) findViewById(R.id.startLiveImage_btn);
		stopLiveImage_Button = (Button) findViewById(R.id.stopLiveImage_btn);

		wifiUp_Button = (Button) findViewById(R.id.up_btn);
		wifiRight_Button = (Button) findViewById(R.id.right_btn);
		wifiDown_Button = (Button) findViewById(R.id.down_btn);
		wifiLeft_Button = (Button) findViewById(R.id.left_btn);

		wifiUpButtonContainer = (RelativeLayout) findViewById(R.id.up_button_container);
		wifiRightButtonContainer = (RelativeLayout) findViewById(R.id.right_button_container);
		wifiDownButtonContainer = (RelativeLayout) findViewById(R.id.down_button_container);
		wifiLeftButtonContainer = (RelativeLayout) findViewById(R.id.left_button_container);

		zoomBar = (SeekBar) findViewById(R.id.zoomBar);
		zoom_one_view = (TextView) findViewById(R.id.zoom_one);
		zoom_two_view = (TextView) findViewById(R.id.zoom_two);
		zoom_four_view = (TextView) findViewById(R.id.zoom_four);
		zoom_eight_view = (TextView) findViewById(R.id.zoom_eight);

		htime_txt = (TextView) findViewById(R.id.htime_txt);
		htime_label = (TextView) findViewById(R.id.htime_lbl);
		deviceType_txt = (TextView) findViewById(R.id.deviceType_txt);
		deviceType_label = (TextView) findViewById(R.id.deviceType_lbl);
		equipment_txt = (TextView) findViewById(R.id.equipment_txt);
		equipment_label = (TextView) findViewById(R.id.equipment_lbl);
		face_txt = (TextView) findViewById(R.id.face_txt);
		face_label = (TextView) findViewById(R.id.face_lbl);
		motwhile_txt = (TextView) findViewById(R.id.motwhile_txt);
		motwhile_label = (TextView) findViewById(R.id.motwhile_lbl);
		hz_temp_txt = (TextView) findViewById(R.id.hz_temp_txt);

		View.OnClickListener buttonListener = new RndisInformationActivity.ButtonListener();
		setOnClickListener(startLiveImage_Button, buttonListener);
		setOnClickListener(stopLiveImage_Button, buttonListener);

		SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBarChangeListener();

		if (zoomBar != null) {
			zoomBar.setOnSeekBarChangeListener(seekBarListener);
		}


		//Init class responsible for motor movement events
		MotorControlsListener motorControlsListener = new MotorControlsListener();

		if (wifiUp_Button != null) {
			wifiUp_Button.setOnTouchListener(motorControlsListener);
		}

		if (wifiRight_Button != null) {
			wifiRight_Button.setOnTouchListener(motorControlsListener);
		}

		if (wifiDown_Button != null) {
			wifiDown_Button.setOnTouchListener(motorControlsListener);
		}

		if (wifiLeft_Button != null) {
			wifiLeft_Button.setOnTouchListener(motorControlsListener);
		}
		if (wifiUpButtonContainer != null) {
			wifiUpButtonContainer.setOnTouchListener(motorControlsListener);
		}

		if (wifiRightButtonContainer != null) {
			wifiRightButtonContainer.setOnTouchListener(motorControlsListener);
		}

		if (wifiDownButtonContainer != null) {
			wifiDownButtonContainer.setOnTouchListener(motorControlsListener);
		}

		if (wifiLeftButtonContainer != null) {
			wifiLeftButtonContainer.setOnTouchListener(motorControlsListener);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		final String METHODTAG = ".onResume";
	}

	@Override
	protected void onStop() {
		super.onStop();
		final String METHODTAG = ".onStop";

		stopLiveImage();
	}

	/**
	 * Remove all the listeners associated with the currentDeviceController
	 * and the reconnection procedure
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}




	@Override
	public void onLowMemory() {
		super.onLowMemory();

		final String METHODTAG = ".onLowMemory";
		Log.v(CLASSTAG, METHODTAG +" called");

		stopLiveImage();
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	DATA EXTRACTION - INTERFACE IMPLEMENTATION - WifiDataListener -
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	///////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void onDeviceType_Received(int deviceType) {
		setTextView(deviceType_txt, String.valueOf(deviceType));
	}

	@Override
	public void onFace_Received(int face) {
		setTextView(face_txt, String.valueOf(face));
	}

	@Override
	public void onImageReceived(Image image) {
		imageController.setImage(image, zoomBar.getProgress());
	}



	///////////////////////////////////////////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	INTERFACE IMPLEMENTATION - CommandListener -
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	///////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void setCommandsUIOff() {

		super.setCommandsUIOff();

		float alpha = .5f;
		boolean clickable = false;

		setUIButtonState(startLiveImage_Button, alpha, clickable);
		setUIButtonState(stopLiveImage_Button, alpha, clickable);

	}

	@Override
	public void setCommandsUIOn() {

		super.setCommandsUIOn();

		float alpha = 1f;
		boolean clickable = true;

		setUIButtonState(startLiveImage_Button, alpha, clickable);
		setUIButtonState(stopLiveImage_Button, alpha, clickable);

	}



	///////////////////////////////////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	LIVEIMAGE
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//////////////////////////////////////////////////////////////////////////////////
	/**
	 * Start live image transfer.
	 */
	private void startLiveImage() {

		final String METHODTAG = ".startLiveImage";
		Log.v(CLASSTAG, String.format("%s Starting live image", METHODTAG));

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// set zoom to specific level
				zoomBar.setProgress(0);

				if(previewImage != null){
					previewImage.setOnTouchListener(new CameraMovementListener());
				}
			}
		});

		new Thread(new Runnable() {
			@Override
			public void run() {

				Response response = currentDeviceController.sendCommand(Types.Commands.ImageZoomWide);
				ErrorObject error = response.getError();
				if(error == null) {
					currentDeviceController.connectLiveChannel(Disto3DDevice.LiveImageSpeed.FAST);
				}else{
					showAlert(formatErrorMessage(error));
				}
			}

		}).start();
	}


	/**
	 * Stop live image transfer.
	 */
	private void stopLiveImage() {
		final String METHODTAG = ".stopLiveImage";
		Log.v(CLASSTAG, METHODTAG +" stopping, live Image");

		currentDeviceController.disconnectLiveChannel();

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(previewImage != null){
					previewImage.setImageDrawable(null);
					previewImage.setOnTouchListener(null);
				}
			}
		});
	}



	//////////////////////////////////////////////////////////////////////////////////
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	UI
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//////////////////////////////////////////////////////////////////////////////////
	@Override
	public void setActivityUI(final String model) {

		final String METHODTAG = ".setActivityUI";
		if (currentDeviceController == null) {
			Logs.logNullValues(CLASSTAG, METHODTAG, "");
			return;
		}
		if (currentDeviceController.getCurrentDevice() == null) {
			return;
		}

		setElementVisibility(wifiUp_Button, true);
		setElementVisibility(wifiUpButtonContainer, true);

		setElementVisibility(wifiRight_Button, true);
		setElementVisibility(wifiRightButtonContainer, true);

		setElementVisibility(wifiDown_Button, true);
		setElementVisibility(wifiDownButtonContainer, true);

		setElementVisibility(wifiLeft_Button, true);
		setElementVisibility(wifiLeftButtonContainer, true);

		setElementVisibility(zoomBar, true);
		setElementVisibility(zoom_one_view, true);
		setElementVisibility(zoom_two_view, true);
		setElementVisibility(zoom_four_view, true);
		setElementVisibility(zoom_eight_view, true);

		setTextView(deviceName_txt, currentDeviceController.getDeviceID());
		setTextView(modelName_txt, currentDeviceController.getModel());

	}

	/**
	 * Clears all textfields
	 */
	public void clearUI() {

		final String METHODTAG = "clearUI";

		super.clearUI();
		String defaultValue = getResources().getString(R.string.default_value);
		setTextView(htime_txt, defaultValue);
		setTextView(deviceType_txt, defaultValue);
		setTextView(equipment_txt, defaultValue);
		setTextView(face_txt, defaultValue);
		setTextView(motwhile_txt, defaultValue);

		Log.i(CLASSTAG, String.format("%s UI cleared.", METHODTAG));
	}




	//////////////////////////////////////////////////////////////////////////////////
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	INNER CLASSES
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//////////////////////////////////////////////////////////////////////////////////
	/**
	 * SeekBarChangeListener allows the user to select a zoom level
	 * ImageZoomWide, ImagezoomNormal, ImageZoomtele, ImageZoomSuper
	 */
	private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

		/**
		 * ClassName
		 */
		private final String CLASSTAG = SeekBarChangeListener.class.getSimpleName();

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

			final String METHODTAG = ".onProgressChanged";

			Response response = null;

			Log.v(CLASSTAG, String.format("%sProgress: %d", METHODTAG, progress));
			switch (progress) {
				case 0:
					response = currentDeviceController.sendCommand(Types.Commands.ImageZoomWide);
					break;
				case 1:
					response = currentDeviceController.sendCommand(Types.Commands.ImageZoomNormal);
					break;
				case 2:
					response = currentDeviceController.sendCommand(Types.Commands.ImageZoomTele);
					break;
				case 3:
					response = currentDeviceController.sendCommand(Types.Commands.ImageZoomSuper);
					break;
				default:
					Log.d(CLASSTAG, METHODTAG + ": Invalid zoom level");
					break;
			}

			ErrorObject error = response.getError();
			if (error != null) {
				showAlert(formatErrorMessage(error));
			}
		}

		/**
		 * Method needs to be implemented but has no functionality in this use case
		 */
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			return;
		}

		/**
		 * Method needs to be implemented but has no functionality in this use case
		 */
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			return;
		}
	}


	/**
	 * Listener for the following button events:
	 * 1. Press on send command (Pop up list with all commands appears)
	 * 2. Measure Distance & Turn on the laser
	 * 3. Start live image transfer
	 * 4. Stop live image transfer
	 */
	private class ButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {

			final String METHODTAG = ".ButtonListener.onClick";
			switch (view.getId()) {
				case R.id.sendCommand_btn:
					showCommandDialog();
					break;
				case R.id.distance_btn:
					ErrorObject error =
							((WifiDeviceController) currentDeviceController).sendMeasurePolarCommand();
					if(error != null) {
						showAlert(formatErrorMessage(error));
					}
					break;
				case R.id.startLiveImage_btn:
					startLiveImage();
					break;
				case R.id.stopLiveImage_btn:
					stopLiveImage();
					break;
				default:
					Log.d(CLASSTAG, METHODTAG + ": "+ view.getId() + " not implemented");
			}
		}
	}


	/**
	 * Class is relevant for vertical and horizontal movements of the motor
	 */
	private class MotorControlsListener implements View.OnTouchListener {

		/**
		 * ClassName
		 */
		private final String CLASSTAG = MotorControlsListener.class.getSimpleName();


		/**
		 * Captures the touch event of one of the buttons (left, right, up or down) responsible for the camera movement
		 * @param view
		 * @param motionEvent
		 * @return
		 */
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {

			final String METHODTAG = ".onTouch";
			Log.v(CLASSTAG, METHODTAG + "X: " + motionEvent.getX() + ", Y: " + motionEvent.getY());

			// if its no app 3D device
			if (!(currentDeviceController.getDeviceType().equals(Types.DeviceType.Disto3D))) {
				return false;
			}

			switch (motionEvent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					return handleDownEvent(view);
				case MotionEvent.ACTION_UP:
					return handleUpEvent(view);
				default:
					return false;
			}
		}

		/**
		 * handles movement of the camera if user presses an arrow button
		 * General sequence:
		 * 1. Send command that sets the velocity of the cammera rotation
		 * 2. Set movement horizontalAngleWithTilt
		 * For Horizontal movement: Move camera around the own center either in a left rotation or right rotation
		 * Vertical movement: Move camera up or down
		 * 3. Set horizontalAngleWithTilt: Either horizontalAngleWithTilt movement or vertical movement of the camer
		 */
		private boolean handleDownEvent(View view) {

			String METHODTAG = "handleDownEvent";
			Log.v(CLASSTAG, METHODTAG + "Called");
			// sending commands "too" fast seems ok for 3DD


			int viewId = view.getId();

			if (viewId == R.id.up_btn || viewId == R.id.up_button_container) {

				currentDeviceController.sendCommandMoveMotorUp(50);
				// FOR TESTING
				// currentDeviceController.sendCommandMotorPositionAbsolute(1.0, 1.0, false);
				// currentDeviceController.sendCommandMotorPositionRelative(1.0, 1.0, false);


			} else if (viewId == R.id.right_btn || viewId == R.id.right_button_container) {

				currentDeviceController.sendCommandMoveMotorRight(50);

			} else if (viewId == R.id.down_btn || viewId == R.id.down_button_container) {

				currentDeviceController.sendCommandMoveMotorDown(50);

			} else if (viewId == R.id.left_btn || viewId == R.id.left_button_container) {

				currentDeviceController.sendCommandMoveMotorLeft(50);

			} else {
				return false;
			}

			return true;
		}


		/**
		 * Handles if the user releases button.
		 * Release of button stops camera movement.
		 *
		 * @param view view
		 * @return
		 */
		private boolean handleUpEvent(View view) {

			String METHODTAG = "handleUpEvent";

			int viewId = view.getId();

			if (viewId == R.id.up_btn || viewId == R.id.up_button_container) {
				currentDeviceController.sendCommandPositionStopVertical();


			} else if (viewId == R.id.right_btn || viewId == R.id.right_button_container) {
				currentDeviceController.sendCommandPositionStopHorizontal();


			} else if (viewId == R.id.down_btn || viewId == R.id.down_button_container) {
				currentDeviceController.sendCommandPositionStopVertical();


			} else if (viewId == R.id.left_btn || viewId == R.id.left_button_container) {
				currentDeviceController.sendCommandPositionStopHorizontal();


			} else {
				return false;
			}

			return true;
		}
	}

	/**
	 /**
	 * Class is relevant for camera movements.
	 */
	private class CameraMovementListener implements View.OnTouchListener {
		/**
		 * ClassName
		 */
		private final String CLASSTAG = CameraMovementListener.class.getSimpleName();

		/**
		 * onTouch: Defines the relationship beteen the UI and the 3DD motor movement
		 */
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {

			final String METHODTAG = ".CameraMovementListener.onTouch";

			double x_equivalent = motionEvent.getX() * 640 / previewImage.getWidth();
			double y_equivalent = motionEvent.getY() * 480 / previewImage.getHeight();

			if (imageController != null) {
				LiveImagePixelConverter.PolarCoordinates polar =
						imageController.touchHandler(x_equivalent, y_equivalent);

				currentDeviceController.
						sendCommandMotorPositionAbsolute(polar._Hz, polar._V, false);
			}
			return true;
		}
	}
}
