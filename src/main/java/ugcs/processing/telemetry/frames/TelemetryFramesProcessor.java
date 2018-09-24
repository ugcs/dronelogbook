package ugcs.processing.telemetry.frames;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.common.LazyFieldEvaluator;
import ugcs.net.SessionController;

import java.time.Instant;
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

    public TelemetryFramesProcessor(SessionController controller, Vehicle vehicle,
                                    long startTimeEpochMilli, long endTimeEpochMilli) {
        this(controller, vehicle, toZonedDateTime(startTimeEpochMilli), toZonedDateTime(endTimeEpochMilli));
    }

    private TelemetryFramesProcessor(SessionController controller, Vehicle vehicle,
                                     ZonedDateTime startTime, ZonedDateTime endTime) {
        this.controller = controller;
        this.vehicle = vehicle;
        this.originTimeEpochMilli = startTime.toInstant().toEpochMilli();
        this.number = (int) (between(startTime, endTime).toMillis() / getIntervalMillis());
        this.startTime = startTime;
    }

    private List<Boolean> getFrames() {
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

                            final FlightFrame flightFrame =
                                    new FlightFrame(toEpochMilli(flightStartTime), toEpochMilli(currentTime), vehicle);

                            flightFrames.add(flightFrame);
                        }
                    }

                    return unmodifiableList(flightFrames);
                }
        );
    }

    private long getIntervalNanos() {
        return DEFAULT_INTERVAL_MILLIS * 1000_000;
    }

    private double getIntervalSec() {
        return DEFAULT_INTERVAL_MILLIS / 1000.0;
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

    private static long toEpochMilli(ZonedDateTime zonedDateTime) {
        final Instant instant = zonedDateTime.toInstant();
        return instant.getEpochSecond() * 1000 + instant.getNano() / 1000_000;
    }
}
