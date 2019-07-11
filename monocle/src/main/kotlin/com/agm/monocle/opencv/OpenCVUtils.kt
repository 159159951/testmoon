package com.agm.monocle.opencv



object OpenCVUtils {
    external fun convertNativeGray(mRgba: Long, mGrey: Long): Int
    external fun processImage(
        InputMRgba: Long,
        imageToDisplayScale: Float,
        screenWidth: Int,
        screenHeight: Int,
        nativeObjAddr: Long
    ): DoubleArray
}

