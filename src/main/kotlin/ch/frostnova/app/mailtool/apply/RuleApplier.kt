package ch.frostnova.app.mailtool.apply

import ch.frostnova.app.mailtool.config.DataRetentionSettings
import ch.frostnova.app.mailtool.config.MailRule
import ch.frostnova.app.mailtool.config.MailRuleAction.COPY
import ch.frostnova.app.mailtool.config.MailRuleAction.DELETE
import ch.frostnova.app.mailtool.config.MailRuleAction.MOVE
import ch.frostnova.app.mailtool.connector.MailAdapter
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import java.time.Instant

enum class ActionType {
    MOVE,
    COPY,
    DELETE
}

enum class ActionOrigin {
    RULE,
    RETENTION
}

/**
 * Progress information emitted while applying rules: the folder/message currently
 * being examined and how many items have been processed of the total known up
 * front. A [total] of zero means the total is not known yet (indeterminate).
 */
data class ApplyProgress(
    val folder: String? = null,
    val message: String? = null,
    val processed: Int = 0,
    val total: Int = 0
)

/**
 * A single action that was (or would be) taken by the rule engine.
 */
data class MailAction(
    val type: ActionType,
    val origin: ActionOrigin,
    val subject: String?,
    val sender: String?,
    val date: Instant?,
    val size: Int,
    val targetFolder: String? = null,
    val sourceFolder: String? = null
)

/**
 * Applies mail rules and retention policies to a connected mailbox.
 * When [dryRun] is true, nothing is modified: the actions are only reported.
 */
class RuleApplier(
    private val mailAdapter: MailAdapter,
    private val rules: List<MailRule>,
    private val retention: List<DataRetentionSettings>,
    private val now: Instant = Instant.now()
) {

    fun run(
        dryRun: Boolean,
        onProgress: (ApplyProgress) -> Unit = {},
        onAction: (MailAction) -> Unit
    ) {
        onProgress(ApplyProgress())
        val folders = mailAdapter.listFolders()
        folders.forEach { folder -> open(folder, dryRun) }
        try {
            val total = folders.sumOf { messageCount(it) } + retentionTotal(folders)
            var processed = 0
            val progress: (Folder, Message) -> Unit = { folder, message ->
                processed++
                onProgress(
                    ApplyProgress(
                        folder = folder.fullName,
                        message = runCatching { message.subject }.getOrNull(),
                        processed = processed,
                        total = total
                    )
                )
            }
            applyMailRules(folders, dryRun, onAction, progress)
            applyRetention(folders, dryRun, onAction, progress)
        } finally {
            folders.forEach { folder ->
                runCatching { if (folder.isOpen) folder.close(!dryRun) }
            }
        }
    }

    private fun applyMailRules(
        folders: List<Folder>,
        dryRun: Boolean,
        onAction: (MailAction) -> Unit,
        onProgress: (Folder, Message) -> Unit
    ) {
        folders.forEach { folder ->
            if (!folder.isOpen) return@forEach
            listMessages(folder).forEach messageLoop@{ message ->
                onProgress(folder, message)
                if (isDeleted(message)) return@messageLoop
                val rule = firstMatchingRule(message) ?: return@messageLoop
                val target = rule.folder?.let { firstMatchingFolder(folders, it) }
                when (rule.action) {
                    MOVE -> if (target != null && target != message.folder) {
                        onAction(action(ActionType.MOVE, ActionOrigin.RULE, message, targetFolder = target.fullName))
                        if (!dryRun) {
                            message.folder.copyMessages(arrayOf(message), target)
                            message.setFlag(Flags.Flag.DELETED, true)
                        }
                    }

                    COPY -> if (target != null && target != message.folder) {
                        onAction(action(ActionType.COPY, ActionOrigin.RULE, message, targetFolder = target.fullName))
                        if (!dryRun) {
                            message.folder.copyMessages(arrayOf(message), target)
                        }
                    }

                    DELETE -> {
                        onAction(action(ActionType.DELETE, ActionOrigin.RULE, message))
                        if (!dryRun) {
                            message.setFlag(Flags.Flag.DELETED, true)
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun applyRetention(
        folders: List<Folder>,
        dryRun: Boolean,
        onAction: (MailAction) -> Unit,
        onProgress: (Folder, Message) -> Unit
    ) {
        retention.forEach { settings ->
            val period = settings.retentionPeriod ?: return@forEach
            val folderName = settings.folder ?: return@forEach
            val deleteBefore = now.minus(period.toDuration())
            val folder = firstMatchingFolder(folders, folderName) ?: return@forEach
            listMessages(folder).forEach retentionLoop@{ message ->
                onProgress(folder, message)
                if (isDeleted(message)) return@retentionLoop
                val sentDate = runCatching { message.sentDate?.toInstant() }.getOrNull()
                if (sentDate != null && sentDate.isBefore(deleteBefore)) {
                    onAction(
                        action(
                            ActionType.DELETE,
                            ActionOrigin.RETENTION,
                            message,
                            sourceFolder = folder.fullName
                        )
                    )
                    if (!dryRun) {
                        message.setFlag(Flags.Flag.DELETED, true)
                    }
                }
            }
        }
    }

    private fun isDeleted(message: Message): Boolean =
        runCatching { message.isSet(Flags.Flag.DELETED) }.getOrDefault(false)

    private fun open(folder: Folder, dryRun: Boolean) {
        runCatching {
            if (!folder.isOpen && folder.parent != null) {
                folder.open(if (dryRun) Folder.READ_ONLY else Folder.READ_WRITE)
            }
        }
    }

    private fun listMessages(folder: Folder): List<Message> =
        runCatching { mailAdapter.listMessages(folder) }.getOrDefault(emptyList())

    private fun firstMatchingRule(message: Message): MailRule? =
        rules.firstOrNull { rule ->
            runCatching { message.from?.any { from -> rule.senders.any { from.toString().contains(it, ignoreCase = true) } } == true }
                .getOrDefault(false)
        }

    private fun firstMatchingFolder(folders: Collection<Folder>, name: String): Folder? =
        folders.firstOrNull { folder -> folder.name.contains(name, ignoreCase = true) }

    /** Best-effort message count of an open folder, used as the progress total. */
    private fun messageCount(folder: Folder): Int =
        runCatching { folder.messageCount }.getOrDefault(0)

    private fun retentionTotal(folders: List<Folder>): Int =
        retention.sumOf { settings ->
            if (settings.retentionPeriod == null) return@sumOf 0
            settings.folder?.let { firstMatchingFolder(folders, it) }?.let { messageCount(it) } ?: 0
        }

    private fun action(
        type: ActionType,
        origin: ActionOrigin,
        message: Message,
        targetFolder: String? = null,
        sourceFolder: String? = null
    ): MailAction = MailAction(
        type = type,
        origin = origin,
        subject = runCatching { message.subject }.getOrNull(),
        sender = runCatching { message.from?.joinToString(",") }.getOrNull(),
        date = runCatching { message.receivedDate?.toInstant() }.getOrNull(),
        size = runCatching { message.size }.getOrDefault(0),
        targetFolder = targetFolder,
        sourceFolder = sourceFolder
    )
}
