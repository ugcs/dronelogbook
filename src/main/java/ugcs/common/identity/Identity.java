package ugcs.common.identity;

import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper for typed identity
 *
 * @param <T> type of the identity
 */
public class Identity<T> {
    private static final AtomicLong uniqueID = new AtomicLong(0L);

    @Getter
    private final T id;

    private final String textRepresentation;

    private Identity(T id, String textRepresentation) {
        this.id = id;
        this.textRepresentation = textRepresentation;
    }

    private Identity(T id) {
        this(id, id.toString());
    }

    public static <T> Identity<T> of(T id) {
        return new Identity<>(id);
    }

    public static <T> Identity<T> of(T id, String textRepresentation) {
        return new Identity<>(id, textRepresentation);
    }

    public static Identity<Long> generateId() {
        return new Identity<>(nextUniqueId());
    }

    public static Identity<Long> generateId(String textRepresentation) {
        return new Identity<>(nextUniqueId(), textRepresentation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Identity<?> identity = (Identity<?>) o;
        return Objects.equals(id, identity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return textRepresentation;
    }

    private static long nextUniqueId() {
        return uniqueID.incrementAndGet();
    }
}
