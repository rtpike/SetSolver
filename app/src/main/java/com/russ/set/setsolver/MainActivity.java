package com.russ.set.setsolver;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener, View.OnTouchListener {

    private static final String  TAG = ":MainActivity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private processImage mProcessImage;
    private MenuItem mItemDebugCard;
    private MenuItem mItemDebugMode;

    private boolean camRunning = true;

    private int currentCard = 0;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    /* Now enable camera view to start receiving frames */
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(TAG, "Creating and setting view");
        mOpenCvCameraView = new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setMaxFrameSize(800, 600);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mProcessImage = new processImage();
        //FIXME mProcessImage.prepareNewGame();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //TODO: put into XML
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemDebugCard = menu.add("Debug Cards");
        mItemDebugCard.setCheckable(true);
        mItemDebugMode = menu.add("Debug Mode");
        mItemDebugMode.setCheckable(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == mItemDebugMode) {
            /* Toggle Debug mode */
            if (mProcessImage != null){
                mProcessImage.debug = !mItemDebugMode.isChecked();
                mItemDebugMode.setChecked(mProcessImage.debug);
            }
            //FIXME mProcessImage.prepareNewGame();
        } else if (item == mItemDebugCard) {
            //Toggle check box
            mItemDebugCard.setChecked(!mItemDebugCard.isChecked());
            /* We need to enable or disable drawing of the tile numbers */
            //FIXME mProcessImage.toggleTileNumbers();
        }
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public boolean onTouch(View view, MotionEvent event) {

        //FIXME toggle(); //UI

        if (mItemDebugCard.isChecked() && !camRunning) {
            showCard(view);
            return false;
        }

        if (camRunning) { //pause
            onPause();
            //mOpenCvCameraView.disableView();
            camRunning =false;
            //testImage2(); //debug
        }else{
            onResume();
            //mOpenCvCameraView.enableView();
            camRunning =true;
        }
        return false;


    }


    public void showCard(View v) {
        /* Get next card on touch */
        //imgView.setOnTouchListener(new OnTouchListener(){

        Card cardObj;
        List<Card> cards = mProcessImage.getCards();
        if (cards.size() <= 0) {return;} //no cards found

        setContentView(R.layout.activity_fullscreen);
        ImageView imgView = (ImageView) findViewById(R.id.imageView);
        cardObj = cards.get(currentCard % cards.size());
        Mat cardMat = cardObj.cardImg_markup;
        //Imgproc.cvtColor(cardObj.cardImg_markup, cardMat, Imgproc.COLOR_BGR2BGRA);

        Bitmap img = Bitmap.createBitmap(cardMat.cols(), cardMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cardMat, img);
        imgView.setImageBitmap(img);
        currentCard++;

        //clear image (use either)
        //imgView.setImageResource(android.R.color.transparent);
        //imgView.setImageResource(0);
    }


    private static final int WHAT_PROCESS_IMAGE = 0;
    public Mat onCameraFrame(Mat inputFrame) {


        Handler mHandler = null;
        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == WHAT_PROCESS_IMAGE) {
                    threadPool.post(new Runnable() {
                        @Override
                        public void run() {
                            //do everything
                        }
                    });
                }
                return true;
            }
        });

        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGBA2RGB);  //without this getRectSubPix will crash
        Mat image =  mProcessImage.detectCards(inputFrame);
/*        if (mProcessImage.numCards() >= 3) {
            onPause();
            camRunning =false;
        }*/
        return image;
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }


    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

/*
    // result Integer is passed here after
    // this method is run on main UI thread
    @Override
    protected void onPostExecute(Integer result) {
    }
*/

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


}

