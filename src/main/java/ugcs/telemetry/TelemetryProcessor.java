package ugcs.telemetry;

import com.ugcs.ucs.proto.DomainProto.Telemetry;
import com.ugcs.ucs.proto.DomainProto.Value;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.ugcs.common.util.Strings.isNullOrEmpty;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class TelemetryProcessor {
    private final List<Telemetry> telemetryList;
    private SortedMap<Long, Map<String, Telemetry>> processedTelemetry = null;
    private Set<String> allFieldCodes = null;

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
                                    toMap(t -> t.getTelemetryField().getCode(), t -> t, (t1, t2) -> {
                                        System.err.println("*** Merge fail:");
                                        System.err.println(t1);
                                        System.err.println(t2);
                                        return t1;
                                    })));
                }
            }
        }
        return processedTelemetry;
    }

    private Set<String> getAllFieldCodes() {
        if (allFieldCodes == null) {
            synchronized (this) {
                if (allFieldCodes == null) {
                    allFieldCodes = getProcessedTelemetry().values().stream()
                            .flatMap(m -> m.values().stream())
                            .map(t -> t.getTelemetryField().getCode())
                            .collect(Collectors.toSet());
                }
            }
        }
        return allFieldCodes;
    }


    public void printAsCsv(OutputStream out, Charset charset) {
        final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, charset)));
        printCsvHeader(writer);

        final Map<String, Value> currentRecord = new HashMap<>();
        getAllFieldCodes().forEach(fieldCode -> currentRecord.put(fieldCode, null));
        getProcessedTelemetry().forEach((epochMilli, telemetryMap) -> {
            telemetryMap.forEach((fieldCode, telemetry) -> {
                final Value value = telemetry.getValue();
                if (!isNullOrEmpty(value.toString())) {
                    currentRecord.put(fieldCode, value);
                }
            });
            final String fieldCodeValues = getAllFieldCodes().stream()
                    .map(fieldCode -> {
                        final Value value = currentRecord.get(fieldCode);
                        if (value == null) {
                            return "";
                        }
                        return valueToString(value);
                    })
                    .collect(joining(","));
            writer.println(convertDateTime(epochMilli) + "," + fieldCodeValues);
        });
        writer.flush();
    }

    private void printCsvHeader(PrintWriter writer) {
        writer.println("Time," + getAllFieldCodes().stream()
                .map(fieldCode -> CsvFieldMapper.mapper().convertTypeName(fieldCode))
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
