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
package mekhq.campaign.market.contractMarket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AbstractContractMarket#violatesHomeworldsExclusion}: outside Operation Bulldog, no non-Clan faction
 * should be able to target a system within 450 light years of Strana Mechty.
 */
class AbstractContractMarketHomeworldsExclusionTest {

    private static final LocalDate OUTSIDE_BULLDOG = LocalDate.of(3049, 12, 1);
    private static final LocalDate INSIDE_BULLDOG = LocalDate.of(3059, 6, 1);

    private final AbstractContractMarket contractMarket = new AtbMonthlyContractMarket();

    @AfterEach
    void tearDown() {
        Systems.setInstance(null);
    }

    private static PlanetarySystem mockStranaMechty() {
        PlanetarySystem stranaMechty = mock(PlanetarySystem.class);
        Systems systems = mock(Systems.class);
        when(systems.getSystemById("Strana Mechty")).thenReturn(stranaMechty);
        Systems.setInstance(systems);
        return stranaMechty;
    }

    private static Faction mockFaction(boolean isClan) {
        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(isClan);
        return faction;
    }

    private static AtBContract mockContract(Faction attackerFaction, double distanceToStranaMechty,
          PlanetarySystem stranaMechty, int travelDays) {
        AtBContract contract = mock(AtBContract.class);
        when(contract.isPlayerAttacker()).thenReturn(true);
        when(contract.getEmployerFaction()).thenReturn(attackerFaction);
        PlanetarySystem targetSystem = mock(PlanetarySystem.class);
        when(targetSystem.getDistanceTo(stranaMechty)).thenReturn(distanceToStranaMechty);
        when(contract.getSystem()).thenReturn(targetSystem);
        when(contract.getTravelDays(any())).thenReturn(travelDays);
        return contract;
    }

    private static Campaign mockCampaign(LocalDate currentDate) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(currentDate);
        return campaign;
    }

    @Test
    void nonClanAttackerWithinRadiusOutsideBulldogViolates() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        Campaign campaign = mockCampaign(OUTSIDE_BULLDOG);
        AtBContract contract = mockContract(mockFaction(false), 200, stranaMechty, 0);

        assertTrue(contractMarket.violatesHomeworldsExclusion(contract, campaign),
              "A non-Clan faction striking within the exclusion radius outside Operation Bulldog should violate "
                    + "the restriction");
    }

    @Test
    void nonClanAttackerWithinRadiusDuringBulldogIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        Campaign campaign = mockCampaign(INSIDE_BULLDOG);
        AtBContract contract = mockContract(mockFaction(false), 200, stranaMechty, 0);

        assertFalse(contractMarket.violatesHomeworldsExclusion(contract, campaign),
              "Operation Bulldog is the one historical window where non-Clan forces legitimately operated within "
                    + "the exclusion radius");
    }

    @Test
    void clanAttackerWithinRadiusOutsideBulldogIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        Campaign campaign = mockCampaign(OUTSIDE_BULLDOG);
        AtBContract contract = mockContract(mockFaction(true), 200, stranaMechty, 0);

        assertFalse(contractMarket.violatesHomeworldsExclusion(contract, campaign),
              "Clan factions are native to the Homeworlds and are never restricted by this rule");
    }

    @Test
    void nonClanAttackerOutsideRadiusOutsideBulldogIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        Campaign campaign = mockCampaign(OUTSIDE_BULLDOG);
        AtBContract contract = mockContract(mockFaction(false), 451, stranaMechty, 0);

        assertFalse(contractMarket.violatesHomeworldsExclusion(contract, campaign),
              "A target outside the exclusion radius is never restricted by this rule");
    }

    @Test
    void travelTimeThatPushesArrivalIntoBulldogWindowIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        // The current date is before Operation Bulldog starts, but travel time pushes the actual arrival date into
        // the window - the rule is keyed on arrival date, not the date the contract was generated.
        Campaign campaign = mockCampaign(MHQConstants.OPERATION_BULLDOG_START.minusDays(10));
        AtBContract contract = mockContract(mockFaction(false), 200, stranaMechty, 15);

        assertFalse(contractMarket.violatesHomeworldsExclusion(contract, campaign),
              "The restriction is keyed on the arrival date (current date plus travel time), not the date the "
                    + "contract was generated");
    }
}
