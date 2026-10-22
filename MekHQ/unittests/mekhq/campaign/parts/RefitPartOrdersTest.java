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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Ordering a refit's parts ahead, before the refit begins (issue #7848). The orders are ordinary spare part orders;
 * the refit shows them as on order, does not order them twice, waits for parts already on their way, and takes the
 * open orders over when it begins so nothing is bought twice.
 */
class RefitPartOrdersTest {
    private static final String SMALL_LASER = "Small Laser";

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    /** The LCT-1E needs a Medium Laser and two Small Lasers the LCT-1V does not have */
    private Refit planLocustRefit() {
        return new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, false);
    }

    /** How many of a part the player has ordered, leaving out the orders a refit placed for itself */
    private int countPlayerOrders(String partName) {
        int count = 0;
        for (IAcquisitionWork order : campaign.getPlayerForce().getShoppingList().getPartList()) {
            boolean isPlayerOrder = !(order instanceof Refit) && (((Part) order).getRefitUnit() == null);
            boolean isThePart = (order.getNewEquipment() instanceof Part orderedPart)
                  && partName.equals(orderedPart.getName());
            if (isPlayerOrder && isThePart) {
                count += order.getQuantity();
            }
        }
        return count;
    }

    /** Every order and part labelled as ordered ahead for the Locust's refit */
    private List<Part> labelledForTheLocust() {
        List<Part> labelled = new ArrayList<>();
        for (IAcquisitionWork order : campaign.getPlayerForce().getShoppingList().getPartList()) {
            if ((order instanceof Part orderPart) && locust.getId().equals(orderPart.getOrderedAheadForUnitId())) {
                labelled.add(orderPart);
            }
        }
        for (Part part : scenario.getWarehouse().getParts()) {
            if (locust.getId().equals(part.getOrderedAheadForUnitId())) {
                labelled.add(part);
            }
        }
        return labelled;
    }

    private IAcquisitionWork firstSmallLaserOrder() {
        return campaign.getPlayerForce().getShoppingList().getPartList().stream()
                     .filter(order -> (order.getNewEquipment() instanceof Part orderedPart)
                           && SMALL_LASER.equals(orderedPart.getName()))
                     .findFirst()
                     .orElseThrow();
    }

    private static List<String> describe(List<Refit.RefitNeed> needs, Refit.Supply supply) {
        return needs.stream().filter(need -> need.supply() == supply).map(Refit.RefitNeed::description).toList();
    }

    private Part smallLaserFromAnotherLocust() {
        Unit donor = scenario.withUnit(UnitFixture.LOCUST_LCT_1E);
        for (EquipmentPart part : PartsScenario.unitParts(donor, EquipmentPart.class)) {
            if (SMALL_LASER.equals(part.getName())) {
                return part.clone();
            }
        }
        throw new IllegalStateException("The LCT-1E has no Small Laser");
    }

    @Test
    void orderPartsPutsWhatTheRefitNeedsOnTheProcurementList() {
        Refit refit = planLocustRefit();

        RefitPartOrders.OrderResult result = RefitPartOrders.orderMissingParts(campaign, refit);

        assertEquals(2, countPlayerOrders(SMALL_LASER), result.ordered().toString());
        assertEquals(1, countPlayerOrders("Medium Laser"), result.ordered().toString());
        assertEquals(List.of(), describe(refit.describeNeeds(), Refit.Supply.TO_ORDER));
        assertTrue(describe(refit.describeNeeds(), Refit.Supply.ON_ORDER).contains("2 × Small Laser"),
              refit.describeNeeds().toString());
        assertEquals(RefitPartsStatus.State.WAITING_FOR_DELIVERY, RefitPartsStatus.of(campaign, refit).state());
        assertFalse(locust.isRefitting(), "Ordering ahead leaves the unit in service");
    }

    @Test
    void orderingAheadShowsTheUnitAsPendingRefit() {
        assertFalse(locust.isPendingRefit(), "No refit is pending before anything is ordered");

        RefitPartOrders.orderMissingParts(campaign, planLocustRefit());

        assertEquals("LCT-1E", locust.getPendingRefitModel());
        assertTrue(locust.isPendingRefit());
        assertTrue(locust.getColorReasonKeys().contains("colorReason.unit.pendingRefit"),
              locust.getColorReasonKeys().toString());
        assertEquals(locust.getId(), ((Part) firstSmallLaserOrder()).getOrderedAheadForUnitId(),
              "The order says which refit it is for");
    }

    @Test
    void aPartBoughtFromAnOrderSaysWhichRefitItIsFor() {
        RefitPartOrders.orderMissingParts(campaign, planLocustRefit());

        firstSmallLaserOrder().find(5, 1.0);

        Part boughtLaser = scenario.getWarehouse().getParts().stream()
                                 .filter(part -> SMALL_LASER.equals(part.getName()) && !part.isPresent())
                                 .findFirst()
                                 .orElseThrow();
        assertEquals(locust.getId(), boughtLaser.getOrderedAheadForUnitId());
        assertEquals("Ordered ahead for the refit of " + locust.getName(), boughtLaser.getRepairDesc());
        Part plainSpare = boughtLaser.clone();
        plainSpare.setDaysToArrival(boughtLaser.getDaysToArrival());
        assertFalse(boughtLaser.isSamePartTypeAndStatus(plainSpare),
              "A part ordered ahead does not stack with a plain spare, so it keeps its label");
        assertTrue(locust.isPendingRefit(), "The laser on its way keeps the refit pending");
    }

    @Test
    void theRefitIsReadyOnceEverythingOrderedAheadHasArrived() {
        RefitPartOrders.orderMissingParts(campaign, planLocustRefit());
        // Procurement buys every labelled order, and the parts go on their way
        List<IAcquisitionWork> orders = new ArrayList<>(campaign.getPlayerForce().getShoppingList().getPartList());
        for (IAcquisitionWork order : orders) {
            for (int bought = 0; bought < order.getQuantity(); bought++) {
                order.find(3, 1.0);
            }
        }
        campaign.getPlayerForce().getShoppingList().getShoppingList().removeAll(orders);
        List<Part> onTheirWay = labelledForTheLocust();

        assertFalse(onTheirWay.isEmpty(), "The bought parts keep their labels");
        assertFalse(RefitPartOrders.isEverythingOrderedAheadHere(campaign, locust), "The parts are still on their way");
        assertEquals(List.of(), RefitPartOrders.findRefitsReadyToBegin(campaign, List.of()));

        onTheirWay.forEach(part -> part.setDaysToArrival(0));

        assertTrue(RefitPartOrders.isEverythingOrderedAheadHere(campaign, locust));
        assertEquals(List.of(locust), RefitPartOrders.findRefitsReadyToBegin(campaign, onTheirWay),
              "The player is told the refit can begin");
        assertTrue(locust.getColorReasonKeys().contains("colorReason.unit.pendingRefitReady"),
              locust.getColorReasonKeys().toString());
        assertFalse(locust.isRefitting(), "The refit does not begin by itself");
    }

    @Test
    void theArrivalNoticeReadsForThePlayer() {
        String inCharacter = getFormattedTextAt("mekhq.resources.Parts", "RefitPartsArrivedNotice.inCharacter",
              "Captain", "Uziel UZL-2S", "UZL-3S");
        assertEquals("Captain, everything we ordered for the Uziel UZL-2S refit to the UZL-3S has arrived.",
              inCharacter);
        String outOfCharacter = getFormattedTextAt("mekhq.resources.Parts", "RefitPartsArrivedNotice.outOfCharacter",
              "Captain", "Uziel UZL-2S", "UZL-3S");
        assertTrue(outOfCharacter.contains("has not begun") && outOfCharacter.contains("select the UZL-3S"),
              outOfCharacter);
    }

    @Test
    void orderPartsLabelsAnOrderPlacedEarlier() {
        IAcquisitionWork earlierOrder = (IAcquisitionWork) smallLaserFromAnotherLocust().getMissingPart();
        campaign.getPlayerForce().getShoppingList().addShoppingItem(earlierOrder, 1, campaign);

        RefitPartOrders.orderMissingParts(campaign, planLocustRefit());

        assertEquals(locust.getId(), ((Part) earlierOrder).getOrderedAheadForUnitId(),
              "An order already covering the refit, such as one in an older save, is labelled for it");
    }

    @Test
    void cancellingTheOrdersEndsThePendingRefit() {
        RefitPartOrders.orderMissingParts(campaign, planLocustRefit());

        campaign.getPlayerForce().getShoppingList().getShoppingList().removeIf(order -> order instanceof Part);

        assertFalse(locust.isPendingRefit(), "Nothing is left on order for the refit");
    }

    @Test
    void theLabelIsSavedWithThePart() throws Exception {
        Part laser = smallLaserFromAnotherLocust();
        laser.setOrderedAheadForUnitId(locust.getId());

        StringWriter partXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partXml);
        laser.writeToXML(printWriter, 0);
        printWriter.flush();
        Element partElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          partXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();

        assertEquals(locust.getId(), Part.generateInstanceFromXML(partElement, new Version()).getOrderedAheadForUnitId());
    }

    @Test
    void thePendingRefitIsSavedWithTheUnit() throws Exception {
        RefitPartOrders.orderMissingParts(campaign, planLocustRefit());

        StringWriter unitXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(unitXml);
        locust.writeToXML(printWriter, 0);
        printWriter.flush();
        Element unitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          unitXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        Unit loadedLocust = Unit.generateInstanceFromXML(unitElement, new Version(), campaign);

        assertEquals("LCT-1E", loadedLocust.getPendingRefitModel());
    }

    @Test
    void orderingTwiceOrdersNothingMore() {
        Refit refit = planLocustRefit();
        RefitPartOrders.orderMissingParts(campaign, refit);

        RefitPartOrders.OrderResult secondOrder = RefitPartOrders.orderMissingParts(campaign, refit);

        assertEquals(List.of(), secondOrder.ordered());
        assertEquals(2, countPlayerOrders(SMALL_LASER));
    }

    @Test
    void anOpenOrderCoversOnlyAsManyAsItHolds() {
        campaign.getPlayerForce().getShoppingList()
              .addShoppingItem((IAcquisitionWork) smallLaserFromAnotherLocust().getMissingPart(), 1, campaign);

        List<Refit.RefitNeed> needs = planLocustRefit().describeNeeds();

        assertEquals(List.of(SMALL_LASER), describe(needs, Refit.Supply.ON_ORDER));
        assertTrue(describe(needs, Refit.Supply.TO_ORDER).contains(SMALL_LASER), needs.toString());
    }

    @Test
    void aPartOnItsWayIsWaitedForAndNotBoughtAgain() throws Exception {
        // Three Small Lasers bought and five days out; the refit needs two of them
        Part smallLasers = smallLaserFromAnotherLocust();
        smallLasers.setCampaign(campaign);
        smallLasers.setQuantity(3);
        campaign.getQuartermaster().addPart(smallLasers, 5, false);

        Refit refit = planLocustRefit();

        assertEquals(List.of("2 × Small Laser"), describe(refit.describeNeeds(), Refit.Supply.ON_ITS_WAY));
        boolean isBuyingASmallLaser = refit.getShoppingList().stream()
                                            .anyMatch(part -> SMALL_LASER.equals(part.getName()));
        assertFalse(isBuyingASmallLaser, "The lasers on their way are not bought again");

        assertTrue(refit.begin());

        List<Part> reservedLasers = scenario.getWarehouse().getParts().stream()
                                          .filter(part -> SMALL_LASER.equals(part.getName()))
                                          .filter(part -> part.getRefitUnit() == locust)
                                          .toList();
        assertEquals(2, reservedLasers.size(), "Both lasers are held for the refit");
        for (Part laser : reservedLasers) {
            assertEquals(5, laser.getDaysToArrival(), "A laser split off the stack still arrives with it");
            assertEquals("Held for the refit of " + locust.getName(), laser.getRepairDesc(),
                  "The warehouse says which refit a laser on its way is for");
        }
        Part spareLaser = scenario.getWarehouse().getParts().stream()
                                .filter(part -> SMALL_LASER.equals(part.getName()))
                                .filter(part -> (part.getRefitUnit() == null) && (part.getUnit() == null))
                                .filter(part -> !part.isPresent())
                                .findFirst()
                                .orElseThrow();
        assertEquals("No repairs needed", spareLaser.getRepairDesc(), "The spare laser on its way needs no repairs");
        assertTrue(refit.partsInTransit(), "The refit waits for the lasers to arrive");
    }

    @Test
    void beginningTheRefitTakesOverTheOpenOrders() throws Exception {
        Refit refit = planLocustRefit();
        RefitPartOrders.orderMissingParts(campaign, refit);

        assertTrue(refit.begin());

        assertEquals(0, countPlayerOrders(SMALL_LASER), "The refit orders the lasers itself");
        assertEquals(0, countPlayerOrders("Medium Laser"));
        boolean isOrderedByTheRefit = campaign.getPlayerForce().getShoppingList().getPartList().stream()
                                            .anyMatch(order -> ((Part) order).getRefitUnit() == locust);
        assertTrue(isOrderedByTheRefit, "The refit placed its own orders");
        assertNull(locust.getPendingRefitModel(), "The refit itself now tracks its parts");
        assertEquals(List.of(), labelledForTheLocust(), "The refit claims the orders, so their labels go");
        assertTrue(locust.isPendingRefit(), "The refit is still waiting for the parts it ordered");
    }

    @Test
    void ammunitionIsOrderedByTheTon() {
        Unit javelin = campaign.addNewUnit(UnitTestUtilities.getJavelinJVN10N(), false, 0, PartQuality.QUALITY_D);
        Refit refit = new Refit(javelin, UnitTestUtilities.getJavelinJVN10A(), true, false, false);

        RefitPartOrders.orderMissingParts(campaign, refit);

        List<Refit.RefitNeed> needs = refit.describeNeeds();
        boolean isAmmunitionOnOrder = describe(needs, Refit.Supply.ON_ORDER).stream()
                                            .anyMatch(item -> item.contains("LRM 15") && item.contains("rounds"));
        assertTrue(isAmmunitionOnOrder, needs.toString());
        boolean isAmmunitionLeftToOrder = describe(needs, Refit.Supply.TO_ORDER).stream()
                                                .anyMatch(item -> item.contains("rounds"));
        assertFalse(isAmmunitionLeftToOrder, needs.toString());
    }

    @Test
    void theReportsReadForThePlayer() {
        String ordered = getFormattedTextAt("mekhq.resources.Parts", "RefitPartOrders.ordered", "Locust LCT-1V",
              "LCT-1E", "2 × Small Laser");
        assertTrue(ordered.startsWith("Ordered ahead of the Locust LCT-1V refit to the LCT-1E: 2 × Small Laser."),
              ordered);
        String cannotBeBought = getFormattedTextAt("mekhq.resources.Parts", "RefitPartOrders.cannotBeBought",
              "ER Medium Laser");
        assertTrue(cannotBeBought.contains("campaign's") && cannotBeBought.endsWith("ER Medium Laser."),
              cannotBeBought);
        String waiting = getFormattedTextAt("mekhq.resources.Parts", "RefitPartsStatus.waiting", "Small Laser");
        assertEquals("Nothing left to order. Waiting for: Small Laser", waiting);
    }

    @Test
    void theOrderConfirmationTellsThePlayerWhatComesNext() {
        String dialogBundle = "mekhq.resources.ChooseRefitDialog";
        String ordered = getFormattedTextAt(dialogBundle, "orderedConfirm.ordered", "Captain", "Uziel UZL-2S",
              "UZL-3S", "LB 2-X AC, 2 × ER Small Laser");
        assertTrue(ordered.startsWith("Captain, I've ordered what the Uziel UZL-2S needs for its refit to the UZL-3S: "
              + "LB 2-X AC, 2 × ER Small Laser."), ordered);
        String labelled = getFormattedTextAt(dialogBundle, "orderedConfirm.labelled", "Captain", "Uziel UZL-2S",
              "UZL-3S", "");
        assertTrue(labelled.contains("already on order") && labelled.contains("Pending Refit"), labelled);
        String nextSteps = getFormattedTextAt(dialogBundle, "orderedConfirm.nextSteps",
              "Acquire and Use Refit Kit", "Customize to Model", "Close");
        assertTrue(nextSteps.contains("<b>Acquire and Use Refit Kit</b>") && nextSteps.contains("<b>Close</b>"),
              nextSteps);
        assertEquals("Close", getTextAt(dialogBundle, "btnClose.text"), "The way out does not read as cancelling");
        String cannotBeBought = getFormattedTextAt(dialogBundle, "orderedConfirm.cannotBeBought", "ER Medium Laser");
        assertTrue(cannotBeBought.contains("don't allow buying ER Medium Laser"), cannotBeBought);
    }
}
