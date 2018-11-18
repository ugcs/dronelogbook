package ugcs.ucsHub.ui.util;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static java.time.Instant.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PeriodRepresentationTest {
    @Test
    void testPeriodOfRounding() {
        final Date startDate = fromString("2000-01-01T00:00:00.00Z");

        final Date endDate_1 = fromString("2000-01-01T00:03:46.769Z");
        final Date endDate_2 = fromString("2000-01-01T00:03:46.499Z");
        final Date endDate_3 = fromString("2000-01-01T01:01:00.501Z");

        final String result_1 = PresentationUtil.periodToString(startDate, endDate_1);
        assertEquals("3 minutes 47 s", result_1);

        final String result_2 = PresentationUtil.periodToString(startDate, endDate_2);
        assertEquals("3 minutes 46 s", result_2);

        final String result_3 = PresentationUtil.periodToString(startDate, endDate_3);
        assertEquals("1 hour 1 minute 1 s", result_3);
    }

    private Date fromString(String isoDate) {
        return new Date(parse(isoDate).toEpochMilli());
    }
}