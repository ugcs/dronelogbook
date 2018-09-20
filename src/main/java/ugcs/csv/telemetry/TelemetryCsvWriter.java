package ugcs.csv.telemetry;

import com.ugcs.ucs.proto.DomainProto;
import ugcs.csv.CsvWriter;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Map;

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
import static ugcs.csv.telemetry.TelemetryFieldCodeToCsvColumnNameMapper.mapper;

public class TelemetryCsvWriter extends CsvWriter {
    public static final Charset CSV_FILE_CHARSET = Charset.forName("UTF-8");

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

    public TelemetryCsvWriter(List<String> columnNames, OutputStream out) {
        super(columnNames, out, CSV_FILE_CHARSET);
    }

    public void printHeader() {
        super.printHeader(fieldCode -> mapper().convert(fieldCode));
    }

    public void printTelemetryRecord(long timeEpochMilli, Map<String, DomainProto.Telemetry> telemetryRecord) {
        printRecord(colName -> {
            if ("Time".equals(colName)) {
                return convertDateTime(timeEpochMilli);
            }
            final DomainProto.Telemetry telemetry = telemetryRecord.get(colName);
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

    private static String valueToCsvString(DomainProto.Value value, DomainProto.Semantic semantic) {
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
}
