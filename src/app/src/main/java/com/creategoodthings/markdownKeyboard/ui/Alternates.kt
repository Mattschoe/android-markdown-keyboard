package com.creategoodthings.markdownKeyboard.ui

/**
 * The characters a key offers when held.
 *
 * Kept as plain data, free of Android types, and lowercase throughout: the letter rows upper-case
 * both the key and its alternates together when shift is on.
 */
internal object Alternates {

    /** Looked up by the symbol and number pages. */
    val symbols: Map<String, List<String>> = mapOf(
        "1" to listOf("¹", "½", "⅓", "¼"),
        "2" to listOf("²", "⅔"),
        "3" to listOf("³", "¾"),
        "0" to listOf("⁰", "ø"),
        "$" to listOf("¢", "£", "€", "¥", "₹"),
        "&" to listOf("§"),
        "-" to listOf("–", "—", "·"),
        "+" to listOf("±"),
        "(" to listOf("[", "{", "<"),
        ")" to listOf("]", "}", ">"),
        "/" to listOf("\\"),
        "*" to listOf("†", "‡", "★"),
        "\"" to listOf("«", "»", "„", "“", "”"),
        "'" to listOf("‘", "’", "‚"),
        "!" to listOf("¡"),
        "?" to listOf("¿"),
        "%" to listOf("‰"),
        "=" to listOf("≠", "≈", "∞"),
        "." to listOf("…"),
        "#" to listOf("№"),
        "^" to listOf("↑", "↓"),
        "°" to listOf("′", "″"),
    )

    /** Looked up by the letter rows, and upper-cased along with the key when shift is on. */
    val letters: Map<String, List<String>> = mapOf(
        "a" to listOf("à", "á", "â", "ä", "å", "æ", "ã"),
        "c" to listOf("ç", "ć", "č"),
        "e" to listOf("è", "é", "ê", "ë", "ē", "ę"),
        "i" to listOf("ì", "í", "î", "ï", "ī"),
        "l" to listOf("ł"),
        "n" to listOf("ñ", "ń"),
        "o" to listOf("ò", "ó", "ô", "ö", "õ", "ø", "œ"),
        "s" to listOf("ß", "š", "ś"),
        "u" to listOf("ù", "ú", "û", "ü", "ū"),
        "y" to listOf("ÿ", "ý"),
        "z" to listOf("ž", "ź", "ż"),
    )

    fun symbol(character: String): List<String> = symbols[character].orEmpty()

    fun letter(character: String): List<String> = letters[character].orEmpty()
}
