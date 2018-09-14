package ugcs.processing.telemetry;

import com.ugcs.ucs.proto.DomainProto.Telemetry;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.processing.AbstractFlight;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

public class FlightTelemetry extends AbstractFlight {
    private final List<Pair<Long, Map<String, Telemetry>>> telemetry;

    FlightTelemetry(List<Pair<Long, Map<String, Telemetry>>> telemetryRecords, Vehicle vehicle) throws IllegalArgumentException {
        super(telemetryRecords.get(0).getLeft(), telemetryRecords.get(telemetryRecords.size() - 1).getLeft(), vehicle);
        this.telemetry = unmodifiableList(telemetryRecords);
    }

    public List<Pair<Long, Map<String, Telemetry>>> getTelemetry() {
        return telemetry;
    }
}
