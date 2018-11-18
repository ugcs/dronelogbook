package ugcs.ucsHub.ui.components;

import lombok.Getter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static javax.swing.SwingUtilities.invokeLater;

/**
 * Extended version of {@link JPasswordField} which shows only fixed count of echo chars instead of password-sized
 * string
 */
public class JPasswordFieldEx extends JPasswordField {
    private final static int DEFAULT_ECHO_CHARS_COUNT = 6;

    private final char[] dummyPassword;
    private final String dummyPasswordStr;

    private boolean isPasswordChangedByUser = false;

    private boolean isCurrentPasswordChangedByUser = true;

    @Getter
    private String currentPassword;

    public JPasswordFieldEx(String text) {
        this(text, 0);
    }

    public JPasswordFieldEx(int columns) {
        this(null, columns);
    }

    public JPasswordFieldEx(String text, int columns) {
        this(text, columns, DEFAULT_ECHO_CHARS_COUNT);
    }

    public JPasswordFieldEx(String text, int columns, int echoCharsCount) {
        super(null, columns);

        this.currentPassword = text;

        this.dummyPassword = new char[echoCharsCount];
        this.dummyPasswordStr = new String(dummyPassword);
        this.setText(dummyPasswordStr);

        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!isPasswordChangedByUser) {
                    setTextWithoutPasswordUpdate("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!isPasswordChangedByUser) {
                    setTextWithoutPasswordUpdate(dummyPasswordStr);
                }
            }
        });

        this.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCurrentPassword();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCurrentPassword();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCurrentPassword();
            }
        });
    }

    @Override
    public String getText() {
        return dummyPasswordStr;
    }

    @Override
    public char[] getPassword() {
        return dummyPassword;
    }

    private void setTextWithoutPasswordUpdate(String text) {
        isCurrentPasswordChangedByUser = false;
        invokeLater(() -> {
            setText(text);
            isCurrentPasswordChangedByUser = true;
        });
    }

    private void updateCurrentPassword() {
        if (isCurrentPasswordChangedByUser) {
            isPasswordChangedByUser = true;
            currentPassword = new String(super.getPassword());
        }
    }
}
