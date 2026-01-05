package ch.frostnova.app.mailtool

import jakarta.mail.Folder
import jakarta.mail.Store

class MailAdapter(private val store: Store) : AutoCloseable by store {

    fun listFolders(): List<Folder> {
        val result = mutableListOf<Folder>()
        traverse(store.defaultFolder) { folder ->
            result.add(folder)
        }
        return result
    }

    fun traverse(folder: Folder, consumer: (Folder) -> Unit) {
        consumer(folder)
        folder.list().forEach { traverse(it, consumer) }
    }
}