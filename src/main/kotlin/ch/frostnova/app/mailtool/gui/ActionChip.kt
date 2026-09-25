package ch.frostnova.app.mailtool.gui

import java.awt.Color
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

internal data class ChipPalette(val background: Color, val foreground: Color)

internal fun actionPalette(action: String?): ChipPalette = when (action) {
    "MOVE" -> ChipPalette(Color(0x243E5C), Color(0x8FC1FF))
    "COPY" -> ChipPalette(Color(0x4A3D1E), Color(0xE8C46A))
    "DELETE" -> ChipPalette(Color(0x4E2A2E), Color(0xF0A6AE))
    else -> ChipPalette(Theme.SURFACE_ALT, Theme.TEXT)
}

/**
 * Renders rule action values as small colored chips (MOVE / COPY / DELETE).
 */
class ActionChipRenderer : TableCellRenderer {

    private val chip = PillLabel()
    private val container = JPanel(GridBagLayout()).apply {
        isOpaque = true
        add(
            chip,
            GridBagConstraints().apply {
                gridx = 0
                weightx = 1.0
                fill = GridBagConstraints.NONE
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 8, 0, 8)
            }
        )
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val action = value?.toString()
        val palette = actionPalette(action)
        chip.text = action.orEmpty()
        chip.pillColor = palette.background
        chip.foreground = palette.foreground
        container.background = if (isSelected) table.selectionBackground else table.background
        return container
    }
}
