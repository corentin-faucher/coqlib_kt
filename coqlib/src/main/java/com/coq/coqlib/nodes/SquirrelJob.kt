/*------------------------------------------------------------------*/
/*-- Quelques extensions (de Node) utilisant les Squirrels.       --*/
/*-- Fonctions utiles pour les opérations de base sur les arbres. --*/
/*------------------------------------------------------------------*/
@file:Suppress("unused")
package com.coq.coqlib.nodes

import com.coq.coqlib.maths.Vector2
import com.coq.coqlib.printerror
import com.coq.coqlib.printwarning


/** Ajouter des flags à une branche (noeud et descendants s'il y en a). */
fun Node.forEachAddFlags(flags: Long) {
    addFlags(flags)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.addFlags(flags)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

/** Retirer des flags à toute la branche (noeud et descendants s'il y en a). */
fun Node.forEachRemoveFlags(flags: Long) {
    removeFlags(flags)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.removeFlags(flags)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

fun Node.forEachNodeInBranch(block: (Node)-> Unit) {
    block(this)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        block(sq.pos)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

fun Node.forEachChild(block: (Node) -> Unit) {
    val sq = Squirrel(firstChild ?: return)
    do {
        block(sq.pos)
    } while(sq.goRight())
}

inline fun <reified T: Node> Node.forEachTypedNodeInBranch(block: (T)->Unit) {
    (this as? T)?.let { block(it) }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        (sq.pos as? T)?.let { block(it) }
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

inline fun <reified T: Node> Node.forEachTypedChild(block: (T) -> Unit) {
    val sq = Squirrel(firstChild ?: return)
    do {
        (sq.pos as? T)?.let { block(it) }
    } while(sq.goRight())
}

/** Genre de "zip" : itère sur les enfants direct et la liste simultanément. */
inline fun <reified N: Node, E> Node.forEachTypedChildWithList(list: List<E>, block: (N, E) -> Unit) {
    val sq = Squirrel(firstChild ?: return)
    val it = list.listIterator()
    do {
        (sq.pos as? N)?.let { n ->
            if (!it.hasNext()) {
                printerror("Not enough element in list for all children.")
                return
            }
            block(n, it.next())
        }
    } while(sq.goRight())
    if(it.hasNext())
        printwarning("Still got unused list elements.")
}

/** Retirer des flags à la loop de frère où se situe le noeud présent. */
fun Node.removeBroLoopFlags(flags: Long) {
    removeFlags(flags)
    var sq = Squirrel(this)
    while (sq.goRight()) {
        sq.pos.removeFlags(flags)
    }
    sq = Squirrel(this)
    while (sq.goLeft()) {
        sq.pos.removeFlags(flags)
    }
}

/** Ajouter/retirer des flags à une branche (noeud et descendants s'il y en a). */
fun Node.addRemoveBranchFlags(flagsAdded: Long, flagsRemoved: Long) {
    addRemoveFlags(flagsAdded, flagsRemoved)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.addRemoveFlags(flagsAdded, flagsRemoved)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}
/** Ajouter un flag aux "parents" (pas au noeud présent). */
fun Node.addRootFlag(flag: Long) {
    val sq = Squirrel(parent ?: return)
    do {
        if (sq.pos.containsAFlag(flag)) {
            break
        } else {
            sq.pos.addFlags(flag)
        }
    } while (sq.goUp())
}

/** Flag le noeud comme "selectable" et trace sont chemin dans l'arbre pour être retrouvable. */
fun Node.makeSelectable() {
    addRootFlag(Flag1.selectableRoot)
    addFlags(Flag1.selectable)
}

/**  Pour chaque noeud :
 * 1. Applique open(),
 * 2. ajoute "show" si non caché,
 * (show peut être ajouté manuellement avant pour afficher une branche cachée)
 * 3. visite si est une branche avec "show".
 * (show peut avoir été ajouté extérieurement) */
fun Node.openAndShowBranch() {
    this.open()
    if (!containsAFlag(Flag1.hidden)) {
        addFlags(Flag1.show)
    }
    if (!containsAFlag(Flag1.show)) {
        return
    }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.open()
        if (!sq.pos.containsAFlag(Flag1.hidden)) {
            sq.pos.addFlags(Flag1.show)
        }
        if (sq.pos.containsAFlag(Flag1.show)) if (sq.goDown()) {
            continue
        }
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch."); return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}
fun Node.unhideAndTryToOpen() {
    removeFlags(Flag1.hidden)
    if(parent?.containsAFlag(Flag1.show) == true)
        openAndShowBranch()
}

/** Enlever "show" aux noeud de la branche (sauf les alwaysShow) et appliquer la "closure". */
fun Node.closeBranch() {
    if (!containsAFlag(Flag1.exposed)) {
        removeFlags(Flag1.show)
    }
    close()
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        if (!sq.pos.containsAFlag(Flag1.exposed)) {
            sq.pos.removeFlags(Flag1.show)
        }
        sq.pos.close()
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de this."); return
            } else if (sq.pos === this) {return}
        }
    }
}

fun Node.hideAndTryToClose() {
    addFlags(Flag1.hidden)
    if(containsAFlag(Flag1.show))
        closeBranch()
}

fun Node.reshapeBranch() {
    if (!containsAFlag(Flag1.show))
        return
    reshape()
    if(!containsAFlag(Flag1.reshapeableRoot)) return
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        if (sq.pos.containsAFlag(Flag1.show)) {
            sq.pos.reshape()
            if(sq.pos.containsAFlag(Flag1.reshapeableRoot)) if(sq.goDown())
                continue
        }
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de root."); return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

/** Recherche d'un noeud sélectionnable dans le noeud présent.
 * La position est dans le référentiel du noeud présent (comme les enfants du noeud présent).
 * Retourne nil si rien trouvé. */
fun Node.searchBranchForSelectableAt(relPos: Vector2, nodeToAvoid: Node?) : Node? {
    val sq = Squirrel(this, relPos, ScaleInit.Ones)
    var candidate: Node? = null

    // 0. Vérifier si on peut aller en profondeur.
    if (!sq.isIn || !sq.pos.containsAFlag(Flag1.show) || sq.pos === nodeToAvoid) {
        return null
    }
    // 1. Cas particulier pour le point de départ -> On ne peut pas aller au littleBro...
    // 1.1 Possibilité trouvé
    if (sq.pos.containsAFlag(Flag1.selectable)) {
        candidate = sq.pos
        if (!sq.pos.containsAFlag(Flag1.selectableRoot)) {return candidate}
    }
    // 1.2 Aller en profondeur
    if (sq.pos.containsAFlag(Flag1.selectableRoot)) {
        if (!sq.goDownP()) return candidate
    } else {
        return candidate
    }
    // 2. Cas général
    while (true) {
        if (sq.isIn) if (sq.pos.containsAFlag(Flag1.show) && (sq.pos !== nodeToAvoid)) {
            // 1. Possibilité trouvé
            if (sq.pos.containsAFlag(Flag1.selectable)) {
                candidate = sq.pos
                if (!sq.pos.containsAFlag(Flag1.selectableRoot)) {
                    return candidate
                }
            }
            // 2. Aller en profondeur
            if (sq.pos.containsAFlag(Flag1.selectableRoot)) {
                if (sq.goDownP()) {
                    continue
                } else {
                    printerror("selectableRoot sans desc.")
                }
            }
        }
        // 3. Remonter, si plus de petit-frère
        while (!sq.goRight()) {
            if (!sq.goUpP()) {
                printerror("Pas de root."); return candidate
            } else if (sq.pos === this) {return candidate}
        }
    }
}

inline fun <reified T: Node>Node.searchBranchForFirstSelectableTyped() : T? {
    if(!containsAFlag(Flag1.show)) return null
    if(containsAFlag(Flag1.selectable) && this is T)
        return this
    if(!containsAFlag(Flag1.selectableRoot)) return null
    val sq = Squirrel(firstChild ?: return null)
    while(true) {
        if(sq.pos.containsAFlag(Flag1.show)) {
            if(sq.pos.containsAFlag(Flag1.selectable))
                (sq.pos as? T)?.let { return it }
            if(sq.pos.containsAFlag(Flag1.selectableRoot)) if(sq.goDown())
                continue
        }
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de root."); return null
            } else if (sq.pos === this) {
                return null
            }
        }
    }
}

fun Node.searchBranchForFirstSelectableUsing(testIsValid: (Node)->Boolean) : Node? {
    if(!containsAFlag(Flag1.show)) return null
    if(containsAFlag(Flag1.selectable) && testIsValid(this))
        return this
    if(!containsAFlag(Flag1.selectableRoot)) return null
    val sq = Squirrel(firstChild ?: return null)
    while(true) {
        if(sq.pos.containsAFlag(Flag1.show)) {
            if(sq.pos.containsAFlag(Flag1.selectable) && testIsValid(sq.pos))
                return sq.pos
            if(sq.pos.containsAFlag(Flag1.selectableRoot)) if(sq.goDown())
                continue
        }
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de root."); return null
            } else if (sq.pos === this) {
                return null
            }
        }
    }
}

