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
package mekhq.campaign;

import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.ProcurementEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.location.LocationNewDayUtil;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.TestSystems;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.PlanetaryAcquisitionModifiers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Characterization tests for what happens after the acquisition target number is known: a Locust LCT-1V has lost its
 * medium laser, the player orders a replacement through the shopping list, and the day's shopping either finds it
 * (the laser is paid for, travels, arrives and becomes a spare the repair can use) or fails (the order waits).
 *
 * <p>The order is made the way the Acquisitions dialog makes it, from the missing part's acquisition part. Shopping is
 * run by calling {@link Campaign#goShopping} and storing the list it returns, and arrival by
 * {@link LocationNewDayUtil#processAllLocationUnits}, which are the two steps a new day takes; the calendar itself is
 * not advanced. The acquisition roll and the transit roll go through MegaMek's {@code Compute}, so each case fixes
 * them with {@link FixedDieRolls}.</p>
 *
 * <p>Options are set explicitly: any tech may shop, standard (not planetary) acquisition, parts are paid for, a failed
 * roll waits 7 days, and transit is counted in months.</p>
 */
class AcquisitionCharacterizationTest {
    private static final int WAITING_PERIOD_DAYS = 7;
    private static final Money STARTING_FUNDS = Money.of(1_000_000);
    private static final int OUT_OF_REACH_TARGET = 13;
    private static final String OUT_OF_REACH_REPORT = "can reach the target number for";
    private static final String NO_CONTACT_REPORT = "is unable to find contacts for";

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private Person tech;
    private MissingPart missingLaser;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.ACQUISITIONS_TYPE, AcquisitionsType.ANY_TECH);
        options.set(CampaignOption.USE_PLANETARY_ACQUISITION, false);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.WAITING_PERIOD, WAITING_PERIOD_DAYS);
        options.set(CampaignOption.MAX_ACQUISITIONS, 0);
        options.set(CampaignOption.UNIT_TRANSIT_TIME, CampaignOptions.TRANSIT_UNIT_MONTH);
        campaign.getPlayerForce().getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(),
              STARTING_FUNDS, "test funds");

        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(EXP_REGULAR);
        mediumLaser().remove(false);
        missingLaser = PartsScenario.unitParts(locust, MissingPart.class).getFirst();
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private EquipmentPart mediumLaser() {
        return equipment("Medium Laser");
    }

    private EquipmentPart equipment(String name) {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if (name.equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Locust LCT-1V fixture has no " + name);
    }

    /**
     * Takes one of the Locust's machine guns off and orders a replacement the way the Acquisitions dialog does.
     */
    private IAcquisitionWork orderReplacementMachineGun() {
        equipment("Machine Gun").remove(false);
        for (MissingPart missingPart : PartsScenario.unitParts(locust, MissingPart.class)) {
            if ("Machine Gun".equals(missingPart.getName())) {
                IAcquisitionWork order = missingPart.getAcquisitionPart().getAcquisitionWork();
                shoppingList().addShoppingItem(order, 1, campaign);
                return order;
            }
        }
        throw new IllegalStateException("Removing the machine gun left no missing machine gun");
    }

    /**
     * Shops by planet, in the current system only, without the per-planet report.
     *
     * <p>The test campaign's systems are loaded without being indexed by location, so the search would find no
     * system at all; they are indexed here first.</p>
     */
    private void shopByPlanetInTheCurrentSystem() {
        TestSystems systems = TestSystems.getInstance();
        assertTrue(systems.getSystems().containsValue(campaign.getCurrentSystem()),
              "the test campaign's systems are the TestSystems instance");
        systems.indexLoadedSystems();

        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.USE_PLANETARY_ACQUISITION, true);
        options.set(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION, 0);
        options.set(CampaignOption.PLANET_ACQUISITION_VERBOSE, false);
    }

    /**
     * Orders one replacement for the missing laser the way the Acquisitions dialog does.
     */
    private IAcquisitionWork orderReplacementLaser() {
        IAcquisitionWork order = missingLaser.getAcquisitionPart().getAcquisitionWork();
        shoppingList().addShoppingItem(order, 1, campaign);
        return order;
    }

    private ForceShoppingList shoppingList() {
        return campaign.getPlayerForce().getShoppingList();
    }

    private void shopForTheDayWithEveryDieShowing(int face) {
        FixedDieRolls.everyDieShows(face);
        ForceShoppingList remainingItems = campaign.goShopping(shoppingList());
        campaign.getPlayerForce().setShoppingList(remainingItems);
    }

    private Money balance() {
        return campaign.getPlayerForce().getFinances().getBalance();
    }

    /**
     * @return the target number {@code procurer} needs for {@code order}, including the current system's modifiers
     *       when shopping by planet
     */
    private int targetNumberFor(Person procurer, IAcquisitionWork order) {
        return PlanetaryAcquisitionModifiers.addForCurrentSystem(campaign, order,
              campaign.checkAcquisition(order, procurer)).getTargetNumber().getValue();
    }

    /**
     * Raises the order's target number through the tech-base acquisition penalty until {@code procurer} needs a 13,
     * which no roll can reach. Both penalties are set, so the one applied does not depend on the laser's tech base.
     */
    private void putOrderOutOfReachOf(Person procurer, IAcquisitionWork order) {
        putOrdersOutOfReachOf(procurer, order);
    }

    /**
     * Raises every order's target number by the same tech-base acquisition penalty until {@code procurer} needs at
     * least a 13 for each of them; the easiest of them needs exactly 13.
     */
    private void putOrdersOutOfReachOf(Person procurer, IAcquisitionWork... orders) {
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.CLAN_ACQUISITION_PENALTY, 0);
        options.set(CampaignOption.IS_ACQUISITION_PENALTY, 0);
        int easiestTargetNumber = Integer.MAX_VALUE;
        for (IAcquisitionWork order : orders) {
            easiestTargetNumber = Math.min(easiestTargetNumber, targetNumberFor(procurer, order));
        }
        assertTrue(easiestTargetNumber < OUT_OF_REACH_TARGET, "the procurer must start within reach");

        int penalty = OUT_OF_REACH_TARGET - easiestTargetNumber;
        options.set(CampaignOption.CLAN_ACQUISITION_PENALTY, penalty);
        options.set(CampaignOption.IS_ACQUISITION_PENALTY, penalty);
        easiestTargetNumber = Integer.MAX_VALUE;
        for (IAcquisitionWork order : orders) {
            easiestTargetNumber = Math.min(easiestTargetNumber, targetNumberFor(procurer, order));
        }
        assertEquals(OUT_OF_REACH_TARGET, easiestTargetNumber);
    }

    /**
     * Adds an ultra-green tech who shops before the regular one, and puts the order out of their reach while leaving
     * it within the regular tech's.
     */
    private Person withUltraGreenTechWhoCannotReach(IAcquisitionWork order) {
        Person ultraGreenTech = scenario.withTech(EXP_ULTRA_GREEN);
        putOrderOutOfReachOf(ultraGreenTech, order);
        assertTrue(targetNumberFor(tech, order) < OUT_OF_REACH_TARGET, "the regular tech must still be within reach");

        List<Person> shoppingOrder = campaign.getPlayerForce().getHumanResources()
                                           .getLogisticsPersonnel(campaign.getCampaignOptions(),
                                                 campaign.getPlayerForce().isClanForce(), campaign.getLocalDate());
        assertEquals(List.of(ultraGreenTech, tech), shoppingOrder, "procurers shop worst first");
        return ultraGreenTech;
    }

    private boolean acquisitionsReportContains(String text) {
        for (String report : campaign.getDailyReportLog().getLines(ACQUISITIONS)) {
            if (report.contains(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Command Center's shopping list redraws only when it hears a procurement event. Ordering a part that is
     * already on the list adds to the existing line, and that must still announce the change, or the Command Center
     * keeps showing the old quantity until the next day (MekHQ issue 4997).
     */
    @Test
    void orderingAPartAlreadyOnTheListAnnouncesTheChange() {
        IAcquisitionWork firstOrder = orderReplacementLaser();

        try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class, CALLS_REAL_METHODS)) {
            IAcquisitionWork secondOrder = missingLaser.getAcquisitionPart().getAcquisitionWork();
            shoppingList().addShoppingItem(secondOrder, 3, campaign);

            mekHQ.verify(() -> MekHQ.triggerEvent(isA(ProcurementEvent.class)), times(1));
        }
        assertEquals(1, shoppingList().getShoppingList().size());
        assertEquals(4, firstOrder.getQuantity());
    }

    @Test
    void orderingTheSameReplacementTwiceMergesIntoOneLine() {
        IAcquisitionWork firstOrder = orderReplacementLaser();
        orderReplacementLaser();

        List<IAcquisitionWork> lines = shoppingList().getShoppingList();
        assertEquals(1, lines.size());
        assertSame(firstOrder, lines.getFirst());
        assertEquals(2, firstOrder.getQuantity());
        // Placing an order buys nothing by itself; that waits for the day's shopping
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(STARTING_FUNDS, balance());
    }

    @Test
    void successfulRollPaysForTheLaserAndPutsItInTransit() {
        IAcquisitionWork order = orderReplacementLaser();
        Money price = order.getBuyCost();

        // A roll of 12 finds the laser; the transit roll of 6 sets the delivery time
        shopForTheDayWithEveryDieShowing(6);

        assertTrue(shoppingList().getShoppingList().isEmpty());
        assertEquals(Map.of("Medium Laser" + PartsCensus.IN_TRANSIT_TAG, 1),
              PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(STARTING_FUNDS.minus(price), balance());
        assertEquals(1, tech.getAcquisitions());
        // (7 + transit roll 6 + the laser's availability) / 4 comes to three months, counted in days from today
        LocalDate today = campaign.getLocalDate();
        assertEquals(ChronoUnit.DAYS.between(today, today.plusMonths(3)), deliveredLaser().getDaysToArrival());
        assertFalse(missingLaser.isReplacementAvailable());
    }

    @Test
    void laserInTransitArrivesAsASpareTheRepairCanUse() {
        orderReplacementLaser();
        shopForTheDayWithEveryDieShowing(6);
        Part laserInTransit = deliveredLaser();
        int transitDays = laserInTransit.getDaysToArrival();

        for (int day = 1; day < transitDays; day++) {
            LocationNewDayUtil.processAllLocationUnits(campaign);
        }
        assertEquals(1, laserInTransit.getDaysToArrival());
        assertFalse(missingLaser.isReplacementAvailable());

        LocationNewDayUtil.processAllLocationUnits(campaign);

        assertEquals(Map.of("Medium Laser", 1), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertTrue(deliveredLaser().isPresent());
        assertTrue(deliveredLaser().isBrandNew());
        assertTrue(missingLaser.isReplacementAvailable());
    }

    @Test
    void failedRollKeepsTheOrderWaitingForTheWaitingPeriod() {
        IAcquisitionWork order = orderReplacementLaser();

        // A roll of 2 fails any possible target
        shopForTheDayWithEveryDieShowing(1);

        assertSame(order, shoppingList().getShoppingList().getFirst());
        assertEquals(1, order.getQuantity());
        assertEquals(WAITING_PERIOD_DAYS, order.getDaysToWait());
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(STARTING_FUNDS, balance());
        assertEquals(1, tech.getAcquisitions());

        // The next day only counts the wait down; even a roll of 12 is not attempted
        shopForTheDayWithEveryDieShowing(6);

        assertEquals(WAITING_PERIOD_DAYS - 1, order.getDaysToWait());
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(1, tech.getAcquisitions());
    }

    @Test
    void procurerWhoCannotReachTheTargetSpendsNoAttemptAndLeavesTheOrderToTheNext() {
        IAcquisitionWork order = orderReplacementLaser();
        Person ultraGreenTech = withUltraGreenTechWhoCannotReach(order);
        Money price = order.getBuyCost();

        // A roll of 12 cannot meet the ultra-green tech's 13, but finds the laser for the regular tech after them
        shopForTheDayWithEveryDieShowing(6);

        assertTrue(shoppingList().getShoppingList().isEmpty());
        assertEquals(Map.of("Medium Laser" + PartsCensus.IN_TRANSIT_TAG, 1),
              PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(STARTING_FUNDS.minus(price), balance());
        assertEquals(0, ultraGreenTech.getAcquisitions());
        assertEquals(1, tech.getAcquisitions());
        assertFalse(acquisitionsReportContains(OUT_OF_REACH_REPORT));
    }

    @Test
    void failedRollByTheNextProcurerStartsTheWaitWithoutTheOutOfReachReport() {
        IAcquisitionWork order = orderReplacementLaser();
        Person ultraGreenTech = withUltraGreenTechWhoCannotReach(order);

        // A roll of 2 fails the regular tech's target too; it is their failed roll that starts the wait
        shopForTheDayWithEveryDieShowing(1);

        assertSame(order, shoppingList().getShoppingList().getFirst());
        assertEquals(WAITING_PERIOD_DAYS, order.getDaysToWait());
        assertEquals(0, ultraGreenTech.getAcquisitions());
        assertEquals(1, tech.getAcquisitions());
        assertFalse(acquisitionsReportContains(OUT_OF_REACH_REPORT));
    }

    @Test
    void orderNoProcurerCanReachWaitsForTheWaitingPeriodWithoutSpendingAttempts() {
        IAcquisitionWork order = orderReplacementLaser();
        putOrderOutOfReachOf(tech, order);

        // Not even a roll of 12 can meet a 13, so no roll is made
        shopForTheDayWithEveryDieShowing(6);

        assertSame(order, shoppingList().getShoppingList().getFirst());
        assertEquals(1, order.getQuantity());
        assertEquals(WAITING_PERIOD_DAYS, order.getDaysToWait());
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(STARTING_FUNDS, balance());
        assertEquals(0, tech.getAcquisitions());
        assertTrue(acquisitionsReportContains(OUT_OF_REACH_REPORT));

        // The next day only counts the wait down, as after any failed roll
        shopForTheDayWithEveryDieShowing(6);

        assertEquals(WAITING_PERIOD_DAYS - 1, order.getDaysToWait());
        assertEquals(0, tech.getAcquisitions());
    }

    /**
     * The regular tech could reach the machine gun, but spends their one search of the day on the laser first. The
     * ultra-green tech before them could reach neither, yet that does not make the machine gun out of reach: it was
     * simply not tried, so it is tried again tomorrow instead of being reported and made to wait.
     */
    @Test
    void orderTheProcurerWhoCouldReachItHadNoTimeForIsNotReportedOutOfReach() {
        IAcquisitionWork laserOrder = orderReplacementLaser();
        IAcquisitionWork machineGunOrder = orderReplacementMachineGun();
        Person ultraGreenTech = scenario.withTech(EXP_ULTRA_GREEN);
        putOrdersOutOfReachOf(ultraGreenTech, laserOrder, machineGunOrder);
        assertTrue(targetNumberFor(ultraGreenTech, laserOrder) >= OUT_OF_REACH_TARGET);
        assertTrue(targetNumberFor(ultraGreenTech, machineGunOrder) >= OUT_OF_REACH_TARGET);
        assertTrue(targetNumberFor(tech, laserOrder) < OUT_OF_REACH_TARGET, "the regular tech can reach the laser");
        assertTrue(targetNumberFor(tech, machineGunOrder) < OUT_OF_REACH_TARGET,
              "the regular tech can reach the machine gun");
        campaign.getCampaignOptions().set(CampaignOption.MAX_ACQUISITIONS, 1);
        assertEquals(List.of(laserOrder, machineGunOrder), shoppingList().getShoppingList());
        assertEquals(List.of(ultraGreenTech, tech), campaign.getPlayerForce().getHumanResources()
                                                          .getLogisticsPersonnel(campaign.getCampaignOptions(),
                                                                campaign.getPlayerForce().isClanForce(),
                                                                campaign.getLocalDate()),
              "procurers shop worst first");

        // A roll of 12 finds the laser for the regular tech, who then has no search left for the machine gun
        shopForTheDayWithEveryDieShowing(6);

        assertEquals(List.of(machineGunOrder), shoppingList().getShoppingList());
        assertEquals(0, ultraGreenTech.getAcquisitions());
        assertEquals(1, tech.getAcquisitions());
        assertFalse(acquisitionsReportContains(OUT_OF_REACH_REPORT));
        assertTrue(machineGunOrder.getDaysToWait() <= 0, "an order nobody had time for is due again tomorrow");
    }

    /**
     * Shopping by planet, the search in the current system is the same one standard shopping makes, with the
     * planet's modifiers added; this guards that the planetary cases below actually search there.
     */
    @Test
    void planetarySearchFindsTheLaserWithinReach() {
        shopByPlanetInTheCurrentSystem();
        IAcquisitionWork order = orderReplacementLaser();
        assertTrue(targetNumberFor(tech, order) < OUT_OF_REACH_TARGET, "the tech must be within reach here");

        shopForTheDayWithEveryDieShowing(6);

        assertTrue(shoppingList().getShoppingList().isEmpty());
        assertEquals(Map.of("Medium Laser" + PartsCensus.IN_TRANSIT_TAG, 1),
              PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(1, tech.getAcquisitions());
    }

    @Test
    void planetaryOrderNoProcurerCanReachIsReportedAndWaitsWithoutSpendingAttempts() {
        shopByPlanetInTheCurrentSystem();
        IAcquisitionWork order = orderReplacementLaser();
        putOrderOutOfReachOf(tech, order);

        // Not even a roll of 12 can meet a 13 for the contact, so nothing is rolled for
        shopForTheDayWithEveryDieShowing(6);

        assertSame(order, shoppingList().getShoppingList().getFirst());
        assertEquals(1, order.getQuantity());
        assertEquals(WAITING_PERIOD_DAYS, order.getDaysToWait());
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
        assertEquals(STARTING_FUNDS, balance());
        assertEquals(0, tech.getAcquisitions());
        assertTrue(acquisitionsReportContains(OUT_OF_REACH_REPORT));
        // the per-planet report is off, so the summary is the only line about it
        assertFalse(acquisitionsReportContains(NO_CONTACT_REPORT));
    }

    @Test
    void verbosePlanetaryReportStillShowsTheContactFailingAtTheUnreachableTargetNumber() {
        shopByPlanetInTheCurrentSystem();
        campaign.getCampaignOptions().set(CampaignOption.PLANET_ACQUISITION_VERBOSE, true);
        IAcquisitionWork order = orderReplacementLaser();
        putOrderOutOfReachOf(tech, order);

        shopForTheDayWithEveryDieShowing(6);

        assertTrue(acquisitionsReportContains(NO_CONTACT_REPORT));
        assertTrue(acquisitionsReportContains("at TN: " + OUT_OF_REACH_TARGET));
        assertTrue(acquisitionsReportContains(OUT_OF_REACH_REPORT));
        assertEquals(0, tech.getAcquisitions());
    }

    /**
     * @return the one medium laser in the warehouse that is not installed on the Locust
     */
    private Part deliveredLaser() {
        List<Part> spareParts = scenario.getWarehouse().getSpareParts();
        assertEquals(1, spareParts.size(), "expected exactly one laser outside the Locust");
        return spareParts.getFirst();
    }
}
