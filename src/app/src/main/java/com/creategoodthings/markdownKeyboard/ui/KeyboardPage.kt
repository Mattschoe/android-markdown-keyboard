package com.creategoodthings.markdownKeyboard.ui

/**
 * Which set of rows is showing under the markdown rows.
 *
 * The markdown rows never change, so every page is the same six rows tall and the keyboard does
 * not resize under the user's thumb when they switch.
 */
enum class KeyboardPage {
    /** The QWERTY letters. */
    Letters,

    /** `?123`: digits and the common punctuation. */
    Symbols,

    /** `=\<`: currency, maths and the marks that did not fit on [Symbols]. */
    SymbolsExtra,

    /** `1234`: the number pad, with the arithmetic operators down the side. */
    Numeric,
}
