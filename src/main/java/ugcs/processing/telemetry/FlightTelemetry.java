package ugcs.processing.telemetry;

import com.ugcs.ucs.proto.DomainProto.Telemetry;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.processing.Flight;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

public class FlightTelemetry implements Flight {
    private final List<Pair<Long, Map<String, Telemetry>>> telemetry;
    private final long flightStartEpochMilli;
    private final long flightEndEpochMilli;

    FlightTelemetry(List<Pair<Long, Map<String, Telemetry>>> telemetryRecords) throws IllegalArgumentException {
        final int recordsCount = telemetryRecords.size();
        if (recordsCount < 2) {
            throw new IllegalArgumentException("Flight should have more than one telemetry record");
        }

        this.telemetry = unmodifiableList(telemetryRecords);
        this.flightStartEpochMilli = telemetryRecords.get(0).getLeft();
        this.flightEndEpochMilli = telemetryRecords.get(recordsCount - 1).getLeft();
    }

    public List<Pair<Long, Map<String, Telemetry>>> getTelemetry() {
        return telemetry;
    }

    @Override
    public long getFlightStartEpochMilli() {
        return flightStartEpochMilli;
    }

    @Override
    public long getFlightEndEpochMilli() {
        return flightEndEpochMilli;
    }
}
