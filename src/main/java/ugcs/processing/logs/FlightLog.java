package ugcs.processing.logs;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.processing.AbstractFlight;

class FlightLog extends AbstractFlight {
    FlightLog(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle) {
        super(flightStartEpochMilli, flightEndEpochMilli, vehicle);
    }
}
