package com.jayrave.moshi.pristineModels

/**
 * Extracts a value using the passed in [FieldMapping]
 */
interface Value<out T : Any> {
    infix fun <F> of(fieldMapping: FieldMapping<T, F>): F
}