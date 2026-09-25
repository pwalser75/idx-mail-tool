package ch.frostnova.app.mailtool

enum class Command(val description: String) {
    SETUP("Setup IMAP connector"),
    APPLY("Apply all rules"),
}

fun command(arg: String): Command? = Command.entries.firstOrNull { arg.equals(it.name, ignoreCase = true) }
