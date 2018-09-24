package ugcs.ucsHub.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import lombok.SneakyThrows;
import ugcs.common.operation.FutureWrapper;
import ugcs.common.operation.Operation;
import ugcs.common.operation.OperationPerformer;
import ugcs.net.SessionController;
import ugcs.processing.Flight;
import ugcs.processing.telemetry.FlightTelemetry;
import ugcs.processing.telemetry.FlightTelemetryProcessor;
import ugcs.processing.telemetry.TelemetryProcessor;
import ugcs.processing.telemetry.frames.TelemetryFramesProcessor;
import ugcs.upload.logbook.FlightUploadResponse;
import ugcs.upload.logbook.LogBookUploader;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.time.ZoneId.systemDefault;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.invokeLater;
import static ugcs.csv.telemetry.TelemetryDataSaver.saveTelemetryDataToCsvFile;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.WaitForm.waitForm;
import static ugcs.upload.logbook.FlightUploadPerformerFactory.performerFactory;

public class VehicleListForm extends JPanel {
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final Map<String, Vehicle> vehicleMap;
    private final JList<String> vehicleJList;

    private final SessionController controller;

    private final FlightTablePanel flightTable;

    private final DatePicker datePicker;

    public VehicleListForm(SessionController controller) {
        super(new BorderLayout());

        this.controller = controller;

        vehicleMap = controller.getVehicles().stream()
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
        uploadTelemetryButton.addActionListener(event -> getSelectedVehicle().ifPresent(vehicle -> {
            final Set<? extends Flight> selectedFlights = flightTable.getSelectedFlights();
            if (selectedFlights.isEmpty()) {
                return;
            }

            final List<FlightTelemetry> flightTelemetries = waitForm().waitOnCallable(
                    "Acquiring data from UgCS..."
                    , () -> selectedFlights.stream()
                            .flatMap(flight -> flight instanceof FlightTelemetry
                                    ? Stream.of((FlightTelemetry) flight)
                                    : new TelemetryProcessor(
                                    controller.getTelemetry(
                                            vehicle,
                                            flight.getStartEpochMilli(),
                                            flight.getEndEpochMilli()
                                    ).getTelemetryList(), vehicle)
                                    .getFlightTelemetries().stream())
                            .collect(toList())
                    , this
            );

            final long startTimeEpochMilli = selectedFlights.stream()
                    .mapToLong(Flight::getStartEpochMilli)
                    .min().orElse(0);
            final long endTimeEpochMilli = selectedFlights.stream()
                    .mapToLong(Flight::getEndEpochMilli)
                    .max().orElse(0);
            final Path pathToTelemetryFile = getPathToTelemetryFile(vehicle, startTimeEpochMilli, endTimeEpochMilli);
            final FlightTelemetryProcessor flightTelemetryProcessor = new FlightTelemetryProcessor(flightTelemetries, vehicle);
            waitForm().waitOnAction("Saving telemetry data...",
                    () -> saveTelemetryDataToCsvFile(pathToTelemetryFile,
                            flightTelemetryProcessor.getProcessedTelemetry(),
                            flightTelemetryProcessor.getAllFieldCodes()), this);

            if (flightTelemetries.size() > 0) {
                uploadFlightTelemetry(vehicle, flightTelemetries);
            }

        }));
        this.add(BorderLayout.SOUTH, bottomPanel);

        flightTable.addTableChangeAction(
                () -> uploadTelemetryButton.setEnabled(!flightTable.getSelectedFlights().isEmpty()));

        final JPanel timePickersPanel = new JPanel();
        timePickersPanel.setLayout(new BoxLayout(timePickersPanel, BoxLayout.Y_AXIS));

        final JPanel datePickerPanel = new JPanel();
        datePickerPanel.setBorder(BorderFactory.createTitledBorder("Pick the date"));
        final DatePickerSettings datePickerSettings = new DatePickerSettings();
        datePickerSettings.setAllowEmptyDates(false);
        datePicker = new DatePicker(datePickerSettings);
        datePickerPanel.add(datePicker);
        timePickersPanel.add(datePickerPanel);

        bottomPanel.add(BorderLayout.CENTER, timePickersPanel);

        Consumer<Vehicle> updateFlightsTableForVehicle = vehicle ->
                updateFlightsTable(getSelectedStartTimeAsEpochMilli(), getSelectedEndTimeAsEpochMilli(), vehicle);

        vehicleJList.addListSelectionListener(event ->
                invokeLater(() -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle)));

        datePicker.addDateChangeListener(event ->
                invokeLater(() -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle))
        );
    }

    @SneakyThrows
    private void updateFlightsTable(long startTimeEpochMilli, long endTimeEpochMilli, Vehicle vehicle) {
        final Callable<List<? extends Flight>> getFlightListCallable =
                () -> getFlightsByTelemetryFrames(vehicle, startTimeEpochMilli, endTimeEpochMilli);

        final List<? extends Flight> flights =
                waitForm().waitOnCallable("Acquiring data from UgCS...", getFlightListCallable, this);

        flightTable.updateModel(flights);
    }

    private List<? extends Flight> getFlightsByTelemetry(Vehicle vehicle,
                                                         long startTimeEpochMilli, long endTimeEpochMilli) {
        final List<DomainProto.Telemetry> telemetryList =
                controller.getTelemetry(vehicle, startTimeEpochMilli, endTimeEpochMilli).getTelemetryList();
        final TelemetryProcessor telemetryProcessor = new TelemetryProcessor(telemetryList, vehicle);
        return telemetryProcessor.getFlightTelemetries();
    }

    private List<? extends Flight> getFlightsByTelemetryFrames(Vehicle vehicle,
                                                               long startTimeEpochMilli, long endTimeEpochMilli) {
        final TelemetryFramesProcessor framesProcessor =
                new TelemetryFramesProcessor(controller, vehicle, startTimeEpochMilli, endTimeEpochMilli);
        return framesProcessor.getFlightFrames();
    }

    private Path getPathToTelemetryFile(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        Path pathToTelemetryFile = settings().getTelemetryPath()
                .resolve(generateFileName(vehicle.getName(), startTimeEpochMilli, endTimeEpochMilli, ""));
        int numberOfTries = 1;
        while (exists(pathToTelemetryFile) && numberOfTries < 1000) {
            final String telemetryFileName =
                    generateFileName(vehicle.getName(), startTimeEpochMilli, endTimeEpochMilli, "-" + numberOfTries);
            pathToTelemetryFile = settings().getTelemetryPath().resolve(telemetryFileName);
            ++numberOfTries;
        }
        return pathToTelemetryFile;
    }

    private Optional<Vehicle> getSelectedVehicle() {
        return Optional.ofNullable(vehicleMap.get(vehicleJList.getSelectedValue()));
    }

    private long getSelectedStartTimeAsEpochMilli() {
        return getTimeAsEpochMilli(datePicker.getDate(), LocalTime.of(0, 0));
    }

    private long getSelectedEndTimeAsEpochMilli() {
        return getTimeAsEpochMilli(datePicker.getDate().plusDays(1), LocalTime.of(0, 0));
    }

    private static long getTimeAsEpochMilli(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).atZone(systemDefault()).toEpochSecond() * 1000L;
    }

    private static String generateFileName(String vehicleName, long startTimeEpochMilli, long endTimeEpochMilli,
                                           String fileSuffix) {
        return (vehicleName + "-" + FILE_DATE_FORMAT.format(new Date(startTimeEpochMilli)) +
                "-" + FILE_DATE_FORMAT.format(new Date(endTimeEpochMilli)) + fileSuffix + ".csv")
                .replaceAll("[*/\\\\!|:?<>]", "_")
                .replaceAll("(%22)", "_");
    }

    private static Path generateUniqueFileName(Path targetFolder, String vehicleName, FlightUploadResponse flightResponse) {
        final long flightStartEpochMilli = flightResponse.getFlightTelemetry().getStartEpochMilli();
        final long flightEndEpochMilli = flightResponse.getFlightTelemetry().getEndEpochMilli();
        Path uniqueFilePath = targetFolder.resolve(
                generateFileName(vehicleName, flightStartEpochMilli, flightEndEpochMilli, "")
        );

        int numberOfTries = 1;
        while (exists(uniqueFilePath) && numberOfTries < 1000) {
            uniqueFilePath = targetFolder.resolve(
                    generateFileName(vehicleName, flightStartEpochMilli, flightEndEpochMilli, "-" + numberOfTries)
            );
            ++numberOfTries;
        }

        return uniqueFilePath;
    }

    @SneakyThrows
    private static void storeUploadedFlights(Collection<FlightUploadResponse> responses, String vehicleName) {
        Path targetFolder = settings().getUploadedFlightsPath();

        if (!isDirectory(targetFolder)) {
            Files.createDirectory(targetFolder);
        }

        for (FlightUploadResponse response : responses) {
            final Path targetFilePath = generateUniqueFileName(targetFolder, vehicleName, response);
            response.storeFlightTelemetry(targetFilePath);
        }
    }

    private void uploadFlightTelemetry(Vehicle vehicle, List<FlightTelemetry> flightTelemetries) {
        LogBookUploader uploader =
                new LogBookUploader(settings().getUploadServerUrl(),
                        settings().getUploadServerLogin(),
                        settings().getUploadServerPassword());

        final OperationPerformer<Flight, FlightUploadResponse> performer = performerFactory().getPerformer();
        final List<Operation<Flight, FlightUploadResponse>> operationResults =
                waitForm().waitOnCallable("Uploading flights to LogBook...",
                        () -> flightTelemetries.stream()
                                .map(flight -> performer.submit(flight, () -> uploader.uploadFlight(flight)))
                                .map(FutureWrapper::of)
                                .map(FutureWrapper::get)
                                .collect(toList()),
                        this);

        final Collection<FlightUploadResponse> performedResponses = operationResults.stream()
                .flatMap(Operation::getResultAsStream)
                .collect(toSet());
        storeUploadedFlights(performedResponses, vehicle.getName());

        UploadReportForm.showReport(this, operationResults);
    }
}
