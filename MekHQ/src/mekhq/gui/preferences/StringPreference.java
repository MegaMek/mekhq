package mekhq.gui.preferences;

import mekhq.gui.utilities.ObservableString;
import mekhq.preferences.PreferenceElement;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

public class StringPreference extends PreferenceElement implements PropertyChangeListener {
    private WeakReference<ObservableString> weakRef;
    private String value;

    public StringPreference(ObservableString stringProperty) {
        super(stringProperty.getName());

        this.value = stringProperty.getValue();
        stringProperty.addPropertyChangeListener(this);
        this.weakRef = new WeakReference<>(stringProperty);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        ObservableString element = weakRef.get();
        if (element != null) {
            this.value = element.getValue();
        }
    }

    @Override
    protected String getValue() {
        return this.value;
    }

    @Override
    protected void protectedSetInitialValue(String value) {
        ObservableString element = weakRef.get();
        if (element != null) {
            element.setValue(value);
        }
    }
}
