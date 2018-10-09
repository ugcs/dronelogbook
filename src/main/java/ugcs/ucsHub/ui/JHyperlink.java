package ugcs.ucsHub.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * {@link JLabel} with clickable hyperlink
 */
class JHyperlink extends JLabel {

    JHyperlink(String hyperlink, String text) {
        super(String.format("<html><a href=\\\"%s\\\">%s</a></html>", hyperlink, text));

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI(hyperlink));
                    } catch (IOException | URISyntaxException ignored) {
                    }
                }
            }
        });
    }

}
