package ugcs.ucsHub.ui;

import com.github.lgooddatepicker.optionalusertools.DateHighlightPolicy;
import com.github.lgooddatepicker.zinternaltools.HighlightInformation;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.processing.telemetry.frames.TelemetryFramesProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;

import static java.awt.Color.BLACK;
import static java.awt.Color.PINK;
import static java.awt.Color.WHITE;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static ugcs.net.SessionController.sessionController;

/**
 * {@link DateHighlightPolicy} highlighting dates containing telemetry data
 */
public class TelemetryDatesHighlighter implements DateHighlightPolicy {
    private static final long MILLIS_IN_DAY = 24L * 60 * 60 * 1000;

    private static HighlightInformation NORMAL = new HighlightInformation(WHITE);
    private static HighlightInformation HIGHLIGHTED = new HighlightInformation(PINK, BLACK);

    private Map<LocalDate, HighlightInformation> dateToHighlightInfo = emptyMap();
    private Vehicle currentVehicle = null;

    @Override
    public HighlightInformation getHighlightInformationOrNull(LocalDate date) {
        if (dateToHighlightInfo.containsKey(date)) {
            return dateToHighlightInfo.get(date);
        }
        return updateHighlightInformationAndGet(date);
    }

    void setCurrentVehicle(Vehicle currentVehicle) {
        this.currentVehicle = currentVehicle;
        invalidateHighlightInformation();
    }

    private HighlightInformation updateHighlightInformationAndGet(LocalDate date) {
        return getCurrentVehicle().map(vehicle -> {
            final LocalDate startDate = date.withDayOfMonth(1);
            final LocalDate endDate = startDate.plusMonths(1);

            final List<Boolean> frames =
                    new TelemetryFramesProcessor(sessionController(), vehicle, startDate, endDate, MILLIS_IN_DAY).getFrames();

            final Map<LocalDate, HighlightInformation> highlightMapping = LongStream
                    .range(0, frames.size())
                    .boxed()
                    .collect(toMap(startDate::plusDays, i -> frames.get(i.intValue()) ? HIGHLIGHTED : NORMAL));

            setHighlightInformation(highlightMapping);

            return highlightMapping.get(date);
        }).orElse(NORMAL);
    }

    private Optional<Vehicle> getCurrentVehicle() {
        return ofNullable(currentVehicle);
    }

    private void invalidateHighlightInformation() {
        dateToHighlightInfo = emptyMap();
    }

    private void setHighlightInformation(Map<LocalDate, HighlightInformation> highlightMapping) {
        this.dateToHighlightInfo = highlightMapping;
    }
}
