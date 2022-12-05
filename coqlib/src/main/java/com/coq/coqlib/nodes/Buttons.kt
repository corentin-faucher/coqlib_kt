@file:Suppress("unused", "LeakingThis", "ConvertSecondaryConstructorToPrimary",
    "MemberVisibilityCanBePrivate"
)

package com.coq.coqlib.nodes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.coq.coqlib.*
import com.coq.coqlib.graph.Color
import com.coq.coqlib.graph.Disk
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleGL
import java.util.*
import kotlin.math.max
import kotlin.math.min


/** Classe de base des boutons.
 * Par défaut un bouton n'est qu'un carré sans surface.
 * Un bouton est un SelectableNode avec "action()". */
abstract class Button : Node
{
    constructor(refNode: Node?,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0
    ) : super(refNode, x, y, height, height, lambda, flags)
    {
        makeSelectable()
    }
    protected constructor(other: Button) : super(other) {
        makeSelectable()
    }
    abstract fun action()
}

// Simple bouton avec fond et devant affichant un texte lorsque survolé.
abstract class ButtonWithPopover : Button, HoverableWithPopover {
    final override val inFrontScreen: Boolean
    final override val popFrameTex: Texture
    override var popStringTex: Texture? = null
    override var popTimer: Timer? = null
    override var framedString: FramedString? = null

    constructor(ref: Node?,
                popFrameTex: Texture, popInFront: Boolean,
                x: Float, y: Float, height: Float, lambda: Float, flags: Long
    ) : super(ref, x, y, height, lambda, flags)
    {
        inFrontScreen = popInFront
        this.popFrameTex = popFrameTex
    }
    /** Convenience init bouton simple png. */
    constructor(ref: Node?,
                tile: Int, @StringRes popLocStrId: Int,
                x: Float, y: Float, height: Float, lambda: Float, flags: Long,
                @DrawableRes iconResId: Int? = defaultIconId,
                @DrawableRes backResId: Int = defaultBackId, backI: Int = defaultBackTile,
                @DrawableRes popFrameId: Int = defaultPopFrameId, popInFront: Boolean = defaultPopInFront,
    ) : this(ref, Texture.getPng(popFrameId), popInFront,
        x, y, height, lambda, flags)
    {
        TiledSurface(this, backResId, 0f, 0f, height, lambda, backI)
        iconResId?.let {
            TiledSurface(this, it, 0f, 0f, height, lambda, tile)
        }
        setPopString(Texture.getLocalizedString(popLocStrId))
    }
    override fun action() {
        popTimer?.cancel()
        popTimer = null
    }
    companion object {
        @DrawableRes var defaultIconId: Int = R.drawable.icons
        @DrawableRes var defaultBackId: Int = R.drawable.disks
        var defaultBackTile: Int = Disk.Yellow.ordinal
        @DrawableRes var defaultPopFrameId: Int = R.drawable.frame_gray_back
        var defaultPopInFront = false
    }
}

abstract class SecureButton : Node, Draggable {
    private val holdTimeInSec: Float
    private val diskTex: Texture
    private val diskI: Int
    private val failPopFrameTex: Texture
    private val failPopStrTex: Texture
    private var popDisk: PopDisk? = null
    private var actionTimer: Timer? = null

    constructor(ref: Node?,
                holdTimeInSec: Float, diskTex: Texture, diskI: Int,
                failPopStrTex: Texture, failPopFrameTex: Texture,
                x: Float, y: Float, height: Float, lambda: Float, flags: Long
    ) : super(ref, x, y, height, height, lambda, flags)
    {
        this.holdTimeInSec = holdTimeInSec
        this.diskTex = diskTex
        this.diskI = diskI
        this.failPopFrameTex = failPopFrameTex
        this.failPopStrTex = failPopStrTex
        makeSelectable()
    }

    abstract fun action()
    override fun grab(posInit: Vector2) {
        popDisk?.disconnect()
        val h = height.realPos
        popDisk = PopDisk(this, diskTex, holdTimeInSec,
            -0.5f*h, 0f, h, 10f, diskI)
        val newTimer = Timer()
        actionTimer = newTimer
        newTimer.scheduleGL(holdTimeInSec) {
            popDisk?.disconnect()
            popDisk = null
            action()
        }
    }
    override fun drag(posNow: Vector2) {
        // (pass)
    }
    override fun letGo() {
        if(actionTimer == null) return
        actionTimer?.cancel()
        popDisk?.discard()
        actionTimer = null
        popDisk = null
        PopMessage.over(this, failPopStrTex, failPopFrameTex)
    }
    companion object {
        @DrawableRes var defaultDiskId: Int = R.drawable.disks
        var defaultDiskTile: Int = Disk.Red.ordinal
        @DrawableRes var defaultFailFrameId: Int = R.drawable.frame_red
        @StringRes var defaultFailPopStrId = R.string.hold_down
    }
}

abstract class SecureButtonWithPopover : SecureButton, HoverableWithPopover {
    final override val inFrontScreen: Boolean
    final override val popFrameTex: Texture
    override var popStringTex: Texture? = null
    override var popTimer: Timer? = null
    override var framedString: FramedString? = null

    constructor(ref: Node?,
                popFrameTex: Texture, popInFront: Boolean,
                holdTimeInSec: Float, diskTex: Texture, diskI: Int,
                failPopStrTex: Texture, failPopFrameTex: Texture,
                x: Float, y: Float, height: Float, lambda: Float, flags: Long
    ) : super(ref, holdTimeInSec, diskTex, diskI, failPopStrTex, failPopFrameTex,
        x, y, height, lambda, flags)
    {
        inFrontScreen = popInFront
        this.popFrameTex = popFrameTex
    }
    /** Convenience init d'un bouton sécuritaire avec simple icon. */
    constructor(ref: Node?,
                tile: Int, @StringRes popLocStrId: Int,
                x: Float, y: Float, height: Float, lambda: Float, flags: Long,
                @DrawableRes iconResId: Int? = ButtonWithPopover.defaultIconId,
                @DrawableRes backResId: Int = ButtonWithPopover.defaultBackId,
                backI: Int = ButtonWithPopover.defaultBackTile,
                @DrawableRes popFrameId: Int = ButtonWithPopover.defaultPopFrameId,
                popInFront: Boolean = ButtonWithPopover.defaultPopInFront,
                holdTimeInSec: Float = 2f,
                @StringRes failPopStrId: Int = defaultFailPopStrId
    ) : this(ref,
        Texture.getPng(popFrameId), popInFront,
        holdTimeInSec, Texture.getPng(defaultDiskId), defaultDiskTile,
        Texture.getLocalizedString(failPopStrId), Texture.getPng(defaultFailFrameId),
        x, y, height, lambda, flags)
    {
        // Back
        TiledSurface(this, backResId, 0f, 0f, height, 0f, backI)
        iconResId?.let {
            TiledSurface(this, it, 0f, 0f, height, 0f, tile)
        }
        setPopString(Texture.getLocalizedString(popLocStrId))
    }

    override fun action() {
        popTimer?.cancel()
        popTimer = null
    }
}


/** Classe de base des boutons de type "on/off".
 * Contient déjà les sous-noeuds de surface d'une switch. */
abstract class SwitchButton : Node, Draggable
{
    var isOn: Boolean
    private lateinit var back: TiledSurface
    private lateinit var nub: TiledSurface
    private var didDrag = false

    constructor(refNode: Node?, isOn: Boolean,
                            x: Float, y: Float, height: Float,
                            lambda: Float = 0f, flags: Long = 0
    ) : super(refNode, x, y, 2f, 1f, lambda, flags) {
        this.isOn = isOn
        scaleX.set(height)
        scaleY.set(height)
        initStructure()
    }
    private constructor(other: SwitchButton) : super(other) {
        isOn = other.isOn
        initStructure()
    }
    private fun initStructure() {
        makeSelectable()
        back = TiledSurface( this, R.drawable.switch_back,
            0f, 0f, 1f, 0f)
        nub = TiledSurface(this, R.drawable.switch_front,
            if(isOn) 0.375f else -0.375f, 0f, 1f, 10f)
        setBackColor()
    }
    fun fix(isOn: Boolean) {
        this.isOn = isOn
        nub.x.set(if(isOn) 0.375f else -0.375f)
        setBackColor()
    }
    abstract fun action()
    override fun grab(posInit: Vector2) {
        didDrag = false
    }
    /** Déplacement en cours du "nub", aura besoin de letGoNub.
     * newX doit être dans le ref. du SwitchButton.
     * Retourne true si l'état à changer (i.e. action requise ?) */
    override fun drag(posNow: Vector2) {
        // 1. Ajustement de la position du nub.
        nub.x.pos = min(max(posNow.x, -0.375f), 0.375f)
        // 2. Vérifier si changement
        if(isOn != (posNow.x > 0f)) {
            isOn = posNow.x > 0f
            setBackColor()
            action()
        }
        didDrag = true
    }
    /** Ne fait que placer le nub comme il faut. (À faire après avoir dragué.) */
    override fun letGo() {
        if(!didDrag) {
            isOn = !isOn
            setBackColor()
            action()
        }
        nub.x.pos = if(isOn) 0.375f else -0.375f
        didDrag = false
    }

    private fun setBackColor() {
        if(isOn) {
            back.piu.color[0] = 0.2f; back.piu.color[1] = 1f; back.piu.color[2] = 0.5f
        } else {
            back.piu.color[0] = 1f; back.piu.color[1] = 0.3f; back.piu.color[2] = 0.1f
        }
    }
}

/** Une switch qui ne marche pas (grayed out) */
abstract class DummySwitchButton : Button {
    constructor(ref: Node?, x: Float, y: Float, height: Float, lambda: Float = 0f, flags: Long = 0L
    ) : super(ref, x, y, 1f, lambda, flags)
    {
        width.set(2f)
        scaleX.set(height)
        scaleY.set(height)
        initStructure()
    }
    protected constructor(other: DummySwitchButton) : super(other) {
        initStructure()
    }
    private fun initStructure() {
        TiledSurface( this, R.drawable.switch_back,
            0f, 0f, 1f, 0f
        ).piu.color = Color.gray2
        TiledSurface(this, R.drawable.switch_front,
            0f, 0f, 1f, 10f
        ).piu.color = Color.gray3
    }
}

/** Si actionAtLetGo, l'action n'est effectué que lorsque le bouton est lâché.
 * Sinon l'action est effectué à chaque petit changement.
 * value : valeur du slider entre 0 et 1. */
abstract class SliderButton : Node, Draggable
{
    var value: Float
        private set
    private val actionAtLetGo: Boolean
    private val slideWidth: Float
    private lateinit var nub: Surface

    constructor(ref: Node?, value: Float, actionAtLetGo: Boolean,
                            x: Float, y: Float, height: Float, slideWidth: Float,
                            lambda: Float = 0f, flags: Long = 0
    ) : super(ref, x, y, height, height, lambda, flags)
    {
        this.value = value
        this.actionAtLetGo = actionAtLetGo
        this.slideWidth = max(slideWidth, height)
        width.set(this.slideWidth + height)
        initStructure()
    }
    protected constructor(other: SliderButton) : super(other) {
        value = other.value
        actionAtLetGo = other.actionAtLetGo
        slideWidth = other.slideWidth
        initStructure()
    }
    private fun initStructure() {
        makeSelectable()
        Bar(this, Framing.inside, 0.25f * this.height.realPos, slideWidth,
            R.drawable.bar_in)
        val xPos: Float = (value - 0.5f) * slideWidth
        nub = TiledSurface(this, R.drawable.switch_front,
            xPos, 0f, this.height.realPos, 20f)
    }

    abstract fun action()

    override fun grab(posInit: Vector2) {
        // (pass)
    }

    override fun drag(posNow: Vector2) {
        // 1. Ajustement de la position du nub.
        nub.x.pos = min(max(posNow.x, -slideWidth/2), slideWidth/2)
        value = nub.x.realPos / slideWidth + 0.5f
        // 2. Action!
        if (!actionAtLetGo)
            action()
    }

    override fun letGo() {
        if(actionAtLetGo)
            action()
    }
}