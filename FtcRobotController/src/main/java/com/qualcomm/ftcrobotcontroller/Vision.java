package com.qualcomm.ftcrobotcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.qualcomm.ftcrobotcontroller.opmodes.depreciated.PostNNTeleOp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Matt on 12/27/2015.
 */
public class Vision {
    private static final int FIRST_LABEL = 0;
    private static final int MIN_NEEDED_TO_BE_AN_EDGE = 8;
    private static final int DIFFERENCE_IN_RADIUS_FOR_RECTANGLE_BOUNDS = 1;
    private static final double TOLERANCE_FOR_RADIUS_DIFFERENCE = .7;

    public static int FOCUS_TIME = 2400;
    public static int RETRIEVE_FILE_TIME = FOCUS_TIME + 1500;

    public static double EDGE_THRESHOLD = 85;
    public static double CONTRAST_ADJUSTMENT = .85;
    public static int BRIGHTNESS_ADJUSTMENT = 0;
    public static double LOWER_BOUNDS_BLUE_HUE = 172;
    public static double UPPER_BOUNDS_BLUE_HUE = 240;
    //red is right where the circle turns around
    public static double LOWER_BOUNDS_PINK_HUE = 290;
    public static double UPPER_BOUNDS_RED_HUE = 15;
    public static double UPPER_BOUNDS_BLUE_VIBRANCY = 50;
    public static double UPPER_BOUNDS_BLUE_SATURATION = 50;
    public static double UPPER_BOUNDS_RED_VIBRANCY = 80;
    public static int CONVERTGRAYSCALETOEDGED_DATA_NUMBER_OF_LABELS = 0;
    public static int CONVERTGRAYSCALETOEDGED_DATA_BITMAP = 1;
    public static int CONVERTGRAYSCALETOEDGED_DATA_EDGETHRESHOLDUSED=2;
    public static int CONSOLIDATEEDGES_DATA_NUMBEROFCHANGES = 0;
    public static int CONSOLIDATEEDGES_DATA_TOTALLABELS = 1;
    public static int CONSOLDIATEEDGES_DATA_BITMAP = 2;
    public static int REMOVERANDOMNESS_DATA_LABELS = 0;
    public static  int REMOVERANDOMNESS_DATA_BITMAP = 1 ;
    public static int RETURNCIRCLES_DATA_BITMAP = 0;
    public static int RETURNCIRCLES_DATA_XYCOORSCENTER = 1;
    public static double THRESHOLD_FOR_CENTERS_OF_TWO_BUTTONS = 2;

    //these values are all based off of the color wheel
    public static String findViaSplitImageInHalfAndSeeWhichColorIsOnWhichSide(Bitmap image) {
        //find the avg hue for right side, find avg hue for left side
        //compare
        //blue hues tend to be unvariably 180, so like 170 - 190
        //red hues tend to be pink or purple, so somehwere around 295 higher (inaccurate from our testing). but, they can also be very low numbereed hued reds so like 15
        //so basically we can say that blues will be less than reds, since the red comes out as pink
        int xMidPoint = image.getWidth() / 2;
        double avgHueLeft = 0;
        int pixelCounter = 0;
        for (int i = 0; i < xMidPoint; i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                int pixel = image.getPixel(i, j);
                //pixel is in rgb value. let's break this into hsv values
                // hsv[0] is Hue [0 .. 360)
                // hsv[1] is Saturation [0...1]
                // hsv[2] is Value [0...1]
                pixelCounter++;
                float[] hsv = new float[3];
                Color.colorToHSV(pixel, hsv);
                avgHueLeft += hsv[0];
            }
        }
        //done getting left side
        avgHueLeft = avgHueLeft / pixelCounter;
        //let's do for right side
        double avgHueRight = 0;
        pixelCounter = 0;
        for (int i = image.getWidth() - 1; i > xMidPoint; i--) {
            for (int j = 0; j < image.getHeight(); j++) {
                int pixel = image.getPixel(i, j);
                //pixel is in rgb value. let's break this into hsv values
                // hsv[0] is Hue [0 .. 360)
                // hsv[1] is Saturation [0...1]
                // hsv[2] is Value [0...1]
                pixelCounter++;
                float[] hsv = new float[3];
                Color.colorToHSV(pixel, hsv);
                avgHueRight += hsv[0];
            }
        }
        //done getting right side
        //compare and return
        String returnThis = "avgHueLeft:" + avgHueLeft + "avgHueRight:" + avgHueRight;
        if (isRedHue(avgHueLeft)) {
            //left was red
            returnThis += "left=red";
            return returnThis;
        } else if (isBlueHue(avgHueLeft)) {
            returnThis += "left=blue";
            return returnThis;
        } else {
            returnThis += "left=unknown";
        }
        //if you're here, it means we're unknown
        //so let's check the right side
        if (isRedHue(avgHueRight)) {
            //right was red
            returnThis += "right=red";
            return returnThis;
        } else if (isBlueHue(avgHueRight)) {
            returnThis += "right=blue";
            return returnThis;
        } else {
            returnThis += "right=unknown";
            //well that's not good

        }
        return returnThis;

    }

    public static boolean isRedHue(double hue) {
        //returns if the color is red, based solely on hue. inaccuracies with saturataion/vibrancy variables
        return 0 < hue && hue < UPPER_BOUNDS_RED_HUE || hue > LOWER_BOUNDS_PINK_HUE;
    }

    public static boolean isBlueHue(double hue) {
        //returns if the color is blue, based solely on hue. inaccuracies with saturataion/vibrancy variables
        return LOWER_BOUNDS_BLUE_HUE < hue && hue < UPPER_BOUNDS_BLUE_HUE;
    }

    public static boolean isRed(double hue, double V) {
        //it is too dark to be red, lightness doesnt really matter for red since lighter red is pink which is basically red
        return V >= UPPER_BOUNDS_BLUE_VIBRANCY && (0 < hue && hue < UPPER_BOUNDS_RED_HUE || hue > LOWER_BOUNDS_PINK_HUE);
        /* UNSIMPLIFIED CODE cuz this makes more sense then the random stuff up there
        if (V<UPPER_BOUNDS_BLUE_VIBRANCY) {
            //it is too dark to be red, lightness doesnt really matter for red since lighter red is pink which is basically red
            return false;
        }
        if (0<hue&&hue<UPPER_BOUNDS_RED_HUE||hue>LOWER_BOUNDS_PINK_HUE) {
            //it's red
            return true;
        }
        return false;
         */
    }

    public static boolean isBlue(double hue, double S, double V) {
        //becuase blue can be less vibrant and still be blue, as well as lighter and still be blue, there's more lenienacy for "blue"
        return !(V < UPPER_BOUNDS_BLUE_VIBRANCY && S < UPPER_BOUNDS_BLUE_SATURATION) && LOWER_BOUNDS_BLUE_HUE < hue && hue < UPPER_BOUNDS_BLUE_HUE;
        /* UNSIMPLIFIED CODE (because this makes more sense then the random stuff up there.
        if (V<UPPER_BOUNDS_BLUE_VIBRANCY&& S<UPPER_BOUNDS_BLUE_SATURATION) {

            return false;
            //becuase blue can be less vibrant and still be blue, as well as lighter and still be blue, there's more lenienacy for "blue"

        }
        if (LOWER_BOUNDS_BLUE_HUE<hue&&hue<UPPER_BOUNDS_BLUE_HUE) {
            // it's blue
            return true;
        }
        return false;
        */
    }

    public static double toLum(int r, int g, int b) {
        //luminace returns between 0, 255, 0 being black and 255 being white
        //formula from https://github.com/rayning0/Princeton-Algorithms-Java/blob/master/introcs/Luminance.java
        return .299 * r + .587 * g + .114 * b;
    }

    public static Bitmap toGrayscaleBitmap(Bitmap original) {

        Bitmap palet = original.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < palet.getWidth(); i++) {
            for (int j = 0; j < palet.getHeight(); j++) {
                //getPixel returns a color int of the pixel
                int red = Color.red(palet.getPixel(i, j));
                int green = Color.green(palet.getPixel(i, j));
                int blue = Color.blue(palet.getPixel(i, j));
                int lum = (int) (Math.round(toLum(red, green, blue)));
                palet.setPixel(i, j, Color.argb(255, lum, lum, lum));
            }
        }
        return palet;

    }

    public static ArrayList<Object> convertGrayscaleToEdged(Bitmap grayscale, double edgeThresholdUsed) {
        //sort through image matrix pixxel by pixel
        //for each pixel, analyze each of the 8 pixels surrounding it

        //record the value of the darkest pixel, and the lightest pixel

        // if (darkest_pixel_value - lightest_pixel_value) > threshold)
        //then rewrite that pixel as 1;
        //else rewrite that pixel as 0;
        //255 = white
        //0 = dark;
        //so we need to find max and min
        Bitmap clean = Bitmap.createBitmap(grayscale.getWidth(), grayscale.getHeight(), Bitmap.Config.ARGB_8888);
        //make entire bitmap white
        for (int i = 0; i < clean.getWidth(); i++) {
            for (int j = 0; j < grayscale.getHeight(); j++) {
                clean.setPixel(i, j, Color.WHITE);
            }
        }
        //Log.e("Done with clean", "DONE");
        int label = Vision.FIRST_LABEL;
        for (int i = 0; i < grayscale.getWidth(); i++) {
            for (int j = 0; j < grayscale.getHeight(); j++) {
                int max = 0;
                int min = 255;
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        int x = i + a;
                        int y = j + b;
                        //check bottom left first, then moves down, then next column
                        //greyscale = all rgb values are equal to luminace value
                        if (!(x < 0 || y < 0 || x >= grayscale.getWidth() || y >= grayscale.getHeight() || (a == 0 && b == 0))) {
                            //if not any of these conditions, then it's a real pixel
                            //get the lum and see if it's a max or min
                            int pixel = grayscale.getPixel(x, y);
                            int lum = Color.red(pixel);
                            if (lum > max) {
                                max = lum;
                            }
                            if (lum < min) {
                                min = lum;
                            }
                        }
                    }
                }
                //ok so now u set max/min for this group of surrounding pixels
                //if darkest - lightest > threshold, then it's an edge
                if (max - min > edgeThresholdUsed) {
                    //his is an edge
                    //mark i,j as an edge

                    //if neighbours are unlabled, label pixel with label. increment label by one.
                    //else label the neighbor label
                    boolean neighborHasLabel = false;
                    for (int a = -1; a <= 1; a++) {
                        for (int b = -1; b <= 1; b++) {
                            int x = i + a;
                            int y = j + b;
                            if (!(x < 0 || y < 0 || x >= grayscale.getWidth() || y >= grayscale.getHeight() || (a == 0 && b == 0))) {
                                int pixel = clean.getPixel(x, y);
                                if (Color.red(pixel) != 255) {
                                    //if neighbor pixel is not 0, then it has a label
                                    //if it finds itself, it should still be equal to 0 so no problems there
                                    clean.setPixel(i, j, Color.argb(255, Color.red(pixel), Color.red(pixel), Color.red(pixel)));
                                    neighborHasLabel = true;
                                }
                            }
                        }
                    }
                    //if after checking all labels, neighborHasLabel is still fallse, then sit it to label and icrement label
                    if (neighborHasLabel == false) {
                        clean.setPixel(i, j, Color.argb(255, label, label, label));
                        label++;
                    }
                }


            }
        }
        ArrayList<Object> data = new ArrayList();
        //let the first bitmap be raw, second be made pretty
        data.add(CONVERTGRAYSCALETOEDGED_DATA_NUMBER_OF_LABELS, label);
        //add in a raw edge with no shading
        //ok so now you have a graph with a bunch of labeled pixels.
        if (label > 255) {
            //we have a problem because then we cant scale values to different shades of grade
            Log.e("ERROR", "label overflow" + label);
        } else if (label == 50) {
            Log.e("LOL", "got fifty shades of gray");
        } /*else {
            //shade the gray
            //shade it to 255/label
            //ok so now you've found edge, and successfully labeled it
            for (int i = 0; i < grayscale.getWidth(); i++) {
                for (int j = 0; j < grayscale.getHeight(); j++) {
                    if (Color.red(clean.getPixel(i, j)) != 255) {
                        //if it's not white, dont do this
                        int shade = (255 / (label + 1)) * Color.red(clean.getPixel(i, j));
                        clean.setPixel(i, j, Color.argb(255, shade, shade, shade));
                    }
                }
            }

        }*/
        //beautiful. now you've got a shaded grayed image. EDITED: WE DONT WANT BEAUTY ANYMORE, it was screwing around. plus black = 0 so whatever
        data.add(CONVERTGRAYSCALETOEDGED_DATA_BITMAP, clean);
        data.add(CONVERTGRAYSCALETOEDGED_DATA_EDGETHRESHOLDUSED, edgeThresholdUsed);
        return data;   //not clean anymore doe
    }

    public static Bitmap applyContrastBrightnessFilter(Bitmap coloredImage, double contrastFactor, int brightnessFactor) {
        //f(x) = ax + B, where a>1 means more contrast and 0<a<1 means less contrast
        //b = brightness
        //for easier separation of "brightness" and contrast" do
        //f(x) = a(x-128) +128+b, where x = rgb value
        // contrast correction factor = 259(C+255)/ 255(259-c)
        //where C = the desried level of contrast, negative = less contrast
        //formula from http://math.stackexchange.com/questions/906240/algorithms-to-increase-or-decrease-the-contrast-of-an-image
        //and http://www.dfstudios.co.uk/articles/programming/image-programming-algorithms/image-processing-algorithms-part-5-contrast-adjustment/
        Bitmap mutableEdited = coloredImage.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < mutableEdited.getWidth(); i++) {
            for (int j = 0; j < mutableEdited.getHeight(); j++) {
                int red = Color.red(coloredImage.getPixel(i, j));
                int green = Color.green(coloredImage.getPixel(i, j));
                int blue = Color.blue(coloredImage.getPixel(i, j));

                int improvedRed = (int) (contrastFactor * (red - 128) + 128 + brightnessFactor);
                int improvedBlue = (int) (contrastFactor * (blue - 128) + 128 + brightnessFactor);
                int improvedGreen = (int) (contrastFactor * (green - 128) + 128 + brightnessFactor);
                mutableEdited.setPixel(i, j, Color.argb(255, improvedRed, improvedGreen, improvedBlue));

            }
        }
        return mutableEdited;
    }

    public static ArrayList<Object> consolidateEdges(Bitmap clumpy, int totalLabels) {
        clumpy = clumpy.copy(Bitmap.Config.ARGB_8888,true);
        boolean didIMakeAnEdit;
        int numberOfChanges = 0;
        do {
            //before, reset didImake an edit. don't know if I made an edit yet.
            didIMakeAnEdit=false;
            for (int i = 0; i < clumpy.getWidth(); i++) {
                for (int j = 0; j < clumpy.getHeight(); j++) {
                    if (Color.red(clumpy.getPixel(i, j)) != 255) {
                        //if the pixel color is not white, meaning that it IS an edge
                        //check neighboring pixels and set it to lowest "label" value
                        int minLabel = Color.red(clumpy.getPixel(i, j));
                        for (int a = -1; a <= 1; a++) {
                            for (int b = -1; b <= 1; b++) {
                                int x = i + a;
                                int y = j + b;
                                if (!(x < 0 || y < 0 || x >= clumpy.getWidth() || y >= clumpy.getHeight() || (a == 0 && b == 0))) {
                                    //make sure no errors thrown
                                    int neighborsLabel = Color.red(clumpy.getPixel(x, y));
                                    //Log.e("data","neighbor"+neighborsLabel+"min"+minLabel);
                                    if (neighborsLabel < minLabel) {
                                        minLabel = neighborsLabel;
                                        //i made a change
                                        didIMakeAnEdit = true;
                                    }
                                }
                            }
                        }
                        //great now we've got smallest label surrounding
                        if (didIMakeAnEdit) {
                            clumpy.setPixel(i, j, Color.argb(255, minLabel, minLabel, minLabel));
                            numberOfChanges++;
                        }
                    }

                }
            }
            //Log.e("didMakEdit",String.valueOf(didIMakeAnEdit));

        } while (didIMakeAnEdit == true);

        ArrayList<Object>data = new ArrayList<Object>();
        totalLabels =  getNumberOfLabelsAssumingOrganized(clumpy);
        data.add(CONSOLIDATEEDGES_DATA_NUMBEROFCHANGES,numberOfChanges);
        data.add(CONSOLIDATEEDGES_DATA_TOTALLABELS,totalLabels);
        data.add(CONSOLDIATEEDGES_DATA_BITMAP,clumpy);
        return data;  //although not clumpy anymore
    }

    public static int getNumberOfLabelsAssumingOrganized(Bitmap image) {
        int label = 0;
        for (int i =0; i< image.getWidth();i++) {
            for (int j =0; j< image.getHeight();j++) {
                if (Color.red(image.getPixel(i,j))!=255) {
                    int myLabel = Color.red(image.getPixel(i,j));
                    if (myLabel>label) {
                        label=myLabel;
                    }
                }
            }
        }
        return label;
    }

    public static ArrayList<Object> getRidOfRandomEdges (Bitmap image, int labels) {
        //image= image.copy(Bitmap.Config.ALPHA_8,true);
        //that jusut makes sure it's mutable
        int [] labelCount = new int[labels+1];
        for (int i =0; i <image.getWidth();i++) {
            for (int j =0; j<image.getHeight();j++) {
                if (Color.red(image.getPixel(i,j))!=255) {
                    //if it's not white
                    labelCount[Color.red(image.getPixel(i,j))]++;
                    //increase the counter within the slot of labels
                }
            }
        }
        //let's log this out
        String labelCountString = "";
        for (int i=Vision.FIRST_LABEL;i<labelCount.length;i++) {
            labelCountString+="("+i+","+labelCount[i]+")";
        }
        Log.e("labelCount",labelCountString);
        int howManyRemoved = 0;
        boolean [] representsIfLabelIsRepresented = new boolean [labels+1];
        for (int i=0;i<representsIfLabelIsRepresented.length;i++) {
            representsIfLabelIsRepresented[i]=true;
        }
        for (int label = Vision.FIRST_LABEL;label<labelCount.length;label++) {
            if (labelCount[label]<=Vision.MIN_NEEDED_TO_BE_AN_EDGE) {
                //remove all these
                howManyRemoved++;
                representsIfLabelIsRepresented[label]=false;
                for (int x = 0; x<image.getWidth();x++) {
                    for (int y= 0; y<image.getHeight();y++) {
                        if (Color.red(image.getPixel(x,y))==label) {
                            //since we determined the label was too less, remove all parts of it
                            image.setPixel(x,y,Color.argb(255,255,255,255));

                        }
                    }
                }
            }
            //implicit else: then it has enough pixels to be an edge, leave it
        }


        //this will organize it
        image =(Bitmap)convertGrayscaleToEdged(image, EDGE_THRESHOLD).get(1);
        labels = getNumberOfLabelsAssumingOrganized(image);
        ArrayList<Object> data = new ArrayList();
        data.add(REMOVERANDOMNESS_DATA_LABELS,labels);
        data.add(REMOVERANDOMNESS_DATA_BITMAP,image);
        return data;

    }

    public static ArrayList<Integer> getLabels(Bitmap src) {
        ArrayList<Integer> data = new ArrayList<Integer>();
        for (int i =0; i <src.getWidth();i++) {
            for (int j =0; j <src.getHeight();j++) {
                if (!data.contains(Color.red(src.getPixel(i,j)))&&Color.red(src.getPixel(i,j))!=255) {
                    data.add(Color.red(src.getPixel(i,j)));
                }
            }
        }
        return data;
    }

    public static ArrayList<Object> returnCircles (Bitmap image, int numOfLabels) {
        Log.e("starting","circle");
        //for each label, determine if it's a circle
        //find a bounding rectangle to find the center and radius of the circle.
        //calculate distance from edge to center
        //if equal to radius or almost equal to radius, for ALL of them, then it's a circle.
        //will have to check for tolerances
        //also elminate radius equal to one
        ArrayList<XYCoor> centers = new ArrayList<XYCoor>();
        for (int label = 0; label <numOfLabels;label++) {
            //so for each label, determine if it's a circle
            //comb thru the image vertically first to determine left most pixel
            image = image.copy(Bitmap.Config.ARGB_8888,true);
            //make image mutable
            XYCoor leftMost = new XYCoor();

            for (int i =0; i <image.getWidth()&&leftMost.getX()==-1;i++) {
                for (int j =0; j<image.getHeight()&&leftMost.getX()==-1;j++) {
                    //if leftMost.getX is set, exit out
                    if( Color.red( image.getPixel(i,j)) ==label) {
                        //cool u found the label
                        //Log.e("found",leftMost.toString());
                        leftMost= new XYCoor(i,j);
                    }

                }
            }
            //now do for right most. so this, you will satrt from right side going to 0
            XYCoor rightMost = new XYCoor();

            for (int i =image.getWidth()-1; i >=0&&rightMost.getX()==-1;i--) {
                for (int j =0; j<image.getHeight()&&rightMost.getX()==-1;j++) {
                    //if rightMost.getX is set, exit out
                    if( Color.red( image.getPixel(i,j)) ==label) {
                        //cool u found the label
                        rightMost= new XYCoor(i,j);
                    }
                }
            }

            XYCoor topMost = new XYCoor();

            for (int i =0; i <image.getHeight()&&topMost.getX()==-1;i++) {
                for (int j =0; j<image.getWidth()&&topMost.getX()==-1;j++) {
                    //if topMost.getX is set, exit out
                    if( Color.red( image.getPixel(j,i)) ==label) {
                        //cool u found the label
                        topMost= new XYCoor(j,i);
                    }
                }
            }
            XYCoor bottomMost = new XYCoor();

            for (int i =image.getHeight()-1; i >=0&&bottomMost.getX()==-1;i--) {
                for (int j =0; j<image.getWidth()&&bottomMost.getX()==-1;j++) {
                    //if bottom.getX is set, exit out
                    if( Color.red( image.getPixel(j,i)) ==label) {
                        //cool u found the label
                        bottomMost= new XYCoor(j,i);
                    }
                }
            }
            //ok so now u have all the corners set.

            //first check. if top and bottom is not equal to right and left radius, then it's not a circle
            double leftRightRadius = Math.abs(rightMost.getX()-leftMost.getX());
            double topBottomRadius = Math.abs(bottomMost.getY()-topMost.getY());

            if (Math.abs(leftRightRadius-topBottomRadius)>=Vision.DIFFERENCE_IN_RADIUS_FOR_RECTANGLE_BOUNDS) {
                //then it isn't a cirlce
                //so we'll white out and move on
                image = removeLabel (image, label);
                Log.e("not a circle"," radius - delete");

            }
            else {
                //ok so if u made it thus far, u should still check the distance to the center
                //midpoint of right/left
                //midpoint of top/bottom
                double centerX = (leftMost.getX()+rightMost.getX())/2.0;
                double centerY = (topMost.getY()+bottomMost.getY())/2.0;
                XYCoor center = new XYCoor(centerX,centerY);
                double radius = (leftRightRadius+topBottomRadius)/2.0;
                Log.e("coordinates","right"+rightMost.toString()+"left"+leftMost.toString()+"top"+topMost.toString()+"bottom"+bottomMost.toString()+"center"+center.toString()+"radius"+radius);
                //we need to find all the edges
                ArrayList<XYCoor> edgesOfShape = getCoordinatesOfEdges(image, label);
                boolean litmustTest = true;
                //if litmusTest comes out false, meaning we broke out of statement becuase one was out of ordinairy, then it's not a circle
                for (int i = 0; i < edgesOfShape.size(); i++) {
                    //retrieve each coordinate and test
                    double distanceBetweenEdgeAndCenter = XYCoor.getDistance(center, edgesOfShape.get(i));
                    if (distanceBetweenEdgeAndCenter - radius > Vision.TOLERANCE_FOR_RADIUS_DIFFERENCE) {
                        //then ur not a circle
                        litmustTest = false;
                        break;
                    }
                    //implied else, ur good. check next pixel
                }
                if (litmustTest == false) {
                    //failed that litmus test, one was out of the circle
                    //not a circle
                    image = removeLabel(image, label);
                    Log.e("not a circle", "delete");
                }

                else {
                    //implied else, ur a circle, let's keep you
                    //also type in the center
                    centers.add(center);
                }

            }
        }
        //turn it into organized
        ArrayList <Object> imageRecountedByEdge = convertGrayscaleToEdged(image,EDGE_THRESHOLD);
        ArrayList <Object> data = new ArrayList<Object>();
        data.add(RETURNCIRCLES_DATA_BITMAP, (Bitmap)consolidateEdges((Bitmap)imageRecountedByEdge.get(CONVERTGRAYSCALETOEDGED_DATA_BITMAP),(Integer)imageRecountedByEdge.get(CONVERTGRAYSCALETOEDGED_DATA_NUMBER_OF_LABELS)).get(CONSOLDIATEEDGES_DATA_BITMAP));
        data.add(RETURNCIRCLES_DATA_XYCOORSCENTER, centers);
        return data;
    }

    private static ArrayList<XYCoor> getCoordinatesOfEdges(Bitmap image, int label) {
        ArrayList <XYCoor> data = new ArrayList<XYCoor>();
        for (int i =0; i<image.getWidth();i++) {
            for (int j = 0; j <image.getHeight();j++) {
                if (Color.red(image.getPixel(i,j))==label) {
                    data.add(new XYCoor(i, j));
                }
            }
        }
        return data;
    }

    private static Bitmap removeLabel(Bitmap image, int label) {
        image = image.copy(Bitmap.Config.ARGB_8888,true);
        for (int i =0; i<image.getWidth();i++) {
            for (int j = 0; j <image.getHeight();j++) {
                if (Color.red(image.getPixel(i,j))==label) {
                    //white it out
                    image.setPixel(i, j, Color.argb(255, 255, 255, 255));
                }
            }
        }
        return image;
    }

    public static Bitmap findAndIsolateBeaconButtons (Bitmap image, ArrayList<XYCoor> centersOfCircles) {
        if (centersOfCircles.size()<2) {
            return Bitmap.createBitmap(image.getWidth(),image.getHeight(), Bitmap.Config.ARGB_8888);
            //returns a new (blank) bitmap. there were less than one, so return an image with no circles, since no set of buttons were found
        }
        if (centersOfCircles.size()==2) {
            //check to make sure the two x values are equal
            if (Math.abs(centersOfCircles.get(0).getY()-centersOfCircles.get(1).getY())> THRESHOLD_FOR_CENTERS_OF_TWO_BUTTONS) {
                //check y's. the x's will have a huge diff. but the y placement should be about the same
                return image;
                //you already had the two
            }
            else {
                return Bitmap.createBitmap(image.getWidth(),image.getHeight(), Bitmap.Config.ARGB_8888);
                //else those two werent the buttons, so send empty
            }
        }
        else {
            //more than 2. check to see if any two y's are equal
            int[][] numberOfMatchesPerCircle = new int[centersOfCircles.size()][centersOfCircles.size()];
            ArrayList<XYCoor> pairsOfCircles = new ArrayList<XYCoor>();
            //we can use XYCoor to represnt the pair
            for (int i =0; i <centersOfCircles.size()-1;i++) {
                for (int j =i+1;j<centersOfCircles.size();j++) {
                    //compare i and j y values. note the number of matches
                    if (Math.abs(centersOfCircles.get(i).getY()-centersOfCircles.get(j).getY())>THRESHOLD_FOR_CENTERS_OF_TWO_BUTTONS) {
                        numberOfMatchesPerCircle[i][j]++;
                    }
                }
            }
            //k so now we've got that half triangle adjacency matrix
            int[] sumOfRows = new int[centersOfCircles.size()];
            for (int i =0; i <centersOfCircles.size();i++) {
                int sumOfI = 0;
                for (int j =0; j<centersOfCircles.size();j++) {
                    sumOfI+=numberOfMatchesPerCircle[i][j];
                }
                sumOfRows[i]=sumOfI;
            }
            //now check to see which ones have a sum of 1. then check the one
            for (int i =0; i<centersOfCircles.size();i++) {
                if (sumOfRows[i]==1) {
                    //check the sum of Column associated with that match
                    int matchOfI = findPair (numberOfMatchesPerCircle,i);
                    //now check for sum
                    int sumOfMatchPair = sumOfPairColumn(matchOfI, numberOfMatchesPerCircle);
                    if (sumOfMatchPair==1) {
                        //then we found the pair
                        Log.e("found pair!","circle one label: "+i+"circle two label:"+matchOfI);
                        pairsOfCircles.add(new XYCoor(i,matchOfI));
                    }
                    //implied else, didn't work out, don't save
                }
            }
            //remove everything from the picture but the paired circles
            for (int label =0; label <centersOfCircles.size();label++) {
                if (!ArrayListOfXYCoorsIncludes(pairsOfCircles,label)) {
                    removeLabel(image,label);
                }
            }
            if (pairsOfCircles.size()==1) {
                //awesomeee my job is easy
                return image;
            }
            else {
                //TODO find the best fit ie. what is the usual button size, radius size
                //now u have to rule out circles :/

                return image;
            }
        }
    }

    private static boolean ArrayListOfXYCoorsIncludes(ArrayList<XYCoor> pairsOfCircles, int label) {
        for (int i =0; i <pairsOfCircles.size();i++) {
            if (pairsOfCircles.get(i).getX()==label||pairsOfCircles.get(i).getY()==label) {
                return true;
            }
        }
        return false;
    }

    private static int sumOfPairColumn(int matchOfI, int[][] fullAdjacencyMatrix) {
        int sum = 0;
        for (int i =0; i <fullAdjacencyMatrix.length;i++) {
            sum+=fullAdjacencyMatrix[i][matchOfI];
        }
        return sum;
    }

    private static int findPair(int[][] fullAdjacencyMatrix, int rowThatHasOneMatch) {
        for (int i = 0; i< fullAdjacencyMatrix.length;i++) {
            //go to that row and search until it's not 0
            if (fullAdjacencyMatrix[rowThatHasOneMatch][i]==1) {
                return i;
            }
        }
        //this should never happen...
        return -1;
    }

    public static String findViaWhiteOutNotWorthyPixelsAndThenFindANonWhiteFromLeftAndSeeColor(Bitmap image, Context context) {
        // a drop of 150 in all rgb (or at least two) means that relatively, the pixel is black, relative to all the other pixels
        //scan horizontally downward
        //here's how we'll represnt a dot
        //represent it via an arraylist of pixels
        //keep on esarching vertically down until you don't hit a black dot anymore
        //then go from oringial found. go left and down until no more black
        //then go from origninal and go right and down
        //done
        //here's how we'll decide if the surrounding is black: using luminance formula
        //0.2126*R + 0.7152*G + 0.0722*B
        //where 0 is black and 255 is white
        //we'll do less than 90 on luminance scale is considered a black
        //we also need an array list with pixels we already defined as black, so as to not create duplicate black dots with different original dot points
        //ArrayList checkedBlackPixels = new ArrayList<XYCoor>();
        //TODO edit description because it's kinda different now
        //change image to mutable
        image = image.copy(Bitmap.Config.ARGB_8888, true);
        String returnThis = "";
        for (int i = 0; i < image.getHeight(); i++) {
            int numberOfPixelsThatAreNoteWorthy = 0;
            //noteworthy pixels are those within the hue range of blue and pink
            //so around 165 - 190 for blues, and from 0-15 for reds and 295 up for reds
            for (int j = 0; j < image.getWidth(); j++) {
                //where j is x and i is y, 0,0 is top left corner
                int pixel = image.getPixel(j, i);
                //pixel is in rgb value. let's break this into hsv values
                // hsv[0] is Hue [0 .. 360)
                // hsv[1] is Saturation [0...1]
                // hsv[2] is Value [0...1]
                float[] hsv = new float[3];
                Color.colorToHSV(pixel, hsv);
                //check hue to see if noteworthy
                if ((1 <= hsv[0] && hsv[0] <= 15) || hsv[0] > 295 || (170 < hsv[0] && hsv[0] < 190)) {
                    //then it is either red or blue, and is noteworthy
                    //note a complete white pixel has hue values of 0, hence the greater than or equal to 1
                    numberOfPixelsThatAreNoteWorthy++;
                }
            }
            //from our testing, about 25% of the row is taken up by the beacon
            //so noteworthiness must cross the 25% threshold
            double percentNoteworthy = (double) (numberOfPixelsThatAreNoteWorthy) / (double) (image.getWidth());
            Log.e("numPixNote", String.valueOf(numberOfPixelsThatAreNoteWorthy));
            Log.e("total", String.valueOf(image.getWidth()));
            Log.e("percentNoteWorthy", String.valueOf(percentNoteworthy));
            if (percentNoteworthy < .25) {
                //delete this row
                Log.e("delete!", "Deleting...");
                /*for (int x = 0; x<image.getWidth();x++) {
                    if (x>=image.getWidth()) {
                        x=image.getWidth()-1;
                    }
                    image.setPixel(x,i,Color.argb(255,255,255,255));
                    //set the pixel to white
                    //note a complete white pixel has hue value of 0
                }*/
            }
        }
        //ok so now you have a corrected image.
        //save the image for logging and debugging
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + " EditedWhiteRows";
        File pictureFile = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, timeStamp, context, false);
        if (pictureFile == null) {
            Log.d("ERROR", "Error creating media file, check storage permissions: "
            );
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            fos.write(byteArray);
            fos.close();


        } catch (FileNotFoundException e) {
            Log.d("ERROR", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("ERROR", "Error accessing file: " + e.getMessage());
        }
        //ok saved
        returnThis = "pathRows:" + "IMG_" + timeStamp + "EditedWhiteRows" + ".jpg";

        //so now all we have left is the bacon and a bunch of white lines, horizontally
        //do the same vertically now, to eliminate all of the excess stuff
        for (int i = 0; i < image.getWidth(); i++) {
            int numberOfWorthy = 0;
            for (int j = 0; j < image.getHeight(); j++) {
                int pixel = image.getPixel(i, j);
                float[] hsv = new float[3];
                Color.colorToHSV(pixel, hsv);
                if ((1 <= hsv[0] && hsv[0] <= 15) || hsv[0] > 295 || (170 < hsv[0] && hsv[0] < 190)) {
                    //then it is either red or blue, and is noteworthy
                    //note a complete white pixel has hue values of 0, hence the greater than or equal to 1
                    numberOfWorthy++;
                }
            }
            double percentNoteworthy = numberOfWorthy / image.getWidth();
            if (percentNoteworthy < .25) {
                //delete this row
                for (int x = 0; x < image.getHeight(); x++) {
                    image.setPixel(i, x, Color.argb(255, 255, 255, 255));
                    //set the pixel to white
                    //note a complete white pixel has hue value of 0
                }
            }
        }
        String timeStamp2 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + " EditedWhiteAll";
        File pictureFile2 = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, timeStamp, context, false);
        if (pictureFile == null) {
            Log.d("ERROR", "Error creating media file, check storage permissions: "
            );
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            fos.write(byteArray);
            fos.close();


        } catch (FileNotFoundException e) {
            Log.d("ERROR", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("ERROR", "Error accessing file: " + e.getMessage());
        }
        returnThis = "pathAll:" + "IMG_" + timeStamp + "EditedWhiteAll" + ".jpg";

        //now all we have to do is see what the first hue value from the left is
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                //go down and move to the right (start from left) until you hit a non white
                int pixel = image.getPixel(i, j);
                float[] hsv = new float[3];
                Color.colorToHSV(pixel, hsv);
                if (hsv[0] != 0) {
                    //because r=255,g=255,b=255, means hue = 0; (which is white)
                    if (isBlue(hsv[0], hsv[1], hsv[2])) {
                        returnThis += "firstColorFound:blue XYCoor:" + i + "," + j;
                        return returnThis;
                    } else if (isRed(hsv[0], hsv[2])) {
                        returnThis += "firstColorFound:red XYCoor:" + i + "," + j;
                        return returnThis;
                    } else {
                        //uhoh, something was wrong
                        returnThis += "error!";
                    }
                }
            }
        }

        return returnThis;
    }
    public static String savePictureOuput (Bitmap bitmap, Context context) {
        return savePicture(bitmap, context, "OUTPUT", true);
    }
    public static String savePicture(Bitmap bitmap, Context context, String tag, boolean debug) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + tag;
        File pictureFile = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, timeStamp, context, debug);
        if (pictureFile == null) {
            Log.d("ERROR", "Error creating media file, check storage permissions: "
            );
            return "ERROR";
        }
        try {

            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

            fos.close();


        } catch (FileNotFoundException e) {
            Log.d("ERROR", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("ERROR", "Error accessing file: " + e.getMessage());
        }
        return pictureFile.getName();
    }

    private static File getOutputMediaFile(int type, String timeStamp, Context context, boolean debug) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Matt Quan is a boss");
        if (debug) {
            mediaStorageDir =  new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Testing");

        }
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name

        File mediaFile;
        if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
            String path = mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".png";
            Log.e("savedPath", path);
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(
                    "com.quan.companion", Context.MODE_PRIVATE);
            prefs.edit().putString(Keys.pictureImagePathSharedPrefsKeys, path).apply();
            Log.e("saved path", "saved path in shared prefs");
            mediaFile = new File(path);

        } else {
            return null;
        }

        return mediaFile;
    }


}