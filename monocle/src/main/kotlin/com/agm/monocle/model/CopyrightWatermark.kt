package com.agm.monocle.model

import com.google.gson.annotations.SerializedName

data class CopyrightWatermark(
    @SerializedName("bgColor") var bgColor: String,
    @SerializedName("fontSize") var fontSize: Float,
    @SerializedName("rotateAngle") var rotateAngle: Float,
    @SerializedName("textValue") var textValue: String,
    @SerializedName("textColor") var textColor: String
)