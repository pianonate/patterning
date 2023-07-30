package patterning.life

/**
 * you exist because there are more to come
 */
class FileFormat {
    // first of many parsers
    fun parseRLE(text: String): LifeForm {
        return RLEFormatParser(text).result
    }
}