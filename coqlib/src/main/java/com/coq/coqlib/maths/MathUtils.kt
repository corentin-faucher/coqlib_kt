@file:Suppress("unused", "OPT_IN_USAGE")

package com.coq.coqlib.maths


import android.util.Base64
import java.nio.ByteBuffer
import kotlin.math.*
import kotlin.random.Random

/** Vecteurs 2D */
data class Vector2(var x: Float = 0.0f, var y: Float = 0.0f)
operator fun Vector2.plus(other: Vector2) = Vector2(x + other.x, y + other.y)
operator fun Vector2.minus(other: Vector2) = Vector2(x - other.x, y - other.y)
operator fun Vector2.times(other: Float) = Vector2(x*other, y*other)
operator fun Vector2.times(other: Vector2) = Vector2(x*other.x, y*other.y)
operator fun Float.times(other: Vector2) = Vector2(this * other.x, this * other.y)
operator fun Vector2.minusAssign(v: Vector2) {
    this.x -= v.x; this.y -= v.y
}
//operator fun Vector2.timesAssign(v: Vector2) {
//    this.x *= v.x; this.y *= v.y
//}
fun dotProduct(v1: Vector2, v2: Vector2) : Float
    = v1.x * v2.x + v1.y * v2.y
fun Vector2.distanceTo(other: Vector2) : Float
    = sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
fun Vector2.normalize() : Vector2 {
    val fA = sqrt(this.x*this.x + this.y*this.y)
    this.x /= fA
    this.y /= fA
    return this
}
fun crossProduct(v1: Vector2) : Vector2
        = Vector2(v1.y, -v1.x)
/** Vecteur 3D */
data class Vector3(var x: Float = 0.0f, var y: Float = 0.0f, var z: Float = 0.0f)
operator fun Vector3.minusAssign(v: Vector3) {
    this.x -= v.x; this.y -= v.y; this.z -= v.z
}
operator fun Vector3.plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
operator fun Vector3.minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
fun crossProduct(v1: Vector3, v2: Vector3) = Vector3(
    v1.y * v2.z - v1.z * v2.y,
    v1.z * v2.x - v1.x * v2.z,
    v1.x * v2.y - v1.y * v2.x
)
fun dotProduct(v1: Vector3, v2: Vector3) : Float
        = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
fun Vector3.normalize() : Vector3 {
    val fA = sqrt(this.x*this.x + this.y*this.y + this.z*this.z)
    this.x /= fA
    this.y /= fA
    this.z /= fA
    return this
}



//println("Err in ${functionName}: ${message}")

fun getIdentity() = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f)

fun FloatArray.translate(tx: Float, ty: Float, tz: Float) {
    this[12] += this[0] * tx + this[4] * ty + this[8] * tz
    this[13] += this[1] * tx + this[5] * ty + this[9] * tz
    this[14] += this[2] * tx + this[6] * ty + this[10] * tz
}

fun FloatArray.scale(sx: Float, sy: Float, sz: Float) {
    this[0] *= sx;    this[1] *= sx;    this[2] *= sx;     this[3] *= sx
    this[4] *= sy;    this[5] *= sy;    this[6] *= sy;     this[7] *= sy
    this[8] *= sz;    this[9] *= sz;    this[10] *= sz;    this[11] *= sz
}

fun FloatArray.rotateX(thetaX: Float) {
    val c = cos(thetaX)
    val s = sin(thetaX)

    val v2 = this.copyOfRange(4, 8) // colonne des y
    val v3 = this.copyOfRange(8, 12) // colonne des z

    this[4] = c*v2[0] + s*v3[0]
    this[5] = c*v2[1] + s*v3[1]
    this[6] = c*v2[2] + s*v3[2]

    this[8]  = c*v3[0] - s*v2[0]
    this[9]  = c*v3[1] - s*v2[1]
    this[10] = c*v3[2] - s*v2[2]
}

fun FloatArray.rotateY(thetaY: Float) {
    val c = cos(thetaY)
    val s = sin(thetaY)

    val v1 = this.copyOfRange(0, 4) // colonne des x
    val v3 = this.copyOfRange(8, 12) // colonne des z

    this[0] = c*v1[0] - s*v3[0]
    this[1] = c*v1[1] - s*v3[1]
    this[2] = c*v1[2] - s*v3[2]

    this[8] = c*v3[0] + s*v1[0]
    this[9] = c*v3[1] + s*v1[1]
    this[10] = c*v3[2] + s*v1[2]
}

fun FloatArray.rotateZ(thetaZ: Float) {
    val c = cos(thetaZ)
    val s = sin(thetaZ)

    val v1 = this.copyOfRange(0, 4) // colonne des x
    val v2 = this.copyOfRange(4, 8) // colonne des y

    this[0] = c*v1[0] + s*v2[0]
    this[1] = c*v1[1] + s*v2[1]
    this[2] = c*v1[2] + s*v2[2]

    this[4] = c*v2[0] - s*v1[0]
    this[5] = c*v2[1] - s*v1[1]
    this[6] = c*v2[2] - s*v1[2]
}

fun FloatArray.rotateYandTranslateYZ(thetaY: Float, ty: Float, tz: Float) {
    val c = cos(thetaY)
    val s = sin(thetaY)

    val v1 = this.copyOfRange(0, 4) // colonne des x
    val v3 = this.copyOfRange(8, 12) // colonne des z

    this[0] = c*v1[0] - s*v3[0]
    this[1] = c*v1[1] - s*v3[1]
    this[2] = c*v1[2] - s*v3[2]

    this[8] =  c*v3[0] + s*v1[0]
    this[9] =  c*v3[1] + s*v1[1]
    this[10] = c*v3[2] + s*v1[2]

    this[12] += s*v1[0]*tz + this[4] * ty + c*v3[0]*tz
    this[13] += s*v1[1]*tz + this[5] * ty + c*v3[1]*tz
    this[14] += s*v1[2]*tz + this[6] * ty + c*v3[2]*tz
}

fun getLookAt(eye: Vector3, center: Vector3, up: Vector3) : FloatArray {
    val n = eye.copy()
    n -= center
    n.normalize()
    val u = crossProduct(up, n)
    u.normalize()
    val v = crossProduct(n, u)

    return floatArrayOf(
        u.x, v.x, n.x, 0f,
        u.y, v.y, n.y, 0f,
        u.z, v.z, n.z, 0f,
        -dotProduct(u, eye), -dotProduct(v, eye), -dotProduct(n, eye), 1f
    )
}

fun getPerspective(nearZ: Float, farZ: Float, middleZ: Float,
                   deltaX: Float, deltaY: Float)
        = floatArrayOf(
    2f * middleZ / deltaX, 0f, 0f, 0f,
    0f, 2f * middleZ / deltaY, 0f, 0f,
    0f, 0f, (farZ + nearZ) / (nearZ - farZ), -1f,
    0f, 0f, (2f * farZ * nearZ) / (nearZ - farZ), 0f
)


enum class Digits {
    Zero,
    One,
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine,
    Space,
    Unused1,
    Underscore,
    Plus,
    Minus,
    Mult,
    Div,
    Dot,
    Comma,
    Second,
    Percent,
    Equal,
    Question,
    Unused3;
}

/*-- Extension des Float --*/
fun Float.toNormalizedAngle()
    = this - ceil((this - PI.toFloat()) / (2f * PI.toFloat())) * 2f * PI.toFloat()

/** Retourne la plus grosse "subdivision" pour le nombre présent en base 10.
     * Le premier chiffre peut être : 1, 2 ou 5. Utile pour les axes de graphiques.
     * e.g.: 792 -> 500, 192 -> 100, 385 -> 200. */
fun Float.toRoundedSubDiv() : Float {
    val pow10 = 10f.pow(floor(log10(this)))
    val mantissa = this / pow10
    if(mantissa < 2f)
        return pow10
    if(mantissa < 5f)
        return pow10 * 2f
    return pow10 * 5f
}

fun Float.truncated(delta: Float)
    = if(this > 0f) max(0f, this - delta)
      else min(0f, this + delta)

fun Float.Companion.random(moy: Float, delta: Float)
    = (Random.nextFloat() - 0.5f) * 2f * delta + moy

/*-- Traitement de Long comme un ensemble de flags. */
fun Long.containsFlag(flag: Long)
    = (this and flag) != 0L
fun Long.removingFlag(toRemove: Long)
    = this and toRemove.inv()
fun Long.addingFlag(toAdd: Long)
    = this or toAdd
fun Long.addingAndRemovingFlag(toAdd: Long, toRemove: Long)
    = (this or toAdd) and toRemove.inv()
fun Long.togglingOption(toToggle: Long)
    = this xor toToggle
fun Long.settingFlag(toSet: Long, isOn: Boolean)
    = if(isOn) this or toSet
        else this and toSet.inv()

fun Int.containsFlag(flag: Int)
    = (this and flag) != 0

/*-- Extension de Int --*/
fun UInt.getHighestDecimal() : Int {
    val highestDecimal = pow2numberOfDigit[getHighestBitIndex()]
    return highestDecimal + (if(this > pow10m1[highestDecimal]) 1 else 0)
}
fun UInt.getTheDigitAt(decimal: Int) : UInt
        = (this / pow10[decimal]) % 10u

private fun UInt.getHighestBitIndex() : Int {
    // flsl() en swift ?
    var index = 31
    while (index > 0) {
        val mask = 1u.shl(index)
        if ((mask and this) != 0u) {
            return index
        }
        index -= 1
    }
    return 0
}

private val pow10 = uintArrayOf(
    1u,       10u,       100u,
    1000u,    10000u,    100000u,
    1000000u, 10000000u, 100000000u,
    1000000000u
)
private val pow10m1 = uintArrayOf(
    9u,       99u,       999u,
    9999u,    99999u,    999999u,
    9999999u, 99999999u, 999999999u,
    4294967295u
)
private val pow2numberOfDigit = intArrayOf(
    0, 0, 0, 0, 1, 1, 1, 2, 2, 2, // 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, ...
    3, 3, 3, 3, 4, 4, 4, 5, 5, 5,
    6, 6, 6, 6, 7, 7, 7, 8, 8, 8,
    9, 9, 9
)

fun IntArray.toByteArray() : ByteArray {
    val byteBuffer = ByteBuffer.allocate(size * 4)
    val intBuffer = byteBuffer.asIntBuffer()
    intBuffer.put(this)
    return byteBuffer.array()
}
/** Convertie l'array de int en une String (avec Base64). */
fun IntArray.serialized() : String {
    return Base64.encodeToString(toByteArray(), Base64.DEFAULT)
}
fun ByteArray.toIntArray() : IntArray {
    val byteBuffer = ByteBuffer.wrap(this)
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = IntArray(intBuffer.capacity())
    intBuffer.get(intArray) // Ici "get" est : get from intBuffer et put to intArray...
    return intArray
}

/** Reconvertie une string en array de int. */
fun String.unserialized() : IntArray? {
    if(isEmpty())
        return null
    val byteArray = Base64.decode(this, Base64.DEFAULT)
    return byteArray.toIntArray()
}

/** Convertir un arry de float en une String (avec Base64). */
fun FloatArray.serialized() : String {
    // 1. Convertion en byteBuffer/byteArray.
    val byteBuffer = ByteBuffer.allocate(size * 4)
    val floatBuffer = byteBuffer.asFloatBuffer()
    floatBuffer.put(this)
    val byteArray = byteBuffer.array()
    // 2. Conversion en string.
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}
/** Reconvertie une string en array de int. */
fun String.unserializedAsFloatArray() : FloatArray? {
    if(isEmpty())
        return null
    val byteArray = Base64.decode(this, Base64.DEFAULT)
    val byteBuffer = ByteBuffer.wrap(byteArray)
    // 2. Remettre dans un floatArray
    val floatBuffer = byteBuffer.asFloatBuffer()
    val floatArray = FloatArray(floatBuffer.capacity())
    floatBuffer.get(floatArray)
    return floatArray
}


/** Retourne un nouvel array étant la version encodée avec la clef "key". */
fun IntArray.encoded(key: Int) : IntArray {
    val intArray = this.copyOf()
    // 2. 1re passe
    var uA : Int = 0xeafc8f75.toInt() xor key
    var uE = 0
    for (index in intArray.indices) {
        uE = intArray[index] xor uA xor uE
        intArray[index] = uE
        uA = uA.shl(1) xor uA.shr(1)
    }
    // 3. 2e passe (laisse le dernier)
    uA = uE
    uE = 0
    for (index in 0..(intArray.size-2)) {
        uE = intArray[index] xor uA xor uE
        intArray[index] = uE
        uA = uA.shl(1) xor uA.shr(1)
    }
    return intArray
}

/** Décode l'array présent avec la clef "key". */
fun IntArray.decode(key: Int) {
    // (Pas besoin d'une copie, on n'a pas besoin de l'original)
    // 1re passe
    var uA : Int = this[size-1]
    var uD: Int
    var uE = 0
    for (index in 0..(size-2)) {
        uD = this[index] xor uE xor uA
        uE = this[index]
        this[index] = uD
        uA = uA.shl(1) xor uA.shr(1)
    }
    // 2e passe
    uA = 0xeafc8f75.toInt() xor key
    uE = 0
    for (index in indices) {
        uD = this[index] xor uE xor uA
        uE = this[index]
        this[index] = uD
        uA = uA.shl(1) xor uA.shr(1)
    }
}
