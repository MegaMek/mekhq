/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.commandGeneration;

import java.awt.Component;
import java.awt.Insets;
import javax.swing.JButton;
import megamek.client.ui.util.UIUtil;
import mekhq.campaign.campaignOptions.CampaignOption;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.PersonUtility.reRollAdvantages;
import static mekhq.campaign.personnel.PersonUtility.reRollLoyalty;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.getCommandGenerationResourceBundle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.enums.ValidationState;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.reputation.camOpsReputation.ForceReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.StartingSimulation;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;
import mekhq.campaign.universe.commandGeneration.ratgen.RulesetEngineBootstrap;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.campaignOptions.optionChangeDialogs.AdvancedScoutingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;

/**
 * Top-level dialog for the Command Generator. Hosts a
 * {@link CommandGenerationPane} with three tabs (Personnel &amp; Officers, Force Generator,
 * Spares &amp; Finances) and runs the ratgen pipeline on Accept &amp; Build.
 *
 * <p>The tabs persist user preferences into {@link CommandGenerationOptions} and the campaign's
 * auto-logistics percentages, then {@link CommandGenerator} materializes the previewed command into
 * the campaign.</p>
 *
 * @author Justin "Windchild" Bowen (original)
 */
public class CommandGenerationDialog extends AbstractMHQValidationButtonDialog {

    private static final MMLogger LOGGER = MMLogger.create(CommandGenerationDialog.class);

    /**
     * The settings as they stood when the model was last generated, or {@code null} while there is no model.
     * Accept compares the tabs against this so a setting changed after Generate cannot silently produce a
     * command that differs from the one previewed.
     */
    private CommandGenerationOptions settingsAtLastGenerate;

    private Campaign campaign;
    private CommandGenerationOptions commandGenerationOptions;
    private CommandGenerationPane pane;

    public CommandGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CommandGenerationDialog", "CommandGenerationDialog.title");
        setCampaign(campaign);
        setCommandGenerationOptions(null);
        initialize();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
    }

    public @Nullable CommandGenerationOptions getCommandGenerationOptions() {
        return commandGenerationOptions;
    }

    public void setCommandGenerationOptions(final @Nullable CommandGenerationOptions commandGenerationOptions) {
        this.commandGenerationOptions = commandGenerationOptions;
    }

    public CommandGenerationPane getPane() {
        return pane;
    }

    @Override
    protected Container createCenterPane() {
        CommandGenerationOptions startingOptions;
        if (commandGenerationOptions != null) {
            startingOptions = commandGenerationOptions;
        } else {
            startingOptions = new CommandGenerationOptions();
            seedSpecifiedFactionFromCampaign(startingOptions, "createCenterPane");
        }
        pane = new CommandGenerationPane(getFrame(), getCampaign(), startingOptions);

        // Populate every tab from the supplied options on first show.
        pane.getSetupTab().loadValuesFromOptions(startingOptions);
        pane.getForceGeneratorTab().loadValuesFromOptions(startingOptions);
        pane.getSparesAndFinancesTab().loadValuesFromOptions(startingOptions);
        pane.getForceGeneratorTab().setForceGeneratedListener(this::rememberSettingsAtGenerate);

        // Settings that follow from the faction are applied after the tabs are populated, not before.
        // Loading the options sets the naming method directly, so a Clan's Greek naming would
        // otherwise be applied while the pane is built and then overwritten a moment later.
        pane.applyFactionDrivenDefaults();

        // Persistent design-stage banner so the player always knows this workspace is a draft: nothing
        // reaches the campaign until "Accept & Build Command". Text-only with a bottom separator (no
        // background fill) so it reads cleanly under both the light and Darcula look-and-feels.
        JPanel content = new JPanel(new BorderLayout());
        JLabel designBanner = new JLabel(resources.getString("CommandGenerationDialog.banner.text"));
        Color separatorColor = UIManager.getColor("Separator.foreground");
        if (separatorColor == null) {
            separatorColor = Color.GRAY;
        }
        designBanner.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor),
              BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        content.add(designBanner, BorderLayout.NORTH);
        // The tabbed pane is added directly rather than inside a scroll pane of its own: each tab
        // already scrolls its own content, and a scroll pane wrapped around them gave the inner ones
        // all the height they asked for. They then had nothing to scroll but still swallowed every
        // mouse-wheel event, so the wheel did nothing anywhere in the dialog. Adding it directly
        // bounds each tab's viewport to the dialog and puts the wheel back to work, and keeps the tab
        // strip in place instead of letting it scroll off the top.
        content.add(pane, BorderLayout.CENTER);
        return content;
    }

    @Override
    protected JPanel createButtonPanel() {
        // Standard dialog button arrangement: destructive / reset actions on the far left,
        // primary actions on the right with the default Generate button at the far right so
        // Enter triggers it. Replaces the previous 2x2 grid where Cancel sat diagonally
        // opposite Generate — that layout was awkward to scan.
        final JPanel panel = new JPanel(new BorderLayout());

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        // The Force Generator's own Generate, its options and Clear Force belong on this bar rather than mid-tab:
        // they are dialog-level actions like the ones either side of them, and the tab needs the vertical room for
        // the formation mix. Adding them here re-parents them out of the tab.
        pane.getForceGeneratorTab().getGenerateControls().forEach(control -> leftButtons.add(control));
        // Restore Defaults sits after them: it undoes the settings those buttons act on, so it reads as the end of
        // that group rather than as something to pass over on the way to Generate.
        leftButtons.add(new MMButton("btnRestore", resources, "RestoreDefaults.text",
              "CommandGenerationDialog.btnRestore.toolTipText", this::restoreDefaultsActionListener));
        panel.add(leftButtons, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        // "Close Without Building" rather than a generic Cancel: the dialog is a design workspace, so closing
        // it throws away an uncommitted model rather than cancelling a settings edit.
        rightButtons.add(new MMButton("btnCancel", resources, "CommandGenerationDialog.btnCancel.text",
              "CommandGenerationDialog.btnCancel.toolTipText", this::cancelActionPerformed));
        // The model is built on the Force Generator tab (its Generate button accumulates rolls);
        // "Accept & Build Command" is the one action that commits the model to the campaign TOE.
        setOkButton(new MMButton("btnAccept", resources, "CommandGenerationDialog.btnAccept.text",
              "CommandGenerationDialog.btnAccept.toolTipText", this::confirmationActionListener));
        rightButtons.add(getOkButton());
        panel.add(rightButtons, BorderLayout.EAST);

        enlargeButtons(leftButtons);
        enlargeButtons(rightButtons);
        return panel;
    }

    /**
     * Gives every button on the bar more padding and a slightly larger face. At default size the bar reads as
     * an afterthought under a dialog this large, and Accept &amp; Build is the one action that changes the
     * campaign, so it should not be the smallest thing on screen.
     *
     * @param bar the button strip to enlarge; checkboxes and labels on it are left alone
     */
    private static void enlargeButtons(JPanel bar) {
        Insets padding = new Insets(UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(14),
              UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(14));
        for (Component component : bar.getComponents()) {
            if (component instanceof JButton button) {
                button.setMargin(padding);
                setFontScaling(button, false, 1.1);
            }
        }
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        // Persist the Force Generator tab's options/TO&E divider so it reopens where the user left it.
        JSplitPane forceGeneratorSplit = pane.getForceGeneratorTab().getSplitPane();
        if (forceGeneratorSplit != null) {
            preferences.manage(new JSplitPanePreference(forceGeneratorSplit));
        }
    }

    private void confirmationActionListener(final ActionEvent evt) {
        okButtonActionPerformed(evt);

        Faction campaignFaction = campaign.getPlayerForce().getFaction();
        String campaignFactionCode = campaignFaction.getShortName();
        if (campaignFactionCode.equals(MERCENARY_FACTION_CODE)) {
            final boolean IS_STARTUP = true;
            final boolean IS_NEW_ORGANIZATION = true;
            campaign.checkForNewMercenaryOrganizationStartUp(IS_STARTUP, IS_NEW_ORGANIZATION);
            return;
        }

        PersonnelRole role = campaignFaction.isClan() ? PersonnelRole.MEKWARRIOR : PersonnelRole.MILITARY_LIAISON;
        Person speaker = campaign.getPlayerForce().getHumanResources().newPerson(campaign, role, campaignFactionCode, Gender.RANDOMIZE);
        new FactionJudgmentDialog(campaign, speaker, campaign.getPlayerForce().getHumanResources().getCommander(campaign.getCampaignOptions(), campaign.getPlayerForce().isClanForce(), campaign.getLocalDate()), "HELLO", campaignFaction,
              FactionStandingJudgmentType.WELCOME, ImmersiveDialogWidth.MEDIUM, null, null);
    }

    private void restoreDefaultsActionListener(final ActionEvent evt) {
        CommandGenerationOptions defaults = new CommandGenerationOptions();
        seedSpecifiedFactionFromCampaign(defaults, "restoreDefaults");
        pane.getSetupTab().loadValuesFromOptions(defaults);
        pane.getForceGeneratorTab().loadValuesFromOptions(defaults);
        // The formation mix lives on the embedded MegaMek view rather than in CommandGenerationOptions, so loading
        // defaults does not reach it and it has to be cleared explicitly.
        pane.getForceGeneratorTab().clearFormationMix();
        pane.getSparesAndFinancesTab().loadValuesFromOptions(defaults);
        // As on first show: restoring defaults sets the naming method directly, so the faction's own
        // convention is re-applied afterwards rather than being overwritten by it.
        pane.applyFactionDrivenDefaults();
    }

    /**
     * Overwrites {@link CommandGenerationOptions#getSpecifiedFaction()} with the current
     * campaign's faction. The CommandGenerationOptions constructor defaults specifiedFaction to a
     * global default (typically Mercenary) regardless of campaign, so a Clan campaign loading the
     * dialog for the first time would otherwise have a MERC rank-authority faction seeded —
     * meaning {@code RulesetRankAssigner} resolves to MERC, picks the IS rank-index policy
     * (enlisted=12 / support=8), and assigns IS rank names to Clan Persons. Seeding here makes
     * the campaign-creation faction choice the default for rank assignment too.
     *
     * @param options the fresh options about to be handed to the tabs
     * @param caller  identifier for the call site (logged so traces can distinguish first-open
     *                from restore-defaults from OK paths)
     */
    private void seedSpecifiedFactionFromCampaign(CommandGenerationOptions options, String caller) {
        if (options == null || campaign == null) {
            return;
        }
        Faction campaignFaction = campaign.getPlayerForce().getFaction();
        if (campaignFaction == null) {
            LOGGER.warn("[CompanyGen][Dialog][Faction] seed({}): campaign has no faction, leaving specifiedFaction='{}'",
                  caller,
                  options.getSpecifiedFaction() == null ? "null" : options.getSpecifiedFaction().getShortName());
            return;
        }
        Faction previous = options.getSpecifiedFaction();
        options.setSpecifiedFaction(campaignFaction);
        LOGGER.info("[CompanyGen][Dialog][Faction] seed({}): specifiedFaction '{}' -> '{}' (from campaign.getPlayerForce().getFaction())",
              caller,
              previous == null ? "null" : previous.getShortName(),
              campaignFaction.getShortName());
    }

    /**
     * Reads every tab into a fresh options object. Reading the tabs has no side effect on the campaign; the
     * spares spinners, which are campaign options, are written separately at Accept.
     */
    private CommandGenerationOptions collectOptionsFromTabs(String caller) {
        CommandGenerationOptions options = new CommandGenerationOptions();
        seedSpecifiedFactionFromCampaign(options, caller);
        pane.getSetupTab().writeValuesToOptions(options);
        pane.getForceGeneratorTab().writeValuesToOptions(options);
        pane.getSparesAndFinancesTab().writeValuesToOptions(options);
        return options;
    }

    /**
     * Keeps the settings that produced the model. A roll snapshots the tabs; Clear Force drops the snapshot
     * with the model it described.
     *
     * @param generated the rolled force, or {@code null} on Clear Force
     */
    private void rememberSettingsAtGenerate(@Nullable ForceDescriptor generated) {
        if (generated == null) {
            settingsAtLastGenerate = null;
            LOGGER.debug("[CompanyGen][Dialog][Settings] force cleared; no settings to hold the build to");
            return;
        }
        settingsAtLastGenerate = collectOptionsFromTabs("generate");
        LOGGER.info("[CompanyGen][Dialog][Settings] settings captured with the generated model");
    }

    /**
     * Decides which settings the build uses when the tabs have moved since the last Generate.
     *
     * @param current the settings as the tabs stand now
     *
     * @return the settings to build with, or {@code null} if the build should not go ahead
     */
    private @Nullable CommandGenerationOptions settingsToBuildWith(CommandGenerationOptions current) {
        if ((settingsAtLastGenerate == null) || settingsAtLastGenerate.equals(current)) {
            return current;
        }
        LOGGER.info("[CompanyGen][Dialog][Settings] settings changed since the last Generate; asking the player");
        Object[] buttonLabels = { resources.getString("CommandGenerationDialog.settingsChanged.previewed"),
                                   resources.getString("CommandGenerationDialog.settingsChanged.regenerate"),
                                   resources.getString("Cancel.text") };
        int choice = JOptionPane.showOptionDialog(getFrame(),
              resources.getString("CommandGenerationDialog.settingsChanged.text"),
              resources.getString("CommandGenerationDialog.settingsChanged.title"),
              JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, buttonLabels, buttonLabels[0]);
        if (choice == 0) {
            LOGGER.info("[CompanyGen][Dialog][Settings] building with the settings the model was generated under");
            return settingsAtLastGenerate;
        }
        if (choice == 1) {
            LOGGER.info("[CompanyGen][Dialog][Settings] regenerating with the current settings");
            pane.showForceGeneratorTab();
            pane.getForceGeneratorTab().requestGenerate();
        } else {
            LOGGER.info("[CompanyGen][Dialog][Settings] build cancelled at the changed-settings prompt");
        }
        return null;
    }

    @Override
    protected void okAction() {
        CommandGenerationOptions options = settingsToBuildWith(collectOptionsFromTabs("okAction"));
        if (options == null) {
            return;
        }
        // The spares spinners are campaign options; they take effect now, at the commit.
        pane.getSparesAndFinancesTab().writeSparesToCampaignOptions();

        // Accept commits the exact force the player previewed on the Force Generator tab (its Generate
        // button rolls the ForceDescriptor and fills the TO&E tree + Composition Summary). Require a
        // preview first so Accept never silently rolls something the player never saw.
        ForceDescriptor previewedForce = pane.getForceGeneratorTab().getGeneratedForce();
        if (previewedForce == null) {
            JOptionPane.showMessageDialog(getFrame(),
                  resources.getString("CommandGenerationDialog.noPreview.text"),
                  resources.getString("CommandGenerationDialog.title"),
                  JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // One commit gate: this is the moment the design model becomes real. The confirmation states
        // what will be built and folds in any resource-cost warnings (long generation, high Person
        // counts) so the player makes a single Build/Cancel decision.
        //
        // Plain text rather than HTML for the message: an HTML-bearing JOptionPane goes through
        // BasicHTML / BasicTextUI / DefaultCaret on dismissal. When this modal disposes and the
        // next modal (the GenerationProgressDialog) opens immediately afterward, the pending caret
        // repaint event fires against a torn-down view and throws
        // "Cannot invoke java.util.Vector.add(Object) because this.viewBuffer is null" (a
        // long-standing Swing bug). Plain text routes through BasicLabelUI instead and avoids the
        // FlowView code path entirely.
        Integer chosenEchelon = options.getForceDescriptorSnapshot().getEchelon();
        int combatUnitCount = countCombatUnits(previewedForce);
        if (!confirmBuildCommand(options, chosenEchelon, combatUnitCount)) {
            return;
        }

        // Build the command in two background phases behind a modal progress dialog (long
        // generations would otherwise look frozen). Phase one commits the model's combat force to
        // the TOE without support; phase two sizes support to the committed combat force. Support is
        // command-creation-only, so - unlike the old interim auto-prompt MVP - it always runs as part
        // of the build rather than asking again mid-flow. The worker's done() handler runs the
        // post-gen extras on the EDT.
        LOGGER.info("[CompanyGen][Worker] okAction: starting combat-commit phase (thread={})",
              Thread.currentThread().getName());
        // Snapshot the hangar before the combat phase so the starting-cash stage after support can
        // price exactly the units this build creates (combat + support), not pre-existing ones.
        Set<UUID> preExistingUnitIds = CommandGenerator.snapshotHangarUnitIds(getCampaign());
        runGenerationPhase("Materializing combat forces...", listener -> {
            // RATGenerator init must not run on the EDT; ensureLoaded is idempotent.
            ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();
            RulesetEngineBootstrap.ensureLoaded(snapshot.getYear());
            return CommandGenerator.applyToCampaign(getCampaign(), options, previewedForce, listener, false);
        }, combatResult -> {
            if (combatResult == null) {
                LOGGER.info("[CompanyGen][Worker] combat phase produced no result; skipping support");
                return;
            }
            List<Person> generatedPersons = new ArrayList<>(combatResult.generatedPersons());
            runGenerationPhase("Generating support forces...",
                  supportListener -> CommandGenerator.generateSupportFromToe(getCampaign(), options,
                        supportListener),
                  supportPersons -> {
                      generatedPersons.addAll(supportPersons);
                      CommandGenerator.processStartingCash(getCampaign(), options, preExistingUnitIds,
                            combatResult.rolledUnitIds(), generatedPersons, combatResult.spareCosts());
                      runStartingSimulationThen(options, generatedPersons);
                  });
        });
    }

    /**
     * Runs the starting simulation as a background phase of its own when it was asked for, then the
     * post-generation extras. Without it the extras run straight away.
     */
    private void runStartingSimulationThen(CommandGenerationOptions options, List<Person> generatedPersons) {
        if (!options.isRunStartingSimulation()) {
            LOGGER.info("[CompanyGen][Simulation] off; the command starts with no history");
            applyPostGenerationExtras(options, generatedPersons);
            return;
        }
        runGenerationPhase("Simulating the command's history...",
              simulationListener -> StartingSimulation.run(getCampaign(), options, generatedPersons,
                    simulationListener),
              simulationResult -> applyPostGenerationExtras(options, generatedPersons));
    }

    /**
     * Counts the included combat units in the design model - the leaves the commit walker will
     * actually materialize. Mirrors {@code ForceDescriptorWalker}'s commit rule (a leaf is committed
     * only when it is {@link ForceDescriptor#isIncluded()} and has an {@link ForceDescriptor#getEntity()
     * entity}), so the confirmation count matches what ends up in the TOE, excluding units the player
     * struck out in the preview.
     *
     * @param descriptor the model (or subtree) to count
     *
     * @return the number of included combat-unit leaves under {@code descriptor}
     */
    private static int countCombatUnits(ForceDescriptor descriptor) {
        boolean hasChildren = (descriptor.getSubForces() != null && !descriptor.getSubForces().isEmpty())
              || (descriptor.getAttached() != null && !descriptor.getAttached().isEmpty());
        if (!hasChildren) {
            return (descriptor.isIncluded() && descriptor.getEntity() != null) ? 1 : 0;
        }
        int count = 0;
        if (descriptor.getSubForces() != null) {
            for (ForceDescriptor child : descriptor.getSubForces()) {
                count += countCombatUnits(child);
            }
        }
        if (descriptor.getAttached() != null) {
            for (ForceDescriptor child : descriptor.getAttached()) {
                count += countCombatUnits(child);
            }
        }
        return count;
    }

    /**
     * The commit confirmation - the single point at which the design model becomes part of the
     * campaign. States what will be built ({@code combatUnitCount} combat units plus generated
     * support) and appends any applicable resource-cost warnings, then offers a Build / Cancel
     * choice.
     *
     * @param options         the generation options driving the build
     * @param chosenEchelon   the model's top echelon, used to size the resource-cost warnings, or
     *                        {@code null} if unknown
     * @param combatUnitCount the number of combat units that will be committed
     *
     * @return {@code true} if the player chose Build, {@code false} if they cancelled
     */
    private boolean confirmBuildCommand(CommandGenerationOptions options, Integer chosenEchelon,
          int combatUnitCount) {
        StringBuilder message = new StringBuilder(
              MessageFormat.format(resources.getString("CommandGenerationDialog.confirmBuild.text"),
                    combatUnitCount));

        List<String> warnings = collectPreGenerationWarnings(options, chosenEchelon);
        if (!warnings.isEmpty()) {
            message.append("\n\n").append(String.join("\n\n", warnings));
        }

        Object[] buttonLabels = { resources.getString("CommandGenerationDialog.confirmBuild.build"),
                                   resources.getString("Cancel.text") };
        int choice = JOptionPane.showOptionDialog(getFrame(),
              message.toString(),
              resources.getString("CommandGenerationDialog.confirmBuild.title"),
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              buttonLabels,
              buttonLabels[0]);
        boolean build = (choice == 0);
        if (!build) {
            LOGGER.info("[CompanyGen][Worker] user cancelled at build confirmation "
                        + "(combatUnits={}, echelon={}, astechsAsPersonnel={}, medicsAsPersonnel={})",
                  combatUnitCount, chosenEchelon, options.isAstechsAsPersonnel(), options.isMedicsAsPersonnel());
        }
        return build;
    }

    /**
     * Runs one generation phase on a background thread behind a modal progress dialog. Sets the
     * {@code bulkGenerationInProgress} guard for the duration (so event-driven tab refreshes do not read
     * the half-built campaign off the EDT), runs {@code work} with the phase's progress listener, and
     * hands the result to {@code onSuccess} on the EDT. On failure a notification is shown and
     * {@code onSuccess} is skipped.
     *
     * @param progressMessage the initial progress message
     * @param work            the background work, given the progress listener
     * @param onSuccess       EDT callback receiving the work's result
     * @param <T>             the phase result type
     */
    private <T> void runGenerationPhase(String progressMessage,
          Function<Ruleset.ProgressListener, T> work, Consumer<T> onSuccess) {
        GenerationProgressDialog progressDialog = new GenerationProgressDialog(getFrame());
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() {
                progressDialog.asListener().updateProgress(0.0, progressMessage);
                getCampaign().setBulkGenerationInProgress(true);
                try {
                    return work.apply(progressDialog.asListener());
                } finally {
                    getCampaign().setBulkGenerationInProgress(false);
                }
            }

            @Override
            protected void done() {
                progressDialog.finish();
                T result;
                try {
                    result = get();
                } catch (Exception exception) {
                    // SwingWorker wraps the real failure in an ExecutionException whose own stack is
                    // just the EDT plumbing, so logging it alone says nothing about where generation
                    // actually broke. Log the cause separately to get the failing frames.
                    Throwable cause = (exception instanceof ExecutionException) ? exception.getCause() : exception;
                    LOGGER.error(cause, "[CompanyGen][Worker] generation phase failed");
                    new ImmersiveDialogNotification(campaign,
                          "Force generation failed: "
                                + ((cause == null) ? exception.getMessage() : cause.toString()), true);
                    return;
                }
                onSuccess.accept(result);
            }
        };
        worker.execute();
        // Modal dialog blocks the EDT until the worker's done() calls finish().
        progressDialog.setVisible(true);
    }

    /**
     * Collects the resource-cost warnings that apply to the current options, for the build
     * confirmation to display. Two kinds of warning can apply:
     *
     * <ul>
     *   <li><b>Long generation</b> — echelon ≥ 7 (Brigade / Galaxy / Level V or higher). The
     *       ratgen engine takes minutes at these scales; the user might have picked an SLDF Army
     *       by accident while testing.</li>
     *   <li><b>High Person count</b> — echelon ≥ 6 (Regiment / Cluster / Level IV+) with astech
     *       or medic Person mode on. A regiment-sized force at full coverage produces hundreds
     *       of named astechs and medics, which is real (and intentional) but worth flagging
     *       since it bloats the Personnel list and slows generation noticeably.</li>
     * </ul>
     *
     * @return the applicable warning lines, empty if none apply
     */
    private List<String> collectPreGenerationWarnings(CommandGenerationOptions options, Integer chosenEchelon) {
        List<String> warnings = new ArrayList<>();

        if (chosenEchelon != null && chosenEchelon >= 7) {
            warnings.add("Large force: estimated generation time "
                  + estimateGenerationDuration(chosenEchelon) + ".");
        }

        if (chosenEchelon != null && chosenEchelon >= 6) {
            boolean astechPersons = options.isGenerateAstechs() && options.isAstechsAsPersonnel();
            boolean medicPersons = options.isGenerateMedics() && options.isMedicsAsPersonnel();
            if (astechPersons || medicPersons) {
                String who = astechPersons && medicPersons ? "astechs and medics"
                      : (astechPersons ? "astechs" : "medics");
                warnings.add("At this echelon, generating " + who + " as individual personnel can "
                      + "create hundreds of Persons (6 astechs per tech, 4 medics per doctor). "
                      + "Pool mode is faster and keeps the Personnel list manageable.");
            }
        }

        return warnings;
    }

    /**
     * Returns a human-readable duration estimate for generating a force at the given ratgen echelon.
     * Numbers come from empirical observation of generation runs on Phase 1 hardware; the engine's
     * processRoot is roughly exponential in the echelon, and the per-leaf walker work is roughly
     * 50ms per unit (RATGenerator selection + Entity construction + Person creation + Formation
     * attachment). The estimate is intentionally rough — it just needs to convey "seconds" vs
     * "minutes" vs "many minutes" so the user can decide whether to proceed.
     */
    private static String estimateGenerationDuration(int echelon) {
        return switch (echelon) {
            case 7 -> "1-3 minutes (Brigade / Galaxy / Level V)";
            case 8 -> "3-8 minutes (Division / Touman / Level VI)";
            case 9 -> "10-20 minutes (Corps)";
            case 10 -> "20+ minutes (Army)";
            default -> "less than a minute";
        };
    }

    /**
     * Runs the post-generation extras (organization-changed event, auto-awards, reputation, bonus
     * units) after {@link CommandGenerator#generate} completes. Split out of {@link #okAction()} so
     * the EDT-side cleanup is the only thing the {@link SwingWorker#done()} callback has to do.
     */
    private void applyPostGenerationExtras(CommandGenerationOptions options, List<Person> generatedPersons) {
        long startNanos = System.nanoTime();
        LOGGER.info("[CompanyGen][PostGen] START (thread={}, generatedPersons={})",
              Thread.currentThread().getName(), generatedPersons.size());
        LOGGER.info("[CompanyGen][PostGen] firing OrganizationChangedEvent");
        MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(), getCampaign().getPlayerForce().getFormations()));

        if (campaign.getCampaignOptions().get(CampaignOption.ENABLE_AUTO_AWARDS)) {
            LOGGER.info("[CompanyGen][PostGen] running AutoAwardsController");
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(campaign, false);
        }

        LOGGER.info("[CompanyGen][PostGen] initializing ForceReputationController");
        ForceReputationController reputationController = new ForceReputationController();
        reputationController.initializeReputation(campaign);
        campaign.getPlayerForce().setCamOpsReputation(reputationController);

        LOGGER.info("[CompanyGen][PostGen] running processBonusUnitsBasedOnCampaignOptions");
        processBonusUnitsBasedOnCampaignOptions(generatedPersons, options);
        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        LOGGER.info("[CompanyGen][PostGen] DONE in {}ms", totalMs);
    }

    private void processBonusUnitsBasedOnCampaignOptions(List<Person> generatedPersons,
          CommandGenerationOptions options) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        // Medical reserve: a generation-only option (independent of the campaign-wide Alternative
        // Advanced Medical rule). Creates spare unassigned MekWarriors as injury replacements, sized
        // as a percentage of the generated combatants.
        if (options.isGenerateMedicalReserve()) {
            int combatants = 0;
            for (Person person : generatedPersons) {
                if (person.isCombat()) {
                    combatants++;
                }
            }

            int spares = (int) Math.ceil(combatants * options.getMedicalReservePercent() / 100.0);
            if (spares > 0) {
                new ImmersiveDialogNotification(campaign,
                      resources.getString("CommandGenerationDialog.campaignOptions.altAdvancedMedical"),
                      true);
                for (int i = 0; i < spares; i++) {
                    generateSparePersonnel(options);
                }
            }
        }

        // Support-vehicle generation (salvage, MASH, fatigue, StratCon convoy, security) is handled
        // during generation by SupportPersonnelToTOE and SupportUnitGenerator, so it is not repeated
        // here. The settings-panel confirmation dialogs still cover toggling those options later.

        if (campaignOptions.get(CampaignOption.USE_ADVANCED_SCOUTING) && campaignOptions.isUseStratCon()) {
            AdvancedScoutingCampaignOptionsChangedConfirmationDialog.processFreeSkills(campaign, true);
        }
    }

    private void generateSparePersonnel(CommandGenerationOptions options) {
        Person person = campaign.getPlayerForce().getHumanResources().newPerson(campaign, PersonnelRole.MEKWARRIOR);

        overrideSkills(campaign, person, PersonnelRole.MEKWARRIOR, SkillLevel.GREEN, true);

        SkillLevel actualSkillLevel = person.getSkillLevel(campaign, false);
        reRollLoyalty(person, actualSkillLevel);
        reRollAdvantages(campaign, person, actualSkillLevel);

        if (options.isAutomaticallyAssignRanks()) {
            final Faction faction = options.isUseSpecifiedFactionToAssignRanks()
                                          ? options.getSpecifiedFaction()
                                          : campaign.getPlayerForce().getFaction();
            person.setRank((faction.isComStarOrWoB() || faction.isClan())
                                 ? 4
                                 : 12);
        }

        campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, person, true, true);
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        // Alpha: no validation. The Force Generator panel doesn't expose an obviously-invalid state,
        // and the tabs persist or default cleanly. Validation can grow as user feedback identifies
        // genuinely invalid combinations.
        return ValidationState.SUCCESS;
    }
}
