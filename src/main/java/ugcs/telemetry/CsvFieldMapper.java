package ugcs.telemetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class CsvFieldMapper {
    private final Map<String, String> typeNameToCsvFieldName;

    private static volatile CsvFieldMapper instance;

    static CsvFieldMapper mapper() {
        if (instance == null) {
            synchronized (CsvFieldMapper.class) {
                if (instance == null) {
                    instance = new CsvFieldMapper();
                }
            }
        }
        return instance;
    }

    private CsvFieldMapper() {
        Map<String, String> typeNameToCsvFieldName = new HashMap<>();
        typeNameToCsvFieldName.put("latitude", "fc:latitude");
        typeNameToCsvFieldName.put("longitude", "fc:longitude");
        typeNameToCsvFieldName.put("altitude_agl", "cs:altitude_agl");
        typeNameToCsvFieldName.put("ground_speed", "fc:ground_speed");
        typeNameToCsvFieldName.put("main_voltage", "fc:main_voltage");
        typeNameToCsvFieldName.put("main_current", "fc:main_current");

        this.typeNameToCsvFieldName = Collections.unmodifiableMap(typeNameToCsvFieldName);
    }

    String convertTypeName(String telemetryTypeName) {
        return typeNameToCsvFieldName.getOrDefault(telemetryTypeName, telemetryTypeName);
    }
}
