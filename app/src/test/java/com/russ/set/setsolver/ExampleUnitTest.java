package com.russ.set.setsolver;

import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.imgcodecs.Imgcodecs;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */


public class ExampleUnitTest {
    private static final String TAG  = "UnitTest";

    @Before
    public void loadLib () {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void detectCardsTest() throws Exception {

        Mat mRgba = new Mat();

        try {
            mRgba = Utils.loadResource(MainDebug.getAppContext(), R.drawable.set_test, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        } catch (Exception except) {
            Log.e(TAG, except.getMessage(), except);
        }

        processImage mProcessImage = new processImage();
        mProcessImage.detectCards(mRgba);

        assertEquals(12, mProcessImage.numCards()

        ); //12 cards should be detected

    }

}