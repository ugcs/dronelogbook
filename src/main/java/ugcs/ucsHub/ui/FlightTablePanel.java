package ugcs.ucsHub.ui;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.TimeUnit;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Minute;
import ugcs.common.Action;
import ugcs.processing.Flight;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

class FlightTablePanel extends JPanel {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private final static String[] columnNames = {"Upload", "Flight start time", "Flight end time", "Flight duration"};

    private final JTable flightTable = new JTable();
    private final Component flightTablePane;

    private final JLabel noFlightsLabel = new JLabel("Select vehicle...");
    private final Component noFlightsLabelPane;

    private final List<Action> tableChangeListeners = new CopyOnWriteArrayList<>();

    private static class FlightTableModel extends AbstractTableModel {
        private static final long MILLIS_IN_DAY = 1000L * 60 * 60 * 24;
        private final List<MutablePair<? extends Flight, Boolean>> flightsAndSelection;
        private final boolean hideDate;

        FlightTableModel(List<? extends Flight> flights) {
            this.flightsAndSelection = flights.stream()
                    .map(flight -> MutablePair.of(flight, FALSE))
                    .collect(toList());

            hideDate = flights.stream().findAny()
                    .map(Flight::getStartEpochMilli)
                    .map(FlightTableModel::epochMilliToEpochDay)
                    .map(epochDay -> flights.stream()
                            .map(Flight::getStartEpochMilli)
                            .map(FlightTableModel::epochMilliToEpochDay)
                            .allMatch(epochDay::equals)
                    )
                    .orElse(FALSE);
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
            switch (columnIndex) {
                case 0:
                    return flightsAndSelection.get(rowIndex).getRight();
                case 1:
                    return flightEpochToString(getFlight(rowIndex).getStartEpochMilli());
                case 2:
                    return flightEpochToString(getFlight(rowIndex).getEndEpochMilli());
                case 3:
                    return formatFlightDuration(getFlight(rowIndex));
            }
            return null;
        }

        @Override
        public void setValueAt(Object boolValue, int rowIndex, int columnIndex) {
            if (boolValue instanceof Boolean) {
                final Boolean isSelected = (Boolean) boolValue;
                if (isSelected) {
                    clearSelection();
                }
                flightsAndSelection.get(rowIndex).setValue(isSelected);

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

        private String flightEpochToString(long epochMilli) {
            final Date flightDate = new Date(epochMilli);
            return hideDate ? TIME_FORMAT.format(flightDate) : DATE_TIME_FORMAT.format(flightDate);
        }

        private Flight getFlight(int rowIndex) {
            return flightsAndSelection.get(rowIndex).getLeft();
        }

        private static long epochMilliToEpochDay(long epochMilli) {
            return epochMilli / MILLIS_IN_DAY;
        }

        private static String formatFlightDuration(Flight flight) {
            final PrettyTime prettyTime = new PrettyTime(flight.getStartDate(), ENGLISH);
            final List<Duration> durations = prettyTime.calculatePreciseDuration(flight.getEndDate());
            return durations.stream()
                    .map(FlightTableModel::formatDuration)
                    .collect(joining(" "));
        }

        private static String formatDuration(Duration duration) {
            final Class<? extends TimeUnit> timeUnitClass = duration.getUnit().getClass();
            if (Minute.class.equals(timeUnitClass)) {
                return new PrettyTime(ENGLISH).formatDuration(duration) + " " + formatMillis(duration.getDelta());
            }

            if (JustNow.class.equals(timeUnitClass)) {
                return formatMillis(duration.getQuantity());
            }

            return new PrettyTime(ENGLISH).formatDuration(duration);
        }

        private static String formatMillis(long millis) {
            return millis / 1000L + " s";
        }
    }

    FlightTablePanel() {
        super(new BorderLayout());

        flightTablePane = new JScrollPane(flightTable);
        add(flightTablePane, BorderLayout.CENTER);
        flightTablePane.setVisible(true);

        noFlightsLabelPane = new JPanel(new GridBagLayout()).add(noFlightsLabel).getParent();
        add(noFlightsLabelPane, BorderLayout.NORTH);
    }

    void updateModel(List<? extends Flight> flights) {
        if (flights.isEmpty()) {
            noFlightsLabel.setText("No flights found for the selected date...");
            flightTablePane.setVisible(false);
            noFlightsLabelPane.setVisible(true);
            flightTable.setModel(new FlightTableModel(emptyList()));
        } else {
            flightTable.setModel(new FlightTableModel(flights));
            flightTablePane.setVisible(true);
            noFlightsLabelPane.setVisible(false);
            flightTable.getModel().addTableModelListener(this::tableChanged);
        }
        notifyAll(tableChangeListeners);
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
