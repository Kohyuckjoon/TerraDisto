package com.terra.terradisto.distosdkapp.utilities.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;

import com.terra.terradisto.R;
import com.terra.terradisto.distosdkapp.activities.BaseInformationActivity;
import ch.leica.sdk.logging.Logs;


public class DialogHandler {

    private Handler uiThread;
    private AlertDialog alertDialog = null;


    public void setDialog(final Context context,
                          final String title,
                          final String message,
                          final boolean cancellable){


        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {

                          @Override
                          public void run() {
                              AlertDialog.Builder builder = new AlertDialog.Builder(context);

                              builder.
                                      setMessage(message).
                                      setTitle(title).
                                      setCancelable(cancellable).
                                      setNegativeButton("Ok", null);
                              alertDialog = builder.create();
                          }
                      });
    }


    public void setDialog(final Context context,
                          final String title,
                          final String message,
                          final boolean cancellable,
                          final String positiveButtonText,
                          final Runnable positiveButtonRunnable,
                          final String negativeButtonText,
                          final Runnable negativeButtonRunnable){

        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {

            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.
                        setMessage(message).
                        setTitle(title).
                        setCancelable(cancellable);


                if(positiveButtonRunnable != null) {
                    builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new Thread(positiveButtonRunnable).start();

                        }
                    });
                }

                if(negativeButtonRunnable != null) {
                    builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new Thread(negativeButtonRunnable).start();

                        }
                    });
                }



                alertDialog = builder.create();

            }
        });

    }

    /**
     * Show alert message
     */
    public void setAlert(final Context context, final String message) {

        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {
            @Override
            public void run() {
                try {
                    AlertDialog.Builder alertBuilder = new android.app.AlertDialog.Builder(context);
                    alertBuilder.setMessage(message);
                    alertBuilder.setPositiveButton("Ok", null);
                    alertDialog = alertBuilder.create();


                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Show a dialog in which a user can type in a text and send it as a command.
     * The custom command method will not return a response object containing the data.
     */
    public void setCommandDialog(final Context context,
                                 final String title,
                                 final String message,
                                 final String[] commandsToUse,
                                 final BaseInformationActivity.SetItemsRunnable setItemsRunnable) {

        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {

            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.
                        setTitle(title).
                        setCancelable(true);

                if (setItemsRunnable != null) {
                    builder.setItems(commandsToUse, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setItemsRunnable.setSelectedNumber(which);
                            new Thread(setItemsRunnable).start();

                        }
                    });
                }

                alertDialog = builder.create();
            }
        });
    }


    /**
     * Show a dialog in which a user can type in a text and send it as a command.
     * The custom command method will not return a response object containing the data.
     */
    public void setCustomCommandDialog(final Context context,
                                       final String title,
                                       final String positiveButtonText,
                                       final Runnable positiveButtonRunnable,
                                       final EditText input) {

        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {

            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.
                        setTitle(title).
                        setCancelable(true).
                        setView(input);

                if (positiveButtonRunnable != null) {
                    builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new Thread(positiveButtonRunnable).start();

                        }
                    });
                }

                builder.setNegativeButton(context.getString(R.string.cancel_ui), null);

                alertDialog = builder.create();
            }
        });
    }



    public void show(){

        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {
            @Override
            public void run() {
                if(alertDialog != null) {
                    try {
                        alertDialog.show();

                    }catch(Exception e){
                        Logs.log(Logs.LogTypes.exception, String.format("Error displaying the error alert %s", e.getMessage()), e);
                    }
                }
            }
        });

    }

    public void dismiss(){

        // Defines a Handler object that's attached to the UI thread
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {
            @Override
            public void run() {
                if(alertDialog != null){
                    alertDialog.dismiss();
                }
            }
        });

    }
    public boolean isShowing(){
        if(this.alertDialog != null) {
            return this.alertDialog.isShowing();
        }else{
            return false;
        }
    }

    public void setMessage(final String message) {
        uiThread = new Handler(Looper.getMainLooper());
        uiThread.post(new Runnable() {
            @Override
            public void run() {
                alertDialog.setMessage(message);
            }
        });
    }
}

