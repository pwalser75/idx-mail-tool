package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.gui.ActionChipRenderer
import ch.frostnova.app.mailtool.gui.IconType
import ch.frostnova.app.mailtool.gui.TableCard
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.enableRowStyling
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.dialog.RuleDialog
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
 * Mail sorting rules section: add, edit and remove rules for incoming messages.
 */
class RulesPanel(private val folderNames: () -> List<String>) : JPanel(BorderLayout()), SetupPanel {

    private val rules = mutableListOf<MailRule>()
    private val model = RulesTableModel()
    private val table = JTable(model)

    private val editButton = secondaryButton(I18n.t("rules.edit"), editIcon(Theme.TEXT)) { editRule() }
    private val removeButton = secondaryButton(I18n.t("rules.remove"), trashIcon(Theme.TEXT)) { removeRule() }

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
        table.columnModel.getColumn(1).preferredWidth = 90
        table.columnModel.getColumn(2).preferredWidth = 180
        table.columnModel.getColumn(1).cellRenderer = ActionChipRenderer()
        table.autoCreateRowSorter = true
        table.rowSorter.sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) editRule()
            }
        })
        table.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeRule")
        table.actionMap.put("removeRule", object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) = removeRule()
        })

        val toolbar = JPanel(FlowLayout(FlowLayout.LEADING, 10, 14)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 20, 0, 20)
            add(primaryButton(I18n.t("rules.add"), plusIcon(Theme.ON_ACCENT)) { addRule() })
            add(editButton)
            add(removeButton)
        }
        updateSelectionActions()
        table.selectionModel.addListSelectionListener { updateSelectionActions() }

        val tableCard = TableCard(
            table,
            IconType.FILTER,
            I18n.t("rules.empty"),
            I18n.t("rules.add")
        ) { addRule() }

        val body = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(toolbar, BorderLayout.NORTH)
            add(tableCard, BorderLayout.CENTER)
        }

        add(sectionHeader(I18n.t("rules.header"), I18n.t("rules.header.description")), BorderLayout.NORTH)
        add(card(body), BorderLayout.CENTER)
    }

    override fun load(account: AccountProperties) {
        rules.clear()
        rules.addAll(account.rules)
        refresh()
    }

    override fun readInto(account: AccountProperties) {
        account.rules = rules.toList()
    }

    fun mailRules(): List<MailRule> = rules.toList()

    /** Opens the editor to add a new rule (also triggered by Ctrl+N). */
    fun addNew() {
        addRule()
    }

    private fun addRule() {
        val dialog = RuleDialog(windowOwner(), null, folderNames)
        dialog.isVisible = true
        dialog.result?.let {
            rules.add(it)
            refresh(rules.size - 1)
        }
    }

    private fun editRule() {
        val viewRow = table.selectedRow
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("rules.edit.select"), I18n.t("rules.select.title"), JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val index = table.convertRowIndexToModel(viewRow)
        val dialog = RuleDialog(windowOwner(), rules[index], folderNames)
        dialog.isVisible = true
        dialog.result?.let {
            rules[index] = it
            refresh(index)
        }
    }

    private fun removeRule() {
        val viewRow = table.selectedRow
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, I18n.t("rules.remove.select"), I18n.t("rules.select.title"), JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val index = table.convertRowIndexToModel(viewRow)
        val confirm = JOptionPane.showConfirmDialog(
            this,
            I18n.t("rules.remove.confirm", rules[index].senders.joinToString(", ")),
            I18n.t("rules.remove.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (confirm == JOptionPane.YES_OPTION) {
            rules.removeAt(index)
            refresh()
        }
    }

    private fun refresh(selectRow: Int = -1) {
        model.setRules(rules)
        if (selectRow in rules.indices) {
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

private class RulesTableModel : AbstractTableModel() {

    private val columns = listOf(
        I18n.t("rules.column.senders"),
        I18n.t("rules.column.action"),
        I18n.t("rules.column.folder")
    )
    private var rows: List<MailRule> = emptyList()

    fun setRules(rules: List<MailRule>) {
        rows = rules.toList()
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]
    override fun isCellEditable(row: Int, column: Int): Boolean = false

    override fun getValueAt(row: Int, column: Int): Any {
        val rule = rows[row]
        return when (column) {
            0 -> rule.senders.joinToString(", ")
            1 -> rule.action?.name ?: ""
            else -> rule.folder.orEmpty()
        }
    }
}
