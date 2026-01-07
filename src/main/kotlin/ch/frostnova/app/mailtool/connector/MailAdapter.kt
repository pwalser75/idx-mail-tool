package ch.frostnova.app.mailtool.connector

import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store

class MailAdapter(private val store: Store) : AutoCloseable by store {

    fun listFolders(): List<Folder> {
        val result = mutableListOf<Folder>()
        traverse(store.defaultFolder) { folder ->
            if (folder.parent != null) {
                result.add(folder)
            }
        }
        return result
    }

    fun listMessages(folder: Folder): List<Message> {
        if (folder.parent == null) return emptyList()

        val messages = folder.messages

        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
        }
        folder.fetch(messages, fetchProfile)
        messages.sortByDescending { it.receivedDate }
        return messages.toList()
    }

    fun traverse(folder: Folder, consumer: (Folder) -> Unit) {
        consumer(folder)
        folder.list().forEach { traverse(it, consumer) }
    }
}