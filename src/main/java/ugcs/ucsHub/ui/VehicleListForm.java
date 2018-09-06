package ugcs.ucsHub.ui;

import com.github.lgooddatepicker.components.DateTimePicker;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.exceptions.ExpectedException;
import ugcs.net.SessionController;
import ugcs.processing.logs.FlightLog;
import ugcs.processing.logs.LogsProcessor;
import ugcs.processing.telemetry.FlightTelemetry;
import ugcs.processing.telemetry.TelemetryProcessor;
import ugcs.upload.logbook.FlightUploadResponse;
import ugcs.upload.logbook.LogBookUploader;
import ugcs.upload.logbook.UploadResponse;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
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
                final Collection<FlightUploadResponse> uploadResponses =
                        waitForm().waitOnCallable("Uploading flights to LogBook..."
                                , () -> uploader.uploadFlights(flights, vehicle.getName())
                                , this
                        );

                storeUploadedFlights(uploadResponses, vehicle.getName());

                showUploadResponseMessage(uploadResponses);
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

    private static String generateFileName(String vehicleName, long startTimeEpochMilli, long endTimeEpochMilli,
                                           String fileSuffix) {
        return (vehicleName + "-" + FILE_DATE_FORMAT.format(new Date(startTimeEpochMilli)) +
                "-" + FILE_DATE_FORMAT.format(new Date(endTimeEpochMilli)) + fileSuffix + ".csv")
                .replaceAll("[*/\\\\!|:?<>]", "_")
                .replaceAll("(%22)", "_");
    }

    private static Path generateUniqueFileName(Path targetFolder, String vehicleName, FlightUploadResponse flightResponse) {
        final long flightStartEpochMilli = flightResponse.getFlightTelemetry().getFlightStartEpochMilli();
        final long flightEndEpochMilli = flightResponse.getFlightTelemetry().getFlightEndEpochMilli();
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

    private static void storeUploadedFlights(Collection<FlightUploadResponse> responses, String vehicleName) {
        try {
            Path targetFolder = settings().getUploadedFlightsPath();

            if (!isDirectory(targetFolder)) {
                Files.createDirectory(targetFolder);
            }

            for (FlightUploadResponse response : responses) {
                final Path targetFilePath = generateUniqueFileName(targetFolder, vehicleName, response);
                response.storeFlightTelemetry(targetFilePath);
            }
        } catch (IOException toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    private void showUploadResponseMessage(Collection<FlightUploadResponse> uploadResponses) {
        final UploadResponse uploadResponse = uploadResponses.stream()
                .findAny()
                .map(FlightUploadResponse::getUploadResponse)
                .orElseThrow(() -> new ExpectedException("No flights were uploaded."));


        JPanel panel = new JPanel();

        panel.add(new JLabel(uploadResponse.getDescription().orElse("No description.")));
        uploadResponse.getUrl().ifPresent(url -> panel.add(new JHyperlink(url, "click to view on LogBook")));

        JOptionPane.showMessageDialog(this, panel, "LogBook response",
                uploadResponse.isWarning() ? WARNING_MESSAGE : INFORMATION_MESSAGE);
    }
}
