package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type
import java.util.*

class PristineModelsJsonAdapterFactory private constructor(
        private val mappersForModels: Map<Class<*>, Mapper<*>>) :
        JsonAdapter.Factory {

    /**
     * Used by [Moshi]. Library users needn't usually call this method directly
     */
    override fun create(
            type: Type, annotations: MutableSet<out Annotation>?, moshi: Moshi):
            JsonAdapter<*>? {

        return mappersForModels[type]?.buildJsonAdapter(moshi)
    }


    class Builder {

        private val mappersForModels = HashMap<Class<*>, Mapper<*>>()

        fun build() = PristineModelsJsonAdapterFactory(mappersForModels)
        fun <T : Any> add(clazz: Class<T>, mapper: Mapper<T>): Builder {
            mappersForModels[clazz] = mapper
            return this
        }
    }
}