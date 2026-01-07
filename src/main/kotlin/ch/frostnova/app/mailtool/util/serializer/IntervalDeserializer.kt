package ch.frostnova.app.mailtool.util.serializer

import ch.frostnova.app.mailtool.util.Interval
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class IntervalDeserializer : StdDeserializer<Interval>(Interval::class.java) {
    override fun deserialize(p: JsonParser, context: DeserializationContext?): Interval? {
        val string = p.valueAsString
        return if (string.isNullOrBlank()) null else Interval.Companion.parse(string)
    }
}