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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.icons.FormationPieceIcon;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.SupportCapability;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;
import mekhq.campaign.universe.commandGeneration.SupportVehicleSelector;

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
 * driven by {@link CommandGenerationOptions#isUseOriginNodeFormationIconLogo()}.</p>
 *
 * <p>This is the spiritual replacement for {@code AbstractCompanyGenerator.createLayeredFormationIcon}
 * scaled up to the full {@link FormationLevel} space.</p>
 */
public final class FormationIconBuilder {

    private static final MMLogger LOGGER = MMLogger.create(FormationIconBuilder.class);

    // The rulebook's common symbology (Core Rulebook p.28): a support formation is drawn for the job it does, not
    // for the machines it rides in. Carriers are conventional infantry, so without these every support formation in
    // the TOE would show the same infantry symbol.
    /** StratOps type icon for medical staff and MASH-equipped vehicles. */
    private static final String MEDICAL_TYPE_FILENAME = "Medical.png";
    /** StratOps type icon for technicians and recovery vehicles. */
    private static final String MAINTENANCE_TYPE_FILENAME = "Maintenance.png";
    /** StratOps type icon for administrative staff. */
    private static final String HEADQUARTERS_TYPE_FILENAME = "Headquarters (Staff).png";
    /** StratOps type icon for a logistics convoy. */
    private static final String SUPPLY_TYPE_FILENAME = "Supply.png";

    private static final String AEROSPACE_TYPE_FILENAME = "Aerospace (Left).png";
    private static final String PROTOMEK_TYPE_FILENAME = "ProtoMek.png";
    private static final String BATTLE_ARMOR_TYPE_FILENAME = "Battle Armor.png";
    private static final String INFANTRY_TYPE_FILENAME = "Infantry (Xenoplanetary).png";
    private static final String DROPSHIP_TYPE_FILENAME = "DropShip.png";
    private static final String JUMPSHIP_TYPE_FILENAME = "JumpShip.png";
    private static final String WARSHIP_TYPE_FILENAME = "WarShip.png";
    private static final String SPACE_STATION_TYPE_FILENAME = "Space Station.png";
    private static final String VTOL_TYPE_FILENAME = "Airship.png";
    private static final String GUN_EMPLACEMENT_TYPE_FILENAME = "Headquarters.png";

    /**
     * Every StratOps type file this builder draws. A TYPE layer made only of these was drawn here, or by a player who
     * picked exactly what the builder would have, so redrawing it to match the formation's units takes nothing from
     * anyone.
     */
    private static final Set<String> BUILDER_TYPE_FILENAMES = builderTypeFilenames();

    private FormationIconBuilder() {
        // utility class
    }

    /**
     * Walks the campaign's Formation tree rooted at {@code root} and applies a layered icon to every
     * non-leaf Formation. Honors the icon-related toggles on {@link CommandGenerationOptions}: if
     * {@code isGenerateFormationIcons()} is off, returns immediately.
     *
     * @param root           the force's top-level Formation (typically {@code force.getFormations()})
     * @param forceFaction   the faction the force is organised as, used unless the options name another
     * @param unitLookup     resolves the unit ids in the formations, whose units feed the weight-class math
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     * @param gameYear       the current year, which picks the faction logo for the origin node
     * @param options        the options driving icon-generation toggles
     */
    public static void applyIcons(Formation root, Faction forceFaction, Function<UUID, Unit> unitLookup,
          @Nullable Formation supportCommand, int gameYear, CommandGenerationOptions options) {
        if (root == null || forceFaction == null || unitLookup == null || options == null) {
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
              : forceFaction;
        if (iconFaction == null) {
            iconFaction = forceFaction;
        }

        FormationPieceIcon background = buildBackgroundPiece(iconFaction);
        LOGGER.info("[CompanyGen] FormationIconBuilder.applyIcons START rootName='{}' iconFaction={} background={}",
              root.getName(), iconFaction.getShortName(), background == null ? "none" : "set");

        // Root gets a logo-or-default-type icon if the toggle is on.
        if (options.isGenerateOriginNodeFormationIcon()) {
            LayeredFormationIcon rootIcon = buildOriginIcon(iconFaction, gameYear,
                  options.isUseOriginNodeFormationIconLogo(), background);
            root.setFormationIcon(rootIcon);
            LOGGER.info("[CompanyGen]   root '{}' -> origin icon applied", root.getName());
        }

        // Recurse into the rest of the tree.
        int applied = applyToSubtree(root, unitLookup, supportCommand, iconFaction, background);
        LOGGER.info("[CompanyGen] FormationIconBuilder.applyIcons DONE; {} non-root formations decorated", applied);
    }

    /**
     * Decorates one existing formation and everything under it, using the force's own faction.
     *
     * <p>The generation pass above needs {@link CommandGenerationOptions} because it also decides the origin node's
     * icon. Support teams organized after generation - a campaign that switched the option on - have no generation
     * options to consult, but their formations should look like the generated ones, so this is the same builder
     * without the origin-node question.</p>
     *
     * @param root           the formation to decorate, along with its subtree
     * @param iconFaction    the faction the force is organised as, which sets the shape set and background
     * @param unitLookup     resolves the unit ids in the formations
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return the number of formations decorated
     */
    public static int applyIconsToSubtree(@Nullable Formation root, @Nullable Faction iconFaction,
          @Nullable Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand) {
        return applyIconsToFormations(root == null ? List.of() : List.of(root), iconFaction, unitLookup,
              supportCommand);
    }

    /**
     * Decorates the named formations and everything under them, using the force's own faction.
     *
     * <p>Used by the support-team conversion, which knows exactly which formations it created and must not touch the
     * player's own - their icons are their business.</p>
     *
     * @param formations     the formations to decorate, each along with its subtree
     * @param iconFaction    the faction the force is organised as, which sets the shape set and background
     * @param unitLookup     resolves the unit ids in the formations
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return the number of formations decorated
     */
    public static int applyIconsToFormations(List<Formation> formations, @Nullable Faction iconFaction,
          @Nullable Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand) {
        if ((iconFaction == null) || (unitLookup == null) || formations.isEmpty()) {
            return 0;
        }
        if (MHQStaticDirectoryManager.getFormationIcons() == null) {
            LOGGER.warn("[SupportTOE] applyIconsToSubtree: formation-icon directory unavailable, skipping");
            return 0;
        }

        FormationPieceIcon background = buildBackgroundPiece(iconFaction);

        int applied = 0;
        for (Formation formation : formations) {
            if (formation == null) {
                continue;
            }
            LayeredFormationIcon icon = buildFormationIcon(formation, unitLookup, supportCommand, iconFaction,
                  background);
            if (icon != null) {
                formation.setFormationIcon(icon);
                applied++;
            }
            applied += applyToSubtree(formation, unitLookup, supportCommand, iconFaction, background);
        }

        LOGGER.info("[SupportTOE] applyIconsToFormations DONE; {} formation(s) decorated", applied);
        return applied;
    }

    /**
     * Brings the echelon symbol on a formation's icon into line with its level, and leaves the rest of the icon alone.
     *
     * <p>Support formations are re-sized as their teams and vehicles change, long after their icons were drawn. A
     * Medical section drawn while it held one MASH truck kept the Team symbol after ten more trucks made it a company.
     * Only an icon whose formation layer is a single echelon shape from this builder's folder is touched, so an icon
     * the player designed is never overwritten.</p>
     *
     * @param formation   the formation whose icon may be stale
     * @param iconFaction the faction the force is organised as, whose family decides the shape set
     *
     * @return {@code true} if the symbol was changed
     */
    public static boolean refreshEchelonPiece(@Nullable Formation formation, @Nullable Faction iconFaction) {
        if ((formation == null) || (iconFaction == null)
                  || !(formation.getFormationIcon() instanceof LayeredFormationIcon icon)
                  || !hasBuilderEchelonPiece(icon, iconFaction)) {
            return false;
        }
        List<FormationPieceIcon> pieces = icon.getPieces().get(LayeredFormationIconLayer.FORMATION);
        FormationPieceIcon piece = pieces.get(0);
        String wanted = formationFilenameFor(formation.getFormationLevel(), iconFaction);
        if ((wanted == null) || wanted.equals(piece.getFilename())) {
            return false;
        }
        pieces.set(0, new FormationPieceIcon(LayeredFormationIconLayer.FORMATION, piece.getCategory(), wanted));
        LOGGER.info("[SupportTOE] formation '{}' re-sized to {}: echelon symbol '{}' -> '{}'", formation.getName(),
              formation.getFormationLevel(), piece.getFilename(), wanted);
        return true;
    }

    /**
     * Draws the icon of {@code root} and every formation under it that still carries the blank default icon, and
     * leaves every other icon alone.
     *
     * <p>Support formations granted mid-campaign - a convoy when StratCon is switched on, canteens under fatigue, the
     * security detail - were filed with no icon at all, because only command generation and the Support Teams
     * conversion drew them. A formation already decorated, by this builder or by the player, is never redrawn.</p>
     *
     * @param root           the formation to decorate, along with its subtree
     * @param iconFaction    the faction the force is organised as, which sets the shape set and background
     * @param unitLookup     resolves the unit ids in the formations
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return the number of formations decorated
     */
    public static int decorateUndecorated(@Nullable Formation root, @Nullable Faction iconFaction,
          @Nullable Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand) {
        if ((root == null) || (iconFaction == null) || (unitLookup == null)
                  || (MHQStaticDirectoryManager.getFormationIcons() == null)) {
            return 0;
        }
        int decorated = decorateUndecoratedSubtree(root, unitLookup, supportCommand, iconFaction,
              buildBackgroundPiece(iconFaction));
        if (decorated > 0) {
            LOGGER.info("[SupportTOE] '{}': drew {} missing formation icon(s)", root.getName(), decorated);
        }
        return decorated;
    }

    private static int decorateUndecoratedSubtree(Formation formation, Function<UUID, Unit> unitLookup,
          @Nullable Formation supportCommand, Faction iconFaction, @Nullable FormationPieceIcon background) {
        int decorated = 0;
        if (isUndecorated(formation)
                  && decorate(formation, unitLookup, supportCommand, iconFaction, background)) {
            decorated++;
        }
        for (Formation child : formation.getSubFormations()) {
            decorated += decorateUndecoratedSubtree(child, unitLookup, supportCommand, iconFaction, background);
        }
        return decorated;
    }

    /**
     * Brings every icon this builder drew under {@code root} into line with its formation, and draws the ones still
     * blank.
     *
     * <p>This is the on-demand version of what command generation does, for a TOE that has grown since: new
     * formations get their icon, a lance that became a company gets the company symbol, and a lance whose 'Meks were
     * traded for tanks loses the 'Mek silhouette. Icons the player designed are left alone.</p>
     *
     * @param root           the formation to refresh, along with its subtree
     * @param iconFaction    the faction the force is organised as, which sets the shape set and background
     * @param unitLookup     resolves the unit ids in the formations
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return the number of formations whose icon was drawn or changed
     */
    public static int refreshIcons(@Nullable Formation root, @Nullable Faction iconFaction,
          @Nullable Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand) {
        if ((root == null) || (iconFaction == null) || (unitLookup == null)
                  || (MHQStaticDirectoryManager.getFormationIcons() == null)) {
            return 0;
        }
        int changed = refreshSubtree(root, unitLookup, supportCommand, iconFaction, buildBackgroundPiece(iconFaction));
        if (changed > 0) {
            LOGGER.info("[FormationIcons] '{}': drew or updated {} formation icon(s)", root.getName(), changed);
        }
        return changed;
    }

    private static int refreshSubtree(Formation formation, Function<UUID, Unit> unitLookup,
          @Nullable Formation supportCommand, Faction iconFaction, @Nullable FormationPieceIcon background) {
        int changed = 0;
        if (isUndecorated(formation)) {
            if (decorate(formation, unitLookup, supportCommand, iconFaction, background)) {
                changed++;
            }
        } else {
            // Both run: the echelon first, as the type refresh asks whether the echelon is still the builder's.
            boolean echelonChanged = refreshEchelonPiece(formation, iconFaction);
            boolean typeChanged = refreshTypePiece(formation, iconFaction, unitLookup, supportCommand);
            if (echelonChanged || typeChanged) {
                changed++;
            }
        }
        for (Formation child : formation.getSubFormations()) {
            changed += refreshSubtree(child, unitLookup, supportCommand, iconFaction, background);
        }
        return changed;
    }

    /**
     * Redraws the unit-type silhouette on a formation's icon to match the units it holds now, and leaves the rest of
     * the icon alone.
     *
     * <p>A Mek lance whose 'Meks were sold for tanks, or a medium company that bought heavies, kept the silhouette it
     * was drawn with. Only an icon this builder drew is touched: its formation layer must be the builder's echelon
     * shape and every type piece one the builder draws, so an icon the player designed is never overwritten.</p>
     *
     * @param formation      the formation whose icon may be stale
     * @param iconFaction    the faction the force is organised as, whose family decides the shape set
     * @param unitLookup     resolves the unit ids in the formation
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return {@code true} if the silhouette was changed
     */
    public static boolean refreshTypePiece(@Nullable Formation formation, @Nullable Faction iconFaction,
          @Nullable Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand) {
        // Without the images every type resolves to the generic fallback, which would overwrite a good silhouette
        if ((formation == null) || (iconFaction == null) || (unitLookup == null)
                  || (MHQStaticDirectoryManager.getFormationIcons() == null)
                  || !(formation.getFormationIcon() instanceof LayeredFormationIcon icon)
                  || !hasBuilderEchelonPiece(icon, iconFaction)) {
            return false;
        }
        List<FormationPieceIcon> pieces = icon.getPieces().get(LayeredFormationIconLayer.TYPE);
        if ((pieces == null) || pieces.isEmpty()
                  || !pieces.stream().allMatch(FormationIconBuilder::isBuilderTypePiece)) {
            return false;
        }
        LayeredFormationIcon redrawn = new LayeredFormationIcon();
        appendTypePieces(redrawn, formation, unitLookup, supportCommand);
        List<FormationPieceIcon> wanted = redrawn.getPieces().get(LayeredFormationIconLayer.TYPE);
        if (wanted.equals(pieces)) {
            return false;
        }
        LOGGER.info("[FormationIcons] formation '{}': type pieces {} -> {}", formation.getName(),
              pieces.stream().map(FormationPieceIcon::getFilename).toList(),
              wanted.stream().map(FormationPieceIcon::getFilename).toList());
        pieces.clear();
        pieces.addAll(wanted);
        return true;
    }

    /**
     * Gives a blank formation its icon, keeping the operational status indicator if the new day has already put one
     * on it, so drawing an icon mid-day does not wipe the status until tomorrow.
     */
    private static boolean decorate(Formation formation, Function<UUID, Unit> unitLookup,
          @Nullable Formation supportCommand, Faction iconFaction, @Nullable FormationPieceIcon background) {
        LayeredFormationIcon icon = buildFormationIcon(formation, unitLookup, supportCommand, iconFaction,
              background);
        if (icon == null) {
            return false;
        }
        if (formation.getFormationIcon() instanceof LayeredFormationIcon blank) {
            List<FormationPieceIcon> status = blank.getPieces().get(LayeredFormationIconLayer.SPECIAL_MODIFIER);
            if ((status != null) && !status.isEmpty()) {
                icon.getPieces().put(LayeredFormationIconLayer.SPECIAL_MODIFIER, new ArrayList<>(status));
            }
        }
        formation.setFormationIcon(icon);
        return true;
    }

    /** Whether the formation still has the blank layered icon every new formation starts with. */
    private static boolean isUndecorated(Formation formation) {
        if (!(formation.getFormationIcon() instanceof LayeredFormationIcon icon)) {
            // A single image the player picked.
            return false;
        }
        // Blank means the frame and nothing else; any other piece was put there by someone. The operational status
        // indicator is the exception: the new day puts it on every layered icon, blank ones included, so counting it
        // would make every blank formation look decorated the day after it was created.
        for (Map.Entry<LayeredFormationIconLayer, List<FormationPieceIcon>> layer : icon.getPieces().entrySet()) {
            if ((layer.getKey() != LayeredFormationIconLayer.FRAME)
                      && (layer.getKey() != LayeredFormationIconLayer.SPECIAL_MODIFIER)
                      && !layer.getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Whether the icon's formation layer is a single echelon shape from this builder's folder for the faction. */
    private static boolean hasBuilderEchelonPiece(LayeredFormationIcon icon, Faction iconFaction) {
        List<FormationPieceIcon> pieces = icon.getPieces().get(LayeredFormationIconLayer.FORMATION);
        if ((pieces == null) || (pieces.size() != 1)) {
            return false;
        }
        FormationPieceIcon piece = pieces.get(0);
        return formationFolderFor(iconFaction).equals(piece.getCategory())
                     && isEchelonShape(piece.getFilename(), iconFaction);
    }

    /** Whether the piece is one of the StratOps type files this builder draws. */
    private static boolean isBuilderTypePiece(FormationPieceIcon piece) {
        return MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH.equals(piece.getCategory())
                     && BUILDER_TYPE_FILENAMES.contains(piece.getFilename());
    }

    private static Set<String> builderTypeFilenames() {
        Set<String> filenames = new HashSet<>(List.of(MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME,
              MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME, AEROSPACE_TYPE_FILENAME,
              PROTOMEK_TYPE_FILENAME, BATTLE_ARMOR_TYPE_FILENAME, INFANTRY_TYPE_FILENAME, DROPSHIP_TYPE_FILENAME,
              JUMPSHIP_TYPE_FILENAME, WARSHIP_TYPE_FILENAME, SPACE_STATION_TYPE_FILENAME, VTOL_TYPE_FILENAME,
              GUN_EMPLACEMENT_TYPE_FILENAME, MEDICAL_TYPE_FILENAME, MAINTENANCE_TYPE_FILENAME,
              HEADQUARTERS_TYPE_FILENAME, SUPPLY_TYPE_FILENAME));
        // The weight-class letters, drawn beside a silhouette or alone for vehicles.
        for (int weightClass = 0; weightClass < EntityWeightClass.SIZE; weightClass++) {
            filenames.add(EntityWeightClass.getClassName(weightClass) + ".png");
        }
        return Set.copyOf(filenames);
    }

    /** Whether the file is one of the echelon shapes this builder draws for the faction family. */
    private static boolean isEchelonShape(@Nullable String filename, Faction faction) {
        if (filename == null) {
            return false;
        }
        for (FormationLevel level : FormationLevel.values()) {
            if (filename.equals(formationFilenameFor(level, faction))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks every sub-Formation under {@code parent} and applies a layered icon. The {@code parent}
     * itself is NOT decorated by this method — the caller already handled the root in
     * {@link #applyIcons}. Returns the count of decorated Formations.
     */
    private static int applyToSubtree(Formation parent, Function<UUID, Unit> unitLookup,
          @Nullable Formation supportCommand, Faction iconFaction, FormationPieceIcon background) {
        int count = 0;
        for (Formation child : parent.getSubFormations()) {
            LayeredFormationIcon icon = buildFormationIcon(child, unitLookup, supportCommand, iconFaction,
                  background);
            if (icon != null) {
                child.setFormationIcon(icon);
                count++;
                LOGGER.info("[CompanyGen]   formation '{}' (level={}) -> icon applied",
                      child.getName(), child.getFormationLevel());
            }
            count += applyToSubtree(child, unitLookup, supportCommand, iconFaction, background);
        }
        return count;
    }

    /**
     * Builds the LOGO / TYPE / BACKGROUND icon used for the campaign's root Formation. When
     * {@code useLogo} is {@code true} and the faction has a logo for {@code gameYear} (see
     * {@link #getOriginLogo(Faction, int)}), it is used as the LOGO layer; otherwise the generic 'Mek-center
     * silhouette goes into the TYPE layer as a fallback.
     */
    private static LayeredFormationIcon buildOriginIcon(Faction iconFaction, int gameYear, boolean useLogo,
          FormationPieceIcon background) {
        LayeredFormationIcon icon = new LayeredFormationIcon();

        FormationPieceIcon logoIcon = useLogo ? getOriginLogo(iconFaction, gameYear) : null;
        if (logoIcon != null) {
            icon.getPieces().putIfAbsent(LayeredFormationIconLayer.LOGO, new ArrayList<>());
            icon.getPieces().get(LayeredFormationIconLayer.LOGO).add(logoIcon);
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
     * Returns the LOGO piece for the faction's icon for {@code gameYear}, found under the
     * {@link MHQConstants#LAYERED_FORCE_ICON_FACTION_LOGO_CATEGORY} logo category. Older faction files without an icon
     * use their legacy logo from the formation icon logos instead.
     *
     * @return the logo piece, or {@code null} if the faction has no logo or its image cannot be found
     */
    private static @Nullable FormationPieceIcon getOriginLogo(Faction iconFaction, int gameYear) {
        String iconAddress = RATGenerator.getCanonicalFactionLogoAddress(gameYear, iconFaction.getShortName());
        String logo;
        if (iconAddress != null) {
            // Keep any subdirectory of the icon, as the faction icons are registered with their directory structure
            logo = MHQConstants.LAYERED_FORCE_ICON_FACTION_LOGO_CATEGORY + '/'
                         + iconAddress.substring(RATGenerator.FACTION_LOGO_DIRECTORY.length());
        } else {
            logo = Factions.getLegacyFactionLogo(gameYear, iconFaction.getShortName());
            if (logo == null) {
                return null;
            }
        }

        int separator = logo.lastIndexOf('/');
        FormationPieceIcon logoIcon = new FormationPieceIcon(LayeredFormationIconLayer.LOGO,
              logo.substring(0, separator + 1), logo.substring(separator + 1));
        if (logoIcon.getBaseImage() == null) {
            LOGGER.warn("[CompanyGen]   origin logo '{}' not found, using the type fallback", logo);
            return null;
        }
        return logoIcon;
    }

    /**
     * Builds the TYPE / FORMATION / BACKGROUND icon for a single Formation node. Returns {@code null}
     * if the formation's level is unknown (in which case the caller leaves the formation icon-less).
     */
    private static @Nullable LayeredFormationIcon buildFormationIcon(Formation formation,
          Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand, Faction iconFaction,
          FormationPieceIcon background) {
        String formationFilename = formationFilenameFor(formation.getFormationLevel(), iconFaction);
        if (formationFilename == null) {
            // Unknown level (e.g. INVALID, NONE) — skip; the formation gets no auto-icon.
            return null;
        }

        LayeredFormationIcon icon = new LayeredFormationIcon();

        // TYPE: dominant-unit-type silhouette (Mek by default, Aero / BA / ProtoMek / Infantry /
        // vehicle / vessel when those dominate the formation's subtree).
        appendTypePieces(icon, formation, unitLookup, supportCommand);

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
    private static void appendTypePieces(LayeredFormationIcon icon, Formation formation,
          Function<UUID, Unit> unitLookup, @Nullable Formation supportCommand) {
        icon.getPieces().putIfAbsent(LayeredFormationIconLayer.TYPE, new ArrayList<>());

        // A field hospital or a recovery detachment is a formation of vehicles, and vehicles have no StratOps
        // silhouette, so the generic path below would give them nothing but a weight-class letter. StratOps ships an
        // icon for both jobs, so use it: what the formation is for reads better than how heavy it is.
        String purposeIcon = purposeIconFor(formation, unitLookup, supportCommand);
        if (purposeIcon != null) {
            try {
                appendSingle(icon, MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH, purposeIcon);
                LOGGER.info("[CompanyGen][Icons] formation '{}' -> purpose icon {}", formation.getName(), purposeIcon);
                return;
            } catch (Exception exception) {
                LOGGER.warn("[CompanyGen][Icons] cannot resolve purpose icon {} for '{}'; using the unit-type icon",
                      purposeIcon, formation.getName());
                icon.getPieces().get(LayeredFormationIconLayer.TYPE).clear();
            }
        }

        int dominantType = determineDominantUnitType(formation, unitLookup);
        int weightClass = determineWeightClass(formation, unitLookup);
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
                      appendPairOrCenter(icon, stratOps, AEROSPACE_TYPE_FILENAME, stratOps, weightFilename);
                case UnitType.PROTOMEK -> appendSingle(icon, stratOps, PROTOMEK_TYPE_FILENAME);
                case UnitType.BATTLE_ARMOR -> appendSingle(icon, stratOps, BATTLE_ARMOR_TYPE_FILENAME);
                case UnitType.INFANTRY -> appendSingle(icon, stratOps, INFANTRY_TYPE_FILENAME);
                case UnitType.DROPSHIP, UnitType.SMALL_CRAFT -> appendSingle(icon, stratOps, DROPSHIP_TYPE_FILENAME);
                case UnitType.JUMPSHIP -> appendSingle(icon, stratOps, JUMPSHIP_TYPE_FILENAME);
                case UnitType.WARSHIP -> appendSingle(icon, stratOps, WARSHIP_TYPE_FILENAME);
                case UnitType.SPACE_STATION -> appendSingle(icon, stratOps, SPACE_STATION_TYPE_FILENAME);
                case UnitType.VTOL -> appendSingle(icon, stratOps, VTOL_TYPE_FILENAME);
                case UnitType.GUN_EMPLACEMENT -> appendSingle(icon, stratOps, GUN_EMPLACEMENT_TYPE_FILENAME);
                // Tank / Naval have no generic chassis silhouette under StratOps. Fall back to
                // weight-class file alone — matches the "H"/"A"/etc. letter the existing vehicle
                // formations already render, distinct from Mek formations' chassis-plus-letter
                // pair. Adding StratOps-style tank/boat silhouettes to mm-data would let us
                // upgrade these to a paired icon later without changing this code.
                case UnitType.TANK, UnitType.NAVAL -> appendSingle(icon, stratOps, weightFilename);
                default -> appendSingle(icon, stratOps,
                      MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_CENTER_FILENAME);
            }
        } catch (Exception exception) {
            LOGGER.error(exception, "[CompanyGen][Icons] cannot resolve TYPE for '{}' (dominantType={}); falling back to BattleMek (Center)",
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
    /**
     * The icon for what a formation is <em>for</em>, when that is clear from the equipment its units carry.
     *
     * <p>Every unit has to agree, so a company of MASH trucks is a field hospital while a mixed formation that merely
     * contains one is not.</p>
     *
     * @param formation      the formation
     * @param unitLookup     resolves the unit ids in the formation
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return the StratOps type filename, or {@code null} when the formation has no single clear purpose
     */
    static @Nullable String purposeIconFor(Formation formation, Function<UUID, Unit> unitLookup,
          @Nullable Formation supportCommand) {
        if (isHeadquarters(formation, supportCommand)) {
            return HEADQUARTERS_TYPE_FILENAME;
        }

        if (formation.isFormationType(FormationType.CONVOY)) {
            return SUPPLY_TYPE_FILENAME;
        }

        // Asked of the formation rather than of its units, because a recovery vehicle does not have to carry
        // recovery equipment. A BattleMek Recovery Vehicle is an ordinary Tank with a cargo bay and a winch that
        // exists only in its fluff text, so the equipment count below finds nothing on the very formation this icon
        // is for.
        if (formation.isFormationType(FormationType.SALVAGE)) {
            return MAINTENANCE_TYPE_FILENAME;
        }

        // A formation whose vehicles all do one support job carries that job's icon, judged by the same rule that
        // counts them. Recovery vehicles need carry no recovery gear and canteens carry nothing the equipment check
        // below looks for, so without this a Recovery Company or the Commissary fell back to a 'Mek silhouette.
        String capabilityIcon = capabilityIconFor(formation, unitLookup);
        if (capabilityIcon != null) {
            return capabilityIcon;
        }

        int units = 0;
        int medical = 0;
        int recovery = 0;
        Set<SupportSection> sections = EnumSet.noneOf(SupportSection.class);
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = unitLookup.apply(unitId);
            if ((unit == null) || (unit.getEntity() == null)) {
                continue;
            }
            units++;
            if (unit.getEntity().hasWorkingMisc(MiscTypeFlag.F_MASH)) {
                medical++;
            } else if (unit.getEntity().hasWorkingMisc(MiscTypeFlag.F_LIFT_HOIST)
                             || unit.getEntity().hasWorkingMisc(MiscTypeFlag.F_SALVAGE_ARM)) {
                recovery++;
            }
            if (unit.isCarrier()) {
                SupportSection section = sectionOf(unit);
                // EnumSet rejects null, and an empty carrier has no vote to cast anyway.
                if (section != null) {
                    sections.add(section);
                }
            }
        }
        if (units == 0) {
            return null;
        }
        if (medical == units) {
            return MEDICAL_TYPE_FILENAME;
        }
        if (recovery == units) {
            return MAINTENANCE_TYPE_FILENAME;
        }
        // A support team's job is its crew's job. One section only, so a mixed formation keeps the generic icon.
        if (sections.size() == 1) {
            return switch (sections.iterator().next()) {
                case MAINTENANCE -> MAINTENANCE_TYPE_FILENAME;
                case MEDICAL -> MEDICAL_TYPE_FILENAME;
                case COMMAND -> HEADQUARTERS_TYPE_FILENAME;
            };
        }
        return null;
    }

    /**
     * The icon for the one support job every vehicle in the formation does, or {@code null} when they do different jobs,
     * any of them does none, or there are none. Support carriers are the staff's squads and take no part.
     */
    private static @Nullable String capabilityIconFor(Formation formation, Function<UUID, Unit> unitLookup) {
        SupportCapability shared = null;
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = unitLookup.apply(unitId);
            if ((unit == null) || unit.isCarrier()) {
                continue;
            }
            SupportCapability capability = SupportVehicleSelector.capabilityOf(unit.getEntity());
            if ((capability == null) || ((shared != null) && (shared != capability))) {
                return null;
            }
            shared = capability;
        }
        if (shared == null) {
            return null;
        }
        return switch (shared) {
            case SALVAGE -> MAINTENANCE_TYPE_FILENAME;
            case MEDICAL -> MEDICAL_TYPE_FILENAME;
            case LOGISTICS, COMMISSARY -> SUPPLY_TYPE_FILENAME;
            case SECURITY -> null;
        };
    }

    /**
     * Whether this formation is the command's headquarters.
     *
     * <p>Asked of the structure rather than the name: the support tree hangs off the headquarters, so whatever the
     * player has renamed it to, it is the formation the Support Command sits under.</p>
     *
     * @param formation      the formation
     * @param supportCommand the force's Support Command formation, or {@code null} if it has none
     *
     * @return {@code true} if the Support Command formation is a direct child of this formation
     */
    private static boolean isHeadquarters(Formation formation, @Nullable Formation supportCommand) {
        if (supportCommand == null) {
            return false;
        }
        Formation headquarters = supportCommand.getParentFormation();
        return (headquarters != null) && (headquarters.getId() == formation.getId());
    }

    /**
     * The support section a carrier's crew belongs to.
     *
     * <p>Every seat on a carrier holds the same profession, so the first crew member answers for the unit. A carrier
     * that has lost its crew answers for nothing, which keeps an empty one from claiming a section.</p>
     *
     * @param carrier the support carrier
     *
     * @return the section, or {@code null} when the carrier is empty or its crew is not support staff
     */
    private static @Nullable SupportSection sectionOf(Unit carrier) {
        for (Person person : carrier.getCrew()) {
            return SupportPersonnelToTOE.sectionFor(person.getPrimaryRole());
        }
        return null;
    }

    private static int determineDominantUnitType(Formation formation, Function<UUID, Unit> unitLookup) {
        Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = unitLookup.apply(unitId);
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
    private static int determineWeightClass(Formation formation, Function<UUID, Unit> unitLookup) {
        int totalClass = 0;
        int unitCount = 0;
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = unitLookup.apply(unitId);
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
    private static @Nullable String formationFilenameFor(FormationLevel level, Faction faction) {
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
    private static @Nullable FormationPieceIcon buildBackgroundPiece(Faction iconFaction) {
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
