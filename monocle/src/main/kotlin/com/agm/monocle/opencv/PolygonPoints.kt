package com.agm.monocle.opencv

import android.graphics.PointF

data class PolygonPoints(
    val topLeftPoint: PointF,
    val topRightPoint: PointF,
    val bottomLeftPoint: PointF,
    val bottomRightPoint: PointF
)
