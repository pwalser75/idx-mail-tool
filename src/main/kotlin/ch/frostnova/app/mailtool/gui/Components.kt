package ch.frostnova.app.mailtool.gui

import com.formdev.flatlaf.FlatClientProperties
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.text.ParseException
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

/**
 * A section header with a title and a short explanatory subtitle.
 */
fun sectionHeader(title: String, subtitle: String): JComponent {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.background = Theme.BACKGROUND
    panel.border = EmptyBorder(4, 0, 18, 0)

    val titleLabel = JLabel(title).apply {
        font = Theme.SECTION_FONT
        foreground = Theme.TEXT
        alignmentX = Component.LEFT_ALIGNMENT
    }
    val subtitleLabel = JLabel(subtitle).apply {
        font = Theme.SUBTITLE_FONT
        foreground = Theme.MUTED
        alignmentX = Component.LEFT_ALIGNMENT
    }
    panel.add(titleLabel)
    panel.add(Box.createVerticalStrut(4))
    panel.add(subtitleLabel)
    return panel
}

fun formLabel(text: String): JLabel = JLabel(text).apply {
    font = Theme.LABEL_FONT
    foreground = Theme.TEXT
}

fun primaryButton(text: String, icon: Icon? = null, action: (ActionEvent) -> Unit): JButton =
    JButton(text, icon).apply {
        isFocusable = false
        iconTextGap = if (icon != null) 8 else 0
        putClientProperty(
            FlatClientProperties.STYLE,
            "arc: 0; background: #4c9aff; foreground: #ffffff; " +
                    "hoverBackground: #5ea9ff; pressedBackground: #3d86e0; " +
                    "borderWidth: 0; focusWidth: 0"
        )
        addActionListener { action(it) }
    }

fun secondaryButton(text: String, icon: Icon? = null, action: (ActionEvent) -> Unit): JButton =
    JButton(text, icon).apply {
        isFocusable = false
        foreground = Theme.TEXT
        iconTextGap = if (icon != null) 8 else 0
        putClientProperty(
            FlatClientProperties.STYLE,
            "arc: 0; background: #33363a; foreground: #dfe1e5; " +
                    "hoverBackground: #3d4145; pressedBackground: #2a2c2f; " +
                    "borderColor: #3c3f41; borderWidth: 1; focusWidth: 0"
        )
        addActionListener { action(it) }
    }

fun textField(columns: Int, placeholder: String? = null): JTextField =
    JTextField(columns).apply {
        font = Theme.LABEL_FONT
        placeholder?.let { putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, it) }
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, placeholder != null)
    }

fun placeholder(component: JComponent, text: String) {
    component.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, text)
}

/**
 * A simple two-column form panel (label left, editor right).
 */
class FormPanel : JPanel(GridBagLayout()) {

    private var row = 0

    init {
        background = Theme.SURFACE
        border = EmptyBorder(20, 24, 20, 24)
        // Vertical glue keeps the form content top-aligned instead of centered.
        add(
            Box.createGlue(),
            GridBagConstraints().apply {
                gridx = 0
                gridy = 10000
                gridwidth = 2
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
        )
    }

    fun row(label: String, field: JComponent): FormPanel {
        val labelConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.LINE_START
            insets = Insets(7, 0, 7, 16)
            fill = GridBagConstraints.NONE
            weightx = 0.0
        }
        val fieldConstraints = GridBagConstraints().apply {
            gridx = 1
            gridy = row
            anchor = GridBagConstraints.LINE_START
            insets = Insets(7, 0, 7, 0)
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }
        add(formLabel(label), labelConstraints)
        add(field, fieldConstraints)
        row++
        return this
    }

    fun fullRow(component: JComponent): FormPanel {
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            anchor = GridBagConstraints.LINE_START
            insets = Insets(7, 0, 7, 0)
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }
        add(component, constraints)
        row++
        return this
    }

    fun spacer(height: Int): FormPanel {
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            weighty = 1.0
            fill = GridBagConstraints.VERTICAL
        }
        add(Box.createVerticalStrut(height), constraints)
        row++
        return this
    }
}

/**
 * A rounded card container used to group content on the dark background.
 */
fun card(content: JComponent): JPanel = JPanel().apply {
    background = Theme.SURFACE
    border = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Theme.BORDER, 1, true),
        EmptyBorder(0, 0, 0, 0)
    )
    layout = BorderLayout()
    add(content, BorderLayout.CENTER)
}

fun statusLabel(): JLabel = JLabel(" ").apply {
    font = Theme.LABEL_FONT
    foreground = Theme.MUTED
    horizontalAlignment = SwingConstants.LEFT
    preferredSize = Dimension(400, 24)
}

/**
 * Makes [defaultButton] the dialog's default button and closes the dialog on Escape.
 */
fun customizeDialog(dialog: javax.swing.JDialog, defaultButton: JButton) {
    dialog.rootPane.defaultButton = defaultButton
    val escape = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0)
    dialog.rootPane.registerKeyboardAction({ dialog.dispose() }, escape, JComponent.WHEN_IN_FOCUSED_WINDOW)
}

/**
 * Commits a spinner's editor text into its model. Returns `false` if the entered
 * text is not a valid value. Needed because our buttons are not focusable, so the
 * editor never loses focus and thus never commits on its own.
 */
fun JSpinner.commitValue(): Boolean = try {
    commitEdit()
    true
} catch (ex: ParseException) {
    false
}

/**
 * Reads the current text of an editable combo box, including text the user typed
 * but that was not yet committed to the model (e.g. because the Save button is
 * not focusable). Falls back to the selected item.
 */
fun JComboBox<*>.editorText(): String =
    (editor.item?.toString() ?: selectedItem?.toString()).orEmpty().trim()

/**
 * Table cell renderer that shows the full cell value as a tooltip.
 */
class TooltipCellRenderer : javax.swing.table.DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: javax.swing.JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): java.awt.Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (component is JLabel) {
            component.toolTipText = value?.toString()
            component.border = EmptyBorder(0, 8, 0, 8)
        }
        return component
    }
}
