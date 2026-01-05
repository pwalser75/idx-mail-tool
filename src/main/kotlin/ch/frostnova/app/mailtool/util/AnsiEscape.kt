package ch.frostnova.app.mailtool.util

import ch.frostnova.app.mailtool.util.AnsiEscapeCode.ANSI_RESET

enum class AnsiEscapeCode(code: String) {

    ANSI_RESET("0m"),
    ANSI_BOLD("1m"),
    ANSI_GRAY("38;5;244m"),

    ANSI_BLUE("38;5;75m"),
    ANSI_CYAN("38;5;79m"),
    ANSI_GREEN("38;5;70m"),
    ANSI_YELLOW("38;5;178m"),
    ANSI_ORANGE("38;5;208m"),
    ANSI_RED("38;5;196m"),

    CURSOR_START_LINE("1000D"),
    CLEAR_FROM_CURSOR("0K");

    val escapeSequence = "\u001b[$code"
}

fun String.ansiFormat(vararg style: AnsiEscapeCode): String {
    var builder = StringBuilder()
    style.forEach { builder.append(it.escapeSequence) }
    builder.append(this)
    builder.append(ANSI_RESET.escapeSequence)
    return builder.toString()
}