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
        // Firstly trying to distinguish by platform specific API (https://www.oracle.com/technetwork/java/hotspotfaq-138619.html#64bit_detection)
        if ("32".equals(System.getProperty("sun.arch.data.model")) || "64".equals(System.getProperty("sun.arch.data.model"))) {
            return "32".equals(System.getProperty("sun.arch.data.model"));
        }

        // Or else by trying to make decision based on public property (in case of running on non-HotSpot JVMs)
        return System.getProperty("os.arch").endsWith("86");
    }
}
