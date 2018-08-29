package ugcs.ucsHub;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class Settings {
    private final static String SETTINGS_FILE_NAME = "client.properties";

    private final static String DEFAULT_HOST = "127.0.0.1";
    private final static int    DEFAULT_PORT = 3334;
    private final static String DEFAULT_UCS_LOGIN = "undefined";
    private final static String DEFAULT_UCS_PASSWORD = "undefined";


    private final static String DEFAULT_UPLOAD_SERVER_URL = "https://www.dronelogbook.com/webservices/importFlight-ugcs.php";
    private final static String DEFAULT_UPLOAD_SERVER_LOGIN = "undefined";
    private final static String DEFAULT_UPLOAD_SERVER_PASSWORD = "undefined";
    private final static String DEFAULT_UPLOADED_FILE_FOLDER = "uploaded";

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
        host = properties.getOrDefault("server.host", DEFAULT_HOST).toString();
        port = Integer.parseInt(properties.getOrDefault("server.port", DEFAULT_PORT).toString());
        ucsServerLogin = properties.getOrDefault("server.login", DEFAULT_UCS_LOGIN).toString();
        ucsServerPassword=properties.getOrDefault("server.password", DEFAULT_UCS_PASSWORD).toString();

        uploadServerUrl = properties.getOrDefault("upload.server.url", DEFAULT_UPLOAD_SERVER_URL).toString();
        uploadServerLogin = properties.getOrDefault("upload.server.login", DEFAULT_UPLOAD_SERVER_LOGIN).toString();
        uploadServerPassword = properties.getOrDefault("upload.server.password", DEFAULT_UPLOAD_SERVER_PASSWORD).toString();

        uploadedFileFolder = properties.getOrDefault("uploaded.file.folder", DEFAULT_UPLOADED_FILE_FOLDER).toString();
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
}
