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
package mekhq.gui.commandGeneration.contents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JFrame;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.ExistingLift;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorViewUi;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.DraftBloodnames;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorWalker;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer;
import mekhq.campaign.universe.enums.ForceNamingMethod;

/**
 * The force-rolling engine behind the Command Designer's Force page.
 *
 * <p>MegaMek's {@link ForceGeneratorViewUi} does the work - which factions, unit types, sizes and ratings a selection
 * offers, rolling the force, accumulating several rolls into one command - and this keeps one alive without showing
 * it. The Force page draws its own heads-up controls over the view's {@link ForceGeneratorOptionsView} through that
 * view's host-control API, and its own board over the rolled tree this exposes.</p>
 *
 * <p>The year is locked to the campaign year because {@code CommandGenerator}'s Stage 0 re-anchors year from the
 * campaign regardless. The dialog reads the selections on Accept via {@link #writeValuesToOptions}.</p>
 */
public class ForceGeneratorTab {
    private static final MMLogger LOGGER = MMLogger.create(ForceGeneratorTab.class);

    private final JFrame frame;
    private final Campaign campaign;
    private CommandGenerationOptions options;
    private ForceGeneratorViewUi viewUi;

    // Final TOE names for the previewed formations, keyed by descriptor identity. Rebuilt lazily by
    // previewName after every model change (roll, include/exclude, naming-method switch) so the
    // board always shows the names the committed TOE will use.
    private Map<ForceDescriptor, String> previewNameCache;
    // Live source of the People page's Formation Names choice. The options object only receives
    // the choice on Accept, so the preview must read the control itself to stay in sync mid-dialog.
    private Supplier<ForceNamingMethod> namingMethodSupplier;
    private BooleanSupplier numberRegimentsSupplier;

    private static final int PREFERRED_ECHELON_INNER_SPHERE = 4;
    private static final int PREFERRED_ECHELON_CLAN = 5;

    private Consumer<FactionRecord> factionChangeListener;
    private final List<Consumer<ForceDescriptor>> forceGeneratedListeners = new ArrayList<>();
    private final List<Runnable> modelChangeListeners = new ArrayList<>();

    /** "Generate Company Command Lance": a campaign-layer option that changes what a roll produces. */
    private boolean generateCompanyCommandLance;

    public ForceGeneratorTab(JFrame frame, Campaign campaign, CommandGenerationOptions options) {
        this.frame = frame;
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * Builds the force generator view and seeds it from the campaign. Constructed lazily so the RATGenerator /
     * Ruleset / MekSummaryCache init cost is only paid when the designer is actually shown.
     */
    public void initializeEngine() {
        viewUi = new ForceGeneratorViewUi(frame, campaign == null ? null : campaign.getGameOptions());
        // A command built in layers starts each roll from the ships the campaign already owns, so a
        // second session's armour company rides in the spare bays of the first session's DropShips
        // rather than drawing its own.
        viewUi.setHostLiftSupplier(this::liftAlreadyInHangar);
        viewUi.setToeExclusionMode(true);
        // Each roll accumulates into one command, so the player can mix and match several rolls before
        // committing.
        viewUi.setAccumulateModel(true);
        viewUi.setToeChangeListener(this::modelChanged);
        viewUi.setFormationNameProvider(this::previewName);
        viewUi.setForceGeneratedListener(this::forceGenerated);

        ForceGeneratorOptionsView optionsView = viewUi.getOptionsView();
        optionsView.setOnFactionChanged(this::factionChanged);
        optionsView.setExportMULButtonVisible(false);
        // The role filters restrict every draw in the tree to units that can fill the ticked role, which
        // suits picking a lance of artillery carriers and not designing a command; the formation mix is
        // the tool for shaping a command.
        optionsView.setMissionRoleFiltersVisible(false);
        optionsView.setYearFieldEditable(false);
        if (campaign != null) {
            // Before setCurrentYear, which populates the pickers: the preference decides what they open on.
            applyPreferredEchelon(campaign.getPlayerForce().isClanForce());
            optionsView.setCurrentYear(campaign.getGameYear());
            // Seed the faction from the campaign so the designer opens aligned to the campaign's faction
            // rather than the view's built-in "IS" default. Must run after setCurrentYear, which would
            // otherwise reset the selection.
            Faction campaignFaction = campaign.getPlayerForce().getFaction();
            if (campaignFaction != null) {
                String code = campaignFaction.getShortName();
                boolean seeded = optionsView.setSelectedFaction(code);
                LOGGER.info("[CompanyGen][ForceGenTab][Faction] seed faction picker with campaign faction '{}': {}",
                      code, seeded ? "applied" : "skipped (no matching FactionRecord in RATGenerator)");
            }
        }
    }

    private ExistingLift liftAlreadyInHangar() {
        if (campaign == null) {
            return ExistingLift.NONE;
        }
        List<Entity> entities = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if (unit.getEntity() != null) {
                entities.add(unit.getEntity());
            }
        }
        return ExistingLift.of(entities);
    }

    /**
     * @return the accumulated command the player has rolled, wrapped in the view's model root, or {@code null} if
     *       nothing has been rolled yet; Accept commits exactly this
     */
    public @Nullable ForceDescriptor getGeneratedForce() {
        return viewUi == null ? null : viewUi.getGeneratedForce();
    }

    /** Rolls another force into the command, as the view's own Generate button does. */
    public void requestGenerate() {
        if (viewUi != null) {
            viewUi.getOptionsView().requestGenerate();
        }
    }

    /** Discards everything rolled so far, so the next roll starts a fresh command. */
    public void clearRolls() {
        if (viewUi != null) {
            viewUi.clearForce();
        }
    }

    /**
     * Registers a listener told after each roll, with the rolled force, and after the rolls are cleared, with
     * {@code null}.
     *
     * @param listener the listener
     */
    public void addForceGeneratedListener(Consumer<ForceDescriptor> listener) {
        forceGeneratedListeners.add(listener);
    }

    /**
     * Registers a listener told after anything that changes what a commit would produce: a roll, clearing the rolls,
     * or a unit struck out or brought back.
     *
     * @param listener the listener
     */
    public void addModelChangeListener(Runnable listener) {
        modelChangeListeners.add(listener);
    }

    private void forceGenerated(@Nullable ForceDescriptor rolled) {
        awardDraftBloodnames(rolled);
        for (Consumer<ForceDescriptor> listener : List.copyOf(forceGeneratedListeners)) {
            listener.accept(rolled);
        }
    }

    /**
     * Tells the engine that what a commit would produce has changed outside the view, such as a unit struck out on
     * the board, so the preview names are recomputed and the listeners told.
     */
    public void modelChanged() {
        invalidatePreviewNames();
        for (Runnable listener : List.copyOf(modelChangeListeners)) {
            listener.run();
        }
    }

    /**
     * Gives a freshly rolled Clan force its share of Bloodnames, on the crews the board shows. The award has to land
     * on the descriptor's crews before the build, so the board can show them and the build can see them.
     */
    private void awardDraftBloodnames(@Nullable ForceDescriptor rolled) {
        if ((rolled == null) || (campaign == null) || (rolled.getFaction() == null)) {
            return;
        }
        Faction faction = resolveRankAuthority(rolled.getFaction());
        int awarded = DraftBloodnames.award(campaign, rolled, faction);
        if (awarded > 0) {
            invalidatePreviewNames();
        }
    }

    public void setFactionChangeListener(@Nullable Consumer<FactionRecord> listener) {
        this.factionChangeListener = listener;
        // The faction was seeded during initializeEngine, before the pane had a host listener. Registering again
        // reports the current selection immediately so the host can apply its faction-driven defaults.
        if (viewUi != null) {
            viewUi.getOptionsView().setOnFactionChanged(this::factionChanged);
        }
    }

    private void factionChanged(@Nullable FactionRecord faction) {
        applyPreferredEchelon((faction != null) && faction.isClan());
        if (factionChangeListener != null) {
            factionChangeListener.accept(faction);
        }
    }

    /**
     * Opens the size picker on a company for an Inner Sphere faction and a Trinary-equivalent for a Clan, the sizes a
     * new command most often starts at.
     */
    private void applyPreferredEchelon(boolean isClanFaction) {
        if (viewUi == null) {
            return;
        }
        int echelon = isClanFaction ? PREFERRED_ECHELON_CLAN : PREFERRED_ECHELON_INNER_SPHERE;
        viewUi.getOptionsView().setPreferredEchelon(echelon);
        LOGGER.info("[CompanyGen][ForceGenTab][Echelon] size picker opens on echelon {} (clan={})", echelon,
              isClanFaction);
    }

    public void setNamingMethodSupplier(@Nullable Supplier<ForceNamingMethod> supplier) {
        this.namingMethodSupplier = supplier;
    }

    public void setNumberRegimentsSupplier(@Nullable BooleanSupplier supplier) {
        this.numberRegimentsSupplier = supplier;
    }

    private boolean currentNumberRegiments() {
        if (numberRegimentsSupplier != null) {
            return numberRegimentsSupplier.getAsBoolean();
        }
        return (options != null) && options.isAlwaysNumberRegiments();
    }

    /** Drops the cached preview names, so the next read recomputes them from the current model and naming. */
    public void invalidatePreviewNames() {
        previewNameCache = null;
        if (viewUi != null) {
            viewUi.repaintForceTree();
        }
    }

    /**
     * @param descriptor a formation in the rolled command
     *
     * @return the name the committed TO&amp;E will give it, or {@code null} if it gets none of its own
     */
    public @Nullable String previewName(ForceDescriptor descriptor) {
        if (previewNameCache == null) {
            List<String> existingFormationNames = campaign == null
                  ? List.of()
                  : campaign.getPlayerForce().getAllFormations().stream()
                          .map(Formation::getName)
                          .toList();
            FormationNamer namer = new FormationNamer(currentNamingMethod(), existingFormationNames);
            namer.setAlwaysNumberRegiments(currentNumberRegiments());
            previewNameCache = ForceDescriptorWalker.previewNames(getGeneratedForce(), namer);
        }
        return previewNameCache.get(descriptor);
    }

    private @Nullable ForceNamingMethod currentNamingMethod() {
        if (namingMethodSupplier != null) {
            return namingMethodSupplier.get();
        }
        return options == null ? null : options.getForceNamingMethod();
    }

    /** @return the view whose pickers the Force page drives, or {@code null} before {@link #initializeEngine()} */
    public @Nullable ForceGeneratorOptionsView getOptionsView() {
        return viewUi == null ? null : viewUi.getOptionsView();
    }

    public ForceDescriptor buildForceDescriptor() {
        ForceGeneratorOptionsView optionsView = getOptionsView();
        return optionsView == null ? null : optionsView.buildForceDescriptor();
    }

    /** Forgets the formation the mix asks for, so a restored set of defaults asks for none. */
    public void clearFormationMix() {
        if (viewUi != null) {
            viewUi.getOptionsView().clearFormationSelection();
        }
    }

    /**
     * Points the formation mix at a formation on the board, so its editor offers that formation's options rather than
     * the settings'.
     *
     * @param node the formation, or {@code null} to hand the mix back to the settings
     */
    public void setFormationMixContext(@Nullable ForceDescriptor node) {
        if (viewUi != null) {
            viewUi.getOptionsView().setFormationMixContext(node);
        }
    }

    public boolean isGenerateCompanyCommandLance() {
        return generateCompanyCommandLance;
    }

    public void setGenerateCompanyCommandLance(boolean generateCompanyCommandLance) {
        this.generateCompanyCommandLance = generateCompanyCommandLance;
    }

    public void loadValuesFromOptions(CommandGenerationOptions sourceOptions) {
        this.options = sourceOptions;
        if (sourceOptions != null) {
            generateCompanyCommandLance = sourceOptions.isGenerateMercenaryCompanyCommandLance();
        }
    }

    public void writeValuesToOptions(CommandGenerationOptions targetOptions) {
        ForceGeneratorOptionsView optionsView = getOptionsView();
        if (targetOptions == null || optionsView == null) {
            return;
        }
        targetOptions.setGenerateMercenaryCompanyCommandLance(generateCompanyCommandLance);
        ForceDescriptor fd = optionsView.buildForceDescriptor();
        if (fd != null) {
            targetOptions.getForceDescriptorSnapshot().populateFromForceDescriptor(fd);
            // Override the rank-authority faction with whatever the player picked on the Force page.
            // CommandGenerationDialog seeded both inputs from campaign.getPlayerForce().getFaction() at
            // dialog open (the picker via initializeEngine, CommandGenerationOptions.specifiedFaction via
            // seedSpecifiedFactionFromCampaign). If the player changed the faction mid-dialog, that becomes
            // the final authority for rank assignment so the rank picker and the unit picker stay aligned.
            String snapshotFactionCode = fd.getFaction();
            if (snapshotFactionCode != null && !snapshotFactionCode.isBlank()) {
                Faction override = resolveRankAuthority(snapshotFactionCode);
                if (override != null) {
                    Faction previous = targetOptions.getSpecifiedFaction();
                    targetOptions.setSpecifiedFaction(override);
                    LOGGER.info("[CompanyGen][ForceGenTab][Faction] override specifiedFaction: '{}' -> '{}' (sourced from the Force page's faction picker)",
                          previous == null ? "null" : previous.getShortName(),
                          override.getShortName());
                } else {
                    LOGGER.warn("[CompanyGen][ForceGenTab][Faction] faction code '{}' has no matching mekhq.campaign.universe.Faction, nor does any parent of it; leaving specifiedFaction='{}' unchanged",
                          snapshotFactionCode,
                          targetOptions.getSpecifiedFaction() == null
                                ? "null"
                                : targetOptions.getSpecifiedFaction().getShortName());
                }
            }
        }
    }

    /**
     * Finds the faction whose ranks a command generated for the given RAT Generator faction key should
     * use.
     *
     * <p>Sub-faction keys are a RAT Generator idea and mostly have no {@link Faction} of their own -
     * there is no Word of Blake Shadow Divisions faction, only Word of Blake. Read literally, picking
     * a sub-faction therefore found nothing and left the rank authority as whatever it already was,
     * which is the campaign's own faction, so a Shadow Division came out wearing the player's ranks
     * instead of the Word of Blake Militia's. Falling back through the parents fixes that, and does
     * the same for every other sub-faction: {@code CFM.MiKrKl} resolves to {@code CFM}.</p>
     *
     * @param factionCode the RAT Generator faction key the force was generated for
     *
     * @return the closest matching faction, or {@code null} if neither it nor any parent of it is one
     */
    static @Nullable Faction resolveRankAuthority(String factionCode) {
        // Membership has to be tested against the key list: getFaction never returns null, handing
        // back a blank "Unknown" faction for anything it does not have. A null check therefore always
        // passed, and picking a sub-faction set the rank authority to that placeholder - which is what
        // put a Shadow Division in the wrong ranks rather than merely leaving them alone.
        Collection<String> knownFactionCodes = Factions.getInstance().getFactionList();
        String candidate = factionCode;
        while (!candidate.isBlank()) {
            if (knownFactionCodes.contains(candidate)) {
                if (!candidate.equals(factionCode)) {
                    LOGGER.info("[CompanyGen][ForceGenTab][Faction] '{}' is a sub-faction with no"
                                + " faction of its own; taking ranks from its parent '{}'",
                          factionCode, candidate);
                }
                return Factions.getInstance().getFaction(candidate);
            }
            int lastSeparator = candidate.lastIndexOf('.');
            if (lastSeparator < 0) {
                return null;
            }
            candidate = candidate.substring(0, lastSeparator);
        }
        return null;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CommandGenerationOptions getOptions() {
        return options;
    }
}
