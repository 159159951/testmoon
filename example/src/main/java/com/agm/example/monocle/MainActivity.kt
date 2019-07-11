package com.agm.example.monocle

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.agm.monocle.model.AGMMonocleConfiguration
import com.agm.monocle.model.CopyrightWatermark
import com.agm.monocle.model.StampWatermark
import com.agm.monocle.opencv.CameraWithAutoCaptureActivity
import com.agm.monocle.opencv.MemoryCache
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val OPEN_CAMERA_REQ: Int = 100
    private lateinit var userConfigObject: AGMMonocleConfiguration;
    private lateinit var copyrightWatermark: CopyrightWatermark;
    private lateinit var stampWatermark: StampWatermark;

//    companion object {
//         val BUNDLE_USER_CONFIG = "MAIN_ACTIVITY_BUNDLE_USER_CONFIG"
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()

//        startActivityForResult(Intent(this, CameraWithAutoCaptureActivity::class.java), OPEN_CAMERA_REQ)

    }


    private fun init() {
        userConfigObject = AGMMonocleConfiguration(
                null,
                false,
                null,
                false,
                -1)

        copyrightWatermark = CopyrightWatermark(
                "#e9d5e7",
                20f,
                30f,
                "#copyrightWatermark",
                "#eaff4f"
        )

        stampWatermark = StampWatermark(
                "#c4546f",
                "#050505",
                10f,
                "#stampWatermark",
                "#5530eb"

        )

        btnOpenMonocle.setOnClickListener { v -> openCamera() }
        switchWaterMarkLogo.setOnCheckedChangeListener { buttonView, isChecked ->
            setupCameraView(0, isChecked)
        }
        switchPolicyText.setOnCheckedChangeListener { buttonView, isChecked ->
            setupCameraView(1, isChecked)
        }
        switchStampText.setOnCheckedChangeListener { buttonView, isChecked ->
            setupCameraView(2, isChecked)
        }
        switchToggleCrop.setOnCheckedChangeListener { buttonView, isChecked ->
            setupCameraView(3, isChecked)
        }
        switchUseDetection.setOnCheckedChangeListener { buttonView, isChecked ->
            setupCameraView(4, isChecked)
        }

//        switchWaterMarkLogo.isChecked = true
//        switchPolicyText.isChecked = true
//        switchStampText.isChecked = true
//        switchToggleCrop.isChecked = true
//        switchUseDetection.isChecked = true

    }

    private fun openCamera() {
        var cameraIntent = Intent(this, CameraWithAutoCaptureActivity::class.java);
        cameraIntent.putExtra("BUNDLE_USER_CONFIG", Gson().toJson(userConfigObject));
        startActivityForResult(cameraIntent, OPEN_CAMERA_REQ)
    }


    private fun setupCameraView(case: Int, isChecked: Boolean) {
        when (case) {
            //Water mark logo
            0 -> if (isChecked) {
                userConfigObject.waterMarkLogo = R.drawable.water_mark_icon
            } else {
                userConfigObject.waterMarkLogo = -1
            }
            //Policy Text
            1 -> if (isChecked) {
                userConfigObject.copyrightWatermark = copyrightWatermark
            } else {
                userConfigObject.copyrightWatermark = null
            }
            //Stamp Text
            2 -> if (isChecked) {
                userConfigObject.stampWatermark = stampWatermark
            } else {
                userConfigObject.stampWatermark = null
            }
            //Toggle Crop
            3 -> userConfigObject.isCropEnabled = isChecked
            4 -> userConfigObject.useDetection = isChecked
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            imageViewResult.setImageBitmap(MemoryCache.getBitmapFromMemCache(data!!.getStringExtra("key")))
        }
    }
}
