package com.agm.monocle.opencv


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.agm.monocle.R
import java.util.*

class CropView : FrameLayout {
    private var paint: Paint? = null
    private var pointer1: ImageView? = null
    private var pointer2: ImageView? = null
    private var pointer3: ImageView? = null
    private var pointer4: ImageView? = null
    private var polygonView: CropView? = null
    private var circleFillPaint: Paint? = null
    private var radius: Int = 0

    var points: Map<Int, PointF>
        get() {

            val points = ArrayList<PointF>()
            points.add(PointF(pointer1!!.x - radius, pointer1!!.y - radius))
            points.add(PointF(pointer2!!.x - radius, pointer2!!.y - radius))
            points.add(PointF(pointer3!!.x - radius, pointer3!!.y - radius))
            points.add(PointF(pointer4!!.x - radius, pointer4!!.y - radius))

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

    fun getDotRadius(): Int {
        return radius;
    }

    private fun init() {
        radius = dp2px(context, 18f)
        polygonView = this
        pointer1 = getImageView(0, 0)
        pointer2 = getImageView(width, 0)
        pointer3 = getImageView(0, height)
        pointer4 = getImageView(width, height)
        addView(pointer1)
        addView(pointer2)
        addView(pointer3)
        addView(pointer4)
        initPaint()

    }

    private fun initPaint() {
        paint = Paint()
        paint!!.color = ContextCompat.getColor(context, R.color.crop_color)
        paint!!.strokeWidth = 3f
        paint!!.isAntiAlias = true

        circleFillPaint = Paint()
        circleFillPaint!!.style = Paint.Style.FILL
        circleFillPaint!!.color = ContextCompat.getColor(context, R.color.crop_color)
        circleFillPaint!!.isAntiAlias = true
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

    private fun setPointsCoordinates(pointFMap: Map<Int, PointF>) {
        pointer1!!.x = pointFMap.getValue(0).x - radius
        pointer1!!.y = pointFMap.getValue(0).y - radius

        pointer2!!.x = pointFMap.getValue(1).x - radius
        pointer2!!.y = pointFMap.getValue(1).y - radius

        pointer3!!.x = pointFMap.getValue(2).x - radius
        pointer3!!.y = pointFMap.getValue(2).y - radius

        pointer4!!.x = pointFMap.getValue(3).x - radius
        pointer4!!.y = pointFMap.getValue(3).y - radius
    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawLine(
                pointer1!!.x + radius,
                pointer1!!.y + radius,
                pointer3!!.x + radius,
                pointer3!!.y + radius,
                paint!!
        )
        canvas.drawLine(
                pointer3!!.x + radius,
                pointer3!!.y + radius,
                pointer4!!.x + radius,
                pointer4!!.y + radius,
                paint!!
        )
        canvas.drawLine(
                pointer1!!.x + radius,
                pointer1!!.y + radius,
                pointer2!!.x + radius,
                pointer2!!.y + radius,
                paint!!
        )
        canvas.drawLine(
                pointer2!!.x + radius,
                pointer2!!.y + radius,
                pointer4!!.x + radius,
                pointer4!!.y + radius,
                paint!!
        )

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getImageView(x: Int, y: Int): ImageView {
        val imageView = ImageView(context)
        val layoutParams =
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.circle)
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        imageView.setOnTouchListener(TouchListenerImpl())
        return imageView
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    private inner class TouchListenerImpl : View.OnTouchListener {

        private val downPT = PointF() // Record Mouse Position When Pressed Down
        private var startPT = PointF() // Record Start Position of 'img'
        private var latestPoint = PointF()

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val eid = event.action
            when (eid) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (startPT.x + mv.x + v.width.toFloat() < polygonView!!.width && startPT.y + mv.y + v.width.toFloat() < polygonView!!.height && startPT.x + mv.x > 0 && startPT.y + mv.y > 0) {
                        v.x = (startPT.x + mv.x).toInt().toFloat()
                        v.y = (startPT.y + mv.y).toInt().toFloat()
                        startPT = PointF(v.x, v.y)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    CameraWithAutoCaptureActivity.allDraggedPointsStack.push(
                            PolygonPoints(
                                    PointF(pointer1!!.x, pointer1!!.y),
                                    PointF(pointer2!!.x, pointer2!!.y),
                                    PointF(pointer3!!.x, pointer3!!.y),
                                    PointF(pointer4!!.x, pointer4!!.y)
                            )
                    )
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                    latestPoint = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    val color = ContextCompat.getColor(context, R.color.crop_color)
                    latestPoint.x = v.x
                    latestPoint.y = v.y
                    paint!!.color = color
                }
                else -> {
                }
            }

            polygonView!!.invalidate()
            return true
        }
    }

    private fun dp2px(context: Context, dp: Float): Int {
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
        return Math.round(px)
    }
}