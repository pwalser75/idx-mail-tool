package ch.frostnova.app.mailtool

import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.config.readConfigProperties
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BLUE
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_BOLD
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_CYAN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_GREEN
import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_RED
import ch.frostnova.app.mailtool.util.ansiFormat
import kotlin.system.exitProcess

fun main(vararg args: String) {
    printLogo()
    if (args.size != 1) {
        printUsage()
        exitProcess(1)
    }
    try {
        val arg = args[0]
        val command = command(arg) ?: throw IllegalArgumentException("Unknown command: $arg")
        val configuration = readConfigProperties() ?: ConfigurationProperties()
        MailTool(configuration).run(command)

    } catch (ex: Exception) {
        println("${ex.javaClass.simpleName.ansiFormat(ANSI_BOLD, ANSI_RED)} - ${ex.message?.ansiFormat(ANSI_RED)}")
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

