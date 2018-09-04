package ugcs.processing.telemetry;

import com.ugcs.ucs.proto.DomainProto.Telemetry;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import ugcs.processing.AbstractProcessor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class TelemetryProcessor extends AbstractProcessor {
    private final List<Telemetry> telemetryList;

    public TelemetryProcessor(List<Telemetry> telemetryList) {
        this.telemetryList = telemetryList;
    }

    public SortedMap<Long, Map<String, Telemetry>> getProcessedTelemetry() {
        return evaluateField("processedTelemetry",
                () -> telemetryList.stream()
                        .sorted(comparing(Telemetry::getTime))
                        .collect(groupingBy(Telemetry::getTime, TreeMap::new,
                                toMap(t -> t.getTelemetryField().getCode(), t -> t, (t1, t2) -> {
                                    System.err.println("*** Merge fail:");
                                    System.err.println(t1);
                                    System.err.println(t2);
                                    return t1;
                                }))));
    }

    public Set<String> getAllFieldCodes() {
        return evaluateField("allFieldCodes",
                () -> getProcessedTelemetry().values().stream()
                        .flatMap(m -> m.values().stream())
                        .map(t -> t.getTelemetryField().getCode())
                        .collect(Collectors.toSet()));
    }

    public List<FlightTelemetry> getFlightTelemetries() {
        return evaluateField("flightTelemetries",
                () -> {
                    final List<FlightTelemetry> flightTelemetries = new ArrayList<>();

                    final SortedMap<Long, Map<String, Telemetry>> allTelemetry = getProcessedTelemetry();

                    long lastTelemetryTime = 0;
                    final List<Triple<Long, Long, Map<String, Telemetry>>> telemetryByTimeDiff = new LinkedList<>();
                    for (Map.Entry<Long, Map<String, Telemetry>> entry : allTelemetry.entrySet()) {
                        if (lastTelemetryTime == 0) {
                            telemetryByTimeDiff.add(Triple.of(Long.MAX_VALUE, entry.getKey(), entry.getValue()));
                        } else {
                            telemetryByTimeDiff.add(Triple.of(entry.getKey() - lastTelemetryTime, entry.getKey(), entry.getValue()));
                        }
                        lastTelemetryTime = entry.getKey();
                    }

                    List<Pair<Long, Map<String, Telemetry>>> currentFlightTelemetry = new LinkedList<>();
                    for (Triple<Long, Long, Map<String, Telemetry>> telemetryWithTimeDiff : telemetryByTimeDiff) {
                        final Pair<Long, Map<String, Telemetry>> telemetryRecord =
                                Pair.of(telemetryWithTimeDiff.getMiddle(), telemetryWithTimeDiff.getRight());
                        if (currentFlightTelemetry.isEmpty()) {
                            currentFlightTelemetry.add(telemetryRecord);
                        } else {
                            if (telemetryWithTimeDiff.getLeft() < 15000) {
                                currentFlightTelemetry.add(telemetryRecord);
                            } else {
                                if (currentFlightTelemetry.size() > 1) {
                                    flightTelemetries.add(new FlightTelemetry(currentFlightTelemetry));
                                }
                                currentFlightTelemetry = new LinkedList<>();
                                currentFlightTelemetry.add(telemetryRecord);
                            }
                        }
                    }

                    if (currentFlightTelemetry.size() > 1) {
                        flightTelemetries.add(new FlightTelemetry(currentFlightTelemetry));
                    }

                    return flightTelemetries;
                });
    }
}
