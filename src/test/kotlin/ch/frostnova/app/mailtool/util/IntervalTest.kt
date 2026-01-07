package ch.frostnova.app.mailtool.util

import ch.frostnova.app.mailtool.util.Interval.Companion.parse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class IntervalTest {

    @Test
    fun `should create interval`() {
        assertThat(Interval()).hasToString("")
        assertThat(Interval(0)).hasToString("")
        assertThat(Interval(0, 0, 0, 0)).hasToString("")
        assertThat(Interval(1)).hasToString("1d")
        assertThat(Interval(1, 2)).hasToString("1d 2h")
        assertThat(Interval(1, 2, 3)).hasToString("1d 2h 3m")
        assertThat(Interval(1, 2, 3, 4)).hasToString("1d 2h 3m 4s")
        assertThat(Interval(0, 2, 3, 4)).hasToString("2h 3m 4s")
        assertThat(Interval(1, 0, 3, 0)).hasToString("1d 3m")
        assertThat(Interval(0, 0, 0, 4)).hasToString("4s")

        assertThatThrownBy { Interval(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("days must be positive")
        assertThatThrownBy { Interval(0, -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("hours must be positive")
        assertThatThrownBy { Interval(0, 0, -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("minutes must be positive")
        assertThatThrownBy { Interval(-0, 0, 0, -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("seconds must be positive")
    }

    @Test
    fun `should recognize empty interval`() {
        assertThat(Interval().isEmpty()).isTrue
        assertThat(Interval(0).isEmpty()).isTrue
        assertThat(Interval(0, 0).isEmpty()).isTrue
        assertThat(Interval(0, 0, 0).isEmpty()).isTrue

        assertThat(Interval(1).isEmpty()).isFalse
        assertThat(Interval(0, 1).isEmpty()).isFalse
        assertThat(Interval(0, 0, 1).isEmpty()).isFalse
    }

    @Test
    fun `should parse interval`() {
        assertThat(parse("")).hasToString("")
        assertThat(parse("0")).hasToString("")
        assertThat(parse("0h")).hasToString("")
        assertThat(parse("1d")).hasToString("1d")
        assertThat(parse("2h")).hasToString("2h")
        assertThat(parse("33m")).hasToString("33m")
        assertThat(parse("44s")).hasToString("44s")
        assertThat(parse("1h 30m")).hasToString("1h 30m")
        assertThat(parse("5m20s")).hasToString("5m 20s")
        assertThat(parse("111d222H333m444s")).hasToString("111d 222h 333m 444s")
        assertThat(parse("111D 222H 333M 444S")).hasToString("111d 222h 333m 444s")
        assertThat(parse(" 111  d 222 H 333  m  444 s ")).hasToString("111d 222h 333m 444s")

        assertThat(parse(" 33m 44s 22h 11d ")).hasToString("11d 22h 33m 44s")

        assertThat(parse("1d 2d 3d 4d")).hasToString("10d")
        assertThat(parse("4h 9d 18m 31m 12s 9h 22m 3h 8d")).hasToString("17d 16h 71m 12s")

        assertThatThrownBy { parse("Hello") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid interval: Hello")
        assertThatThrownBy { parse("123") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parse("-2h") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parse("1w") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parse("1 2 3 d") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parse("2.2m") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parse("100x") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { parse("1h 25x") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should convert to duration`() {
        assertThat(parse("").toDuration()).hasToString("PT0S")
        assertThat(parse("0h").toDuration()).hasToString("PT0S")
        assertThat(parse("1d").toDuration()).hasToString("PT24H")
        assertThat(parse("2h").toDuration()).hasToString("PT2H")
        assertThat(parse("33m").toDuration()).hasToString("PT33M")
        assertThat(parse("44s").toDuration()).hasToString("PT44S")
        assertThat(parse("1h 30m").toDuration()).hasToString("PT1H30M")
        assertThat(parse("5m20s").toDuration()).hasToString("PT5M20S")
        assertThat(parse("111d222H333m444s").toDuration()).hasToString("PT2891H40M24S")
        assertThat(parse("111D 222H 333M 444S").toDuration()).hasToString("PT2891H40M24S")
        assertThat(parse(" 111  d 222 H 333  m  444 s ").toDuration()).hasToString("PT2891H40M24S")
    }
}