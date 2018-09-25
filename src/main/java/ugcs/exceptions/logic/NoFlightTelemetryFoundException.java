package ugcs.exceptions.logic;

import ugcs.exceptions.ExpectedException;
import ugcs.processing.Flight;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class NoFlightTelemetryFoundException extends ExpectedException {

    private static DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public NoFlightTelemetryFoundException(Flight flight) {
        super("No flight related telemetry found for flight starting at " + DATE_TIME_FORMAT.format(flight.getStartDate()) + ".");
    }
}
