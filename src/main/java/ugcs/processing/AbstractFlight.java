package ugcs.processing;

import com.ugcs.ucs.proto.DomainProto.Vehicle;

public abstract class AbstractFlight implements Flight {
    private final long flightStartEpochMilli;
    private final long flightEndEpochMilli;
    private final Vehicle vehicle;

    public AbstractFlight(long flightStartEpochMilli, long flightEndEpochMilli, Vehicle vehicle) {
        this.flightStartEpochMilli = flightStartEpochMilli;
        this.flightEndEpochMilli = flightEndEpochMilli;
        this.vehicle = vehicle;
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
}
