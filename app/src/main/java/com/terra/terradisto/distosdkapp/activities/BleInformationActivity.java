package com.terra.terradisto.distosdkapp.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.terra.terradisto.distosdkapp.Models;
import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.BleDeviceController;
import com.terra.terradisto.distosdkapp.utilities.Logs;
import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;
import ch.leica.sdk.Devices.BleDevice;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.DeviceManager;
import ch.leica.sdk.ErrorHandling.DeviceException;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.Types;
import ch.leica.sdk.commands.response.Response;
import ch.leica.sdk.connection.ble.BleCharacteristic;


/**
 * UI to diplay bluetooth device information.
 * Excluding Yeti.
 */
public class BleInformationActivity
        extends BaseInformationActivity
        implements BleDeviceController.BleDataListener,
        BleDeviceController.RequestListener {

    /**
     * ClassName
     */
    private final String CLASSTAG = BleInformationActivity.class.getSimpleName();


    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ Textfields present information to user
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++
    /**
     * Current status of the connection to the currentDevice
     */
    private TextView status_txt;
    private TextView modelName_txt;

    /**
     * Device ID
     */
    private TextView deviceName_txt;


    private TextView distance_lbl;
    private TextView distance_txt;

    private TextView distanceUnit_lbl;
    private TextView distanceUnit_txt;

    private TextView inclination_lbl;
    private TextView inclination_txt;

    private TextView inclinationUnit_lbl;
    private TextView inclinationUnit_txt;

    private TextView direction_lbl;
    private TextView direction_txt;

    private TextView directionUnit_lbl;
    private TextView directionUnit_txt;


    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ BUTTONS implementing functionality of leica device
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++
    /**
     * Open the available commands panel
     */
    private Button sendCommand_btn; //

    /**
     * Sends "distance_txt"
     */
    private Button distance_btn; //Sends "measure distance_txt"

    /**
     * Sends "start tracking"
     */
    private Button startTracking_btn;

    /**
     * Sends "stop tracking"
     */
    private Button stopTracking_btn;

    /**
     * Read all available Bluetooth characteristics
     */
    private Button read_btn;

    /**
     * Clear textfields on smartphone and on leica device
     */
    private Button clear_btn;


    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ BROADCAST RECEIVER
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    /**
     * listen to changes to the Bluetooth adapter
     */
    BroadcastReceiver RECEIVER_bluetoothAdapter = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String METHODTAG = ".RECEIVER_bluetoothAdapter.receive()";
            Log.d(CLASSTAG, METHODTAG);
            currentDeviceController.checkForReconnection(BleInformationActivity.this);
        }
    };
    //private TimerTask statusCheckerTask;
    //private Timer statusCheckerTimer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String METHODTAG = ".onCreate";
        Log.i(CLASSTAG, String.format("%s - Activity created successfully. ", METHODTAG));
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();

    }

    @Override
    public void setDeviceController() {
        if(this.currentDeviceController == null) {
            this.currentDeviceController = new BleDeviceController(this);
        }
    }

    @Override
    public void setContentView() {
        setContentView(R.layout.activity_ble_information);
    }

    @Override
    void initMembers() {

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++ Initialize textfields
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        status_txt = (TextView) findViewById(R.id.status_txt);
        modelName_txt = (TextView) findViewById(R.id.modelName_txt);
        deviceName_txt = (TextView) findViewById(R.id.deviceName_txt);

        distance_lbl = (TextView) findViewById(R.id.distance_lbl);
        distance_txt = (TextView) findViewById(R.id.distance_txt);


        distanceUnit_lbl = (TextView) findViewById(R.id.distance_unit_lbl);
        distanceUnit_txt = (TextView) findViewById(R.id.distance_unit_txt);


        inclination_lbl = (TextView) findViewById(R.id.inclination_lbl);
        inclination_txt = (TextView) findViewById(R.id.inclination_txt);
        inclinationUnit_lbl = (TextView) findViewById(R.id.inclinationUnit_lbl);
        inclinationUnit_txt = (TextView) findViewById(R.id.inclinationUnit_txt);
        direction_lbl = (TextView) findViewById(R.id.direction_lbl);
        direction_txt = (TextView) findViewById(R.id.direction_txt);
        directionUnit_lbl = (TextView) findViewById(R.id.directionUnit_lbl);
        directionUnit_txt = (TextView) findViewById(R.id.directionUnit_txt);

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++ Set UI Listeners
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++//
        View.OnClickListener bl = new ButtonListener();

        distance_btn = (Button) findViewById(R.id.distance_btn);
        sendCommand_btn = (Button) findViewById(R.id.sendCommand_btn);
        startTracking_btn = (Button) findViewById(R.id.startTracking_btn);
        stopTracking_btn = (Button) findViewById(R.id.stopTracking_btn);
        read_btn = (Button) findViewById(R.id.read_btn);
        clear_btn = (Button) findViewById(R.id.clear_btn);


        setOnClickListener(distance_btn, bl);
        setOnClickListener(sendCommand_btn, bl);
        setOnClickListener(startTracking_btn, bl);
        setOnClickListener(stopTracking_btn, bl);
        setOnClickListener(read_btn, bl);
        setOnClickListener(clear_btn, bl);


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
        setReceivers(BluetoothAdapter.ACTION_STATE_CHANGED, RECEIVER_bluetoothAdapter);


        //get Available characteristics in the device,
        // does not require sending command to device
        getAvailableCharacteristics();

        currentDeviceController.checkForReconnection(BleInformationActivity.this);

        resumeActivity();

        Log.d(CLASSTAG, String.format("%s Activity onResume successful.", METHODTAG));

    }

    public void resumeActivity() {
        final String METHODTAG = "resumeActivity";

        final CountDownLatch latch = new CountDownLatch(1);
        // setup according to device
        //Set Status
        setTextView(status_txt, currentDeviceController.status);

        //Set DeviceName
        setTextView(deviceName_txt, currentDeviceController.getDeviceName());

        setTextView(modelName_txt, currentDeviceController.getModel());


        new Thread(new Runnable() {
            @Override
            public void run() {
                // start bt connection
                currentDeviceController.startBTConnection(new BleDevice.BTConnectionCallback() {
                    @Override
                    public void onFinished() {
                        Log.i(METHODTAG, "NOW YOU CAN SEND COMMANDS TO THE DEVICE");
                        // Ask for model number and serial number, this will result async
                        String model = currentDeviceController.getModel();
                        if (model.isEmpty() == false) {
                            setActivityUI(model);
                        }

                        setCommandsUIOn();

                        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
                        data.isSearchingEnabled = false;

                        latch.countDown();
                    }
                });

                try {
                    latch.await(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();


    }

    /**
     * Show the available characteristics on the bluetooth device
     * This are obtained on connection, before connecting with the device
     */
    public void getAvailableCharacteristics() {

        final String METHODTAG = ".getAvailableCharacteristics";

        List<BleCharacteristic> characteristics = currentDeviceController.getAvailableCharacteristics();

        for (BleCharacteristic bGC : characteristics) {
            Log.d(CLASSTAG, METHODTAG + " ALL Characteristics UI:strValue:" + bGC.toString());
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        this.onStopDetailed();

        //Start Timer

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        data.isSearchingEnabled = true;


    }

    protected void onStopDetailed(){
        final String METHODTAG = ".onStop";

        final CountDownLatch latch = new CountDownLatch(1);

        if (currentDeviceController == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return;
        }

        this.currentDeviceController.reconnectionIsRunning = false;

        //Unregister activity for bluetooth adapter changes
        unregisterReceivers(RECEIVER_bluetoothAdapter);

        // Pause the bluetooth connection

        Log.d(CLASSTAG, METHODTAG+ ".pauseBtConnectionCalled");


        ErrorObject error = currentDeviceController.pauseBTConnection(new BleDevice.BTConnectionCallback() {
            @Override
            public void onFinished() {
                Log.i("onStop", "NOW Notifications are deactivated in the device");


                setActivityUI(currentDeviceController.getModel());
                setCommandsUIOff();
                latch.countDown();
            }
        });


        try {
            latch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (error != null) {
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	INTERFACE IMPLEMENTATION - DEVICE STATUS LISTENER
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if the device is disconnected, if it is disconnected launch the reconnection functiono
     *
     * @param deviceID the deviceID which the connection state changed
     * @param state    the current connection state. If state is disconnected,
     *                 the device object is not valid anymore.
     *                 No connection can be established with this object any more.
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
            // if disconnected, try reconnecting

            showConnectedDisconnectedDialog(false);
            if(isAdapterEnabled() == true){

                currentDeviceController.checkForReconnection(BleInformationActivity.this);

            }

            setCommandsUIOff();

        }

    }

    /**
     * Get object error and show it in the UI
     * Elements of errorObject send by the Sdk
     *
     * @param errorCode    code
     * @param errorMessage message
     */
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
        currentDeviceController.setReconnectionHelper(BleInformationActivity.this);
    }

    @Override
    public void onStatusChange(String string) {
        if(status_txt == null){
            status_txt = (TextView) findViewById(R.id.status_txt);
        }
        setTextView(status_txt, string);
        if ("Reconnecting".equals(string)) {
            showAlert("Try to automatically reconnect now.");
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	DATA EXTRACTION - INTERFACE IMPLEMENTATION - BleDataListener -
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onDistance_Received(String value, String unit) {

        setTextView(distance_txt, value);
        setTextView(distanceUnit_txt, unit);
    }

    @Override
    public void onInclination_Received(String value, String unit) {
        setTextView(inclination_txt, value);
        setTextView(inclinationUnit_txt, unit);
    }

    @Override
    public void onDirection_Received(String value, String unit) {
        setTextView(direction_txt, value);
        setTextView(directionUnit_txt, unit);

    }

    @Override
    public void onSerialNumber_Received(String data) {

        setTextView(deviceName_txt, String.format("%s %s", deviceName_txt.getText(), data));
    }

    @Override
    public void onFirmwareRevision_Received(String data) {
    }

    @Override
    public void onHardwareRevision_Received(String data) {
    }

    @Override
    public void onManufacturerNameString_Received(String data) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    // Commands UI
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void setCommandsUIOff() {

        float alpha = .5f;
        boolean clickable = false;

        setUIButtonState(sendCommand_btn, alpha, clickable);
        setUIButtonState(distance_btn, alpha, clickable);
        setUIButtonState(startTracking_btn, alpha, clickable);
        setUIButtonState(stopTracking_btn, alpha, clickable);
        setUIButtonState(read_btn, alpha, clickable);
        setUIButtonState(clear_btn, alpha, clickable);

    }

    @Override
    public void setCommandsUIOn() {

        float alpha = 1f;
        boolean clickable = true;

        setUIButtonState(sendCommand_btn, alpha, clickable);
        setUIButtonState(distance_btn, alpha, clickable);
        setUIButtonState(startTracking_btn, alpha, clickable);
        setUIButtonState(stopTracking_btn, alpha, clickable);
        setUIButtonState(read_btn, alpha, clickable);
        setUIButtonState(clear_btn, alpha, clickable);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    // INTERFACE IMPLEMENTATION - RequestListener -
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onRequestBluetoothTurnOn() {
        this.requestBLEAdapterTurnOn();
    }

    /**
     * Show bluetooth turnOn dialog
     */
    synchronized void requestBLEAdapterTurnOn() {
        final String METHODTAG = ".showBluetoothTurnOn";

        if (isAdapterEnabled() == true) {
            Log.d(CLASSTAG, String.format("%s: ble is already turned on", METHODTAG));
            return;
        }

        currentDeviceController.turnOnAdapterDialogIsShown = true;
        Log.d(CLASSTAG, String.format("%s: turnOnAdapterDialogIsShown is true", METHODTAG));

        String title = "Request turn-on Bluetooth";
        String message = "Bluetooth has to be turned on.";
        String positiveButtonText = "Turn it on";
        String negativeButtonText = "Cancel";

        Runnable positiveButtonRunnable = new Runnable() {
            @Override
            public void run() {
                currentDeviceController.turnOnAdapterDialogIsShown = false;
                DeviceManager.getInstance(getApplicationContext()).enableBLE();
                //currentDeviceController.checkForReconnection(BleInformationActivity.this);
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
                BleInformationActivity.this,
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
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                return false;
            }
        }
        return true;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    // BLE - ADDITIONAL METHODS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////
    void readAllBleCharacteristics() {
        final String METHODTAG = ".readAllBleCharacteristics";
        try {

            currentDeviceController.getCurrentDevice().readAllBleCharacteristics(new BleDevice.BTConnectionCallback() {
                @Override
                public void onFinished() {
                    Log.d(
                            CLASSTAG,
                            String.format(
                                    "%sCharacteristics were read_btn successfully, " +
                                            "ready to perform another task",
                                    METHODTAG));
                }
            });
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s: Error Reading Characteristics", METHODTAG), e);
            showAlert("Error ReadingCharacteristics. " + e.getMessage());
        }
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
    protected void setActivityUI(final String deviceModel) {

        final String METHODTAG = ".setActivityUI";

        Logs.logAvailableCommands(CLASSTAG, METHODTAG, this.getAvailableCommandsString());
        Log.d(CLASSTAG, String.format("%s deviceModel: %s", METHODTAG, deviceModel));

        if (currentDeviceController == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return;
        }


        if (deviceModel != null) {

            setTextView(modelName_txt, deviceModel);

            Models models = new Models();
            List<String> modelsD110 = models.getLeicaBleModels().get(0);
            List<String> modelsS910 = models.getLeicaBleModels().get(1);
            List<String> modelsD510 = models.getLeicaBleModels().get(2);


            //Show only the available elements for the models D110, D1, D2
            //Put the corresponding proprietary model here
            if ( modelsD110.contains(deviceModel) ) {

                //FIELDS
                setElementVisibility(distance_lbl, true);
                setElementVisibility(distance_txt, true);
                setElementVisibility(distanceUnit_lbl, true);
                setElementVisibility(distanceUnit_txt, true);
                setElementVisibility(inclination_lbl, false);
                setElementVisibility(inclination_txt, false);
                setElementVisibility(inclinationUnit_lbl, false);
                setElementVisibility(inclinationUnit_txt, false);
                setElementVisibility(direction_lbl, false);
                setElementVisibility(direction_lbl, false);
                setElementVisibility(directionUnit_lbl, false);
                setElementVisibility(directionUnit_txt, false);

                //Buttons
                setElementVisibility(read_btn, true);
                setElementVisibility(clear_btn, true);
                setElementVisibility(sendCommand_btn, true);
                setElementVisibility(distance_btn, true);
                setElementVisibility(startTracking_btn, true);
                setElementVisibility(stopTracking_btn, true);

            }
            //Show only the available elements for the models S910 and D810

            else if (  modelsS910.contains(deviceModel) ) {

                setElementVisibility(distance_lbl, true);
                setElementVisibility(distance_txt, true);
                setElementVisibility(distanceUnit_lbl, true);
                setElementVisibility(distanceUnit_txt, true);
                setElementVisibility(inclination_lbl, true);
                setElementVisibility(inclination_txt, true);
                setElementVisibility(inclinationUnit_lbl, true);
                setElementVisibility(inclinationUnit_txt, true);
                setElementVisibility(direction_lbl, true);
                setElementVisibility(direction_lbl, true);
                setElementVisibility(directionUnit_lbl, true);
                setElementVisibility(directionUnit_txt, true);

                //Buttons
                setElementVisibility(read_btn, true);
                setElementVisibility(clear_btn, true);
                setElementVisibility(sendCommand_btn, true);
                setElementVisibility(distance_btn, true);
                setElementVisibility(startTracking_btn, true);
                setElementVisibility(stopTracking_btn, true);
            }
            //Show only the available elements for the models D510
            else if (  modelsD510.contains(deviceModel)  ) {

                setElementVisibility(distance_lbl, true);
                setElementVisibility(distance_txt, true);
                setElementVisibility(distanceUnit_lbl, true);
                setElementVisibility(distanceUnit_txt, true);
                setElementVisibility(inclination_lbl, true);
                setElementVisibility(inclination_txt, true);
                setElementVisibility(inclinationUnit_lbl, true);
                setElementVisibility(inclinationUnit_txt, true);
                setElementVisibility(direction_lbl, false);
                setElementVisibility(direction_lbl, false);
                setElementVisibility(directionUnit_lbl, false);
                setElementVisibility(directionUnit_txt, false);

                //Buttons
                setElementVisibility(read_btn, true);
                setElementVisibility(clear_btn, true);
                setElementVisibility(sendCommand_btn, false);
                setElementVisibility(distance_btn, false);
                setElementVisibility(startTracking_btn, false);
                setElementVisibility(stopTracking_btn, false);

            }
        } else {
            Log.d(CLASSTAG, METHODTAG + ": parameter deviceModel is null");
        }
    }


    /**
     * set all textviews to zeros
     */
    @Override
    void clearUI() {

        String defaultValue = getResources().getString(R.string.default_value);
        setTextView(distance_txt, defaultValue);
        setTextView(distanceUnit_txt, defaultValue);
        setTextView(inclination_txt, defaultValue);
        setTextView(inclinationUnit_txt, defaultValue);
        setTextView(direction_txt, defaultValue);
        setTextView(directionUnit_txt, defaultValue);

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
        public void onClick(final View v) {

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
                                    ((BleDeviceController) currentDeviceController).sendDistanceCommand();
                            if (error != null) {
                                showAlert(formatErrorMessage(error));
                            }
                        }
                    };
                    new Thread(sendDistanceCommandRunnable).start();
                }
                break;
                case R.id.clear_btn: {

                    String deviceModel = currentDeviceController.getModel();

                    Models models = new Models();
                    List<String> modelsD510 = models.getLeicaBleModels().get(2);

                    if(modelsD510.contains(deviceModel)) {
                        clearUI();


                    }else {
                        Runnable sendClearCommandRunnable = new Runnable() {
                            @Override
                            public void run() {
                                ErrorObject error =
                                        ((BleDeviceController) currentDeviceController).sendClearCommand();
                            }
                        };
                        new Thread(sendClearCommandRunnable).start();
                    }
                }
                break;
                case R.id.startTracking_btn: {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Response response =
                                    currentDeviceController.
                                            sendCommand(Types.Commands.StartTracking);
                            ErrorObject error = response.getError();
                            if (error == null) {
                                currentDeviceController.deviceIsInTrackingMode = true;
                            }
                        }
                    }).start();
                }
                break;
                case R.id.stopTracking_btn: {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Response response =
                                    currentDeviceController.sendCommand(Types.Commands.StopTracking);
                            ErrorObject error = response.getError();
                            if (error == null) {
                                currentDeviceController.deviceIsInTrackingMode = false;
                            }
                        }
                    }).start();
                }
                break;
                case R.id.read_btn: {
                    readAllBleCharacteristics();
                }
                break;
                default: {
                    Log.e(CLASSTAG, METHODTAG + ": Error in ButtonListener.onClick");
                }
                break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        savedInstanceState.putBoolean("isSearchingEnabled", data.isSearchingEnabled);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        data.isSearchingEnabled = savedInstanceState.getBoolean("isSearchingEnabled");

    }
}