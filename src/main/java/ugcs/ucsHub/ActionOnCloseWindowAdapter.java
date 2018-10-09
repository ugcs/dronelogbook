package ugcs.ucsHub;

import lombok.SneakyThrows;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * {@link WindowAdapter} implementation with given {@link CloseAction} performed on window close event
 */
public class ActionOnCloseWindowAdapter extends WindowAdapter {

    @FunctionalInterface
    public interface CloseAction {
        void doAction() throws Exception;
    }

    private final CloseAction closeAction;

    public ActionOnCloseWindowAdapter(CloseAction closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    @SneakyThrows
    public void windowClosing(WindowEvent e) {
        closeAction.doAction();
    }

    @Override
    @SneakyThrows
    public void windowClosed(WindowEvent e) {
        closeAction.doAction();
    }
}
