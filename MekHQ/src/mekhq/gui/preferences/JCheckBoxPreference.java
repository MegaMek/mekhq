package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;

public class JCheckBoxPreference extends PreferenceElement {
    private JCheckBox checkBox;

    public JCheckBoxPreference(JCheckBox checkBox){
        assert checkBox.getName() != null && checkBox.getName().trim().length() > 0;
        this.checkBox = checkBox;
    }

    @Override
    public String getElementName() {
        return this.checkBox.getName();
    }

    @Override
    public String getCurrentValue() {
        return Boolean.toString(this.checkBox.isSelected());
    }

    @Override
    protected void protectedSetInitialValue(String value) {
        assert value != null && value.trim().length() > 0;
        this.checkBox.setSelected(Boolean.parseBoolean(value));
    }
}
