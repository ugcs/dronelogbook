package ugcs.processing.telemetry.frames;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.processing.AbstractFlight;

import java.time.Instant;
import java.time.ZonedDateTime;

class FlightFrame extends AbstractFlight {
    FlightFrame(ZonedDateTime flightStart, ZonedDateTime flightEnd, Vehicle vehicle) {
        super(toEpochMilli(flightStart), toEpochMilli(flightEnd), vehicle);
    }

    private static long toEpochMilli(ZonedDateTime zonedDateTime) {
        final Instant instant = zonedDateTime.toInstant();
        return instant.getEpochSecond() * 1000 + instant.getNano() / 1000_000;
    }
}
