package com.rtpike.setsolver;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.*;



/**
 * Card Class for the Set cards
 * Created by rtpike on 1/19/2016.
 */


public class Card implements Runnable {
    static final boolean debug = true;
    private static final String TAG = "Card";
    public String cardName = null;
    public Thread thread = null;

    /*FEATURES = {"symbol": ["oval", "squiggle", "diamond"],
        "color": ["red", "green", "purple"],
        "amount": [1, 2, 3],
        "shading": ["solid", "open", "striped"]
    }*/

    public Mat cardImg;
    public Mat cardImg_markup = new Mat();
    public Mat cardThreshold = new Mat();
    public Mat cardHSV = new Mat();

    public MatOfPoint2f warpBox;
    public Mat parrentImage;
    public int number = -1; //number of shapes
    public colorEnum color = colorEnum.INVALID;  //red=0,green=1,purple=2
    public shadeEnum shade = shadeEnum.INVALID;  //0=empty, 1=lines, 2=solid
    public shapeEnum shape = shapeEnum.INVALID;  //"oval", "squiggle", "diamond"
    public Point[] corners; //From full parent image

    Card() {
    }


    Card(Mat cardImg) {
        int cropSize = 15;  //crop of 15 pixels per side
        this.cardImg = cardImg.submat(cropSize, cardImg.rows() - cropSize, cropSize, cardImg.cols() - cropSize); //crop off the edges
    }
    /* preload warpBox and input image. Use with the runnable */
    Card(MatOfPoint2f warpBox, Mat in) {
        this.warpBox = warpBox;
        this.parrentImage = warpBox;
    }

    /* Rotate and crop the given image */
    public void processCardWarp(MatOfPoint2f warpBox, Mat in) {

        Mat cropped = new Mat();
        Point points[] = new Point[4];
        points[0] = new Point(0, 0);
        points[1] = new Point(350, 0);
        points[2] = new Point(350, 350);
        points[3] = new Point(0, 350);
        MatOfPoint2f destWarp = new MatOfPoint2f(points);

        Mat transform = Imgproc.getPerspectiveTransform(warpBox, destWarp);
        Imgproc.warpPerspective(in, cropped, transform, new Size(350, 350), Imgproc.INTER_NEAREST);

        int cropSize = 15;  //crop of 15 pixels per side
        this.cardImg = cropped.submat(cropSize, cropped.rows() - cropSize, cropSize, cropped.cols() - cropSize); //crop off the edges
        processCard();
    }

    public void processCard() {


        if (debug) {
            Log.d(TAG, "Card name: " + cardName + " vvvvvvvvvvvvvvvvvvvvvv");
        }
        //cardImg_markup = cardImg;
        detectColor();
        detectShape();

        //debug prints
        if (debug) {

            cardImg_markup.width();
            cardImg_markup.height();
            Point center = new Point(cardImg_markup.width() / 10, cardImg_markup.height() / 8);
            Imgproc.putText(cardImg_markup, decodeCard(), center, Core.FONT_HERSHEY_PLAIN, 2, new Scalar(70, 70, 70));
            //cardImg_markup = cardThreshold;  //debug
            Log.d(TAG, "Card Info: " + decodeCard() + " ^^^^^^^^^^^^^^^^^^^^^^^");

        }

    }

    /* Detect the shape and fill of the card */
    private void detectShape() {
        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat thresh = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

/*        List<Mat> XYZ = new ArrayList<Mat>(3); //debug
        Size ksize = new Size(1, 1);
        Imgproc.cvtColor(cardImg_markup, blur, Imgproc.COLOR_RGB2XYZ);
        split(blur,XYZ);
        gray = XYZ.get(1); //debug*/

        //FIXME: shadows can cause problem with shape detection
        Imgproc.cvtColor(cardImg_markup, gray, Imgproc.COLOR_RGB2GRAY);

        //Imgproc.Canny(gray, thresh, 15, 60, 3, false); //debug
        Imgproc.adaptiveThreshold(gray, thresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C , Imgproc.THRESH_BINARY_INV, 15, 9); //debug
        ///Imgproc.threshold(gray, thresh, 220, 255, Imgproc.THRESH_BINARY_INV);
        //adaptiveCanny(gray,thresh);

        //inRange(cardHSV, new Scalar(0, 25, 200), new Scalar(255, 255, 255), cardThreshold); //debug
        ////blur = cardThreshold; //debug
        //Imgproc.GaussianBlur(thresh, blur, new Size(5,5),0);
        ///Imgproc.dilate(cardThreshold,cardThreshold,Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
        //Imgproc.morphologyEx(cardThreshold,cardThreshold,Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 17)));
        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        ///Core.bitwise_and(cardImg_markup,cardImg_markup,blur,cardThreshold); //mask cardThreshold



        //cardImg_markup = thresh; //Debug
        //cardImg_markup = cardThreshold; //Debug
        double area = 0, validConArea = 0;
        MatOfPoint contours_0 = null;

        int conNum, validConNum = 0;

        int contour_size = contours.size();
        for (conNum = 0; conNum < contour_size; conNum++) {
            area = Imgproc.contourArea(contours.get(conNum));
            if (area > 2000) {//debug
                validConNum++;
                validConArea = area;
                contours_0 = contours.get(conNum);
                Imgproc.drawContours(cardImg_markup, contours, conNum, new Scalar(0, 255, 255), 1);
            }
        }

        if (validConNum <= 3) {
            number = validConNum;
        } else {
            number = -2; //error
        }

        if (number <= 0) {
            return;
        } //invalid number for concurs, exit

        Rect rec = Imgproc.boundingRect(contours_0);
        Point center = new Point(rec.x + rec.width / 2, rec.y + rec.height / 2);

        Rect sampleRec = new Rect(new Point(center.x - rec.width / 8, center.y - rec.height / 8), new Point(center.x + rec.width / 8, center.y + rec.height / 8));

        if (debug) {
            Imgproc.circle(cardImg_markup, center, 4, new Scalar(0, 0, 0));
            Imgproc.rectangle(cardImg_markup, sampleRec.tl(), sampleRec.br(), new Scalar(240, 0, 0));

        }

        double perimeter = Imgproc.arcLength(new MatOfPoint2f(contours_0.toArray()), true);
        double areaVsPer = validConArea / perimeter;
        Mat centerMat = cardHSV.submat(sampleRec);
        Scalar mean = mean(centerMat);

        MatOfPoint2f card_2f = new MatOfPoint2f(contours_0.toArray());
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        //-------------------------------
        Imgproc.approxPolyDP(card_2f, approxCurve, 0.014 * perimeter, true);

        if (debug) {
            Imgproc.putText(cardImg_markup, "" + approxCurve.height() + ", " + String.format("%.2f", areaVsPer), center, Core.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0));
            //Imgproc.draw(approxCurve, contours, conNum, new Scalar(0, 0, 255), 2);
            Point[] curve_vertices = approxCurve.toArray(); //Fixme :faster way?
            for (int p = 0; p < curve_vertices.length; p++) {
                Imgproc.line(cardImg_markup, curve_vertices[p], curve_vertices[(p + 1) % curve_vertices.length], new Scalar(250, 0, 0), 1);
            }
            Log.d(TAG, "    Shape Info: " +
                    " Num Poly corners:  " + approxCurve.height() +
                    " areaVsPer: " + String.format("%.2f", areaVsPer));
            Log.d(TAG, "    Fill Info: " + String.format("%.2f", mean.val[1]));
            Log.d(TAG, "    mean color Info: " + String.format("%.2f", mean.val[0]) + ", " + String.format("%.2f", mean.val[2]));
        }

        /* Fill
         * Solid < 150
         * lines: 240 - 220
         * empty: 255 - 250
         **/
        if (mean.val[1] > 115) {
            shade = shadeEnum.SOLID;
        } else if (mean.val[1] <= 115 && mean.val[1] > 14) {//> 220 && mean.val[1] < 244){
            shade = shadeEnum.LINES;
        } else if (mean.val[1] <= 14) {// 244 && mean.val[1] <= 255) {
            shade = shadeEnum.EMPTY;
        }

        /* Find Shape using area vs perimeter
         * oval: 24 -28
         * squiggle: 18-20
         * demand:  < 18
         **/
        MatOfPoint approxf1 = new MatOfPoint();
        approxCurve.convertTo(approxf1, CvType.CV_32S);
        Boolean isConvex = Imgproc.isContourConvex(approxf1);
        if(debug) {
            Log.d(TAG, "    isConvex: " + isConvex);
        }

        if (isConvex && (areaVsPer > 22 && areaVsPer < 32)) {
            shape = shapeEnum.OVAL;
        } else if (!isConvex && (areaVsPer > 16 && areaVsPer < 26) && (approxCurve.height() >= 7)) {
            shape = shapeEnum.SQUIGGLE;
        } else if (approxCurve.height() <= 6 || areaVsPer < 18) {
            shape = shapeEnum.DIAMOND;
        }

    }

    private void detectColor() {

        Mat sampleMat = null;
        //Mat blur = new Mat();

        //  ----------  White Balance ---------
        //sample "white" pixels at image at corners
        //Imgproc.medianBlur(cardImg, cardImg, 5);
        double[] whitePixel = new double[3];
        //whitePixel = cardImg.get(15, 15);
        sampleMat = cardImg.submat(0, 5, 0, 5);
        //Imgproc.medianBlur(sampleMat, blur, 9);
        //Imgproc.GaussianBlur(sampleMat, blur, new Size(9,9),0);
        whitePixel = mean(sampleMat).val;

        if (debug) {
            Log.d(TAG, "    White sample: " + String.format("[%.0f, %.0f, %.0f]", whitePixel[0], whitePixel[1], whitePixel[2]));
        }

         double[] transMatrix = {256 / whitePixel[0], 0, 0, //Red
                0, 256 / whitePixel[1], 0, //Green
                0, 0, 256 / whitePixel[2]}; //Blue

        Mat whiteCorrect = new Mat(3, 3, CvType.CV_64FC1);
        whiteCorrect.put(0, 0, transMatrix); //FIXME: change to setTo for speed
        transform(cardImg, cardImg_markup, whiteCorrect); //FIXME: try split/merge instead

/*        double[] transMatrix = {256 / whitePixel[0], //Red
                 256 / whitePixel[1],  //Green
                 256 / whitePixel[2]}; //Blue
        colorTransform(cardImg,transMatrix,cardImg_markup);*/
        //  ---------- End White Balance ---------

        Imgproc.cvtColor(cardImg_markup, cardHSV, Imgproc.COLOR_RGB2HSV);

        int rCnt, gCnt, pCnt;
        Mat redThresh0 = new Mat();
        Mat redThresh1 = new Mat();
        Mat greenThresh = null;
        //Red
        inRange(cardHSV, new Scalar(0, 15, 100), new Scalar(15, 255, 245), redThresh0); //red
        inRange(cardHSV, new Scalar(165, 50, 100), new Scalar(180, 255, 245), redThresh1); //pinkish
        bitwise_or(redThresh0,redThresh1,cardThreshold);
        rCnt = countNonZero(cardThreshold);
        //redThresh0 =cardThreshold.clone();

        //Green
        inRange(cardHSV, new Scalar(50, 50, 100), new Scalar(80, 255, 245), cardThreshold);
        //inRange(cardHSV, new Scalar(45, 40, 100), new Scalar(80, 255, 255), cardThreshold);
        gCnt = countNonZero(cardThreshold);
        //greenThresh = cardThreshold.clone();

        //Purple
        inRange(cardHSV, new Scalar(115, 50, 100), new Scalar(150, 255, 245), cardThreshold);
        //inRange(cardHSV, new Scalar(120, 40, 100), new Scalar(165, 255, 255), cardThreshold);
        pCnt = countNonZero(cardThreshold);
        //purpleThresh.clone();

        //cardThreshold = cardHSV; //TODO
        if (rCnt > gCnt && rCnt > pCnt) {
            color = colorEnum.RED;
            //cardThreshold = redThresh0;
        } else if (gCnt > rCnt && gCnt > pCnt) {
            color = colorEnum.GREEN;
            //cardThreshold = greenThresh;
        } else if (pCnt > rCnt && pCnt > gCnt) {
            color = colorEnum.PURPLE;
        }

        if (debug) {
            Log.d(TAG, "    Red cnt:  " + rCnt +
                    " Green cnt:  " + gCnt +
                    " Purple cnt:  " + pCnt);
        }

    }

    /* Card is valid if all properties of the card are detected */
    public boolean isValid() {
        return (color != colorEnum.INVALID && shade != shadeEnum.INVALID &&
                shape != shapeEnum.INVALID && number > 0);

    }

    public String decodeCard() {
        /*  Decode the Card attributes into a string
            example: "2,G,S"
         */
        char colorChar, shadeChar, shapeChar;

        switch (color) {
            case RED:
                colorChar = 'R';
                break;
            case GREEN:
                colorChar = 'G';
                break;
            case PURPLE:
                colorChar = 'P';
                break;
            default:
                colorChar = '?';
                break;
        }

        switch (shade) {
            case EMPTY:
                shadeChar = 'E';
                break;
            case LINES:
                shadeChar = 'L';
                break;
            case SOLID:
                shadeChar = 'S';
                break;
            default:
                shadeChar = '?';
                break;
        }

        switch (shape) {
            case DIAMOND:
                shapeChar = 'D';
                break;
            case OVAL:
                shapeChar = 'O';
                break;
            case SQUIGGLE:
                shapeChar = 'S';
                break;
            default:
                shapeChar = '?';
                break;
        }

        if (cardName != null) {
            return String.format("(%s)%d:%s:%s:%s", cardName, number, colorChar, shadeChar, shapeChar);
        } else {
            return String.format("%d:%s:%s:%s", number, colorChar, shadeChar, shapeChar);
        }
    }

    @Override
    public String toString() {
        return decodeCard();
    }

    /* Color transform */
    private void colorTransform(Mat in, double tran[], Mat out) {

        List<Mat> channels = new ArrayList<Mat>();
        Core.split(in, channels);

        for (int k = 0; k < channels.size(); k++) {

            Mat channel = channels.get(k);

            Size channel_size = channel.size();  //this should have only one channel
            for (int x = 0; x < channel_size.width; x++) {
                for (int y = 0; y < channel_size.height; y++) {
                    try {
                        channel.put(x, y, channel.get(x, y)[0] * tran[k]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Core.merge(channels, out);
        }
    }

    private void adaptiveCanny(Mat in,Mat out) {
        double sigma = 0.33;
        Mat blur= new Mat();
        //Imgproc.medianBlur(in, blur, 15); //TODO: I'm not sure which blur is best
        Imgproc.GaussianBlur(in, blur, new Size(11,11),0);
        Scalar mean = Core.mean(blur);

        /*
        from: http://www.pyimagesearch.com/2015/04/06/zero-parameter-automatic-canny-edge-detection-with-python-and-opencv/
        # apply automatic Canny edge detection using the computed median
        lower = int(max(0, (1.0 - sigma) * v))
        upper = int(min(255, (1.0 + sigma) * v))
        */

        int lower = (int) Math.max(0, (1.0 - sigma) * mean.val[0]);
        int upper = (int) Math.min(255, (1.0 + sigma) * mean.val[0]);

        Imgproc.Canny(blur, out, lower, upper, 3, false);
    }

    /* Used for threading */
    @Override
    public void run() {
        //processCardWarp(warpBox, parrentImage);
        processCard();
    }


    enum colorEnum {
        RED, GREEN, PURPLE, INVALID
    }

    enum shapeEnum {
        OVAL, SQUIGGLE, DIAMOND, INVALID
    }

    enum shadeEnum {
        EMPTY, LINES, SOLID, INVALID
    }
}