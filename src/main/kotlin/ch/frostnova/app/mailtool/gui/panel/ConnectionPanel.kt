package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.DotIcon
import ch.frostnova.app.mailtool.gui.FormPanel
import ch.frostnova.app.mailtool.gui.PillLabel
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.commitValue
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.sectionHeader
import ch.frostnova.app.mailtool.gui.textField
import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.validate
import com.formdev.flatlaf.FlatClientProperties
import jakarta.validation.ValidationException
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
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

    private val accountNameField = textField(28, I18n.t("connection.accountName.placeholder"))
    private val protocolCombo = JComboBox(arrayOf("imaps", "imap")).apply { font = Theme.LABEL_FONT }
    private val hostField = textField(28, I18n.t("connection.host.placeholder"))
    private val portSpinner = JSpinner(SpinnerNumberModel(993, 1, 65564, 1)).apply {
        preferredSize = Dimension(110, preferredSize.height)
    }
    private val tlsCheck = JCheckBox(I18n.t("connection.tls"), true).apply {
        font = Theme.LABEL_FONT
        foreground = Theme.TEXT
        background = Theme.SURFACE
        isOpaque = false
    }
    private val usernameField = textField(28, I18n.t("connection.username.placeholder"))
    private val passwordField = JPasswordField(28).apply {
        font = Theme.LABEL_FONT
        putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true")
    }

    private val statusPill = PillLabel()
    private val statusDetail = JLabel(" ").apply {
        font = Theme.LABEL_FONT
        foreground = Theme.MUTED
    }
    private val testButton = primaryButton(I18n.t("connection.test")) { testConnection() }

    var accountName: String
        get() = accountNameField.text.trim()
        set(value) {
            accountNameField.text = value
        }

    init {
        background = Theme.BACKGROUND
        border = EmptyBorder(28, 32, 28, 32)

        val form = FormPanel().apply {
            row(I18n.t("connection.accountName"), accountNameField)
            row(I18n.t("connection.protocol"), protocolCombo)
            row(I18n.t("connection.host"), hostField)
            row(I18n.t("connection.port"), portSpinner)
            row(I18n.t("connection.encryption"), tlsCheck)
            row(I18n.t("connection.username"), usernameField)
            row(I18n.t("connection.password"), passwordField)
        }

        val testBar = JPanel(FlowLayout(FlowLayout.LEADING, 12, 4)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 24, 20, 24)
            add(testButton)
            add(statusPill)
            add(statusDetail)
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

        add(sectionHeader(I18n.t("connection.header"), I18n.t("connection.header.description")), BorderLayout.NORTH)
        add(cardHolder, BorderLayout.CENTER)

        setStatus(Status.UNTESTED, " ")
    }

    override fun load(account: AccountProperties) {
        protocolCombo.selectedItem = account.protocol
        hostField.text = account.host.orEmpty()
        portSpinner.value = account.port
        tlsCheck.isSelected = account.tlsEnabled
        usernameField.text = account.username.orEmpty()
        passwordField.text = account.password.orEmpty()
        setStatus(Status.UNTESTED, " ")
    }

    override fun readInto(account: AccountProperties) {
        portSpinner.commitValue()
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
            JOptionPane.showMessageDialog(
                this,
                ex.message,
                I18n.t("connection.invalid.title"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        testButton.isEnabled = false
        setStatus(Status.TESTING, I18n.t("connection.connecting", properties.host.orEmpty(), properties.port))

        object : SwingWorker<Int, Void>() {
            override fun doInBackground(): Int =
                connector.connect(properties).use { adapter -> adapter.listFolders().size }

            override fun done() {
                testButton.isEnabled = true
                try {
                    val folderCount = get()
                    setStatus(
                        Status.CONNECTED,
                        I18n.t("connection.success", folderCount) + " · " +
                                I18n.t("connection.lastTested", currentTime())
                    )
                } catch (ex: Exception) {
                    val cause = ex.cause ?: ex
                    setStatus(Status.FAILED, cause.message ?: cause.toString())
                    JOptionPane.showMessageDialog(
                        this@ConnectionPanel,
                        cause.message ?: cause.toString(),
                        I18n.t("connection.failed.title"),
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.execute()
    }

    private fun setStatus(status: Status, detail: String) {
        statusPill.text = I18n.t(status.key)
        statusPill.pillColor = status.background
        statusPill.foreground = status.foreground
        statusPill.icon = DotIcon(status.dot, 8)
        statusDetail.text = detail
    }

    private fun currentTime(): String =
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    private enum class Status(val key: String, val background: Color, val foreground: Color, val dot: Color) {
        UNTESTED("connection.status.untested", Theme.SURFACE_ALT, Theme.MUTED, Theme.MUTED),
        TESTING("connection.status.testing", Color(0x4A3D1E), Color(0xE8C46A), Color(0xE8C46A)),
        CONNECTED("connection.status.connected", Color(0x24402C), Color(0x7FD194), Color(0x5FB878)),
        FAILED("connection.status.failed", Color(0x4E2A2E), Color(0xF0A6AE), Color(0xE06C75))
    }
}
