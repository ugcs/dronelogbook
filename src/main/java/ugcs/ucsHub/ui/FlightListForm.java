package ugcs.ucsHub.ui;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ugcs.processing.telemetry.FlightTelemetry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.util.stream.Collectors.toList;

@Deprecated
class FlightListForm extends JDialog {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private final static String[] columnNames = {"Upload", "Flight start time", "Flight end time"};

    private final List<MutablePair<FlightTelemetry, Boolean>> flightsAndSelection;

    FlightListForm(List<FlightTelemetry> flights, String vehicleName) throws HeadlessException {
        super((JFrame) null, true);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.setTitle("Choose flight of '" + vehicleName + "' to upload");

        this.flightsAndSelection = flights.stream().map(flight -> MutablePair.of(flight, FALSE)).collect(toList());

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel bottomPanel = new JPanel();

        JButton uploadBtn = new JButton("Upload");
        uploadBtn.setEnabled(false);
        JButton cancelBtn = new JButton("Cancel");

        uploadBtn.addActionListener(e -> setVisible(false));
        cancelBtn.addActionListener(e -> {
            flightsAndSelection.forEach(pair -> pair.setValue(false));
            setVisible(false);
        });

        bottomPanel.add(uploadBtn);
        bottomPanel.add(cancelBtn);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        JTable flightsTable = new JTable();
        flightsTable.setModel(new AbstractTableModel() {
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
                        return epochToString(flightsAndSelection.get(rowIndex).getLeft().getFlightStartEpochMilli());
                    case 2:
                        return epochToString(flightsAndSelection.get(rowIndex).getLeft().getFlightEndEpochMilli());
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

                    uploadBtn.setEnabled(!getSelectedFlights().isEmpty());
                    fireTableDataChanged();
                }
            }

            private void clearSelection() {
                flightsAndSelection.forEach(pair -> pair.setValue(false));
            }
        });
        mainPanel.add(new JScrollPane(flightsTable), BorderLayout.CENTER);
        add(mainPanel);
        pack();
    }

    List<FlightTelemetry> getSelectedFlights() {
        return flightsAndSelection.stream()
                .filter(Pair::getRight)
                .map(Pair::getLeft).collect(toList());
    }

    private static String epochToString(long epochMilli) {
        return DATE_FORMAT.format(new Date(epochMilli));
    }
}
