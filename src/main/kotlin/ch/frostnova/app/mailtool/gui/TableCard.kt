package ch.frostnova.app.mailtool.gui

import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable

/**
 * Hosts a table and shows a friendly empty state while the table has no rows.
 */
class TableCard(
    private val table: JTable,
    emptyIcon: IconType,
    emptyMessage: String,
    actionText: String? = null,
    action: (() -> Unit)? = null
) : JPanel(java.awt.CardLayout()) {

    private var showingEmpty = true

    init {
        background = Theme.SURFACE

        val scrollPane = JScrollPane(table).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = Theme.SURFACE
        }
        add(scrollPane, TABLE)
        add(emptyState(emptyIcon, emptyMessage, actionText, action), EMPTY)

        table.model.addTableModelListener { updateState() }
        table.rowSorter?.addRowSorterListener { updateState() }
        updateState()
    }

    fun updateState() {
        showingEmpty = table.rowCount == 0
        (layout as java.awt.CardLayout).show(this, if (showingEmpty) EMPTY else TABLE)
    }

    internal fun isShowingEmptyState(): Boolean = showingEmpty

    private companion object {

        const val TABLE = "table"
        const val EMPTY = "empty"

        fun emptyState(
            icon: IconType,
            message: String,
            actionText: String?,
            action: (() -> Unit)?
        ): JPanel {
            val content = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
            }
            val iconLabel = JLabel(VectorIcon(icon, Theme.FAINT, 44)).apply {
                alignmentX = CENTER_ALIGNMENT
            }
            val messageLabel = JLabel(message).apply {
                font = Theme.SECTION_FONT
                foreground = Theme.MUTED
                alignmentX = CENTER_ALIGNMENT
            }
            content.add(iconLabel)
            content.add(Box.createVerticalStrut(12))
            content.add(messageLabel)
            if (actionText != null && action != null) {
                val button = primaryButton(actionText) { action() }
                button.alignmentX = CENTER_ALIGNMENT
                content.add(Box.createVerticalStrut(16))
                content.add(button)
            }
            return JPanel(GridBagLayout()).apply {
                background = Theme.SURFACE
                add(content)
            }
        }
    }
}
