package ch.frostnova.app.mailtool.util

import java.time.Duration

/**
 * A simplified time interval class that only allows positive durations, from seconds to days.
 */
data class Interval(val days: Int = 0, val hours: Int = 0, val minutes: Int = 0, val seconds: Int = 0) {

    init {
        require(days >= 0) { "days must be positive" }
        require(hours >= 0) { "hours must be positive" }
        require(minutes >= 0) { "minutes must be positive" }
        require(seconds >= 0) { "seconds must be positive" }
    }

    fun isEmpty() = days == 0 && hours == 0 && minutes == 0 && seconds == 0

    fun toDuration(): Duration =
        Duration.ofDays(days.toLong())
            .plus(Duration.ofHours(hours.toLong()))
            .plus(Duration.ofMinutes(minutes.toLong()))
            .plus(Duration.ofSeconds(seconds.toLong()))

    override fun toString(): String {
        return listOfNotNull(
            if (days > 0) "${days}d" else null,
            if (hours > 0) "${hours}h" else null,
            if (minutes > 0) "${minutes}m" else null,
            if (seconds > 0) "${seconds}s" else null
        ).joinToString(" ")
    }

    companion object {
        private const val PATTERN = """(\d+)\s*([dDhHmMsS])"""
        private val regex = Regex(PATTERN)
        private val regexAll = Regex("""(?:\s*$PATTERN\s*)*""")

        fun parse(input: String): Interval {
            if (input.isBlank() || input.trim() == "0") {
                return Interval()
            }
            require(regexAll.matches(input)) { "Invalid interval: $input" }
            val matches = regex.findAll(input)
            val tokens = matches.map { matchResult ->
                val (number, unit) = matchResult.destructured
                number.toInt() to unit.lowercase()
            }
            val sum: (String) -> Int =
                { tokens.mapNotNull { (number, unit) -> if (unit == it) number else null }.sum() }

            return Interval(sum("d"), sum("h"), sum("m"), sum("s"))
        }
    }
}

