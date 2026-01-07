package ch.frostnova.app.mailtool.config

import ch.frostnova.app.mailtool.config.MailRuleAction.COPY
import ch.frostnova.app.mailtool.config.MailRuleAction.DELETE
import ch.frostnova.app.mailtool.config.MailRuleAction.MOVE
import ch.frostnova.app.mailtool.util.Interval
import ch.frostnova.app.mailtool.util.ObjectMappers
import ch.frostnova.app.mailtool.util.validate
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ConfigurationPropertiesTest {

    private val properties = ConfigurationProperties().apply {
        accounts = mapOf("test" to AccountProperties().apply {
            protocol = "imaps"
            host = "imap.host.org"
            port = 12345
            tlsEnabled = true
            username = "user@host.org"
            password = "Secret#007"
            rules = listOf(
                MailRule().apply {
                    senders = listOf("company.com")
                    action = MOVE
                    folder = "business"
                },
                MailRule().apply {
                    senders = listOf("digitec.ch", "galaxus.ch")
                    action = MOVE
                    folder = "shopping"
                },
                MailRule().apply {
                    senders = listOf("john.doe@something.com", "mary-jane@gmail.com")
                    action = COPY
                    folder = "friends"
                },
                MailRule().apply {
                    senders = listOf("info@inovagoals.com")
                    action = DELETE
                }
            )
            dataRetention = listOf(
                DataRetentionProperties().apply {
                    folder = "Inbox"
                    retentionPeriod = Interval(365)
                },
                DataRetentionProperties().apply {
                    folder = "Sent"
                    retentionPeriod = Interval(90)
                },
                DataRetentionProperties().apply {
                    folder = "Spam"
                    retentionPeriod = Interval(0, 23, 59, 59)
                }
            )
        })
    }

    @Test
    fun `should validate properties OK`() {
        assertThatCode { validate(properties) }.doesNotThrowAnyException()
    }

    @Test
    fun `should validate properties with errors`() {
        val invalidProperties = ConfigurationProperties().apply {
            accounts = mapOf("test" to AccountProperties().apply {
                protocol = ""
                host = "imap.host.org"
                port = 9999999
                tlsEnabled = true
                username = " "
                password = null
                rules = listOf(
                    MailRule(),
                    MailRule().apply {
                        senders = listOf("")
                    }
                )
                dataRetention = listOf(
                    DataRetentionProperties()
                )
            })
        }
        assertThatThrownBy { validate(invalidProperties) }
            .isInstanceOfSatisfying(ValidationException::class.java) { ex ->
                assertThat(ex.message).isEqualTo(
                    """
                        accounts[test].dataRetention[0].folder: (null) must not be blank
                        accounts[test].dataRetention[0].retentionPeriod: (null) must not be null
                        accounts[test].password: (null) must not be blank
                        accounts[test].port: (9999999) must be less than or equal to 65564
                        accounts[test].protocol: () must not be blank
                        accounts[test].rules[0].action: (null) must not be null
                        accounts[test].rules[0].senders: ([]) must not be empty
                        accounts[test].rules[1].action: (null) must not be null
                        accounts[test].username: ( ) must not be blank
                """.trimIndent()
                )
            }
    }

    @Test
    fun `should serialize and deserialize properties using YAML`() {
        testSerialization(properties, ObjectMappers.yaml())
    }

    @Test
    fun `should serialize and deserialize properties using JSON`() {
        testSerialization(properties, ObjectMappers.json())
    }
}

fun <T : Any> testSerialization(value: T, objectMapper: ObjectMapper) {
    val serialized = objectMapper.writeValueAsString(value)
    println(serialized)
    val deserialized = objectMapper.readValue(serialized, ConfigurationProperties::class.java)
    assertThat(deserialized).usingRecursiveComparison().isEqualTo(value)
}