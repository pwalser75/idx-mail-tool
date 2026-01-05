package ch.frostnova.app.mailtool.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SetWithCountTest {

    @Test
    fun `should get entries with count`() {
        val set = SetWithCount<String>()
        set.add("One")
        set.add("Two")
        set.add("Three")
        set.add("Two")
        set.add("Three")
        set.add("Three")

        assertThat(set.topItems()).containsExactly(
            3 to "Three",
            2 to "Two",
            1 to "One"
        )
    }
}