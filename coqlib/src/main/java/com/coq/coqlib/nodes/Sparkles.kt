package com.coq.coqlib.nodes

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.coq.coqlib.R
import com.coq.coqlib.SoundManager
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleGL
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.maths.random
import com.coq.coqlib.printerror
import java.util.*
import kotlin.math.min
import kotlin.random.Random

class Sparkles : Node {
    private constructor(
        ref: Node,
        x: Float, y: Float, height: Float,
        tex: Texture, soundPoolId: Int,
    ) : super(ref, x, y, 1f, 1f)
    {
        scaleX.set(height)
        scaleY.set(height)
        val i0 = Random.nextInt() % 32
        for (i in 0..8) {
            TiledSurface(this, tex,
                Float.random(0f, 0.05f),
                Float.random(0f, 0.05f), 0.3f,
                5f, i + i0, Flag1.popping
            ).also { surf ->
                surf.x.pos = Float.random(0f, 0.6f)
                surf.y.pos = Float.random(0f, 0.6f)
            }
        }
        SoundManager.playWithSoundPoolId(soundPoolId)
        openAndShowBranch()
        Timer().scheduleGL(0.6f) {
            closeBranch()
        }
        Timer().scheduleGL(1.6f) {
            disconnect()
        }
    }

    companion object {
        operator fun invoke(
            x: Float, y: Float, height: Float,
            @DrawableRes drawableId: Int? = null, soundPoolId: Int? = null
        ) : Sparkles?
        {
            if(!::screen.isInitialized) {
                printerror("Sparkles not init. Called to Sparkles.initWith needed.")
                return null
            }
            val tex = drawableId?.let { Texture.getPng(drawableId) } ?: sparklesTex
            val spId = soundPoolId ?: Sparkles.soundPoolId
            return Sparkles(screen, x, y, height, tex, spId)
        }

        operator fun invoke(@DrawableRes drawableId: Int? = null, soundPoolId: Int? = null,
        ) : Sparkles?
        {
            if(!::screen.isInitialized) {
                printerror("Sparkles not init. Called to Sparkles.initWith needed.")
                return null
            }
            val x = Float.random(0f, 0.45f * screen.width.realPos)
            val y = Float.random(0f, 0.45f * screen.height.realPos)
            val height = 0.30f * min(screen.width.realPos, screen.height.realPos)
            val tex = drawableId?.let { Texture.getPng(drawableId) } ?: sparklesTex
            val spId = soundPoolId ?: Sparkles.soundPoolId
            return Sparkles(screen, x, y, height, tex, spId)
        }

        fun over(ref: Node, height: Float? = null,
                 @DrawableRes drawableId: Int? = null, soundPoolId: Int? = null,
        ) : Sparkles?
        {
            if(!::screen.isInitialized) {
                printerror("Sparkles not init. Called to Sparkles.initWith needed.")
                return null
            }
            val sq = Squirrel(ref, ScaleInit.Deltas)
            @Suppress("ControlFlowWithEmptyBody")
            while(sq.goUpPS()) {}
            val theHeight = height ?: (min(sq.sy, sq.sx) * 2f)
            val tex = drawableId?.let { Texture.getPng(drawableId) } ?: sparklesTex
            val spId = soundPoolId ?: Sparkles.soundPoolId
            return Sparkles(screen,
                Float.random(sq.x, theHeight), Float.random(sq.y, theHeight),
                theHeight, tex, spId)
        }

        lateinit var screen: Screen
        lateinit var sparklesTex: Texture
        var soundPoolId: Int = 0
    }
}