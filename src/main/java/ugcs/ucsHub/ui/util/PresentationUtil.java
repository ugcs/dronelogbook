package ugcs.ucsHub.ui.util;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.TimeUnit;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Minute;

import java.util.Date;
import java.util.List;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;

public class PresentationUtil {

    public static String periodToString(Date startDate, Date endDate) {
        final PrettyTime prettyTime = new PrettyTime(startDate, ENGLISH);
        final List<Duration> durations = prettyTime.calculatePreciseDuration(endDate);
        return durations.stream()
                .map(PresentationUtil::durationToString)
                .collect(joining(" "));
    }

    private static String durationToString(Duration duration) {
        final Class<? extends TimeUnit> timeUnitClass = duration.getUnit().getClass();
        if (Minute.class.equals(timeUnitClass)) {
            return new PrettyTime(ENGLISH).formatDuration(duration) + " " + formatMillis(duration.getDelta());
        }

        if (JustNow.class.equals(timeUnitClass)) {
            return formatMillis(duration.getQuantity());
        }

        return new PrettyTime(ENGLISH).formatDuration(duration);
    }

    private static String formatMillis(long millis) {
        return millis / 1000L + " s";
    }


    private PresentationUtil() {
    }
}
