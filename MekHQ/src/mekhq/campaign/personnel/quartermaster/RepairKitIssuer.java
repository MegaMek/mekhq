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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.persons.PersonChangedEvent;
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
 * consumes it and records the kit on the technician (see {@link Person#getRepairKitName()}); removing a kit, or issuing
 * a replacement, returns the old one to stores; kits are bought through the ordinary shopping list, so procuring more is
 * the same order the parts store would place. Like an armor kit, a technician carries at most one tool kit at a time.</p>
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
        // A technician carries only one tool kit; the one being replaced goes back to stores.
        returnWornKit(person, campaign);
        person.setRepairKitName(kit.getInternalName());
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
        if (!person.hasRepairKit(kit.getInternalName())) {
            return false;
        }
        person.setRepairKitName(null);
        LocalWarehouse warehouse = person.getWarehouse();
        if (warehouse != null) {
            warehouse.addPart(new EquipmentPart(0, kit, -1, 1.0, false, campaign), true);
        }
        return true;
    }

    /** Returns the tool kit a technician is currently carrying (if any) to their local stores. */
    private static void returnWornKit(Person person, Campaign campaign) {
        String worn = person.getRepairKitName();
        if (worn == null) {
            return;
        }
        EquipmentType kit = EquipmentType.get(worn);
        LocalWarehouse warehouse = person.getWarehouse();
        if ((kit != null) && (warehouse != null)) {
            warehouse.addPart(new EquipmentPart(0, kit, -1, 1.0, false, campaign), true);
        }
        person.setRepairKitName(null);
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
     * On joining the campaign, a new technician is issued the default tool kit configured for each of their technician
     * professions (Mek Tech, Mechanic, Aero Tech, BA Tech; Astechs excluded). A kit already in local stores is issued
     * at once; otherwise, when the campaign is set to procure recruits' kits, it is ordered and remembered so it is
     * issued when it arrives. Does nothing for a profession whose default is "none", a kit the technician already owns,
     * or an unknown kit.
     *
     * @param person   the freshly recruited technician
     * @param campaign the campaign they joined
     * @param gmAdd    {@code true} if the character is being added by the GM (kits are granted directly)
     */
    public static void equipDefaultToolKitOnRecruitment(Person person, Campaign campaign, boolean gmAdd) {
        // A technician carries only one tool kit, so the first profession with a configured default is the one issued.
        for (RepairKitCatalog.KitProfession profession : RepairKitCatalog.professionsFor(person)) {
            String kitName = defaultKitFor(profession, campaign);
            if (kitName.isBlank() || person.hasRepairKit(kitName)) {
                continue;
            }
            EquipmentType kit = EquipmentType.get(kitName);
            if (kit == null) {
                continue;
            }
            if (issueFromStock(person, kit, campaign)) {
                return; // issued straight from local stores
            }
            if (campaign.getCampaignOptions().get(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT)) {
                if (gmAdd) {
                    person.setRepairKitName(kit.getInternalName());
                } else {
                    order(kit, 1, campaign);
                    person.setIntendedRepairKitName(kit.getInternalName());
                }
                return;
            }
        }
    }

    /** The configured default equipment kit for a profession, or the "none" sentinel. */
    private static String defaultKitFor(RepairKitCatalog.KitProfession profession, Campaign campaign) {
        return switch (profession) {
            case MEK_TECH -> campaign.getCampaignOptions().get(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT);
            case MECHANIC -> campaign.getCampaignOptions().get(CampaignOption.MECHANIC_DEFAULT_TOOL_KIT);
            case AERO_TEK -> campaign.getCampaignOptions().get(CampaignOption.AERO_TECH_DEFAULT_TOOL_KIT);
            case BA_TECH -> campaign.getCampaignOptions().get(CampaignOption.BA_TECH_DEFAULT_TOOL_KIT);
            case DOCTOR -> campaign.getCampaignOptions().get(CampaignOption.DOCTOR_DEFAULT_TOOL_KIT);
            case ADMIN -> campaign.getCampaignOptions().get(CampaignOption.ADMIN_DEFAULT_TOOL_KIT);
        };
    }

    /**
     * Issues any awaited tool kits that have since arrived in local stores. For each active person waiting on a kit, if
     * their stores now hold it, the kit is drawn and issued and the intent cleared. Run each day, after deliveries.
     *
     * @param campaign the campaign to sweep
     */
    public static void fulfillPendingToolKits(Campaign campaign) {
        int fulfilled = 0;
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            String intended = person.getIntendedRepairKitName();
            if (!person.getStatus().isActive() || (intended == null)) {
                continue;
            }
            EquipmentType kit = EquipmentType.get(intended);
            if ((kit == null) || person.hasRepairKit(intended)) {
                person.setIntendedRepairKitName(null);
                continue;
            }
            if (issueFromStock(person, kit, campaign)) {
                person.setIntendedRepairKitName(null);
                MekHQ.triggerEvent(new PersonChangedEvent(person));
                fulfilled++;
            }
        }
        if (fulfilled > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt("mekhq.resources.IssueEquipmentDialog", "report.toolKitsFulfilled", fulfilled));
        }
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
