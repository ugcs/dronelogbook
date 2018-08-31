package ugcs.ucsHub;

import ugcs.net.SessionController;
import ugcs.ucsHub.forms.LoginForm;
import ugcs.ucsHub.forms.VehicleListForm;
import ugcs.upload.logbook.LogBookUploader;

import javax.swing.*;
import java.awt.*;

import static ugcs.ucsHub.Settings.settings;
import static ugcs.ucsHub.forms.WaitForm.waitForm;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("UCS Hub " + Main.class.getPackage().getImplementationVersion());

        final Container contentPane = frame.getContentPane();

        final LoginForm loginForm = new LoginForm(frame);
        contentPane.add(BorderLayout.CENTER, loginForm);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        });

        loginForm.makeLoginButtonDefault();
        loginForm.addLoginButtonListener(event -> {
            final SessionController sessionController =
                    new SessionController(settings().getHost(), settings().getPort(), loginForm.getLogin(), loginForm.getPassword());

            waitForm().waitOnAction("Connecting to UgCS...", sessionController::connect, loginForm);

            final LogBookUploader logBookUploader =
                    new LogBookUploader(settings().getUploadServerUrl(), loginForm.getDlbLogin(), loginForm.getDlbPassword());

            frame.addWindowListener(new ActionOnCloseWindowAdapter(sessionController::close));

            final VehicleListForm vehicleForm = new VehicleListForm(sessionController, logBookUploader);
            contentPane.remove(loginForm);
            contentPane.add(vehicleForm);
            frame.setSize(600, 500);
        });
    }
}