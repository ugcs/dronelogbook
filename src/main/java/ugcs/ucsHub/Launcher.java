package ugcs.ucsHub;

import ugcs.common.helpers.JvmHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static ugcs.ucsHub.Settings.settings;

/**
 * Launcher for the main {@link Main} class of application with support of specified JVM arguments
 */
public class Launcher {
    private static final long MAX_HEAP_SIZE_MB = settings().getMaxHeapSize();

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "ExecutedFromLauncher".equals(args[0])) {
            Main.main(args);
            return;
        }

        final Path javaBinPath = JvmHelper.getJavaBinPath();
        final String jarPath =
                new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toString();

        final String[] cmdArray = {
                javaBinPath.toString(),
                format("-Xmx%dm", getMaxHeapSizeMb()),
                "-jar", jarPath,
                "ExecutedFromLauncher"
        };

        getRuntime().exec(cmdArray);
    }

    private static long getMaxHeapSizeMb() {
        if (JvmHelper.isCurrentJre32Bit()) {
            return 1024;
        }

        return MAX_HEAP_SIZE_MB;
    }
}
