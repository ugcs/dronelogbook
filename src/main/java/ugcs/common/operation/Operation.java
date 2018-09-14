package ugcs.common.operation;

import lombok.Getter;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static ugcs.common.operation.State.FAULT;
import static ugcs.common.operation.State.INITIAL;
import static ugcs.common.operation.State.PERFORMED;
import static ugcs.common.operation.State.PERFORMING;

public class Operation<T, R> {
    @Getter
    private final T param;
    private final R result;
    @Getter
    private final State state;
    private final Throwable error;
    @Getter
    private final Callable<R> operation;

    private Operation(T param, R result, State state, Throwable error, Callable<R> operation) {
        this.param = param;
        this.result = result;
        this.state = state;
        this.error = error;
        this.operation = operation;
    }

    public static <T, R> Operation<T, R> of(T param, Callable<R> operation) {
        return new Operation<>(param, null, INITIAL, null, operation);
    }

    static <T, R> Operation<T, R> toPerforming(Operation<T, R> operation) {
        return new Operation<>(operation.getParam(), null, PERFORMING, null, operation.getOperation());
    }

    static <T, R> Operation<T, R> toPerformed(Operation<T, R> operation, R result) {
        return new Operation<>(operation.getParam(), result, PERFORMED, null, operation.getOperation());
    }

    static <T, R> Operation<T, R> toFault(Operation<T, R> operation, Throwable error) {
        return new Operation<>(operation.getParam(), null, FAULT, error, operation.getOperation());
    }

    public Optional<R> getResult() {
        return ofNullable(result);
    }

    public Stream<R> getResultAsStream() {
        return getResult().map(Stream::of).orElse(Stream.empty());
    }

    public Optional<Throwable> getError() {
        return ofNullable(error);
    }

    public boolean isError () {
        return getError().isPresent();
    }
}
