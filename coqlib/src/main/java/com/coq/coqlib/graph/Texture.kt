// Texture.kt
// Class contenant les info d'une texture OpenGL.
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "ConvertSecondaryConstructorToPrimary")

package com.coq.coqlib.graph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import com.coq.coqlib.*
import java.lang.ref.WeakReference

class Texture {
    data class Tiling(val m: Int, val n: Int, val asLinear: Boolean = true)

    enum class TextureType {
        Png,
        ConstantString,
        MutableString,
        LocalizedString,
    }

    val m: Int
    val n: Int
    val asLinear: Boolean
    var name: String = ""
        private set
    val type: TextureType
    var scaleX: Float = 1f
        private set
    var scaleY: Float = 1f
        private set
    var width: Float = 1f
        private set
    var height: Float = 1f
        private set
    var ratio: Float = 1f
        private set
    private val resId: Int
    @FontRes private val fontRes: Int?
    private var glID: Int = -1

    private constructor(resId: Int, name: String, type: TextureType, @FontRes fontRes: Int?) {
        this.type = type
        this.resId = resId
        this.name = name
        if(type == TextureType.Png) {
            val tiling = tilingOfDrawableRes[resId]
            this.fontRes = null
            m = tiling?.m ?: 1
            n = tiling?.n ?: 1
            asLinear = tiling?.asLinear ?: true
            drawAsPng()
            return
        }
        // Sinon, une string.
        m = 1; n = 1; asLinear = true
        this.fontRes = fontRes
        drawAsString()
    }
    protected fun finalize() {
        freeOpenGLTexture()
    }
    private fun freeOpenGLTexture() {
        if (glID < 0 ) return
        val tmp = IntArray(1)
        tmp[0] = glID
        GLES20.glDeleteTextures(1, tmp, 0)
        glID = -1
    }

    fun updateAsMutableString(string: String) {
        if (type != TextureType.MutableString) {
            printerror("str: $string n'est pas une string mutable.")
            return
        }
        this.name = string
        drawAsString()
    }
    fun drawBitmap() : Bitmap? {
        // 1. Obtenir le typeface approprié pour dessiner la string.
        val (typeface, ratios) = FontManager.getTypefaceAndRatio(fontRes, ctx.get())
        // 2. Init de l'objet Paint pour dessiner.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = AndroidColor.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = typeface
        // 3. Dimension de l'image.
        val string = name.ifEmpty { " " }
        val metric = paint.fontMetrics
        printdebug("font metric of $string : top ${metric.top}, bottom ${metric.bottom}, desc ${metric.descent}, asc ${metric.ascent}, leading ${metric.leading}.")
        printdebug("Now using typeface ${typeface.toString()}")
//        val spreadingWidth = 0.5f
//        val spreadingHeight = 0.4f
        val extraWidth = -ratios.x * metric.ascent
        val textHeight = metric.descent - metric.ascent
        val xHeight = -ratios.y * metric.ascent
        val stringWidth = paint.measureText(string) + 0.5f
        val bitmapWidth = (stringWidth + extraWidth).toInt()
        val bitmapHeight = (metric.bottom - metric.top + 0.5f).toInt()
        val yPos = 0.5f*(bitmapHeight + xHeight) - yStringRelShift * xHeight
        scaleX = stringWidth/ bitmapWidth
        scaleY = 2f*xHeight / bitmapHeight
        if(bitmapWidth < 2 || bitmapHeight < 2) return null
        // 4. Créer un bitmap aux bonnes dimensions
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        // 5. Lier le bitmap à une "toile" qui gère l'action de dessiner...
        val canvas = Canvas(bitmap)
        // 6. Dessiner la string.
        canvas.drawText(string, 0.5f * extraWidth, yPos, paint)
        // 7. fini...
        return bitmap
    }
    private fun drawAsString() {
        // 0. Check si libre...
        if (glID >=0)
            freeOpenGLTexture()
        // 1. Générer une nouvelle texture.
        val textureIdArrayTmp = IntArray(1)
        GLES20.glGenTextures(1, textureIdArrayTmp, 0)
        if (textureIdArrayTmp[0] == 0) {
            printerror("Ne peut générer de texture pour la string ${name}."); return
        }
        // 2. Création de la texture...
        val bitmap = drawBitmap() ?:
        run {
            freeOpenGLTexture()
            printerror("Ne peut générer le bitmap pour la string ${name}.")
            return
        }
        // 3. Binding et paramètres OpenGL...
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdArrayTmp[0])
        val glFilter = if(asLinear) GLES20.GL_LINEAR else GLES20.GL_NEAREST
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, glFilter
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, glFilter
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        // 4. Enregistrer l'ID de la texture OpenGL et ses info.
        glID = textureIdArrayTmp[0]
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        // 5. Libérer le bitmap.
        bitmap.recycle()
    }
    private fun drawAsPng() {
        // 0. Check
        val context = ctx.get() ?: run {
            printerror("No context for Texture init.")
            return
        }
        if (glID >=0)
            freeOpenGLTexture()
        // 1. Générer une nouvelle texture.
        val textureIdArrayTmp = IntArray(1)
        GLES20.glGenTextures(1, textureIdArrayTmp, 0)
        if (textureIdArrayTmp[0] == 0) {
            printerror("Ne peut générer de texture."); return
        }
        // 2. Création de la texture...
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
        name = bitmap.toString()
        // 3. Binding et paramètres OpenGL...
        val glFilter = if(asLinear) GLES20.GL_LINEAR else GLES20.GL_NEAREST
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIdArrayTmp[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, glFilter
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, glFilter
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        // 4. Enregistrer l'ID de la texture OpenGL et ses info.
        glID = textureIdArrayTmp[0]
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        ratio = bitmap.width.toFloat() / bitmap.height.toFloat() *
                n.toFloat() / m.toFloat()
        bitmap.recycle()
    }

    companion object {
        /*-- Static fields --*/
        var loaded = false
        var isInit = false
        /** Dessiner la string avec le typeface présent (ou custom si fontRes != null). */
        var textSize = 64f
        val yStringRelShift = -0.15f
        /*-- Static methods --*/
        fun getConstantString(string: String, @FontRes fontRes: Int? = null) : Texture {
            allConstantStringTextures[string]?.get()?.let { texture ->
                return texture
            }
            val newCstStr = Texture(0, string, TextureType.ConstantString, fontRes)
            allConstantStringTextures[string] = WeakReference(newCstStr)
            allStringTextures.add(WeakReference(newCstStr))
            return newCstStr
        }
        fun getNewMutableString(string: String = "", @FontRes fontRes: Int? = null) : Texture {
            val newMutStr = Texture(0, string, TextureType.MutableString, fontRes)
            allStringTextures.add(WeakReference(newMutStr))
            return newMutStr
        }
        fun getLocalizedString(@StringRes resId: Int, @FontRes fontRes: Int? = null) : Texture {
            allLocalizedStringTextures[resId]?.get()?.let { texture ->
                return texture
            }
            val locStr = Language.localizedStringForCurrent(resId)
            val locStrTex = Texture(resId, locStr ?: "Error",
                TextureType.LocalizedString, fontRes)
            allLocalizedStringTextures[resId] = WeakReference(locStrTex)
            allStringTextures.add(WeakReference(locStrTex))
            return locStrTex
        }
        fun getPng(@DrawableRes resID: Int) : Texture {
            allPngTextures[resID]?.let { weak_tex ->
                weak_tex.get()?.let { texture ->
                    return texture
                } ?: run {
                    printdebug("Texture has been deallocated (redrawing...)")
                }
            }
            val newPng = Texture(resID, "png", TextureType.Png, null)
            allPngTextures[resID] = WeakReference(newPng)
            return newPng
        }

        internal fun init(ctx: Context, programID: Int, extraTiling: Map<Int, Tiling>?) {
            // On garde une référence au context pour dessiner les pngs.
            Companion.ctx = WeakReference(ctx)
            // ID des "Per Texture Uniform" dans le shader.
            ptuTexWHId = GLES20.glGetUniformLocation(programID, "texWH")
            ptuTexMNId = GLES20.glGetUniformLocation(programID, "texMN")
            allPngTextures.clear()
            allConstantStringTextures.clear()
            allLocalizedStringTextures.clear()
            allStringTextures.clear()
            // Ajout à la liste des pngs.
            extraTiling?.forEach { (resId, tiling) ->
                tilingOfDrawableRes.putIfAbsent(resId, tiling)
            }
            isInit = true
            loaded = true
        }
        internal fun bindTo(newTex: Texture) {
            if(newTex === currentTex) {
                return
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, newTex.glID)
            GLES20.glUniform2f(ptuTexWHId, newTex.width, newTex.height)
            GLES20.glUniform2f(ptuTexMNId, newTex.m.toFloat(), newTex.n.toFloat())
            currentTex = newTex
        }
        internal fun unbind() {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            currentTex = null
        }
        internal fun suspend() {
            if(!loaded) {
                printwarning("Textures already unloaded.")
                return
            }
            printdebug("free all opengl textures")
            allStringTextures.strip()
            allStringTextures.forEach { weakTexture ->
                weakTexture.get()?.freeOpenGLTexture()
            }
            allPngTextures.strip()
            allPngTextures.forEach { (_, weakTexture) ->
                weakTexture.get()?.freeOpenGLTexture()
            }
            loaded = false
        }
        internal fun resume() {
            if(!isInit) {
                printwarning("Texture pas init")
                return
            }
            if(loaded) {
                printwarning("Textures already loaded.")
                return
            }
            printdebug("restoring all opengl textures")
            allStringTextures.forEach { weakTexture ->
                weakTexture.get()?.drawAsString()
            }
            allPngTextures.forEach { (_, weakTexture) ->
                weakTexture.get()?.drawAsPng()
            }
            loaded = true
        }
        // Pour changement de langue...
        internal fun updateAllLocalizedStrings() {
            val locCtx = Language.currentCtx ?: run {
                printerror("No localized context.")
                return
            }
            allLocalizedStringTextures.forEach { (resId, wt) ->
                wt.get()?.let { tex ->
                    tex.name = locCtx.getString(resId)
                    tex.drawAsString()
                } ?: run {
                    allLocalizedStringTextures.remove(resId)
                }
            }
        }

        private var ctx: WeakReference<Context> = WeakReference(null)

        private var currentTex: Texture? = null
        private var ptuTexMNId: Int = -1
        private var ptuTexWHId: Int = -1
        // Listes de textures...
        private val allStringTextures = mutableListOf<WeakReference<Texture> >()
        private val allConstantStringTextures = mutableMapOf<String, WeakReference<Texture> >()
        private val allLocalizedStringTextures = mutableMapOf<Int, WeakReference<Texture> >()
        private val allPngTextures = mutableMapOf<Int, WeakReference<Texture> >()
        private val defaultTiling = Tiling(1, 1)
        private var tilingOfDrawableRes: MutableMap<Int, Tiling> = mutableMapOf(
            R.drawable.bar_gray to defaultTiling,
            R.drawable.bar_in to defaultTiling,
            R.drawable.country_flags to Tiling(8, 4),
            R.drawable.digits_black to Tiling(12, 2),
            R.drawable.digits_brown to Tiling(12, 2),
            R.drawable.digits_green to Tiling(12, 2),
            R.drawable.digits_red to Tiling(12, 2),
            R.drawable.disks to Tiling(4, 4),
            R.drawable.frame_gray_back to defaultTiling,
            R.drawable.frame_mocha to defaultTiling,
            R.drawable.frame_red to defaultTiling,
            R.drawable.frame_white_back to defaultTiling,
            R.drawable.icons to Tiling(8, 4),
            R.drawable.language_flags to Tiling(4, 4),
            R.drawable.scroll_bar_back to Tiling(1, 3),
            R.drawable.scroll_bar_front to Tiling(1, 3),
            R.drawable.sliding_menu_back to defaultTiling,
            R.drawable.some_animals to Tiling(4, 7),
            R.drawable.sparkle_stars to Tiling(3, 2),
            R.drawable.switch_back to defaultTiling,
            R.drawable.switch_front to defaultTiling,
            R.drawable.test_frame to Tiling(1, 1, false),
            R.drawable.the_cat to Tiling(1, 1, false),
            R.drawable.white to defaultTiling,
        )
    }
}
