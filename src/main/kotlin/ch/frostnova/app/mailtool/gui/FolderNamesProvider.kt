package ch.frostnova.app.mailtool.gui

/**
 * Shared, mutable holder for the folder names discovered over the connection.
 * The folders section updates it; the rule/retention editors read it to offer
 * existing folders while still allowing free-text folder references.
 */
class FolderNamesProvider {

    @Volatile
    var names: List<String> = emptyList()
        private set

    fun update(names: Collection<String>) {
        this.names = names.filter { it.isNotBlank() }.distinct().sorted()
    }
}
