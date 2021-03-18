/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.gui.stratcon;

import java.util.Collection;

import javax.swing.DefaultListModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;

/**
 * List data model for the StratCon scenario wizard.
 * @author NickAragua
 */
public class ScenarioWizardLanceModel extends DefaultListModel<Force> {
    /**
     * Constructor - sometimes, you have a list of force IDs.
     */
    public ScenarioWizardLanceModel(Campaign campaign, Collection<Integer> forceIDs) {
        for (int forceID : forceIDs) {
            super.addElement(campaign.getForce(forceID));
        }
    }
}