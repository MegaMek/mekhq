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
import java.util.List;
import java.util.Map;

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
        Shortfall shortfall = find(campaign, refit).findShortfall();
        List<String> cannotBeBought = RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit);
        ForceShoppingList procurementList = campaign.getPlayerForce().getShoppingList();
        IPlace place = findBaseOf(campaign, refit.getOriginalUnit());

        List<String> ordered = new ArrayList<>();
        List<String> partNames = new ArrayList<>();
        for (Part part : shortfall.partsToOrder()) {
            if (cannotBeBought.contains(part.getName()) || !(part instanceof IAcquisitionWork work)) {
                continue;
            }
            procurementList.addShoppingItem(Refit.newOrderFor(work), 1, campaign, place);
            partNames.add(part.getName());
        }
        ordered.addAll(tally(partNames));
        for (AmountShortfall ammo : shortfall.ammunition()) {
            if ((ammo.toOrder() > 0) && (ammo.ammoType() != null)) {
                IAcquisitionWork order = new AmmoStorage(0, ammo.ammoType(), 0, campaign).getAcquisitionWork();
                int tons = ordersFor(ammo.toOrder(), (Part) order);
                procurementList.addShoppingItem(order, tons, campaign, place);
                ordered.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitNeed.rounds", ammo.ammoType().getName(),
                      tons * amountPerOrder((Part) order)));
            }
        }
        AmountShortfall armor = shortfall.armor();
        if ((armor != null) && (armor.toOrder() > 0)) {
            Part order = refit.getNewArmorSupplies().getNewPart();
            int lots = ordersFor(armor.toOrder(), order);
            procurementList.addShoppingItem((IAcquisitionWork) order, lots, campaign, place);
            ordered.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitNeed.armor", order.getName(),
                  lots * amountPerOrder(order)));
        }

        LOGGER.info("[RefitParts] Ordered ahead of the refit of {} to {}: {}; cannot be bought: {}",
              refit.getOriginalUnit().getName(), refit.getNewEntity().getShortName(), ordered, cannotBeBought);
        reportOrders(campaign, refit, ordered, cannotBeBought);
        return new OrderResult(ordered, cannotBeBought);
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
