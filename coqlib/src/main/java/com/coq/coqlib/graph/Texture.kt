// Texture.kt
// Class contenant les info d'une texture OpenGL.
// 9 novembre 2022
// Corentin Faucher

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "ConvertSecondaryConstructorToPrimary")

package com.coq.coqlib.graph

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toBitmap
import com.coq.coqlib.*
import java.lang.ref.WeakReference

// Fonctions utiles pour dessiner les bitmaps de png et de strings...

private data class StringBitmap(val bitmap: Bitmap, val scaleX: Float, val scaleY: Float)

class Texture {
    data class Tiling(val m: Int, val n: Int, val asLinear: Boolean = true, val withMini: Boolean = false)

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

    private fun uploadBitmapToGLTexture(bitmap: Bitmap, recycleBitmap: Boolean) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glID)
        val glFilter = if(asLinear) GLES20.GL_LINEAR else GLES20.GL_NEAREST
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, glFilter
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, glFilter
        )
        // Utile ?
//        GLES20.glTexParameteri(
//            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
//        GLES20.glTexParameteri(
//            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        if(recycleBitmap)
            bitmap.recycle()
    }
    private fun drawAsString() {
        // 0. Check si libre...
        if (glID >=0)
            freeOpenGLTexture()
        // 1. Générer une nouvelle texture OpenGL.
        val textureIdArrayTmp = IntArray(1)
        GLES20.glGenTextures(1, textureIdArrayTmp, 0)
        if (textureIdArrayTmp[0] == 0) {
            printerror("Ne peut générer de texture pour la string ${name}."); return
        }
        // 2. Créer un bitmap de la string.
        val stringBitmap = name.toBitmap(fontRes) ?:
        run {
            freeOpenGLTexture()
            printerror("Ne peut générer le bitmap de la string ${name}.")
            return
        }
        // 3. Sauvegarder les infos
        glID = textureIdArrayTmp[0]
        scaleX = stringBitmap.scaleX
        scaleY = stringBitmap.scaleY
        width = stringBitmap.bitmap.width.toFloat()
        height = stringBitmap.bitmap.height.toFloat()
        ratio = width / height
        // 4. Charger le bitmap dans la texture OpenGL.
        uploadBitmapToGLTexture(stringBitmap.bitmap, true)
    }
    private fun drawAsPng() {
        // 0. Check
        val context = ctx.get() ?: throw Exception("No context for Texture.")
        if (glID >=0)
            freeOpenGLTexture()
        val resources = context.resources
        name = resources.getResourceEntryName(resId)
        // 1. Générer une nouvelle texture OpenGL.
        val textureIdArrayTmp = IntArray(1)
        GLES20.glGenTextures(1, textureIdArrayTmp, 0)
        if (textureIdArrayTmp[0] == 0) {
            printerror("Ne peut générer la texture pour $name."); return
        }
        glID = textureIdArrayTmp[0]
        // 2. S'il y a un mini préchargé, on le met temporairement
//        var isMini = false
        minisBitmaps[resId]?.let { mini ->
            uploadBitmapToGLTexture(mini, false)
            width = mini.width.toFloat()
            height = mini.height.toFloat()
//            isMini = true
//            printdebug("Setting $name with mini $width.")
        } ?:
        // Sinon charger au moins les dimensions
        run {
            val dim_options = BitmapFactory.Options().apply {
                inScaled = false
                inJustDecodeBounds = true // -> Juste prendre les dimensions pour l'instant.
            }
            BitmapFactory.decodeResource(resources, resId, dim_options)
            width = dim_options.outWidth.toFloat()
            height = dim_options.outHeight.toFloat()
        }
        ratio = width / height  *  n.toFloat() / m.toFloat()
        // 4. Charger le "full-sized" bitmap dans la thread principale pour pas perdre de frames...
        Handler(Looper.getMainLooper()).post {
            val bitmap = getBitmapOf(resId, false) ?: return@post
            // 5. Retour à la thread OpenGL pour uploader le bitmap dans la texture OpenGL.
            context.view.queueEvent{
                width = bitmap.width.toFloat()
                height = bitmap.height.toFloat()
//                if(isMini)
//                    printdebug("Now adding real texture to $name new width $width")
                uploadBitmapToGLTexture(bitmap, true)
            }
        }
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

        internal fun init(ctx: CoqActivity, programID: Int, extraTiling: Map<Int, Tiling>?) {
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
            // Loader les minis bitmap
            tilingOfDrawableRes.forEach { (id, tiling) ->
                if(tiling.withMini) {
                    getBitmapOf(id, asMini = true)?.let { mini ->
                        minisBitmaps[id] = mini
                    }
                }
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

        private fun String.toBitmap(@FontRes fontRes: Int?) : StringBitmap? {
            // 1. Obtenir le typeface approprié pour dessiner la string.
            val (typeface, ratios) = FontManager.getTypefaceAndRatio(fontRes, ctx.get())
            // 2. Init de l'objet Paint pour dessiner.
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textSize = Texture.textSize
            paint.color = AndroidColor.WHITE
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = typeface
            // 3. Dimension de l'image.
            val string = this.ifEmpty { " " }
            val metric = paint.fontMetrics
//          printdebug("font metric of $string : top ${metric.top}, bottom ${metric.bottom}, desc ${metric.descent}, asc ${metric.ascent}, leading ${metric.leading}.")
//          printdebug("Now using typeface ${typeface.toString()}")
//          val spreadingWidth = 0.5f
//          val spreadingHeight = 0.4f
            val extraWidth = -ratios.x * metric.ascent
            val textHeight = metric.descent - metric.ascent
            val xHeight = -ratios.y * metric.ascent
            val stringWidth = paint.measureText(string) + 0.5f
            val bitmapWidth = (stringWidth + extraWidth).toInt()
            val bitmapHeight = (metric.bottom - metric.top + 0.5f).toInt()
            val yPos = 0.5f*(bitmapHeight + xHeight) - Texture.yStringRelShift * xHeight
            if(bitmapWidth < 2 || bitmapHeight < 2) return null
            // 4. Créer un bitmap aux bonnes dimensions
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            // 5. Lier le bitmap à une "toile" qui gère l'action de dessiner...
            val canvas = Canvas(bitmap)
            // 6. Dessiner la string.
            canvas.drawText(string, 0.5f * extraWidth, yPos, paint)
            // 7. fini...
            return StringBitmap(bitmap, stringWidth/ bitmapWidth, 2f*xHeight / bitmapHeight)
        }

        private fun getBitmapOf(@DrawableRes resId: Int, asMini: Boolean) : Bitmap? {
            val resources = ctx.get()?.resources ?: throw Exception("No context for Texture ?")
            val drawable = resources.getDrawableForDensity(resId,
                if(asMini) DisplayMetrics.DENSITY_LOW else DisplayMetrics.DENSITY_MEDIUM,
                null
            ) ?: run {
                printerror("Cannot load drawable for id $resId.")
                return null
            }
            return drawable.toBitmap()
        }

        private var ctx: WeakReference<CoqActivity> = WeakReference(null)

        private var currentTex: Texture? = null
        private var ptuTexMNId: Int = -1
        private var ptuTexWHId: Int = -1
        // Listes de textures...
        private val allStringTextures = mutableListOf<WeakReference<Texture> >()
        private val allConstantStringTextures = mutableMapOf<String, WeakReference<Texture> >()
        private val allLocalizedStringTextures = mutableMapOf<Int, WeakReference<Texture> >()
        private val allPngTextures = mutableMapOf<Int, WeakReference<Texture> >()
        private val minisBitmaps = mutableMapOf<Int, Bitmap>()
        val defaultTiling = Tiling(1, 1)
        /** Les pngs inclus par défaut avec coqlib. */
        private var tilingOfDrawableRes: MutableMap<Int, Tiling> = mutableMapOf(
            R.drawable.bar_gray to defaultTiling,
            R.drawable.bar_in to defaultTiling,
            R.drawable.digits_black to Tiling(12, 2),
            R.drawable.disks to Tiling(4, 4),
            R.drawable.frame_gray_back to defaultTiling,
            R.drawable.frame_mocha to defaultTiling,
            R.drawable.frame_red to defaultTiling,
            R.drawable.frame_white_back to defaultTiling,
            R.drawable.language_flags to Tiling(4, 4),
            R.drawable.scroll_bar_back to Tiling(1, 3),
            R.drawable.scroll_bar_front to Tiling(1, 3),
            R.drawable.sliding_menu_back to defaultTiling,
            R.drawable.sparkle_stars to Tiling(3, 2),
            R.drawable.switch_back to defaultTiling,
            R.drawable.switch_front to defaultTiling,
            R.drawable.test_frame to Tiling(1, 1, false),
            R.drawable.the_cat to Tiling(1, 1, false),
            R.drawable.white to defaultTiling,
        )
    }
}
