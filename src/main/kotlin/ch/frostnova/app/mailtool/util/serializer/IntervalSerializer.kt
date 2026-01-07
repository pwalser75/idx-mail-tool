package ch.frostnova.app.mailtool.util.serializer

import ch.frostnova.app.mailtool.util.Interval
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class IntervalSerializer : StdSerializer<Interval>(Interval::class.java) {
    @Throws(IOException::class)
    override fun serialize(value: Interval, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(value.toString())
    }
}