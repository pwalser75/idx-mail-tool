package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.readConfigProperties
import ch.frostnova.app.mailtool.util.validate
import org.junit.jupiter.api.Test

class MailToolTest {
    private val configurationProperties = readConfigProperties()?.also { validate(it) } ?: ConfigurationProperties()
    private val mailTool = MailTool(configurationProperties)

    @Test
    fun listFolders() {
        mailTool.run(Command.FOLDERS)
    }

    @Test
    fun listMails() {
        mailTool.run(Command.MAILS)
    }

    @Test
    fun listSenders() {
        mailTool.run(Command.SENDERS)
    }

    @Test
    fun listRules() {
        mailTool.run(Command.RULES)
    }
}
