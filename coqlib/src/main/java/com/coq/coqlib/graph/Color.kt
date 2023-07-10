@file:Suppress("unused")

package com.coq.coqlib.graph

import kotlin.math.exp

enum class Color(private val arr: FloatArray) {
    Black(floatArrayOf(0f, 0f, 0f, 1f)),
    BlackBack(floatArrayOf(0.1f, 0.1f, 0.05f, 1f)),
    White(floatArrayOf(1f, 1f, 1f, 1f)),
    WhiteBeige(floatArrayOf(0.95f, 0.92f, 0.85f, 1f)),
    GrayDark(floatArrayOf(0.40f, 0.40f, 0.40f, 1f)),
    GrayDark2(floatArrayOf(0.25f, 0.25f, 0.25f, 0.7f)),
    Gray(floatArrayOf(0.6f, 0.6f, 0.6f, 1f)),
    GrayLight(floatArrayOf(0.80f, 0.80f, 0.80f, 1f)),
    Gray2(floatArrayOf(0.75f, 0.75f, 0.75f, 0.9f)),
    Gray3(floatArrayOf(0.90f, 0.90f, 0.90f, 0.70f)),
    Red(floatArrayOf(1f, 0f, 0f, 1f)),
    RedVermilion(floatArrayOf(1f, 0.3f, 0.1f, 1f)),
    RedCoquelicot(floatArrayOf(1f, 0.2f, 0f, 1f)),
    RedOrange2(floatArrayOf(1f, 0.4f, 0.4f, 1f)),
    RedCoral(floatArrayOf(1f, 0.5f, 0.3f, 1f)),
    RedDark(floatArrayOf(0.2f, 0.1f, 0.1f, 1f)),
    Orange(floatArrayOf(1f, 0.6f, 0f, 1f)),
    OrangeAmber(floatArrayOf(1f, 0.5f, 0f, 1f)),
    OrangeBronze(floatArrayOf(0.8f, 0.5f, 0.2f, 1f)),
    OrangeSaffron(floatArrayOf(1.0f, 0.6f, 0.2f, 1f)),
    OrangeSaffron2(floatArrayOf(1.0f, 0.7f, 0.4f, 1f)),
    YellowCadmium(floatArrayOf(1f, 1f, 0f, 1f)),
    YellowAmber(floatArrayOf(1f, 0.75f, 0f, 1f)),
    YellowCitrine(floatArrayOf(0.90f, 0.82f, 0.04f, 1f)),
    YellowLemon(floatArrayOf(1f, 0.95f, 0.05f, 1f)),
    GreenElectric(floatArrayOf(0f, 1f, 0f, 1f)),
    GreenElectric2(floatArrayOf(0.25f, 1f, 0.25f, 1f)),
    GreenFluo(floatArrayOf(0.5f, 1f, 0.5f, 1f)),
    GreenAo(floatArrayOf(0.0f, 0.55f, 0.0f, 1f)),
    GreenSpring(floatArrayOf(0.2f, 1f, 0.5f, 1f)),
    GreenAvocado(floatArrayOf(0.34f, 0.51f, 0.01f, 1f)),
    GreenDarkCyan(floatArrayOf(0.0f, 0.55f, 0.55f, 1f)),
    Aqua(floatArrayOf(0f, 1f, 1f, 1f)),
    Blue(floatArrayOf(0f, 0.25f, 1f, 1f)),
    BlueSky(floatArrayOf(0.40f, 0.70f, 1f, 1f)),
    BlueSky2(floatArrayOf(0.55f, 0.77f, 1f, 1f)),
    BluePale(floatArrayOf(0.8f, 0.9f, 1f, 1f)),
    BlueAzure(floatArrayOf(0.00f, 0.50f, 1f, 1f)),
    Purple(floatArrayOf(0.8f, 0f, 0.8f, 1f)),
    PurbleChinaPink(floatArrayOf(0.87f, 0.44f, 0.63f, 1f)),
    PurbleElectricIndigo(floatArrayOf(0.44f, 0.00f, 1f, 1f)),
    PurbleBlueViolet(floatArrayOf(0.54f, 0.17f, 0.89f, 1f));

    fun getColor() = arr.copyOf()
    fun blendedWith(other: Color, alpha: Float)
        = floatArrayOf(
        (1f-alpha)*other.arr[0] + alpha*arr[0],
        (1f-alpha)*other.arr[1] + alpha*arr[1],
        (1f-alpha)*other.arr[2] + alpha*arr[2],
        (1f-alpha)*other.arr[3] + alpha*arr[3],
    )
}

fun Float.toColor() : FloatArray
    = when {
        this < 0.0f -> Color.BluePale.getColor()
        this < 0.3f -> {
            val alpha = this / 0.3f
            Color.GreenSpring.blendedWith(Color.BluePale, alpha)
        }
        this < 0.5f -> {
            val alpha = (this - 0.3f) / 0.2f
            Color.YellowCadmium.blendedWith(Color.GreenSpring, alpha)
        }
        this < 0.8f -> {
            val alpha = (this - 0.5f) / 0.3f
            Color.Red.blendedWith(Color.YellowCadmium, alpha)
        }
        this < 1.0f -> {
            val alpha = (this - 0.8f) / 0.2f
            Color.RedDark.blendedWith(Color.Red, alpha)
        }
        else -> Color.RedDark.getColor()
    }

fun FloatArray.toGray(level: Float, alpha: Float) : FloatArray
    = floatArrayOf(
        (1f-alpha)*this[0] + level*alpha,
        (1f-alpha)*this[1] + level*alpha,
        (1f-alpha)*this[2] + level*alpha, this[3]
    )

fun FloatArray.toDark(intensity: Float) : FloatArray {
    val lumIn = (this[0] + this[1] + this[2]) / 3f
    // (factor = 1 -> pas de changement, factor = 0 -> completement noir.)
    val factor = (1f - exp(-intensity * lumIn)) / (lumIn * intensity)
    return floatArrayOf(
        this[0] * factor,
        this[1] * factor,
        this[2] * factor,
        this[3]
    )
}

fun FloatArray.toLight(intensity: Float) : FloatArray {
    val darkIn = 1f - (this[0] + this[1] + this[2]) / 3f
    val factor = (1f - exp(-intensity * darkIn)) / (darkIn * intensity)
    return floatArrayOf(
        1f - factor * (1f - this[0]),
        1f - factor * (1f - this[1]),
        1f - factor * (1f - this[2]),
        this[3]
    )
}