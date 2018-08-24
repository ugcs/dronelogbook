package ugcs.ucsHub;

import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Telemetry;
import com.ugcs.ucs.proto.DomainProto.Value;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class TelemetryProcessor {
    private final List<Telemetry> telemetryList;

    private final static List<String> ALL_TELEMETRY_TYPE_NAMES;

    static {
        ALL_TELEMETRY_TYPE_NAMES = Arrays.stream(DomainProto.Semantic.values())
                .map(Enum::toString).collect(Collectors.toList());
    }

    private SortedMap<Long, Map<String, Telemetry>> processedTelemetry = null;

    public TelemetryProcessor(List<Telemetry> telemetryList) {
        this.telemetryList = telemetryList;
    }

    private SortedMap<Long, Map<String, Telemetry>> getProcessedTelemetry() {
        if (processedTelemetry == null) {
            synchronized (this) {
                if (processedTelemetry == null) {
                    processedTelemetry = telemetryList.stream()
                            .sorted(comparing(Telemetry::getTime))
                            .collect(groupingBy(Telemetry::getTime, TreeMap::new,
                                    toMap(t -> t.getTelemetryField().getSemantic().toString(), t -> t, (t1, t2) -> {
                                        System.err.println("*** Merge fail:");
                                        System.err.println(t1);
                                        System.err.println(t2);
                                        return t1; // TODO: instead of loosing t2 value, there should be way without data loss
                                    })));
                }
            }
        }
        return processedTelemetry;
    }

    public void printAsCsv(OutputStream out) {
        final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
        printCsvHeader(writer);

        final Map<String, String> currentRecord = new HashMap<>();
        ALL_TELEMETRY_TYPE_NAMES.forEach(typeName -> currentRecord.put(typeName, null));

        getProcessedTelemetry().forEach((epochMilli, telemetryMap) -> {
            telemetryMap.forEach((typeName, telemetry) ->
                    currentRecord.compute(typeName, (k, v) -> valueToString(telemetry.getValue())));
            writer.println(convertDateTime(epochMilli) + "," +
                    ALL_TELEMETRY_TYPE_NAMES.stream()
                            .map(typeName -> {
                                final String value = currentRecord.get(typeName);
                                if (value == null) {
                                    return "";
                                }
                                return value;
                            })
                            .collect(joining(",")));
        });
        writer.flush();
    }

    private void printCsvHeader(PrintWriter writer) {
        writer.println("Time," + ALL_TELEMETRY_TYPE_NAMES.stream()
                .map(typeName -> CsvFieldMapper.mapper().convertTypeName(typeName))
                .collect(joining(",")));
    }

    private static String convertDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()).toString();
    }

    private static String valueToString(Value value) {
        if (value.hasFloatValue()) {
            return String.valueOf(value.getFloatValue());
        }
        if (value.hasDoubleValue()) {
            return String.valueOf(value.getDoubleValue());
        }
        if (value.hasIntValue()) {
            return String.valueOf(value.getIntValue());
        }
        if (value.hasLongValue()) {
            return String.valueOf(value.getLongValue());
        }
        if (value.hasBoolValue()) {
            return String.valueOf(value.getBoolValue());
        }
        return value.getStringValue();
    }
}
