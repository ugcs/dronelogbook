package ugcs.ucsHub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.privatejgoodies.common.base.Strings.isEmpty;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.isNull;

public final class Settings {
    private final static String SETTINGS_FILE_NAME = "client.properties";
    private final static String DATA_FOLDER = System.getProperty("user.home") + "/.dronelogbook";

    private final static String DEFAULT_HOST = "127.0.0.1";
    private final static String DEFAULT_PORT = "3334";
    private final static String DEFAULT_UCS_LOGIN = "undefined";
    private final static String DEFAULT_UCS_PASSWORD = "";

    private final static String DEFAULT_UPLOAD_SERVER_URL = "https://www.dronelogbook.com/webservices/importFlight-ugcs.php";
    private final static String DEFAULT_UPLOAD_SERVER_LOGIN = "undefined";
    private final static String DEFAULT_UPLOAD_SERVER_PASSWORD = "";
    private final static String DEFAULT_UPLOADED_FILE_FOLDER = "uploaded";

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
    private String ucsServerLogin;
    private final String ucsServerPassword;
    private final String uploadServerUrl;
    private String uploadServerLogin;
    private final String uploadServerPassword;
    private final String uploadedFileFolder;
    private final String telemetryFolder;

    private final Properties globalSettings;
    private final Properties localSettings;

    private Settings() {
        this.globalSettings = new Properties();
        final Path pathToGlobalSettings = resolveOnDataFolder(SETTINGS_FILE_NAME);
        if (isRegularFile(pathToGlobalSettings)) {
            try (InputStream in = new FileInputStream(pathToGlobalSettings.toFile())) {
                this.globalSettings.load(in);
            } catch (IOException ignored) {
            }
        } else {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try (InputStream in = classLoader.getResourceAsStream(SETTINGS_FILE_NAME)) {
                if (in != null) {
                    this.globalSettings.load(in);
                    try (final OutputStream out = Files.newOutputStream(pathToGlobalSettings)) {
                        this.globalSettings.store(out, "");
                    }
                }
            } catch (IOException ignored) {
            }
        }

        this.localSettings = new Properties();
        final Path pathToLocalSettings = Paths.get(SETTINGS_FILE_NAME);
        if (isRegularFile(pathToLocalSettings)) {
            try (InputStream in = new FileInputStream(pathToLocalSettings.toFile())) {
                this.localSettings.load(in);
            } catch (IOException ignored) {
            }
        }

        host = getProperty("server.host", DEFAULT_HOST);
        port = Integer.parseInt(getProperty("server.port", DEFAULT_PORT));
        ucsServerLogin = getProperty("server.login", DEFAULT_UCS_LOGIN);
        ucsServerPassword = getProperty("server.password", DEFAULT_UCS_PASSWORD);

        uploadServerUrl = getProperty("upload.server.url", DEFAULT_UPLOAD_SERVER_URL);
        uploadServerLogin = getProperty("upload.server.login", DEFAULT_UPLOAD_SERVER_LOGIN);
        uploadServerPassword = getProperty("upload.server.password", DEFAULT_UPLOAD_SERVER_PASSWORD);

        uploadedFileFolder = getProperty("uploaded.file.folder", DEFAULT_UPLOADED_FILE_FOLDER);
        telemetryFolder = getProperty("telemetry.file.folder", DEFAULT_TELEMETRY_FOLDER);
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
        return DATA_FOLDER;
    }

    public String getTelemetryFolder() {
        return telemetryFolder;
    }

    public void storeUcsServerLogin(String ucsServerLogin) {
        storeLocalProperty("server.login", ucsServerLogin);
        this.ucsServerLogin = ucsServerLogin;
    }

    public void storeUploadServerLogin(String uploadServerLogin) {
        storeLocalProperty("upload.server.login", uploadServerLogin);
        this.uploadServerLogin = uploadServerLogin;
    }

    public Path getTelemetryPath() {
        return createFolderIfNotPresent(resolveOnDataFolder(getTelemetryFolder()));
    }

    public Path getUploadedFlightsPath() {
        return createFolderIfNotPresent(resolveOnDataFolder(getUploadedFileFolder()));
    }

    String getProductVersion() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/settings/version.info")))
        ) {
            return reader.readLine();
        } catch (IOException toRethrow) {
            throw new RuntimeException(toRethrow);
        }
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

    private String getProperty(String propertyName, String defaultValue) {
        final Object globalPropVal = globalSettings.get(propertyName);
        final Object localPropVal = localSettings.get(propertyName);

        if (!isNull(localPropVal) && !isEmpty(localPropVal.toString())) {
            return localPropVal.toString();
        }

        if (isNull(globalPropVal) || isEmpty(globalPropVal.toString())) {
            return defaultValue;
        }

        return globalPropVal.toString();
    }

    private void storeLocalProperty(String propName, String propValue) {
        localSettings.setProperty(propName, propValue);
        try (OutputStream out = new FileOutputStream(new File(SETTINGS_FILE_NAME))) {
            this.localSettings.store(out, "");
        } catch (IOException ignored) {
        }
    }
}
