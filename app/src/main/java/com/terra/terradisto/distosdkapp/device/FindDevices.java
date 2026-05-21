package com.terra.terradisto.distosdkapp.device;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.DeviceManager;
import ch.leica.sdk.ErrorHandling.PermissionException;


public class FindDevices implements DeviceManager.FoundAvailableDeviceListener{


    /**
     * ClassName
     */
    private final String CLASSTAG = FindDevices.class.getSimpleName();

    // to do infinite rounds of finding devices
    private Timer findDevicesTimer;

    // for finding and connecting to a device
    private DeviceManager deviceManager;
    private Context context;

    private AvailableDevicesListener availableDevicesListener;

    /**
     * List with all the devices available in BLE and wifi mode
     */
    private List<Device> availableDevices;

    /**
     * Receiver for Ble Events (connection - Disconnection)
     */
    private BroadcastReceiver bluetoothAdapterChangedReceiver;

    public FindDevices(Context context, AvailableDevicesListener listener ) {

        this.context = context;
        this.deviceManager = DeviceManager.getInstance(this.context);
        this.deviceManager.setFoundAvailableDeviceListener(this);
        this.availableDevicesListener = listener;
        this.availableDevices = new ArrayList<>();

    }


    /**
     * start looking for available devices
     * - first clears the UI and show only connected devices
     * - setups deviceManager and start finding devices process
     * - setups timer for restarting finding process (after 25 seconds start a new search iteration)
     */
    public void findAvailableDevices(Context context) {

        this.context = context;

        this.deviceManager.setFoundAvailableDeviceListener(this);

        long findAvailableDevicesDelay = 0;
        long findAvailableDevicesPeriod = 20000;

        //
        // restart for finding devices:
        // a) because already found devices may be out of reach by now,
        // b) the user may changed adapter settings meanwhile
        if (findDevicesTimer == null) {
            findDevicesTimer = new Timer();
            findDevicesTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    availableDevices.clear();
                    availableDevicesListener.onAvailableDevicesChanged(availableDevices);
                    findDevices();
                }
            }, findAvailableDevicesDelay, findAvailableDevicesPeriod);
        }
    }

    private void findDevices(){

        requestConnectedDevices();

        final String METHODTAG = ".findAvailableDevices";

        Log.d(CLASSTAG, METHODTAG + ": The app is attempting to find available devices.");

        try {
            InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
            if(data.isSearchingEnabled == true) {
                deviceManager.findAvailableDevices(context);
            }
        } catch (PermissionException e) {

            Log.e(CLASSTAG, METHODTAG + ": missing permission: " + e.getMessage());

        }
    }

    /**
     * stop finding devices
     */
    public void stopFindingDevices() {
        final String METHODTAG = ".stopFindingAvailableDevices";

        stopFindAvailableDevicesTimer();

        Log.i(
                CLASSTAG,
                String.format(
                        "%s: Stop find Devices Task and set BroadcastReceivers to Null",
                        METHODTAG
                )
        );
        deviceManager.stopFindingDevices();
    }


    private void stopFindAvailableDevicesTimer() {

        final String METHODTAG = ".stopFindAvailableDevicesTimer";
        // stop finding devices when activity stops
        // stop the timer for finding devices
        if (findDevicesTimer != null) {
            findDevicesTimer.cancel();
            findDevicesTimer.purge();
            findDevicesTimer = null;
        }
        Log.i(CLASSTAG, METHODTAG + ": FindAvailableDevices timer stopped");

    }

    public void onDestroy() {
        // stop finding devices when activity finishes
        // stop the timer for finding devices
        if (findDevicesTimer != null) {
            findDevicesTimer.cancel();
            findDevicesTimer.purge();
            findDevicesTimer = null;
        }
    }

    @Override
    public void onAvailableDeviceFound(Device device) {
        final String METHODTAG = ".onAvailableDeviceFound";
        Log.i(CLASSTAG,
                String.format("%s: deviceId: %s, deviceName: %s", METHODTAG, device.getDeviceID(), device.getDeviceName()));

        // synchronized, because internally onAvailableDeviceFound() can be called from different threads
        synchronized (availableDevices) {

            // in rare cases it can happen, that a device is found twice. so here is a double check.
            for (Device availableDevice : availableDevices) {
                if (availableDevice.getDeviceID().equalsIgnoreCase(device.getDeviceID())) {
                    return;
                }
            }
            availableDevices.add(device);
        }

        //Update the list shown to the user
        availableDevicesListener.onAvailableDeviceFound();

    }

    public void requestConnectedDevices() {
        // synchronized, because internally onAvailableDeviceFound() can be called from different threads
        synchronized (availableDevices) {
            availableDevices = new ArrayList<>(this.deviceManager.getConnectedDevices());
            availableDevicesListener.onAvailableDevicesChanged(availableDevices);
        }
    }

    public List<Device> getAvailableDevices() {
        return availableDevices;
    }


    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ REGISTER / UNREGISTER RECEIVERS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    /**
     * Registers the needed broadcast receivers for ble devices - detect bluetooth adapter turned on / off
     */
    public void registerReceivers() {

        final String METHODTAG = "registerReceivers";
        // BLE
        if (bluetoothAdapterChangedReceiver == null) {

            bluetoothAdapterChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {

                    final String action = intent.getAction();
                    Log.d(CLASSTAG, String.format("%s: Action: %s",METHODTAG,action));

                    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR);

                        //Actions to be executed when the BLE status changes.
                        switch (state) {
                            case BluetoothAdapter.STATE_OFF:
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                Log.d(CLASSTAG, String.format("%s: Action STATE OFF:  %s",METHODTAG,action));
                                availableDevices.clear();
                                availableDevicesListener.onAvailableDevicesChanged(availableDevices);

                                break;

                            case BluetoothAdapter.STATE_ON:
                                Log.d(CLASSTAG, String.format("%s: Action STATE ON:  %s",METHODTAG,action));
                                break;

                            case BluetoothAdapter.STATE_TURNING_ON:
                                Log.d(CLASSTAG, String.format("%s: Action STATE TURNING ON :  %s",METHODTAG,action));
                                break;

                            default:
                                Log.w(CLASSTAG, "Unknown State");
                                break;
                        }
                    }else{
                        Log.w(CLASSTAG, "Action variable is null");
                    }
                }
            };
            Log.d(CLASSTAG, "bluetoothAdapterChangedReceiver start registering");
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            this.context.registerReceiver(bluetoothAdapterChangedReceiver, filter);
            Log.d(CLASSTAG, "BluetoothAdapterChangedReceiver registered");
        }


    }

    public void unregisterReceivers() {

        try {
            if (bluetoothAdapterChangedReceiver != null) {
                this.context.unregisterReceiver(bluetoothAdapterChangedReceiver);
                bluetoothAdapterChangedReceiver = null;
            }
            Log.d(CLASSTAG, "BluetoothAdapterChangedReceiver unregistered");

        } catch (Exception e) {
            Log.e(CLASSTAG, String.format("Error Unregistering the Receiver%s", e.getMessage()));
        }
    }
}