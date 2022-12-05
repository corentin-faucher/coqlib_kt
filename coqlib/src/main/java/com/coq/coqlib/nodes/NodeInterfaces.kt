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
    fun grab(posInit: Vector2)
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
        internal var inTouchScreen = true
    }
}

/** Cas particulier de Hoverable où un popover apparaît. */
interface HoverableWithPopover : Hoverable {
    val inFrontScreen: Boolean
    var popTimer: Timer?
    var popStringTex: Texture?
    val popFrameTex: Texture
    var framedString: FramedString?

    override fun startHovering() {
        // (Si hovering -> isStatic = false)
        if(popTimer != null) return
        if(framedString != null) return
        val newTimer = Timer()
        popTimer = newTimer
        newTimer.scheduleGL(350L) {
            showPopMessage()
        }
    }
    override fun stopHovering() {
        popTimer?.cancel()
        popTimer = null
    }
    // Crée un popover
    private fun showPopMessage() {
        if (this !is Node) { printerror("Not a node..."); return }
        // (pas de pop over si dans un touch screen ou pas de popString)
        if(Hoverable.inTouchScreen) return
        val popStringTex = popStringTex ?: return
        PopMessage.over(this, popStringTex, popFrameTex, inFrontScreen = inFrontScreen)
    }
    /// Met à jour la string à afficher dans le popover. En mode inTouchScreen, crée un framed string "static".
    fun setPopString(newStrTex: Texture?) {
        if (this !is Node) { printerror("Not a node..."); return }
        popStringTex = newStrTex
        if(!Hoverable.inTouchScreen || newStrTex == null) {
            framedString?.disconnect()
            framedString = null
            return
        }
        // Touch Screen... Créer/updater la framedString (pas un popover)
        framedString?.string?.updateStringTexture(newStrTex) ?:
        run {
            val height = height.realPos
            framedString = FramedString(this, popFrameTex, newStrTex,
                0f, 0.5f * height, 5f*height, 0.5f*height)
        }
    }
}