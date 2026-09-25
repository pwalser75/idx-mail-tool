package ch.frostnova.app.mailtool.gui

import java.awt.Cursor
import java.awt.Dimension
import java.awt.Frame
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel

internal const val EDGE_NORTH = 1
internal const val EDGE_SOUTH = 2
internal const val EDGE_WEST = 4
internal const val EDGE_EAST = 8

/**
 * Determines which window edge (if any) the point at ([x], [y]) is near.
 */
internal fun edgeAt(x: Int, y: Int, width: Int, height: Int, margin: Int): Int {
    var edge = 0
    if (x < margin) edge = edge or EDGE_WEST else if (x >= width - margin) edge = edge or EDGE_EAST
    if (y < margin) edge = edge or EDGE_NORTH else if (y >= height - margin) edge = edge or EDGE_SOUTH
    return edge
}

internal fun resizeCursor(edge: Int): Int = when (edge) {
    EDGE_NORTH -> Cursor.N_RESIZE_CURSOR
    EDGE_SOUTH -> Cursor.S_RESIZE_CURSOR
    EDGE_WEST -> Cursor.W_RESIZE_CURSOR
    EDGE_EAST -> Cursor.E_RESIZE_CURSOR
    EDGE_NORTH or EDGE_WEST -> Cursor.NW_RESIZE_CURSOR
    EDGE_NORTH or EDGE_EAST -> Cursor.NE_RESIZE_CURSOR
    EDGE_SOUTH or EDGE_WEST -> Cursor.SW_RESIZE_CURSOR
    EDGE_SOUTH or EDGE_EAST -> Cursor.SE_RESIZE_CURSOR
    else -> Cursor.DEFAULT_CURSOR
}

/**
 * Computes the new window bounds when dragging [edge] by ([dx], [dy]) from [start],
 * enforcing the given [minimum] size.
 */
internal fun resizeBounds(start: Rectangle, edge: Int, dx: Int, dy: Int, minimum: Dimension): Rectangle {
    var x = start.x
    var y = start.y
    var width = start.width
    var height = start.height

    if (edge and EDGE_WEST != 0) {
        x += dx
        width -= dx
    }
    if (edge and EDGE_NORTH != 0) {
        y += dy
        height -= dy
    }
    if (edge and EDGE_EAST != 0) width += dx
    if (edge and EDGE_SOUTH != 0) height += dy

    if (width < minimum.width) {
        if (edge and EDGE_WEST != 0) x = start.x + start.width - minimum.width
        width = minimum.width
    }
    if (height < minimum.height) {
        if (edge and EDGE_NORTH != 0) y = start.y + start.height - minimum.height
        height = minimum.height
    }
    return Rectangle(x, y, width, height)
}

/**
 * Makes an undecorated frame resizable by dragging its edges and corners.
 * Installed as the frame's glass pane; it only claims the outer [margin] pixels,
 * so all events in the middle of the window pass through to the real components.
 */
class WindowResizer(private val frame: Frame, private val margin: Int = 6) : JPanel() {

    private var edge = 0
    private var startBounds: Rectangle? = null
    private var startPoint: Point? = null

    init {
        isOpaque = false
        cursor = Cursor.getDefaultCursor()

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                edge = edgeAt(event.x, event.y, width, height, margin)
                if (edge != 0) {
                    startBounds = frame.bounds
                    startPoint = event.locationOnScreen
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                edge = 0
                startBounds = null
                startPoint = null
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                cursor = Cursor.getPredefinedCursor(edgeAt(event.x, event.y, width, height, margin).let(::resizeCursor))
            }

            override fun mouseDragged(event: MouseEvent) {
                val bounds = startBounds ?: return
                val point = startPoint ?: return
                if (edge == 0) return
                val dx = event.locationOnScreen.x - point.x
                val dy = event.locationOnScreen.y - point.y
                frame.bounds = resizeBounds(bounds, edge, dx, dy, frame.minimumSize)
            }
        })
    }

    override fun contains(x: Int, y: Int): Boolean =
        x < margin || y < margin || x >= width - margin || y >= height - margin
}
