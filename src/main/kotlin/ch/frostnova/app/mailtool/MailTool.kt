package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.Command.APPLY
import ch.frostnova.app.mailtool.Command.GUI
import ch.frostnova.app.mailtool.Command.SETUP
import ch.frostnova.app.mailtool.apply.ActionOrigin
import ch.frostnova.app.mailtool.apply.ActionType
import ch.frostnova.app.mailtool.apply.MailAction
import ch.frostnova.app.mailtool.apply.RuleApplier
import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.writeConfigProperties
import ch.frostnova.app.mailtool.connector.Console
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.MailSetupWindow
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BOLD
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GRAY
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_ORANGE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_RED
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_YELLOW
import ch.frostnova.app.mailtool.util.ansiFormat
import ch.frostnova.app.mailtool.util.validate

class MailTool(
    private val connector: MailConnector,
    private val configuration: ConfigurationProperties,
    private val console: Console
) {

    fun run(command: Command) {
        if (configuration.accounts.isEmpty()) {
            console.output("No accounts configured yet".ansiFormat(ANSI_ORANGE))
        }

        when (command) {
            GUI -> openGui()
            SETUP -> setup()
            APPLY -> applyRules()
        }
    }

    private fun openGui() {
        MailSetupWindow.open(configuration, connector) { writeConfigProperties(it) }
    }

    private fun setup() {
        MailSetupWindow.open(configuration, connector, openSetup = true) { writeConfigProperties(it) }
    }

    private fun applyRules() {
        validate(configuration)
        configuration.accounts.forEach { (_, properties) ->
            if (properties.rules.isNotEmpty() || properties.dataRetention.isNotEmpty()) {
                connector.connect(properties).use { adapter ->
                    RuleApplier(adapter, properties.rules, properties.dataRetention)
                        .run(dryRun = false) { action -> printAction(action) }
                }
            }
        }
        console.output("done.")
    }

    private fun printAction(action: MailAction) {
        val subject = action.subject ?: "<no subject>"
        val details = "on ${action.date} from ${action.sender} , ${action.size} bytes"
        when (action.type) {
            ActionType.MOVE -> {
                console.output("> move \"$subject\" to folder \"${action.targetFolder}\"".ansiFormat(ANSI_BOLD, ANSI_ORANGE))
                console.output("  $details".ansiFormat(ANSI_GRAY))
            }

            ActionType.COPY -> {
                console.output("> copy \"$subject\" to folder \"${action.targetFolder}\"".ansiFormat(ANSI_BOLD, ANSI_YELLOW))
                console.output("  $details".ansiFormat(ANSI_GRAY))
            }

            ActionType.DELETE -> if (action.origin == ActionOrigin.RETENTION) {
                console.output("> delete from folder \"${action.sourceFolder}\"".ansiFormat(ANSI_BOLD, ANSI_RED))
                console.output("  $subject".ansiFormat(ANSI_GRAY))
                console.output("  $details".ansiFormat(ANSI_GRAY))
            } else {
                console.output("> delete \"$subject\"".ansiFormat(ANSI_BOLD, ANSI_RED))
                console.output("  $details".ansiFormat(ANSI_GRAY))
            }
        }
    }
}
