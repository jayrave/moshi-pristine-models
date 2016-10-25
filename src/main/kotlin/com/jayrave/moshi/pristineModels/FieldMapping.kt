package com.jayrave.moshi.pristineModels

/**
 * Knows how to map a property to/from a JSON field
 */
interface FieldMapping<in T : Any, out F> {

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
}