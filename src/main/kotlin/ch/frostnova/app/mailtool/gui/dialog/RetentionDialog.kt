package ch.frostnova.app.mailtool.gui.dialog

import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.gui.FormPanel
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.commitValue
import ch.frostnova.app.mailtool.gui.customizeDialog
import ch.frostnova.app.mailtool.gui.editorText
import ch.frostnova.app.mailtool.gui.formLabel
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.Interval
import java.awt.BorderLayout
import java.awt.Dialog.ModalityType
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Window
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Modal dialog to add or edit a data retention setting for a folder.
 * The retention period is configured in days.
 */
class RetentionDialog(
    owner: Window,
    private val setting: DataRetentionSettings?,
    folderNames: () -> List<String>
) :
    JDialog(
        owner,
        I18n.t(if (setting == null) "retention.title.add" else "retention.title.edit"),
        ModalityType.APPLICATION_MODAL
    ) {

    /**
     * Editable so that an existing folder can be picked, while a free-text
     * reference (which may not exist yet or anymore) is still allowed.
     */
    private val folderCombo = JComboBox(folderNames().toTypedArray()).apply {
        isEditable = true
        font = Theme.LABEL_FONT
        toolTipText = I18n.t("folder.select.tooltip")
        selectedItem = setting?.folder.orEmpty()
    }
    private val daysSpinner = JSpinner(
        SpinnerNumberModel(setting?.retentionPeriod?.days ?: 30, 1, 36500, 1)
    ).apply {
        preferredSize = Dimension(90, preferredSize.height)
    }

    var result: DataRetentionSettings? = null
        private set

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        contentPane.background = Theme.BACKGROUND

        val intervalPanel = JPanel(FlowLayout(FlowLayout.LEADING, 8, 0)).apply {
            background = Theme.SURFACE
            add(daysSpinner)
            add(formLabel(I18n.t("retention.days")))
        }

        val form = FormPanel().apply {
            row(I18n.t("retention.folder"), folderCombo)
            row(I18n.t("retention.period"), intervalPanel)
        }

        val saveButton = primaryButton(I18n.t("common.save")) { onSave() }
        val buttonBar = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 14)).apply {
            background = Theme.BACKGROUND
            add(secondaryButton(I18n.t("common.cancel")) { dispose() })
            add(saveButton)
        }

        contentPane.layout = BorderLayout()
        contentPane.add(form, BorderLayout.CENTER)
        contentPane.add(buttonBar, BorderLayout.SOUTH)

        customizeDialog(this, saveButton)
        pack()
        setLocationRelativeTo(owner)
    }

    private fun onSave() {
        val folder = folderCombo.editorText()
        if (folder.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                I18n.t("retention.invalid.folder"),
                I18n.t("retention.invalid.title"),
                JOptionPane.WARNING_MESSAGE
            )
            folderCombo.requestFocusInWindow()
            return
        }

        if (!daysSpinner.commitValue()) {
            JOptionPane.showMessageDialog(
                this,
                I18n.t("retention.invalid.days"),
                I18n.t("retention.invalid.title"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        result = DataRetentionSettings().apply {
            this.folder = folder
            this.retentionPeriod = Interval(days = daysSpinner.value as Int)
        }
        dispose()
    }
}
