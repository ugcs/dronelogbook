package ugcs.ucsHub;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.ugcs.ucs.proto.DomainProto.Semantic.*;

public final class CsvFieldMapper {
    private final Map<String, String> typeNameToCsvFieldName;

    private static volatile CsvFieldMapper instance;

    public static CsvFieldMapper mapper() {
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
        typeNameToCsvFieldName.put(S_LATITUDE.toString(), "fc:latitude");
        typeNameToCsvFieldName.put(S_LONGITUDE.toString(), "fc:longitude");
        typeNameToCsvFieldName.put(S_ALTITUDE_AGL.toString(), "cs:altitude_agl");
        typeNameToCsvFieldName.put(S_GROUND_SPEED.toString(), "fc:ground_speed");
        typeNameToCsvFieldName.put(S_VOLTAGE.toString(), "fc:main_voltage");
        typeNameToCsvFieldName.put(S_CURRENT.toString(), "fc:main_current");

        this.typeNameToCsvFieldName = Collections.unmodifiableMap(typeNameToCsvFieldName);
    }

    public String convertTypeName(String telemetryTypeName) {
        return typeNameToCsvFieldName.getOrDefault(telemetryTypeName, telemetryTypeName);
    }
}
