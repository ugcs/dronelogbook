package ugcs.common.operation;

/**
 * {@link Operation} state while performing by {@link OperationPerformer}
 */
public enum State {
    INITIAL,
    PERFORMING,
    PERFORMED,
    FAULT,
    CANCELLED
}
