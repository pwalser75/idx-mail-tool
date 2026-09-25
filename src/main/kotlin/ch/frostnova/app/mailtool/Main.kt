package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.readConfigProperties
import ch.frostnova.app.mailtool.connector.StandardConsole
import ch.frostnova.app.mailtool.connector.impl.MailConnectorImpl
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BLUE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BOLD
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_CYAN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GREEN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_RED
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_YELLOW
import ch.frostnova.app.mailtool.util.ansiFormat
import kotlin.system.exitProcess

fun main(vararg args: String) {
    printLogo()
    try {
        val configuration = readConfigProperties()
        val selectedCommand = if (configuration == null) {
            println("No configuration found, starting setup ...".ansiFormat(ANSI_YELLOW))
            Command.SETUP
        } else {
            if (args.size != 1) {
                printUsage()
                exitProcess(1)
            }
            val arg = args[0]
            command(arg) ?: throw IllegalArgumentException("Unknown command: $arg")
        }
        val connector = MailConnectorImpl()
        val console = StandardConsole()
        MailTool(connector, configuration ?: ConfigurationProperties(), console).run(selectedCommand)

    } catch (ex: Exception) {
        println("${ex.javaClass.simpleName.ansiFormat(ANSI_BOLD, ANSI_RED)} - ${ex.message?.ansiFormat(ANSI_RED)}")
        ex.printStackTrace()
        printUsage()
        exitProcess(1)
    }
}

private fun printLogo() {
    println("> IDX Mail Tool".trimIndent().ansiFormat(ANSI_BOLD, ANSI_BLUE))
}

private fun printUsage() {
    println("Usage:")
    println("java -jar idx-mail-tool.jar [command]".ansiFormat(ANSI_BOLD, ANSI_CYAN))
    println("\nCommands:")
    Command.entries.forEach { command ->
        println("- ${command.name.lowercase().ansiFormat(ANSI_BOLD, ANSI_GREEN)}: ${command.description}")
    }
    println()
}

