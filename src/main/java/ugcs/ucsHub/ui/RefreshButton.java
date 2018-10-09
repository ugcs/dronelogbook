package ugcs.ucsHub.ui;

import lombok.SneakyThrows;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

import static javax.swing.SwingUtilities.invokeLater;
import static ugcs.ucsHub.ui.WaitForm.waitForm;

/**
 * Service supplying button for calling application refreshment
 */
class RefreshButton {
    private List<RefreshListener> listeners = new LinkedList<>();

    @FunctionalInterface
    interface RefreshListener {
        void refreshPerformed() throws Exception;
    }

    private static volatile RefreshButton instance;

    static RefreshButton refresher() {
        if (instance == null) {
            synchronized (RefreshButton.class) {
                if (instance == null) {
                    instance = new RefreshButton();
                }
            }
        }
        return instance;
    }

    private RefreshButton() {
    }

    JButton createButton() {
        final JButton button = new JButton("Refresh...");
        button.addActionListener(e -> invokeLater(
                () -> waitForm().waitOnAction("Application refresh...", this::refresh, button)
        ));
        return button;
    }

    void addRefreshListener(RefreshListener listener) {
        listeners.add(listener);
    }

    @SneakyThrows
    private void refresh() {
        for (RefreshListener listener : listeners) {
            listener.refreshPerformed();
        }
    }
}
