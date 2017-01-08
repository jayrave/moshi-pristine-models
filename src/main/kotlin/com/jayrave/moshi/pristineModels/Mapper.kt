package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class Mapper<T : Any> {

    private var jsonAdapter: JsonAdapter<T>? = null
    private val jsonAdapterBuilt = AtomicBoolean(false)
    private val fieldMappings = LinkedHashMap<String, FieldMappingImpl<T, *>>()

    /**
     * @param [name] of the JSON field
     * @param [valueCanBeNull] whether this property & JSON can be null
     * @param [propertyExtractor] to extract the property ([F]) from an instance of [T]
     * @param [jsonAdapter] to be used to map this property from/to JSON. If `null`, whatever
     * [Moshi] provides is used
     */
    fun <F> field(
            name: String, valueCanBeNull: Boolean, propertyExtractor: PropertyExtractor<T, F>,
            jsonAdapter: JsonAdapter<F>? = null): FieldMapping<T, F> {

        return when (jsonAdapterBuilt.get()) {
            true -> throw IllegalStateException("Trying to add a field mapping too late")
            else -> {
                val fieldMapping = FieldMappingImpl(
                        name, valueCanBeNull, propertyExtractor, jsonAdapter
                )

                fieldMappings.put(name, fieldMapping)
                fieldMapping
            }
        }
    }


    internal fun getJsonAdapter(moshi: Moshi): JsonAdapter<T> {
        synchronized(jsonAdapterBuilt) { // Use flag to not have a separate lock object

            // Do the following only if it hasn't been already done
            if (jsonAdapterBuilt.compareAndSet(false, true)) {

                // Let all the field mappings acquire the adapter they need
                fieldMappings.values.forEach { it.acquireJsonAdapterIfRequired(moshi) }

                // Build the adapter this mapper needs (make it null safe too)
                jsonAdapter = JsonAdapterForMapper().nullSafe()
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