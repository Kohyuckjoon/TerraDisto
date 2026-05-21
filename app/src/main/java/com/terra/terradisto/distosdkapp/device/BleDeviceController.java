package com.terra.terradisto.distosdkapp.device;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.utilities.ErrorController;
import com.terra.terradisto.distosdkapp.utilities.Logs;

import ch.leica.sdk.Defines;
import ch.leica.sdk.Devices.BleDevice;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.DeviceManager;
import ch.leica.sdk.ErrorHandling.DeviceException;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.Reconnection.ReconnectionHelper;
import ch.leica.sdk.Types;
import ch.leica.sdk.Utilities.WaitAmoment;
import ch.leica.sdk.commands.MeasuredValue;
import ch.leica.sdk.commands.ReceivedBleDataPacket;
import ch.leica.sdk.commands.ReceivedData;
import ch.leica.sdk.commands.response.Response;
import ch.leica.sdk.commands.response.ResponseBLEMeasurements;

public class BleDeviceController extends DeviceController {

    private static final int NO_METHOD_CODE = 20000;
    private static final String NO_METHOD_MESSAGE = "Method not implemented, the method is not valid for this type of device.";
    private static final int START_BT_CONNECTION_CODE = 30000;
    private static final int STOP_BT_CONNECTION_CODE = 30001;

    // LISTENERS
    public interface BleDataListener {
        void onDistance_Received(String convertedValueStrNoUnit, String unitStr);
        void onInclination_Received(String convertedValueStrNoUnit, String unitStr);
        void onDirection_Received(String convertedValueStrNoUnit, String unitStr);
        void onFirmwareRevision_Received(String data);
        void onHardwareRevision_Received(String data);
        void onManufacturerNameString_Received(String data);
        void onSerialNumber_Received(String data);
    }
    public interface RequestListener { void onRequestBluetoothTurnOn(); }

    private static final String CLASSTAG = BleDeviceController.class.getSimpleName();

    private BleDataListener bleDataListener;
    private RequestListener requestListener;
    private ErrorObject errorDeviceInfo;

    private boolean hasDistanceMeasurement = false;

    private MeasuredValue distanceValue;
    private MeasuredValue inclinationValue;
    private MeasuredValue directionValue;

    // 생성자 (일반)
    public BleDeviceController(Context context,
                               BleDataListener bleDataListener,
                               RequestListener requestListener,
                               DeviceStatusListener statusListener) {
        super(context, statusListener);
        this.bleDataListener = bleDataListener;
        this.requestListener = requestListener;
        this.turnOnAdapterDialogIsShown = false;
    }

    // 레거시 호환 (액티비티가 모든 리스너를 구현해야 함)
    @Deprecated
    public BleDeviceController(com.terra.terradisto.distosdkapp.activities.BleInformationActivity activity) {
        super(activity.getApplicationContext(),
                (activity instanceof DeviceStatusListener)
                        ? (DeviceStatusListener) activity
                        : new DeviceStatusListener() {
                    @Override public void onConnectionStateChanged(String deviceId, Device.ConnectionState state) {}
                    @Override public void onStatusChange(String status) {}
                    @Override public void onReconnect() {}
                    @Override public void onError(int code, String message) {}
                });
        this.bleDataListener = activity;
        this.requestListener = activity;
        this.turnOnAdapterDialogIsShown = false;
    }

    // ErrorListener
    @Override
    public void onError(ErrorObject error, Device device) {
        final String METHODTAG = ".requestErrorMessageDialog";
        if (deviceStatusListener != null) {
            deviceStatusListener.onError(error.getErrorCode(), error.getErrorMessage());
        }
    }

    // Commands
    @Override
    public Response sendCommand(Types.Commands command) {
        final String METHODTAG = ".sendCommand";
        Response response = new Response(command);
        ErrorObject error = null;

        if (currentDevice == null) {
            error = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response.setError(error);
            return response;
        }

        try {
            response = currentDevice.sendCommand(command);
            response.waitForData();
            error = response.getError();

            if (error == null) {
                readDataFromResponseObject(response);
            }
        } catch (DeviceException e) {
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response = new Response(command);
            response.setError(error);
        }
        return response;
    }

    @Override
    public Response sendCustomCommand(String command) {
        final String METHODTAG = ".sendCustomCommand";
        Response response = new Response(Types.Commands.Custom);
        ErrorObject error = null;

        if (currentDevice == null) {
            error = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response.setError(error);
            return response;
        }

        try {
            currentDevice.sendCustomCommand(command);
        } catch (DeviceException e) {
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response = new Response(Types.Commands.Custom);
            response.setError(error);
        }
        return response;
    }

    public ErrorObject sendClearCommand() {
        final String METHODTAG = ".sendClearCommand";
        errorSendingCommand = null;

        if (currentDevice == null) {
            errorSendingCommand = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, errorSendingCommand);
            return errorSendingCommand;
        }

        Response response = sendCommand(Types.Commands.Clear);
        errorSendingCommand = response.getError();

        if (errorSendingCommand == null) {
            if (deviceIsInTrackingMode) {
                response = sendCommand(Types.Commands.StopTracking);
                errorSendingCommand = response.getError();

                if (errorSendingCommand == null) {
                    deviceIsInTrackingMode = false;
                    try { Thread.sleep(1000); } catch (InterruptedException e) {
                        Log.e(CLASSTAG, METHODTAG + ": Error in thread sleep. ", e);
                    }
                }
            }
        }
        requestClearUI();
        return errorSendingCommand;
    }

    public ErrorObject sendDistanceCommand() {
        final String METHODTAG = ".sendDistanceCommand";
        errorSendingCommand = null;

        if (currentDevice == null) {
            errorSendingCommand = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, errorSendingCommand);
            return errorSendingCommand;
        }

        if (deviceIsInTrackingMode) {
            int DEVICE_IN_TRACKING_MODE_CODE = 10700;
            String DEVICE_IN_TRACKING_MODE_MESSAGE = "Device is in tracking mode, distance command can not be sent.";
            errorSendingCommand = ErrorController.createErrorObject(DEVICE_IN_TRACKING_MODE_CODE, DEVICE_IN_TRACKING_MODE_MESSAGE);
            return errorSendingCommand;
        }

        try {
            final ResponseBLEMeasurements response =
                    (ResponseBLEMeasurements) currentDevice.sendCommand(Types.Commands.Distance);

            response.waitForData();
            errorSendingCommand = response.getError();
            if (errorSendingCommand != null) {
                Log.e(CLASSTAG, String.format("%s: response has error: %s", METHODTAG, errorSendingCommand.getErrorMessage()));
            } else {
                setBLEMeasurementsData(response);
            }
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s: Error sending the o command. %s", METHODTAG, e.getMessage()), e);
            errorSendingCommand = new ErrorObject(COMMAND_ERROR_CODE, String.format("Error sending the o command. %s", e.getMessage()));
        }

        return errorSendingCommand;
    }

    private void setBLEMeasurementsData(final ResponseBLEMeasurements response) {
        final String METHODTAG = ".readDatafromResponseBLEMeasurementsObject";

        try {
            MeasuredValue data = response.getDistanceValue();
            if (bleDataListener != null && data != null) {
                bleDataListener.onDistance_Received(
                        data.getConvertedValueStrNoUnit(),
                        data.getUnitStr()
                );
            }

            data = response.getAngleInclination();
            if (bleDataListener != null && data != null) {
                bleDataListener.onInclination_Received(
                        data.getConvertedValueStrNoUnit(),
                        data.getUnitStr()
                );
            }

            data = response.getAngleDirection();
            if (bleDataListener != null && data != null) {
                bleDataListener.onDirection_Received(
                        data.getConvertedValueStrNoUnit(),
                        data.getUnitStr()
                );
            }
        } catch (Exception e) {
            Log.e(CLASSTAG, String.format("%s: Error sending the distance ( g ) command", METHODTAG), e);
        }
    }

    // Device Info
    @Override
    public ErrorObject getDeviceInfo() {
        final String METHODTAG = ".getDeviceInfo";
        errorDeviceInfo = new ErrorObject(NO_METHOD_CODE, NO_METHOD_MESSAGE);
        return errorDeviceInfo;
    }

    // ReconnectListener
    @Override
    public void onReconnect(Device device) {
        final String METHODTAG = ".onReconnect";
        Log.v(CLASSTAG, String.format("%s: in progress", METHODTAG));
        currentDevice = device;

        if (currentDevice == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG,"currentDevice");
            return;
        }
        reconnectionIsRunning = false;
        if (deviceStatusListener != null) deviceStatusListener.onReconnect();
        this.setListeners();
        onConnectionStateChanged(currentDevice, currentDevice.getConnectionState());
    }

    @Override
    public synchronized void checkForReconnection(final Context context) {
        final String METHODTAG = ".checkForReconnection";
        Log.v(CLASSTAG, String.format("%s: checking. ", METHODTAG));

        Context appCtx = (context != null) ? context.getApplicationContext() : this.appContext;

        if (reconnectionHelper == null) {
            reconnectionHelper = new ReconnectionHelper(currentDevice, appCtx);
            reconnectionHelper.setErrorListener(this);
            reconnectionHelper.setReconnectListener(this);
        }

        if (currentDevice == null) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDevice");
            return;
        }
        if (currentDevice.getConnectionState() == Device.ConnectionState.connected) {
            Log.d(CLASSTAG, String.format("%s: Device is already connected", METHODTAG));
            InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
            return;
        }
        if (!currentDevice.getConnectionType().equals(Types.ConnectionType.ble)) {
            Log.w(CLASSTAG, String.format("%s: Wrong, connection type. This should not happen", METHODTAG));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                WaitAmoment wait = new WaitAmoment();
                wait.waitAmoment(2000);

                boolean bluetoothIsAvailable =
                        DeviceManager.getInstance(appCtx).checkBluetoothAvailability();

                if (!bluetoothIsAvailable) {
                    Log.d(CLASSTAG, METHODTAG + ": bluetooth is not available");
                    requestBluetoothTurnOnDialog();
                    return;
                }

                if (!reconnectionIsRunning && reconnectionHelper != null) {
                    reconnectionIsRunning = true;
                    reconnectionHelper.startReconnecting();
                    status = "Reconnecting";
                    if (deviceStatusListener != null) deviceStatusListener.onStatusChange("Reconnecting");

                    InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
                    data.isSearchingEnabled = true;
                }
            }
        }).start();
    }

    // BleDeviceController.java (요약)
    public ErrorObject startBTConnection(final BleDevice.BTConnectionCallback cb) {
        if (currentDevice == null)
            return ErrorController.createErrorObject(NULL_DEVICE_CODE, "No device selected.");
        if (currentDevice.getConnectionState() != Device.ConnectionState.connected)
            return ErrorController.createErrorObject(START_BT_CONNECTION_CODE, "Device is not connected.");
        try {
            currentDevice.startBTConnection(new BleDevice.BTConnectionCallback() {
                @Override public void onFinished() { if (cb != null) cb.onFinished(); }
            });
            return null;
        } catch (DeviceException e) {
            return ErrorController.createErrorObject(START_BT_CONNECTION_CODE, e.getMessage());
        }
    }


    public ErrorObject pauseBTConnection(final BleDevice.BTConnectionCallback btConnectionCallback) {
        final String METHODTAG = ".pauseBTConnection";
        ErrorObject error = null;
        try {
            Log.i("BleDeviceController ", METHODTAG + " called");
            currentDevice.pauseBTConnection(new BleDevice.BTConnectionCallback() {
                @Override
                public void onFinished() {
                    if (btConnectionCallback != null) btConnectionCallback.onFinished();
                }
            });
        } catch (DeviceException e) {
            error = ErrorController.createErrorObject(STOP_BT_CONNECTION_CODE, e.getMessage());
        }
        return error;
    }

    // ReceivedDataListener
    @Override
    public void onAsyncDataReceived(final ReceivedData receivedData) {
        final String METHODTAG = ".onAsyncDataReceived";
        Log.v(CLASSTAG, String.format("%s Asynchronous data received.", METHODTAG));

        if (receivedData == null) {
            Log.e(CLASSTAG, String.format("%s: Error: receivedData object is null.  ", METHODTAG));
            return;
        }

        ReceivedBleDataPacket receivedBleDataPacket =
                (ReceivedBleDataPacket) receivedData.dataPacket;

        if (receivedBleDataPacket == null) {
            Log.e(CLASSTAG, String.format("%s: Error: receivedYetiDataPacket object is null. Returning.", METHODTAG));
            return;
        }

        try {
            String id = receivedBleDataPacket.dataId;

            switch (id) {
                case Defines.ID_DI_FIRMWARE_REVISION: {
                    String data = receivedBleDataPacket.getFirmwareRevision();
                    Logs.logDataReceived(METHODTAG, id, data);
                    if (bleDataListener != null) bleDataListener.onFirmwareRevision_Received(data);
                }
                break;
                case Defines.ID_DI_HARDWARE_REVISION: {
                    String data = receivedBleDataPacket.getHardwareRevision();
                    Logs.logDataReceived(METHODTAG, id, data);
                    if (bleDataListener != null) bleDataListener.onHardwareRevision_Received(data);
                }
                break;
                case Defines.ID_DI_MANUFACTURER_NAME_STRING: {
                    String data = receivedBleDataPacket.getManufacturerNameString();
                    Logs.logDataReceived(METHODTAG, id, data);
                    if (bleDataListener != null) bleDataListener.onManufacturerNameString_Received(data);
                }
                break;
                case Defines.ID_DI_SERIAL_NUMBER: {
                    String data = receivedBleDataPacket.getSerialNumber();
                    if (currentDevice != null && currentDevice.getDeviceName().length() <= 10) {
                        if (bleDataListener != null) bleDataListener.onSerialNumber_Received(data);
                    }
                    Logs.logDataReceived(METHODTAG, id, data);
                }
                break;

                case Defines.ID_DS_DISTANCE: {
                    if (!deviceIsInTrackingMode) requestClearUI();
                    float data = receivedBleDataPacket.getDistance();
                    distanceValue = new MeasuredValue(data);
                    hasDistanceMeasurement = true;
                    Logs.logDataReceived(METHODTAG, id, String.valueOf(data));
                }
                break;

                case Defines.ID_DS_DISTANCE_UNIT: {
                    if (distanceValue != null) {
                        short data = receivedBleDataPacket.getDistanceUnit();
                        distanceValue.setUnit(data);
                        distanceValue.convertDistance();
                        hasDistanceMeasurement = true;

                        Logs.logMeasurementReceived(
                                METHODTAG,
                                id,
                                distanceValue.getConvertedValueStrNoUnit(),
                                distanceValue.getUnitStr()
                        );

                        if (bleDataListener != null) {
                            bleDataListener.onDistance_Received(
                                    distanceValue.getConvertedValueStrNoUnit(),
                                    distanceValue.getUnitStr()
                            );
                        }
                    }
                }
                break;

                case Defines.ID_DS_INCLINATION: {
                    if (!deviceIsInTrackingMode && bleDataListener != null) {
                        bleDataListener.onInclination_Received("", "");
                    }
                    float data = receivedBleDataPacket.getInclination();
                    inclinationValue = new MeasuredValue(data);
                    Logs.logDataReceived(METHODTAG, id, String.valueOf(data));
                }
                break;

                case Defines.ID_DS_INCLINATION_UNIT: {
                    if (!deviceIsInTrackingMode && bleDataListener != null) {
                        bleDataListener.onInclination_Received("", "");
                    }
                    if (inclinationValue != null) {
                        short data = receivedBleDataPacket.getInclinationUnit();
                        inclinationValue.setUnit(data);
                        inclinationValue.convertAngle();

                        Logs.logMeasurementReceived(
                                METHODTAG,
                                id,
                                inclinationValue.getConvertedValueStrNoUnit(),
                                inclinationValue.getUnitStr()
                        );

                        if (bleDataListener != null) {
                            bleDataListener.onInclination_Received(
                                    inclinationValue.getConvertedValueStrNoUnit(),
                                    inclinationValue.getUnitStr()
                            );
                        }
                    }
                    if (!hasDistanceMeasurement && bleDataListener != null) {
                        bleDataListener.onDistance_Received("", "");
                    }
                }
                break;

                case Defines.ID_DS_DIRECTION: {
                    if (!deviceIsInTrackingMode && bleDataListener != null) {
                        bleDataListener.onDirection_Received("", "");
                    }
                    float data = receivedBleDataPacket.getDirection();
                    short unit = receivedBleDataPacket.getDirectionUnit();

                    directionValue = new MeasuredValue(data, unit);
                    directionValue.convertAngle();

                    if (bleDataListener != null) {
                        bleDataListener.onDirection_Received(
                                directionValue.getConvertedValueStrNoUnit(),
                                directionValue.getUnitStr()
                        );
                    }

                    Logs.logMeasurementReceived(
                            METHODTAG,
                            id,
                            directionValue.getConvertedValueStrNoUnit(),
                            directionValue.getUnitStr()
                    );

                    if (!hasDistanceMeasurement && bleDataListener != null) {
                        bleDataListener.onDistance_Received("", "");
                    }
                }
                break;

                case Defines.ID_DS_DIRECTION_UNIT: {
                    if (!deviceIsInTrackingMode && bleDataListener != null) {
                        bleDataListener.onDirection_Received("", "");
                    }
                    if (directionValue != null) {
                        short data = receivedBleDataPacket.getDirectionUnit();
                        directionValue.setUnit(data);
                        directionValue.convertAngle();

                        if (bleDataListener != null) {
                            bleDataListener.onDirection_Received(
                                    directionValue.getConvertedValueStrNoUnit(),
                                    directionValue.getUnitStr()
                            );
                        }

                        Logs.logMeasurementReceived(
                                METHODTAG,
                                id,
                                directionValue.getConvertedValueStrNoUnit(),
                                directionValue.getUnitStr()
                        );
                    }

                    if (!hasDistanceMeasurement && bleDataListener != null) {
                        bleDataListener.onDistance_Received("", "");
                    }
                }
                break;
            }

        } catch (IllegalArgumentCheckedException e) {
            Log.e(CLASSTAG, METHODTAG + ": Error onAsyncDataReceived ", e);
        } catch (Exception e) {
            Log.e(CLASSTAG, METHODTAG + ": Error onAsyncDataReceived ", e);
        }
    }

    @Override
    public void readDataFromResponseObject(Response response) {
        final String METHODTAG = ".readDataFromResponseObject";

        if (response.getError() != null) {
            Log.e(CLASSTAG, String.format("%s: response error: %s", METHODTAG, response.getError().getErrorMessage()));
            return;
        }
        if (response instanceof ResponseBLEMeasurements) {
            this.extractDataFromBLEResponseObject((ResponseBLEMeasurements) response);
        }
    }

    private void extractDataFromBLEResponseObject(final ResponseBLEMeasurements response) {
        final String METHODTAG = ".extractDataFromBLEResponseObject";
        Log.v(CLASSTAG, String.format("%s called. ", METHODTAG));

        MeasuredValue data = response.getDistanceValue();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue && !data.getUnitStr().equals(Defines.defaultStringValue)) {
            if (bleDataListener != null) {
                bleDataListener.onDistance_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
            }
        }

        data = response.getAngleInclination();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue && !data.getUnitStr().equals(Defines.defaultStringValue)) {
            if (bleDataListener != null) {
                bleDataListener.onInclination_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
            }
        }

        data = response.getAngleDirection();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue && !data.getUnitStr().equals(Defines.defaultStringValue)) {
            if (bleDataListener != null) {
                bleDataListener.onDirection_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
            }
        }
    }

    @Override
    public String[] getAvailableCommands() {
        String METHODTAG = ".getAvailableCommands";
        if (currentDevice == null) {
            ErrorObject error = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            return new String[0];
        }

        ArrayList<String> commandsTemp = new ArrayList<>();
        Collections.addAll(commandsTemp, currentDevice.getAvailableCommands());
        return commandsTemp.toArray(new String[]{});
    }

    private void requestClearUI() {
        String defaultValue = Defines.defaultStringValue;
        if (bleDataListener != null) {
            bleDataListener.onDistance_Received(defaultValue, defaultValue);
            bleDataListener.onInclination_Received(defaultValue, defaultValue);
            bleDataListener.onDirection_Received(defaultValue, defaultValue);
        }
    }

    synchronized private void requestBluetoothTurnOnDialog() {
        final String METHODTAG = ".requestBluetoothTurnOnDialog";
        Log.d(CLASSTAG, String.format("%s: turnOnAdapterDialogIsShown is %s", METHODTAG, turnOnAdapterDialogIsShown));
        if (turnOnAdapterDialogIsShown) return;
        if (requestListener != null) requestListener.onRequestBluetoothTurnOn();
    }
}
