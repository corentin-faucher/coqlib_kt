package com.coq.coqlib.nodes

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.coq.coqlib.R
import com.coq.coqlib.SoundManager
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleGL
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.maths.random
import java.util.*
import kotlin.math.min
import kotlin.random.Random

class Sparkles : Node {
    private constructor(
        ref: Node,
        x: Float, y: Float, height: Float,
        soundPoolId: Int, @DrawableRes pngId: Int
    ) : super(ref, x, y, 1f, 1f)
    {
        scaleX.set(height)
        scaleY.set(height)
        val i0 = Random.nextInt() % 32
        for (i in 0..8) {
            TiledSurface(this, pngId,
                Float.random(0f, 0.05f),
                Float.random(0f, 0.05f), 0.3f,
                5f, i + i0, Flag1.popping
            ).also { surf ->
                surf.x.pos = Float.random(0f, 0.6f)
                surf.y.pos = Float.random(0f, 0.6f)
            }
        }
        SoundManager.play(soundPoolId)
        openAndShowBranch()
        Timer().scheduleGL(0.6f) {
            closeBranch()
        }
        Timer().scheduleGL(1.6f) {
            disconnect()
        }
    }

    companion object {
        fun initWith(frontScreen: Screen, @RawRes soundId: Int = R.raw.fireworks,
                     @DrawableRes sparkleDrawableId: Int = R.drawable.sparkle_stars)
        {
            screen = frontScreen
            pngId = sparkleDrawableId
            soundPoolId = SoundManager.getSoundPoolId(soundId)
        }
        operator fun invoke(
            x: Float, y: Float, height: Float,
            soundPoolId: Int? = null, @DrawableRes drawableId: Int? = null
        ) : Sparkles
        {
            val spId = soundPoolId ?: Sparkles.soundPoolId
            return Sparkles(screen, x, y, height, spId, drawableId ?: pngId)
        }

        operator fun invoke(soundPoolId: Int? = null, @DrawableRes drawableId: Int? = null
        ) : Sparkles
        {
            val x = Float.random(0f, 0.45f * screen.width.realPos)
            val y = Float.random(0f, 0.45f * screen.height.realPos)
            val height = 0.30f * min(screen.width.realPos, screen.height.realPos)
            return Sparkles(x, y, height, soundPoolId, drawableId)
        }

        fun over(ref: Node, height: Float? = null,
                 soundPoolId: Int? = null, @DrawableRes drawableId: Int? = null
        ) : Sparkles
        {
            val sq = Squirrel(ref, Vector2(ref.x.realPos, ref.y.realPos), ScaleInit.Deltas)
            @Suppress("ControlFlowWithEmptyBody")
            while(sq.goUpPS()) {}
            val theHeight = height ?: (min(sq.sy, sq.sx) * 2f)
            return Sparkles(
                Float.random(sq.x, theHeight), Float.random(sq.y, theHeight),
                theHeight, soundPoolId, drawableId)
        }

        private lateinit var screen: Screen
        @DrawableRes private var pngId: Int = R.drawable.sparkle_stars
        private var soundPoolId: Int = 0
    }
}