package ugcs.processing.telemetry;

import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.net.SessionController;
import ugcs.processing.Flight;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ugcs.net.SessionController.sessionController;

/**
 * {@link TelemetryProcessor} extension with support of dynamic {@link FlightTelemetry} calculation for a {@link Flight}
 */
public class FlightTelemetryProcessor extends TelemetryProcessor {
    private final List<FlightTelemetry> flightTelemetries;

    public FlightTelemetryProcessor(List<FlightTelemetry> flightTelemetries, Vehicle vehicle) {
        super(null, vehicle);

        this.flightTelemetries = flightTelemetries;
    }

    public FlightTelemetryProcessor(Flight flight) {
        super(null, flight.getVehicle());

        flightTelemetries = getFlightTelemetry(flight, sessionController());
    }

    @Override
    public SortedMap<Long, Map<String, DomainProto.Telemetry>> getProcessedTelemetry() {
        return evaluateField("processedTelemetry",
                () -> flightTelemetries.stream()
                        .flatMap(flightTelemetry -> flightTelemetry.getTelemetry().stream())
                        .collect(toMap(Pair::getKey, Pair::getValue, (t1, t2) -> t1, TreeMap::new))
        );
    }

    private static List<FlightTelemetry> getFlightTelemetry(Flight flight, SessionController sessionController) {
        return Stream.of(flight)
                .flatMap(f -> f instanceof FlightTelemetry
                        ? Stream.of((FlightTelemetry) f)
                        : acquireFlightTelemetry(f, sessionController).stream())
                .collect(toList());
    }

    private static List<FlightTelemetry> acquireFlightTelemetry(Flight flight, SessionController sessionController) {
        final Vehicle vehicle = flight.getVehicle();
        final long startEpochMilli = flight.getStartEpochMilli();
        final long endEpochMilli = flight.getEndEpochMilli();

        return new TelemetryProcessor(
                sessionController.getTelemetry(vehicle, startEpochMilli, endEpochMilli).getTelemetryList(),
                vehicle
        ).getFlightTelemetries();
    }
}
