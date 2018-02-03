package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

internal class FieldMappingImpl<in T : Any, F>(
        override val name: String, override val valueCanBeNull: Boolean,
        override val propertyExtractor: PropertyExtractor<T, F>,
        private var jsonAdapter: JsonAdapter<F>? = null) :
        FieldMapping<T, F> {

    private val readValues = ThreadLocal<F?>()

    override fun existingJsonAdapterOrAcquireFrom(moshi: Moshi): JsonAdapter<F> {
        return jsonAdapter ?: moshi.adapter(propertyExtractor.type)
    }


    fun acquireJsonAdapterIfRequired(moshi: Moshi) {
        if (jsonAdapter == null) {
            jsonAdapter = moshi.adapter(propertyExtractor.type)
        }
    }


    @Throws(ValueCanNotBeNullException::class)
    fun lastReadValueInCurrentThread(): F {
        // It seems that even if [F] is a non-null type, this method will happily return a
        // `null` value (they are no nullability checks I could see in the decompiled code.
        // I guess because of erasure there isn't enough information). Therefore do a manual
        // check & throw if required

        val value = readValues.get()

        @Suppress("UNCHECKED_CAST")
        return when (valueCanBeNull) {
            true -> value as F
            else -> when (value) {
                null -> throw ValueCanNotBeNullException("$name can't be null")
                else -> value
            }
        }
    }


    fun clearLastReadValueInCurrentThread() = readValues.remove()
    fun read(reader: JsonReader) = readValues.set(acquiredAdapter().fromJson(reader))
    fun write(writer: JsonWriter, model: T) = acquiredAdapter().toJson(
            writer, propertyExtractor.extractFrom(model)
    )


    private fun acquiredAdapter(): JsonAdapter<F> {
        return jsonAdapter ?: throw IllegalStateException(
                "Adapter is still not acquired for property: $name"
        )
    }



    /**
     * Thrown when `null` seems to be the value for a non-null type
     */
    internal class ValueCanNotBeNullException(message: String) : RuntimeException(message)
}