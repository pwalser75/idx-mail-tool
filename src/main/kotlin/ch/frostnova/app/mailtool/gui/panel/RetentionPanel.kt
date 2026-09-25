package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.TooltipCellRenderer
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.dialog.RetentionDialog
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.gui.sectionHeader
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
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel

/**
 * Data retention section: define after how long messages in a folder may be deleted.
 */
class RetentionPanel : JPanel(BorderLayout()), SetupPanel {

    private val settings = mutableListOf<DataRetentionSettings>()
    private val model = RetentionTableModel()
    private val table = JTable(model)

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
            setDefaultRenderer(Any::class.java, TooltipCellRenderer())
            border = BorderFactory.createEmptyBorder()
        }
        table.columnModel.getColumn(0).preferredWidth = 320
        table.columnModel.getColumn(1).preferredWidth = 180
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
            add(primaryButton("Add retention rule") { addSetting() })
            add(secondaryButton("Edit") { editSetting() })
            add(secondaryButton("Remove") { removeSetting() })
        }

        val scrollPane = JScrollPane(table).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = Theme.SURFACE
        }

        val body = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(toolbar, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }

        add(
            sectionHeader("Data retention", "Automatically delete messages from folders after a given period."),
            BorderLayout.NORTH
        )
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

    private fun addSetting() {
        val dialog = RetentionDialog(windowOwner(), null)
        dialog.isVisible = true
        dialog.result?.let {
            settings.add(it)
            refresh(settings.size - 1)
        }
    }

    private fun editSetting() {
        val index = table.selectedRow
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a retention rule to edit.", "No rule selected", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val dialog = RetentionDialog(windowOwner(), settings[index])
        dialog.isVisible = true
        dialog.result?.let {
            settings[index] = it
            refresh(index)
        }
    }

    private fun removeSetting() {
        val index = table.selectedRow
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a retention rule to remove.", "No rule selected", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove the retention rule for folder \"${settings[index].folder}\"?",
            "Remove rule",
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
            table.setRowSelectionInterval(selectRow, selectRow)
        }
    }

    private fun windowOwner() = SwingUtilities.getWindowAncestor(this)
}

private class RetentionTableModel : AbstractTableModel() {

    private val columns = listOf("Folder", "Retention period")
    private var rows: List<DataRetentionSettings> = emptyList()

    fun setSettings(settings: List<DataRetentionSettings>) {
        rows = settings.toList()
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]
    override fun isCellEditable(row: Int, column: Int): Boolean = false

    override fun getValueAt(row: Int, column: Int): Any {
        val setting = rows[row]
        return when (column) {
            0 -> setting.folder.orEmpty()
            else -> formatRetention(setting)
        }
    }

    private fun formatRetention(setting: DataRetentionSettings): String {
        val period = setting.retentionPeriod ?: return ""
        return if (period.hours == 0 && period.minutes == 0 && period.seconds == 0) {
            "${period.days} ${if (period.days == 1) "day" else "days"}"
        } else {
            period.toString()
        }
    }
}
