package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.gui.IconType
import ch.frostnova.app.mailtool.gui.RetentionValue
import ch.frostnova.app.mailtool.gui.TableCard
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.enableRowStyling
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.dialog.RetentionDialog
import ch.frostnova.app.mailtool.gui.editIcon
import ch.frostnova.app.mailtool.gui.plusIcon
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.gui.sectionHeader
import ch.frostnova.app.mailtool.gui.trashIcon
import ch.frostnova.app.mailtool.i18n.I18n
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel

/**
 * Data retention section: define after how long messages in a folder may be deleted.
 */
class RetentionPanel(private val folderNames: () -> List<String>) : JPanel(BorderLayout()), SetupPanel {

    private val settings = mutableListOf<DataRetentionSettings>()
    private val model = RetentionTableModel()
    private val table = JTable(model)

    private val editButton = secondaryButton(I18n.t("retention.edit"), editIcon(Theme.TEXT)) { editSetting() }
    private val removeButton = secondaryButton(I18n.t("retention.remove"), trashIcon(Theme.TEXT)) { removeSetting() }

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
            border = BorderFactory.createEmptyBorder()
        }
        table.enableRowStyling()
        table.columnModel.getColumn(0).preferredWidth = 320
        table.columnModel.getColumn(1).preferredWidth = 180
        table.autoCreateRowSorter = true
        table.rowSorter.sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editSetting()
            }
        })
        table.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeSetting")
        table.actionMap.put("removeSetting", object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) = removeSetting()
        })

        val toolbar = JPanel(FlowLayout(FlowLayout.LEADING, 10, 14)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 20, 0, 20)
            add(primaryButton(I18n.t("retention.add"), plusIcon(Theme.ON_ACCENT)) { addSetting() })
            add(editButton)
            add(removeButton)
        }
        updateSelectionActions()
        table.selectionModel.addListSelectionListener { updateSelectionActions() }

        val tableCard = TableCard(
            table,
            IconType.CLOCK,
            I18n.t("retention.empty"),
            I18n.t("retention.add")
        ) { addSetting() }

        val body = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(toolbar, BorderLayout.NORTH)
            add(tableCard, BorderLayout.CENTER)
        }

        add(sectionHeader(I18n.t("retention.header"), I18n.t("retention.header.description")), BorderLayout.NORTH)
        add(card(body), BorderLayout.CENTER)
    }

    override fun load(account: AccountProperties) {
        settings.clear()
        settings.addAll(account.dataRetention)
        refresh()
    }

    override fun readInto(account: AccountProperties) {
        account.dataRetention = settings.toList()
    }

    fun retentionSettings(): List<DataRetentionSettings> = settings.toList()

    /** Opens the editor to add a new retention rule (also triggered by Ctrl+N). */
    fun addNew() {
        addSetting()
    }

    private fun addSetting() {
        val dialog = RetentionDialog(windowOwner(), null, folderNames)
        dialog.isVisible = true
        dialog.result?.let {
            settings.add(it)
            refresh(settings.size - 1)
        }
    }

    private fun editSetting() {
        val viewRow = table.selectedRow
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("retention.edit.select"), I18n.t("retention.select.title"), JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val index = table.convertRowIndexToModel(viewRow)
        val dialog = RetentionDialog(windowOwner(), settings[index], folderNames)
        dialog.isVisible = true
        dialog.result?.let {
            settings[index] = it
            refresh(index)
        }
    }

    private fun removeSetting() {
        val viewRow = table.selectedRow
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("retention.remove.select"), I18n.t("retention.select.title"), JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val index = table.convertRowIndexToModel(viewRow)
        val confirm = JOptionPane.showConfirmDialog(
            this,
            I18n.t("retention.remove.confirm", settings[index].folder.orEmpty()),
            I18n.t("retention.remove.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (confirm == JOptionPane.YES_OPTION) {
            settings.removeAt(index)
            refresh()
        }
    }

    private fun refresh(selectRow: Int = -1) {
        model.setSettings(settings)
        if (selectRow in settings.indices) {
            val viewRow = table.convertRowIndexToView(selectRow)
            if (viewRow >= 0) {
                table.setRowSelectionInterval(viewRow, viewRow)
            }
        }
    }

    private fun updateSelectionActions() {
        val hasSelection = table.selectedRow >= 0
        editButton.isEnabled = hasSelection
        removeButton.isEnabled = hasSelection
    }

    private fun windowOwner() = SwingUtilities.getWindowAncestor(this)
}

private class RetentionTableModel : AbstractTableModel() {

    private val columns = listOf(
        I18n.t("retention.column.folder"),
        I18n.t("retention.column.period")
    )
    private var rows: List<DataRetentionSettings> = emptyList()

    fun setSettings(settings: List<DataRetentionSettings>) {
        rows = settings.toList()
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]
    override fun isCellEditable(row: Int, column: Int): Boolean = false

    override fun getColumnClass(columnIndex: Int): Class<*> =
        if (columnIndex == 1) RetentionValue::class.java else String::class.java

    override fun getValueAt(row: Int, column: Int): Any {
        val setting = rows[row]
        return when (column) {
            0 -> setting.folder.orEmpty()
            else -> RetentionValue(setting.retentionPeriod)
        }
    }
}
