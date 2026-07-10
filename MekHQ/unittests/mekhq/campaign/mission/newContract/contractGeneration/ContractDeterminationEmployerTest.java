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
package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.WORD_OF_BLAKE_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ContractDeterminationEmployerTest {
    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);

    private static Faction governmentFaction() {
        Faction faction = mock(Faction.class);
        when(faction.isPirate()).thenReturn(false);
        when(faction.isMercenary()).thenReturn(false);
        return faction;
    }

    // ---- GOVERNMENT branch (deterministic) --------------------------------------------------

    @Test
    public void testGovernment_MajorPower_EmploysItself() {
        Faction campaignFaction = governmentFaction();
        when(campaignFaction.isMajorPower()).thenReturn(true);

        EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
              mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

        assertSame(campaignFaction, selection.employerFaction());
        assertEquals(GlobalEmployerTableValue.MAJOR_POWER, selection.globalEmployerTableValue());
        assertNull(selection.independentEmployerTableValue());
    }

    @Test
    public void testGovernment_MinorPower() {
        Faction campaignFaction = governmentFaction();
        when(campaignFaction.isMinorPower()).thenReturn(true);

        EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
              mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

        assertSame(campaignFaction, selection.employerFaction());
        assertEquals(GlobalEmployerTableValue.MINOR_POWER, selection.globalEmployerTableValue());
        assertNull(selection.independentEmployerTableValue());
    }

    @Test
    public void testGovernment_SuperPower() {
        Faction campaignFaction = governmentFaction();
        when(campaignFaction.isSuperPower()).thenReturn(true);

        EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
              mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

        assertEquals(GlobalEmployerTableValue.SUPER_POWER, selection.globalEmployerTableValue());
    }

    @Test
    public void testGovernment_NoPowerClass_DefaultsToIndependent() {
        // A government faction that answers false to every power predicate falls through to INDEPENDENT.
        Faction campaignFaction = governmentFaction();

        EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
              mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

        assertSame(campaignFaction, selection.employerFaction());
        assertEquals(GlobalEmployerTableValue.INDEPENDENT, selection.globalEmployerTableValue());
        assertNull(selection.independentEmployerTableValue());
    }

    // ---- Special employer overrides ---------------------------------------------------------

    @Test
    public void testMercenary_ComStarOverride() {
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);

        Faction comStar = mock(Faction.class);
        when(comStar.getEndYear()).thenReturn(9999);
        when(comStar.isMajorPower()).thenReturn(true);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(COMSTAR_FACTION_CODE)).thenReturn(comStar);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> computeStatic = mockStatic(Compute.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);
            // COMSTAR_EMPLOYER_CHANCE roll of 0 selects ComStar as the employer.
            computeStatic.when(() -> Compute.randomInt(100)).thenReturn(0);

            EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
                  mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

            assertSame(comStar, selection.employerFaction());
            assertEquals(GlobalEmployerTableValue.MAJOR_POWER, selection.globalEmployerTableValue());
            assertNull(selection.independentEmployerTableValue());
        }
    }

    @Test
    public void testPirate_ComStarOverride_RoutesThroughMercenaryMethod() {
        // A pirate campaign also runs the mercenary method, so the ComStar override applies here too.
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isPirate()).thenReturn(true);

        Faction comStar = mock(Faction.class);
        when(comStar.getEndYear()).thenReturn(9999);
        when(comStar.isSuperPower()).thenReturn(true);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(COMSTAR_FACTION_CODE)).thenReturn(comStar);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> computeStatic = mockStatic(Compute.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);
            computeStatic.when(() -> Compute.randomInt(100)).thenReturn(0);

            EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
                  mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

            assertSame(comStar, selection.employerFaction());
            assertEquals(GlobalEmployerTableValue.SUPER_POWER, selection.globalEmployerTableValue());
        }
    }

    @Test
    public void testMercenary_WordOfBlakeOverride_WhenComStarDeclines() {
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);

        Faction comStar = mock(Faction.class);
        when(comStar.getEndYear()).thenReturn(9999);

        Faction wordOfBlake = mock(Faction.class);
        when(wordOfBlake.getStartYear()).thenReturn(2788);
        when(wordOfBlake.getEndYear()).thenReturn(9999);
        when(wordOfBlake.isMajorPower()).thenReturn(true);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(COMSTAR_FACTION_CODE)).thenReturn(comStar);
        when(factions.getFaction(WORD_OF_BLAKE_FACTION_CODE)).thenReturn(wordOfBlake);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> computeStatic = mockStatic(Compute.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);
            // ComStar declines (non-zero roll), Word of Blake accepts (roll of 0).
            computeStatic.when(() -> Compute.randomInt(100)).thenReturn(5);
            computeStatic.when(() -> Compute.randomInt(40)).thenReturn(0);

            EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
                  mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

            assertSame(wordOfBlake, selection.employerFaction());
            assertEquals(GlobalEmployerTableValue.MAJOR_POWER, selection.globalEmployerTableValue());
        }
    }

    // ---- Fall-through to the mercenary employer table ---------------------------------------

    @Test
    public void testMercenary_FallsThroughToEmployerTable() {
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);

        // Neither special employer is operating in 3025, so no override fires and randomInt is never consulted.
        Faction comStar = mock(Faction.class);
        when(comStar.getEndYear()).thenReturn(2000);
        Faction wordOfBlake = mock(Faction.class);
        when(wordOfBlake.getStartYear()).thenReturn(3100);
        when(wordOfBlake.getEndYear()).thenReturn(9999);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(COMSTAR_FACTION_CODE)).thenReturn(comStar);
        when(factions.getFaction(WORD_OF_BLAKE_FACTION_CODE)).thenReturn(wordOfBlake);

        Faction generatedEmployer = mock(Faction.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        // GREAT hiring hall (employerModifier +2) + d6(2)=6 -> roll 8 -> MAJOR_POWER band.
        when(generator.getRandomEmployerFaction(any(ILocation.class), any(LocalDate.class),
              eq(GlobalEmployerTableValue.MAJOR_POWER), eq(true))).thenReturn(generatedEmployer);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> computeStatic = mockStatic(Compute.class);
              MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            computeStatic.when(() -> Compute.d6(2)).thenReturn(6);

            EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
                  mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.GREAT, 0.0);

            assertSame(generatedEmployer, selection.employerFaction());
            assertEquals(GlobalEmployerTableValue.MAJOR_POWER, selection.globalEmployerTableValue());
            // A non-INDEPENDENT global roll means no independent employer is generated.
            assertNull(selection.independentEmployerTableValue());
        }
    }

    @Test
    public void testMercenary_IndependentRoll_ResolvesSecondaryGlobalEmployer() {
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);

        // Neither special employer is operating in 3025.
        Faction comStar = mock(Faction.class);
        when(comStar.getEndYear()).thenReturn(2000);
        Faction wordOfBlake = mock(Faction.class);
        when(wordOfBlake.getStartYear()).thenReturn(3100);
        when(wordOfBlake.getEndYear()).thenReturn(9999);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(COMSTAR_FACTION_CODE)).thenReturn(comStar);
        when(factions.getFaction(WORD_OF_BLAKE_FACTION_CODE)).thenReturn(wordOfBlake);

        Faction generatedEmployer = mock(Faction.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        // The independent roll picked an "override" type, so the faction search uses the SECONDARY global roll.
        when(generator.getRandomEmployerFaction(any(ILocation.class), any(LocalDate.class),
              eq(GlobalEmployerTableValue.MAJOR_POWER), eq(true))).thenReturn(generatedEmployer);

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class);
              MockedStatic<Compute> computeStatic = mockStatic(Compute.class);
              MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            factionsStatic.when(Factions::getInstance).thenReturn(factions);
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            // NONE hiring hall applies employerModifier -2, so each roll is d6(2) - 2:
            //   1) global roll:      6 -> 4  -> GlobalEmployerTableValue.INDEPENDENT
            //   2) independent roll: 6 -> 4  -> IndependentEmployerTableValue.PLANETARY_GOVERNMENT (an override)
            //   3) secondary global: 12 -> 10 -> GlobalEmployerTableValue.MAJOR_POWER (used only for the search)
            computeStatic.when(() -> Compute.d6(2)).thenReturn(6, 6, 12);

            EmployerFactionSelection selection = ContractDeterminationEmployer.getEmployerFactionSelectionData(
                  mock(ILocation.class), 0, campaignFaction, DATE, HiringHallLevel.NONE, 0.0);

            assertSame(generatedEmployer, selection.employerFaction());
            // The stored global/independent types reflect the FIRST two rolls, not the secondary search type.
            assertEquals(GlobalEmployerTableValue.INDEPENDENT, selection.globalEmployerTableValue());
            assertEquals(IndependentEmployerTableValue.PLANETARY_GOVERNMENT,
                  selection.independentEmployerTableValue());
        }
    }
}
