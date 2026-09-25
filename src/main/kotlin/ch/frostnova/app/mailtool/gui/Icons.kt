package ch.frostnova.app.mailtool.gui

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import javax.swing.Icon

enum class IconType {
    PLUS,
    EDIT,
    TRASH
}

/**
 * A small vector icon drawn with Java2D, so it stays crisp at any size and
 * does not require an external icon library.
 */
class VectorIcon(
    private val type: IconType,
    private val color: Color,
    private val size: Int = 16
) : Icon {

    override fun getIconWidth(): Int = size
    override fun getIconHeight(): Int = size

    override fun paintIcon(component: Component?, graphics: Graphics, x: Int, y: Int) {
        val g = graphics.create() as Graphics2D
        try {
            g.translate(x, y)
            g.scale(size / 16.0, size / 16.0)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.color = color
            when (type) {
                IconType.PLUS -> drawPlus(g)
                IconType.EDIT -> drawEdit(g)
                IconType.TRASH -> drawTrash(g)
            }
        } finally {
            g.dispose()
        }
    }

    private fun drawPlus(g: Graphics2D) {
        g.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.drawLine(8, 3, 8, 13)
        g.drawLine(3, 8, 13, 8)
    }

    private fun drawEdit(g: Graphics2D) {
        g.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val tip = Path2D.Float().apply {
            moveTo(3.2f, 12.8f)
            lineTo(7.7f, 10.9f)
            lineTo(5.1f, 8.3f)
            closePath()
        }
        val body = Path2D.Float().apply {
            moveTo(7.7f, 10.9f)
            lineTo(14.0f, 4.6f)
            lineTo(11.4f, 2.0f)
            lineTo(5.1f, 8.3f)
            closePath()
        }
        g.draw(tip)
        g.draw(body)
    }

    private fun drawTrash(g: Graphics2D) {
        g.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.drawLine(3, 4, 13, 4)
        val handle = Path2D.Float().apply {
            moveTo(6.3f, 4f)
            lineTo(6.3f, 2.8f)
            lineTo(9.7f, 2.8f)
            lineTo(9.7f, 4f)
        }
        g.draw(handle)
        val body = Path2D.Float().apply {
            moveTo(4.8f, 5.4f)
            lineTo(5.4f, 12.7f)
            quadTo(5.5f, 13.6f, 6.4f, 13.6f)
            lineTo(9.6f, 13.6f)
            quadTo(10.5f, 13.6f, 10.6f, 12.7f)
            lineTo(11.2f, 5.4f)
        }
        g.draw(body)
        g.drawLine(7, 7, 7, 12)
        g.drawLine(9, 7, 9, 12)
    }
}

fun plusIcon(color: Color, size: Int = 16): Icon = VectorIcon(IconType.PLUS, color, size)

fun editIcon(color: Color, size: Int = 16): Icon = VectorIcon(IconType.EDIT, color, size)

fun trashIcon(color: Color, size: Int = 16): Icon = VectorIcon(IconType.TRASH, color, size)
