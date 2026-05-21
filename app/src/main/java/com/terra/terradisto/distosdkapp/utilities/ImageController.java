package com.terra.terradisto.distosdkapp.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import com.terra.terradisto.distosdkapp.activities.Wifi3DInformationActivity;
import ch.leica.sdk.Defines;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.ErrorHandling.WrongDataException;
import ch.leica.sdk.Types;
import ch.leica.sdk.Utilities.LiveImagePixelConverter;
import ch.leica.sdk.commands.Image;
import ch.leica.sdk.commands.LiveImage;


import static java.lang.System.currentTimeMillis;

public class ImageController {

    private LiveImagePixelConverter liveImagePixelConverter;

    public interface ImageListener{
        void onImageProcessed(Bitmap image, boolean setImageInProcess);
    }
    /**
     * ClassName
     */
    private static final String CLASSTAG = Wifi3DInformationActivity.class.getSimpleName();

    ImageListener imageListener;

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++
	++ ADDITIONAL MEMBERS
 	++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    public ImageController(ImageListener imageListener){
        this.imageListener = imageListener;
    }

    public void setImageInProcess(Boolean imageInProcess) {
        this.imageInProcess = imageInProcess;
    }

    private Boolean imageInProcess = Boolean.FALSE;

    private Bitmap imageBitmap;

    // Crosshair
    private LiveImagePixelConverter.Point crosshair = null;


    synchronized public void setImage(final byte[] imageData) {
        final String METHODTAG = ".setImage";

        synchronized (imageInProcess) {

            if (imageInProcess == Boolean.TRUE) {
                return;
            }

            imageInProcess = Boolean.TRUE;


            try {
                imageBitmap =
                        BitmapFactory.decodeByteArray(
                                imageData,
                                0,
                                imageData.length
                        );
                imageListener.onImageProcessed(imageBitmap, false);

            } catch (Exception e) {
                Log.e(
                        CLASSTAG,
                        String.format(
                                "%s: Unknown error setting the image. error. Please verify your source code. ",
                                METHODTAG),
                        e
                );
            }


        }
    }


    public void setImage(Image image, int progress) {

        final String METHODTAG = ".setImage(3DD)";
        try {

            if(crosshair == null) {
                crosshair =
                        new LiveImagePixelConverter.Point(
                                image.getxCoordinateCrosshair(),
                                image.getyCoordinateCrosshair()
                        );
            }

            if (image instanceof LiveImage) {
                LiveImage currentLiveImage = (LiveImage) image;

                if (liveImagePixelConverter == null) {
                    // Image size
                    Bitmap bitmap =
                            null;

                    bitmap = BitmapFactory.decodeByteArray(
                            currentLiveImage.getImageBytes(),
                            0,
                            currentLiveImage.getImageBytes().length
                    );

                    liveImagePixelConverter =
                            new LiveImagePixelConverter(
                                    Types.DeviceType.Disto3D,
                                    new LiveImagePixelConverter.Size(bitmap.getWidth(), bitmap.getHeight())
                            );
                }

                // Direction
                LiveImagePixelConverter.SensorDirection sensorDirection =
                        new LiveImagePixelConverter.SensorDirection(
                                currentLiveImage.getHorizontalAngleCorrected(),
                                currentLiveImage.getVerticalAngleCorrected(),
                                currentLiveImage.getHorizontalAngleNotCorrected(),
                                currentLiveImage.getVerticalAngleNotCorrected(),
                                0
                        );

                // Orientation
                LiveImagePixelConverter.VerticalAxisFace orientation;
                if (currentLiveImage.getOrientation() == 1) {
                    orientation = LiveImagePixelConverter.VerticalAxisFace.One;
                } else {
                    orientation = LiveImagePixelConverter.VerticalAxisFace.Two;
                }
                // zoomFactor
                int zoomFactor = Defines.ID_ZOOM_WIDE;

                switch (progress) {
                    case 0:
                        zoomFactor = Defines.ID_ZOOM_WIDE;
                        break;
                    case 1:
                        zoomFactor = Defines.ID_ZOOM_NORMAL;
                        break;
                    case 2:
                        zoomFactor = Defines.ID_ZOOM_TELE;
                        break;
                    case 3:
                        zoomFactor = Defines.ID_ZOOM_SUPER;
                        break;
                }

                liveImagePixelConverter.UpdateValues(
                        sensorDirection,
                        orientation,
                        zoomFactor,
                        crosshair
                );
            }

            setImage(image);
        } catch (IllegalArgumentCheckedException e) {
            Log.e(CLASSTAG, String.format(
                    "%s: Error displaying the Image. Wrong Argument Received ",
                    METHODTAG), e);
        } catch (WrongDataException e) {
            Log.e(CLASSTAG, String.format(
                    "%s: Error displaying the Image. Wrong Argument Received ",
                    METHODTAG), e);
        }
    }

    public void setImage(final Image image) {

        final String METHODTAG = ".setImage";

        try {

            try {

                if(crosshair == null) {
                    crosshair =
                            new LiveImagePixelConverter.Point(
                                    image.getxCoordinateCrosshair(),
                                    image.getyCoordinateCrosshair()
                            );
                }

                byte[] data = image.getImageBytes();
                if (data != null) {
                    setImage(data);
                    if (crosshair._X > 0 && crosshair._Y > 0) {
                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setColor(Color.RED);

                        Bitmap workingBitmap = Bitmap.createBitmap(imageBitmap);
                        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);

                        canvas.drawLine(
                                (float) crosshair._X - 25,
                                (float) crosshair._Y,
                                (float) crosshair._X + 25,
                                (float) crosshair._Y, paint
                        );

                        canvas.drawLine(
                                (float) crosshair._X,
                                (float) crosshair._Y - 25,
                                (float) crosshair._X,
                                (float) crosshair._Y + 25,
                                paint
                        );

                        imageListener.onImageProcessed(mutableBitmap, false);
                    }
                } else {
                    Log.d(CLASSTAG, String.format("%s: IMAGE - not available.", METHODTAG));
                }
            } catch (WrongDataException e) {
                Log.e(CLASSTAG, String.format("%s: Error displaying the Image. Wrong Data Received", METHODTAG), e);
            } catch (IllegalArgumentCheckedException e) {
                Log.e(CLASSTAG, String.format(
                        "%s: Error displaying the Image. Wrong Argument Received ",
                        METHODTAG), e);
            }
        } catch (Exception e) {
            Log.e(CLASSTAG, String.format("%s: Error displaying the Image. ", METHODTAG), e);
        }
        crosshair = null;
    }

    public LiveImagePixelConverter.PolarCoordinates touchHandler(double x_equivalent, double y_equivalent){

        LiveImagePixelConverter.Point touched = new LiveImagePixelConverter.Point(
                x_equivalent,
                y_equivalent
        );
        LiveImagePixelConverter.PolarCoordinates polar = this.liveImagePixelConverter.ToPolarCoordinates(touched);

        return polar;
        //toImagePointProfile();
        //LiveImagePixelConverter.Point imagePoint = liveImagePixelConverter.ToImagePoint(polar);


    }

    @VisibleForTesting
    public void toImagePointProfile(){

        long initialTime = currentTimeMillis();

        for(int i = 0; i<1500; i++) {
            long initialPointTime = currentTimeMillis();
            this.liveImagePixelConverter.ToImagePoint(new LiveImagePixelConverter.PolarCoordinates(0.00018*i, 3.14-(0.00018*i)));
            long endPointTime = currentTimeMillis();

        }

        long endTime = currentTimeMillis();

        Log.i(
                "toImagePointProfile",
                String.format(
                        "Initial Time: %dEnd Time: %d Total Time: %d Average Time per record: %d",
                        initialTime,
                        endTime,
                        endTime - initialTime,
                        (endTime - initialTime) / 1500
                )
        );
    }
}
