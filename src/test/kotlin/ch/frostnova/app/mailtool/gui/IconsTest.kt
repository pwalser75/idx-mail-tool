package ch.frostnova.app.mailtool.gui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class IconsTest {

    @Test
    fun `every icon type has the requested size`() {
        IconType.entries.forEach { type ->
            val icon = VectorIcon(type, Color.WHITE, 16)
            assertThat(icon.iconWidth).isEqualTo(16)
            assertThat(icon.iconHeight).isEqualTo(16)
        }
    }

    @Test
    fun `every icon type paints visible pixels`() {
        IconType.entries.forEach { type ->
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            try {
                VectorIcon(type, Color.WHITE, 16).paintIcon(null, g, 0, 0)
            } finally {
                g.dispose()
            }

            val painted = (0 until 16).any { x ->
                (0 until 16).any { y -> (image.getRGB(x, y) ushr 24) != 0 }
            }
            assertThat(painted).describedAs("$type should paint something").isTrue()
        }
    }
}
