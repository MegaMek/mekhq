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
import java.util.Map;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.icons.FormationPieceIcon;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Builds layered formation icons for the Force Generator pipeline, covering every {@link FormationLevel}
 * the engine can produce — not just the lance / company pair the legacy {@code AbstractCompanyGenerator}
 * supported.
 *
 * <p>For each non-leaf {@link Formation}, the icon stacks three layers:</p>
 * <ul>
 *   <li><b>TYPE</b> — a weight-class-based 'Mek silhouette computed from the units in the formation's
 *       subtree (Light / Medium / Heavy / Assault / Super-Heavy).</li>
 *   <li><b>FORMATION</b> — a shape file matching the formation's {@link FormationLevel} and faction
 *       family (IS / Clan / ComStar). All echelons from Team / Point / Level I up through
 *       Brigade / Galaxy / Level VI are covered.</li>
 *   <li><b>BACKGROUND</b> — the faction's standard background, when defined.</li>
 * </ul>
 *
 * <p>The campaign root Formation additionally gets a LOGO layer (or a generic-'Mek TYPE fallback)
 * driven by {@link CompanyGenerationOptions#isUseOriginNodeFormationIconLogo()}.</p>
 *
 * <p>This is the spiritual replacement for {@code AbstractCompanyGenerator.createLayeredFormationIcon}
 * scaled up to the full {@link FormationLevel} space.</p>
 */
public final class FormationIconBuilder {

    private static final MMLogger LOGGER = MMLogger.create(FormationIconBuilder.class);

    private FormationIconBuilder() {
        // utility class
    }

    /**
     * Walks the campaign's Formation tree rooted at {@code root} and applies a layered icon to every
     * non-leaf Formation. Honors the icon-related toggles on {@link CompanyGenerationOptions}: if
     * {@code isGenerateFormationIcons()} is off, returns immediately.
     *
     * @param root     the campaign's top-level Formation (typically {@code campaign.getFormations()})
     * @param campaign the campaign whose units feed the weight-class math
     * @param options  the options driving icon-generation toggles
     */
    public static void applyIcons(Formation root, Campaign campaign, CompanyGenerationOptions options) {
        if (root == null || campaign == null || options == null) {
            return;
        }
        if (!options.isGenerateFormationIcons()) {
            LOGGER.info("[CompanyGen] FormationIconBuilder.applyIcons: disabled by options, skipping");
            return;
        }
        if (MHQStaticDirectoryManager.getFormationIcons() == null) {
            LOGGER.warn("[CompanyGen] FormationIconBuilder.applyIcons: formation-icon directory unavailable, skipping");
            return;
        }

        Faction iconFaction = options.isUseSpecifiedFactionToGenerateFormationIcons()
              ? options.getSpecifiedFaction()
              : campaign.getFaction();
        if (iconFaction == null) {
            iconFaction = campaign.getFaction();
        }

        FormationPieceIcon background = buildBackgroundPiece(iconFaction);
        LOGGER.info("[CompanyGen] FormationIconBuilder.applyIcons START rootName='{}' iconFaction={} background={}",
              root.getName(), iconFaction.getShortName(), background == null ? "none" : "set");

        // Root gets a logo-or-default-type icon if the toggle is on.
        if (options.isGenerateOriginNodeFormationIcon()) {
            LayeredFormationIcon rootIcon = buildOriginIcon(iconFaction, options.isUseOriginNodeFormationIconLogo(),
                  background);
            root.setFormationIcon(rootIcon);
            LOGGER.info("[CompanyGen]   root '{}' -> origin icon applied", root.getName());
        }

        // Recurse into the rest of the tree.
        int applied = applyToSubtree(root, campaign, iconFaction, background);
        LOGGER.info("[CompanyGen] FormationIconBuilder.applyIcons DONE; {} non-root formations decorated", applied);
    }

    /**
     * Walks every sub-Formation under {@code parent} and applies a layered icon. The {@code parent}
     * itself is NOT decorated by this method — the caller already handled the root in
     * {@link #applyIcons}. Returns the count of decorated Formations.
     */
    private static int applyToSubtree(Formation parent, Campaign campaign, Faction iconFaction,
          FormationPieceIcon background) {
        int count = 0;
        for (Formation child : parent.getSubFormations()) {
            LayeredFormationIcon icon = buildFormationIcon(child, campaign, iconFaction, background);
            if (icon != null) {
                child.setFormationIcon(icon);
                count++;
                LOGGER.info("[CompanyGen]   formation '{}' (level={}) -> icon applied",
                      child.getName(), child.getFormationLevel());
            }
            count += applyToSubtree(child, campaign, iconFaction, background);
        }
        return count;
    }

    /**
     * Builds the LOGO / TYPE / BACKGROUND icon used for the campaign's root Formation. When
     * {@code useLogo} is true and the faction has a logo, the LOGO layer is used; otherwise the
     * generic 'Mek-center silhouette goes into the TYPE layer as a fallback.
     */
    private static LayeredFormationIcon buildOriginIcon(Faction iconFaction, boolean useLogo,
          FormationPieceIcon background) {
        LayeredFormationIcon icon = new LayeredFormationIcon();

        if (useLogo && iconFaction.getLayeredFormationIconLogoFilename() != null) {
            FormationPieceIcon logoIcon = new FormationPieceIcon(LayeredFormationIconLayer.LOGO,
                  iconFaction.getLayeredFormationIconLogoCategory(),
                  iconFaction.getLayeredFormationIconLogoFilename());
            if (logoIcon.getBaseImage() != null) {
                icon.getPieces().putIfAbsent(LayeredFormationIconLayer.LOGO, new ArrayList<>());
                icon.getPieces().get(LayeredFormationIconLayer.LOGO).add(logoIcon);
            }
        } else {
            icon.getPieces().putIfAbsent(LayeredFormationIconLayer.TYPE, new ArrayList<>());
            icon.getPieces().get(LayeredFormationIconLayer.TYPE)
                  .add(new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                        MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                        MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME));
        }

        if (background != null) {
            icon.getPieces().putIfAbsent(LayeredFormationIconLayer.BACKGROUND, new ArrayList<>());
            icon.getPieces().get(LayeredFormationIconLayer.BACKGROUND).add(background.clone());
        }

        return icon;
    }

    /**
     * Builds the TYPE / FORMATION / BACKGROUND icon for a single Formation node. Returns {@code null}
     * if the formation's level is unknown (in which case the caller leaves the formation icon-less).
     */
    private static LayeredFormationIcon buildFormationIcon(Formation formation, Campaign campaign,
          Faction iconFaction, FormationPieceIcon background) {
        String formationFilename = formationFilenameFor(formation.getFormationLevel(), iconFaction);
        if (formationFilename == null) {
            // Unknown level (e.g. INVALID, NONE) — skip; the formation gets no auto-icon.
            return null;
        }

        LayeredFormationIcon icon = new LayeredFormationIcon();

        // TYPE: dominant-unit-type silhouette (Mek by default, Aero / BA / ProtoMek / Infantry /
        // vehicle / vessel when those dominate the formation's subtree).
        appendTypePieces(icon, formation, campaign);

        // FORMATION: shape file per level + faction family.
        String formationFolder = formationFolderFor(iconFaction);
        icon.getPieces().putIfAbsent(LayeredFormationIconLayer.FORMATION, new ArrayList<>());
        icon.getPieces().get(LayeredFormationIconLayer.FORMATION)
              .add(new FormationPieceIcon(LayeredFormationIconLayer.FORMATION, formationFolder,
                    formationFilename));

        // BACKGROUND: faction-supplied, cloned so each formation gets its own piece instance.
        if (background != null) {
            icon.getPieces().putIfAbsent(LayeredFormationIconLayer.BACKGROUND, new ArrayList<>());
            icon.getPieces().get(LayeredFormationIconLayer.BACKGROUND).add(background.clone());
        }

        return icon;
    }

    /**
     * Adds the TYPE piece(s) to the icon based on which unit type dominates the formation's
     * subtree. All file references stay inside {@code Pieces/Types/StratOps/} so the icon style
     * is consistent across the campaign. Mek-dominant formations get the existing two-piece
     * BattleMek (Left) + weight-class pair; Aerospace gets the matching Aerospace (Left) pair;
     * ProtoMek / BattleArmor / Infantry / vessels get their single-piece silhouette. Ground
     * vehicles (Tank, Naval) have no StratOps chassis silhouette — they fall back to the weight-
     * class file alone, which is exactly the "H"/"A"/etc. letter seen on existing vehicle
     * formations and visually distinguishes them from Mek formations. VTOL borrows Airship.png;
     * Gun Emplacement uses Headquarters.png. Mixed / empty formations fall back to the generic
     * BattleMek (Center) silhouette.
     */
    private static void appendTypePieces(LayeredFormationIcon icon, Formation formation, Campaign campaign) {
        icon.getPieces().putIfAbsent(LayeredFormationIconLayer.TYPE, new ArrayList<>());
        int dominantType = determineDominantUnitType(formation, campaign);
        int weightClass = determineWeightClass(formation, campaign);
        String weightFilename = EntityWeightClass.getClassName(weightClass) + ".png";
        String stratOps = MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH;
        LOGGER.info("[CompanyGen][Icons] formation '{}' dominantType={} ({}) weightClass={} ({})",
              formation.getName(), dominantType, UnitType.getTypeName(dominantType),
              weightClass, EntityWeightClass.getClassName(weightClass));
        try {
            switch (dominantType) {
                case UnitType.MEK -> appendPairOrCenter(icon, stratOps,
                      MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME,
                      stratOps, weightFilename);
                case UnitType.AEROSPACE_FIGHTER, UnitType.AERO, UnitType.CONV_FIGHTER ->
                      appendPairOrCenter(icon, stratOps, "Aerospace (Left).png", stratOps, weightFilename);
                case UnitType.PROTOMEK -> appendSingle(icon, stratOps, "ProtoMek.png");
                case UnitType.BATTLE_ARMOR -> appendSingle(icon, stratOps, "Battle Armor.png");
                case UnitType.INFANTRY -> appendSingle(icon, stratOps, "Infantry (Xenoplanetary).png");
                case UnitType.DROPSHIP, UnitType.SMALL_CRAFT -> appendSingle(icon, stratOps, "DropShip.png");
                case UnitType.JUMPSHIP -> appendSingle(icon, stratOps, "JumpShip.png");
                case UnitType.WARSHIP -> appendSingle(icon, stratOps, "WarShip.png");
                case UnitType.SPACE_STATION -> appendSingle(icon, stratOps, "Space Station.png");
                case UnitType.VTOL -> appendSingle(icon, stratOps, "Airship.png");
                case UnitType.GUN_EMPLACEMENT -> appendSingle(icon, stratOps, "Headquarters.png");
                // Tank / Naval have no generic chassis silhouette under StratOps. Fall back to
                // weight-class file alone — matches the "H"/"A"/etc. letter the existing vehicle
                // formations already render, distinct from Mek formations' chassis-plus-letter
                // pair. Adding StratOps-style tank/boat silhouettes to mm-data would let us
                // upgrade these to a paired icon later without changing this code.
                case UnitType.TANK, UnitType.NAVAL -> appendSingle(icon, stratOps, weightFilename);
                default -> appendSingle(icon, stratOps,
                      MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "[CompanyGen][Icons] cannot resolve TYPE for '{}' (dominantType={}); falling back to BattleMek (Center)",
                  formation.getName(), dominantType);
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).clear();
            // Add the BattleMek (Center) fallback directly without going through appendSingle's
            // getItem existence check — appendSingle itself throws, and we're already inside the
            // catch block. Center is shipped in mm-data; if it ever isn't, the icon just renders
            // without a TYPE layer.
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                  new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                        MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                        MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME));
        }
    }

    /**
     * Adds a single TYPE piece; verifies it exists on disk before adding so a missing icon falls
     * back to the BattleMek (Center) generic silhouette.
     */
    private static void appendSingle(LayeredFormationIcon icon, String folder, String filename) throws Exception {
        if (MHQStaticDirectoryManager.getFormationIcons().getItem(
              LayeredFormationIconLayer.TYPE.getLayerPath() + folder, filename) == null) {
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                  new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                        MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                        MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME));
            return;
        }
        icon.getPieces().get(LayeredFormationIconLayer.TYPE)
              .add(new FormationPieceIcon(LayeredFormationIconLayer.TYPE, folder, filename));
    }

    /**
     * Adds a two-piece silhouette + weight-class pair; verifies both files exist before adding.
     * Falls back to a single piece (just the silhouette) if the weight-class file is missing,
     * and to BattleMek (Center) if the silhouette itself is missing.
     */
    private static void appendPairOrCenter(LayeredFormationIcon icon, String silhouetteFolder,
          String silhouetteFilename, String weightFolder, String weightFilename) throws Exception {
        if (MHQStaticDirectoryManager.getFormationIcons().getItem(
              LayeredFormationIconLayer.TYPE.getLayerPath() + silhouetteFolder,
              silhouetteFilename) == null) {
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                  new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                        MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                        MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME));
            return;
        }
        icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
              new FormationPieceIcon(LayeredFormationIconLayer.TYPE, silhouetteFolder, silhouetteFilename));
        if (MHQStaticDirectoryManager.getFormationIcons().getItem(
              LayeredFormationIconLayer.TYPE.getLayerPath() + weightFolder, weightFilename) != null) {
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                  new FormationPieceIcon(LayeredFormationIconLayer.TYPE, weightFolder, weightFilename));
        }
    }

    /**
     * Returns the most common {@link UnitType} among the units in the formation's subtree, or
     * {@link UnitType#MEK} when the formation has no units or all units' entities are null. Used
     * by {@link #appendTypePieces} to pick the chassis silhouette.
     */
    private static int determineDominantUnitType(Formation formation, Campaign campaign) {
        Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }
            Entity entity = unit.getEntity();
            if (entity == null) {
                continue;
            }
            counts.merge(entity.getUnitType(), 1, Integer::sum);
        }
        int dominantType = UnitType.MEK;
        int dominantCount = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > dominantCount) {
                dominantType = entry.getKey();
                dominantCount = entry.getValue();
            }
        }
        return dominantType;
    }

    /**
     * Computes the formation's average weight class across every unit in its subtree.
     *
     * <p>Uses {@link Entity#getWeightClass()} (which dispatches on per-unit-type breakpoints —
     * Mek {35/55/75/100/135}, Tank {39/59/79/100/300}, Aerospace {45/70/100}, etc.) and averages
     * the class codes rather than raw tonnage. This avoids a long-standing bug where a 79-ton
     * tank — Heavy by tank rules — was rendered Assault because the old code applied Mek
     * thresholds to every unit type.</p>
     *
     * <p>Logs per-unit weight + class plus the formation total at {@code [CompanyGen][Icons][Weight]}
     * so the rendered icon can be traced back to specific units.</p>
     */
    private static int determineWeightClass(Formation formation, Campaign campaign) {
        int totalClass = 0;
        int unitCount = 0;
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null || unit.getEntity() == null) {
                continue;
            }
            Entity entity = unit.getEntity();
            int entityClass = entity.getWeightClass();
            totalClass += entityClass;
            unitCount++;
            LOGGER.info("[CompanyGen][Icons][Weight] formation '{}' unit '{} {}' type={} weight={}t class={} ({})",
                  formation.getName(), entity.getChassis(), entity.getModel(),
                  UnitType.getTypeName(entity.getUnitType()),
                  entity.getWeight(), entityClass, EntityWeightClass.getClassName(entityClass));
        }
        if (unitCount == 0) {
            LOGGER.info("[CompanyGen][Icons][Weight] formation '{}' has no units; defaulting to MEDIUM",
                  formation.getName());
            return EntityWeightClass.WEIGHT_MEDIUM;
        }
        int averaged = (int) Math.round((double) totalClass / unitCount);
        LOGGER.info("[CompanyGen][Icons][Weight] formation '{}' averaged class={} ({}) over {} units",
              formation.getName(), averaged, EntityWeightClass.getClassName(averaged), unitCount);
        return averaged;
    }

    /**
     * Returns the icon-folder path for the formation layer based on the faction family.
     */
    private static String formationFolderFor(Faction faction) {
        if (faction.isClan()) {
            return MHQConstants.LAYERED_FORCE_ICON_FORMATION_CLAN_PATH;
        }
        if (faction.isComStarOrWoB()) {
            return MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMSTAR_PATH;
        }
        return MHQConstants.LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH;
    }

    /**
     * Maps a {@link FormationLevel} to the formation-shape filename appropriate for the faction
     * family. Every level from the smallest (Team / Point / Level I) up through the largest the
     * ratgen engine can produce is covered:
     *
     * <ul>
     *   <li>Inner Sphere / Periphery / Mercenary: TEAM / LANCE / COMPANY / BATTALION / REGIMENT /
     *       BRIGADE map directly. DIVISION / CORPS / ARMY / ARMY_GROUP fall back to the BRIGADE icon
     *       because mm-data only ships IS shapes up to Brigade.</li>
     *   <li>Clan: TEAM (Point) / STAR_OR_NOVA / BINARY_OR_TRINARY / CLUSTER / GALAXY map directly.
     *       TOUMAN falls back to the GALAXY icon (no Touman shape ships in mm-data).</li>
     *   <li>ComStar / WoB: TEAM (Level I) / LEVEL_II_OR_CHOIR through LEVEL_VI all map directly;
     *       all five levels ship in mm-data.</li>
     * </ul>
     *
     * <p>Returns {@code null} only for non-canonical levels (NONE, INVALID, REMOVE_OVERRIDE) so the
     * caller can skip those nodes cleanly.</p>
     */
    private static String formationFilenameFor(FormationLevel level, Faction faction) {
        if (level == null) {
            return null;
        }
        if (faction.isClan()) {
            return switch (level) {
                case TEAM -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_POINT_FILENAME;
                case STAR_OR_NOVA -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_STAR_FILENAME;
                case BINARY_OR_TRINARY -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_TRINARY_FILENAME;
                case CLUSTER -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_CLUSTER_FILENAME;
                // GALAXY is the largest Clan shape shipped in mm-data; TOUMAN falls back to it.
                case GALAXY, TOUMAN -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_GALAXY_FILENAME;
                default -> null;
            };
        }
        if (faction.isComStarOrWoB()) {
            return switch (level) {
                case TEAM -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_I_FILENAME;
                case LEVEL_II_OR_CHOIR -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_II_FILENAME;
                case LEVEL_III -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_III_FILENAME;
                case LEVEL_IV -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_IV_FILENAME;
                case LEVEL_V -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_V_FILENAME;
                case LEVEL_VI -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_VI_FILENAME;
                default -> null;
            };
        }
        // Inner Sphere / Periphery / Mercenary default. BRIGADE is the largest IS shape shipped in
        // mm-data; the SLDF (SL.xml) can produce DIVISION / CORPS / ARMY, and FormationLevel also
        // declares ARMY_GROUP, so all three fall back to BRIGADE rather than going icon-less.
        return switch (level) {
            case TEAM -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_TEAM_FILENAME;
            case LANCE -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_LANCE_FILENAME;
            case COMPANY -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMPANY_FILENAME;
            case BATTALION -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_BATTALION_FILENAME;
            case REGIMENT -> MHQConstants.LAYERED_FORCE_ICON_FORMATION_REGIMENT_FILENAME;
            case BRIGADE, DIVISION, CORPS, ARMY, ARMY_GROUP ->
                  MHQConstants.LAYERED_FORCE_ICON_FORMATION_BRIGADE_FILENAME;
            default -> null;
        };
    }

    /**
     * Builds the BACKGROUND piece from the faction. Returns {@code null} when the faction has no
     * background, or when the background's base image is missing on disk.
     */
    private static FormationPieceIcon buildBackgroundPiece(Faction iconFaction) {
        if (iconFaction.getLayeredFormationIconBackgroundFilename() == null) {
            return null;
        }
        FormationPieceIcon background = new FormationPieceIcon(LayeredFormationIconLayer.BACKGROUND,
              iconFaction.getLayeredFormationIconBackgroundCategory(),
              iconFaction.getLayeredFormationIconBackgroundFilename());
        if (background.getBaseImage() == null) {
            return null;
        }
        return background;
    }
}
