package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage

class PillTest {

    @Test
    fun `dot icon has the requested size and paints its colour`() {
        val icon = DotIcon(Color(0xFF0000), 8)
        assertThat(icon.iconWidth).isEqualTo(8)
        assertThat(icon.iconHeight).isEqualTo(8)

        val image = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            icon.paintIcon(null, g, 0, 0)
        } finally {
            g.dispose()
        }
        assertThat(image.getRGB(4, 4) and 0xFFFFFF).isEqualTo(0xFF0000)
    }

    @Test
    fun `pill label paints its background colour`() {
        val pill = PillLabel().apply {
            text = "X"
            pillColor = Color(0x123456)
            size = Dimension(60, 20)
        }

        val image = BufferedImage(60, 20, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            pill.paint(g)
        } finally {
            g.dispose()
        }
        assertThat(image.getRGB(30, 2) and 0xFFFFFF).isEqualTo(0x123456)
    }
}
