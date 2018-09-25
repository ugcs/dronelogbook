package ugcs.processing.telemetry.frames;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.common.LazyFieldEvaluator;
import ugcs.net.SessionController;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import static java.time.Duration.between;
import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Collections.unmodifiableList;

public class TelemetryFramesProcessor extends LazyFieldEvaluator {
    private final static long DEFAULT_INTERVAL_MILLIS = 30000;

    private final SessionController controller;
    private final Vehicle vehicle;
    private final long originTimeEpochMilli;
    private final int number;
    private final ZonedDateTime startTime;
    private final long intervalMillis;

    public TelemetryFramesProcessor(SessionController controller, Vehicle vehicle,
                                    long startTimeEpochMilli, long endTimeEpochMilli) {
        this(controller, vehicle, toZonedDateTime(startTimeEpochMilli), toZonedDateTime(endTimeEpochMilli), DEFAULT_INTERVAL_MILLIS);
    }

    public TelemetryFramesProcessor(SessionController controller, Vehicle vehicle,
                                    LocalDate startDateInclusive, LocalDate endDateExclusive, long intervalMillis) {
        this(controller, vehicle,
                startDateInclusive.atStartOfDay(systemDefault()),
                endDateExclusive.atStartOfDay(systemDefault()),
                intervalMillis);
    }

    private TelemetryFramesProcessor(SessionController controller, Vehicle vehicle,
                                     ZonedDateTime startTime, ZonedDateTime endTime, long intervalMillis) {
        this.controller = controller;
        this.vehicle = vehicle;
        this.originTimeEpochMilli = startTime.toInstant().toEpochMilli();
        this.startTime = startTime;
        this.intervalMillis = intervalMillis;
        this.number = (int) (between(startTime, endTime).toMillis() / getIntervalMillis());
    }

    public List<Boolean> getFrames() {
        return evaluateField("frames",
                () -> controller
                        .traceTelemetryFrames(vehicle, originTimeEpochMilli, getIntervalSec(), getNumber())
                        .getFramesList()
        );
    }

    public List<FlightFrame> getFlightFrames() {
        return evaluateField("flightFrames",
                () -> {
                    final List<Boolean> frames = getFrames();

                    ZonedDateTime currentTime = startTime;
                    ZonedDateTime flightStartTime = null;
                    boolean isFlightFrame = false;
                    final List<FlightFrame> flightFrames = new LinkedList<>();

                    for (boolean telemetryFrame : frames) {
                        if (telemetryFrame && !isFlightFrame) {
                            isFlightFrame = true;
                            flightStartTime = currentTime;
                        }

                        currentTime = currentTime.plusNanos(getIntervalNanos());

                        if (!telemetryFrame && isFlightFrame) {
                            isFlightFrame = false;

                            final FlightFrame flightFrame = new FlightFrame(flightStartTime, currentTime, vehicle);

                            flightFrames.add(flightFrame);
                        }
                    }

                    return unmodifiableList(flightFrames);
                }
        );
    }

    private long getIntervalNanos() {
        return intervalMillis * 1000_000;
    }

    private double getIntervalSec() {
        return intervalMillis / 1000.0;
    }

    private long getIntervalMillis() {
        return (long) (getIntervalSec() * 1000.0);
    }

    private int getNumber() {
        return number;
    }

    private static ZonedDateTime toZonedDateTime(long epochMilli) {
        return ofInstant(ofEpochMilli(epochMilli), systemDefault());
    }
}
