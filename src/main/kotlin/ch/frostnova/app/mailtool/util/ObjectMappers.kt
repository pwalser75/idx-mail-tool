package ch.frostnova.app.mailtool.util

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
import com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Lazy-created object mappers for various serialization formats.
 *
 * @author pwalser
 */
object ObjectMappers {

    private val objectMappers = ConcurrentHashMap<Type, ObjectMapper>()

    fun forResource(url: URL) = forResource(url.toString())
    fun forResource(file: File) = forResource(file.name)

    fun forResource(name: String): ObjectMapper {
        val nameLowerCase = name.lowercase()
        if (nameLowerCase.endsWith(".json")) return json()
        if (nameLowerCase.endsWith(".yaml") || nameLowerCase.endsWith(".yml")) return yaml()
        throw IllegalArgumentException("$name: unsupported file format, only json and yaml|yml are supported")
    }

    fun json(): ObjectMapper {
        return objectMappers.computeIfAbsent(Type.JSON) {
            configure(ObjectMapper())
        }
    }

    fun yaml(): ObjectMapper {
        return objectMappers.computeIfAbsent(Type.YAML) {
            configure(
                ObjectMapper(
                    YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                )
            ).apply {
                propertyNamingStrategy = PropertyNamingStrategies.KEBAB_CASE
            }
        }
    }

    fun merge(vararg nodes: JsonNode): JsonNode {
        var merged = NullNode.getInstance() as JsonNode
        nodes.forEach { node ->
            merged = mergeNodes(merged, node)
        }
        return merged
    }

    private fun mergeNodes(main: JsonNode, overlay: JsonNode): JsonNode {
        return when (main) {
            is NullNode -> overlay
            is BooleanNode -> overlay
            is NumericNode -> overlay
            is TextNode -> overlay
            is ArrayNode ->
                if (overlay is ArrayNode) {
                    for (i in 0 until overlay.size()) {
                        main.add(overlay[i])
                    }
                    main
                } else overlay

            is ObjectNode ->
                if (overlay is ObjectNode) {
                    val mainProperties = main.properties().map { it.key }
                    val addProperties = overlay.properties().map { it.key }
                    (mainProperties + addProperties).distinct().forEach {
                        if (mainProperties.contains(it) && addProperties.contains(it)) {
                            main.replace(it, mergeNodes(main[it], overlay[it]))
                        } else if (addProperties.contains(it)) {
                            main.replace(it, overlay[it])
                        }
                    }
                    main
                } else overlay

            else -> main
        }
    }

    private fun configure(mapper: ObjectMapper): ObjectMapper {
        return mapper
            .setAnnotationIntrospector(JacksonAnnotationIntrospector())
            .registerModule(JavaTimeModule())
            .setDateFormat(StdDateFormat())
            .enable(INDENT_OUTPUT)
            .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            .enable(FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(NON_EMPTY)
    }

    internal enum class Type {
        JSON, //  JavaScript Object Notation
        YAML //  Yet Another Markup Language
    }
}
