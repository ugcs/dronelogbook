package ugcs.ucsHub.ui;

import lombok.SneakyThrows;
import ugcs.common.operation.FutureWrapper;
import ugcs.common.operation.OperationPerformer;
import ugcs.ucsHub.ActionOnCloseWindowAdapter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.Color.decode;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showConfirmDialog;
import static ugcs.ucsHub.Settings.settings;

/**
 * Modal window with progress bar used for the list of long running blocking operations
 */
public class WaitWithProgressBarForm extends JDialog {

    private static String DEFAULT_MESSAGE_TEMPLATE = "{0} of {1} completed";
    private static String DEFAULT_CANCEL_MESSAGE = "Do you want to cancel all waiting operations?";
    private static Color PROGRESS_COLOR = decode("0x74BD44");

    private final JProgressBar progressBar;
    private final AtomicInteger progressCounter = new AtomicInteger(0);

    private final JLabel progressLabel;

    private int totalCount = 1;
    private String messageTemplate = DEFAULT_MESSAGE_TEMPLATE;
    private String cancelMessage = DEFAULT_CANCEL_MESSAGE;

    private WaitWithProgressBarForm() {
        super((JFrame) null, true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel progressBarPanel = new JPanel(new BorderLayout());
        progressBarPanel.setBorder(createEmptyBorder(0, 5, 0, 5));
        progressBar = new JProgressBar();
        progressBarPanel.add(progressBar, BorderLayout.CENTER);
        mainPanel.add(progressBarPanel, BorderLayout.CENTER);
        progressBar.setStringPainted(false);
        progressBar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionForeground() {
                return Color.BLACK;
            }

            @Override
            protected Color getSelectionBackground() {
                return Color.BLACK;
            }
        });
        progressBar.setForeground(PROGRESS_COLOR);

        progressLabel = new JLabel();
        mainPanel.add(new JPanel().add(progressLabel).getParent(), BorderLayout.SOUTH);

        JLabel loadingIcon = new JLabel("", settings().getLoadingIcon(), SwingConstants.CENTER);
        mainPanel.add(new JPanel().add(loadingIcon).getParent(), BorderLayout.NORTH);

        add(mainPanel);

        setIconImage(settings().getLogoIcon().getImage());

        pack();
    }

    private static volatile WaitWithProgressBarForm instance;

    public static WaitWithProgressBarForm waitWithProgressBarForm() {
        if (instance == null) {
            synchronized (WaitWithProgressBarForm.class) {
                if (instance == null) {
                    instance = new WaitWithProgressBarForm();
                }
            }
        }
        return instance.withMessageTemplate(DEFAULT_MESSAGE_TEMPLATE);
    }

    public WaitWithProgressBarForm withMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
        return this;
    }

    public WaitWithProgressBarForm withCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
        return this;
    }

    @SneakyThrows
    public <T> List<T> waitOnFutures(List<Future<T>> futures, Component parentOrNull, OperationPerformer operationPerformer) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        totalCount = futures.size();
        progressCounter.set(0);
        progressBar.setMaximum(totalCount);
        progressBar.setMinimum(0);

        ActionOnCloseWindowAdapter actionOnCloseButtonClick = null;

        try {
            actionOnCloseButtonClick = new ActionOnCloseWindowAdapter(() -> {
                final int dialogResult = showConfirmDialog(WaitWithProgressBarForm.this, cancelMessage, "Cancel",
                        OK_CANCEL_OPTION, PLAIN_MESSAGE, settings().getQuestionIcon()
                );

                if (dialogResult == OK_OPTION) {
                    removeAllCloseActionListeners();
                    updateTitle("Cancelling...", Font.BOLD);
                    operationPerformer.cancelAllWaitingOperations();
                }
            });
            addWindowListener(actionOnCloseButtonClick);

            final Future<List<T>> resultFuture = executorService.submit(() -> {
                try {
                    updateProgress(0, parentOrNull);
                    return futures.stream()
                            .map(FutureWrapper::of)
                            .map(FutureWrapper::get)
                            .peek(result -> updateProgress(1, parentOrNull))
                            .collect(toList());
                } finally {
                    SwingUtilities.invokeLater(this::dispose);
                }
            });

            updateTitle("Please wait...", Font.PLAIN);
            setVisible(true);
            return resultFuture.get();
        } finally {
            removeAllCloseActionListeners();
            executorService.shutdown();
        }
    }

    private void updateTitle(String title, int fontType) {
        setTitle(title);
        try {
            final Component titleComponent = getLayeredPane().getComponent(0);
            final Font titleFont = titleComponent.getFont().deriveFont(fontType);
            titleComponent.setFont(titleFont);
        } catch (ArrayIndexOutOfBoundsException itsOkNotToUpdateFontType) {
        }
    }

    private void updateProgress(int updateCounterDelta, Component parentOrNull) {
        SwingUtilities.invokeLater(() -> {
            final int progress = progressCounter.addAndGet(updateCounterDelta);
            progressBar.setValue(progress);

            final String message = format(messageTemplate, progress, totalCount);
            progressBar.setString(message);
            progressLabel.setText(message);

            pack();
            setLocationRelativeTo(parentOrNull);
        });
    }

    private void removeAllCloseActionListeners() {
        for (WindowListener windowFocusListener : getWindowListeners()) {
            if (windowFocusListener instanceof ActionOnCloseWindowAdapter) {
                removeWindowListener(windowFocusListener);
            }
        }
    }
}