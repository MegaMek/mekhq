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

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * List data model for the StratCon scenario wizard.
 * @author NickAragua
 */
public class ScenarioWizardLanceModel extends DefaultListModel<Force> {
    /**
     * Constructor - sometimes, you have a list of force IDs.
     */
    public ScenarioWizardLanceModel(Campaign campaign, Collection<Integer> forceIDs) {
        List<Force> sortedForces = new ArrayList<>();

        for (int forceID : forceIDs) {
            sortedForces.add(campaign.getForce(forceID));
        }

        // let's sort these guys by alphabetical order
        sortedForces.sort(Comparator.comparing(Force::getName));

        super.addAll(sortedForces);
    }
}
