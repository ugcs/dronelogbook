package ugcs.csv.telemetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper between telemetry field codes and column names in csv-formatted data
 */
final class TelemetryFieldCodeToCsvColumnNameMapper {
    private final Map<String, String> filedCodeToCsvColumnName;

    private static volatile TelemetryFieldCodeToCsvColumnNameMapper instance;

    static TelemetryFieldCodeToCsvColumnNameMapper mapper() {
        if (instance == null) {
            synchronized (TelemetryFieldCodeToCsvColumnNameMapper.class) {
                if (instance == null) {
                    instance = new TelemetryFieldCodeToCsvColumnNameMapper();
                }
            }
        }
        return instance;
    }

    private TelemetryFieldCodeToCsvColumnNameMapper() {
        Map<String, String> filedCodeToCsvColumnName = new HashMap<>();
        filedCodeToCsvColumnName.put("latitude", "fc:latitude");
        filedCodeToCsvColumnName.put("longitude", "fc:longitude");
        filedCodeToCsvColumnName.put("altitude_agl", "cs:altitude_agl");
        filedCodeToCsvColumnName.put("ground_speed", "fc:ground_speed");
        filedCodeToCsvColumnName.put("main_voltage", "fc:main_voltage");
        filedCodeToCsvColumnName.put("main_current", "fc:main_current");

        this.filedCodeToCsvColumnName = Collections.unmodifiableMap(filedCodeToCsvColumnName);
    }

    String convert(String telemetryFieldCode) {
        return filedCodeToCsvColumnName.getOrDefault(telemetryFieldCode, telemetryFieldCode);
    }
}
