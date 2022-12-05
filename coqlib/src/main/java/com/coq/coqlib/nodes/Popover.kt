@file:Suppress("ConvertSecondaryConstructorToPrimary", "unused", "MemberVisibilityCanBePrivate",
    "LocalVariableName"
)

package com.coq.coqlib.nodes

import androidx.annotation.StringRes
import com.coq.coqlib.*
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleAtFixedRateGL
import com.coq.coqlib.graph.scheduleGL
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.maths.times
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Suppress("Tester PopMessages...")

/** Noeud temporaire qui "popover" (et disparaît). */
class PopMessage : Node
{
    private constructor(refNode: Node, deltaT: Float,
                        frameTex: Texture, strTex: Texture,
                        fadePos: Vector2, fadeScale: Vector2,
                        x: Float, y: Float, width: Float, height: Float,
    ) : super(refNode, x, y, width, height, 4f, Flag1.notToAlign)
    {
        // Structure (une string encadrée)
        addFrameAndString(frameTex, strTex, 0.2f)
        // Ouvrir en premier pour que la string aie sa vrai largeur.
        openAndShowBranch()
        // Dépassement ? -> Ajustement
        val (v, vS) = positionAndDeltaAbsolute()
        @Suppress("ControlFlowWithEmptyBody") val x_over_left = v.x - vS.x + 0.5f * screen.width.realPos
        val x_over_right = v.x + vS.x - 0.5f * screen.width.realPos
        val x_adj: Float = min(x_over_left, max(x_over_right, 0f)) * deltaX / vS.x
        val y_over_bottom = v.y - vS.y + 0.5f * screen.height.realPos
        val y_over_top = v.y + vS.y - 0.5f * screen.height.realPos
        val y_adj: Float = min(y_over_bottom, max(y_over_top, 0f)) * deltaY / vS.y
        // Effet d'apparition
        this.x.setRelToDef(fadePos.x, true)
        this.x.setRelToDef(-x_adj, false)
        this.y.setRelToDef(fadePos.y, true)
        this.y.setRelToDef(-y_adj, false)
        scaleX.fadeIn(fadeScale.x)
        scaleY.fadeIn(fadeScale.y)
        // Fini
        Timer().scheduleGL((deltaT*1000f).toLong()) {
            discard()
        }
    }

    fun discard() {
        if(containsAFlag(Flag1.show))
            closeBranch()
        // Retirer de la structure...
        Timer().scheduleGL(1000L) {
            disconnect()
        }
    }

    companion object {
        fun initWith(frontScreen: Screen, frameTexture: Texture) {
            screen = frontScreen
            defaultFrameTexture = frameTexture
        }
        //
        operator fun invoke(
            strTex: Texture, frameTex: Texture?,
            x: Float, y: Float, width: Float, height: Float,
            fadePos: Vector2, fadeScale: Vector2, deltaT: Float
        ) : PopMessage
        {
            val frame = frameTex ?: defaultFrameTexture
            return PopMessage(screen, deltaT,
                frame, strTex, fadePos, fadeScale,
                x, y, width, height)
        }
        /** Convenience init pour les localized string. */
        operator fun invoke(
            @StringRes locStrId: Int, frameTex: Texture? = null
        ) : PopMessage
        {
            return PopMessage(Texture.getLocalizedString(locStrId), frameTex,
                0f, 0f, 1f, 0.2f,
                Vector2(0f, -0.1f), Vector2(-0.5f, -0.5f), 2f)
        }
        operator fun invoke(
            string: String, frameTex: Texture? = null
        ) : PopMessage
        {
            return PopMessage(Texture.getConstantString(string), frameTex,
                0f, 0f, 1f, 0.2f,
                Vector2(0f, -0.1f), Vector2(-0.5f, -0.5f), 2f)
        }
        fun over(ref: Node, strTex: Texture, frameTex: Texture? = null,
            deltaT: Float = 2f, inFrontScreen: Boolean = true, relHeight: Float = 0.5f,
        ) : PopMessage
        {
            val frame = frameTex ?: defaultFrameTexture
            var height = ref.height.realPos * relHeight
            var fadePos = Vector2(0f, -0.5f*height)
            val fadeScale = Vector2(-0.5f, -0.5f)
            if(!inFrontScreen) {
                printdebug("PopMessage in ref with height")
                return PopMessage(ref, deltaT, frame, strTex,
                    fadePos, fadeScale,
                    0f, 0.5f*ref.height.realPos, 20f*height, height)
            }
            val sq = Squirrel(ref, Vector2(ref.x.realPos, ref.y.realPos + ref.deltaY), ScaleInit.Ones)
            @Suppress("ControlFlowWithEmptyBody")
            while(sq.goUpPS()) {}
            fadePos *= sq.vS
            height *= sq.sx
            return PopMessage(screen, deltaT, frame, strTex,
                    fadePos, fadeScale,
                    sq.x, sq.y, 20f*height, height)
        }
        /** Convenience init */
        fun over(ref: Node, @StringRes locStrId: Int, frameTex: Texture? = null,
                 deltaT: Float = 2f, inFrontScreen: Boolean = true, relHeight: Float = 0.5f,
        ) : PopMessage
        {
            return over(ref, Texture.getLocalizedString(locStrId), frameTex, deltaT, inFrontScreen, relHeight)
        }
        fun over(ref: Node, cstString: String, frameTex: Texture? = null,
                 deltaT: Float = 2f, inFrontScreen: Boolean = true, relHeight: Float = 0.5f,
        ) : PopMessage
        {
            return over(ref, Texture.getConstantString(cstString), frameTex, deltaT, inFrontScreen, relHeight)
        }

//        val maxWidth: Float = screen.width.realPos * 0.95f

        private lateinit var defaultFrameTexture: Texture
        internal lateinit var screen: Screen
    }
}

/** Progress disk qui pop et disparaît
 * Superflu ? En faire un cas particulier de Popover ? */
class PopDisk : ProgressDisk {
    private var timer1 = Timer()
    private var timer2 = Timer()
    private var chrono = Chrono()
    private val deltaT: Float

    constructor(refNode: Node, pngTex: Texture, deltaT: Float,
                x: Float, y: Float, height: Float,
                lambda: Float, i: Int, flags: Long = 0L
    ) : super(refNode, pngTex, x, y, height, lambda, i, flags)
    {
        this.deltaT = deltaT
        start()
    }
    // Retirer prématurément le popDisk.
    fun discard() {
        // Essayer plus tard s'il vient juste d'être créé. (faut quand même le voir un peu...)
        if(chrono.elapsedSec < 0.3f*deltaT) {
            Timer().scheduleGL((320f*deltaT).toLong() - chrono.elapsedMS) {
                discard()
            }
            return
        }
        timer1.cancel()
        timer2.cancel()
        closeBranch()
        // Retirer de la structure...
        Timer().scheduleGL(1000L) {
            disconnect()
        }
    }
    /** Copie... Superflu pour les popover ?... */
    private constructor(other: PopDisk) : super(other) {
        deltaT = other.deltaT
        start()
    }
    override fun clone() = PopDisk(this)

    private fun start() {
        // Commence "vide" -> ratio = 0.
        updateRatio(0f)
        chrono.start()
        // Effet d'apparition.
        this.y.fadeInFromDef(height.realPos)
        width.fadeIn(-height.realPos * 0.3f)
        this.height.fadeIn(-height.realPos * 0.3f)
        // Ouvrir le noeud
        openAndShowBranch()
        // Mise à jour en continue
        timer1.scheduleAtFixedRateGL(50L, 50L) {
            updateRatio(min(chrono.elapsedSec / deltaT, 1f))
        }
        // Fini
        timer2.scheduleGL((1000f * deltaT).toLong()) {
            discard()
        }
    }

}

/** Surface "TiledSurface" qui popover. Superflu ? */
class PopSurface : TiledSurface {
    private constructor(pngTex: Texture, time: Float,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, i: Int = 0, flags: Long = 0
    ) : super(PopMessage.screen, pngTex, x, y, height, lambda, i, flags) {
        openAndShowBranch()
        Timer().scheduleGL((1000f*time).toLong()) {
            closeBranch()
            Timer().scheduleGL(1000L) {
                disconnect()
            }
        }
    }
    companion object {
        operator fun invoke(
            overed: Node, pngTex: Texture, time: Float,
            x_rel: Float, y_rel: Float, h_rel: Float,
            lambda: Float = 0f, i: Int = 0, flags: Long = 0
        ) : PopSurface
        {
            // Hauteur absolue du noeud ref sert pour la largeur.
            val (v, vS) = overed.positionAndDeltaAbsolute()
            val href = 2*vS.y
            return PopSurface(pngTex, time,
                v.x + x_rel*href, v.y + y_rel*href, h_rel*href,
                lambda, i, flags
            )
        }
    }
}

