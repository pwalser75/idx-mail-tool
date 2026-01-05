package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.config.readConfigProperties
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BLUE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BOLD
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_CYAN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GREEN
import ch.frostnova.app.mailtool.util.ansiFormat
import ch.frostnova.app.mailtool.util.validate


fun main(vararg args: String) {
    printLogo()
    printUsage()
    val configurationProperties = readConfigProperties()?.also { validate(it) }
    configurationProperties?.accounts?.forEach { (account, properties) ->
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

private fun printLogo() {
    println("---------------".ansiFormat(ANSI_BLUE))
    println(" Idx Mail Tool".ansiFormat(ANSI_BOLD, ANSI_BLUE))
    println("---------------".ansiFormat(ANSI_BOLD, ANSI_BLUE))
}

private fun printUsage() {
    println("Usage:")
    println("java -jar idx-mail-tool.jar [command]".ansiFormat(ANSI_BOLD, ANSI_CYAN))
    println("\nCommands:")
    val commands = mapOf(
        "setup" to "Setup IMAP connector",
        "folders" to "List all folders",
        "mails" to "List all mails",
        "rules" to "List all rules",
        "apply" to "Apply all rules (default)"
    )
    commands.forEach { (command, description) ->
        println("- ${command.ansiFormat(ANSI_BOLD, ANSI_GREEN)}: $description")
    }
    println()
}

