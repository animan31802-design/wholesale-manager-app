package com.animan.wholesalemanager.utils

/**
 * Converts Tamil Unicode text to readable Roman/English transliteration
 * for use when printing on thermal printers that lack Tamil font support.
 *
 * Uses a simple character-by-character mapping covering all Tamil consonants,
 * vowels, vowel signs (matras), and common special characters.
 *
 * Example: "அரிசி" → "arisi"  |  "பால்" → "paal"
 */
object TamilTransliterator {

    // ── Independent vowels ────────────────────────────────────────────
    private val vowels = mapOf(
        'அ' to "a",   'ஆ' to "aa",  'இ' to "i",   'ஈ' to "ii",
        'உ' to "u",   'ஊ' to "uu",  'எ' to "e",   'ஏ' to "ee",
        'ஐ' to "ai",  'ஒ' to "o",   'ஓ' to "oo",  'ஔ' to "au"
    )

    // ── Consonants (base form, without inherent 'a') ──────────────────
    private val consonants: Map<Char, String> = mapOf(
        'க' to "k",   'ங' to "ng",  'ச' to "ch",  'ஞ' to "nj",
        'ட' to "t",   'ண' to "n",   'த' to "th",  'ந' to "n",
        'ப' to "p",   'ம' to "m",   'ய' to "y",   'ர' to "r",
        'ல' to "l",   'வ' to "v",   'ழ' to "zh",  'ள' to "l",
        'ற' to "tr",  'ன' to "n",   'ஜ' to "j",   'ஷ' to "sh",
        //'ஸ' to "s",   'ஹ' to "h",   'க்ஷ' to "ksh", 'ஶ' to "sh"
        'ஸ' to "s",   'ஹ' to "h",   'ஶ' to "sh"
    )

    // ── Vowel signs (matras) that follow a consonant ──────────────────
    // These modify the consonant's vowel sound
    private val vowelSigns = mapOf(
        '\u0BBE' to "aa",   // ா
        '\u0BBF' to "i",    // ி
        '\u0BC0' to "ii",   // ீ
        '\u0BC1' to "u",    // ு
        '\u0BC2' to "uu",   // ூ
        '\u0BC6' to "e",    // ெ
        '\u0BC7' to "ee",   // ே
        '\u0BC8' to "ai",   // ை
        '\u0BCA' to "o",    // ொ
        '\u0BCB' to "oo",   // ோ
        '\u0BCC' to "au",   // ௌ
        '\u0BCD' to "",     // ் (pulli/virama — removes inherent vowel, consonant only)
        '\u0BD7' to "au"    // ௗ
    )

    // ── Digits ────────────────────────────────────────────────────────
    private val tamilDigits = mapOf(
        '௦' to "0", '௧' to "1", '௨' to "2", '௩' to "3", '௪' to "4",
        '௫' to "5", '௬' to "6", '௭' to "7", '௮' to "8", '௯' to "9"
    )

    /**
     * Returns true if the string contains any Tamil Unicode characters.
     */
    fun containsTamil(text: String): Boolean =
        text.any { it.code in 0x0B80..0x0BFF }

    /**
     * Transliterates a Tamil string to Roman script.
     * Non-Tamil characters (English, numbers, symbols) pass through unchanged.
     */
    fun transliterate(text: String): String {
        if (!containsTamil(text)) return text

        val sb = StringBuilder()
        var i  = 0
        val chars = text.toCharArray()

        while (i < chars.size) {
            val ch = chars[i]

            if (i + 2 < chars.size &&
                chars[i] == 'க' &&
                chars[i + 1] == '\u0BCD' &&
                chars[i + 2] == 'ஷ') {

                sb.append("ksh")
                i += 3
                continue
            }

            // Tamil digit
            if (tamilDigits.containsKey(ch)) {
                sb.append(tamilDigits[ch])
                i++
                continue
            }

            // Vowel
            if (vowels.containsKey(ch)) {
                sb.append(vowels[ch])
                i++
                continue
            }

            // Consonant
            if (consonants.containsKey(ch)) {
                val consonantRoman = consonants[ch]!!
                val next = chars.getOrNull(i + 1)
                val sign = next?.let { vowelSigns[it] }

                when {
                    next == '\u0BCD' -> {
                        sb.append(consonantRoman)
                        i += 2
                    }
                    sign != null -> {
                        sb.append(consonantRoman)
                        sb.append(sign)
                        i += 2
                    }
                    else -> {
                        sb.append(consonantRoman)
                        sb.append("a")
                        i++
                    }
                }
                continue
            }

            // Vowel sign alone
            if (vowelSigns.containsKey(ch)) {
                sb.append(vowelSigns[ch])
                i++
                continue
            }

            // Default
            sb.append(ch)
            i++
        }

        return sb.toString()
    }

    /**
     * Transliterate only if the string contains Tamil.
     * Otherwise return unchanged. Use this for all printer text.
     */
    fun forPrinter(text: String): String =
        if (containsTamil(text)) transliterate(text) else text
}