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

import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.IPlace;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

/**
 * Parts ordered ahead of a refit, before it begins (issue #7848). The player can order what a planned refit needs from
 * the Choose Refit Kit dialog without taking the unit out of action. Those orders are ordinary spare part orders, and
 * this class matches them back to the refit: an open order shows as on order and is not ordered twice, and when the
 * refit begins it takes the open orders over and fills them itself, so nothing is bought twice.
 *
 * <p>For example, refitting a Locust LCT-1V to the LCT-1E needs two Small Lasers. With one already on the
 * procurement list, Order Parts orders only the other; beginning the refit then cancels the open order, because the
 * refit orders both lasers for itself.</p>
 */
public final class RefitPartOrders {
    private static final MMLogger LOGGER = MMLogger.create(RefitPartOrders.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private final Campaign campaign;
    private final Refit refit;
    // Parts compare by identity, so two orders for the same part stay separate keys
    /** The player's own open orders, with how many of each this refit has not yet counted */
    private final Map<IAcquisitionWork, Integer> uncountedOrders = new LinkedHashMap<>();
    /** How many of each open order this refit has counted */
    private final Map<IAcquisitionWork, Integer> countedOrders = new LinkedHashMap<>();
    /** Ammunition and armor already bought and on their way, with the rounds or points not yet counted */
    private final Map<Part, Integer> uncountedDeliveries = new LinkedHashMap<>();
    /** The deliveries on their way that this refit has counted some of */
    private final Set<Part> countedDeliveries = new LinkedHashSet<>();

    /**
     * How much of an amount of ammunition or armor is covered.
     *
     * @param ammoType the ammunition, or {@code null} for armor
     * @param onItsWay rounds or points already bought and on their way
     * @param onOrder  rounds or points on the procurement list
     * @param toOrder  rounds or points still to order
     */
    public record AmountShortfall(@Nullable AmmoType ammoType, int onItsWay, int onOrder, int toOrder) {}

    /**
     * What a planned refit still has to buy, and how much of that is already on order.
     *
     * @param partsOnOrder parts on the procurement list
     * @param partsToOrder parts still to order
     * @param ammunition   each ammunition the refit buys
     * @param armor        the armor the refit buys, or {@code null} if it buys none
     */
    public record Shortfall(List<Part> partsOnOrder, List<Part> partsToOrder, List<AmountShortfall> ammunition,
          @Nullable AmountShortfall armor) {}

    /**
     * What Order Parts placed on the procurement list.
     *
     * @param ordered       each item ordered, described for the player
     * @param cannotBeBought the parts not ordered because the campaign's purchasing options do not allow buying them
     */
    public record OrderResult(List<String> ordered, List<String> cannotBeBought) {}

    private RefitPartOrders(Campaign campaign, Refit refit) {
        this.campaign = campaign;
        this.refit = refit;
        for (IAcquisitionWork order : campaign.getPlayerForce().getShoppingList().getPartList()) {
            if (isPlayerOrder(order) && (order.getQuantity() > 0)) {
                uncountedOrders.put(order, order.getQuantity());
            }
        }
        LocalWarehouse warehouse = refit.getOriginalUnit().getWarehouse();
        if (warehouse != null) {
            warehouse.streamSpareParts()
                  .filter(part -> !part.isPresent())
                  .forEach(part -> {
                      if (part instanceof AmmoStorage ammo) {
                          uncountedDeliveries.put(ammo, ammo.getShots() * ammo.getQuantity());
                      } else if (part instanceof Armor armor) {
                          uncountedDeliveries.put(armor, armor.getAmount());
                      }
                  });
        }
    }

    /**
     * Reads the player's open orders and the deliveries on their way, ready to match them to a refit.
     *
     * @param campaign the campaign
     * @param refit    a planned refit
     *
     * @return the orders, none of them yet counted towards the refit
     */
    public static RefitPartOrders find(Campaign campaign, Refit refit) {
        return new RefitPartOrders(campaign, refit);
    }

    /**
     * @return {@code true} for an order the player placed: not a refit kit, and not an order a refit placed for itself
     */
    private static boolean isPlayerOrder(IAcquisitionWork order) {
        if (order instanceof Refit) {
            return false;
        }
        return !((order instanceof Part orderPart) && (orderPart.getRefitUnit() != null));
    }

    /**
     * Matches what the refit has to buy against the player's open orders and the deliveries on their way. Each order
     * and delivery is counted once, so two needs never share one order.
     *
     * @return what the refit still has to buy
     */
    public Shortfall findShortfall() {
        List<Part> partsOnOrder = new ArrayList<>();
        List<Part> partsToOrder = new ArrayList<>();
        for (Part part : refit.getShoppingList()) {
            boolean isOrderedSeparately = (part instanceof Armor) || (part instanceof AmmoBin)
                  || (part instanceof AmmoStorage);
            if (isOrderedSeparately) {
                continue;
            }
            (isCountedAgainstAnOrder(part) ? partsOnOrder : partsToOrder).add(part);
        }

        List<AmountShortfall> ammunition = new ArrayList<>();
        for (Map.Entry<AmmoType, Integer> ammo : refit.getAmmoRoundsToBuy().entrySet()) {
            AmmoStorage wanted = new AmmoStorage(0, ammo.getKey(), 0, campaign);
            ammunition.add(countAmount(ammo.getKey(), wanted, ammo.getValue()));
        }

        AmountShortfall armor = null;
        int armorPointsToBuy = refit.getArmorPointsToBuy();
        if ((refit.getNewArmorSupplies() != null) && (armorPointsToBuy > 0)) {
            armor = countAmount(null, refit.getNewArmorSupplies().getNewPart(), armorPointsToBuy);
        }
        return new Shortfall(partsOnOrder, partsToOrder, ammunition, armor);
    }

    /**
     * Counts one needed part against the first open order for the same part that still has some left.
     */
    private boolean isCountedAgainstAnOrder(Part neededPart) {
        Object wanted = (neededPart instanceof IAcquisitionWork work) ? work.getNewEquipment() : neededPart;
        if (!(wanted instanceof Part wantedPart)) {
            return false;
        }
        for (Map.Entry<IAcquisitionWork, Integer> order : uncountedOrders.entrySet()) {
            boolean isSamePart = (order.getValue() > 0)
                  && (order.getKey().getNewEquipment() instanceof Part orderedPart)
                  && orderedPart.isSamePartType(wantedPart);
            if (isSamePart) {
                countOrder(order, 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Counts an amount of ammunition or armor against the deliveries on their way, then against open orders, which
     * buy a ton of ammunition or a lot of armor at a time.
     */
    private AmountShortfall countAmount(@Nullable AmmoType ammoType, Part wanted, int amountNeeded) {
        int amountLeft = amountNeeded;
        int onItsWay = 0;
        for (Map.Entry<Part, Integer> delivery : uncountedDeliveries.entrySet()) {
            if ((amountLeft > 0) && delivery.getKey().isSamePartType(wanted)) {
                int amountTaken = Math.min(amountLeft, delivery.getValue());
                if (amountTaken > 0) {
                    countedDeliveries.add(delivery.getKey());
                }
                delivery.setValue(delivery.getValue() - amountTaken);
                onItsWay += amountTaken;
                amountLeft -= amountTaken;
            }
        }
        int onOrder = 0;
        for (Map.Entry<IAcquisitionWork, Integer> order : uncountedOrders.entrySet()) {
            if (!(order.getKey().getNewEquipment() instanceof Part orderedPart) || !orderedPart.isSamePartType(wanted)) {
                continue;
            }
            int amountPerOrder = amountPerOrder(orderedPart);
            int ordersTaken = 0;
            while ((amountLeft > 0) && (ordersTaken < order.getValue()) && (amountPerOrder > 0)) {
                int amountTaken = Math.min(amountLeft, amountPerOrder);
                onOrder += amountTaken;
                amountLeft -= amountTaken;
                ordersTaken++;
            }
            countOrder(order, ordersTaken);
        }
        return new AmountShortfall(ammoType, onItsWay, onOrder, amountLeft);
    }

    private void countOrder(Map.Entry<IAcquisitionWork, Integer> order, int count) {
        if (count > 0) {
            order.setValue(order.getValue() - count);
            countedOrders.merge(order.getKey(), count, Integer::sum);
        }
    }

    /**
     * @return the rounds of ammunition or points of armor one order of the part buys
     */
    private static int amountPerOrder(Part orderedPart) {
        if (orderedPart instanceof AmmoStorage ammo) {
            return ammo.getShots();
        }
        if (orderedPart instanceof Armor armor) {
            return armor.getAmount();
        }
        return 0;
    }

    /**
     * Orders everything a planned refit still has to buy, as ordinary spare part orders, without beginning the refit.
     * Anything already on order or on its way is not ordered again, and parts the campaign cannot buy are left out.
     * The orders go to the warehouse kept with the unit, which may be a base's. The daily report says what was
     * ordered and what was left out.
     *
     * @param campaign the campaign
     * @param refit    a planned refit
     *
     * @return what was ordered
     */
    public static OrderResult orderMissingParts(Campaign campaign, Refit refit) {
        RefitPartOrders openOrders = find(campaign, refit);
        Shortfall shortfall = openOrders.findShortfall();
        UUID unitId = refit.getOriginalUnit().getId();
        List<String> cannotBeBought = RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit);
        ForceShoppingList procurementList = campaign.getPlayerForce().getShoppingList();
        IPlace place = findBaseOf(campaign, refit.getOriginalUnit());

        List<String> ordered = new ArrayList<>();
        List<String> partNames = new ArrayList<>();
        for (Part part : shortfall.partsToOrder()) {
            if (cannotBeBought.contains(part.getName()) || !(part instanceof IAcquisitionWork work)) {
                continue;
            }
            procurementList.addShoppingItem(labelled(Refit.newOrderFor(work), unitId), 1, campaign, place);
            partNames.add(part.getName());
        }
        ordered.addAll(tally(partNames));
        for (AmountShortfall ammo : shortfall.ammunition()) {
            if ((ammo.toOrder() > 0) && (ammo.ammoType() != null)) {
                IAcquisitionWork order = new AmmoStorage(0, ammo.ammoType(), 0, campaign).getAcquisitionWork();
                int tons = ordersFor(ammo.toOrder(), (Part) order);
                procurementList.addShoppingItem(labelled(order, unitId), tons, campaign, place);
                ordered.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitNeed.rounds", ammo.ammoType().getName(),
                      tons * amountPerOrder((Part) order)));
            }
        }
        AmountShortfall armor = shortfall.armor();
        if ((armor != null) && (armor.toOrder() > 0)) {
            Part order = refit.getNewArmorSupplies().getNewPart();
            int lots = ordersFor(armor.toOrder(), order);
            procurementList.addShoppingItem(labelled((IAcquisitionWork) order, unitId), lots, campaign, place);
            ordered.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitNeed.armor", order.getName(),
                  lots * amountPerOrder(order)));
        }

        LOGGER.info("[RefitParts] Ordered ahead of the refit of {} to {}: {}; cannot be bought: {}",
              refit.getOriginalUnit().getName(), refit.getNewEntity().getShortName(), ordered, cannotBeBought);
        reportOrders(campaign, refit, ordered, cannotBeBought);
        int ordersLabelled = openOrders.labelWhatCoversTheRefit(unitId);
        LOGGER.debug("[RefitParts] {}: {} open orders and deliveries already covering the refit now say so",
              refit.getOriginalUnit().getName(), ordersLabelled);
        markPendingRefit(refit);
        return new OrderResult(ordered, cannotBeBought);
    }

    private static IAcquisitionWork labelled(IAcquisitionWork order, UUID unitId) {
        if (order instanceof Part orderPart) {
            orderPart.setOrderedAheadForUnitId(unitId);
        }
        return order;
    }

    /**
     * Labels the open orders and the deliveries on their way that this refit counted, so they say which refit they
     * cover. An order or delivery already labelled for another unit's refit keeps its label.
     *
     * @return how many were labelled
     */
    private int labelWhatCoversTheRefit(UUID unitId) {
        List<Part> covering = new ArrayList<>();
        for (IAcquisitionWork order : countedOrders.keySet()) {
            if (order instanceof Part orderPart) {
                covering.add(orderPart);
            }
        }
        covering.addAll(countedDeliveries);
        for (Part part : refit.getNewUnitParts()) {
            boolean isSpareOnItsWay = !part.isPresent() && (part.getUnit() == null) && !part.isReservedForRefit();
            if (isSpareOnItsWay) {
                covering.add(part);
            }
        }
        int labelled = 0;
        for (Part part : covering) {
            if (part.getOrderedAheadForUnitId() == null) {
                part.setOrderedAheadForUnitId(unitId);
                labelled++;
            }
        }
        return labelled;
    }

    /**
     * @return {@code true} if anything ordered ahead for the unit's refit is still on order, on its way or in the
     *       warehouse
     */
    public static boolean isAnythingOrderedAheadFor(Campaign campaign, Unit unit) {
        UUID unitId = unit.getId();
        return streamOrdersAndParts(campaign, unit).anyMatch(part -> unitId.equals(part.getOrderedAheadForUnitId()));
    }

    /**
     * Clears the labels on everything ordered ahead for the unit's refit, and the unit's Pending Refit mark. Called
     * when the refit begins and claims the parts, or when the player forgets the pending refit. The parts and orders
     * themselves stay, as ordinary spares.
     *
     * @return how many labels were cleared
     */
    public static int forgetOrdersAheadFor(Campaign campaign, Unit unit) {
        UUID unitId = unit.getId();
        List<Part> labelledParts = streamOrdersAndParts(campaign, unit)
                                         .filter(part -> unitId.equals(part.getOrderedAheadForUnitId()))
                                         .toList();
        labelledParts.forEach(part -> part.setOrderedAheadForUnitId(null));
        unit.setPendingRefitModel(null);
        LOGGER.debug("[RefitParts] {}: pending refit forgotten, {} orders and parts no longer labelled for it",
              unit.getName(), labelledParts.size());
        return labelledParts.size();
    }

    /**
     * @return the procurement list's orders, then the parts in the main warehouse and the unit's own, if it differs
     */
    private static Stream<Part> streamOrdersAndParts(Campaign campaign, Unit unit) {
        return Stream.concat(streamOrders(campaign), streamParts(campaign, unit));
    }

    /**
     * @return the orders on the procurement list
     */
    private static Stream<Part> streamOrders(Campaign campaign) {
        return campaign.getPlayerForce()
                     .getShoppingList()
                     .getPartList()
                     .stream()
                     .filter(Part.class::isInstance)
                     .map(Part.class::cast);
    }

    /**
     * @return the parts in the main warehouse, then those in the unit's own warehouse if it differs
     */
    private static Stream<Part> streamParts(Campaign campaign, Unit unit) {
        LocalWarehouse mainWarehouse = campaign.getPlayerForce().getWarehouse();
        LocalWarehouse unitWarehouse = unit.getWarehouse();
        Stream<Part> parts = mainWarehouse.getParts().stream();
        if ((unitWarehouse != null) && (unitWarehouse != mainWarehouse)) {
            parts = Stream.concat(parts, unitWarehouse.getParts().stream());
        }
        return parts;
    }

    /**
     * Everything ordered ahead for the unit's refit has arrived when none of it is still on the procurement list or
     * on its way, and some of it is in the warehouse.
     *
     * @return {@code true} if the refit's parts ordered ahead are all here, so it can begin without waiting for them
     */
    public static boolean isEverythingOrderedAheadHere(Campaign campaign, Unit unit) {
        UUID unitId = unit.getId();
        boolean isAnythingStillOnOrder = streamOrders(campaign)
                                               .anyMatch(order -> unitId.equals(order.getOrderedAheadForUnitId()));
        if (isAnythingStillOnOrder) {
            return false;
        }
        List<Part> partsOrderedAhead = streamParts(campaign, unit)
                                             .filter(part -> unitId.equals(part.getOrderedAheadForUnitId()))
                                             .toList();
        boolean isEverythingDelivered = partsOrderedAhead.stream().allMatch(Part::isPresent);
        return !partsOrderedAhead.isEmpty() && isEverythingDelivered;
    }

    /**
     * Finds the units whose parts ordered ahead have all arrived with today's deliveries, so the player can be told
     * once, the day the last of them arrives. A unit already refitting, or no longer waiting for a refit, is left out.
     *
     * @param campaign     the campaign
     * @param arrivedParts the parts that arrived today
     *
     * @return the units whose refit can now begin without waiting for parts
     */
    public static List<Unit> findRefitsReadyToBegin(Campaign campaign, List<Part> arrivedParts) {
        Set<UUID> unitIds = new LinkedHashSet<>();
        for (Part part : arrivedParts) {
            if (part.getOrderedAheadForUnitId() != null) {
                unitIds.add(part.getOrderedAheadForUnitId());
            }
        }
        List<Unit> readyUnits = new ArrayList<>();
        for (UUID unitId : unitIds) {
            Unit unit = campaign.getUnit(unitId);
            boolean isAwaitingTheRefit = (unit != null) && !unit.isRefitting()
                  && (unit.getPendingRefitModel() != null);
            if (isAwaitingTheRefit && isEverythingOrderedAheadHere(campaign, unit)) {
                readyUnits.add(unit);
            }
        }
        if (!unitIds.isEmpty()) {
            LOGGER.info("[RefitParts] Parts ordered ahead arrived for {} units; everything is here for {}",
                  unitIds.size(), readyUnits.stream().map(Unit::getName).toList());
        }
        return readyUnits;
    }

    /**
     * Marks the unit as waiting for refit parts while anything the refit needs is on order or on its way, so the
     * Hangar shows it in the Pending Refit colour until the refit begins.
     */
    private static void markPendingRefit(Refit refit) {
        Unit unit = refit.getOriginalUnit();
        boolean isAwaitingParts = refit.describeNeeds()
                                        .stream()
                                        .anyMatch(need -> (need.supply() == Refit.Supply.ON_ORDER)
                                              || (need.supply() == Refit.Supply.ON_ITS_WAY));
        if (!isAwaitingParts) {
            LOGGER.debug("[RefitParts] {}: nothing is on order or on its way, so no refit is pending", unit.getName());
            return;
        }
        unit.setPendingRefitModel(refit.getNewEntity().getModel());
        LOGGER.debug("[RefitParts] {}: pending refit to {}", unit.getName(), refit.getNewEntity().getModel());
    }

    private static void reportOrders(Campaign campaign, Refit refit, List<String> ordered,
          List<String> cannotBeBought) {
        String unitName = refit.getOriginalUnit().getName();
        String model = refit.getNewEntity().getModel();
        if (ordered.isEmpty()) {
            campaign.addReport(ACQUISITIONS, getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartOrders.nothingOrdered",
                  unitName, model));
        } else {
            campaign.addReport(ACQUISITIONS, getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartOrders.ordered", unitName,
                  model, String.join(", ", ordered)));
        }
        if (!cannotBeBought.isEmpty()) {
            campaign.addReport(ACQUISITIONS, getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartOrders.cannotBeBought",
                  String.join(", ", cannotBeBought)));
        }
    }

    /**
     * Cancels the player's open orders that a refit counted as it was planned, as the refit begins. The refit orders
     * those parts for itself, or buys them in its kit, so leaving the player's orders open would buy them twice. The
     * daily report names each order taken over.
     *
     * @param campaign the campaign
     * @param refit    the refit beginning
     */
    public static void takeOverOpenOrders(Campaign campaign, Refit refit) {
        RefitPartOrders orders = find(campaign, refit);
        orders.findShortfall();
        if (orders.countedOrders.isEmpty()) {
            return;
        }
        List<String> takenOver = new ArrayList<>();
        for (Map.Entry<IAcquisitionWork, Integer> order : orders.countedOrders.entrySet()) {
            for (int count = 0; count < order.getValue(); count++) {
                order.getKey().decrementQuantity();
            }
            takenOver.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitNeed.count", order.getValue(),
                  order.getKey().getAcquisitionName()));
        }
        campaign.getPlayerForce().getShoppingList().removeZeroQuantityFromList();
        LOGGER.info("[RefitParts] The refit of {} takes over open orders: {}", refit.getOriginalUnit().getName(),
              takenOver);
        campaign.addReport(ACQUISITIONS, getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartOrders.takenOver",
              refit.getOriginalUnit().getName(), String.join(", ", takenOver)));
    }

    /**
     * @return how many orders of the part cover the amount, each order buying one ton of ammunition or one lot of armor
     */
    private static int ordersFor(int amount, Part orderedPart) {
        int amountPerOrder = Math.max(1, amountPerOrder(orderedPart));
        return (int) Math.ceil((double) amount / amountPerOrder);
    }

    private static List<String> tally(List<String> names) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String name : names) {
            counts.merge(name, 1, Integer::sum);
        }
        List<String> tallied = new ArrayList<>();
        for (Map.Entry<String, Integer> count : counts.entrySet()) {
            tallied.add((count.getValue() == 1) ? count.getKey()
                              : getFormattedTextAt(RESOURCE_BUNDLE, "RefitNeed.count", count.getValue(),
                                    count.getKey()));
        }
        return tallied;
    }

    /**
     * @return the base the unit is kept at, or {@code null} if it is with the main force
     */
    private static @Nullable IPlace findBaseOf(Campaign campaign, Unit unit) {
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            if (base.getBaseHangar().getUnit(unit.getId()) != null) {
                return base;
            }
        }
        return null;
    }
}
