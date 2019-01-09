package ugcs.upload.logbook;

import lombok.SneakyThrows;
import ugcs.processing.telemetry.FlightTelemetry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result for flight upload operation
 *
 * @see LogbookUploader#uploadFlight
 */
public class FlightUploadResponse {
    private final FlightTelemetry flightTelemetry;
    private final File flightFile;
    private final DroneLogbookResponse droneLogbookResponse;

    FlightUploadResponse(FlightTelemetry flightTelemetry, File flightFile, DroneLogbookResponse droneLogbookResponse) {
        this.flightTelemetry = flightTelemetry;
        this.flightFile = flightFile;
        this.droneLogbookResponse = droneLogbookResponse;
    }

    public FlightTelemetry getFlightTelemetry() {
        return flightTelemetry;
    }

    public DroneLogbookResponse getDroneLogbookResponse() {
        return droneLogbookResponse;
    }

    @SneakyThrows
    public FlightUploadResponse storeFlightTelemetry(Path targetPath) {
        Files.move(flightFile.toPath(), targetPath);

        return this;
    }
}
