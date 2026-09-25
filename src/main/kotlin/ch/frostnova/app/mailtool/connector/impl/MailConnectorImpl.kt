package ch.frostnova.app.mailtool.connector.impl

import ch.frostnova.app.mailtool.config.AccountProperties
import ch.frostnova.app.mailtool.connector.MailAdapter
import ch.frostnova.app.mailtool.connector.MailConnector
import jakarta.mail.Session
import java.util.Properties

class MailConnectorImpl : MailConnector {

    override fun connect(properties: AccountProperties): MailAdapter {

        val props = Properties().apply {
            put("mail.store.protocol", properties.protocol)
            put("mail.${properties.protocol}.host", properties.host)
            put("mail.${properties.protocol}.port", properties.port.toString())
            put("mail.${properties.protocol}.ssl.enable", properties.tlsEnabled.toString())
            put("mail.${properties.protocol}.connectiontimeout", CONNECT_TIMEOUT_MS)
            put("mail.${properties.protocol}.timeout", READ_TIMEOUT_MS)
            put("mail.${properties.protocol}.writetimeout", WRITE_TIMEOUT_MS)
        }

        val session = Session.getInstance(props)
        return MailAdapterImpl(session.store.also {
            it.connect(
                properties.host,
                properties.username,
                properties.password
            )
        })
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = "15000"
        const val READ_TIMEOUT_MS = "30000"
        const val WRITE_TIMEOUT_MS = "30000"
    }
}
