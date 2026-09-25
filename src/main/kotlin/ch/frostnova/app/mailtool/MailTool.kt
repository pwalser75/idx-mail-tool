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
import ch.frostnova.app.mailtool.config.writeConfigProperties
import ch.frostnova.app.mailtool.connector.Console
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.MailSetupWindow
import ch.frostnova.app.mailtool.util.AnsiEscapeCode
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BLUE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BOLD
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_CYAN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GRAY
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_ORANGE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_RED
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_YELLOW
import ch.frostnova.app.mailtool.util.SetWithCount
import ch.frostnova.app.mailtool.util.add
import ch.frostnova.app.mailtool.util.ansiFormat
import ch.frostnova.app.mailtool.util.topItems
import ch.frostnova.app.mailtool.util.validate
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

class MailTool(
    private val connector: MailConnector,
    private val configuration: ConfigurationProperties,
    private val console: Console
) {

    init {
        validate(configuration)
    }

    fun run(command: Command) {
        if (configuration.accounts.isEmpty()) {
            console.output("No accounts configured yet".ansiFormat(ANSI_ORANGE))
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
        MailSetupWindow.open(configuration, connector) { writeConfigProperties(it) }
    }

    private fun listFolders() {
        configuration.accounts.forEach { (account, properties) ->
            console.output("Account: $account")
            connector.connect(properties).use { mailAdapter ->
                mailAdapter.listFolders().forEach { folder ->
                    if (folder.parent != null) {
                        console.output("- ${folder.name} (${folder.fullName})")
                    }
                }
            }
        }
    }

    private fun listMails() {
        configuration.accounts.forEach { (account, properties) ->
            console.output("Account: $account")
            connector.connect(properties).use { mailAdapter ->
                mailAdapter.listFolders().forEach { folder ->
                    console.output("Folder: ${folder.name} (${folder.fullName})".ansiFormat(ANSI_BOLD, ANSI_BLUE))
                    folder.use {
                        folder.open(Folder.READ_ONLY)
                        mailAdapter.listMessages(folder).forEach { message ->
                            console.output("- ${message.formatSubject()}")
                            console.output("  ${message.formatDetails()}")
                        }
                    }
                }
            }
        }
    }

    private fun listSenders() {
        configuration.accounts.forEach { (account, properties) ->
            console.output("Account: $account")
            val senders = SetWithCount<String>()
            connector.connect(properties).use { mailAdapter ->
                mailAdapter.listFolders().forEach { folder ->
                    folder.use {
                        folder.open(Folder.READ_ONLY)
                        mailAdapter.listMessages(folder).forEach { message ->
                            message.from.forEach { sender -> senders.add(sender.toString()) }
                        }
                    }
                }
            }
            senders.topItems().forEach { (count, address) ->
                console.output("${count}x $address")
            }
        }
    }

    private fun listRules() {
        configuration.accounts.forEach { (account, properties) ->
            console.output("Account: $account")
            if (properties.rules.isEmpty()) {
                console.output("- No rules configured yet".ansiFormat(ANSI_GRAY))
            } else {
                console.output("- Rules:")
                properties.rules.forEach { rule ->
                    val action = when (rule.action) {
                        MOVE -> "moved to folder \"${rule.folder}\""
                        COPY -> "copied to folder \"${rule.folder}\""
                        DELETE -> "deleted"
                        else -> "ignored"
                    }
                    console.output("  - Mails from sender ${rule.senders.joinToString(", ") { "\"$it\"" }} will be $action")
                }
            }
            if (properties.dataRetention.isEmpty()) {
                console.output("- No data retention rules configured yet".ansiFormat(ANSI_GRAY))
            } else {
                console.output("- Data retention rules:")
                properties.dataRetention.forEach { rule ->
                    console.output(
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
                connector.connect(properties).use { mailAdapter ->
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
                                            console.output(
                                                "> move \"${message.subject}\" to folder \"${folder?.fullName}\"".ansiFormat(
                                                    ANSI_BOLD, ANSI_ORANGE
                                                )
                                            )
                                            console.output("  ${message.formatDetails()}")
                                            message.folder.copyMessages(arrayOf(message), folder)
                                            message.setFlag(Flags.Flag.DELETED, true)
                                        }
                                    }

                                    COPY -> {
                                        if (folder != message.folder) {
                                            console.output(
                                                "> copy \"${message.subject}\" to folder \"${folder?.fullName}\"".ansiFormat(
                                                    ANSI_BOLD, ANSI_YELLOW
                                                )
                                            )
                                            console.output("  ${message.formatDetails()}")
                                            message.folder.copyMessages(arrayOf(message), folder)
                                        }
                                    }

                                    DELETE -> {
                                        console.output(
                                            "> delete \"${message.subject}\"".ansiFormat(
                                                ANSI_BOLD, ANSI_RED
                                            )
                                        )
                                        console.output("  ${message.formatDetails()}")
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
                                    console.output(
                                        "> delete from folder \"${folder.fullName}\"".ansiFormat(
                                            ANSI_BOLD, ANSI_RED
                                        )
                                    )
                                    console.output("  ${message.formatSubject()}")
                                    console.output("  ${message.formatDetails()}")
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
        console.output("done.")
    }

    private fun firstMatchingRule(acountProperties: AccountProperties, message: Message): MailRule? =
        acountProperties.rules.firstOrNull { rule ->
            message.from.any { from ->
                rule.senders.any { sender -> from.toString().contains(sender, ignoreCase = true) }
            }
        }

    private fun firstMatchingFolder(folders: Collection<Folder>, name: String): Folder? =
        folders.firstOrNull { folder -> folder.name.contains(name, ignoreCase = true) }

    fun Message.formatSubject(color: AnsiEscapeCode = ANSI_CYAN) =
        (subject ?: "<no subject>").ansiFormat(ANSI_BOLD, color)

    fun Message.formatDetails() = "on ${receivedDate.toInstant()} from ".ansiFormat(ANSI_GRAY) +
            from.joinToString(",").ansiFormat(ANSI_BOLD) + " , $size bytes".ansiFormat(ANSI_GRAY)
}