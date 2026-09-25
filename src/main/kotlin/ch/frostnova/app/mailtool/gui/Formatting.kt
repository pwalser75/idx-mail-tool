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
