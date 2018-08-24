package ugcs.ucsHub;

import javax.swing.*;
import java.awt.*;

import static ugcs.ucsHub.Settings.settings;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("UCS Hub");

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
            sessionController.connect();

            frame.addWindowListener(new ActionOnCloseWindowAdapter(sessionController::close));

            final VehicleListForm vehicleForm = new VehicleListForm(sessionController);
            contentPane.remove(loginForm);
            contentPane.add(vehicleForm);
            frame.setSize(600, 500);
        });
    }
}