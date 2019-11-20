package ugcs.common.operation;

import lombok.Getter;
import ugcs.common.identity.Identity;

import java.util.Optional;
import java.util.concurrent.Callable;

import static java.util.Optional.ofNullable;
import static ugcs.common.operation.State.CANCELLED;
import static ugcs.common.operation.State.FAULT;
import static ugcs.common.operation.State.INITIAL;
import static ugcs.common.operation.State.PERFORMED;
import static ugcs.common.operation.State.PERFORMING;

/**
 * Object that represents an operation which can be tracked by {@link Identity}
 *
 * @param <T> identity type for operation tracking
 * @param <R> type of operation result
 */
public class Operation<T extends Identity<?>, R> {
    @Getter
    private final T id;
    private final R result;
    @Getter
    private final State state;
    private final Throwable error;
    @Getter
    private final Callable<R> operation;

    private Operation(T id, R result, State state, Throwable error, Callable<R> operation) {
        this.id = id;
        this.result = result;
        this.state = state;
        this.error = error;
        this.operation = operation;
    }

    public static <T extends Identity<?>, R> Operation<T, R> of(T id, Callable<R> operation) {
        return new Operation<>(id, null, INITIAL, null, operation);
    }

    static <T extends Identity<?>, R> Operation<T, R> toPerforming(Operation<T, R> operation) {
        return new Operation<>(operation.getId(), null, PERFORMING, null, operation.getOperation());
    }

    static <T extends Identity<?>, R> Operation<T, R> toPerformed(Operation<T, R> operation, R result) {
        return new Operation<>(operation.getId(), result, PERFORMED, null, operation.getOperation());
    }

    static <T extends Identity<?>, R> Operation<T, R> toFault(Operation<T, R> operation, Throwable error) {
        return new Operation<>(operation.getId(), null, FAULT, error, operation.getOperation());
    }

    static <T extends Identity<?>, R> Operation<T, R> toCancelled(Operation<T, R> operation) {
        return new Operation<>(operation.getId(), null, CANCELLED, null, operation.getOperation());
    }

    public Optional<R> getResult() {
        return ofNullable(result);
    }

    public Optional<Throwable> getError() {
        return ofNullable(error);
    }

    public boolean isNotStarted() {
        return INITIAL == getState();
    }

    public boolean isCancelled() {
        return CANCELLED == getState();
    }
}
