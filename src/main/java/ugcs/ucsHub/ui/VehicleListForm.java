package ugcs.ucsHub.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.exceptions.ExpectedException;
import ugcs.net.SessionController;
import ugcs.processing.Flight;
import ugcs.processing.telemetry.FlightTelemetry;
import ugcs.processing.telemetry.FlightTelemetryProcessor;
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
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.invokeLater;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.WaitForm.waitForm;

public class VehicleListForm extends JPanel {
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final Map<String, Vehicle> vehicleMap;
    private final JList<String> vehicleJList;

    private final DatePicker datePicker;

    public VehicleListForm(SessionController controller, LogBookUploader uploader) {
        super(new BorderLayout());

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
                                            flight.getFlightStartEpochMilli(),
                                            flight.getFlightEndEpochMilli()
                                    ).getTelemetryList())
                                    .getFlightTelemetries().stream())
                            .collect(toList())
                    , this
            );

            final long startTimeEpochMilli = selectedFlights.stream()
                    .mapToLong(Flight::getFlightStartEpochMilli)
                    .min().orElse(0);
            final long endTimeEpochMilli = selectedFlights.stream()
                    .mapToLong(Flight::getFlightEndEpochMilli)
                    .max().orElse(0);
            final Path pathToTelemetryFile = getPathToTelemetryFile(vehicle, startTimeEpochMilli, endTimeEpochMilli);
            final FlightTelemetryProcessor flightTelemetryProcessor = new FlightTelemetryProcessor(flightTelemetries);
            waitForm().waitOnAction("Saving telemetry data...",
                    () -> uploader.saveTelemetryDataToCsvFile(pathToTelemetryFile,
                            flightTelemetryProcessor.getProcessedTelemetry(),
                            flightTelemetryProcessor.getAllFieldCodes()), this);

            if (flightTelemetries.size() > 0) {
                final Collection<FlightUploadResponse> uploadResponses =
                        waitForm().waitOnCallable("Uploading flights to LogBook..."
                                , () -> uploader.uploadFlights(flightTelemetries, vehicle.getName())
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

        final JPanel datePickerPanel = new JPanel();
        datePickerPanel.setBorder(BorderFactory.createTitledBorder("Pick the date"));
        final DatePickerSettings datePickerSettings = new DatePickerSettings();
        datePickerSettings.setAllowEmptyDates(false);
        datePicker = new DatePicker(datePickerSettings);
        datePickerPanel.add(datePicker);
        timePickersPanel.add(datePickerPanel);

        bottomPanel.add(BorderLayout.CENTER, timePickersPanel);

        Consumer<Vehicle> updateFlightsTableForVehicle = vehicle ->
                updateFlightsTable(controller, getSelectedStartTimeAsEpochMilli(),
                        getSelectedEndTimeAsEpochMilli(), flightTable, vehicle);

        vehicleJList.addListSelectionListener(event ->
                invokeLater(() -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle)));

        datePicker.addDateChangeListener(event ->
                invokeLater(() -> getSelectedVehicle().ifPresent(updateFlightsTableForVehicle))
        );
    }

    private void updateFlightsTable(SessionController controller, long startTimeEpochMilli,
                                    long endTimeEpochMilli, FlightTablePanel flightTable, Vehicle vehicle) {

        try {
            final long telemetryCount = controller.countTelemetry(vehicle, startTimeEpochMilli, endTimeEpochMilli);

            final Callable<List<FlightTelemetry>> getFlightListCallable = () -> {
                final List<DomainProto.Telemetry> telemetryList =
                        controller.getTelemetry(vehicle, startTimeEpochMilli, endTimeEpochMilli).getTelemetryList();
                final TelemetryProcessor telemetryProcessor = new TelemetryProcessor(telemetryList);
                return telemetryProcessor.getFlightTelemetries();
            };

            final List<FlightTelemetry> flightTelemetries = telemetryCount > 10000
                    ? waitForm().waitOnCallable("Acquiring data from UgCS...", getFlightListCallable, this)
                    : getFlightListCallable.call();

            flightTable.updateModel(flightTelemetries);
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
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

    private static long getTimeAsEpochMilli(DateTimePicker dateTimePicker) {
        return dateTimePicker.getDateTimePermissive().atZone(systemDefault()).toInstant().getEpochSecond() * 1000L;
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
