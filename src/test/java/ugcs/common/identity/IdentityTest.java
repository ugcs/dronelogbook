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

    @Test
    void testStringIdentities() {
        final Identity<?> stringIdentity1 = Identity.of("test_id_1", "Test representation");
        final Identity<?> stringIdentity2 = Identity.of("test_id_2", "Test representation");
        final Identity<?> stringIdentity3 = Identity.of(new String("test_id_1"));

        assertEquals(stringIdentity1.toString(), "Test representation");
        assertEquals(stringIdentity1.getId().toString(), "test_id_1");
        assertNotEquals(stringIdentity1, stringIdentity2);
        assertNotEquals(stringIdentity1.getId(), stringIdentity2.getId());

        assertNotSame(stringIdentity1.getId(), stringIdentity3.getId());
        assertEquals(stringIdentity1, stringIdentity3);
    }
}