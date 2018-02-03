package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

/**
 * Knows how to map a property to/from a JSON field
 */
interface FieldMapping<in T : Any, F> {

    /**
     * [name] of the JSON field
     */
    val name: String

    /**
     * Whether this property & JSON can be null
     */
    val valueCanBeNull: Boolean

    /**
     * To extract the property ([F]) from an instance of [T]
     */
    val propertyExtractor: PropertyExtractor<T, F>

    /**
     * @return [JsonAdapter] that already exists (may be an adapter was passed
     * in while building this mapping) or find one from [moshi] that can
     * handle [F]
     */
    fun existingJsonAdapterOrAcquireFrom(moshi: Moshi): JsonAdapter<F>
}