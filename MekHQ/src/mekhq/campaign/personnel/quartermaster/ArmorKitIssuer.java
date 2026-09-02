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
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANIC;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

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
import mekhq.campaign.unit.Unit;

/**
 * The warehouse side of issuing armor kits: what a character's local stores hold, drawing a kit out to issue it,
 * returning one when a kit is stripped, and ordering more when the shelf is bare.
 *
 * <p>A kit is a spare {@link EquipmentPart} sitting in the {@link LocalWarehouse} nearest the character. Issuing one
 * consumes it; stripping a kit returns one; kits are bought through the ordinary shopping list, so procuring more is
 * the same order the parts store would place.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ArmorKitIssuer {
    private ArmorKitIssuer() {
    }

    /**
     * Expands a mixed selection of people and units into the set of personnel to kit: every selected person, plus the
     * crew of every selected unit, keeping only those who can be issued a kit and dropping duplicates.
     *
     * @param people the directly selected personnel, or {@code null}
     * @param units  the selected units whose crews should be kitted, or {@code null}
     *
     * @return the personnel to kit, in selection order without repeats
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Set<Person> gatherPersonnel(Collection<Person> people, Collection<Unit> units) {
        Set<Person> gathered = new LinkedHashSet<>();
        if (people != null) {
            for (Person person : people) {
                if (ArmorKitCatalog.canBeIssuedKit(person)) {
                    gathered.add(person);
                }
            }
        }
        if (units != null) {
            for (Unit unit : units) {
                for (Person crew : unit.getCrew()) {
                    if (ArmorKitCatalog.canBeIssuedKit(crew)) {
                        gathered.add(crew);
                    }
                }
            }
        }
        return gathered;
    }

    /**
     * How many of this kit the character's local stores hold.
     *
     * @param person the character whose local warehouse is asked
     * @param kit    the kit to count
     *
     * @return the number in stock, or {@code 0} if the character has no local warehouse
     *
     * @author Illiani
     * @since 0.51.01
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
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money unitPrice(EquipmentType kit, Campaign campaign) {
        return template(kit, campaign).getStickerPrice();
    }

    /**
     * Draws one kit from the character's local stores and issues it to them. Does nothing and reports failure if the
     * character has no local warehouse or none of the kit is in stock.
     *
     * @param person the character to kit
     * @param kit    the kit to issue
     *
     * @return {@code true} if a kit was drawn from stock and issued
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean issueFromStock(Person person, EquipmentType kit, Campaign campaign) {
        if (kit.getInternalName().equals(person.getArmorKitName())) {
            return true; // already wearing it — nothing to draw
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
        // A kit taken off a person goes back on the shelf, so re-kitting returns the old one.
        returnWornKit(person, campaign);
        person.setArmorKitName(kit.getInternalName());
        return true;
    }

    /**
     * Returns the character's current kit to their local stores and puts them back in coveralls. Does nothing if they
     * are already in coveralls, their kit is no longer a known part, or they have no local warehouse.
     *
     * @param person   the character to strip
     * @param campaign the campaign the returned kit belongs to
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void strip(Person person, Campaign campaign) {
        returnWornKit(person, campaign);
        person.setArmorKitName(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
    }

    /**
     * Puts the kit a person is currently wearing back into their local stores, if it is a real kit and not coveralls.
     * Does not change what the person wears — the caller sets that.
     */
    private static void returnWornKit(Person person, Campaign campaign) {
        String current = person.getArmorKitName();
        if ((current == null) || current.equals(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)) {
            return;
        }
        EquipmentType kit = EquipmentType.get(current);
        LocalWarehouse warehouse = person.getWarehouse();
        if ((kit != null) && (warehouse != null)) {
            warehouse.addPart(new EquipmentPart(0, kit, -1, 1.0, false, campaign), true);
        }
    }

    /**
     * Orders more of a kit through the ordinary shopping list, the same purchase the parts store would place. Used to
     * procure the shortfall when more kits are wanted than the local stores hold.
     *
     * @param kit      the kit to order
     * @param quantity how many to order
     * @param campaign the campaign placing the order
     *
     * @author Illiani
     * @since 0.51.01
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
     * Issues any awaited kits that have since arrived in local stores. For each active person meant to wear a kit they
     * do not yet have, if their stores now hold it, the kit is drawn and issued; the intent is cleared once met (or
     * once they already wear it, or the awaited kit is no longer known). Run each day, after deliveries.
     *
     * @param campaign the campaign to sweep
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void fulfillPendingIssues(Campaign campaign) {
        int fulfilled = 0;
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            String intended = person.getIntendedArmorKitName();
            if ((intended == null) || !person.getStatus().isActive()) {
                continue;
            }
            if (intended.equals(person.getArmorKitName())) {
                person.setIntendedArmorKitName(null);
                continue;
            }
            EquipmentType kit = EquipmentType.get(intended);
            if (kit == null) {
                person.setIntendedArmorKitName(null);
                continue;
            }
            if (issueFromStock(person, kit, campaign)) {
                person.setIntendedArmorKitName(null);
                MekHQ.triggerEvent(new PersonChangedEvent(person));
                if (person.getUnit() != null) {
                    person.getUnit().resetPilotAndEntity();
                }
                fulfilled++;
            }
        }

        // Infantry platoons waiting on an ordered kit: issue it once the whole platoon can be outfitted from stores.
        for (Unit unit : campaign.getUnits()) {
            String intended = unit.getIntendedArmorKitName();
            if (intended == null) {
                continue;
            }
            EquipmentType kit = EquipmentType.get(intended);
            if (kit == null) {
                unit.setIntendedArmorKitName(null);
                continue;
            }
            int troopers = Math.max(1, unit.getCrew().size());
            LocalWarehouse warehouse = platoonWarehouse(unit);
            if (localStockCount(warehouse, kit) >= troopers) {
                applyPlatoonKit(unit, warehouse, kit, troopers, false, campaign);
                fulfilled++;
            }
        }

        if (fulfilled > 0) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  getFormattedTextAt("mekhq.resources.IssueArmorKitsDialog", "report.fulfilled", fulfilled));
        }
    }

    /**
     * Issues an armor kit to a conventional infantry platoon, where the kit is the unit's field armor rather than a
     * crew member's. One kit per trooper is drawn from the unit's local stores (any shortfall is ordered), the
     * platoon's previously issued kit is returned to stores, and the kit is recorded on the unit and applied to the
     * entity for deployment. The unit's designed armor Parts are left unchanged — a deploy-time overlay, not a refit.
     *
     * @param unit     the infantry platoon to kit
     * @param kit      the kit to issue
     * @param campaign the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void issuePlatoonKit(Unit unit, EquipmentType kit, Campaign campaign) {
        int troopers = Math.max(1, unit.getCrew().size());
        LocalWarehouse warehouse = platoonWarehouse(unit);
        captureDesignedKit(unit);
        boolean coveralls = kit.getInternalName().equals(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);

        // A platoon wears one kit type for all: unless the whole platoon can be outfitted from stores (or it is being
        // stripped to free coveralls), the kit is ordered and not applied until enough arrive.
        if (!coveralls && (localStockCount(warehouse, kit) < troopers)) {
            order(kit, troopers - localStockCount(warehouse, kit), campaign);
            unit.setIntendedArmorKitName(kit.getInternalName());
            return;
        }

        applyPlatoonKit(unit, warehouse, kit, troopers, coveralls, campaign);
    }

    /** Draws the kit for the whole platoon, returns the old one, and applies it. Assumes stores hold enough. */
    private static void applyPlatoonKit(Unit unit, LocalWarehouse warehouse, EquipmentType kit, int troopers,
          boolean coveralls, Campaign campaign) {
        returnPlatoonKit(unit, warehouse, troopers, campaign);
        if (!coveralls) {
            drawKits(warehouse, kit, troopers);
        }
        unit.setIntendedArmorKitName(null);
        unit.setArmorKitName(kit.getInternalName());
        unit.resetPilotAndEntity();
    }

    private static int localStockCount(LocalWarehouse warehouse, EquipmentType kit) {
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
     * Returns a conventional infantry platoon to its designed armor: the captured designed kit is re-issued (drawn from
     * stores, ordering any shortfall) and the platoon's current kit is returned. If the platoon had no designed kit, it
     * is stripped to coveralls instead.
     *
     * @param unit     the infantry platoon
     * @param campaign the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void restorePlatoonDesigned(Unit unit, Campaign campaign) {
        captureDesignedKit(unit);
        String designed = unit.getDesignedInfantryKitName();
        EquipmentType designedKit = (designed != null) ? EquipmentType.get(designed) : null;
        if (designedKit != null) {
            issuePlatoonKit(unit, designedKit, campaign);
        }
    }

    /** Remembers the platoon's designed armor kit before the first issued kit overrides it. */
    private static void captureDesignedKit(Unit unit) {
        if (unit.getDesignedInfantryKitName() != null) {
            return;
        }
        EquipmentType designed = (unit.getEntity() instanceof megamek.common.units.ConvInfantry convInfantry)
                                       ? convInfantry.getArmorKit()
                                       : null;
        unit.setDesignedInfantryKitName((designed != null)
                                              ? designed.getInternalName()
                                              : ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
    }

    /** The stores the platoon draws from: the unit's local warehouse, or a trooper's if the unit resolves none. */
    private static LocalWarehouse platoonWarehouse(Unit unit) {
        LocalWarehouse warehouse = unit.getWarehouse();
        if (warehouse == null) {
            for (Person trooper : unit.getCrew()) {
                LocalWarehouse trooperWarehouse = trooper.getWarehouse();
                if (trooperWarehouse != null) {
                    return trooperWarehouse;
                }
            }
        }
        return warehouse;
    }

    /**
     * Puts the platoon's currently worn kit (override, else designed) back in stores, one per trooper, unless it is
     * coveralls.
     */
    private static void returnPlatoonKit(Unit unit, LocalWarehouse warehouse, int troopers, Campaign campaign) {
        String worn = (unit.getArmorKitName() != null) ? unit.getArmorKitName() : unit.getDesignedInfantryKitName();
        if ((warehouse == null) || (worn == null) || worn.equals(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)) {
            return;
        }
        EquipmentType wornKit = EquipmentType.get(worn);
        if (wornKit == null) {
            return;
        }
        for (int i = 0; i < troopers; i++) {
            warehouse.addPart(new EquipmentPart(0, wornKit, -1, 1.0, false, campaign), true);
        }
    }

    private static void drawKits(LocalWarehouse warehouse, EquipmentType kit, int wanted) {
        if (warehouse == null) {
            return;
        }
        int drawn = 0;
        while (drawn < wanted) {
            Part inStock = warehouse.findSparePart(part -> isKitPart(part, kit));
            if (inStock == null) {
                break;
            }
            warehouse.removePart(inStock, 1);
            drawn++;
        }
    }

    /**
     * On joining the campaign, a character tries to equip the default kit configured for their group. If it is in their
     * local stores, it is issued at once; otherwise, when the campaign is set to procure recruits' kits, it is ordered
     * and remembered so it is issued when it arrives. Does nothing if the group has no default (coveralls), the person
     * cannot wear a kit, or the kit is unknown.
     *
     * @param person   the freshly recruited character
     * @param campaign the campaign they joined
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipDefaultKitOnRecruitment(Person person, Campaign campaign) {
        if (!ArmorKitCatalog.canBeIssuedKit(person)) {
            return;
        }
        String defaultKit = defaultKitFor(ArmorKitCatalog.categoryFor(person), campaign);
        if ((defaultKit == null) || defaultKit.equals(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)) {
            return;
        }
        EquipmentType kit = EquipmentType.get(defaultKit);
        if (kit == null) {
            return;
        }
        if (issueFromStock(person, kit, campaign)) {
            return; // equipped straight from local stores
        }
        if (campaign.getCampaignOptions().get(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT)) {
            order(kit, 1, campaign);
            person.setIntendedArmorKitName(kit.getInternalName());
        }
    }

    private static String defaultKitFor(ArmorKitCatalog.Category category, Campaign campaign) {
        return switch (category) {
            case MEKWARRIOR -> campaign.getCampaignOptions().get(CampaignOption.MEKWARRIOR_DEFAULT_KIT);
            case AIRCRAFT -> campaign.getCampaignOptions().get(CampaignOption.AIRCRAFT_DEFAULT_KIT);
            case INFANTRY -> campaign.getCampaignOptions().get(CampaignOption.VEHICLE_CREW_DEFAULT_KIT);
            // Soldiers have no per-recruit default kit; their platoon's kit is issued to the unit instead.
            case SOLDIER -> ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME;
        };
    }

    /**
     * The acquisition target number a Regular-skilled acquirer would face to procure this kit — a measure of how hard
     * it is to come by. Special results ({@code AUTOMATIC_SUCCESS}, impossible) come through on the {@link TargetRoll}
     * for the caller to render.
     *
     * @param kit      the kit being priced for difficulty
     * @param campaign the campaign the acquisition is quoted to
     *
     * @return the acquisition {@link TargetRoll} for a Regular acquirer
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static TargetRoll acquisitionTarget(EquipmentType kit, Campaign campaign) {
        return campaign.checkAcquisition(template(kit, campaign).getAcquisitionWork(), regularAcquirer(campaign), false)
                     .getTargetNumber();
    }

    /** A throwaway acquirer at Regular skill, so the displayed difficulty is a fixed reference, not the current staff. */
    private static Person regularAcquirer(Campaign campaign) {
        Person acquirer = new Person(campaign);
        for (String skill : new String[] { S_NEGOTIATION, S_ADMIN, S_TECH_MECHANIC }) {
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
