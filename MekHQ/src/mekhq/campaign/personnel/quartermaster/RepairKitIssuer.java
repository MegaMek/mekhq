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
package mekhq.campaign.personnel.quartermaster;

import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VEHICLE;

import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * The warehouse side of issuing specialized repair kits, mirroring {@link ArmorKitIssuer}: what a technician's local
 * stores hold, drawing a kit out to issue it, returning one when a kit is removed, and ordering more through the
 * ordinary shopping list when the shelf is bare.
 *
 * <p>A kit is a spare {@link EquipmentPart} sitting in the {@link LocalWarehouse} nearest the technician. Issuing one
 * consumes it and records the kit on the technician (see {@link Person#getRepairKitNames()}); removing a kit returns
 * one to stores; kits are bought through the ordinary shopping list, so procuring more is the same order the parts
 * store would place. Unlike an armor kit, a technician may own several different repair kits at once.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class RepairKitIssuer {
    private RepairKitIssuer() {
    }

    /**
     * How many of this kit the technician's local stores hold.
     *
     * @param person the technician whose local warehouse is asked
     * @param kit    the kit to count
     *
     * @return the number in stock, or {@code 0} if the technician has no local warehouse
     */
    public static int localStock(Person person, EquipmentType kit) {
        LocalWarehouse warehouse = person.getWarehouse();
        if (warehouse == null) {
            return 0;
        }
        int count = 0;
        for (Part part : warehouse.getSpareParts()) {
            if (isKitPart(part, kit)) {
                count += Math.max(1, part.getQuantity());
            }
        }
        return count;
    }

    /**
     * The price of one kit, for display alongside the choice.
     *
     * @param kit      the kit being priced
     * @param campaign the campaign the price is quoted to
     *
     * @return the sticker price of a single kit
     */
    public static Money unitPrice(EquipmentType kit, Campaign campaign) {
        return template(kit, campaign).getStickerPrice();
    }

    /**
     * Draws one kit from the technician's local stores and issues it to them. Does nothing and reports failure if the
     * technician has no local warehouse or none of the kit is in stock. Issuing a kit the technician already owns
     * succeeds without drawing stock.
     *
     * @param person   the technician to kit
     * @param kit      the kit to issue
     * @param campaign the campaign the kit belongs to
     *
     * @return {@code true} if the technician now owns the kit
     */
    public static boolean issueFromStock(Person person, EquipmentType kit, Campaign campaign) {
        if (person.hasRepairKit(kit.getInternalName())) {
            return true; // already owned — nothing to draw
        }
        LocalWarehouse warehouse = person.getWarehouse();
        if (warehouse == null) {
            return false;
        }
        Part inStock = warehouse.findSparePart(part -> isKitPart(part, kit));
        if (inStock == null) {
            return false;
        }
        warehouse.removePart(inStock, 1);
        person.getRepairKitNames().add(kit.getInternalName());
        return true;
    }

    /**
     * Removes a kit the technician owns and returns it to their local stores. Does nothing if they do not own it.
     *
     * @param person   the technician to strip the kit from
     * @param kit      the kit to remove
     * @param campaign the campaign the returned kit belongs to
     *
     * @return {@code true} if the kit was owned and has been removed
     */
    public static boolean removeKit(Person person, EquipmentType kit, Campaign campaign) {
        if (!person.getRepairKitNames().remove(kit.getInternalName())) {
            return false;
        }
        LocalWarehouse warehouse = person.getWarehouse();
        if (warehouse != null) {
            warehouse.addPart(new EquipmentPart(0, kit, -1, 1.0, false, campaign), true);
        }
        return true;
    }

    /**
     * Orders more of a kit through the ordinary shopping list, the same purchase the parts store would place.
     *
     * @param kit      the kit to order
     * @param quantity how many to order
     * @param campaign the campaign placing the order
     */
    public static void order(EquipmentType kit, int quantity, Campaign campaign) {
        if (quantity <= 0) {
            return;
        }
        campaign.getPlayerForce()
              .getShoppingList()
              .addShoppingItem(template(kit, campaign).getAcquisitionWork(), quantity, campaign);
    }

    /**
     * The acquisition target number a Regular-skilled acquirer would face to procure this kit — a measure of how hard
     * it is to come by.
     *
     * @param kit      the kit being priced for difficulty
     * @param campaign the campaign the acquisition is quoted to
     *
     * @return the acquisition {@link TargetRoll} for a Regular acquirer
     */
    public static TargetRoll acquisitionTarget(EquipmentType kit, Campaign campaign) {
        return campaign.checkAcquisition(template(kit, campaign).getAcquisitionWork(), regularAcquirer(campaign), false)
                     .getTargetNumber();
    }

    /** A throwaway acquirer at Regular skill, so the displayed difficulty is a fixed reference, not the current staff. */
    private static Person regularAcquirer(Campaign campaign) {
        Person acquirer = new Person(campaign);
        for (String skill : new String[] { S_NEGOTIATION, S_ADMIN, S_TECH_VEHICLE }) {
            acquirer.addSkill(skill, SkillType.getType(skill).getRegularLevel(), 0);
        }
        return acquirer;
    }

    private static boolean isKitPart(Part part, EquipmentType kit) {
        // Only present (delivered) kits count — a part still in transit cannot be issued yet.
        return part.isPresent()
                     && part.isSpare()
                     && (part instanceof EquipmentPart equipmentPart)
                     && kit.equals(equipmentPart.getType());
    }

    private static EquipmentPart template(EquipmentType kit, Campaign campaign) {
        return new EquipmentPart(0, kit, -1, 1.0, false, campaign);
    }
}
