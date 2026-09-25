package ch.frostnova.app.mailtool.gui

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.panel.ConnectionPanel
import ch.frostnova.app.mailtool.gui.panel.RetentionPanel
import ch.frostnova.app.mailtool.gui.panel.RulesPanel
import ch.frostnova.app.mailtool.util.ObjectMappers
import ch.frostnova.app.mailtool.util.validate
import jakarta.validation.ValidationException
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import kotlin.system.exitProcess

/**
 * The setup window: a split view with the configuration sections on the left and
 * the editor for the selected section on the right.
 */
class MailSetupWindow(
    private val configuration: ConfigurationProperties,
    connector: MailConnector,
    private val onSave: (ConfigurationProperties) -> Unit
) : JFrame("IDX Mail Tool - Setup") {

    private enum class SetupSection(val title: String, val description: String) {
        CONNECTION("Connection", "IMAP server, credentials and connectivity"),
        RULES("Mail rules", "Sort incoming messages into folders"),
        RETENTION("Data retention", "Delete old messages from folders")
    }

    private val connectionPanel = ConnectionPanel(connector)
    private val rulesPanel = RulesPanel()
    private val retentionPanel = RetentionPanel()

    private val sections = SetupSection.entries.toTypedArray()
    private val sectionList = JList(sections)
    private val contentPanel = JPanel(CardLayout())

    private var currentSection = SetupSection.CONNECTION
    private var originalAccountName: String? = null
    private var savedAccountName = "default"
    private var savedAccount = AccountProperties()

    init {
        isUndecorated = true
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        contentPane.background = Theme.BACKGROUND

        loadAccount()
        savedAccountName = connectionPanel.accountName.ifEmpty { "default" }
        savedAccount = currentAccount()

        val closeButton = JButton("\u2715").apply {
            font = Font(Font.SANS_SERIF, Font.PLAIN, 16)
            foreground = Theme.MUTED
            isFocusable = false
            isContentAreaFilled = false
            isBorderPainted = false
            border = EmptyBorder(4, 8, 4, 8)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Close"
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(event: MouseEvent) {
                    foreground = Theme.DANGER
                }

                override fun mouseExited(event: MouseEvent) {
                    foreground = Theme.MUTED
                }
            })
            addActionListener { requestClose() }
        }
        val closeRow = JPanel(BorderLayout()).apply {
            background = Theme.BACKGROUND
            border = EmptyBorder(8, 12, 0, 14)
            add(closeButton, BorderLayout.EAST)
        }
        enableWindowDrag(closeRow)

        val appName = JLabel("IDX Mail Tool").apply {
            font = Theme.TITLE_FONT
            foreground = Theme.ACCENT
        }
        val appSubtitle = JLabel("Setup").apply {
            font = Theme.SUBTITLE_FONT
            foreground = Theme.MUTED
        }

        val sidebarHeader = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = Theme.SURFACE
            border = EmptyBorder(14, 20, 16, 20)
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
            appName.alignmentX = Component.LEFT_ALIGNMENT
            appSubtitle.alignmentX = Component.LEFT_ALIGNMENT
            add(appName)
            add(appSubtitle)
        }
        enableWindowDrag(sidebarHeader)

        sectionList.apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            background = Theme.SURFACE
            selectionBackground = Theme.ACCENT_DARK
            selectionForeground = Theme.TEXT
            cellRenderer = SectionRenderer()
            selectedIndex = 0
            addListSelectionListener { event ->
                if (event.valueIsAdjusting) return@addListSelectionListener
                val selected = sectionList.selectedValue ?: return@addListSelectionListener
                currentSection = selected
                showSection(selected)
            }
        }

        contentPanel.background = Theme.BACKGROUND
        contentPanel.add(connectionPanel, SetupSection.CONNECTION.name)
        contentPanel.add(rulesPanel, SetupSection.RULES.name)
        contentPanel.add(retentionPanel, SetupSection.RETENTION.name)

        val sidebar = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            preferredSize = Dimension(260, 0)
            minimumSize = Dimension(200, 0)
            add(sidebarHeader, BorderLayout.NORTH)
            add(sectionList, BorderLayout.CENTER)
        }

        val rightPane = JPanel(BorderLayout()).apply {
            background = Theme.BACKGROUND
            add(closeRow, BorderLayout.NORTH)
            add(contentPanel, BorderLayout.CENTER)
        }

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, rightPane).apply {
            border = BorderFactory.createEmptyBorder()
            dividerSize = 1
            resizeWeight = 0.0
            dividerLocation = 260
        }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) = requestClose()
        })

        contentPane.layout = BorderLayout()
        contentPane.add(splitPane, BorderLayout.CENTER)

        showSection(currentSection)
        minimumSize = Dimension(880, 520)
        size = Dimension(1040, 640)
        setLocationRelativeTo(null)
    }

    private fun loadAccount() {
        val entry = configuration.accounts.entries.firstOrNull()
        val account = entry?.value ?: AccountProperties()
        originalAccountName = entry?.key
        connectionPanel.accountName = entry?.key ?: "default"
        connectionPanel.load(account)
        rulesPanel.load(account)
        retentionPanel.load(account)
    }

    private fun currentAccount(): AccountProperties = AccountProperties().apply {
        connectionPanel.readInto(this)
        rulesPanel.readInto(this)
        retentionPanel.readInto(this)
    }

    private fun isDirty(): Boolean {
        val currentName = connectionPanel.accountName.ifEmpty { "default" }
        if (currentName != savedAccountName) return true
        return ObjectMappers.json().writeValueAsString(currentAccount()) !=
                ObjectMappers.json().writeValueAsString(savedAccount)
    }

    private fun showSection(section: SetupSection) {
        (contentPanel.layout as CardLayout).show(contentPanel, section.name)
    }

    private fun saveConfiguration(): Boolean {
        val accountName = connectionPanel.accountName.ifEmpty { "default" }
        val account = currentAccount()

        try {
            validate(account)
        } catch (ex: ValidationException) {
            JOptionPane.showMessageDialog(
                this,
                ex.message,
                "Invalid configuration",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }

        val accounts = LinkedHashMap(configuration.accounts)
        originalAccountName?.let { accounts.remove(it) }
        accounts[accountName] = account
        configuration.accounts = accounts

        return try {
            onSave(configuration)
            originalAccountName = accountName
            savedAccountName = accountName
            savedAccount = account
            true
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(this, ex.message, "Could not save configuration", JOptionPane.ERROR_MESSAGE)
            false
        }
    }

    private fun requestClose() {
        if (confirmApplyChanges()) {
            dispose()
            exitProcess(0)
        }
    }

    private fun confirmApplyChanges(): Boolean {
        if (!isDirty()) return true
        val options = arrayOf("Yes", "No", "Cancel")
        val choice = JOptionPane.showOptionDialog(
            this,
            "Do you want to apply your changes before closing?",
            "Unsaved changes",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )
        return when (choice) {
            0 -> saveConfiguration()
            1 -> true
            else -> false
        }
    }

    private fun enableWindowDrag(component: Component) {
        if (component is AbstractButton) return
        var origin: Point? = null
        component.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                origin = event.point
            }

            override fun mouseReleased(event: MouseEvent) {
                origin = null
            }
        })
        component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                origin?.let { point ->
                    setLocation(location.x + event.x - point.x, location.y + event.y - point.y)
                }
            }
        })
        if (component is Container) {
            component.components.forEach { enableWindowDrag(it) }
        }
    }

    private class SectionRenderer : JPanel(BorderLayout()), ListCellRenderer<SetupSection> {

        private val titleLabel = JLabel().apply { font = Theme.SECTION_FONT }
        private val descriptionLabel = JLabel().apply { font = Theme.SUBTITLE_FONT }

        init {
            border = EmptyBorder(10, 12, 10, 12)
            add(titleLabel, BorderLayout.NORTH)
            add(descriptionLabel, BorderLayout.CENTER)
        }

        override fun getListCellRendererComponent(
            list: JList<out SetupSection>?,
            value: SetupSection?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            titleLabel.text = value?.title.orEmpty()
            descriptionLabel.text = value?.description.orEmpty()
            titleLabel.foreground = Theme.TEXT
            descriptionLabel.foreground = if (isSelected) Theme.TEXT else Theme.MUTED
            background = if (isSelected) Theme.ACCENT_DARK else Theme.SURFACE
            return this
        }
    }

    companion object {
        /**
         * Installs the dark theme and shows the setup window on the Swing event dispatch thread.
         */
        fun open(
            configuration: ConfigurationProperties,
            connector: MailConnector,
            onSave: (ConfigurationProperties) -> Unit
        ) {
            SwingUtilities.invokeLater {
                Theme.install()
                MailSetupWindow(configuration, connector, onSave).isVisible = true
            }
        }
    }
}
