package ugcs.common.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityTest {
    @Test
    void testUniqueGeneration() {
        final Identity<Long> longIdentity1 = Identity.generateId();
        final Identity<Long> longIdentity2 = Identity.generateId("Test representation");

        assertNotEquals(longIdentity1, longIdentity2);
        assertNotEquals(longIdentity1.getId(), longIdentity2.getId());
        assertEquals(longIdentity2.toString(), "Test representation");
    }
}