package ugcs.upload.logbook;

import lombok.SneakyThrows;
import ugcs.processing.telemetry.FlightTelemetry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Result for flight upload operation
 *
 * @see LogBookUploader#uploadFlight
 */
public class FlightUploadResponse {
    private final FlightTelemetry flightTelemetry;
    private final File flightFile;
    private final DroneLogBookResponse droneLogBookResponse;

    FlightUploadResponse(FlightTelemetry flightTelemetry, File flightFile, DroneLogBookResponse droneLogBookResponse) {
        this.flightTelemetry = flightTelemetry;
        this.flightFile = flightFile;
        this.droneLogBookResponse = droneLogBookResponse;
    }

    public FlightTelemetry getFlightTelemetry() {
        return flightTelemetry;
    }

    public DroneLogBookResponse getDroneLogBookResponse() {
        return droneLogBookResponse;
    }

    @SneakyThrows
    public FlightUploadResponse storeFlightTelemetry(Path targetPath) {
        Files.move(flightFile.toPath(), targetPath);

        return this;
    }
}
