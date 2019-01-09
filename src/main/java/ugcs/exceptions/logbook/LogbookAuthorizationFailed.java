package ugcs.exceptions.logbook;

import ugcs.exceptions.ExpectedException;

public class LogbookAuthorizationFailed extends ExpectedException {
    static String INCORRECT_CREDENTIALS_MESSAGE = "Incorrect DroneLogbook username or password. Please try again.";

    public LogbookAuthorizationFailed() {
        super("DroneLogbook login failed.");
    }

    public LogbookAuthorizationFailed(String message) {
        super(getAuthorizationFailedMessage(message));
    }

    private static String getAuthorizationFailedMessage(String message) {
        final String lowerCaseMessage = message.toLowerCase();

        if (lowerCaseMessage.contains("bad password")) {
            return INCORRECT_CREDENTIALS_MESSAGE;
        }

        if (lowerCaseMessage.contains("email not exist")) {
            return INCORRECT_CREDENTIALS_MESSAGE;
        }

        return "DroneLogbook login failed: " + message;
    }
}
