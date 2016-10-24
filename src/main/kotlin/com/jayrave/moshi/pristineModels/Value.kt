package com.jayrave.moshi.pristineModels

/**
 * Extracts a value using the passed in [FieldMapping]
 */
interface Value<T : Any> {
    infix fun <F> of(fieldMapping: FieldMapping<T, F>): F
}