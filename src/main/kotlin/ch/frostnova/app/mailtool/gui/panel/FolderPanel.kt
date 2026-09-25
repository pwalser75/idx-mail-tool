package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.FolderNamesProvider
import ch.frostnova.app.mailtool.gui.IconType
import ch.frostnova.app.mailtool.gui.RetentionValue
import ch.frostnova.app.mailtool.gui.TableCard
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.retentionLabel
import ch.frostnova.app.mailtool.gui.sectionHeader
import ch.frostnova.app.mailtool.gui.statusLabel
import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.validate
import jakarta.mail.Folder
import jakarta.validation.ValidationException
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.SwingWorker
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel

private data class FolderCount(val name: String, val fullName: String, val messageCount: Int?)

private data class FolderRow(
    val name: String,
    val fullName: String,
    val messageCount: Int?,
    val retention: RetentionValue,
    val rules: String
) {
    val displayName: String get() = if (name == fullName) name else "$name ($fullName)"
}

/**
 * Folders section: shows the folders available on the server together with their
 * message count, the configured retention and the rules targeting them.
 */
class FolderPanel(
    private val connector: MailConnector,
    private val connectionSupplier: () -> AccountProperties,
    private val retentionSupplier: () -> List<DataRetentionSettings>,
    private val rulesSupplier: () -> List<MailRule>,
    private val folderNamesProvider: FolderNamesProvider
) : JPanel(BorderLayout()), SetupPanel {

    private val model = FoldersTableModel()
    private val table = JTable(model)
    private val statusLabel = statusLabel()
    private val refreshButton = primaryButton(I18n.t("folders.refresh")) { refresh() }

    private var loaded = false
    private var loading = false

    init {
        background = Theme.BACKGROUND
        border = EmptyBorder(28, 32, 28, 32)

        table.apply {
            font = Theme.LABEL_FONT
            foreground = Theme.TEXT
            background = Theme.SURFACE
            selectionBackground = Theme.ACCENT_DARK
            selectionForeground = Theme.TEXT
            gridColor = Theme.BORDER
            rowHeight = 30
            fillsViewportHeight = true
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            setDefaultRenderer(Any::class.java, FoldersCellRenderer())
            border = BorderFactory.createEmptyBorder()
        }
        table.columnModel.getColumn(0).preferredWidth = 300
        table.columnModel.getColumn(1).preferredWidth = 100
        table.columnModel.getColumn(2).preferredWidth = 150
        table.columnModel.getColumn(3).preferredWidth = 320
        table.autoCreateRowSorter = true
        table.rowSorter.sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))

        val toolbar = JPanel(FlowLayout(FlowLayout.LEADING, 12, 14)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 20, 0, 20)
            add(refreshButton)
            add(statusLabel)
        }

        val tableCard = TableCard(
            table,
            IconType.FOLDER,
            I18n.t("folders.empty"),
            I18n.t("folders.refresh")
        ) { refresh() }

        val body = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(toolbar, BorderLayout.NORTH)
            add(tableCard, BorderLayout.CENTER)
        }

        add(sectionHeader(I18n.t("folders.header"), I18n.t("folders.header.description")), BorderLayout.NORTH)
        add(card(body), BorderLayout.CENTER)
    }

    override fun load(account: AccountProperties) {
        // not a configuration editor
    }

    override fun readInto(account: AccountProperties) {
        // not a configuration editor
    }

    /** Loads the folders once, when the section is shown for the first time. */
    fun ensureLoaded() {
        if (!loaded && !loading) {
            refresh()
        }
    }

    fun refresh() {
        if (loading) return
        val properties = connectionSupplier()
        try {
            validate(properties)
        } catch (ex: ValidationException) {
            JOptionPane.showMessageDialog(
                this,
                ex.message,
                I18n.t("connection.invalid.title"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        loading = true
        refreshButton.isEnabled = false
        statusLabel.foreground = Theme.MUTED
        statusLabel.text = I18n.t("folders.loading")

        object : SwingWorker<List<FolderCount>, Void>() {
            override fun doInBackground(): List<FolderCount> = fetch(properties)

            override fun done() {
                loading = false
                refreshButton.isEnabled = true
                try {
                    val counts = get()
                    applyCounts(counts)
                    statusLabel.foreground = Theme.SUCCESS
                    statusLabel.text = I18n.t("folders.loaded", counts.size)
                } catch (ex: Exception) {
                    val cause = ex.cause ?: ex
                    statusLabel.foreground = Theme.DANGER
                    statusLabel.text = I18n.t("folders.failed", cause.message ?: cause.toString())
                    JOptionPane.showMessageDialog(
                        this@FolderPanel,
                        cause.message ?: cause.toString(),
                        I18n.t("folders.failed.title"),
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.execute()
    }

    /**
     * Quietly loads the folder names in the background (e.g. on startup) so that
     * the folder drop-downs in the rule editors are populated. Errors are ignored.
     */
    fun preload() {
        if (loaded || loading) return
        val properties = connectionSupplier()
        try {
            validate(properties)
        } catch (ex: ValidationException) {
            return
        }
        loading = true
        object : SwingWorker<List<FolderCount>, Void>() {
            override fun doInBackground(): List<FolderCount> = fetch(properties)

            override fun done() {
                loading = false
                runCatching { get() }.getOrNull()?.let { applyCounts(it) }
            }
        }.execute()
    }

    private fun fetch(properties: AccountProperties): List<FolderCount> =
        connector.connect(properties).use { adapter ->
            adapter.listFolders().map { folder -> countMessages(folder) }
        }

    private fun applyCounts(counts: List<FolderCount>) {
        folderNamesProvider.update(counts.map { it.name })
        model.setFolders(counts.map { toRow(it) })
        loaded = true
    }

    private fun countMessages(folder: Folder): FolderCount {
        val count = try {
            if (folder.parent != null) {
                folder.open(Folder.READ_ONLY)
                val messages = folder.messageCount
                folder.close(false)
                messages
            } else {
                null
            }
        } catch (ex: Exception) {
            try {
                if (folder.isOpen) folder.close(false)
            } catch (ignored: Exception) {
                // ignore
            }
            null
        }
        return FolderCount(folder.name, folder.fullName, count)
    }

    private fun toRow(folder: FolderCount): FolderRow {
        val retention = retentionValueFor(folder.name, retentionSupplier())
        val rules = rulesFor(folder.name, rulesSupplier())
        return FolderRow(folder.name, folder.fullName, folder.messageCount, retention, rules)
    }
}

internal fun retentionValueFor(folderName: String, settings: List<DataRetentionSettings>): RetentionValue {
    val periods = settings.filter { matchesFolder(folderName, it.folder) }.mapNotNull { it.retentionPeriod }
    val label = periods.joinToString(", ") { retentionLabel(it) }
    val seconds = periods.minOfOrNull { it.toDuration().seconds } ?: Long.MIN_VALUE
    return RetentionValue(label, seconds)
}

internal fun retentionFor(
    folderName: String,
    settings: List<DataRetentionSettings>
): String = retentionValueFor(folderName, settings).toString()

internal fun rulesFor(
    folderName: String,
    rules: List<MailRule>
): String = rules
    .filter { it.action == MailRuleAction.MOVE || it.action == MailRuleAction.COPY }
    .filter { matchesFolder(folderName, it.folder) }
    .joinToString("; ") { rule -> "${rule.action?.name.orEmpty()}: ${rule.senders.joinToString(", ")}" }

/**
 * Matches a configured folder reference against an actual folder name, the same
 * way rule application does: case-insensitive substring match, no full match.
 */
internal fun matchesFolder(folderName: String, target: String?): Boolean {
    if (target.isNullOrBlank()) return false
    return folderName.contains(target, ignoreCase = true)
}

private class FoldersTableModel : AbstractTableModel() {

    private val columns = listOf(
        I18n.t("folders.column.folder"),
        I18n.t("folders.column.messages"),
        I18n.t("folders.column.retention"),
        I18n.t("folders.column.rules")
    )
    private var rows: List<FolderRow> = emptyList()

    fun setFolders(folders: List<FolderRow>) {
        rows = folders
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]
    override fun isCellEditable(row: Int, column: Int): Boolean = false

    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        1 -> Int::class.javaObjectType
        2 -> RetentionValue::class.java
        else -> String::class.java
    }

    override fun getValueAt(row: Int, column: Int): Any {
        val folder = rows[row]
        return when (column) {
            0 -> folder.displayName
            1 -> folder.messageCount ?: UNKNOWN_COUNT
            2 -> folder.retention
            else -> folder.rules
        }
    }

    companion object {
        /** Sentinel for folders whose message count is not available. */
        const val UNKNOWN_COUNT = -1
    }
}

private class FoldersCellRenderer : javax.swing.table.DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): java.awt.Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (component is javax.swing.JLabel) {
            val display = if (column == 1 && value is Int && value < 0) "\u2013" else value?.toString().orEmpty()
            component.text = display
            component.toolTipText = display
            component.border = EmptyBorder(0, 8, 0, 8)
            component.horizontalAlignment =
                if (column == 1) javax.swing.SwingConstants.RIGHT else javax.swing.SwingConstants.LEFT
        }
        return component
    }
}
