/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.units.Entity;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.parts.PartArrivedEvent;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.InfantryAmmoStorage;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * Manages machines and material for a campaign.
 */
public record Quartermaster(Campaign campaign) {
    public enum PartAcquisitionResult {
        PartInherentFailure,
        PlanetSpecificFailure,
        Success
    }

    /**
     * Initializes a new instance of the Quartermaster class.
     *
     * @param campaign The campaign being managed by the Quartermaster.
     */
    public Quartermaster(Campaign campaign) {
        this.campaign = Objects.requireNonNull(campaign);
    }

    /**
     * Gets the Campaign being managed by the Quartermaster.
     */
    @Override
    public Campaign campaign() {
        return campaign;
    }

    /**
     * Gets the CampaignOptions from the Campaign.
     */
    private CampaignOptions getCampaignOptions() {
        return campaign().getCampaignOptions();
    }

    /**
     * Gets the Warehouse from the Campaign.
     */
    private Warehouse getWarehouse() {
        return campaign().getWarehouse();
    }

    /**
     * Adds a part to the campaign, arriving in a set number of days. By default, the part is treated as not brand new.
     * This method is deprecated in favor of the overloaded method that explicitly accepts a flag to indicate whether
     * the part is brand new.
     *
     * <p>This method delegates its behavior to {@link #addPart(Part, int, boolean)} with the
     * {@code isBrandNew} flag set to {@code false}.</p>
     *
     * @param part        The part to add to the campaign. Cannot be {@code null}.
     * @param transitDays The number of days until the part arrives, or zero if the part is already here.
     *
     * @deprecated Use {@link #addPart(Part, int, boolean)} instead to explicitly indicate whether the part is brand
     *       new.
     */
    @Deprecated
    public void addPart(Part part, int transitDays) {
        addPart(part, transitDays, false);
    }

    /**
     * Adds a part to the campaign's warehouse, specifying the number of transit days for its arrival and whether the
     * part is considered brand new. The method validates the input and decides whether to skip the addition based on
     * specific conditions, such as test units, spare ammo bins, or missing parts without associated units.
     *
     * <p>Once validated, the part is marked as new or used, set to arrive in the specified number of
     * days, processed for campaign addition, and added to the campaign's warehouse.</p>
     *
     * @param part        The part to add to the campaign. Cannot be {@code null}.
     * @param transitDays The number of days until the part arrives. If the value is negative, it will be adjusted to
     *                    zero, indicating the part is already here.
     * @param isBrandNew  A {@code boolean} indicating whether the part is brand new. {@code true} if the part is new,
     *                    otherwise {@code false}.
     *
     * @throws NullPointerException If {@code part} is {@code null}.
     */
    public void addPart(Part part, int transitDays, boolean isBrandNew) {
        Objects.requireNonNull(part);

        // Refit kits are special, if this is for a refit kit use that method
        if (part instanceof Refit refit) {
            refit.addRefitKitParts(transitDays);
            return;
        }

        if (part.getUnit() instanceof TestUnit) {
            // If this is a test unit, then we won't add the part
            return;
        }

        // don't add missing parts if they don't have units or units with not id
        if ((part instanceof MissingPart) && (null == part.getUnit())) {
            return;
        }

        // don't keep around spare ammo bins
        if ((part instanceof AmmoBin) && (null == part.getUnit())) {
            return;
        }

        part.setDaysToArrival(Math.max(transitDays, 0));

        part.setBrandNew(isBrandNew);

        // be careful in using this next line
        part.postProcessCampaignAddition();

        // Add the part to our warehouse and merge it with any existing part if possible
        getWarehouse().addPart(part, true);
    }

    /**
     * Adds ammo to the campaign.
     *
     * @param ammoType The type of ammo to add.
     * @param shots    The number of rounds of ammo to add.
     */
    public void addAmmo(AmmoType ammoType, int shots) {
        Objects.requireNonNull(ammoType);

        if (shots >= 0) {
            addPart(new AmmoStorage(0, ammoType, shots, campaign()), 0);
        }
    }

    /**
     * Adds infantry ammo to the campaign.
     *
     * @param ammoType       The type of ammo to add.
     * @param infantryWeapon The type of infantry weapon using the ammo.
     * @param shots          The number of rounds of ammo to add.
     */
    public void addAmmo(AmmoType ammoType, InfantryWeapon infantryWeapon, int shots) {
        Objects.requireNonNull(ammoType);
        Objects.requireNonNull(infantryWeapon);

        if (shots >= 0) {
            addPart(new InfantryAmmoStorage(0, ammoType, shots, infantryWeapon, campaign()), 0);
        }
    }

    /**
     * Removes ammo from the campaign, if available.
     *
     * @param ammoType    The type of ammo to remove.
     * @param shotsNeeded The number of rounds of ammo needed.
     *
     * @return The number of rounds of ammo removed from the campaign. This value may be less than or equal to
     *       {@code shotsNeeded}.
     */
    public int removeAmmo(AmmoType ammoType, int shotsNeeded) {
        Objects.requireNonNull(ammoType);

        if (shotsNeeded <= 0) {
            return 0;
        }

        AmmoStorage ammoStorage = findSpareAmmo(ammoType);

        int shotsRemoved = removeAmmo(ammoStorage, shotsNeeded);

        // See if we still need some more ammo ...
        int shotsRemaining = shotsNeeded - shotsRemoved;

        // ... then check if we can use compatible ammo ...
        if ((shotsRemaining > 0) && getCampaignOptions().isUseAmmoByType()) {
            shotsRemoved += removeCompatibleAmmo(ammoType, shotsRemaining);
        }

        // Inform the caller how many shots we actually removed for them ...
        return shotsRemoved;
    }

    /**
     * Remove ammo directly from an AmmoStorage part.
     *
     * @param shotsNeeded The number of shots needed.
     *
     * @return The number of shots removed.
     */
    private int removeAmmo(@Nullable AmmoStorage ammoStorage, int shotsNeeded) {
        if ((ammoStorage == null) || (ammoStorage.getShots() == 0)) {
            return 0;
        }

        // We've got at least one round of ammo,
        // so calculate how many shots we can take
        // from this AmmoStorage.
        int shotsRemoved = Math.min(ammoStorage.getShots(), shotsNeeded);

        // Update the number of rounds available ...
        ammoStorage.changeShots(-shotsRemoved);
        if (ammoStorage.getShots() == 0) {
            // ... and remove the part if we've run out.
            getWarehouse().removePart(ammoStorage);
        } else {
            MekHQ.triggerEvent(new PartChangedEvent(ammoStorage));
        }

        return shotsRemoved;
    }

    /**
     * Removes compatible ammo from the campaign, if available.
     *
     * @param ammoType    The type of ammo to remove.
     * @param shotsNeeded The number of rounds of ammo needed.
     *
     * @return The number of rounds of ammo removed from the campaign. This value may be less than or equal to
     *       {@code shotsNeeded}.
     */
    public int removeCompatibleAmmo(AmmoType ammoType, int shotsNeeded) {
        Objects.requireNonNull(ammoType);

        if (shotsNeeded <= 0) {
            return 0;
        }

        int shotsRemoved = 0;

        List<AmmoStorage> compatibleAmmo = findCompatibleSpareAmmo(ammoType);
        for (AmmoStorage compatible : compatibleAmmo) {
            if (shotsRemoved >= shotsNeeded) {
                break;
            }

            // Check to see if it has at least one shot we can use in our target ammo type ...
            int shotsAvailable = convertShots(compatible.getType(), compatible.getShots(), ammoType);
            if (shotsAvailable <= 0) {
                // ... and if not, skip this ammo storage.
                continue;
            }

            // Calculate the shots needed in the compatible ammo type ...
            int compatibleShotsNeeded = convertShotsNeeded(ammoType, shotsNeeded, compatible.getType());

            // Try removing at least one shot of ammo from the compatible ammo storage.
            int compatibleShotsRemoved = removeAmmo(compatible, compatibleShotsNeeded);
            if (compatibleShotsRemoved > 0) {
                // If we did remove some ammo, adjust the number of shots we removed and needed
                shotsRemoved += convertShots(compatible.getType(), compatibleShotsRemoved, ammoType);
            }
        }

        // Check if we removed more than we needed (e.g. we pull LRM20 ammo for an LRM5) ...
        int unusedShots = shotsRemoved - shotsNeeded;
        if (unusedShots > 0) {
            // ... and if we did, return it to the campaign.
            addAmmo(ammoType, unusedShots);
        }

        return Math.min(shotsNeeded, shotsRemoved);
    }

    /**
     * Finds spare ammo of a given type, if any.
     *
     * @param ammoType The AmmoType to search for.
     *
     * @return The matching spare {@code AmmoStorage} part, otherwise {@code null}.
     */
    private @Nullable AmmoStorage findSpareAmmo(AmmoType ammoType) {
        return (AmmoStorage) getWarehouse().findSparePart(part -> isAvailableAsSpareAmmo(part)
                                                                        &&
                                                                        ((AmmoStorage) part).isSameAmmoType(ammoType));
    }

    /**
     * Find compatible ammo in the warehouse.
     *
     * @param ammoType The AmmoType to search for compatible types.
     *
     * @return A list of spare {@code AmmoStorage} parts in the warehouse.
     */
    private List<AmmoStorage> findCompatibleSpareAmmo(AmmoType ammoType) {
        List<AmmoStorage> compatibleAmmo = new ArrayList<>();
        getWarehouse().forEachSparePart(part -> {
            if (!isAvailableAsSpareAmmo(part)) {
                return;
            }

            AmmoStorage spare = (AmmoStorage) part;
            if (spare.isSameAmmoType(ammoType)) {
                // We are looking for compatible ammo, not identical ammo.
                return;
            }

            // If we found a spare ammo bin with at least one shot available ...
            if (spare.isCompatibleAmmo(ammoType) && (spare.getShots() > 0)) {
                // ... add it to our list of compatible ammo.
                compatibleAmmo.add(spare);
            }
        });

        return compatibleAmmo;
    }

    /**
     * Converts shots from one ammo type to another. NB: it is up to the caller to ensure the ammo types are
     * compatible.
     *
     * @param from  The AmmoType for which {@code shots} represents.
     * @param shots The number of shots of {@code from}.
     * @param to    The AmmoType which {@code shots} should be converted to.
     *
     * @return The value of {@code shots} when converted to a specific AmmoType.
     */
    public static int convertShots(AmmoType from, int shots, AmmoType to) {
        if (shots <= 0) {
            return 0;
        }

        int fromRackSize = Math.max(from.getRackSize(), 1);
        int toRackSize = Math.max(to.getRackSize(), 1);
        if (fromRackSize == toRackSize) {
            // Exactly compatible rack sizes
            return shots;
        }

        // Convert the shots (rounding down)
        return (shots * fromRackSize) / toRackSize;
    }

    /**
     * Calculates the shots needed when converting from a source ammo to a target ammo. NB: it is up to the caller to
     * ensure the ammo types are compatible.
     *
     * @param target      The target ammo type.
     * @param shotsNeeded The number of shots needed in the target ammo type.
     * @param source      The source ammo type.
     *
     * @return The number of shots needed from the source ammo type.
     */
    public static int convertShotsNeeded(AmmoType target, int shotsNeeded, AmmoType source) {
        if (shotsNeeded <= 0) {
            return 0;
        }

        int targetRackSize = Math.max(target.getRackSize(), 1);
        int sourceRackSize = Math.max(source.getRackSize(), 1);
        if (targetRackSize == sourceRackSize) {
            // Exactly compatible rack sizes
            return shotsNeeded;
        }

        // Calculate the converted shots needed (rounding up)

        return (shotsNeeded * targetRackSize - 1) / sourceRackSize + 1;
    }

    /**
     * Gets the amount of ammo available of a given type.
     *
     * @param ammoType The type of ammo.
     *
     * @return The number of shots available of the given ammo type.
     */
    public int getAmmoAvailable(AmmoType ammoType) {
        Objects.requireNonNull(ammoType);

        if (!getCampaignOptions().isUseAmmoByType()) {
            // We can't just use findSpareAmmo, that will return the first
            // matching ammo. There may be multiple instances of matching
            // ammo that have different qualities, so we should return
            // all of those counts as viable and not just the first we find.
            return getWarehouse()
                         .streamSpareParts()
                         .filter(Quartermaster::isAvailableAsSpareAmmo)
                         .mapToInt(part -> {
                             AmmoStorage spare = (AmmoStorage) part;
                             if (spare.isSameAmmoType(ammoType)) {
                                 return spare.getShots();
                             }
                             return 0;
                         })
                         .sum();

        } else {
            // If we're using ammo by type, stream through all
            // the ammo that matches strictly or is compatible.
            return getWarehouse()
                         .streamSpareParts()
                         .filter(Quartermaster::isAvailableAsSpareAmmo)
                         .mapToInt(part -> {
                             AmmoStorage spare = (AmmoStorage) part;
                             if (spare.isSameAmmoType(ammoType)) {
                                 return spare.getShots();
                             } else if (spare.isCompatibleAmmo(ammoType)) {
                                 return convertShots(spare.getType(), spare.getShots(), ammoType);
                             }
                             return 0;
                         })
                         .sum();
        }
    }

    /**
     * Gets a value indicating whether a given {@code Part} is available for use as spare ammo.
     *
     * @param part The part to check if it can be used as spare ammo.
     */
    private static boolean isAvailableAsSpareAmmo(@Nullable Part part) {
        return (part instanceof AmmoStorage)
                     && part.isPresent()
                     && !part.isReservedForRefit();
    }

    /**
     * Gets the amount of ammo available of a given type.
     *
     * @param ammoType The type of ammo.
     *
     * @return The number of shots available of the given ammo type.
     */
    public int getAmmoAvailable(AmmoType ammoType, InfantryWeapon weaponType) {
        Objects.requireNonNull(ammoType);

        InfantryAmmoStorage spare = findSpareAmmo(ammoType, weaponType);
        return (spare != null) ? spare.getShots() : 0;
    }

    /**
     * Finds spare infantry ammo of a given type, if any.
     *
     * @param ammoType   The {@code AmmoType} to search for.
     * @param weaponType The {@code InfantryWeapon} which carries the ammo.
     *
     * @return The matching spare {@code InfantryAmmoStorage} part, otherwise {@code null}.
     */
    private @Nullable InfantryAmmoStorage findSpareAmmo(AmmoType ammoType, InfantryWeapon weaponType) {
        return (InfantryAmmoStorage) getWarehouse().findSparePart(part -> {
            if (!(part instanceof InfantryAmmoStorage) || !isAvailableAsSpareAmmo(part)) {
                return false;
            }
            return ((InfantryAmmoStorage) part).isSameAmmoType(ammoType, weaponType);
        });
    }

    /**
     * Removes infantry ammo from the campaign, if available.
     *
     * @param ammoType       The type of ammo to remove.
     * @param infantryWeapon The infantry weapon using the ammo.
     * @param shotsNeeded    The number of rounds of ammo needed.
     *
     * @return The number of rounds of ammo removed from the campaign. This value may be less than or equal to
     *       {@code shotsNeeded}.
     */
    public int removeAmmo(AmmoType ammoType, InfantryWeapon infantryWeapon, int shotsNeeded) {
        Objects.requireNonNull(ammoType);

        if (shotsNeeded <= 0) {
            return 0;
        }

        InfantryAmmoStorage ammoStorage = findSpareAmmo(ammoType, infantryWeapon);

        // Inform the caller how many shots we actually removed for them.
        return removeAmmo(ammoStorage, shotsNeeded);
    }

    /**
     * Denotes that a part in-transit has arrived. Should be called when a part goes from 1 daysToArrival to zero.
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
        campaign().addReport(ACQUISITIONS, part.getArrivalReport());

        // Add the part back to the Warehouse, asking that
        // it be merged with any existing spare part.
        part = getWarehouse().addPart(part, true);
        MekHQ.triggerEvent(new PartArrivedEvent(part));
    }

    public boolean buyUnit(Entity en, int days) {
        return buyUnit(en, days, 1.0);
    }

    /**
     * Tries to buy a unit.
     *
     * @param en              The entity which represents the unit.
     * @param days            The number of days until the new unit arrives.
     * @param valueMultiplier A multiplier to apply to the unit's value.
     *
     * @return True if the unit was purchased, otherwise false.
     */
    public boolean buyUnit(Entity en, int days, double valueMultiplier) {
        Objects.requireNonNull(en);

        PartQuality quality = PartQuality.QUALITY_D;

        if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
            quality = Unit.getRandomUnitQuality(0);
        }

        if (getCampaignOptions().isPayForUnits()) {
            Money cost = new Unit(en, campaign()).getBuyCost().multipliedBy(valueMultiplier);
            if (campaign().getFinances().debit(TransactionType.UNIT_PURCHASE, campaign().getLocalDate(),
                  cost, "Purchased " + en.getShortName())) {

                campaign().addNewUnit(en, false, days, quality);

                return true;
            } else {
                return false;
            }
        } else {
            campaign().addNewUnit(en, false, days, quality);
            return true;
        }
    }

    /**
     * Sells a unit.
     *
     * @param unit The unit to sell.
     */
    public void sellUnit(Unit unit) {
        Objects.requireNonNull(unit);

        Money sellValue = unit.getSellValue();

        campaign().getFinances().credit(TransactionType.UNIT_SALE, campaign().getLocalDate(),
              sellValue, "Sale of " + unit.getName());

        campaign().removeUnit(unit.getId());
    }

    /**
     * Sell all the parts on hand.
     *
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
     *
     * @param part     The part to sell.
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

        campaign().getFinances().credit(TransactionType.EQUIPMENT_SALE, campaign().getLocalDate(),
              cost, "Sale of " + quantity + " " + part.getName() + plural);

        getWarehouse().removePart(part, quantity);
    }

    /**
     * Sell all the ammo on hand.
     *
     * @param ammo The ammo to sell.
     */
    public void sellAmmo(AmmoStorage ammo) {
        Objects.requireNonNull(ammo);

        sellAmmo(ammo, ammo.getShots());
    }

    /**
     * Sell one or more shots of ammo.
     *
     * @param ammo  The ammo to sell.
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

        campaign().getFinances().credit(TransactionType.EQUIPMENT_SALE, campaign().getLocalDate(),
              cost, "Sale of " + shots + " " + ammo.getName());

        getWarehouse().removeAmmo(ammo, shots);
    }

    /**
     * Sell all the armor on hand.
     *
     * @param armor The armor to sell.
     */
    public void sellArmor(Armor armor) {
        Objects.requireNonNull(armor);

        sellArmor(armor, armor.getAmount());
    }

    /**
     * Sell one or more points of armor
     *
     * @param armor  The armor to sell.
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

        campaign().getFinances().credit(TransactionType.EQUIPMENT_SALE, campaign().getLocalDate(),
              cost, "Sale of " + points + " " + armor.getName());

        getWarehouse().removeArmor(armor, points);
    }

    /**
     * Removes one or more parts from its OmniPod.
     *
     * @param part The OmniPodded part.
     */
    public void remotePartFromPod(Part part) {
        Objects.requireNonNull(part);

        remotePartFromPod(part, part.getQuantity());
    }

    /**
     * Removes one or more parts from its OmniPod.
     *
     * @param part     The OmniPodded part.
     * @param quantity The number of OmniPodded parts to de-pod.
     */
    public void remotePartFromPod(Part part, int quantity) {
        Objects.requireNonNull(part);

        if (!part.isOmniPodded()) {
            // We cannot de-pod non-OmniPodded parts.
            return;
        }

        // We cannot de-pod any more than we have
        quantity = Math.min(quantity, part.getQuantity());
        if (quantity <= 0) {
            return;
        }

        // Create a copy of the part that is no longer in an OmniPod.
        Part unpodded = part.clone();
        unpodded.setOmniPodded(false);

        // Create a new OmniPod part to hold a part of this type
        OmniPod pod = new OmniPod(unpodded, campaign());

        while (quantity > 0) {
            // Now when we 'de-pod' the part we add to the warehouse
            // the part itself and the OmniPod which held the part.
            addPart(unpodded.clone(), 0);
            addPart(pod.clone(), 0);

            part.changeQuantity(-1);
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
     * Tries to buy a refurbishment for a given part.
     *
     * @param part The part being refurbished.
     *
     * @return True if the refurbishment was purchased, otherwise false.
     */
    public boolean buyRefurbishment(Part part) {
        if (getCampaignOptions().isPayForParts()) {
            return campaign().getFinances().debit(TransactionType.EQUIPMENT_PURCHASE,
                  campaign().getLocalDate(), part.getActualValue(),
                  "Purchase of " + part.getName());
        } else {
            return true;
        }
    }

    /**
     * Tries to buy a part arriving in a given number of days.
     *
     * @param part        The part to buy.
     * @param transitDays The number of days until the new part arrives.
     *
     * @return True if the part was purchased, otherwise false.
     */
    public boolean buyPart(Part part, int transitDays) {
        return buyPart(part, 1.0, transitDays);
    }

    /**
     * Tries to buy a part with a cost multiplier, arriving in a given number of days.
     *
     * @param part           The part to buy.
     * @param costMultiplier The cost multiplier for the purchase.
     * @param transitDays    The number of days until the new part arrives.
     *
     * @return True if the part was purchased, otherwise false.
     */
    public boolean buyPart(Part part, double costMultiplier, int transitDays) {
        Objects.requireNonNull(part);

        if (getCampaignOptions().isPayForParts()) {
            Money cost = part.getActualValue().multipliedBy(costMultiplier);
            if (campaign().getFinances().debit(TransactionType.EQUIPMENT_PURCHASE,
                  campaign().getLocalDate(), cost, "Purchase of " + part.getName())) {
                addPart(part, transitDays, true);
                return true;
            } else {
                return false;
            }
        } else {
            addPart(part, transitDays, true);
            return true;
        }
    }
}
