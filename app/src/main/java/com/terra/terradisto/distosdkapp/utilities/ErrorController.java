package com.terra.terradisto.distosdkapp.utilities;


import ch.leica.sdk.ErrorHandling.ErrorObject;

public class ErrorController {

    public static final int NULL_OBJECT_CODE = 20101;

    public static ErrorObject createErrorObject(int commandErrorCode, String errorMessage) {

        return new ErrorObject(commandErrorCode, errorMessage);

    }
}
