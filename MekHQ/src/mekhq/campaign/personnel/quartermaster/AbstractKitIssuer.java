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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 * The shared warehouse mechanics behind issuing personal kits, common to {@link ArmorKitIssuer} (armor kits) and
 * {@link EquipmentKitIssuer} (specialized tool kits).
 *
 * <p>A kit, whichever kind, is a spare {@link EquipmentPart} sitting in the {@link LocalWarehouse} nearest the person.
 * Both issuers count what a person's local stores hold, price a kit, draw one out to issue it, return one to stores,
 * and order more through the ordinary shopping list when the shelf is bare. That machinery lives here; the subclasses
 * differ only in how a worn kit is recorded on the {@link Person} and in the profession-specific issuing rules built on
 * top.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class AbstractKitIssuer {
    protected AbstractKitIssuer() {
    }

    /**
     * A single reference acquirer at Regular skill, shared by every kit issuer, so a displayed acquisition difficulty
     * is a fixed benchmark rather than a reading off the current staff. Only its acquisition skill matters to
     * {@code checkAcquisition} (the campaign supplies every other modifier), and MekHQ only ever has one campaign, so
     * it is built once and reused rather than reconstructed for every card — a {@link Person} is expensive to build and
     * a kit dialog builds dozens of cards on the EDT. The acquirer holds no reference back to the campaign.
     */
    private static Person regularAcquirer;

    /**
     * The shared Regular-skill reference acquirer, built lazily on first use and reused thereafter.
     *
     * @param campaign the campaign the acquirer is (once) constructed against
     *
     * @return the shared reference acquirer
     */
    protected static Person regularAcquirer(Campaign campaign) {
        if (regularAcquirer == null) {
            Person acquirer = new Person(campaign);
            for (String skill : new String[] { S_NEGOTIATION, S_ADMIN, S_TECH_VEHICLE }) {
                acquirer.addSkill(skill, SkillType.getType(skill).getRegularLevel(), 0);
            }
            regularAcquirer = acquirer;
        }
        return regularAcquirer;
    }

    /**
     * How many of this kit the person's local stores hold.
     *
     * @param person the person whose local warehouse is asked
     * @param kit    the kit to count
     *
     * @return the number in stock, or {@code 0} if the person has no local warehouse
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
     * The number of each present, spare kit these people's distinct local warehouses hold, tallied by kit type in a
     * single pass. Callers building many cards should use this once instead of
     * {@link #localStock(Person, EquipmentType)} per kit, which rescans the whole spare-parts list every call.
     *
     * @param people the people whose local warehouses to tally
     *
     * @return kit equipment type -&gt; count in stock across those warehouses
     */
    public static Map<EquipmentType, Integer> localStock(Collection<Person> people) {
        Map<EquipmentType, Integer> counts = new HashMap<>();
        Set<LocalWarehouse> counted = new HashSet<>();
        for (Person person : people) {
            LocalWarehouse warehouse = person.getWarehouse();
            if ((warehouse == null) || !counted.add(warehouse)) {
                continue;
            }
            for (Part part : warehouse.getSpareParts()) {
                if (part.isPresent() && part.isSpare() && (part instanceof EquipmentPart equipmentPart)) {
                    counts.merge(equipmentPart.getType(), Math.max(1, part.getQuantity()), Integer::sum);
                }
            }
        }
        return counts;
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
     * it is to come by. Special results ({@code AUTOMATIC_SUCCESS}, impossible) come through on the {@link TargetRoll}
     * for the caller to render.
     *
     * @param kit      the kit being priced for difficulty
     * @param campaign the campaign the acquisition is quoted to
     *
     * @return the acquisition {@link TargetRoll} for the shared Regular acquirer
     */
    public static TargetRoll acquisitionTarget(EquipmentType kit, Campaign campaign) {
        return campaign.checkAcquisition(template(kit, campaign).getAcquisitionWork(), regularAcquirer(campaign), false)
                     .getTargetNumber();
    }

    /**
     * Whether a spare part is a present, issuable copy of the given kit.
     *
     * @param part the spare part being examined
     * @param kit  the kit type to match
     *
     * @return {@code true} if the part is a delivered spare of that kit
     */
    protected static boolean isKitPart(Part part, EquipmentType kit) {
        // Only present (delivered) kits count — a part still in transit cannot be issued yet.
        return part.isPresent()
                     && part.isSpare()
                     && (part instanceof EquipmentPart equipmentPart)
                     && kit.equals(equipmentPart.getType());
    }

    /** A throwaway single-unit spare of the kit, used to price it and to place its acquisition on the shopping list. */
    protected static EquipmentPart template(EquipmentType kit, Campaign campaign) {
        return new EquipmentPart(0, kit, -1, 1.0, false, campaign);
    }

    /** A fresh, present spare of the kit to drop back into a person's local stores when a kit is returned. */
    protected static EquipmentPart returnedKit(EquipmentType kit, Campaign campaign) {
        return new EquipmentPart(0, kit, -1, 1.0, false, campaign);
    }
}
