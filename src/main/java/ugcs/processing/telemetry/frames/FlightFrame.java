package ugcs.processing.telemetry.frames;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.processing.AbstractFlight;

class FlightFrame extends AbstractFlight {
    FlightFrame(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle) {
        super(flightStartEpochMilli, flightEndEpochMilli, vehicle);
    }
}
