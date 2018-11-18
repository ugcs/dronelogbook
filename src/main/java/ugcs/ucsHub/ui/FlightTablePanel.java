package ugcs.ucsHub.ui;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.common.Action;
import ugcs.exceptions.ugcs.UgcsDisconnectedException;
import ugcs.processing.Flight;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;
import static javax.swing.SwingUtilities.invokeLater;
import static ugcs.exceptions.ExceptionsHandler.handler;
import static ugcs.ucsHub.ui.RefreshButton.refresher;
import static ugcs.ucsHub.ui.util.PresentationUtil.periodToString;

/**
 * Part of {@link VehicleListForm} representing current list of {@link Flight}s
 */
class FlightTablePanel extends JPanel {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private final static String[] columnNames = {"Upload", "Date", "Start time", "End time", "Duration"};

    private final JTable flightTable = new JTable();
    private final Component flightTablePane;

    private final JLabel messageLabel = new JLabel();
    private final JPanel messageLabelPane;

    private final List<Action> tableChangeListeners = new CopyOnWriteArrayList<>();

    private final JLabel errorIconLabel = new JLabel((Icon) UIManager.get("OptionPane.errorIcon"));

    private final JButton refreshButton;

    private static class FlightTableModel extends AbstractTableModel {
        private final List<MutablePair<? extends Flight, Boolean>> flightsAndSelection;

        FlightTableModel(List<? extends Flight> flights) {
            this.flightsAndSelection = flights.stream()
                    .map(flight -> MutablePair.of(flight, FALSE))
                    .collect(toList());
        }

        @Override
        public int getRowCount() {
            return flightsAndSelection.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final Flight flight = getFlight(rowIndex);
            switch (columnIndex) {
                case 0:
                    return flightsAndSelection.get(rowIndex).getRight();
                case 1:
                    return flightEpochToDateString(flight.getStartEpochMilli());
                case 2:
                    return flightEpochToTimeString(flight.getStartEpochMilli());
                case 3:
                    return flightEpochToTimeString(flight.getEndEpochMilli());
                case 4:
                    return formatFlightDuration(flight);
            }
            return null;
        }

        @Override
        public void setValueAt(Object boolValue, int rowIndex, int columnIndex) {
            if (boolValue instanceof Boolean) {
                flightsAndSelection.get(rowIndex).setValue((Boolean) boolValue);

                fireTableDataChanged();
            }
        }

        private void clearSelection() {
            flightsAndSelection.forEach(pair -> pair.setValue(false));
        }

        Set<Flight> getSelectedFlights() {
            return flightsAndSelection.stream()
                    .filter(Pair::getRight)
                    .map(Pair::getLeft)
                    .collect(toSet());
        }

        private String flightEpochToTimeString(long epochMilli) {
            return formatEpoch(epochMilli, TIME_FORMAT);
        }

        private String flightEpochToDateString(long epochMilli) {
            return formatEpoch(epochMilli, DATE_FORMAT);
        }

        private static String formatEpoch(long epochMilli, DateFormat format) {
            return format.format(new Date(epochMilli));
        }

        private Flight getFlight(int rowIndex) {
            return flightsAndSelection.get(rowIndex).getLeft();
        }

        private static String formatFlightDuration(Flight flight) {
            return periodToString(flight.getStartDate(), flight.getEndDate());
        }
    }

    FlightTablePanel() {
        super(new BorderLayout());

        flightTablePane = new JScrollPane(flightTable);
        add(flightTablePane, BorderLayout.CENTER);
        flightTablePane.setVisible(true);

        refreshButton = refresher().createButton();
        refreshButton.addActionListener(e -> updateMessageOnlyView("Updating flight's table..."));

        messageLabelPane = new JPanel();
        messageLabelPane.setLayout(new BoxLayout(messageLabelPane, BoxLayout.Y_AXIS));
        messageLabelPane.add(new JPanel().add(errorIconLabel).getParent());
        messageLabelPane.add(new JPanel().add(messageLabel).getParent());
        messageLabelPane.add(new JPanel().add(refreshButton).getParent());
        add(messageLabelPane, BorderLayout.NORTH);

        handler().addExceptionListener(UgcsDisconnectedException.class,
                ex -> invokeLater(() -> updateExceptionView(ex)));

        updateMessageOnlyView("Select vehicle...");
    }

    void updateModel(List<? extends Flight> flights) {
        if (flights.isEmpty()) {
            updateMessageOnlyView("No flights found for the selected options...");
        } else {
            flightTable.setModel(new FlightTableModel(flights));
            flightTablePane.setVisible(true);
            messageLabelPane.setVisible(false);
            flightTable.getModel().addTableModelListener(this::tableChanged);
            flightTable.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
            flightTable.getColumn("Upload").setMaxWidth(70);
        }
        notifyAll(tableChangeListeners);
    }

    private void updateExceptionView(Throwable ex) {
        errorIconLabel.setVisible(true);
        refreshButton.setVisible(true);
        updateMessageView(ex.getMessage());
    }

    private void updateMessageOnlyView(String messageText) {
        errorIconLabel.setVisible(false);
        refreshButton.setVisible(false);
        updateMessageView(messageText);
    }

    private void updateMessageView(String messageText) {
        messageLabel.setText(messageText);
        flightTablePane.setVisible(false);
        messageLabelPane.setVisible(true);
        flightTable.setModel(new FlightTableModel(emptyList()));
    }

    void addTableChangeAction(Action action) {
        tableChangeListeners.add(action);
    }

    Set<Flight> getSelectedFlights() {
        return flightTable.getModel() instanceof FlightTableModel
                ? ((FlightTableModel) flightTable.getModel()).getSelectedFlights()
                : emptySet();
    }

    private void tableChanged(TableModelEvent e) {
        notifyAll(tableChangeListeners);
    }

    private static void notifyAll(List<Action> actions) {
        actions.forEach(action -> {
            try {
                action.run();
            } catch (Exception toRethrow) {
                throw new RuntimeException(toRethrow);
            }
        });
    }
}
