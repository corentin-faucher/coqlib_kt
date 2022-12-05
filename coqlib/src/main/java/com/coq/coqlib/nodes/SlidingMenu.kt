@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib.nodes

import com.coq.coqlib.*
import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleGL
import com.coq.coqlib.maths.SmoothPos
import com.coq.coqlib.maths.Vector2
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


/** Menu déroulant: root->menu->(item1, item2,... )
 * Vide au départ, doit être rempli quand on veut l'afficher.
 * if(spacing < 1) -> Recouvrement, if(spacing > 1) -> espacement.
 * addNewItem : Typiquement un constructeur de noeud-bouton.
 * checkItem : Methode/ext de noeud pour mettre à jour les noeud-boutons.
 * getIndicesRangeAtOpening : exécuter à l'ouverture du sliding menu et retourne le range attendu des items.
 * getPosIndex : la position de l'indice où on est centré à l'ouverture. */
class SlidingMenu : Node, Draggable, Scrollable
{
    /*-- fields --*/
    var openIndex: Int = 0 // Seul variable publique : l'id de l'item où on doit être à l'ouverture.

    private val displayedCount: Int // Nombre d'items affichés (sans dérouler), e.g. 4.
    private var totalCount: Int = 0 // Nombre total d'items dans le menu, e.g. 10 -> il faut dérouler...
    private val spacing: Float      // Espacement vertical relatif entre item, e.g. ~1.
    private lateinit var menu: Node // Le menu qui "glisse" sur le noeud racine
                                    //   (grand rectangle avec les totalCount items).
    private lateinit var scrollBar: SlidingMenuScrollBar
    private lateinit var back: Frame
    /*-- Computed properties --*/
    /** Largeur relative d'un item dans le menu (i.e. w/h ratio d'un item) */
    val itemRelativeWidth: Float
        get() = menu.width.realPos / height.realPos * displayedCount.toFloat() * spacing
    private val itemHeight: Float
        get() = height.realPos / displayedCount
    private val menuMaxDeltaY : Float?
        get() = if (totalCount <= displayedCount) null
            else 0.5f * itemHeight * (totalCount - displayedCount).toFloat()

    /*-- Init --*/
    constructor(ref: Node?, displayedCount: Int,
                x: Float, y: Float, width: Float, height: Float,
                spacing: Float, flags: Long
    ) : super(ref, x, y, width, height, 10f, flags or Flag1.selectableRoot)
    {
        this.displayedCount = displayedCount
        this.spacing = spacing
        addStructure()
    }
    private constructor(other: SlidingMenu) : super(other)
    {
        displayedCount = other.displayedCount
        spacing = other.spacing
        addStructure()
    }
    override fun clone() = SlidingMenu(this)
    private fun addStructure() {
        makeSelectable()
        val w = width.realPos
        val h = height.realPos
        val scrollBarWidth = max(w, h) * 0.025f
        back = Frame(this, Framing.inside, scrollBarWidth, R.drawable.sliding_menu_back)
        menu = Node(this, -0.5f*scrollBarWidth, 0f,
            w - scrollBarWidth, h, 20f, Flag1.selectableRoot)
        scrollBar = SlidingMenuScrollBar(this, scrollBarWidth)
    }
    /*-- Edition du SlidingMenu. Après modification des items, il faut recaller "open". --*/
    fun addItem(node: Node) {
        node.simpleMoveToParent(menu, false)
        totalCount += 1
    }
    fun removeAllItems() {
        while (menu.firstChild != null) {
            menu.firstChild?.disconnect()
        }
        totalCount = 0
    }
    /*-- Open : normalise et aligne les noeud du menu. --*/
    override fun open() {
        // Mettre tout de suite le flag "show".
        if(!menu.containsAFlag(Flag1.hidden))
            menu.addFlags(Flag1.show)
        flingChrono.stop()
        deltaT.stop()
        // 1. Ajuster la scroll bar
        val nubRelativeHeight = displayedCount.toFloat() / max(1f, totalCount.toFloat())
        if (scrollBar.setNubWith(nubRelativeHeight)) {
            back.removeFlags(Flag1.hidden)
        } else {
            back.addFlags(Flag1.hidden)
        }
        // 2. Normaliser les hauteurs des items
        val sq = Squirrel(menu.firstChild ?: return)
        val smallItemHeight = itemHeight / spacing
        do {
            // Scaling -> taille attendu / taille actuelle
            val scale = smallItemHeight / sq.pos.height.realPos
            sq.pos.scaleX.set(scale)
            sq.pos.scaleY.set(scale)
        } while (sq.goRight())
        // 3. Aligner/placer les éléments
        menu.alignTheChildren(AlignOpt.vertically or AlignOpt.fixPos, 1f, spacing)
        // (On remet la largeur à celle du sliding menu, pas la width max des éléments.)
        menu.width.set(menu.width.defPos)
        menuMaxDeltaY?.let { deltaY ->
            setMenuYPos(itemHeight * openIndex.toFloat() - deltaY, snap = true, fix = true)
        } ?: run {
            setMenuYPos(0f, snap = true, fix = true)
        }
        checkItemsVisibility(false)
        // 4. Open par défaut (relative pos...)
        super.open()
    }
    /*-- Scrollable --*/
    override fun scroll(deltaY: Float) {
        // Simple modification binaire pour l'instant...
        setMenuYPos(menu.y.realPos + if(deltaY > 0f) -itemHeight else itemHeight,
            snap = true, fix = false)
        checkItemsVisibility(true)
    }
    /*-- Draggable --*/
    // Sauvegarde des positions et vitesses...
    private var yInitToMenu: Float = 0f
    private var y0: Float = 0f
    private var y1: Float = 0f
    private var yp0: Float = 0f
    private var yp1: Float = 0f
    private val ypSmooth = SmoothPos(0f, 4f) // La vitesse lors du "fling"
    private val deltaT = Chrono()      // Delta t pour estimer la vitesse.
    private val flingChrono = Chrono() // Temps de "vol"
    override fun grab(posInit: Vector2) {
        flingChrono.stop()
        y0 = posInit.y
        y1 = posInit.y
        yp0 = 0f
        yp1 = 0f
        deltaT.start()
        yInitToMenu = y1 - menu.y.realPos
    }
    /** Scrolling vertical de menu. (déplacement en cours) */
    override fun drag(posNow: Vector2) {
        if (deltaT.elapsedMS < 10L) return
        // Estimation de la vitesse
        y0 = y1                     // Last y position.
        y1 = posNow.y               // New y position.
        yp0 = yp1                   // Last y speed
        yp1 = (y1 - y0) / deltaT.elapsedSec  // New speed.
        deltaT.start()              // (Remettre le chrono à zéro.)
        // Mise à jour de la position du menu
        val newMenuY = y1 - yInitToMenu
        setMenuYPos(newMenuY, snap = false, fix = false)
        checkItemsVisibility(true)
    }
    override fun letGo() {
        // 1. Estimé de la vitesse (moyenne des deux dernière)
        val yp = (yp1 + yp0) / 2f
        // 2. Cas stop (bouge pas très vite). Pas de "fling", snap ici.
        if (abs(yp) < 1) {
            setMenuYPos(menu.y.realPos, snap = true, fix = false)
            checkItemsVisibility(true)
            return
        }
        // 3. Cas on laisse en "fling"
        ypSmooth.set(yp, fix = true, setAsDef = false)
        flingChrono.start()
        deltaT.start()
        checkFling()
    }
    private fun checkFling() {
        if (!flingChrono.isActive) return
        // 1. Ralentir, arrêter ?
        if (flingChrono.elapsedMS > 100L) {
            // On arrête le "fling" après une seconde...
            if (flingChrono.elapsedMS > 1000L) {
                flingChrono.stop()
                deltaT.stop()
                setMenuYPos(menu.y.realPos, snap = true, fix = false)
                return
            }
            // Sinon, ralentir.
            ypSmooth.pos = 0f
        }
        // 2. Mettre à jour la position
        if (deltaT.elapsedMS > 30L) {
            val deltaY =  deltaT.elapsedSec * ypSmooth.pos
            setMenuYPos(menu.y.realPos + deltaY,
                snap = false, fix = false)
            deltaT.start()
        }
        // 3. Vérifier la visibilité des éléments.
        checkItemsVisibility(true)
        // 4. Répéter plus tard...
        Timer().scheduleGL(40L) {
            checkFling()
        }
    }
    /** Mise à jour des items à montrer... */
    private fun checkItemsVisibility(openNode: Boolean) {
        // 0. Sortir s'il n'y a rien.
        val sq = Squirrel(menu)
        if(!sq.goDown() || !menu.containsAFlag(Flag1.show)) {
            flingChrono.stop()
            deltaT.stop()
            return
        }
        // 1. Ajuster la visibilité des items
        val yActual = menu.y.realPos // TODO: Toujours realPos ??
        do {
            val toShow = abs(yActual + sq.pos.y.realPos) < 0.5f * height.realPos

            if (toShow && sq.pos.containsAFlag(Flag1.hidden)) {
                sq.pos.removeFlags(Flag1.hidden)
                if(openNode) {
                    sq.pos.openAndShowBranch()
                }
            }
            if (!toShow && !sq.pos.containsAFlag(Flag1.hidden)) {
                sq.pos.addFlags(Flag1.hidden)
                if(openNode) {
                    sq.pos.closeBranch()
                }
            }
        } while (sq.goRight())
    }
    /** Ajuste la position de menu et vérifie les contraintes (snap, max/min). */
    private fun setMenuYPos(newY: Float, snap: Boolean, fix: Boolean) {
        val deltaY = menuMaxDeltaY ?: run {
            menu.y.set(0f)
            return
        }
        val menuY = if (snap) round((newY - deltaY) / itemHeight) * itemHeight + deltaY
            else newY
        menu.y.set(max(min(menuY, deltaY), -deltaY), fix, setAsDef = false)
        scrollBar.setNubRelY(menu.y.realPos / deltaY)
    }
}

private class SlidingMenuScrollBar : Node {
    private lateinit var nub: Node
    private lateinit var nubTop: TiledSurface
    private lateinit var nubMid: TiledSurface
    private lateinit var nubBot: TiledSurface
    constructor(parent: SlidingMenu, width: Float
    ) : super(parent, parent.width.realPos/2f - width/2f, 0f, width, parent.height.realPos)
    {
        addStructure()
    }
    private constructor(other: SlidingMenuScrollBar) : super(other) {
        addStructure()
    }
    override fun clone() = SlidingMenuScrollBar(this)
    private fun addStructure() {
        val width = this.width.realPos
        val parHeight: Float = parent?.height?.realPos ?: run {
            printerror("No parent.")
            1f
        }
        val backTex = Texture.getPng(R.drawable.scroll_bar_back)
        val frontTex = Texture.getPng(R.drawable.scroll_bar_front)
        // Back of scrollBar
        TiledSurface(this, backTex,
            0f, parHeight/2f - width/2f, width, 0f)
        TiledSurface(this, backTex,
            0f, 0f, width, parHeight - 2f*width, 0f, 1)
        TiledSurface(this, backTex,
            0f, -parHeight/2f + width/2f, width, 0f, 2)
        // Nub (sliding)
        nub = Node(this, 0f, parHeight/4, width, width*3f, 30f)
        nubTop = TiledSurface(nub, frontTex,
            0f, width, width, 0f)
        nubMid = TiledSurface(nub, frontTex,
            0f, 0f, width, 0f, 1)
        nubBot = TiledSurface(nub, frontTex,
            0f, -width, width, 0f, 2)
    }

    // Retourne true s'il y a un scroll bar à afficher.
    fun setNubWith(relativeHeight: Float) : Boolean {
        if (relativeHeight >= 1 || relativeHeight <= 0) {
            addFlags(Flag1.hidden)
            closeBranch()
            return false
        }
        removeFlags(Flag1.hidden)
        val w = width.realPos
        val heightTmp = height.realPos * relativeHeight
        val heightMid = max(0f, heightTmp - 2f * w)
        nub.height.set(heightMid + 2f * w)
        nubTop.y.set((heightMid + w)/2f)
        nubBot.y.set(-(heightMid + w)/2f)
        nubMid.height.set(heightMid)
        return true
    }
    fun setNubRelY(newRelY: Float) {
        val deltaY = (height.realPos - nub.height.realPos)/2f
        nub.y.pos = -newRelY * deltaY
        val nubRelY = nub.y.realPos / height.realPos
    }
}


/* GARBAGE

    override fun trackpadScrollBegan() {
        flingChrono.stop()
        vitYm1 = 0f
        vitY.set(0f)
        deltaT.start()
    }

    override fun trackpadScroll(deltaY: Float) {
        val menuDeltaY = -0.015f * deltaY
        setMenuYpos(menu.y.realPos + menuDeltaY, snap = true, fix = false)
        checkItemsVisibility(true)
        if(deltaT.elapsedSec > 0f) {
            vitYm1 = vitY.realPos
            vitY.set(menuDeltaY / deltaT.elapsedSec)
        }
        deltaT.start()
    }

    override fun trackpadScrollEnded() {
        vitY.set((vitY.realPos + vitYm1)/2)
        if (abs(vitY.realPos) < 6f) {
            setMenuYpos(menu.y.realPos, snap = true, fix = false)
            return
        }
        flingChrono.start()
        deltaT.start()

        checkFling()
    }

     */