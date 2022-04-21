package mekhq.gui.utilities;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

public class ObservableString {
    private String name;
    private String value;

    private PropertyChangeSupport support;

    public ObservableString(String name, String initialValue) {
        Objects.requireNonNull(name);
        if (name.isBlank()) {
            throw new AssertionError();
        }

        this.name = name;
        this.value = initialValue;
        support = new PropertyChangeSupport(this);
    }
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String newValue) {
        if (!this.value.equals(newValue)) {
            String oldValue = this.value;
            this.value = newValue;
            this.support.firePropertyChange("value", oldValue, newValue);
        }
    }
}
