package ch.frostnova.app.mailtool.gui

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.ConfigurationProperties
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.panel.ConnectionPanel
import ch.frostnova.app.mailtool.gui.panel.FolderPanel
import ch.frostnova.app.mailtool.gui.panel.RetentionPanel
import ch.frostnova.app.mailtool.gui.panel.RulesPanel
import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.ObjectMappers
import ch.frostnova.app.mailtool.util.validate
import jakarta.validation.ValidationException
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.Locale
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
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
class MailSetupWindow private constructor(
    private val configuration: ConfigurationProperties,
    private val connector: MailConnector,
    private val onSave: (ConfigurationProperties) -> Unit,
    private val initialState: SetupState
) : JFrame(I18n.t("app.window")) {

    constructor(
        configuration: ConfigurationProperties,
        connector: MailConnector,
        onSave: (ConfigurationProperties) -> Unit
    ) : this(configuration, connector, onSave, SetupState.initial(configuration))

    private enum class SetupSection(val titleKey: String, val descriptionKey: String) {
        CONNECTION("section.connection.title", "section.connection.description"),
        FOLDERS("section.folders.title", "section.folders.description"),
        RULES("section.rules.title", "section.rules.description"),
        RETENTION("section.retention.title", "section.retention.description")
    }

    private class SetupState(
        val originalAccountName: String?,
        val savedAccountName: String,
        val savedAccount: AccountProperties,
        val accountName: String,
        val account: AccountProperties,
        val sectionIndex: Int
    ) {
        companion object {
            fun initial(configuration: ConfigurationProperties): SetupState {
                val entry = configuration.accounts.entries.firstOrNull()
                val account = entry?.value ?: AccountProperties()
                val name = entry?.key ?: "default"
                return SetupState(entry?.key, name, account, name, account, 0)
            }
        }
    }

    private val folderNamesProvider = FolderNamesProvider()
    private val connectionPanel = ConnectionPanel(connector)
    private val rulesPanel = RulesPanel { folderNamesProvider.names }
    private val retentionPanel = RetentionPanel { folderNamesProvider.names }
    private val folderPanel = FolderPanel(
        connector,
        { AccountProperties().apply { connectionPanel.readInto(this) } },
        { retentionPanel.retentionSettings() },
        { rulesPanel.mailRules() },
        folderNamesProvider
    )

    private val sections = SetupSection.entries.toTypedArray()
    private val sectionList = JList(sections)
    private val contentPanel = JPanel(CardLayout())

    private var currentSection = SetupSection.entries[initialState.sectionIndex]
    private var originalAccountName: String? = initialState.originalAccountName
    private var savedAccountName: String = initialState.savedAccountName
    private var savedAccount: AccountProperties = initialState.savedAccount

    private var switchingLocale = false

    init {
        isUndecorated = true
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        contentPane.background = Theme.BACKGROUND
        rootPane.border = BorderFactory.createLineBorder(Theme.BORDER, 1)

        connectionPanel.accountName = initialState.accountName
        connectionPanel.load(initialState.account)
        rulesPanel.load(initialState.account)
        retentionPanel.load(initialState.account)

        val closeButton = JButton("\u2715").apply {
            font = Font(Font.SANS_SERIF, Font.PLAIN, 16)
            foreground = Theme.MUTED
            isFocusable = false
            isContentAreaFilled = false
            isBorderPainted = false
            border = EmptyBorder(4, 8, 4, 8)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = I18n.t("common.cancel")
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

        val languageCombo = JComboBox(I18n.supported.toTypedArray()).apply {
            font = Theme.LABEL_FONT
            toolTipText = I18n.t("language.label")
            renderer = LocaleRenderer()
            selectedItem = I18n.locale
        }
        languageCombo.addActionListener {
            if (switchingLocale) return@addActionListener
            val locale = languageCombo.selectedItem as? Locale ?: return@addActionListener
            switchLocale(locale)
        }

        val topControls = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0)).apply {
            background = Theme.BACKGROUND
            add(languageCombo)
            add(closeButton)
        }
        val topBar = JPanel(BorderLayout()).apply {
            background = Theme.BACKGROUND
            border = EmptyBorder(8, 12, 0, 14)
            add(topControls, BorderLayout.EAST)
        }
        enableWindowDrag(topBar)

        val appName = JLabel(I18n.t("app.name")).apply {
            font = Theme.TITLE_FONT
            foreground = Theme.ACCENT
        }
        val appSubtitle = JLabel(I18n.t("app.subtitle")).apply {
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
            selectedIndex = initialState.sectionIndex
            addListSelectionListener { event ->
                if (event.valueIsAdjusting) return@addListSelectionListener
                val selected = sectionList.selectedValue ?: return@addListSelectionListener
                currentSection = selected
                showSection(selected)
            }
        }

        contentPanel.background = Theme.BACKGROUND
        contentPanel.add(connectionPanel, SetupSection.CONNECTION.name)
        contentPanel.add(folderPanel, SetupSection.FOLDERS.name)
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
            add(topBar, BorderLayout.NORTH)
            add(contentPanel, BorderLayout.CENTER)
        }

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, rightPane).apply {
            border = BorderFactory.createEmptyBorder()
            dividerSize = 4
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

        rootPane.glassPane = WindowResizer(this)
        rootPane.glassPane.isVisible = true
        folderPanel.preload()
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
        if (section == SetupSection.FOLDERS) {
            folderPanel.ensureLoaded()
        }
    }

    private fun switchLocale(locale: Locale) {
        if (locale.language == I18n.locale.language) return
        val state = SetupState(
            originalAccountName,
            savedAccountName,
            savedAccount,
            connectionPanel.accountName.ifEmpty { "default" },
            currentAccount(),
            SetupSection.entries.indexOf(currentSection)
        )
        val location = location
        switchingLocale = true
        I18n.locale = locale
        val next = MailSetupWindow(configuration, connector, onSave, state)
        next.setLocation(location)
        next.isVisible = true
        dispose()
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
                I18n.t("save.invalid.title"),
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
            JOptionPane.showMessageDialog(this, ex.message, I18n.t("save.error.title"), JOptionPane.ERROR_MESSAGE)
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
        val options = arrayOf(
            I18n.t("unsaved.apply"),
            I18n.t("unsaved.discard"),
            I18n.t("unsaved.cancel")
        )
        val choice = JOptionPane.showOptionDialog(
            this,
            I18n.t("unsaved.message"),
            I18n.t("unsaved.title"),
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
        if (component is AbstractButton || component is JComboBox<*>) return
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

    private class LocaleRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is Locale) {
                text = I18n.displayName(value)
            }
            return this
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
            titleLabel.text = value?.let { I18n.t(it.titleKey) }.orEmpty()
            descriptionLabel.text = value?.let { I18n.t(it.descriptionKey) }.orEmpty()
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
