package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import java.lang.reflect.Type
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType

/**
 * Convenience extension function for easily mapping a kotlin property to/from JSON
 */
fun <T : Any, F> Mapper<T>.field(
        property: KProperty1<T, F>,
        name: String = CamelCaseToSnakeCaseFormatter.format(property.name),
        jsonAdapter: JsonAdapter<F>? = null):
        FieldMapping<T, F> {

    // This method is tested in MapperTest

    val propertyExtractor = object : PropertyExtractor<T, F> {
        override val type: Type = property.returnType.javaType
        override fun extractFrom(t: T): F = property.get(t)
    }

    return field(
            name = name,
            valueCanBeNull = property.returnType.isMarkedNullable,
            propertyExtractor = propertyExtractor,
            jsonAdapter = jsonAdapter
    )
}