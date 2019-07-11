package com.agm.monocle.model

import com.google.gson.annotations.SerializedName

data class AGMMonocleConfiguration(
        @SerializedName("copyrightWatermark") var copyrightWatermark: CopyrightWatermark?,
        @SerializedName("isCropEnabled") var isCropEnabled: Boolean,
        @SerializedName("stampWatermark") var stampWatermark: StampWatermark?,
        @SerializedName("useDetection") var useDetection: Boolean,
        @SerializedName("waterMarkLogo") var waterMarkLogo: Int
)