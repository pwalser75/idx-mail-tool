package ch.frostnova.app.mailtool.config

import ch.frostnova.app.mailtool.util.Interval
import ch.frostnova.app.mailtool.util.ObjectMappers
import ch.frostnova.app.mailtool.util.serializer.IntervalDeserializer
import ch.frostnova.app.mailtool.util.serializer.IntervalSerializer
import ch.frostnova.app.mailtool.util.serializer.StringListDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.io.File

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
    @Max(65564)
    var port: Int = 993

    var tlsEnabled = true

    @NotBlank
    var username: String? = null

    @NotBlank
    var password: String? = null

    @Valid
    var dataRetention: List<DataRetentionProperties> = emptyList()

    @Valid
    var rules: List<MailRule> = emptyList()
}

class DataRetentionProperties {
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
}

enum class MailRuleAction {
    MOVE,
    COPY,
    DELETE
}

fun readConfigProperties(): ConfigurationProperties? {
    val userHome = System.getProperty("user.home")
    val configFile = File(File(userHome), ".idx-mail-tool.yaml")
    if (!configFile.exists()) {
        return null
    }
    return ObjectMappers.forResource(configFile).readValue(configFile, ConfigurationProperties::class.java)
}