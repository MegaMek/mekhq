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
package mekhq.campaign.universe.companyGeneration.ratgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;

/**
 * Pre-order traversal of a generated {@link ForceDescriptor} tree that mirrors the structure into MekHQ
 * {@link Formation} nodes and hands each leaf descriptor to a caller-supplied handler.
 *
 * <p>The walker creates one Formation per non-leaf descriptor (Lance, Company, Battalion, Regiment, etc.)
 * via {@link Campaign#addFormation(Formation, Formation)}, recurses into both {@code subForces} and
 * {@code attached} children, and invokes {@link LeafHandler#handle} for each descriptor whose subtree is
 * empty (leaves carry the actual {@code Entity}). The handler is responsible for building the MekHQ
 * {@code Unit}, generating crew, and attaching the unit to the supplied parent Formation.</p>
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
     * @param onLeaf            handler invoked for each leaf descriptor
     * @return total number of leaves visited
     */
    public static int walk(ForceDescriptor root, Campaign campaign, Formation parentInCampaign,
          LeafHandler onLeaf) {
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
        boolean rootHasChildren = (root.getSubForces() != null && !root.getSubForces().isEmpty())
              || (root.getAttached() != null && !root.getAttached().isEmpty());

        int leaves;
        if (rootHasChildren && parentInCampaign != null) {
            FormationLevel level = mapEchelonToFormationLevel(root.getEchelon(), root.getFaction());
            if (level != null) {
                parentInCampaign.setFormationLevel(level);
            }
            LOGGER.info("[CompanyGen][Walker] merged root descriptor into existing campaign Formation '{}' (id={} formationLevel={}); recursing into {} subForces + {} attached",
                  parentInCampaign.getName(), parentInCampaign.getId(), level,
                  root.getSubForces() == null ? 0 : root.getSubForces().size(),
                  root.getAttached() == null ? 0 : root.getAttached().size());

            leaves = 0;
            if (root.getSubForces() != null) {
                for (ForceDescriptor child : root.getSubForces()) {
                    leaves += walkInternal(child, campaign, parentInCampaign, onLeaf, 0);
                }
            }
            if (root.getAttached() != null) {
                // RATGenerator often hands support/attachment forces in as loose platoons directly
                // under the root (a stack of BA or infantry platoons with no company wrapper). Group
                // those by unit type under a synthesized company so the TOE reads as
                // "<Unit Type> Company -> platoons"; anything that is already a proper formation
                // (its children are sub-formations, e.g. an aerospace squadron) walks through as-is.
                List<ForceDescriptor> loosePlatoons = new ArrayList<>();
                for (ForceDescriptor child : root.getAttached()) {
                    if (isLoosePlatoon(child)) {
                        loosePlatoons.add(child);
                    } else {
                        LOGGER.info("[CompanyGen][Walker]   (attached child of root)");
                        leaves += walkInternal(child, campaign, parentInCampaign, onLeaf, 0);
                    }
                }
                leaves += wrapLoosePlatoonsByUnitType(loosePlatoons, campaign, parentInCampaign,
                      root.getFaction(), onLeaf);
            }
        } else {
            // Edge cases: the root descriptor is itself a leaf (single-unit generation), or no
            // existing campaign Formation was supplied. Fall through to the normal walkInternal
            // path which creates a Formation for the root and proceeds.
            leaves = walkInternal(root, campaign, parentInCampaign, onLeaf, 0);
        }

        LOGGER.info("[CompanyGen][Walker] walk DONE; {} leaves visited", leaves);
        return leaves;
    }

    private static int walkInternal(ForceDescriptor descriptor, Campaign campaign, Formation parent,
          LeafHandler onLeaf, int depth) {
        String indent = "  ".repeat(depth);
        boolean hasChildren = (descriptor.getSubForces() != null && !descriptor.getSubForces().isEmpty())
              || (descriptor.getAttached() != null && !descriptor.getAttached().isEmpty());

        if (!hasChildren) {
            // Leaf — let the caller turn it into a Unit + crew, unless the user excluded it in the
            // preview (right-click -> Exclude from TOE), in which case it is skipped entirely.
            if (!descriptor.isIncluded()) {
                LOGGER.info("[CompanyGen][Walker] {}LEAF excluded by user, skipping '{}'",
                      indent, descriptor.parseName());
                return 0;
            }
            String entityChassis = descriptor.getEntity() == null ? "n/a" : descriptor.getEntity().getChassis();
            String entityModel = descriptor.getEntity() == null ? "n/a" : descriptor.getEntity().getModel();
            LOGGER.info("[CompanyGen][Walker] {}LEAF parseName='{}' echelon={} unitType={} hasEntity={} hasCo={} chassis='{}' model='{}'",
                  indent, descriptor.parseName(), descriptor.getEchelon(),
                  descriptor.getUnitType(), descriptor.getEntity() != null,
                  descriptor.getCo() != null, entityChassis, entityModel);
            LOGGER.info("[CompanyGen][Walker] {}-> onLeaf.handle('{} {}')", indent, entityChassis, entityModel);
            onLeaf.handle(descriptor, parent);
            LOGGER.info("[CompanyGen][Walker] {}<- onLeaf.handle returned for '{} {}'", indent, entityChassis, entityModel);
            return 1;
        }

        // Non-leaf — create a Formation for this echelon and recurse.
        // getName() returns the raw template ("{ordinal} Lance"); parseName() resolves the
        // {ordinal}/{greek}/{phonetic}/{parent} substitutions populated by Ruleset.assignPositions
        // into the display string ("First Lance", "Alpha Company", etc.).
        String name = descriptor.parseName();
        if (name == null || name.isBlank()) {
            name = "Unnamed Formation";
        }
        int subCount = descriptor.getSubForces() == null ? 0 : descriptor.getSubForces().size();
        int attCount = descriptor.getAttached() == null ? 0 : descriptor.getAttached().size();

        // Drop empty formations: if every unit under this node was excluded in the preview, skip the
        // whole subtree so no empty Formation is created. A single re-included unit keeps it.
        if (!hasIncludedLeaf(descriptor)) {
            LOGGER.info("[CompanyGen][Walker] {}NODE name='{}' has no included units; dropping empty formation",
                  indent, name);
            return 0;
        }

        LOGGER.info("[CompanyGen][Walker] {}NODE name='{}' echelon={} subForces={} attached={} -> creating Formation",
              indent, name, descriptor.getEchelon(), subCount, attCount);

        Formation formation = new Formation(name);
        FormationLevel level = mapEchelonToFormationLevel(descriptor.getEchelon(), descriptor.getFaction());
        if (level != null) {
            formation.setFormationLevel(level);
        }
        campaign.addFormation(formation, parent);
        LOGGER.info("[CompanyGen][Walker] {}  Formation registered id={} formationLevel={} parentId={}",
              indent, formation.getId(), level,
              parent == null ? "null" : parent.getId());

        int leaves = 0;
        if (descriptor.getSubForces() != null) {
            for (ForceDescriptor child : descriptor.getSubForces()) {
                leaves += walkInternal(child, campaign, formation, onLeaf, depth + 1);
            }
        }
        if (descriptor.getAttached() != null) {
            for (ForceDescriptor child : descriptor.getAttached()) {
                LOGGER.info("[CompanyGen][Walker] {}  (attached child)", indent);
                leaves += walkInternal(child, campaign, formation, onLeaf, depth + 1);
            }
        }
        return leaves;
    }

    /**
     * Whether {@code descriptor} contains at least one included unit anywhere beneath it. Used to drop
     * formations whose units were all excluded in the preview, while keeping a formation that still has
     * a single re-included unit inside an otherwise-excluded branch.
     *
     * @param descriptor the descriptor to test
     * @return {@code true} if any leaf under {@code descriptor} is included and has an entity
     */
    private static boolean hasIncludedLeaf(ForceDescriptor descriptor) {
        boolean hasChildren = (descriptor.getSubForces() != null && !descriptor.getSubForces().isEmpty())
              || (descriptor.getAttached() != null && !descriptor.getAttached().isEmpty());
        if (!hasChildren) {
            return descriptor.isIncluded() && descriptor.getEntity() != null;
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
     * for that unit type (for example "Battle Armor Company") under {@code parent}, then walks the
     * group's platoons beneath it.
     *
     * @param platoons    the loose platoons to wrap (may be empty)
     * @param campaign    the campaign the formations are added to
     * @param parent      the formation the synthesized companies hang under
     * @param factionCode the descriptor faction, used to map the company echelon
     * @param onLeaf      the leaf handler forwarded to {@link #walkInternal}
     *
     * @return the number of leaves visited
     */
    private static int wrapLoosePlatoonsByUnitType(List<ForceDescriptor> platoons, Campaign campaign,
          Formation parent, String factionCode, LeafHandler onLeaf) {
        if (platoons.isEmpty()) {
            return 0;
        }
        Map<Integer, List<ForceDescriptor>> byUnitType = new LinkedHashMap<>();
        for (ForceDescriptor platoon : platoons) {
            byUnitType.computeIfAbsent(platoon.getUnitType(), key -> new ArrayList<>()).add(platoon);
        }

        int leaves = 0;
        for (Map.Entry<Integer, List<ForceDescriptor>> entry : byUnitType.entrySet()) {
            List<ForceDescriptor> group = entry.getValue();
            String companyName = companyNameForUnitType(entry.getKey());
            Formation company = new Formation(companyName);
            // The company sits one echelon above its platoons.
            Integer platoonEchelon = group.get(0).getEchelon();
            FormationLevel companyLevel = mapEchelonToFormationLevel(
                  platoonEchelon == null ? null : platoonEchelon + 1, factionCode);
            if (companyLevel != null) {
                company.setFormationLevel(companyLevel);
            }
            campaign.addFormation(company, parent);
            LOGGER.info("[CompanyGen][Walker] synthesized '{}' (level {}) for {} loose platoon(s)",
                  companyName, companyLevel, group.size());
            for (ForceDescriptor platoon : group) {
                leaves += walkInternal(platoon, campaign, company, onLeaf, 1);
            }
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
     * @param factionCode the faction code on the descriptor (e.g. "FS", "CW", "WOB"); may be null
     * @return the matching {@link FormationLevel}, or {@code null} if the echelon doesn't map
     */
    private static FormationLevel mapEchelonToFormationLevel(Integer echelon, String factionCode) {
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

    private static FactionFamily resolveFactionFamily(String factionCode) {
        if (factionCode == null || factionCode.isBlank()) {
            return FactionFamily.INNER_SPHERE;
        }
        // Take the first comma-separated token; ratgen sometimes packs an "FS,FedSuns,3030" style code.
        String primary = factionCode.split(",")[0].trim();
        if ("CS".equalsIgnoreCase(primary) || "WOB".equalsIgnoreCase(primary)
              || primary.toUpperCase().startsWith("CS.") || primary.toUpperCase().startsWith("WOB.")) {
            return FactionFamily.COMSTAR;
        }
        FactionRecord faction = RATGenerator.getInstance().getFaction(primary);
        if (faction != null && faction.isClan()) {
            return FactionFamily.CLAN;
        }
        return FactionFamily.INNER_SPHERE;
    }
}
