package com.agm.monocle.model

import com.google.gson.annotations.SerializedName

data class StampWatermark(
    @SerializedName("bgColor") var bgColor: String,
    @SerializedName("borderColor") var borderColor: String,
    @SerializedName("fontSize") var fontSize: Float,
    @SerializedName("textValue") var textValue: String,
    @SerializedName("textColor") var textColor: String
)