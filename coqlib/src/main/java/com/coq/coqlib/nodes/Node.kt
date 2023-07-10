@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib.nodes

import com.coq.coqlib.Flagable
import com.coq.coqlib.maths.SmoothPos
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.graph.PerInstanceUniforms
import com.coq.coqlib.printdebug
import com.coq.coqlib.printerror
import kotlin.math.abs

open class Node : Flagable, Cloneable {
    /*-- Données de bases --*/
    /** Flags : Les options sur le noeud. */
    final override var flags: Long
    /** Positions, tailles, etc. */
    val x : SmoothPos
    val y : SmoothPos
    val z : SmoothPos
    val width  : SmoothPos
    val height : SmoothPos
    val scaleX : SmoothPos
    val scaleY : SmoothPos
    /** Demi espace occupé en x. (width * scaleX) / 2 */
    val deltaX: Float
        get() = width.realPos * scaleX.realPos / 2.0f
    /** Demi espace occupé en y. (height * scaleY) / 2 */
    val deltaY: Float
        get() = height.realPos * scaleY.realPos / 2.0f

    /** Données d'affichage. */
    val piu : PerInstanceUniforms

    // Liens
    var parent: Node? = null
    var firstChild: Node? = null
    var lastChild: Node? = null
    var littleBro: Node? = null
    var bigBro: Node? = null

    /** Vérifie si un noeud / branche doit être parcouru pour l'affichage.
     * Cas particulier de lecture de flags.
     * Utile car : définition différente pour les surface (actif plus longtemps, i.e. tant que visible).
     */
    open fun isDisplayActive() = containsAFlag(Flag1.show or Flag1.branchToDisplay)

    /*-- Ouverture, fermeture, reshape... --*/
    /** Open "base" ajuste la position (fading et relativeToParent) */
    open fun open() {
        if(!containsAFlag(Flag1.openFlags))
            return
        setRelatively(true)
        if(!containsAFlag(Flag1.show) && containsAFlag(Flag1.fadeInRight))
            x.fadeIn()
    }
    open fun close() {
        if(containsAFlag(Flag1.fadeInRight))
            x.fadeOut()
    }
    open fun reshape() {
        setRelatively(false)
    }

    /*-- Positions absolue et relative du noeud. --*/
    /** Obtenir la position absolue d'un noeud. (pos par rapport à root) */
    @Suppress("ControlFlowWithEmptyBody")
    fun positionAbsolute() : Vector2 {
        val sq = Squirrel(this)
        while (sq.goUpP()) {}
        return sq.v
    }
    @Suppress("ControlFlowWithEmptyBody")
    fun positionAndDeltaAbsolute() : Pair<Vector2, Vector2> {
        val sq = Squirrel(this, ScaleInit.Deltas)
        while (sq.goUpPS()) {}
        return Pair(sq.v, sq.vS)
    }
    // Obtenir la position et dimension dans le ref d'un (grand)parent.
    // On remonte jusqu'à trouver "parent".
    // (Si parent est le premier parent la position retournée est la position du noeud.)
    fun positionInParent(parent: Node) : Vector2 {
        val sq = Squirrel(this)
        do {
            if(sq.pos.parent === parent)
                return sq.v
        } while(sq.goUpP())
        printerror("No parent $parent encountered.")
        return sq.v
    }
    fun positionAndDeltaInParent(parent: Node) : Pair<Vector2, Vector2> {
        val sq = Squirrel(this, ScaleInit.Deltas)
        do {
            if(sq.pos.parent === parent)
                return Pair(sq.v, sq.vS)
        } while(sq.goUpPS())
        printerror("No parent $parent encountered.")
        return Pair(sq.v, sq.vS)
    }

    /*-- Constructeurs... --*/
    /** Noeud "vide" et "seul" (inutile) */
    /* constructor() {
        flags = 0L
        x = SmoothPos(0f)
        y = SmoothPos(0f)
        z = SmoothPos(0f)
        width = SmoothPos(4f)
        height = SmoothPos(4f)
        scaleX = SmoothPos(1f)
        scaleY = SmoothPos(1f)
        piu = PerInstanceUniforms()
    } */
    /** Constructeur standard. */
    constructor(refNode: Node?,
                x: Float, y: Float, width: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, asParent:Boolean = true, asElderBigbro: Boolean = false) {
        // 1. Set data...
        this.flags = flags
        this.x = SmoothPos(x, lambda)
        this.y = SmoothPos(y, lambda)
        this.z = SmoothPos(0f, lambda)
        this.width = SmoothPos(width, lambda)
        this.height = SmoothPos(height, lambda)
        scaleX = SmoothPos(1f, lambda)
        scaleY = SmoothPos(1f, lambda)
        piu = PerInstanceUniforms()
        // 2. Ajustement des références
        refNode?.let {
            if (asParent) {
                connectToParent(it, asElderBigbro)
            } else {
                connectToBro(it, asElderBigbro)
            }
        }
    }
    protected constructor(other: Node) {
        flags = other.flags
        x = other.x.clone()
        y = other.y.clone()
        z = other.z.clone()
        width = other.width.clone()
        height = other.height.clone()
        scaleX = other.scaleX.clone()
        scaleY = other.scaleY.clone()
        piu = PerInstanceUniforms(other.piu)
    }
    public override fun clone(): Node {
        return Node(this)
    }

    /*-----------------------------*/
    /*-- Effacement (discconnect) ---*/
    /** Se retire de sa chaine de frère et met les optionals à nil.
     *  Sera effacé par l'ARC, si n'est pas référencié (swift)
     *  ou ramassé par le Garbage Collector ? (Kotlin) */
    fun disconnect() {
        // 1. Retrait
        bigBro?.let{it.littleBro = littleBro} ?: run{parent?.firstChild = littleBro}
        littleBro?.let{it.bigBro = bigBro} ?: run{parent?.lastChild = bigBro}
        // 2. Déconnexion (superflu ?)
        parent = null
        littleBro = null
        bigBro = null
    }

    /** Deconnexion d'un descendant, i.e. Effacement direct.
     *  Retourne "true" s'il y a un descendant a effacer. */
    fun disconnectChild(elder: Boolean) : Boolean {
        if(elder) { firstChild?.disconnect() ?: return false }
        else      { lastChild?.disconnect() ?: return false }
        return true
    }
    /** Deconnexion d'un frère, i.e. Effacement direct.
     *  Retourne "true" s'il y a un frère a effacer. */
    fun disconnectBro(big: Boolean) : Boolean {
        if(big) { bigBro?.disconnect() ?: return false }
        else    { littleBro?.disconnect() ?: return false }
        return true
    }
    /** Déconnexion rapide de la branche des descendants...
     * (ne sera plus référencié -> sera ramassé par le garbage collector) */
    fun disconnectAllChildren() {
        firstChild = null
        lastChild = null
    }

    /*-- Déplacements --*/
    /** Change un frère de place dans sa liste de frère. */
    fun moveWithinBrosTo(bro: Node, asBigBro: Boolean) {
        if (bro === this) {return}
        val parent = bro.parent ?: run{ printerror("Pas de parent."); return}
        if (parent !== this.parent) {
            printerror("Parent pas commun."); return}
        // Retrait
        bigBro?.let{it.littleBro = littleBro} ?: run{parent.firstChild = littleBro}
        littleBro?.let{it.bigBro = bigBro} ?: run{parent.lastChild = bigBro}

        if (asBigBro) {
            // Insertion
            littleBro = bro
            bigBro = bro.bigBro
            // Branchement
            littleBro?.bigBro = this
            bigBro?.littleBro = this
            if (bigBro == null) {
                parent.firstChild = this
            }
        } else {
            // Insertion
            littleBro = bro.littleBro
            bigBro = bro
            // Branchement
            littleBro?.bigBro = this
            bigBro?.littleBro = this
            if (littleBro == null) {
                parent.lastChild = this
            }
        }
    }
    fun moveAsElderOrCadet(asElder: Boolean) {
        // 0. Checks
        if(asElder and (bigBro == null))
            return
        if(!asElder and (littleBro == null))
            return
        val theParent = parent ?: run{ printerror("Pas de parent."); return}
        // 1. Retrait
        bigBro?.let{it.littleBro = littleBro} ?: run{parent?.firstChild = littleBro}
        littleBro?.let{it.bigBro = bigBro} ?: run{parent?.lastChild = bigBro}
        // 2. Insertion
        if (asElder) {
            bigBro = null
            littleBro = theParent.firstChild
            // Branchement
            littleBro?.bigBro = this
            theParent.firstChild = this
        } else { // Ajout à la fin de la chaine
            littleBro = null
            bigBro = theParent.lastChild
            // Branchement
            bigBro?.littleBro = this
            theParent.lastChild = this
        }
    }
    /** Change de noeud de place (et ajuste sa position relative). */
    fun moveToBro(bro: Node, asBigBro: Boolean) {
        val newParent = bro.parent ?: run { printerror("Bro sans parent."); return}
        setInReferentialOf(newParent)
        disconnect()
        connectToBro(bro, asBigBro)
    }
    /** Change de noeud de place (sans ajuster sa position relative). */
    fun simpleMoveToBro(bro: Node, asBigBro: Boolean) {
        disconnect()
        connectToBro(bro, asBigBro)
    }
    /** Change de noeud de place (et ajuste sa position relative). */
    fun moveToParent(newParent: Node, asElder: Boolean) {
        setInReferentialOf(newParent)
        disconnect()
        connectToParent(newParent, asElder)
    }
    /** Change de noeud de place (sans ajuster sa position relative). */
    fun simpleMoveToParent(newParent: Node, asElder: Boolean) {
        disconnect()
        connectToParent(newParent, asElder)
    }
    /** "Monte" un noeud au niveau du parent. Cas particulier (simplifier) de moveTo(...).
     *  Si c'est une feuille, on ajuste width/height, sinon, on ajuste les scales. */
    fun moveUp(asBigBro: Boolean) : Boolean {
        val theParent = parent ?: run {
            printerror("Pas de parent."); return false
        }
        disconnect()
        connectToBro(theParent, asBigBro)
        x.referentialUp(theParent.x.realPos, theParent.scaleX.realPos)
        y.referentialUp(theParent.y.realPos, theParent.scaleY.realPos)
        if (firstChild == null) {
            width.referentialUpAsDelta(theParent.scaleX.realPos)
            height.referentialUpAsDelta(theParent.scaleY.realPos)
        } else {
            scaleX.referentialUpAsDelta(theParent.scaleX.realPos)
            scaleY.referentialUpAsDelta(theParent.scaleY.realPos)
        }
        return true
    }
    /** "Descend" dans le référentiel d'un frère. Cas particulier (simplifier) de moveTo(...).
     *  Si c'est une feuille, on ajuste width/height, sinon, on ajuste les scales. */
    fun moveDownIn(bro: Node, asElder: Boolean) : Boolean {
        if (bro === this) {return false}
        val oldParent = bro.parent ?: run { printerror("Manque parent."); return false}
        if (oldParent !== this.parent) {
            printerror("Parent pas commun."); return false}
        disconnect()
        connectToParent(bro, asElder)

        x.referentialDown(bro.x.realPos, bro.scaleX.realPos)
        y.referentialDown(bro.y.realPos, bro.scaleY.realPos)

        if (firstChild == null) {
            width.referentialDownAsDelta(bro.scaleX.realPos)
            height.referentialDownAsDelta(bro.scaleY.realPos)
        } else {
            scaleX.referentialDownAsDelta(bro.scaleX.realPos)
            scaleY.referentialDownAsDelta(bro.scaleY.realPos)
        }
        return true
    }
    /** Échange de place avec "node". */
    fun permuteWith(node: Node) {
        val oldParent = parent ?: run { printerror("Manque le parent."); return}
        if (node.parent === null) {
            printerror("Manque parent 2."); return}

        if (oldParent.firstChild === this) { // Cas ainé...
            moveToBro(node, true)
            node.moveToParent(oldParent, true)
        } else {
            val theBigBro = bigBro ?: run { printerror("Pas de bigBro."); return}
            moveToBro(node, true)
            node.moveToBro(theBigBro, false)
        }
    }

    /*-- Private stuff... --*/
    /** Connect au parent. (Doit être fullyDeconnect -> optionals à nil.) */
    private fun connectToParent(parent: Node, asElder: Boolean) {
        // Dans tout les cas, on a le parent:
        this.parent = parent
        // Cas parent pas d'enfants
        if (parent.firstChild == null) {
            parent.firstChild = this
            parent.lastChild = this
            return
        }
        // Ajout au début
        if (asElder) {
            // Insertion
            this.littleBro = parent.firstChild
            // Branchement
            parent.firstChild?.bigBro = this
            parent.firstChild = this
        } else { // Ajout à la fin de la chaine
            // Insertion
            this.bigBro = parent.lastChild
            // Branchement
            parent.lastChild?.littleBro = this
            parent.lastChild = this
        }
    }
    private fun connectToBro(bro: Node, asBigBro: Boolean) {
        if (bro.parent == null) {println("Boucle sans parents")}
        parent = bro.parent
        if (asBigBro) {
            // Insertion
            littleBro = bro
            bigBro = bro.bigBro
            // Branchement
            bro.bigBro = this // littleBro.bigBro = this
            if (bigBro != null) {
                bigBro?.littleBro = this
            } else {
                parent?.firstChild = this
            }
        } else {
            // Insertion
            littleBro = bro.littleBro
            bigBro = bro
            // Branchement
            bro.littleBro = this // bigBro.littleBro = this
            if (littleBro != null) {
                littleBro?.bigBro = this
            } else {
                parent?.lastChild = this
            }
        }
    }
    /** Change le référentiel. Pour moveTo de node. */
    @Suppress("ControlFlowWithEmptyBody")
    private fun setInReferentialOf(node: Node) {
        val sqP = Squirrel(this, ScaleInit.Ones)
        while (sqP.goUpPS()) {}
        val sqQ = Squirrel(node, ScaleInit.Scales)
        while (sqQ.goUpPS()) {}

        x.newReferential(sqP.v.x, sqQ.v.x, sqP.sx, sqQ.sx)
        y.newReferential(sqP.v.y, sqQ.v.y, sqP.sy, sqQ.sy)

        if (firstChild != null) {
            scaleX.newReferentialAsDelta(sqP.sx, sqQ.sx)
            scaleY.newReferentialAsDelta(sqP.sy, sqQ.sy)
        } else {
            width.newReferentialAsDelta(sqP.sx, sqQ.sx)
            height.newReferentialAsDelta(sqP.sy, sqQ.sy)
        }
    }

    companion object {
        var showFrame = false
    }
}

/** Convertie une position absolue en une position dans le référentiel du noeud node. */
@Suppress("ControlFlowWithEmptyBody")
fun Vector2.inReferentialOf(node: Node?) // , asDelta: Boolean=false)
: Vector2 {
    val sq = Squirrel(
        node ?: return this,
        ScaleInit.Scales
    )
    while (sq.goUpPS()) {}
    // Maintenant, sq contient la position absolue de theNode.
    return this.inReferentialOf(sq)
//    return if(asDelta) sq.getRelDeltaOf(this) else sq.getRelPosOf(this)
}

/*-- Ajustements de position/taille --*/
fun Node.adjustWidthAndHeightFromChildren() {
    var w = 0f
    var h = 0f
    var htmp: Float
    var wtmp: Float
    val sq = Squirrel(this)
    if(!sq.goDownWithout(Flag1.hidden)) { return}
    do {
        htmp = (sq.pos.deltaY + abs(sq.pos.y.realPos)) * 2f
        if (htmp > h)
            h = htmp
        wtmp = (sq.pos.deltaX + abs(sq.pos.x.realPos)) * 2f
        if (wtmp > w)
            w = wtmp
    } while (sq.goRightWithout(Flag1.hidden))
    width.set(w)
    height.set(h)
}

fun Node.setRelatively(fix: Boolean) {
    if(!containsAFlag(Flag1.relativeFlags)) return
    val theParent = parent ?: return
    var xDec = 0.0f
    var yDec = 0.0f
    if(containsAFlag(Flag1.relativeToRight))
        xDec = theParent.width.realPos * 0.5f
    else if(containsAFlag(Flag1.relativeToLeft))
        xDec = -theParent.width.realPos * 0.5f
    if(containsAFlag(Flag1.relativeToTop))
        yDec = theParent.height.realPos * 0.5f
    else if(containsAFlag(Flag1.relativeToBottom))
        yDec = -theParent.height.realPos * 0.5f
    if(containsAFlag(Flag1.justifiedRight))
        xDec -= deltaX
    else if(containsAFlag(Flag1.justifiedLeft)) {
        xDec += deltaX
    }
    if(containsAFlag(Flag1.justifiedTop))
        yDec -= deltaY
    else if(containsAFlag(Flag1.justifiedBottom))
        yDec += deltaY
    x.setRelToDef(xDec, fix)
    y.setRelToDef(yDec, fix)
}

/** Aligner les descendants d'un noeud. Retourne le nombre de descendants traités. */
fun Node.alignTheChildren(alignOpt: Int, ratio: Float = 1f, spacingRef: Float = 1f) : Int {
    var sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden or Flag1.notToAlign)) {
        printerror("pas de child.");return 0}
    // 0. Les options...
    val fix = (alignOpt and AlignOpt.fixPos != 0)
    val horizontal = (alignOpt and AlignOpt.vertically == 0)
    val setAsDef = (alignOpt and AlignOpt.setAsDefPos != 0)
    val setSecondaryToDefPos = (alignOpt and AlignOpt.setSecondaryToDefPos != 0)
    // 1. Setter largeur/hauteur
    var w = 0f
    var h = 0f
    var n = 0
    if (horizontal) {
        do {
            w += sq.pos.deltaX * 2f * spacingRef
            n += 1
            if (sq.pos.deltaY*2f > h) {
                h = sq.pos.deltaY*2f
            }
        } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))
    }
    else {
        do {
            h += sq.pos.deltaY * 2f * spacingRef
            n += 1
            if (sq.pos.deltaX*2f > w) {
                w = sq.pos.deltaX*2f
            }
        } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))
    }
    // 2. Ajuster l'espacement supplémentaire pour respecter le ratio
    var spacing = 0f
    if (alignOpt and AlignOpt.respectRatio != 0) {
        if(horizontal) {
            if  (w/h < ratio) {
                spacing = (ratio * h - w) / n.toFloat()
                w = ratio * h
            }
        } else {
            if (w/h > ratio) {
                spacing = (w/ratio - h) / n.toFloat()
                h = w / ratio
            }
        }
    }
    // 3. Setter les dims.
    if (alignOpt and AlignOpt.dontUpdateSizes == 0) {
        width.set(w, fix, setAsDef)
        height.set(h, fix, setAsDef)
    }
    // 4. Aligner les éléments
    sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden or Flag1.notToAlign)) {
        printerror("pas de child2.");return 0}
    // 4.1 Placement horizontal
    if(horizontal) {
        var x = - w / 2f
        do {
            x += sq.pos.deltaX * spacingRef + spacing/2f

            sq.pos.x.set(x, fix, setAsDef)
            if (setSecondaryToDefPos) {
                sq.pos.y.setRelToDef(0f, fix)
            } else {
                sq.pos.y.set(0f, fix, false)
            }

            x += sq.pos.deltaX * spacingRef + spacing/2f
        } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))
        return n
    }
    // 4.2 Placement vertical
    var y =  h / 2f
    do {
        y -= sq.pos.deltaY * spacingRef + spacing/2f

        sq.pos.y.set(y, fix, setAsDef)
        if (setSecondaryToDefPos) {
            sq.pos.x.setRelToDef(0f, fix)
        } else {
            sq.pos.x.set(0f, fix, false)
        }

        y -= sq.pos.deltaY * spacingRef + spacing/2f
    } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))

    return n
}

object AlignOpt {
    const val vertically = 1
    const val dontUpdateSizes = 2
    const val respectRatio = 4
    const val fixPos = 8
    /** En horizontal, le "primary" est "x" des children,
     * le "secondary" est "y". (En vertical prim->"y", sec->"x".)
     * Place la position "alignée" comme étant la position par défaut pour le primary des children
     * et pour le width/height du parent. Ne touche pas à defPos du secondary des children. */
    const val setAsDefPos = 16
    /** S'il y a "setSecondaryToDefPos", on place "y" à sa position par défaut,
     * sinon, on le place à zéro. */
    const val setSecondaryToDefPos = 32
}