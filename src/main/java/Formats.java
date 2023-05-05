/**
 * you exist because there are more to come
 */
public final class Formats {

    LifeForm parseRLE(String text) throws NotLifeException {
        return new RLEParser(text).getResult();
    }
}