package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction
import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.Interval
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class FolderMatchingTest {

    @BeforeEach
    fun useEnglish() {
        I18n.locale = Locale.ENGLISH
    }

    @Test
    fun `retention matches folder by case-insensitive substring`() {
        val settings = listOf(
            retention("Sent", Interval(days = 3650)),
            retention("Spam", Interval(days = 21)),
            retention("Drafts", Interval(days = 1))
        )

        assertThat(retentionFor("Sent", settings)).isEqualTo("3650 days")
        assertThat(retentionFor("INBOX.Sent", settings)).isEqualTo("3650 days")
        assertThat(retentionFor("spam", settings)).isEqualTo("21 days")
        assertThat(retentionFor("Archive", settings)).isEmpty()
    }

    @Test
    fun `rules targeting a folder list move and copy rules only`() {
        val rules = listOf(
            rule(listOf("swica.ch", "generali.com"), MailRuleAction.MOVE, "Insurance"),
            rule(listOf("digitec.ch"), MailRuleAction.COPY, "Shopping"),
            rule(listOf("spam.ru"), MailRuleAction.DELETE, null),
            rule(listOf("x.com"), MailRuleAction.MOVE, "Insurance")
        )

        assertThat(rulesFor("Insurance", rules)).isEqualTo("MOVE: swica.ch, generali.com; MOVE: x.com")
        assertThat(rulesFor("MyShopping", rules)).isEqualTo("COPY: digitec.ch")
        assertThat(rulesFor("Archive", rules)).isEmpty()
    }

    @Test
    fun `retention label handles singular and legacy sub-day values`() {
        assertThat(retentionFor("A", listOf(retention("A", Interval(days = 1))))).isEqualTo("1 day")
        assertThat(retentionFor("B", listOf(retention("B", Interval(0, 12, 30, 0))))).isEqualTo("12h 30m")
    }

    private fun retention(folder: String, period: Interval) = DataRetentionSettings().apply {
        this.folder = folder
        this.retentionPeriod = period
    }

    private fun rule(senders: List<String>, action: MailRuleAction, folder: String?) = MailRule().apply {
        this.senders = senders
        this.action = action
        this.folder = folder
    }
}
