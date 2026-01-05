package ch.frostnova.app.mailtool

enum class Command(val description: String) {
    SETUP("Setup IMAP connector"),
    FOLDERS("List all folders"),
    MAILS("List all mails"),
    SENDERS("List all senders"),
    RULES("List all rules"),
    APPLY("Apply all rules"),
}

fun command(arg: String): Command? = Command.entries.firstOrNull { arg.equals(it.name, ignoreCase = true) }