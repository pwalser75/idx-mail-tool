package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JComboBox

class ComboBoxEditorTextTest {

    @Test
    fun `reads the selected item`() {
        val combo = editableCombo()
        combo.selectedItem = "Insurance"

        assertThat(combo.editorText()).isEqualTo("Insurance")
    }

    @Test
    fun `reads text typed by the user before it is committed`() {
        val combo = editableCombo()
        combo.selectedItem = "Insurance"

        combo.editor.item = "Not yet existing"

        assertThat(combo.selectedItem)
            .describedAs("model still holds the previous selection until commit")
            .isEqualTo("Insurance")
        assertThat(combo.editorText()).isEqualTo("Not yet existing")
    }

    private fun editableCombo(): JComboBox<String> =
        JComboBox(arrayOf("INBOX", "Insurance", "Shopping")).apply { isEditable = true }
}
