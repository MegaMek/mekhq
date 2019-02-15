package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JCheckBoxPreference extends PreferenceElement implements ChangeListener {
    private JCheckBox checkBox;
    private boolean value;


    public JCheckBoxPreference(JCheckBox checkBox){
        assert checkBox.getName() != null && checkBox.getName().trim().length() > 0;

        this.checkBox = checkBox;
        this.checkBox.addChangeListener(this);
        this.value = this.checkBox.isSelected();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.value = this.checkBox.isSelected();
    }

    @Override
    public String getElementName() {
        return this.checkBox.getName();
    }

    @Override
    public String getCurrentValue() {
        return Boolean.toString(this.value);
    }

    @Override
    protected void protectedSetInitialValue(String value) {
        assert value != null && value.trim().length() > 0;

        this.checkBox.setSelected(Boolean.parseBoolean(value));
    }
}
