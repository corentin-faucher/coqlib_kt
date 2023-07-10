@file:Suppress("unused")

package com.coq.coqlib.nodes

import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import com.coq.coqlib.R
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.nodes.Node.Companion.showFrame
import com.coq.coqlib.printdebug
import kotlin.math.max
import kotlin.math.min

/*---------------------------------------*/
/*-- Quelques extensions utiles. --------*/
/*---------------------------------------*/

/** Ajouter un frame au noeud courant. Le frame s'ajuste à la taille du noeud. */
fun Node.addFrame(
    @DrawableRes rid: Int, delta: Float, framing: Framing = Framing.center, lambda: Float = 0f
) = Frame(this, framing, delta, rid, lambda, Flag1.frameOfParent)

/** Remplir avec une string encadrée. La taille du noeud sera ajusté pour la taille de la string. */
fun Node.addFrameAndString(
    frameTex: Texture, strTex: Texture,
    deltaRatio: Float = 0.2f, framing: Framing = Framing.center
) : Pair<Frame, StringSurface>
{
    val frame = Frame(this, framing, deltaRatio * height.defPos, frameTex,
        0f, Flag1.giveSizesToParent
    )
    val string = frame.addLittleBroString(strTex, this.width.defPos, this.height.defPos)
    return frame to string
}
/** Convenience extensions */
fun Node.addFrameAndString(
    @DrawableRes frmResId: Int, cstString: String,
    deltaRatio: Float = 0.2f, framing: Framing = Framing.center
) : Pair<Frame, StringSurface>
{
    return addFrameAndString(Texture.getPng(frmResId), Texture.getConstantString(cstString), deltaRatio, framing)
}
fun Node.addFrameAndString(
    @DrawableRes frmResId: Int, @StringRes locStrResId: Int,
    deltaRatio: Float = 0.2f, framing: Framing = Framing.center
) : Pair<Frame, StringSurface>
{
    return addFrameAndString(Texture.getPng(frmResId), Texture.getLocalizedString(locStrResId), deltaRatio, framing)
}



class FramedString : Node {
    val frame: Frame
    val string : StringSurface

    constructor(
        ref: Node?, frameTex: Texture, strTex: Texture,
        x: Float, y: Float, width: Float, height: Float, flags: Long = 0L,
        framing: Framing = Framing.center, deltaRatio: Float = 0.2f, setWidth: Boolean = false
    ) : super(ref, x, y, width, height, 0f, flags) {
        val delta = max(0.01f, min(deltaRatio, 0.45f)) * height
        frame = Frame(this, framing, delta, frameTex)
        string = frame.addLittleBroString(strTex, width, height)
        if(setWidth)
            string.setWidth(true)
    }

    constructor(
        ref: Node?, @DrawableRes pngId: Int, strTex: Texture,
        x: Float, y: Float, width: Float, height: Float, flags: Long = 0L,
        framing: Framing = Framing.center, deltaRatio: Float = 0.2f, setWidth: Boolean = false
    ) : super(ref, x, y, width, height, 0f, flags) {
        val delta = max(0.01f, min(deltaRatio, 0.45f)) * height
        frame = Frame(this, framing, delta, pngId, flags = Flag1.giveSizesToParent)
        string = frame.addLittleBroString(strTex, width, height)
        if(setWidth)
            string.setWidth(true)
    }
}


/** !Debug Option!
 * Ajout d'une surface "frame" pour visualiser la taille d'un "bloc".
 * L'option Node.showFrame doit être "true". */
fun Node.tryToAddFrame() {
    if (!showFrame) return
    TestFrame(this)
}

/** Noeud pour tester les polices de strings et leurs dimensions pour être mieux cadré. */
class TestString(ref: Node, string: String = "jTqx", @FontRes fontRes: Int? = null,
                 x: Float, y: Float, height: Float
) : Node(ref, x, y, 1f, 1f)
{
    init {
        scaleX.set(height)
        scaleY.set(height)
        val stringTex = Texture.getConstantString(string, fontRes)
        val surf = StringSurface(this, stringTex, 0f, 0f, 1f)
        surf.setWidth(true)
        TiledSurface(this, R.drawable.test_frame,
            0f, 0f, surf.width.realPos, surf.height.realPos, 0f)
        TiledSurface(this, R.drawable.test_frame,
            0f, surf.deltaY * Texture.yStringRelShift, 2f*surf.deltaX, surf.deltaY, 0f)
    }
}