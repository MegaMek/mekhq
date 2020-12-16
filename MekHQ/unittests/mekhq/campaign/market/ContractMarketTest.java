/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.market;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

import org.junit.After;
import org.junit.Test;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Vector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ContractMarketTest {
    @Test
    public void addMercWithoutRetainerAtBContractSucceeds() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(IUnitRating.DRAGOON_C);
        when(campaign.getLocalDate()).thenReturn(LocalDate.now());

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.getVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.usePeacetimeCost()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        Accountant accountant = mock(Accountant.class);
        when(accountant.getContractBase()).thenReturn(Money.of(1));
        when(accountant.getOverheadExpenses()).thenReturn(Money.of(1));
        when(campaign.getAccountant()).thenReturn(accountant);

        Hangar hangar = mock(Hangar.class);
        doReturn(Money.of(1)).when(hangar).getUnitCosts(any(), any());
        when(campaign.getHangar()).thenReturn(hangar);

        Force forces = mock(Force.class);
        doReturn(new Vector<UUID>()).when(forces).getAllUnits(anyBoolean());
        when(campaign.getForces()).thenReturn(forces);

        Factions factions = mock(Factions.class);
        Factions.setInstance(factions);

        String employer = "EMPLOYER";
        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Systems systems = mock(Systems.class);
        Systems.setInstance(systems);

        String current = "CURRENT";
        PlanetarySystem currentSystem = mock(PlanetarySystem.class);
        when(currentSystem.getId()).thenReturn(current);
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);
        doReturn(currentSystem).when(systems).getSystemById(eq(current));
        doReturn(currentSystem).when(campaign).getSystemByName(eq(current));

        CurrentLocation currentLocation = mock(CurrentLocation.class);
        when(campaign.getLocation()).thenReturn(currentLocation);

        String missionTarget = "TARGET";
        PlanetarySystem targetSystem = mock(PlanetarySystem.class);
        when(targetSystem.getId()).thenReturn(missionTarget);
        doReturn(targetSystem).when(systems).getSystemById(eq(missionTarget));
        doReturn(targetSystem).when(campaign).getSystemByName(eq(missionTarget));

        RandomFactionGenerator rfg = mock(RandomFactionGenerator.class);
        RandomFactionGenerator.setInstance(rfg);
        when(rfg.getEmployer()).thenReturn(employer);
        doReturn(enemy).when(rfg).getEnemy(eq(employer), anyBoolean());
        doReturn(missionTarget).when(rfg).getMissionTarget(anyString(), anyString());

        FactionHints hints = mock(FactionHints.class);
        doReturn(true).when(hints).isISMajorPower(eq(employerFaction));
        doReturn(true).when(hints).isISMajorPower(eq(enemyFaction));
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        ContractMarket market = new ContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @After
    public void cleanupAfterTests() {
        Factions.setInstance(null);
        Systems.setInstance(null);
        RandomFactionGenerator.setInstance(null);
    }
}
