package ugcs.csv.telemetry;

import com.ugcs.ucs.proto.DomainProto;
import lombok.SneakyThrows;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static ugcs.csv.telemetry.TelemetryFieldCodeToCsvColumnNameMapper.mapper;

/**
 * Utility class for saving telemetry data to csv-file
 */
public class TelemetryDataSaver {

    @SneakyThrows
    public static void saveTelemetryDataToCsvFile(Path pathToFile,
                                           SortedMap<Long, Map<String, DomainProto.Telemetry>> telemetryData,
                                           Set<String> fieldCodes) {
        final List<String> columnNames = new LinkedList<>();
        columnNames.add("Time");
        columnNames.addAll(fieldCodes);

        try (final OutputStream out = new FileOutputStream(pathToFile.toFile())) {
            final TelemetryCsvWriter telemetryWriter = new TelemetryCsvWriter(columnNames, out);
            telemetryWriter.printHeader(fieldCode -> mapper().convert(fieldCode));
            telemetryData.forEach(telemetryWriter::printTelemetryRecord);
        }
    }
}
