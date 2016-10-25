package com.jayrave.moshi.pristineModels

import java.lang.reflect.Type

interface PropertyExtractor<in T, out F> {

    /**
     * The type of this property
     */
    val type: Type

    /**
     * To extract the property ([F]) from an instance of [T]
     */
    fun extractFrom(t: T): F
}