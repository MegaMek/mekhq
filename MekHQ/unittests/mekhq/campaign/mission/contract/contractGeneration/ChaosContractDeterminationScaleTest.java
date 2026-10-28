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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DecimalFormat;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ChaosContractDeterminationScale#generateScaleForDetachment}. Scale is the detachment's combat Battle
 * Value divided by a fixed per-scale figure (4500 + 32*500 = 20500), rounded up. Only STANDARD formations in a combat
 * role count.
 */
class ChaosContractDeterminationScaleTest {
    private static final double DELTA = 1e-9;

    private static final int BATTLE_VALUE_PER_SCALE = 20_500;

    private final PlayerForce playerForce = mock(PlayerForce.class);

    /**
     * Wires a unit into {@link #playerForce}: its formation reports the given role and formation-type membership, and
     * its entity the given Battle Value ({@code null} BV means the unit has no entity).
     */
    private Unit unit(final int formationId, final CombatRole role, final boolean standard, final Integer battleValue) {
        Formation formation = mock(Formation.class);
        when(formation.getCombatRoleInMemory()).thenReturn(role);
        when(formation.isFormationType(eq(FormationType.STANDARD))).thenReturn(standard);
        when(playerForce.getFormation(formationId)).thenReturn(formation);

        Unit unit = mock(Unit.class);
        when(unit.getFormationId()).thenReturn(formationId);
        if (battleValue == null) {
            when(unit.getEntity()).thenReturn(null);
        } else {
            Entity entity = mock(Entity.class);
            when(entity.calculateBattleValue(true, true)).thenReturn(battleValue);
            when(unit.getEntity()).thenReturn(entity);
        }
        return unit;
    }

    private int scaleFor(final Unit... units) {
        return ChaosContractDeterminationScale.roundUpScale(exactScaleFor(units));
    }

    private double exactScaleFor(final Unit... units) {
        LocalHangar hangar = mock(LocalHangar.class);
        when(hangar.getUnits()).thenReturn(List.of(units));
        return ChaosContractDeterminationScale.generateScaleForDetachment(playerForce, hangar, true);
    }

    @Test
    void battleValueIsDividedByThePerScaleFigureAndRoundedUp() {
        assertEquals(1, scaleFor(unit(1, CombatRole.MANEUVER, true, BATTLE_VALUE_PER_SCALE)),
              "exactly one scale's worth of BV is one scale");
        assertEquals(2, scaleFor(unit(1, CombatRole.MANEUVER, true, BATTLE_VALUE_PER_SCALE + 1)),
              "one BV over the boundary rounds up to the next scale");
        assertEquals(1, scaleFor(unit(1, CombatRole.MANEUVER, true, 100)),
              "any positive combat BV is at least one scale");
    }

    @Test
    void exactScaleIsTheUnroundedBattleValueRatio() {
        assertEquals(1.5,
              exactScaleFor(unit(1, CombatRole.MANEUVER, true, BATTLE_VALUE_PER_SCALE + (BATTLE_VALUE_PER_SCALE / 2))),
              0.000001);
    }

    @Test
    void oneBattleValueOverABoundaryStillRoundsUpAfterCleaning() {
        // 1 BV in 20,500 is 0.0000488 of a Scale - the precision cleaning must not swallow it
        assertEquals(3, scaleFor(unit(1, CombatRole.MANEUVER, true, (BATTLE_VALUE_PER_SCALE * 2) + 1)));
    }

    @Test
    void anEmptyDetachmentHasNoScale() {
        assertEquals(0, scaleFor());
    }

    @Test
    void nonCombatRolesDoNotContribute() {
        assertEquals(0, scaleFor(unit(1, CombatRole.RESERVE, true, BATTLE_VALUE_PER_SCALE)),
              "a reserve formation is not committed to the detachment");
    }

    @Test
    void nonStandardFormationsDoNotContribute() {
        assertEquals(0, scaleFor(unit(1, CombatRole.MANEUVER, false, BATTLE_VALUE_PER_SCALE)),
              "only STANDARD formations count toward scale");
    }

    @Test
    void aCountedUnitWithoutAnEntityAddsNothing() {
        assertEquals(0, scaleFor(unit(1, CombatRole.MANEUVER, true, null)));
    }

    @Test
    void roundUpScaleRoundsPartScalesUpButLeavesWholeScales() {
        assertEquals(3, ChaosContractDeterminationScale.roundUpScale(2.01));
        assertEquals(2, ChaosContractDeterminationScale.roundUpScale(2.0));
        assertEquals(2, ChaosContractDeterminationScale.roundUpScale(2.0000000001),
              "floating-point noise must not round a whole Scale up a point");
    }

    @Test
    void economicScaleIsWholeWithoutFractionalScale() {
        assertEquals(3.0, ChaosContractDeterminationScale.getEconomicScale(2.3, false), DELTA);
    }

    @Test
    void economicScaleIsExactWithFractionalScale() {
        assertEquals(2.3, ChaosContractDeterminationScale.getEconomicScale(2.3, true), DELTA);
    }

    @Test
    void fractionalEconomicScaleIsAtLeastTheMinimumForAnyForce() {
        assertEquals(ChaosContractDeterminationScale.MINIMUM_FRACTIONAL_SCALE,
              ChaosContractDeterminationScale.getEconomicScale(0.01, true), DELTA);
        assertEquals(0.0, ChaosContractDeterminationScale.getEconomicScale(0.0, true), DELTA,
              "no force at all is still Scale 0");
    }

    @Test
    void scaleIsDisplayedToTwoDecimalPlacesWithFractionalScale() {
        assertEquals(new DecimalFormat("0.00").format(3.46), ChaosContractDeterminationScale.formatScale(3.456789, true));
        assertEquals(new DecimalFormat("0.00").format(0.1), ChaosContractDeterminationScale.formatScale(0.02, true));
    }

    @Test
    void scaleIsDisplayedAsAWholeNumberWithoutFractionalScale() {
        assertEquals("4", ChaosContractDeterminationScale.formatScale(3.456789, false));
    }
}
