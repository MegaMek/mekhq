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

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Disabled // All tests under this class rely on randomness, and thus doesn't work properly.
public class ContractMarketIntegrationTest {
    private static final int REASONABLE_GENERATION_ATTEMPTS = 3;

    private Campaign campaign;

    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
        try {
            Factions.setInstance(Factions.loadDefault());
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        Ranks.initializeRankSystems();
    }

    @BeforeEach
    public void beforeEach() {
        CampaignOptions options = new CampaignOptions();
        options.setUnitRatingMethod(UnitRatingMethod.NONE);

        campaign = new Campaign();
        campaign.setCampaignOptions(options);

        RandomFactionGenerator.getInstance().startup(campaign);

        fillHangar(campaign);
    }

    @Test
    public void addAtBContractMercsTest() {
        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        // Simulate clicking GM Add on the contract market three times
        for (int ii = 0; ii < REASONABLE_GENERATION_ATTEMPTS; ii++) {
            market.addContract(campaign);
        }

        assertFalse(market.getContracts().isEmpty());
    }

    @Test
    public void generateContractOffersMercsTest() {
        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        // Simulate three months of contract generation ...
        boolean foundContract = false;
        for (int ii = 0; ii < REASONABLE_GENERATION_ATTEMPTS; ii++) {
            market.generateContractOffers(campaign, true);

            // ... and one of these three should get us a contract!
            foundContract |= !market.getContracts().isEmpty();
        }

        assertTrue(foundContract);
    }

    @Test
    public void addAtBContractMercRetainerTest() {
        campaign.setRetainerEmployerCode("LA");

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        // Simulate clicking GM Add on the contract market three times
        for (int ii = 0; ii < 3; ii++) {
            market.addContract(campaign);
        }

        assertFalse(market.getContracts().isEmpty());
    }

    @Test
    public void generateContractOffersMercRetainerTest() {
        campaign.setRetainerEmployerCode("CS");

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        // Simulate three months of contract generation ...
        boolean foundContract = false;
        for (int ii = 0; ii < REASONABLE_GENERATION_ATTEMPTS; ii++) {
            market.generateContractOffers(campaign, true);

            // ... and one of these three should get us a contract!
            foundContract |= !market.getContracts().isEmpty();
        }

        assertTrue(foundContract);
    }

    @Test
    public void generateContractOffersMercSubcontractTest() {
        AtBContract existing = mock(AtBContract.class);
        when(existing.getId()).thenReturn(1);
        when(existing.getScenarios()).thenReturn(new ArrayList<>());
        when(existing.getContractType()).thenReturn(AtBContractType.GARRISON_DUTY);
        when(existing.getStatus()).thenReturn(MissionStatus.ACTIVE);
        when(existing.getEmployerCode()).thenReturn("FWL");
        when(existing.getEnemyCode()).thenReturn("CC");
        when(existing.getSystemId()).thenReturn("Sian");
        when(existing.getStartDate()).thenReturn(campaign.getLocalDate().minusDays(3000));
        when(existing.getEndingDate()).thenReturn(campaign.getLocalDate().plusDays(3000));
        when(existing.isActiveOn(campaign.getLocalDate(), false)).thenCallRealMethod();
        when(existing.getCommandRights()).thenReturn(ContractCommandRights.INDEPENDENT);
        campaign.importMission(existing);

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        SecureRandom realRng = new SecureRandom();
        MMRandom rng = mock(MMRandom.class);
        // Override and ensure we are guaranteed a sub-contract
        MMRoll roll1d6 = mock(MMRoll.class);
        when(roll1d6.getIntValue()).thenReturn(6);
        doReturn(roll1d6).when(rng).d6();
        doReturn(roll1d6).when(rng).d6(eq(1));
        MMRoll roll2d6 = mock(MMRoll.class);
        when(roll2d6.getIntValue()).thenReturn(12);
        doReturn(roll2d6).when(rng).d6(eq(2));
        // Keep the rest random
        doAnswer(inv -> {
            int max = inv.getArgument(0);
            return realRng.nextInt(max);
        }).when(rng).randomInt(anyInt());

        try {
            Compute.setRNG(rng);

            // Simulate three months of contract generation to get a sub contract ...
            boolean foundContract = false;
            for (int ii = 0; ii < REASONABLE_GENERATION_ATTEMPTS; ii++) {
                market.generateContractOffers(campaign, true);

                // ... and hopefully, one of these should get us a sub-contract! 3 of 12 chance.
                for (Contract c : market.getContracts()) {
                    foundContract |= (c instanceof AtBContract)
                            && (((AtBContract) c).getParentContract() == existing);
                }

                if (foundContract) {
                    break;
                }
            }

            assertTrue(foundContract);
        } finally {
            Compute.setRNG(MMRandom.R_DEFAULT);
        }
    }

    @Test
    public void addAtBContractHouseTest() {
        campaign.setFactionCode("DC");

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        // Simulate clicking GM Add on the contract market three times
        for (int ii = 0; ii < 3; ii++) {
            market.addContract(campaign);
        }

        assertFalse(market.getContracts().isEmpty());
    }

    @Test
    public void generateContractOffersHouseTest() {
        campaign.setFactionCode("FS");

        AtbMonthlyContractMarket market = new AtbMonthlyContractMarket();

        // Simulate three months of contract generation ...
        boolean foundContract = false;
        for (int ii = 0; ii < REASONABLE_GENERATION_ATTEMPTS; ii++) {
            market.generateContractOffers(campaign, true);

            // ... and one of these three should get us a contract!
            foundContract |= !market.getContracts().isEmpty();
        }

        assertTrue(foundContract);
    }

    private void fillHangar(Campaign campaign) {
        // Add 12 meks in 3 forces
        for (int jj = 0; jj < 3; ++jj) {
            Force force = new Force("Force " + jj);
            for (int ii = 0; ii < 4; ++ii) {
                Unit unit = createMek(campaign);
                force.addUnit(unit.getId());

                campaign.getHangar().addUnit(unit);
            }

            campaign.addForce(force, campaign.getForces());
        }
    }

    private Unit createMek(Campaign campaign) {
        Mek entity = mock(Mek.class);
        when(entity.getCrew()).thenReturn(new Crew(CrewType.SINGLE));
        when(entity.getTransportBays()).thenReturn(new Vector<>());
        Unit unit = new Unit(entity, campaign);
        unit.setId(UUID.randomUUID());
        unit.addPilotOrSoldier(createPilot());
        return unit;
    }

    private Person createPilot() {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        return person;
    }
}
