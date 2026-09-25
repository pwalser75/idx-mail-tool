package ch.frostnova.app.mailtool

enum class Command(val description: String) {
    GUI("Open the GUI"),
    SETUP("Open the GUI in the setup section"),
    APPLY("Apply all rules"),
}

fun command(arg: String): Command? = Command.entries.firstOrNull { arg.equals(it.name, ignoreCase = true) }
