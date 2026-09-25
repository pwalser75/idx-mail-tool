package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.TooltipCellRenderer
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.dialog.RuleDialog
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.gui.sectionHeader
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyEvent
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
 * Mail sorting rules section: add, edit and remove rules for incoming messages.
 */
class RulesPanel : JPanel(BorderLayout()), SetupPanel {

    private val rules = mutableListOf<MailRule>()
    private val model = RulesTableModel()
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
        table.columnModel.getColumn(1).preferredWidth = 90
        table.columnModel.getColumn(2).preferredWidth = 180
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) {
                if (event.clickCount == 2) editRule()
            }
        })
        table.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeRule")
        table.actionMap.put("removeRule", object : AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent) = removeRule()
        })

        val toolbar = JPanel(FlowLayout(FlowLayout.LEADING, 10, 14)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 20, 0, 20)
            add(primaryButton("Add rule") { addRule() })
            add(secondaryButton("Edit") { editRule() })
            add(secondaryButton("Remove") { removeRule() })
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
            sectionHeader("Mail rules", "Sort incoming messages into folders based on the sender address."),
            BorderLayout.NORTH
        )
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

    private fun addRule() {
        val dialog = RuleDialog(windowOwner(), null)
        dialog.isVisible = true
        dialog.result?.let {
            rules.add(it)
            refresh(rules.size - 1)
        }
    }

    private fun editRule() {
        val index = table.selectedRow
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a rule to edit.", "No rule selected", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val dialog = RuleDialog(windowOwner(), rules[index])
        dialog.isVisible = true
        dialog.result?.let {
            rules[index] = it
            refresh(index)
        }
    }

    private fun removeRule() {
        val index = table.selectedRow
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a rule to remove.", "No rule selected", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove this rule?\n${rules[index].senders.joinToString(", ")}",
            "Remove rule",
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
            table.setRowSelectionInterval(selectRow, selectRow)
        }
    }

    private fun windowOwner() = SwingUtilities.getWindowAncestor(this)
}

private class RulesTableModel : AbstractTableModel() {

    private val columns = listOf("Senders", "Action", "Target folder")
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
