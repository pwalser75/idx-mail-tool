package ch.frostnova.app.mailtool.connector

import ch.frostnova.app.mailtool.config.AccountProperties

interface MailConnector {

    fun connect(properties: AccountProperties): MailAdapter
}