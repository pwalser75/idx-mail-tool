package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction
import ch.frostnova.app.mailtool.connector.Console
import ch.frostnova.app.mailtool.connector.MailAdapter
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.util.AnsiEscapeCode
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.mail.Address
import jakarta.mail.Folder
import jakarta.mail.Message
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.OffsetDateTime
import java.util.Date

@ExtendWith(MockKExtension::class)
class MailToolTest {

    @MockK
    private lateinit var mailTool: MailTool

    @MockK
    private lateinit var connector: MailConnector

    @MockK
    private lateinit var adapter: MailAdapter

    @MockK
    private lateinit var console: Console

    private val messages = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        val configuration = ConfigurationProperties().apply {
            accounts = mapOf("test" to AccountProperties().apply {
                host = "mailbox.org"
                username = "testuser"
                password = "Secret007!"
                rules = listOf(
                    MailRule().apply {
                        senders = listOf("some.org")
                        action = MailRuleAction.MOVE
                        folder = "archive"
                    },
                    MailRule().apply {
                        senders = listOf(".ru")
                        action = MailRuleAction.DELETE
                    }
                )
            })
        }


        mailTool = MailTool(connector, configuration, console)
        messages.clear()

        every { console.output(any()) } answers {
            println(firstArg<String>())
            messages.add(firstArg())
        }
        every { connector.connect(any()) } returns adapter

        val message1 = mockMessage("Test", "sender@some.org", OffsetDateTime.parse("2020-03-04T10:15:30+00:00"))
        val message2 =
            mockMessage("Re: question", "sender@some.org", OffsetDateTime.parse("2021-07-09T15:20:18+01:00"))
        val message3 = mockMessage("Order #123", "orders@shop.com", OffsetDateTime.parse("2022-12-30T22:13:27+02:00"))
        val message4 = mockMessage("Spam", "orders@shop.ru", OffsetDateTime.parse("2023-09-17T02:03:09+05:00"))

        val root = mockFolder("Root")
        val folder1 = mockFolder("Inbox", root)
        val folder2 = mockFolder("Spam", root)
        val folder3 = mockFolder("Archive", root)

        val folders = listOf(folder1, folder2, folder3)
        val messages = listOf(message1, message2, message3, message4)

        every { adapter.listFolders() } returns folders
        every { message1.folder } returns folder1
        every { message2.folder } returns folder1
        every { message3.folder } returns folder3
        every { message4.folder } returns folder1

        every { adapter.listMessages(any()) } answers {
            messages.filter { it.folder == firstArg() }
        }
        every { adapter.close() } just runs
    }

    @Test
    fun applyRules() {
        mailTool.run(Command.APPLY)
        assertThat(consoleOutput()).isEqualTo(
            """
            > move "Test" to folder "Archive"
              on 2020-03-04T10:15:30Z from sender@some.org , 12345 bytes
            > move "Re: question" to folder "Archive"
              on 2021-07-09T14:20:18Z from sender@some.org , 12345 bytes
            > delete "Spam"
              on 2023-09-16T21:03:09Z from orders@shop.ru , 12345 bytes
            done.
            """.trimIndent()
        )
    }

    private fun consoleOutput(): String {
        var s = messages.joinToString("\n")
        AnsiEscapeCode.entries.forEach { s = s.replace(it.escapeSequence, "") }
        return s
    }

    private fun mockMessage(subject: String, sender: String, timestamp: OffsetDateTime): Message {
        val message = mockk<Message>()
        val address = mockk<Address>()
        every { message.subject } returns subject
        every { message.receivedDate } returns Date.from(timestamp.toInstant())
        every { message.from } returns arrayOf(address)
        every { message.size } returns 12345
        every { address.toString() } returns sender
        every { message.setFlag(any(), any()) } just runs
        return message
    }

    private fun mockFolder(name: String, parent: Folder? = null): Folder {
        val folder = mockk<Folder>()
        every { folder.parent } returns parent
        every { folder.name } returns name
        every { folder.fullName } returns name
        every { folder.open(any()) } just runs
        every { folder.close() } just runs
        every { folder.isOpen } returns true
        every { folder.copyMessages(any(), any()) } just runs

        return folder
    }
}
