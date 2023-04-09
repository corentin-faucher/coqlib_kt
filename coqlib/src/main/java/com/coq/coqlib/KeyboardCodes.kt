@file:Suppress("unused")

package com.coq.coqlib

/** KeyboardKey peut être lié à un noeud-bouton-"touche" ou simplement un event du clavier. */
interface KeyboardInput {
    val scancode: Int
    val keycode: Int
    val keymod: Int
    val isVirtual: Boolean
}

data class KeyboardInputStruct(
    override val scancode: Int,
    override val keycode: Int,
    override val keymod: Int,
    override val isVirtual: Boolean
) : KeyboardInput

/**--- Pour les MODIFIERS :  ---*/
/**  -> KeyEvent.META_...    ---*/

/** MyKeyCode... */
object MKC {
    const val space = 51 // (Fait parti des Keycode "ordinaire")
    // Keycodes spéciaux ayant une string associable
    const val delete = 52
    const val return_ = 53
    const val tab = 54
    // Keycodes de contrôle
    const val firstExtraMKC = 55
    const val capsLock = 60
    const val control = 61
    const val shift = 62
    const val option = 63
    const val command = 64
    const val rightControl = 65
    const val rightShift = 66
    const val rightOption = 67
    const val rightCommand = 68
    // Autre Keycodes Spéciaux
    const val escape = 70
    const val eisu = 71
    const val kana = 72
    // Pour les "autres" non définie (e.g. fn, quelconque...)
    const val empty = 73
    const val totalMKC = 74

    /** Les mkcs de la ligne principale du clavier */
    val homerow: IntArray = (24..34).toIntArray()
    const val homerowFirst: Int = 24 // i.e. la touche "A".
    /** Ordre "standard" d'utilisation des touches du clavier,
     * i.e.: homerow, qwerty row, zxcv row, 01234 row, mod/space row...
     * Pour l'ordre en haut à gauche vers en bas à droite, c'est juste (0 until MKC.totalMKC).*/
    val ordoredMKCs: IntArray = (24..34).toIntArray() + (12..23).toIntArray() +
                (35..44).toIntArray() + (0..11).toIntArray() + (45 until MKC.totalMKC).toIntArray()
}

object Scancode {
    // Modifiers
    const val shiftLeft = 42
    const val shiftRight = 54
    const val altLeft = 56
    const val altRight = 100
    const val capsLock = 58
    const val control = 29
    // Touches importantes
    const val space = 57
    const val backspace = 14
    const val tab = 15
    const val enter = 28
    const val escape = 1
    // Directions
    const val left = 105
    const val right = 106
    const val up = 103
    const val down = 108
    // Divers
    const val empty = 0

    val ofMKC = mapOf(
        // 1->+,
        0 to 2,   1 to 3,   2 to 4,   3 to 5,   4 to 6,   5 to 7,   6 to 8,   7 to 9,   8 to 10,  9 to 11,  10 to 12, 11 to 13,
        // Q->},
        12 to 16, 13 to 17, 14 to 18, 15 to 19, 16 to 20, 17 to 21, 18 to 22, 19 to 23, 20 to 24, 21 to 25, 22 to 26, 23 to 27,
        // A->',
        24 to 30, 25 to 31, 26 to 32, 27 to 33, 28 to 34, 29 to 35, 30 to 36, 31 to 37, 32 to 38, 33 to 39, 34 to 40,
        // Z->?,
        35 to 44, 36 to 45, 37 to 46, 38 to 47, 39 to 48, 40 to 49, 41 to 50, 42 to 51, 43 to 52, 44 to 53,
        // `(ansi haut-gauche/iso bas-gauche), §(iso), jis1(ro bas-droite), jis2(yen haut-droite), ansi_backsl, iso_backsl
        45 to 41, 46 to 86, 47 to 89, 48 to 124, 49 to 43, 50 to 43,
        // Autres touches importantes
        MKC.space to space, MKC.return_ to enter,
        MKC.tab to tab, MKC.escape to escape,
        MKC.delete to backspace,
        MKC.control to control, MKC.rightControl to control,
        MKC.capsLock to capsLock,
        MKC.shift to shiftLeft, MKC.rightShift to shiftRight,
        MKC.option to altLeft, MKC.rightOption to altRight,
        MKC.empty to empty,
    )
}
