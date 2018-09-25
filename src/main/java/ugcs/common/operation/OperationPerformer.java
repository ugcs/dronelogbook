package ugcs.common.operation;

import ugcs.common.identity.Identity;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static ugcs.common.operation.Operation.toFault;
import static ugcs.common.operation.Operation.toPerformed;
import static ugcs.common.operation.Operation.toPerforming;

public class OperationPerformer<T extends Identity<?>, R> {
    private final ExecutorService executorService;
    private final ConcurrentMap<T, Operation<T, R>> operations = new ConcurrentHashMap<>();

    public OperationPerformer(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void shutDown() {
        getExecutorService().shutdown();
    }

    public Future<Operation<T, R>> submit(T param, Callable<R> callable) {
        final Operation<T, R> newOperation = Operation.of(param, callable);
        updateOperations(param, newOperation);

        return executorService.submit(() -> {
            final Operation<T, R> performingOperation = toPerforming(newOperation);
            updateOperations(param, performingOperation);
            try {
                final R result = performingOperation.getOperation().call();
                final Operation<T, R> performedOperation = toPerformed(performingOperation, result);
                updateOperations(param, performedOperation);
                return performedOperation;
            } catch (Exception e) {
                final Operation<T, R> faultOperation = toFault(performingOperation, e);
                updateOperations(param, faultOperation);
                return faultOperation;
            }
        });
    }

    private ExecutorService getExecutorService() {
        return executorService;
    }

    private void updateOperations(T key, Operation<T, R> newState) {
        operations.put(key, newState);
    }
}
