package mekhq.gui.stratcon;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;

public class ScenarioWizardLanceRenderer extends JLabel implements ListCellRenderer<Force> {
    private Campaign campaign;

    public ScenarioWizardLanceRenderer(Campaign campaign) {
        this.campaign = campaign;
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Force> list, Force value, int index,
            boolean isSelected, boolean cellHasFocus) {

        setText(String.format("%s (BV: %d)", value.getName(), value.getTotalBV(campaign)));

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }

}