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
package mekhq.campaign.digitalGM.stratCon.generation;

import static mekhq.campaign.digitalGM.stratCon.generation.StratConSectorPlanner.ALTERNATE_FORMATIONS_PER_SECTOR;
import static mekhq.campaign.digitalGM.stratCon.generation.StratConSectorPlanner.MAXIMUM_SECTORS;
import static mekhq.campaign.digitalGM.stratCon.generation.StratConSectorPlanner.generateSectorSpecs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConSectorPlanner}.
 */
class StratConSectorPlannerTest {
    private static final int LEGACY_LANCES_PER_SECTOR = StratConContractInitializer.NUM_FORMATIONS_PER_TRACK;

    private static long countWithUnits(List<SectorSpec> specs, int units) {
        return specs.stream().filter(spec -> spec.unitCount() == units).count();
    }

    private static int totalUnits(List<SectorSpec> specs) {
        return specs.stream().mapToInt(SectorSpec::unitCount).sum();
    }

    private static void assertEveryLatitudeAssigned(List<SectorSpec> specs) {
        assertFalse(specs.isEmpty());
        specs.forEach(spec -> assertNotNull(spec.latitudeBand()));
    }

    // ---- Legacy layout (alternate count off) ----

    @Test
    void legacy_evenlyDivisible_producesOneSectorPerThreeTeams() {
        List<SectorSpec> specs = generateSectorSpecs(9, false, false);

        assertEquals(3, specs.size());
        specs.forEach(spec -> {
            assertEquals(1, spec.unitCount());
            assertEquals(LEGACY_LANCES_PER_SECTOR, spec.requiredLances());
        });
        assertEveryLatitudeAssigned(specs);
    }

    @Test
    void legacy_withRemainder_finalSectorHoldsRemainderLances() {
        List<SectorSpec> specs = generateSectorSpecs(10, false, false);

        // 3 full sectors of 3 lances, plus 1 sector of the remaining 1 lance
        assertEquals(4, specs.size());
        assertEquals(3, specs.stream().filter(spec -> spec.requiredLances() == LEGACY_LANCES_PER_SECTOR).count());
        assertEquals(1, specs.stream().filter(spec -> spec.requiredLances() == 1).count());
    }

    @Test
    void legacy_fewerThanThreeTeams_producesSingleSector() {
        List<SectorSpec> specs = generateSectorSpecs(2, false, false);

        assertEquals(1, specs.size());
        assertEquals(2, specs.get(0).requiredLances());
    }

    @Test
    void legacy_zeroTeams_producesFallbackSector() {
        List<SectorSpec> specs = generateSectorSpecs(0, false, false);

        assertEquals(1, specs.size());
        assertEquals(1, specs.get(0).requiredLances());
        assertEquals(1, specs.get(0).unitCount());
    }

    @Test
    void legacy_ignoresCondenseFlag() {
        // 135 teams would legacy-generate 45 sectors; condense must not cap them when alternate count is off.
        List<SectorSpec> specs = generateSectorSpecs(135, false, true);

        assertEquals(45, specs.size());
        specs.forEach(spec -> assertEquals(1, spec.unitCount()));
    }

    // ---- Alternate count (condense off) ----

    @Test
    void alternate_evenlyDivisible_producesOneSectorPerNineTeams() {
        List<SectorSpec> specs = generateSectorSpecs(9, true, false);

        assertEquals(1, specs.size());
        assertEquals(ALTERNATE_FORMATIONS_PER_SECTOR, specs.get(0).requiredLances());
        assertEquals(1, specs.get(0).unitCount());
    }

    @Test
    void alternate_roundsToNearestSector() {
        // 13 / 9 = 1.44 -> 1 sector; 14 / 9 = 1.56 -> 2 sectors
        assertEquals(1, generateSectorSpecs(13, true, false).size());
        assertEquals(2, generateSectorSpecs(14, true, false).size());
    }

    @Test
    void alternate_atCap_isNotCondensed() {
        List<SectorSpec> specs = generateSectorSpecs(90, true, true);

        assertEquals(MAXIMUM_SECTORS, specs.size());
        specs.forEach(spec -> assertEquals(1, spec.unitCount()));
    }

    @Test
    void alternate_condenseOff_exceedsCap() {
        List<SectorSpec> specs = generateSectorSpecs(135, true, false);

        assertEquals(15, specs.size());
        specs.forEach(spec -> assertEquals(1, spec.unitCount()));
    }

    // ---- Condensed layout (alternate count and condense on) ----

    @Test
    void condensed_fifteenSectors_splitsIntoFiveDoubleAndFiveSingle() {
        List<SectorSpec> specs = generateSectorSpecs(135, true, true); // 135 / 9 = 15 desired

        assertEquals(MAXIMUM_SECTORS, specs.size());
        assertEquals(5, countWithUnits(specs, 2));
        assertEquals(5, countWithUnits(specs, 1));
        assertEquals(15, totalUnits(specs));
    }

    @Test
    void condensed_twentyFiveSectors_splitsIntoFiveTripleAndFiveDouble() {
        List<SectorSpec> specs = generateSectorSpecs(225, true, true); // 225 / 9 = 25 desired

        assertEquals(MAXIMUM_SECTORS, specs.size());
        assertEquals(5, countWithUnits(specs, 3));
        assertEquals(5, countWithUnits(specs, 2));
        assertEquals(25, totalUnits(specs));
    }

    @Test
    void condensed_scalesRequiredLancesWithUnitCount() {
        List<SectorSpec> specs = generateSectorSpecs(225, true, true);

        specs.forEach(spec -> assertEquals(spec.unitCount() * ALTERNATE_FORMATIONS_PER_SECTOR, spec.requiredLances()));
    }

    @Test
    void condensed_leadingSectorsAreLarger() {
        List<SectorSpec> specs = generateSectorSpecs(135, true, true);

        // The uneven extra unit goes to the earlier ("older") sectors.
        assertEquals(2, specs.get(0).unitCount());
        assertEquals(1, specs.get(specs.size() - 1).unitCount());
    }

    @Test
    void condensed_preservesTotalAreaAcrossManyForceSizes() {
        for (int desired = MAXIMUM_SECTORS + 1; desired <= 40; desired++) {
            int teams = desired * ALTERNATE_FORMATIONS_PER_SECTOR;
            List<SectorSpec> specs = generateSectorSpecs(teams, true, true);

            assertEquals(MAXIMUM_SECTORS, specs.size());
            assertEquals(desired, totalUnits(specs), "total units should equal the uncondensed desired count");
            assertTrue(specs.stream().allMatch(spec -> spec.unitCount() >= 1));
        }
    }
}
