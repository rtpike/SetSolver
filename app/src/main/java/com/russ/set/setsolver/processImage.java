package com.russ.set.setsolver;

import android.content.Context;
import android.graphics.Bitmap;
//import android.os.AsyncTask;
import android.util.Log;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import java.lang.Math;


/**
 * Created by rtpike on 1/21/2016.
 * Identity cards in the camera images
 * Does not decode individual card images
 */
public class processImage { //} extends AsyncTask<Card, Void, Integer> {

    static final boolean debug=true;
    private static final String TAG = "processImage";
    /*
    Detects cards by finding contours
     */

    private List<Card> cards = null; //new ArrayList<Card>();
    private int numCards = 0;
    //private boolean setFound = false;
    private Mat inputImage = null;
    public List<Card[]> mySets = new ArrayList<Card[]>();


    public int numCards() {
        /* Returns number of cards detected */
        return this.numCards;
    }

    public synchronized Mat detectCards() {
        return detectCards(inputImage);
    }

    public synchronized Mat detectCards(Mat in) {

        Mat outMat = in.clone();  //copy of input so we can draw on it without messing up card detection
        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat thresh = new Mat();
        Mat hierarchy = new Mat();
        this.cards = new ArrayList<>();
        mySets = new ArrayList<Card[]>();

        double peri, area;
        List<MatOfPoint> contours = new ArrayList<>();
        //Size ksize = new Size(1, 1);

        Imgproc.cvtColor(in, gray, Imgproc.COLOR_RGB2GRAY);

        //Imgproc.GaussianBlur(gray, blur, ksize, 500);
        //Imgproc.Canny(gray, thresh, 1500, 3000, 5, false);
        adaptiveCanny(gray, thresh);
        ///Imgproc.threshold(gray, thresh, 155, 255, Imgproc.THRESH_BINARY);
        ///Imgproc.medianBlur(gray, blur, 11);
        //Imgproc.adaptiveThreshold(gray, thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 1);
        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //FIXME Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //in = thresh; //debug
        //Imgproc.drawContours(thresh, contours, 0, new Scalar(0, 100, 255),-1);

        int numCards = 0;
        int contours_size = contours.size();
        for (int i = 0; i < contours_size; i++) {
            //System.out.println(Imgproc.contourArea(contours.get(i)));

            MatOfPoint card = contours.get(i);
            MatOfPoint2f card_2f = new MatOfPoint2f(card.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();

            peri = Imgproc.arcLength(card_2f, true);
            //approx = rectify( );
            Imgproc.approxPolyDP(card_2f, approxCurve, 0.02 * peri, true);

            area = Imgproc.contourArea(approxCurve);
            RotatedRect rect;
            if (area > 4000) {
                //FIXME Rect rect = Imgproc.boundingRect(contours.get(i));
                rect = Imgproc.minAreaRect(approxCurve);
                Point[] curve_vertices = approxCurve.toArray();
                if (curve_vertices.length == 4) {
                    numCards++;

/*                    Point[] vertices;
                    if (debug) {
                        vertices = new Point[4];
                        rect.points(vertices);
                        for (int j = 0; j < 4; j++) {
                            Imgproc.line(in, vertices[j], vertices[(j + 1) % 4], new Scalar(100, 100, 255));
                        }
                        //Imgproc.rectangle(in, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
                    }*/

                    //Mat ROI = in.submat(rectB.y, rectB.y + rectB.height, rectB.x, rectB.x + rectB.width);
                    //FIXME rotateSubImage(rect, in, ROI);

                    Mat ROI = new Mat();
                    warpSubImage(rect, approxCurve, in, ROI);

                    Card cardObj = new Card(ROI);
                    //Card cardObj = new Card();
                    //Card cardObj = new Card(approxCurve, in);;
                    cardObj.cardName = String.format("%d", numCards); //used for debugging
                    cardObj.corners = curve_vertices.clone();  //corners of card
                    cards.add(cardObj);  // add to list

                    //cardObj.processCard();
                    //cardObj.processCardWarp(approxCurve, in);

                    cardObj.thread = new Thread(cardObj);
                    cardObj.thread.start();

                }
                //FIXME if (true) return cardObj.cardImg_markup; //debug
            }
        }


        this.numCards = numCards; //class var

        // highlight cards
        for (int i = 0; i < cards.size(); i++) {
            Card cardObj = cards.get(i);
            if (cardObj.thread != null ) {
                try {
                    cardObj.thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            //FIXME while (cardObj.getStatus() != AsyncTask.Status.FINISHED){ } //wait until card is done
            Scalar color;
            if (cardObj.isValid()) {
                color = new Scalar(0, 0, 255); //blue
            } else {
                color = new Scalar(255, 0, 0); //Red
            }

            if (debug) {
                //FIXME Imgproc.polylines(outMat,)
                int p = 0;
                for (p = 0; p < cardObj.corners.length; p++) {
                    Imgproc.line(outMat, cardObj.corners[p], cardObj.corners[(p + 1) % cardObj.corners.length], color, 3);
                }
            }

            if (debug) {  //debug
                if (cardObj.corners[0].x < cardObj.corners[1].x) {
                    Imgproc.putText(outMat, cardObj.decodeCard(), cardObj.corners[0], Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 240, 240));
                } else {
                    Imgproc.putText(outMat, cardObj.decodeCard(), cardObj.corners[1], Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 240, 240));
                }
            }
        }
        // Search for a set and draw a green box around the cards
        Card setCards[] = new Card[3];



        if (findSet(mySets)) {
            for (int c = 0; c < mySets.size(); c++) { //Set (3 cards)
                 Scalar color = new Scalar(c*10, 255-c*45, c*45); //Green

                setCards = mySets.get(c); //TODO: got trough all lists
                Log.d(TAG, "Set:");

                for (int i = 0; i < setCards.length; i++) { //Card
                    Log.d(TAG, "    " + setCards[i].decodeCard());

                    for (int p = 0; p < setCards[i].corners.length; p++) { //Draw Boarder Lines
                        Imgproc.line(outMat, setCards[i].corners[p], setCards[i].corners[(p + 1) % 4], color, 7);
                    }
                }
            }
        }

        return outMat;
    }

    public void rotateSubImage(RotatedRect rect, Mat in, Mat cropped) {
        /* Rotate and crop the given image */

        // matrices we'll use
        Mat rotMat, out = new Mat();
        // get angle and size from the bounding box
        double angle = rect.angle;
        Size rect_size = rect.size;

        if (rect.angle < -45.) {
            angle += 90.0;
            Double tmp;
            tmp = rect_size.width;
            rect_size.width = rect_size.height;
            rect_size.height = tmp;
        }

        //Get rid of card boarders
        rect_size.width = rect_size.width * 0.9;
        rect_size.height = rect_size.height * 0.9;

        // get the rotation matrix
        rotMat = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0);  // <<<<<<<<<<<<
        // perform the affine transformation
        Imgproc.warpAffine(in, out, rotMat, in.size(), Imgproc.INTER_CUBIC); //<<<<<<<<<<<<
        // crop the resulting image
        Imgproc.getRectSubPix(out, rect_size, rect.center, cropped);


    }

    public void warpSubImage(RotatedRect rect,MatOfPoint2f warpBox, Mat in, Mat cropped) {
        /* Rotate and crop the given image */

        // get the rotation matrix
        //rotMat = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0);  // <<<<<<<<<<<<
        // perform the affine transformation
        //Imgproc.warpAffine(in, out, rotMat, in.size(), Imgproc.INTER_CUBIC); //<<<<<<<<<<<<
        // crop the resulting image
        //Imgproc.getRectSubPix(out, rect_size, rect.center, cropped);

        Point points[] = new Point[4];
        points[0] =  new Point(0,0);
        points[1] =  new Point(350,0);
        points[2] =  new Point(350,350);
        points[3] =  new Point(0,350);
        MatOfPoint2f destWarp = new MatOfPoint2f(points);

        Mat transform = Imgproc.getPerspectiveTransform(warpBox,destWarp);
        //poker card size 822x1122
        Imgproc.warpPerspective(in, cropped, transform, new Size(350, 350), Imgproc.INTER_NEAREST);

    }


    private void adaptiveCanny(Mat in,Mat out) {
        double sigma = 0.33;
        Mat blur= new Mat();
        //FIXME Imgproc.medianBlur(in, blur, 11); //TODO: I'm not sure which blur is faster
        Imgproc.GaussianBlur(in, blur, new Size(11,11),0);
        Scalar mean = Core.mean(blur);

/*        # apply automatic Canny edge detection using the computed median
        lower = int(max(0, (1.0 - sigma) * v))
        upper = int(min(255, (1.0 + sigma) * v))*/

        int lower = (int) Math.max(0, (1.0 - sigma) * mean.val[0]);
        int upper = (int) Math.max(0, (1.0 + sigma) * mean.val[0]);

        Imgproc.Canny(blur, out, lower, upper, 5, true);
    }

    public void loadTestImage(Context context, int resourceId) {
        try {
            //Mat tempMat = new Mat();
            inputImage = Utils.loadResource(context, resourceId, Imgcodecs.CV_LOAD_IMAGE_COLOR);
            Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_BGR2RGB);

        } catch (Exception except) {
            Log.e(TAG, except.getMessage(), except);
        }

    }

    public Bitmap getBitmap() {
        Bitmap img = Bitmap.createBitmap(inputImage.cols(), inputImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputImage, img);
        return img;
        //imgView.setImageBitmap(img);

    }

    public List<Card> getCards() { return cards;}

    /* brute force method to find a set
    */
    public boolean findSet(List<Card[]> setCards) {

        //setCards = new Card[3];
        //At least three cards are needed to find a set
        int card_size = cards.size();
        if (card_size < 3 ) { return false; }

        for (int i=0;i < card_size; i++ ){
            for (int j=i+1;j < card_size;j++){
                for (int k=j+1;k < card_size;k++){
                    if (isSet(cards.get(i),cards.get(j),cards.get(k))) { //FIXME: find all sets
                        setCards.add(new Card[] {cards.get(i),cards.get(j),cards.get(k)});
/*                        setCards[0] = cards.get(i);
                        setCards[1] = cards.get(j);
                        setCards[2] = cards.get(k);
                        return true; //set found*/
                    }
                }
            }
        }

        // no set found
        return setCards.size() > 0;
    }


    private boolean isSet(Card card0,Card card1,Card card2){
        /*
        private  int number=-1; //number of shapes
        private  colorEnum color=colorEnum.INVALID;  //red=0,green=1,purple=2
        private  shadeEnum shade=shadeEnum.INVALID;  //0=empty, 1=lines, 2=solid
        private  shapeEnum shape=shapeEnum.INVALID;  //"oval", "squiggle", "diamond"
        */

        boolean numberSet=false;
        boolean colorSet=false;
        boolean shadeSet=false;
        boolean shapeSet=false;

        //if any of the cards are not valid bale
        if (!(card0.isValid() && card1.isValid() && card2.isValid())) {
            return false;
        }

        numberSet = (card0.number == card1.number && card1.number == card2.number ) ||
                (card0.number != card1.number && card1.number != card2.number && card0.number != card2.number );

        colorSet = (card0.color == card1.color && card1.color == card2.color ) ||
                (card0.color != card1.color && card1.color != card2.color && card0.color != card2.color);

        shadeSet = (card0.shade == card1.shade && card1.shade == card2.shade ) ||
                (card0.shade != card1.shade && card1.shade != card2.shade && card0.shade != card2.shade);

        shapeSet = (card0.shape == card1.shape && card1.shape == card2.shape ) ||
                (card0.shape != card1.shape && card1.shape != card2.shape && card0.shape != card2.shape);

        return numberSet && colorSet && shadeSet && shapeSet;
    }

    public Mat findLargestRectangle(Mat original_image) {
        Mat imgSource = original_image.clone();
        //Mat untouched = original_image.clone();

        //convert the image to black and white
        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BGR2GRAY);

        //convert the image to black and white does (8 bit)
        Imgproc.Canny(imgSource, imgSource, 50, 50);

        //apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource, imgSource, new Size(5, 5), 5);

        //find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        int maxAreaIdx = -1;

        if (contours.size() <= 0) {
            return original_image;
        }
        MatOfPoint temp_contour;// = contours.get(0); //the largest is at the index 0 for starting point
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f maxCurve = new MatOfPoint2f();
        List<MatOfPoint> largest_contours = new ArrayList<>();
        //create the new image here using the largest detected square
        //FIXME vvv
        Mat new_image = new Mat(imgSource.size(), CvType.CV_8U); //we will create a new black blank image with the largest contour
        Imgproc.cvtColor(new_image, new_image, Imgproc.COLOR_BayerBG2RGB);
        //FIXME ^^^
        int contours_size = contours.size();
        for (int idx = 0; idx < contours_size; idx++) {
            temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            //compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                //check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                int contourSize = (int) temp_contour.total();
                Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.05, true);
                if (approxCurve.total() == 4) {
                    maxCurve = approxCurve;
                    maxArea = contourarea;
                    maxAreaIdx = idx;
                    largest_contours.add(temp_contour);
                }
            }
        }

        //maxAreaIdx = idx;
        //create the new image here using the largest detected square
        //FIXME Mat new_image = new Mat(imgSource.size(), CvType.CV_8U); //we will create a new black blank image with the largest contour
        //Imgproc.cvtColor(new_image, new_image, Imgproc.COLOR_BayerBG2RGB);
        Imgproc.drawContours(new_image, contours, maxAreaIdx, new Scalar(255, 255, 255), 1); //will draw the largest square/rectangle

        double temp_double[] = maxCurve.get(0, 0);
        if (temp_double == null) {
            return new_image;
        }
        Point p1 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p1.x, p1.y), 20, new Scalar(255, 0, 0), 5); //p1 is colored red
        String temp_string = "Point 1: (" + p1.x + ", " + p1.y + ")";

        temp_double = maxCurve.get(1, 0);
        if (temp_double == null) {
            return new_image;
        }
        Point p2 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p2.x, p2.y), 20, new Scalar(0, 255, 0), 5); //p2 is colored green
        temp_string += "\nPoint 2: (" + p2.x + ", " + p2.y + ")";

        temp_double = maxCurve.get(2, 0);
        if (temp_double == null) {
            return new_image;
        }
        Point p3 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p3.x, p3.y), 20, new Scalar(0, 0, 255), 5); //p3 is colored blue
        temp_string += "\nPoint 3: (" + p3.x + ", " + p3.y + ")";

        temp_double = maxCurve.get(3, 0);
        if (temp_double == null) {
            return new_image;
        }
        Point p4 = new Point(temp_double[0], temp_double[1]);
        Imgproc.circle(new_image, new Point(p4.x, p4.y), 20, new Scalar(0, 255, 255), 5); //p1 is colored violet
        temp_string += "\nPoint 4: (" + p4.x + ", " + p4.y + ")";

        //TextView temp_text = (TextView)MainActivity.findViewById(R.id.TextView);
        //temp_text.setText(temp_string);
 /*           }
        }}
 */
        return new_image;
    }

    private Mat testImage() {

        //Debug
        Mat image = new Mat(new Size(800,480),CvType.CV_8UC1);
        Mat rgbLoadedImage = new Mat();
        //File root = Environment.getExternalStorageDirectory();
        //context.getResources().getDrawable(R.drawable.set_test)''
        String imageUri = "drawable://" + R.drawable.set_test2;
        //File file = new File(getAssets().open("set_test.jpg");
        image = Imgcodecs.imread(imageUri);
        //image = Utils.loadResource(MainActivity, R.drawable.test, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        //image = Imgcodecs.imread(file.getAbsolutePath(), Imgproc.COLOR_BGR2GRAY);

        if(image.width()>0)

        {

            rgbLoadedImage = new Mat(image.size(), image.type());
            Imgproc.cvtColor(image, rgbLoadedImage, Imgproc.COLOR_BGR2RGB);

            if (debug)
                Log.d(TAG, "loadedImage: " + "chans: " + image.channels()
                        + ", (" + image.width() + ", " + image.height() + ")");

            image.release();
            image = null;
        }

        return rgbLoadedImage;
    }



/*
    // run detection method in background thread
    // takes in parameter in the .execute(Mat mGray) call on the class that is created
    @Override
    protected Integer doInBackground(Card... params ) { //Mat... params) {
        Log.d(TAG, "background task started: ");

        Card cardObj = params[0];

*//*        Card cardObj = new Card(ROI);
        cardObj.cardName = String.format("%d",numCards); //used for debugging
        cardObj.corners = curve_vertices.clone();  //corners of card
        cards.add(cardObj);  // add to list*//*
        cardObj.processCard();

        return 1;
    }*/

/*    // result Integer is passed here after
    // this method is run on maing UI thread
    @Override
    protected void onPostExecute(Integer result) {

        Log.i(TAG,"Card DETECTION Done");

        // add methods here to be executed after circle is detected

        // stop blocking and allow the next frame to be started
        //detectionRunning = false;

    }*/
}
