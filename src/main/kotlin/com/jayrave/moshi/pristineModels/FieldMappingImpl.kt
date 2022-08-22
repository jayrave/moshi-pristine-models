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

    override fun existingJsonAdapterOrAcquireFrom(moshi: Moshi): JsonAdapter<F> {
        return jsonAdapter ?: moshi.adapter(propertyExtractor.type)
    }


    fun acquireJsonAdapterIfRequired(moshi: Moshi) {
        if (jsonAdapter == null) {
            jsonAdapter = moshi.adapter(propertyExtractor.type)
        }
    }

    fun read(reader: JsonReader): F = acquiredAdapter().fromJson(reader)
    fun write(writer: JsonWriter, model: T) = acquiredAdapter().toJson(
            writer, propertyExtractor.extractFrom(model)
    )


    private fun acquiredAdapter(): JsonAdapter<F> {
        return jsonAdapter ?: throw IllegalStateException(
                "Adapter is still not acquired for property: $name"
        )
    }
}