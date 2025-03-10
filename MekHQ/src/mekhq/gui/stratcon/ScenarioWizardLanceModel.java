/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */

package mekhq.gui.stratcon;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;

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
