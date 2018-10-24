package ugcs.processing.flights;

import ugcs.common.LazyFieldEvaluator;
import ugcs.processing.Flight;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class for processing list of {@link Flight}s
 */
public class FlightListProcessor extends LazyFieldEvaluator {
    private final List<? extends Flight> flights;

    public FlightListProcessor(List<? extends Flight> flights) {
        this.flights = flights;
    }

    public boolean hasFlights(LocalDate date) {
        return getFlightDates().contains(date);
    }

    private Set<LocalDate> getFlightDates() {
        return evaluateField("flightDates",
                () -> flights.stream()
                        .map(Flight::getStartLocalDate)
                        .collect(Collectors.toSet())
        );
    }
}
