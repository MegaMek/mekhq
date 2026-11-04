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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.unit.Unit;

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
     * Files support units under their capability's formation, grouped the way the command's combat formations are: a
     * command's recovery vehicles and cargo trucks read as the lances, companies and battalions they are fielded as
     * rather than as one long list.
     *
     * <p>A command given twelve recovery vehicles gets three lances of four under Recovery Operations; given thirty, it
     * gets three companies of lances. A formation of one lance or fewer is that lance, so its vehicles stay filed
     * directly under it; see {@link #arrangeIntoEchelons}.</p>
     *
     * <p>Units fill the lowest numbered formation with room in it before a new one is created, so regenerating
     * support against a grown force tops up the last part-filled lance rather than opening a new one beside it.</p>
     *
     * @param campaign       the active campaign that owns the TOE
     * @param units          the units to file; an empty list does nothing
     * @param formationTypes the capability formation the sub-formations sit under
     * @param echelons       how the vehicles are grouped; {@code null} files them flat instead
     */
    public static void addSupportUnitsToTOE(Campaign campaign, List<Unit> units,
          SupportTOEFormationTypes formationTypes, @Nullable SupportEchelons echelons) {
        if (units.isEmpty()) {
            return;
        }
        if ((echelons == null) || (echelons.sizeOf(1) <= 0)) {
            addSupportUnitsToTOE(campaign, units, formationTypes);
            return;
        }

        Formation hqFormation = getHqFormation(campaign);
        Formation capabilityFormation = findOrCreateChild(campaign, hqFormation, formationTypes.getLabel(),
              formationTypes.getType());

        for (Unit unit : units) {
            campaign.getPlayerForce().addUnitToFormation(unit, capabilityFormation.getId(), campaign);
        }
        arrangeIntoEchelons(campaign, capabilityFormation, echelons, formationTypes.getType());
    }

    /**
     * As {@link #addSupportUnitsToTOE(Campaign, List, SupportTOEFormationTypes, SupportEchelons)}, filing into lances
     * with no grouping above them.
     *
     * @param campaign          the active campaign that owns the TOE
     * @param units             the units to file; an empty list does nothing
     * @param formationTypes    the capability formation the lances sit under
     * @param subFormationSize  units per lance; zero or less files them flat instead
     * @param subFormationNamer the name for the lance at a given position, counting from one
     */
    public static void addSupportUnitsToTOE(Campaign campaign, List<Unit> units,
          SupportTOEFormationTypes formationTypes, int subFormationSize, IntFunction<String> subFormationNamer) {
        addSupportUnitsToTOE(campaign, units, formationTypes,
              SupportEchelons.lancesOnly(subFormationSize, subFormationNamer));
    }

    /**
     * Arranges the vehicles a support formation holds into lances, with no grouping above them. See
     * {@link #arrangeIntoEchelons}.
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
        return arrangeIntoEchelons(campaign, group, SupportEchelons.lancesOnly(lanceSize, namer), type);
    }

    /**
     * Arranges the vehicles a support formation holds into the command's echelons, so the order of battle reads as a
     * tree: a battalion of companies of lances, not a formation holding thirty vehicles.
     *
     * <p>A formation of one lance or fewer is itself that lance and is left flat. Past that, every vehicle filed
     * directly under it moves into the lowest numbered lance with room, and new lances are opened as needed. Once
     * there are more lances than make a company, they are grouped into companies the same way, and companies into
     * battalions. Vehicles already in a lance stay where they are, so losing a vehicle never reshuffles the others,
     * and a lance or company left with nothing in it is removed. Support carriers - the squads and platoons that carry
     * the support staff - are never moved.</p>
     *
     * <p>Convoy, Salvage and Security describe the team doing the job, so they stay on the lances only: the companies
     * and battalions above them, and the group itself once it holds lances, are Standard formations. See
     * {@link #isLeafOnlyType}.</p>
     *
     * @param campaign the campaign that owns the TOE
     * @param group    the formation whose vehicles are arranged
     * @param echelons how the vehicles are grouped
     * @param type     the formation type to give a new lance; a group already lanced as Convoy, Salvage or Security
     *                 keeps its lances' type even when its own (now Standard) type is passed
     *
     * @return the formations created, lances and the companies and battalions above them; empty when none were needed
     */
    public static List<Formation> arrangeIntoEchelons(Campaign campaign, Formation group, SupportEchelons echelons,
          FormationType type) {
        List<Formation> created = new ArrayList<>();
        if (echelons.sizeOf(1) <= 0) {
            return created;
        }
        removeEmptyEchelons(campaign, group, echelons);
        // A group already split into lances has been reset to Standard, so on a top-up its own type no longer says
        // what its lances are for; the lances themselves still do.
        FormationType lanceType = isLeafOnlyType(type) ? type : existingLanceType(group, echelons, type);
        FormationType groupingType = isLeafOnlyType(lanceType) ? FormationType.STANDARD : lanceType;

        fileLooseVehiclesIntoLances(campaign, group, echelons, lanceType, created);
        for (int echelon = 2; echelon <= echelons.depth(); echelon++) {
            groupLooseFormations(campaign, group, echelons, echelon, groupingType, created);
        }
        applyEchelonLevels(group, echelons);
        settleLeafOnlyType(group, lanceType);
        return created;
    }

    private static void fileLooseVehiclesIntoLances(Campaign campaign, Formation group, SupportEchelons echelons,
          FormationType type, List<Formation> created) {
        List<Unit> looseVehicles = new ArrayList<>();
        for (UUID unitId : group.getUnits()) {
            Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && !unit.isCarrier()) {
                looseVehicles.add(unit);
            }
        }
        int lanceSize = echelons.sizeOf(1);
        List<Formation> lances = echelonsUnder(group, echelons, 1);
        if (looseVehicles.isEmpty() || (lances.isEmpty() && (looseVehicles.size() <= lanceSize))) {
            return;
        }

        int newLances = 0;
        for (Unit vehicle : looseVehicles) {
            Formation lance = firstWithRoom(lances, lanceSize, formation -> formation.getUnits().size());
            if (lance == null) {
                lance = createEchelon(campaign, group, echelons, 1, type);
                lances.add(lance);
                created.add(lance);
                newLances++;
            }
            campaign.getPlayerForce().addUnitToFormation(vehicle, lance.getId(), campaign);
        }
        LOGGER.info("[SupportTOE] formation #{}: {} vehicle(s) filed into lances of {}, {} new lance(s)", group.getId(),
              looseVehicles.size(), lanceSize, newLances);
    }

    /**
     * Groups the formations of the echelon below {@code echelon} that sit directly under {@code group} into formations
     * of {@code echelon}, once there are more of them than make one. Existing groupings are filled before new ones are
     * opened, so a top-up never reshuffles formations already grouped. A formation that joins a grouping is renumbered
     * within it.
     */
    private static void groupLooseFormations(Campaign campaign, Formation group, SupportEchelons echelons,
          int echelon, FormationType type, List<Formation> created) {
        List<Formation> loose = new ArrayList<>();
        for (Formation child : group.getSubFormations()) {
            if (echelons.echelonOf(child) == (echelon - 1)) {
                loose.add(child);
            }
        }
        int size = echelons.sizeOf(echelon);
        List<Formation> containers = echelonsUnder(group, echelons, echelon);
        if (loose.isEmpty() || (size <= 0) || (containers.isEmpty() && (loose.size() <= size))) {
            return;
        }

        int newContainers = 0;
        for (Formation formation : loose) {
            Formation container = firstWithRoom(containers, size,
                  candidate -> countEchelonChildren(candidate, echelons, echelon - 1));
            if (container == null) {
                container = createEchelon(campaign, group, echelons, echelon, type);
                containers.add(container);
                created.add(container);
                newContainers++;
            }
            // Numbered within the formation it joins - Able, Baker, Charlie in every company - however it was
            // numbered in the row it came from.
            formation.setName(echelons.nameOf(echelon - 1, nextFreePosition(container, echelons, echelon - 1)));
            moveKeepingParentTypes(campaign, formation, container);
        }
        LOGGER.info("[SupportTOE] formation #{}: {} formation(s) grouped at echelon {} in groups of {}, {} new",
              group.getId(), loose.size(), echelon, size, newContainers);
    }

    /** Creates the next numbered formation of {@code echelon} directly under {@code parent}. */
    private static Formation createEchelon(Campaign campaign, Formation parent, SupportEchelons echelons, int echelon,
          FormationType type) {
        String label = echelons.nameOf(echelon, nextFreePosition(parent, echelons, echelon));
        return findOrCreateChild(campaign, parent, label, type);
    }

    /** The lowest position whose name no child of {@code parent} already carries at {@code echelon}. */
    private static int nextFreePosition(Formation parent, SupportEchelons echelons, int echelon) {
        Set<String> taken = new HashSet<>();
        for (Formation child : parent.getSubFormations()) {
            taken.add(child.getName().toLowerCase());
        }
        int position = 1;
        while (taken.contains(echelons.nameOf(echelon, position).toLowerCase())) {
            position++;
        }
        return position;
    }

    /** The first formation in order with fewer than {@code size} in it, or {@code null} when every one is full. */
    private static @Nullable Formation firstWithRoom(List<Formation> formations, int size,
          ToIntFunction<Formation> fill) {
        for (Formation formation : formations) {
            if (fill.applyAsInt(formation) < size) {
                return formation;
            }
        }
        return null;
    }

    private static int countEchelonChildren(Formation formation, SupportEchelons echelons, int echelon) {
        int count = 0;
        for (Formation child : formation.getSubFormations()) {
            if (echelons.echelonOf(child) == echelon) {
                count++;
            }
        }
        return count;
    }

    /**
     * Every formation of {@code echelon} the ladder made under {@code group}, in TOE order. Only the ladder's own
     * formations are looked into, so a formation the player made, and what is in it, is never touched.
     */
    private static List<Formation> echelonsUnder(Formation group, SupportEchelons echelons, int echelon) {
        List<Formation> found = new ArrayList<>();
        collectEchelons(group, echelons, echelon, found);
        return found;
    }

    private static void collectEchelons(Formation formation, SupportEchelons echelons, int echelon,
          List<Formation> found) {
        for (Formation child : formation.getSubFormations()) {
            int childEchelon = echelons.echelonOf(child);
            if (childEchelon == echelon) {
                found.add(child);
            } else if (childEchelon > echelon) {
                collectEchelons(child, echelons, echelon, found);
            }
        }
    }

    /**
     * Gives every formation the ladder made under {@code group} the level of its echelon, and the group the level
     * above the highest echelon it holds. Does nothing for a ladder that sets no levels.
     */
    private static void applyEchelonLevels(Formation group, SupportEchelons echelons) {
        if (echelons.levelAtDepth() == null) {
            return;
        }
        int highest = 0;
        for (int echelon = 1; echelon <= echelons.depth(); echelon++) {
            List<Formation> formations = echelonsUnder(group, echelons, echelon);
            for (Formation formation : formations) {
                formation.setOverrideFormationLevel(echelons.levelOf(echelon));
            }
            if (!formations.isEmpty()) {
                highest = echelon;
            }
        }
        if (highest > 0) {
            group.setOverrideFormationLevel(echelons.levelOf(highest + 1));
        }
    }

    /**
     * Moves {@code formation} under {@code newParent} without changing the type of any formation above it.
     *
     * <p>Moving a Salvage or Standard formation turns every formation above it into a Standard one, which is right when
     * a player moves a salvage lance into a combat company by hand. Here it would turn Support Command, and the
     * headquarters above it, into Standard formations just because a lance was grouped into a company.</p>
     *
     * @param campaign  the campaign that owns the TOE
     * @param formation the formation to move
     * @param newParent the formation it moves under
     */
    static void moveKeepingParentTypes(Campaign campaign, Formation formation, Formation newParent) {
        Map<Formation, FormationType> parentTypes = new LinkedHashMap<>();
        parentTypes.put(newParent, newParent.getFormationType());
        for (Formation parent : newParent.getAllParents()) {
            parentTypes.put(parent, parent.getFormationType());
        }
        campaign.getPlayerForce().moveFormation(formation, newParent, campaign);
        parentTypes.forEach((parent, formationType) -> parent.setFormationType(formationType, false));
    }

    /**
     * Whether a formation type describes the units doing the job - a convoy, a salvage team, a security detail - and so
     * belongs only on the bottom-most formations. A company of three salvage lances is three salvage teams; marking the
     * company Salvage as well makes it one team of every vehicle under it, and a Convoy company is treated as one
     * convoy that is always intercepted.
     *
     * @param type the formation type
     *
     * @return {@code true} for Convoy, Salvage and Security
     */
    static boolean isLeafOnlyType(@Nullable FormationType type) {
        return (type == FormationType.CONVOY) || (type == FormationType.SALVAGE) || (type == FormationType.SECURITY);
    }

    /**
     * The leaf-only type the lances under {@code group} already carry, or {@code fallback} when it has none.
     */
    private static FormationType existingLanceType(Formation group, SupportEchelons echelons,
          FormationType fallback) {
        for (Formation lance : echelonsUnder(group, echelons, 1)) {
            if (isLeafOnlyType(lance.getFormationType())) {
                return lance.getFormationType();
            }
        }
        return fallback;
    }

    /**
     * Keeps a leaf-only type on the bottom-most formation: a group that now holds formations becomes Standard, while a
     * group left holding only vehicles (its lances emptied and removed) is the team itself again and takes the type
     * back. Does nothing when {@code leafType} is not a leaf-only type.
     *
     * @param group    the formation whose vehicles were arranged
     * @param leafType the type its bottom-most formations carry
     */
    private static void settleLeafOnlyType(Formation group, FormationType leafType) {
        if (!isLeafOnlyType(leafType)) {
            return;
        }
        boolean holdsFormations = !group.getSubFormations().isEmpty();
        if (holdsFormations && isLeafOnlyType(group.getFormationType())) {
            group.setFormationType(FormationType.STANDARD, false);
        } else if (!holdsFormations && group.getFormationType().isStandard() && !group.getUnits().isEmpty()) {
            group.setFormationType(leafType, false);
        }
    }

    /**
     * Removes the ladder's formations under {@code group} that no longer hold anything, lances first, so a company
     * whose last lance went goes with it.
     */
    private static void removeEmptyEchelons(Campaign campaign, Formation group, SupportEchelons echelons) {
        for (Formation child : new ArrayList<>(group.getSubFormations())) {
            if (echelons.echelonOf(child) == 0) {
                continue;
            }
            removeEmptyEchelons(campaign, child, echelons);
            if (child.getUnits().isEmpty() && child.getSubFormations().isEmpty()) {
                LOGGER.info("[SupportTOE] formation #{}: removing empty formation #{}", group.getId(), child.getId());
                campaign.getPlayerForce().removeFormation(child, campaign);
            }
        }
    }

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
        for (int position = 1; position <= SupportEchelons.MAXIMUM_POSITIONS; position++) {
            if (name.equalsIgnoreCase(namer.apply(position))) {
                return true;
            }
        }
        return false;
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

    static @Nonnull Formation getHqFormation(Campaign campaign) {
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
