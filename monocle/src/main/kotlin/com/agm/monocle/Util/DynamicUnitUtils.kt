package com.agm.monocle.Util

import android.content.res.Resources
import android.util.TypedValue


object DynamicUnitUtils {

    /**
     * Converts DP into pixels.
     *
     * @param dp The value in DP to be converted into pixels.
     *
     * @return The converted value in pixels.
     */
    fun convertDpToPixels(dp: Float): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp, Resources.getSystem().getDisplayMetrics()
            )
        )
    }

    /**
     * Converts pixels into DP.
     *
     * @param pixels The value in pixels to be converted into DP.
     *
     * @return The converted value in DP.
     */
    fun convertPixelsToDp(pixels: Int): Int {
        return Math.round(pixels / Resources.getSystem().getDisplayMetrics().density)
    }

    /**
     * Converts SP into pixels.
     *
     * @param sp The value in SP to be converted into pixels.
     *
     * @return The converted value in pixels.
     */
    fun convertSpToPixels(sp: Float): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp, Resources.getSystem().getDisplayMetrics()
            )
        )
    }

    /**
     * Converts pixels into SP.
     *
     * @param pixels The value in pixels to be converted into SP.
     *
     * @return The converted value in SP.
     */
    fun convertPixelsToSp(pixels: Int): Int {
        return Math.round(pixels / Resources.getSystem().getDisplayMetrics().density)
    }

    /**
     * Converts DP into SP.
     *
     * @param dp The value in DP to be converted into SP.
     *
     * @return The converted value in SP.
     */
    fun convertDpToSp(dp: Float): Int {
        return Math.round(
            convertDpToPixels(dp) / convertSpToPixels(
                dp
            ).toFloat())
    }
}