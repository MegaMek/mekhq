package mekhq.gui.stratcon;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;

public class ScenarioWizardLanceModel implements ListModel<Force> {
    private List<Force> lances;

    /**
     * Constructor. Populates a scenario wizard lance model with all forces in a campaign indiscriminately
     * @param campaign
     */
    public ScenarioWizardLanceModel(Campaign campaign) {
        lances = new ArrayList<Force>();

        for(int key : campaign.getLances().keySet()) {
            lances.add(campaign.getForce(key));
        }
    }

    /**
     * Constructor. Populates a scenario wizard lance model with forces from the given force list
     * @param campaign
     */
    public ScenarioWizardLanceModel(List<Force> forces) {
        lances = forces;
    }

    @Override
    public int getSize() {
        return lances.size();
    }

    @Override
    public Force getElementAt(int index) {
        return lances.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        // TODO Auto-generated method stub

    }
}