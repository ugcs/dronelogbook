package ugcs.common.files;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import static java.nio.file.Files.exists;
import static java.util.stream.Collectors.joining;

/**
 * Generates unique file name from given tokens on given path in the file system
 */
public class FileNameGenerator {
    private static final DateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final Path targetFolder;
    private final String extension;
    private final String coreName;

    public FileNameGenerator(Path targetFolder, String extension, Object... tokens) {
        this.targetFolder = targetFolder;
        this.extension = extension;
        this.coreName = Stream.of(tokens).map(FileNameGenerator::objectToString).collect(joining("-"));
    }

    public Path generateUnique() {
        Path uniqueFilePath = targetFolder.resolve(generateFileName(""));

        int numberOfTries = 1;
        while (exists(uniqueFilePath) && numberOfTries < 10000) {
            uniqueFilePath = targetFolder.resolve(generateFileName("-" + numberOfTries));
            ++numberOfTries;
        }

        return uniqueFilePath;
    }

    private String generateFileName(String fileSuffix) {
        return (coreName + fileSuffix + "." + extension)
                .replaceAll("[*/\\\\!|:?<>]", "_")
                .replaceAll("(%22)", "_");
    }

    private static String objectToString(Object o) {
        if (o instanceof Date) {
            return dateToString((Date) o);
        }
        return o.toString();
    }

    private static String dateToString(Date date) {
        return FILE_DATE_FORMAT.format(date);
    }
}
