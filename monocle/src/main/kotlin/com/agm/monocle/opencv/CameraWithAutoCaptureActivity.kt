package com.agm.monocle.opencv

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.agm.monocle.R
import com.agm.monocle.Util.DynamicUnitUtils
import com.agm.monocle.model.AGMMonocleConfiguration
import com.google.gson.Gson
import com.otaliastudios.cameraview.*
import kotlinx.android.synthetic.main.activity_auto_capture.*
import org.nield.kotlinstatistics.standardDeviation
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core.BORDER_DEFAULT
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class CameraWithAutoCaptureActivity : AppCompatActivity() {
    companion object {
        val allDraggedPointsStack: Stack<PolygonPoints> = Stack()

        const val TAG = "CameraWithAutoCaptureAc"
        const val DEF_STAMPWATERMARK_BGCOLOR = "#FFFFFF"
        const val DEF_STAMPWATERMARK_BORDERCOLOR = "#FFFFFF"
        const val DEF_STAMPWATERMARK_FONTSIZE = 12f
        const val DEF_STAMPWATERMARK_TEXTVALUE = "#stampWaterMark"
        const val DEF_STAMPWATERMARK_TEXTCOLOR = "#000000"

        const val DEF_COPYRIGHTWATERMARK_BGCOLOR = "#FFFFFF"
        const val DEF_COPYRIGHTWATERMARK_FONTSIZE = 14f
        const val DEF_COPYRIGHTWATERMARK_ROTATEANGLE = 45f
        const val DEF_COPYRIGHTWATERMARK_TEXTVALUE = "#copyrightWaterMark"
        const val DEF_COPYRIGHTWATERMARK_TEXTCOLOR = "#000000"
    }

    init {
        System.loadLibrary("monocle-lib")
    }

    private lateinit var userConfigObject: AGMMonocleConfiguration
    private val mInterval = 1.toLong()// 5 seconds by default, can be changed later
    private lateinit var mHandler: Handler
    private var screenHeight = 0
    private var screenWidth = 0
    private var cardOnScreenPoints: DoubleArray = DoubleArray(18)
    private var frameRecorder: MutableList<CardObjectModel> = mutableListOf()
    private var ratio: Float = 0.0F
    private lateinit var bitmapResult: Bitmap
    private val pointFs = hashMapOf<Int, PointF>()
    private val glarePointFs = hashMapOf<Int, PointF>()


    private val processing = FrameProcessor { frame ->
        val data = frame.data
        val size = frame.size
        val format = frame.format

        val yuv = YuvImage(data, format, size.width, size.height, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, size.width, size.height), 100, out)
        val bmp = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        val orig = Mat(Size(size.width.toDouble(), size.height.toDouble()), CvType.CV_8UC3)
        Utils.bitmapToMat(bmp, orig)
        val m = Mat()
        ratio = screenWidth.toFloat() / size.width.toFloat()
        cardOnScreenPoints =
                OpenCVUtils.processImage(
                        orig.nativeObjAddr,
                        ratio,
                        screenWidth,
                        screenHeight,
                        m.nativeObjAddr)
        val bitmap = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(m, bitmap)

        val glareDetect = cardOnScreenPoints[16].toInt()
        val isLongType = cardOnScreenPoints[17].toInt()
        if (glareDetect != 1) {
            if (isLongType == 1) {
                frameRecorder.add(
                        CardObjectModel(
                                cardOnScreenPoints[0],
                                TypeOfCard.LONG)
                )
                if (frameRecorder.size > 3) {
                    determineImageOdometry()
                    if (frameRecorder.size > 7) {
                        //capture
                        takePhoto()
                    }
                }
            } else {
                frameRecorder.add(
                        CardObjectModel(
                                cardOnScreenPoints[0],
                                TypeOfCard.SHORT
                        )
                )
                if (frameRecorder.size > 3) {
                    determineImageOdometry()
                    if (frameRecorder.size > 7) {
                        //capture
                        takePhoto()
                    }
                }
            }
        }
    }

    private val mStatusChecker by lazy {
        object : Runnable {
            override fun run() {
                mHandler.postDelayed(this, mInterval)
                setDetectView()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_capture)
        //get user config
        val userConfigString = intent.extras!!.getString("BUNDLE_USER_CONFIG")
//        "{\n" +
//                "  \"isCropEnabled\": true,\n" +
//                "  \"useDetection\": true,\n" +
//                "  \"waterMarkLogo\": " + R.drawable.water_mark_icon + ",\n" +
//                "  \"stampWatermark\": {\n" +
//                "    \"fontSize\": 12,\n" +
//                "    \"textColor\": \"#5530eb\",\n" +
//                "    \"textValue\": \"#stampWatermark\",\n" +
//                "    \"bgColor\": \"#c4546f\",\n" +
//                "    \"borderColor\": \"#050505\"\n" +
//                "  },\n" +
//                "  \"copyrightWatermark\": {\n" +
//                "    \"fontSize\": 30,\n" +
//                "    \"textColor\": \"#eaff4f\",\n" +
//                "    \"textValue\": \"#copyrightWatermark\",\n" +
//                "    \"bgColor\": \"#26ff0f\",\n" +
//                "    \"rotateAngle\": 30\n" +
//                "  }\n" +
//                "}"

        userConfigObject = Gson().fromJson(userConfigString, AGMMonocleConfiguration::class.java)

        preprocessingUserConfig()

        if (userConfigObject.waterMarkLogo > 0) {
            imvWaterMark.visibility = View.VISIBLE
            imvWaterMark.background = getDrawable(userConfigObject.waterMarkLogo)

        } else {
            imvWaterMark.visibility = View.GONE
        }

        userConfigObject.stampWatermark?.let {
            textViewStamp.textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    it.fontSize,
                    resources.displayMetrics
            )

            textViewStamp.text = it.textValue

            textViewStamp.setTextColor(Color.parseColor(it.textColor))

            var drawable = textViewStamp.background as GradientDrawable
            drawable.setColor(Color.parseColor(it.bgColor))
            drawable.setStroke(5, Color.parseColor(it.borderColor))
        }

        userConfigObject.copyrightWatermark?.let {
            textViewCopyright.textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    it.fontSize,
                    resources.displayMetrics
            )

            textViewCopyright.text =
                    it.textValue

            textViewCopyright.setTextColor(Color.parseColor(it.textColor))

            var drawable = textViewCopyright.background as GradientDrawable
            drawable.setColor(Color.parseColor(it.bgColor))

            textViewCopyright.rotation = -it.rotateAngle
        }

        val display = window!!.windowManager.defaultDisplay
        val sizes = Point()

        display.getSize(sizes)

        screenWidth = sizes.x
        screenHeight = sizes.y
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setDetectView()

        camera.setPictureSize(SizeSelectors.biggest())
        camera.setPreviewStreamSize(SizeSelectors.biggest())

        with(camera) {
            audio = Audio.OFF
            playSounds = true
            addCameraListener(object : CameraListener() {
                override fun onPictureTaken(result: PictureResult) {
                    super.onPictureTaken(result)
                    setCropView(result)
                }

                override fun onCameraOpened(options: CameraOptions) {
                    super.onCameraOpened(options)
                    frameRecorder.clear()

                    if (userConfigObject.useDetection) {
                        addFrameProcessor(processing)
                    }

                    startRepeatingTask()
                }
            })
            setLifecycleOwner(this@CameraWithAutoCaptureActivity)
        }

        mHandler = Handler()

        txtViewShuttle.setOnClickListener {
            camera.takePicture()
        }

        txtViewCancel.setOnClickListener {
            finish()
        }

        buttonCancel.setOnClickListener {
            bottomBar.visibility = View.GONE
            textViewCopyright.visibility = View.GONE
            textViewStamp.visibility = View.GONE
            cropView.visibility = View.INVISIBLE
            setDetectView()
            imv.visibility = View.GONE
            imv.setImageBitmap(null)
//            bitmap.recycle()
            camera.visibility = View.VISIBLE
            camera.open()

            rightBar.visibility = View.VISIBLE

            startRepeatingTask()
        }

        buttonCropToggle.setOnClickListener {
            userConfigObject.isCropEnabled = !userConfigObject.isCropEnabled
            if (userConfigObject.isCropEnabled
                    && bottomBar.visibility == View.VISIBLE) {
                cropView.visibility = View.VISIBLE
            } else {
                cropView.visibility = View.GONE
            }
        }

        buttonUse.setOnClickListener {
            bottomBar.visibility = View.INVISIBLE
            cropView.visibility = View.INVISIBLE

            var textViewStampBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            if (userConfigObject.stampWatermark != null) {
                textViewStampBitmap = createBitmapFromView(textViewStamp, 0f, 0f)
                textViewStamp.visibility = View.INVISIBLE
            }


            var bitmapResult = createBitmapFromView(rootView, 0f, 0f)
            if (userConfigObject.isCropEnabled
                    && cropView.points.size == 4) {
                //Step 1 crop and scale selected bitmap
                bitmapResult = imageCrop(bitmapResult)
            }
            if (userConfigObject.stampWatermark != null) {
                //Step 2 add watermark and stamp text
                bitmapResult = overlayBitmap(bitmapResult, textViewStampBitmap)
            }

            val result = "result"
            MemoryCache.addBitmapToCache(result, bitmapResult)


            val intent = Intent().apply {
                putExtra("key", result)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun preprocessingUserConfig() {
        userConfigObject.stampWatermark?.let {
            if (it.bgColor == null) {
                it.bgColor = DEF_STAMPWATERMARK_BGCOLOR
            }
            if (it.borderColor == null) {
                it.borderColor = DEF_STAMPWATERMARK_BORDERCOLOR
            }
            if (it.fontSize == null) {
                it.fontSize = DEF_STAMPWATERMARK_FONTSIZE
            }
            if (it.textValue == null) {
                it.textValue = DEF_STAMPWATERMARK_TEXTVALUE
            }
            if (it.textColor == null) {
                it.textColor = DEF_STAMPWATERMARK_TEXTCOLOR
            }
        }

        userConfigObject.copyrightWatermark?.let {
            if (it.bgColor == null) {
                it.bgColor = DEF_COPYRIGHTWATERMARK_BGCOLOR
            }
            if (it.fontSize == null) {
                it.fontSize = DEF_COPYRIGHTWATERMARK_FONTSIZE
            }
            if (it.rotateAngle == null) {
                it.rotateAngle = DEF_COPYRIGHTWATERMARK_ROTATEANGLE
            }
            if (it.textValue == null) {
                it.textValue = DEF_COPYRIGHTWATERMARK_TEXTVALUE
            }
            if (it.textColor == null) {
                it.textColor = DEF_COPYRIGHTWATERMARK_TEXTCOLOR
            }
        }
    }

    override fun onPause() {
        camera.close()
        super.onPause()
    }

    override fun onDestroy() {
        camera.destroy()
        super.onDestroy()
    }

    fun overlayBitmap(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        val bitmap1Width = bitmap1.getWidth()
        val bitmap1Height = bitmap1.getHeight()
        val bitmap2Width = bitmap2.getWidth()
//        val bitmap2Height = bitmap2.getHeight()

        val overlayBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, bitmap1.config)
        val canvas = Canvas(overlayBitmap)
        canvas.drawBitmap(bitmap1, Matrix(), null)
        canvas.drawBitmap(bitmap2,
                bitmap1Width - bitmap2Width.toFloat(),
                0f,
                null)
        return overlayBitmap
    }

    private fun createBitmapFromView(view: View, width: Float, height: Float): Bitmap {
        if (width > 0 && height > 0) {
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            DynamicUnitUtils
                                    .convertDpToPixels(width), View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                            DynamicUnitUtils
                                    .convertDpToPixels(height), View.MeasureSpec.EXACTLY
                    )
            )
        }
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val background = view.background

        background?.draw(canvas)
        view.draw(canvas)

        return bitmap
    }

    private fun imageCrop(bitmap: Bitmap): Bitmap {
        var sourceBitmap = bitmap
        var descBitmap = bitmap.copy(sourceBitmap.config, true)
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        val inputMat = Mat()
        val outputMat = Mat()
        Utils.bitmapToMat(sourceBitmap, inputMat)

        //Dont know why `cropView.points` does not store exactly center point of 4 vertex?
        //Seem like it shift a little bit.
        //So the delta value is exactly inserted pieces

        val delta = cropView.getDotRadius() * 2

        val sourcePoints = ArrayList<org.opencv.core.Point>()
        val p0 = org.opencv.core.Point(cropView.points[0]!!.x.toDouble() + delta, cropView.points[0]!!.y.toDouble() + delta)
        val p1 = org.opencv.core.Point(cropView.points[2]!!.x.toDouble() + delta, cropView.points[2]!!.y.toDouble() + delta)
        val p2 = org.opencv.core.Point(cropView.points[3]!!.x.toDouble() + delta, cropView.points[3]!!.y.toDouble() + delta)
        val p3 = org.opencv.core.Point(cropView.points[1]!!.x.toDouble() + delta, cropView.points[1]!!.y.toDouble() + delta)
        sourcePoints.add(p0)
        sourcePoints.add(p1)
        sourcePoints.add(p2)
        sourcePoints.add(p3)

        val startM = Converters.vector_Point2f_to_Mat(sourcePoints)

        val destinationPoints = ArrayList<org.opencv.core.Point>()
        val p4 = org.opencv.core.Point(0.0, 0.0)
        val p5 = org.opencv.core.Point(0.0, sourceBitmap.getHeight().toDouble())
        val p6 = org.opencv.core.Point(sourceBitmap.getWidth().toDouble(), sourceBitmap.getHeight().toDouble())
        val p7 = org.opencv.core.Point(sourceBitmap.getWidth().toDouble(), 0.0)
        destinationPoints.add(p4)
        destinationPoints.add(p5)
        destinationPoints.add(p6)
        destinationPoints.add(p7)
        val endM = Converters.vector_Point2f_to_Mat(destinationPoints)

        val perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM)
        val size = Size(sourceBitmap.getWidth().toDouble(), sourceBitmap.getHeight().toDouble())
        val scalar = Scalar(100.0)
        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS, BORDER_DEFAULT, scalar)

        Utils.matToBitmap(outputMat, descBitmap)

        return descBitmap
    }

    fun setCropView(result: PictureResult) {
        result.toBitmap(screenWidth, screenHeight) { bmp ->
            bitmapResult = bmp!!
            imv.setImageBitmap(bmp)
            imv.visibility = View.VISIBLE
            camera.close()
            camera.visibility = View.GONE

            bottomBar.visibility = View.VISIBLE
            rightBar.visibility = View.GONE

            cropView.points = polygonView.points
            val layoutParams = RelativeLayout.LayoutParams(screenWidth, screenHeight)
            cropView.layoutParams = layoutParams
            if (userConfigObject.isCropEnabled) {
                cropView.visibility = View.VISIBLE
            } else {
                cropView.visibility = View.INVISIBLE
            }

            if (userConfigObject.copyrightWatermark != null) {
                textViewCopyright.visibility = View.VISIBLE
            } else {
                textViewCopyright.visibility = View.GONE
            }

            if (userConfigObject.stampWatermark != null) {
                textViewStamp.visibility = View.VISIBLE
            } else {
                textViewStamp.visibility = View.GONE
            }
        }
    }

    private fun takePhoto() {
        if (userConfigObject.useDetection) {
            camera.removeFrameProcessor(processing)
        }
        stopRepeatingTask()
        camera.takePicture()
    }

    private fun startRepeatingTask() {
        mStatusChecker.run()
    }

    private fun stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker)
    }

    private fun setDetectView() {
        val firstPoint = PointF(
                cardOnScreenPoints[0].toFloat(),
                cardOnScreenPoints[1].toFloat()
        )
        val secondPoint = PointF(
                cardOnScreenPoints[6].toFloat(),
                cardOnScreenPoints[7].toFloat()
        )
        val thirdPoint = PointF(
                cardOnScreenPoints[2].toFloat(),
                cardOnScreenPoints[3].toFloat()
        )
        val forthPoint = PointF(
                cardOnScreenPoints[4].toFloat(),
                cardOnScreenPoints[5].toFloat()
        )

        pointFs[0] = firstPoint
        pointFs[1] = secondPoint
        pointFs[2] = thirdPoint
        pointFs[3] = forthPoint

        val firstGlarePoint = PointF(
                cardOnScreenPoints[8].toFloat(),
                cardOnScreenPoints[9].toFloat()
        )
        val secondGlarePoint = PointF(
                cardOnScreenPoints[14].toFloat(),
                cardOnScreenPoints[15].toFloat()
        )
        val thirdGlarePoint = PointF(
                cardOnScreenPoints[10].toFloat(),
                cardOnScreenPoints[11].toFloat()
        )
        val forthGlarePoint = PointF(
                cardOnScreenPoints[12].toFloat(),
                cardOnScreenPoints[13].toFloat()
        )

        glarePointFs[0] = firstGlarePoint
        glarePointFs[1] = secondGlarePoint
        glarePointFs[2] = thirdGlarePoint
        glarePointFs[3] = forthGlarePoint

        polygonView.setGlarePoint(glarePointFs)
        polygonView.points = pointFs

        val layoutParams = RelativeLayout.LayoutParams(screenWidth, screenHeight)
        polygonView.layoutParams = layoutParams
    }

    private fun determineImageOdometry() {
        val x1 = frameRecorder.last().x1
        val x1Std = frameRecorder.map { it.x1 }.standardDeviation()
        val x1Avg = frameRecorder.map { it.x1 }.average()
        if (x1 > x1Avg + 2 * x1Std || x1 < x1Avg - 2 * x1Std) {
            frameRecorder.clear()
        }
    }
}
