package ch.frostnova.app.mailtool.gui

import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.Interval
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class RetentionValueTest {

    @BeforeEach
    fun useEnglish() {
        I18n.locale = Locale.ENGLISH
    }

    @Test
    fun `sorts by duration, not alphabetically by label`() {
        val values = listOf(
            RetentionValue(Interval(days = 21)),
            RetentionValue(Interval(days = 100)),
            RetentionValue(Interval(days = 5))
        )

        val sorted = values.sorted().map { it.toString() }

        assertThat(sorted).containsExactly("5 days", "21 days", "100 days")
    }

    @Test
    fun `sub-day durations sort before multi-day ones`() {
        val values = listOf(
            RetentionValue(Interval(days = 1)),
            RetentionValue(Interval(hours = 12))
        )

        val sorted = values.sorted().map { it.toString() }

        assertThat(sorted).containsExactly("12h", "1 day")
    }

    @Test
    fun `missing retention sorts before any duration`() {
        val withPeriod = RetentionValue(Interval(days = 1))
        val empty = RetentionValue(null as Interval?)

        assertThat(listOf(withPeriod, empty).sorted()).containsExactly(empty, withPeriod)
    }
}
