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
 * NOTICE: The MekHQ organization is a non-profit group of volunteers
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
package mekhq.campaign.universe.commandGeneration;

import static mekhq.campaign.universe.commandGeneration.SupportTOEFormationTypes.HQ_FORMATION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.unit.Unit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Utility class responsible for inserting support units into a campaign's TOE.
 *
 * <p>Support units are grouped into a new sub-{@link Formation} whose label and type are derived from a
 * {@link SupportTOEFormationTypes} descriptor. The sub-formation is attached as a child of the campaign's HQ formation,
 * which is located or created on demand.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public class AddSupportUnitsToTOE {
    private static final MMLogger LOGGER = MMLogger.create(AddSupportUnitsToTOE.class);


    /**
     * Adds the given support {@link Unit} list to the campaign's TOE under the HQ formation.
     *
     * <p>A new sub-{@link Formation} is created using the label and {@link FormationType} supplied by
     * {@code  supportTOEFormationTypes}, and every unit in {@code units} is registered within it. The sub-formation is
     * then attached to the campaign's HQ formation, which is retrieved or created via
     * {@link #getHqFormation(Campaign)}.</p>
     *
     * @param campaign                 the active {@link Campaign} that owns the TOE
     * @param units                    the list of support {@link Unit} objects to add; must not be {@code null}, but
     *                                 may be empty
     * @param supportTOEFormationTypes the {@link SupportTOEFormationTypes} descriptor that provides the formation label
     *                                 and type for the new sub-formation
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static void addSupportUnitsToTOE(Campaign campaign, List<Unit> units,
          SupportTOEFormationTypes supportTOEFormationTypes) {
        if (units.isEmpty()) {
            return;
        }

        Formation hqFormation = getHqFormation(campaign);

        FormationType type = supportTOEFormationTypes.getType();
        String label = supportTOEFormationTypes.getLabel();
        createSubFormation(campaign, label, type, units, hqFormation);
    }

    /**
     * Creates a new {@link Formation} populated with the given units.
     *
     * <p>The formation is assigned the provided {@code label} as its display name and configured with the given
     * {@link FormationType}. Each unit's ID is then registered with the formation. The formation type is largely
     * presentational for support formations and does not affect gameplay calculations.</p>
     *
     * @param campaign    the campaign context, used to register the units with the formation
     * @param label       the display name for the new formation
     * @param type        the {@link FormationType} to assign to the formation
     * @param units       the list of {@link Unit} objects whose IDs will be added to the formation; must not be
     *                    {@code null}, but may be empty
     * @param hqFormation the HQ {@link Formation} to attach the new formation to; must not be {@code null}
     *
     * @return a fully populated, non-{@code null} {@link Formation} ready to be registered with the campaign
     *
     * @author Illiani
     * @since 0.51.0
     */
    /**
     * Adds units to a formation nested one level deeper than {@link #addSupportUnitsToTOE}, so related
     * sub-commands can be grouped under a shared parent rather than sitting as siblings directly under
     * the HQ.
     *
     * <p>Both the parent and the child are reused when they already exist, matching the top-up
     * behaviour of the flat version: regenerating against a grown force lands in the existing
     * formations instead of creating duplicates beside them.</p>
     *
     * @param campaign the active campaign that owns the TOE
     * @param units    the units to place in the child formation; may be empty, in which case the
     *                 formations are still created so the structure is present
     * @param parent   the umbrella formation created under the HQ
     * @param child    the formation the units are placed in, created under {@code parent}
     */
    public static void addSupportUnitsToTOE(Campaign campaign, List<Unit> units,
          SupportTOEFormationTypes parent, SupportTOEFormationTypes child) {
        Formation hqFormation = getHqFormation(campaign);
        Formation parentFormation = findOrCreateChild(campaign, hqFormation, parent.getLabel(),
              parent.getType());
        createSubFormation(campaign, child.getLabel(), child.getType(), units, parentFormation);
    }

    /**
     * Files support units into numbered sub-formations under their capability's formation, so a command's recovery
     * vehicles and cargo trucks read as the lances or Stars they are fielded as rather than as one long list.
     *
     * <p>A command given twelve recovery vehicles gets three lances of four under Recovery Operations, not twelve
     * vehicles under a single marker. A formation of one lance or fewer is that lance, so its vehicles stay filed
     * directly under it; see {@link #arrangeIntoLances}.</p>
     *
     * <p>Units fill the lowest numbered sub-formation with room in it before a new one is created, so regenerating
     * support against a grown force tops up the last part-filled lance rather than opening a new one beside it.</p>
     *
     * @param campaign            the active campaign that owns the TOE
     * @param units               the units to file; an empty list does nothing
     * @param formationTypes      the capability formation the sub-formations sit under
     * @param subFormationSize  units per sub-formation; zero or less files them flat instead
     * @param subFormationNamer the name for the sub-formation at a given position, counting from one
     */
    public static void addSupportUnitsToTOE(Campaign campaign, List<Unit> units,
          SupportTOEFormationTypes formationTypes, int subFormationSize, IntFunction<String> subFormationNamer) {
        if (units.isEmpty()) {
            return;
        }
        if (subFormationSize <= 0) {
            addSupportUnitsToTOE(campaign, units, formationTypes);
            return;
        }

        Formation hqFormation = getHqFormation(campaign);
        Formation capabilityFormation = findOrCreateChild(campaign, hqFormation, formationTypes.getLabel(),
              formationTypes.getType());

        for (Unit unit : units) {
            campaign.getPlayerForce().addUnitToFormation(unit, capabilityFormation.getId(), campaign);
        }
        arrangeIntoLances(campaign, capabilityFormation, subFormationSize, subFormationNamer,
              formationTypes.getType());
    }

    /**
     * Arranges the vehicles a support formation holds into lances once it holds more than one lance's worth, so the
     * order of battle reads as a tree: a company of lances, not a company holding eleven vehicles.
     *
     * <p>A formation of one lance or fewer is itself that lance and is left flat. Past that, every vehicle filed
     * directly under it moves into the lowest numbered lance with room, and new lances are opened as needed. Vehicles
     * already in a lance stay where they are, so losing a vehicle never reshuffles the others; a lance left with
     * nothing in it is removed. Support carriers - the squads and platoons that carry the support staff - are never
     * moved.</p>
     *
     * @param campaign  the campaign that owns the TOE
     * @param group     the formation whose vehicles are arranged
     * @param lanceSize vehicles in a full lance; zero or less leaves the formation as it is
     * @param namer     the name of the lance at a given position, counting from one
     * @param type      the formation type to give a new lance
     *
     * @return the lances created; empty when none were needed
     */
    public static List<Formation> arrangeIntoLances(Campaign campaign, Formation group, int lanceSize,
          IntFunction<String> namer, FormationType type) {
        List<Formation> created = new ArrayList<>();
        if (lanceSize <= 0) {
            return created;
        }
        removeEmptyLances(campaign, group, namer);

        List<Unit> looseVehicles = new ArrayList<>();
        for (UUID unitId : group.getUnits()) {
            Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && !unit.isCarrier()) {
                looseVehicles.add(unit);
            }
        }
        boolean hasLances = false;
        for (Formation child : group.getSubFormations()) {
            if (isLanceName(child.getName(), namer)) {
                hasLances = true;
                break;
            }
        }
        if (looseVehicles.isEmpty() || (!hasLances && (looseVehicles.size() <= lanceSize))) {
            return created;
        }

        Set<Integer> before = new HashSet<>();
        for (Formation child : group.getSubFormations()) {
            before.add(child.getId());
        }
        for (Unit vehicle : looseVehicles) {
            Formation lance = nextSubFormationWithRoom(campaign, group, namer, lanceSize, type, looseVehicles.size());
            campaign.getPlayerForce().addUnitToFormation(vehicle, lance.getId(), campaign);
        }
        for (Formation child : group.getSubFormations()) {
            if (!before.contains(child.getId())) {
                created.add(child);
            }
        }
        LOGGER.info("[SupportTOE] '{}': {} vehicle(s) filed into lances of {}, {} new lance(s)", group.getName(),
              looseVehicles.size(), lanceSize, created.size());
        return created;
    }

    /** Removes the lances under {@code group} that no longer hold anything. */
    private static void removeEmptyLances(Campaign campaign, Formation group, IntFunction<String> namer) {
        for (Formation child : new ArrayList<>(group.getSubFormations())) {
            if (isLanceName(child.getName(), namer) && child.getUnits().isEmpty()
                      && child.getSubFormations().isEmpty()) {
                LOGGER.info("[SupportTOE] '{}': removing empty lance '{}'", group.getName(), child.getName());
                campaign.getPlayerForce().removeFormation(child, campaign);
            }
        }
    }

    /** The most lances a support formation is looked through for, well past what any command fields. */
    private static final int MAXIMUM_LANCES = 26;

    /**
     * Whether a formation name is one the lance namer produces, which is how a lance this class made is told apart
     * from a formation the player named.
     *
     * @param name  the formation's name
     * @param namer the name of the lance at a given position, counting from one
     *
     * @return {@code true} if the name is one of the namer's
     */
    public static boolean isLanceName(@Nullable String name, IntFunction<String> namer) {
        if (name == null) {
            return false;
        }
        for (int position = 1; position <= MAXIMUM_LANCES; position++) {
            if (name.equalsIgnoreCase(namer.apply(position))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The lowest numbered sub-formation of {@code parent} that still has room, creating the next one when every
     * existing sub-formation is full.
     *
     * @param campaign         the campaign whose formations are searched
     * @param parent           the capability formation the sub-formations sit under
     * @param namer            the name for the sub-formation at a given position, counting from one
     * @param subFormationSize units a sub-formation holds when full
     * @param type             the formation type to give a newly created sub-formation
     * @param unitsToFile      how many units are being filed, which bounds the search
     *
     * @return a sub-formation with room in it, never {@code null}
     */
    private static Formation nextSubFormationWithRoom(Campaign campaign, Formation parent,
          IntFunction<String> namer, int subFormationSize, FormationType type, int unitsToFile) {
        // Bounded rather than open ended: the units being filed cannot need more sub-formations than there are
        // units, even if every existing one is full.
        int searchLimit = unitsToFile + 1;
        for (int number = 1; number <= searchLimit; number++) {
            String label = namer.apply(number);
            Formation existing = findChildFormationByName(campaign, parent, label);
            if (existing == null) {
                return findOrCreateChild(campaign, parent, label, type);
            }
            if (existing.getUnits().size() < subFormationSize) {
                return existing;
            }
        }
        // Unreachable while the limit above holds, but a formation must be returned rather than a null.
        return findOrCreateChild(campaign, parent, namer.apply(searchLimit + 1), type);
    }

    /** Finds the named child of {@code parent}, creating and attaching it when absent. */
    private static Formation findOrCreateChild(Campaign campaign, Formation parent, String label,
          FormationType type) {
        Formation existing = findChildFormationByName(campaign, parent, label);
        if (existing != null) {
            return existing;
        }
        Formation created = new Formation(label);
        campaign.getPlayerForce().addFormation(created, parent, campaign);
        created.setFormationType(type, true);
        return created;
    }

    private static void createSubFormation(Campaign campaign, String label, FormationType type,
          List<Unit> units, Formation hqFormation) {
        // Reuse an existing sub-formation of this label under the HQ, so a top-up generation (for
        // example one more canteen when support is regenerated against a grown force) lands in the
        // existing formation rather than creating a duplicate alongside it.
        Formation subFormation = findChildFormationByName(campaign, hqFormation, label);
        if (subFormation == null) {
            subFormation = new Formation(label);
            // needs to be before we add units
            campaign.getPlayerForce().addFormation(subFormation, hqFormation, campaign);
            subFormation.setFormationType(type, true); //subtype propagation is largely irrelevant
        }

        int subFormationId = subFormation.getId();
        for (Unit unit : units) {
            campaign.getPlayerForce().addUnitToFormation(unit, subFormationId, campaign);
        }
    }

    /**
     * Finds the child {@link Formation} of {@code parent} whose name matches {@code label}
     * (case-insensitive), or {@code null} if none exists. Used to reuse an existing support
     * sub-formation instead of creating a duplicate on a top-up generation.
     *
     * @param campaign the campaign whose formations are searched
     * @param parent   the formation whose children are considered
     * @param label    the sub-formation label to match
     *
     * @return the matching child formation, or {@code null}
     */
    private static @Nullable Formation findChildFormationByName(Campaign campaign, Formation parent,
          String label) {
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            Formation formationParent = formation.getParentFormation();
            if (formationParent != null && formationParent.getId() == parent.getId()
                      && formation.getName().equalsIgnoreCase(label)) {
                return formation;
            }
        }
        return null;
    }

    /**
     * Retrieves the campaign's HQ {@link Formation}, creating it if one does not already exist.
     *
     * <p>All existing formations are searched for a name that matches {@link SupportTOEFormationTypes#HQ_FORMATION}'s
     * label (case-insensitive). If a match is found, it is returned immediately. Otherwise, a new HQ formation is
     * created, attached to the campaign's origin formation ({@link Formation#FORMATION_ORIGIN}), and returned.</p>
     *
     * @param campaign the active {@link Campaign} whose formation list is searched and potentially modified
     *
     * @return the existing or newly created HQ {@link Formation}; never {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    /**
     * The formation a campaign already keeps its support under, if it has one.
     *
     * <p>A campaign built by hand rarely has a formation called "Headquarters" - it has a Command Battalion, a
     * Support Group, a Train, whatever the player named it - but that formation is recognisable by what hangs off it:
     * the logistics, salvage, security and support formations. Adding a second headquarters beside it would leave the
     * campaign with two support structures that neither the player nor the game can tell apart.</p>
     *
     * <p>The winner is the formation with the most support-typed formations under it, needing at least two so a
     * single stray convoy does not claim the job. A tie goes to the first found, which keeps the answer stable across
     * loads because the TOE is walked in order.</p>
     *
     * @param campaign the campaign whose TOE is searched
     *
     * @return the formation that already holds the campaign's support, or {@code null} if nothing qualifies
     */
    static @Nullable Formation findSupportHome(Campaign campaign) {
        Formation best = null;
        int bestCount = 0;
        for (Formation candidate : campaign.getPlayerForce().getAllFormations()) {
            int supportChildren = 0;
            for (Formation child : candidate.getAllSubFormations()) {
                if (isSupportType(child)) {
                    supportChildren++;
                }
            }
            // The root holds everything, so it always wins on count and never means anything.
            if ((candidate.getParentFormation() != null) && (supportChildren > bestCount)) {
                best = candidate;
                bestCount = supportChildren;
            }
        }
        return (bestCount >= 2) ? best : null;
    }

    /**
     * The formation a capability's units were filed into under the HQ, without creating anything.
     *
     * @param campaign       the campaign whose TOE is searched
     * @param formationTypes the capability formation
     *
     * @return the formation, or {@code null} when the campaign has none
     */
    public static @Nullable Formation findCapabilityFormation(Campaign campaign,
          SupportTOEFormationTypes formationTypes) {
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            Formation parent = formation.getParentFormation();
            if ((parent != null) && parent.getName().equalsIgnoreCase(HQ_FORMATION.getLabel())
                      && formation.getName().equalsIgnoreCase(formationTypes.getLabel())) {
                return formation;
            }
        }
        return null;
    }

    /** @return {@code true} if this formation is one of the support kinds, rather than a fighting formation */
    private static boolean isSupportType(Formation formation) {
        return formation.isFormationType(FormationType.SUPPORT)
                     || formation.isFormationType(FormationType.SALVAGE)
                     || formation.isFormationType(FormationType.CONVOY)
                     || formation.isFormationType(FormationType.SECURITY);
    }

    static @NonNull Formation getHqFormation(Campaign campaign) {
        final Formation ORIGIN_FORMATION = campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN);

        // I would prefer to not use string comparison here, but we don't have a more reliable option
        final String HQ_FORMATION_NAME = HQ_FORMATION.getLabel();

        List<Formation> formations = campaign.getPlayerForce().getAllFormations();
        for (Formation formation : formations) {
            if (formation.getName().equalsIgnoreCase(HQ_FORMATION.getLabel())) {
                return formation;
            }
        }

        Formation newFormation = new Formation(HQ_FORMATION_NAME);
        campaign.getPlayerForce().addFormation(newFormation, ORIGIN_FORMATION, campaign);

        return newFormation;
    }
}
