@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.coqlib.nodes

import com.coq.coqlib.KeyboardInput
import com.coq.coqlib.printerror
import kotlin.math.min

/**-- Interfaces pour les screens... --*/

interface Escapable {
    fun escapeAction()
}

interface Enterable {
    fun enterAction()
}

interface KeyResponder {
    fun keyDown(key: KeyboardInput)
    fun keyUp(key: KeyboardInput)
    fun modifiersChangedTo(newModifiers: Int)
}

/** Modèle pour les noeuds racine d'un screen.
 * escapeAction: l'action dans cet écran quand on appuie "escape" (e.g. aller au "main" menu).
 * enterAction: l'action quand on tape "enter". */
abstract class Screen : Node
{
    var compactAlign: Boolean = false
    var landscapePortraitThreshold: Float = 1f

    /** Les écrans sont toujours ajoutés juste après l'ainé.
     * add f (front) : 0->f,  add b (back) : 0->{b, f},  add 1 : 0->{b, 1, f},  add 2 : 0->{b, 1, 2, f}, ...
     * i.e. les deux premiers écrans sont le front et le back respectivement,
     * les autres sont au milieu. */
    constructor(root: AppRootBase
    ) : super(null, 0f, 0f, 4f, 4f, 0f, Flag1.reshapeableRoot)
    {
        (root.firstChild as? Screen)?.let { elder ->
            // Si l'ainé est le "front", on place cette nouvelle fenêtre en ainé.
            // Sinon, comme deuxième.
            simpleMoveToBro(elder, elder is AppRootBase.FrontScreen)
        } ?: run {
            simpleMoveToParent(root, false)
        }
    }
    protected constructor(other: Screen) : super(other) {
        compactAlign = other.compactAlign
        landscapePortraitThreshold = other.landscapePortraitThreshold
    }

    fun getScreenRatio() : Float {
        val parent = parent ?: run { printerror("No parent."); return 1f }
        return parent.width.realPos / parent.height.realPos
    }

    override fun open() {
        alignScreenElements(true)
    }
    override fun reshape() {
        alignScreenElements(false)
    }
    /** En général un écran est constitué de deux "blocs"
     * alignés horizontalement ou verticalement en fonction de l'orientation de l'appareil. */
    private fun alignScreenElements(isOpening:  Boolean) {
        val theParent = parent ?: run { printerror("Pas de parent."); return}
        // 1. Pas d'alignement, juste ajuster la taille avec la root.
        if (containsAFlag(Flag1.dontAlignScreenElements)) {
            scaleX.set(1f, isOpening)
            scaleY.set(1f, isOpening)
            width.set(theParent.width.realPos, isOpening)
            height.set(theParent.height.realPos, isOpening)
            return
        }
        // 2. Aligner les élements (horizontalement ou verticalement)
        val screenRatio = getScreenRatio()
        var alignOpt = AlignOpt.setSecondaryToDefPos
        if(!compactAlign)
            alignOpt = alignOpt or AlignOpt.respectRatio
        if (screenRatio < landscapePortraitThreshold)
            alignOpt = alignOpt or AlignOpt.vertically
        if (isOpening)
            alignOpt = alignOpt or AlignOpt.fixPos
        this.alignTheChildren(alignOpt, screenRatio)
        val scale = min(
            theParent.width.realPos/ width.realPos,
            theParent.height.realPos / height.realPos)
        scaleX.set(scale, isOpening)
        scaleY.set(scale, isOpening)
    }
}
