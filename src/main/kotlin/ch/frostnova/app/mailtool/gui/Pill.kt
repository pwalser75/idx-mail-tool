package ch.frostnova.app.mailtool.gui

import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.border.EmptyBorder

/**
 * A small capsule-shaped label, optionally combined with an icon (e.g. a status dot).
 */
class PillLabel : JLabel() {

    var pillColor: Color = Theme.SURFACE_ALT

    init {
        isOpaque = false
        font = Theme.LABEL_FONT.deriveFont(Font.BOLD, 11f)
        border = EmptyBorder(3, 10, 3, 10)
        horizontalAlignment = CENTER
        iconTextGap = 6
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = pillColor
            g2.fillRoundRect(0, 0, width, height, height, height)
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }
}

/** A small filled circle, e.g. for a status indicator. */
class DotIcon(private val color: Color, private val diameter: Int = 8) : Icon {

    override fun getIconWidth(): Int = diameter
    override fun getIconHeight(): Int = diameter

    override fun paintIcon(component: Component?, graphics: Graphics, x: Int, y: Int) {
        val g2 = graphics.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = color
            g2.fillOval(x, y, diameter, diameter)
        } finally {
            g2.dispose()
        }
    }
}
