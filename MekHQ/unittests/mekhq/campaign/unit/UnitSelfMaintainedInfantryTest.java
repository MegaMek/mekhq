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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.Test;

/**
 * Conventional infantry maintain themselves unless the "Mechanics Maintain Conventional Infantry" campaign option is
 * enabled, in which case they are treated like any other Mechanic-maintained unit.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitSelfMaintainedInfantryTest {

    @Test
    void conventionalInfantryMaintainThemselvesByDefault() {
        Unit unit = unitFor(conventionalInfantry(), false);

        assertTrue(unit.isSelfMaintainedInfantry());
        assertTrue(unit.isSelfCrewed());
        assertEquals("", unit.determineUnitTechSkillType());
    }

    @Test
    void conventionalInfantryAreMaintainedByMechanicsWhenOptionEnabled() {
        Unit unit = unitFor(conventionalInfantry(), true);

        assertFalse(unit.isSelfMaintainedInfantry());
        assertFalse(unit.isSelfCrewed());
        assertEquals(SkillType.S_TECH_VEHICLE, unit.determineUnitTechSkillType());
    }

    @Test
    void conventionalInfantryWithoutCampaignMaintainThemselves() {
        Unit unit = new Unit(conventionalInfantry(), null);

        assertTrue(unit.isSelfMaintainedInfantry());
        assertTrue(unit.isSelfCrewed());
    }

    @Test
    void battleArmorIsNeverSelfMaintainedInfantry() {
        for (boolean isOptionEnabled : new boolean[] { false, true }) {
            Unit unit = unitFor(mock(BattleArmor.class), isOptionEnabled);

            assertFalse(unit.isSelfMaintainedInfantry());
            assertFalse(unit.isSelfCrewed());
            assertEquals(SkillType.S_TECH_BA, unit.determineUnitTechSkillType());
        }
    }

    @Test
    void optionDoesNotAffectOtherUnitTypes() {
        for (boolean isOptionEnabled : new boolean[] { false, true }) {
            Unit tankUnit = unitFor(mock(Tank.class), isOptionEnabled);
            assertFalse(tankUnit.isSelfMaintainedInfantry());
            assertFalse(tankUnit.isSelfCrewed());

            Unit dropshipUnit = unitFor(mock(Dropship.class), isOptionEnabled);
            assertFalse(dropshipUnit.isSelfMaintainedInfantry());
            assertTrue(dropshipUnit.isSelfCrewed());
        }
    }

    private static ConvInfantry conventionalInfantry() {
        ConvInfantry infantry = mock(ConvInfantry.class);
        when(infantry.isConventionalInfantry()).thenReturn(true);
        return infantry;
    }

    private static Unit unitFor(Entity entity, boolean isTechsMaintainConventionalInfantry) {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.TECHS_MAINTAIN_CONVENTIONAL_INFANTRY))
              .thenReturn(isTechsMaintainConventionalInfantry);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        return new Unit(entity, campaign);
    }
}
