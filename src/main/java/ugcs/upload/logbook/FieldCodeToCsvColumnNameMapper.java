package ugcs.upload.logbook;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class FieldCodeToCsvColumnNameMapper {
    private final Map<String, String> filedCodeToCsvColumnName;

    private static volatile FieldCodeToCsvColumnNameMapper instance;

    static FieldCodeToCsvColumnNameMapper mapper() {
        if (instance == null) {
            synchronized (FieldCodeToCsvColumnNameMapper.class) {
                if (instance == null) {
                    instance = new FieldCodeToCsvColumnNameMapper();
                }
            }
        }
        return instance;
    }

    private FieldCodeToCsvColumnNameMapper() {
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
