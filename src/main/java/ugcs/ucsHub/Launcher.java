package ugcs.ucsHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static ugcs.ucsHub.Settings.settings;

public class Launcher {
    private static final long MAX_HEAP_SIZE_MB = settings().getMaxHeapSize();

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "ExecutedFromLauncher".equals(args[0])) {
            Main.main(args);
            return;
        }

        String javaPath = Paths.get(System.getProperty("java.home"), "bin", "javaw").toString();
        final String jarPath = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toString();
        final String launchCommand = format("\"%s\" -Xmx%dm -jar \"%s\" ExecutedFromLauncher", javaPath, MAX_HEAP_SIZE_MB, jarPath);

        getRuntime().exec(launchCommand);
    }
}
