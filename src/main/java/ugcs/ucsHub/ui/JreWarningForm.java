package ugcs.ucsHub.ui;

import ugcs.ucsHub.ui.components.JHyperlink;

import javax.swing.*;
import java.awt.*;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static ugcs.ucsHub.Settings.settings;

/**
 * The panel is displayed when application is running on 32bit JVM.
 */
class JreWarningForm extends JPanel {

    JreWarningForm() {
        super(new BorderLayout());

        this.setBackground(new Color(0xffffce));

        final JPanel detailInfoPanel = new JPanel();
        detailInfoPanel.setLayout(new BoxLayout(detailInfoPanel, BoxLayout.Y_AXIS));
        detailInfoPanel.add(centered("Seems that you have installed a 32-bit version of Java."));
        detailInfoPanel.add(centered("The program can still work but you can experience problems with uploading large flights."));
        final JHyperlink downloadLink = new JHyperlink("https://www.java.com/ru/download/manual.jsp", "Java download page");
        detailInfoPanel.add(textWithLink("Please proceed to ", downloadLink, " and check a 64-bit installation for your system."));

        final JHyperlink warningLabel = new JHyperlink(
                () -> showWarning(this, detailInfoPanel), "", "WARNING: you are running 32-bit Java"
        );
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.add(warningLabel, BorderLayout.CENTER);
    }

    private void showWarning(Component parent, Component content) {
        showMessageDialog(parent, content, "Warning details", PLAIN_MESSAGE, settings().getWarningIcon());
    }

    private Component centered(String text) {
        final JPanel panel = new JPanel();
        panel.add(new JLabel(text));
        return panel;
    }

    private Component textWithLink(String textBefore, JHyperlink link, String textAfter) {
        final JPanel panel = new JPanel();

        panel.add(new JLabel(textBefore));
        panel.add(link);
        panel.add(new JLabel(textAfter));

        return panel;
    }
}
