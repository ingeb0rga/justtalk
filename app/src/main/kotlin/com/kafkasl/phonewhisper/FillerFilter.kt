package com.kafkasl.phonewhisper

/**
 * Removes filler sounds (междометия, мычание) from a transcription:
 * «эм», «ээ», «мм», «м-м», «хм», «ах», «ох», en: um, uh, hmm, er.
 *
 * Conservative: only standalone tokens are dropped; real words are never touched.
 */
object FillerFilter {

    // Matched against the token with surrounding punctuation stripped.
    // ru: э/ээ/эээ, эм/эмм, м/мм/ммм, э-э, м-м, хм/хмм, ах, ох
    // en: um/umm, uh, mm/mmm, hmm, er
    private val FILLER = Regex(
        "^(э+м*|м+|хм+|а+х+|о+х+|э[-­]?э*|м[-]?м*|um+|uh+|hmm+|er+)$",
        RegexOption.IGNORE_CASE
    )

    private val EDGE_PUNCT = Regex("^[^\\p{L}\\p{M}\\d]+|[^\\p{L}\\p{M}\\d]+$")

    fun isFiller(token: String): Boolean {
        val stem = token.replace(EDGE_PUNCT, "")
        if (stem.isEmpty()) return false
        return FILLER.matches(stem)
    }

    /** Returns text with filler tokens removed; whitespace normalized, trailing space trimmed. */
    fun clean(text: String): String {
        if (text.isBlank()) return text
        val kept = text.split(Regex("\\s+")).filterNot { isFiller(it) }
        val out = kept.joinToString(" ").trim()
        return out.ifEmpty { text.trim() }
    }
}
