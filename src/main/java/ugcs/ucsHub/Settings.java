package ugcs.ucsHub;

import lombok.Getter;
import lombok.SneakyThrows;
import ugcs.common.security.MD5HashCalculator;
import ugcs.net.SessionSettings;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.privatejgoodies.common.base.Strings.isEmpty;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.isNull;

public final class Settings implements SessionSettings {
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
    private String uploadServerPassword;
    private final String uploadedFileFolder;
    private final String telemetryFolder;

    private final Properties globalSettings;
    private final Properties localSettings;

    @Getter
    private final ImageIcon logoIcon;

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

        logoIcon = new ImageIcon(Settings.class.getResource("/graphics/logo.png"));
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUcsServerLogin() {
        return ucsServerLogin;
    }

    @Override
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

    private String getUploadedFileFolder() {
        return uploadedFileFolder;
    }

    private String getDataFolder() {
        return DATA_FOLDER;
    }

    private String getTelemetryFolder() {
        return telemetryFolder;
    }

    public void storeUcsServerLogin(String ucsServerLogin) {
        if (!this.ucsServerLogin.equals(ucsServerLogin)) {
            storePropertyLocal("server.login", ucsServerLogin);
            this.ucsServerLogin = ucsServerLogin;
        }
    }

    public void storeUploadServerLogin(String uploadServerLogin) {
        if (!this.uploadServerLogin.equals(uploadServerLogin)) {
            storePropertyLocal("upload.server.login", uploadServerLogin);
            this.uploadServerLogin = uploadServerLogin;
        }
    }

    public void storeUploadServerPassword(String uploadServerPassword) {
        if (!this.uploadServerPassword.equals(uploadServerPassword)) {
            final String passwordHash = MD5HashCalculator.of(uploadServerPassword).hash();
            storePropertyLocal("upload.server.password", passwordHash);
            this.uploadServerPassword = passwordHash;
        }
    }

    public Path getTelemetryPath() {
        return createFolderIfNotPresent(resolveOnDataFolder(getTelemetryFolder()));
    }

    public Path getUploadedFlightsPath() {
        return createFolderIfNotPresent(resolveOnDataFolder(getUploadedFileFolder()));
    }

    @SneakyThrows
    String getProductVersion() {
        try (final InputStream in = getClass().getResourceAsStream("/settings/version.properties")) {
            final Properties versionProperties = new Properties();
            versionProperties.load(in);
            final String version = versionProperties.getProperty("project.version");
            final String buildNumber = versionProperties.getProperty("build.number");
            final boolean isReleaseBuild = "true".equals(versionProperties.getProperty("project.release", ""));
            return version + (isReleaseBuild ? "" : format(", build %s", buildNumber));
        }
    }

    @SneakyThrows
    long getMaxHeapSize() {
        try (final InputStream in = getClass().getResourceAsStream("/settings/jvm.properties")) {
            final Properties jvmProperties = new Properties();
            jvmProperties.load(in);
            return Long.valueOf(jvmProperties.getProperty("jvm.maxHeapSizeMb"));
        }
    }

    private Path resolveOnDataFolder(String folderToResolve) {
        final Path folderPath = Paths.get(folderToResolve);
        if (folderPath.isAbsolute()) {
            return folderPath;
        }
        return Paths.get(getDataFolder()).resolve(folderPath);
    }

    @SneakyThrows
    private Path createFolderIfNotPresent(Path pathToFolder) {
        return createDirectories(pathToFolder);
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

    private void storePropertyLocal(String propName, String propValue) {
        localSettings.setProperty(propName, propValue);
        try (OutputStream out = new FileOutputStream(new File(SETTINGS_FILE_NAME))) {
            this.localSettings.store(out, "");
        } catch (IOException ignored) {
        }
    }
}
