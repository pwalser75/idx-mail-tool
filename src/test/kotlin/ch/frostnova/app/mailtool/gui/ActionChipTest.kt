package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActionChipTest {

    @Test
    fun `each action uses a distinct chip colour`() {
        val backgrounds = listOf("MOVE", "COPY", "DELETE").map { actionPalette(it).background }

        assertThat(backgrounds).doesNotHaveDuplicates()
    }

    @Test
    fun `unknown or missing action uses a neutral chip`() {
        assertThat(actionPalette(null).background).isEqualTo(Theme.SURFACE_ALT)
        assertThat(actionPalette("SOMETHING").background).isEqualTo(Theme.SURFACE_ALT)
        assertThat(actionPalette(null).foreground).isEqualTo(Theme.TEXT)
    }
}
