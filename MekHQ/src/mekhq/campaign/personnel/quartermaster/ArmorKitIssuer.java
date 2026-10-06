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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
import mekhq.campaign.unit.Unit;

/**
 * The warehouse side of issuing armor kits, mirroring {@link EquipmentKitIssuer}: drawing a kit out of a character's
 * local stores to issue it, returning one when a kit is stripped, and outfitting conventional infantry platoons.
 *
 * <p>The shared warehouse mechanics — counting local stock, pricing, ordering, and the acquisition-difficulty
 * benchmark — live in {@link AbstractKitIssuer}. This class adds the armor-kit specifics: a character records their kit
 * on {@link Person#getArmorKitName()}, coveralls ({@link ArmorKitCatalog#DEFAULT_ARMOR_KIT_NAME}) stand in for "no
 * kit", and infantry platoons wear a single kit across the whole unit.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ArmorKitIssuer extends AbstractKitIssuer {
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
     * Draws one kit from the character's stores (their local warehouse, or the main warehouse if they have none) and
     * issues it to them. Does nothing and reports failure if there are no stores or none of the kit is in stock.
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
        LocalWarehouse warehouse = warehouseFor(person, campaign);
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
        LocalWarehouse warehouse = warehouseFor(person, campaign);
        if ((kit != null) && (warehouse != null)) {
            warehouse.addPart(returnedKit(kit, campaign), true);
        }
    }

    private static void add(EquipmentType kit, Person person) {
        person.setArmorKitName(kit.getInternalName());
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
                  getFormattedTextAt("mekhq.resources.IssueEquipmentDialog", "report.fulfilled", fulfilled));
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
            warehouse.addPart(returnedKit(wornKit, campaign), true);
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
     * On joining the campaign, a character tries to equip the default kit configured for their group. If it is in
     * their stores, it is issued at once. Otherwise a GM-added character is granted it directly, and a regular recruit
     * has it ordered and remembered (when the campaign is set to procure recruits' kits) so it is issued when it
     * arrives. Does nothing if the group has no default (coveralls), the person cannot wear a kit, or the kit is
     * unknown.
     *
     * @param person   the freshly recruited character
     * @param campaign the campaign they joined
     * @param gmAdd    {@code true} if the character is being added by the GM (the kit is granted directly)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipDefaultKitOnRecruitment(Person person, Campaign campaign, boolean gmAdd) {
        EquipmentType kit = defaultKit(person, campaign);
        if (kit == null) {
            return;
        }
        if (issueFromStock(person, kit, campaign)) {
            return; // equipped straight from stores
        }

        if (gmAdd) {
            returnWornKit(person, campaign);
            add(kit, person);
        } else if (campaign.getCampaignOptions().get(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT)) {
            order(kit, 1, campaign);
            person.setIntendedArmorKitName(kit.getInternalName());
        }
    }

    /**
     * After a person's role changes, issues their new group's default armor kit — but only if they are still in
     * coveralls and not already awaiting a kit. Handled like a regular recruit: from stores, else ordered when the
     * campaign is set to procure recruits' kits.
     *
     * @param person   the person whose role changed
     * @param campaign the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipDefaultKitOnRoleChange(Person person, Campaign campaign) {
        if (!ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME.equals(person.getArmorKitName())
                  || (person.getIntendedArmorKitName() != null)) {
            return;
        }
        equipDefaultKitOnRecruitment(person, campaign, false);
    }

    /**
     * Swaps each of the given people onto their group's default armor kit, returning what they wore to stores. Kits
     * come from stores; any shortfall is ordered (and paid for) and issued when it arrives. People whose group has no
     * default, and soldiers (whose kit is their platoon's), are left unchanged.
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
            EquipmentType kit = defaultKit(person, campaign);
            if ((kit != null) && issueOrQueue(person, kit, campaign, shortfall, totals)) {
                MekHQ.triggerEvent(new PersonChangedEvent(person));
                if (person.getUnit() != null) {
                    person.getUnit().resetPilotAndEntity();
                }
            }
        }
        orderShortfall(shortfall, campaign, totals);
    }

    /**
     * Moves everyone in a group wearing its old default armor kit onto the new default, after that campaign option has
     * changed. The new kit comes from stores, returning the old one; any shortfall is ordered (and paid for) and
     * swapped in when it arrives.
     *
     * @param campaign   the campaign
     * @param category   the group whose default changed
     * @param oldKitName the previous default kit internal name
     * @param newKit     the new default kit
     * @param totals     accumulates how many kits were issued and ordered
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void switchDefaultKit(Campaign campaign, ArmorKitCatalog.Category category, String oldKitName,
          EquipmentType newKit, KitIssueTotals totals) {
        Map<EquipmentType, Integer> shortfall = new LinkedHashMap<>();
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            if (!person.getStatus().isActive()
                      || !ArmorKitCatalog.canBeIssuedKit(person)
                      || (ArmorKitCatalog.categoryFor(person) != category)
                      || !oldKitName.equals(person.getArmorKitName())) {
                continue;
            }
            if (issueOrQueue(person, newKit, campaign, shortfall, totals)) {
                MekHQ.triggerEvent(new PersonChangedEvent(person));
                if (person.getUnit() != null) {
                    person.getUnit().resetPilotAndEntity();
                }
            }
        }
        orderShortfall(shortfall, campaign, totals);
    }

    /**
     * Issues a kit from stores or — when out of stock — records it as awaited and tallies it for ordering. Does
     * nothing when the person already wears it.
     *
     * @return {@code true} if a kit was issued
     */
    private static boolean issueOrQueue(Person person, EquipmentType kit, Campaign campaign,
          Map<EquipmentType, Integer> shortfall, KitIssueTotals totals) {
        if (kit.getInternalName().equals(person.getArmorKitName())) {
            person.setIntendedArmorKitName(null);
            return false;
        }
        if (issueFromStock(person, kit, campaign)) {
            person.setIntendedArmorKitName(null);
            totals.issued++;
            return true;
        }
        if (!kit.getInternalName().equals(person.getIntendedArmorKitName())) {
            person.setIntendedArmorKitName(kit.getInternalName());
            shortfall.merge(kit, 1, Integer::sum);
        }
        return false;
    }

    /**
     * The default armor kit for this person's group, or {@code null} if they cannot wear one, their group has no
     * default (coveralls), or the kit is unknown.
     */
    private static @Nullable EquipmentType defaultKit(Person person, Campaign campaign) {
        if (!ArmorKitCatalog.canBeIssuedKit(person)) {
            return null;
        }
        CampaignOption<String> option = defaultKitOption(ArmorKitCatalog.categoryFor(person));
        String kitName = (option == null) ? null : campaign.getCampaignOptions().get(option);
        if ((kitName == null) || kitName.equals(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)) {
            return null;
        }
        return EquipmentType.get(kitName);
    }

    /**
     * Equips every active player MekWarrior who is not already wearing the given kit with it, directly — a one-off
     * convenience granted when the deployment requirement is first enabled, so the force is not immediately grounded.
     * No stock is consumed and nothing is charged; the kit is simply set on each MekWarrior and pushed to their crew
     * slot.
     *
     * @param campaign        the campaign whose player MekWarriors are equipped
     * @param kitInternalName the internal name of the MekWarrior kit to issue
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipAllMekWarriorsWithKit(Campaign campaign, String kitInternalName) {
        equipAllWithKit(campaign, kitInternalName,
              person -> ArmorKitCatalog.categoryFor(person) == ArmorKitCatalog.Category.MEKWARRIOR);
    }

    /**
     * Equips every active player aerospace pilot who is not already wearing the Aerospace Fighter Pilot Kit with it,
     * directly — a one-off convenience granted when the aerospace deployment requirement is first enabled, so the
     * fighters are not immediately grounded. No stock is consumed and nothing is charged.
     *
     * @param campaign the campaign whose player aerospace pilots are equipped
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void equipAllAerospacePilotsWithKit(Campaign campaign) {
        equipAllWithKit(campaign, ArmorKitCatalog.KIT_AEROSPACE_PILOT,
              person -> person.getPrimaryRole().isAerospacePilot() || person.getSecondaryRole().isAerospacePilot());
    }

    /** Sets the kit directly on every active person matching the filter who is not already wearing it. */
    private static void equipAllWithKit(Campaign campaign, String kitInternalName, Predicate<Person> eligible) {
        if (EquipmentType.get(kitInternalName) == null) {
            return;
        }
        for (Person person : campaign.getPlayerForce().getPersonnel().values()) {
            if (!person.getStatus().isActive()
                      || !eligible.test(person)
                      || kitInternalName.equals(person.getArmorKitName())) {
                continue;
            }
            person.setIntendedArmorKitName(null);
            person.setArmorKitName(kitInternalName);
            MekHQ.triggerEvent(new PersonChangedEvent(person));
            if (person.getUnit() != null) {
                person.getUnit().resetPilotAndEntity();
            }
        }
    }

    /**
     * @param category an armor kit group
     *
     * @return the campaign option holding that group's default kit, or {@code null} for soldiers, who have no
     *       per-recruit default (their platoon's kit is issued to the unit instead)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable CampaignOption<String> defaultKitOption(ArmorKitCatalog.Category category) {
        return switch (category) {
            case MEKWARRIOR -> CampaignOption.MEKWARRIOR_DEFAULT_KIT;
            case AIRCRAFT -> CampaignOption.AIRCRAFT_DEFAULT_KIT;
            case INFANTRY -> CampaignOption.VEHICLE_CREW_DEFAULT_KIT;
            case SOLDIER -> null;
        };
    }
}
