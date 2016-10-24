package com.jayrave.moshi.pristineModels

/**
 * Knows how to map to/from a JSON field
 */
interface FieldMapping<T : Any, F> {
    val name: String
}