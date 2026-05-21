package com.terra.terradisto.distosdkapp.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.WifiDeviceController;
import com.terra.terradisto.distosdkapp.utilities.ImageController;
import com.terra.terradisto.distosdkapp.utilities.Logs;
import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.commands.Image;
import ch.leica.sdk.Devices.DeviceManager;


public class WifiInformationActivity
		extends BaseInformationActivity
		implements WifiDeviceController.WifiDataListener,
		WifiDeviceController.UiListener,
		WifiDeviceController.WifiRequestListener,
		ImageController.ImageListener{

	/**
	 * ClassName
	 */
	private static final String CLASSTAG = WifiInformationActivity.class.getSimpleName();

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ UI-ELEMENTS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//Textfields present information to user
	/**
	 * Current status of the connection to the currentDeviceController
	 */
	public TextView status_txt;

	/**
	 * ModelName
	 */
	public TextView modelName_txt;

	/**
	 * Device ID
	 */
	public TextView deviceName_txt;

	/**
	 * Distance
	 */
	public TextView distance_txt;

	/**
	 * Distance Unit
	 */
	public TextView distanceUnit_txt;

	/**
	 * Horizontal angle with tilt
	 */
	public TextView horizontalAngleWithTilt_txt;

	/**
	 * Vertical angle with tilt
	 */
	public TextView verticalAngleWithTilt_txt;

	/**
	 * Horizontal angle without tilt
	 */
	public TextView horizontalAngleWithoutTilt_txt;

	/**
	 * Vertical angle without tilt
	 */
	public TextView verticalAngleWithoutTilt_txt;

	/**
	 * Angle unit
	 */
	public TextView angleUnit_txt;

	/**
	 * Serial Number
	 */
	public TextView serialNumber_txt;

	/**
	 * Software Version
	 */
	public TextView softwareVersion_txt;

	/**
	 * IP-Address
	 */
	public TextView ipAddress_txt;

	/**
	 * Software Name
	 */
	public TextView softwareName_txt;

	/**
	 * Temperature of HZ sensor
	 */
	public TextView hz_temp_txt;

	/**
	 * Temperature of V sensor
	 */
	public TextView v_temp_txt;

	/**
	 * Temperature of Bluetooth Chip
	 */
	public TextView ble_temp_txt;

	/**
	 * Temperature of EDM sensor
	 */
	public TextView edm_temp_txt;

	/**
	 * Battery voltage
	 */
	public TextView batteryVoltage_txt;

	/**
	 * Battery status
	 */
	public TextView batteryStatus_txt;

	/**
	 * Data comming through event channel
	 */
	public TextView event_txt;

	/**
	 * LED System Error
	 */
	public TextView led_se_txt;

	/**
	 * LED Warning
	 */
	public TextView led_w_txt;

	/**
	 * Horizontal angle with all corrections for which the tilt applies
	 */
	public TextView ihz_txt;

	/**
	 * Longitudinal tilt to horizontal angle
	 */
	public TextView ilen_txt;

	/**
	 * Transverse tilt to horizontal angle
	 */
	public TextView iCross_txt;

	/**
	 * Mac Address
	 */
	public TextView macAddress_txt;

	/**
	 * Firmwareversion of Wifi module
	 */
	public TextView wifiModuleVersion_txt;

	/**
	 * WLan channel
	 */
	public TextView wifiChannelNumber_txt;

	/**
	 * Wlan Frequency
	 */
	public TextView wifiFrequency_txt;

	/**
	 * Current network name
	 */
	public TextView wifiESSID_txt;

	/**
	 * Height index
	 */
	public TextView userVind_txt;

	/**
	 * X offset
	 */
	public TextView userCamlasX_txt;

	/**
	 * Y offset
	 */
	public TextView userCamlasY_txt;

	/**
	 * Raw Device Response
	 */
	public TextView deviceResponse_txt;

	/**
	 * Sensivity Mode (0 = normal mode, 1 = rugged mode)
	 */
	public TextView sensitiveMode_txt;

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ BUTTONS implementing functionality of leica device
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * Open the available commands panel
	 */
	public Button sendCommand_btn;

	/**
	 * Sends "Distance"
	 */
	public Button distance_btn;


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ IMAGE MEMBERS
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++

	//Variables hold information transferred from the Leica devices
	public ImageView previewImage;
	public ImageView levelImage; //Indicates level status
	public ImageController imageController;


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ BROADCAST RECEIVER
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * listen to changes to the wifi adapter
	 */
	BroadcastReceiver RECEIVER_wifiChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			currentDeviceController.checkForReconnection(WifiInformationActivity.this);
		}
	};


	/////////////////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	ANDROID LIFECYCLE
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String METHODTAG = ".onCreate";
		Log.i(CLASSTAG, String.format("%s - Activity created successfully. ", METHODTAG));
	}

	@Override
	public void setDeviceController() {
		if(this.currentDeviceController == null) {
			this.currentDeviceController = new WifiDeviceController(this);
		}
	}

	@Override
	public void setContentView() {
		setContentView(R.layout.activity_wifi_information);
	}

	@Override
	void initMembers() {

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++ Initialize Image Fields
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++
		previewImage = findViewById(R.id.picture_preview);
		levelImage = findViewById(R.id.level_view);


		//+++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++ Initialize textfields
 		//++++++++++++++++++++++++++++++++++++++++++++++++++++++

        status_txt =  findViewById(R.id.status_txt);
        modelName_txt =  findViewById(R.id.modelName_txt);
        deviceName_txt =  findViewById(R.id.deviceName_txt);
		distance_txt =  findViewById(R.id.distance_txt);
		distanceUnit_txt =  findViewById(R.id.distance_unit_txt);
		verticalAngleWithTilt_txt =  findViewById(R.id.vertical_angle_with_tilt_txt);
		horizontalAngleWithTilt_txt =  findViewById(R.id.horizontal_angle_with_tilt_txt);
		horizontalAngleWithoutTilt_txt =  findViewById(R.id.horizontal_angle_without_tilt_txt);
		verticalAngleWithoutTilt_txt =  findViewById(R.id.vertical_angle_without_tilt_txt);
		angleUnit_txt =  findViewById(R.id.angle_unit_txt);
		ipAddress_txt =  findViewById(R.id.ip_txt);

		softwareName_txt =  findViewById(R.id.softwareName_txt);
		v_temp_txt =  findViewById(R.id.v_temp_txt);
		ble_temp_txt =  findViewById(R.id.ble_temp_txt);
		edm_temp_txt =  findViewById(R.id.edm_temp_txt);
		hz_temp_txt =  findViewById(R.id.hz_temp_txt);
		batteryVoltage_txt =  findViewById(R.id.batteryVoltage_txt);
		batteryStatus_txt =  findViewById(R.id.batteryStatus_txt);
		led_se_txt =  findViewById(R.id.led_se_txt);
		led_w_txt =  findViewById(R.id.led_w_txt);
		ihz_txt =  findViewById(R.id.ihz_txt);
		ilen_txt =  findViewById(R.id.ilen_txt);
		iCross_txt =  findViewById(R.id.icross_txt);
		macAddress_txt =  findViewById(R.id.macaddress_txt);
		wifiModuleVersion_txt =  findViewById(R.id.wifimoduleversion_txt);
		wifiChannelNumber_txt =  findViewById(R.id.wifichannel_txt);
		wifiFrequency_txt =  findViewById(R.id.wififreq_txt);
		wifiESSID_txt =  findViewById(R.id.wifiessid_txt);
		userVind_txt =  findViewById(R.id.usrvind_txt);
		userCamlasX_txt =  findViewById(R.id.usercamlasx_txt);
		userCamlasY_txt =  findViewById(R.id.usercamlasy_txt);
		deviceResponse_txt =  findViewById(R.id.deviceresponse_txt);
		sensitiveMode_txt =  findViewById(R.id.sensitivemode_txt);
		event_txt =  findViewById(R.id.event_txt);
		serialNumber_txt =  findViewById(R.id.serialNumber_txt);
		softwareVersion_txt =  findViewById(R.id.softwareVersion_txt);

		sendCommand_btn =  findViewById(R.id.sendCommand_btn);
		distance_btn =  findViewById(R.id.distance_btn);

		imageController = new ImageController(WifiInformationActivity.this);

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++ Set UI Listeners
 		//++++++++++++++++++++++++++++++++++++++++++++++++++++++
		View.OnClickListener buttonListener = new ButtonListener();

		setOnClickListener(sendCommand_btn, buttonListener);
		setOnClickListener(distance_btn, buttonListener);

		// set values when activity got recreated
		if (currentDeviceController != null) {
			setTextView(deviceName_txt, currentDeviceController.getDeviceName());
			setTextView(modelName_txt, currentDeviceController.getModel());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		final String METHODTAG = ".onResume";

		//Receivers
		setReceivers("android.net.wifi.STATE_CHANGE", RECEIVER_wifiChange);

		// check if wifi adapter is enabled
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		// if wifi is off do nothing
		if (wifiManager != null) {
			if (!wifiManager.isWifiEnabled()) {
				showAlert("Please turn on Wifi.");
			} else {
				currentDeviceController.checkForReconnection(WifiInformationActivity.this);
			}
		}

		if (currentDeviceController.getConnectionState().equals(Device.ConnectionState.connected)) {

			Log.i(CLASSTAG, METHODTAG + " device is connected.");
			this.currentDeviceController.
					currentSSIDforAPmode = currentDeviceController.getWifiName(getApplicationContext());
		}

        //Set Status
        setTextView(status_txt, currentDeviceController.status);
		setTextView(deviceName_txt, currentDeviceController.getDeviceName());

		//Set all wifi UI Elements with default values and configurations
		setActivityUI(currentDeviceController.getDeviceID());

		//Ask for device Information (Serial number & software version)
		ErrorObject errorObject = currentDeviceController.getDeviceInfo();

		Log.d(CLASSTAG, String.format("%s Activity onResume successful.", METHODTAG));
	}

	@Override
	protected void onStop() {
		super.onStop();

		final String METHODTAG = ".onStop";

		if(currentDeviceController == null){
			Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
			return;
		}

		unregisterReceivers(RECEIVER_wifiChange);

		if (previewImage != null) {
			previewImage.setImageDrawable(null);
		}

		InformationActivityData informationActivityData = Clipboard.INSTANCE.getInformationActivityData();
		informationActivityData.isSearchingEnabled = true;

		Log.d(CLASSTAG, String.format("%s: Activity Stopped successfully ", METHODTAG));
	}


	/**
	 * Remove all the listeners associated with the currentDeviceController and the reconnection procedure
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}


	///////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	INTERFACE IMPLEMENTATION - DeviceStatusListener
	//++++++++++++++++++++++++++++++++++++++++++++++++++//
	//////////////////////////////////////////////////////

	/**
	 * Gets called as soon the connection state of a devices changes.
	 * Show dialog if device gets connected or disconnected.
	 * Change status text.
	 *
	 * @param deviceID deviceID
	 * @param state    connection State
	 */
	@Override
	public void onConnectionStateChanged(final String deviceID,
										 final Device.ConnectionState state) {

		final String METHODTAG = ".onConnectionStateChanged";

		Log.d(CLASSTAG, String.format("%s: %s, state: %s", METHODTAG, deviceID, state));

		if (currentDeviceController == null) {
			Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
			return;
		}


		setTextView(status_txt, state.toString());

		if (state == Device.ConnectionState.connected) {
			currentDeviceController.currentSSIDforAPmode =
					currentDeviceController.getWifiName(getApplicationContext());

			Log.d(CLASSTAG,
					String.format(
							"%s: currentSSIDforAPmode: %s",
							METHODTAG,
							currentDeviceController.currentSSIDforAPmode
					)
			);

			showConnectedDisconnectedDialog(true);
			currentDeviceController.stopFindingDevices();
			setActivityUI(currentDeviceController.getModel());
			setCommandsUIOn();


		} else if (state == Device.ConnectionState.disconnected) {

			showConnectedDisconnectedDialog(false);
			currentDeviceController.checkForReconnection(WifiInformationActivity.this);
			setCommandsUIOff();

		}

	}

	@Override
	public void onError(int errorCode, String errorMessage) {

		String METHODTAG = ".requestErrorMessageDialog";
		Log.e(
				CLASSTAG,
				String.format("%s: errorCode: %d Message: %s", METHODTAG, errorCode, errorMessage)
				, null
		);

		showAlert(
				String.format(Locale.getDefault(), "errorCode: %d, %s", errorCode, errorMessage)
		);
	}

	@Override
	public void onReconnect() {
		final String METHODTAG = ".onReconnect";
		Log.d(CLASSTAG, String.format("%s: in progress", METHODTAG));
		currentDeviceController.setReconnectionHelper(WifiInformationActivity.this);
	}

	@Override
	public void onStatusChange(String string) {
		setTextView(status_txt, string);
		if ("reconnecting".equals(string)) {
			showAlert("Try to automatically reconnect now.");
		}
	}

	/////////////////////////////////////////////////////////////////////////////////
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	DATA EXTRACTION - INTERFACE IMPLEMENTATION - WifiDataListener -
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	////////////////////////////////////////////////////////////////////////////////

    /**
     * Method gets called after data were received
     * Extract which information are send by the leica devices to properly update the corresponding
     * ui element (E.g. if a distance measurement is transferred update textfield which corresponds to the distance_txt )
     * Switch case cover all measurements which can be transferred.
     *
     * @param receivedData string
     */
	@Override
	public void onEventReceived(String receivedData) {
		setTextView(event_txt, receivedData);
	}

	@Override
	public void onLevelReceived(int dataInt) {
		setIstateImage(dataInt);
	}

	@Override
	public void onImageReceived(Image image) {

		imageController.setImage(image);
	}


	/**
	IState value:
		/*
		 * Level status [0..5]
		 0  tilt plane "probably" up to date (depends on next value 1 or 3)
		 1  Tilt plane was recently redetermined and is identical
		 2  Tilt plane (old) cannot currently be measured (system overload)
		 3  Tilt plane was recently redetermined (value has altered)
		 4  Tilt plane has not yet been determined (after power up, level enabled)
		 5  Tilt measurement deactivated
		 6  Tilt plane "unstable" (possible reason vibrations, oscillations, …)
		 **/
	public void setIstateImage(final int data) {

		final String METHODTAG = ".setIstateImage";
		Log.d(CLASSTAG, String.format("%s: called with value: %d", METHODTAG, data));

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				levelImage.invalidate();

				if (data == 0 || data == 1 || data == 2) {
					levelImage.setImageResource(R.drawable.ic_level012);
				}
				if (data == 3 || data == 4) {
					levelImage.setImageResource(R.drawable.ic_level4);
				}
				if (data == 5 || data == 6) {
					levelImage.setImageResource(R.drawable.ic_level356);
				}
			}
		});

	}

	//	MEASUREMENTS
	@Override
	public void onDistance_Received(String value, String unit) {
		setTextView(distance_txt, value);
		setTextView(distanceUnit_txt, unit);
	}

	@Override
	public void onHorizontalAngleWithTilt_hz_Received(String value, String unit) {
		setTextView(horizontalAngleWithTilt_txt, value);
		setTextView(angleUnit_txt, unit);
	}

	@Override
	public void onVerticalAngleWithTilt_v_Received(String value, String unit) {
		setTextView(verticalAngleWithTilt_txt, value);
		setTextView(angleUnit_txt, unit);
	}

	@Override
	public void onHorizontalAngleWithoutTilt_ni_hz_Received(String value, String unit) {
		setTextView(horizontalAngleWithoutTilt_txt, value);
		setTextView(angleUnit_txt, unit);
	}

	@Override
	public void onVerticalAngleWithoutTilt_ni_hz_Received(String value, String unit) {
		setTextView(verticalAngleWithoutTilt_txt, value);
		setTextView(angleUnit_txt, unit);
	}

	@Override
	public void onICross_Received(float iCross) {
		setTextView(iCross_txt, String.valueOf(iCross));
	}

	@Override
	public void onIhz_Received(float ihz) {
		setTextView(ihz_txt, String.valueOf(ihz));
	}

	@Override
	public void onIlen_Received(float ilen) {
		setTextView(ilen_txt, String.valueOf(ilen));
	}

	@Override
	public void onIState_Received(int iState) {
		setIstateImage(iState);
	}

	@Override
	public void onIP_Received(String ip) {
		setTextView(ipAddress_txt, ip);
	}

	@Override
	public void onSerialNumber_Received(String serialNumber) {
		setTextView(serialNumber_txt, serialNumber);
	}

	@Override
	public void onSoftwareName_Received(String softwareName) {
		setTextView(softwareName_txt, softwareName);
	}

	@Override
	public void onSoftwareVersion_Received(String softwareVersion) {
		setTextView(softwareVersion_txt, softwareVersion);
	}

	@Override
	public void onDeviceType_Received(int deviceType) {

	}

	@Override
	public void onMacAddress_Received(String macAddress) {
		setTextView(macAddress_txt, macAddress);
	}

	@Override
	public void onWifiModuleVersion_Received(String wifiModuleVersion) {
		setTextView(wifiModuleVersion_txt, wifiModuleVersion);
	}

	@Override
	public void onWifiESSID_Received(String wifiESSID) {
		setTextView(wifiESSID_txt, wifiESSID);
	}

	@Override
	public void onWifiChannelNumber_Received(int wifiChannelNumber) {
		setTextView(wifiChannelNumber_txt, String.valueOf(wifiChannelNumber));
	}

	@Override
	public void onWifiFrequency_Received(int wifiFrequency) {
		setTextView(wifiFrequency_txt, String.valueOf(wifiFrequency));
	}

	@Override
	public void onUserVind_Received(float userVind) {
		setTextView(userVind_txt, String.valueOf(userVind));
	}

	@Override
	public void onUserCamLasX_Received(float userCamLasX) {
		setTextView(userCamlasX_txt, String.valueOf(userCamLasX));
	}

	@Override
	public void onUserCamLasY_Received(float userCamLasY) {
		setTextView(userCamlasY_txt, String.valueOf(userCamLasY));
	}

	@Override
	public void onSensitiveMode_Received(float sensitiveMode) {
		setTextView(sensitiveMode_txt, String.valueOf(sensitiveMode));
	}

	@Override
	public void onFace_Received(int face) {

	}

	@Override
	public void onBatteryVoltageData_Received(float batteryVoltage) {
		setTextView(batteryVoltage_txt, String.valueOf(batteryVoltage));
	}

	@Override
	public void onBatteryStatusData_Received(int batteryStatus) {
		setTextView(batteryStatus_txt, String.valueOf(batteryStatus));
	}

	@Override
	public void onTemperatureHorizontalAngleSensor_Hz_Received(float temperatureHorizontalAngleSensor_hz) {
		setTextView(hz_temp_txt, String.valueOf(temperatureHorizontalAngleSensor_hz));
	}

	@Override
	public void onTemperatureVerticalAngleSensor_V_Received(float temperatureVerticalAngleSensor_v) {
		setTextView(v_temp_txt, String.valueOf(temperatureVerticalAngleSensor_v));
	}

	@Override
	public void onTemperatureDistanceMeasurementSensor_Edm_Received(float temperatureDistanceMeasurementSensor_edm) {
		setTextView(edm_temp_txt, String.valueOf(temperatureDistanceMeasurementSensor_edm));
	}

	@Override
	public void onTemperatureBLESensor_Received(float temperatureBLESensor) {
		setTextView(ble_temp_txt, String.valueOf(temperatureBLESensor));
	}


	/////////////////////////////////////////////////////////////////
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	INTERFACE IMPLEMENTATION - ImageListener -
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	////////////////////////////////////////////////////////////////
	/**
	 * The ResponseImage class contains all information about the image.
	 * Please note that Live image is handled differently.
	 * @param image image
	 */
	@Override
	public void onImageData_Received(byte[] image) {
		final String METHODTAG = ".onImageData_Received";
		Log.v(CLASSTAG, String.format("%s Setting values in UI", METHODTAG));

		imageController.setImage(image);
	}

	@Override
	public void onPlainData_Received(String data) {
		final String METHODTAG = ".onPlainData_Received";
		Log.v(CLASSTAG, String.format("%s Setting values in UI", METHODTAG));

		setTextView(deviceResponse_txt, data);
	}

	@Override
	public void onImageProcessed(final Bitmap image, boolean imageInProcess) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				previewImage.setImageBitmap(image);
			}
		});


		// this runnable will be executed when the imageview has layouted the image.
		// "Runnable provided to post() method will be executed after view measuring and layouting"
		// - https://stackoverflow.com/questions/17606140/how-to-get-when-an-imageview-is-completely-loaded-in-android
		// without delaying the release of the setImageInProcess, the set image "hangs" more often
		previewImage.postDelayed(new Runnable() {
			@Override
			public void run() {
				imageController.setImageInProcess(Boolean.FALSE);
			}
		}, 50);
	}



	//////////////////////////////////////////////////////////////////////////////////
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	UI
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//////////////////////////////////////////////////////////////////////////////////
	@Override
	public void setCommandsUIOff() {
		float alpha = .5f;
		boolean clickable = false;

		setUIButtonState(sendCommand_btn, alpha, clickable);
		setUIButtonState(distance_btn, alpha, clickable);
	}

	@Override
	public void setCommandsUIOn() {
		float alpha = 1f;
		boolean clickable = true;

		setUIButtonState(sendCommand_btn, alpha, clickable);
		setUIButtonState(distance_btn, alpha, clickable);
	}


	@Override
	public void setActivityUI(final String model) {

		final String METHODTAG = ".setActivityUI";
		Logs.logAvailableCommands(CLASSTAG, METHODTAG, this.getAvailableCommandsString());

		Log.d(CLASSTAG, METHODTAG+" deviceModel: "+ currentDeviceController.getModel());

		if (currentDeviceController == null) {
			return;
		}

		if (currentDeviceController.getCurrentDevice() == null ) {
			return;
		}

		if (deviceName_txt != null) {
			setTextView(deviceName_txt, currentDeviceController.getDeviceID());
		}

		setTextView(modelName_txt, currentDeviceController.getModel());
	}

	/**
	 * Clears all textfields
	 */
	public void clearUI() {

		final String METHODTAG = "clearUI";

		String defaultValue = getResources().getString(R.string.default_value);

		setTextView(distance_txt, defaultValue);
		setTextView(distanceUnit_txt, defaultValue);
		setTextView(verticalAngleWithTilt_txt, defaultValue);
		setTextView(angleUnit_txt, defaultValue);
		setTextView(horizontalAngleWithTilt_txt, defaultValue);
		setTextView(horizontalAngleWithoutTilt_txt, defaultValue);
		setTextView(verticalAngleWithoutTilt_txt, defaultValue);
		setTextView(ipAddress_txt, defaultValue);
		setTextView(softwareName_txt, defaultValue);
		setTextView(hz_temp_txt, defaultValue);
		setTextView(v_temp_txt, defaultValue);
		setTextView(ble_temp_txt, defaultValue);
		setTextView(edm_temp_txt, defaultValue);
		setTextView(batteryVoltage_txt, defaultValue);
		setTextView(batteryStatus_txt, defaultValue);
		setTextView(event_txt, defaultValue);
		setTextView(led_se_txt, defaultValue);
		setTextView(led_w_txt, defaultValue);
		setTextView(ihz_txt, defaultValue);
		setTextView(ilen_txt, defaultValue);
		setTextView(iCross_txt, defaultValue);
		setTextView(macAddress_txt, defaultValue);
		setTextView(wifiModuleVersion_txt, defaultValue);
		setTextView(wifiChannelNumber_txt, defaultValue);
		setTextView(wifiFrequency_txt, defaultValue);
		setTextView(wifiESSID_txt, defaultValue);
		setTextView(userVind_txt, defaultValue);
		setTextView(userCamlasX_txt, defaultValue);
		setTextView(userCamlasY_txt, defaultValue);
		setTextView(sensitiveMode_txt, defaultValue);

		Log.i(CLASSTAG, String.format("%s UI cleared.", METHODTAG));
	}

	@Override
	public void turnCommandsButtonsOn() {
		setCommandsUIOn();
	}

	@Override
	public void turnCommandsButtonsOff() {
		setCommandsUIOff();
	}


	/////////////////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	INNER CLASSES
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	/////////////////////////////////////////////////////////////////
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

			if (currentDeviceController.getCurrentDevice() == null) {
				return;
			}

			switch (view.getId()) {
				case R.id.sendCommand_btn:
					showCommandDialog();
					break;
				case R.id.distance_btn:
					Runnable sendMeasurePolarCommandRunnable = new Runnable() {
						@Override
						public void run() {

							ErrorObject error =
									((WifiDeviceController) currentDeviceController).
											sendMeasurePolarCommand();

							if (error != null) {
								showAlert(formatErrorMessage(error));
							}
						}
					};
					new Thread(sendMeasurePolarCommandRunnable).start();
					break;
				default:
					Log.d(CLASSTAG, METHODTAG + ": " + view.getId() + " not implemented");
			}
		}
	}


	///////////////////////////////////////////////////////////////////////////////////////////
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	// INTERFACE IMPLEMENTATION - RequestListener -
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
	///////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void onRequestWifiTurnOn() {
		this.requestWifiAdapterTurnOn();
	}
	/**
	 * Show bluetooth turnOn dialog
	 */
	synchronized void requestWifiAdapterTurnOn() {
		final String METHODTAG = ".showBluetoothTurnOn";

		if (isAdapterEnabled() == true) {
			Log.d(CLASSTAG, String.format("%s: wifi is already turned on", METHODTAG));
			return;
		}

		currentDeviceController.turnOnAdapterDialogIsShown = true;
		Log.d(CLASSTAG, String.format("%s: turnOnWifiIsShown is true", METHODTAG));

		String title = "Request turn-on Wifi";
		String message = "Wifi has to be turned on.";
		String positiveButtonText = "Turn it on";
		String negativeButtonText = "Cancel";

		Runnable positiveButtonRunnable = new Runnable() {
			@Override
			public void run() {
				currentDeviceController.turnOnAdapterDialogIsShown = false;
				DeviceManager.getInstance(getApplicationContext()).enableWifi();
			}
		};
		Runnable negativeButtonRunnable = new Runnable() {
			@Override
			public void run() {
				currentDeviceController.turnOnAdapterDialogIsShown = false;
			}
		};

		DialogHandler dialogHandler = new DialogHandler();
		dialogHandler.setDialog(
				WifiInformationActivity.this,
				title,
				message,
				false,
				positiveButtonText,
				positiveButtonRunnable,
				negativeButtonText,
				negativeButtonRunnable
		);
		dialogHandler.show();
	}

	public boolean isAdapterEnabled(){
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		if(wifiManager != null){
			return wifiManager.isWifiEnabled();
		}
		return false;

	}
}