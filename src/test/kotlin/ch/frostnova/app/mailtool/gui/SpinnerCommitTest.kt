package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class SpinnerCommitTest {

    @Test
    fun `commitValue stores a manually typed value`() {
        val spinner = JSpinner(SpinnerNumberModel(30, 1, 36500, 1))
        (spinner.editor as JSpinner.DefaultEditor).textField.text = "100"

        assertThat(spinner.value).isEqualTo(30)

        assertThat(spinner.commitValue()).isTrue()
        assertThat(spinner.value).isEqualTo(100)
    }

    @Test
    fun `commitValue rejects invalid text`() {
        val spinner = JSpinner(SpinnerNumberModel(30, 1, 36500, 1))
        (spinner.editor as JSpinner.DefaultEditor).textField.text = "not a number"

        assertThat(spinner.commitValue()).isFalse()
        assertThat(spinner.value).isEqualTo(30)
    }

    @Test
    fun `commitValue rejects values outside the model range`() {
        val spinner = JSpinner(SpinnerNumberModel(30, 1, 36500, 1))
        (spinner.editor as JSpinner.DefaultEditor).textField.text = "999999"

        assertThat(spinner.commitValue()).isFalse()
        assertThat(spinner.value).isEqualTo(30)
    }
}
