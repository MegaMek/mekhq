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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerForceRating.ForceRating;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

class ChaosEmployerForceRatingTest {
    /** A year past the early-Renaissance recovery window, so era degradation is zero. */
    private static final int RENAISSANCE_END_YEAR = 3050;

    private static Faction faction(final boolean clan, final boolean comStarOrWoB, final boolean rebelOrPirate,
          final boolean independent, final boolean minorPower) {
        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(clan);
        when(faction.isComStarOrWoB()).thenReturn(comStarOrWoB);
        when(faction.isRebelOrPirate()).thenReturn(rebelOrPirate);
        when(faction.isIndependent()).thenReturn(independent);
        when(faction.isMinorPower()).thenReturn(minorPower);
        return faction;
    }

    private static Faction majorHouse() {
        return faction(false, false, false, false, false);
    }

    private static Faction clan() {
        return faction(true, false, false, false, false);
    }

    private static Faction pirate() {
        return faction(false, false, true, false, false);
    }

    // --- factionDelta ---

    @Test
    void factionDeltaByFactionType() {
        assertEquals(0, ChaosEmployerForceRating.factionDelta(majorHouse()));
        assertEquals(2, ChaosEmployerForceRating.factionDelta(clan()));
        assertEquals(1, ChaosEmployerForceRating.factionDelta(faction(false, true, false, false, false)));
        assertEquals(-2, ChaosEmployerForceRating.factionDelta(pirate()));
        assertEquals(-1, ChaosEmployerForceRating.factionDelta(faction(false, false, false, true, false)));
        assertEquals(-1, ChaosEmployerForceRating.factionDelta(faction(false, false, false, false, true)));
        // Penalties stack: an independent minor power is doubly disadvantaged.
        assertEquals(-2, ChaosEmployerForceRating.factionDelta(faction(false, false, false, true, true)));
    }

    // --- eraDelta ---

    @Test
    void eraDeltaAcrossPeriods() {
        Faction house = majorHouse();
        assertEquals(0, ChaosEmployerForceRating.eraDelta(house, 2829));
        assertEquals(-1, ChaosEmployerForceRating.eraDelta(house, 2830));
        assertEquals(-1, ChaosEmployerForceRating.eraDelta(house, 2865));
        assertEquals(-2, ChaosEmployerForceRating.eraDelta(house, 2866));
        assertEquals(-2, ChaosEmployerForceRating.eraDelta(house, 3038));
        assertEquals(-1, ChaosEmployerForceRating.eraDelta(house, 3039));
        assertEquals(-1, ChaosEmployerForceRating.eraDelta(house, 3048));
        assertEquals(0, ChaosEmployerForceRating.eraDelta(house, 3049));
    }

    @Test
    void eraDeltaIsZeroForClansInEveryPeriod() {
        assertEquals(0, ChaosEmployerForceRating.eraDelta(clan(), 2866));
        assertEquals(0, ChaosEmployerForceRating.eraDelta(clan(), 3038));
    }

    // --- playerScalingDelta ---

    @Test
    void playerScalingDeltaFavorsWeakPlayerAlliesAndWeakensEnemies() {
        // Employer/ally side: weaker player -> stronger allies (positive).
        assertEquals(0, ChaosEmployerForceRating.playerScalingDelta(true, SkillLevel.REGULAR));
        assertEquals(1, ChaosEmployerForceRating.playerScalingDelta(true, SkillLevel.GREEN));
        assertEquals(-2, ChaosEmployerForceRating.playerScalingDelta(true, SkillLevel.ELITE));
        // Enemy side: weaker player -> weaker enemies (mirror image).
        assertEquals(-1, ChaosEmployerForceRating.playerScalingDelta(false, SkillLevel.GREEN));
        assertEquals(2, ChaosEmployerForceRating.playerScalingDelta(false, SkillLevel.ELITE));
    }

    // --- resolveSkill ---

    @Test
    void resolveSkillBaselineIsRegularForAModernMajorHouse() {
        SkillLevel skill = ChaosEmployerForceRating.resolveSkill(majorHouse(), false, true, RENAISSANCE_END_YEAR,
              ContractImportance.STANDARD, false, SkillLevel.REGULAR);
        assertEquals(SkillLevel.REGULAR, skill);
    }

    @Test
    void resolveSkillRisesWithImportanceAndAttackerEdge() {
        // +2 importance (CRITICAL) -> ELITE; the attacker's +1 on top -> HEROIC.
        assertEquals(SkillLevel.ELITE, ChaosEmployerForceRating.resolveSkill(majorHouse(), false, true,
              RENAISSANCE_END_YEAR,
              ContractImportance.CRITICAL, false, SkillLevel.REGULAR));
        assertEquals(SkillLevel.HEROIC, ChaosEmployerForceRating.resolveSkill(majorHouse(), true, true,
              RENAISSANCE_END_YEAR,
              ContractImportance.CRITICAL, false, SkillLevel.REGULAR));
    }

    @Test
    void resolveSkillFallsForMinorImportance() {
        assertEquals(SkillLevel.GREEN, ChaosEmployerForceRating.resolveSkill(majorHouse(), false, true,
              RENAISSANCE_END_YEAR,
              ContractImportance.MINOR, false, SkillLevel.REGULAR));
    }

    @Test
    void resolveSkillClampsClansToAtLeastVeteran() {
        // Clan +2, MINOR -1, employer-side scaling against a Legendary player (3-7 = -4): net -3 would land below
        // Veteran, but the Clan floor pins it at Veteran.
        SkillLevel skill = ChaosEmployerForceRating.resolveSkill(clan(), false, true, RENAISSANCE_END_YEAR,
              ContractImportance.MINOR, true, SkillLevel.LEGENDARY);
        assertEquals(SkillLevel.VETERAN, skill);
    }

    // --- resolveEquipment ---

    @Test
    void resolveEquipmentBaselineIsDragoonCForAModernMajorHouse() {
        assertEquals(DragoonRating.DRAGOON_C.getRating(),
              ChaosEmployerForceRating.resolveEquipment(majorHouse(),
                    RENAISSANCE_END_YEAR,
                    ContractImportance.STANDARD));
    }

    @Test
    void resolveEquipmentRisesForClansAndImportance() {
        assertEquals(DragoonRating.DRAGOON_A.getRating(),
              ChaosEmployerForceRating.resolveEquipment(clan(), RENAISSANCE_END_YEAR, ContractImportance.STANDARD));
        // Clan +2 and CRITICAL +2 pushes past A and clamps at A*.
        assertEquals(DragoonRating.DRAGOON_ASTAR.getRating(),
              ChaosEmployerForceRating.resolveEquipment(clan(), RENAISSANCE_END_YEAR, ContractImportance.CRITICAL));
    }

    @Test
    void resolveEquipmentClampsPiratesAtDragoonF() {
        assertEquals(DragoonRating.DRAGOON_F.getRating(),
              ChaosEmployerForceRating.resolveEquipment(pirate(), RENAISSANCE_END_YEAR, ContractImportance.STANDARD));
        // Already at the floor; MINOR importance can't push below it.
        assertEquals(DragoonRating.DRAGOON_F.getRating(),
              ChaosEmployerForceRating.resolveEquipment(pirate(), RENAISSANCE_END_YEAR, ContractImportance.MINOR));
    }

    // --- determine (composition) ---

    @Test
    void determineComposesSkillAndEquipment() {
        // Major house, attacker, STRATEGIC (+1): skill 0+1+1 = ELITE; equipment C(2)+1 = B(3).
        ForceRating rating = ChaosEmployerForceRating.determine(majorHouse(), true, true, RENAISSANCE_END_YEAR,
              ContractImportance.STRATEGIC, false, SkillLevel.REGULAR);
        assertEquals(SkillLevel.ELITE, rating.forceSkill());
        assertEquals(DragoonRating.DRAGOON_B.getRating(), rating.equipmentRating());
    }
}
