@file:Suppress("unused")

package com.coq.coqlib.nodes

/*----------------------------------------------*/
/*-- Les flags de base pour l'état d'un noeud.--*/
/*----------------------------------------------*/

/** Les flags "de base" pour les noeuds. */
object Flag1 {
    const val show = 1L
    /** Noeud qui n'apparaît pas quand on "openAndShowBranch" (voir SquirrelJob.kt) */
    const val hidden = 2L
    /** Noeud qui ne disparaît pas quand on "close" */
    const val exposed = 4L
    /** Pour les noeuds qui peuvent être sélectionnés (e.g. les boutons) */
    const val selectableRoot = 1L.shl(3)
    const val selectable = 1L.shl(4)
    /** Noeud qui apparaît en grossissant. */
    const val popping = 1L.shl(5)
    const val isRoot = 1L.shl(6)

    /*-- Pour les surfaces --*/
    /** Par défaut on ajuste la largeur pour respecter les proportion d'une image. */
    const val surfaceDontRespectRatio = 1L.shl(7)
    const val stringWithCeiledWidth = 1L.shl(8)
    /*-- Pour les ajustement de height/width du parent ou du frame --*/
    const val giveSizesToBigBroFrame = 1L.shl(9)
    const val giveSizesToParent = 1L.shl(10)
    /** Frame qui prend la taille de sont parent. */
    const val frameOfParent = 1L.shl(11)

    /*-- Pour les screens --*/
    /** Lors du reshape, le screen réaligne les "blocs" (premiers descendants).
     * Par défaut on aligne, il faut préciser seulement si on ne veut PAS aligner. */
    const val dontAlignScreenElements = 1L.shl(12)
    const val persistentScreen = 1L.shl(13)

    /*-- Affichage de branche --*/
    /** Pour l'affichage. La branche a encore des descendant à afficher. */
    const val branchToDisplay = 1L.shl(14)

    /*-- Placements à l'ouverture --*/
    /** Positionner relativement au cadre du parent. */
    const val relativeToRight = 1L.shl(15)
    const val relativeToLeft = 1L.shl(16)
    const val relativeToTop = 1L.shl(17)
    const val relativeToBottom = 1L.shl(18)
    const val fadeInRight = 1L.shl(19)
    /** Le noeud n'est pas centré, il est "justifié" (aligné) par rapport au côté... */
    const val justifiedRight = 1L.shl(20)
    const val justifiedLeft = 1L.shl(21)
    const val justifiedTop = 1L.shl(22)
    const val justifiedBottom = 1L.shl(23)
    const val relativeFlags = relativeToRight or relativeToLeft or relativeToTop or relativeToBottom or
            justifiedRight or justifiedLeft or justifiedTop or justifiedBottom
    const val openFlags = relativeFlags or fadeInRight

    const val notToAlign = 1L.shl(24)
    // Contient des noeuds devant être "reshapés" après un changement de taille de l'écran.
    const val reshapeableRoot = 1L.shl(25)

    /** Le premier flag pouvant être utilisé dans un projet spécifique. */
    const val firstCustomFlag = 1L.shl(26)
}
