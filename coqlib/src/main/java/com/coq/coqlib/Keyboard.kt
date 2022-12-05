package com.coq.coqlib

/** KeyboardKey peut être lié à un noeud-bouton-"touche" ou simplement un event du clavier. */
interface KeyboardKey {
    val scancode: Int
    val keycode: Int
    val keymod: Int
    val isVirtual: Boolean
}

data class KeyData(
    override val scancode: Int,
    override val keycode: Int,
    override val keymod: Int,
    override val isVirtual: Boolean
) : KeyboardKey