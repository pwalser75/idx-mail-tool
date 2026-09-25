package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableModel

class TableStylingTest {

    @Test
    fun `the hovered row is highlighted`() {
        val table = JTable(DefaultTableModel(arrayOf("Column"), 0)).apply { background = Theme.SURFACE }
        val renderer = TableRowRenderer().apply { hoverRow = 1 }

        val background = (renderer.getTableCellRendererComponent(table, "a", false, false, 1, 0) as JLabel).background

        assertThat(background).isEqualTo(Theme.HOVER)
    }

    @Test
    fun `non-hovered rows keep the table background`() {
        val table = JTable(DefaultTableModel(arrayOf("Column"), 0)).apply { background = Theme.SURFACE }
        val renderer = TableRowRenderer()

        val background = (renderer.getTableCellRendererComponent(table, "a", false, false, 1, 0) as JLabel).background

        assertThat(background).isEqualTo(Theme.SURFACE)
    }

    @Test
    fun `integer columns are right aligned`() {
        val model = object : DefaultTableModel(arrayOf("Column"), 0) {
            override fun getColumnClass(columnIndex: Int): Class<*> = Int::class.javaObjectType
        }
        val renderer = TableRowRenderer()

        val component = renderer.getTableCellRendererComponent(JTable(model), 5, false, false, 0, 0) as JLabel

        assertThat(component.horizontalAlignment).isEqualTo(SwingConstants.RIGHT)
    }

    @Test
    fun `a negative integer (unknown count) renders as a dash`() {
        val table = JTable(DefaultTableModel(arrayOf("Column"), 0))
        val renderer = TableRowRenderer()

        val component = renderer.getTableCellRendererComponent(table, -1, false, false, 0, 0) as JLabel

        assertThat(component.text).isEqualTo("\u2013")
    }
}
