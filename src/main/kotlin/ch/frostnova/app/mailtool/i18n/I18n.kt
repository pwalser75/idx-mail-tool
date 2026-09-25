package ch.frostnova.app.mailtool.i18n

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

/**
 * Tiny localization helper backed by the `messages*.properties` resource bundles.
 * The default locale follows the user's language (system property `user.language`)
 * and falls back to English for unsupported languages.
 */
object I18n {

    val supported: List<Locale> = listOf(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH, Locale.ITALIAN)

    private var bundle: ResourceBundle = ResourceBundle.getBundle("messages", detectLocale())

    var locale: Locale = detectLocale()
        set(value) {
            field = value
            bundle = ResourceBundle.getBundle("messages", value)
        }

    /**
     * Looks up the message [key] and formats it with the given arguments.
     */
    fun t(key: String, vararg args: Any): String {
        val pattern = bundle.getString(key)
        return if (args.isEmpty()) pattern else MessageFormat.format(pattern, *args)
    }

    fun selectedLocale(): Locale = locale

    fun displayName(locale: Locale): String =
        locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }

    private fun detectLocale(): Locale {
        val language = System.getProperty("user.language")?.lowercase()
            ?: Locale.getDefault().language
        return supported.firstOrNull { it.language == language } ?: Locale.ENGLISH
    }
}
