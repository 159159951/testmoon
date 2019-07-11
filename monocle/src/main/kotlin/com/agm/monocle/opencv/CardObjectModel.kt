package com.agm.monocle.opencv

data class CardObjectModel(
    val x1: Double,
    val type: TypeOfCard
)

enum class TypeOfCard {
    LONG, SHORT
}