package ugcs.exceptions;

public class ExpectedException extends RuntimeException {
    private final Throwable attachedCause;

    public ExpectedException(String message, Throwable attachedCause) {
        super(message);
        this.attachedCause = attachedCause;
    }

    public ExpectedException(String message) {
        this(message, null);
    }

    public Throwable getAttachedCause() {
        return attachedCause;
    }
}
