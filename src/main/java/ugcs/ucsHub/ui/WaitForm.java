package ugcs.ucsHub.ui;

import lombok.SneakyThrows;
import ugcs.common.Action;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ugcs.ucsHub.Settings.settings;

/**
 * Modal window appearing for long running blocking operations
 */
public class WaitForm extends JDialog {

    private final JLabel messageLabel;

    private WaitForm() {
        super((JFrame) null, true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("Please wait...");
        this.messageLabel = new JLabel("Loading", settings().getLoadingIcon(), SwingConstants.RIGHT);
        add(new JPanel().add(messageLabel).getParent());

        setIconImage(settings().getLogoIcon().getImage());

        pack();
    }

    private static volatile WaitForm instance;

    public static WaitForm waitForm() {
        if (instance == null) {
            synchronized (WaitForm.class) {
                if (instance == null) {
                    instance = new WaitForm();
                }
            }
        }
        return instance;
    }

    public void waitOnAction(String message, Action action, Component parentOrNull) {
        waitOnCallable(message, () -> {
                    action.run();
                    return 0;
                }, parentOrNull
        );
    }

    @SneakyThrows
    public <T> T waitOnCallable(String message, Callable<T> callable, Component parentOrNull) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final Future<T> resultFuture = executorService.submit(() -> {
                try {
                    SwingUtilities.invokeLater(() -> {
                        messageLabel.setText(message);
                        pack();
                        setLocationRelativeTo(parentOrNull);
                    });
                    return callable.call();
                } finally {
                    SwingUtilities.invokeLater(this::dispose);
                }
            });

            setVisible(true);
            return resultFuture.get();
        } finally {
            executorService.shutdown();
        }
    }
}