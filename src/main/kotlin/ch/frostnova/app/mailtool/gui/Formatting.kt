package ch.frostnova.app.mailtool.gui

import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.Interval

/**
 * Formats a retention period for display, e.g. "1 day" or "21 days".
 * Sub-day legacy values fall back to the interval notation (e.g. "12h 30m").
 */
fun retentionLabel(period: Interval?): String {
    if (period == null) return ""
    return if (period.hours == 0 && period.minutes == 0 && period.seconds == 0) {
        if (period.days == 1) I18n.t("retention.days.one") else I18n.t("retention.days.other", period.days.toString())
    } else {
        period.toString()
    }
}

/**
 * Table cell value for a retention period that sorts by the actual duration
 * (not alphabetically by its formatted label) while rendering as "x days".
 */
class RetentionValue(val label: String, private val seconds: Long) : Comparable<RetentionValue> {

    constructor(period: Interval?) : this(retentionLabel(period), period?.toDuration()?.seconds ?: Long.MIN_VALUE)

    override fun compareTo(other: RetentionValue): Int = seconds.compareTo(other.seconds)

    override fun toString(): String = label

    override fun equals(other: Any?): Boolean =
        other is RetentionValue && other.label == label && other.seconds == seconds

    override fun hashCode(): Int = 31 * label.hashCode() + seconds.hashCode()
}
