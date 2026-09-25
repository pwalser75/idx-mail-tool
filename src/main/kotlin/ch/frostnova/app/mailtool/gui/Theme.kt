package ch.frostnova.app.mailtool.gui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import java.awt.Color
import java.awt.Font
import javax.swing.UIManager

/**
 * Central place for the dark theme and the shared look of the setup UI.
 */
object Theme {

    val ACCENT = Color(0x4C9AFF)
    val ACCENT_DARK = Color(0x2F6FBF)
    val BACKGROUND = Color(0x1E1F22)
    val SURFACE = Color(0x2B2D30)
    val SURFACE_ALT = Color(0x33363A)
    val BORDER = Color(0x3C3F41)
    val TEXT = Color(0xDFE1E5)
    val MUTED = Color(0x9DA0A8)
    val SUCCESS = Color(0x5FB878)
    val DANGER = Color(0xE06C75)

    val TITLE_FONT: Font = Font(Font.SANS_SERIF, Font.BOLD, 20)
    val SUBTITLE_FONT: Font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    val SECTION_FONT: Font = Font(Font.SANS_SERIF, Font.BOLD, 14)
    val LABEL_FONT: Font = Font(Font.SANS_SERIF, Font.PLAIN, 13)

    /**
     * Installs the dark FlatLaf look and feel together with a few rounded,
     * modern defaults. Must be called before any Swing component is created.
     */
    fun install() {
        FlatLaf.setGlobalExtraDefaults(
            mapOf(
                "@accentColor" to "#4c9aff",
                "Component.focusColor" to "#4c9aff"
            )
        )
        FlatDarkLaf.setup()
        UIManager.put("Component.arc", 10)
        UIManager.put("Button.arc", 0)
        UIManager.put("TextComponent.arc", 8)
        UIManager.put("CheckBox.arc", 6)
        UIManager.put("ProgressBar.arc", 8)
        UIManager.put("ScrollBar.thumbArc", 999)
        UIManager.put("ScrollBar.thumbInsets", java.awt.Insets(2, 2, 2, 2))
        UIManager.put("ScrollBar.width", 12)
        UIManager.put("Table.showHorizontalLines", true)
        UIManager.put("Table.showVerticalLines", false)
        UIManager.put("Table.intercellSpacing", java.awt.Dimension(0, 1))
        UIManager.put("Table.rowHeight", 28)
        UIManager.put("Panel.background", BACKGROUND)
        UIManager.put("OptionPane.background", SURFACE)
    }
}
