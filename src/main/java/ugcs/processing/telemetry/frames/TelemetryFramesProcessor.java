package ugcs.processing.telemetry.frames;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.net.SessionController;
import ugcs.common.LazyFieldEvaluator;

import java.time.ZonedDateTime;
import java.util.List;

import static java.time.Duration.between;

public class TelemetryFramesProcessor extends LazyFieldEvaluator {
    private final static double DEFAULT_INTERVAL_SEC = 30.0;

    private final SessionController controller;
    private final Vehicle vehicle;
    private final long originTimeEpochMilli;
    private final int number;

    public TelemetryFramesProcessor(SessionController controller, Vehicle vehicle, ZonedDateTime startTime, ZonedDateTime endTime) {
        this.controller = controller;
        this.vehicle = vehicle;
        this.originTimeEpochMilli = startTime.toInstant().toEpochMilli();
        this.number = (int) (between(startTime, endTime).toMillis() / getIntervalMillis());
    }

    private List<Boolean> getFrames() {
        return evaluateField("frames",
                () -> controller
                        .traceTelemetryFrames(vehicle, originTimeEpochMilli, getIntervalSec(), getNumber())
                        .getFramesList()
        );
    }

    private double getIntervalSec() {
        return DEFAULT_INTERVAL_SEC;
    }

    private long getIntervalMillis() {
        return (long) (getIntervalSec() * 1000.0);
    }

    private int getNumber() {
        return number;
    }
}
