package ugcs.ucsHub;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
    public void windowClosing(WindowEvent e) {
        try {
            closeAction.doAction();
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        try {
            closeAction.doAction();
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }
}
