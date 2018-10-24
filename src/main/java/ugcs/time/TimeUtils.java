package ugcs.time;

import java.time.LocalDate;
import java.time.ZoneId;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZonedDateTime.ofInstant;

public class TimeUtils {

    private static volatile TimeUtils instance;

    public static TimeUtils time() {
        if (instance == null) {
            synchronized (TimeUtils.class) {
                if (instance == null) {
                    instance = new TimeUtils();
                }
            }
        }
        return instance;
    }

    public ZoneId defaultZoneId() {
        return ZoneId.systemDefault();
    }

    public LocalDate toLocalDate(long epochMilli) {
        return ofInstant(ofEpochMilli(epochMilli), time().defaultZoneId()).toLocalDate();
    }
}
