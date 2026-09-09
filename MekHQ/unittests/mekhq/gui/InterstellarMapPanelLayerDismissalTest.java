package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

class InterstellarMapPanelLayerDismissalTest {
    @Test
    void layerControlsAndTriggerRemainInsideThePopoverInteraction() {
        JPanel control = new JPanel();
        JCheckBox checkbox = new JCheckBox();
        control.add(checkbox);
        JButton trigger = new JButton();

        assertTrue(InterstellarMapPanel.isLayerControlInteraction(control, control, trigger));
        assertTrue(InterstellarMapPanel.isLayerControlInteraction(checkbox, control, trigger));
        assertTrue(InterstellarMapPanel.isLayerControlInteraction(trigger, control, trigger));
    }

    @Test
    void dropdownPopupBelongsToItsLayerControlInvoker() {
        JPanel control = new JPanel();
        JComboBox<String> dropdown = new JComboBox<>();
        control.add(dropdown);
        JPopupMenu popup = new JPopupMenu();
        popup.setInvoker(dropdown);
        JList<String> choices = new JList<>();
        popup.add(new JScrollPane(choices));

        assertTrue(InterstellarMapPanel.isLayerControlInteraction(choices, control, null));
    }

    @Test
    void mapAndInspectorClicksAreOutsideEvenUnderTheSameParent() {
        JPanel workspace = new JPanel();
        JPanel control = new JPanel();
        JPanel map = new JPanel();
        JButton inspectorButton = new JButton();
        workspace.add(control);
        workspace.add(map);
        workspace.add(inspectorButton);

        assertFalse(InterstellarMapPanel.isLayerControlInteraction(map, control, null));
        assertFalse(InterstellarMapPanel.isLayerControlInteraction(inspectorButton, control, null));
    }

    @Test
    void unrelatedPopupDoesNotCountAsALayerInteraction() {
        JPanel control = new JPanel();
        JPopupMenu popup = new JPopupMenu();
        popup.setInvoker(new JButton());

        assertFalse(InterstellarMapPanel.isLayerControlInteraction(popup, control, null));
    }
}