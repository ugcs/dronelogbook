package ugcs.common;

@FunctionalInterface
public interface Action {
    void run() throws Exception;
}
