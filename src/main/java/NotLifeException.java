public class NotLifeException extends Exception {
    public NotLifeException() {
        super();
    }

    public NotLifeException(String message) {
        super(message);
    }

    public NotLifeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotLifeException(Throwable cause) {
        super(cause);
    }
}
