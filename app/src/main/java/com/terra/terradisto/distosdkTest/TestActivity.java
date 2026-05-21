package com.terra.terradisto.distosdkTest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import com.terra.terradisto.distosdkapp.AppLicenses;
import com.terra.terradisto.R;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.LeicaSdk;


public class TestActivity extends AppCompatActivity {

    /**
     * ClassName
     */
    private final String CLASSTAG = TestActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        //init();
    }

    /**
     * read data from the Commands file and load it in the commands class
     * <p>
     * sets the name pattern to filter the scan results.
     * <p>
     * sets up DeviceManager
     */
    void init() {

        final String METHODTAG = ".init";

        if (LeicaSdk.isInit == false){


            Log.d(CLASSTAG, METHODTAG + ": Called");


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
                ArrayList<char[]> licenses = new ArrayList<>();

                //
                //
                // LeicaSdk.setLicenses(appLicenses.keys);


            } catch (JSONException e) {
                Toast.makeText(this, "Error in the structure of the JSON File, closing the application", Toast.LENGTH_LONG).show();

                Log.e(CLASSTAG, METHODTAG + ": Error in the structure of the JSON File, closing the application", e);

                finish();

            } catch (IllegalArgumentCheckedException e) {
                Toast.makeText(this, "Error in the data of the JSON File, closing the application", Toast.LENGTH_LONG).show();

                Log.e(CLASSTAG, METHODTAG + ": Error in the data of the JSON File, closing the application", e);

                finish();

            } catch (IOException e) {
                Toast.makeText(this, "Error reading JSON File, closing the application", Toast.LENGTH_LONG).show();

                Log.e(CLASSTAG, METHODTAG + ": Error reading JSON File, closing the application", e);

                finish();
            }
        }

        Log.d(CLASSTAG, METHODTAG + ": Activity initialization was finished completely");
    }
}
