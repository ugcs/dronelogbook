package ugcs.processing.logs;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import com.ugcs.ucs.proto.DomainProto.VehicleLogEntry;
import ugcs.common.LazyFieldEvaluator;

import java.util.LinkedList;
import java.util.List;

import static com.ugcs.ucs.proto.DomainProto.ProcessStage.PS_SUCCESS;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class LogsProcessor extends LazyFieldEvaluator {
    private final static long UNDEFINED = Long.MIN_VALUE;

    private final List<VehicleLogEntry> sortedByTimeLogEntryList;
    private final Vehicle vehicle;

    public LogsProcessor(List<VehicleLogEntry> logEntryList, Vehicle vehicle) {
        this.sortedByTimeLogEntryList = logEntryList.stream()
                .sorted((e1, e2) -> (int) (e1.getTime() - e2.getTime()))
                .collect(toList());

        this.vehicle = vehicle;
    }

    public List<FlightLog> getFlightLogs() {
        return evaluateField("flightLogs",
                () -> {
                    final List<FlightLog> flightLogs = new LinkedList<>();

                    long armTime = UNDEFINED;
                    for (VehicleLogEntry logEntry : sortedByTimeLogEntryList) {
                        if (armTime == UNDEFINED && checkLogEntry(logEntry, "arm")) {
                            armTime = logEntry.getTime();
                        } else if (armTime != UNDEFINED && checkLogEntry(logEntry, "disarm")) {
                            final long disarmTime = logEntry.getTime();
                            flightLogs.add(new FlightLog(armTime, disarmTime, vehicle));
                            armTime = UNDEFINED;
                        }
                    }

                    return unmodifiableList(flightLogs);
                });
    }

    private static boolean checkLogEntry(VehicleLogEntry entry, String commandCode) {
        return entry.getStage() == PS_SUCCESS &&
                entry.hasCommandArguments() &&
                commandCode.equals(entry.getCommandArguments().getCommandCode());
    }
}
