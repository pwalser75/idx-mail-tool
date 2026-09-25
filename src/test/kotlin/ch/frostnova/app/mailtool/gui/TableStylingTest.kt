package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableModel

class TableStylingTest {

    @Test
    fun `zebra striping alternates the row background`() {
        val table = JTable(DefaultTableModel(arrayOf("Column"), 0)).apply { background = Theme.SURFACE }
        val renderer = StripedCellRenderer()

        val row0 = (renderer.getTableCellRendererComponent(table, "a", false, false, 0, 0) as JLabel).background
        val row1 = (renderer.getTableCellRendererComponent(table, "a", false, false, 1, 0) as JLabel).background

        assertThat(row0).isEqualTo(Theme.SURFACE)
        assertThat(row1).isEqualTo(Theme.SURFACE_ALT)
    }

    @Test
    fun `the hovered row is highlighted`() {
        val table = JTable(DefaultTableModel(arrayOf("Column"), 0)).apply { background = Theme.SURFACE }
        val renderer = StripedCellRenderer().apply { hoverRow = 1 }

        val background = (renderer.getTableCellRendererComponent(table, "a", false, false, 1, 0) as JLabel).background

        assertThat(background).isEqualTo(Theme.HOVER)
    }

    @Test
    fun `integer columns are right aligned`() {
        val model = object : DefaultTableModel(arrayOf("Column"), 0) {
            override fun getColumnClass(columnIndex: Int): Class<*> = Int::class.javaObjectType
        }
        val renderer = StripedCellRenderer()

        val component = renderer.getTableCellRendererComponent(JTable(model), 5, false, false, 0, 0) as JLabel

        assertThat(component.horizontalAlignment).isEqualTo(SwingConstants.RIGHT)
    }

    @Test
    fun `a negative integer (unknown count) renders as a dash`() {
        val table = JTable(DefaultTableModel(arrayOf("Column"), 0))
        val renderer = StripedCellRenderer()

        val component = renderer.getTableCellRendererComponent(table, -1, false, false, 0, 0) as JLabel

        assertThat(component.text).isEqualTo("\u2013")
    }
}
