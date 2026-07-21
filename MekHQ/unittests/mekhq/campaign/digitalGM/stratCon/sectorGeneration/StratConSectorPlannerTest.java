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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorPlanner.ALTERNATE_FORMATIONS_PER_SECTOR;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorPlanner.MAXIMUM_SECTORS;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorPlanner.REGIMENTAL_FORMATIONS_PER_SECTOR;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorPlanner.generateSectorSpecs;
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

    /** A per-sector team ceiling high enough never to bind, for the tests that are about the count rules alone. */
    private static final int UNLIMITED = Integer.MAX_VALUE;

    private static int totalTeams(List<SectorSpec> specs) {
        return specs.stream().mapToInt(SectorSpec::requiredLances).sum();
    }

    private static void assertEveryLatitudeAssigned(List<SectorSpec> specs) {
        assertFalse(specs.isEmpty());
        specs.forEach(spec -> assertNotNull(spec.latitudeBand()));
    }

    // ---- Legacy layout (alternate count off) ----

    @Test
    void legacy_evenlyDivisible_producesOneSectorPerThreeTeams() {
        List<SectorSpec> specs = generateSectorSpecs(9, StratConSectorCountMethod.LEGACY, UNLIMITED);

        assertEquals(3, specs.size());
        specs.forEach(spec -> {
            assertEquals(LEGACY_LANCES_PER_SECTOR, spec.requiredLances());
        });
        assertEveryLatitudeAssigned(specs);
    }

    @Test
    void legacy_withRemainder_finalSectorHoldsRemainderLances() {
        List<SectorSpec> specs = generateSectorSpecs(10, StratConSectorCountMethod.LEGACY, UNLIMITED);

        // 3 full sectors of 3 lances, plus 1 sector of the remaining 1 lance
        assertEquals(4, specs.size());
        assertEquals(3, specs.stream().filter(spec -> spec.requiredLances() == LEGACY_LANCES_PER_SECTOR).count());
        assertEquals(1, specs.stream().filter(spec -> spec.requiredLances() == 1).count());
    }

    @Test
    void legacy_fewerThanThreeTeams_producesSingleSector() {
        List<SectorSpec> specs = generateSectorSpecs(2, StratConSectorCountMethod.LEGACY, UNLIMITED);

        assertEquals(1, specs.size());
        assertEquals(2, specs.get(0).requiredLances());
    }

    @Test
    void legacy_zeroTeams_producesFallbackSector() {
        List<SectorSpec> specs = generateSectorSpecs(0, StratConSectorCountMethod.LEGACY, UNLIMITED);

        assertEquals(1, specs.size());
        assertEquals(1, specs.get(0).requiredLances());
    }

    @Test
    void regimental_aContractUpToARegimentFightsOverOneSector() {
        // 45 lances is a five-battalion regiment; anything up to it rounds to a single sector.
        List<SectorSpec> specs = generateSectorSpecs(45, StratConSectorCountMethod.REGIMENTAL, UNLIMITED);

        assertEquals(1, specs.size());
        assertEquals(45, specs.getFirst().requiredLances(), "the one sector should hold the whole regiment");
    }

    @Test
    void regimental_producesOneSectorPerRegiment() {
        // 135 lances is three regiments, so three sectors of 45.
        List<SectorSpec> specs = generateSectorSpecs(135, StratConSectorCountMethod.REGIMENTAL, UNLIMITED);

        assertEquals(3, specs.size());
        assertEquals(135, totalTeams(specs));
        specs.forEach(spec -> assertEquals(45, spec.requiredLances()));
    }

    @Test
    void regimental_roundsToNearestRegiment() {
        // 22 / 45 = 0.49 -> 1 sector; 23 / 45 = 0.51 -> 1 sector; 68 / 45 = 1.51 -> 2 sectors.
        assertEquals(1, generateSectorSpecs(22, StratConSectorCountMethod.REGIMENTAL, UNLIMITED).size());
        assertEquals(1, generateSectorSpecs(23, StratConSectorCountMethod.REGIMENTAL, UNLIMITED).size());
        assertEquals(2, generateSectorSpecs(68, StratConSectorCountMethod.REGIMENTAL, UNLIMITED).size());
    }

    @Test
    void regimental_sharesEveryTeamOutAcrossItsSectors() {
        List<SectorSpec> specs = generateSectorSpecs(100, StratConSectorCountMethod.REGIMENTAL, UNLIMITED);

        assertEquals(100, totalTeams(specs), "no team should be dropped by the split");
    }

    @Test
    void everyImprovedMethodOverflowsRatherThanExceedTheSizeCeiling() {
        // The point of the threshold: no sector may front more teams than its planet's ground will hold, so a wet or
        // oversized world splits the contract further instead of letting the ceiling clip each sector's map.
        int maximumTeamsPerSector = 5;

        for (StratConSectorCountMethod method : StratConSectorCountMethod.values()) {
            if (method == StratConSectorCountMethod.LEGACY) {
                continue;
            }

            List<SectorSpec> specs = generateSectorSpecs(100, method, maximumTeamsPerSector);

            assertEquals(100, totalTeams(specs), method + " dropped teams while overflowing");
            specs.forEach(spec -> assertTrue(spec.requiredLances() <= maximumTeamsPerSector,
                  method + " left a sector fronting " + spec.requiredLances() + " teams, over the ceiling of " +
                        maximumTeamsPerSector));
        }
    }

    @Test
    void legacyIgnoresTheSizeCeiling() {
        // Legacy sizing is a flat 28 hexes a team and never approaches the ceiling, so its historical layout stands.
        List<SectorSpec> specs = generateSectorSpecs(9, StratConSectorCountMethod.LEGACY, 1);

        assertEquals(3, specs.size());
        specs.forEach(spec -> assertEquals(LEGACY_LANCES_PER_SECTOR, spec.requiredLances()));
    }

    @Test
    void regimental_neverProducesZeroSectors() {
        // A contract requiring no combat teams still needs somewhere to fight.
        List<SectorSpec> specs = generateSectorSpecs(0, StratConSectorCountMethod.REGIMENTAL, UNLIMITED);

        assertEquals(1, specs.size());
        assertTrue(specs.getFirst().requiredLances() >= 1);
    }

    @Test
    void alternate_evenlyDivisible_producesOneSectorPerNineTeams() {
        List<SectorSpec> specs = generateSectorSpecs(9, StratConSectorCountMethod.ALTERNATE, UNLIMITED);

        assertEquals(1, specs.size());
        assertEquals(ALTERNATE_FORMATIONS_PER_SECTOR, specs.get(0).requiredLances());
    }

    @Test
    void alternate_roundsToNearestSector() {
        // 13 / 9 = 1.44 -> 1 sector; 14 / 9 = 1.56 -> 2 sectors
        assertEquals(1, generateSectorSpecs(13, StratConSectorCountMethod.ALTERNATE, UNLIMITED).size());
        assertEquals(2, generateSectorSpecs(14, StratConSectorCountMethod.ALTERNATE, UNLIMITED).size());
    }

    @Test
    void alternate_atCap_isNotCondensed() {
        List<SectorSpec> specs = generateSectorSpecs(90, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        assertEquals(MAXIMUM_SECTORS, specs.size());
    }

    @Test
    void alternate_condenseOff_exceedsCap() {
        List<SectorSpec> specs = generateSectorSpecs(135, StratConSectorCountMethod.ALTERNATE, UNLIMITED);

        assertEquals(15, specs.size());
    }

    // ---- Condensed layout (alternate count and condense on) ----

    @Test
    void condensed_fifteenSectorsWorthOfForceFitsIntoTen() {
        // 135 teams would make fifteen sectors uncondensed; condensed they share out across ten, 13-14 teams apiece.
        List<SectorSpec> specs = generateSectorSpecs(135, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        assertEquals(MAXIMUM_SECTORS, specs.size());
        assertEquals(135, totalTeams(specs), "condensing should redistribute the teams, not drop them");
        specs.forEach(spec -> assertTrue((spec.requiredLances() == 13) || (spec.requiredLances() == 14),
              "expected an even 13-14 team split, got " + spec.requiredLances()));
    }

    @Test
    void condensed_yieldsTheTenSectorCapRatherThanExceedARegimentPerSector() {
        // Ten sectors hold 450 teams at a regiment apiece. Past that the regiment limit outranks the sector cap,
        // because an over-regimental sector asks for more ground than the area ceiling grants and loses the rest.
        List<SectorSpec> specs = generateSectorSpecs(500, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        assertTrue(specs.size() > MAXIMUM_SECTORS, "expected the sector cap to yield, got " + specs.size());
        assertEquals(500, totalTeams(specs), "no team should be dropped when the cap yields");
        specs.forEach(spec -> assertTrue(spec.requiredLances() <= REGIMENTAL_FORMATIONS_PER_SECTOR,
              "a sector fronted " + spec.requiredLances() + " teams, more than a regiment"));
    }

    @Test
    void condensed_holdsAtTenSectorsRightUpToTheRegimentLimit() {
        // 450 teams is exactly ten regiments, so the cap should still hold at ten.
        List<SectorSpec> specs = generateSectorSpecs(450, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        assertEquals(MAXIMUM_SECTORS, specs.size());
        specs.forEach(spec -> assertEquals(REGIMENTAL_FORMATIONS_PER_SECTOR, spec.requiredLances()));
    }

    @Test
    void condensed_neverExceedsTheSectorCap() {
        // 225 / 9 = 25 sectors uncondensed
        List<SectorSpec> specs = generateSectorSpecs(225, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        assertEquals(MAXIMUM_SECTORS, specs.size());
    }

    @Test
    void condensed_sharesEveryTeamOutAcrossTheSectors() {
        // Condensing must not lose or invent teams: the contract's whole force is still accounted for.
        List<SectorSpec> specs = generateSectorSpecs(225, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        assertEquals(225, totalTeams(specs), "condensing should redistribute the teams, not drop them");
    }

    @Test
    void condensed_spreadsTheExtraEvenlyRatherThanEnlargingTheLeadingSectors() {
        // Previously the surplus piled onto the earlier sectors, leaving some enormous and the rest ordinary. Sharing
        // it out means no sector differs from another by more than the one team an integer split cannot divide.
        List<SectorSpec> specs = generateSectorSpecs(225, StratConSectorCountMethod.CONDENSED, UNLIMITED);

        int smallest = specs.stream().mapToInt(SectorSpec::requiredLances).min().orElseThrow();
        int largest = specs.stream().mapToInt(SectorSpec::requiredLances).max().orElseThrow();

        assertTrue((largest - smallest) <= 1,
              "sectors should be within one team of each other, but ranged " + smallest + " to " + largest);
    }

    @Test
    void condensed_staysEvenAcrossManyForceSizes() {
        for (int desired = MAXIMUM_SECTORS + 1; desired <= 40; desired++) {
            int teams = desired * ALTERNATE_FORMATIONS_PER_SECTOR;
            List<SectorSpec> specs = generateSectorSpecs(teams, StratConSectorCountMethod.CONDENSED, UNLIMITED);

            assertEquals(MAXIMUM_SECTORS, specs.size());
            assertEquals(teams, totalTeams(specs), "every team should still be accounted for at " + teams);

            int smallest = specs.stream().mapToInt(SectorSpec::requiredLances).min().orElseThrow();
            int largest = specs.stream().mapToInt(SectorSpec::requiredLances).max().orElseThrow();
            assertTrue((largest - smallest) <= 1, "uneven split at " + teams + " teams");
            assertTrue(smallest >= 1, "every sector needs at least one team");
        }
    }
}
