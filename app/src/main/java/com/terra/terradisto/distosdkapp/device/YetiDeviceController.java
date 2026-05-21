package com.terra.terradisto.distosdkapp.device;

import android.content.Context;
import android.util.Log;

import com.terra.terradisto.distosdkapp.update.UpdateController;
import com.terra.terradisto.distosdkapp.utilities.ErrorController;
import com.terra.terradisto.distosdkapp.utilities.Logs;

import ch.leica.sdk.Defines;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.YetiDevice;
import ch.leica.sdk.ErrorHandling.DeviceException;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.ErrorHandling.WrongDataException;
import ch.leica.sdk.Types;
import ch.leica.sdk.commands.MeasuredValueYeti;
import ch.leica.sdk.commands.MeasurementConverter;
import ch.leica.sdk.commands.ReceivedData;
import ch.leica.sdk.commands.ReceivedYetiDataPacket;
import ch.leica.sdk.commands.response.Response;
import ch.leica.sdk.commands.response.ResponsePlain;

public class YetiDeviceController
        extends BleDeviceController
        implements UpdateController.UpdateProcessListener {

    private String appSoftwareVersion;

    // DATA MODELS
    public class BasicData { public String distance=""; public String distanceUnit=""; public String inclination=""; public String inclinationUnit=""; public String direction=""; public String directionUnit=""; public String timestamp=""; }
    public class P2PData { public String hzValue=""; public String hzUnit=""; public String veValue=""; public String veUnit=""; public String inclinationStatus=""; public String timestamp; }
    public class QuaternionData { public String quaternionX=""; public String quaternionY=""; public String quaternionZ=""; public String quaternionW=""; public String timestamp=""; }
    public class AccRotData { public String accelerationX=""; public String accelerationY=""; public String accelerationZ=""; public String accSensitivity=""; public String rotationX=""; public String rotationY=""; public String rotationZ=""; public String rotationSensitivity=""; public String timestamp=""; }
    public class MagnetometerData { public String magnetometerX=""; public String magnetometerY=""; public String magnetometerZ=""; public String timestamp=""; }

    // LISTENER
    public interface YetiDataListener {
        void onBasicMeasurements_Received(BasicData basicData);
        void onP2PMeasurements_Received(P2PData p2pData);
        void onQuaternionMeasurement_Received(QuaternionData quaternionData);
        void onAccRotationMeasurement_Received(AccRotData accRotatonMeasurement);
        void onMagnetometerMeasurement_Received(MagnetometerData magnetometerData);
        void onDistocomTransmit_Received(String data);
        void onDistocomEvent_Received(String data);
        void onBrand_Received(String data);
        void onAPPSoftwareVersion_Received(String data);
        void onId_Received(String data);
        void onEDMSoftwareVersion_Received(String data);
        void onFTASoftwareVersion_Received(String data);
        void onAPPSerial_Received(String data);
        void onEDMSerial_Received(String data);
        void onFTASerial_Received(String data);
        void onModel_Received(String data);
    }

    private static final String CLASSTAG = YetiDeviceController.class.getSimpleName();

    private YetiDataListener yetiDataListener;
    private ErrorObject errorDeviceInfo;
    private UpdateController.UpdateProcessListener updateProcessListener;
    private UpdateController updateController;

    // NO-OP update listener
    private static final UpdateController.UpdateProcessListener NO_OP_UPDATE_LISTENER =
            new UpdateController.UpdateProcessListener() {
                @Override public void requestUpdateProgressDialog(String title) {}
                @Override public void requestDismissUpdateProgressDialog() {}
                @Override public void requestUpdateConnectionSelectorDialog() {}
                @Override public void onUpdateError(ErrorObject error) {}
                @Override public void requestRegionSelectorDialog(String message, boolean hasDeviceUpdate, boolean hasComponentsUpdate) {}
            };

    // NO-OP status listener
    private static final DeviceStatusListener NO_OP_DEVICE_STATUS_LISTENER =
            new DeviceStatusListener() {
                @Override public void onConnectionStateChanged(String deviceId, Device.ConnectionState state) {}
                @Override public void onStatusChange(String status) {}
                @Override public void onReconnect() {}
                @Override public void onError(int code, String message) {}
            };

    // 일반화된 생성자
    public YetiDeviceController(Context context,
                                YetiDataListener dataListener,
                                UpdateController.UpdateProcessListener updateListener,
                                DeviceStatusListener statusListener) {
        super(context,
                null, // BLE 데이터 리스너는 Yeti 용으로 따로 처리하므로 사용 안 함
                null, // RequestListener 사용 안 함(필요시 확장)
                (statusListener != null) ? statusListener : NO_OP_DEVICE_STATUS_LISTENER);
        this.yetiDataListener = dataListener;
        this.updateProcessListener = (updateListener != null) ? updateListener : NO_OP_UPDATE_LISTENER;
        this.turnOnAdapterDialogIsShown = false;
        this.updateController = new UpdateController(this);
    }

    // 편의 생성자 (프래그먼트에서 Update/Status 콜백 없이 사용)
    public YetiDeviceController(Context context, YetiDataListener dataListener) {
        this(context, dataListener, null, null);
    }

    // 레거시 액티비티 생성자 (점진 전환용)
    @Deprecated
    public YetiDeviceController(com.terra.terradisto.distosdkapp.activities.YetiInformationActivity activity) {
        this(activity.getApplicationContext(),
                activity,
                activity,
                (activity instanceof DeviceStatusListener) ? (DeviceStatusListener) activity : NO_OP_DEVICE_STATUS_LISTENER);
    }

    // COMMANDS
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
            response = currentDevice.sendCustomCommand(command);
            response.waitForData();
            yetiDataListener.onDistocomTransmit_Received(((ResponsePlain)response).getReceivedDataString());
        } catch (DeviceException e) {
            error = ErrorController.createErrorObject(COMMAND_ERROR_CODE, e.getMessage());
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            response = new Response(Types.Commands.Custom);
            response.setError(error);
        }
        return response;
    }

    public ErrorObject sendDistanceCommand() {
        final String METHODTAG = ".sendDistanceCommand";
        errorSendingCommand = null;

        if (currentDevice == null) {
            errorSendingCommand = ErrorController.createErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
            Logs.logErrorObject(CLASSTAG, METHODTAG, errorSendingCommand);
            return errorSendingCommand;
        }

        try {
            final ResponsePlain response =
                    (ResponsePlain) currentDevice.sendCommand(Types.Commands.DistanceDC);
            response.waitForData();
            yetiDataListener.onDistocomTransmit_Received(response.getReceivedDataString());
        } catch (DeviceException e) {
            errorSendingCommand = ErrorController.createErrorObject(COMMAND_ERROR_CODE, "Error sending the distance command");
            Logs.logErrorObject(CLASSTAG, METHODTAG, errorSendingCommand);
        }
        return errorSendingCommand;
    }

    // DEVICE INFO
    @Override
    public ErrorObject getDeviceInfo() {
        String METHODTAG = ".getDeviceInfo";
        if (currentDevice == null) {
            return new ErrorObject(NULL_DEVICE_CODE, NULL_DEVICE_MESSAGE);
        }

        ResponsePlain response;
        try {
            response = (ResponsePlain) sendCommand(Types.Commands.GetBrandDistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onBrand_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetIDDistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onId_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetSoftwareVersionAPPDistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onAPPSoftwareVersion_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetSoftwareVersionEDMDistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onEDMSoftwareVersion_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetSoftwareVersionFTADistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onFTASoftwareVersion_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetSerialAPPDistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onAPPSerial_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetSerialEDMDistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onEDMSerial_Received(response.getReceivedDataString());

            response = (ResponsePlain) sendCommand(Types.Commands.GetSerialFTADistocom);
            errorDeviceInfo = response.getError();
            yetiDataListener.onFTASerial_Received(response.getReceivedDataString());

        } catch (Exception e) {
            Logs.logException(CLASSTAG, METHODTAG, e);
        }

        return errorDeviceInfo;
    }

    // Async Data
    @Override
    public void onAsyncDataReceived(final ReceivedData receivedData) {
        final String METHODTAG = ".onAsyncDataReceived";
        final String CLASSMETHODTAG = CLASSTAG + "." + METHODTAG;

        Log.v(CLASSTAG, String.format("%s Asynchronous data received.", METHODTAG));
        if (receivedData == null) {
            Log.e(CLASSTAG, String.format("%s: Error: receivedData object is null. Returning.", METHODTAG));
            return;
        }

        if(receivedData.dataPacket instanceof ReceivedYetiDataPacket) {
            ReceivedYetiDataPacket p = (ReceivedYetiDataPacket) receivedData.dataPacket;
            try {
                String id = p.dataId;
                switch (id) {
                    case Defines.ID_DS_MODEL_NAME: {
                        String data = p.getModelName();
                        yetiDataListener.onModel_Received(data);
                    }
                    break;

                    case Defines.ID_IMU_BASIC_MEASUREMENTS: {
                        BasicData basicData = new BasicData();
                        MeasuredValueYeti distanceValue;
                        MeasuredValueYeti inclinationValue;
                        MeasuredValueYeti directionValue;

                        ReceivedYetiDataPacket.YetiBasicMeasurements data = p.getBasicMeasurements();

                        distanceValue = new MeasuredValueYeti(data.getDistance());
                        distanceValue.setUnit(data.getDistanceUnit());
                        distanceValue.convertDistance();
                        basicData.distance = distanceValue.getConvertedValueStrNoUnit();
                        basicData.distanceUnit = distanceValue.getUnitStr();

                        inclinationValue = new MeasuredValueYeti(data.getInclination());
                        inclinationValue.setUnit(data.getInclinationUnit());
                        inclinationValue.convertAngle();
                        basicData.inclination = inclinationValue.getConvertedValueStrNoUnit();
                        basicData.inclinationUnit = inclinationValue.getUnitStr();

                        directionValue = new MeasuredValueYeti(data.getDirection());
                        directionValue.setUnit(data.getDirectionUnit());
                        directionValue.convertAngle();
                        basicData.direction = directionValue.getConvertedValueStrNoUnit();
                        basicData.directionUnit = directionValue.getUnitStr();

                        basicData.timestamp = String.valueOf(data.getTimestampAndFlags());
                        yetiDataListener.onBasicMeasurements_Received(basicData);
                        Logs.logBasicMeasurement(CLASSMETHODTAG, basicData);
                    }
                    break;

                    case Defines.ID_IMU_P2P: {
                        P2PData p2p = new P2PData();
                        ReceivedYetiDataPacket.YetiP2P data = p.getP2P();

                        MeasuredValueYeti HzP2PValue = new MeasuredValueYeti(data.getHzAngle());
                        HzP2PValue.setUnit(MeasurementConverter.getDefaultDirectionAngleUnit());
                        HzP2PValue.convertAngle();
                        p2p.hzValue = HzP2PValue.getConvertedValueStrNoUnit();
                        p2p.hzUnit = HzP2PValue.getUnitStr();

                        MeasuredValueYeti VeP2PValue = new MeasuredValueYeti(data.getVeAngle());
                        VeP2PValue.setUnit(MeasurementConverter.getDefaultDirectionAngleUnit());
                        VeP2PValue.convertAngle();
                        p2p.veValue = VeP2PValue.getConvertedValueStrNoUnit();
                        p2p.veUnit = VeP2PValue.getUnitStr();

                        p2p.inclinationStatus = String.valueOf(data.getInclinationStatus());
                        p2p.timestamp = String.valueOf(data.getTimestampAndFlags());

                        yetiDataListener.onP2PMeasurements_Received(p2p);
                        Logs.logP2PData(CLASSMETHODTAG, p2p);
                    }
                    break;

                    case Defines.ID_IMU_QUATERNION: {
                        ReceivedYetiDataPacket.YetiQuaternion data = p.getQuaternion();
                        QuaternionData q = new QuaternionData();
                        q.quaternionX = String.valueOf(data.getQuaternion_X());
                        q.quaternionY = String.valueOf(data.getQuaternion_Y());
                        q.quaternionZ = String.valueOf(data.getQuaternion_Z());
                        q.quaternionW = String.valueOf(data.getQuaternion_W());
                        q.timestamp = String.valueOf(data.getTimestampAndFlags());
                        yetiDataListener.onQuaternionMeasurement_Received(q);
                        Logs.logQuaternionData(CLASSMETHODTAG, q);
                    }
                    break;

                    case Defines.ID_IMU_ACELERATION_AND_ROTATION: {
                        ReceivedYetiDataPacket.YetiAccelerationAndRotation d = p.getAccelerationAndRotation();
                        AccRotData a = new AccRotData();
                        a.accelerationX = String.valueOf(d.getAcceleration_X());
                        a.accelerationY = String.valueOf(d.getAcceleration_Y());
                        a.accelerationZ = String.valueOf(d.getAcceleration_Z());
                        a.accSensitivity = String.valueOf(d.getAccSensitivity());
                        a.rotationX = String.valueOf(d.getRotation_X());
                        a.rotationY = String.valueOf(d.getRotation_Y());
                        a.rotationZ = String.valueOf(d.getRotation_Z());
                        a.rotationSensitivity = String.valueOf(d.getRotationSensitivity());
                        a.timestamp = String.valueOf(d.getTimestampAndFlags());
                        yetiDataListener.onAccRotationMeasurement_Received(a);
                        Logs.logAccRotationData(CLASSMETHODTAG, a);
                    }
                    break;

                    case Defines.ID_IMU_MAGNETOMETER: {
                        ReceivedYetiDataPacket.YetiMagnetometer d = p.getMagnetometer();
                        MagnetometerData m = new MagnetometerData();
                        m.magnetometerX = String.valueOf(d.getMagnetometer_X());
                        m.magnetometerY = String.valueOf(d.getMagnetometer_Y());
                        m.magnetometerZ = String.valueOf(d.getMagnetometer_Z());
                        m.timestamp = String.valueOf(d.getTimestampAndFlags());
                        yetiDataListener.onMagnetometerMeasurement_Received(m);
                        Logs.logMagnetometerData(CLASSMETHODTAG, m);
                    }
                    break;

                    case Defines.ID_IMU_DISTOCOM_TRANSMIT: {
                        String data = p.getDistocomReceivedMessage();
                        data = data.trim();
                        yetiDataListener.onDistocomTransmit_Received(data);
                    }
                    break;

                    case Defines.ID_IMU_DISTOCOM_EVENT: {
                        String data = p.getDistocomReceivedMessage();
                        yetiDataListener.onDistocomEvent_Received(data);
                        Logs.logDataReceived(CLASSMETHODTAG, id, data);
                    }
                    break;

                    default: {
                        Log.d(CLASSTAG, METHODTAG + ":  Error setting data in the UI");
                    }
                    break;
                }

            } catch (IllegalArgumentCheckedException e) {
                Log.e(CLASSTAG, METHODTAG, e);
            } catch (WrongDataException e) {
                Log.d(CLASSTAG, METHODTAG + " A wrong value has been set into the UI");
            } catch (Exception e) {
                Log.e(CLASSTAG, METHODTAG + " Wrong data was received");
            }
        }
    }

    @Override
    public void readDataFromResponseObject(Response response) {
        final String METHODTAG = ".readDataFromResponseObject";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));

        if (response.getError() != null) {
            Logs.logErrorObject(CLASSTAG, METHODTAG, response.getError());
            return;
        }

        if (response instanceof ResponsePlain) {
            extractDataFromPlainResponse((ResponsePlain) response);
        }
    }

    private void extractDataFromPlainResponse(ResponsePlain response) {
        final String METHODTAG = "extractDataFromPlainResponse";
        Log.v(CLASSTAG, String.format("%s extracting", METHODTAG));
        yetiDataListener.onDistocomTransmit_Received(response.getReceivedDataString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        String METHODTAG = "cleanThreads";
    }

    // UPDATE / REINSTALL
    private String getAppSoftwareVersion() {
        String appSoftwareVersion = "";
        ResponsePlain response = (ResponsePlain) this.sendCommand(Types.Commands.GetSoftwareVersionAPPDistocom);
        if(response.getError() == null){
            appSoftwareVersion = response.getReceivedDataString();
        }
        return appSoftwareVersion;
    }

    @Override
    public ErrorObject startUpdateProcess(Context context) {
        appSoftwareVersion = getAppSoftwareVersion();
        updateController.startUpdateProcess(
                (YetiDevice) currentDevice,
                context.getApplicationContext(),
                appSoftwareVersion
        );
        return null;
    }

    @Override
    public ErrorObject startReinstallProcess(Context context) {
        appSoftwareVersion = getAppSoftwareVersion();
        updateController.startReinstallProcess(
                (YetiDevice) currentDevice,
                appSoftwareVersion
        );
        return null;
    }

    @Override
    public void requestUpdateProgressDialog(String title) {
        updateProcessListener.requestUpdateProgressDialog(title);
    }

    @Override
    public void requestDismissUpdateProgressDialog() {
        updateProcessListener.requestDismissUpdateProgressDialog();
    }

    @Override
    public void requestUpdateConnectionSelectorDialog() {
        this.updateProcessListener.requestUpdateConnectionSelectorDialog();
    }

    @Override
    public void onUpdateError(ErrorObject error) {
        if (this.deviceStatusListener != null) {
            this.deviceStatusListener.onError(error.getErrorCode(), error.getErrorMessage());
        }
    }

    @Override
    public void requestRegionSelectorDialog(String message, boolean hasDeviceUpdate, boolean hasComponentsUpdate) {
        this.updateProcessListener.requestRegionSelectorDialog(message, hasDeviceUpdate, hasComponentsUpdate);
    }

    public void handleAvailableFirmwareUpdate(boolean offlineFirmwareUpdateSelected, Context context) {
        this.updateController.handleAvailableFirmwareUpdate(
                context,
                offlineFirmwareUpdateSelected,
                this.appSoftwareVersion,
                (YetiDevice) this.currentDevice
        );
    }

    public void runUpdateProcess(UpdateController.UpdateRegion updateRegion) {
        ErrorObject error = null;
        if (updateRegion == UpdateController.UpdateRegion.device) {
            error = updateController.clearComponents();
        } else if (updateRegion == UpdateController.UpdateRegion.components) {
            error = updateController.clearBinaries();
        }

        if(error == null){
            updateController.runUpdateProcess((YetiDevice) currentDevice);
        }
    }
}
