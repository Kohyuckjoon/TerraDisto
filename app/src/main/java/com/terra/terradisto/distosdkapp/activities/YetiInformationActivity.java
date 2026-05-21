package com.terra.terradisto.distosdkapp.activities;


import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.YetiDeviceController;
import com.terra.terradisto.distosdkapp.permissions.PermissionsHelper;
import com.terra.terradisto.distosdkapp.update.UpdateController;
import com.terra.terradisto.distosdkapp.utilities.Logs;
import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;
import com.terra.terradisto.distosdkapp.utilities.dialog.UpdateConnectionSelectorDialog;
import com.terra.terradisto.distosdkapp.utilities.dialog.UpdateRegionSelectorDialog;
import ch.leica.sdk.Devices.BleDevice;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.YetiDevice;
import ch.leica.sdk.ErrorHandling.ErrorObject;


public class YetiInformationActivity
        extends BleInformationActivity
        implements YetiDevice.UpdateDeviceListener,
                    YetiDeviceController.YetiDataListener,
                    UpdateController.UpdateProcessListener {
    /**
     * Classname
     */
    private final String CLASSTAG = YetiInformationActivity.class.getSimpleName();
    private final static int STORAGE_PERMISSIONS = 9001;


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++
	//++ Textfields present information to user
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++

    private TextView status_txt;
    private TextView modelName_txt;
    private TextView deviceName_txt;

    //BASIC MEASUREMENTS
    private TextView distance_txt;
    private TextView distanceUnit_txt;
    private TextView inclination_txt;
    private TextView inclinationUnit_txt;
    private TextView direction_txt;
    private TextView directionUnit_txt;
    private TextView timestampBasicMeasurements_txt;

    //P2P MEASUREMENTS
    private TextView hzAngle_txt;
    private TextView veAngle_txt;
    private TextView inclinationStatus_txt;
    private TextView timestampP2PMeasurements_txt;

    //QUATERNION MEASUREMENTS
    private TextView quaternion_X_txt;
    private TextView quaternion_Y_txt;
    private TextView quaternion_Z_txt;
    private TextView quaternion_W_txt;
    private TextView timestampQuaternionMeasurements_txt;

    //ACCELERATION MEASUREMENTS
    private TextView accelerationX_txt;
    private TextView accelerationY_txt;
    private TextView accelerationZ_txt;
    private TextView accSensitivity_txt;
    private TextView rotationX_txt;
    private TextView rotationY_txt;
    private TextView rotationZ_txt;
    private TextView timestampACCRotationMeasurements_txt;
    private TextView rotationSensitivity_txt;

    //MAGNETOMETER MEASUREMENTS
    private TextView magnetometerX_txt;
    private TextView magnetometerY_txt;
    private TextView magnetometerZ_txt;
    private TextView timestampMagnetometerMeasurements_txt;

    //DISTOCOM CHANNEL
    private TextView distocomResponse_txt;
    private TextView distocomEvent_txt;

    //DEVICE INFORMATION
    private TextView brandDistocom_txt;
    private TextView idDistocom_txt;
    private TextView appSoftwareVersion_txt;
    private TextView edmSoftwareVersion_txt;
    private TextView ftaSoftwareVersion_txt;
    private TextView appSerial_txt;
    private TextView edmSerial_txt;
    private TextView ftaSerial_txt;


    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ BUTTONS implementing functionality of leica device
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private Button deviceInfo_btn;
    private Button clear_btn;
    private Button sendCommand_btn;
    private Button update_btn;
    private Button reinstall_btn;
    private Button distance_btn;


    public DialogHandler updateProgressDialog;

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ ADDITIONAL MEMBERS
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    private String version;


    PermissionsHelper storagePermission;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String METHODTAG = ".onCreate";


        updateProgressDialog = new DialogHandler();
        Log.i(CLASSTAG, String.format("%s - Activity created successfully. ", METHODTAG));
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();

    }

    @Override
    public void setDeviceController() {
        //Handle Activity relaunch
        if(this.currentDeviceController == null) {
            this.currentDeviceController = new YetiDeviceController(this);
        }
    }

    @Override
    public void setContentView() {
        setContentView(R.layout.activity_yeti_information);
    }

    @Override
    void initMembers() {

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++ Initialize textfields
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        status_txt              = (TextView) findViewById(R.id.status_txt);
        modelName_txt           = (TextView) findViewById(R.id.modelName_txt);
        deviceName_txt          = (TextView) findViewById(R.id.deviceName_txt);

        distance_txt            = (TextView) findViewById(R.id.distance_txt);
        distanceUnit_txt        = (TextView) findViewById(R.id.distance_unit_txt);
        inclination_txt         = (TextView) findViewById(R.id.inclination_txt);
        inclinationUnit_txt     = (TextView) findViewById(R.id.inclinationUnit_txt);
        direction_txt           = (TextView) findViewById(R.id.direction_txt);
        directionUnit_txt       = (TextView) findViewById(R.id.directionUnit_txt);
        timestampBasicMeasurements_txt
                                = (TextView) findViewById(R.id.timestampBasicMeasurements_txt);

        hzAngle_txt             = (TextView) findViewById(R.id.hzAngle_txt);
        veAngle_txt             = (TextView) findViewById(R.id.veAngle_txt);
        inclinationStatus_txt   = (TextView) findViewById(R.id.inclinationStatus_txt);
        timestampP2PMeasurements_txt
                                = (TextView) findViewById(R.id.timestampP2PMeasurements_txt);

        quaternion_X_txt        = (TextView) findViewById(R.id.quaternionX_txt);
        quaternion_Y_txt        = (TextView) findViewById(R.id.quaternionY_txt);
        quaternion_Z_txt        = (TextView) findViewById(R.id.quaternionZ_txt);
        quaternion_W_txt        = (TextView) findViewById(R.id.quaternionW_txt);
        timestampQuaternionMeasurements_txt
                                = (TextView) findViewById(R.id.timestampQuaternionMeasurements_txt);

        accelerationX_txt       = (TextView) findViewById(R.id.accelerationX_txt);
        accelerationY_txt       = (TextView) findViewById(R.id.accelerationY_txt);
        accelerationZ_txt       = (TextView) findViewById(R.id.accelerationZ_txt);
        accSensitivity_txt      = (TextView) findViewById(R.id.accSensitivity_txt);
        rotationX_txt           = (TextView) findViewById(R.id.rotationX_txt);
        rotationY_txt           = (TextView) findViewById(R.id.rotationY_txt);
        rotationZ_txt           = (TextView) findViewById(R.id.rotationZ_txt);
        rotationSensitivity_txt = (TextView) findViewById(R.id.rotationSensitivity_txt);
        timestampACCRotationMeasurements_txt
                                = (TextView) findViewById(R.id.timestampAccRotationMeasurements_txt);

        magnetometerX_txt       = (TextView) findViewById(R.id.magnetometerX_txt);
        magnetometerY_txt       = (TextView) findViewById(R.id.magnetometerY_txt);
        magnetometerZ_txt       = (TextView) findViewById(R.id.magnetometerZ_txt);
        timestampMagnetometerMeasurements_txt
                                = (TextView) findViewById(R.id.timestampMagnetometerMeasurements_txt);

        distocomResponse_txt    = (TextView) findViewById(R.id.distocomResponse_txt);
        distocomEvent_txt       = (TextView) findViewById(R.id.distocomEvent_txt);

        brandDistocom_txt       = (TextView) findViewById(R.id.brand_txt);
        idDistocom_txt          = (TextView) findViewById(R.id.idDistocom_txt);
        appSoftwareVersion_txt  = (TextView) findViewById(R.id.appSoftwareVersion_txt);
        edmSoftwareVersion_txt  = (TextView) findViewById(R.id.edmSoftwareVersion_txt);
        ftaSoftwareVersion_txt  = (TextView) findViewById(R.id.ftaSoftwareVersion_txt);
        appSerial_txt           = (TextView) findViewById(R.id.appSerial_txt);
        edmSerial_txt           = (TextView) findViewById(R.id.edmSerial_txt);
        ftaSerial_txt           = (TextView) findViewById(R.id.ftaSerial_txt);


        //+++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++ Set UI Listeners
        //++++++++++++++++++++++++++++++++++++++++++++++++++++//
        View.OnClickListener bl = new ButtonListener();

        sendCommand_btn         = (Button) findViewById(R.id.sendCommand_btn);
        distance_btn            = (Button) findViewById(R.id.distance_btn);
        deviceInfo_btn          = (Button) findViewById(R.id.deviceInfo_btn);
        clear_btn               = (Button) findViewById(R.id.clear_btn);
        update_btn              = (Button) findViewById(R.id.update_btn);
        reinstall_btn           = (Button) findViewById(R.id.reinstall_btn);

        setOnClickListener(sendCommand_btn, bl);
        setOnClickListener(distance_btn, bl);
        setOnClickListener(deviceInfo_btn, bl);
        setOnClickListener(clear_btn, bl);
        setOnClickListener(update_btn, bl);
        setOnClickListener(reinstall_btn, bl);

        setCommandsUIOff();
    }


    @Override
    protected void onResume() {
        super.onResume();
        final String METHODTAG = ".onResume";


        Log.d(CLASSTAG, String.format("%s Activity onResume successful.", METHODTAG));

    }

    @Override
    public void resumeActivity(){
        final String METHODTAG = "resumeActivity";

        //Handle Activity relaunch
        if(isAdapterEnabled() == true){
            currentDeviceController.checkForReconnection(YetiInformationActivity.this);
        }

        // setup according to device
        //Set Status
        setTextView(status_txt, currentDeviceController.status);

        //Set DeviceName
        setTextView(deviceName_txt, currentDeviceController.getDeviceName());

        setTextView(modelName_txt, currentDeviceController.getModel());


        //Handle Activity relaunch
        if(currentDeviceController.isUpdateRequested == true){
            startUpdateProcess();
        }else {
            // start bt connection
            ErrorObject error = currentDeviceController.startBTConnection(
                    new BleDevice.BTConnectionCallback() {
                        @Override
                        public void onFinished() {
                            Log.i(METHODTAG, "NOW YOU CAN SEND COMMANDS TO THE DEVICE");
                            // Ask for model number and serial number, this will result async
                            String model = currentDeviceController.getModel();
                            if (model.isEmpty() == false) {
                                setActivityUI(model);
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    getDeviceInfo();

                                }
                            }).start();

                            InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
                            data.isSearchingEnabled = false;



                        }
                    });

            if(error != null){
                Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            }
        }

        if(storagePermission == null){
            storagePermission = new PermissionsHelper(YetiInformationActivity.this);
        }

    }

    private void getDeviceInfo() {
        setCommandsUIOff();
        ErrorObject error = currentDeviceController.getDeviceInfo();
        setCommandsUIOn();
    }


    @Override
    protected void onStopDetailed(){
        final String METHODTAG = ".onStop";

        if (currentDeviceController == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return;
        }



        //Dismiss update dialog
        dismissUpdateProgressDialog();

        //Unregister activity for bluetooth adapter changes
        unregisterReceivers(RECEIVER_bluetoothAdapter);

        // Pause the bluetooth connection
        Log.d(CLASSTAG, String.format("%sonStop.pauseBtConnectionCalled", METHODTAG));


        ErrorObject error = currentDeviceController.pauseBTConnection(new BleDevice.BTConnectionCallback() {
            @Override
            public void onFinished() {
                Log.d("onStop", "NOW Notifications are deactivated in the device");
                currentDeviceController.findAvailableDevices(YetiInformationActivity.this);

            }
        });
        if(error != null) {
            Log.e(CLASSTAG, String.format("%s %s", METHODTAG, error.toString()));
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	INTERFACE IMPLEMENTATION - DEVICE STATUS LISTENER
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /**
     * Check if the device is disconnected, if it is disconnected launch the reconnection function
     *
     * @param deviceID the device ID on which the connection state changed
     * @param state  the current connection state. If state is disconnected,
     *               the device object is not valid anymore.
     *               No connection can be established with this object any more.
     */
    @Override
    public void onConnectionStateChanged(final String deviceID,
                                         final Device.ConnectionState state) {

        final String METHODTAG = ".onConnectionStateChanged";

        Log.d(CLASSTAG,String.format("%s: %s, state: %s",METHODTAG,deviceID,state));

        if (currentDeviceController == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return;
        }


            // set the current state as text
            setTextView(status_txt, state.toString());

            if (currentDeviceController.isReconnectionIsRunning()) {
                setTextView(status_txt, getResources().getString(R.string.reconnecting));
            }

            if (state == Device.ConnectionState.connected) {

                showConnectedDisconnectedDialog(true);
                currentDeviceController.stopFindingDevices();
                // if connected ask for model. this will result in onDataReceived()
                setActivityUI(currentDeviceController.getModel());
                setCommandsUIOn();


            } else if (state == Device.ConnectionState.disconnected) {

                dismissUpdateProgressDialog();
                showConnectedDisconnectedDialog(false);
                if(isAdapterEnabled() == true){
                    currentDeviceController.checkForReconnection(YetiInformationActivity.this);
                }
                setCommandsUIOff();
            }

    }


    @Override
    public void onReconnect() {
        final String METHODTAG = ".onReconnect";
        Log.d(CLASSTAG, String.format("%s: in progress", METHODTAG));
        currentDeviceController.setReconnectionHelper(YetiInformationActivity.this);

    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	DATA EXTRACTION - INTERFACE IMPLEMENTATION - BleDataListener -
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onBasicMeasurements_Received(YetiDeviceController.BasicData basicData) {

        setTextView(distance_txt, basicData.distance);
        setTextView(distanceUnit_txt, basicData.distanceUnit);
        setTextView(inclination_txt, basicData.inclination);
        setTextView(inclinationUnit_txt, basicData.inclinationUnit);
        setTextView(direction_txt, basicData.direction);
        setTextView(directionUnit_txt, basicData.directionUnit);
        setTextView(timestampBasicMeasurements_txt, basicData.timestamp);

    }

    @Override
    public void onP2PMeasurements_Received(YetiDeviceController.P2PData p2pData) {
        setTextView(hzAngle_txt, p2pData.hzValue);
        setTextView(veAngle_txt, p2pData.veValue);
        setTextView(inclinationStatus_txt, p2pData.inclinationStatus);
        setTextView(timestampP2PMeasurements_txt, p2pData.timestamp);

    }

    @Override
    public void onQuaternionMeasurement_Received(YetiDeviceController.QuaternionData quaternionData) {

        setTextView(quaternion_X_txt, quaternionData.quaternionX);
        setTextView(quaternion_Y_txt, quaternionData.quaternionY);
        setTextView(quaternion_Z_txt, quaternionData.quaternionZ);
        setTextView(quaternion_W_txt, quaternionData.quaternionW);
        setTextView(timestampQuaternionMeasurements_txt, quaternionData.timestamp);
    }

    @Override
    public void onAccRotationMeasurement_Received(YetiDeviceController.AccRotData accRotatonMeasurement) {
        setTextView(accelerationX_txt, accRotatonMeasurement.accelerationX);
        setTextView(accelerationY_txt, accRotatonMeasurement.accelerationY);
        setTextView(accelerationZ_txt, accRotatonMeasurement.accelerationZ);
        setTextView(accSensitivity_txt, accRotatonMeasurement.accSensitivity);
        setTextView(rotationX_txt, accRotatonMeasurement.rotationX);
        setTextView(rotationY_txt, accRotatonMeasurement.rotationY);
        setTextView(rotationZ_txt, accRotatonMeasurement.rotationZ);
        setTextView(rotationSensitivity_txt, accRotatonMeasurement.rotationSensitivity);
        setTextView(timestampACCRotationMeasurements_txt, accRotatonMeasurement.timestamp);
    }

    @Override
    public void onMagnetometerMeasurement_Received(YetiDeviceController.MagnetometerData magnetometerData) {
        setTextView(magnetometerX_txt, magnetometerData.magnetometerX);
        setTextView(magnetometerY_txt, magnetometerData.magnetometerY);
        setTextView(magnetometerZ_txt, magnetometerData.magnetometerZ);
        setTextView(timestampMagnetometerMeasurements_txt, magnetometerData.timestamp);

    }

    @Override
    public void onDistocomTransmit_Received(String data) {
        setTextView(distocomResponse_txt, data);
    }

    @Override
    public void onDistocomEvent_Received(String data) {
        setTextView(distocomEvent_txt, data);
    }

    @Override
    public void onBrand_Received(String data) {
        setTextView(brandDistocom_txt, data);
    }

    @Override
    public void onAPPSoftwareVersion_Received(String data) {
        setTextView(appSoftwareVersion_txt, data);
    }

    @Override
    public void onId_Received(String data) {
        setTextView(idDistocom_txt, data);
    }

    @Override
    public void onEDMSoftwareVersion_Received(String data) {
        setTextView(edmSoftwareVersion_txt, data);
    }

    @Override
    public void onFTASoftwareVersion_Received(String data) {
        setTextView(ftaSoftwareVersion_txt, data);
    }

    @Override
    public void onAPPSerial_Received(String data) {
        setTextView(appSerial_txt, data);
    }

    @Override
    public void onEDMSerial_Received(String data) {
        setTextView(edmSerial_txt, data);
    }

    @Override
    public void onFTASerial_Received(String data) {
        setTextView(ftaSerial_txt, data);
    }

    @Override
    public void onModel_Received(String data) {
        setTextView(modelName_txt, data);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    // Commands UI
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setCommandsUIOff() {

        float alpha = .5f;
        final boolean clickable = false;

        setUIButtonState(distance_btn, alpha, clickable);
        setUIButtonState(sendCommand_btn, alpha, clickable);
        setUIButtonState(reinstall_btn, alpha, clickable);
        setUIButtonState(update_btn, alpha, clickable);
        setUIButtonState(clear_btn, alpha, clickable);
        setUIButtonState(deviceInfo_btn, alpha, clickable);

    }

    @Override
    public void setCommandsUIOn() {

        float alpha = 1f;
        final boolean clickable = true;

        setUIButtonState(distance_btn, alpha, clickable);
        setUIButtonState(sendCommand_btn, alpha, clickable);
        setUIButtonState(reinstall_btn, alpha, clickable);
        setUIButtonState(update_btn, alpha, clickable);
        setUIButtonState(clear_btn, alpha, clickable);
        setUIButtonState(deviceInfo_btn, alpha, clickable);

    }


    ////////////////////////////////////////////////////////////////////////////////////////
    /// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	UI
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Show the corresponding UI elements for each of the models
     * Different Leica models support different BTLE functionality.
     *
     * @param deviceModel Device Model
     */
    @Override
    protected void setActivityUI(String deviceModel) {

        final String METHODTAG = ".setActivityUI";

        Logs.logAvailableCommands(CLASSTAG, METHODTAG, this.getAvailableCommandsString());
        Log.d(CLASSTAG, String.format("%s deviceModel: %s", METHODTAG, deviceModel));

        setTextView(modelName_txt, deviceModel);

        if (currentDeviceController == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return;
        }
        if (currentDeviceController.getCurrentDevice() == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController.getCurrentDevice()");
            return;
        }

        setElementVisibility(distance_btn, true);
        setElementVisibility(sendCommand_btn, true);
        setElementVisibility(clear_btn, true);
        setElementVisibility(update_btn, true);
        setElementVisibility(reinstall_btn, true);
        setElementVisibility(deviceInfo_btn, true);

    }

    /**
     * Clear all textfields
     */
    void clearUI() {

        String defaultValue = getResources().getString(R.string.default_value);
        setTextView(distance_txt, defaultValue);
        setTextView(distanceUnit_txt, defaultValue);
        setTextView(inclination_txt, defaultValue);
        setTextView(inclinationUnit_txt, defaultValue);
        setTextView(direction_txt, defaultValue);
        setTextView(directionUnit_txt, defaultValue);
        setTextView(timestampBasicMeasurements_txt, defaultValue);

        setTextView(hzAngle_txt, defaultValue);
        setTextView(veAngle_txt, defaultValue);
        setTextView(inclinationStatus_txt, defaultValue);
        setTextView(timestampP2PMeasurements_txt, defaultValue);

        setTextView(quaternion_X_txt, defaultValue);
        setTextView(quaternion_Y_txt, defaultValue);
        setTextView(quaternion_Z_txt, defaultValue);
        setTextView(quaternion_W_txt, defaultValue);
        setTextView(timestampQuaternionMeasurements_txt, defaultValue);

        setTextView(accelerationX_txt, defaultValue);
        setTextView(accelerationY_txt, defaultValue);
        setTextView(accelerationZ_txt, defaultValue);
        setTextView(accSensitivity_txt, defaultValue);
        setTextView(rotationX_txt, defaultValue);
        setTextView(rotationY_txt, defaultValue);
        setTextView(rotationZ_txt, defaultValue);
        setTextView(rotationSensitivity_txt, defaultValue);
        setTextView(timestampACCRotationMeasurements_txt, defaultValue);

        setTextView(magnetometerX_txt, defaultValue);
        setTextView(magnetometerY_txt, defaultValue);
        setTextView(magnetometerZ_txt, defaultValue);
        setTextView(timestampMagnetometerMeasurements_txt, defaultValue);

        setTextView(distocomEvent_txt, defaultValue);
        setTextView(distocomResponse_txt, defaultValue);

        setTextView(brandDistocom_txt, defaultValue);
        setTextView(idDistocom_txt, defaultValue);
        setTextView(appSoftwareVersion_txt, defaultValue);
        setTextView(edmSoftwareVersion_txt, defaultValue);
        setTextView(ftaSoftwareVersion_txt, defaultValue);
        setTextView(appSerial_txt, defaultValue);
        setTextView(edmSerial_txt, defaultValue);
        setTextView(ftaSerial_txt, defaultValue);

    }





    /**
     * Implemented method from Device.UpdateDeviceListener
     *
     * @param bytesSent        the number of bytes that are already sent
     * @param bytesTotalNumber the total number of bytes that should be sent
     */
    @Override
    public void onProgress(long bytesSent, long bytesTotalNumber) {

        final String METHODTAG = ".onProgress";

        Log.d(CLASSTAG,
                String.format(
                        "%s: number of bytes sent: %d of total bytes: %d",
                        METHODTAG, bytesSent, bytesTotalNumber)
        );

        String title = String.format("Transferring data to Device: %s", dialogTitle);
        String message = String.format("%s\n Progress: %s kb /%s kb", version, bytesSent / 1024, bytesTotalNumber / 1024);
        setUpdateProgressDialog(title, message);


    }

    @Override
    public void onFirmwareUpdateStarted(String filename, String version) {
        final String METHODTAG = ".onFirmwareUpdateStarted";

        if (filename != null && version != null) {
            Log.d(CLASSTAG,
                    String.format(
                            "%s: Update Information: %s Version: %s",
                            METHODTAG,
                            filename,
                            version
                    )
            );

            this.dialogTitle = filename;
            this.version = String.format(" Version: %s", version);
        }

    }

    @Override
    public void requestDismissUpdateProgressDialog() {
       dismissUpdateProgressDialog();

    }

    @Override
    public void requestUpdateProgressDialog(String title) {
        this.showInitialUpdateProgressDialog(title);
    }

    @Override
    public void onUpdateError(ErrorObject errorObject) {

        setCommandsUIOn();
        this.currentDeviceController.isUpdateRequested = false;
        DialogHandler errorDialog = new DialogHandler();
        String title = "UPDATE ERROR: ";
        errorDialog.setDialog(YetiInformationActivity.this,title, errorObject.getErrorMessage(), true );
        errorDialog.show();
    }

    @Override
    public void requestRegionSelectorDialog(final String message, final boolean hasDeviceUpdate, final boolean hasComponentsUpdate) {

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        UpdateRegionSelectorDialog dialog =
                                new UpdateRegionSelectorDialog(
                                        YetiInformationActivity.this,
                                        new UpdateRegionSelectorDialog.IUpdateRegion() {
                                            @Override
                                            public void onSelectUpdateTarget(UpdateController.UpdateRegion updateRegion) {

                                                ((YetiDeviceController)currentDeviceController).runUpdateProcess(updateRegion);
                                            }
                                            @Override
                                            public void onCancel() {
                                                currentDeviceController.isUpdateRequested = false;
                                                setCommandsUIOn();
                                            }

                                        });

                        dialog.setCanceledOnTouchOutside(false);

                        dialog.setMessage(message);
                        dialog.setTxt_updateStr("?");
                        dialog.show();

                        if (hasDeviceUpdate == false) {
                            dialog.updateDevice.setEnabled(false);
                            dialog.updateDevice.setVisibility(View.INVISIBLE);
                            dialog.updateBoth.setEnabled(false);
                            dialog.updateBoth.setVisibility(View.INVISIBLE);

                        }
                        if (hasComponentsUpdate == false) {
                            dialog.updateComponent.setEnabled(false);
                            dialog.updateComponent.setVisibility(View.INVISIBLE);
                            dialog.updateBoth.setEnabled(false);
                            dialog.updateBoth.setVisibility(View.INVISIBLE);
                        }
                    }
                }
        );
    }

    @Override
    public void requestUpdateConnectionSelectorDialog() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UpdateConnectionSelectorDialog dialog =
                        new UpdateConnectionSelectorDialog(
                                YetiInformationActivity.this,
                                new UpdateConnectionSelectorDialog.IUpdateConnection() {
                                    @Override
                                    public void onSelectUpdateConnection(final UpdateController.UpdateConn updateConn) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if(updateConn.equals(UpdateController.UpdateConn.offline)){
                                                    // firmware update on disk found

                                                    ((YetiDeviceController)currentDeviceController).handleAvailableFirmwareUpdate(
                                                            true,
                                                            YetiInformationActivity.this
                                                    );

                                                }else if(updateConn.equals(UpdateController.UpdateConn.online)){
                                                    // firmware update online
                                                    ((YetiDeviceController)currentDeviceController).handleAvailableFirmwareUpdate(
                                                            false,
                                                            YetiInformationActivity.this
                                                    );
                                                }
                                            }
                                        }).start();
                                    }

                                    @Override
                                    public void onCancel() {
                                        currentDeviceController.isUpdateRequested = false;
                                        setCommandsUIOn();
                                    }
                                }
                        );


                dialog.setCanceledOnTouchOutside(false);

                String message = "";
                message = String.format("%s Which kind of update do you want? \n", message);
                dialog.setMessage(message);
                dialog.show();//TODO: check  FATAL EXCEPTION: main Process: ch.leica.distosdkapp, PID: 16879, after cancelling the update
            }
        });

    }


    private void dismissUpdateProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (updateProgressDialog != null) {
                    updateProgressDialog.dismiss();
                }
            }
        });
    }

    private String dialogTitle = "Transferring data to Device: ";

    private void setUpdateProgressDialog(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (updateProgressDialog == null) {
                    showInitialUpdateProgressDialog(title);
                }
                else {

                    updateProgressDialog.setMessage(message);
                    updateProgressDialog.show();
                }
            }
        });
    }


    void showInitialUpdateProgressDialog(final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (updateProgressDialog != null && updateProgressDialog.isShowing()) {
                    updateProgressDialog.dismiss();
                }

                String message = "Please wait...";
                if(updateProgressDialog != null) {
                    updateProgressDialog.setDialog(
                            YetiInformationActivity.this,
                            title,
                            message,
                            false
                    );
                    updateProgressDialog.show();
                }
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	INNER CLASSES
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////
    /**
     * Defines the behavior of the buttons in the Activity
     */
    private class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            final String METHODTAG = ".ButtonListener.onClick";

            if (currentDeviceController.getCurrentDevice() == null) {
                Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController.getCurrentDevice()");
                return;
            }

            switch (v.getId()) {
                case R.id.sendCommand_btn: {
                    showCommandDialog();
                }
                break;
                case R.id.distance_btn: {
                    Runnable sendDistanceCommandRunnable = new Runnable() {
                        @Override
                        public void run() {
                            ErrorObject error =
                                    ((YetiDeviceController) currentDeviceController).sendDistanceCommand();
                            if (error != null) {
                                showAlert(formatErrorMessage(error));
                            }
                        }
                    };
                    new Thread(sendDistanceCommandRunnable).start();

                }
                break;
                case R.id.clear_btn: {
                    clearUI();
                }
                break;
                case R.id.deviceInfo_btn: {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ErrorObject error = currentDeviceController.getDeviceInfo();
                        }
                    }).start();
                }
                break;
                case R.id.update_btn: {
                    setCommandsUIOff();
                    startUpdateProcess();

                }
                break;
                case R.id.reinstall_btn: {
                    setCommandsUIOff();
                    startReinstallProcess();
                }
                break;
                default: {
                    Log.d(CLASSTAG, String.format("%s: Error setting data in the UI", METHODTAG));
                }
                break;
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	UPDATE PROCESS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////
    private void startUpdateProcess() {
        final String METHODTAG = ".launchUpdateProcess";

        this.currentDeviceController.isUpdateRequested = true;

        try {
            // Here, this Activity is the current activity


            Boolean hasStoragePermissions = storagePermission.requestStoragePermission();

            if(hasStoragePermissions == null){
                this.currentDeviceController.isUpdateRequested = false;

                String title = "NEEDED PERMISSIONS";
                String message = " Unable to update, add Read and write permissions to the app.";
                setCommandsUIOn();

                DialogHandler storagePermissionsDialog = new DialogHandler();
                storagePermissionsDialog.setDialog(
                        YetiInformationActivity.this,
                        title,
                        message,
                        true
                );
                storagePermissionsDialog.show();



            }else if(hasStoragePermissions == true){
                this.currentDeviceController.startUpdateProcess(YetiInformationActivity.this);
                this.currentDeviceController.isUpdateRequested = false;
            }

            Log.d(CLASSTAG, "StoragePermission, Storage permission requested");

            /*}*/


        } catch (Exception e) {
            Log.e(CLASSTAG, METHODTAG, e);
        }

    }

    private void startReinstallProcess() {
        final String METHODTAG = ".launchUpdateProcess";

        ErrorObject errorObject = null;

        this.currentDeviceController.isUpdateRequested = true;

        try {
            // Here, this Activity is the current activity


            Boolean hasStoragePermissions = storagePermission.requestStoragePermission();

            if(hasStoragePermissions == null){
                this.currentDeviceController.isUpdateRequested = false;

                String title = "NEEDED PERMISSIONS";
                String message = " Unable to update, add Read and write permissions to the app.";
                setCommandsUIOn();

                DialogHandler storagePermissionsDialog = new DialogHandler();
                storagePermissionsDialog.setDialog(
                        YetiInformationActivity.this,
                        title,
                        message,
                        true
                );
                storagePermissionsDialog.show();



            }else if(hasStoragePermissions == true){
                this.currentDeviceController.startReinstallProcess(YetiInformationActivity.this);
                this.currentDeviceController.isUpdateRequested = false;
            }

            Log.d(CLASSTAG, "StoragePermission, Storage permission requested");

            /*}*/


        } catch (Exception e) {
            Log.e(CLASSTAG, METHODTAG, e);
        }
    }



    /////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	PERMISSIONS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /////////////////////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        String METHODTAG = "onRequestPermissionsResult";
        switch (requestCode) {
            case STORAGE_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    currentDeviceController.isUpdateRequested = true;
                    //currentDeviceController.startUpdateProcess(YetiInformationActivity.this);
                    //this.update_btn.setEnabled(true);
                    //this.reinstall_btn.setEnabled(true);

                } else {
                    currentDeviceController.isUpdateRequested = false;
                    Log.e(CLASSTAG, String.format("%sUnable to perform update/Reinstall, Read/Write to external Storage permissions are needed", METHODTAG));
                    setCommandsUIOn();

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }

    }


}


