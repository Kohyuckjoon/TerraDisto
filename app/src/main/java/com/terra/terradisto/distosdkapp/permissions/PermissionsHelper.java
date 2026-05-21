package com.terra.terradisto.distosdkapp.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;

import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;
import ch.leica.sdk.LeicaSdk;

public class PermissionsHelper {

    /**
     * ClassName
     */
    private final String CLASSTAG = PermissionsHelper.class.getSimpleName();

    /**
     * Array of required permissions
     */
    private ArrayList<String> manifestPermission = new ArrayList<>();

    private Activity activity;

    private boolean firstTimeStorage = true;

    private DialogHandler storagePermissionsDialog;

    public PermissionsHelper(Activity activity  ) {
        this.activity = activity;
    }
    /**
     * To check which kind of devices/connection modes should be scanned for
     * E.g. if wifi permission is not given the sdk only scans for bluetooth devices, vice versa
     * The location permission is needed to scan for bluetooth devices
     *
     * @return an array of booleans which represents what can/should be scanned for
     */
    public boolean[] requestNetworkPermissions() {
        final String METHODTAG = ".requestNetworkPermissions";
        Log.d(CLASSTAG, METHODTAG + ": Network permissions requested");

        this.manifestPermission = new ArrayList<>();
        boolean[] permissions = {false, false};

        addPermission(this.activity, 	Manifest.permission.INTERNET);
        addPermission(this.activity, 	Manifest.permission.ACCESS_NETWORK_STATE);
        addPermission(this.activity, 	Manifest.permission.ACCESS_COARSE_LOCATION);
        addPermission(this.activity, 	Manifest.permission.ACCESS_FINE_LOCATION);
        addPermission(this.activity, 	Manifest.permission.ACCESS_WIFI_STATE);
        addPermission(this.activity, 	Manifest.permission.CHANGE_WIFI_STATE);
        addPermission(this.activity, 	Manifest.permission.BLUETOOTH);
        addPermission(this.activity, 	Manifest.permission.BLUETOOTH_ADMIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermission(this.activity, 	Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            addPermission(this.activity, 	Manifest.permission.BLUETOOTH_CONNECT);
            addPermission(this.activity, 	Manifest.permission.BLUETOOTH_SCAN);
        }

        //Convert List to String[]
        String[] manifestPermissionStrArray = new String[this.manifestPermission.size()];


        for (int i = 0; i < manifestPermission.size(); i++) {
            String permission = manifestPermission.get(i);
            manifestPermissionStrArray[i] = permission;
        }

        //Request all the missing permissions
        try {
            if (manifestPermissionStrArray.length > 0) {
                ActivityCompat.requestPermissions(
                        this.activity,
                        manifestPermissionStrArray, 8000
                );
            }
        } catch (Exception e) {

            Log.e(CLASSTAG, String.format("%sPermissions error: ", METHODTAG), e);

        }

        LocationManager lm =
                (LocationManager) this.activity.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean location_enabled = false;


        try {
            if (lm != null) {
                location_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {

            Log.e(CLASSTAG, METHODTAG + "NETWORK PROVIDER, network not enabled", e);

        }

        if (!location_enabled) {

            LeicaSdk.scanConfig.setWifiAdapterOn(false);

            /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            ++
            ++ ActivateLocationServicesDialog
            ++
            ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

            DialogHandler locationServicesDialog = new DialogHandler();
            String title = "Location Services";
            String message = "Please activate Location Services.";
            String positiveButtonText = "Ok";
            Runnable positiveButtonRunnable =
                    new Runnable() {
                        @Override
                        public void run() {

                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            activity.startActivity(myIntent);
                        }
                    };

            String negativeButtonText = "Cancel";
            Runnable negativeButtonRunnable =
                    new Runnable() {
                        @Override
                        public void run() {

                        }
                    };


            locationServicesDialog.setDialog(
                    this.activity,
                    title, message,
                    false,
                    positiveButtonText,
                    positiveButtonRunnable,
                    negativeButtonText,
                    negativeButtonRunnable
            );

            locationServicesDialog.show();

            /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


        }

        verifyPermissions();

        Log.i(
                CLASSTAG,
                String.format(
                        "%s: Permissions: WIFI: %s, BLE: %s",
                        METHODTAG,
                        LeicaSdk.scanConfig.isWifiAdapterOn(),
                        LeicaSdk.scanConfig.isBleAdapterOn()
                )
        );

        return permissions;
    }

    public Boolean requestStoragePermission() {

        // storage permission for "getExternalCacheDir()" only required prior Kitkat
        if(Build.VERSION.SDK_INT  >= Build.VERSION_CODES.KITKAT){
            return true;
        }

        if (ContextCompat.checkSelfPermission(this.activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this.activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this.activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                String title = "NEEDED PERMISSIONS";
                String message = "Read and write permissions are needed to update the device";


                storagePermissionsDialog = new DialogHandler();

                storagePermissionsDialog.setDialog(
                        this.activity,
                        title,
                        message,
                        true
                );
                if(storagePermissionsDialog.isShowing() == true) {
                    storagePermissionsDialog.show();
                }

            }else{
                if(firstTimeStorage == true){
                    firstTimeStorage = false;

                }
                else{
                    return null;
                }

            }

            ActivityCompat.requestPermissions(this.activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},9000);

            return false;
        }

        return true;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 8000: {
                // If request is cancelled, the result arrays are empty.
                verifyPermissions();

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    private void verifyPermissions(){

        boolean bleAdapter = false;
        boolean wifiAdapter = false;

        int accessWifiStatePermStatus =
                ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_WIFI_STATE);
        int changeWifiStatePermStatus =
                ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.CHANGE_WIFI_STATE);
        int bluetoothPermStatus =
                ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.BLUETOOTH);
        int bluetoothAdminPermStatus =
                ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.BLUETOOTH_ADMIN);

        if ( accessWifiStatePermStatus == PackageManager.PERMISSION_GRANTED
                &&  changeWifiStatePermStatus == PackageManager.PERMISSION_GRANTED) {
            wifiAdapter = true;

        }

        if (bluetoothPermStatus == PackageManager.PERMISSION_GRANTED
                && bluetoothAdminPermStatus == PackageManager.PERMISSION_GRANTED
                && this.activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bleAdapter = true;

        }

        int fineLocation =
                ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if ( fineLocation == PackageManager.PERMISSION_DENIED) {
            wifiAdapter = false;
            bleAdapter = false;


        }

        LeicaSdk.scanConfig.setBleAdapterOn(bleAdapter);
        LeicaSdk.scanConfig.setWifiAdapterOn(wifiAdapter);
    }

    /**
     * If the App is missing the permission add it to the request array
     * @param context context
     * @param permission permission String
     */
    void addPermission(Context context, String permission){

        //If the App is missing the permission add it to the request array
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
            manifestPermission.add(permission);
        }

    }
}
