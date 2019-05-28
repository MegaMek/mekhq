package mekhq.gui.stratcon;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

public class ScenarioWizardUnitRenderer extends JLabel implements ListCellRenderer<Unit> {
    private Campaign campaign;

    public ScenarioWizardUnitRenderer(Campaign campaign) {
        this.campaign = campaign;
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Unit> list, Unit value, int index,
            boolean isSelected, boolean cellHasFocus) {

        setText(String.format("%s (BV: %d)", value.getName(), value.getEntity().calculateBattleValue()));

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