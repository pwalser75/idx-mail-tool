package ch.frostnova.app.mailtool.gui.panel

import ch.frostnova.app.mailtool.config.AccountProperties

/**
 * A configuration section shown on the right-hand side of the setup window.
 */
interface SetupPanel {

    /** Loads the given account into the editors. */
    fun load(account: AccountProperties)

    /** Writes the edited values back into the given account. */
    fun readInto(account: AccountProperties)
}
