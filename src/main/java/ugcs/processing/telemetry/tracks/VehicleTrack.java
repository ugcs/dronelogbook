package ugcs.processing.telemetry.tracks;

import com.ugcs.ucs.proto.DomainProto;
import ugcs.processing.AbstractFlight;
import ugcs.processing.Flight;

import java.util.function.Supplier;

/**
 * {@link Flight} implementation based on telemetry frames
 *
 * @see VehicleTracksProcessor
 */
class VehicleTrack extends AbstractFlight {
    VehicleTrack(DomainProto.Vehicle vehicle, DomainProto.VehicleTrack dto) {
        super(getFlightStartTimeEpochMilli(dto), getFlightEndTimeEpochMilli(dto), vehicle);
    }

    private static long getFlightStartTimeEpochMilli(DomainProto.VehicleTrack dto) {
        return dto.getTrack().getPointsList().stream()
                .findFirst()
                .orElseThrow(getNoPointsExceptionSupplier())
                .getTime();
    }

    private static long getFlightEndTimeEpochMilli(DomainProto.VehicleTrack dto) {
        if (dto.getTrack().getPointsCount() <= 0) {
            throw getNoPointsExceptionSupplier().get();
        }

        return dto.getTrack().getPointsList()
                .get(dto.getTrack().getPointsCount() - 1)
                .getTime();
    }

    private static Supplier<? extends RuntimeException> getNoPointsExceptionSupplier() {
        return () -> new RuntimeException("Got track without points");
    }
}
