package ugcs.common.operation;

import ugcs.common.identity.Identity;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.Objects.requireNonNull;
import static ugcs.common.operation.Operation.toCancelled;
import static ugcs.common.operation.Operation.toFault;
import static ugcs.common.operation.Operation.toPerformed;
import static ugcs.common.operation.Operation.toPerforming;

/**
 * Wrapper for {@link ExecutorService} for performing and tracking {@link Operation} in separate threads
 *
 * @param <T> identity type for operation tracking
 * @param <R> type of operation result
 */
public class OperationPerformer<T extends Identity<?>, R> {
    private final ExecutorService executorService;
    private final ConcurrentMap<T, Operation<T, R>> operations = new ConcurrentHashMap<>();

    public OperationPerformer(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void shutDown() {
        getExecutorService().shutdown();
    }

    public Future<Operation<T, R>> submit(T operationId, Callable<R> callable) {
        final Operation<T, R> newOperation = Operation.of(operationId, callable);
        updateOperations(operationId, newOperation);

        return executorService.submit(() -> {
            final Operation<T, R> operation =
                    operations.computeIfPresent(operationId, (_unused_, currentOperationState) -> {
                        if (currentOperationState.isCancelled()) {
                            return currentOperationState;
                        } else {
                            return toPerforming(newOperation);
                        }
                    });

            if (requireNonNull(operation).isCancelled()) {
                return operation;
            }

            try {
                final R result = operation.getOperation().call();
                final Operation<T, R> performedOperation = toPerformed(operation, result);
                updateOperations(operationId, performedOperation);
                return performedOperation;
            } catch (Exception e) {
                final Operation<T, R> faultOperation = toFault(operation, e);
                updateOperations(operationId, faultOperation);
                return faultOperation;
            }
        });
    }

    public void cancelAllWaitingOperations() {
        operations.keySet().forEach(
                id -> operations.computeIfPresent(id, (_unused_, operation) -> {
                    if (operation.isNotStarted()) {
                        return toCancelled(operation);
                    } else {
                        return operation;
                    }
                })
        );
    }

    private ExecutorService getExecutorService() {
        return executorService;
    }

    private void updateOperations(T key, Operation<T, R> newState) {
        operations.put(key, newState);
    }
}
