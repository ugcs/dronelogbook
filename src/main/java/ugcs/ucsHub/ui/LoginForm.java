package ugcs.ucsHub.ui;

import ugcs.ucsHub.ui.components.JPasswordFieldEx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.WEST;
import static java.awt.FlowLayout.LEFT;
import static java.awt.FlowLayout.RIGHT;
import static ugcs.ucsHub.Settings.settings;

/**
 * Login form for UCS and LogBook services
 */
public class LoginForm extends JPanel {
    private final JTextField loginField = new JTextField(settings().getUcsServerLogin(), 25);
    private final JPasswordFieldEx passwordField = new JPasswordFieldEx(settings().getUcsServerPassword(), 25);

    private final JTextField loginDlbField = new JTextField(settings().getUploadServerLogin(), 25);
    private final JPasswordFieldEx passwordDlbField = new JPasswordFieldEx(settings().getUploadServerPassword(), 25);

    private final JButton loginButton = new JButton("Login");

    public LoginForm(JFrame parentFrame) {
        super(new BorderLayout());

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        this.add(alignCenter(centerPanel), BorderLayout.CENTER);

        final JPanel loginPanel = new JPanel(new BorderLayout());
        loginPanel.add(alignLeft(new JLabel("UCS login:")), WEST);
        loginPanel.add(alignRight(loginField), CENTER);
        centerPanel.add(loginPanel);

        final JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.add(alignLeft(new JLabel("UCS Password:")), WEST);
        passwordPanel.add(alignRight(passwordField), CENTER);
        centerPanel.add(passwordPanel);

        final JPanel loginDlbPanel = new JPanel(new BorderLayout());
        loginDlbPanel.add(alignLeft(new JLabel("LogBook login:")), WEST);
        loginDlbPanel.add(alignRight(loginDlbField), CENTER);
        centerPanel.add(loginDlbPanel);

        final JPanel passwordDlbPanel = new JPanel(new BorderLayout());
        passwordDlbPanel.add(alignLeft(new JLabel("LogBook password:")), WEST);
        passwordDlbPanel.add(alignRight(passwordDlbField), CENTER);
        centerPanel.add(passwordDlbPanel);

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
        return passwordDlbField.getCurrentPassword();
    }


    public String getPassword() {
        return passwordField.getCurrentPassword();
    }

    public void makeLoginButtonDefault() {
        SwingUtilities.getRootPane(loginButton).setDefaultButton(loginButton);
    }

    private static Container alignCenter(Component component) {
        return new JPanel(new GridBagLayout()).add(component).getParent();
    }

    private static Container alignRight(Component component) {
        return new JPanel(new FlowLayout(RIGHT)).add(component).getParent();
    }

    private static Container alignLeft(Component component) {
        return new JPanel(new FlowLayout(LEFT)).add(component).getParent();
    }
}
