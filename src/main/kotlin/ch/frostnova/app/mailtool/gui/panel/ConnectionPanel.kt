package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.FormPanel
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.sectionHeader
import ch.frostnova.app.mailtool.gui.statusLabel
import ch.frostnova.app.mailtool.gui.textField
import ch.frostnova.app.mailtool.util.validate
import jakarta.validation.ValidationException
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.SwingWorker
import javax.swing.border.EmptyBorder

/**
 * Connection setup section: IMAP server, credentials and a connectivity test.
 */
class ConnectionPanel(private val connector: MailConnector) : JPanel(BorderLayout()), SetupPanel {

    private val accountNameField = textField(28, "account name")
    private val protocolCombo = JComboBox(arrayOf("imaps", "imap")).apply { font = Theme.LABEL_FONT }
    private val hostField = textField(28, "imap.example.org")
    private val portSpinner = JSpinner(SpinnerNumberModel(993, 1, 65564, 1)).apply {
        preferredSize = Dimension(110, preferredSize.height)
    }
    private val tlsCheck = JCheckBox("Use TLS / SSL", true).apply {
        font = Theme.LABEL_FONT
        foreground = Theme.TEXT
        background = Theme.SURFACE
        isOpaque = false
    }
    private val usernameField = textField(28, "user@example.org")
    private val passwordField = JPasswordField(28).apply {
        font = Theme.LABEL_FONT
    }

    private val statusLabel = statusLabel()
    private val testButton = primaryButton("Test connection") { testConnection() }

    var accountName: String
        get() = accountNameField.text.trim()
        set(value) {
            accountNameField.text = value
        }

    init {
        background = Theme.BACKGROUND
        border = EmptyBorder(28, 32, 28, 32)

        val form = FormPanel().apply {
            row("Account name", accountNameField)
            row("Protocol", protocolCombo)
            row("Host", hostField)
            row("Port", portSpinner)
            row("Encryption", tlsCheck)
            row("Username", usernameField)
            row("Password", passwordField)
        }

        val testBar = JPanel(FlowLayout(FlowLayout.LEADING, 12, 0)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 24, 20, 24)
            add(testButton)
            add(statusLabel)
        }

        val cardContent = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(form, BorderLayout.CENTER)
            add(testBar, BorderLayout.SOUTH)
        }

        val cardHolder = JPanel(BorderLayout()).apply {
            background = Theme.BACKGROUND
            add(card(cardContent), BorderLayout.NORTH)
        }

        add(sectionHeader("Connection", "Configure the IMAP server and verify that the credentials work."), BorderLayout.NORTH)
        add(cardHolder, BorderLayout.CENTER)
    }

    override fun load(account: AccountProperties) {
        protocolCombo.selectedItem = account.protocol
        hostField.text = account.host.orEmpty()
        portSpinner.value = account.port
        tlsCheck.isSelected = account.tlsEnabled
        usernameField.text = account.username.orEmpty()
        passwordField.text = account.password.orEmpty()
        statusLabel.text = " "
    }

    override fun readInto(account: AccountProperties) {
        account.protocol = protocolCombo.selectedItem as? String ?: "imaps"
        account.host = hostField.text.trim()
        account.port = portSpinner.value as Int
        account.tlsEnabled = tlsCheck.isSelected
        account.username = usernameField.text.trim()
        account.password = String(passwordField.password)
    }

    private fun buildProperties(): AccountProperties = AccountProperties().apply { readInto(this) }

    private fun testConnection() {
        val properties = try {
            buildProperties().also { validate(it) }
        } catch (ex: ValidationException) {
            JOptionPane.showMessageDialog(this, ex.message, "Invalid connection settings", JOptionPane.WARNING_MESSAGE)
            return
        }

        testButton.isEnabled = false
        statusLabel.foreground = Theme.MUTED
        statusLabel.text = "Connecting to ${properties.host}:${properties.port} ..."

        object : SwingWorker<Int, Void>() {
            override fun doInBackground(): Int =
                connector.connect(properties).use { adapter -> adapter.listFolders().size }

            override fun done() {
                testButton.isEnabled = true
                try {
                    val folderCount = get()
                    statusLabel.foreground = Theme.SUCCESS
                    statusLabel.text = "Connection successful - $folderCount folders found"
                } catch (ex: Exception) {
                    val cause = ex.cause ?: ex
                    statusLabel.foreground = Theme.DANGER
                    statusLabel.text = "Connection failed: ${cause.message}"
                    JOptionPane.showMessageDialog(
                        this@ConnectionPanel,
                        cause.message ?: cause.toString(),
                        "Connection failed",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.execute()
    }
}
