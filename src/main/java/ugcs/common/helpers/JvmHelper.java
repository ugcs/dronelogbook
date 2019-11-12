package ugcs.common.helpers;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.exists;

/**
 * Helper class for getting current JVM information from the runtime.
 */
public class JvmHelper {
    public static Path getJavaBinPath() {
        final Path javaBinDirPath = Paths.get(System.getProperty("java.home"), "bin");

        return exists(javaBinDirPath.resolve("javaw"))
                ? javaBinDirPath.resolve("javaw")
                : javaBinDirPath.resolve("java");
    }

    public static boolean isCurrentJre32Bit() {
        return System.getProperty("os.arch").endsWith("86");
    }
}
