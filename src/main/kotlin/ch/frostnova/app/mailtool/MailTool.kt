package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.Command.APPLY
import ch.frostnova.app.mailtool.Command.FOLDERS
import ch.frostnova.app.mailtool.Command.MAILS
import ch.frostnova.app.mailtool.Command.RULES
import ch.frostnova.app.mailtool.Command.SENDERS
import ch.frostnova.app.mailtool.Command.SETUP
import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction.COPY
import ch.frostnova.app.mailtool.config.MailRuleAction.DELETE
import ch.frostnova.app.mailtool.config.MailRuleAction.MOVE
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GRAY
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_ORANGE
import ch.frostnova.app.mailtool.util.SetWithCount
import ch.frostnova.app.mailtool.util.add
import ch.frostnova.app.mailtool.util.ansiFormat
import ch.frostnova.app.mailtool.util.topItems
import ch.frostnova.app.mailtool.util.validate
import jakarta.mail.Address
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

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
                    folder.use {
                        folder.open(Folder.READ_ONLY)
                        mailAdapter.listMessages(folder).forEach {
                            println("- ${messageInfo(it)}")
                        }
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
                    folder.use {
                        folder.open(Folder.READ_ONLY)
                        mailAdapter.listMessages(folder).forEach { message ->
                            message.from.forEach { sender -> senders.add(sender) }
                        }
                    }
                }
            }
            senders.topItems().forEach { (count, address) ->
                println("${count}x $address")
            }
        }
    }

    private fun listRules() {
        configuration.accounts.forEach { (account, properties) ->
            println("Account: $account")
            if (properties.rules.isEmpty()) {
                println("- No rules configured yet".ansiFormat(ANSI_GRAY))
            } else {
                println("- Rules:")
                properties.rules.forEach { rule ->
                    val action = when (rule.action) {
                        MOVE -> "moved to folder \"${rule.folder}\""
                        COPY -> "copied to folder \"${rule.folder}\""
                        DELETE -> "deleted"
                        else -> "ignored"
                    }
                    println("  - Mails from sender ${rule.senders.joinToString(", ") { "\"$it\"" }} will be $action")
                }
            }
            if (properties.dataRetention.isEmpty()) {
                println("- No data retention rules configured yet".ansiFormat(ANSI_GRAY))
            } else {
                println("- Data retention rules:")
                properties.dataRetention.forEach { rule ->
                    println(
                        "  - Mails in folder \"${rule.folder}\" will be deleted after ${rule.retentionPeriod} (any before ${
                            (Instant.now().minus(rule.retentionPeriod!!.toDuration()).truncatedTo(SECONDS))
                        })"
                    )
                }
            }
        }
    }

    private fun applyRules() {
        configuration.accounts.forEach { (_, properties) ->
            if (properties.rules.isNotEmpty() || properties.dataRetention.isNotEmpty()) {
                MailConnector.connect(properties).use { mailAdapter ->
                    val folders = mailAdapter.listFolders()
                    folders.forEach { folder ->
                        folder.open(Folder.READ_WRITE)
                        val messages = mailAdapter.listMessages(folder)

                        // Apply message rules
                        messages.forEach { message ->
                            firstMatchingRule(properties, message)?.let { rule ->
                                val folder = rule.folder?.let { firstMatchingFolder(folders, it) }
                                when (rule.action) {
                                    MOVE -> {
                                        if (folder != message.folder) {
                                            println("> move ${messageInfo(message)} to folder \"${folder?.fullName}\"")
                                            message.folder.copyMessages(arrayOf(message), folder)
                                            message.setFlag(Flags.Flag.DELETED, true)
                                        }
                                    }

                                    COPY -> {
                                        if (folder != message.folder) {
                                            println("> copy ${messageInfo(message)} to folder \"${folder?.fullName}\"")
                                            message.folder.copyMessages(arrayOf(message), folder)
                                        }
                                    }

                                    DELETE -> {
                                        println("> delete ${messageInfo(message)}")
                                        message.setFlag(Flags.Flag.DELETED, true)
                                    }

                                    else -> {
                                        // noop
                                    }
                                }
                            }
                        }
                    }

                    // Apply retention policies

                    properties.dataRetention.forEach { dataRetention ->
                        val deleteBefore = Instant.now().minus(dataRetention.retentionPeriod!!.toDuration())
                        firstMatchingFolder(folders, dataRetention.folder!!)?.let { folder ->
                            mailAdapter.listMessages(folder).forEach { message ->
                                if (message.sentDate.toInstant().isBefore(deleteBefore)) {
                                    println("> delete ${messageInfo(message)} from folder \"${folder.fullName}\"")
                                    message.setFlag(Flags.Flag.DELETED, true)
                                }
                            }
                        }
                    }

                    // finally, close all folders
                    folders.forEach { if (it.isOpen) it.close() }
                }
            }
        }
        println("done.")
    }

    private fun firstMatchingRule(acountProperties: AccountProperties, message: Message): MailRule? =
        acountProperties.rules.firstOrNull { rule ->
            message.from.any { from ->
                rule.senders.any { sender -> from.toString().contains(sender, ignoreCase = true) }
            }
        }

    private fun firstMatchingFolder(folders: Collection<Folder>, name: String): Folder? =
        folders.firstOrNull { folder -> folder.name.contains(name, ignoreCase = true) }

    private fun messageInfo(message: Message) =
        "\"${message.subject}\" (on ${message.receivedDate.toInstant()} from ${message.from.joinToString(",")}, ${message.size} bytes)"
}