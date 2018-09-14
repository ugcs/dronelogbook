package ugcs.processing.telemetry;

import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.stream.Collectors.toMap;

public class FlightTelemetryProcessor extends TelemetryProcessor {
    private final List<FlightTelemetry> flightTelemetries;

    public FlightTelemetryProcessor(List<FlightTelemetry> flightTelemetries, Vehicle vehicle) {
        super(null, vehicle);

        this.flightTelemetries = flightTelemetries;
    }

    @Override
    public SortedMap<Long, Map<String, DomainProto.Telemetry>> getProcessedTelemetry() {
        return evaluateField("processedTelemetry",
                () -> flightTelemetries.stream()
                        .flatMap(flightTelemetry -> flightTelemetry.getTelemetry().stream())
                        .collect(toMap(Pair::getKey, Pair::getValue, (t1, t2) -> t1, TreeMap::new))
        );
    }
}
