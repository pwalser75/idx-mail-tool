package ch.frostnova.app.mailtool.connector

interface Console {

    fun output(message: String)
}

class StandardConsole : Console {
    override fun output(message: String) {
        println(message)
    }
}