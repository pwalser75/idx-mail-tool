package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Rectangle

class WindowResizerTest {

    private val width = 1000
    private val height = 600
    private val margin = 6

    @Test
    fun `detects edges and corners`() {
        assertThat(edgeAt(500, 300, width, height, margin)).isEqualTo(0)
        assertThat(edgeAt(3, 300, width, height, margin)).isEqualTo(EDGE_WEST)
        assertThat(edgeAt(997, 300, width, height, margin)).isEqualTo(EDGE_EAST)
        assertThat(edgeAt(500, 3, width, height, margin)).isEqualTo(EDGE_NORTH)
        assertThat(edgeAt(500, 597, width, height, margin)).isEqualTo(EDGE_SOUTH)
        assertThat(edgeAt(3, 3, width, height, margin)).isEqualTo(EDGE_NORTH or EDGE_WEST)
        assertThat(edgeAt(997, 597, width, height, margin)).isEqualTo(EDGE_SOUTH or EDGE_EAST)
    }

    @Test
    fun `maps edges to resize cursors`() {
        assertThat(resizeCursor(0)).isEqualTo(Cursor.DEFAULT_CURSOR)
        assertThat(resizeCursor(EDGE_WEST)).isEqualTo(Cursor.W_RESIZE_CURSOR)
        assertThat(resizeCursor(EDGE_NORTH or EDGE_EAST)).isEqualTo(Cursor.NE_RESIZE_CURSOR)
        assertThat(resizeCursor(EDGE_SOUTH or EDGE_WEST)).isEqualTo(Cursor.SW_RESIZE_CURSOR)
    }

    @Test
    fun `resizes from the south east keeping the origin`() {
        val start = Rectangle(100, 100, 800, 500)

        val result = resizeBounds(start, EDGE_SOUTH or EDGE_EAST, 120, 80, Dimension(400, 300))

        assertThat(result).isEqualTo(Rectangle(100, 100, 920, 580))
    }

    @Test
    fun `resizes from the north west moving the origin`() {
        val start = Rectangle(100, 100, 800, 500)

        val result = resizeBounds(start, EDGE_NORTH or EDGE_WEST, 50, 30, Dimension(400, 300))

        assertThat(result).isEqualTo(Rectangle(150, 130, 750, 470))
    }

    @Test
    fun `enforces the minimum size when shrinking from the north west`() {
        val start = Rectangle(100, 100, 800, 500)

        val result = resizeBounds(start, EDGE_NORTH or EDGE_WEST, 900, 600, Dimension(400, 300))

        assertThat(result).isEqualTo(Rectangle(500, 300, 400, 300))
    }
}
