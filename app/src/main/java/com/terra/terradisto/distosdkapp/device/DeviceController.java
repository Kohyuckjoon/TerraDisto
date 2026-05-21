package com.terra.terradisto.distosdkapp.device;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.utilities.ErrorController;
import com.terra.terradisto.distosdkapp.utilities.Logs;

import ch.leica.sdk.Devices.BleDevice;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.Disto3DDevice;
import ch.leica.sdk.ErrorHandling.DeviceException;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.PermissionException;
import ch.leica.sdk.Listeners.ErrorListener;
import ch.leica.sdk.Listeners.ReceivedDataListener;
import ch.leica.sdk.Reconnection.ReconnectionHelper;
import ch.leica.sdk.Types;
import ch.leica.sdk.Utilities.WifiHelper;
import ch.leica.sdk.commands.response.Response;
import ch.leica.sdk.connection.ble.BleCharacteristic;

public abstract class DeviceController
        implements Device.ConnectionListener,
        ErrorListener,
        ReceivedDataListener,
        ReconnectionHelper.ReconnectListener {

    // ERROR CODES
    static final int COMMAND_ERROR_CODE = 10500;
    static final int NULL_DEVICE_CODE = 10600;
    static final String NULL_DEVICE_MESSAGE = "Try to access a null device object";
    private static final int NOT_IMPLEMENTED_METHOD_CODE = 10700;
    private static final String NOT_IMPLEMENTED_METHOD_MESSAGE = "Method has not been implemented";

    private final String CLASSTAG = DeviceController.class.getSimpleName();

    // AP MODE
    public String currentSSIDforAPmode;

    // DEVICE & LISTENER
    Device currentDevice;
    protected DeviceStatusListener deviceStatusListener;

    // EXTRA
    public boolean deviceIsInTrackingMode = false; // wifi용 플래그(ble도 같이 사용)
    public boolean turnOnAdapterDialogIsShown = false;

    // RECONNECTION
    ReconnectionHelper reconnectionHelper;
    public boolean reconnectionIsRunning = false;

    // ERROR HOLDER
    ErrorObject errorSendingCommand;

    // THREADS
    HandlerThread setDeviceInfoThread;
    Handler setDeviceInfoHandler;

    public String status;
    public boolean isUpdateRequested = false;

    // App context
    protected final Context appContext;

    // ABSTRACTS
    public abstract Response sendCommand(final Types.Commands command);
    public abstract Response sendCustomCommand(final String command);
    public abstract void checkForReconnection(final Context context);
    public abstract ErrorObject getDeviceInfo();
    abstract void readDataFromResponseObject(final Response response);

    // CONSTRUCTOR (새로 변경)
    public DeviceController(Context context, DeviceStatusListener statusListener) {
        this.appContext = context.getApplicationContext();
        this.deviceStatusListener = statusListener;
    }

    // ConnectionListener
    @Override
    public void onConnectionStateChanged(Device device, Device.ConnectionState state) {
        if (device != null) {
            currentDevice = device;
            this.status = state.toString();
            if (deviceStatusListener != null) {
                deviceStatusListener.onConnectionStateChanged(currentDevice.getDeviceID(), state);
            }
        } else {
            if (deviceStatusListener != null) {
                deviceStatusListener.onConnectionStateChanged("", Device.ConnectionState.disconnected);
            }
        }
    }

    // Reconnection
    public void setReconnectionHelper(ReconnectionHelper reconnectionHelper) {
        this.reconnectionHelper = reconnectionHelper;
    }

    public void setListeners() {
        if (currentDevice != null) {
            currentDevice.setConnectionListener(this);
            currentDevice.setErrorListener(this);
            currentDevice.setReceiveDataListener(this);
        }
        if (reconnectionHelper != null) {
            reconnectionHelper.setReconnectListener(this);
            reconnectionHelper.setErrorListener(this);
        }
    }

    // WifiDeviceController area (not implemented defaults)
    public ErrorObject sendCommandMotorPositionAbsolute(double hz, double v, boolean withTilt) {
        return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE);
    }
    public ErrorObject sendCommandMoveMotorUp(int velocity) { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject sendCommandMoveMotorRight(int velocity) { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject sendCommandMoveMotorDown(int velocity) { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject sendCommandMoveMotorLeft(int velocity) { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject sendCommandPositionStopVertical() { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject sendCommandPositionStopHorizontal() { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject disconnectLiveChannel() { return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }
    public ErrorObject connectLiveChannel(Disto3DDevice.LiveImageSpeed fast){ return new ErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE); }

    // DEVICE getters
    public Types.DeviceType getDeviceType(){ return currentDevice.getDeviceType(); }
    public Device getCurrentDevice() { return currentDevice; }
    public void setCurrentDevice(Device currentDevice) {
        this.currentDevice = currentDevice;
        status = this.currentDevice.getConnectionState().toString();
    }
    public String getDeviceName() { return (this.currentDevice != null) ? this.currentDevice.getDeviceName() : ""; }
    public String getModel() { return (this.currentDevice != null) ? this.currentDevice.getModel() : ""; }
    public Device.ConnectionState getConnectionState() { return (this.currentDevice != null) ? this.currentDevice.getConnectionState() : Device.ConnectionState.disconnected; }
    public String getDeviceID() { return (this.currentDevice != null) ? this.currentDevice.getDeviceID() : ""; }

    public String[] getAvailableCommands(){
        String METHODTAG = ".getAvailableCommands";
        if(this.currentDevice == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDevice");
            return new String[0];
        }
        String[] data = this.currentDevice.getAvailableCommands();
        if(data == null) data = new String[0];
        return data;
    }

    public String getWifiName(Context context) {
        return WifiHelper.getWifiName(context.getApplicationContext());
    }

    public boolean isReconnectionIsRunning() { return reconnectionIsRunning; }

    public void setReconnectionHelper(Context context) {
        this.reconnectionHelper = new ReconnectionHelper(currentDevice, context);
    }

    // Finding Devices
    public void stopFindingDevices() {
        final String METHODTAG = ".stopFindingAvailableDevices";
        Log.i(CLASSTAG, String.format("%s: Stop find Devices Task and set BroadcastReceivers to Null", METHODTAG));
        InformationActivityData informationActivityData = Clipboard.INSTANCE.getInformationActivityData();
        if(informationActivityData.deviceManager != null) {
            informationActivityData.deviceManager.stopFindingDevices();
        }
    }

    public void findAvailableDevices(Context context) {
        String METHODTAG = ".findAvailableDevices";
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        try {
            data.deviceManager.findAvailableDevices(context.getApplicationContext());
        } catch (PermissionException e) {
            Log.e(CLASSTAG, String.format("%s: Permissions Error. ", METHODTAG), e);
        }
    }

    // BLE start/pause (default not implemented)
    public ErrorObject startBTConnection(final BleDevice.BTConnectionCallback btConnectionCallback) {
        return ErrorController.createErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE);
    }
    public ErrorObject pauseBTConnection(final BleDevice.BTConnectionCallback btConnectionCallback) {
        return ErrorController.createErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE);
    }

    // UPDATE default
    public ErrorObject startUpdateProcess(Context context) {
        return ErrorController.createErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE);
    }
    public ErrorObject startReinstallProcess(Context context) {
        return ErrorController.createErrorObject(NOT_IMPLEMENTED_METHOD_CODE, NOT_IMPLEMENTED_METHOD_MESSAGE);
    }

    // Cleanup
    public void onDestroy() {
        final String METHODTAG = ".onDestroy";
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();

        if (reconnectionHelper != null){
            data.reconnectionHelper = reconnectionHelper;
            reconnectionHelper.setErrorListener(null);
            reconnectionHelper.setReconnectListener(null);
            reconnectionHelper.stopReconnecting();
            reconnectionHelper = null;
        }

        if(currentDevice != null) {
            data.device = currentDevice;
            currentDevice.setReceiveDataListener(null);
            currentDevice.setConnectionListener(null);
            currentDevice.setErrorListener(null);
            currentDevice = null;
        }

        if (setDeviceInfoThread != null) {
            setDeviceInfoThread.interrupt();
            setDeviceInfoThread = null;
            setDeviceInfoHandler = null;
        }

        reconnectionIsRunning = false;
        Log.d(CLASSTAG, String.format("%s: Activity destroyed. ", METHODTAG));
    }

    public List<BleCharacteristic> getAvailableCharacteristics() {
        List<BleCharacteristic> bleCharacteristics = new ArrayList<>();
        String METHODTAG = ".getAvailableCharacteristics";

        if(currentDevice == null){
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDevice");
            return bleCharacteristics;
        }

        try {
            currentDevice.getAllCharacteristics();
        } catch (DeviceException e) {
            Logs.logException(CLASSTAG, METHODTAG, e);
            return bleCharacteristics;
        }
        return bleCharacteristics;
    }
}
