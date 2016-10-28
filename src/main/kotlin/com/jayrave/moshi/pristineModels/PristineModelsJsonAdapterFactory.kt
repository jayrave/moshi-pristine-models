package com.jayrave.moshi.pristineModels

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type
import java.util.*

class PristineModelsJsonAdapterFactory private constructor(
        private val mappersForModels: Map<Class<*>, Mapper<*>>,
        private val mapperCreatorsForModels: Map<Class<*>, () -> Mapper<*>>) :
        JsonAdapter.Factory {

    /**
     * Used by [Moshi]. Library users needn't usually call this method directly
     */
    override fun create(
            type: Type, annotations: MutableSet<out Annotation>?, moshi: Moshi):
            JsonAdapter<*>? {

        return mappersForModels[type]?.buildJsonAdapter(moshi) ?:
                mapperCreatorsForModels[type]?.invoke()?.buildJsonAdapter(moshi)
    }



    class Builder {

        private var mappersForModels: HashMap<Class<*>, Mapper<*>>? = null
        private var mapperCreatorsForModels: HashMap<Class<*>, () -> Mapper<*>>? = null

        /**
         * Register a [mapper] to be used for a [clazz]. Calling this again for a [clazz] that
         * has been registered will overwrite the existing mapper
         */
        fun <T : Any> add(clazz: Class<T>, mapper: Mapper<T>): Builder {
            if (mappersForModels == null) {
                mappersForModels = HashMap()
            }

            mappersForModels!!.put(clazz, mapper)
            return this
        }


        /**
         * Register a [mapperCreator] to be used to lazily create the mapper for a [clazz].
         * Calling this again for a [clazz] that has been registered will overwrite the
         * existing mapper
         */
        fun <T : Any> add(clazz: Class<T>, mapperCreator: () -> Mapper<T>): Builder {
            if (mapperCreatorsForModels == null) {
                mapperCreatorsForModels = HashMap()
            }

            mapperCreatorsForModels!!.put(clazz, mapperCreator)
            return this
        }


        /**
         * Builds a factory that provides json adapters for all the registered types
         *
         * *Note:* If both a mapper & a mapper creator are registered for the same class,
         * the mapper has precedence over the mapper creator
         */
        fun build(): PristineModelsJsonAdapterFactory {
            val mappers: Map<Class<*>, Mapper<*>> = when (mappersForModels) {
                null -> emptyMap()
                else -> HashMap(mappersForModels)
            }

            val mapperCreators: Map<Class<*>, () -> Mapper<*>> = when (mapperCreatorsForModels) {
                null -> emptyMap()
                else -> HashMap(mapperCreatorsForModels)
            }

            return PristineModelsJsonAdapterFactory(mappers, mapperCreators)
        }
    }
}