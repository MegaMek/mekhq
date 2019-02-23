package mekhq.gui.preferences;


import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.lang.ref.WeakReference;

public class JTextFieldPreference extends PreferenceElement implements DocumentListener {
    private final WeakReference<JTextField> weakRef;
    private String value;

    public JTextFieldPreference(JTextField textField) {
        super(textField.getName());

        this.value = textField.getText();
        this.weakRef = new WeakReference<>(textField);
        textField.getDocument().addDocumentListener(this);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        JTextField element = weakRef.get();
        if (element != null) {
            this.value = element.getText();
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        JTextField element = weakRef.get();
        if (element != null) {
            this.value = element.getText();
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        JTextField element = weakRef.get();
        if (element != null) {
            this.value = element.getText();
        }
    }

    @Override
    protected String getValue() {
        return this.value;
    }

    @Override
    protected void initialize(String value) {
        JTextField element = weakRef.get();
        if (element != null) {
            element.setText(value);
        }
    }

    @Override
    protected void dispose() {
        JTextField element = weakRef.get();
        if (element != null) {
            element.getDocument().removeDocumentListener(this);
            weakRef.clear();
        }
    }
}
