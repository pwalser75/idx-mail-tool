package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.apply.ActionOrigin
import ch.frostnova.app.mailtool.apply.ActionType
import ch.frostnova.app.mailtool.apply.MailAction
import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.i18n.I18n
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ApplyPanelModelTest {

    @BeforeEach
    fun useEnglish() {
        I18n.locale = Locale.ENGLISH
    }

    @Test
    fun `lists action, subject, sender, date and details`() {
        val model = ActionsTableModel()
        model.setActions(
            listOf(
                MailAction(
                    type = ActionType.MOVE,
                    origin = ActionOrigin.RULE,
                    subject = "Newsletter",
                    sender = "news@example.org",
                    date = Instant.parse("2020-03-04T10:15:30Z"),
                    size = 1234,
                    targetFolder = "Archive"
                )
            )
        )

        assertThat((0 until model.columnCount).map { model.getColumnName(it) })
            .containsExactly("Action", "Subject", "Sender", "Date", "Details")
        assertThat(model.getValueAt(0, 0)).isEqualTo("MOVE")
        assertThat(model.getValueAt(0, 1)).isEqualTo("Newsletter")
        assertThat(model.getValueAt(0, 2)).isEqualTo("news@example.org")
        assertThat(model.getValueAt(0, 3)).isEqualTo(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
                .format(Instant.parse("2020-03-04T10:15:30Z"))
        )
        assertThat(model.getValueAt(0, 4)).isEqualTo("to Archive")
    }

    @Test
    fun `retention delete shows the source folder`() {
        val model = ActionsTableModel()
        model.setActions(
            listOf(
                MailAction(
                    type = ActionType.DELETE,
                    origin = ActionOrigin.RETENTION,
                    subject = "Old mail",
                    sender = "x@y.z",
                    date = Instant.parse("2019-01-01T00:00:00Z"),
                    size = 42,
                    sourceFolder = "INBOX/Spam"
                )
            )
        )

        assertThat(model.getValueAt(0, 4)).isEqualTo("from folder INBOX/Spam")
    }

    @Test
    fun `input signature changes when rules change`() {
        var rules = listOf(MailRule().apply { senders = listOf("a.ch"); action = MailRuleAction.DELETE })
        val panel = ApplyPanel(
            connector = object : MailConnector {
                override fun connect(properties: AccountProperties) = throw UnsupportedOperationException()
            },
            connectionSupplier = { AccountProperties() },
            rulesSupplier = { rules },
            retentionSupplier = { emptyList<DataRetentionSettings>() },
            onApplied = {}
        )

        val before = panel.inputSignature()
        rules = listOf(MailRule().apply { senders = listOf("b.ch"); action = MailRuleAction.DELETE })

        assertThat(panel.inputSignature()).isNotEqualTo(before)
    }
}
