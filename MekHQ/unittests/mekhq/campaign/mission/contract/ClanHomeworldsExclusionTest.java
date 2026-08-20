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
package mekhq.campaign.mission.contract;

import static mekhq.MHQConstants.IS_INVASION_OF_HUNTRESS_END;
import static mekhq.MHQConstants.IS_INVASION_OF_HUNTRESS_START;
import static mekhq.campaign.mission.contract.contractGeneration.targetFinder.ClanHomeworldsExclusion.violatesHomeworldsExclusion;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the Clan Homeworlds exclusion zone: no Inner Sphere force may campaign within 450 light years of Strana
 * Mechty except during Task Force Serpent, and even then only against the Smoke Jaguars.
 *
 * <p>Strana Mechty is stubbed into the {@link Systems} singleton and the jump path is mocked, so no universe or map
 * fixtures are needed.</p>
 */
class ClanHomeworldsExclusionTest {
    private static final LocalDate OUTSIDE_SERPENT = IS_INVASION_OF_HUNTRESS_END.plusDays(1);
    private static final LocalDate INSIDE_SERPENT = IS_INVASION_OF_HUNTRESS_START.plusDays(1);

    /** Comfortably inside the 450 light year exclusion radius. */
    private static final double INSIDE_RADIUS = 200;
    /** Comfortably outside the 450 light year exclusion radius. */
    private static final double OUTSIDE_RADIUS = 451;

    private static final String SMOKE_JAGUAR = "CSJ";
    private static final String DRACONIS_COMBINE = "DC";
    private static final String WOLF = "CW";

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

    private static Faction mockFaction(boolean isClan, String shortName) {
        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(isClan);
        when(faction.getShortName()).thenReturn(shortName);
        return faction;
    }

    /**
     * A contract whose employer holds {@code employerObjective} and whose enemy holds {@code enemyObjective}, targeting
     * a system {@code distanceToStranaMechty} light years from the Clan capital.
     */
    private static AbstractContract mockContract(Faction employerFaction, ContractObjectiveType employerObjective,
          Faction enemyFaction, ContractObjectiveType enemyObjective, double distanceToStranaMechty,
          PlanetarySystem stranaMechty) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getEmployerFaction()).thenReturn(employerFaction);
        when(contract.getObjectiveType()).thenReturn(employerObjective);
        when(contract.getEnemyFaction()).thenReturn(enemyFaction);
        when(contract.getOpposingObjectiveType()).thenReturn(enemyObjective);

        PlanetarySystem targetSystem = mock(PlanetarySystem.class);
        when(targetSystem.getDistanceTo(stranaMechty)).thenReturn(distanceToStranaMechty);
        when(contract.getTargetSystem()).thenReturn(targetSystem);
        return contract;
    }

    /**
     * A campaign sitting at {@code currentDate} whose only jump path to anywhere takes {@code travelDays}. The
     * exclusion is keyed on the arrival date, so travel time has to be controllable.
     */
    private static Campaign mockCampaign(LocalDate currentDate, int travelDays) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(currentDate);

        PlayerForce playerForce = mock(PlayerForce.class);
        when(playerForce.isOverridingCommandCircuitRequirements()).thenReturn(false);
        when(playerForce.getFactionStandings()).thenReturn(mock(FactionStandings.class));
        when(campaign.getPlayerForce()).thenReturn(playerForce);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getTotalTime(any(), anyDouble(), anyBoolean())).thenReturn((double) travelDays);
        when(campaign.calculateJumpPath(any(), any())).thenReturn(jumpPath);
        return campaign;
    }

    private static CurrentLocation mockLocation() {
        CurrentLocation location = mock(CurrentLocation.class);
        when(location.getCurrentSystem()).thenReturn(mock(PlanetarySystem.class));
        return location;
    }

    /** An Inner Sphere invader striking a Clan defender - the arrangement the exclusion zone exists to police. */
    private static AbstractContract innerSphereInvasionOf(Faction defender, double distance,
          PlanetarySystem stranaMechty) {
        return mockContract(mockFaction(false, DRACONIS_COMBINE),
              ContractObjectiveType.PLANETARY_ASSAULT,
              defender,
              ContractObjectiveType.GARRISON_DUTY,
              distance,
              stranaMechty);
    }

    @Test
    void innerSphereAttackerWithinRadiusOutsideSerpentViolates() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR),
              INSIDE_RADIUS,
              stranaMechty);

        assertTrue(violatesHomeworldsExclusion(contract, mockCampaign(OUTSIDE_SERPENT, 0), mockLocation()),
              "An Inner Sphere force striking within the exclusion radius outside Task Force Serpent should violate "
                    + "the restriction");
    }

    @Test
    void innerSphereAttackerAgainstSmokeJaguarDuringSerpentIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR),
              INSIDE_RADIUS,
              stranaMechty);

        assertFalse(violatesHomeworldsExclusion(contract, mockCampaign(INSIDE_SERPENT, 0), mockLocation()),
              "Task Force Serpent is the one historical window where Inner Sphere forces legitimately operated "
                    + "within the exclusion radius");
    }

    @Test
    void innerSphereAttackerAgainstAnotherClanDuringSerpentStillViolates() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, WOLF), INSIDE_RADIUS, stranaMechty);

        assertTrue(violatesHomeworldsExclusion(contract, mockCampaign(INSIDE_SERPENT, 0), mockLocation()),
              "Task Force Serpent was mounted against the Smoke Jaguars specifically; it does not license attacks "
                    + "on other Clans");
    }

    @Test
    void clanOnClanFightWithinRadiusIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = mockContract(mockFaction(true, WOLF),
              ContractObjectiveType.PLANETARY_ASSAULT,
              mockFaction(true, SMOKE_JAGUAR),
              ContractObjectiveType.GARRISON_DUTY,
              INSIDE_RADIUS,
              stranaMechty);

        assertFalse(violatesHomeworldsExclusion(contract, mockCampaign(OUTSIDE_SERPENT, 0), mockLocation()),
              "Clan factions are native to the Homeworlds and are never restricted by this rule");
    }

    @Test
    void innerSphereDefenderAgainstClanAttackerWithinRadiusIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = mockContract(mockFaction(false, DRACONIS_COMBINE),
              ContractObjectiveType.GARRISON_DUTY,
              mockFaction(true, SMOKE_JAGUAR),
              ContractObjectiveType.PLANETARY_ASSAULT,
              INSIDE_RADIUS,
              stranaMechty);

        assertFalse(violatesHomeworldsExclusion(contract, mockCampaign(OUTSIDE_SERPENT, 0), mockLocation()),
              "The rule polices Inner Sphere forces reaching into the Homeworlds, not Clans attacking a garrison "
                    + "there");
    }

    @Test
    void innerSphereAttackerOutsideRadiusIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR),
              OUTSIDE_RADIUS,
              stranaMechty);

        assertFalse(violatesHomeworldsExclusion(contract, mockCampaign(OUTSIDE_SERPENT, 0), mockLocation()),
              "A target outside the exclusion radius is never restricted by this rule");
    }

    @Test
    void contractWithoutATargetSystemIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR),
              INSIDE_RADIUS,
              stranaMechty);
        when(contract.getTargetSystem()).thenReturn(null);

        assertFalse(violatesHomeworldsExclusion(contract, mockCampaign(OUTSIDE_SERPENT, 0), mockLocation()),
              "A contract with no target yet cannot be measured against the exclusion radius");
    }

    @Test
    void unknownStranaMechtyIsAllowed() {
        Systems systems = mock(Systems.class);
        when(systems.getSystemById(anyString())).thenReturn(null);
        Systems.setInstance(systems);

        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR), INSIDE_RADIUS, null);

        assertFalse(violatesHomeworldsExclusion(contract, mockCampaign(OUTSIDE_SERPENT, 0), mockLocation()),
              "Without Strana Mechty in the loaded universe there is no center to measure the exclusion zone from");
    }

    @Test
    void travelTimeThatPushesArrivalIntoSerpentWindowIsAllowed() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR),
              INSIDE_RADIUS,
              stranaMechty);
        // The current date is before Task Force Serpent starts, but travel time pushes the actual arrival date into
        // the window - the rule is keyed on arrival date, not the date the contract was generated.
        Campaign campaign = mockCampaign(IS_INVASION_OF_HUNTRESS_START.minusDays(10), 15);

        assertFalse(violatesHomeworldsExclusion(contract, campaign, mockLocation()),
              "The restriction is keyed on the arrival date (current date plus travel time), not the date the "
                    + "contract was generated");
    }

    @Test
    void travelTimeThatPushesArrivalPastSerpentWindowViolates() {
        PlanetarySystem stranaMechty = mockStranaMechty();
        AbstractContract contract = innerSphereInvasionOf(mockFaction(true, SMOKE_JAGUAR),
              INSIDE_RADIUS,
              stranaMechty);
        // Departing inside the window is not enough: by the time the force arrives, Serpent is over.
        Campaign campaign = mockCampaign(IS_INVASION_OF_HUNTRESS_END.minusDays(5), 30);

        assertTrue(violatesHomeworldsExclusion(contract, campaign, mockLocation()),
              "Departing during Task Force Serpent does not license an arrival after it has ended");
    }
}
