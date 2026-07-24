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

import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorViewUi;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorWalker;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer;
import mekhq.campaign.universe.enums.ForceNamingMethod;

/**
 * Wraps the embedded {@link ForceGeneratorOptionsView} from MegaMek. The faction / echelon / unit
 * type / weight class / rating / experience / augmentation / transport-percent / mission-role pickers
 * the user actually drives at generation time.
 *
 * <p>The view's own Generate / Export MUL / Clear buttons are hidden — the dialog's OK button drives
 * the pipeline. The year field is locked to the campaign year because {@code CommandGenerator}'s
 * Stage 0 re-anchors year from the campaign regardless of what the panel shows; locking the field
 * keeps the displayed value honest.</p>
 *
 * <p>The dialog reads the user's selections on OK via {@link #getOptionsView()} →
 * {@link ForceGeneratorOptionsView#buildForceDescriptor()}, then feeds the result into
 * {@code CommandGenerationOptions.getForceDescriptorSnapshot().populateFromForceDescriptor(fd)}.</p>
 */
public class ForceGeneratorTab {

    private static final MMLogger LOGGER = MMLogger.create(ForceGeneratorTab.class);

    /** Component name for the options/TO&E divider, used to persist its position across dialog opens. */
    private static final String FORCE_GENERATOR_SPLIT_NAME = "forceGeneratorSplitPane";

    private final JFrame frame;
    private final Campaign campaign;
    private CommandGenerationOptions options;
    private ForceGeneratorViewUi viewUi;
    private JSplitPane splitPane;

    // Final TOE names for the previewed formations, keyed by descriptor identity. Rebuilt lazily by
    // previewNameFor after every model change (Generate, Include/Exclude, naming-method switch) so the
    // preview tree always shows the callsigns the committed TOE will use.
    private Map<ForceDescriptor, String> previewNameCache;

    // Live source of the Setup tab's Formation Naming Method combo. The options object only receives
    // the combo's value on OK, so the preview must read the control itself to stay in sync mid-dialog.
    private Supplier<ForceNamingMethod> namingMethodSupplier;

    public ForceGeneratorTab(JFrame frame, Campaign campaign, CommandGenerationOptions options) {
        this.frame = frame;
        this.campaign = campaign;
        this.options = options;
    }

    public JPanel createTab() {
        // Embed MegaMek's full force-generator view: the options panel (left) plus the TO&E tree
        // (right). The view's own Generate button rolls a preview and fills the TO&E tree and the
        // Composition Summary; the dialog's Accept button then commits the rolled ForceDescriptor.
        // Constructed lazily so we only pay the RATGenerator / Ruleset / MekSummaryCache init cost
        // when the dialog is actually shown.
        viewUi = new ForceGeneratorViewUi(frame, campaign == null ? null : campaign.getGameOptions());
        // The dialog commits the previewed tree into the campaign TOE, so let the user right-click to
        // include/exclude nodes; excluded units are struck out here and skipped by ForceDescriptorWalker.
        viewUi.setToeExclusionMode(true);
        // Command Designer: each Generate accumulates into a Model so the player can mix-and-match
        // several rolls into one command before committing.
        viewUi.setAccumulateModel(true);
        // Show the final TOE callsigns ("Able Company", "Able-1 Battle Lance") on the preview's
        // formation nodes instead of the engine's per-parent names, recomputed whenever the model
        // changes so the tree always matches what Accept will commit.
        viewUi.setToeChangeListener(this::invalidatePreviewNames);
        viewUi.setFormationNameProvider(this::previewNameFor);

        ForceGeneratorOptionsView optionsView = viewUi.getOptionsView();
        optionsView.setExportMULButtonVisible(false);
        optionsView.setYearFieldEditable(false);
        if (campaign != null) {
            optionsView.setCurrentYear(campaign.getGameYear());
            // Seed the faction picker from the campaign so the Force Generator opens pre-aligned to
            // the user's New Campaign choice instead of the megamek view's built-in "IS" default.
            // Must run AFTER setCurrentYear (which calls yearUpdated -> refreshFactions and would
            // otherwise reset our selection). On Accept, writeValuesToOptions reads the picker back
            // as an override on CommandGenerationOptions.specifiedFaction so ranks follow.
            Faction campaignFaction = campaign.getFaction();
            if (campaignFaction != null) {
                String code = campaignFaction.getShortName();
                boolean seeded = optionsView.setSelectedFaction(code);
                LOGGER.info("[CompanyGen][ForceGenTab][Faction] seed cbFaction with campaign faction '{}': {}",
                      code, seeded ? "applied" : "skipped (no matching FactionRecord in RATGenerator)");
            }
        }

        // Named so the dialog can register a JSplitPanePreference that persists the divider position.
        // The saved location (if any) is restored when the dialog manages the preference, overriding
        // this default; on first run the default below applies.
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
              viewUi.getLeftPanel(), viewUi.getRightPanel());
        splitPane.setName(FORCE_GENERATOR_SPLIT_NAME);
        splitPane.setResizeWeight(0.55);
        // 1000 is the divider position that shows the full options column without clipping - taken
        // from a saved JSplitPanePreference after dragging it to fit. Restored from that preference on
        // subsequent opens; this default applies on first run.
        splitPane.setDividerLocation(UIUtil.scaleForGUI(1000));

        JPanel host = new JPanel(new BorderLayout());
        host.setName("pnlForceGeneratorTab");
        host.add(splitPane, BorderLayout.CENTER);
        return host;
    }

    /**
     * The options/TO&E divider, or {@code null} if {@link #createTab()} hasn't run yet. The dialog
     * registers this with a {@code JSplitPanePreference} so its position persists across opens.
     *
     * @return the tab's split pane, or {@code null} if not yet built
     */
    public @Nullable JSplitPane getSplitPane() {
        return splitPane;
    }

    /**
     * The force rolled by the view's most recent Generate, or {@code null} if the player hasn't
     * previewed yet. The dialog's Accept button commits exactly this descriptor.
     */
    public ForceDescriptor getGeneratedForce() {
        return viewUi == null ? null : viewUi.getGeneratedForce();
    }

    /**
     * Wires the live source of the Setup tab's Formation Naming Method combo into the preview naming,
     * and is invoked by the pane during construction.
     *
     * @param supplier reads the combo's current selection; {@code null} falls back to the options value
     */
    public void setNamingMethodSupplier(@Nullable Supplier<ForceNamingMethod> supplier) {
        this.namingMethodSupplier = supplier;
    }

    /**
     * Drops the cached preview names and repaints the tree. Called by the view after each Generate and
     * Include/Exclude toggle, and by the pane when the Setup tab's naming method changes.
     */
    public void invalidatePreviewNames() {
        previewNameCache = null;
        if (viewUi != null) {
            viewUi.repaintForceTree();
        }
    }

    /**
     * The final TOE name for {@code descriptor}, from a cache rebuilt on demand by running the same
     * naming traversal the build uses over the currently previewed tree.
     *
     * @param descriptor the formation node being rendered
     * @return the callsign the committed TOE will use, or {@code null} for nodes the walker does not
     *       name (the tree falls back to the engine name)
     */
    private @Nullable String previewNameFor(ForceDescriptor descriptor) {
        if (previewNameCache == null) {
            List<String> existingFormationNames = campaign == null
                  ? List.of()
                  : campaign.getPlayerForce().getAllFormations().stream()
                          .map(Formation::getName)
                          .toList();
            FormationNamer namer = new FormationNamer(currentNamingMethod(), existingFormationNames);
            previewNameCache = ForceDescriptorWalker.previewNames(getGeneratedForce(), namer);
        }
        return previewNameCache.get(descriptor);
    }

    /**
     * The naming method the build would use right now: the Setup tab's live combo selection when wired,
     * otherwise the value already on the options.
     *
     * @return the current naming method, or {@code null} for the namer's default
     */
    private @Nullable ForceNamingMethod currentNamingMethod() {
        if (namingMethodSupplier != null) {
            return namingMethodSupplier.get();
        }
        return options == null ? null : options.getForceNamingMethod();
    }

    /**
     * Returns the embedded {@link ForceGeneratorOptionsView}, or {@code null} if {@link #createTab()}
     * hasn't run yet. The Company Generation dialog calls this on OK to read the user's selections.
     */
    public ForceGeneratorOptionsView getOptionsView() {
        return viewUi == null ? null : viewUi.getOptionsView();
    }

    /**
     * Convenience: builds the {@link ForceDescriptor} from the current view state. Equivalent to
     * {@code getOptionsView().buildForceDescriptor()} with a null-guard.
     */
    public ForceDescriptor buildForceDescriptor() {
        ForceGeneratorOptionsView optionsView = getOptionsView();
        return optionsView == null ? null : optionsView.buildForceDescriptor();
    }

    /**
     * The view's current state is the source of truth — there's nothing to push from the options here
     * because the snapshot the dialog persists is generated from the view's controls on OK rather than
     * driving them. If a future preset round-trip needs to set the view's controls from saved values,
     * that goes here.
     */
    public void loadValuesFromOptions(CommandGenerationOptions sourceOptions) {
        this.options = sourceOptions;
    }

    /**
     * Reads the user's force-shape picks back into the options' snapshot. The dialog can call this on
     * OK as an alternative to going through {@link #getOptionsView()} directly.
     */
    public void writeValuesToOptions(CommandGenerationOptions targetOptions) {
        ForceGeneratorOptionsView optionsView = getOptionsView();
        if (targetOptions == null || optionsView == null) {
            return;
        }
        ForceDescriptor fd = optionsView.buildForceDescriptor();
        if (fd != null) {
            targetOptions.getForceDescriptorSnapshot().populateFromForceDescriptor(fd);
            // Override the rank-authority faction with whatever the user picked in the Force Gen
            // panel's faction selector. CommandGenerationDialog seeded both inputs from
            // campaign.getFaction() at dialog open (this tab into cbFaction via createTab,
            // CommandGenerationOptions.specifiedFaction via seedSpecifiedFactionFromCampaign).
            // If the user changed cbFaction here mid-dialog, that becomes the final authority for
            // rank assignment so the rank picker and the unit picker stay aligned.
            String snapshotFactionCode = fd.getFaction();
            if (snapshotFactionCode != null && !snapshotFactionCode.isBlank()) {
                Faction override = Factions.getInstance().getFaction(snapshotFactionCode);
                if (override != null) {
                    Faction previous = targetOptions.getSpecifiedFaction();
                    targetOptions.setSpecifiedFaction(override);
                    LOGGER.info("[CompanyGen][ForceGenTab][Faction] override specifiedFaction: '{}' -> '{}' (sourced from cbFaction in Force Gen panel)",
                          previous == null ? "null" : previous.getShortName(),
                          override.getShortName());
                } else {
                    LOGGER.warn("[CompanyGen][ForceGenTab][Faction] cbFaction code '{}' has no matching mekhq.campaign.universe.Faction; leaving specifiedFaction='{}' unchanged",
                          snapshotFactionCode,
                          targetOptions.getSpecifiedFaction() == null
                                ? "null"
                                : targetOptions.getSpecifiedFaction().getShortName());
                }
            }
        }
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CommandGenerationOptions getOptions() {
        return options;
    }
}
