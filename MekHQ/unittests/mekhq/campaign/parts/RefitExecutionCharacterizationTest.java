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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Pins down what a refit does today to a unit, the warehouse, the procurement list and the campaign's money as it
 * begins, waits for its kit, completes, fails a check or is cancelled. Every scenario refits a Locust LCT-1V into an
 * LCT-1E in a real campaign with a real warehouse: the refit removes two Machine Guns and their ammo bin, moves the
 * Medium Laser, and needs one new Medium Laser and two Small Lasers.
 *
 * <p>These are characterization tests. They record current behaviour, including behaviour the refit audit found to
 * be wrong; such assertions are named after the audit finding and change when that finding is fixed.</p>
 *
 * <p>No roll decides anything here. The kit is delivered the way {@code Campaign.acquireEquipment} delivers a found
 * item, but with a fixed transit time instead of a rolled one, and the refit check result is chosen by calling
 * {@link Refit#succeed()} or {@link Refit#fail(int)} directly. {@code Campaign.refit} is not used because its skill
 * check rolls through MegaMek's {@code Compute}, not through the campaign dice.</p>
 */
class RefitExecutionCharacterizationTest {
    private static final Money STARTING_FUNDS = Money.of(1000000);
    /** Price of the LCT-1E kit: one Medium Laser and two Small Lasers, plus 10 percent for a kit. */
    private static final Money LOCUST_KIT_PRICE = Money.of(62500).multipliedBy(1.1);
    private static final int KIT_TRANSIT_DAYS = 3;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private SortedMap<String, Integer> locustPartsBeforeRefit;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_PARTS, true);
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.STARTING_CAPITAL, campaign.getLocalDate(), STARTING_FUNDS, "Test funds");
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        locustPartsBeforeRefit = PartsCensus.ofUnit(locust);
    }

    private Refit beginKitRefit() throws Exception {
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);
        refit.begin();
        return refit;
    }

    /**
     * Delivers the refit kit the way {@code Campaign.acquireEquipment} does once an acquisition roll succeeds, with a
     * fixed transit time, and then drops the fulfilled order the way the daily shopping pass does.
     */
    private void deliverKit(Refit refit, int transitDays) {
        refit.find(transitDays, 1.0);
        refit.changeQuantity(-1);
        campaign.getPlayerForce().getShoppingList().removeZeroQuantityFromList();
    }

    /** Lands every part still in transit, as the new-day processing does when its countdown reaches zero. */
    private void arriveAllParts() {
        List<Part> partsInTransit = new ArrayList<>();
        for (Part part : scenario.getWarehouse().getParts()) {
            if (!part.isPresent()) {
                partsInTransit.add(part);
            }
        }
        for (Part partInTransit : partsInTransit) {
            campaign.getQuartermaster().arrivePart(partInTransit);
        }
    }

    private Money balance() {
        return campaign.getPlayerForce().getFinances().getBalance();
    }

    private SortedMap<String, Integer> warehouseStock() {
        return PartsCensus.ofWarehouseStock(scenario.getWarehouse());
    }

    /** Counts the procurement list by acquisition name, adding up each order's quantity. */
    private SortedMap<String, Integer> procurementList() {
        SortedMap<String, Integer> census = new TreeMap<>();
        ForceShoppingList shoppingList = campaign.getPlayerForce().getShoppingList();
        for (IAcquisitionWork order : shoppingList.getShoppingList()) {
            census.merge(order.getAcquisitionName(), order.getQuantity(), Integer::sum);
        }
        return census;
    }

    @Test
    void beginReservesNothingAndOrdersTheKit() throws Exception {
        assertEquals(Map.of(), warehouseStock());
        assertEquals(Map.of(), procurementList());

        Refit refit = beginKitRefit();

        assertSame(refit, locust.getRefit());
        assertEquals(locustPartsBeforeRefit, PartsCensus.ofUnit(locust), "Begin must not touch the unit's parts");
        assertEquals(Map.of(), warehouseStock(), "The warehouse held nothing to reserve");
        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 2), PartsCensus.ofParts(refit.getShoppingList()));
        assertEquals(Map.of(refit.getAcquisitionName(), 1), procurementList(), "The kit itself is ordered");
        assertFalse(refit.kitFound());
        assertFalse(refit.acquireParts());
        assertEquals(STARTING_FUNDS, balance(), "Nothing is paid until the kit is found");
    }

    @Test
    void kitInTransitHoldsTheRefitUntilItArrives() throws Exception {
        Refit refit = beginKitRefit();

        deliverKit(refit, KIT_TRANSIT_DAYS);

        assertEquals(STARTING_FUNDS.minus(LOCUST_KIT_PRICE), balance());
        assertEquals(Map.of(), procurementList());
        assertEquals(Map.of("Medium Laser [reserved] [in transit]", 1, "Small Laser [reserved] [in transit]", 2),
              warehouseStock());
        assertEquals(List.of(), refit.getShoppingList());
        assertTrue(refit.kitFound());
        assertTrue(refit.partsInTransit());
        assertFalse(refit.acquireParts(), "The refit waits for parts in transit");

        arriveAllParts();

        assertEquals(Map.of("Medium Laser [reserved]", 1, "Small Laser [reserved]", 2), warehouseStock());
        assertTrue(refit.acquireParts());
    }

    @Test
    void completedRefitMatchesAFreshLct1eAndReturnsTheRemovedParts() throws Exception {
        Refit refit = beginKitRefit();
        deliverKit(refit, 0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        assertNull(locust.getRefit());
        assertEquals("LCT-1E", locust.getEntity().getModel());
        Unit freshLct1e = scenario.withUnit(UnitFixture.LOCUST_LCT_1E);
        assertEquals(PartsCensus.ofUnit(freshLct1e), PartsCensus.ofUnit(locust));
        // The Machine Gun bin is unloaded into 200 loose rounds; the empty bin is not kept (PW-23)
        assertEquals(Map.of("Machine Gun", 2, "Machine Gun Ammo [Full]", 200), warehouseStock());
        for (Part sparePart : scenario.getSpareParts()) {
            assertFalse(sparePart.isBrandNew(), sparePart.getName() + " came off the unit and is not brand new");
        }
        assertEquals(STARTING_FUNDS.minus(LOCUST_KIT_PRICE), balance(), "Completing costs nothing further");
        assertEquals(Map.of(), procurementList());
    }

    @Test
    void failedCheckRestartsTheWorkAndKeepsThePartsReserved() throws Exception {
        Refit refit = beginKitRefit();
        deliverKit(refit, 0);
        refit.addTimeSpent(refit.getTime());

        refit.fail(SkillType.EXP_GREEN);

        // Current behaviour: all the time spent is lost and the next finish succeeds without a roll
        assertTrue(refit.hasFailedCheck());
        assertEquals(0, refit.getTimeSpent());
        assertEquals(refit.getTime(), refit.getTimeLeft());
        assertSame(refit, locust.getRefit());
        assertEquals(locustPartsBeforeRefit, PartsCensus.ofUnit(locust));
        assertEquals(Map.of("Medium Laser [reserved]", 1, "Small Laser [reserved]", 2), warehouseStock());
        assertEquals(STARTING_FUNDS.minus(LOCUST_KIT_PRICE), balance());

        refit.succeed();

        assertEquals("LCT-1E", locust.getEntity().getModel());
    }

    @Test
    void cancelBeforeTheKitIsFoundRemovesTheKitOrder() throws Exception {
        Refit refit = beginKitRefit();

        refit.cancel();

        assertNull(locust.getRefit());
        assertEquals(locustPartsBeforeRefit, PartsCensus.ofUnit(locust));
        assertEquals(Map.of(), warehouseStock());
        assertEquals(STARTING_FUNDS, balance());
        assertEquals(Map.of(), procurementList(), "The cancelled refit's kit is no longer ordered");
    }

    @Test
    void cancelWhileTheKitIsInTransitLeavesThePartsInTransit() throws Exception {
        Refit refit = beginKitRefit();
        deliverKit(refit, KIT_TRANSIT_DAYS);

        refit.cancel();

        assertNull(locust.getRefit());
        assertEquals(locustPartsBeforeRefit, PartsCensus.ofUnit(locust));
        // The kit's lasers, still three days out, become ordinary spares that arrive when they were due
        assertEquals(Map.of("Medium Laser [in transit]", 1, "Small Laser [in transit]", 2), warehouseStock());
        for (Part sparePart : scenario.getSpareParts()) {
            assertEquals(KIT_TRANSIT_DAYS, sparePart.getDaysToArrival(), sparePart.getName());
        }
        assertEquals(STARTING_FUNDS.minus(LOCUST_KIT_PRICE), balance(), "The kit is not refunded");
        assertEquals(Map.of(), procurementList());
    }

    @Test
    void customRefitReservesAWarehouseSpareAndOrdersTheRest() throws Exception {
        Part spareMediumLaser = mediumLaserOf(locust).clone();
        scenario.withSpare(spareMediumLaser, 1);
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, false);

        refit.begin();

        assertEquals(Map.of("Medium Laser [reserved]", 1), warehouseStock());
        assertEquals(2, refit.getShoppingList().size());
        assertEquals(Map.of("Small Laser", 2), procurementList());
        // The procurement list holds its own order, so ordering the second Small Laser leaves the refit's list alone
        assertEquals(Map.of("Small Laser", 2), PartsCensus.ofParts(refit.getShoppingList()));
        assertFalse(refit.acquireParts());
        assertEquals(STARTING_FUNDS, balance());
    }

    @Test
    void cancelledCustomRefitReturnsTheSpareAndRemovesItsOrders() throws Exception {
        Part spareMediumLaser = mediumLaserOf(locust).clone();
        scenario.withSpare(spareMediumLaser, 1);
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, false);
        refit.begin();

        refit.cancel();

        assertNull(locust.getRefit());
        assertEquals(locustPartsBeforeRefit, PartsCensus.ofUnit(locust));
        assertEquals(Map.of("Medium Laser", 1), warehouseStock(), "The reserved spare is free again");
        assertEquals(Map.of(), procurementList(), "The Small Laser orders placed for the refit are removed");
    }

    @Test
    void refitSavedMidwayKeepsItsPartReferences() throws Exception {
        // Only the refit itself is written and read back; a whole-campaign save and load is not yet possible in a
        // unit test, and the target unit design is skipped on reading because it needs the unit data cache.
        Refit refit = beginKitRefit();
        deliverKit(refit, KIT_TRANSIT_DAYS);

        Refit reloaded = writeAndReadBack(refit);
        reloaded.fixReferences(campaign);

        assertEquals(refit.getTime(), reloaded.getTime());
        assertEquals(refit.getCost(), reloaded.getCost());
        assertEquals(refit.getRefitClass(), reloaded.getRefitClass());
        assertEquals(refit.kitFound(), reloaded.kitFound());
        assertEquals(PartsCensus.ofParts(refit.getOldUnitParts()), PartsCensus.ofParts(reloaded.getOldUnitParts()));
        assertEquals(PartsCensus.ofParts(refit.getNewUnitParts()), PartsCensus.ofParts(reloaded.getNewUnitParts()));
        assertEquals(PartsCensus.ofParts(refit.getShoppingList()), PartsCensus.ofParts(reloaded.getShoppingList()));
    }

    private Refit writeAndReadBack(Refit refit) throws Exception {
        StringWriter xmlText = new StringWriter();
        try (PrintWriter xmlWriter = new PrintWriter(xmlText)) {
            refit.writeToXML(xmlWriter, 0);
        }
        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xmlText.toString()
                                                                        .getBytes(StandardCharsets.UTF_8)));
        Element refitElement = document.getDocumentElement();
        Refit reloaded = Refit.generateInstanceFromXML(refitElement, new Version(), campaign, locust, true);
        assertNotNull(reloaded);
        return reloaded;
    }

    private static EquipmentPart mediumLaserOf(Unit unit) {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(unit, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("No Medium Laser on " + unit.getName());
    }
}
