package com.coq.coqlib

/** Les char spéciaux et "importans" */
object SpChar {
    const val delete: Char = '\b'
    const val tab: Char = '\t'
    // TODO : vérifier si Android utilise '\n' -> 10 ou '\r' -> 13
    const val return_: Char = '\r'
    // On utilise return ... const val newline: Char = '\n'
    const val space: Char = ' '
    const val nobreakSpace: Char = ' '
    const val ideographicSpace: Char = '　'
    const val thinSpace: Char = '\u2009'
    const val bottomBracket: Char = '⎵'
    const val spaceSymbol: Char = '␠'
    const val underscore: Char = '_'
    const val openBox: Char = '␣'
    const val interpunct: Char = '·'
    const val dot: Char = '•'
//    const val butterfly: Char = '🦋'
//    const val dodo: Char = '🦤'
}

val Char.isLatin: Boolean
    get() = this.code < 0x0250

val Char.isIdeogram: Boolean
    get() = this.code in 0x3400..0x9fff

val Char.isNewLine: Boolean
    get() = this == '\n' || this == '\r'

fun Char.toNormalized(forceLower: Boolean): Char {
    if(isNewLine)
        return SpChar.return_
    if (this == '、')
        return if(Language.currentIs(Language.Japanese)) ',' else '\\'

    normalizedCharOf[this]?.let { return it }

    return if(forceLower) this.lowercaseChar() else this
}

/** Version simplifié de toNormalized. (pas pour langue asiatique) */
val Char.loweredAndNormalized: Char
    get() {
        if(isNewLine)
            return SpChar.return_
        limitedNormalizedCharOf[this]?.let { return it }

        return this.lowercaseChar()
}

// Superflu, déjà dans Kotlin.
//fun UInt.toChar(): Char {
//    Char(this.toInt())
//}

private val normalizedCharOf: Map<Char, Char> = mapOf(
    SpChar.ideographicSpace to SpChar.space,
    SpChar.nobreakSpace to SpChar.space,
    '，' to ',',
    '。' to '.',
    '；' to ';',
    '：' to ':',
    '—' to '-',  // "EM Dash"
    '–' to '-',  // "EN Dash"
    'ー' to '-', // (Prolongation pour Katakana)
    '・' to '/',
    '！' to '!',
    '？' to '?',
    '’' to '\'',
    '«' to '\"',
    '»' to '\"',
    '“' to '\"', // Left double quotation mark
    '”' to '\"', // Right double quotation mark (ça paraît pas mais ils sont différents...)
    '（' to '(',
    '）' to ')',
    '「' to '[',
    '」' to ']',
    '『' to '{',
    '』' to '}',
    '《' to '<',
    '》' to '>',
    '［' to '[',
    '］' to ']',
    '｛' to '{',
    '｝' to '}',
    '【' to '[',
    '】' to ']',
    '％' to '%',
    '＊' to '*',
    '／' to '/',
    '｜' to '|',
    '＝' to '=',
    '－' to '-',  // Tiret chinois, different du katakana "ー" plus haut.
)

private val limitedNormalizedCharOf: Map<Char, Char> = mapOf(
    SpChar.nobreakSpace to SpChar.space,
    '’' to '\'',
    '«' to '\"',
    '»' to '\"',
    '“' to '\"', // Left double quotation mark
    '”' to '\"', // Right double quotation mark (ça paraît pas mais ils sont différents...)
    '—' to '-',  // "EM Dash"
    '–' to '-',  // "EN Dash"
)