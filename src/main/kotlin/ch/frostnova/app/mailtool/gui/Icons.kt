package ch.frostnova.app.mailtool.gui

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.Icon

enum class IconType {
    PLUS,
    EDIT,
    TRASH,
    LINK,
    FOLDER,
    FILTER,
    CLOCK
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
                IconType.LINK -> drawLink(g)
                IconType.FOLDER -> drawFolder(g)
                IconType.FILTER -> drawFilter(g)
                IconType.CLOCK -> drawClock(g)
            }
        } finally {
            g.dispose()
        }
    }

    private fun outline(g: Graphics2D) {
        g.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    }

    private fun drawPlus(g: Graphics2D) {
        g.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.drawLine(8, 3, 8, 13)
        g.drawLine(3, 8, 13, 8)
    }

    private fun drawEdit(g: Graphics2D) {
        outline(g)
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
        outline(g)
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

    /** A globe, representing the server connection. */
    private fun drawLink(g: Graphics2D) {
        outline(g)
        val r = 5.4f
        val cx = 8f
        val cy = 8f
        g.draw(Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r))
        g.draw(Ellipse2D.Float(cx - r / 2, cy - r, r, 2 * r))
        g.draw(Line2D.Float(cx - r, cy, cx + r, cy))
    }

    /** A folder with a tab. */
    private fun drawFolder(g: Graphics2D) {
        outline(g)
        val path = Path2D.Float().apply {
            moveTo(2.6f, 12.8f)
            lineTo(2.6f, 4.4f)
            lineTo(6.2f, 4.4f)
            lineTo(7.6f, 6.4f)
            lineTo(13.4f, 6.4f)
            lineTo(13.4f, 12.8f)
            closePath()
        }
        g.draw(path)
    }

    /** A funnel, representing filtering/sorting rules. */
    private fun drawFilter(g: Graphics2D) {
        outline(g)
        val path = Path2D.Float().apply {
            moveTo(2.5f, 3.6f)
            lineTo(13.5f, 3.6f)
            lineTo(9.5f, 8.5f)
            lineTo(9.5f, 13.0f)
            lineTo(6.5f, 13.0f)
            lineTo(6.5f, 8.5f)
            closePath()
        }
        g.draw(path)
    }

    /** A clock, representing retention time. */
    private fun drawClock(g: Graphics2D) {
        outline(g)
        g.draw(Ellipse2D.Float(2.6f, 2.6f, 10.8f, 10.8f))
        g.draw(Line2D.Float(8f, 8f, 8f, 4.7f))
        g.draw(Line2D.Float(8f, 8f, 10.6f, 9.4f))
    }
}

fun plusIcon(color: Color, size: Int = 16): Icon = VectorIcon(IconType.PLUS, color, size)

fun editIcon(color: Color, size: Int = 16): Icon = VectorIcon(IconType.EDIT, color, size)

fun trashIcon(color: Color, size: Int = 16): Icon = VectorIcon(IconType.TRASH, color, size)
