package ugcs.common.operation;

import lombok.SneakyThrows;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class FutureWrapper<R> implements Future<R> {
    private final Future<R> future;

    public static <R> FutureWrapper<R> of(Future<R> future) {
        return new FutureWrapper<>(future);
    }

    private FutureWrapper(Future<R> future) {
        this.future = future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    @SneakyThrows
    public R get() {
        return future.get();
    }

    @Override
    @SneakyThrows
    public R get(long timeout, TimeUnit unit) {
        return future.get(timeout, unit);
    }
}
