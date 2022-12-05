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
) {
    val frame = Frame(this, framing, deltaRatio * height.defPos, frameTex,
        0f, Flag1.giveSizesToParent
    )
    frame.addLittleBroString(strTex, this.width.defPos, this.height.defPos)
}
/** Convenience extensions */
fun Node.addFrameAndString(
    @DrawableRes frmResId: Int, cstString: String,
    deltaRatio: Float = 0.2f, framing: Framing = Framing.center
) {
    addFrameAndString(Texture.getPng(frmResId), Texture.getConstantString(cstString), deltaRatio, framing)
}
fun Node.addFrameAndString(
    @DrawableRes frmResId: Int, @StringRes locStrResId: Int,
    deltaRatio: Float = 0.2f, framing: Framing = Framing.center
) {
    addFrameAndString(Texture.getPng(frmResId), Texture.getLocalizedString(locStrResId), deltaRatio, framing)
}



class FramedString : Node {
    val string : StringSurface
        get() = lastChild as StringSurface
    val frame: Frame
        get() = firstChild as Frame



    constructor(ref: Node?,
                frameTex: Texture, strTex: Texture,
                x: Float, y: Float, width: Float, height: Float,
                framing: Framing = Framing.center,
                flags: Long = 0L, deltaRatio: Float = 0.2f, setWidth: Boolean = false
    ) : super(ref, x, y, width, height, 0f, flags) {
        val delta = max(0.01f, min(deltaRatio, 0.45f)) * height
        val frame = Frame(this, framing, delta, frameTex, 0f, 0L)
        val str = frame.addLittleBroString(strTex, width, height)
        if(setWidth)
            str.setWidth(true)
    }
    /** Convenience init */
    constructor(ref: Node?,
                @DrawableRes frameResId: Int, cstString: String,
                x: Float, y: Float, width: Float, height: Float,
                framing: Framing = Framing.center,
                flags: Long = 0L, deltaRatio: Float = 0.2f, setWidth: Boolean = false
    ) : this(ref, Texture.getPng(frameResId), Texture.getConstantString(cstString), x, y, width, height,
        framing, flags, deltaRatio, setWidth)
    constructor(ref: Node?,
                @DrawableRes frameResId: Int, @StringRes locStringId: Int,
                x: Float, y: Float, width: Float, height: Float,
                framing: Framing = Framing.center,
                flags: Long = 0L, deltaRatio: Float = 0.2f, setWidth: Boolean = false
    ) : this(ref, Texture.getPng(frameResId), Texture.getLocalizedString(locStringId), x, y, width, height,
        framing, flags, deltaRatio, setWidth)
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