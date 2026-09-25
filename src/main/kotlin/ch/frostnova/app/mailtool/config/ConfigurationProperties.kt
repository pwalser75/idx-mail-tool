package ch.frostnova.app.mailtool.config

import ch.frostnova.app.mailtool.util.Interval
import ch.frostnova.app.mailtool.util.ObjectMappers
import ch.frostnova.app.mailtool.util.serializer.IntervalDeserializer
import ch.frostnova.app.mailtool.util.serializer.IntervalSerializer
import ch.frostnova.app.mailtool.util.serializer.StringListDeserializer
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet

class ConfigurationProperties {
    @Valid
    var accounts: Map<String, AccountProperties> = emptyMap()
}

class AccountProperties {
    @NotBlank
    var protocol: String = "imaps"

    @NotBlank
    var host: String? = null

    @Min(1)
    @Max(65535)
    var port: Int = 993

    var tlsEnabled = true

    @NotBlank
    var username: String? = null

    @NotBlank
    var password: String? = null

    @Valid
    var dataRetention: List<DataRetentionSettings> = emptyList()

    @Valid
    var rules: List<MailRule> = emptyList()
}
class DataRetentionSettings {
    @NotBlank
    var folder: String? = null

    @NotNull
    @JsonSerialize(using = IntervalSerializer::class)
    @JsonDeserialize(using = IntervalDeserializer::class)
    var retentionPeriod: Interval? = null
}

class MailRule {
    @NotEmpty
    @JsonDeserialize(using = StringListDeserializer::class)
    var senders: List<@NotBlank String> = emptyList()

    @NotNull
    var action: MailRuleAction? = null

    var folder: String? = null

    /** A folder is required for MOVE/COPY and must be absent for DELETE. */
    @get:JsonIgnore
    @get:AssertTrue(message = "folder is required for MOVE and COPY, and must be empty for DELETE")
    val isFolderConsistent: Boolean
        get() = when (action) {
            MailRuleAction.MOVE, MailRuleAction.COPY -> !folder.isNullOrBlank()
            MailRuleAction.DELETE -> folder.isNullOrBlank()
            else -> true
        }
}

enum class MailRuleAction {
    MOVE,
    COPY,
    DELETE
}

fun configFile(): File {
    val userHome = System.getProperty("user.home")
    return File(File(userHome), ".idx-mail-tool.yaml")
}

fun readConfigProperties(): ConfigurationProperties? {
    val configFile = configFile()
    if (!configFile.exists()) {
        return null
    }
    return ObjectMappers.forResource(configFile).readValue(configFile, ConfigurationProperties::class.java)
}

fun writeConfigProperties(configuration: ConfigurationProperties) {
    val file = configFile()
    ObjectMappers.forResource(file).writeValue(file, configuration)
    restrictToOwner(file)
}

/** Restricts the configuration file (which holds the password) to the owner, where supported. */
private fun restrictToOwner(file: File) {
    runCatching {
        val view = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView::class.java)
        view?.setPermissions(
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        )
    }
}