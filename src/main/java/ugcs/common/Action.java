package ugcs.common;

/**
 * Interface used as replacement for {@link Runnable} with possible {@link Exception} being thrown
 */
@FunctionalInterface
public interface Action {
    void run() throws Exception;
}
