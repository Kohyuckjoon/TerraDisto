package com.terra.terradisto.distosdkapp.activities;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.clipboard.Clipboard;
import com.terra.terradisto.distosdkapp.clipboard.InformationActivityData;
import com.terra.terradisto.distosdkapp.device.DeviceController;
import com.terra.terradisto.distosdkapp.device.DeviceStatusListener;
import com.terra.terradisto.distosdkapp.utilities.Logs;
import com.terra.terradisto.distosdkapp.utilities.dialog.DialogHandler;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.Reconnection.ReconnectionHelper;
import ch.leica.sdk.Types;
import ch.leica.sdk.commands.response.Response;
import ch.leica.sdk.Devices.Device;


public abstract class BaseInformationActivity
		extends AppCompatActivity
		implements
        DeviceStatusListener {

    /**
     * ClassName
     */
    private static final String CLASSTAG = BaseInformationActivity.class.getSimpleName();


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Dialogs
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public DialogHandler commandDialog;
    public DialogHandler customCommandDialog;
    public DialogHandler alertDialogConnect;
    public DialogHandler alertDialogDisconnect;
    public DialogHandler generalDialog;


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Additional Members
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public boolean isDestroyed = false;
    private Boolean receiverRegistered = false;

    /**
     *
     */
    public DeviceController currentDeviceController = null;


    public abstract void setDeviceController();

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
    ++ Abstract Methods - UI
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public abstract void setContentView();
    abstract void initMembers();


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    // Commands UI
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ///////////////////////////////////////////////////////////////////////////////////////////
    public abstract void setCommandsUIOff();

    public abstract void setCommandsUIOn();

    abstract void setActivityUI(String deviceModel);
    abstract void clearUI();


    //////////////////////////////////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	ANDROID LIFECYCLE
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView();

        //Initialize Dialogs
        commandDialog = new DialogHandler();
        customCommandDialog = new DialogHandler();
        generalDialog = new DialogHandler();


        String message = "connection established";

        alertDialogConnect = new DialogHandler();
        alertDialogConnect.setAlert(
                BaseInformationActivity.this,
                message
        );

        message = "lost connection to device";
        alertDialogDisconnect = new DialogHandler();
        alertDialogDisconnect.setAlert(
                BaseInformationActivity.this,
                message
        );

    }


	@Override
	protected void onResume() {
		super.onResume();
        final String METHODTAG = "onResume";

        this.initMembers();
        this.clearUI();

        this.setDeviceController();
        this.setInformationActivityData();
        this.currentDeviceController.stopFindingDevices();

        if(currentDeviceController == null ){
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return;
        }
        

        //Set listeners for the activity
        setListeners();

	}

	@Override
	protected void onStop() {
		super.onStop();

        dismissOpenDialogs();
	}

	/**
	 * Remove all the listeners associated with the currentDeviceController and the reconnection procedure
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
        final String METHODTAG = ".onDestroy";
        isDestroyed = true;
        //unregister activity for connection changes
        if (currentDeviceController != null){
            currentDeviceController.onDestroy();
        }
        Log.v(CLASSTAG, String.format("%s: Activity Destroyed successfully ", METHODTAG));
	}

    /**
     * when closing main activity, disconnect from all devices
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        InformationActivityData data = Clipboard.INSTANCE.getInformationActivityData();
        data.isSearchingEnabled = true;

        finish();
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	RECEIVERS
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ////////////////////////////////////////////////////////////////////////////////////////
    protected void setReceivers(String action, BroadcastReceiver broadcastReceiver) {
        // Register activity for bluetooth adapter changes
        if (!receiverRegistered) {
            receiverRegistered = true;
            IntentFilter filter = new IntentFilter(action);
            registerReceiver(broadcastReceiver, filter);
        }
    }

    protected void unregisterReceivers(BroadcastReceiver broadcastReceiver){
        // Unregister activity for adapter changes
        if (receiverRegistered) {
            receiverRegistered = false;
            unregisterReceiver(broadcastReceiver);
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	LISTENERS
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Set listeners for: CurrentDevice, ReconnectionHelper objects
     */
    protected void setListeners() {
        currentDeviceController.setListeners();
    }

    public void setOnClickListener(final Button button, final View.OnClickListener buttonListener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (button != null) {
                    button.setOnClickListener(buttonListener);
                }
                else{
                    Log.d(CLASSTAG, "Unable to set buttonListener on Button: "+ button.getText());
                }
            }
        });
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	HELPERS
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    ////////////////////////////////////////////////////////////////////////////////////////
    protected void setInformationActivityData(){
        InformationActivityData informationActivityData = Clipboard.INSTANCE.getInformationActivityData();
        if(informationActivityData !=null){

            Device device = informationActivityData.device;
            ReconnectionHelper reconnectionHelper = informationActivityData.reconnectionHelper;

            if (device != null) {
                if(currentDeviceController.getCurrentDevice() == null) {
                    currentDeviceController.setCurrentDevice(device);
                }else{
                    informationActivityData.device = currentDeviceController.getCurrentDevice();
                }
            }

            if (reconnectionHelper != null) {
                currentDeviceController.setReconnectionHelper(reconnectionHelper);
            }
            this.setListeners();
        }
    }


    protected String getAvailableCommandsString() {

        String METHODTAG = "showAvailableCommands";

        StringBuilder commandListStr = new StringBuilder();

        if(currentDeviceController == null ) {
            Logs.logNullValues(CLASSTAG, METHODTAG, "currentDeviceController");
            return "";
        }

        for (String commandStr : currentDeviceController.getAvailableCommands()) {
            commandListStr.append(", ").append(commandStr);
        }
        return commandListStr.toString();


    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	DIALOGS
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Show alert message in the UI
     * @param message Message to be shown to the user
     */
    public void showAlert(final String message) {

        if (isDestroyed) {
            return;
        }
        generalDialog.setAlert(BaseInformationActivity.this,message);
        generalDialog.show();
    }

    /**
     * Presents a dialog to user after the connection states changed
     * @param connected
     * connected = true -> connected
     * connected = false -> disconnected
     */
    synchronized void showConnectedDisconnectedDialog(final boolean connected) {

        if (connected) {
            alertDialogConnect.show();
            if(alertDialogDisconnect.isShowing()) {
                alertDialogDisconnect.dismiss();
            }

        } else {
            alertDialogDisconnect.show();
            if(alertDialogDisconnect.isShowing()) {
                alertDialogConnect.dismiss();
            }
        }
    }

    /**
     * Dismiss all the opened dialogs.
     */
    public void dismissOpenDialogs() {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Dismiss all the dialogs before closing the app.
                if (commandDialog != null) {
                    commandDialog.dismiss();
                }
                if (customCommandDialog != null) {
                    customCommandDialog.dismiss();
                }
                if (alertDialogConnect != null) {
                    alertDialogConnect.dismiss();
                }
                if (alertDialogDisconnect != null) {
                    alertDialogDisconnect.dismiss();
                }

                if (generalDialog != null) {
                    generalDialog.dismiss();
                }

            }
        });
    }



    /**
     * show a list of available commands in a dialog
     */
    public void showCommandDialog() {

        final String METHODTAG = ".showCommandDialog";

        final String[] commandsToUse = this.currentDeviceController.getAvailableCommands();

        commandDialog = new DialogHandler();
        String title = "Select Command";
        String message = "";


        SetItemsRunnable setItemsRunnable = new SetItemsRunnable(commandsToUse);

        commandDialog.setCommandDialog(
                BaseInformationActivity.this,
                title,
                message,
                commandsToUse,
                setItemsRunnable
        );

        commandDialog.show();
        Log.v(CLASSTAG, String.format("%s command Dialog shown.", METHODTAG));
    }

    /**
     * Show a dialog in which a user can type in a text and send it as a command.
     * The custom command method will not return a response object containing the data.
     */
    private void showCustomCommandDialog(){

        final String METHODTAG = ".showCustomCommandDialog";
        final EditText input = new EditText(this);

        DialogHandler customCommandDialog = new DialogHandler();
        String title = getApplicationContext().getString(R.string.custom_command_lbl);
        String positiveButtonText = getApplicationContext().getString(R.string.ok_ui);

        Runnable positiveButtonRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        Response response =
                                currentDeviceController.sendCustomCommand(input.getText().toString());
                        ErrorObject error = response.getError();
                        if(error != null){
                            showAlert(formatErrorMessage(error));
                        }
                    }
                };

        customCommandDialog.setCustomCommandDialog(
                BaseInformationActivity.this,
                title,
                positiveButtonText,
                positiveButtonRunnable,
                input
        );

        customCommandDialog.show();
        Log.v(CLASSTAG, String.format("%s custom command dialog created.", METHODTAG));
    }



    public String formatErrorMessage(ErrorObject error) {
        return String.format(
                "Error Sending Command - code: %s Message: %s",
                error.getErrorCode(),
                error.getErrorMessage()
        );
    }

    protected void showError(String methodTag, ErrorObject error) {
        Log.e(
                CLASSTAG,
                String.format( "%s: send command error, %s", methodTag, error.toString() ));

        showAlert(
                String.format(
                        Locale.getDefault(),
                        "send command error, Code: %d Message: %s",
                        error.getErrorCode(),
                        error.getErrorMessage()
                ));
    }


    protected void showError(String methodTag, Exception exc) {
        Log.e(
                CLASSTAG,
                String.format(
                        "%s: send command error, %s",
                        methodTag,
                        exc.toString()
                )
        );

        showAlert(
                String.format(
                        Locale.getDefault(),
                        "%s: send command error, %s",
                        methodTag,
                        exc.toString()
                ));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	UI SET RAW DATA
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    /////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Set text value in a TextView
     * @param textView textView
     * @param text text
     */
    public void setTextView(final TextView textView, final String text){
        String METHODTAG = ".setTextView";
        if(textView == null){
            Logs.logNullValues(CLASSTAG, METHODTAG, String.format("textView: %s", text));
            return;
        }
        
        String resourceName = getResources().getResourceName(textView.getId());
        if(resourceName.equals("status_txt"))
        Log.d(
                CLASSTAG,
                String.format(
                        "TextView: %s set to: %s",
                        resourceName,
                        text
                )
        );

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    public void setUIButtonState(final Button button,
                                 final float alpha,
                                 final boolean clickable){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setAlpha(alpha);
                button.setClickable(clickable);
            }
        });
    }

    public void setElementVisibility(final View view, final boolean visible) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    if(visible == true) {
                        view.setVisibility(View.VISIBLE);
                    }else{
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //	INNER CLASSES
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    //////////////////////////////////////////////////////////////////////////////////
    public class SetItemsRunnable implements Runnable{

        /**
         * ClassName
         */
        private final String CLASSTAG = SetItemsRunnable.class.getSimpleName();

        private String[] commandsToUse;

        int which;
        public SetItemsRunnable (String[] commandsToUse){
            this.commandsToUse = commandsToUse;
        }
        public void setSelectedNumber(int which){
            this.which = which;
        }

        @Override
        public void run() {
            final String command = commandsToUse[this.which];

            if (command.equals(Types.Commands.Custom.name())) {
                showCustomCommandDialog();
            }
            else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Response response =
                                currentDeviceController.sendCommand(Types.Commands.valueOf(command));

                        ErrorObject error = response.getError();
                        if(error != null) {
                            showAlert(formatErrorMessage(error));
                        }
                    }
                }).start();
            }
        }
    }

}