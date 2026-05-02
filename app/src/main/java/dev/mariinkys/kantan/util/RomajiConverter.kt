package dev.mariinkys.kantan.util

/**
 * Converts a romaji string to hiragana.
 * Handles standard Hepburn romanization including digraphs and double-consonant
 * gemination (e.g. "taberu" → "たべる", "tatte" → "たって").
 */
object RomajiConverter {

    // Ordered longest-first so digraphs (shi, tsu, chi…) match before singles
    private val TABLE: List<Pair<String, String>> = listOf(
        // Special digraphs
        "sha" to "しゃ", "shi" to "し", "shu" to "しゅ", "she" to "しぇ", "sho" to "しょ",
        "chi" to "ち", "cha" to "ちゃ", "chu" to "ちゅ", "che" to "ちぇ", "cho" to "ちょ",
        "tsu" to "つ",
        "tchi" to "っち",
        // N before vowel/y needs special care — handled in code
        // Ki-row combinations
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        // Gi-row
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        // Shi-row
        "sya" to "しゃ", "syu" to "しゅ", "syo" to "しょ",
        // Ji-row
        "ja" to "じゃ", "ji" to "じ", "ju" to "じゅ", "je" to "じぇ", "jo" to "じょ",
        "jya" to "じゃ", "jyu" to "じゅ", "jyo" to "じょ",
        "zya" to "じゃ", "zyu" to "じゅ", "zyo" to "じょ",
        // Ni-row
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        // Hi-row
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        // Bi/Pi-row
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        // Mi-row
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        // Ri-row
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
        // Chi-row
        "cya" to "ちゃ", "cyu" to "ちゅ", "cyo" to "ちょ",
        // Fu combinations
        "fa" to "ふぁ", "fi" to "ふぃ", "fu" to "ふ", "fe" to "ふぇ", "fo" to "ふぉ",
        // Ti/Di (alternative spellings)
        "tya" to "ちゃ", "tyi" to "ちぃ", "tyu" to "ちゅ", "tye" to "ちぇ", "tyo" to "ちょ",
        "dya" to "ぢゃ", "dyu" to "ぢゅ", "dyo" to "ぢょ",
        // Basic ka-row
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        // Sa-row
        "sa" to "さ", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        // Ta-row
        "ta" to "た", "ti" to "ち", "tu" to "つ", "te" to "て", "to" to "と",
        // Na-row
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        // Ha-row
        "ha" to "は", "hi" to "ひ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        // Ma-row
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        // Ya-row
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        // Ra-row
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        // Wa-row
        "wa" to "わ", "wi" to "ゐ", "we" to "ゑ", "wo" to "を",
        // Ga-row
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        // Za-row
        "za" to "ざ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        // Da-row
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        // Ba-row
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        // Pa-row
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        // Vowels
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
        // N
        "n" to "ん"
    )

    fun convert(romaji: String): String {
        val input = romaji.lowercase().trim()
        val result = StringBuilder()
        var i = 0

        while (i < input.length) {
            // Double-consonant gemination → っ
            // e.g. "tt" → っ + continue with single "t..."
            if (i + 1 < input.length
                && input[i].isLetter()
                && input[i] == input[i + 1]
                && input[i] != 'n'          // "nn" → ん, handled below
                && input[i] != 'a'
                && input[i] != 'i'
                && input[i] != 'u'
                && input[i] != 'e'
                && input[i] != 'o'
            ) {
                result.append("っ")
                i++
                continue
            }

            // "nn" or "n" before consonant/end → ん
            if (input[i] == 'n') {
                val next = input.getOrNull(i + 1)
                input.getOrNull(i + 2)
                when {
                    next == 'n' -> {
                        result.append("ん"); i += 1; continue
                    }

                    next == null || (next != 'a' && next != 'i' && next != 'u'
                            && next != 'e' && next != 'o' && next != 'y') -> {
                        // n at end, or before a consonant that isn't y
                        result.append("ん"); i += 1; continue
                    }
                    // else: "na", "ni", "nu", "ne", "no", "nya"… fall through to table
                }
            }

            // Table lookup (longest match first)
            var matched = false
            for ((rom, kana) in TABLE) {
                if (input.startsWith(rom, i)) {
                    result.append(kana)
                    i += rom.length
                    matched = true
                    break
                }
            }

            // Pass-through for unrecognized chars
            if (!matched) {
                result.append(input[i])
                i++
            }
        }

        return result.toString()
    }

    /** True if the string contains at least one hiragana, katakana, or kanji character. */
    fun isJapanese(text: String): Boolean = text.any { c ->
        c.code in 0x3040..0x309F   // hiragana
                || c.code in 0x30A0..0x30FF // katakana
                || c.code in 0x4E00..0x9FFF // CJK unified
                || c.code in 0x3400..0x4DBF // CJK ext-A
    }
}