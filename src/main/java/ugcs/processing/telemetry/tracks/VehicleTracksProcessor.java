package ugcs.processing.telemetry.tracks;

import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.common.LazyFieldEvaluator;
import ugcs.net.SessionController;
import ugcs.processing.AbstractFlight;
import ugcs.processing.Flight;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static ugcs.net.SessionController.sessionController;

/**
 * Class for {@link Flight} calculation based on vehicle tracks
 */
public class VehicleTracksProcessor extends LazyFieldEvaluator {
    private final static int UNLIMITED_TRACKS = -1;

    private final SessionController controller;
    private final Vehicle vehicle;
    private final ZonedDateTime fromTime;
    private final ZonedDateTime toTime;
    private final int tracksLimit;

    public VehicleTracksProcessor(ZonedDateTime fromTime, ZonedDateTime toTime, Vehicle vehicle) {
        this(fromTime, toTime, UNLIMITED_TRACKS, vehicle);
    }

    public VehicleTracksProcessor(ZonedDateTime fromTime, ZonedDateTime toTime, int tracksLimit, Vehicle vehicle) {
        this.controller = sessionController();
        this.vehicle = vehicle;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.tracksLimit = tracksLimit;
    }

    public List<VehicleTrack> getVehicleTracks() {
        return evaluateField("vehicleTracks",
                () -> getVehicleTracksProto().stream()
                        .map(dto -> new VehicleTrack(vehicle, dto))
                        .sorted(comparing(AbstractFlight::getStartEpochMilli))
                        .collect(toList())
        );
    }

    private List<DomainProto.VehicleTrack> getVehicleTracksProto() {
        return evaluateField("tracksDto", () -> {
            final long fromTimeEpochMilli = fromTime.toInstant().toEpochMilli();
            final long toTimeEpochMilli = toTime.toInstant().toEpochMilli();
            return controller
                    .getVehicleTracks(singletonList(vehicle), fromTimeEpochMilli, toTimeEpochMilli, tracksLimit)
                    .getVehicleTracksList();
        });
    }
}
