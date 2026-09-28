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

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.location.LocationNewDayUtil;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Locust LCT-1V fixture has no Medium Laser");
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

    /**
     * @return the one medium laser in the warehouse that is not installed on the Locust
     */
    private Part deliveredLaser() {
        List<Part> spareParts = scenario.getWarehouse().getSpareParts();
        assertEquals(1, spareParts.size(), "expected exactly one laser outside the Locust");
        return spareParts.getFirst();
    }
}
