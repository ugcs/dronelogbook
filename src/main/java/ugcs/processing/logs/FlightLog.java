package ugcs.processing.logs;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.processing.AbstractFlight;
import ugcs.processing.Flight;

/**
 * {@link Flight} implementation based on vehicle logs
 */
class FlightLog extends AbstractFlight {
    FlightLog(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle) {
        super(flightStartEpochMilli, flightEndEpochMilli, vehicle);
    }
}
