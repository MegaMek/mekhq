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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.mission.contract.utilities.EmployerLostPlanet.isEmployerLosingControl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link EmployerLostPlanet#isEmployerLosingControl}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class EmployerLostPlanetTest {
    private static final String EMPLOYER = "FS";
    private static final String ENEMY = "CC";
    private static final LocalDate BEFORE = LocalDate.of(3028, 8, 19);
    private static final LocalDate AFTER = BEFORE.plusDays(1);

    private AbstractContract contract;
    private Planet planet;

    @BeforeEach
    void init() {
        contract = mock(AbstractContract.class);
        planet = mock(Planet.class);
        Faction employer = mock(Faction.class);

        when(employer.getShortName()).thenReturn(EMPLOYER);
        when(contract.getStandingEmployerFaction()).thenReturn(employer);
        when(contract.getTargetPlanet()).thenReturn(planet);
        when(contract.getStratConCampaignState()).thenReturn(mock(StratConCampaignState.class));
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
        when(contract.isPlayerAttacker()).thenReturn(false);
    }

    @Test
    void employerLosesPlanet() {
        when(planet.getFactions(BEFORE)).thenReturn(List.of(EMPLOYER));
        when(planet.getFactions(AFTER)).thenReturn(List.of(ENEMY));

        assertTrue(isEmployerLosingControl(contract, BEFORE, AFTER));
    }

    @Test
    void employerLosesSharedPlanet() {
        when(planet.getFactions(BEFORE)).thenReturn(List.of(EMPLOYER, ENEMY));
        when(planet.getFactions(AFTER)).thenReturn(List.of(ENEMY));

        assertTrue(isEmployerLosingControl(contract, BEFORE, AFTER));
    }

    @Test
    void employerStillOneOfSeveralOwners() {
        when(planet.getFactions(BEFORE)).thenReturn(List.of(EMPLOYER));
        when(planet.getFactions(AFTER)).thenReturn(List.of(ENEMY, EMPLOYER));

        assertFalse(isEmployerLosingControl(contract, BEFORE, AFTER));
    }

    @Test
    void employerKeepsPlanet() {
        when(planet.getFactions(BEFORE)).thenReturn(List.of(EMPLOYER));
        when(planet.getFactions(AFTER)).thenReturn(List.of(EMPLOYER));

        assertFalse(isEmployerLosingControl(contract, BEFORE, AFTER));
    }

    @Test
    void employerNeverHeldPlanet() {
        when(planet.getFactions(BEFORE)).thenReturn(List.of(ENEMY));
        when(planet.getFactions(AFTER)).thenReturn(List.of(ENEMY));

        assertFalse(isEmployerLosingControl(contract, BEFORE, AFTER));
    }

    @Test
    void attackingContractIgnored() {
        when(contract.isPlayerAttacker()).thenReturn(true);
        when(planet.getFactions(BEFORE)).thenReturn(List.of(EMPLOYER));
        when(planet.getFactions(AFTER)).thenReturn(List.of(ENEMY));

        assertFalse(isEmployerLosingControl(contract, BEFORE, AFTER));
    }

    @Test
    void contractWithoutStratConIgnored() {
        when(contract.getStratConCampaignState()).thenReturn(null);
        when(planet.getFactions(BEFORE)).thenReturn(List.of(EMPLOYER));
        when(planet.getFactions(AFTER)).thenReturn(List.of(ENEMY));

        assertFalse(isEmployerLosingControl(contract, BEFORE, AFTER));
    }
}
