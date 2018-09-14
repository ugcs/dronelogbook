package ugcs.upload.logbook;

import com.ugcs.ucs.proto.DomainProto.Semantic;
import com.ugcs.ucs.proto.DomainProto.Telemetry;
import com.ugcs.ucs.proto.DomainProto.Value;
import lombok.SneakyThrows;
import ugcs.csv.CsvWriter;
import ugcs.exceptions.ExpectedException;
import ugcs.processing.telemetry.FlightTelemetry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.ugcs.ucs.proto.DomainProto.Semantic.S_LATITUDE;
import static com.ugcs.ucs.proto.DomainProto.Semantic.S_LONGITUDE;
import static java.lang.Math.toDegrees;
import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.util.Objects.isNull;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static ugcs.upload.logbook.FieldCodeToCsvColumnNameMapper.mapper;
import static ugcs.upload.logbook.UploadResponse.fromList;

public class LogBookUploader {
    private static final Predicate<String> MD5_HASH_PREDICATE = Pattern.compile("^[a-fA-F0-9]{32}$").asPredicate();
    private static final Charset CSV_FILE_CHARSET = Charset.forName("UTF-8");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .append(ISO_LOCAL_DATE)
            .appendLiteral("T")
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendFraction(NANO_OF_SECOND, 3, 3, true)
            .toFormatter();

    private static final List<String> FILED_CODES = Arrays.asList(
            "Time",
            "latitude",
            "longitude",
            "altitude_agl",
            "ground_speed",
            "main_voltage",
            "main_current"
    );

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

    @SneakyThrows
    public void saveTelemetryDataToCsvFile(Path pathToFile,
                                           SortedMap<Long, Map<String, Telemetry>> telemetryData,
                                           Set<String> fieldCodes) {
        final List<String> columnNames = new LinkedList<>();
        columnNames.add("Time");
        columnNames.addAll(fieldCodes);

        try (final OutputStream out = new FileOutputStream(pathToFile.toFile())) {
            final CsvWriter csvWriter = new CsvWriter(columnNames, out, CSV_FILE_CHARSET);
            csvWriter.printHeader(fieldCode -> mapper().convert(fieldCode));
            telemetryData.forEach((epochMilli, telemetryRecord) ->
                    printCsvRecord(csvWriter, epochMilli, telemetryRecord));
        }
    }

    @SneakyThrows
    public FlightUploadResponse uploadFlight(FlightTelemetry flight) {
        final File csvFile = File.createTempFile(flight.getVehicle().getName(), "");
        try (final OutputStream out = new FileOutputStream(csvFile)) {
            final CsvWriter csvWriter = new CsvWriter(FILED_CODES, out, CSV_FILE_CHARSET);
            csvWriter.printHeader(fieldCode -> mapper().convert(fieldCode));
            flight.getTelemetry().forEach(timeAndTelemetry ->
                    printCsvRecord(csvWriter, timeAndTelemetry.getLeft(), timeAndTelemetry.getRight())
            );
        }

        return new FlightUploadResponse(flight, csvFile, fromList(uploadFile(csvFile)));
    }

    private List<String> uploadFile(File file) {
        try {
            MultipartUtility multipart = new MultipartUtility(serverUrl, CSV_FILE_CHARSET.displayName());

            multipart.addFormField("login", login);
            multipart.addFormField("password", passwordAsMd5Hash);
            multipart.addFilePart("data", file);

            return multipart.finish();
        } catch (IOException connectException) {
            throw new ExpectedException("LogBook service unavailable.", connectException);
        }
    }

    private static void printCsvRecord(CsvWriter csvWriter, long timeEpochMilli, Map<String, Telemetry> telemetryRecord) {
        csvWriter.printRecord(colName -> {
            if ("Time".equals(colName)) {
                return convertDateTime(timeEpochMilli);
            }
            final Telemetry telemetry = telemetryRecord.get(colName);
            if (isNull(telemetry)) {
                return "";
            }
            return valueToCsvString(telemetry.getValue(), telemetry.getTelemetryField().getSemantic());
        });
    }

    private static String convertDateTime(long epochMilli) {
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(ofEpochMilli(epochMilli), ZoneId.systemDefault());
        return localDateTime.format(DATE_TIME_FORMATTER);
    }

    private static String valueToCsvString(Value value, Semantic semantic) {
        if (S_LATITUDE.equals(semantic) || S_LONGITUDE.equals(semantic)) {
            return String.valueOf(toDegrees(value.getDoubleValue()));
        }

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