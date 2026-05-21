package com.terra.terradisto.distosdkapp.device;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// 레거시 호환용 (Deprecated 생성자에서만 사용)
import com.terra.terradisto.distosdkapp.activities.WifiInformationActivity;

import com.terra.terradisto.distosdkapp.utilities.ErrorController;
import com.terra.terradisto.distosdkapp.utilities.Logs;

import ch.leica.sdk.Defines;
import ch.leica.sdk.Devices.Disto3DDevice;
import ch.leica.sdk.ErrorHandling.DeviceException;
import ch.leica.sdk.ErrorHandling.ErrorDefinitions;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.Reconnection.ReconnectionHelper;
import ch.leica.sdk.Types;
import ch.leica.sdk.Utilities.WaitAmoment;
import ch.leica.sdk.Utilities.WifiHelper;
import ch.leica.sdk.commands.Image;
import ch.leica.sdk.commands.MeasuredValue;
import ch.leica.sdk.commands.MeasurementConverter;
import ch.leica.sdk.commands.ReceivedData;
import ch.leica.sdk.commands.ReceivedWifiDataPacket;
import ch.leica.sdk.commands.response.Response;
import ch.leica.sdk.commands.response.ResponseBatteryStatus;
import ch.leica.sdk.commands.response.ResponseDeviceInfo;
import ch.leica.sdk.commands.response.ResponseFace;
import ch.leica.sdk.commands.response.ResponseImage;
import ch.leica.sdk.commands.response.ResponseMotorStatus;
import ch.leica.sdk.commands.response.ResponsePlain;
import ch.leica.sdk.commands.response.ResponseTemperature;
import ch.leica.sdk.commands.response.ResponseWifiMeasurements;
import ch.leica.sdk.Devices.Device;

import static android.content.Context.WIFI_SERVICE;

public class WifiDeviceController extends DeviceController {

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ INTERFACES
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    public interface WifiDataListener {
        void onImageData_Received(byte[] image);
        void onPlainData_Received(String data);

        void onDistance_Received(String convertedValueStrNoUnit, String unitStr);
        void onHorizontalAngleWithTilt_hz_Received(String convertedValueStrNoUnit, String unitStr);
        void onVerticalAngleWithTilt_v_Received(String convertedValueStrNoUnit, String unitStr);
        void onHorizontalAngleWithoutTilt_ni_hz_Received(String convertedValueStrNoUnit, String unitStr);
        void onVerticalAngleWithoutTilt_ni_hz_Received(String convertedValueStrNoUnit, String unitStr);

        void onICross_Received(float iCross);
        void onIhz_Received(float ihz);
        void onIlen_Received(float iLen);

        void onIState_Received(int iState);

        void onIP_Received(String ip);
        void onSerialNumber_Received(String serialNumber);
        void onSoftwareName_Received(String softwareName);
        void onSoftwareVersion_Received(String softwareVersion);
        void onDeviceType_Received(int deviceType);
        void onMacAddress_Received(String macAddress);
        void onWifiModuleVersion_Received(String wifiModuleVersion);
        void onWifiESSID_Received(String wifiESSID);
        void onWifiChannelNumber_Received(int wifiChannelNumber);
        void onWifiFrequency_Received(int wifiFrequency);
        void onUserVind_Received(float userVind);
        void onUserCamLasX_Received(float userCamLasX);
        void onUserCamLasY_Received(float userCamLasY);
        void onSensitiveMode_Received(float sensitiveMode);

        void onFace_Received(int face);
        void onBatteryVoltageData_Received(float batteryVoltage);
        void onBatteryStatusData_Received(int batteryStatus);

        void onTemperatureHorizontalAngleSensor_Hz_Received(float temperatureHorizontalAngleSensor_hz);
        void onTemperatureVerticalAngleSensor_V_Received(float temperatureVerticalAngleSensor_v);
        void onTemperatureDistanceMeasurementSensor_Edm_Received(float temperatureDistanceMeasurementSensor_edm);
        void onTemperatureBLESensor_Received(float temperatureBLESensor);

        void onEventReceived(String receivedData);
        void onLevelReceived(int dataInt);
        void onImageReceived(Image image);
    }

    public interface UiListener {
        void turnCommandsButtonsOn();
        void turnCommandsButtonsOff();
    }

    public interface WifiRequestListener {
        void onRequestWifiTurnOn();
    }

    private static final String CLASSTAG = WifiDeviceController.class.getSimpleName();

    private WifiDataListener wifiDataListener;
    private UiListener uiListener;
    private WifiRequestListener requestListener;
    private ErrorObject errorDeviceInfo;

    // NO-OP status listener (필요 시 자동 대체)
    private static final DeviceStatusListener NO_OP_DEVICE_STATUS_LISTENER =
            new DeviceStatusListener() {
                @Override public void onConnectionStateChanged(String deviceId, Device.ConnectionState state) {}
                @Override public void onStatusChange(String status) {}
                @Override public void onReconnect() {}
                @Override public void onError(int code, String message) {}
            };

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    //++ CONSTRUCTORS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++
    /** 일반 생성자: Context + 각종 리스너 + 상태리스너 */
    public WifiDeviceController(Context context,
                                WifiDataListener dataListener,
                                UiListener uiListener,
                                WifiRequestListener requestListener,
                                DeviceStatusListener statusListener) {
        super(context, (statusListener != null) ? statusListener : NO_OP_DEVICE_STATUS_LISTENER);
        this.wifiDataListener = dataListener;
        this.uiListener = uiListener;
        this.requestListener = requestListener;
        this.turnOnAdapterDialogIsShown = false;
    }

    /** 레거시 호환: Activity가 모든 리스너를 구현한다고 가정 */
    @Deprecated
    public WifiDeviceController(WifiInformationActivity activity) {
        this(activity.getApplicationContext(),
                activity, // WifiDataListener
                activity, // UiListener
                activity, // WifiRequestListener
                (activity instanceof DeviceStatusListener) ? (DeviceStatusListener) activity : NO_OP_DEVICE_STATUS_LISTENER);
    }

    ///////////////////////////////////////////////////////
    // ErrorListener
    ///////////////////////////////////////////////////////
    @Override
    public void onError(ErrorObject error, Device device) {
        final String METHODTAG = ".requestErrorMessageDialog";

        if (error == null) return;

        // 소켓/채널 단절류는 이미 별도 재시도/흐름이 있으니 조용히 무시
        if (error.getErrorCode() == ErrorDefinitions.EVENTCHANNEL_SOCKET_NOT_CONNECTING_CODE ||
                error.getErrorCode() == ErrorDefinitions.RESPONSECHANNEL_SOCKET_NOT_CONNECTING_CODE ||
                error.getErrorCode() == ErrorDefinitions.RESPONSE_ERROR_RECEIVED_CODE ||
                error.getErrorCode() == ErrorDefinitions.WIFI_EVENT_CH_CLOSED_CODE ||
                error.getErrorCode() == ErrorDefinitions.WIFI_RESPONSE_CH_CLOSED_CODE ||
                error.getErrorCode() == ErrorDefinitions.RESPONSECHANNEL_UNEXPECTEDERROR_CODE ||
                error.getErrorCode() == ErrorDefinitions.RECEIVED_DATA_NULL_STRING_CODE) {
            return;
        }

        if (deviceStatusListener != null) {
            deviceStatusListener.onError(error.getErrorCode(), error.getErrorMessage());
        } else {
            Log.e(CLASSTAG, METHODTAG + " " + error.getErrorMessage());
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public Response sendCommand(Types.Commands command) {
        if (uiListener != null) uiListener.turnCommandsButtonsOff();
        String METHODTAG = ".sendCommand";

        Response response = new Response(command);
        ErrorObject error = null;

        if (currentDevice == null) {
            error = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response.setError(error);
            if (uiListener != null) uiListener.turnCommandsButtonsOn();
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

        if (uiListener != null) uiListener.turnCommandsButtonsOn();
        return response;
    }

    @Override
    public Response sendCustomCommand(String command) {
        final String METHODTAG = ".sendCustomCommand";

        if (uiListener != null) uiListener.turnCommandsButtonsOff();
        Response response = new Response(Types.Commands.Custom);
        ErrorObject error = null;

        if (currentDevice == null) {
            error = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response.setError(error);
            if (uiListener != null) uiListener.turnCommandsButtonsOn();
            return response;
        }

        try {
            response = currentDevice.sendCustomCommand(command);
            response.waitForData();
            error = response.getError();
            if (error == null) {
                extractDataFromPlainResponse((ResponsePlain) response);
            }
        } catch (DeviceException e) {
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);

            response = new Response(Types.Commands.Custom);
            response.setError(error);
        }

        if (uiListener != null) uiListener.turnCommandsButtonsOn();
        return response;
    }

    public ErrorObject sendMeasurePolarCommand() {
        final String METHODTAG = ".sendMeasurePolarCommand";
        if (uiListener != null) uiListener.turnCommandsButtonsOff();

        try {
            final ResponseWifiMeasurements response =
                    (ResponseWifiMeasurements) currentDevice.sendCommand(Types.Commands.MeasurePolar);

            response.waitForData();
            errorSendingCommand = response.getError();
            if (errorSendingCommand == null) {
                extractDataFromWifiResponseObject(response);
            }
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s Error Sending Command. ", METHODTAG), e);
            errorSendingCommand = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }

        if (uiListener != null) uiListener.turnCommandsButtonsOn();
        return errorSendingCommand;
    }

    // 3DD motor controls (Wifi 전용)
    public ErrorObject sendCommandMotorPositionAbsolute(double hz, double v, boolean withTilt) {
        final String METHODTAG = ".sendCommandMotorPositionAbsolute";
        ErrorObject error = null;
        try {
            if (currentDevice != null) {
                this.currentDevice.sendCommandMotorPositionAbsolute(hz, v, withTilt);
            }
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%sNF%s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject sendCommandMoveMotorUp(int velocity) {
        final String METHODTAG = ".sendCommandMoveMotorUp";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.sendCommandMoveMotorUp(velocity);
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject sendCommandMoveMotorRight(int velocity) {
        final String METHODTAG = ".sendCommandMoveMotorRight";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.sendCommandMoveMotorRight(velocity);
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject sendCommandMoveMotorDown(int velocity) {
        final String METHODTAG = ".sendCommandMoveMotorDown";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.sendCommandMoveMotorDown(velocity);
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject sendCommandMoveMotorLeft(int velocity) {
        final String METHODTAG = ".sendCommandMoveMotorLeft";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.sendCommandMoveMotorLeft(velocity);
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject sendCommandPositionStopVertical() {
        final String METHODTAG = ".sendCommandPositionStopVertical";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.sendCommandPositionStopVertical();
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject sendCommandPositionStopHorizontal() {
        final String METHODTAG = ".sendCommandPositionStopHorizontal";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.sendCommandPositionStopHorizontal();
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject disconnectLiveChannel() {
        final String METHODTAG = ".disconnectLiveChannel";
        ErrorObject error = null;
        try {
            if (currentDevice != null) currentDevice.disconnectLiveChannel();
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    public ErrorObject connectLiveChannel(Disto3DDevice.LiveImageSpeed fast){
        final String METHODTAG = ".connectLiveChannel";
        ErrorObject error = null;
        try {
            if (currentDevice != null) this.currentDevice.connectLiveChannel(fast);
        } catch (DeviceException e) {
            Log.e(CLASSTAG, String.format("%s. Message: %s", METHODTAG, e.getMessage()));
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        }
        return error;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // DEVICE INFO
    //////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public ErrorObject getDeviceInfo() {
        final String METHODTAG = ".getDeviceInfo";
        final CountDownLatch deviceInfoLatch = new CountDownLatch(1);

        try {
            if (setDeviceInfoThread == null) {
                setDeviceInfoThread = new HandlerThread("setDeviceHandler", HandlerThread.MAX_PRIORITY);
                setDeviceInfoThread.start();
                setDeviceInfoHandler = new Handler(setDeviceInfoThread.getLooper());
            }

            setDeviceInfoHandler.post(new Runnable() {
                @Override
                public void run() {
                    Response response = sendCommand(Types.Commands.GetSerialNumber);
                    errorDeviceInfo = response.getError();
                    if (errorDeviceInfo == null) {
                        response = sendCommand(Types.Commands.GetSoftwareVersion);
                        errorDeviceInfo = response.getError();
                    }
                    deviceInfoLatch.countDown();
                }
            });

            boolean ok = deviceInfoLatch.await(5000, TimeUnit.MILLISECONDS);
            if (!ok) {
                errorDeviceInfo = new ErrorObject(COMMAND_ERROR_CODE, "GET_DEVICE_INFORMATION_TIMEOUT");
            }

        } catch (InterruptedException e) {
            errorDeviceInfo = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
        } catch (Exception e) {
            errorDeviceInfo = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
            Logs.logErrorObject(CLASSTAG, METHODTAG, errorDeviceInfo);
        }
        return errorDeviceInfo;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // ReconnectListener
    //////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onReconnect(Device device) {
        final String METHODTAG = ".onReconnect";
        Log.v(CLASSTAG, String.format("%s: in progress", METHODTAG));

        currentDevice = device;
        if (currentDevice == null) return;

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

        if (currentDevice == null) return;
        if (currentDevice.getConnectionState() == Device.ConnectionState.connected) return;

        // 현재 구현은 "AP 모드에 대해서만" 재연결 로직을 태움 (SSID 일치 확인)
        if (!currentDevice.getConnectionType().equals(Types.ConnectionType.wifiAP)) {
            Log.d(CLASSTAG, METHODTAG + ": Reconnection is implemented only for AP mode.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // AP 모드
                Log.d(CLASSTAG, String.format("%s: for AP", METHODTAG));

                WifiManager wifiManager =
                        (WifiManager) appCtx.getSystemService(WIFI_SERVICE);

                // wait a sec to let adapter work
                new WaitAmoment().waitAmoment(2000);

                // wifi 꺼져 있으면 사용자에게 켜달라 요청
                if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                    Log.d(CLASSTAG, METHODTAG + ": Wifi disabled, not reconnecting");
                    requestWifiTurnOnDialog();
                    return;
                }

                // 현재 연결된 SSID
                String wifiName = WifiHelper.getWifiName(appCtx);
                Log.i(CLASSTAG, String.format("%s: currentWifi: %s, currentSSIDforAPmode: %s", METHODTAG, wifiName, currentSSIDforAPmode));

                if (wifiName == null) {
                    Logs.logNullValues(CLASSTAG, METHODTAG, "wifiName");
                    return;
                }

                if (!wifiName.equalsIgnoreCase(currentSSIDforAPmode)) {
                    Log.i(CLASSTAG, METHODTAG + ": SSID not matched; skip reconnection");
                    return;
                }

                Log.i(CLASSTAG, String.format("%s: Reconnection running? %s", METHODTAG, reconnectionIsRunning));
                status = "Reconnecting";
                if (!reconnectionIsRunning && reconnectionHelper != null) {
                    reconnectionIsRunning = true;
                    if (deviceStatusListener != null) deviceStatusListener.onStatusChange("Reconnecting");
                    reconnectionHelper.startReconnecting();
                }
            }
        }).start();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // ReceivedDataListener
    //////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onAsyncDataReceived(ReceivedData receivedData) {
        final String METHODTAG = "onAsyncDataReceived";

        if (receivedData == null) {
            Log.e(CLASSTAG, String.format("%s: Error: receivedData object is null.  ", METHODTAG));
            return;
        }

        ReceivedWifiDataPacket receivedWifiDataPacket =
                (ReceivedWifiDataPacket) receivedData.dataPacket;

        if (receivedWifiDataPacket == null) {
            Log.e(CLASSTAG, String.format("%s: Error: receivedWifiDataPacket object is null. Returning.", METHODTAG));
            return;
        }

        try {
            String id = receivedWifiDataPacket.dataId;
            switch (id) {
                case Defines.ID_EVPOS: {
                    String dataString = String.format(
                            "EvPosMotor: %s%s",
                            receivedWifiDataPacket.getEvPosMotor(),
                            receivedWifiDataPacket.getEvPosMotorResult()
                    );
                    if (wifiDataListener != null) wifiDataListener.onEventReceived(dataString);
                }
                break;

                case Defines.ID_EVKEY: {
                    int dataInt = receivedWifiDataPacket.getEvKeyKey();
                    String data = receivedWifiDataPacket.getEvKeyEvent();
                    String dataString = String.format(Locale.getDefault(), "EvKey: %d%s", dataInt, data);
                    if (wifiDataListener != null) wifiDataListener.onEventReceived(dataString);
                }
                break;

                case Defines.ID_EVLINE: {
                    String data = receivedWifiDataPacket.getEvLineLine();
                    if (wifiDataListener != null) wifiDataListener.onEventReceived("EvLine: " + data);
                }
                break;

                case Defines.ID_EVBAT: {
                    String data = receivedWifiDataPacket.getEvLineBattery();
                    if (wifiDataListener != null) wifiDataListener.onEventReceived("EvBat: " + data);
                }
                break;

                case Defines.ID_EVLEV: {
                    int dataInt = receivedWifiDataPacket.getEvLevel();
                    if (wifiDataListener != null) {
                        wifiDataListener.onEventReceived(String.format(Locale.getDefault(), "EvLev: %d", dataInt));
                        wifiDataListener.onLevelReceived(dataInt);
                    }
                }
                break;

                case Defines.ID_EVMSG: {
                    String dataString = String.format(
                            "EvMsg: %s%s%s",
                            receivedWifiDataPacket.getEvMsgAction(),
                            receivedWifiDataPacket.getEvMsgMessage(),
                            receivedWifiDataPacket.getEvMsgBit()
                    );
                    if (wifiDataListener != null) wifiDataListener.onEventReceived(dataString);
                }
                break;

                case Defines.ID_EVCAL: {
                    String dataString = "EvCal: " + receivedWifiDataPacket.getEvCalResult();
                    if (wifiDataListener != null) wifiDataListener.onEventReceived(dataString);
                }
                break;

                case Defines.ID_EVMP: {
                    setMeasurePolarData(receivedWifiDataPacket);
                    if (wifiDataListener != null) wifiDataListener.onEventReceived("EvMp");
                }
                break;

                case Defines.ID_EVMPI: {
                    setMeasurePolarData(receivedWifiDataPacket);
                    Image image = receivedWifiDataPacket.getImage();
                    if (wifiDataListener != null) {
                        wifiDataListener.onImageReceived(image);
                        wifiDataListener.onEventReceived("EvMpi");
                    }
                }
                break;

                case Defines.ID_LIVEIMAGE: {
                    Image image = receivedWifiDataPacket.getImage();
                    if (wifiDataListener != null) wifiDataListener.onImageReceived(image);
                }
                break;
            }
        } catch (IllegalArgumentCheckedException e) {
            Log.e(CLASSTAG, METHODTAG, e);
        } catch (Exception e) {
            Log.e(CLASSTAG, String.format("%s: Error in the UI", METHODTAG), e);
        }
    }

    private void setMeasurePolarData(ReceivedWifiDataPacket p) throws IllegalArgumentCheckedException {
        final String METHODTAG = "setMeasurePolarData";
        Log.v(CLASSTAG, String.format("%s called. ", METHODTAG));

        MeasuredValue mv;

        // Distance
        float f = p.getDistance();
        if (!Float.isNaN(f)) {
            mv = new MeasuredValue(f, MeasurementConverter.getDefaultWifiDistanceUnit());
            mv.convertDistance();
            if (wifiDataListener != null) wifiDataListener.onDistance_Received(mv.getConvertedValueStrNoUnit(), mv.getUnitStr());
        }

        // HZ with tilt
        f = p.getHorizontalAnglewithTilt_hz();
        if (!Float.isNaN(f)) {
            mv = new MeasuredValue(f, MeasurementConverter.getDefaultWifiAngleUnit());
            mv.convertAngle();
            if (wifiDataListener != null) wifiDataListener.onHorizontalAngleWithTilt_hz_Received(mv.getConvertedValueStrNoUnit(), mv.getUnitStr());
        }

        // V with tilt
        f = p.getVerticalAngleWithTilt_v();
        if (!Float.isNaN(f)) {
            mv = new MeasuredValue(f, MeasurementConverter.getDefaultWifiAngleUnit());
            mv.convertAngle();
            if (wifiDataListener != null) wifiDataListener.onVerticalAngleWithTilt_v_Received(mv.getConvertedValueStrNoUnit(), mv.getUnitStr());
        }

        // HZ without tilt
        f = p.getHorizontalAngleWithoutTilt_ni_hz();
        mv = new MeasuredValue(f, MeasurementConverter.getDefaultWifiAngleUnit());
        if (!Float.isNaN(f)) {
            mv.convertAngle();
            if (wifiDataListener != null) wifiDataListener.onHorizontalAngleWithoutTilt_ni_hz_Received(mv.getConvertedValueStrNoUnit(), mv.getUnitStr());
        }

        // V without tilt
        f = p.getVerticalAngleWithoutTilt_ni_v();
        if (!Float.isNaN(f)) {
            mv = new MeasuredValue(f, MeasurementConverter.getDefaultWifiAngleUnit());
            mv.convertAngle();
            if (wifiDataListener != null) wifiDataListener.onVerticalAngleWithoutTilt_ni_hz_Received(mv.getConvertedValueStrNoUnit(), mv.getUnitStr());
        }
    }

    @Override
    public void readDataFromResponseObject(final Response response) {
        final String METHODTAG = ".readDataFromResponseObject";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));

        if (response.getError() != null) {
            Log.e(CLASSTAG, String.format("%s: response error: %s", METHODTAG, response.getError().getErrorMessage()));
            return;
        }

        if (response instanceof ResponseWifiMeasurements) {
            extractDataFromWifiResponseObject((ResponseWifiMeasurements) response);
        } else if (response instanceof ResponseImage) {
            extractDataFromImageResponse((ResponseImage) response);
        } else if (response instanceof ResponseDeviceInfo) {
            extractDataFromDeviceInfoResponse((ResponseDeviceInfo) response);
        } else if (response instanceof ResponseMotorStatus) {
            // motor status 처리 필요 시 여기에
        } else if (response instanceof ResponseTemperature) {
            extractDataFromTemperatureResponse((ResponseTemperature) response);
        } else if (response instanceof ResponseFace) {
            extractDataFromFaceResponse((ResponseFace) response);
        } else if (response instanceof ResponseBatteryStatus) {
            extractDataFromBatteryResponse((ResponseBatteryStatus) response);
        } else if (response instanceof ResponsePlain) {
            extractDataFromPlainResponse((ResponsePlain) response);
        }
    }

    private void extractDataFromTemperatureResponse(ResponseTemperature response){
        final String METHODTAG = ".extractDataFromTemperatureResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));

        if (wifiDataListener == null) return;

        if (Defines.defaultFloatValue < response.getTemperatureDistanceMeasurementSensor_Edm()) {
            wifiDataListener.onTemperatureDistanceMeasurementSensor_Edm_Received(
                    response.getTemperatureDistanceMeasurementSensor_Edm()
            );
        }
        if (Defines.defaultFloatValue < response.getTemperatureHorizontalAngleSensor_Hz()) {
            wifiDataListener.onTemperatureHorizontalAngleSensor_Hz_Received(
                    response.getTemperatureHorizontalAngleSensor_Hz()
            );
        }
        if (Defines.defaultFloatValue < response.getTemperatureVerticalAngleSensor_V()) {
            wifiDataListener.onTemperatureVerticalAngleSensor_V_Received(
                    response.getTemperatureVerticalAngleSensor_V()
            );
        }
        if (Defines.defaultFloatValue < response.getTemperatureBLESensor()) {
            wifiDataListener.onTemperatureBLESensor_Received(
                    response.getTemperatureBLESensor()
            );
        }
    }

    private void extractDataFromBatteryResponse(ResponseBatteryStatus response){
        final String METHODTAG = ".extractDataFromBatteryResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        if (wifiDataListener == null) return;

        wifiDataListener.onBatteryVoltageData_Received(response.getBatteryVoltage());
        wifiDataListener.onBatteryStatusData_Received(response.getBatteryStatus());
    }

    private void extractDataFromFaceResponse(ResponseFace response){
        final String METHODTAG = ".extractDataFromFaceResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        if (wifiDataListener != null) wifiDataListener.onFace_Received(response.getFace());
    }

    private void extractDataFromDeviceInfoResponse(ResponseDeviceInfo response){
        final String METHODTAG = ".extractDataFromDeviceInfoResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        if (wifiDataListener == null) return;

        if (!response.getIP().equals(Defines.defaultStringValue)) wifiDataListener.onIP_Received(response.getIP());
        if (!response.getSerialNumber().equals(Defines.defaultStringValue)) wifiDataListener.onSerialNumber_Received(response.getSerialNumber());
        if (!response.getSoftwareName().equals(Defines.defaultStringValue)) wifiDataListener.onSoftwareName_Received(response.getSoftwareName());
        if (!response.getSoftwareVersion().equals(Defines.defaultStringValue)) wifiDataListener.onSoftwareVersion_Received(response.getSoftwareVersion());
        if (response.getDeviceType() != Defines.defaultIntValue) wifiDataListener.onDeviceType_Received(response.getDeviceType());
        if (!response.getMacAddress().equals(Defines.defaultStringValue)) wifiDataListener.onMacAddress_Received(response.getMacAddress());
        if (!response.getWifiModuleVersion().equals(Defines.defaultStringValue)) wifiDataListener.onWifiModuleVersion_Received(response.getWifiModuleVersion());
        if (!response.getWifiESSID().equals(Defines.defaultStringValue)) wifiDataListener.onWifiESSID_Received(response.getWifiESSID());
        if (response.getWifiChannelNumber() != Defines.defaultIntValue) wifiDataListener.onWifiChannelNumber_Received(response.getWifiChannelNumber());
        if (response.getWifiFrequency() != Defines.defaultIntValue) wifiDataListener.onWifiFrequency_Received(response.getWifiFrequency());
        if (Defines.defaultFloatValue < response.getUserVind()) wifiDataListener.onUserVind_Received(response.getUserVind());
        if (Defines.defaultFloatValue < response.getUserCamLasX()) wifiDataListener.onUserCamLasX_Received(response.getUserCamLasX());
        if (Defines.defaultFloatValue < response.getUserCamLasY()) wifiDataListener.onUserCamLasY_Received(response.getUserCamLasY());
        if (Defines.defaultFloatValue < response.getSensitiveMode()) wifiDataListener.onSensitiveMode_Received(response.getSensitiveMode());
    }

    private void extractDataFromImageResponse(ResponseImage response) {
        final String METHODTAG = ".extractDataFromImageResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        try {
            byte[] image = response.getImageBytes();
            if (wifiDataListener != null) wifiDataListener.onImageData_Received(image);
        } catch (IllegalArgumentCheckedException e) {
            Log.e(CLASSTAG, METHODTAG, e);
        }
    }

    private void extractDataFromWifiResponseObject(ResponseWifiMeasurements response){
        final String METHODTAG = "extractDataFromWifiResponseObject";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        if (wifiDataListener == null) return;

        MeasuredValue data = response.getDistanceValue();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue && !data.getUnitStr().equals(Defines.defaultStringValue)) {
            wifiDataListener.onDistance_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
        }

        data = response.getHorizontalAngleWithTilt_HZ();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue && !data.getUnitStr().equals(Defines.defaultStringValue)) {
            wifiDataListener.onHorizontalAngleWithTilt_hz_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
        }

        data = response.getVerticalAngleWithTilt_V();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue && !data.getUnitStr().equals(Defines.defaultStringValue)) {
            wifiDataListener.onVerticalAngleWithTilt_v_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
        }

        data = response.getHorizontalAngleWithoutTilt_NI_HZ();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue) {
            wifiDataListener.onHorizontalAngleWithoutTilt_ni_hz_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
        }

        data = response.getVerticalAngleWithoutTilt_NI_V();
        if (data != null && data.getConvertedValue() != Defines.defaultFloatValue) {
            wifiDataListener.onVerticalAngleWithoutTilt_ni_hz_Received(data.getConvertedValueStrNoUnit(), data.getUnitStr());
        }

        if (response.getICross() != Defines.defaultFloatValue) wifiDataListener.onICross_Received(response.getICross());
        if (response.getIhz() != Defines.defaultFloatValue) wifiDataListener.onIhz_Received(response.getIhz());
        if (response.getILen() != Defines.defaultFloatValue) wifiDataListener.onIlen_Received(response.getILen());

        wifiDataListener.onIState_Received(response.getIState());
    }

    private void extractDataFromPlainResponse(ResponsePlain response) {
        final String METHODTAG = "extractDataFromPlainResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        if (wifiDataListener != null) {
            wifiDataListener.onPlainData_Received(response.getReceivedDataString());
        }
    }

    /////////////////////////////////////////////////////////////
    // CLEANING
    /////////////////////////////////////////////////////////////
    @Override
    public void onDestroy() {
        super.onDestroy();
        String METHODTAG = "cleanThreads";
        if (setDeviceInfoThread != null) {
            setDeviceInfoThread.interrupt();
            setDeviceInfoThread = null;
            setDeviceInfoHandler = null;
        }
        Log.i(CLASSTAG, String.format("%s setDeviceInfoThread cleaned..", METHODTAG));
    }

    /////////////////////////////////////////////////////////////
    // EXTRA
    /////////////////////////////////////////////////////////////
    @Override
    public String[] getAvailableCommands(){
        if (currentDevice == null) return new String[0];
        ArrayList<String> commandsTemp = new ArrayList<>();
        for (String command : currentDevice.getAvailableCommands()) {
            if (!"GetSoftwareVersion".equals(command) && !"GetSerialNumber".equals(command)) {
                commandsTemp.add(command);
            }
        }
        return commandsTemp.toArray(new String[] {});
    }

    /** Show Wi-Fi turn on dialog (if not already shown) */
    synchronized private void requestWifiTurnOnDialog() {
        final String METHODTAG = ".requestWifiTurnOnDialog";
        Log.d(CLASSTAG, String.format("%s: turnOnWifiDialogIsShown is %s", METHODTAG, turnOnAdapterDialogIsShown));
        if (turnOnAdapterDialogIsShown) return;
        if (requestListener != null) requestListener.onRequestWifiTurnOn();
    }
}
