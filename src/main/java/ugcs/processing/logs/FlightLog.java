package ugcs.processing.logs;

import ugcs.processing.Flight;

public class FlightLog implements Flight {
    private final long flightStartEpochMilli;
    private final long flightEndEpochMilli;

    FlightLog(long flightStartEpochMilli, long flightEndEpochMilli) {
        this.flightStartEpochMilli = flightStartEpochMilli;
        this.flightEndEpochMilli = flightEndEpochMilli;
    }

    @Override
    public long getStartEpochMilli() {
        return flightStartEpochMilli;
    }

    @Override
    public long getEndEpochMilli() {
        return flightEndEpochMilli;
    }
}
