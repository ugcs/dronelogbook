package ugcs.ucsHub.ui;

import lombok.SneakyThrows;
import ugcs.common.operation.FutureWrapper;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static ugcs.ucsHub.Settings.settings;

/**
 * Modal window with progress bar used for the list of long running blocking operations
 */
public class WaitWithProgressBarForm extends JDialog {

    private static String DEFAULT_MESSAGE_TEMPLATE = "{0} of {1} completed";

    private final JProgressBar progressBar;
    private final AtomicInteger progressCounter = new AtomicInteger(0);

    private int totalCount = 1;
    private String messageTemplate = DEFAULT_MESSAGE_TEMPLATE;

    private WaitWithProgressBarForm() {
        super((JFrame) null, true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("Please wait...");
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setBorder(createEmptyBorder(0, 10, 10, 10));
        progressBar = new JProgressBar();
        progressBarPanel.add(progressBar);
        mainPanel.add(progressBarPanel, BorderLayout.SOUTH);
        progressBar.setStringPainted(true);
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

        JLabel loadingIcon = new JLabel("", settings().getLoadingIcon(), SwingConstants.CENTER);
        mainPanel.add(new JPanel().add(loadingIcon).getParent(), BorderLayout.CENTER);


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

    @SneakyThrows
    public <T> List<T> waitOnFutures(List<Future<T>> futures, Component parentOrNull) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        totalCount = futures.size();
        progressCounter.set(0);
        progressBar.setMaximum(totalCount);
        progressBar.setMinimum(0);
        try {
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

            setVisible(true);
            return resultFuture.get();
        } finally {
            executorService.shutdown();
        }
    }

    private void updateProgress(int updateCounterDelta, Component parentOrNull) {
        SwingUtilities.invokeLater(() -> {
            final int progress = progressCounter.addAndGet(updateCounterDelta);
            progressBar.setValue(progress);
            progressBar.setString(format(messageTemplate, progress, totalCount));
            pack();
            setLocationRelativeTo(parentOrNull);
        });
    }
}