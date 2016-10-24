package com.jayrave.moshi.pristineModels

interface NameFormatter {

    /**
     * @throws [NameFormatException] whenever a [NameFormatter] finds something wrong with
     * the data it has to work with
     */
    @Throws(NameFormatException::class)
    fun format(input: String): String


    class NameFormatException(message: String?) : RuntimeException(message)
}