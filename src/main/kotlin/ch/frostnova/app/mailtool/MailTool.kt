package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.Command.APPLY
import ch.frostnova.app.mailtool.Command.FOLDERS
import ch.frostnova.app.mailtool.Command.MAILS
import ch.frostnova.app.mailtool.Command.RULES
import ch.frostnova.app.mailtool.Command.SENDERS
import ch.frostnova.app.mailtool.Command.SETUP
import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_ORANGE
import ch.frostnova.app.mailtool.util.SetWithCount
import ch.frostnova.app.mailtool.util.add
import ch.frostnova.app.mailtool.util.ansiFormat
import ch.frostnova.app.mailtool.util.topItems
import ch.frostnova.app.mailtool.util.validate
import jakarta.mail.Address

class MailTool(val configuration: ConfigurationProperties) {

    init {
        validate(configuration)
    }

    fun run(command: Command) {
        if (configuration.accounts.isEmpty()) {
            println("No accounts configured yet".ansiFormat(ANSI_ORANGE))
        }

        when (command) {
            SETUP -> setup()
            FOLDERS -> listFolders()
            MAILS -> listMails()
            SENDERS -> listSenders()
            RULES -> listRules()
            APPLY -> applyRules()
        }
    }

    private fun setup() {

    }

    private fun listFolders() {
        configuration.accounts.forEach { (account, properties) ->
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

    private fun listMails() {
        configuration.accounts.forEach { (account, properties) ->
            println("Account: $account")
            MailConnector.connect(properties).use { mailAdapter ->
                mailAdapter.listFolders().forEach { folder ->
                    println("Folder: ${folder.name} (${folder.fullName})")
                    mailAdapter.listMessages(folder).forEach {
                        println("- ${it.receivedDate.toInstant()} | ${it.subject} | ${it.from.joinToString(",")}, ${it.size} bytes")
                    }
                }
            }
        }
    }

    private fun listSenders() {
        configuration.accounts.forEach { (account, properties) ->
            println("Account: $account")
            val senders = SetWithCount<Address>()
            MailConnector.connect(properties).use { mailAdapter ->
                mailAdapter.listFolders().forEach { folder ->
                    mailAdapter.listMessages(folder).forEach { message ->
                        message.from.forEach { sender -> senders.add(sender) }
                    }
                }
            }
            senders.topItems().forEach { (count, address) ->
                println("${count}x $address")
            }
        }
    }

    private fun listRules() {

    }

    private fun applyRules() {

    }
}