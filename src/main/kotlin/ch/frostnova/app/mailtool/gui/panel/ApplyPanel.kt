package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.apply.ActionOrigin
import ch.frostnova.app.mailtool.apply.ActionType
import ch.frostnova.app.mailtool.apply.ApplyProgress
import ch.frostnova.app.mailtool.apply.MailAction
import ch.frostnova.app.mailtool.apply.RuleApplier
import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.connector.MailConnector
import ch.frostnova.app.mailtool.gui.ActionChipRenderer
import ch.frostnova.app.mailtool.gui.IconType
import ch.frostnova.app.mailtool.gui.TableCard
import ch.frostnova.app.mailtool.gui.Theme
import ch.frostnova.app.mailtool.gui.card
import ch.frostnova.app.mailtool.gui.enableRowStyling
import ch.frostnova.app.mailtool.gui.playIcon
import ch.frostnova.app.mailtool.gui.primaryButton
import ch.frostnova.app.mailtool.gui.secondaryButton
import ch.frostnova.app.mailtool.gui.sectionHeader
import ch.frostnova.app.mailtool.gui.statusLabel
import ch.frostnova.app.mailtool.i18n.I18n
import ch.frostnova.app.mailtool.util.ObjectMappers
import ch.frostnova.app.mailtool.util.validate
import jakarta.validation.ValidationException
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingWorker
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel

/**
 * Apply section: runs the configured rules against the mailbox, or previews the
 * actions that would be taken, listing each action in a table.
 */
class ApplyPanel(
    private val connector: MailConnector,
    private val connectionSupplier: () -> AccountProperties,
    private val rulesSupplier: () -> List<MailRule>,
    private val retentionSupplier: () -> List<DataRetentionSettings>,
    private val onApplied: () -> Unit
) : JPanel(BorderLayout()), SetupPanel {

    private val model = ActionsTableModel()
    private val table = JTable(model)
    private val statusLabel = statusLabel()
    private val progressBar = JProgressBar().apply {
        isIndeterminate = true
        isVisible = false
        preferredSize = Dimension(90, 6)
    }
    private val refreshButton = secondaryButton(I18n.t("apply.button.refresh")) { run(dryRun = true) }
    private val applyButton = primaryButton(I18n.t("apply.button.apply"), playIcon(Theme.ON_ACCENT)) { run(dryRun = false) }

    private var loading = false
    private var lastPreviewSignature: String? = null

    init {
        background = Theme.BACKGROUND
        border = EmptyBorder(28, 32, 28, 32)

        table.apply {
            font = Theme.LABEL_FONT
            foreground = Theme.TEXT
            background = Theme.SURFACE
            selectionBackground = Theme.ACCENT_DARK
            selectionForeground = Theme.TEXT
            gridColor = Theme.BORDER
            rowHeight = 30
            fillsViewportHeight = true
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            border = BorderFactory.createEmptyBorder()
        }
        table.enableRowStyling()
        table.columnModel.getColumn(0).preferredWidth = 80
        table.columnModel.getColumn(1).preferredWidth = 280
        table.columnModel.getColumn(2).preferredWidth = 200
        table.columnModel.getColumn(3).preferredWidth = 130
        table.columnModel.getColumn(4).preferredWidth = 200
        table.columnModel.getColumn(0).cellRenderer = ActionChipRenderer()
        table.autoCreateRowSorter = true

        val toolbar = JPanel(FlowLayout(FlowLayout.LEADING, 12, 14)).apply {
            background = Theme.SURFACE
            border = EmptyBorder(0, 20, 0, 20)
            add(applyButton)
            add(refreshButton)
            add(progressBar)
            add(statusLabel)
        }

        val tableCard = TableCard(table, IconType.PLAY, I18n.t("apply.empty"))

        val body = JPanel(BorderLayout()).apply {
            background = Theme.SURFACE
            add(toolbar, BorderLayout.NORTH)
            add(tableCard, BorderLayout.CENTER)
        }

        add(sectionHeader(I18n.t("apply.header"), I18n.t("apply.header.description")), BorderLayout.NORTH)
        add(card(body), BorderLayout.CENTER)
    }

    private fun run(dryRun: Boolean) {
        if (loading) return
        val properties = connectionSupplier()
        try {
            validate(properties)
        } catch (ex: ValidationException) {
            JOptionPane.showMessageDialog(
                this,
                ex.message,
                I18n.t("connection.invalid.title"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val signature = inputSignature()
        loading = true
        applyButton.isEnabled = false
        refreshButton.isEnabled = false
        progressBar.isVisible = true
        progressBar.isIndeterminate = true
        statusLabel.foreground = Theme.MUTED
        statusLabel.text = I18n.t("apply.running")

        object : SwingWorker<List<MailAction>, ApplyProgress>() {
            override fun doInBackground(): List<MailAction> {
                val actions = mutableListOf<MailAction>()
                connector.connect(properties).use { adapter ->
                    RuleApplier(adapter, rulesSupplier(), retentionSupplier())
                        .run(dryRun, onProgress = { publish(it) }) { actions.add(it) }
                }
                return actions
            }

            override fun process(chunks: MutableList<ApplyProgress>) {
                if (!loading) return
                chunks.lastOrNull()?.let { showProgress(it) }
            }

            override fun done() {
                loading = false
                applyButton.isEnabled = true
                refreshButton.isEnabled = true
                progressBar.isVisible = false
                try {
                    val actions = get()
                    statusLabel.foreground = if (actions.isEmpty()) Theme.MUTED else Theme.SUCCESS
                    if (dryRun) {
                        model.setActions(actions)
                        statusLabel.text = I18n.t("apply.preview.done", actions.size)
                        lastPreviewSignature = signature
                    } else {
                        // the applied actions are done, so clear them and refresh on the next visit
                        model.setActions(emptyList())
                        statusLabel.text = I18n.t("apply.applied.done", actions.size)
                        lastPreviewSignature = null
                        onApplied()
                    }
                } catch (ex: Exception) {
                    val cause = ex.cause ?: ex
                    statusLabel.foreground = Theme.DANGER
                    statusLabel.text = I18n.t("apply.failed", cause.message ?: cause.toString())
                    JOptionPane.showMessageDialog(
                        this@ApplyPanel,
                        cause.message ?: cause.toString(),
                        I18n.t("apply.failed.title"),
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.execute()
    }

    private fun showProgress(progress: ApplyProgress) {
        statusLabel.foreground = Theme.MUTED
        if (progress.total <= 0) {
            progressBar.isIndeterminate = true
            statusLabel.text = I18n.t("apply.running")
            return
        }
        progressBar.isIndeterminate = false
        progressBar.maximum = progress.total
        progressBar.value = progress.processed.coerceIn(0, progress.total)
        statusLabel.text = progressText(progress)
    }

    private fun progressText(progress: ApplyProgress): String {
        val base = I18n.t("apply.progress", progress.folder.orEmpty(), progress.processed, progress.total)
        val subject = progress.message?.trim()?.takeIf { it.isNotEmpty() } ?: return base
        return "$base · ${subject.take(PROGRESS_SUBJECT_LENGTH)}"
    }

    /**
     * Runs a preview automatically only when the section is opened for the first
     * time, or when the connection/rules/retention changed since the last preview.
     */
    fun autoPreview() {
        if (loading || !isShowing) return
        if (lastPreviewSignature != null && lastPreviewSignature == inputSignature()) return
        run(dryRun = true)
    }

    /** Serialized signature of the preview inputs, used to detect stale previews. */
    internal fun inputSignature(): String =
        ObjectMappers.json().writeValueAsString(
            listOf(connectionSupplier(), rulesSupplier(), retentionSupplier())
        )

    private companion object {
        const val PROGRESS_SUBJECT_LENGTH = 40
    }
}

internal class ActionsTableModel : AbstractTableModel() {

    private val columns = listOf(
        I18n.t("apply.column.action"),
        I18n.t("apply.column.subject"),
        I18n.t("apply.column.sender"),
        I18n.t("apply.column.date"),
        I18n.t("apply.column.details")
    )
    private var rows: List<MailAction> = emptyList()

    fun setActions(actions: List<MailAction>) {
        rows = actions
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]
    override fun isCellEditable(row: Int, column: Int): Boolean = false

    override fun getValueAt(row: Int, column: Int): Any {
        val action = rows[row]
        return when (column) {
            0 -> action.type.name
            1 -> action.subject.orEmpty()
            2 -> action.sender.orEmpty()
            3 -> formatDate(action.date)
            else -> detailsOf(action)
        }
    }

    private fun formatDate(date: Instant?): String = date?.let { DATE_FORMAT.format(it) }.orEmpty()

    private fun detailsOf(action: MailAction): String = when {
        action.type == ActionType.MOVE || action.type == ActionType.COPY ->
            I18n.t("apply.toFolder", action.targetFolder.orEmpty())

        action.origin == ActionOrigin.RETENTION ->
            I18n.t("apply.fromFolder", action.sourceFolder.orEmpty())

        else -> ""
    }

    private companion object {
        val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    }
}
