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

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.RATGenerator;
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
            LOGGER.warn("[CompanyGen] ForceDescriptorWalker.walk called with null root; returning 0");
            return 0;
        }
        LOGGER.info("[CompanyGen] ForceDescriptorWalker.walk START rootEchelon={} rootFaction={} parentFormation={}",
              root.getEchelon(), root.getFaction(),
              parentInCampaign == null ? "null" : parentInCampaign.getName());

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
            LOGGER.info("[CompanyGen] Merged root descriptor into existing campaign Formation '{}' (id={} formationLevel={}); recursing into {} subForces + {} attached",
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
                for (ForceDescriptor child : root.getAttached()) {
                    LOGGER.info("[CompanyGen]   (attached child of root)");
                    leaves += walkInternal(child, campaign, parentInCampaign, onLeaf, 0);
                }
            }
        } else {
            // Edge cases: the root descriptor is itself a leaf (single-unit generation), or no
            // existing campaign Formation was supplied. Fall through to the normal walkInternal
            // path which creates a Formation for the root and proceeds.
            leaves = walkInternal(root, campaign, parentInCampaign, onLeaf, 0);
        }

        LOGGER.info("[CompanyGen] ForceDescriptorWalker.walk DONE; {} leaves visited", leaves);
        return leaves;
    }

    private static int walkInternal(ForceDescriptor descriptor, Campaign campaign, Formation parent,
          LeafHandler onLeaf, int depth) {
        String indent = "  ".repeat(depth);
        boolean hasChildren = (descriptor.getSubForces() != null && !descriptor.getSubForces().isEmpty())
              || (descriptor.getAttached() != null && !descriptor.getAttached().isEmpty());

        if (!hasChildren) {
            // Leaf — let the caller turn it into a Unit + crew.
            LOGGER.info("[CompanyGen] {}LEAF parseName='{}' echelon={} unitType={} hasEntity={} hasCo={}",
                  indent, descriptor.parseName(), descriptor.getEchelon(),
                  descriptor.getUnitType(), descriptor.getEntity() != null,
                  descriptor.getCo() != null);
            onLeaf.handle(descriptor, parent);
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
        LOGGER.info("[CompanyGen] {}NODE name='{}' echelon={} subForces={} attached={} -> creating Formation",
              indent, name, descriptor.getEchelon(), subCount, attCount);

        Formation formation = new Formation(name);
        FormationLevel level = mapEchelonToFormationLevel(descriptor.getEchelon(), descriptor.getFaction());
        if (level != null) {
            formation.setFormationLevel(level);
        }
        campaign.addFormation(formation, parent);
        LOGGER.info("[CompanyGen] {}  Formation registered id={} formationLevel={} parentId={}",
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
                LOGGER.info("[CompanyGen] {}  (attached child)", indent);
                leaves += walkInternal(child, campaign, formation, onLeaf, depth + 1);
            }
        }
        return leaves;
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
