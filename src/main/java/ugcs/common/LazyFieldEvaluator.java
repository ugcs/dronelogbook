package ugcs.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Base class supporting thread-safe lazy field initialization
 */
public abstract class LazyFieldEvaluator {
    private final ConcurrentMap<String, Object> evaluatedFields = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    protected final <T> T evaluateField(String fieldName, Supplier<T> evaluator) {
        return (T) evaluatedFields.computeIfAbsent(fieldName, k -> evaluator.get());
    }
}
