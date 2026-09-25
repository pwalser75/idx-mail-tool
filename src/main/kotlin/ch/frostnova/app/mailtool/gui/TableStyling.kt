package ch.frostnova.app.mailtool.gui

import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer

/**
 * Table cell renderer with row hover highlight, tooltips and right-alignment
 * for integer columns.
 */
open class TableRowRenderer : DefaultTableCellRenderer() {

    var hoverRow: Int = -1

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (component is JLabel) {
            component.background = when {
                isSelected -> table?.selectionBackground ?: Theme.ACCENT_DARK
                row == hoverRow -> Theme.HOVER
                else -> table?.background ?: Theme.SURFACE
            }
            component.foreground = if (isSelected) table?.selectionForeground ?: Theme.TEXT else Theme.TEXT
            component.border = EmptyBorder(0, 8, 0, 8)
            val text = displayText(value)
            component.text = text
            component.toolTipText = text
            val rightAligned = table != null && table.getColumnClass(column) == Int::class.javaObjectType
            component.horizontalAlignment = if (rightAligned) SwingConstants.RIGHT else SwingConstants.LEFT
        }
        return component
    }

    protected open fun displayText(value: Any?): String {
        if (value is Int && value < 0) return "\u2013"
        return value?.toString().orEmpty()
    }
}

/**
 * Installs row hover highlighting on the table.
 */
fun JTable.enableRowStyling() {
    val renderer = TableRowRenderer()
    setDefaultRenderer(Any::class.java, renderer)

    addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(event: MouseEvent) {
            val row = rowAtPoint(event.point)
            if (row != renderer.hoverRow) {
                renderer.hoverRow = row
                repaint()
            }
        }
    })
    addMouseListener(object : MouseAdapter() {
        override fun mouseExited(event: MouseEvent) {
            if (renderer.hoverRow != -1) {
                renderer.hoverRow = -1
                repaint()
            }
        }
    })
}
