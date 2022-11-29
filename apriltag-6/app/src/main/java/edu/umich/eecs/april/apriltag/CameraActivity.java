package edu.umich.eecs.april.apriltag;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity implements Orientation.Listener {


    private static final String TAG = "AprilTag";
    private Camera camera;
    int currentCamera;
    private TagView tagView;


    double pitch;
    double roll;

    double rollToDegree;

    DecimalFormat df;


    private Orientation orientation;
    private static Handler mGameHandler;
    private TextView mScoreView;


    private TextView mAngleView;


    private Button switchCamera;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 77;
    private int has_camera_permissions = 0;

    private void verifyPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        int nthreads = Integer.parseInt(sharedPreferences.getString("nthreads_value", "0"));
        if (nthreads <= 0) {
            int nproc = Runtime.getRuntime().availableProcessors();
            if (nproc <= 0) {
                nproc = 1;
            }
            //Log.i(TAG, "available processors: " + nproc);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("nthreads_value", Integer.toString(nproc)).apply();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {

        currentCamera = 0;

        df = new DecimalFormat("#.##");
        super.onCreate(savedInstanceState);
        mAngleView = new TextView(this);
        mAngleView.setTextColor(0xFF00FF00); // Green
        mAngleView.setTextSize(18);
        mAngleView.setPadding(20, 150, 10, 10);


        switchCamera = new Button(this);
        //switchCamera.setText("Switch");

        switchCamera.setAlpha((float) 0.75);
        switchCamera.setWidth(20);
        switchCamera.setHeight(20);
        //switchCamera.scale


        switchCamera.setPadding(20, 10, 10, 20);
        switchCamera.setBackgroundResource(R.drawable.ic_restore_white_24dp);
//
//        switchCamera.setLayoutParams(new ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT));

        // switchCamera.setGravity(Gravity.RIGHT);

        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchCameraMethod();

            }
        });

        mScoreView = new TextView(this);
        mScoreView.setTextColor(0xFF00FF00); // Green
        mScoreView.setTextSize(18);
        mScoreView.setPadding(20, 225, 10, 10);


        // Ensure we have permission to use the camera (Permission Requesting for Android 6.0/SDK 23 and higher)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Assume user knows enough about the app to know why we need the camera, just ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            this.has_camera_permissions = 1;
        }


        orientation = new Orientation(this);

        mGameHandler = new Handler() {
            public void handleMessage(Message msg) {


                if (msg != null) {


                    Object[] values;

                    Class ofArray = msg.obj.getClass().getComponentType();
                    if (ofArray.isPrimitive()) {
                        List ar = new ArrayList();
                        int length = Array.getLength(msg.obj);
                        for (int i = 0; i < length; i++) {
                            ar.add(Array.get(msg.obj, i));
                        }
                        values = ar.toArray();
                    } else {
                        values = (Object[]) msg.obj;
                    }


                    DecimalFormat df = new DecimalFormat("#.#");


                    if (values.length == 1) {


                        Double[] doubles = new Double[values.length];

                        doubles[0] = Double.parseDouble(values[0].toString());
                        //doubles[1] = Double.parseDouble(values[1].toString());


                        double angleGreen = 0;
                        rollToDegree = Math.toDegrees(roll);

                        if (rollToDegree < 0) {


                            if ((rollToDegree >= -30) && (rollToDegree < 0)) {

                                // angleGreen = Math.toDegrees(doubles[0]*2 + (Math.abs(roll)+ Math.abs(roll / 2.3 )));

                                angleGreen = Math.toDegrees(doubles[0] - roll);

                            } else if ((rollToDegree >= -180) && (rollToDegree < -90)) {


                                angleGreen = 0;


                            }

                        } else if (rollToDegree >= 0) {


                            if (rollToDegree >= 0 && rollToDegree <= 30) {

                                // angleGreen = Math.toDegrees(doubles[0]*2 - ((roll) - (roll / 2.3)));
                                angleGreen = Math.toDegrees((doubles[0]) - roll);


                            } else {


                                angleGreen = 0;


                            }
//
                        }


                        mAngleView.setText("Slope Green: " + df.format(angleGreen));


                    } else if (values.length == 2) {


                        Double[] doubles = new Double[values.length];

                        doubles[0] = Double.parseDouble(values[0].toString());
                        doubles[1] = Double.parseDouble(values[1].toString());

                        double angleGreen = 0;
                        double angleBlue = 0;

                        rollToDegree = Math.toDegrees(roll);

                        if (rollToDegree < 0) {
//
//                            if((rollToDegree<-89&&rollToDegree>=-95)
//
//                                    //||(rollToDegree<6&&rollToDegree>-6)
//                            )
//                            {
//                                angleGreen = Math.toDegrees(doubles[0] + Math.abs((roll)));
//                                angleBlue = Math.toDegrees(doubles[0] + Math.abs((roll)));she
//
//                            }
//                            //double slopeGreen = doubles[0]+roll;

//
//                            else {

                            if ((rollToDegree >= -30) && (rollToDegree < 0)) {

//                                angleGreen = Math.toDegrees(doubles[0]*2 + (Math.abs(roll)+ Math.abs(roll / 2.3 )));
//
//                                angleBlue = Math.toDegrees(doubles[1]*2 + (Math.abs(roll)+ Math.abs(roll / 2.3 )));


                                angleGreen = Math.toDegrees(doubles[0] - roll);

                                angleBlue = Math.toDegrees(doubles[1] - roll);
                            } else if ((rollToDegree >= -180) && (rollToDegree < -90)) {


                                angleGreen = 0;

                                angleBlue = 0;
                            }
                            // }
                        } else if (rollToDegree >= 0) {


//                            if((rollToDegree>=89&&rollToDegree<=95)
//                            //||(rollToDegree<=5&&rollToDegree>=5)
//                                     )
//                            {
//                                angleGreen = Math.toDegrees(doubles[0] - (roll));
//
//                                angleBlue = Math.toDegrees(doubles[1] - (roll));
//
//                            }
//                            else {

                            if (rollToDegree >= 0 && rollToDegree <= 30) {

//                                angleGreen = Math.toDegrees(doubles[0]*2 - ((roll) - (roll / 2.3)));
//
//                                angleBlue = Math.toDegrees(doubles[1]*2 - ((roll) - (roll / 2.3)));


                                angleGreen = Math.toDegrees((doubles[0]) - roll);

                                angleBlue = Math.toDegrees((doubles[1]) - roll);


                            } else {


                                angleGreen = 0;
//
                                angleBlue = 0;

                            }
//                            else if (rollToDegree > 91 && rollToDegree <= 180) {
//
////                                angleGreen = Math.toDegrees(doubles[0]+(roll-(roll / 3)));
////                                        //+((roll / 3))
////
////
////                                angleBlue = Math.toDegrees(doubles[1]+(roll-(roll / 3)));
//
//                            }

                        }

                        //}


                        //System.out.println("Slope Green: "+slopeGreen);
                        //System.out.println("Slope Blue" +slopeBlue);

                        mAngleView.setText("Angle Green: " + df.format(angleGreen) + " Angle Blue: " + df.format(angleBlue));


                    } else if (values.length == 3) {

                        mAngleView.setText
                                ("Alpha: " + df.format(values[0]) + " Beta:  " +
                                        df.format(values[1]) + "  Gama: " + df.format(values[2]));

                    }


                    //mAngleView.setText(msg.obj.toString());

//                    Object[] objects = (Object[]) msg.obj;
//
//                    for (Object num : objects) {
//                        System.out.println("Element Value is " + num);
//
//                    }
                }


            }
        };


        setContentView(R.layout.main);

        addContentView(switchCamera, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addContentView(mScoreView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addContentView(mAngleView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        SurfaceView overlayView = new SurfaceView(this);

        tagView = new TagView(this, overlayView.getHolder());

        tagView.initiateHandler(mGameHandler);

        FrameLayout layout = (FrameLayout) findViewById(R.id.tag_view);
        //layout.addView(overlayView); // TODO: Not needed?
        layout.addView(tagView);

        // Add toolbar/actionbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // Make the screen stay awake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void switchCameraMethod() {


        camera.release();

        // int camidx = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();

        //int numOfCameras = Camera.getNumberOfCameras();

        //System.out.println(numOfCameras);


        for (int i = 0; i < Camera.getNumberOfCameras(); i += 1) {
            Camera.getCameraInfo(i, info);


            if (info.facing != currentCamera) {
                currentCamera = i;
                break;
            }
        }


        Camera.getCameraInfo(currentCamera, info);





        try {
            camera = Camera.open(currentCamera);
           // camera.setDisplayOrientation(90);

            camera.getParameters().setRotation(90);
            //camera.setDisplayOrientation(detectCameraDisplayOrientation(this,info));
            //camera.setDisplayOrientation(getCorrectCameraOrientation(info, camera));
            //camera.getParameters().setRotation(getCorrectCameraOrientation(info, camera));

        } catch (Exception e) {

            return;
        }
      //  camera.setDisplayOrientation(90);
        Camera.Parameters params = camera.getParameters();




        for (Camera.Size s : params.getSupportedPreviewSizes()) {
        }



        //params.setRotation(180);


       camera.getParameters().setRotation(90);

        tagView.setCamera(camera);


    }






    @Override
    protected void onStart() {
        super.onStart();
        orientation.startListening(this);
    }


    /**
     * Release the camera when application focus is lost
     */
    protected void onPause() {
        super.onPause();
        tagView.onPause();

        //Log.i(TAG, "Pause");
        // TODO move camera management to TagView class

        if (camera != null) {
            tagView.setCamera(null);
            camera.release();
            camera = null;
        }
    }

    /**
     * (Re-)initialize the camera
     */
    protected void onResume() {
        super.onResume();
        tagView.onResume();

        if (this.has_camera_permissions == 0) {
            return;
        }

        //Log.i(TAG, "Resume");

        // Re-initialize the Apriltag detector as settings may have changed
        verifyPreferences();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        double decimation = Double.parseDouble(sharedPreferences.getString("decimation_list", "4"));
        double sigma = Double.parseDouble(sharedPreferences.getString("sigma_value", "0"));
        int nthreads = Integer.parseInt(sharedPreferences.getString("nthreads_value", "0"));
        String tagFamily = sharedPreferences.getString("tag_family_list", "tag36h11");


        boolean useRear = (sharedPreferences.getString("device_settings_camera_facing", "1").equals("1")) ? true : false;

        ApriltagNative.apriltag_init(tagFamily, 2, decimation, sigma, nthreads);

        // Find the camera index of front or rear camera
        int camidx = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i += 1) {
            Camera.getCameraInfo(i, info);
            int desiredFacing = useRear ? Camera.CameraInfo.CAMERA_FACING_BACK :
                    Camera.CameraInfo.CAMERA_FACING_FRONT;
            if (info.facing == desiredFacing) {
                camidx = i;
                break;
            }
        }

        Camera.getCameraInfo(camidx, info);
//       // log.i(TAG, "using camera " + camidx);
//       // log.i(TAG, "camera rotation: " + info.orientation);

        try {
            camera = Camera.open(camidx);
            camera.setDisplayOrientation(90);
        } catch (Exception e) {
            // log.d(TAG, "Couldn't open camera: " + e.getMessage());
            return;
        }

        // log.i(TAG, "supported resolutions:");
        Camera.Parameters params = camera.getParameters();
        //params.setRotation(180);
        for (Camera.Size s : params.getSupportedPreviewSizes()) {
            // log.i(TAG, " " + s.width + "x" + s.height);
        }

        //setCameraDisplayOrientation(this,camidx,camera);


        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            params.set("orientation", "portrait");
            params.set("rotation", 180);
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
           // params.set("orientation", "landscape");
           // params.set("rotation", 90);
        }
        tagView.setCamera(camera);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.settings:
                verifyPreferences();
                Intent intent = new Intent();
                intent.setClassName(this, "edu.umich.eecs.april.apriltag.SettingsActivity");
                startActivity(intent);
                return true;


//            case R.id.FourTags:
//                Intent fourTagsIntent = new Intent(this,FourTags.class);
//                startActivity(fourTagsIntent);
//                return true;


            case R.id.reset:
                // Reset all shared preferences to default values
                PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();

                // Restart the camera preview
                onPause();
                onResume();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // log.i(TAG, "App GRANTED camera permissions");

                    // Set flag
                    this.has_camera_permissions = 1;

                    // Restart the TagViewer
                    SurfaceView overlayView = new SurfaceView(this);
                    tagView = new TagView(this, overlayView.getHolder());
                    FrameLayout layout = (FrameLayout) findViewById(R.id.tag_view);
                    layout.addView(tagView);

                    // Restart the camera
                    onPause();
                    onResume();
                } else {
                    // log.i(TAG, "App DENIED camera permissions");
                    this.has_camera_permissions = 0;
                }
                return;
            }
        }
    }

    @Override
    public void onOrientationChanged(float pitch, float roll) {

        this.pitch = (pitch);
        this.roll = (roll);

        mScoreView.setText("Pitch: " + df.format(Math.toDegrees(pitch)) + " Roll: " + df.format(Math.toDegrees(roll)));


    }
}
