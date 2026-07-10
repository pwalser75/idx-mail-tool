package ch.frostnova.app.mailtool.connector

import jakarta.mail.Folder
import jakarta.mail.Message

interface MailAdapter : AutoCloseable {

    fun listFolders(): List<Folder>

    fun listMessages(folder: Folder): List<Message>
}