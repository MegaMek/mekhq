package mekhq.gui.stratcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.stratcon.StratconTrackState;

public class ScenarioWizardLanceModel extends DefaultListModel<Force> {
    public ScenarioWizardLanceModel(Campaign campaign, List<Integer> forceIDs) {
        super();
        for(int forceID : forceIDs) {
            super.addElement(campaign.getForce(forceID));
        }
    }
    
    public ScenarioWizardLanceModel(Campaign campaign, Set<Integer> forceIDs) {
        super();
        for(int forceID : forceIDs) {
            super.addElement(campaign.getForce(forceID));
        }
    }
}