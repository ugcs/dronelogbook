package ugcs.upload.logbook;

import ugcs.common.LazyFieldEvaluator;
import ugcs.common.operation.OperationPerformer;
import ugcs.processing.Flight;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class FlightUploadPerformerFactory extends LazyFieldEvaluator {
    private static volatile FlightUploadPerformerFactory instance;

    public static FlightUploadPerformerFactory performerFactory() {
        if (instance == null) {
            synchronized (FlightUploadPerformerFactory.class) {
                if (instance == null) {
                    instance = new FlightUploadPerformerFactory();
                }
            }
        }
        return instance;
    }

    private FlightUploadPerformerFactory() {
    }

    public OperationPerformer<Flight, FlightUploadResponse> getPerformer() {
        return evaluateField("uploadPerformer", () ->
                new OperationPerformer<>(newSingleThreadExecutor())
        );
    }
}
