/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import mekhq.campaign.*;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ContractMarketAtBGenerationTests {

    public static List<Arguments> generateData() {
        final List<Arguments> arguments = new ArrayList<>();
        for (final int gameYear : Arrays.asList(2750, 3025, 3055, 3067, 3120)) {
            for (int rating = IUnitRating.DRAGOON_F; rating <= IUnitRating.DRAGOON_ASTAR; rating++) {
                arguments.add(Arguments.of(gameYear, rating, false));
                arguments.add(Arguments.of(gameYear, rating, true));
            }
        }
        return arguments;
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithoutRetainerAtBContractSucceeds(final int gameYear, final int unitRating,
                                                          final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        assertNotNull(new AtbMonthlyContractMarket().addAtBContract(campaign));
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithoutRetainerMinorPowerAtBContractSucceeds(final int gameYear,
                                                                    final int unitRating,
                                                                    final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(false);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithoutRetainerEmployerNeutralAtBContractSucceeds(final int gameYear,
                                                                         final int unitRating,
                                                                         final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithoutRetainerEmployerNeutralAtWarAtBContractSucceeds(final int gameYear,
                                                                              final int unitRating,
                                                                              final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        doReturn(true).when(hints).isAtWarWith(eq(employerFaction), eq(enemyFaction), any());
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void mercEmployerRetries(final int gameYear, final int unitRating,
                                    final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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

        Faction mercenary = mock(Faction.class);
	when(mercenary.isMercenary()).thenReturn(true);
        doReturn(mercenary).when(factions).getFaction(eq("MERC"));

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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
        assertTrue(contract.isMercSubcontract());
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void mercEmployerRetriesFail(final int gameYear, final int unitRating,
                                        final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        Factions factions = mock(Factions.class);
        Factions.setInstance(factions);

	// Make a fake "MERC" employer for getFaction purposes in the rfg
        Faction employerFaction = mock(Faction.class);
        when(employerFaction.isMercenary()).thenReturn(true);

        doReturn(employerFaction).when(factions).getFaction(eq("MERC"));

        RandomFactionGenerator rfg = mock(RandomFactionGenerator.class);
        RandomFactionGenerator.setInstance(rfg);
        // Return "MERC" every time
        when(rfg.getEmployer()).thenReturn("MERC");

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void mercMissiongTargetRetries(final int gameYear, final int unitRating,
                                          final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void mercMissionTargetRetriesFail(final int gameYear, final int unitRating,
                                             final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void mercJumpPathRetries(final int gameYear, final int unitRating,
                                    final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        // Fail to find a jump path at first, kicking off a retry
        doReturn(null).doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void mercJumpPathFails(final int gameYear, final int unitRating,
                                  final boolean isClanEnemy) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        // Fail to find a jump path
        doReturn(null).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithRetainerAtBContractSucceeds(final int gameYear, final int unitRating,
                                                       final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithRetainerMinorPowerAtBContractSucceeds(final int gameYear,
                                                                 final int unitRating,
                                                                 final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(false);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithRetainerEmployerNeutralAtBContractSucceeds(final int gameYear,
                                                                      final int unitRating,
                                                                      final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void addMercWithRetainerEmployerNeutralAtWarAtBContractSucceeds(final int gameYear,
                                                                           final int unitRating,
                                                                           final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn("MERC");
        when(campaign.getRetainerEmployerCode()).thenReturn(employer);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        doReturn(true).when(hints).isAtWarWith(eq(employerFaction), eq(enemyFaction), any());
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void nonMercAtBContractSucceeds(final int gameYear, final int unitRating,
                                           final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(false);
        when(campaignFaction.getShortName()).thenReturn(employer);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void nonMercMinorPowerAtBContractSucceeds(final int gameYear, final int unitRating,
                                                     final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(false);
        when(campaignFaction.getShortName()).thenReturn(employer);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(false);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(false).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void nonMercNeutralAtBContractSucceeds(final int gameYear, final int unitRating,
                                                  final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(false);
        when(campaignFaction.getShortName()).thenReturn(employer);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @ParameterizedTest
    @MethodSource(value = "generateData")
    public void nonMercNeutralAtWarAtBContractSucceeds(final int gameYear, final int unitRating,
                                                       final boolean isClanEnemy) {
        String employer = "EMPLOYER";
        Campaign campaign = mock(Campaign.class);
        when(campaign.getFactionCode()).thenReturn(employer);
        when(campaign.getRetainerEmployerCode()).thenReturn(null);
        when(campaign.getAtBUnitRatingMod()).thenReturn(unitRating);
        when(campaign.getLocalDate()).thenReturn(LocalDate.ofYearDay(gameYear, 1));
        when(campaign.getGameYear()).thenReturn(gameYear);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(false);
        when(campaignFaction.getShortName()).thenReturn(employer);
        when(campaign.getFaction()).thenReturn(campaignFaction);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isVariableContractLength()).thenReturn(false);
        when(campaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.FLD_MAN_MERCS_REV);
        when(campaignOptions.isUsePeacetimeCost()).thenReturn(false);
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
        when(employerFaction.isISMajorOrSuperPower()).thenReturn(true);
        when(enemyFaction.isISMajorOrSuperPower()).thenReturn(true);
        doReturn(true).when(hints).isNeutral(eq(employerFaction));
        doReturn(false).when(hints).isNeutral(eq(enemyFaction));
        doReturn(true).when(hints).isAtWarWith(eq(employerFaction), eq(enemyFaction), any());
        when(rfg.getFactionHints()).thenReturn(hints);

        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getJumps()).thenReturn(1);
        when(jumpPath.getFirstSystem()).thenReturn(currentSystem);
        when(jumpPath.getLastSystem()).thenReturn(targetSystem);
        doReturn(10.0).when(jumpPath).getTotalTime(any(), anyDouble());
        doReturn(jumpPath).when(campaign).calculateJumpPath(eq(currentSystem), eq(targetSystem));
        doReturn(Money.of(1)).when(campaign).calculateCostPerJump(anyBoolean(), anyBoolean());

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        AtBContract contract = market.addAtBContract(campaign);
        assertNotNull(contract);
    }

    @AfterAll
    public static void cleanupAfterTests() {
        Factions.setInstance(null);
        Systems.setInstance(null);
        RandomFactionGenerator.setInstance(null);
    }
}
