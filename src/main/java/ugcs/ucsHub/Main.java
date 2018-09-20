package ugcs.ucsHub;

import ugcs.exceptions.ExpectedException;
import ugcs.net.SessionController;
import ugcs.ucsHub.ui.LoginForm;
import ugcs.ucsHub.ui.VehicleListForm;
import ugcs.upload.logbook.LogBookUploader;

import javax.swing.*;
import java.awt.*;

import static java.util.Objects.isNull;
import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.ui.WaitForm.waitForm;
import static ugcs.upload.logbook.FlightUploadPerformerFactory.performerFactory;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("UCS Hub " + settings().getProductVersion());

        final Container contentPane = frame.getContentPane();

        final LoginForm loginForm = new LoginForm(frame);
        contentPane.add(BorderLayout.CENTER, loginForm);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            ex.printStackTrace();

            Throwable rootException = getRootException(ex);

            if (rootException instanceof ExpectedException) {
                System.err.println("Cause of ExpectedException:");
                final Throwable attachedCause = ((ExpectedException) rootException).getAttachedCause();
                if (!isNull(attachedCause)) {
                    attachedCause.printStackTrace();
                }
            }

            JOptionPane.showMessageDialog(null, getExceptionMessage(rootException), "Error", JOptionPane.ERROR_MESSAGE);
        });

        loginForm.makeLoginButtonDefault();
        loginForm.addLoginButtonListener(event -> {
            settings().storeUcsServerLogin(loginForm.getLogin());
            settings().storeUploadServerLogin(loginForm.getDlbLogin());
            settings().storeUploadServerPassword(loginForm.getDlbPassword());

            final SessionController sessionController =
                    new SessionController(settings().getHost(), settings().getPort(), loginForm.getLogin(), loginForm.getPassword());

            frame.addWindowListener(new ActionOnCloseWindowAdapter(() -> {
                sessionController.close();
                performerFactory().getPerformer().shutDown();
            }));

            waitForm().waitOnAction("Connecting to UgCS...", sessionController::connect, loginForm);

            final LogBookUploader logBookUploader =
                    new LogBookUploader(settings().getUploadServerUrl(), loginForm.getDlbLogin(), loginForm.getDlbPassword());

            final VehicleListForm vehicleForm = new VehicleListForm(sessionController, logBookUploader);
            contentPane.remove(loginForm);
            contentPane.add(vehicleForm);
            frame.setSize(750, 600);
            frame.setLocationRelativeTo(null);
        });

    }

    private static String getExceptionMessage(Throwable ex) {
        if (ex instanceof ExpectedException) {
            return ex.getMessage();
        }
        return "Unknown error: " + ex.getMessage() + "\nTry to restart application.";
    }

    private static Throwable getRootException(Throwable ex) {
        Throwable rootException = ex;
        while (rootException.getCause() != null) {
            rootException = rootException.getCause();
        }
        return rootException;
    }
}