package ugcs.processing.telemetry;

import ugcs.common.files.FileNameGenerator;
import ugcs.processing.Flight;

import java.nio.file.Path;

/**
 * {@link FileNameGenerator} for files with .csv extension
 */
public class CsvFileNameGenerator extends FileNameGenerator {

    public CsvFileNameGenerator(Path targetFolder, Flight flight) {
        super(targetFolder, "csv", flight.getVehicle().getName(), flight.getStartDate(), flight.getEndDate());
    }
}
