package ugcs.processing.logs;

public class FlightLog {
    private final long flightStartEpochMilli;
    private final long flightEndEpochMilli;

    public FlightLog(long flightStartEpochMilli, long flightEndEpochMilli) {
        this.flightStartEpochMilli = flightStartEpochMilli;
        this.flightEndEpochMilli = flightEndEpochMilli;
    }

    public long getFlightStartEpochMilli() {
        return flightStartEpochMilli;
    }

    public long getFlightEndEpochMilli() {
        return flightEndEpochMilli;
    }
}
