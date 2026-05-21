package com.terra.terradisto.distosdkapp.update;

import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import com.terra.terradisto.distosdkapp.utilities.ErrorController;
import com.terra.terradisto.distosdkapp.utilities.Logs;

import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.YetiDevice;
import ch.leica.sdk.ErrorHandling.DeviceException;
import ch.leica.sdk.ErrorHandling.ErrorDefinitions;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.commands.response.ResponseUpdate;
import ch.leica.sdk.update.FirmwareUpdate.DataClasses.FirmwareBinary;
import ch.leica.sdk.update.FirmwareUpdate.DataClasses.FirmwareComponent;
import ch.leica.sdk.update.FirmwareUpdate.DataClasses.FirmwareUpdate;
import ch.leica.sdk.update.UpdateFirmwareDeviceHelper;

public class UpdateController implements YetiDevice.UpdateDeviceListener {

    final static String CLASSTAG = UpdateController.class.getSimpleName();
    private static final int UPDATE_ERROR_CODE = 20000;

    public enum UpdateRegion { device, components, both }
    public enum UpdateConn { online, offline }

    public interface UpdateProcessListener{
        void requestDismissUpdateProgressDialog();
        void requestUpdateConnectionSelectorDialog();
        void requestRegionSelectorDialog(String message, boolean hasDeviceUpdate, boolean hasComponentsUpdate);
        void requestUpdateProgressDialog(String title);
        void onUpdateError(ErrorObject errorObject);
    }

    private UpdateProcessListener updateReinstallProcessListener;

    FirmwareUpdate fwOnDisk;
    FirmwareUpdate fwOnline;
    FirmwareUpdate fwToUse;

    private YetiDevice.UpdateDeviceListener updateDeviceListener;

    // NO-OP listener to avoid NPE when caller doesn't implement YetiDevice.UpdateDeviceListener
    private static final YetiDevice.UpdateDeviceListener NO_OP_UPDATE_DEVICE_LISTENER =
            new YetiDevice.UpdateDeviceListener() {
                @Override public void onFirmwareUpdateStarted(String filename, String version) { }
                @Override public void onProgress(long bytesSent, long bytesTotalNumber) { }
            };

    // ✅ 권장 생성자: 프로세스 UI 콜백만 필수, 펌웨어 진행 콜백은 선택적으로(if implemented) 사용
    public UpdateController(UpdateProcessListener listener){
        this.updateReinstallProcessListener = listener;
        if (listener instanceof YetiDevice.UpdateDeviceListener) {
            this.updateDeviceListener = (YetiDevice.UpdateDeviceListener) listener;
        } else {
            this.updateDeviceListener = NO_OP_UPDATE_DEVICE_LISTENER;
        }
    }

    // (선택) 별도 콜백으로 받는 생성자도 제공
    public UpdateController(UpdateProcessListener processListener,
                            YetiDevice.UpdateDeviceListener deviceListener) {
        this.updateReinstallProcessListener = processListener;
        this.updateDeviceListener = (deviceListener != null) ? deviceListener : NO_OP_UPDATE_DEVICE_LISTENER;
    }

    public void startUpdateProcess(final YetiDevice yetiDevice,
                                   final Context context,
                                   final String deviceAPPSoftwareVersion) {

        final String METHODTAG = "startUpdateProcess";

        if (yetiDevice == null) return;
        Device.ConnectionState connectionState = yetiDevice.getConnectionState();
        if (!connectionState.equals(Device.ConnectionState.connected)) {
            Log.e("Error", String.format("%s: device is not connected. cannot update fw", METHODTAG));
            return;
        }

        updateReinstallProcessListener.requestUpdateProgressDialog("Update Device:");

        Runnable r = new Runnable() {
            @Override
            public void run() {

                boolean fwAvailableOffline = false;
                boolean fwAvailableOnline = false;

                if (deviceAPPSoftwareVersion != null) {
                    OfflineFirmwareUpdateHelper offlineHelper = new OfflineFirmwareUpdateHelper();
                    fwOnDisk = offlineHelper.getNextFirmwareUpdate(
                            yetiDevice.getDeviceID(),
                            deviceAPPSoftwareVersion,
                            context.getApplicationContext()
                    );

                    if (fwOnDisk != null) {
                        fwAvailableOffline = ((fwOnDisk.binaries != null && fwOnDisk.binaries.size() > 0)
                                || (fwOnDisk.components != null && fwOnDisk.components.size() > 0));
                    }

                    try {
                        fwOnline = yetiDevice.getAvailableFirmwareUpdate();
                        if (fwOnline != null) {
                            fwAvailableOnline = ((fwOnline.binaries != null && fwOnline.binaries.size() > 0)
                                    || (fwOnline.components != null && fwOnline.components.size() > 0));
                        }

                        if (fwAvailableOffline && fwAvailableOnline) {
                            updateReinstallProcessListener.requestUpdateConnectionSelectorDialog();

                        } else if (fwAvailableOffline) {
                            handleAvailableFirmwareUpdate(context.getApplicationContext(), true, deviceAPPSoftwareVersion, yetiDevice);

                        } else if (fwAvailableOnline) {
                            handleAvailableFirmwareUpdate(context.getApplicationContext(), false, deviceAPPSoftwareVersion, yetiDevice);

                        } else {
                            String errorMessage = "";
                            if (fwOnline != null) {
                                for (ErrorObject error : fwOnline.errors) {
                                    errorMessage = String.format("%s code: %s : %s", errorMessage, error.getErrorCode(), error.getErrorMessage());
                                }
                                ErrorObject error = new ErrorObject(UPDATE_ERROR_CODE, errorMessage);
                                updateReinstallProcessListener.onUpdateError(error);
                            }
                        }
                    } catch (DeviceException e) {
                        String errorMessage = "Not Able to get the Device State.";
                        updateReinstallProcessListener.onUpdateError(ErrorController.createErrorObject(UPDATE_ERROR_CODE, errorMessage));
                        Logs.logException(CLASSTAG, METHODTAG, e);
                        Log.e(CLASSTAG, String.format("%s: Not Able to get the Device State.", METHODTAG), e);
                    } finally {
                        updateReinstallProcessListener.requestDismissUpdateProgressDialog();
                    }
                }
            }
        };

        new Thread(r).start();
    }

    public void handleAvailableFirmwareUpdate(final Context context,
                                              final boolean offlineFirmwareUpdateSelected,
                                              String appSoftwareVersion,
                                              final YetiDevice currentDevice) {

        final String METHODTAG = "handleAvailableFirmwareUpdate";
        fwToUse = null;

        if(offlineFirmwareUpdateSelected){
            if(fwOnDisk == null) {
                ErrorObject error = ErrorController.createErrorObject(ErrorController.NULL_OBJECT_CODE, "Null fwOnDisk Object");
                Logs.logErrorObject(CLASSTAG, METHODTAG, error);
                Log.w("FirmwareUpdate", "fwOnDisk - This should never happen");
                return;
            }else{
                fwToUse = fwOnDisk;
            }
        }else{
            if(fwOnline == null) {
                ErrorObject error = ErrorController.createErrorObject(ErrorController.NULL_OBJECT_CODE, "Null fwOnline Object");
                Logs.logErrorObject(CLASSTAG, METHODTAG, error);
                Log.w("FirmwareUpdate", " fwOnline - This should never happen");
                return;
            }else{
                fwToUse = fwOnline;
            }
        }

        final UpdateFirmwareDeviceHelper updateFirmwareDeviceHelper = new UpdateFirmwareDeviceHelper();

        if ((fwToUse.getBinaries() == null || fwToUse.getBinaries().size() < 1)
                && (fwToUse.getComponents() == null || fwToUse.getComponents().size() < 1)) {

            updateReinstallProcessListener.requestDismissUpdateProgressDialog();

            for (ErrorObject error : fwToUse.errors) {
                updateReinstallProcessListener.onUpdateError(
                        ErrorController.createErrorObject(
                                error.getErrorCode(),
                                error.getErrorMessage()
                        )
                );
            }
            return;
        }

        OfflineFirmwareUpdateHelper offlineHelper = new OfflineFirmwareUpdateHelper();
        if(appSoftwareVersion.isEmpty()) {
            appSoftwareVersion = fwToUse.forCurrentVersion;
        }

        String jsonResult =
                offlineHelper.saveNextFirmwareUpdate(
                        fwToUse,
                        currentDevice.getDeviceID(),
                        appSoftwareVersion,
                        context.getApplicationContext()
                );

        if (jsonResult == null) {
            ErrorObject error =
                    new ErrorObject(
                            ErrorDefinitions.UPDATE_UNABLE_TO_SAVEDATA_CODE,
                            ErrorDefinitions.UPDATE_UNABLE_TO_SAVEDATA_MESSAGE
                    );

            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
            Log.e("fwUpdate", "save fwUpdate object failed");
            updateReinstallProcessListener.requestDismissUpdateProgressDialog();
            updateReinstallProcessListener.onUpdateError(error);
        }

        final FirmwareUpdate fwUpdateToUse = fwToUse;
        updateReinstallProcessListener.requestDismissUpdateProgressDialog();

        String deviceUpdatename = (fwUpdateToUse.getName() == null) ? "Not Available." : fwUpdateToUse.getName();
        String deviceUpdateNextVersion = (fwUpdateToUse.getVersion() == null) ? "Not Available." : fwUpdateToUse.getVersion();
        boolean hasDeviceUpdate = (fwUpdateToUse.getBinaries() != null && fwUpdateToUse.getBinaries().size() > 0);

        boolean hasComponentsUpdate = false;
        if (fwUpdateToUse.getComponents() != null && fwUpdateToUse.getComponents().size() > 0) {
            List<FirmwareComponent> fwComponents = fwUpdateToUse.getComponents();
            Iterator<FirmwareComponent> iterator = fwComponents.iterator();
            while (iterator.hasNext()) {
                FirmwareComponent fwComponent = iterator.next();
                boolean isComponentConnected = updateFirmwareDeviceHelper.isComponentConnected(
                        currentDevice,
                        fwComponent.getSerialCommand()
                );
                if (!isComponentConnected) {
                    iterator.remove();
                }
            }
            if (fwComponents.size() > 0) hasComponentsUpdate = true;
        }

        String updateTypeIndicator = offlineFirmwareUpdateSelected ? "offline" : "online";
        String message = "";
        message = String.format("%sYETI Brand: %s", message, fwUpdateToUse.getBrandIdentifier());
        message = String.format("%s (%s)\n", message, updateTypeIndicator);

        if (hasDeviceUpdate) {
            message = String.format("%s Firmware found for Device \n\n Name: %s\n Next Version: %s\n", message, deviceUpdatename, deviceUpdateNextVersion);
            for (FirmwareBinary fwBinary : fwUpdateToUse.getBinaries()) {
                message = String.format("%sFiles: %s\n", message, fwBinary.getCommand());
            }
        }

        if (hasComponentsUpdate) {
            message = String.format("%s\n Firmware found for COMPONENTS \n", message);
            for (FirmwareComponent fwComponent : fwUpdateToUse.getComponents()) {
                message = String.format("%sComponent: %s\nName: %s\nVersion: %s\n", message, fwComponent.getIdentifier(), fwComponent.getName(), fwComponent.getCurrentVersion());
            }
        }

        if (!offlineFirmwareUpdateSelected) {
            updateReinstallProcessListener.requestRegionSelectorDialog(message, hasDeviceUpdate, hasComponentsUpdate);
        } else {
            runUpdateProcess(currentDevice);
        }
    }

    public void runUpdateProcess(final YetiDevice currentDevice)  {
        final String METHODTAG = ".launchUpdate";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateReinstallProcessListener.requestDismissUpdateProgressDialog();
                    updateReinstallProcessListener.requestUpdateProgressDialog("Transferring data to Device: ");

                    if(fwToUse != null) {
                        ResponseUpdate responseUpdate =
                                currentDevice.updateDeviceFirmwareWithFirmwareUpdate(
                                        fwToUse,
                                        UpdateController.this
                                );

                        if (responseUpdate == null) {
                            updateReinstallProcessListener.onUpdateError(
                                    new ErrorObject(ErrorDefinitions.UPDATE_FIRMWARE_FAIL_CODE, ErrorDefinitions.UPDATE_FIRMWARE_FAIL_MESSAGE)
                            );
                            updateReinstallProcessListener.requestDismissUpdateProgressDialog();

                        } else if (responseUpdate.getError() != null) {
                            updateReinstallProcessListener.onUpdateError(responseUpdate.getError());
                            updateReinstallProcessListener.requestDismissUpdateProgressDialog();

                        } else {
                            String title = "Update Successful";
                            updateReinstallProcessListener.requestDismissUpdateProgressDialog();
                            updateReinstallProcessListener.requestUpdateProgressDialog(title);
                        }
                    } else {
                        updateReinstallProcessListener.onUpdateError(
                                new ErrorObject(
                                        ErrorDefinitions.UPDATE_FIRMWARE_FAIL_CODE,
                                        ErrorDefinitions.UPDATE_FIRMWARE_FAIL_MESSAGE
                                )
                        );
                    }
                } catch (DeviceException e) {
                    Logs.logException(CLASSTAG, METHODTAG, e);
                    updateReinstallProcessListener.onUpdateError(
                            new ErrorObject(
                                    ErrorDefinitions.UPDATE_FIRMWARE_FAIL_CODE,
                                    ErrorDefinitions.UPDATE_FIRMWARE_FAIL_MESSAGE
                            )
                    );
                }
            }
        }).start();
    }

    public void startReinstallProcess(final YetiDevice yetiDevice,
                                      final String deviceAPPSoftwareVersion) {

        final String METHODTAG = "startReinstallProcess";
        if (yetiDevice == null) return;

        Device.ConnectionState connectionState = yetiDevice.getConnectionState();
        if (!connectionState.equals(Device.ConnectionState.connected)) {
            Log.e("Error", String.format("%s: device is not connected. cannot update fw", METHODTAG));
            return;
        }

        updateReinstallProcessListener.requestUpdateProgressDialog("Reinstall Device:");

        Runnable r = new Runnable() {
            @Override
            public void run() {
                updateReinstallProcessListener.requestDismissUpdateProgressDialog();
                updateReinstallProcessListener.requestUpdateProgressDialog("Reinstall Device:");
                handleAvailableFirmwareReinstall(deviceAPPSoftwareVersion, yetiDevice);
                updateReinstallProcessListener.requestDismissUpdateProgressDialog();
            }
        };

        new Thread(r).start();
    }

    void handleAvailableFirmwareReinstall(String appSoftwareVersion, final YetiDevice yetiDevice) {
        final String METHODTAG = "handleAvailableFwUpdate";

        final FirmwareUpdate fwReinstall;
        boolean hasReinstall = false;
        try {
            fwReinstall = yetiDevice.getReinstallFirmware();
            if (fwReinstall == null) {
                updateReinstallProcessListener.requestDismissUpdateProgressDialog();
                updateReinstallProcessListener.onUpdateError(ErrorController
                        .createErrorObject(
                                ErrorDefinitions.UPDATE_UNMAPPED_ERROR_CODE,
                                ErrorDefinitions.UPDATE_UNMAPPED_ERROR_MESSAGE
                        )
                );
                Log.w("FirmwareUpdate", "This should never happen");
                return;
            }

            if ((fwReinstall.binaries != null && fwReinstall.binaries.size() > 0)
                    || (fwReinstall.components != null && fwReinstall.components.size() > 0)) {
                hasReinstall = true;
            } else {
                String errorMessage = "";
                if (fwOnline != null) {
                    for (ErrorObject error : fwOnline.errors) {
                        errorMessage = String.format("There is no reinstall. %s code: %s : %s", errorMessage, error.getErrorCode(), error.getErrorMessage());
                    }
                    ErrorObject error = new ErrorObject(UPDATE_ERROR_CODE, errorMessage);
                    updateReinstallProcessListener.onUpdateError(error);
                } else {
                    ErrorObject error = new ErrorObject(UPDATE_ERROR_CODE, "There is no reinstall.");
                    updateReinstallProcessListener.onUpdateError(error);
                }
            }

            if(hasReinstall) {
                final UpdateFirmwareDeviceHelper updateFirmwareDeviceHelper = new UpdateFirmwareDeviceHelper();

                if ((fwReinstall.getBinaries() == null || fwReinstall.getBinaries().size() < 1)
                        && (fwReinstall.getComponents() == null || fwReinstall.getComponents().size() < 1)) {

                    updateReinstallProcessListener.requestDismissUpdateProgressDialog();
                    for (ErrorObject error : fwReinstall.errors) {
                        updateReinstallProcessListener.onUpdateError(error);
                    }
                    return;
                }

                updateReinstallProcessListener.requestDismissUpdateProgressDialog();

                String deviceUpdatename = (fwReinstall.getName() == null) ? "Not Available." : fwReinstall.getName();
                String deviceUpdateNextVersion = (fwReinstall.getVersion() == null) ? "Not Available." : fwReinstall.getVersion();
                boolean hasDeviceUpdate = (fwReinstall.getBinaries() != null && fwReinstall.getBinaries().size() > 0);

                boolean hasComponentsUpdate = false;
                if (fwReinstall.getComponents() != null && fwReinstall.getComponents().size() > 0) {
                    List<FirmwareComponent> fwComponents = fwReinstall.getComponents();
                    Iterator<FirmwareComponent> iterator = fwComponents.iterator();
                    while (iterator.hasNext()) {
                        FirmwareComponent fwComponent = iterator.next();
                        boolean isComponentConnected = updateFirmwareDeviceHelper.isComponentConnected(
                                yetiDevice,
                                fwComponent.getSerialCommand()
                        );
                        if (!isComponentConnected) iterator.remove();
                    }
                    if (fwComponents.size() > 0) hasComponentsUpdate = true;
                }

                String message = "";
                message = String.format("%sYETI Brand: %s", message, fwReinstall.getBrandIdentifier());
                message = String.format("%s ( Online )\n", message);

                if (hasDeviceUpdate) {
                    message = String.format("%s Firmware found for Device \n\n Name: %s\n Current Version: %s\n", message, deviceUpdatename, deviceUpdateNextVersion);
                    for (FirmwareBinary fwBinary : fwReinstall.getBinaries()) {
                        message = String.format("%sFiles: %s\n", message, fwBinary.getCommand());
                    }
                }

                if (hasComponentsUpdate) {
                    message = String.format("%s\n Firmware found for COMPONENTS \n", message);
                    for (FirmwareComponent fwComponent : fwReinstall.getComponents()) {
                        message = String.format("%sComponent: %s\nName: %s\nVersion: %s\n", message, fwComponent.getIdentifier(), fwComponent.getName(), fwComponent.getCurrentVersion());
                    }
                }

                this.fwToUse = fwReinstall;
                updateReinstallProcessListener.requestRegionSelectorDialog(message, hasDeviceUpdate, hasComponentsUpdate);
            }
        } catch (DeviceException e) {
            Log.d(METHODTAG, String.format("Error reinstalling the device: %s", e.getMessage()),e);
        }
    }

    public ErrorObject clearComponents() {
        String METHODTAG = "clearComponents";
        ErrorObject error = null;
        if(fwToUse!=null) {
            fwToUse.components = null;
        } else {
            error = ErrorController.createErrorObject(
                    ErrorController.NULL_OBJECT_CODE,
                    "Trying to access null object: fwToUse. Method should be called after selecting (online/offline/both) and (device/component/both)"
            );
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
        }
        return error;
    }

    public ErrorObject clearBinaries() {
        String METHODTAG = ".clearComponents";
        ErrorObject error = null;
        if(fwToUse!=null) {
            fwToUse.binaries = null;
        } else {
            error = ErrorController.createErrorObject(
                    ErrorController.NULL_OBJECT_CODE,
                    "Trying to access null object: fwToUse. Method should be called after selecting (online/offline/both) and (device/component/both)"
            );
            Logs.logErrorObject(CLASSTAG, METHODTAG, error);
        }
        return error;
    }

    // YetiDevice.UpdateDeviceListener (delegate -> updateDeviceListener)
    @Override public void onFirmwareUpdateStarted(String filename, String version) {
        if (updateDeviceListener != null) updateDeviceListener.onFirmwareUpdateStarted(filename,version);
    }

    @Override public void onProgress(long bytesSent, long bytesTotalNumber) {
        if (updateDeviceListener != null) updateDeviceListener.onProgress(bytesSent, bytesTotalNumber);
    }
}
