package com.jayrave.moshi.pristineModels

/**
 * Formats strings from camelCase to snake_case
 *
 * Examples:
 *      - counter to counter
 *      - intCounter to int_counter
 *      - intCounter1 to int_counter_1
 *
 */
class CamelCaseToSnakeCaseFormatter : NameFormatter {

    @Throws(NameFormatter.NameFormatException::class)
    override fun format(input: String): String {
        return if (input.isEmpty()) {
            throw NameFormatter.NameFormatException("Name is empty!! can't format")

        } else {
            val sb = StringBuilder(input.length + 4) // Some extra space for connectors
            sb.append(Character.toLowerCase(input.first()))
            for (index in 1..input.length - 1) {
                val ch = input[index]
                when {
                    ch.isLetter() && ch.isUpperCase() -> sb.append("_${ch.toLowerCase()}")
                    ch.isDigit() && !sb.last().isDigit() -> sb.append("_$ch")
                    else -> sb.append(ch)
                }
            }

            sb.toString()
        }
    }
}