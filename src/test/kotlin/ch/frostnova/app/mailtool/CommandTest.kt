package ch.frostnova.app.mailtool

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommandTest {

    @Test
    fun `resolves commands case-insensitively`() {
        assertThat(command("gui")).isEqualTo(Command.GUI)
        assertThat(command("SETUP")).isEqualTo(Command.SETUP)
        assertThat(command("Apply")).isEqualTo(Command.APPLY)
    }

    @Test
    fun `returns null for an unknown command`() {
        assertThat(command("nope")).isNull()
    }
}
