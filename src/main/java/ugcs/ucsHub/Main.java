package ugcs.ucsHub;

import ugcs.exceptions.ExceptionsHandler;
import ugcs.exceptions.ExpectedException;
import ugcs.ucsHub.ui.LoginForm;
import ugcs.ucsHub.ui.VehicleListForm;
import ugcs.upload.logbook.MultipartUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static ugcs.net.SessionController.sessionController;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.WaitForm.waitForm;
import static ugcs.upload.logbook.FlightUploadPerformerFactory.performerFactory;

/**
 * Main entry point of the application
 */
public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("UCS Hub " + settings().getProductVersion());

        final Container contentPane = frame.getContentPane();

        final LoginForm loginForm = new LoginForm(frame);
        contentPane.add(BorderLayout.CENTER, loginForm);

        frame.setIconImage(settings().getLogoIcon().getImage());

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        ExceptionsHandler.handler().addUncaughtExceptionListener(rootException ->
                showMessageDialog(frame, getExceptionMessage(rootException), "Error", JOptionPane.ERROR_MESSAGE));

        loginForm.makeLoginButtonDefault();
        loginForm.addLoginButtonListener(event -> {
            settings().storeUcsServerLogin(loginForm.getLogin());
            settings().storeUcsServerPassword(loginForm.getPassword());
            settings().storeUploadServerLogin(loginForm.getDlbLogin());
            settings().storeUploadServerPassword(loginForm.getDlbPassword());

            sessionController().updateSettings(settings());

            frame.addWindowListener(new ActionOnCloseWindowAdapter(() -> {
                sessionController().close();
                performerFactory().shutDown();
            }));

            waitForm().waitOnAction("Connecting to UgCS...", sessionController()::connect, loginForm);

            waitForm().waitOnCallable("Connecting to LogBook...", () ->
                            new MultipartUtility(settings().getUploadServerUrl())
                                    .withCredentials(settings().getUploadServerLogin(), settings().getUploadServerPassword())
                                    .authorizationTestOnly()
                                    .performRequest(),
                    loginForm)
                    .assertAuthorizationSucceed();

            final VehicleListForm vehicleForm = new VehicleListForm();
            contentPane.remove(loginForm);
            contentPane.add(vehicleForm);
            frame.setSize(750, 600);
            frame.setLocationRelativeTo(null);

            vehicleForm.addLogoutButtonActionListener(
                    getLogoutActionListener(frame, loginForm, vehicleForm)
            );
        });

    }

    private static String getExceptionMessage(Throwable ex) {
        if (ex instanceof ExpectedException) {
            return ex.getMessage();
        }
        return "Unknown error: " + ex.getMessage() + "\nTry to restart application.";
    }

    private static ActionListener getLogoutActionListener(JFrame mainFrame, LoginForm loginForm, VehicleListForm currentVehicleForm) {
        return event -> {
            final int dialogResult =
                    showConfirmDialog(mainFrame, "Do you want to logout from the application?", "Logout", OK_CANCEL_OPTION);

            if (dialogResult == OK_OPTION) {
                sessionController().close();

                final Container contentPane = mainFrame.getContentPane();
                contentPane.remove(currentVehicleForm);
                contentPane.add(loginForm);

                mainFrame.pack();
                mainFrame.setLocationRelativeTo(null);
            }
        };
    }
}