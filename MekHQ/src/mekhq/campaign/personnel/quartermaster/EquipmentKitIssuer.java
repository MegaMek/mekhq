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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KitProfession;

/**
 * The warehouse side of issuing equipment kits, mirroring {@link ArmorKitIssuer}: drawing a kit out of a person's
 * stores to issue it, returning one when a kit is removed, and issuing the configured defaults on recruitment, on a
 * role change, on request, and when a default is changed.
 *
 * <p>The shared warehouse mechanics — counting stock, pricing, ordering, and the acquisition-difficulty benchmark —
 * live in {@link AbstractKitIssuer}. This class adds the equipment-kit specifics: a person carries up to two equipment
 * kits, one per {@link KitSlot}, separate from their armor kit. The primary slot takes the default kit of their
 * primary role, the secondary slot that of their secondary role.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class EquipmentKitIssuer extends AbstractKitIssuer {
    private EquipmentKitIssuer() {
    }

    /**
     * Draws one kit from the person's stores and puts it in their primary kit slot.
     *
     * @see #issueFromStock(Person, EquipmentType, KitSlot, Campaign)
     */
    public static boolean issueFromStock(Person person, EquipmentType kit, Campaign campaign) {
        return issueFromStock(person, kit, KitSlot.PRIMARY, campaign);
    }

    /**
     * Draws one kit from the person's stores and puts it in the given kit slot, returning whatever that slot held to
     * stores. Does nothing and reports failure if the person has no stores or none of the kit is in stock. Issuing a
     * kit the person already carries (in either slot) succeeds without drawing stock.
     *
     * @param person   the person to kit
     * @param kit      the kit to issue
     * @param slot     the kit slot to fill
     * @param campaign the campaign the kit belongs to
     *
     * @return {@code true} if the person now carries the kit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean issueFromStock(Person person, EquipmentType kit, KitSlot slot, Campaign campaign) {
        if (person.hasRepairKit(kit.getInternalName())) {
            return true; // already owned — nothing to draw
        }
        LocalWarehouse warehouse = warehouseFor(person, campaign);
        if (warehouse == null) {
            return false;
        }
        Part inStock = warehouse.findSparePart(part -> isKitPart(part, kit));
        if (inStock == null) {
            return false;
        }
        warehouse.removePart(inStock, 1);
        returnKitInSlot(person, slot, campaign);
        person.setKitName(slot, kit.getInternalName());
        return true;
    }

    /**
     * Removes a kit the person carries, from whichever slot holds it, and returns it to their stores. Does nothing if
     * they do not carry it.
     *
     * @param person   the person to strip the kit from
     * @param kit      the kit to remove
     * @param campaign the campaign the returned kit belongs to
     *
     * @return {@code true} if the kit was carried and has been removed
     */
    public static boolean removeKit(Person person, EquipmentType kit, Campaign campaign) {
        if (!person.hasRepairKit(kit.getInternalName())) {
            return false;
        }
        for (KitSlot slot : KitSlot.values()) {
            if (kit.getInternalName().equals(person.getKitName(slot))) {
                returnKitInSlot(person, slot, campaign);
                return true;
            }
        }
        return false;
    }

    /**
     * Empties a kit slot, returning its kit to the person's stores.
     *
     * @param person   the person to strip
     * @param slot     the kit slot to empty
     * @param campaign the campaign the returned kit belongs to
     *
     * @return {@code true} if the slot held a kit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean removeKit(Person person, KitSlot slot, Campaign campaign) {
        if (person.getKitName(slot) == null) {
            return false;
        }
        returnKitInSlot(person, slot, campaign);
        return true;
    }

    /** Returns the kit in a slot (if any) to the person's stores and empties the slot. */
    private static void returnKitInSlot(Person person, KitSlot slot, Campaign campaign) {
        String worn = person.getKitName(slot);
        if (worn == null) {
            return;
        }
        EquipmentType kit = EquipmentType.get(worn);
        LocalWarehouse warehouse = warehouseFor(person, campaign);
        if ((kit != null) && (warehouse != null)) {
            warehouse.addPart(returnedKit(kit, campaign), true);
        }
        person.setKitName(slot, null);
    }

    /**
     * Issues a Basic Toolkit to every active technician who carries no tool kit at all — a one-off convenience granted
     * when the "Techs Need a Tool Kit" requirement is first enabled, so the workforce is not immediately unable to make
     * repairs. No stock is consumed and nothing is charged. The toolkit goes into a free kit slot; a technician with
     * both slots full has their secondary kit returned to stores to make room.
     *
     * @param campaign the campaign whose technicians are equipped
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipAllTechsWithBasicToolKit(Campaign campaign) {
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            if (person.getStatus().isActive()
                      && person.isTechExpanded()
                      && !EquipmentKitCatalog.hasToolKit(person)) {
                KitSlot slot = freeSlot(person);
                if (slot == null) {
                    returnKitInSlot(person, KitSlot.SECONDARY, campaign);
                    slot = KitSlot.SECONDARY;
                }
                person.setKitName(slot, EquipmentKitCatalog.KIT_BASIC_TOOLKIT);
                MekHQ.triggerEvent(new PersonChangedEvent(person));
            }
        }
    }

    /**
     * On joining the campaign, a new person is issued the default kit configured for each of their roles: their
     * primary role's default into the primary kit slot, their secondary role's into the secondary slot. A kit in stores
     * is issued at once. Otherwise a GM-added character is granted it directly, and a regular recruit has it ordered
     * and remembered (when the campaign is set to procure recruits' kits) so it is issued when it arrives. Does nothing
     * for a role whose default is "none", a kit the person already carries or awaits, or an unknown kit.
     *
     * @param person   the freshly recruited person
     * @param campaign the campaign they joined
     * @param gmAdd    {@code true} if the character is being added by the GM (kits are granted directly)
     */
    public static void equipDefaultToolKitOnRecruitment(Person person, Campaign campaign, boolean gmAdd) {
        for (KitSlot slot : KitSlot.values()) {
            EquipmentType kit = defaultKit(slot.roleFor(person), campaign);
            if ((kit == null) || isOwnedOrAwaited(person, kit)) {
                continue;
            }
            if (issueFromStock(person, kit, slot, campaign)) {
                continue; // issued straight from stores
            }
            if (gmAdd) {
                returnKitInSlot(person, slot, campaign);
                person.setKitName(slot, kit.getInternalName());
            } else if (campaign.getCampaignOptions().get(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT)) {
                order(kit, 1, campaign);
                person.setIntendedKitName(slot, kit.getInternalName());
            }
        }
    }

    /**
     * After a person's role changes, issues the new role's default kit — but only into a free kit slot, never over a
     * kit they already carry or await. The slot matching the changed role is preferred, falling back to the other
     * slot. A kit in stores is issued at once; otherwise, when the campaign is set to procure recruits' kits, it is
     * ordered and remembered.
     *
     * @param person      the person whose role changed
     * @param changedSlot the slot whose role changed ({@link KitSlot#PRIMARY} for the primary role)
     * @param campaign    the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipDefaultToolKitOnRoleChange(Person person, KitSlot changedSlot, Campaign campaign) {
        EquipmentType kit = defaultKit(changedSlot.roleFor(person), campaign);
        if ((kit == null) || isOwnedOrAwaited(person, kit)) {
            return;
        }
        KitSlot slot = isSlotFree(person, changedSlot) ? changedSlot
                             : (isSlotFree(person, changedSlot.other()) ? changedSlot.other() : null);
        if (slot == null) {
            return;
        }
        if (!issueFromStock(person, kit, slot, campaign)
                  && campaign.getCampaignOptions().get(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT)) {
            order(kit, 1, campaign);
            person.setIntendedKitName(slot, kit.getInternalName());
        }
    }

    /**
     * Swaps each of the given people onto their roles' default kits: the primary role's default into the primary slot
     * and the secondary role's into the secondary slot, returning whatever those slots held to stores. Kits come from
     * stores; any shortfall is ordered (and paid for) and issued when it arrives. A role with no default leaves its
     * slot unchanged.
     *
     * @param people   the people to re-kit
     * @param campaign the campaign
     * @param totals   accumulates how many kits were issued and ordered
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void issueDefaultKits(Collection<Person> people, Campaign campaign, KitIssueTotals totals) {
        Map<EquipmentType, Integer> shortfall = new LinkedHashMap<>();
        for (Person person : people) {
            if (!person.getStatus().isActive()) {
                continue;
            }
            boolean changed = false;
            for (KitSlot slot : KitSlot.values()) {
                EquipmentType kit = defaultKit(slot.roleFor(person), campaign);
                if (kit != null) {
                    changed |= issueToSlot(person, kit, slot, campaign, shortfall, totals);
                }
            }
            if (changed) {
                MekHQ.triggerEvent(new PersonChangedEvent(person));
            }
        }
        orderShortfall(shortfall, campaign, totals);
    }

    /**
     * Moves everyone carrying a profession's old default kit onto its new default, after that campaign option has
     * changed. Only the slot filled by that profession's role is switched. The new kit comes from stores, returning the
     * old one; any shortfall is ordered (and paid for) and swapped in when it arrives.
     *
     * @param campaign   the campaign
     * @param profession the profession whose default changed
     * @param oldKitName the previous default kit internal name
     * @param newKit     the new default kit
     * @param totals     accumulates how many kits were issued and ordered
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void switchDefaultKit(Campaign campaign, KitProfession profession, String oldKitName,
          EquipmentType newKit, KitIssueTotals totals) {
        Map<EquipmentType, Integer> shortfall = new LinkedHashMap<>();
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            if (!person.getStatus().isActive()) {
                continue;
            }
            for (KitSlot slot : KitSlot.values()) {
                if ((EquipmentKitCatalog.professionFor(slot.roleFor(person)) == profession)
                          && oldKitName.equals(person.getKitName(slot))
                          && issueToSlot(person, newKit, slot, campaign, shortfall, totals)) {
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                }
            }
        }
        orderShortfall(shortfall, campaign, totals);
    }

    /**
     * Puts a kit into a slot from stores, or — when out of stock — records it as awaited and tallies it for ordering.
     * Does nothing when the person already carries the kit in either slot.
     *
     * @return {@code true} if a kit was issued
     */
    private static boolean issueToSlot(Person person, EquipmentType kit, KitSlot slot, Campaign campaign,
          Map<EquipmentType, Integer> shortfall, KitIssueTotals totals) {
        if (person.hasRepairKit(kit.getInternalName())) {
            if (kit.getInternalName().equals(person.getKitName(slot))) {
                person.setIntendedKitName(slot, null);
            }
            return false;
        }
        if (issueFromStock(person, kit, slot, campaign)) {
            person.setIntendedKitName(slot, null);
            totals.issued++;
            return true;
        }
        if (!kit.getInternalName().equals(person.getIntendedKitName(slot))) {
            person.setIntendedKitName(slot, kit.getInternalName());
            shortfall.merge(kit, 1, Integer::sum);
        }
        return false;
    }

    /** The configured default equipment kit for a role, or {@code null} if it has none or the kit is unknown. */
    private static @Nullable EquipmentType defaultKit(@Nullable PersonnelRole role, Campaign campaign) {
        KitProfession profession = EquipmentKitCatalog.professionFor(role);
        if (profession == null) {
            return null;
        }
        String kitName = campaign.getCampaignOptions().get(defaultKitOption(profession));
        if ((kitName == null) || kitName.isBlank()) {
            return null;
        }
        return EquipmentType.get(kitName);
    }

    /**
     * @param profession a kit profession
     *
     * @return the campaign option holding that profession's default equipment kit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static CampaignOption<String> defaultKitOption(KitProfession profession) {
        return switch (profession) {
            case MEK_TECH -> CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT;
            case MECHANIC -> CampaignOption.MECHANIC_DEFAULT_TOOL_KIT;
            case AERO_TEK -> CampaignOption.AERO_TECH_DEFAULT_TOOL_KIT;
            case BA_TECH -> CampaignOption.BA_TECH_DEFAULT_TOOL_KIT;
            case VESSEL_CREW -> CampaignOption.VESSEL_CREW_DEFAULT_TOOL_KIT;
            case ASTECH -> CampaignOption.ASTECH_DEFAULT_TOOL_KIT;
            case DOCTOR -> CampaignOption.DOCTOR_DEFAULT_TOOL_KIT;
            case MEDIC -> CampaignOption.MEDIC_DEFAULT_TOOL_KIT;
            case ADMIN -> CampaignOption.ADMIN_DEFAULT_TOOL_KIT;
        };
    }

    /** Whether the person carries the kit in either slot, or is already waiting on it for either slot. */
    private static boolean isOwnedOrAwaited(Person person, EquipmentType kit) {
        String kitName = kit.getInternalName();
        return person.hasRepairKit(kitName)
                     || kitName.equals(person.getIntendedKitName(KitSlot.PRIMARY))
                     || kitName.equals(person.getIntendedKitName(KitSlot.SECONDARY));
    }

    /** Whether a slot holds no kit and awaits none. */
    private static boolean isSlotFree(Person person, KitSlot slot) {
        return (person.getKitName(slot) == null) && (person.getIntendedKitName(slot) == null);
    }

    /** The first empty kit slot, or {@code null} if both are full. */
    private static @Nullable KitSlot freeSlot(Person person) {
        for (KitSlot slot : KitSlot.values()) {
            if (person.getKitName(slot) == null) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Issues any awaited equipment kits that have since arrived in stores. For each active person waiting on a kit for
     * either slot, if their stores now hold it, the kit is drawn into that slot and the intent cleared. Run each day,
     * after deliveries.
     *
     * @param campaign the campaign to sweep
     */
    public static void fulfillPendingToolKits(Campaign campaign) {
        int fulfilled = 0;
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            if (!person.getStatus().isActive()) {
                continue;
            }
            for (KitSlot slot : KitSlot.values()) {
                String intended = person.getIntendedKitName(slot);
                if (intended == null) {
                    continue;
                }
                EquipmentType kit = EquipmentType.get(intended);
                if ((kit == null) || person.hasRepairKit(intended)) {
                    person.setIntendedKitName(slot, null);
                    continue;
                }
                if (issueFromStock(person, kit, slot, campaign)) {
                    person.setIntendedKitName(slot, null);
                    MekHQ.triggerEvent(new PersonChangedEvent(person));
                    fulfilled++;
                }
            }
        }
        if (fulfilled > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt("mekhq.resources.IssueEquipmentDialog", "report.toolKitsFulfilled", fulfilled));
        }
    }
}
