package ugcs.processing;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.common.identity.Identity;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ugcs.common.identity.Identity.of;

/**
 * Abstract {@link Vehicle} flight implementation
 */
public abstract class AbstractFlight implements Flight {
    private static SimpleDateFormat FLIGHT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final long flightStartEpochMilli;
    private final long flightEndEpochMilli;
    private final Vehicle vehicle;
    private final Identity<?> id;

    public AbstractFlight(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle, Identity<?> id) {
        this.flightStartEpochMilli = flightStartEpochMilli;
        this.flightEndEpochMilli = flightEndEpochMilli;
        this.vehicle = vehicle;
        this.id = id;
    }

    public AbstractFlight(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle) {
        this(flightStartEpochMilli, flightEndEpochMilli, vehicle,
                getIdentity(flightStartEpochMilli, flightEndEpochMilli, vehicle));
    }

    public AbstractFlight(Flight flight) {
        this(flight.getStartEpochMilli(), flight.getEndEpochMilli(), flight.getVehicle(), flight.getId());
    }

    @Override
    public long getStartEpochMilli() {
        return flightStartEpochMilli;
    }

    @Override
    public long getEndEpochMilli() {
        return flightEndEpochMilli;
    }

    @Override
    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public Identity<?> getId() {
        return id;
    }

    private static String textRepresentation(long flightStartEpochMilli) {
        return "Flight at " + FLIGHT_DATE_FORMAT.format(new Date(flightStartEpochMilli));
    }

    private static Identity<String> getIdentity(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle) {
        return of(flightStartEpochMilli + "_" + flightEndEpochMilli + "_"
                + vehicle.getSerialNumber(), textRepresentation(flightStartEpochMilli));
    }
}
