package ch.frostnova.app.mailtool.apply

import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction
import ch.frostnova.app.mailtool.connector.MailAdapter
import ch.frostnova.app.mailtool.util.Interval
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.mail.Address
import jakarta.mail.Folder
import jakarta.mail.Message
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@ExtendWith(MockKExtension::class)
class RuleApplierTest {

    @MockK
    private lateinit var adapter: MailAdapter

    private val inboxRoot: Folder = mockk()
    private val inbox: Folder = mockk()
    private val archive: Folder = mockk()

    private val moveMessage: Message = mockk()
    private val deleteMessage: Message = mockk()

    private val rules = listOf(
        MailRule().apply { senders = listOf("some.org"); action = MailRuleAction.MOVE; folder = "archive" },
        MailRule().apply { senders = listOf(".ru"); action = MailRuleAction.DELETE }
    )

    private fun setUpMailbox() {
        every { inboxRoot.parent } returns null
        every { inbox.parent } returns inboxRoot
        every { inbox.name } returns "Inbox"
        every { inbox.fullName } returns "Inbox"
        every { inbox.isOpen } returns true
        every { inbox.close(any()) } just runs
        every { archive.parent } returns inboxRoot
        every { archive.name } returns "Archive"
        every { archive.fullName } returns "Archive"
        every { archive.isOpen } returns true
        every { archive.close(any()) } just runs

        every { adapter.listFolders() } returns listOf(inbox, archive)
        every { adapter.listMessages(inbox) } returns listOf(moveMessage, deleteMessage)
        every { adapter.listMessages(archive) } returns emptyList()

        mockMessage(moveMessage, "Newsletter", "sender@some.org")
        mockMessage(deleteMessage, "Spam", "orders@shop.ru")

        every { inbox.copyMessages(any(), any()) } just runs
        every { moveMessage.setFlag(any(), any()) } just runs
        every { deleteMessage.setFlag(any(), any()) } just runs
    }

    private fun mockMessage(message: Message, subject: String, sender: String) {
        val address = mockk<Address>()
        every { message.subject } returns subject
        every { message.receivedDate } returns Date(0)
        every { message.size } returns 1234
        every { message.from } returns arrayOf(address)
        every { address.toString() } returns sender
        every { message.folder } returns inbox
    }

    @Test
    fun `preview reports actions without modifying anything`() {
        setUpMailbox()
        val actions = mutableListOf<MailAction>()

        RuleApplier(adapter, rules, emptyList()).run(dryRun = true) { actions.add(it) }

        assertThat(actions.map { it.type }).containsExactly(ActionType.MOVE, ActionType.DELETE)
        assertThat(actions.first().targetFolder).isEqualTo("Archive")
        verify(exactly = 0) { inbox.copyMessages(any(), any()) }
        verify(exactly = 0) { moveMessage.setFlag(any(), any()) }
        verify(exactly = 0) { deleteMessage.setFlag(any(), any()) }
    }

    @Test
    fun `reports progress with the folder and total message count`() {
        setUpMailbox()
        every { inbox.messageCount } returns 2
        every { archive.messageCount } returns 0
        val progress = mutableListOf<ApplyProgress>()

        RuleApplier(adapter, rules, emptyList()).run(
            dryRun = true,
            onAction = {},
            onProgress = { progress.add(it) }
        )

        val updates = progress.filter { it.total > 0 }
        assertThat(updates).hasSize(2)
        assertThat(updates.map { it.folder }).containsOnly("Inbox")
        assertThat(updates.map { it.processed }).containsExactly(1, 2)
        assertThat(updates.map { it.total }).containsOnly(2)
    }

    @Test
    fun `apply performs the actions`() {
        setUpMailbox()
        val actions = mutableListOf<MailAction>()

        RuleApplier(adapter, rules, emptyList()).run(dryRun = false) { actions.add(it) }

        assertThat(actions).hasSize(2)
        verify { inbox.copyMessages(any(), archive) }
        verify { moveMessage.setFlag(any(), true) }
        verify { deleteMessage.setFlag(any(), true) }
    }

    @Test
    fun `a move to a non-existent folder is skipped`() {
        setUpMailbox()
        val rules = listOf(MailRule().apply { senders = listOf("some.org"); action = MailRuleAction.MOVE; folder = "Nowhere" })
        val actions = mutableListOf<MailAction>()

        RuleApplier(adapter, rules, emptyList()).run(dryRun = false) { actions.add(it) }

        assertThat(actions).isEmpty()
        verify(exactly = 0) { inbox.copyMessages(any(), any()) }
        verify(exactly = 0) { moveMessage.setFlag(any(), any()) }
    }

    @Test
    fun `retention deletes messages older than the period`() {
        setUpMailbox()
        every { moveMessage.sentDate } returns Date.from(Instant.now().minus(100, ChronoUnit.DAYS))
        val retention = listOf(
            DataRetentionSettings().apply { folder = "Inbox"; retentionPeriod = Interval(days = 30) }
        )
        val actions = mutableListOf<MailAction>()

        RuleApplier(adapter, emptyList(), retention).run(dryRun = false) { actions.add(it) }

        assertThat(actions.map { it.type }).containsExactly(ActionType.DELETE)
        assertThat(actions.single().origin).isEqualTo(ActionOrigin.RETENTION)
        verify { moveMessage.setFlag(any(), true) }
    }
}
