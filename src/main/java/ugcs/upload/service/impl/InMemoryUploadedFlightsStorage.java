package ugcs.upload.service.impl;

import ugcs.common.identity.Identity;
import ugcs.processing.Flight;
import ugcs.upload.service.UploadedFlightsStorage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service storing information about uploaded flights in memory of process (information erases on process restart).
 */
public class InMemoryUploadedFlightsStorage implements UploadedFlightsStorage {
    private final Set<Identity<?>> uploadedFlights = ConcurrentHashMap.newKeySet();

    private static volatile InMemoryUploadedFlightsStorage instance;

    public static InMemoryUploadedFlightsStorage storage() {
        if (instance == null) {
            synchronized (InMemoryUploadedFlightsStorage.class) {
                if (instance == null) {
                    instance = new InMemoryUploadedFlightsStorage();
                }
            }
        }
        return instance;
    }

    @Override
    public void storeAsUploaded(Flight flight) {
        uploadedFlights.add(flight.getId());
    }

    @Override
    public boolean isUploaded(Flight flight) {
        return uploadedFlights.contains(flight.getId());
    }

    @Override
    public void removeFromUploaded(Flight flight) {
        uploadedFlights.remove(flight.getId());
    }

    private InMemoryUploadedFlightsStorage() {
    }
}