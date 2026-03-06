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
package mekhq.campaign.unit;

import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Misc. unit tests for other functions of Unit.
 */
public class UnitTest {
    @Test
    void testQuirksList() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);

        Entity entity = MHQTestUtilities.getEntityForUnitTesting("Griffin GRF-1E Sparky", false);
        entity.setGame(campaign.getGame());

        Unit unit = new Unit(entity, campaign);
        List<IOption> quirks = unit.getQuirks();

        assertFalse(quirks.isEmpty());
    }

    @Test
    void testQuirksListEmptyWhenQuirksOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(false);

        Entity entity = MHQTestUtilities.getEntityForUnitTesting("Griffin GRF-1E Sparky", false);
        entity.setGame(campaign.getGame());

        Unit unit = new Unit(entity, campaign);
        List<IOption> quirks = unit.getQuirks();

        assertTrue(quirks.isEmpty());
    }

    @Test
    void TestQuirksListHTML() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);

        Entity entity = MHQTestUtilities.getEntityForUnitTesting("Griffin GRF-1E Sparky", false);
        entity.setGame(campaign.getGame());

        Unit unit = new Unit(entity, campaign);
        String quirksList = unit.getQuirksListHTML();

        assertEquals(
              "<html>Battle Fists (LA)<br/>Battle Fists (RA)<br/>Ubiquitous (Clans)<br/>Rugged (1 Point)<br/>" +
                           "Ubiquitous (Inner Sphere)</html>",
              quirksList
        );
    }
    @Test
    void TestQuirksListHTMLEmptyWhenQuirksOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(false);

        Entity entity = MHQTestUtilities.getEntityForUnitTesting("Griffin GRF-1E Sparky", false);
        entity.setGame(campaign.getGame());

        Unit unit = new Unit(entity, campaign);
        String quirksList = unit.getQuirksListHTML();

        assertNull(quirksList);
    }

}
