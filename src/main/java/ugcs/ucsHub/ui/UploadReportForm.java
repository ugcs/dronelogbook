package ugcs.ucsHub.ui;

import ugcs.common.identity.Identity;
import ugcs.common.operation.Operation;
import ugcs.exceptions.logbook.LogBookAuthorizationFailed;
import ugcs.ucsHub.ui.components.JHyperlink;
import ugcs.upload.logbook.DroneLogBookResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static ugcs.ucsHub.Settings.settings;

/**
 * Representation for results of flights upload {@link Operation}
 */
final class UploadReportForm extends JPanel {
    private static Color SUCCESS_COLOR = Color.getHSBColor(0.269f, 0.1f, 1.0f);
    private static Color WARNING_COLOR = Color.getHSBColor(0.147f, 0.14f, 1.0f);

    private final Collection<Operation<Identity<?>, DroneLogBookResponse>> uploadResponses;

    static void showReport(Component parentComponent, List<Operation<Identity<?>, DroneLogBookResponse>> uploadResponses) {
        final UploadReportForm reportForm = new UploadReportForm(uploadResponses);
        showMessageDialog(parentComponent, reportForm, "Upload result", reportForm.getMessageType());
    }

    private UploadReportForm(Collection<Operation<Identity<?>, DroneLogBookResponse>> uploadResponses) {
        this.uploadResponses = uploadResponses;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (isLogBookAuthorizationError()) {
            formAuthorizationFailedReport();
        } else {
            formReport();
        }
    }

    private boolean isLogBookAuthorizationError() {
        final long authorizationErrorCount = uploadResponses.stream()
                .map(Operation::getError)
                .filter(Optional::isPresent)
                .filter(error -> error.get() instanceof LogBookAuthorizationFailed)
                .count();

        return authorizationErrorCount > 0;
    }

    private int getMessageType() {
        final long errorsCount = uploadResponses.stream()
                .map(Operation::getError)
                .filter(Optional::isPresent)
                .count();

        final long warningCount = uploadResponses.stream()
                .map(Operation::getResult)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(DroneLogBookResponse::isWarning)
                .count();

        if (errorsCount > 0) {
            return ERROR_MESSAGE;
        }

        if (warningCount > 0) {
            return WARNING_MESSAGE;
        }

        return INFORMATION_MESSAGE;
    }

    private void formAuthorizationFailedReport() {
        final JPanel reportRow = new JPanel();

        reportRow.add(new JLabel("LogBook authorization failed."));

        final JLabel changeCredentialsLabel =
                new JLabel(format("<html><a href=\\\"_\\\">%s</a></html>", "click to change credentials"));
        changeCredentialsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeCredentialsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showLogBookAuthorizationForm();
            }
        });
        reportRow.add(changeCredentialsLabel);

        this.add(reportRow);
    }

    private void showLogBookAuthorizationForm() {
        final JPanel mainPanel = new JPanel(new GridLayout(0,1));

        final JTextField loginDlbField = new JTextField(settings().getUploadServerLogin(), 25);
        mainPanel.add(new JLabel("LogBook login:"));
        mainPanel.add(new JPanel().add(loginDlbField).getParent());

        final JTextField passwordDlbField = new JPasswordField(settings().getUploadServerPassword(), 25);
        mainPanel.add(new JLabel("LogBook password:"));
        mainPanel.add(new JPanel().add(passwordDlbField).getParent());

        final int dialogResult = showConfirmDialog(this, mainPanel, "LogBook credentials", OK_CANCEL_OPTION);
        if (dialogResult == OK_OPTION) {
            settings().storeUploadServerLogin(loginDlbField.getText());
            settings().storeUploadServerPassword(passwordDlbField.getText());
        }
    }

    private void formReport() {
        uploadResponses.forEach(operationResult -> {
            final JPanel reportRow = new JPanel();

            final String identityTextRepresentation = operationResult.getId().toString();
            final String statusString = operationStatusString(operationResult);
            reportRow.add(new JLabel(format("%s - %s:", identityTextRepresentation, statusString)));

            operationResult.getResult()
                    .ifPresent(uploadResponse -> formReportRow(uploadResponse, reportRow));

            operationResult.getError().ifPresent(error -> formReportRow(error, reportRow));

            this.add(reportRow);
        });
    }

    private void formReportRow(DroneLogBookResponse droneLogBookResponse, JPanel rowContainer) {
        final String description = droneLogBookResponse.getDescription().orElse("No description.");
        rowContainer.add(new JLabel(description));
        droneLogBookResponse.getUrl().ifPresent(url -> rowContainer.add(new JHyperlink(url, "click to view on LogBook")));
        rowContainer.setBackground(getResponseColor(droneLogBookResponse));
    }

    private void formReportRow(Throwable error, JPanel rowContainer) {
        rowContainer.add(new JLabel(error.getMessage()));
    }

    private Color getResponseColor(DroneLogBookResponse droneLogBookResponse) {
        if (droneLogBookResponse.isUploadSucceed()) {
            return SUCCESS_COLOR;
        }

        return WARNING_COLOR;
    }

    private String operationStatusString(Operation<Identity<?>, DroneLogBookResponse> operationResult) {
        return operationResult.getResult().map(uploadResponse -> {
            if (uploadResponse.isUploadSucceed()) {
                return "uploaded";
            }
            if (uploadResponse.isWarning()) {
                return "upload rejected";
            }
            return "upload error";
        }).orElse("upload error");
    }
}