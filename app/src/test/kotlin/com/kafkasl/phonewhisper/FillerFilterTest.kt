package com.kafkasl.phonewhisper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillerFilterTest {

    @Test fun `removes russian fillers`() {
        assertEquals("привет, как дела", FillerFilter.clean("Эм привет, мм как эээ дела м-м"))
    }

    @Test fun `removes english fillers`() {
        assertEquals("hello world", FillerFilter.clean("Um, hello uh world hmm"))
    }

    @Test fun `keeps real words`() {
        val t = "Мы много работаем, это э-э хорошо"
        assertEquals("Мы много работаем, это хорошо", FillerFilter.clean(t))
    }

    @Test fun `does not break words containing filler letters`() {
        assertEquals("Мама мыла раму", FillerFilter.clean("Мама мыла раму"))
        assertEquals("Эмалированный", FillerFilter.clean("Эмалированный"))
    }

    @Test fun `keeps meaningful interjections`() {
        assertEquals("Ой, всё пропало", FillerFilter.clean("Ой, всё пропало"))
        assertEquals("А что дальше", FillerFilter.clean("А что дальше"))
    }

    @Test fun `punctuation attached to filler is dropped`() {
        assertEquals("начали.", FillerFilter.clean("Эм... начали."))
    }

    @Test fun `all fillers only`() {
        // Everything is filler -> keep original rather than inject empty text.
        assertEquals("Эм. Ээ. Ммм.", FillerFilter.clean("Эм. Ээ. Ммм."))
    }

    @Test fun `blank passthrough`() {
        assertEquals("", FillerFilter.clean(""))
        assertEquals("  ", FillerFilter.clean("  "))
    }

    @Test fun `isFiller edge cases`() {
        assertTrue(FillerFilter.isFiller("эм"))
        assertTrue(FillerFilter.isFiller("ЭМ,"))
        assertTrue(FillerFilter.isFiller("м-м"))
        assertFalse(FillerFilter.isFiller("мир"))
        assertFalse(FillerFilter.isFiller("..."))
        assertFalse(FillerFilter.isFiller("ой"))
    }
}
