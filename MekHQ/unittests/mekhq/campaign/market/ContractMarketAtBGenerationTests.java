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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ContractMarketAtBGenerationTests {

    private final int gameYear;
    private final int unitRating;
    private final boolean isClanEnemy;

    public ContractMarketAtBGenerationTests(int gameYear, int unitRating, boolean isClanEnemy) {
        this.gameYear = gameYear;
        this.unitRating = unitRating;
        this.isClanEnemy = isClanEnemy;
    }

    @Parameters(name = "Run {index}: gameYear={0}, unitRating={1}, isClanEnemy={2}")
    public static Iterable<Object[]> data() throws Throwable {
        List<Integer> gameYears = Arrays.asList(new Integer[] { 2750, 3025, 3055, 3067, 3120 });
        
        List<Object[]> parameters = new ArrayList<>();
        for (int gameYear : gameYears) {
            for (int rating = IUnitRating.DRAGOON_F; rating <= IUnitRating.DRAGOON_ASTAR; ++rating) {
                parameters.add(new Object[] { gameYear, rating, false });
                parameters.add(new Object[] { gameYear, rating, true });
            }
        }
        return parameters;
    }

    @Test
    public void addMercWithoutRetainerAtBContractSucceeds() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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

    @Test
    public void addMercWithoutRetainerMinorPowerAtBContractSucceeds() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(false).when(hints).isISMajorPower(eq(employerFaction));
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

    @Test
    public void addMercWithoutRetainerEmployerNeutralAtBContractSucceeds() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
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

    @Test
    public void addMercWithoutRetainerEmployerNeutralAtWarAtBContractSucceeds() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        doReturn(true).when(hints).isAtWarWith(eq(employerFaction), eq(enemyFaction), any());
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

    @Test
    public void mercEmployerRetries() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));
        
        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        // Return "MERC" the first time to coerce a retry
        when(rfg.getEmployer()).thenReturn("MERC").thenReturn(employer);
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
        assertTrue(contract.isMercSubcontract());
    }

    @Test
    public void mercEmployerRetriesFail() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Factions factions = mock(Factions.class);
        Factions.setInstance(factions);

        RandomFactionGenerator rfg = mock(RandomFactionGenerator.class);
        RandomFactionGenerator.setInstance(rfg);
        // Return "MERC" every time
        when(rfg.getEmployer()).thenReturn("MERC");

        ContractMarket market = new ContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNull(contract);
    }

    @Test
    public void mercMissiongTargetRetries() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));
        
        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        // Don't find the mission target and force a retry
        doReturn(null).doReturn(missionTarget).when(rfg).getMissionTarget(anyString(), anyString());

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

    @Test
    public void mercMissionTargetRetriesFail() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));
        
        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        // Don't ever find the mission target and force a retry failure
        doReturn(null).when(rfg).getMissionTarget(anyString(), anyString());

        FactionHints hints = mock(FactionHints.class);
        doReturn(true).when(hints).isISMajorPower(eq(employerFaction));
        doReturn(true).when(hints).isISMajorPower(eq(enemyFaction));
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        ContractMarket market = new ContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNull(contract);
    }
    
    @Test
    public void mercJumpPathRetries() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));
        
        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        // Fail to find a jump path at first, kicking off a retry
        doReturn(null).doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        ContractMarket market = new ContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }
    
    @Test
    public void mercJumpPathFails() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));
        
        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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

        // Fail to find a jump path
        doReturn(null).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));

        ContractMarket market = new ContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNull(contract);
    }

    @Test
    public void addMercWithRetainerAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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

    @Test
    public void addMercWithRetainerMinorPowerAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(false).when(hints).isISMajorPower(eq(employerFaction));
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

    @Test
    public void addMercWithRetainerEmployerNeutralAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
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

    @Test
    public void addMercWithRetainerEmployerNeutralAtWarAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(enemy).when(rfg).getEnemy(eq(employer), anyBoolean());
        doReturn(missionTarget).when(rfg).getMissionTarget(anyString(), anyString());

        FactionHints hints = mock(FactionHints.class);
        doReturn(true).when(hints).isISMajorPower(eq(employerFaction));
        doReturn(true).when(hints).isISMajorPower(eq(enemyFaction));
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        doReturn(true).when(hints).isAtWarWith(eq(employerFaction), eq(enemyFaction), any());
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

    @Test
    public void nonMercAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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

    @Test
    public void nonMercMinorPowerAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(false).when(hints).isISMajorPower(eq(employerFaction));
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

    @Test
    public void nonMercNeutralAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
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

    @Test
    public void nonMercNeutralAtWarAtBContractSucceeds() {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

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

        String employerFullName = "Contract Employer";
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.getShortName()).thenReturn(employer);
        doReturn(employerFullName).when(employerFaction).getFullName(anyInt());
        doReturn(employerFaction).when(factions).getFaction(eq(employer));

        String enemy = "ENEMY";
        String enemyFullName = "Contract Enemy";
        Faction enemyFaction = mock(Faction.class);
        when(enemyFaction.getShortName()).thenReturn(enemy);
        when(enemyFaction.isClan()).thenReturn(isClanEnemy);
        doReturn(enemyFullName).when(employerFaction).getFullName(anyInt());
        doReturn(enemyFaction).when(factions).getFaction(eq(enemy));

        Faction pirates = mock(Faction.class);
        doReturn(pirates).when(factions).getFaction(eq("PIR"));

        Faction rebels = mock(Faction.class);
        doReturn(rebels).when(factions).getFaction(eq("REB"));

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
        doReturn(enemy).when(rfg).getEnemy(eq(employer), anyBoolean());
        doReturn(missionTarget).when(rfg).getMissionTarget(anyString(), anyString());

        FactionHints hints = mock(FactionHints.class);
        doReturn(true).when(hints).isISMajorPower(eq(employerFaction));
        doReturn(true).when(hints).isISMajorPower(eq(enemyFaction));
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        doReturn(true).when(hints).isAtWarWith(eq(employerFaction), eq(enemyFaction), any());
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
