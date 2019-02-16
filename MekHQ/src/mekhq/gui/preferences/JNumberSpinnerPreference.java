package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.lang.ref.WeakReference;

public class JNumberSpinnerPreference extends PreferenceElement implements ChangeListener {
    private WeakReference<JSpinner> weakRef;
    private String name;
    private int value;

    public JNumberSpinnerPreference(JSpinner spinner){
        super (spinner.getName());
        assert spinner.getModel() instanceof SpinnerNumberModel;

        this.value = (Integer)spinner.getValue();
        spinner.addChangeListener(this);
        this.weakRef = new WeakReference<>(spinner);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSpinner element = weakRef.get();
        if (element != null) {
            this.value = (Integer)element.getValue();
        }
    }

    @Override
    protected String getValue() {
        return Integer.toString(this.value);
    }

    @Override
    protected void protectedSetInitialValue(String value) {
        assert value != null && value.trim().length() > 0;

        JSpinner element = weakRef.get();
        if (element != null) {
            element.setValue(Integer.parseInt(value));
        }
    }
}

