package ch.frostnova.app.mailtool.gui

import javax.swing.BorderFactory
import javax.swing.JLayeredPane
import javax.swing.JRootPane
import javax.swing.Timer

/**
 * Shows a short-lived, non-modal toast message near the bottom of the window.
 */
object Toast {

    fun show(rootPane: JRootPane, message: String, durationMs: Int = 2500) {
        val layered = rootPane.layeredPane
        val toast = PillLabel().apply {
            font = Theme.LABEL_FONT.deriveFont(font.style, 12f)
            text = message
            pillColor = Theme.SURFACE_ALT
            foreground = Theme.TEXT
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                border
            )
        }
        toast.size = toast.preferredSize
        toast.setLocation(
            (layered.width - toast.width) / 2,
            layered.height - toast.height - 28
        )
        layered.add(toast, JLayeredPane.POPUP_LAYER)
        layered.repaint()

        Timer(durationMs) {
            layered.remove(toast)
            layered.repaint()
        }.apply {
            isRepeats = false
            start()
        }
    }
}
