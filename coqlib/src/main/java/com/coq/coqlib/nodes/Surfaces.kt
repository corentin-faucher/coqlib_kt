/** Surfaces.kt
 * Les noeuds surfaces : des noeuds feuilles ayant une mesh et texture OpenGL à afficher.
 * (Ce sont les seuls noeud retenus par le Renderer pour caller glDrawElements ou glDrawArrays)
 * Corentin Faucher
 * 30 novembre 2022 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate",
    "EnumEntryName", "LocalVariableName", "PropertyName"
)

package com.coq.coqlib.nodes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.coq.coqlib.*
import com.coq.coqlib.graph.*
import com.coq.coqlib.maths.SmTrans
import kotlin.math.max
import kotlin.math.min

/** Un noeud "surface". Noeud qui est affiché. Possède une texture (image png par exemple)
 * et une mesh (sprite par défaut).
 * Utilisez de préférence les sous-classes un peu plus bas... */
open class Surface : Node {
    // Fields pour Surface:
    var tex: Texture
    var mesh: Mesh
    val trShow: SmTrans
    val trExtra: SmTrans
    var x_margin: Float = 0f

    /** Init comme une surface ordinaire avec texture directement.
     * La surface est carrée par défaut.
     * Pour une sprite ordinaire, utiliser TiledSurface. */
    constructor(refNode: Node?, tex: Texture,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0,
                mesh: Mesh = Mesh.sprite,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigBro) {
        this.tex = tex
        this.mesh = mesh
        trShow = SmTrans()
        trExtra = SmTrans()
    }
    /** Init comme une surface ordinaire avec id de texture.
     * La surface est carrée par défaut.
     * Pour une sprite ordinaire, utiliser TiledSurface. */
    constructor(refNode: Node?, @DrawableRes pngResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0,
                mesh: Mesh = Mesh.sprite,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigBro)
    {
        this.tex = Texture.getPng(pngResId)
        this.mesh = mesh
        trShow = SmTrans()
        trExtra = SmTrans()
    }
    /** White sprite. Modifiez piu.color pour changer la couleur.
     * Init avec la largeur -> Inclue implicitement le flag Flag1.surfaceDontRespectRatio. */
    constructor(refNode: Node?,
                x: Float, y: Float, width: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0,
                mesh: Mesh = Mesh.sprite,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, x, y, width, height, lambda, flags or Flag1.surfaceDontRespectRatio,
        asParent, asElderBigBro)
    {
        tex = Texture.getPng(R.drawable.white)
        this.mesh = mesh
        trShow = SmTrans()
        trExtra = SmTrans()
    }

    /** Constructeur de copie. */
    protected constructor(other: Surface) : super(other) {
        tex = other.tex  // (Ici, pas de copie, juste la référence vers la même texture...)
        mesh = other.mesh
        trShow = SmTrans(other.trShow)
        trExtra = SmTrans(other.trExtra)
        x_margin = other.x_margin
    }
    override fun clone() = Surface(this)

    /** S'il n'y a pas le flag surfaceDontRespectRatio, la largeur est ajustée.
     * Sinon, on ne fait que vérifier le frame voisin
     * et le parent. */
    open fun setWidth(fix: Boolean) {
        if(containsAFlag(Flag1.surfaceDontRespectRatio))
            return
        val extra_x = deltaY * x_margin
        if (containsAFlag(Flag1.stringWithCeiledWidth) && width.defPos > 2 * extra_x) {
            width.set(
                min(height.realPos * tex.ratio, (width.defPos - extra_x) / tex.scaleX),
                fix = fix, setAsDef = false)

        } else {
            width.set(height.realPos * tex.ratio,
                fix = fix, setAsDef = true)
        }
        // Ajustement du spacing en x.
        scaleX.set(tex.scaleX + extra_x / width.realPos)
        // Ajuster le frame si présent.
        if (containsAFlag(Flag1.giveSizesToBigBroFrame))
            (bigBro as? Frame)?.updateWithLittleBro(fix)
        // Donner les dimensions au parent si présent.
        if (containsAFlag(Flag1.giveSizesToParent))
            parent?.let{ parent ->
                parent.width.set(deltaX * 2)
                parent.height.set(deltaY * 2)
                parent.setRelatively(fix)
            }
    }

    override fun isDisplayActive() : Boolean {
        return trShow.isActive
    }
}

class StringSurface: Surface
{
    constructor(refNode: Node?, strTex: Texture,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0, ceiledWidth: Float? = null,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, strTex, x, y, height, lambda, flags, Mesh.sprite,
        asParent, asElderBigBro)
    {
        width.set(ceiledWidth ?: height)
        if(strTex.type == Texture.TextureType.Png) {
            printerror("Pas une texture de string.")
            tex = Texture.getConstantString("?")
        }
        if(ceiledWidth != null) {
            addFlags(Flag1.stringWithCeiledWidth)
        }
        piu.color = floatArrayOf(0f, 0f, 0f, 1f)  // (Text noir par défaut.)
    }
    /** "Convenience init" pour string constante */
    constructor(refNode: Node?, cstString: String,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0, ceiledWidth: Float? = null
    ) : this(refNode, Texture.getConstantString(cstString),
        x, y, height, lambda, flags, ceiledWidth)
    /** Convenience init pour string localisées */
    constructor(refNode: Node?, @StringRes locResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0, ceiledWidth: Float? = null
    ) : this(refNode, Texture.getLocalizedString(locResId),
        x, y, height, lambda, flags, ceiledWidth)
    /** Copie... */
    private constructor(other: StringSurface) : super(other)
    override fun clone() = StringSurface(this)

    override fun setWidth(fix: Boolean) {
        // 1. Vérifier l'espacement en y.
        scaleY.set(tex.scaleY) // y en premier... (scaleX dépend de deltaY...)
        // 2. Height s'ajuste au scaling pour garder deltaY constant... defPos == 2 * deltaY
        height.set(height.defPos / scaleY.realPos, fix = true, setAsDef = false)
        super.setWidth(fix)
    }

    override fun open() {
        setWidth(true)
        super.open()
    }

    override fun reshape() {
        setWidth(false)
        super.reshape()
    }

    fun updateStringTexture(newTexture: Texture) {
        if (newTexture.type == Texture.TextureType.Png) {
            printerror("Not a string texture.")
            return
        }
        tex = newTexture
    }
    /** "Convenience function": Ne change pas la texture.
     *  Ne fait que mettre à jour la string de la texture. */
    fun updateAsMutableString(newString: String) {
        if(tex.type != Texture.TextureType.MutableString) {
            printerror("Not a mutable string texture.")
            return
        }
        tex.updateAsMutableString(newString)
    }
    /** "Convenience function": Remplace la texture actuel pour
     * une texture de string constant (non mutable). */
    fun updateTextureToConstantString(newString: String) {
        tex = Texture.getConstantString(newString)
    }
}

/** Cas standard pour les surfaces. Init avec les proportions du png. */
open class TiledSurface: Surface {
    constructor(refNode: Node?, pngTex: Texture,
                x: Float, y: Float, height: Float,
                lambda: Float, i: Int = 0, flags: Long = 0,
                mesh: Mesh = Mesh.sprite,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, pngTex,
        x, y, height, lambda, flags,
        mesh, asParent, asElderBigBro)
    {
        setWidth(true)
        updateTile(i, 0)
    }
    constructor(refNode: Node?, @DrawableRes pngResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, i: Int = 0, flags: Long = 0,
                mesh: Mesh = Mesh.sprite,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, pngResId, x, y, height, lambda, flags, mesh, asParent, asElderBigBro) {
        setWidth(true)
        updateTile(i, 0)
    }
    /** Init avec largeur -> ajout du flag surfaceDontRespectRatio. */
    constructor(refNode: Node?, @DrawableRes pngResId: Int,
                x: Float, y: Float, width: Float, height: Float,
                lambda: Float = 0f, i: Int = 0, flags: Long = 0,
                mesh: Mesh = Mesh.sprite,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, pngResId,
        x, y, height, lambda, flags or Flag1.surfaceDontRespectRatio,
        mesh, asParent, asElderBigBro)
    {
        this.width.set(width)
        // (pas de setWidth)
        updateTile(i, 0)
    }
    /** Copie... */
    protected constructor(other: TiledSurface) : super(other)
    override fun clone() = TiledSurface(this)

    /** Si i > m -> va sur les lignes suivantes. */
    fun updateTile(i: Int, j: Int) {
        piu.i = (i % tex.m).toFloat()
        piu.j = ((j + i / tex.m) % tex.n).toFloat()
    }
    /** Ne change que l'index "i" de la tile (ligne) */
    fun updateTileI(index: Int) {
        piu.i = (index % tex.m).toFloat()
    }
    /** Ne change que l'index "j" de la tile (colonne) */
    fun updateTileJ(index: Int) {
        piu.j = (index % tex.n).toFloat()
    }

    /** Ne change que la texture (pas de updateRatio). */
    fun updateTexture(newTexture: Texture) {
        if(newTexture.type != Texture.TextureType.Png) {
            printerror("Not a png texture.")
            return
        }
        tex = newTexture
    }
    /** Ne change que la texture (pas de updateRatio). */
    fun updatePng(@DrawableRes pngResId: Int) {
        tex = Texture.getPng(pngResId)
    }
}

/** Surface avec une mesh "FanMesh" pour afficher une section de disque. */
open class ProgressDisk: TiledSurface {
    constructor(refNode: Node?, @DrawableRes pngResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, i: Int = 0, flags: Long = 0,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, pngResId, x, y, height, lambda, i, flags,
        FanMesh(), asParent, asElderBigBro)
    /** Copie... */
    protected constructor(other: ProgressDisk) : super(other) {
        mesh = FanMesh()  // Chaque ProgressDisk a sa propre FanMesh...
    }
    override fun clone() = ProgressDisk(this)

    fun updateRatio(ratio: Float) {
        (mesh as FanMesh).updateWithRatio(ratio)
    }
}

/** LanguageSurface : cas particulier de TiledSurface. La tile est fonction de la langue. */
open class LanguageSurface : Surface {
    constructor(refNode: Node?,
                @DrawableRes pngResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0,
                asParent: Boolean = true, asElderBigBro: Boolean = false
    ) : super(refNode, Texture.getPng(pngResId), x, y, height, lambda, flags, Mesh.sprite,
        asParent, asElderBigBro)
    {
        setWidth(true)
    }

    /** Copie... */
    private constructor(other: LanguageSurface) : super(other)
    override fun clone() = LanguageSurface(this)

    override fun open() {
        super.open()
        val i = Language.currentTileId
        piu.i = (i % tex.m).toFloat()
        piu.j = ((i / tex.m) % tex.n).toFloat()
    }

    /** Ne change que la texture (pas de updateRatio). */
    fun updateTexture(newTexture: Texture) {
        if(newTexture.type != Texture.TextureType.Png) {
            printerror("Not a png texture.")
            return
        }
        tex = newTexture
    }
}

enum class Framing {
    outside,
    center,
    inside
}

class Bar : Surface {
    private val framing: Framing
    private val delta: Float

    constructor(parent: Node, framing: Framing, delta: Float, width: Float,
                @DrawableRes pngResId: Int,
                lambda: Float = 0f
    ) : super(parent, Texture.getPng(pngResId), 0f, 0f, delta * 2f,
        lambda, Flag1.surfaceDontRespectRatio, BarMesh())
    {
        this.framing = framing
        this.delta = delta
        update(width, true)
    }
    /** Copie... */
    private constructor(other: Bar) : super(other) {
        framing = other.framing
        delta = other.delta
        mesh = other.mesh.clone()
    }
    override fun clone() = Bar(this)

    fun update(width: Float, fix: Boolean) {
        if (width < 0) {
            printerror("deltaX < 0")
            return
        }
        val smallDeltaX: Float = when(framing) {
            Framing.outside -> max(0f, width/2f - 2f * delta)
            Framing.center -> max(0f, width/2f - delta)
            Framing.inside -> width/2f
        }
        this.width.set(2f * (smallDeltaX + 2f * delta), fix)
        (mesh as BarMesh).updateWithCenterRatio(
            smallDeltaX / (smallDeltaX + 2f * delta)
        )
    }

    fun updateWithLittleBro(fix: Boolean) {
        littleBro?.let { bro ->
            x.set(bro.x.realPos, fix)
            y.set(bro.y.realPos, fix)
            update(bro.deltaX * 2f, fix)
        }
    }
}

/**
 * Les frames ne sont pas fixés relativement à leur parent. (pas de open.super)
 * A priori, utiliser les extensions pour créer des frames (voir NodeStructures.kt).
 * 2 cas :
 * - frame de littleBro String : Prend ses dimension sur la string littleBro
 *   et peut ajuster le parent avec ces dimensions.
 * - frame du parent : Reste en (0, 0) dans le référentiel du parent.
 * Prend ses dimension du parent. (Cadre d'un "bloc") */
class Frame : Surface {
    private val framing: Framing
    private val delta: Float

    constructor(parent: Node, framing: Framing, delta: Float,
                @DrawableRes pngResId: Int,
                lambda: Float = 0f, flags: Long = 0L
    ) : super(parent, pngResId, 0f, 0f, delta * 2f, lambda,
        Flag1.surfaceDontRespectRatio or flags, FrameMesh()
    ) {
        this.framing = framing
        this.delta = delta
    }
    constructor(parent: Node, framing: Framing, delta: Float,
                tex: Texture,
                lambda: Float = 0f, flags: Long = 0L
    ) : super(parent, tex, 0f, 0f, delta * 2f, lambda,
        Flag1.surfaceDontRespectRatio or flags, FrameMesh()
    ) {
        this.framing = framing
        this.delta = delta
    }

    /** Copie... */
    private constructor(other: Frame) : super(other) {
        framing = other.framing
        delta = other.delta
        mesh = other.mesh.clone()
    }
    override fun clone() = Frame(this)

    override fun open() {
        // super.open()  (No call to super)
        if(containsAFlag(Flag1.frameOfParent))
            parent?.let { parent ->
                update(parent.width.realPos, parent.height.realPos, true)
            }
    }

    override fun reshape() {
        // super.reshape() (pareil ici, pas de super)
        // On vérifie tout de fois si le parent a changé...
        if(containsAFlag(Flag1.frameOfParent))
            parent?.let { parent ->
                update(parent.width.realPos, parent.height.realPos, false)
            }
    }

    fun addLittleBroString(strTex: Texture, framedWidth: Float, framedHeight: Float)
    : StringSurface {
        if(framedHeight < 3 * delta || framedWidth < 3 * delta)
            printwarning("Frame too small.", 2)
        return StringSurface(this, strTex, 0f, 0f,
            max(framedHeight - 2*delta, delta),
            0f, Flag1.giveSizesToBigBroFrame,
            max(framedWidth - 2*delta, delta), false
        ).also { string ->
            string.x_margin = 0.7f
        }
    }
    fun updateWithLittleBro(fix: Boolean) {
        littleBro?.let { bro ->
            x.set(bro.x.realPos, fix)
            y.set(bro.y.realPos, fix)
            update(bro.deltaX * 2f, bro.deltaY * 2f, fix)
        }
    }

    /** Pour setter width et height du frame. En général ce n'est pas nécessaire,
     * le frame est ajuster avec sont parent ou petit frère... */
    fun update(width: Float, height: Float, fix: Boolean) {
        if (width < 0 || height < 0) {
            printerror("width or height < 0")
            return
        }
        val smallDeltaX: Float = when(framing) {
            Framing.inside -> max(0f, 0.5f*width - delta)
            Framing.center -> max(0f, 0.5f*width - 0.5f*delta)
            Framing.outside -> 0.5f*width
        }
        val smallDeltaY: Float = when(framing) {
            Framing.inside -> max(0f, 0.5f*height - delta)
            Framing.center -> max(0f, 0.5f*height - 0.5f*delta)
            Framing.outside -> 0.5f*height
        }
        this.width.set(2f * (smallDeltaX + delta), fix)
        this.height.set(2f * (smallDeltaY + delta), fix)

        val xRatio = smallDeltaX / (smallDeltaX + delta)
        val yRatio = smallDeltaY / (smallDeltaY + delta)
        (mesh as FrameMesh).updateWithCenterRatios(xRatio, yRatio)

        if(containsAFlag(Flag1.giveSizesToParent)) parent?.let { parent ->
            parent.width.set(this.width.realPos)
            parent.height.set(this.height.realPos)
            parent.setRelatively(fix)
        }
    }
}


internal class TestFrame : Surface {
    constructor(refNode: Node) : super(refNode, Texture.getPng(R.drawable.test_frame),
        0f, 0f, refNode.height.realPos, 10f,
        Flag1.surfaceDontRespectRatio or Flag1.notToAlign)
    {
        width.set(refNode.width.realPos)
    }
    /** Copie... */
    private constructor(other: TestFrame) : super(other)
    override fun clone() = TestFrame(this)

    override fun open() {
        parent?.let { theParent ->
            height.pos = theParent.height.realPos
            width.pos = theParent.width.realPos
        } ?: run {
            printerror("Pas de parent.")
        }
        super.open()
    }

    override fun reshape() {
        open()
    }
}