package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.readConfigProperties
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.util.validate
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import org.junit.jupiter.api.Test

class MailTest {
    val configurationProperties = readConfigProperties()?.also { validate(it) } ?: ConfigurationProperties()

    @Test
    fun listFolders() {
        configurationProperties.accounts.forEach { (account, properties) ->
            println("Account: $account")
            MailConnector.connect(properties).use { mailAdapter ->
                mailAdapter.listFolders().forEach { folder ->
                    if (folder.parent != null) {
                        println("- ${folder.name} (${folder.fullName})")
                    }
                }
            }
        }
    }

    @Test
    fun listMails() {
        configurationProperties.accounts.forEach { (account, properties) ->
            println("Account: $account")
            MailConnector.connect(properties).use { mailAdapter ->

                mailAdapter.listFolders().forEach { folder ->
                    if (folder.parent != null) {
                        println("Folder: ${folder.name} (${folder.fullName})")
                        folder.open(Folder.READ_WRITE)

                        val messages = folder.messages

                        val fetchProfile = FetchProfile().apply {
                            add(FetchProfile.Item.ENVELOPE)
                            add(FetchProfile.Item.FLAGS)
                        }
                        folder.fetch(messages, fetchProfile)
                        messages.sortByDescending { it.receivedDate }

                        messages.forEach {
                            println("- ${it.receivedDate.toInstant()} | ${it.subject} | ${it.from.joinToString(",")}, ${it.size} bytes")
                        }
                    }
                }
            }
        }
    }
}
