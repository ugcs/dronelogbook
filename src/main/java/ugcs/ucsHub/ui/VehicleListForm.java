package ugcs.ucsHub.ui;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import lombok.SneakyThrows;
import ugcs.common.identity.Identity;
import ugcs.common.operation.Operation;
import ugcs.exceptions.logic.NoFlightTelemetryFoundException;
import ugcs.processing.Flight;
import ugcs.processing.telemetry.CsvFileNameGenerator;
import ugcs.processing.telemetry.FlightTelemetry;
import ugcs.processing.telemetry.FlightTelemetryProcessor;
import ugcs.processing.telemetry.tracks.VehicleTracksProcessor;
import ugcs.upload.logbook.DroneLogbookResponse;
import ugcs.upload.logbook.LogbookUploader;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.Box.createGlue;
import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.invokeLater;
import static org.slf4j.LoggerFactory.getLogger;
import static ugcs.csv.telemetry.TelemetryDataSaver.saveTelemetryDataToCsvFile;
import static ugcs.net.SessionController.sessionController;
import static ugcs.processing.telemetry.FlightTelemetry.withId;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.RefreshButton.refresher;
import static ugcs.ucsHub.ui.WaitWithProgressBarForm.waitWithProgressBarForm;
import static ugcs.upload.logbook.FlightUploadPerformerFactory.performerFactory;

/**
 * Form containing controls for flight list representation and uploading
 */
public class VehicleListForm extends JPanel {
    private Map<String, Vehicle> vehicleMap;
    private final JList<String> vehicleJList;

    private final FlightTablePanel flightTable;

    private final TelemetryDatesHighlighter datesHighlighter = new TelemetryDatesHighlighter();
    private final DatePickerPanel datePicker = new DatePickerPanel(datesHighlighter);

    private final JButton logoutButton = new JButton("Logout");

    public VehicleListForm() {
        super(new BorderLayout());

        vehicleJList = new JList<>();
        reloadVehicles();
        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createBevelBorder(0));
        leftPanel.add(BorderLayout.NORTH, new JLabel("List of all vehicles:"));
        vehicleJList.setBorder(createTitledBorder(""));
        vehicleJList.setSelectionMode(SINGLE_SELECTION);
        leftPanel.add(BorderLayout.CENTER, new JScrollPane(vehicleJList));

        final JButton vehicleListButton = new JButton("Reload vehicles");
        leftPanel.add(BorderLayout.SOUTH, new JPanel().add(vehicleListButton).getParent());
        vehicleListButton.addActionListener(e -> reloadVehicles());

        final JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(createTitledBorder("Flight list"));
        flightTable = new FlightTablePanel();
        centerPanel.add(BorderLayout.CENTER, flightTable);

        this.add(BorderLayout.CENTER, new JSplitPane(HORIZONTAL_SPLIT, leftPanel, centerPanel));

        final JPanel bottomPanel = new JPanel(new BorderLayout());
        final JButton uploadTelemetryButton = new JButton("Upload");
        uploadTelemetryButton.setEnabled(false);
        uploadTelemetryButton.addActionListener(event -> uploadCurrentlySelectedFlights());

        final JPanel uploadButtonPanel = new JPanel();
        final TitledBorder bottomPanelBorder = createTitledBorder("1");
        bottomPanelBorder.setBorder(createEmptyBorder());
        bottomPanelBorder.setTitleColor(uploadButtonPanel.getBackground());
        uploadButtonPanel.setBorder(bottomPanelBorder);
        uploadButtonPanel.add(new JPanel().add(uploadTelemetryButton).getParent());
        bottomPanel.add(BorderLayout.EAST, uploadButtonPanel);

        this.add(BorderLayout.SOUTH, bottomPanel);

        final JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, X_AXIS));
        topPanel.add(logoutButton);
        topPanel.add(createGlue());
        topPanel.add(flightTable.createSelectAllButton());
        topPanel.setBorder(createCompoundBorder(createEtchedBorder(), createEmptyBorder(3, 3, 3, 3)));

        this.add(BorderLayout.NORTH, topPanel);

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

    public void addLogoutButtonActionListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    private void refreshView() {
        getSelectedVehicle().ifPresent(vehicle -> {
            updateFlightsTable(getSelectedStartTime(), getSelectedEndTime(), getSelectedTracksLimit(), vehicle);
            datesHighlighter.setCurrentVehicle(vehicle);
        });
    }

    @SneakyThrows
    private void updateFlightsTable(ZonedDateTime startTime, ZonedDateTime endTime, int tracksLimit, Vehicle vehicle) {
        final List<? extends Flight> flights =
                new VehicleTracksProcessor(startTime, endTime, tracksLimit, vehicle).getVehicleTracks();
        flightTable.updateModel(flights);
    }

    private Optional<Vehicle> getSelectedVehicle() {
        return Optional.ofNullable(vehicleMap.get(vehicleJList.getSelectedValue()));
    }

    private void reloadVehicles() {
        vehicleMap = sessionController().getVehicles().stream()
                .collect(toMap(this::getDisplayableName, v -> v));
        vehicleJList.setListData(vehicleMap.keySet().toArray(new String[0]));
    }

    private String getDisplayableName(Vehicle vehicle) {
        final String name = vehicle.getName().trim();
        final String serialNumber = vehicle.getSerialNumber().trim();

        if (name.contains(serialNumber)) {
            return name;
        }

        return format("{0}-{1}", name, serialNumber);
    }

    private ZonedDateTime getSelectedStartTime() {
        return datePicker.getSelectedStartTime();
    }

    private ZonedDateTime getSelectedEndTime() {
        return datePicker.getSelectedEndTime();
    }

    private int getSelectedTracksLimit() {
        return datePicker.getSelectedFlightsLimit();
    }

    private DroneLogbookResponse uploadFlightTelemetry(FlightTelemetry flightTelemetry) {
        final String url = settings().getUploadServerUrl();
        final Path pathForUploadedFiles = settings().getUploadedFlightsPath();
        return new LogbookUploader(url, settings().getUploadServerLogin(), settings().getUploadServerPassword())
                .uploadFlight(flightTelemetry)
                .storeFlightTelemetry(new CsvFileNameGenerator(pathForUploadedFiles, flightTelemetry).generateUnique())
                .getDroneLogbookResponse();
    }

    private static void saveTelemetry(FlightTelemetryProcessor flightTelemetryProcessor, Flight flight) {
        final Path telemetryFilePath =
                new CsvFileNameGenerator(settings().getTelemetryPath(), flight).generateUnique();
        saveTelemetryDataToCsvFile(telemetryFilePath,
                flightTelemetryProcessor.getProcessedTelemetry(),
                flightTelemetryProcessor.getAllFieldCodes());
    }

    private Future<Operation<Identity<?>, DroneLogbookResponse>> submitFlightForUploading(Flight flight) {
        return performerFactory().getUploadPerformer().submit(flight.getId(), () -> {
            final FlightTelemetryProcessor flightTelemetryProcessor = new FlightTelemetryProcessor(flight);

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

        final List<Future<Operation<Identity<?>, DroneLogbookResponse>>> uploadOperationFutures = selectedFlights.stream()
                .map(this::submitFlightForUploading)
                .collect(toList());

        final List<Operation<Identity<?>, DroneLogbookResponse>> uploadResults = waitWithProgressBarForm()
                .withMessageTemplate(" {0} of {1} flights uploaded to DroneLogbook ")
                .waitOnFutures(uploadOperationFutures, this);

        flightTable.repaint();

        UploadReportForm.showReport(this, uploadResults);
    }
}
