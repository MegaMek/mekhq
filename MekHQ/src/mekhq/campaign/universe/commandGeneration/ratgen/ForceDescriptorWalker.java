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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.annotations.Nullable;
import megamek.common.units.UnitType;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer.FormationRequest;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer.NamedFormation;

/**
 * Pre-order traversal of a generated {@link ForceDescriptor} tree that mirrors the structure into MekHQ
 * {@link Formation} nodes and hands each leaf descriptor to a caller-supplied handler.
 *
 * <p>The walker creates one Formation per non-leaf descriptor (Lance, Company, Battalion, Regiment, etc.)
 * via {@link mekhq.campaign.force.PlayerForce#addFormation(Formation, Formation, Campaign)}, recurses into both {@code subForces} and
 * {@code attached} children, and invokes {@link LeafHandler#handle} for each descriptor whose subtree is
 * empty (leaves carry the actual {@code Entity}). The handler is responsible for building the MekHQ
 * {@code Unit}, generating crew, and attaching the unit to the supplied parent Formation.</p>
 *
 * <p>Formation names come from the supplied {@link FormationNamer}, which is handed each sibling group
 * as a whole rather than one formation at a time. That is what lets designator sequences restart under
 * each parent - company letters begin again in every battalion ("1/Alpha Company", "2/Alpha Company") -
 * and what lets a name collision be resolved the same way for every member of a group. Names the
 * faction ruleset already got right, such as Clan "Trinary [Battle]" and ComStar "IV-alpha", are passed
 * through untouched.</p>
 *
 * <p>{@link #previewNames(ForceDescriptor, FormationNamer)} runs the same traversal without touching the
 * campaign and returns the name each descriptor would receive, so the Command Designer's preview tree can
 * show exactly what the committed TO&amp;E will look like. Both entry points share one traversal
 * implementation ({@link #traverse}), which is what guarantees the preview matches the build.</p>
 */
public final class ForceDescriptorWalker {

    private static final MMLogger LOGGER = MMLogger.create(ForceDescriptorWalker.class);

    /**
     * Callback for leaf descriptors. The walker has already created or located the parent {@link Formation}
     * inside the campaign before calling this; the handler creates the Unit and crews it.
     */
    @FunctionalInterface
    public interface LeafHandler {
        /**
         * Handle one leaf descriptor.
         *
         * @param leaf   the leaf {@link ForceDescriptor} (non-null; its {@code getEntity()} is the unit)
         * @param parent the MekHQ {@link Formation} that owns this leaf
         */
        void handle(ForceDescriptor leaf, Formation parent);
    }

    /**
     * Receiver for traversal events, so the campaign build and the name-only preview share one traversal.
     * Handles are opaque: the build sink returns the created {@link Formation}, the preview sink returns
     * {@code null}.
     */
    private interface FormationSink {
        /**
         * A formation node was resolved.
         *
         * @param descriptor   the descriptor the node mirrors, or {@code null} for a synthesized
         *                     loose-platoon company that has no descriptor
         * @param named        the unique name (and designator) chosen for it
         * @param level        the mapped formation level, or {@code null} when the echelon has no mapping
         * @param parentHandle the handle returned for the node's parent, or {@code null} at the root
         * @return the handle passed to this node's children
         */
        @Nullable
        Object formation(@Nullable ForceDescriptor descriptor, NamedFormation named,
              @Nullable FormationLevel level, @Nullable Object parentHandle);

        /**
         * An included leaf (a unit) was reached.
         *
         * @param leaf         the leaf descriptor carrying the entity
         * @param parentHandle the handle of the formation that owns the leaf
         */
        void leaf(ForceDescriptor leaf, @Nullable Object parentHandle);

        /** Whether the traversal should emit its per-node INFO logging (build yes, preview no). */
        boolean verbose();
    }

    private ForceDescriptorWalker() {
        // utility class
    }

    /**
     * Walks the tree rooted at {@code root}. For each non-leaf descriptor, a Formation is created and
     * registered in the campaign under {@code parentInCampaign}. For each leaf, {@code onLeaf} is invoked.
     *
     * @param root              the root descriptor returned by {@code Ruleset.processRoot}
     * @param campaign          the target campaign (receives new Formation nodes)
     * @param parentInCampaign  the Formation under which to root the generated tree (typically the
     *                          campaign's force-tree root)
     * @param namer             produces campaign-wide unique names for the created Formations
     * @param onLeaf            handler invoked for each leaf descriptor
     * @return total number of leaves visited
     */
    public static int walk(ForceDescriptor root, Campaign campaign, Formation parentInCampaign,
          FormationNamer namer, LeafHandler onLeaf) {
        if (root == null) {
            LOGGER.warn("[CompanyGen][Walker] walk called with null root; returning 0");
            return 0;
        }
        LOGGER.info("[CompanyGen][Walker] walk START rootEchelon={} rootFaction={} parentFormation={} thread={}",
              root.getEchelon(), root.getFaction(),
              parentInCampaign == null ? "null" : parentInCampaign.getName(),
              Thread.currentThread().getName());

        // Merge the root descriptor into the campaign's existing top-level Formation instead of
        // nesting under it. Without this, the user gets a redundant level — their campaign's
        // unit-name Formation (e.g. "The Operations Global") with a single child carrying the
        // ratgen-generated top name (e.g. "Regiment") which then contains the real battalions.
        // After merge: the campaign Formation keeps its user-chosen name, picks up the
        // FormationLevel from the descriptor, and the descriptor's children become direct
        // children of the campaign root.
        boolean mergeRoot = hasChildDescriptors(root) && parentInCampaign != null;
        if (mergeRoot) {
            FormationLevel level = mapEchelonToFormationLevel(root.getEchelon(), root.getFaction());
            if (level != null) {
                parentInCampaign.setFormationLevel(level);
            }
            LOGGER.info("[CompanyGen][Walker] merging root descriptor into existing campaign Formation '{}' (id={} formationLevel={})",
                  parentInCampaign.getName(), parentInCampaign.getId(), level);
        }

        FormationSink buildSink = new FormationSink() {
            @Override
            public Object formation(ForceDescriptor descriptor, NamedFormation named,
                  FormationLevel level, Object parentHandle) {
                Formation formation = new Formation(named.name());
                if (level != null) {
                    formation.setFormationLevel(level);
                }
                Formation parent = (Formation) parentHandle;
                campaign.getPlayerForce().addFormation(formation, parent, campaign);
                LOGGER.info("[CompanyGen][Walker]   Formation registered id={} name='{}' formationLevel={} parentId={}",
                      formation.getId(), named.name(), level,
                      parent == null ? "null" : parent.getId());
                return formation;
            }

            @Override
            public void leaf(ForceDescriptor leaf, Object parentHandle) {
                onLeaf.handle(leaf, (Formation) parentHandle);
            }

            @Override
            public boolean verbose() {
                return true;
            }
        };

        int leaves = traverse(root, namer, buildSink, parentInCampaign, mergeRoot);
        LOGGER.info("[CompanyGen][Walker] walk DONE; {} leaves visited", leaves);
        return leaves;
    }

    /**
     * Runs the naming traversal without creating anything, returning the name every non-leaf descriptor
     * would receive if the tree were committed right now. Because this shares {@link #traverse} with
     * {@link #walk}, the returned names are exactly the names the build will produce given an equally
     * configured {@link FormationNamer}. Synthesized loose-platoon companies have no descriptor and are
     * absent from the map (their platoons still carry the resulting callsigns).
     *
     * @param root  the currently previewed descriptor tree (may be {@code null} for an empty map)
     * @param namer a freshly built namer matching the one the build will use
     * @return descriptor-identity map of final display names; never {@code null}
     */
    public static Map<ForceDescriptor, String> previewNames(@Nullable ForceDescriptor root,
          FormationNamer namer) {
        Map<ForceDescriptor, String> names = new IdentityHashMap<>();
        if (root == null) {
            return names;
        }
        FormationSink previewSink = new FormationSink() {
            @Override
            public Object formation(ForceDescriptor descriptor, NamedFormation named,
                  FormationLevel level, Object parentHandle) {
                if (descriptor != null) {
                    names.put(descriptor, named.name());
                }
                return null;
            }

            @Override
            public void leaf(ForceDescriptor leaf, Object parentHandle) {
                // Preview only names formations; leaves keep their unit labels.
            }

            @Override
            public boolean verbose() {
                return false;
            }
        };
        // The dialog build always merges the root into the campaign's root formation, so the preview
        // must take the same branch for the names to line up.
        traverse(root, namer, previewSink, null, hasChildDescriptors(root));
        LOGGER.debug("[CompanyGen][Walker] previewNames computed for {} formation node(s)", names.size());
        return names;
    }

    /**
     * The shared traversal behind {@link #walk} and {@link #previewNames}. With {@code mergeRoot} the
     * root descriptor itself creates no node: its children hang directly off {@code rootHandle} and its
     * loose attached platoons are wrapped in synthesized unit-type companies. Without it (single-unit
     * roots), the root is walked as a regular node.
     *
     * @return total number of included leaves visited
     */
    private static int traverse(ForceDescriptor root, FormationNamer namer, FormationSink sink,
          @Nullable Object rootHandle, boolean mergeRoot) {
        if (!mergeRoot) {
            List<NamedFormation> rootName = namer.nameSiblings(List.of(requestFor(root)), null);
            return walkNode(root, rootName.get(0), namer, sink, rootHandle, null, 0);
        }

        // RATGenerator often hands support/attachment forces in as loose platoons directly under the
        // root (a stack of BA or infantry platoons with no company wrapper). Group those by unit type
        // under a synthesized company so the TOE reads as "<Unit Type> Company -> platoons"; anything
        // that is already a proper formation (its children are sub-formations, e.g. an aerospace
        // squadron) walks through as a normal child of the root.
        List<ForceDescriptor> directChildren = new ArrayList<>();
        List<ForceDescriptor> loosePlatoons = new ArrayList<>();
        if (root.getSubForces() != null) {
            directChildren.addAll(root.getSubForces());
        }
        if (root.getAttached() != null) {
            for (ForceDescriptor child : root.getAttached()) {
                if (isLoosePlatoon(child)) {
                    loosePlatoons.add(child);
                } else {
                    directChildren.add(child);
                }
            }
        }

        int leaves = walkChildren(directChildren, namer, sink, rootHandle, null, 0);
        leaves += wrapLoosePlatoonsByUnitType(loosePlatoons, root.getFaction(), namer, sink, rootHandle);
        return leaves;
    }

    /**
     * Names one sibling group and walks it.
     *
     * <p>Naming the whole group in a single call is what lets the namer restart designator sequences
     * under each parent, and what lets it resolve a name collision the same way for every member of
     * the group rather than leaving the first sibling bare and suffixing the rest.</p>
     *
     * @param children         every child of one parent, in tree order; leaves and empty formations may
     *                         be included and are skipped for naming purposes
     * @param parentDesignator the parent's designator, or {@code null} at the top of the command
     *
     * @return the number of included leaves visited beneath {@code children}
     */
    private static int walkChildren(List<ForceDescriptor> children, FormationNamer namer,
          FormationSink sink, @Nullable Object parentHandle, @Nullable String parentDesignator,
          int depth) {
        // Only nodes that will actually become a Formation take part in naming: a unit keeps its own
        // label rather than taking a formation name, and a formation whose units were all excluded is
        // dropped without ever spending a designator.
        List<ForceDescriptor> namedChildren = new ArrayList<>();
        for (ForceDescriptor child : children) {
            if (isFormation(child) && hasIncludedLeaf(child)) {
                namedChildren.add(child);
            }
        }

        List<FormationRequest> requests = new ArrayList<>(namedChildren.size());
        for (ForceDescriptor child : namedChildren) {
            requests.add(requestFor(child));
        }
        List<NamedFormation> names = namer.nameSiblings(requests, parentDesignator);

        int leaves = 0;
        int nameIndex = 0;
        for (ForceDescriptor child : children) {
            NamedFormation named = null;
            if ((nameIndex < namedChildren.size()) && (namedChildren.get(nameIndex) == child)) {
                named = names.get(nameIndex);
                nameIndex++;
            }
            leaves += walkNode(child, named, namer, sink, parentHandle, parentDesignator, depth);
        }
        return leaves;
    }

    /** Describes {@code descriptor} to the namer: its engine name plus the faction and echelon that
     * select the naming rule. */
    private static FormationRequest requestFor(ForceDescriptor descriptor) {
        return new FormationRequest(descriptor.parseName(),
              mapEchelonToFormationLevel(descriptor.getEchelon(), descriptor.getFaction()),
              descriptor.getEchelon(), descriptor.getUnitType(), descriptor.getFaction());
    }

    /**
     * @param named            the name the caller's sibling-group naming pass chose for this node, or
     *                         {@code null} when the node is a unit or an empty formation and so was never named
     * @param parentDesignator the designator of the formation this node sits in, or {@code null} at the top of
     *                         the command; a unit's nested units are named under it
     */
    private static int walkNode(ForceDescriptor descriptor, @Nullable NamedFormation named,
          FormationNamer namer, FormationSink sink, @Nullable Object parentHandle,
          @Nullable String parentDesignator, int depth) {
        String indent = "  ".repeat(depth);
        boolean hasChildren = hasChildDescriptors(descriptor);

        if (!isFormation(descriptor)) {
            int leaves = walkUnit(descriptor, sink, parentHandle, indent);
            if (hasChildren) {
                // A unit with units nested under it: a DropShip and the fighters it carries. The ship is a unit,
                // not a formation, so what it carries hangs off the same formation the ship is in.
                leaves += walkChildren(childrenOf(descriptor), namer, sink, parentHandle, parentDesignator,
                      depth + 1);
            }
            return leaves;
        }

        String engineName = descriptor.parseName();

        // Drop empty formations: if every unit under this node was excluded in the preview, skip the
        // whole subtree so no empty Formation is created (and no designator is spent on it). A single
        // re-included unit keeps it.
        if (!hasIncludedLeaf(descriptor)) {
            if (sink.verbose()) {
                LOGGER.info("[CompanyGen][Walker] {}NODE name='{}' has no included units; dropping empty formation",
                      indent, engineName);
            }
            return 0;
        }

        // Non-leaf — mirror this echelon into a Formation and recurse.
        FormationLevel level = mapEchelonToFormationLevel(descriptor.getEchelon(), descriptor.getFaction());
        if (named == null) {
            // Defensive: every node reaching this point should have been named with its siblings. Name
            // it alone rather than dropping it, and say so, because a silently unnamed formation would
            // be very hard to trace back from a TOE screenshot.
            LOGGER.warn("[CompanyGen][Walker] node '{}' (echelon={}) reached naming without a"
                        + " sibling-group name; naming it in isolation",
                  engineName, descriptor.getEchelon());
            named = namer.nameSiblings(List.of(requestFor(descriptor)), null).get(0);
        }

        if (sink.verbose()) {
            LOGGER.info("[CompanyGen][Walker] {}NODE name='{}' engineName='{}' echelon={} subForces={} attached={}",
                  indent, named.name(), engineName, descriptor.getEchelon(),
                  descriptor.getSubForces() == null ? 0 : descriptor.getSubForces().size(),
                  descriptor.getAttached() == null ? 0 : descriptor.getAttached().size());
        }

        Object handle = sink.formation(descriptor, named, level, parentHandle);

        // Subforces and attached forces are one sibling group for naming purposes: they sit at the same
        // level of the tree, so their designators have to come from a single sequence.
        return walkChildren(childrenOf(descriptor), namer, sink, handle, named.designator(), depth + 1);
    }

    /**
     * Hands one unit to the sink, unless the user excluded it in the preview (right-click -> Exclude from TOE),
     * in which case it is skipped.
     *
     * @return the number of units handed over: one or zero
     */
    private static int walkUnit(ForceDescriptor descriptor, FormationSink sink, @Nullable Object parentHandle,
          String indent) {
        if (!descriptor.isIncluded()) {
            if (sink.verbose()) {
                LOGGER.info("[CompanyGen][Walker] {}LEAF excluded by user, skipping '{}'",
                      indent, unitLabel(descriptor));
            }
            return 0;
        }
        if (sink.verbose()) {
            String entityChassis = descriptor.getEntity() == null ? "n/a" : descriptor.getEntity().getChassis();
            String entityModel = descriptor.getEntity() == null ? "n/a" : descriptor.getEntity().getModel();
            LOGGER.info("[CompanyGen][Walker] {}LEAF parseName='{}' echelon={} unitType={} hasEntity={} hasCo={} chassis='{}' model='{}'",
                  indent, descriptor.parseName(), descriptor.getEchelon(),
                  descriptor.getUnitType(), descriptor.getEntity() != null,
                  descriptor.getCo() != null, entityChassis, entityModel);
        }
        sink.leaf(descriptor, parentHandle);
        return 1;
    }

    /**
     * @return the unit's name for the log: a unit descriptor carries no formation name of its own, so the entity's
     *       short name is what identifies it
     */
    private static String unitLabel(ForceDescriptor descriptor) {
        String name = descriptor.parseName();
        boolean hasName = (name != null) && !name.isBlank();
        if (hasName || (descriptor.getEntity() == null)) {
            return hasName ? name : "(no name)";
        }
        return descriptor.getEntity().getShortName();
    }

    /** Whether {@code descriptor} has any subforce or attached children. */
    private static boolean hasChildDescriptors(ForceDescriptor descriptor) {
        return (descriptor.getSubForces() != null && !descriptor.getSubForces().isEmpty())
              || (descriptor.getAttached() != null && !descriptor.getAttached().isEmpty());
    }

    /**
     * Whether {@code descriptor} stands for a unit rather than a formation. Generation sets an entity on
     * unit descriptors only, and a unit can have descriptors beneath it: a carrier is generated with the
     * fighters it carries nested under it, and it is still the ship, not a formation of fighters.
     *
     * @param descriptor the descriptor to test
     *
     * @return {@code true} if the descriptor carries an entity
     */
    private static boolean carriesUnit(ForceDescriptor descriptor) {
        return descriptor.getEntity() != null;
    }

    /**
     * Whether {@code descriptor} is walked as a formation: it has children and is not itself a unit. A
     * childless descriptor with no entity is a unit the engine failed to load, and is handed over as a
     * unit so the handler can report it.
     *
     * @param descriptor the descriptor to test
     *
     * @return {@code true} if the descriptor becomes a Formation
     */
    private static boolean isFormation(ForceDescriptor descriptor) {
        return hasChildDescriptors(descriptor) && !carriesUnit(descriptor);
    }

    /**
     * Whether {@code descriptor} contains at least one included unit anywhere beneath it. Used to drop
     * formations whose units were all excluded in the preview, while keeping a formation that still has
     * a single re-included unit inside an otherwise-excluded branch.
     *
     * @param descriptor the descriptor to test
     * @return {@code true} if {@code descriptor} or any unit under it is included and has an entity
     */
    private static boolean hasIncludedLeaf(ForceDescriptor descriptor) {
        if (carriesUnit(descriptor) && descriptor.isIncluded()) {
            return true;
        }
        if (!hasChildDescriptors(descriptor)) {
            return false;
        }
        if (descriptor.getSubForces() != null) {
            for (ForceDescriptor child : descriptor.getSubForces()) {
                if (hasIncludedLeaf(child)) {
                    return true;
                }
            }
        }
        if (descriptor.getAttached() != null) {
            for (ForceDescriptor child : descriptor.getAttached()) {
                if (hasIncludedLeaf(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether {@code descriptor} is a "loose platoon": a formation whose children are all leaf units
     * (the smallest grouping, such as a BA or infantry platoon) rather than a proper multi-formation
     * unit. Used to decide which attached nodes to wrap in a synthesized company; a node whose
     * children are themselves formations (for example an aerospace squadron of flights) is not one.
     *
     * @param descriptor the attached descriptor to classify
     * @return {@code true} if the node's children are all leaf units
     */
    private static boolean isLoosePlatoon(ForceDescriptor descriptor) {
        List<ForceDescriptor> children = childrenOf(descriptor);
        if (children.isEmpty()) {
            return false;
        }
        for (ForceDescriptor child : children) {
            if (!childrenOf(child).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The subforce and attached children of {@code descriptor} combined into one list. */
    private static List<ForceDescriptor> childrenOf(ForceDescriptor descriptor) {
        List<ForceDescriptor> children = new ArrayList<>();
        if (descriptor.getSubForces() != null) {
            children.addAll(descriptor.getSubForces());
        }
        if (descriptor.getAttached() != null) {
            children.addAll(descriptor.getAttached());
        }
        return children;
    }

    /**
     * Groups {@code platoons} by unit type and, for each group, synthesizes a company formation named
     * for that unit type (for example "Able Battle Armor Company") under the root handle, then walks the
     * group's platoons beneath it.
     *
     * @param platoons    the loose platoons to wrap (may be empty)
     * @param factionCode the descriptor faction, used to map the company echelon
     * @param namer       produces the unique names for the synthesized companies and their platoons
     * @param sink        the traversal sink receiving the synthesized companies
     * @param rootHandle  the handle the synthesized companies hang under
     *
     * @return the number of leaves visited
     */
    private static int wrapLoosePlatoonsByUnitType(List<ForceDescriptor> platoons, String factionCode,
          FormationNamer namer, FormationSink sink, @Nullable Object rootHandle) {
        if (platoons.isEmpty()) {
            return 0;
        }
        Map<Integer, List<ForceDescriptor>> byUnitType = new LinkedHashMap<>();
        for (ForceDescriptor platoon : platoons) {
            byUnitType.computeIfAbsent(platoon.getUnitType(), key -> new ArrayList<>()).add(platoon);
        }

        // The synthesized companies are siblings of one another, so they are named as one group and
        // draw from a single designator sequence.
        List<FormationRequest> companyRequests = new ArrayList<>(byUnitType.size());
        for (Map.Entry<Integer, List<ForceDescriptor>> entry : byUnitType.entrySet()) {
            // The company sits one echelon above its platoons.
            Integer platoonEchelon = entry.getValue().get(0).getEchelon();
            Integer companyEchelon = (platoonEchelon == null) ? null : platoonEchelon + 1;
            companyRequests.add(new FormationRequest(companyNameForUnitType(entry.getKey()),
                  mapEchelonToFormationLevel(companyEchelon, factionCode), companyEchelon,
                  entry.getKey(), factionCode));
        }
        List<NamedFormation> companyNames = namer.nameSiblings(companyRequests, null);

        int leaves = 0;
        int companyIndex = 0;
        for (Map.Entry<Integer, List<ForceDescriptor>> entry : byUnitType.entrySet()) {
            List<ForceDescriptor> group = entry.getValue();
            NamedFormation named = companyNames.get(companyIndex);
            FormationLevel companyLevel = companyRequests.get(companyIndex).level();
            companyIndex++;

            Object companyHandle = sink.formation(null, named, companyLevel, rootHandle);
            if (sink.verbose()) {
                LOGGER.info("[CompanyGen][Walker] synthesized '{}' (level {}) for {} loose platoon(s)",
                      named.name(), companyLevel, group.size());
            }
            leaves += walkChildren(group, namer, sink, companyHandle, named.designator(), 1);
        }
        return leaves;
    }

    /**
     * A display name for a synthesized company of a unit-type group, e.g. "Battle Armor Company".
     * Falls back to a generic name when the unit type is unknown.
     *
     * @param unitType the {@code UnitType} constant of the group, or {@code null}
     * @return the company display name
     */
    private static String companyNameForUnitType(Integer unitType) {
        if (unitType == null) {
            return "Support Company";
        }
        return UnitType.getTypeDisplayableName(unitType) + " Company";
    }

    /**
     * Maps a Force Generator echelon integer (from {@code data/forcegenerator/faction_rules/constants.txt})
     * to MekHQ's {@link FormationLevel} enum, using the faction code to decide whether the int means IS
     * (LANCE/COMPANY/BATTALION/…), Clan (STAR/BINARY/TRINARY/CLUSTER/…), or ComStar/WoB
     * (LEVEL_II/CHOIR/LEVEL_III/…) since all three families reuse the same integers with different
     * semantics.
     *
     * @param echelon     the {@code ForceDescriptor.echelon} value (boxed, may be null)
     * <p>Package-private so {@link RulesetRankAssigner} can resolve the requested echelon the same
     * way, rather than reading a Formation's own level back after MekHQ has recomputed it.</p>
     *
     * @param echelon     the {@code ForceDescriptor.echelon} value (boxed, may be null)
     * @param factionCode the faction code on the descriptor (e.g. "FS", "CW", "WOB"); may be {@code null}
     * @return the matching {@link FormationLevel}, or {@code null} if the echelon doesn't map
     */
    static FormationLevel mapEchelonToFormationLevel(Integer echelon, String factionCode) {
        if (echelon == null) {
            return null;
        }
        FactionFamily family = resolveFactionFamily(factionCode);
        return switch (family) {
            case CLAN -> switch (echelon) {
                case 2 -> FormationLevel.TEAM;            // Point
                case 3 -> FormationLevel.STAR_OR_NOVA;
                case 4, 5 -> FormationLevel.BINARY_OR_TRINARY;
                case 6 -> FormationLevel.CLUSTER;
                case 7 -> FormationLevel.GALAXY;
                case 8 -> FormationLevel.TOUMAN;
                default -> null;
            };
            case COMSTAR -> switch (echelon) {
                case 2 -> FormationLevel.TEAM;            // Level I
                case 3, 4 -> FormationLevel.LEVEL_II_OR_CHOIR;
                case 5 -> FormationLevel.LEVEL_III;
                case 6 -> FormationLevel.LEVEL_IV;
                case 9 -> FormationLevel.LEVEL_V;
                case 10 -> FormationLevel.LEVEL_VI;
                default -> null;
            };
            case INNER_SPHERE -> switch (echelon) {
                case 2 -> FormationLevel.TEAM;            // Squad / Platoon
                case 3 -> FormationLevel.LANCE;
                case 4 -> FormationLevel.COMPANY;
                case 5 -> FormationLevel.BATTALION;
                case 6 -> FormationLevel.REGIMENT;
                case 7 -> FormationLevel.BRIGADE;
                case 8 -> FormationLevel.DIVISION;
                case 9 -> FormationLevel.CORPS;
                case 10 -> FormationLevel.ARMY;
                default -> null;
            };
        };
    }

    private enum FactionFamily { INNER_SPHERE, CLAN, COMSTAR }

    /**
     * The formation base size that marks a command organised on the Level I to Level VI ladder.
     *
     * <p>ComStar and the Word of Blake build on sixes, and their sub-commands inherit that through
     * {@code fallBackFactions} unless they declare otherwise - which is what tells the Protectorate
     * Militia apart, since it is raised as a conventional planetary force and declares four. Reading
     * the size rather than the faction code keeps that knowledge in the data describing the command,
     * where a new sub-command gets it right without a code change.</p>
     */
    private static final int COMSTAR_LADDER_FORMATION_BASE_SIZE = 6;

    private static FactionFamily resolveFactionFamily(String factionCode) {
        if (factionCode == null || factionCode.isBlank()) {
            return FactionFamily.INNER_SPHERE;
        }
        // Take the first comma-separated token; ratgen sometimes packs an "FS,FedSuns,3030" style code.
        String primary = factionCode.split(",")[0].trim();
        Optional<Faction2> faction = Factions2.getInstance().getFaction(primary);
        if (faction.isPresent()) {
            if (faction.get().getFormationBaseSize() == COMSTAR_LADDER_FORMATION_BASE_SIZE) {
                return FactionFamily.COMSTAR;
            }
            // Clan-ness is asked directly rather than read off the base size. The Clans do build on
            // fives, but so does the Marian Hegemony, and taking a five to mean Clan would have
            // labelled Marian lances and companies as Stars and Clusters.
            if (faction.get().isClan()) {
                return FactionFamily.CLAN;
            }
        }
        // A key the faction data does not carry can still be a ratgen faction, so ask there too.
        FactionRecord factionRecord = RATGenerator.getInstance().getFaction(primary);
        if (factionRecord != null && factionRecord.isClan()) {
            return FactionFamily.CLAN;
        }
        return FactionFamily.INNER_SPHERE;
    }
}
