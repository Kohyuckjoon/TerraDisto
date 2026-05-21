package com.terra.terradisto.distosdkapp.utilities;

import android.util.Log;

import com.terra.terradisto.distosdkapp.device.YetiDeviceController;
import ch.leica.sdk.ErrorHandling.ErrorObject;


public class Logs {

    /**
     * ClassName
     */
    private static final String CLASSTAG = Logs.class.getSimpleName();


    public static void logDataReceived(String METHODTAG, String id, String data1){
        Log.d(
                CLASSTAG,
                String.format(
                        "%s: called with id: %s, value: %s",
                        METHODTAG,
                        id,
                        data1
                )
        );
    }

    public static void logMeasurementReceived(String METHODTAG,
                                              String id,
                                              String data1,
                                              String data2){

        Log.d(
                CLASSTAG,
                String.format(
                        "%s: Measurement - %s, value: %s, Unit: %s",
                        METHODTAG,
                        id,
                        data1,
                        data2
                )
        );

    }


    public static void logBasicMeasurement(String CLASSMETHODTAG,
                                           YetiDeviceController.BasicData basicData) {

        Log.d(
                CLASSTAG,
                String.format(
                        "%s: Basic Measurement:  \n" +
                                "Distance: %s, Unit: %s \n" +
                                "Inclination: %s Unit: %s \n" +
                                "Direction: %s, Unit: %s \n" +
                                "timestamp: %s",
                        CLASSMETHODTAG,
                        basicData.distance,
                        basicData.distanceUnit,
                        basicData.inclination,
                        basicData.inclinationUnit,
                        basicData.direction,
                        basicData.directionUnit,
                        basicData.timestamp
                )
        );
    }

    public static void logP2PData(String CLASSMETHODTAG,
                                  YetiDeviceController.P2PData p2PData) {


        Log.d(
                CLASSTAG,
                String.format(
                        "%s: P2P Measurement:  \n" +
                                "hz: %s, Unit: %s \n" +
                                "ve: %s Unit: %s \n" +
                                "inclinationStatus: %s \n" +
                                "timestamp: %s",
                        CLASSMETHODTAG,
                        p2PData.hzValue,
                        p2PData.hzUnit,
                        p2PData.veValue,
                        p2PData.veUnit,
                        p2PData.inclinationStatus,
                        p2PData.timestamp
                )
        );
    }

    public static void logQuaternionData(String CLASSMETHODTAG,
                                         YetiDeviceController.QuaternionData quaternionData) {


        Log.d(
                CLASSTAG,
                String.format(
                        "%s: Quaternion Measurement:  \n" +
                                "quaternionX: %s \n" +
                                "quaternionY: %s \n" +
                                "quaternionZ: %s \n" +
                                "quaternionW; %s \n" +
                                "timestamp: %s",
                        CLASSMETHODTAG,
                        quaternionData.quaternionX,
                        quaternionData.quaternionY,
                        quaternionData.quaternionZ,
                        quaternionData.quaternionW,
                        quaternionData.timestamp
                )
        );
    }


    public static void logAccRotationData(String CLASSMETHODTAG,
                                          YetiDeviceController.AccRotData accRotData) {


        Log.d(
                CLASSTAG,
                String.format(
                        "%s: AccRotation Measurement:  \n" +
                                "accelerationX: %s \n" +
                                "accelerationY: %s \n" +
                                "accelerationZ: %s \n" +
                                "quaternionW; %s \n" +
                                "accSensitivity; %s \n" +
                                "rotationX; %s \n" +
                                "rotationY; %s \n" +
                                "rotationZ; %s \n" +
                                "rotationSensitivity; %s \n" +
                                "timestamp: %s",
                        CLASSMETHODTAG,
                        accRotData.accelerationX,
                        accRotData.accelerationY,
                        accRotData.accelerationZ,
                        accRotData.accSensitivity,
                        accRotData.rotationX,
                        accRotData.rotationY,
                        accRotData.rotationZ,
                        accRotData.rotationSensitivity,
                        accRotData.timestamp
                )
        );
    }

    public static void logMagnetometerData(String CLASSMETHODTAG,
                                           YetiDeviceController.MagnetometerData magnetometerData) {

        Log.d(
                CLASSTAG,
                String.format(
                        "%s: MagnetometerData Measurement:  \n" +
                                "magnetometerX: %s \n" +
                                "magnetometerY: %s \n" +
                                "magnetometerZ: %s \n" +
                                "timestamp: %s",
                        CLASSMETHODTAG,
                        magnetometerData.magnetometerX,
                        magnetometerData.magnetometerY,
                        magnetometerData.magnetometerZ,
                        magnetometerData.timestamp
                )
        );
    }


    public static void logErrorObject(String CLASSTAGORIG, String METHODTAGORIG,ErrorObject error){

        Log.e(
                CLASSTAG,
                String.format(
                        "%s: response. %s",
                        getClassMethodTag(CLASSTAGORIG, METHODTAGORIG),
                        error.toString()
                )
        );
    }

    private static String getClassMethodTag(String CLASSTAGORIG, String METHODTAGORIG){
        return String.format("%s.%s", CLASSTAGORIG, METHODTAGORIG);
    }

    public static void logNullValues(String CLASSTAGORIG,
                                     String METHODTAGORIG,
                                     String variable) {

        String METHODTAG = "logNullValues";
        Log.e(
                CLASSTAG,
                String.format(
                        "%s: Invalid NULL Value has been found in: %s for Variable %s",
                        METHODTAG,
                        getClassMethodTag(CLASSTAGORIG, METHODTAGORIG),
                        variable
                ));

    }

    public static void logAvailableCommands(String CLASSTAGORIG,
                                            String METHODTAGORIG,
                                            String availableCommandsString) {
        String METHODTAG = "logAvailableCommands";
        Log.d(CLASSTAG,
                String.format(
                        "%s: %s Available Commands: %s",
                        METHODTAG,
                        getClassMethodTag(CLASSTAGORIG, METHODTAGORIG),
                        availableCommandsString
                )
        );
    }

    public static void logException(String CLASSTAGORIG, String METHODTAGORIG, Exception exception) {

        String METHODTAG = ".logException";
        Log.e(CLASSTAG,
                String.format(
                        "%s: %s Message: %s",
                        METHODTAG,
                        getClassMethodTag(CLASSTAGORIG, METHODTAGORIG),
                        exception.getMessage()
                )
        );

    }
}
