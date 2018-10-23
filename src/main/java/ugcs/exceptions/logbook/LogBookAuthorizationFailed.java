package ugcs.exceptions.logbook;

import ugcs.exceptions.ExpectedException;

public class LogBookAuthorizationFailed extends ExpectedException {

    public LogBookAuthorizationFailed() {
        super("LogBook login failed.");
    }

    public LogBookAuthorizationFailed(String message) {
        super("LogBook login failed: " + message);
    }
}
