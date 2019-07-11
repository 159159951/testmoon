package com.agm.monocle.opencv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.agm.monocle.R
import java.util.*

class PolygonView : FrameLayout {

    private var paint: Paint? = null
    private var glarePaint: Paint? = null
    private var pointer1: ImageView? = null
    private var pointer2: ImageView? = null
    private var pointer3: ImageView? = null
    private var pointer4: ImageView? = null

    private var glarePointer1: ImageView? = null
    private var glarePointer2: ImageView? = null
    private var glarePointer3: ImageView? = null
    private var glarePointer4: ImageView? = null

    var points: Map<Int, PointF>
        get() {

            val points = ArrayList<PointF>()
            points.add(PointF(pointer1!!.x, pointer1!!.y))
            points.add(PointF(pointer2!!.x, pointer2!!.y))
            points.add(PointF(pointer3!!.x, pointer3!!.y))
            points.add(PointF(pointer4!!.x, pointer4!!.y))

            return getOrderedPoints(points)
        }
        set(pointFMap) {
            if (pointFMap.size == 4) {
                setPointsCoordinates(pointFMap)
            }
        }


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        pointer1 = getImageView(0, 0)
        pointer2 = getImageView(width, 0)
        pointer3 = getImageView(0, height)
        pointer4 = getImageView(width, height)


        glarePointer1 = getImageView(0, 0)
        glarePointer2 = getImageView(width, 0)
        glarePointer3 = getImageView(0, height)
        glarePointer4 = getImageView(width, height)

        addView(pointer1)
        addView(pointer2)
        //
        addView(pointer3)
        addView(pointer4)

        addView(glarePointer1)
        addView(glarePointer2)
        //
        addView(glarePointer3)
        addView(glarePointer4)


        initPaint()
    }

    private fun initPaint() {
        paint = Paint()
        paint!!.color = ContextCompat.getColor(context, R.color.crop_color)
        paint!!.strokeWidth = 3f
        paint!!.isAntiAlias = true

        glarePaint = Paint()
        glarePaint!!.color = ContextCompat.getColor(context, R.color.orange)
        glarePaint!!.strokeWidth = 3f
        glarePaint!!.isAntiAlias = true
    }

    fun setGlarePoint(pointFMap: Map<Int, PointF>) {
        if (pointFMap.size == 4) {
            setGlarePointsCoordinates(pointFMap)
        }
    }

    private fun setPointsCoordinates(pointFMap: Map<Int, PointF>) {
        pointer1!!.x = Objects.requireNonNull<PointF>(pointFMap[0]).x
        pointer1!!.y = Objects.requireNonNull<PointF>(pointFMap[0]).y

        pointer2!!.x = Objects.requireNonNull<PointF>(pointFMap[1]).x
        pointer2!!.y = Objects.requireNonNull<PointF>(pointFMap[1]).y

        pointer3!!.x = Objects.requireNonNull<PointF>(pointFMap[2]).x
        pointer3!!.y = Objects.requireNonNull<PointF>(pointFMap[2]).y

        pointer4!!.x = Objects.requireNonNull<PointF>(pointFMap[3]).x
        pointer4!!.y = Objects.requireNonNull<PointF>(pointFMap[3]).y

    }


    private fun setGlarePointsCoordinates(pointFMap: Map<Int, PointF>) {
        glarePointer1!!.x = Objects.requireNonNull<PointF>(pointFMap[0]).x
        glarePointer1!!.y = Objects.requireNonNull<PointF>(pointFMap[0]).y

        glarePointer2!!.x = Objects.requireNonNull<PointF>(pointFMap[1]).x
        glarePointer2!!.y = Objects.requireNonNull<PointF>(pointFMap[1]).y

        glarePointer3!!.x = Objects.requireNonNull<PointF>(pointFMap[2]).x
        glarePointer3!!.y = Objects.requireNonNull<PointF>(pointFMap[2]).y

        glarePointer4!!.x = Objects.requireNonNull<PointF>(pointFMap[3]).x
        glarePointer4!!.y = Objects.requireNonNull<PointF>(pointFMap[3]).y

    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val bgPaint = Paint()
        bgPaint.color = ContextCompat.getColor(context, R.color.colorBlackThirtyFivePercentAlpha)
        bgPaint.isAntiAlias = true

        val path1 = drawPolygon()
        canvas.drawPath(path1, bgPaint)


        canvas.drawLine(pointer1!!.x, pointer1!!.y, pointer3!!.x, pointer3!!.y, paint!!)
        canvas.drawLine(pointer1!!.x, pointer1!!.y, pointer2!!.x, pointer2!!.y, paint!!)
        canvas.drawLine(pointer2!!.x, pointer2!!.y, pointer4!!.x, pointer4!!.y, paint!!)
        canvas.drawLine(pointer3!!.x, pointer3!!.y, pointer4!!.x, pointer4!!.y, paint!!)

        canvas.drawLine(glarePointer1!!.x, glarePointer1!!.y, glarePointer3!!.x, glarePointer3!!.y, glarePaint!!)
        canvas.drawLine(glarePointer1!!.x, glarePointer1!!.y, glarePointer2!!.x, glarePointer2!!.y, glarePaint!!)
        canvas.drawLine(glarePointer2!!.x, glarePointer2!!.y, glarePointer4!!.x, glarePointer4!!.y, glarePaint!!)
        canvas.drawLine(glarePointer3!!.x, glarePointer3!!.y, glarePointer4!!.x, glarePointer4!!.y, glarePaint!!)

    }


    private fun drawPolygon(): Path {
        val path = Path()
        path.moveTo(pointer1!!.x, pointer1!!.y)
        path.lineTo(pointer1!!.x, pointer1!!.y)
        path.lineTo(pointer2!!.x, pointer2!!.y)
        path.lineTo(pointer4!!.x, pointer4!!.y)
        path.lineTo(pointer3!!.x, pointer3!!.y)
        path.lineTo(pointer1!!.x, pointer1!!.y)
        path.close()
        return path
    }

    private fun getImageView(x: Int, y: Int): ImageView {
        val imageView = ImageView(context)
        val layoutParams =
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        //        imageView.setImageResource(R.drawable.circle);
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        return imageView
    }

    private fun getOrderedPoints(points: List<PointF>): Map<Int, PointF> {

        val centerPoint = PointF()
        val size = points.size
        for (pointF in points) {
            centerPoint.x += pointF.x / size
            centerPoint.y += pointF.y / size
        }
        val orderedPoints = hashMapOf<Int, PointF>()
        for (pointF in points) {
            var index = -1
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3
            }
            orderedPoints[index] = pointF
        }
        return orderedPoints
    }


}