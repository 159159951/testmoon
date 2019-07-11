# Monocle Android Module

Monocle Android Module is private module. It can detect and auto capture user card. Beside main feature above, it is useful to help developer set and customize all of text size, text value, text color, border color, solid color, image source attributes of copyright Logo, Watermark Text, Stamp Text and Crop captured image for the best fit content.

## Installation
Currently, We have not enough effort to pack this module into a java lib (*.aar, *.jar) and put it in to S3's company server as a package manager. We have tried so much but not successful . But I hope it can be available there in near feature (at least, after Crop feature completed I will take my time to complete it). 

Please feel free to watch this internal [video demo](https://youtu.be/8CD8I1ln9Yo) to have more understand about this library.

```bash
implementation 'agm:monocle:0.1.0' [comming soon]
```

## Usage

```jav
import com.agm.monocle.model.AGMMonocleConfiguration
import com.agm.monocle.model.CopyrightWatermark
import com.agm.monocle.model.StampWatermark
import com.agm.monocle.opencv.CameraWithAutoCaptureActivity
import com.agm.monocle.opencv.MemoryCache
import com.google.gson.Gson

...

private val OPEN_CAMERA_REQ: Int = 100
private lateinit var userConfigObject: AGMMonocleConfiguration
private lateinit var copyrightWatermark: CopyrightWatermark
private lateinit var stampWatermark: StampWatermark

...

userConfigObject = AGMMonocleConfiguration(
null, //copyrightWatermark
false,//isCropEnabled
null, //stampWatermark
false,//useDetection
-1)   //waterMarkLogo

copyrightWatermark = CopyrightWatermark(
"#e9d5e7",//bgColor
20f,      //fontSize
30f,      //rotateAngle
"#copyrightWatermark",//textValue
"#eaff4f" //textColor
)

userConfigObject.copyrightWatermark = copyrightWatermark
userConfigObject.stampWatermark = stampWatermark


stampWatermark = StampWatermark(
"#c4546f",//bgColor
"#050505",//borderColor
10f,      //fontSize
"#stampWatermark",//textValue
"#5530eb" //textColor

)

userConfigObject.waterMarkLogo = R.drawable.water_mark_icon
userConfigObject.isCropEnabled = isChecked
userConfigObject.useDetection = isChecked
```
Declare water_mark_icon.xml like this

```xml
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
android:antialias="true"
android:dither="false"
android:filter="false"
android:gravity="center"
android:src="@drawable/appman_logo_horizontal" //put your small logo watermark *.png here
android:tileMode="repeat" />
```

and after complete configuration above, whenever you want to open camera activity, call function like below:


```jav

private fun openCamera() {
var cameraIntent = Intent(this, CameraWithAutoCaptureActivity::class.java)
cameraIntent.putExtra("BUNDLE_USER_CONFIG", Gson().toJson(userConfigObject))
startActivityForResult(cameraIntent, OPEN_CAMERA_REQ)
}
```

Then, get bitmap backed from CameraWithAutoCaptureActivity
```jav
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
super.onActivityResult(requestCode, resultCode, data)
if (resultCode == Activity.RESULT_OK) {
imageViewResult.setImageBitmap(MemoryCache.getBitmapFromMemCache(data!!.getStringExtra("key")))
}
}
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[Copyright by AppMan Co.](https://www.appman.co.th/)
