package ugcs.exceptions.ugcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ugcs.exceptions.ugcs.UgcsFailure.INCORRECT_CREDENTIALS_MESSAGE;

class UgcsFailureTest {
    @Test
    void testIncorrectCredentials() {
        final UgcsFailure ex = new UgcsFailure(new RuntimeException("Authentication failed."));
        final UgcsFailure innerEx = new UgcsFailure(ex);

        assertEquals(INCORRECT_CREDENTIALS_MESSAGE, ex.getMessage());
        assertEquals(INCORRECT_CREDENTIALS_MESSAGE, innerEx.getMessage());
    }
}