package ugcs.net;

/**
 * Interface for {@link SessionController} connection settings
 */
public interface SessionSettings {
    String getHost();

    int getPort();

    String getUcsServerLogin();

    String getUcsServerPassword();
}
