package ugcs.ucsHub;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.privatejgoodies.common.base.Strings.isEmpty;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.isNull;

public final class Settings {
    private final static String SETTINGS_FILE_NAME = "client.properties";

    private final static String DEFAULT_HOST = "127.0.0.1";
    private final static String DEFAULT_PORT = "3334";
    private final static String DEFAULT_UCS_LOGIN = "undefined";
    private final static String DEFAULT_UCS_PASSWORD = "undefined";

    private final static String DEFAULT_UPLOAD_SERVER_URL = "https://www.dronelogbook.com/webservices/importFlight-ugcs.php";
    private final static String DEFAULT_UPLOAD_SERVER_LOGIN = "undefined";
    private final static String DEFAULT_UPLOAD_SERVER_PASSWORD = "undefined";
    private final static String DEFAULT_UPLOADED_FILE_FOLDER = "uploaded";

    private final static String DEFAULT_DATA_FOLDER = System.getProperty("user.home") + "/.dronelogbook";
    private final static String DEFAULT_TELEMETRY_FOLDER = "telemetry";

    private static volatile Settings instance;

    public static Settings settings() {
        if (instance == null) {
            synchronized (Settings.class) {
                if (instance == null) {
                    instance = new Settings();
                }
            }
        }
        return instance;
    }

    private final String host;
    private final int port;
    private final String ucsServerLogin;
    private final String ucsServerPassword;
    private final String uploadServerUrl;
    private final String uploadServerLogin;
    private final String uploadServerPassword;
    private final String uploadedFileFolder;
    private final String dataFolder;
    private final String telemetryFolder;

    private Settings() {
        final Properties properties = new Properties();
        final Path pathToSettings = Paths.get(SETTINGS_FILE_NAME);
        if (Files.isRegularFile(pathToSettings)) {
            try (InputStream in = new FileInputStream(pathToSettings.toFile())) {
                properties.load(in);
            } catch (IOException ignored) {
            }
        } else {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try (InputStream in = classLoader.getResourceAsStream(SETTINGS_FILE_NAME)) {
                if (in != null) {
                    properties.load(in);
                }
            } catch (IOException ignored) {
            }
        }
        host = getProperty(properties, "server.host", DEFAULT_HOST);
        port = Integer.parseInt(getProperty(properties, "server.port", DEFAULT_PORT));
        ucsServerLogin = getProperty(properties, "server.login", DEFAULT_UCS_LOGIN);
        ucsServerPassword=getProperty(properties, "server.password", DEFAULT_UCS_PASSWORD);

        uploadServerUrl = getProperty(properties, "upload.server.url", DEFAULT_UPLOAD_SERVER_URL);
        uploadServerLogin = getProperty(properties, "upload.server.login", DEFAULT_UPLOAD_SERVER_LOGIN);
        uploadServerPassword = getProperty(properties, "upload.server.password", DEFAULT_UPLOAD_SERVER_PASSWORD);

        uploadedFileFolder = getProperty(properties, "uploaded.file.folder", DEFAULT_UPLOADED_FILE_FOLDER);

        dataFolder = getProperty(properties, "application.data.folder", DEFAULT_DATA_FOLDER);
        telemetryFolder = getProperty(properties, "telemetry.file.folder", DEFAULT_TELEMETRY_FOLDER);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUcsServerLoginLogin() {
        return ucsServerLogin;
    }

    public String getUcsServerPassword() {
        return ucsServerPassword;
    }

    public String getUploadServerUrl() {
        return uploadServerUrl;
    }

    public String getUploadServerLogin() {
        return uploadServerLogin;
    }

    public String getUploadServerPassword() {
        return uploadServerPassword;
    }

    public String getUploadedFileFolder() {
        return uploadedFileFolder;
    }

    public String getDataFolder() {
        return dataFolder;
    }

    public String getTelemetryFolder() {
        return telemetryFolder;
    }

    public Path getTelemetryPath () {
        return createFolderIfNotPresent(resolveOnDataFolder(getTelemetryFolder()));
    }

    public Path getUploadedFlightsPath() {
        return createFolderIfNotPresent(resolveOnDataFolder(getUploadedFileFolder()));
    }

    private Path resolveOnDataFolder(String folderToResolve) {
        final Path folderPath = Paths.get(folderToResolve);
        if (folderPath.isAbsolute()) {
            return folderPath;
        }
        return Paths.get(getDataFolder()).resolve(folderPath);
    }

    private Path createFolderIfNotPresent(Path pathToFolder) {
        try {
            return createDirectories(pathToFolder);
        } catch (IOException toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    private static String getProperty(Properties properties, String propertyName, String defaultValue) {
        final Object propVal = properties.get(propertyName);
        if (isNull(propVal) || isEmpty(propVal.toString())) {
            return defaultValue;
        }
        return propVal.toString();
    }
}
