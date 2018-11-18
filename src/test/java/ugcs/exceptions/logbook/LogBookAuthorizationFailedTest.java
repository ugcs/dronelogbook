package ugcs.exceptions.logbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ugcs.exceptions.logbook.LogBookAuthorizationFailed.INCORRECT_CREDENTIALS_MESSAGE;

class LogBookAuthorizationFailedTest {
    @Test
    void testBadPasswordMessage() {
        final LogBookAuthorizationFailed ex = new LogBookAuthorizationFailed("Bad password.");
        assertEquals(INCORRECT_CREDENTIALS_MESSAGE, ex.getMessage());
    }

    @Test
    void testEmailNotExistMessage() {
        final LogBookAuthorizationFailed ex = new LogBookAuthorizationFailed("Email not exist.");
        assertEquals(INCORRECT_CREDENTIALS_MESSAGE, ex.getMessage());
    }
}