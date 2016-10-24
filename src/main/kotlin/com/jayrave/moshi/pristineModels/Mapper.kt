package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KProperty1

abstract class Mapper<T : Any>(
        private val nameFormatter: NameFormatter = CamelCaseToSnakeCaseFormatter()) {

    private var jsonAdapter: JsonAdapter<T>? = null
    private val jsonAdapterBuilt = AtomicBoolean(false)
    private val fieldMappings = LinkedHashMap<String, FieldMappingImpl<T, *>>()

    /**
     * @param [property] this field corresponds to
     * @param [name] of the JSON field. If `null`, name formatter is used to format
     * property name into field name
     * @param [jsonAdapter] to be used to map this field from/to JSON. If `null`, whatever
     * [Moshi] provides is used
     */
    fun <F> field(
            property: KProperty1<T, F>, name: String = nameFormatter.format(property.name),
            jsonAdapter: JsonAdapter<F>? = null):
            FieldMapping<T, F> {

        return when (jsonAdapterBuilt.get()) {
            true -> throw IllegalStateException("Trying to add a field mapping too late")
            else -> {
                val fieldMapping = FieldMappingImpl(name, property, jsonAdapter)
                fieldMappings.put(name, fieldMapping)
                fieldMapping
            }
        }
    }


    internal fun buildJsonAdapter(moshi: Moshi): JsonAdapter<T> {
        synchronized(jsonAdapterBuilt) { // Use flag to not have a separate lock object

            // Do the following only if it hasn't been already done
            if (jsonAdapterBuilt.compareAndSet(false, true)) {

                // Let all the field mappings acquire the adapter they need
                fieldMappings.values.forEach { it.acquireJsonAdapter(moshi) }

                // Build the adapter this mapper needs
                jsonAdapter = JsonAdapterForMapper()
            }
        }

        return jsonAdapter!!
    }


    /**
     * @return A object representing a single JSON object this class deals with.
     * The values should be extracted out of the passed in [Value] instance
     */
    abstract fun create(value: Value<T>): T



    private inner class JsonAdapterForMapper : JsonAdapter<T>() {
        override fun fromJson(reader: JsonReader): T {

            // Clear cached values in field mappings
            fieldMappings.values.forEach { it.clearLastReadValueInCurrentThread() }

            // Start object
            reader.beginObject()

            // Extract all the possible field values
            while (reader.hasNext()) {
                val fieldName = reader.nextName()
                val fieldMapping = fieldMappings[fieldName]
                when (fieldMapping) {
                    null -> reader.skipValue()
                    else -> fieldMapping.read(reader)
                }
            }

            // End object
            reader.endObject()

            // Build & return object
            return create(object : Value<T> {
                override fun <F> of(fieldMapping: FieldMapping<T, F>): F {
                    return when (fieldMapping) {
                        is FieldMappingImpl -> fieldMapping.lastReadValueInCurrentThread()
                        else -> throw IllegalArgumentException(
                                "Pass in ${FieldMapping::class.java.canonicalName} " +
                                        "built by calling Mapper#field"
                        )
                    }
                }
            })
        }

        override fun toJson(writer: JsonWriter, value: T) {
            // Start object
            writer.beginObject()

            // Write each field
            fieldMappings.values.forEach {
                writer.name(it.name)
                it.write(writer, value)
            }

            // End object
            writer.endObject()
        }
    }
}