package com.coq.coqlib.nodes

import com.coq.coqlib.graph.Texture
import com.coq.coqlib.graph.scheduleGL
import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.printerror
import java.util.*

/** Pour les noeuds "déplaçable".
 * 1. On prend le noeud : "grab",
 * 2. On le déplace : "drag",
 * 3. On le relâche : "letGo".
 * Un noeud Draggable doit être dans une classe descendante de SelectableNode.
 * On utilise les flags selectable et selectableRoot pour les trouver.
 * (On peut être draggable mais pas actionable, e.g. le sliding menu.) */
interface Draggable {
    fun grab(posInit: Vector2) // Les positions sont absolues (par rapport à root)
    fun drag(posNow: Vector2)
    fun letGo()
}

/** Noeud qui peut "défiler" (Sliding menu) avec une roulette par exemple. */
interface Scrollable {
    /** Scrolling with wheel.
     * Dans Android, les deltaY sont (plus ou moins) des unités,
     * e.g. +1, -1, desfois +4, -5 (scroll vite). */
    fun scroll(deltaY: Float)

    /** Scrolling with trackpad. Superflu ? */
//    fun trackpadScrollBegan()
//    fun trackpadScroll(deltaY: Float)
//    fun trackpadScrollEnded()
}

/** Noeud qui peut être "survolé" */
interface Hoverable {
    fun startHovering()
    fun stopHovering()

    companion object {
        // On ne peut hovering que s'il y a un curseur...
        // Si c'est un touch screen -> Juste afficher un framedString.
        // Voir CoqActivity onTouchEvent pour la détection du touch screen.
        var inTouchScreen = true
            internal set
    }
}

/** Cas particulier de Hoverable où un popover apparaît. */
interface HoverableWithPopover : Hoverable {
    val inFrontScreen: Boolean
    val isStatic: Boolean // Init comme dans un touch screen (non hoverable)
    val hideInTouchSreen: Boolean
    var popTimer: Timer?
    var popStringTex: Texture?
    val popFrameTex: Texture
    var framedString: FramedString? // Static, pas un popover

    override fun startHovering() {
        if(isStatic) return
        if(popTimer != null) return
        val newTimer = Timer()
        popTimer = newTimer
        newTimer.scheduleGL(350L) {
            showPopMessage()
        }
    }
    override fun stopHovering() {
        if(isStatic) return
        popTimer?.cancel()
        popTimer = null
    }
    // Crée le popover à la demande.
    private fun showPopMessage() {
        if (this !is Node) { printerror("Not a node..."); return }
        val popStringTex = popStringTex ?: return
        PopMessage.over(this, popStringTex, popFrameTex, inFrontScreen = inFrontScreen)
    }
    /// Met à jour la string à afficher dans le popover.
    fun setPopString(newStrTex: Texture?) {
        if (this !is Node) { printerror("Not a node..."); return }
        popStringTex = newStrTex

        if(!isStatic || hideInTouchSreen || newStrTex == null) {
            framedString?.disconnect()
            framedString = null
            if(containsAFlag(Flag1.show))
                showPopMessage()
            return
        }
        // Static et il faut afficher.
        framedString?.let {
            it.string.updateStringTexture(newStrTex)
            it.string.setWidth(true)
        } ?: run {
            val height = height.realPos
            framedString = FramedString(this, popFrameTex, newStrTex,
                0f, 0.5f * height, 5f*height, 0.5f*height)
        }
    }
}