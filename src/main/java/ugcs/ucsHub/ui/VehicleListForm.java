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
import ugcs.ucsHub.ui.thirdparty.JSplitButton;
import ugcs.ucsHub.ui.thirdparty.SplitButtonActionListener;
import ugcs.upload.logbook.DroneLogBookResponse;
import ugcs.upload.logbook.LogBookUploader;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
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
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;
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

    private final JMenuItem logoutMenuItem = new JMenuItem("Logout");

    public VehicleListForm() {
        super(new BorderLayout());

        vehicleJList = new JList<>();
        reloadVehicles();
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createBevelBorder(0));
        leftPanel.add(BorderLayout.NORTH, new JLabel("List of all vehicles:"));
        vehicleJList.setBorder(createTitledBorder(""));
        vehicleJList.setSelectionMode(SINGLE_SELECTION);
        leftPanel.add(BorderLayout.CENTER, new JScrollPane(vehicleJList));
        this.add(BorderLayout.WEST, leftPanel);

        JSplitButton vehicleListButton = new JSplitButton("Reload vehicles");
        leftPanel.add(BorderLayout.SOUTH, new JPanel().add(vehicleListButton).getParent());
        vehicleListButton.addSplitButtonActionListener(new SplitButtonActionListener() {
            @Override
            public void buttonClicked(ActionEvent e) {
                reloadVehicles();
            }

            @Override
            public void splitButtonClicked(ActionEvent e) {
            }
        });

        final JPopupMenu vehicleListPopupMenu = new JPopupMenu();
        vehicleListPopupMenu.add(logoutMenuItem);
        vehicleListButton.setPopupMenu(vehicleListPopupMenu);
        vehicleJList.setComponentPopupMenu(vehicleListPopupMenu);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(createTitledBorder("Flight list"));
        flightTable = new FlightTablePanel();
        centerPanel.add(BorderLayout.CENTER, flightTable);
        this.add(BorderLayout.CENTER, centerPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
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
        logoutMenuItem.addActionListener(listener);
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
                .collect(toMap(v -> format("{0}-{1}", v.getName(), v.getSerialNumber()), v -> v));
        vehicleJList.setListData(vehicleMap.keySet().toArray(new String[0]));
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

    private DroneLogBookResponse uploadFlightTelemetry(FlightTelemetry flightTelemetry) {
        final String url = settings().getUploadServerUrl();
        final Path pathForUploadedFiles = settings().getUploadedFlightsPath();
        return new LogBookUploader(url, settings().getUploadServerLogin(), settings().getUploadServerPassword())
                .uploadFlight(flightTelemetry)
                .storeFlightTelemetry(new CsvFileNameGenerator(pathForUploadedFiles, flightTelemetry).generateUnique())
                .getDroneLogBookResponse();
    }

    private static void saveTelemetry(FlightTelemetryProcessor flightTelemetryProcessor, Flight flight) {
        final Path telemetryFilePath =
                new CsvFileNameGenerator(settings().getTelemetryPath(), flight).generateUnique();
        saveTelemetryDataToCsvFile(telemetryFilePath,
                flightTelemetryProcessor.getProcessedTelemetry(),
                flightTelemetryProcessor.getAllFieldCodes());
    }

    private Future<Operation<Identity<?>, DroneLogBookResponse>> submitFlightForUploading(Flight flight) {
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

        final List<Future<Operation<Identity<?>, DroneLogBookResponse>>> uploadOperationFutures = selectedFlights.stream()
                .map(this::submitFlightForUploading)
                .collect(toList());

        final List<Operation<Identity<?>, DroneLogBookResponse>> uploadResults = waitWithProgressBarForm()
                .withMessageTemplate(" {0} of {1} flights uploaded to DroneLogBook ")
                .waitOnFutures(uploadOperationFutures, this);

        UploadReportForm.showReport(this, uploadResults);
    }
}
