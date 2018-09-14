package ugcs.ucsHub.ui;

import org.apache.commons.lang3.tuple.Pair;
import ugcs.common.operation.Operation;
import ugcs.processing.Flight;
import ugcs.upload.logbook.FlightUploadResponse;
import ugcs.upload.logbook.UploadResponse;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static ugcs.ucsHub.ui.util.PresentationUtil.periodToString;

final class UploadReportForm extends JPanel {
    private static Color SUCCESS_COLOR = Color.getHSBColor(0.269f, 0.1f, 1.0f);
    private static Color WARNING_COLOR = Color.getHSBColor(0.147f, 0.14f, 1.0f);
    private static Color ERROR_COLOR = Color.PINK;

    static void showReport(Component parentComponent, List<Operation<Flight, FlightUploadResponse>> uploadResponses) {
        final UploadReportForm reportForm = new UploadReportForm(uploadResponses);
        JOptionPane.showMessageDialog(parentComponent, reportForm, "Upload result", INFORMATION_MESSAGE);
    }

    private UploadReportForm(List<Operation<Flight, FlightUploadResponse>> uploadResponses) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        IntStream.range(0, uploadResponses.size())
                .mapToObj(i -> Pair.of(i, uploadResponses.get(i)))
                .forEach(pair -> {
                    final Operation<Flight, FlightUploadResponse> operationResult = pair.getValue();

                    final JPanel reportRow = new JPanel();

                    final Flight flight = operationResult.getParam();
                    final String flightDurationString = periodToString(flight.getStartDate(), flight.getEndDate());
                    final String statusString = operationStatusString(operationResult);
                    reportRow.add(new JLabel(format("%s flight, %s:", flightDurationString, statusString)));

                    operationResult.getResult().ifPresent(flightUploadResponse -> {
                        final UploadResponse uploadResponse = flightUploadResponse.getUploadResponse();
                        final String description = uploadResponse.getDescription().orElse("No description.");
                        reportRow.add(new JLabel(description));
                        uploadResponse.getUrl().ifPresent(url -> reportRow.add(new JHyperlink(url, "click to view on LogBook")));
                        reportRow.setBackground(getResponseColor(uploadResponse));
                    });

                    operationResult.getError().ifPresent(error -> {
                        reportRow.add(new JLabel(error.getMessage()));
                        reportRow.setBackground(ERROR_COLOR);
                    });

                    this.add(reportRow);

                });
    }

    private Color getResponseColor(UploadResponse uploadResponse) {
        if (uploadResponse.isSuccess()) {
            return SUCCESS_COLOR;
        }

        return WARNING_COLOR;
    }

    private String operationStatusString(Operation<Flight, FlightUploadResponse> operationResult) {
        return operationResult.getResult().map(flightUploadResponse -> {
            final UploadResponse uploadResponse = flightUploadResponse.getUploadResponse();
            if (uploadResponse.isSuccess()) {
                return "uploaded";
            }
            if (uploadResponse.isWarning()) {
                return "upload rejected";
            }
            return "upload error";
        }).orElse("upload error");
    }
}