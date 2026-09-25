package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class TableCardTest {

    @Test
    fun `shows the empty state until rows are added`() {
        val model = DefaultTableModel(arrayOf("Column"), 0)
        val card = TableCard(JTable(model), IconType.FILTER, "No rows", "Add") { }

        assertThat(card.isShowingEmptyState()).isTrue()

        model.addRow(arrayOf("value"))
        assertThat(card.isShowingEmptyState()).isFalse()

        model.removeRow(0)
        assertThat(card.isShowingEmptyState()).isTrue()
    }

    @Test
    fun `updates when a row sorter is installed`() {
        val model = DefaultTableModel(arrayOf("Column"), 0)
        val table = JTable(model).apply { autoCreateRowSorter = true }
        val card = TableCard(table, IconType.FILTER, "No rows", "Add") { }

        assertThat(card.isShowingEmptyState()).isTrue()

        model.addRow(arrayOf("value"))

        assertThat(table.rowCount).isEqualTo(1)
        assertThat(card.isShowingEmptyState()).isFalse()
    }
}
