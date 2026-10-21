/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;

/**
 * The formation shown after a tech's name in the Repair Bay, so techs kept on a sub-contract stand apart from the main
 * force (issue #10601).
 */
class TechTableModelFormationTest {
    @Test
    void aTechsFormationFollowsTheirName() {
        Campaign campaign = mockCampaign();
        Person tech = mock(Person.class);
        Formation formation = mock(Formation.class);
        when(formation.getFullName()).thenReturn("Alpha Lance, 1st Company");
        when(campaign.getPlayerForce().getFormationFor(tech)).thenReturn(formation);

        assertEquals(" (Alpha Lance, 1st Company)", TechTableModel.describeFormation(campaign, tech));
    }

    @Test
    void aTechWithNoFormationShowsNothing() {
        Campaign campaign = mockCampaign();
        Person tech = mock(Person.class);
        when(campaign.getPlayerForce().getFormationFor(tech)).thenReturn(null);

        assertEquals("", TechTableModel.describeFormation(campaign, tech));
    }
}
