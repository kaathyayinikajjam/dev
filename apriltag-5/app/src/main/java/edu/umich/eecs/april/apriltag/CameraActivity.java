package edu.umich.eecs.april.apriltag;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;
import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity implements Orientation.Listener {


    public static final String TAG = "AprilTag";

    int detectedTages=0;

    public boolean recordingEnabled = false;
    public boolean headerAdded = false;

    long startTime = 0;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 77;
    private static Handler mGameHandler;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    int currentCamera;
    double pitch;
    double roll;

    double rollToDegree;

    DecimalFormat df;
    private Camera camera;
    private TagView tagView;
    private Orientation orientation;
    private TextView mScoreView;
    private TextView mAngleView;



    private  TextView angleBlueTextView;
    private  TextView angleGreenTextView;
    private  TextView angleRedTextView;

    private Button switchCamera;
    private int has_camera_permissions = 0;
    private FrameLayout layout;
    private boolean rotated;
    Button recordButtom;

    Button screenShot;
    private File file;
    private FileWriter writer;
    private BufferedWriter bw;
    private CSVWriter csvWriter;
    Handler timerHandler = new Handler();
    String timerValue;
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            long miliSec=millis%1000;

            timerValue=(String.format("%d:%02d:%d", minutes, seconds,miliSec));

            timerHandler.postDelayed(this, 10);
        }
    };
    private LinearLayout lowerLayout;


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

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;


    protected void onCreate(Bundle savedInstanceState) {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        //Remove title bar
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//
////Remove notification bar
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        currentCamera = 0;
        //recordButtom = (Button) findViewById(R.id.recordButton);
        lowerLayout=(LinearLayout)findViewById(R.id.lowerLayout);





        df = new DecimalFormat("#.##");
        super.onCreate(savedInstanceState);
        mAngleView = new TextView(this);
        mAngleView.setTextColor(0xFF00FF00); // Green
        mAngleView.setTextSize(18);
        mAngleView.setPadding(100, 150, 100, 250);




//        ImageView icon = new ImageView(this); // Create an icon
//        icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_info_black_24dp));

//        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
//                .setContentView(icon)
//                .build();
//
//        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
//// repeat many times:
//        ImageView itemIcon = new ImageView(this);
//        itemIcon.setImageDrawable( getResources().getDrawable(R.drawable.ic_info_black_24dp) );
//        SubActionButton button1 = itemBuilder.setContentView(itemIcon).build();
//
//        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
//                .addSubActionView(button1)
//                //.addSubActionView(button2)
//                // ...
//                .attachTo(actionButton)
//                .build();


        switchCamera = new Button(this);
        //switchCamera.setText("Switch");

        switchCamera.setAlpha((float) 0.75);
        switchCamera.setWidth(20);
        switchCamera.setHeight(20);
        //switchCamera.set
        //switchCamera.scale


        switchCamera.setPadding(200, 100, 200, 100);
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
        mScoreView.setPadding(100, 225, 100, 150);


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


        //angleGreenTextView = (TextView)findViewById(R.id.angleGreenTextView) ;

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


                    if ((values[0].equals(2.0))) {

                        angleGreenTextView.setVisibility(View.VISIBLE);

                        angleBlueTextView.setVisibility(View.GONE);

                        angleRedTextView.setVisibility(View.GONE);

                        if (detectedTages==0) {
                            detectedTages = 2;



                        }




                        Double[] doubles = new Double[values.length];

                        doubles[0] = Double.parseDouble(values[1].toString());
                        //doubles[1] = Double.parseDouble(values[1].toString());


                        double angleGreen = 0;
                        rollToDegree = Math.toDegrees(roll);

                        if (rollToDegree < 0) {


                            if ((rollToDegree >= -30) && (rollToDegree < 0)) {

                                // angleGreen = Math.toDegrees(doubles[0]*2 + (Math.abs(roll)+ Math.abs(roll / 2.3 )));

//                                angleGreen = Math.toDegrees(doubles[0] - roll);


                                angleGreen = Math.toDegrees(doubles[0]);

                            } else if ((rollToDegree >= -180) && (rollToDegree < -90)) {


                                //angleGreen = 0;
                                angleGreen = Math.toDegrees(doubles[0]);


                            }

                        } else if (rollToDegree >= 0) {


                            if (rollToDegree >= 0 && rollToDegree <= 30) {

                                // angleGreen = Math.toDegrees(doubles[0]*2 - ((roll) - (roll / 2.3)));
//                                angleGreen = Math.toDegrees((doubles[0]) - roll);

                                angleGreen = Math.toDegrees(doubles[0]);


                            } else {

                                angleGreen = Math.toDegrees(doubles[0]);

                              //  angleGreen = 0;


                            }
//
                        }





//                        mAngleView.setText("Slope Green: " + df.format(angleGreen));

                        angleGreenTextView.setText(df.format(angleGreen));


                        //angleGreenTextView.setText("test"+df.format(angleGreen));





                        String[] headerRecord = {"Time","slope Green","X Center of Tag 1", "Y Center of Tag1", "X Center of Tag 2","Y Center of Tag 2","Roll","Pitch",

                                "C T 1-1x","C T 1-1y", "C T 1-2x","C T 1-2y","C T 1-3x","C T 1-3y","C T 1-4x","C T 1-4y",

                                "C T 2-1x","C T 2-1y", "C T 2-2x","C T 2-2y","C T 2-3x","C T 2-3y","C T 2-4x","C T 2-4y"


                        };

                        if(recordingEnabled==true)


                        {

                            if(headerAdded==false) {

                                addData(new String[]{"number of tags: 2"});
                                addData(headerRecord);
                                headerAdded = true;
                            }


                            if(detectedTages==2)

                            addData(
                                    new String[]
                                            {
                                            timerValue,Double.toString(angleGreen),values[2].toString(),values[3].toString(),values[4].toString(),values[5].toString()
                                    ,Double.toString(rollToDegree),Double.toString(Math.toDegrees(pitch)),


                                                    values[6].toString(),values[7].toString(),values[8].toString(),values[9].toString(),
                                                    values[10].toString(),values[11].toString(),values[12].toString(),values[13].toString()
                                                    ,values[14].toString(),values[15].toString(),values[16].toString(),values[17].toString(),values[18].toString(),
                                                    values[19].toString(),values[20].toString(),values[21].toString()



                                    }



                                    );

                        }




                    }
                    //ends here



                    else if (values[0].equals(4.0)) {


                        angleBlueTextView.setVisibility(View.VISIBLE);
                        angleRedTextView.setVisibility(View.GONE);

                        if(detectedTages==0) {
                            detectedTages = 4;

                           // angleGreenTextView.setVisibility(View.VISIBLE);

                        }


                        Double[] doubles = new Double[values.length];

                        doubles[0] = Double.parseDouble(values[1].toString());
                        doubles[1] = Double.parseDouble(values[2].toString());

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


//                                angleGreen = Math.toDegrees(doubles[0] - roll);
//
//                                angleBlue = Math.toDegrees(doubles[1] - roll);


//                                angleGreen = Math.toDegrees(doubles[0] - roll);
//
//                                angleBlue = Math.toDegrees(doubles[1] - roll);


                                angleGreen = Math.toDegrees(doubles[0] );
//
                                angleBlue  = Math.toDegrees(doubles[1] );



                            } else if ((rollToDegree >= -180) && (rollToDegree < -90)) {

                                angleGreen = Math.toDegrees(doubles[0] );
//
                                angleBlue  = Math.toDegrees(doubles[1] );


//                                angleGreen = 0;
//
//                                angleBlue = 0;
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


//                                angleGreen = Math.toDegrees((doubles[0]) - roll);
//
//                                angleBlue = Math.toDegrees((doubles[1]) - roll);



                                angleGreen = Math.toDegrees(doubles[0] );
//
                                angleBlue  = Math.toDegrees(doubles[1] );//
//                                angleBlue  = Math.toDegrees(doubles[1] );


                            } else {


                                angleGreen = Math.toDegrees(doubles[0] );
//
                                angleBlue  = Math.toDegrees(doubles[1] );

//                                angleGreen = 0;
////
//                                angleBlue = 0;

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

//                        mAngleView.setText("Angle Green: " + df.format(angleGreen) + " Angle Blue: " + df.format(angleBlue));


                        angleGreenTextView.setText(df.format(angleGreen));
                        angleBlueTextView.setText(df.format(angleBlue));

//                        angleGreenTextView.setText(df.format(angleGreen));
//
//                        angleBlueTextView.setText(df.format(angleBlue));



                        String[] headerRecord = {
                                "Time","Slope Green","Slope Blue",
                                "X Center of Tag 1", "Y Center of Tag 1", "X Center of Tag 2","Y Center of Tag 2",
                                "X Center of Tag 3", "Y Center of Tag 3", "X Center of Tag 4","Y Center of Tag 4",
                                "Roll","Pitch",
                                "C T 1-1x","C T 1-1y", "C T 1-2x","C T 1-2y","C T 1-3x","C T 1-3y","C T 1-4x","C T 1-4y",

                                "C T 2-1x","C T 2-1y", "C T 2-2x","C T 2-2y","C T 2-3x","C T 2-3y","C T 2-4x","C T 2-4y",

                                "C T 3-1x","C T 3-1y", "C T 3-2x","C T 3-2y","C T 3-3x","C T 3-3y","C T 3-4x","C T 3-4y",

                                "C T 4-1x","C T 4-1y", "C T 4-2x","C T 4-2y","C T 4-3x","C T 4-3y","C T 4-4x","C T 4-4y"

                                };

                        if(recordingEnabled==true)


                        {

                            if(headerAdded==false) {

                                addData(new String[]{"number of tags: 4"});
                                addData(headerRecord);
                                headerAdded = true;
                            }


                            if(detectedTages==4)
                            addData(
                                    new String[]
                                            {
                                            timerValue,Double.toString(angleGreen),Double.toString(angleBlue),values[3].toString(),values[4].toString()
                                    ,values[5].toString(),values[6].toString(),values[7].toString(),values[8].toString(),values[9].toString(),values[10].toString(),Double.toString(rollToDegree),
                                    Double.toString(Math.toDegrees(pitch)),


                                                    values[11].toString(),values[12].toString(),values[13].toString(),values[14].toString(),
                                                    values[15].toString(),values[16].toString(),values[17].toString(),values[18].toString(),

                                                    values[19].toString(),values[20].toString(),values[21].toString(),values[22].toString(),
                                                    values[23].toString(),values[24].toString(), values[25].toString(),values[26].toString(),

                                                    values[27].toString(),values[28].toString(), values[29].toString(),values[30].toString(),
                                                    values[31].toString(),values[32].toString(), values[33].toString(),values[34].toString(),

                                                    values[35].toString(),values[36].toString(), values[37].toString(),values[38].toString(),
                                                    values[39].toString(),values[40].toString(), values[41].toString(),values[42].toString()


                            });

                        }



                    } else if (values[0].equals(3.0))

                    {

                        // angleGreenTextView.setVisibility(View.VISIBLE);
                        angleBlueTextView.setVisibility(View.VISIBLE);

                        angleRedTextView.setVisibility(View.VISIBLE);

                        if(detectedTages==0) {
                            detectedTages = 3;




                        }

//                        mAngleView.setText
//                                ("Alpha: " + df.format(values[0]) + " Beta:  " +
//                                        df.format(values[1]) + "  Gama: " + df.format(values[2]));


                        angleGreenTextView.setText(df.format(values[3]));
                        angleBlueTextView.setText(df.format(values[1]));
                        angleRedTextView.setText(df.format(values[2]));

                        String[] headerRecord = {
                                "Time","Alpha","Beta","Gama",
                                "X Center of Tag 1", "Y Center of Tag 1", "X Center of Tag 2","Y Center of Tag 2",
                                "X Center of Tag 3", "Y Center of Tag 3","Roll","Pitch",
                                "C T 1-1x","C T 1-1y", "C T 1-2x","C T 1-2y","C T 1-3x","C T 1-3y","C T 1-4x","C T 1-4y",

                                "C T 2-1x","C T 2-1y", "C T 2-2x","C T 2-2y","C T 2-3x","C T 2-3y","C T 2-4x","C T 2-4y",

                                "C T 3-1x","C T 3-1y", "C T 3-2x","C T 3-2y","C T 3-3x","C T 3-3y","C T 3-4x","C T 3-4y"



                        };


                        if(recordingEnabled==true)


                        {

                            if(headerAdded==false) {

                                addData(new String[]{"number of tags: 3"});
                                addData(headerRecord);
                                headerAdded = true;
                            }


                            if(detectedTages==3)
                            addData(

                                    new String[]
                                            {timerValue,values[1].toString(),values[2].toString(),values[3].toString(),values[4].toString()
                                    ,values[5].toString(),values[6].toString(),values[7].toString(),values[8].toString(),values[9].toString(),Double.toString(rollToDegree),Double.toString(Math.toDegrees(pitch)),
                                                    values[10].toString(),values[11].toString(),values[12].toString(),values[13].toString()
                                                    ,values[14].toString(),values[15].toString(),values[16].toString(),values[17].toString(),values[18].toString(),
                                                    values[19].toString(),values[20].toString(),values[21].toString(),

                                                    values[22].toString(),
                                                    values[23].toString(),values[24].toString(),values[25].toString(),values[26].toString()
                                                    ,values[27].toString(),values[28].toString(),values[29].toString(),values[30].toString(),values[31].toString(),
                                                    values[32].toString(), values[33].toString()



                                            }


                                    );

                        }








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



        angleBlueTextView=(TextView)findViewById(R.id.angleBlueTextView);

        //angleBlueTextView.setText("rasmesh");
        angleGreenTextView=(TextView)findViewById(R.id.angleGreenTextView);


        angleRedTextView=(TextView)findViewById(R.id.angleRedTextView);



        lowerLayout = (LinearLayout) findViewById(R.id.lowerLayout) ;


        SurfaceView overlayView = new SurfaceView(this);

        tagView = new TagView(this, overlayView.getHolder());

        tagView.initiateHandler(mGameHandler);

        layout = (FrameLayout) findViewById(R.id.tag_view);

        //AspectFrameLayout layout =  findViewById(R.id.tag_view);


        //layout.addView(overlayView); // TODO: Not needed?
        layout.addView(tagView);


        // Add toolbar/actionbar
        //Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(myToolbar);

        // Make the screen stay awake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        verifyStoragePermissions(this);

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    void takeScreenshot(View view) {
        Date now = new Date();


        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            view = view.getRootView();

            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
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

        recordButtom = (Button) findViewById(R.id.recordButton);
        //screenShot = (Button) findViewById(R.id.takeScreenShot);

        try {


            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)

            {


                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                rotated = true;

                //RotateAnimation rotate= (RotateAnimation) AnimationUtils.loadAnimation(this,R.anim.rotate_animation);
//                mScoreView.setAnimation(rotate);
//                mAngleView.setAnimation(rotate);

                mScoreView.setRotation(180);
               // mAngleView.setRotation(180);
               // recordButtom.setRotation(180);

                lowerLayout.setRotation(180);


//                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//                rotated = true;

               // layout.setRotation(180);
                //angleGreenTextView.setRotation(180);
                //angleBlueTextView.setRotation(180);
                //lowerLayout.setRotation(180);
                //screenShot.setRotation(180);


                //switchCamera.setRotation(180);

            } else {

                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                rotated = false;
                mScoreView.setRotation(0);
                //mAngleView.setRotation(0);
                //lowerLayout.setRotation(0);
                //recordButtom.setRotation(0);
                lowerLayout.setRotation(0);




                angleBlueTextView.setVisibility(View.VISIBLE);

                angleRedTextView.setVisibility(View.VISIBLE);
                //angleGreenTextView.setRotation(0);
                //angleBlueTextView.setRotation(0);
                //screenShot.setRotation(0);
                //switchCamera.setRotation(0);


            }
            camera = Camera.open(currentCamera);
            // camera.setDisplayOrientation(90);

            //camera.getParameters().setRotation(90);
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


        //camera.getParameters().setRotation(90);

//        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }

//
//        int result;
//        //int currentapiVersion = android.os.Build.VERSION.SDK_INT;
//        // do something for phones running an SDK before lollipop
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360; // compensate the mirror
//        } else { // back-facing
//            result = (info.orientation - degrees + 360) % 360;
//        }
//
//        camera.setDisplayOrientation(result);
//        camera.getParameters().setRotation(result);

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
        timerHandler.removeCallbacks(timerRunnable);

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


        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
            //


        }


        Camera.getCameraInfo(camidx, info);
//       // log.i(TAG, "using camera " + camidx);
//       // log.i(TAG, "camera rotation: " + info.orientation);

        try {


            // System.out.println(camidx);
            camera = Camera.open(camidx);
            // camera.setDisplayOrientation(90);
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

//
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
//        {
//            params.set("orientation", "portrait");
//            params.set("rotation", 180);
//        }
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
//        {
//           // params.set("orientation", "landscape");
//           // params.set("rotation", 90);
//        }


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


        if (!rotated)
            this.roll = (roll);
        else
            this.roll = roll + Math.PI;


        mScoreView.setText("Pitch: " + df.format(Math.toDegrees(this.pitch)) + " Roll: " + df.format(Math.toDegrees(this.roll)));


    }


    public void recordData(View view) {


        recordButtom = (Button) findViewById(view.getId());


        String status = recordButtom.getText().toString();

        //System.out.println(status);

        if (status.equals("Record")) {




            recordButtom.setBackgroundColor(Color.parseColor("#f44336"));
            //valueLayout.setWeightSum(1);

            recordButtom.setText("STOP");
            String report = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

            file = new File("/sdcard/" + report + ".csv");

            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            recordingEnabled = true;


            writer = null;
            try {
                writer = new FileWriter(file.getAbsoluteFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
            bw = new BufferedWriter(writer);
            csvWriter = new CSVWriter(bw,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);


            //System.out.println(file);


        } else {
            recordingEnabled = false;
            detectedTages=0;
            headerAdded = false;
            timerHandler.removeCallbacks(timerRunnable);
            try {
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            recordButtom.setText("Record");
            //valueLayout.setVisibility(LinearLayout.GONE);


            recordButtom.setBackgroundColor(Color.parseColor("#4caf50"));


        }

    }


    public void addData(String[]row)


    {


        try {

            csvWriter.writeNext(row);

        }

        catch (Exception e)


        {

            e.printStackTrace();

        }



//            csvWriter.writeNext(new String[]{"Shahin ♥", "khoshgele@gmail.com", "+1-1111111111", "Canada"});
//            csvWriter.writeNext(new String[]{"Masoud", "masoud@outlook.com", "+1-1111111112", "UAE"});


    }


}