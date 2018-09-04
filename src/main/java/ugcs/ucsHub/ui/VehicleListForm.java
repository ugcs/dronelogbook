package ugcs.ucsHub.ui;

import com.github.lgooddatepicker.components.DateTimePicker;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.net.SessionController;
import ugcs.processing.logs.FlightLog;
import ugcs.processing.logs.LogsProcessor;
import ugcs.processing.telemetry.FlightTelemetry;
import ugcs.processing.telemetry.TelemetryProcessor;
import ugcs.upload.logbook.LogBookUploader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.WaitForm.waitForm;

public class VehicleListForm extends JPanel {
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final Map<String, Vehicle> vehicleMap;
    private final JList<String> vehicleJList;

    public VehicleListForm(SessionController controller, LogBookUploader uploader) {
        super(new BorderLayout());

        final DateTimePicker startDateTimePicker = new DateTimePicker();
        final DateTimePicker endDateTimePicker = new DateTimePicker();

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
        final FlightTablePanel flightTable = new FlightTablePanel();
        centerPanel.add(BorderLayout.CENTER, flightTable);
        this.add(BorderLayout.CENTER, centerPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        final JButton uploadTelemetryButton = new JButton("Upload");
        uploadTelemetryButton.setEnabled(false);
        bottomPanel.add(BorderLayout.EAST, new JPanel(new GridBagLayout()).add(uploadTelemetryButton).getParent());
        uploadTelemetryButton.addActionListener(event -> getSelectedVehicle().ifPresent(vehicle -> {
            final Set<FlightLog> selectedFlightLogs = flightTable.getSelectedFlights();
            if (selectedFlightLogs.isEmpty()) {
                return;
            }

            final List<DomainProto.Telemetry> telemetry = waitForm().waitOnCallable(
                    "Acquiring data from UgCS..."
                    , () -> selectedFlightLogs.stream()
                            .flatMap(flightLog -> controller
                                    .getTelemetry(vehicle, flightLog.getFlightStartEpochMilli(), flightLog.getFlightEndEpochMilli())
                                    .getTelemetryList()
                                    .stream())
                            .collect(toList())
                    , this
            );

            final TelemetryProcessor telemetryProcessor = new TelemetryProcessor(telemetry);

            final long startTimeEpochMilli = selectedFlightLogs.stream()
                    .mapToLong(FlightLog::getFlightStartEpochMilli)
                    .min().orElse(0);
            final long endTimeEpochMilli = selectedFlightLogs.stream()
                    .mapToLong(FlightLog::getFlightEndEpochMilli)
                    .max().orElse(0);
            final Path pathToTelemetryFile = getPathToTelemetryFile(vehicle, startTimeEpochMilli, endTimeEpochMilli);
            waitForm().waitOnAction("Saving telemetry data...",
                    () -> uploader.saveTelemetryDataToCsvFile(pathToTelemetryFile,
                            telemetryProcessor.getProcessedTelemetry(),
                            telemetryProcessor.getAllFieldCodes()), this);

            final List<FlightTelemetry> flights = telemetryProcessor.getFlightTelemetries();
            if (flights.size() > 0) {
                final List<Pair<FlightTelemetry, File>> flightsAndUploadedFiles =
                        waitForm().waitOnCallable("Uploading flights to LogBook..."
                                , () -> uploader.uploadFlights(flights, vehicle.getName())
                                , this
                        );

                final Path uploadPath = settings().getUploadedFlightsPath();
                moveUploadedFiles(flightsAndUploadedFiles, uploadPath,
                        (flight, fileSuffix) -> generateFileName(vehicle.getName(),
                                flight.getFlightStartEpochMilli(),
                                flight.getFlightEndEpochMilli(),
                                fileSuffix));

                JOptionPane.showMessageDialog(this,
                        "Telemetry data of the flight is saved to:\n" + uploadPath.toString(),
                        "Server Upload Successful", INFORMATION_MESSAGE);
            }

        }));
        this.add(BorderLayout.SOUTH, bottomPanel);

        flightTable.addTableChangeAction(
                () -> uploadTelemetryButton.setEnabled(!flightTable.getSelectedFlights().isEmpty()));

        final JPanel timePickersPanel = new JPanel();
        timePickersPanel.setLayout(new BoxLayout(timePickersPanel, BoxLayout.Y_AXIS));
        startDateTimePicker.setDateTimePermissive(LocalDateTime.now().minusHours(24));
        endDateTimePicker.setDateTimePermissive(LocalDateTime.now());

        final JPanel startTimePickerPanel = new JPanel();
        startTimePickerPanel.setBorder(BorderFactory.createTitledBorder("Start Date/Time"));
        startTimePickerPanel.add(startDateTimePicker);
        timePickersPanel.add(startTimePickerPanel);

        final JPanel endTimePickerPanel = new JPanel();
        endTimePickerPanel.setBorder(BorderFactory.createTitledBorder("End Date/Time"));
        endTimePickerPanel.add(endDateTimePicker);
        timePickersPanel.add(endTimePickerPanel);

        bottomPanel.add(BorderLayout.CENTER, timePickersPanel);

        Consumer<Vehicle> updateFlightsTableForVehicle = vehicle ->
                updateFlightsTable(controller, getTimeAsEpochMilli(startDateTimePicker),
                        getTimeAsEpochMilli(endDateTimePicker), flightTable, vehicle);

        vehicleJList.addListSelectionListener(event -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle));

        startDateTimePicker.addDateTimeChangeListener(event -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle));
        endDateTimePicker.addDateTimeChangeListener(event -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle));
    }

    private void updateFlightsTable(SessionController controller, long startTimeEpochMilli,
                                    long endTimeEpochMilli, FlightTablePanel flightTable, Vehicle vehicle) {
        final List<DomainProto.VehicleLogEntry> vehicleLogEntriesList =
                controller.getVehicleLog(vehicle, startTimeEpochMilli, endTimeEpochMilli).getVehicleLogEntriesList();

        final LogsProcessor logsProcessor = new LogsProcessor(vehicleLogEntriesList);
        final List<FlightLog> flightLogs = logsProcessor.getFlightLogs();

        flightTable.updateModel(flightLogs);
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

    private static long getTimeAsEpochMilli(DateTimePicker dateTimePicker) {
        return dateTimePicker.getDateTimePermissive().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond() * 1000L;
    }

    private String generateFileName(String vehicleName, long startTimeEpochMilli, long endTimeEpochMilli,
                                    String fileSuffix) {
        return (vehicleName + "-" + FILE_DATE_FORMAT.format(new Date(startTimeEpochMilli)) +
                "-" + FILE_DATE_FORMAT.format(new Date(endTimeEpochMilli)) + fileSuffix + ".csv")
                .replaceAll("[*/\\\\!|:?<>]", "_")
                .replaceAll("(%22)", "_");
    }

    private static void moveUploadedFiles(List<Pair<FlightTelemetry, File>> flightsAndUploadedFiles, Path targetFolder,
                                          BiFunction<FlightTelemetry, String, String> flightToFileName) {
        try {
            if (!isDirectory(targetFolder)) {
                Files.createDirectory(targetFolder);
            }

            for (Pair<FlightTelemetry, File> pair : flightsAndUploadedFiles) {
                Path targetFilePath = targetFolder.resolve(flightToFileName.apply(pair.getLeft(), ""));
                int numberOfTries = 1;
                while (exists(targetFilePath) && numberOfTries < 1000) {
                    targetFilePath = targetFolder.resolve(flightToFileName.apply(pair.getLeft(), "-" + numberOfTries));
                    ++numberOfTries;
                }
                Files.move(pair.getRight().toPath(), targetFilePath);
            }
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }
}
