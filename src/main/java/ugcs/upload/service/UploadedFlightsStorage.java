package ugcs.upload.service;

import ugcs.processing.Flight;
import ugcs.upload.service.impl.InMemoryUploadedFlightsStorage;

/**
 * Service storing information about uploaded flights.
 */
public interface UploadedFlightsStorage {
    void storeAsUploaded(Flight flight);

    boolean isUploaded(Flight flight);

    void removeFromUploaded(Flight flight);

    static UploadedFlightsStorage storage() {
        return InMemoryUploadedFlightsStorage.storage();
    }
}
