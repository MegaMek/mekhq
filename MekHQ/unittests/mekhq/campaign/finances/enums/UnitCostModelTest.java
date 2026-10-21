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
        assertEquals(model == UnitCostModel.CANON, model.isCanon());
        assertEquals(model == UnitCostModel.CHAOS_CAMPAIGN_STRICT, model.isChaosCampaignStrict());
        assertEquals(model == UnitCostModel.CHAOS_CAMPAIGN_REVISED, model.isChaosCampaignRevised());
    }

    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void nameRoundTripsThroughValueOf(UnitCostModel model) {
        assertTrue(model == UnitCostModel.valueOf(model.name()));
    }

    @Test
    void canonNeverUsesBattleValue() {
        assertFalse(UnitCostModel.CANON.isBattleValueCost(false));
        assertFalse(UnitCostModel.CANON.isBattleValueCost(true));
    }

    @Test
    void strictAlwaysUsesBattleValue() {
        assertTrue(UnitCostModel.CHAOS_CAMPAIGN_STRICT.isBattleValueCost(false));
        assertTrue(UnitCostModel.CHAOS_CAMPAIGN_STRICT.isBattleValueCost(true));
    }

    @Test
    void revisedUsesBattleValueForEverythingButLargeCraft() {
        assertTrue(UnitCostModel.CHAOS_CAMPAIGN_REVISED.isBattleValueCost(false));
        assertFalse(UnitCostModel.CHAOS_CAMPAIGN_REVISED.isBattleValueCost(true));
    }

    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void nullEntityNeverUsesBattleValue(UnitCostModel model) {
        assertFalse(model.isBattleValueCost((Entity) null));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void revisedFollowsTheEntityLargeCraftCheck(boolean isLargeCraft) {
        Entity entity = mock(Entity.class);
        when(entity.isLargeCraft()).thenReturn(isLargeCraft);

        assertEquals(!isLargeCraft, UnitCostModel.CHAOS_CAMPAIGN_REVISED.isBattleValueCost(entity));
    }

    /** Matches {@link Entity#isLargeCraft()}: DropShips and every kind of JumpShip, but not small craft. */
    @ParameterizedTest
    @ValueSource(ints = { UnitType.MEK, UnitType.TANK, UnitType.AEROSPACE_FIGHTER, UnitType.SMALL_CRAFT,
                          UnitType.DROPSHIP, UnitType.JUMPSHIP, UnitType.WARSHIP, UnitType.SPACE_STATION })
    void revisedFollowsTheSummaryUnitType(int unitType) {
        MekSummary summary = mock(MekSummary.class);
        when(summary.getUnitType()).thenReturn(UnitType.getTypeName(unitType));
        boolean isLargeCraft = (unitType == UnitType.DROPSHIP) || (unitType == UnitType.JUMPSHIP)
                                     || (unitType == UnitType.WARSHIP) || (unitType == UnitType.SPACE_STATION);

        assertEquals(!isLargeCraft, UnitCostModel.CHAOS_CAMPAIGN_REVISED.isBattleValueCost(summary));
    }
}
