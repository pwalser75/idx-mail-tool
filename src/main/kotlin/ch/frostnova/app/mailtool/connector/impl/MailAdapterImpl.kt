package ch.frostnova.app.mailtool.connector.impl

import ch.frostnova.app.mailtool.connector.MailAdapter
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store

class MailAdapterImpl(private val store: Store) : MailAdapter, AutoCloseable by store {

    override fun listFolders(): List<Folder> {
        val result = mutableListOf<Folder>()
        traverse(store.defaultFolder) { folder ->
            if (folder.parent != null) {
                result.add(folder)
            }
        }
        return result
    }

    override fun listMessages(folder: Folder): List<Message> {
        if (folder.parent == null) return emptyList()

        val messages = folder.messages

        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
        }
        folder.fetch(messages, fetchProfile)
        return messages.toList().filter { message ->
            try {
                message.sentDate
                true
            } catch (ex: Exception) {
                println(
                    "Failed to fetch message: ${message.messageNumber} from folder ${message.folder.fullName}: ${ex.javaClass.simpleName}: ${ex.message}"
                )
                false
            }
        }.toMutableList().also { list ->
            list.sortByDescending { it.sentDate }
        }
    }

    private fun traverse(folder: Folder, consumer: (Folder) -> Unit) {
        consumer(folder)
        folder.list().forEach { traverse(it, consumer) }
    }
}