package ugcs.upload.service;

import org.junit.jupiter.api.Test;
import ugcs.common.identity.Identity;
import ugcs.processing.Flight;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ugcs.upload.service.UploadedFlightsStorage.storage;

class UploadedFlightsStorageTest {

    @Test
    void testStorage() {
        final Flight flightMock = createFlightMock();

        assertFalse(storage().isUploaded(flightMock));

        storage().storeAsUploaded(flightMock);
        assertTrue(storage().isUploaded(flightMock));

        storage().removeFromUploaded(flightMock);
        assertFalse(storage().isUploaded(flightMock));
    }

    private static Flight createFlightMock() {
        final Flight flightMock = mock(Flight.class);
        final Identity<?> identity = Identity.of(UUID.randomUUID().toString());
        doReturn(identity).when(flightMock).getId();

        return flightMock;
    }
}