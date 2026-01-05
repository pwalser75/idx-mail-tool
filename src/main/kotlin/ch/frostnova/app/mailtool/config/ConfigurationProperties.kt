package ch.frostnova.app.mailtool.config

import ch.frostnova.app.mailtool.util.ObjectMappers
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.io.File

class ConfigurationProperties {
    val accounts: Map<String, @Valid AccountProperties> = emptyMap()
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
}

fun readConfigProperties(): ConfigurationProperties? {
    val userHome = System.getProperty("user.home")
    val configFile = File(File(userHome), ".idx-mail-tool.yaml")
    if (!configFile.exists()) {
        return null
    }
    return ObjectMappers.forResource(configFile).readValue(configFile, ConfigurationProperties::class.java)
}