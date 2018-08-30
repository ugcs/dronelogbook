package ugcs.upload.logbook;

import com.ugcs.ucs.proto.DomainProto.Telemetry;
import com.ugcs.ucs.proto.DomainProto.Value;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.telemetry.FlightTelemetry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static ugcs.upload.logbook.FieldCodeToCsvColumnNameMapper.mapper;

public class LogBookUploader {
    private static final Predicate<String> MD5_HASH_PREDICATE = Pattern.compile("^[a-fA-F0-9]{32}$").asPredicate();
    private static final Charset CSV_FILE_CHARSET = Charset.forName("UTF-8");

    private final String serverUrl;
    private final String login;
    private final String passwordAsMd5Hash;

    public LogBookUploader(String serverUrl, String login, String rawPasswordOrMd5Hash) {
        this.serverUrl = serverUrl;
        this.login = login;
        this.passwordAsMd5Hash = Optional.of(rawPasswordOrMd5Hash)
                .filter(MD5_HASH_PREDICATE)
                .orElseGet(() -> calculateMd5Hash(rawPasswordOrMd5Hash.getBytes()));
    }

    public void saveTelemetryDataToCsvFile(File file,
                                           SortedMap<Long, Map<String, Telemetry>> telemetryData,
                                           Set<String> fieldCodes) {
        final List<String> columnNames = new LinkedList<>();
        columnNames.add("Time");
        columnNames.addAll(fieldCodes);

        try (final OutputStream out = new FileOutputStream(file)) {
            final CsvWriter csvWriter = new CsvWriter(columnNames, out, CSV_FILE_CHARSET);
            csvWriter.printHeader(fieldCode -> mapper().convert(fieldCode));
            telemetryData.forEach((epochMilli, telemetryRecord) ->
                    csvWriter.printRecord(colName -> {
                        if ("Time".equals(colName)) {
                            return convertDateTime(epochMilli);
                        }
                        final Telemetry telemetry = telemetryRecord.get(colName);
                        if (isNull(telemetry)) {
                            return "";
                        }
                        return valueToString(telemetry.getValue());
                    }));
        } catch (IOException toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    public List<Pair<FlightTelemetry, File>> uploadFlights(
            List<FlightTelemetry> flights, String vehicleName, Set<String> fieldCodes) {
        final List<String> columnNames = new LinkedList<>();
        columnNames.add("Time");
        columnNames.addAll(fieldCodes);

        final List<Pair<FlightTelemetry, File>> flightsAndCsvFiles = flights.stream().map(flight -> {
            try {
                final File csvFile = File.createTempFile(vehicleName, "");
                try (final OutputStream out = new FileOutputStream(csvFile)) {
                    final CsvWriter csvWriter = new CsvWriter(columnNames, out, CSV_FILE_CHARSET);
                    csvWriter.printHeader(fieldCode -> mapper().convert(fieldCode));
                    flight.getTelemetry().forEach(timeAndTelemetry ->
                            csvWriter.printRecord(colName -> {
                                if ("Time".equals(colName)) {
                                    return convertDateTime(timeAndTelemetry.getLeft());
                                }
                                final Telemetry telemetry = timeAndTelemetry.getRight().get(colName);
                                if (isNull(telemetry)) {
                                    return "";
                                }
                                return valueToString(telemetry.getValue());
                            }));
                }
                return Pair.of(flight, csvFile);
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }).collect(Collectors.toList());

        try {
            MultipartUtility multipart = new MultipartUtility(serverUrl, CSV_FILE_CHARSET.displayName());

            multipart.addFormField("login", login);
            multipart.addFormField("password", passwordAsMd5Hash);
            for (Pair<FlightTelemetry, File> pair : flightsAndCsvFiles) {
                multipart.addFilePart("data", pair.getRight());
            }

            multipart.finish();

            return flightsAndCsvFiles;
        } catch (IOException toRethrow) {
            throw new RuntimeException(toRethrow);
        }
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

    private static String calculateMd5Hash(byte[] rawData) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(rawData);
            final byte[] digest = md.digest();
            return printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
