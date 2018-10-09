package ugcs.ucsHub.ui;

import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import lombok.SneakyThrows;
import ugcs.common.identity.Identity;
import ugcs.common.operation.FutureWrapper;
import ugcs.common.operation.Operation;
import ugcs.exceptions.logic.NoFlightTelemetryFoundException;
import ugcs.processing.Flight;
import ugcs.processing.telemetry.CsvFileNameGenerator;
import ugcs.processing.telemetry.FlightTelemetry;
import ugcs.processing.telemetry.FlightTelemetryProcessor;
import ugcs.processing.telemetry.TelemetryProcessor;
import ugcs.processing.telemetry.frames.TelemetryFramesProcessor;
import ugcs.upload.logbook.LogBookUploader;
import ugcs.upload.logbook.UploadResponse;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.invokeLater;
import static org.slf4j.LoggerFactory.getLogger;
import static ugcs.csv.telemetry.TelemetryDataSaver.saveTelemetryDataToCsvFile;
import static ugcs.net.SessionController.sessionController;
import static ugcs.processing.telemetry.FlightTelemetry.withId;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.RefreshButton.refresher;
import static ugcs.ucsHub.ui.WaitForm.waitForm;
import static ugcs.upload.logbook.FlightUploadPerformerFactory.performerFactory;

public class VehicleListForm extends JPanel {
    private final Map<String, Vehicle> vehicleMap;
    private final JList<String> vehicleJList;

    private final FlightTablePanel flightTable;

    private final TelemetryDatesHighlighter datesHighlighter = new TelemetryDatesHighlighter();
    private final DatePickerPanel datePicker = new DatePickerPanel(datesHighlighter);

    public VehicleListForm() {
        super(new BorderLayout());

        vehicleMap = sessionController().getVehicles().stream()
                .collect(toMap(Vehicle::getName, v -> v));

        final String[] vehicleNames = vehicleMap.keySet().toArray(new String[0]);
        vehicleJList = new JList<>(vehicleNames);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createBevelBorder(0));
        leftPanel.add(BorderLayout.NORTH, new JLabel("List of all vehicles:"));
        vehicleJList.setBorder(BorderFactory.createTitledBorder(""));
        vehicleJList.setSelectionMode(SINGLE_SELECTION);
        leftPanel.add(BorderLayout.CENTER, new JScrollPane(vehicleJList));
        this.add(BorderLayout.WEST, leftPanel);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Flight list"));
        flightTable = new FlightTablePanel();
        centerPanel.add(BorderLayout.CENTER, flightTable);
        this.add(BorderLayout.CENTER, centerPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        final JButton uploadTelemetryButton = new JButton("Upload");
        uploadTelemetryButton.setEnabled(false);
        bottomPanel.add(BorderLayout.EAST, new JPanel(new GridBagLayout()).add(uploadTelemetryButton).getParent());
        uploadTelemetryButton.addActionListener(event -> uploadCurrentlySelectedFlights());
        this.add(BorderLayout.SOUTH, bottomPanel);

        flightTable.addTableChangeAction(
                () -> uploadTelemetryButton.setEnabled(!flightTable.getSelectedFlights().isEmpty()));

        bottomPanel.add(BorderLayout.CENTER, datePicker);

        vehicleJList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                invokeLater(this::refreshView);
            }
        });
        datePicker.addDateChangeListener(() -> invokeLater(this::refreshView));
        refresher().addRefreshListener(this::refreshView);
    }

    private void refreshView() {
        getSelectedVehicle().ifPresent(vehicle -> {
            updateFlightsTable(getSelectedStartTimeAsEpochMilli(), getSelectedEndTimeAsEpochMilli(), vehicle);
            datesHighlighter.setCurrentVehicle(vehicle);
        });
    }

    @SneakyThrows
    private void updateFlightsTable(long startTimeEpochMilli, long endTimeEpochMilli, Vehicle vehicle) {
        final Callable<List<? extends Flight>> getFlightListCallable =
                () -> getFlightsByTelemetry(vehicle, startTimeEpochMilli, endTimeEpochMilli);

        final List<? extends Flight> flights = waitForUgcsData(getFlightListCallable,
                () -> sessionController().countTelemetry(vehicle, startTimeEpochMilli, endTimeEpochMilli));

        flightTable.updateModel(flights);
    }

    private List<? extends Flight> waitForUgcsData(Callable<List<? extends Flight>> flightListCallable,
                                                   Callable<Long> telemetryCountCallable) throws Exception {
        boolean showWaitForm = true;
        if (telemetryCountCallable != null) {
            showWaitForm = telemetryCountCallable.call() > 10000;
        }
        return showWaitForm
                ? waitForm().waitOnCallable("Acquiring data from UgCS...", flightListCallable, this)
                : flightListCallable.call();
    }

    private List<? extends Flight> getFlightsByTelemetry(Vehicle vehicle,
                                                         long startTimeEpochMilli, long endTimeEpochMilli) {
        final List<DomainProto.Telemetry> telemetryList =
                sessionController().getTelemetry(vehicle, startTimeEpochMilli, endTimeEpochMilli).getTelemetryList();
        final TelemetryProcessor telemetryProcessor = new TelemetryProcessor(telemetryList, vehicle);
        return telemetryProcessor.getFlightTelemetries();
    }

    private List<? extends Flight> getFlightsByTelemetryFrames(Vehicle vehicle,
                                                               long startTimeEpochMilli, long endTimeEpochMilli) {
        final TelemetryFramesProcessor framesProcessor =
                new TelemetryFramesProcessor(sessionController(), vehicle, startTimeEpochMilli, endTimeEpochMilli);
        return framesProcessor.getFlightFrames();
    }

    private Optional<Vehicle> getSelectedVehicle() {
        return Optional.ofNullable(vehicleMap.get(vehicleJList.getSelectedValue()));
    }

    private long getSelectedStartTimeAsEpochMilli() {
        return datePicker.getSelectedStartTimeAsEpochMilli();
    }

    private long getSelectedEndTimeAsEpochMilli() {
        return datePicker.getSelectedEndTimeAsEpochMilli();
    }

    private UploadResponse uploadFlightTelemetry(FlightTelemetry flightTelemetry) {
        final String url = settings().getUploadServerUrl();
        final Path pathForUploadedFiles = settings().getUploadedFlightsPath();
        return new LogBookUploader(url, settings().getUploadServerLogin(), settings().getUploadServerPassword())
                .uploadFlight(flightTelemetry)
                .storeFlightTelemetry(new CsvFileNameGenerator(pathForUploadedFiles, flightTelemetry).generateUnique())
                .getUploadResponse();
    }

    private static void saveTelemetry(FlightTelemetryProcessor flightTelemetryProcessor, Flight flight) {
        final Path telemetryFilePath =
                new CsvFileNameGenerator(settings().getTelemetryPath(), flight).generateUnique();
        saveTelemetryDataToCsvFile(telemetryFilePath,
                flightTelemetryProcessor.getProcessedTelemetry(),
                flightTelemetryProcessor.getAllFieldCodes());
    }

    private Future<Operation<Identity<?>, UploadResponse>> submitFlightForUploading(Flight flight) {
        return performerFactory().getUploadPerformer().submit(flight.getId(), () -> {
            final FlightTelemetryProcessor flightTelemetryProcessor =
                    new FlightTelemetryProcessor(flight, sessionController());

            saveTelemetry(flightTelemetryProcessor, flight);

            final List<FlightTelemetry> flightTelemetries = flightTelemetryProcessor.getFlightTelemetries();
            if (flightTelemetries.isEmpty()) {
                throw new NoFlightTelemetryFoundException(flight);
            }
            if (flightTelemetries.size() > 1) {
                getLogger(getClass()).warn("Multiple flights telemetry found, only first flight will be uploaded");
            }

            FlightTelemetry flightForUpload = withId(flightTelemetries.get(0), flight.getId());
            return uploadFlightTelemetry(flightForUpload);
        });
    }

    private void uploadCurrentlySelectedFlights() {
        final Set<? extends Flight> selectedFlights = flightTable.getSelectedFlights();

        final List<Future<Operation<Identity<?>, UploadResponse>>> uploadOperationFutures = selectedFlights.stream()
                .map(this::submitFlightForUploading)
                .collect(toList());

        final List<Operation<Identity<?>, UploadResponse>> uploadResults =
                waitForm().waitOnCallable("Uploading flights to LogBook...",
                        () -> uploadOperationFutures.stream()
                                .map(FutureWrapper::of)
                                .map(FutureWrapper::get)
                                .collect(toList()),
                        this);

        UploadReportForm.showReport(this, uploadResults);
    }
}
