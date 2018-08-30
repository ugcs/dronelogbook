package ugcs.ucsHub.forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import static ugcs.ucsHub.Settings.settings;

public class LoginForm extends JPanel {
    private final JTextField loginField = new JTextField(settings().getUcsServerLoginLogin(), 25);
    private final JTextField passwordField = new JPasswordField(settings().getUcsServerPassword(), 25);

    private final JTextField loginDlbField = new JTextField(settings().getUploadServerLogin(), 25);
    private final JTextField passwordDlbField = new JPasswordField(settings().getUploadServerPassword(), 25);

    private final JButton loginButton = new JButton("Login");

    public LoginForm(JFrame parentFrame) {
        super(new BorderLayout());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        this.add(center, BorderLayout.CENTER);

        final JPanel loginPanel = new JPanel();
        loginPanel.add(new JLabel("UCS login:                  "));
        loginPanel.add(loginField);
        center.add(loginPanel);

        final JPanel passwordPanel = new JPanel();
        passwordPanel.add(new JLabel("UCS Password:       "));
        passwordPanel.add(passwordField);
        center.add(passwordPanel);

        final JPanel loginDlbPanel = new JPanel();
        loginDlbPanel.add(new JLabel("LogBook login:          "));
        loginDlbPanel.add(loginDlbField);
        center.add(loginDlbPanel);

        final JPanel passwordDlbPanel = new JPanel();
        passwordDlbPanel.add(new JLabel("LogBook password:"));
        passwordDlbPanel.add(passwordDlbField);
        center.add(passwordDlbPanel);

        final JPanel bottomPanel = new JPanel();

        bottomPanel.add(loginButton);

        final JButton exitButton = new JButton("Exit");
        bottomPanel.add(exitButton);
        exitButton.addActionListener(event -> parentFrame.dispose());

        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    public void addLoginButtonListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public String getLogin() {
        return loginField.getText();
    }

    public String getDlbLogin() {
        return loginDlbField.getText();
    }

    public String getDlbPassword() {
        return passwordDlbField.getText();
    }


    public char[] getPassword() {
        return passwordField.getText().toCharArray();
    }

    public void makeLoginButtonDefault() {
        SwingUtilities.getRootPane(loginButton).setDefaultButton(loginButton);
    }
}
