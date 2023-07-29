package patterning.life

/**
 * you exist because there are more to come
 */
class LifeFormats {
    // first of many parsers
    @Throws(NotLifeException::class)
    fun parseRLE(text: String): LifeForm {
        return RLEParser(text).result
    }
}