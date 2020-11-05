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

package mekhq.campaign;

import java.util.Objects;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.event.PartArrivedEvent;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * Manages machines and materiel for a campaign.
 */
public class Quartermaster {
    private final Campaign campaign;

    /**
     * Initializes a new instance of the Quartermaster class.
     * @param campaign The campaign being managed by the Quartermaster.
     */
    public Quartermaster(Campaign campaign) {
        this.campaign = Objects.requireNonNull(campaign);
    }

    /**
     * Gets the Campaign being managed by the Quartermaster.
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * Gets the CampaignOptions from the Campaign.
     */
    protected CampaignOptions getCampaignOptions() {
        return getCampaign().getCampaignOptions();
    }

    /**
     * Gets the Warehouse from the Campaign.
     */
    protected Warehouse getWarehouse() {
        return getCampaign().getWarehouse();
    }

    /**
     * Adds a part to the campaign, arriving in a set number of days.
     * @param part The part to add to the campaign.
     * @param transitDays The number of days until the part arrives, or zero
     *                    if the part is already here.
     */
    public void addPart(Part part, int transitDays) {
        Objects.requireNonNull(part);

        if (part.getUnit() instanceof TestUnit) {
            // If this is a test unit, then we won't add the part
            return;
        }

        // don't add missing parts if they don't have units or units with not id
        if ((part instanceof MissingPart) && (null == part.getUnit())) {
            return;
        }

        part.setDaysToArrival(Math.max(transitDays, 0));
        part.setBrandNew(false);

        // be careful in using this next line
        part.postProcessCampaignAddition();

        // Add the part to our warehouse and merge it with any existing part if possible
        getWarehouse().addPart(part, true);
    }

    /**
     * Denotes that a part in-transit has arrived.
     * Should be called when a part goes from 1 daysToArrival to zero.
     *
     * @param part The part which has arrived.
     */
    public void arrivePart(Part part) {
        Objects.requireNonNull(part);

        // Parts on a unit do not need to be reported as being "arrived",
        // the unit itself will receive an arrival event.
        if (part.getUnit() != null) {
            return;
        }

        part.setDaysToArrival(0);

        // TODO: move to an event listener, but ensure PartArrivedEvent
        //       includes the quantity which arrived rather than the
        //       quantity in the warehouse.
        getCampaign().addReport(part.getArrivalReport());

        // Add the part back to the Warehouse, asking that
        // it be merged with any existing spare part.
        part = getWarehouse().addPart(part, true);
        MekHQ.triggerEvent(new PartArrivedEvent(part));
    }

    /**
     * Tries to buy a unit.
     * @param en The entity which represents the unit.
     * @param days The number of days until the new unit arrives.
     * @return True if the unit was purchased, otherwise false.
     */
    public boolean buyUnit(Entity en, int days) {
        Objects.requireNonNull(en);

        if (getCampaignOptions().payForUnits()) {
            Money cost = new Unit(en, getCampaign()).getBuyCost();
            if (getCampaign().getFinances().debit(cost, Transaction.C_UNIT,
                    "Purchased " + en.getShortName(), getCampaign().getLocalDate())) {
                getCampaign().addNewUnit(en, false, days);
                return true;
            } else {
                return false;
            }
        } else {
            getCampaign().addNewUnit(en, false, days);
            return true;
        }
    }

    /**
     * Sells a unit.
     * @param unit The unit to sell.
     */
    public void sellUnit(Unit unit) {
        Objects.requireNonNull(unit);

        Money sellValue = unit.getSellValue();

        getCampaign().getFinances().credit(sellValue, Transaction.C_UNIT_SALE,
                "Sale of " + unit.getName(), getCampaign().getLocalDate());

        getCampaign().removeUnit(unit.getId());
    }

    /**
     * Sell all of the parts on hand.
     * @param part The part to sell.
     */
    public void sellPart(Part part) {
        Objects.requireNonNull(part);

        if (part instanceof AmmoStorage) {
            sellAmmo((AmmoStorage) part);
            return;
        } else if (part instanceof Armor) {
            sellArmor((Armor) part);
            return;
        }

        sellPart(part, part.getQuantity());
    }

    /**
     * Sell one or more units of a part.
     * @param part The part to sell.
     * @param quantity The amount to sell of the part.
     */
    public void sellPart(Part part, int quantity) {
        Objects.requireNonNull(part);

        if (part instanceof AmmoStorage) {
            sellAmmo((AmmoStorage) part, quantity);
            return;
        } else if (part instanceof Armor) {
            sellArmor((Armor) part, quantity);
            return;
        }

        // Do not sell more than we have
        quantity = Math.min(quantity, part.getQuantity());
        if (quantity <= 0) {
            return;
        }

        Money cost = part.getActualValue().multipliedBy(quantity);
        String plural = "";
        if (quantity > 1) {
            plural = "s";
        }

        getCampaign().getFinances().credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + quantity
                + " " + part.getName() + plural, getCampaign().getLocalDate());

        getWarehouse().removePart(part, quantity);
    }

    /**
     * Sell all of the ammo on hand.
     * @param ammo The ammo to sell.
     */
    public void sellAmmo(AmmoStorage ammo) {
        Objects.requireNonNull(ammo);

        sellAmmo(ammo, ammo.getShots());
    }

    /**
     * Sell one or more shots of ammo.
     * @param ammo The ammo to sell.
     * @param shots The number of shots of ammo to sell.
     */
    public void sellAmmo(AmmoStorage ammo, int shots) {
        Objects.requireNonNull(ammo);

        // Do not sell more than we have
        shots = Math.min(shots, ammo.getShots());
        if (shots <= 0) {
            return;
        }

        // How much are we selling?
        double saleProportion = (shots / (double) ammo.getShots());
        if (shots == ammo.getShots()) {
            // Correct for rounding errors
            saleProportion = 1.0;
        }

        Money cost = ammo.getActualValue().multipliedBy(saleProportion);

        getCampaign().getFinances().credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + shots
                + " " + ammo.getName(), getCampaign().getLocalDate());

        getWarehouse().removeAmmo(ammo, shots);
    }

    /**
     * Sell all of the armor on hand.
     * @param armor The armor to sell.
     */
    public void sellArmor(Armor armor) {
        Objects.requireNonNull(armor);

        sellArmor(armor, armor.getAmount());
    }

    /**
     * Sell one or more points of armor
     * @param armor The armor to sell.
     * @param points The number of points of armor to sell.
     */
    public void sellArmor(Armor armor, int points) {
        Objects.requireNonNull(armor);

        // Do not sell more than we have
        points = Math.min(points, armor.getAmount());
        if (points <= 0) {
            return;
        }

        // How much are we selling?
        double saleProportion = ((double) points / armor.getAmount());
        if (points == armor.getAmount()) {
            // Correct for rounding errors
            saleProportion = 1.0;
        }

        Money cost = armor.getActualValue().multipliedBy(saleProportion);

        getCampaign().getFinances().credit(cost, Transaction.C_EQUIP_SALE, "Sale of " + points
                + " " + armor.getName(), getCampaign().getLocalDate());

        getWarehouse().removeArmor(armor, points);
    }

    /**
     * Removes one or more parts from its OmniPod.
     * @param part The omnipodded part.
     */
    public void depodPart(Part part) {
        Objects.requireNonNull(part);

        depodPart(part, part.getQuantity());
    }

    /**
     * Removes one or more parts from its OmniPod.
     * @param part The omnipodded part.
     * @param quantity The number of omnipodded parts to de-pod.
     */
    public void depodPart(Part part, int quantity) {
        Objects.requireNonNull(part);

        if (!part.isOmniPodded()) {
            // We cannot depod non-omnipodded parts.
            return;
        }

        // We cannot depod any more than we have
        quantity = Math.min(quantity, part.getQuantity());
        if (quantity <= 0) {
            return;
        }

        // Create a copy of the part that is no longer in an OmniPod.
        Part unpodded = part.clone();
        unpodded.setOmniPodded(false);

        // Create a new OmniPod part to hold a part of this type
        OmniPod pod = new OmniPod(unpodded, getCampaign());

        while (quantity > 0) {
            // Now when we 'depod' the part we add to the warehouse
            // the part itself and the omnipod which held the part.
            addPart(unpodded.clone(), 0);
            addPart(pod.clone(), 0);

            part.decrementQuantity();
            quantity--;
        }

        // Part::decrementQuantity will handle PartRemovedEvent
        // if the part reaches 0 quantity, otherwise we need to
        // send along the PartChangedEvent if some parts remain.
        if (part.getQuantity() > 0) {
            MekHQ.triggerEvent(new PartChangedEvent(part));
        }
    }

    /**
     * Tries to buys a refurbishment for a given part.
     * @param part The part being refurbished.
     * @return True if the refurbishment was purchased, otherwise false.
     */
    public boolean buyRefurbishment(Part part) {
        if (getCampaignOptions().payForParts()) {
            return getCampaign().getFinances().debit(part.getStickerPrice(), Transaction.C_EQUIP,
                    "Purchase of " + part.getName(), getCampaign().getLocalDate());
        } else {
            return true;
        }
    }

    /**
     * Tries to buy a part arriving in a given number of days.
     * @param part The part to buy.
     * @param transitDays The number of days until the new part arrives.
     * @return True if the part was purchased, otherwise false.
     */
    public boolean buyPart(Part part, int transitDays) {
        return buyPart(part, 1.0, transitDays);
    }

    /**
     * Tries to buy a part with a cost multiplier, arriving in a given number of days.
     * @param part The part to buy.
     * @param costMultiplier The cost multiplier for the purchase.
     * @param transitDays The number of days until the new part arrives.
     * @return True if the part was purchased, otherwise false.
     */
    public boolean buyPart(Part part, double costMultiplier, int transitDays) {
        Objects.requireNonNull(part);

        if (getCampaignOptions().payForParts()) {
            Money cost = part.getStickerPrice().multipliedBy(costMultiplier);
            if (getCampaign().getFinances().debit(cost,
                    Transaction.C_EQUIP, "Purchase of " + part.getName(), getCampaign().getLocalDate())) {
                if (part instanceof Refit) {
                    ((Refit) part).addRefitKitParts(transitDays);
                } else {
                    addPart(part, transitDays);
                }
                return true;
            } else {
                return false;
            }
        } else {
            if (part instanceof Refit) {
                ((Refit) part).addRefitKitParts(transitDays);
            } else {
                addPart(part, transitDays);
            }
            return true;
        }
    }
}
