package ugcs.exceptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;

/**
 * Handler for all unhandled exceptions in application
 */
public class ExceptionsHandler {
    private GlobalExceptionListener uncaughtExceptionListener = null;

    private java.util.Map<Class, Set<ExceptionListener>> listenerMap = new HashMap<>();

    @FunctionalInterface
    public interface ExceptionListener<T extends Throwable> {
        void onException(T exception);
    }

    @FunctionalInterface
    public interface GlobalExceptionListener extends ExceptionListener<Throwable> {
    }

    private static volatile ExceptionsHandler instance;

    public static ExceptionsHandler handler() {
        if (instance == null) {
            synchronized (ExceptionsHandler.class) {
                if (instance == null) {
                    instance = new ExceptionsHandler();
                }
            }
        }
        return instance;
    }

    private ExceptionsHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this::handleException);
    }

    public void addUncaughtExceptionListener(GlobalExceptionListener listener) {
        uncaughtExceptionListener = listener;
    }

    @SuppressWarnings("unchecked")
    private void handleException(Thread thread, Throwable ex) {
        ex.printStackTrace();

        Throwable rootException = getRootException(ex);

        if (rootException instanceof ExpectedException) {
            System.err.println("Cause of ExpectedException:");
            final Throwable attachedCause = ((ExpectedException) rootException).getAttachedCause();
            if (!isNull(attachedCause)) {
                attachedCause.printStackTrace();
            }
        }

        final Set<ExceptionListener> listeners = listenerMap.getOrDefault(rootException.getClass(), emptySet());
        if (listeners.isEmpty() && uncaughtExceptionListener != null) {
            uncaughtExceptionListener.onException(rootException);
        } else {
            listeners.forEach(listener -> listener.onException(rootException));
        }
    }

    public <T extends Throwable> void addExceptionListener(Class<T> exceptionClass, ExceptionListener<T> listener) {
        listenerMap.computeIfAbsent(exceptionClass, c -> new HashSet<>()).add(listener);
    }

    private static Throwable getRootException(Throwable ex) {
        Throwable rootException = ex;
        while (rootException.getCause() != null) {
            rootException = rootException.getCause();
        }
        return rootException;
    }
}
