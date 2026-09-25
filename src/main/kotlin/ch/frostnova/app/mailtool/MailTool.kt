package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.Command.APPLY
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
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BOLD
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_CYAN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GRAY
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_ORANGE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_RED
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_YELLOW
import ch.frostnova.app.mailtool.util.ansiFormat
import ch.frostnova.app.mailtool.util.validate
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import java.time.Instant

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
            APPLY -> applyRules()
        }
    }

    private fun setup() {
        MailSetupWindow.open(configuration, connector) { writeConfigProperties(it) }
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
