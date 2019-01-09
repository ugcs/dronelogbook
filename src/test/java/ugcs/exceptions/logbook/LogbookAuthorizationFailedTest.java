package ugcs.exceptions.logbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ugcs.exceptions.logbook.LogbookAuthorizationFailed.INCORRECT_CREDENTIALS_MESSAGE;

class LogbookAuthorizationFailedTest {
    @Test
    void testBadPasswordMessage() {
        final LogbookAuthorizationFailed ex = new LogbookAuthorizationFailed("Bad password.");
        assertEquals(INCORRECT_CREDENTIALS_MESSAGE, ex.getMessage());
    }

    @Test
    void testEmailNotExistMessage() {
        final LogbookAuthorizationFailed ex = new LogbookAuthorizationFailed("Email not exist.");
        assertEquals(INCORRECT_CREDENTIALS_MESSAGE, ex.getMessage());
    }
}