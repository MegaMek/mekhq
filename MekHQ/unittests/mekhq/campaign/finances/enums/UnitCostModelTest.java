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
package mekhq.campaign.finances.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link UnitCostModel}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitCostModelTest {
    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void labelAndTooltipResolve(UnitCostModel model) {
        assertFalse(model.getLabel().startsWith("!"), () -> "Missing label for " + model.name());
        assertFalse(model.getTooltip().startsWith("!"), () -> "Missing tooltip for " + model.name());
        assertEquals(model.getLabel(), model.toString());
    }

    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void exactlyOneModelCheckMatches(UnitCostModel model) {
        assertEquals(model == UnitCostModel.TOTAL_WARFARE, model.isTotalWarfare());
        assertEquals(model == UnitCostModel.CHAOS_CAMPAIGN, model.isChaosCampaign());
    }

    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void nameRoundTripsThroughFromString(UnitCostModel model) {
        assertTrue(model == UnitCostModel.fromString(model.name()));
    }

    /**
     * The removed MekHQ model, and anything else unrecognised, loads as the default Total Warfare model.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @ParameterizedTest
    @ValueSource(strings = { "MEKHQ", "", "NOT_A_MODEL" })
    void unknownNamesFallBackToTotalWarfare(String text) {
        assertEquals(UnitCostModel.TOTAL_WARFARE, UnitCostModel.fromString(text));
    }

    @Test
    void nullFallsBackToTotalWarfare() {
        assertEquals(UnitCostModel.TOTAL_WARFARE, UnitCostModel.fromString(null));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void totalWarfareNeverUsesBattleValue(boolean isLargeCraft) {
        Entity entity = mock(Entity.class);
        when(entity.isLargeCraft()).thenReturn(isLargeCraft);

        assertFalse(UnitCostModel.TOTAL_WARFARE.isBattleValueCost(entity));
        assertFalse(UnitCostModel.TOTAL_WARFARE.isBattleValueCost(mock(MekSummary.class)));
    }

    /** Chaos Campaign values every unit by Battle Value, large craft included. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void chaosCampaignAlwaysUsesBattleValue(boolean isLargeCraft) {
        Entity entity = mock(Entity.class);
        when(entity.isLargeCraft()).thenReturn(isLargeCraft);

        assertTrue(UnitCostModel.CHAOS_CAMPAIGN.isBattleValueCost(entity));
    }

    @ParameterizedTest
    @ValueSource(ints = { UnitType.MEK, UnitType.TANK, UnitType.AEROSPACE_FIGHTER, UnitType.SMALL_CRAFT,
                          UnitType.DROPSHIP, UnitType.JUMPSHIP, UnitType.WARSHIP, UnitType.SPACE_STATION })
    void chaosCampaignUsesBattleValueForEverySummary(int unitType) {
        MekSummary summary = mock(MekSummary.class);
        when(summary.getUnitType()).thenReturn(UnitType.getTypeName(unitType));

        assertTrue(UnitCostModel.CHAOS_CAMPAIGN.isBattleValueCost(summary));
    }

    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void nullEntityNeverUsesBattleValue(UnitCostModel model) {
        assertFalse(model.isBattleValueCost((Entity) null));
    }
}
