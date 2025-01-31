package app.softwork.serialization.csv

import kotlinx.serialization.*
import kotlinx.serialization.modules.*

/**
 * [RFC-4180](https://datatracker.ietf.org/doc/html/rfc4180)
 */
@ExperimentalSerializationApi
public sealed class CSVFormat(
    private val separator: String,
    private val lineSeparator: String,
    override val serializersModule: SerializersModule
) : StringFormat {
    private class Custom(separator: String, lineSeparator: String, serializersModule: SerializersModule) :
        CSVFormat(separator, lineSeparator, serializersModule)

    public companion object Default : CSVFormat(
        ",", "\n", EmptySerializersModule()
    ) {
        public operator fun invoke(
            separator: String = ",",
            lineSeparator: String = "\n",
            serializersModule: SerializersModule = EmptySerializersModule()
        ): CSVFormat =
            Custom(separator, lineSeparator, serializersModule)
    }

    public inline fun <reified T> decodeFromString(string: String): T =
        decodeFromString(serializersModule.serializer(), string, true)

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        decodeFromString(deserializer, string, true)

    public fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String, withHeader: Boolean): T {
        require(deserializer.descriptor.canBeFlattened()) { "Non-trivial nested collections are not supported" }
        val lines = string.split(lineSeparator)
        val data = if (withHeader) {
            val deserializerNames = deserializer.descriptor.names
            val csvNames = lines.first().split(separator).asSequence()
            require(deserializerNames.zip(csvNames).all { it.first == it.second }) {
                "Header names and their order should match those of the structure"
            }
            lines.drop(1).map { it.split(separator) }
        } else {
            lines.map { it.split(separator) }
        }
        return deserializer.deserialize(
            decoder = CSVDecoder(
                data = data,
                serializersModule = serializersModule
            )
        )
    }

    public inline fun <reified T> encodeToString(value: T, withHeader: Boolean = true): String =
        encodeToString(serializersModule.serializer(), value, withHeader)

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String =
        encodeToString(serializer, value, true)

    public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, withHeader: Boolean): String {
        require(serializer.descriptor.canBeFlattened()) { "Non-trivial nested collections are not supported" }
        return buildString {
            var afterFirst = false

            if (withHeader) {
                serializer.descriptor.names.forEach {
                    if (afterFirst) {
                        append(separator)
                    }
                    append(it)
                    afterFirst = true
                }
                append(lineSeparator)
            }

            serializer.serialize(
                encoder = CSVEncoder(this, separator, lineSeparator, serializersModule),
                value = value
            )
        }.trimEnd()
    }
}
