package ch.frostnova.app.mailtool.gui.dialog

import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.gui.FormPanel
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.customizeDialog
import ch.frostnova.app.mailtool.gui.formLabel
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.gui.textField
import ch.frostnova.app.mailtool.util.Interval
import java.awt.BorderLayout
import java.awt.Dialog.ModalityType
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Window
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Modal dialog to add or edit a data retention setting for a folder.
 * The retention period is configured in days.
 */
class RetentionDialog(owner: Window, private val setting: DataRetentionSettings?) :
    JDialog(owner, if (setting == null) "Add data retention rule" else "Edit data retention rule", ModalityType.APPLICATION_MODAL) {

    private val folderField = textField(24, "folder name").apply {
        text = setting?.folder.orEmpty()
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
            add(formLabel("days"))
        }

        val form = FormPanel().apply {
            row("Folder", folderField)
            row("Retention period", intervalPanel)
        }

        val saveButton = primaryButton("Save") { onSave() }
        val buttonBar = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 14)).apply {
            background = Theme.BACKGROUND
            add(secondaryButton("Cancel") { dispose() })
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
        val folder = folderField.text.trim()
        if (folder.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide a folder name.", "Invalid rule", JOptionPane.WARNING_MESSAGE)
            folderField.requestFocusInWindow()
            return
        }

        result = DataRetentionSettings().apply {
            this.folder = folder
            this.retentionPeriod = Interval(days = daysSpinner.value as Int)
        }
        dispose()
    }
}
