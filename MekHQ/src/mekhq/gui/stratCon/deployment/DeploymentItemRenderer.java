package mekhq.gui.stratCon.deployment;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import mekhq.gui.stratCon.ScenarioWizardLanceRenderer;
import mekhq.gui.stratCon.ScenarioWizardUnitRenderer;

/**
 * A list cell renderer for the deployment board, which shows whole formations on the formation-tier pages (Primary,
 * Reinforce) and individual units on the unit-tier pages (Auxiliaries, Utility). It dispatches each cell to the
 * existing formation or unit renderer by the element's type, so one {@link JList} can serve every page.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DeploymentItemRenderer implements ListCellRenderer<Object> {
    private final ScenarioWizardLanceRenderer formationRenderer;
    private final ScenarioWizardUnitRenderer unitRenderer = new ScenarioWizardUnitRenderer();

    public DeploymentItemRenderer(Campaign campaign) {
        formationRenderer = new ScenarioWizardLanceRenderer(campaign);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        if (value instanceof Formation formation) {
            return formationRenderer.getListCellRendererComponent((JList) list,
                  formation,
                  index,
                  isSelected,
                  cellHasFocus);
        }
        if (value instanceof Unit unit) {
            return unitRenderer.getListCellRendererComponent((JList) list, unit, index, isSelected, cellHasFocus);
        }
        return unitRenderer.getListCellRendererComponent((JList) list, null, index, isSelected, cellHasFocus);
    }
}
