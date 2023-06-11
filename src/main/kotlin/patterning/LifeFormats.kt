package patterning;

/**
 * you exist because there are more to come
 */
public final class LifeFormats {

    // first of many parsers
    LifeForm parseRLE(String text) throws NotLifeException {
        return new RLEParser(text).getResult();
    }
}