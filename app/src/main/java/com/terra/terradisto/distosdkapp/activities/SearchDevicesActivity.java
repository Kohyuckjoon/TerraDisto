package com.terra.terradisto.distosdkapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.BuildConfig;
import com.android.volley.VolleyError;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.material.snackbar.Snackbar;
import com.terra.terradisto.distosdkapp.AppLicenses;
import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.AvailableDevicesListener;
import com.terra.terradisto.distosdkapp.device.FindDevices;
import com.terra.terradisto.distosdkapp.permissions.PermissionsHelper;
import com.terra.terradisto.distosdkapp.utilities.ConnectionTypeAdapter;
import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;

import ch.leica.sdk.ErrorHandling.ErrorDefinitions;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.LeicaSdk;
import ch.leica.sdk.Listeners.ErrorListener;
import ch.leica.sdk.Types;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.DeviceManager;
import ch.leica.sdk.Utilities.WifiHelper;
import ch.leica.sdk.logging.Logs;


// just to check if volley is resolved appside


/**
 * This is the main activity which handles finding available devices and holds the list of available devices.
 */
public class SearchDevicesActivity
        extends AppCompatActivity
        implements Device.ConnectionListener,
        ErrorListener,
        AvailableDevicesListener {


    // just to check if volley is resolved appside
    VolleyError error = new VolleyError();

    // to handle info dialog at app start and only at app start
    // has to be static, otherwise the alert will be displayed more than one time
    static boolean searchInfoShown = false;

    /**
     * ClassName
     */
    private final String CLASSTAG = SearchDevicesActivity.class.getSimpleName();

	/*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ UI - Elements
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * UI holding the available devices
     */
    ListView deviceList;
    ConnectionTypeAdapter connectionTypeAdapter;

    private TextView version;

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Dialogs
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    //ui-alertsDialog to present errorList to users
    DialogHandler connectingDialog = null;
    DialogHandler alertsDialog = null;


	/*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Timers
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    // needed for connection timeout
    Timer connectionTimeoutTimer;
    TimerTask connectionTimeoutTask;


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Device
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // for finding and connecting to a device
    DeviceManager deviceManager;

    /**
     * Current selected device
     */
    Device currentDevice;


    // to handle user cancel connection attempt
    Map<Device, Boolean> connectionAttempts = new HashMap<>();
    Device currentConnectionAttemptToDevice = null;


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Permissions
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private PermissionsHelper permissionsHelper = new PermissionsHelper(this);

    FindDevices findDevices;

    TextView title;


    private Button save_logs_btn;


    /**
     * Inits device list
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String METHODTAG = ".onCreate";

        initMembers();

        //Initialize SDK
        this.initSDK();

        this.deviceManager = DeviceManager.getInstance(this.getApplicationContext());

        findDevices = new FindDevices(this.getApplicationContext(), this);
        findDevices.registerReceivers();


        //configure members
        this.deviceList.setOnItemClickListener(new OnItemClickListener());
        this.deviceList.setAdapter(connectionTypeAdapter);
        updateList();


        //If there is already an existing device object, take it from the clipboard
        InformationActivityData informationActivityData = Clipboard.INSTANCE.getInformationActivityData();
        if (informationActivityData != null) {
            Device device = informationActivityData.device;
            if (device != null) {
                currentDevice = device;
                currentDevice.setConnectionListener(this);
                currentDevice.setErrorListener(this);
            }
        }

        //Connection Timeout timer - 90s
        connectionTimeoutTimer = new Timer();

        String versionValue = LeicaSdk.getVersion();//.substring(0, LeicaSdk.getVersion().lastIndexOf("."));
        //Get the SDK version
        version.setText(String.format("Version: %s", versionValue));


        Log.i(CLASSTAG, String.format("%s: onCreate Finished.", METHODTAG));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!BuildConfig.FLAVOR.equals("leicaDeliver")) {
                    title.setText(title.getText() + " " + BuildConfig.FLAVOR);
                }
            }
        });

    }


    /**
     * read data from the Commands file and load it in the commands class
     * <p>
     * Initialize the LeicaSDK object
     * <p>
     * sets up DeviceManager
     */
    void initSDK() {

        final String METHODTAG = ".initSDK";

        if (LeicaSdk.isInit == false) {

            Log.d(
                    CLASSTAG,
                    String.format("%s: initialization begin.", METHODTAG)
            );

            // this "commands.json" file can be named differently. it only has to exist in the assets folder
            LeicaSdk.InitObject initObject = new LeicaSdk.InitObject("commands.json");

            try {
                LeicaSdk.init(getApplicationContext(), initObject);
                LeicaSdk.setLogLevel(Log.VERBOSE);
                LeicaSdk.setMethodCalledLog(false);

                //boolean distoWifi, boolean distoBle, boolean yeti, boolean disto3DD
                LeicaSdk.setScanConfig(true, true, true, true);

                // set licenses
                AppLicenses appLicenses = new AppLicenses();

                LeicaSdk.setLicenses(appLicenses.keys);
                LeicaSdk.scanConfig.setWifiAdapterOn(false);
                LeicaSdk.scanConfig.setBleAdapterOn(false);

            } catch (JSONException e) {

                Log.e(
                        CLASSTAG,
                        String.format("%s: Error in the structure of the JSON File, closing the application", METHODTAG),
                        e
                );
                finish();

            } catch (IllegalArgumentCheckedException e) {

                Log.e(CLASSTAG, METHODTAG + ": Error in the data of the JSON File, closing the application", e);
                finish();

            } catch (IOException e) {

                Log.e(CLASSTAG, METHODTAG + ": Error reading JSON File, closing the application", e);
                finish();
            }
        }

        deviceManager = DeviceManager.getInstance(this);

        deviceManager.setErrorListener(this);

        Log.i(CLASSTAG, String.format("%s: SDK initialization Finished successfully", METHODTAG));

    }

    void initMembers() {

        setContentView(R.layout.activity_search_devices);

        title = (TextView) findViewById(R.id.title);

        //InitializeFields
        deviceList = (ListView) findViewById(R.id.devices_listView);
        version = (TextView) findViewById(R.id.sdkVersion);

        //Initialize Dialogs
        connectingDialog = new DialogHandler();
        alertsDialog = new DialogHandler();

        //Shows the icon (connection Type) next to each of the available devices
        this.connectionTypeAdapter = new ConnectionTypeAdapter(getApplicationContext(), new ArrayList<Device>());

        save_logs_btn = (Button) findViewById(R.id.save_logs_btn);

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++ Set UI Listeners
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        View.OnClickListener buttonListener = new ButtonListener();

        setOnClickListener(save_logs_btn, buttonListener);
    }


    @Override
    protected void onResume() {
        super.onResume();

        final String METHODTAG = ".onResume";

        //Update the Devices List UI
        updateList();

        // only show info dialog once
        if (searchInfoShown == false) {
            searchInfoShown = true;

            alertsDialog.
                    setAlert(
                            SearchDevicesActivity.this,
                            "장치를 찾기위해 블루투스를 켜주세요"

                    );
            alertsDialog.show();
        }

        permissionsHelper.requestStoragePermission();


        // show only connected devices
        findDevices.requestConnectedDevices();
        // immediately start finding devices when activity resumes
        this.findAvailableDevices();


        Log.i(CLASSTAG, METHODTAG + ": Activity onResume Completed");

    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        final String METHODTAG = ".onResume";
//
//        // 리스트 UI 업데이트
//        updateList();
//
//        if (!searchInfoShown) {
//            showBluetoothGuide();
//            searchInfoShown = true;
//        }
//
//        // 권한 및 장치 검색 로직 최적화
//        checkAndStartDiscovery();
//
//        Log.i(CLASSTAG, METHODTAG + ": Activity onResume Completed");
//    }
//
//    // 사용자에게 블루투스 활성화를 유도하는 안내창
//    private void showBluetoothGuide() {
//        View contextView = findViewById(android.R.id.content);
//        Snackbar snackbar = Snackbar.make(contextView, "'장치를 찾기 위해 블루투스를 켜주세요.", Snackbar.LENGTH_LONG);
//
//        snackbar.setAction("설정", new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
//                startActivity(intent);
//            }
//        });
//
//        snackbar.setBackgroundTint(Color.parseColor("#2C2C2E"));
//        snackbar.setActionTextColor(Color.parseColor("#3182F6"));
//        snackbar.show();
//    }
//
//    // 권한 확인 및 장치 검색 시작 로직 통합
//    private void checkAndStartDiscovery() {
//        // 저장소 및 위치 권한 요청
//        permissionsHelper.requestStoragePermission();
//
//        // 연결된 장치 확인 및 주변 장치 검색 시작
//        if (findDevices != null) {
//            findDevices.requestConnectedDevices();
//            this.findAvailableDevices();
//        }
//    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        data.device = currentDevice;

        final String METHODTAG = ".onDestroy";
        Log.d(METHODTAG, "activity onDestroy method reached.");

        findDevices.unregisterReceivers();
        findDevices.onDestroy();

        findDevices.stopFindingDevices();

        //dismiss all the dialogs that may be activated
        if (connectingDialog != null) {
            connectingDialog.dismiss();
        }

        Log.i(CLASSTAG, METHODTAG + ": Activity destroyed");

    }

    /**
     * when closing main activity, disconnect from all devices
     */
    @Override
    public void onBackPressed() {

        super.onBackPressed();
        final String METHODTAG = ".onBackPressed";


        final Thread disconnectThread;
        disconnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (final Device connectedDevice : deviceManager.getConnectedDevices()) {
                    connectedDevice.disconnect();

                    Log.i(
                            CLASSTAG,
                            String.format(
                                    "%sDisconnected Device model: %s deviceId: %s",
                                    METHODTAG,
                                    connectedDevice.modelName,
                                    connectedDevice.getDeviceID())
                    );
                }
                finish();
            }
        });


        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++//
        //++
        //++ connectingDialog: Disconnecting
        //++
        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++//
        String title = "Disconnecting";
        String message = "You are about to close the app. That results in disconnecting from all devices. Do you want to close the app?";
        String positiveButtonText = "Yes";
        Runnable positiveButtonRunnable =
                new Runnable() {
                    @Override
                    public void run() {

                        final String message = "Disconnecting devices ";

                        alertsDialog.setAlert(SearchDevicesActivity.this, message);
                        alertsDialog.show();

                        if (deviceManager != null) {
                            // Disconnect from all the connected devices
                            Log.i(
                                    CLASSTAG,
                                    String.format(
                                            "%sTo disconnect Devices count: %d",
                                            METHODTAG,
                                            deviceManager.getConnectedDevices().size()
                                    )
                            );

                            disconnectThread.start();
                        }
                    }
                };
        String negativeButtonText = "No";
        Runnable negativeButtonRunnable =
                new Runnable() {
                    @Override
                    public void run() {

                    }
                };


        connectingDialog.setDialog(
                SearchDevicesActivity.this,
                title, message,
                false,
                positiveButtonText,
                positiveButtonRunnable,
                negativeButtonText,
                negativeButtonRunnable
        );
        connectingDialog.show();

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    }


    /**
     * Called when the connection state of a device changed
     *
     * @param device currently connected device
     * @param state  current device connection state
     */
    @Override
    public void onConnectionStateChanged(Device device, Device.ConnectionState state) {

        final String METHODTAG = ".onConnectionStateChanged";

        Log.i(
                CLASSTAG,
                String.format(
                        "%s: onConnectionStateChanged: %s, state: %s",
                        METHODTAG,
                        device.getDeviceID(),
                        state
                )
        );

        switch (state) {
            case connected:

                // update UI
                connectingDialog.dismiss();

                // if connection attempt was canceled
                Boolean canceled = connectionAttempts.get(device);
                if (canceled != null) {

                    // connection attempt was canceled
                    if (canceled == Boolean.TRUE) {
                        // disconnect device
                        device.disconnect();
                        // clean map
                        connectionAttempts.remove(device);
                        // update UI
                        updateList();
                        return;
                    }
                }

                //Go to the device information Screen
                goToInfoScreen(device);

                break;
            case disconnected:
                stopConnectionTimeOutTimer();
                break;
        }
    }

    /**
     * Defines the default behavior when an error is notified.
     * Presents alert to user showing a error message
     *
     * @param errorObject error object comes from different sources SDK or APP.
     */
    @Override
    public void onError(final ErrorObject errorObject, final Device device) {
        final String METHODTAG = ".requestErrorMessageDialog";
        Log.i(CLASSTAG, METHODTAG + ": " + errorObject.getErrorMessage() + ", errorCode: " + errorObject.getErrorCode());

        String message = "";

        if (connectingDialog != null) {
            connectingDialog.dismiss();
        }

        /*
         * first check for gatt 133
         * gatt error 133 handling
         * */
        if (errorObject.getErrorCode() == ErrorDefinitions.BLUETOOTH_DEVICE_133_ERROR_CODE
                || errorObject.getErrorCode() == ErrorDefinitions.BLUETOOTH_DEVICE_62_ERROR_CODE) {


            if (device != null &&
                    currentConnectionAttemptToDevice != null &&
                    device.getDeviceID().equalsIgnoreCase(currentConnectionAttemptToDevice.getDeviceID())) {

                if (connectingDialog != null) {

                    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                    //++ ConnectingDialog: Device  Not found
                    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
                    String title = "Device not found";
                    message = "The Device can not be found, please verify the device is turned ON and in range";
                    connectingDialog.setDialog(SearchDevicesActivity.this, title, message, true);

                }
                connectingDialog.show();
                return;
            }

            stopConnectionAttempt();

            showError(errorObject, message);

            return;
        }

        if (errorObject.getErrorCode() == ErrorDefinitions.HOTSPOT_DEVICE_IP_NOT_REACHABLE_CODE) {

            showError(errorObject, message);
            return;
        }
        if (errorObject.getErrorCode() == ErrorDefinitions.AP_DEVICE_IP_NOT_REACHABLE_CODE) {

            showError(errorObject, message);
            return;
        }
        if (errorObject.getErrorCode() == ErrorDefinitions.BLUETOOTH_DEVICE_UNABLE_TO_PAIR_CODE) {


            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //++ Show Error
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//

            message =
                    String.format(
                            Locale.getDefault(),
                            "%s \n %s",
                            "Please Reset Device and remove pairing Settings manually in Android settings.",
                            "and try again."
                    );
            showError(errorObject, message);

            stopConnectionTimeOutTimer();
            return;
        }

        showError(errorObject, message);
    }

    /////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	PERMISSIONS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//

    /// //////////////////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    /////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	FIND AVAILABLE DEVICES
    //++++++++++++++++++++++++++++++++++++++++++++++++++++//

    /// //////////////////////////////////////////////////////

    public void findAvailableDevices() {

        updateList();


        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();

        if (data.isSearchingEnabled == true) {
            // Verify and enable Wifi and Bluetooth, according to what the user allowed
            permissionsHelper.requestNetworkPermissions();
            deviceManager.setErrorListener(this);
            findDevices.findAvailableDevices(this);
        } else {
            Log.i(CLASSTAG, "findAvailableDevices. This check was created to avoid certain devices i.e.Huawei to call findAvailableDevices due to recreation of the activity ");
        }
    }

    /////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	CONNECTION
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /////////////////////////////////////////////////////////
    /**
     * Connects to device
     *
     * @param device device to connect to
     */
    void connectToDevice(final Device device) {
        final String METHODTAG = ".connectToDevice";

        Log.d(CLASSTAG, METHODTAG + ": App is attempting to connect to device.");


        // remember to which device connection is attempting, for eventually cancelling the connection attempt
        currentConnectionAttemptToDevice = device;
        connectionAttempts.put(device, Boolean.FALSE);

        device.setConnectionListener(this);
        device.setErrorListener(this);
        deviceManager.stopFindingDevices();

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        data.isSearchingEnabled = false;
        device.connect();

    }

    /**
     * stop connection attempt
     */
    synchronized void stopConnectionAttempt() {

        final String METHODTAG = ".stopConnectionAttempt";
        Log.d(CLASSTAG, METHODTAG + ": Called");

        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        data.isSearchingEnabled = true;

        // remember for which device connection attempt is canceled
        if (currentConnectionAttemptToDevice != null) {
            connectionAttempts.put(currentConnectionAttemptToDevice, Boolean.TRUE);
        }

        stopConnectionTimeOutTimer();

        if (connectingDialog != null) {
            connectingDialog.dismiss();
        }

        if (currentDevice != null) {
            currentDevice.disconnect();
        }
    }

    /**
     * Start a timer to stop connecting attempt and show a timeout dialog (only for BTLE devices)
     */
    void startConnectionTimeOutTimer() {
        final String METHODTAG = ".startConnectionTimeOutTimer";

        if (Types.ConnectionType.ble.equals(currentDevice.getConnectionType())) {

            Log.i(
                    CLASSTAG,
                    String.format(
                            "%s: Connection timeout timer started at: %d", METHODTAG, System.currentTimeMillis())
            );

            final long connectionTimeout = 90 * 1000; // 90s - multiple connection attempts

            //Start Timer
            this.connectionTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    // stop connecting attempt
                    stopConnectionAttempt();

                    // show timeout dialog
                    showConnectionTimedOutDialog();
                    if (currentDevice != null) {
                        // show only connected devices
                        findDevices.requestConnectedDevices();
                        updateList();

                        currentDevice.disconnect();

                    }
                    findAvailableDevices();

                }
            };
            this.connectionTimeoutTimer.schedule(connectionTimeoutTask, connectionTimeout);
        }
    }

    /**
     * stop connection timeout timer,  (only for BTLE devices)
     */
    void stopConnectionTimeOutTimer() {

        final String METHODTAG = ".stopConnectionTimeOutTimer";


        if (connectionTimeoutTask == null) {
            return;
        }
        this.connectionTimeoutTask.cancel();
        this.connectionTimeoutTimer.purge();
        Log.i(
                CLASSTAG,
                METHODTAG + ": Connection timeout stopped at: " + System.currentTimeMillis()
        );

    }

    @Override
    public void onAvailableDeviceFound() {

        updateList();
    }

    @Override
    public void onAvailableDevicesChanged(List<Device> availableDevices) {
        updateList();
    }


    /////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	PRIVATE CLASSES
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /////////////////////////////////////////////////////////
    /**
     * Defines what happen on a click on an item in the list
     * If device is already connected directly jump to the coresponding activity
     * - BLE devices - BleInformationActivity
     * - BLE YETI devices - YetiInformationActivity
     * - Wifi devices - WifiInformationActivity
     * <p>
     * For Hotspot devices check if smartphone is connected to the correct hotspot
     * Otherwise connect to the device
     */
    private class OnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String METHODTAG = ".deviceList OnItemClick";

            Log.d(METHODTAG, "OnItemClickListener");

            //Call this to avoid interference in Bluetooth operations
            findDevices.stopFindingDevices();


            // get device
            Device device = findDevices.getAvailableDevices().get(position);

            if (device == null) {
                Log.i(METHODTAG, "device not found");
                return;
            }

            currentDevice = device;

            // already connected
            if (device.getConnectionState() == Device.ConnectionState.connected) {

                goToInfoScreen(device);
                return;
            }

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++//
            //++ 		connectingDialog: Connecting
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++//

            String title = "Connecting";
            String message = "Connecting... This may take up to 30 seconds... ";
            String negativeButtonText = "Cancel";
            Runnable negativeButtonRunnable =
                    new Runnable() {
                        @Override
                        public void run() {

                            // cancel connection attempt
                            stopConnectionAttempt();
                            findAvailableDevices();

                        }
                    };

            connectingDialog.setDialog(
                    SearchDevicesActivity.this,
                    title, message,
                    false,
                    null,
                    null,
                    negativeButtonText,
                    negativeButtonRunnable
            );

            connectingDialog.show();
            /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

            // if hotspot go to wifi settings first if the wifi is incorrect
            if (currentDevice.getConnectionType().equals(Types.ConnectionType.wifiHotspot)) {

                String wifiName = WifiHelper.getWifiName(getApplicationContext());

                //If the wifiName is null or different from the currentDeviceController name.
                //Launch the wifi connection settings
                if (wifiName == null) {
                    gotoWifiPanel();
                    return;

                } else if (wifiName.equalsIgnoreCase(currentDevice.getDeviceName()) == false) {
                    gotoWifiPanel();
                    return;

                } else {
                    // wifi is correct, connect!
                    connectToDevice(currentDevice);
                    return;
                }
            }

            //Start Timer here, if bt device. for wifi we do not need a timer, there we have a socket timeout
            startConnectionTimeOutTimer();


            // connect the device
            connectToDevice(currentDevice);
        }
    }

    /////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	LAUNCH ACTIVITIES
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /////////////////////////////////////////////////////////

    /**
     * Switch activity
     * BTLE devices go to BLEInformationActivty
     * BTLE Yeti go to YetiInformationActivty
     * Wifi devices go to WifiInformationActivity
     */
    void goToInfoScreen(Device device) {

        final String METHODTAG = ".goToInfoScreen";

        Log.d(CLASSTAG, METHODTAG + ": Go to the corresponding Info Screen for the selected device.");

        Class nextActivity = null;

        Clipboard singleton = Clipboard.INSTANCE;
        singleton.setInformationActivityData(
                new InformationActivityData(
                        device,
                        null,
                        deviceManager
                )
        );

        // if bluetooth
        if (device.getConnectionType() == Types.ConnectionType.ble) {
            // Stop Timer here
            stopConnectionTimeOutTimer();


            // if device is Yeti
            if (device.getDeviceType().equals(Types.DeviceType.Yeti)) {

                //YetiInformationActivity.setCurrentDevice(device, getApplicationContext());
                nextActivity = YetiInformationActivity.class;

            } else {

                //Launch the BleInformationActivity
                nextActivity = BleInformationActivity.class;

            }

        } else if (device.getConnectionType() == Types.ConnectionType.wifiHotspot
                || device.getConnectionType() == Types.ConnectionType.wifiAP) {

            if (device.getDeviceType().equals(Types.DeviceType.Disto3D)) {
                //Launch the Wifi3DInformationActivity
                nextActivity = Wifi3DInformationActivity.class;
            } else {
                //Launch the WifiInformationActivity
                nextActivity = WifiInformationActivity.class;
            }


        } else if (device.getConnectionType() == Types.ConnectionType.rndis) {

            //Launch the WifiInformationActivity
            nextActivity = RndisInformationActivity.class;

        } else {
            Log.e(CLASSTAG, METHODTAG + ": unknown connection type. this should never happen.");
        }

        Intent informationIntent = new Intent(SearchDevicesActivity.this, nextActivity);
        informationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(informationIntent);

        // forget current device
        this.currentDevice = null;
    }

    /**
     * show wifi system settings
     * - connection to hotspot devices needs to be done manually by the user since
     * programmatically connection does not work properly for most android devices
     * and android os versions
     */
    void gotoWifiPanel() {

        final String METHODTAG = ".gotoWifiPanel";

        Log.d(CLASSTAG, METHODTAG + ": Entering the Wifi, panel. Select from Available wifi networks. ");


        //Set Dialog WIFI Settings dialog
        String title = "Wifi Settings";
        String message = "Please connect to the WIFI HOTSPOT from the device.";
        String positiveButtonText = "OK";
        Runnable positiveButtonRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        connectingDialog.dismiss();

                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                        Log.i(CLASSTAG, METHODTAG + ": Wifi Panel launched");
                    }
                };


        DialogHandler wifiDialog = new DialogHandler();
        wifiDialog.setDialog(
                SearchDevicesActivity.this,
                title,
                message,
                false,
                positiveButtonText,
                positiveButtonRunnable,
                null,
                null
        );
        wifiDialog.show();

    }


    /// //////////////////////////////////////////////////////
	/*+++++++++++++++++++++++++++++++++++++++++++++++++++++//
	//	UI
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++//
	/////////////////////////////////////////////////////////
	/**
	 * responsible for updating the device list UI
	 */
    void updateList() {

        final String METHODTAG = ".updateList";


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ArrayList<Device> adapterList = new ArrayList<>(findDevices.getAvailableDevices());
                connectionTypeAdapter.setNewDeviceList(adapterList);
                connectionTypeAdapter.notifyDataSetChanged();
                deviceList.setAdapter(connectionTypeAdapter);

                Log.i(CLASSTAG, METHODTAG + ": List Updated");

            }
        });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	DIALOGS
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Displays the timeout dialog, when the app is not able to connect to the DISTO device after 30 seconds.
     *
     * @see #startConnectionTimeOutTimer
     */
    private void showConnectionTimedOutDialog() {

        final String METHODTAG = ".showConnectionTimedOutDialog";
        Log.d(CLASSTAG, METHODTAG + ": Connection timed out. Showing dialog to the user. ");

        if (currentDevice != null) {
            String title = "Connection Timeout";
            String message =
                    String.format(
                            "Could not connect to \n%s\nPlease check your device and adapters and try again.",
                            currentDevice.getDeviceID()
                    );

            connectingDialog.setDialog(SearchDevicesActivity.this, title, message, true);
            connectingDialog.show();
        } else {

            Log.i(CLASSTAG, METHODTAG + " Current Device is null.");
        }
    }


    /**
     * Show Dialog with error infgormation
     *
     * @param error   errorObject
     * @param message message
     */
    private void showError(ErrorObject error, String message) {

        alertsDialog.
                setAlert(
                        SearchDevicesActivity.this,
                        String.format(
                                Locale.getDefault(),
                                "errorCode: %d, %s \n %s",
                                error.getErrorCode(),
                                error.getErrorMessage(),
                                message
                        )
                );
        alertsDialog.show();
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


            switch (view.getId()) {

                case R.id.save_logs_btn:


                    Runnable saveFileRunnable = new Runnable() {
                        @Override
                        public void run() {

                            writeToFile();


                        }
                    };
                    new Thread(saveFileRunnable).start();
                    break;
                default:
                    Log.d(CLASSTAG, METHODTAG + ": " + view.getId() + " not implemented");
            }
        }
    }

    public void setOnClickListener(final Button button, final View.OnClickListener buttonListener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (button != null) {
                    button.setOnClickListener(buttonListener);
                } else {
                    Log.d(CLASSTAG, "Unable to set buttonListener on Button: " + button.getText());
                }
            }
        });
    }

    private void writeToFile() {

        Logs.log(Logs.LogTypes.informative, "WriteLogsToFile");
        try {

            String logsPath = Environment.getExternalStoragePublicDirectory("Documents").getAbsolutePath() + "/sdkLogs";
            File logsDir = new File(logsPath);
            logsDir.mkdir();
            File logFile = new File(logsPath + "/sdklog.txt");
            Logs.log(Logs.LogTypes.informative, "Writing Logs To File: " + logFile.getAbsolutePath());
            logFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(logFile);


            OutputStreamWriter out = new OutputStreamWriter(fOut);
            for (String log : Logs.getLogs()) {
                out.append(log);
                out.append("\n");

            }
            out.close();

            //Thread.sleep(2000);

            Intent email = new Intent(Intent.ACTION_SEND);
            email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // email.putExtra(Intent.EXTRA_EMAIL, new String[]{ "info@leica.ch"});
            email.putExtra(Intent.EXTRA_SUBJECT, "Failing logs");
            email.putExtra(Intent.EXTRA_TEXT, "Error logs");

            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "ch.leica.fileprovider", logFile);

            email.putExtra(Intent.EXTRA_STREAM, uri);

//need this to prompts email client only
            email.setType("message/rfc822");


            startActivity(Intent.createChooser(email, "Choose an Email client :"));
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } /*catch (InterruptedException e) {
			e.printStackTrace();
		}*/
    }
}