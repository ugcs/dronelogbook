package ugcs.processing.telemetry;

import com.ugcs.ucs.proto.DomainProto.Telemetry;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.common.identity.Identity;
import ugcs.processing.AbstractFlight;
import ugcs.processing.Flight;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * {@link Flight} implementation with {@link Telemetry}
 */
public class FlightTelemetry extends AbstractFlight {
    private final List<Pair<Long, Map<String, Telemetry>>> telemetry;

    private FlightTelemetry(List<Pair<Long, Map<String, Telemetry>>> telemetryRecords, Vehicle vehicle, Identity<?> id) {
        super(telemetryRecords.get(0).getLeft(), telemetryRecords.get(telemetryRecords.size() - 1).getLeft(), vehicle, id);
        this.telemetry = unmodifiableList(telemetryRecords);
    }

    FlightTelemetry(List<Pair<Long, Map<String, Telemetry>>> telemetryRecords, Vehicle vehicle) {
        super(telemetryRecords.get(0).getLeft(), telemetryRecords.get(telemetryRecords.size() - 1).getLeft(), vehicle);
        this.telemetry = unmodifiableList(telemetryRecords);
    }

    public static FlightTelemetry withId(FlightTelemetry flight, Identity<?> id) {
        return new FlightTelemetry(flight.getTelemetry(), flight.getVehicle(), id);
    }

    public List<Pair<Long, Map<String, Telemetry>>> getTelemetry() {
        return telemetry;
    }
}
