package ch.frostnova.app.mailtool.gui.dialog

import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction
import ch.frostnova.app.mailtool.gui.FormPanel
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.customizeDialog
import ch.frostnova.app.mailtool.gui.formLabel
import ch.frostnova.app.mailtool.gui.placeholder
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.gui.textField
import java.awt.BorderLayout
import java.awt.Dialog.ModalityType
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Window
import javax.swing.BorderFactory
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * Modal dialog to add or edit a single mail sorting rule.
 */
class RuleDialog(owner: Window, private val rule: MailRule?) :
    JDialog(owner, if (rule == null) "Add mail rule" else "Edit mail rule", ModalityType.APPLICATION_MODAL) {

    private val sendersArea = JTextArea(rule?.senders?.joinToString("\n").orEmpty(), 4, 28).apply {
        font = Theme.LABEL_FONT
        lineWrap = true
        wrapStyleWord = true
        border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
    }
    private val actionCombo = JComboBox(MailRuleAction.entries.toTypedArray()).apply {
        font = Theme.LABEL_FONT
        selectedItem = rule?.action ?: MailRuleAction.MOVE
    }
    private val folderField = textField(24, "folder name").apply {
        text = rule?.folder.orEmpty()
    }

    var result: MailRule? = null
        private set

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        contentPane.background = Theme.BACKGROUND

        val sendersLabel = formLabel("Senders").apply {
            border = BorderFactory.createEmptyBorder(0, 0, 4, 0)
        }
        val sendersPane = JScrollPane(sendersArea).apply {
            alignmentX = LEFT_ALIGNMENT
            preferredSize = Dimension(360, 110)
            border = BorderFactory.createLineBorder(Theme.BORDER, 1, true)
            viewport.background = Theme.SURFACE
        }

        val form = FormPanel().apply {
            row("Action", actionCombo)
            row("Target folder", folderField)
        }

        val sendersPanel = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            border = BorderFactory.createEmptyBorder(20, 24, 0, 24)
            add(sendersLabel, BorderLayout.NORTH)
            add(sendersPane, BorderLayout.CENTER)
        }

        val center = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(sendersPanel, BorderLayout.NORTH)
            add(form, BorderLayout.CENTER)
        }

        val saveButton = primaryButton("Save") { onSave() }
        val buttonBar = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 14)).apply {
            background = Theme.BACKGROUND
            add(secondaryButton("Cancel") { dispose() })
            add(saveButton)
        }

        contentPane.layout = BorderLayout()
        contentPane.add(center, BorderLayout.CENTER)
        contentPane.add(buttonBar, BorderLayout.SOUTH)

        updateFolderEnabled()
        actionCombo.addActionListener { updateFolderEnabled() }

        customizeDialog(this, saveButton)
        pack()
        minimumSize = Dimension(460, size.height)
        setLocationRelativeTo(owner)
    }

    private fun updateFolderEnabled() {
        val action = actionCombo.selectedItem as? MailRuleAction
        folderField.isEnabled = action == MailRuleAction.MOVE || action == MailRuleAction.COPY
    }

    private fun onSave() {
        val senders = sendersArea.text
            .split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val action = actionCombo.selectedItem as? MailRuleAction ?: MailRuleAction.MOVE
        val folder = folderField.text.trim()

        if (senders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide at least one sender.", "Invalid rule", JOptionPane.WARNING_MESSAGE)
            sendersArea.requestFocusInWindow()
            return
        }
        if (action != MailRuleAction.DELETE && folder.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide a target folder.", "Invalid rule", JOptionPane.WARNING_MESSAGE)
            folderField.requestFocusInWindow()
            return
        }

        result = MailRule().apply {
            this.senders = senders
            this.action = action
            this.folder = if (action == MailRuleAction.DELETE) null else folder
        }
        dispose()
    }
}
