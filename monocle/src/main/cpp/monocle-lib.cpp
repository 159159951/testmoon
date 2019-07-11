#include <jni.h>
#include <string>
#include <stdio.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc_c.h>

using namespace cv;
using namespace std;
#define LOG_TAG    "native-lib-jni"


#define MONOCLE_DEV_OPENCV 0
#define DEV_SHOW_CONTOURS 1
#define DEV_SHOW_BOUNDRECTANGLE 0
#define DEV_SHOW_ROTATERECTANGLE 0
#define DEV_SHOW_APPROXDPLINE 0
#define DEV_SHOW_BRACKETING 1
#define DEV_SHOW_WATERMARK 0

#define DEV_OCR_OPENCV 1
#define DEV_OCR_RESULT 0
#define DEV_OCR_DETAIL_RESULT @"ID"
#define DEV_OCR_CAM_DEVICE_PARAM 0
#define OCR_DO_ID 1
#define OCR_DO_TH_NAME 0
#define OCR_DO_EN_NAME 0
#define OCR_DO_TH_DOB 0
#define OCR_DO_EN_DOB 0
#define OCR_DO_TH_ADDR 0
#define MAX_OCR_FIELD_READ 3
#define TOTAL_OCR_READ_COUNT (OCR_DO_ID + OCR_DO_TH_NAME + OCR_DO_EN_NAME + OCR_DO_TH_DOB + OCR_DO_EN_DOB + OCR_DO_TH_ADDR)*MAX_OCR_FIELD_READ

#define DARK_ISO_WARNING_THRESHOLD 380

#define DETECT_STATE_READY 0
#define DETECT_STATE_READING 1
#define DETECT_STATE_FOUND 2


int toGray(Mat mat, Mat &gray) {
    cvtColor(mat, gray, CV_RGB2GRAY);
    if (gray.rows == mat.rows && gray.cols == mat.cols)
        return 1;

    return 0;
}

bool compareContourAreas(std::vector<cv::Point> contour1, std::vector<cv::Point> contour2) {
    double i = fabs(contourArea(cv::Mat(contour1)));
    double j = fabs(contourArea(cv::Mat(contour2)));
    return (i > j);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_agm_monocle_opencv_OpenCVUtils_convertNativeGray(JNIEnv *env, jobject type, jlong addrRgba, jlong addrGray) {

    Mat &mRgb = *(Mat *) addrRgba;
    Mat &mGray = *(Mat *) addrGray;

    int conv;
    jint retVal;

    conv = toGray(mRgb, mGray);
    retVal = (jint) conv;

    return retVal;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_agm_monocle_opencv_OpenCVUtils_processImage(JNIEnv *env, jclass type, jlong InputMRgba,
                                                     float imageToDisplayScale, int screenWidth, int screenHeight,
                                                     jlong matPtr) {

    // TODO
    Mat &image_orig = *(cv::Mat *) InputMRgba;

    //Mat& image_orig = *(cv::Mat*) imageOAddr;

    int IMG_WIDTH = image_orig.size().width;
    int IMG_HEIGHT = image_orig.size().height;
//    int IMG_WIDTH = image_orig.size().width*0.5;
//    int IMG_HEIGHT = image_orig.size().height*0.5;
//
//    float TRUE_WIDTH = image_orig.size().width;
//    float TRUE_HEIGHT = image_orig.size().height;
//
////    cv::Mat image_orig = cv::Mat(TRUE_HEIGHT, TRUE_WIDTH, CV_8UC4, baseAddress, bytePerRow);
//
//    cv::Mat image_cropped;
//
////      std::cout <<image_orig.size() <<std::endl;
//
//    if (TRUE_WIDTH != IMG_WIDTH) {  //handle IOS ipadpro [597x417]
//        float x_offset = std::round((TRUE_WIDTH - IMG_WIDTH) / 2);
//        float y_offset = std::round((TRUE_HEIGHT - IMG_HEIGHT) / 2);
//        cv::Rect temp_roi(x_offset, y_offset, TRUE_WIDTH - x_offset, TRUE_HEIGHT - y_offset);
//        cv::Mat temp_img = image_orig(temp_roi);
////        UIImage *debugImageroi;
////        debugImageroi = MatToUIImage(temp_img);
//        temp_img.copyTo(image_cropped);
//        cout << "size >>>" << image_cropped.size;
//    } else {
//        cout << "size else >>>" << image_cropped.size;
//        image_orig.copyTo(image_cropped);
//    }
//
//


    Mat image_hsv;


    try {
        cvtColor(image_orig, image_hsv, cv::COLOR_BGR2HSV);
        cvtColor(image_orig, image_orig, cv::COLOR_BGR2RGB);
    } catch (...) {
        cout << "Exception occurred (it's about memory, segmentation fault)";
    }


    vector<Mat> channels_hsv;
    Mat image_h, image_s, image_v;

    cv::split(image_hsv, channels_hsv);
    image_h = channels_hsv[0];
    image_s = channels_hsv[1];
    image_v = channels_hsv[2];

// ---- determine color in the middle first ----
//    int brackWidth = 200, brackHeight = 200;
    //// ---- determine color in the middle first ----
    int brackWidth, brackHeight;
    if (IMG_WIDTH > IMG_HEIGHT) {
////landscape
        brackWidth = IMG_WIDTH * 0.26;
        brackHeight = IMG_HEIGHT * 0.33;
    } else {
////portrait
        brackWidth = IMG_WIDTH * 0.33;
        brackHeight = IMG_HEIGHT * 0.26;
    }
//
    cv::Mat docColor = image_hsv;
    docColor(cv::Rect(IMG_WIDTH / 2 - brackWidth / 2, IMG_HEIGHT / 2 - brackHeight / 2, brackWidth,
                      brackHeight)).copyTo(docColor);
    docColor.convertTo(docColor, CV_32FC3); //convert for 3d histrogram

    // ---- HISTROGRAM AT CENTER OF IMAGE
    cv::Mat colorHist;
    float hrange[] = {0, 256};
    float range[] = {0, 256};

    const float *histRange[] = {hrange, range};
    const int histSize[] = {32, 32};
    const int backProjectionchannels[] = {0, 1};
    cv::calcHist(&docColor, 1, backProjectionchannels, cv::Mat(), colorHist, 2, histSize,
                 histRange);

    cv::Mat backHistOutput;
    cv::calcBackProject(&image_hsv, 1, backProjectionchannels, colorHist, backHistOutput,
                        histRange);


    // ---- BACKPROJECTION : clean up with kernel
    cv::Mat bilateralOutput;
    int bilateralKernalSize = 9.0;
    double bilateralColorWeight = 15.0; //same as gausian
    double bilateralSpaceWeight = 150.0; //the range of intensities (or colors)
    cv::bilateralFilter(backHistOutput, bilateralOutput, bilateralKernalSize, bilateralColorWeight,
                        bilateralSpaceWeight);

    // ---- THRESHOLDING
    cv::Mat otsuInput = bilateralOutput;
    cv::Mat otsuThresholdOutput;
    cv::Vec4d backMean, backStd;
    double otsuThresholdKernel;
    otsuThresholdKernel = cv::threshold(otsuInput, otsuThresholdOutput, 0, 255,
                                        CV_THRESH_BINARY | CV_THRESH_OTSU);

    // ---- inRange: Removal black object noise
    cv::Mat intensityMask;
    cv::inRange(image_v, cv::Scalar(0, 0, 0), cv::Scalar(100, 100, 100),
                intensityMask); // OLD inrage param
    otsuThresholdOutput = otsuThresholdOutput - intensityMask;

    // ---- Fillpoly
    cv::rectangle(otsuThresholdOutput, cv::Point(image_orig.cols / 2 - brackWidth / 2,
                                                 image_orig.rows / 2 - brackHeight / 2),
                  cv::Point(image_orig.cols / 2 + brackWidth / 2,
                            image_orig.rows / 2 + brackHeight / 2), cv::Scalar(255, 255, 255), -1,
                  8);

    cv::rectangle(otsuThresholdOutput,
                  cv::Point(IMG_WIDTH / 2 - brackWidth / 2, IMG_HEIGHT / 2 - brackHeight / 2),
                  cv::Point(IMG_WIDTH / 2 + brackWidth / 2, IMG_HEIGHT / 2 + brackHeight / 2),
                  cv::Scalar(255, 255, 255), -1, 8);

    // ---- MORPH : CLOSE ELLIPSE
    cv::Mat morphInput = otsuThresholdOutput;
    cv::Mat morphOutput;
    cv::Mat kernel_close = cv::getStructuringElement(cv::MORPH_ELLIPSE,
                                                     cv::Size(6, 9)); // Ellipse is better
    cv::morphologyEx(morphInput, morphOutput, cv::MORPH_CLOSE, kernel_close);

    ///------- CANNY is crucial for finding contours
    cv::Mat cannyInput = morphOutput;

//    cv::Mat canny = otsuThresholdOutput; //morph is really impact

//    Mat* mat = (Mat*) matPtr;

//   cv::Mat mat = (Mat)matPtr;

//    mat = otsuThresholdOutput;


//    return canny;

    cv::Mat cannyOutput;
    int canny_kernelsize_aperture = 3;
    cv::Canny(cannyInput, cannyOutput, otsuThresholdKernel * 0.9, otsuThresholdKernel,
              canny_kernelsize_aperture);

    // ------ HOUGHLINE ------------------------------
    std::vector<cv::Vec2f> lines;
    //    cv::Mat houghOutput = image;
    cv::Mat houghInput = cannyOutput.clone();
    //    cv::Mat houghOutputImage = image;
    cv::Mat houghOutputImage = cv::Mat::zeros(image_orig.rows, image_orig.cols, CV_8SC1);
    cv::Mat houghOutput = cv::Mat::zeros(cannyOutput.size(), CV_8UC1);
    cv::Mat houghCannyDisplay = cannyOutput;

    cv::HoughLines(houghInput, lines, 1, CV_PI / 180, 80); //CRUCIAL OUTPUT


    // ------ HOUGHLINE: line segmentation to horizontal and vertical by k-mean
    std::vector<cv::Vec2f> horLines;
    std::vector<cv::Vec2f> verLines;
    for (size_t i = 0; i < lines.size(); i++) {
        if (80 < lines[i][1] * (180 / CV_PI) && 100 > lines[i][1] * (180 / CV_PI)) {
            verLines.push_back(lines[i]);
        } else if (170 < lines[i][1] * (180 / CV_PI) || 10 > lines[i][1] * (180 / CV_PI)) {
            horLines.push_back(lines[i]);
        }
    }

    // ------ HOUGHLINE: Display line
//            std::cout << lines.size() << "---------------------------------------- \n";

    for (size_t i = 0; i < lines.size(); i++) {
        float rho = lines[i][0], theta = lines[i][1];
        cv::Point pt1, pt2;
        double a = std::cos(theta), b = sin(theta);
        double x0 = a * rho, y0 = b * rho;
        pt1.x = cvRound(x0 + 2000 * (-b));
        pt1.y = cvRound(y0 + 2000 * (a));
        pt2.x = cvRound(x0 - 2000 * (-b));
        pt2.y = cvRound(y0 - 2000 * (a));
//        cv::line(houghOutput, pt1, pt2, cv::Scalar(255, 255, 255));
        cv::line(houghCannyDisplay, pt1, pt2, cv::Scalar(255, 255, 255));
//                cv::line(houghOutputImage,pt1,pt2,cv::Scalar(255,255,0));
    }

    Mat &matOsu = *(cv::Mat *) matPtr;

    houghCannyDisplay.copyTo(matOsu);
    // ------ HOUGHLINE: Find intersection
    std::vector<cv::Point> houghIntersection;
    for (size_t i = 0; i < horLines.size(); i++) {
        float rho = horLines[i][0], theta = horLines[i][1];
        cv::Point p1, p2;
        double a = std::cos(theta), b = sin(theta);
        double x0 = a * rho, y0 = b * rho;
        p1.x = cvRound(x0 + 2000 * (-b));
        p1.y = cvRound(y0 + 2000 * (a));
        p2.x = cvRound(x0 - 2000 * (-b));
        p2.y = cvRound(y0 - 2000 * (a));

        for (size_t j = 0; j < verLines.size(); j++) {
            float rho = verLines[j][0], theta = verLines[j][1];
            cv::Point p3, p4;
            double a = std::cos(theta), b = sin(theta);
            double x0 = a * rho, y0 = b * rho;
            p3.x = cvRound(x0 + 2000 * (-b));
            p3.y = cvRound(y0 + 2000 * (a));
            p4.x = cvRound(x0 - 2000 * (-b));
            p4.y = cvRound(y0 - 2000 * (a));
            double bottompart = float(
                    (p2.x - p1.x) * (p4.y - p3.y) - (p4.x - p3.x) * (p2.y - p1.y));
            cv::Point intersectPoint;

            intersectPoint.x = (float(p2.x * p1.y - p1.x * p2.y) * (p4.x - p3.x) -
                                (p2.x - p1.x) * float(p4.x * p3.y - p3.x * p4.y)) / bottompart;
            intersectPoint.y = float(((float(p2.x * p1.y - p1.x * p2.y) * float(p4.y - p3.y) -
                                       float(p4.x * p3.y - p3.x * p4.y) * float(p2.y - p1.y))) /
                                     bottompart);
            houghIntersection.push_back(intersectPoint);
        }
    }

    // ------- FINDING OUTERMOST POINT with convex hull -------
    cv::Mat convexOutput = cv::Mat::zeros(image_orig.size(), CV_8UC1);
    if (houghIntersection.size() > 0) {
        std::vector<cv::Point> hull;
        cv::convexHull(cv::Mat(houghIntersection), hull);
        //        std::cout << "cvexhul: " << hull << "\n";

        // draw the freakin convex
        for (size_t i = 0; i < hull.size(); i++) {
//                std::cout << "c: " << hull[i] << " at-i " << hull.at(i) << "\n";  //DEBUG
            cv::line(convexOutput, hull.at(i), hull.at(i + 1 < hull.size() ? i + 1 : 0),
                     cv::Scalar(255, 255, 0));
        }
    }


    std::vector<cv::Point2f> patchingOutput;
    // ------- CONTOUR : Start doing contour on Images
    std::vector<std::vector<cv::Point>> contours;
    std::vector<cv::Vec4i> hierarchy;

    cv::findContours(convexOutput, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE,
                     cv::Point(0, 0));

    cv::RotatedRect minRect;




    // ---- Fill in the barcode and stuff
    cv::Mat intensityMask2 = cv::Mat::zeros(image_orig.size(), CV_8UC1);
    // Must convert  original image color back
    try {
        cvtColor(image_orig, image_orig, cv::COLOR_RGB2BGR);
    } catch (...) {
        cout << "Exception occurred (it's about memory, segmentation fault)";
    }

    jdoubleArray outJNIArray = (env)->NewDoubleArray(18);  // <---- add this  to  allocate

    if (contours.size() > 0) {
        minRect = cv::minAreaRect(cv::Mat(contours[0]));
        float extent = (float) cv::contourArea(contours[0]) /
                       (float) (minRect.size.width * minRect.size.height);

        double minRectRatio = minRect.size.width / minRect.size.height;

        //solve the angle
        if (std::abs(minRect.angle) > 70) {
            minRectRatio = minRect.size.height / minRect.size.width;
        }


//      Check format Card
        if (minRectRatio > 1.27 && minRectRatio < 1.67) {
            std::vector<cv::Point2f> approx;
            float arc_length = cv::arcLength(cv::Mat(contours[0]), true);
            cv::approxPolyDP(cv::Mat(contours[0]), approx, arc_length * 0.01, true);

            cv::Point2f q1, q2, q3, q4;
            cv::Point2f q1It, q3It;
            for (int i = 0; i < 4; i++) { //size of corner Points
                if (approx[i].x < IMG_WIDTH / 2 && approx[i].y < IMG_HEIGHT / 2) {
//// std::cout << " Q1 " << approx[i];
                    q1 = approx[i];
                    q1It = q1;
                } else if (approx[i].x >= IMG_WIDTH / 2 && approx[i].y < IMG_HEIGHT / 2) {
//// std::cout << " Q2 " << approx[i];
                    q2 = approx[i];
                } else if (approx[i].x < IMG_WIDTH / 2 && approx[i].y >= IMG_HEIGHT / 2) {
                    q3 = approx[i];
                    q3It = q3;
                } else if (approx[i].x >= IMG_WIDTH / 2 && approx[i].y >= IMG_HEIGHT / 2) {
                    q4 = approx[i];
                }
            }

            q1It.x = q1It.x - 25;
            q3It.x = q3It.x - 25;

//// ----- finding barcode part
            cv::Mat tempSobelX;
////will need to crop the location of Sobel
            cv::Sobel(image_v, intensityMask2, 2, 0, 1);
            cv::Sobel(image_v, tempSobelX, 2, 1, 0);

            cv::subtract(intensityMask2, tempSobelX, intensityMask2);
            cv::blur(intensityMask2, intensityMask2, cv::Size(5, 5));
            cv::convertScaleAbs(intensityMask2, intensityMask2);
            cv::threshold(intensityMask2, intensityMask2, 100, 255, cv::THRESH_OTSU);

            cv::LineIterator li(intensityMask2, q1It, q3It, 8);
            cv::line(intensityMask2, q1It, q3It, cv::Scalar(255, 255, 255));


            int barcodeCount = 0;
            for (int i = 0; i < li.count; i++, ++li) {
                if (cv::Vec3b(*li)[0] != 0 || cv::Vec3b(*li)[1] != 0 || cv::Vec3b(*li)[2] != 0) {
                    barcodeCount++;
                }
            }

            float CARD_WIDTH = 860; // patching size is 70 px
            float CARD_HEIGHT = 540;

            std::vector<cv::Point2f> fullCardScreenPoints;
            std::vector<cv::Point2f> cardScreenPoints = {q1, q2, q4, q3};
            std::vector<cv::Point2f> patchingOutput;
            cv::Mat perspectiveTransformMatrix;
            bool longCardType; // handle long and short type due to the white area


            if (barcodeCount < 150) {
                longCardType = true;

                fullCardScreenPoints.push_back(cv::Point2f(0, 0));
                fullCardScreenPoints.push_back(cv::Point2f(CARD_WIDTH, 0));
                fullCardScreenPoints.push_back(cv::Point2f(CARD_WIDTH, CARD_HEIGHT));
                fullCardScreenPoints.push_back(cv::Point2f(0, CARD_HEIGHT));

                perspectiveTransformMatrix = cv::getPerspectiveTransform(fullCardScreenPoints, cardScreenPoints);


                std::vector<cv::Point2f> fullCardPatchingPoints;
                fullCardPatchingPoints.push_back(cv::Point2f(0, 0));
                fullCardPatchingPoints.push_back(cv::Point2f(70, 0));
                fullCardPatchingPoints.push_back(cv::Point2f(70, CARD_HEIGHT));
                fullCardPatchingPoints.push_back(cv::Point2f(0, CARD_HEIGHT));
                cv::perspectiveTransform(fullCardPatchingPoints, patchingOutput, perspectiveTransformMatrix);
////                  perspectiveTransformMatrix = cv::getPerspectiveTransform( shortCardScreenPoints, fullCardScreenPoints); 22 APR 2019 why am I switch the matrix
////--------------------------------------------------------------------------------
            }// 150 barcode long type
            else { //
                longCardType = false;

                fullCardScreenPoints.push_back(cv::Point2f(70, 0));
                fullCardScreenPoints.push_back(cv::Point2f(CARD_WIDTH, 0));
                fullCardScreenPoints.push_back(cv::Point2f(CARD_WIDTH, CARD_HEIGHT));
                fullCardScreenPoints.push_back(cv::Point2f(70, CARD_HEIGHT));

                perspectiveTransformMatrix = cv::getPerspectiveTransform(fullCardScreenPoints, cardScreenPoints);

//// short card need patching... will use only certain region?
                std::vector<cv::Point2f> fullCardPatchingPoints;

                fullCardPatchingPoints.push_back(cv::Point2f(0, 0));
                fullCardPatchingPoints.push_back(cv::Point2f(70, 0));
                fullCardPatchingPoints.push_back(cv::Point2f(70, CARD_HEIGHT));
                fullCardPatchingPoints.push_back(cv::Point2f(0, CARD_HEIGHT));

                cv::perspectiveTransform(fullCardPatchingPoints, patchingOutput, perspectiveTransformMatrix);
            } // END LONG TYPE SHORT TYPE THING BARCODE

            cv::Mat warpScreen_v;
            cv::Mat warpScreen_s;
            warpScreen_v.create(CARD_WIDTH, CARD_HEIGHT, CV_32F);
            warpScreen_s.create(CARD_WIDTH, CARD_HEIGHT, CV_32F);
            cv::Mat warpCard;

            cv::Point2f cardOnScreenPoints[4] = {patchingOutput[0], q2, q4, patchingOutput[3]};
            cv::Point2f glareOnScreenPoints[4] = {patchingOutput[0], q2, q4, patchingOutput[3]};

////---------------- find glare --------------------
            cv::Mat perspectiveTransformGlareMatrix = cv::getPerspectiveTransform(cardScreenPoints,
                                                                                  fullCardScreenPoints);
////              cv::Mat perspectiveTransformGlareMatrix = cv::getPerspectiveTransform(cardOnScreenPoints, fullCardScreenPoints);
            cv::warpPerspective(image_v, warpCard,
                                perspectiveTransformMatrix,
                                cv::Size(
                                        CARD_WIDTH,
                                        CARD_HEIGHT));


            cv::warpPerspective(image_v, warpScreen_v, perspectiveTransformGlareMatrix,
                                cv::Size(CARD_WIDTH, CARD_HEIGHT));
            cv::warpPerspective(image_s, warpScreen_s, perspectiveTransformGlareMatrix,
                                cv::Size(CARD_WIDTH, CARD_HEIGHT));
            cv::warpPerspective(image_orig, warpCard, perspectiveTransformGlareMatrix,
                                cv::Size(CARD_WIDTH, CARD_HEIGHT));
            int GLARE_SIZE_THRESHOLD = 1200; //base on assumption 40x30 px
            bool glare_detect_flag = false;

            cv::Mat card_glare;
            card_glare.convertTo(card_glare, CV_32FC1);

////to change the bit-depth you will have to allocate new IplImage of type 32F
            warpScreen_s.convertTo(warpScreen_s, CV_32FC1, 1.f / 255);
            warpScreen_v.convertTo(warpScreen_v, CV_32FC1, 1.f / 255);

            cv::Mat whitematrix(CARD_HEIGHT, CARD_WIDTH, CV_32FC1, cv::Scalar(1)); //NK pause here debuging glare image
            cv::subtract(whitematrix, warpScreen_s, warpScreen_s);
            card_glare = warpScreen_v.mul(warpScreen_s); //find glare?
            card_glare.convertTo(card_glare, CV_8UC1, 255);


            cv::threshold(card_glare, card_glare, 240, 255, cv::THRESH_TOZERO);

            std::vector<std::vector<cv::Point>> glare_contours;
            cv::findContours(card_glare, glare_contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE, cv::Point(0, 0));
            std::sort(glare_contours.begin(), glare_contours.end(), compareContourAreas);

            std::vector<std::vector<cv::Point>> glare_location;

            for (size_t i = 0; i < glare_contours.size(); i++) {
                if (GLARE_SIZE_THRESHOLD < cv::boundingRect(glare_contours[i]).area()) {
////                      std::cout << cv::boundingRect(glare_contours[i]).area() << cv::boundingRect(glare_contours[i]).size() << cv::boundingRect(glare_contours[i]).tl() << std::endl;
                    glare_location.push_back(glare_contours[i]);
                    glare_detect_flag = true;
////                      break; // incase we need only one glare turn this on
                }
            }
////
            double glareDetectFlag = 0;
            double isLongCardType = 0;

            if (longCardType) {
                isLongCardType = 1;
            } else {
                isLongCardType = 0;
            }
            if (!glare_detect_flag) {
                glareDetectFlag = 0;
                jdouble outCArray[] = {cardOnScreenPoints[0].x * imageToDisplayScale,
                                       cardOnScreenPoints[0].y * imageToDisplayScale,
                                       cardOnScreenPoints[1].x * imageToDisplayScale,
                                       cardOnScreenPoints[1].y * imageToDisplayScale,
                                       cardOnScreenPoints[2].x * imageToDisplayScale,
                                       cardOnScreenPoints[2].y * imageToDisplayScale,
                                       cardOnScreenPoints[3].x * imageToDisplayScale,
                                       cardOnScreenPoints[3].y * imageToDisplayScale,
                                       0, 0, 0, 0, 0, 0, 0, 0,
                                       glareDetectFlag,
                                       isLongCardType};

                (env)->SetDoubleArrayRegion(outJNIArray, 0, 18, outCArray);  // copy

                return outJNIArray;
            } else {
                glareDetectFlag = 1;
                for (size_t i = 0; i < glare_location.size(); i++) {
                    glareOnScreenPoints[0] = cv::boundingRect(glare_location[i]).tl();
                    glareOnScreenPoints[1].x =
                            cv::boundingRect(glare_location[i]).tl().x + cv::boundingRect(glare_location[i]).width;
                    glareOnScreenPoints[1].y = cv::boundingRect(glare_location[i]).tl().y;
                    glareOnScreenPoints[2] = cv::boundingRect(glare_location[i]).br();
                    glareOnScreenPoints[3].x = cv::boundingRect(glare_location[i]).tl().x;
                    glareOnScreenPoints[3].y =
                            cv::boundingRect(glare_location[i]).tl().y + cv::boundingRect(glare_location[i]).height;

                    std::vector<cv::Point2f> pointsback;
                    pointsback.push_back(glareOnScreenPoints[0]);
                    pointsback.push_back(glareOnScreenPoints[1]);
                    pointsback.push_back(glareOnScreenPoints[2]);
                    pointsback.push_back(glareOnScreenPoints[3]);

////                      std::cout <<perspectiveTransformMatrix <<std::endl;
                    cv::Mat temp_temp = cv::Mat(pointsback);
////                  std::cout<< temp_temp <<std::endl;
                    cv::perspectiveTransform(temp_temp, temp_temp, perspectiveTransformMatrix);

////                  std::cout<< temp_temp <<std::endl;
                    glareOnScreenPoints[0].x = temp_temp.at<float>(0, 0);
                    glareOnScreenPoints[0].y = temp_temp.at<float>(0, 1);
                    glareOnScreenPoints[1].x = temp_temp.at<float>(1, 0);
                    glareOnScreenPoints[1].y = temp_temp.at<float>(1, 1);
                    glareOnScreenPoints[2].x = temp_temp.at<float>(2, 0);
                    glareOnScreenPoints[2].y = temp_temp.at<float>(2, 1);
                    glareOnScreenPoints[3].x = temp_temp.at<float>(3, 0);
                    glareOnScreenPoints[3].y = temp_temp.at<float>(3, 1);

//                    TODO glare in Image
                    jdouble outCArray[] = {cardOnScreenPoints[0].x * imageToDisplayScale,
                                           cardOnScreenPoints[0].y * imageToDisplayScale,
                                           cardOnScreenPoints[1].x * imageToDisplayScale,
                                           cardOnScreenPoints[1].y * imageToDisplayScale,
                                           cardOnScreenPoints[2].x * imageToDisplayScale,
                                           cardOnScreenPoints[2].y * imageToDisplayScale,
                                           cardOnScreenPoints[3].x * imageToDisplayScale,
                                           cardOnScreenPoints[3].y * imageToDisplayScale,
//                    glare
                                           glareOnScreenPoints[0].x * imageToDisplayScale,
                                           glareOnScreenPoints[0].y * imageToDisplayScale,
                                           glareOnScreenPoints[1].x * imageToDisplayScale,
                                           glareOnScreenPoints[1].y * imageToDisplayScale,
                                           glareOnScreenPoints[2].x * imageToDisplayScale,
                                           glareOnScreenPoints[2].y * imageToDisplayScale,
                                           glareOnScreenPoints[3].x * imageToDisplayScale,
                                           glareOnScreenPoints[3].y * imageToDisplayScale,
                                           glareDetectFlag,
                                           isLongCardType};

                    (env)->SetDoubleArrayRegion(outJNIArray, 0, 18, outCArray);  // copy

                    return outJNIArray;
                }
            }
        } else {
            jdouble outCArray[] = {screenWidth * 0.2,
                                   screenHeight * 0.2,
                                   screenWidth * 0.8,
                                   screenHeight * 0.2,
                                   screenWidth * 0.8,
                                   screenHeight * 0.8,
                                   screenWidth * 0.2,
                                   screenHeight * 0.8,
//                    glare
                                   0, 0, 0, 0, 0, 0, 0, 0,
                                   0,
                                   0};

            (env)->SetDoubleArrayRegion(outJNIArray, 0, 18, outCArray);  // copy
            return outJNIArray;
        }//Close heuristic rule
    }
    return outJNIArray;
}



