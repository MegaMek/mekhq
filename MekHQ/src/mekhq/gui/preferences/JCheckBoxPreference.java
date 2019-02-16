package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.lang.ref.WeakReference;

public class JCheckBoxPreference extends PreferenceElement implements ChangeListener {
    private WeakReference<JCheckBox>  weakRef;
    private String name;
    private boolean value;

    public JCheckBoxPreference(JCheckBox checkBox){
        super(checkBox.getName());

        this.value = checkBox.isSelected();
        checkBox.addChangeListener(this);
        this.weakRef = new WeakReference<>(checkBox);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JCheckBox element = weakRef.get();
        if (element != null) {
            this.value = element.isSelected();
        }
    }

    @Override
    protected String getValue() {
        return Boolean.toString(this.value);
    }

    @Override
    protected void protectedSetInitialValue(String value) {
        assert value != null && value.trim().length() > 0;

        JCheckBox element = weakRef.get();
        if (element != null) {
            element.setSelected(Boolean.parseBoolean(value));
        }
    }
}
