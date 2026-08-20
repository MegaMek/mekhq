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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractEmployerDetermination.EmployerFactions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Covers the flavor/anchor/sponsor resolution in {@link ChaosContractEmployerDetermination#determineEmployerFactions}
 * &mdash; in particular the rebel-sponsor rule: rebels always remain the visible employer, and a ComStar/Word of Blake
 * patron that would otherwise take over the contract instead becomes their covert backer on a mercenary search.
 *
 * <p>The faction singletons, the random faction generator, and the dice are all stubbed so these tests are fully
 * deterministic and need no loaded universe.</p>
 */
class ChaosContractEmployerDeterminationTest {

    private static final int YEAR = 3050;
    private static final LocalDate DATE = LocalDate.of(YEAR, 1, 1);

    private static Faction namedFaction(String shortName) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(shortName);
        return faction;
    }

    /**
     * Stubs the {@link Factions} singleton with the fixed factions the resolution paths look up by code, plus operating
     * years for the ComStar/Word of Blake patrons so their override windows are open in {@link #YEAR}.
     */
    private static void stubFactions(MockedStatic<Factions> factionsStatic, Faction rebels, Faction house,
          Faction comStar, Faction wordOfBlake) {
        Factions factions = mock(Factions.class);
        factionsStatic.when(Factions::getInstance).thenReturn(factions);

        when(factions.getFaction("REB")).thenReturn(rebels);
        when(factions.getFaction("HOUSE")).thenReturn(house);

        when(factions.getFaction("CS")).thenReturn(comStar);
        when(comStar.getEndYear()).thenReturn(3200);

        when(factions.getFaction("WOB")).thenReturn(wordOfBlake);
        when(wordOfBlake.getStartYear()).thenReturn(2788);
        when(wordOfBlake.getEndYear()).thenReturn(3200);
    }

    /** A location whose current system is controlled by the HOUSE faction, so the anchor resolves without a generator. */
    private static ILocation locationOwnedByHouse() {
        PlanetarySystem system = mock(PlanetarySystem.class);
        // getFactions is mutated by Collections.shuffle downstream, so it must be a mutable list.
        when(system.getFactions(any())).thenReturn(new ArrayList<>(List.of("HOUSE")));

        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(system);
        return location;
    }

    @Test
    void rebelsOnAMercenarySearchAreBackedByAComStarPatron() {
        Faction rebels = namedFaction("REB");
        Faction house = namedFaction("HOUSE");
        Faction comStar = namedFaction("CS");
        Faction wordOfBlake = namedFaction("WOB");

        // The generator singleton is deliberately left unmocked: a current-system owner resolves the anchor, so the
        // lazy regional fallback must never be reached (touching the real singleton here would fail the test).
        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            stubFactions(factionsStatic, rebels, house, comStar, wordOfBlake);
            // A roll of 0 opens the ComStar override, which is checked before Word of Blake.
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            EmployerFactions result = ChaosContractEmployerDetermination.determineEmployerFactions(
                  ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS, DATE, locationOwnedByHouse(), true);

            assertSame(rebels, result.flavor(), "rebels remain the visible employer");
            assertSame(house, result.anchor(), "the anchor is the local ruling power the rebels rise against");
            assertSame(comStar, result.sponsor(), "the patron becomes a covert sponsor, not the employer");
        }
    }

    @Test
    void rebelsOnAMercenarySearchWithoutAPatronRollHaveNoSponsor() {
        Faction rebels = namedFaction("REB");
        Faction house = namedFaction("HOUSE");
        Faction comStar = namedFaction("CS");
        Faction wordOfBlake = namedFaction("WOB");

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            stubFactions(factionsStatic, rebels, house, comStar, wordOfBlake);
            // A non-zero roll misses both the ComStar and Word of Blake override windows.
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);

            EmployerFactions result = ChaosContractEmployerDetermination.determineEmployerFactions(
                  ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS, DATE, locationOwnedByHouse(), true);

            assertSame(rebels, result.flavor());
            assertSame(house, result.anchor());
            assertNull(result.sponsor(), "no patron rolled means no sponsor");
        }
    }

    @Test
    void rebelsOutsideAMercenarySearchAreNeverSponsored() {
        Faction rebels = namedFaction("REB");
        Faction house = namedFaction("HOUSE");
        Faction comStar = namedFaction("CS");
        Faction wordOfBlake = namedFaction("WOB");

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            stubFactions(factionsStatic, rebels, house, comStar, wordOfBlake);

            EmployerFactions result = ChaosContractEmployerDetermination.determineEmployerFactions(
                  ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS, DATE, locationOwnedByHouse(), false);

            assertSame(rebels, result.flavor());
            assertSame(house, result.anchor());
            assertNull(result.sponsor(), "sponsorship is a mercenary-search-only interaction");
        }
    }

    @Test
    void aTerritorialEmployerUsesTheCurrentSystemOwnerAsBothFlavorAndAnchor() {
        Faction rebels = namedFaction("REB");
        Faction house = namedFaction("HOUSE");
        Faction comStar = namedFaction("CS");
        Faction wordOfBlake = namedFaction("WOB");

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            stubFactions(factionsStatic, rebels, house, comStar, wordOfBlake);

            EmployerFactions result = ChaosContractEmployerDetermination.determineEmployerFactions(
                  ChaosEmployerType.LOCAL_SYSTEM_OWNER, DATE, locationOwnedByHouse(), false);

            assertSame(house, result.flavor(), "a local system owner is the current system's controller");
            assertSame(house, result.anchor(), "a territorial employer anchors on itself");
            assertNull(result.sponsor());
        }
    }

    @Test
    void aComStarPatronFrontsACorporationFromTheShadows() {
        Faction rebels = namedFaction("REB");
        Faction house = namedFaction("HOUSE");
        Faction comStar = namedFaction("CS");
        Faction wordOfBlake = namedFaction("WOB");
        ILocation location = locationOwnedByHouse();

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            stubFactions(factionsStatic, rebels, house, comStar, wordOfBlake);
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            // Resolves the fronting corporation's flavor faction.
            when(generator.getRandomEmployerFaction(eq(location), eq(DATE), eq(true), any())).thenReturn(house);

            EmployerFactions result = ChaosContractEmployerDetermination.determineEmployerFactions(
                  ChaosEmployerType.CORPORATION, DATE, location, true);

            assertEquals(ChaosEmployerType.CORPORATION, result.type(),
                  "ComStar fronts a corporation, so the effective employer type is CORPORATION");
            assertSame(house, result.flavor(), "a corporation is the player-visible employer, never ComStar");
            assertSame(comStar, result.anchor(), "ComStar anchors the contract itself");
            assertSame(comStar, result.sponsor(), "ComStar bankrolls it from the shadows");
        }
    }

    @Test
    void aWordOfBlakePatronOpenlyTakesOverAsEmployer() {
        Faction rebels = namedFaction("REB");
        Faction house = namedFaction("HOUSE");
        Faction comStar = namedFaction("CS");
        Faction wordOfBlake = namedFaction("WOB");
        ILocation location = locationOwnedByHouse();

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            stubFactions(factionsStatic, rebels, house, comStar, wordOfBlake);
            // Close ComStar's override window so the Word of Blake override is the one that opens.
            when(comStar.getEndYear()).thenReturn(YEAR - 1);
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            when(generator.getRandomEmployerFaction(eq(location), eq(DATE), eq(true), any())).thenReturn(house);

            EmployerFactions result = ChaosContractEmployerDetermination.determineEmployerFactions(
                  ChaosEmployerType.CORPORATION, DATE, location, true);

            assertEquals(ChaosEmployerType.CORPORATION, result.type(), "Word of Blake keeps the rolled employer type");
            assertSame(wordOfBlake, result.flavor(), "Word of Blake takes over as the visible employer outright");
            assertSame(house, result.anchor(), "the conflict still sits inside a landed power near the player");
            assertNull(result.sponsor(), "an openly-employing patron is not a covert sponsor");
        }
    }
}
