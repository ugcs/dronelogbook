package ugcs.ucsHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static ugcs.ucsHub.Settings.settings;

public class Launcher {
    private static final long MAX_HEAP_SIZE_MB = settings().getMaxHeapSize();

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "ExecutedFromLauncher".equals(args[0])) {
            Main.main(args);
            return;
        }

        final Path javaBinDirPath = Paths.get(System.getProperty("java.home"), "bin");
        final Path javaBinPath = exists(javaBinDirPath.resolve("javaw"))
                ? javaBinDirPath.resolve("javaw")
                : javaBinDirPath.resolve("java");

        final String jarPath =
                new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toString();

        final String[] cmdArray = {
                javaBinPath.toString(),
                format("-Xmx%dm", MAX_HEAP_SIZE_MB),
                "-jar", jarPath,
                "ExecutedFromLauncher"
        };

        getRuntime().exec(cmdArray);
    }
}
