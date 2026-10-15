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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Quirks multiply a unit's spare parts cost in turn (Campaign Operations p.24, issue #9698).
 */
class SparePartsQuirkMultiplierTest {
    private static final int GAME_YEAR = 3050;
    private static final double TOLERANCE = 0.0001;

    /** A unit with the given quirks that is not obsolete. */
    private static Entity unitWithQuirks(String... quirks) {
        Entity entity = mock(Entity.class);
        for (String quirk : quirks) {
            when(entity.hasQuirk(quirk)).thenReturn(true);
        }
        when(entity.getObsoleteYearForModifiers(anyInt())).thenReturn(0);
        return entity;
    }

    @Test
    void aUnitWithNoQuirksPaysTheBaseCost() {
        assertEquals(1.0, SparePartsQuirkMultiplier.find(unitWithQuirks(), GAME_YEAR), TOLERANCE);
    }

    @Test
    void theRulebookExampleEasyToMaintainWithNonStandardPartsCostsOnePointSixTimes() {
        Entity entity = unitWithQuirks(OptionsConstants.QUIRK_POS_EASY_MAINTAIN,
              OptionsConstants.QUIRK_NEG_NON_STANDARD);

        assertEquals(1.6, SparePartsQuirkMultiplier.find(entity, GAME_YEAR), TOLERANCE,
              "10,000 x 0.8 x 2.0 = 16,000 C-bills a ton");
    }

    @Test
    void ruggedOfEitherLevelCountsLikeEasyToMaintain() {
        assertEquals(0.8, SparePartsQuirkMultiplier.find(unitWithQuirks(OptionsConstants.QUIRK_POS_RUGGED_1),
              GAME_YEAR), TOLERANCE);
        assertEquals(0.8, SparePartsQuirkMultiplier.find(unitWithQuirks(OptionsConstants.QUIRK_POS_RUGGED_2),
              GAME_YEAR), TOLERANCE);
        assertEquals(0.8, SparePartsQuirkMultiplier.find(unitWithQuirks(OptionsConstants.QUIRK_POS_EASY_MAINTAIN,
              OptionsConstants.QUIRK_POS_RUGGED_1), GAME_YEAR), TOLERANCE, "Both together still give x0.8");
    }

    @Test
    void difficultToMaintainCostsAQuarterMore() {
        assertEquals(1.25, SparePartsQuirkMultiplier.find(
              unitWithQuirks(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN), GAME_YEAR), TOLERANCE);
    }

    @Test
    void clanUbiquitousCountsLikeInnerSphereUbiquitous() {
        assertEquals(0.75, SparePartsQuirkMultiplier.find(
              unitWithQuirks(OptionsConstants.QUIRK_POS_UBIQUITOUS_CLAN), GAME_YEAR), TOLERANCE);
        assertEquals(0.75, SparePartsQuirkMultiplier.find(
              unitWithQuirks(OptionsConstants.QUIRK_POS_UBIQUITOUS_IS), GAME_YEAR), TOLERANCE);
    }

    @Test
    void anObsoleteUnitCostsMoreForEveryTwentyYearsPastItsObsolescence() {
        Entity entity = unitWithQuirks();
        when(entity.getObsoleteYearForModifiers(anyInt())).thenReturn(GAME_YEAR - 45);

        assertEquals(1.3, SparePartsQuirkMultiplier.find(entity, GAME_YEAR), TOLERANCE,
              "1.1, plus 0.1 for each of the two full twenty-year spans");
    }

    @Test
    void aUnitNotYetObsoletePaysNoObsoleteMultiplier() {
        Entity entity = unitWithQuirks();
        when(entity.getObsoleteYearForModifiers(anyInt())).thenReturn(GAME_YEAR + 10);

        assertEquals(1.0, SparePartsQuirkMultiplier.find(entity, GAME_YEAR), TOLERANCE);
    }

    @Test
    void quirksChangeTheCostOnlyWhenTheCampaignUsesQuirks() {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        locust.getEntity().getQuirks().getOption(OptionsConstants.QUIRK_POS_EASY_MAINTAIN).setValue(true);

        useQuirks(campaign, false);
        Money costWithoutQuirks = locust.getSparePartsCost();
        useQuirks(campaign, true);
        Money costWithQuirks = locust.getSparePartsCost();

        // The Locust LCT-1V is Ubiquitous (both the IS and Clan quirks, counted once): 0.8 x 0.75 = 0.6
        assertEquals(costWithoutQuirks.multipliedBy(0.6).getAmount().doubleValue(),
              costWithQuirks.getAmount().doubleValue(), TOLERANCE);
    }
    /** Switches quirks on or off the way the campaign options do, on the campaign and on its game. */
    private static void useQuirks(Campaign campaign, boolean isUsingQuirks) {
        campaign.getCampaignOptions().set(CampaignOption.USE_QUIRKS, isUsingQuirks);
        campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(isUsingQuirks);
    }
}
