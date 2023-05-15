@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib.nodes

import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.printerror
import kotlin.math.abs

enum class ScaleInit {Ones, Scales, Deltas}

class Squirrel(pos: Node) {
    /*-- Données de bases et computed properties --*/
    /** Position dans l'arbre (noeud) de l'écureuil. */
    var pos: Node
        private set
    private var root: Node
    var x: Float
        private set
    var y: Float
        private set
    val v: Vector2
        get() = Vector2(x, y)
    var sx: Float = 0.0f
        private set
    var sy: Float = 0.0f
        private set
    val vS: Vector2
        get() = Vector2(sx, sy)
    /** Vérifie si on tombe dans le cadre du noeud présent (pos). */
    val isIn: Boolean
        get() = (abs(x - pos.x.realPos) <= pos.deltaX) &&
                (abs(y - pos.y.realPos) <= pos.deltaY)

    /*-- Constructeurs... --*/
    init {
        this.pos = pos
        root = pos
        x = pos.x.realPos;  y = pos.y.realPos
        sx = 1.0f;          sy = 1.0f
    }
    constructor(pos: Node, scaleInit: ScaleInit) : this(pos) {
        when(scaleInit) {
            ScaleInit.Ones -> {sx = 1.0f; sy = 1.0f}
            ScaleInit.Scales -> {
                sx = pos.scaleX.realPos; sy = pos.scaleY.realPos
            }
            ScaleInit.Deltas -> {
                sx = pos.deltaX; sy = pos.deltaY
            }
        }
    }
    /// Initialise avec une position relative au lieu de la position du noeud.
    /// La postion relative est dans le reférentiel de pos.parent (comme l'est la position du noeud).
    constructor(pos: Node, relPos: Vector2,
                scaleInit: ScaleInit
    ) : this(pos) {
        x = relPos.x; y = relPos.y
        when(scaleInit) {
            ScaleInit.Ones -> {sx = 1.0f; sy = 1.0f}
            ScaleInit.Scales -> {
                sx = pos.scaleX.realPos; sy = pos.scaleY.realPos
            }
            ScaleInit.Deltas -> {
                sx = pos.deltaX; sy = pos.deltaY
            }
        }
    }
    fun placeAt(pos: Node) {
        this.pos = pos
        root = pos
        x = pos.x.realPos;  y = pos.y.realPos
        sx = 1.0f;          sy = 1.0f
    }

    /*--- Déplacements ----*/
    /** Déconnecte où on est et va au petit frère (par défaut).
     * Si ne peut aller au frère, va au parent.
     * Retourne true s'il y a un frère où aller, false si on doit aller au parent.
     * Throw une erreur si ne peut aller au parent. */
    fun disconnectAndGoToBroOrUp(little: Boolean = true) : Boolean {
        (if(little) pos.littleBro else pos.bigBro)?.let { bro ->
            val toDelete = pos
            pos = bro
            toDelete.disconnect()
            return true
        }
        pos.parent?.let {
            val toDelete = pos
            pos = it
            toDelete.disconnect()
            return false
        }
        throw Exception("Ne peut deconnecter, nul part où aller.")
    }
    /** Va au petit-frère. S'arrête au cadet (et retourne false). */
    fun goRight() : Boolean {
        pos.littleBro?.let {
            pos = it; return true}
        return false
    }
    /** Va au petit-frère. S'il n'existe pas, on le crée. */
    fun goRightForced(copyRef: Node) {
        pos = pos.littleBro ?: copyRef.clone().also { copy ->
            copy.simpleMoveToBro(pos, false)
        }
    }
    /** Revient à l'ainé si arrive en bout de liste (retourne false si ne peut pas y aller, i.e. pas de parent). */
    fun goRightLoop() : Boolean {
        pos.littleBro?.let{pos = it; return true}
        pos.parent?.firstChild?.let{pos = it; return true}
        return false
    }
    /** Tant que l'on est sur un noeud caché, on bouge vers la droite. Retourne false si abouti en bout de liste.
     * Se déplace au moins une fois. */
    fun goRightWithout(flag: Long) : Boolean {
        do {
            if(!goRight()) {return false}
        } while(pos.containsAFlag(flag))
        return true
    }
    /** Va au petit-frère non-caché et du type voulu. */
    /* Pas besoin ?
    inline fun <reified A: Node> goRightWithoutTyped(flag: Long) : Boolean {
        do {
            if(!goRight()) {return false}
        } while((pos !is A) or pos.containsAFlag(flag))
        return true
    }
     */
    /** Va au grand-frère. S'arrête et retourne false si ne peut y aller (dépassé l'ainé) */
    fun goLeft() : Boolean {
        pos.bigBro?.let { pos = it; return true }
        return false
    }
    /** Va au grand-frère. S'il n'existe pas, on le crée. */
    fun goLeftForced(copyRef: Node) {
        pos = pos.bigBro ?: copyRef.clone().also { copy ->
            copy.simpleMoveToBro(pos, true)
        }
    }
    fun goLeftWithout(flag: Long) : Boolean {
        do {
            if(!goLeft()) {return false}
        } while(pos.containsAFlag(flag))
        return true
    }
    /** Va au firstChild. Retourne false si pas de descendants. */
    fun goDown() : Boolean {
        pos.firstChild?.let {pos = it; return true}
        return false
    }
    /** Va au firstChild. S'il n'existe pas on le crée. */
    fun goDownForced(copyRef: Node) {
        pos = pos.firstChild ?: copyRef.clone().also { copy ->
            copy.simpleMoveToParent(pos, true)
        }
    }
    /** Va au lastChild. Retourne false si pas de descendants. */
    fun goDownLast() : Boolean {
        pos.lastChild?.let { pos = it; return true }
        return false
    }
    fun goDownWithout(flag: Long) : Boolean {
        pos.firstChild?.let {
            pos = it
            while(pos.containsAFlag(flag)) {
                if(!goRight()) {return false}
            }
            return true
        }
        return false
    }
    fun goDownLastWithout(flag: Long) : Boolean {
        pos.lastChild?.let {
            pos = it
            while(pos.containsAFlag(flag)) {
                if(!goLeft()) {return false}
            }
            return true
        }
        return false
    }
    fun goDownP() : Boolean {
        pos.firstChild?.let {
            x = (x - pos.x.realPos) / pos.scaleX.realPos
            y = (y - pos.y.realPos) / pos.scaleY.realPos
            pos = it
            return true
        }
        return false
    }
    fun goDownPS() : Boolean {
        pos.firstChild?.let {
            x = (x - pos.x.realPos) / pos.scaleX.realPos
            y = (y - pos.y.realPos) / pos.scaleY.realPos
            sx /= pos.scaleX.realPos
            sy /= pos.scaleY.realPos
            pos = it
            return true
        }
        return false
    }
    fun goUp() : Boolean {
        pos.parent?.let { pos = it; return true }
        return false
    }
    fun goUpP() : Boolean {
        pos.parent?.let {
            pos = it
            x = x * pos.scaleX.realPos + pos.x.realPos
            y = y * pos.scaleY.realPos + pos.y.realPos
            return true
        }
        return false
    }
    fun goUpPS() : Boolean {
        pos.parent?.let {
            pos = it
            x = x * pos.scaleX.realPos + pos.x.realPos
            y = y * pos.scaleY.realPos + pos.y.realPos
            sx *= pos.scaleX.realPos
            sy *= pos.scaleY.realPos
            return true
        }
        return false
    }
    /** Recherche en profondeur d'abord (deep first search)
     * Retourne true si on trouve un noeud non visité. */
    fun goToNextNode() : Boolean {
        if (goDown()) {return true}
        if (pos === root) {return false}
        while (!goRight()) {
            // Remonter...
            if (!goUp()) {
                printerror("Pas de root.")
                return false
            } else if (pos === root) {
                return false
            }
        }
        return true
    }

    /** Cas particulier de déplacement pour l'affichage. */
    fun goToNextToDisplay() : Boolean {
        // 1. Aller en profondeur, pause si branche à afficher.
        if (pos.firstChild != null && pos.containsAFlag(Flag1.show or Flag1.branchToDisplay)) {
            pos.removeFlags(Flag1.branchToDisplay)
            goDown()
            return true
        }
        // 2. Redirection (voisin, parent).
        do {
            // Si le noeud présent est encore actif -> le parent doit l'être aussi.
            if (pos.isDisplayActive()) {
                pos.parent?.addFlags(Flag1.branchToDisplay)
            }
            if (goRight()) {return true}
        } while (goUp())
        return false
    }
}

fun Vector2.inReferentialOf(sq: Squirrel) : Vector2 =
    Vector2((x - sq.x) / sq.sx, (y - sq.y) / sq.sy)
