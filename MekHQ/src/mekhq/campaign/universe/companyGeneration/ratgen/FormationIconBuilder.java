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
import java.util.UUID;

import megamek.common.units.EntityWeightClass;
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

        // TYPE: weight-class-driven 'Mek silhouette.
        appendWeightClassType(icon, formation, campaign);

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
     * Adds the weight-class TYPE piece(s) to the icon. Two-piece left+right pair when both files
     * exist; single center silhouette otherwise.
     */
    private static void appendWeightClassType(LayeredFormationIcon icon, Formation formation, Campaign campaign) {
        int weightClass = determineWeightClass(formation, campaign);
        String weightClassName = EntityWeightClass.getClassName(weightClass);
        String filename = String.format("%s.png", weightClassName);

        icon.getPieces().putIfAbsent(LayeredFormationIconLayer.TYPE, new ArrayList<>());
        try {
            if (MHQStaticDirectoryManager.getFormationIcons().getItem(
                  LayeredFormationIconLayer.TYPE.getLayerPath()
                        + MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                  filename) == null) {
                icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                      new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                            MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                            MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME));
            } else {
                icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                      new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                            MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                            MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME));
                icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                      new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                            MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH, filename));
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Cannot resolve weight-class TYPE for {}; falling back to center silhouette",
                  formation.getName());
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).clear();
            icon.getPieces().get(LayeredFormationIconLayer.TYPE).add(
                  new FormationPieceIcon(LayeredFormationIconLayer.TYPE,
                        MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                        MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME));
        }
    }

    /**
     * Computes the formation's average weight class from the entities of every unit in its subtree.
     * Light / Medium / Heavy / Assault / Super-Heavy.
     *
     * <p>Uses the standard 'Mek tonnage thresholds (Light ≤35, Medium ≤55, Heavy ≤75, Assault ≤100,
     * Super-Heavy >100) regardless of the units' actual chassis types. The icon is a stylistic
     * shorthand, not a strict per-chassis classification.</p>
     */
    private static int determineWeightClass(Formation formation, Campaign campaign) {
        double totalWeight = 0;
        int unitCount = 0;
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null || unit.getEntity() == null) {
                continue;
            }
            totalWeight += unit.getEntity().getWeight();
            unitCount++;
        }
        if (unitCount == 0) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        }
        double averageWeight = totalWeight / unitCount;
        if (averageWeight <= 35) {
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (averageWeight <= 55) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else if (averageWeight <= 75) {
            return EntityWeightClass.WEIGHT_HEAVY;
        } else if (averageWeight <= 100) {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
        return EntityWeightClass.WEIGHT_SUPER_HEAVY;
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
