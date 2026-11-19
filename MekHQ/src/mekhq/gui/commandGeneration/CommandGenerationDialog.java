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

import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.enums.ValidationState;
import megamek.client.ui.util.UIUtil;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CommandBuilder;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.StartingSimulation;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;
import mekhq.campaign.universe.commandGeneration.ratgen.RulesetEngineBootstrap;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.commandGeneration.designer.DesignerPage;
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
    /** The settings {@link #reviewOptions()} read for the refresh under way, or {@code null} between refreshes. */
    private @Nullable CommandGenerationOptions reviewOptionsCache;

    /**
     * Whether the last press of Accept &amp; Build actually started a build.
     *
     * <p>Set once the combat phase has put a force in the TOE, which is the point the build becomes real. Every
     * way of not getting there leaves it {@code false}: the tab settings cannot be collected, no force has been
     * previewed yet, the player cancels at the build confirmation, the phase throws, or it returns no result.
     * None of those should close the designer or greet the player with a command they have not built, so the
     * flag lets {@link #okButtonActionPerformed} and {@link #confirmationActionListener} tell a real commit from
     * everything else.</p>
     *
     * <p>A later phase failing does not clear it. By then the combat force is committed to the campaign, so the
     * designer has nothing left to go back to.</p>
     */
    private boolean buildStarted;

    /**
     * The campaign's own Random Origin options, held while a build runs with the Setup tab's in their place, or
     * {@code null} when no build is holding them.
     */
    private @Nullable RandomOriginOptions campaignRandomOriginOptions;

    private Campaign campaign;
    private CommandGenerationOptions commandGenerationOptions;
    private CommandGenerationPane pane;
    private final JLabel footerSummary = new JLabel();
    private final HudButton buildButton = new HudButton("", true);

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

        // Populate every page from the supplied options on first show.
        pane.loadValuesFromOptions(startingOptions);
        pane.getEngine().addForceGeneratedListener(this::rememberSettingsAtGenerate);
        pane.setReviewFacts(this::reviewWarnings, this::settingsChangedSinceRoll);

        // Settings that follow from the faction are applied after the pages are populated, not before.
        // Loading the options sets the naming method directly, so a Clan's Greek naming would
        // otherwise be applied while the pane is built and then overwritten a moment later.
        pane.applyFactionDrivenDefaults();
        return pane;
    }

    @Override
    protected JPanel createButtonPanel() {
        // The footer of the heads-up display: what the roll adds up to on the left, the dialog's actions on the
        // right. Build is the one action that changes the campaign, and only from the Review page; elsewhere the
        // same button takes the player there.
        JPanel footer = new JPanel(new BorderLayout(UIUtil.scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(HudStyle.SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, HudStyle.BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10), UIUtil.scaleForGUI(18), UIUtil.scaleForGUI(10),
                    UIUtil.scaleForGUI(18))));

        footerSummary.setForeground(HudStyle.TEXT_MUTED);
        footerSummary.setFont(HudStyle.hudFont(Font.PLAIN, 0.9f, 0.0f));
        footer.add(footerSummary, BorderLayout.CENTER);

        HudButton restore = new HudButton(upper(resources.getString("RestoreDefaults.text")), false, true);
        restore.setToolTipText(resources.getString("CommandGenerationDialog.btnRestore.toolTipText"));
        restore.addActionListener(this::restoreDefaultsActionListener);
        HudButton close = new HudButton(upper(resources.getString("CommandGenerationDialog.btnCancel.text")), false);
        close.setToolTipText(resources.getString("CommandGenerationDialog.btnCancel.toolTipText"));
        close.addActionListener(this::cancelActionPerformed);
        buildButton.addActionListener(this::buildButtonPressed);

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(restore);
        buttons.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(16)));
        buttons.add(close);
        buttons.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
        buttons.add(buildButton);
        footer.add(buttons, BorderLayout.EAST);

        pane.addRefreshListener(this::refreshFooter);
        pane.addPageListener(page -> refreshFooter());
        refreshFooter();
        return footer;
    }

    /** Brings the footer's summary and Build button up to date with the roll and the page on show. */
    private void refreshFooter() {
        footerSummary.setText(pane.getFooterSummary());
        boolean onReview = pane.getCurrentPage() == DesignerPage.REVIEW;
        buildButton.setText(upper(resources.getString(onReview ? "CommandGenerationDialog.btnAccept.text"
              : "CommandGenerationDialog.btnReview.text")));
        buildButton.setToolTipText(resources.getString(onReview ? "CommandGenerationDialog.btnAccept.toolTipText"
              : "CommandGenerationDialog.btnReview.toolTipText"));
        buildButton.setArmed(!onReview || pane.canBuild());
    }

    /**
     * Takes the player to the Review page, or builds the command from it. Review lists what the build will do and
     * anything worth a second look, so it is the commit gate.
     */
    private void buildButtonPressed(final ActionEvent evt) {
        if (pane.getCurrentPage() != DesignerPage.REVIEW) {
            pane.showPage(DesignerPage.REVIEW);
            return;
        }
        confirmationActionListener(evt);
    }

    /**
     * Installs the guide drawer once the dialog is assembled, so Escape closes an open guide before it closes the
     * dialog.
     */
    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        pane.installGuide(getRootPane(), () -> cancelActionPerformed(null));
        // The first page's guide waits for the dialog to be on screen, as the drawer floats over it.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                pane.maybeOpenGuide();
            }
        });
    }

    private static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }


    /**
     * Confirms and closes the designer only when {@link #okAction()} actually started a build.
     *
     * <p>The inherited behaviour marks the dialog confirmed and hides it unconditionally, because {@code okAction}
     * returns {@code void} and cannot refuse. That turned every way of backing out - cancelling the build
     * confirmation among them - into a confirmed close, so the designer vanished and the player was greeted for a
     * command that was never built.</p>
     *
     * @param evt the button event
     */
    @Override
    protected void okButtonActionPerformed(final ActionEvent evt) {
        okAction();
        if (!buildStarted) {
            LOGGER.info("[CompanyGen] Accept pressed but no build started; leaving the designer open");
            return;
        }
        setResult(DialogResult.CONFIRMED);
        setVisible(false);
    }

    private void confirmationActionListener(final ActionEvent evt) {
        okButtonActionPerformed(evt);
        if (!buildStarted) {
            return;
        }

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
        pane.loadValuesFromOptions(defaults);
        // As on first show: restoring defaults sets the naming method directly, so the faction's own
        // convention is re-applied afterwards rather than being overwritten by it.
        pane.applyFactionDrivenDefaults();
    }

    /**
     * Overwrites {@link CommandGenerationOptions#getSpecifiedFaction()} with the current
     * campaign's faction. The CommandGenerationOptions constructor defaults specifiedFaction to a
     * global default (typically Mercenary) regardless of campaign; the command is always generated
     * for the campaign's faction, so the options are made to say so. Ranks do not come from it: they
     * follow the rank system chosen in Campaign Options.
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
        pane.writeValuesToOptions(options);
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
            pane.showPage(DesignerPage.FORCE);
            pane.getEngine().requestGenerate();
        } else {
            LOGGER.info("[CompanyGen][Dialog][Settings] build cancelled at the changed-settings prompt");
        }
        return null;
    }

    @Override
    protected void okAction() {
        buildStarted = false;
        CommandGenerationOptions options = settingsToBuildWith(collectOptionsFromTabs("okAction"));
        if (options == null) {
            return;
        }
        // The spares percentages are campaign options; they take effect now, at the commit.
        pane.writeSparesToCampaignOptions();

        // Accept commits the exact force the player previewed on the Force Generator tab (its Generate
        // button rolls the ForceDescriptor and fills the TO&E tree + Composition Summary). Require a
        // preview first so Accept never silently rolls something the player never saw.
        ForceDescriptor previewedForce = pane.getEngine().getGeneratedForce();
        if (previewedForce == null) {
            JOptionPane.showMessageDialog(getFrame(),
                  resources.getString("CommandGenerationDialog.noPreview.text"),
                  resources.getString("CommandGenerationDialog.title"),
                  JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // The Review page is the commit gate: it states what will be built and lists the resource-cost
        // warnings, and Build is only pressed from there.

        // Build the command in two background phases behind a modal progress dialog (long
        // generations would otherwise look frozen). Phase one commits the model's combat force to
        // the TOE without support; phase two sizes support to the committed combat force. Support is
        // command-creation-only, so - unlike the old interim auto-prompt MVP - it always runs as part
        // of the build rather than asking again mid-flow. The worker's done() handler runs the
        // post-gen extras on the EDT.
        LOGGER.info("[CompanyGen][Worker] okAction: starting combat-commit phase (thread={})",
              Thread.currentThread().getName());
        // Snapshot the roster, so the founder flags and callsigns reach exactly the people this
        // build hires: the support staff and the vehicle and ship crews as well as the combat crews.
        Set<UUID> preExistingPersonIds = CommandGenerator.snapshotPersonnelIds(getCampaign());
        // Everyone the build creates, from the combat crews through the starting simulation's children to the
        // post-generation spares, is given an origin from the Setup tab's Random Origin choices.
        applyBuildRandomOrigin(options);
        runGenerationPhase("Materializing combat forces...", listener -> {
            // RATGenerator init must not run on the EDT; ensureLoaded is idempotent.
            ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();
            RulesetEngineBootstrap.ensureLoaded(snapshot.getYear());
            return CommandGenerator.applyToCampaign(getCampaign(), options, previewedForce, listener, false);
        }, combatResult -> {
            if (combatResult == null) {
                LOGGER.info("[CompanyGen][Worker] combat phase produced no result; skipping support");
                restoreCampaignRandomOrigin();
                return;
            }
            // The commit point: the combat force is in the TOE, so this counts as a build even if a later
            // phase falls over. A phase that threw never reaches here, because runGenerationPhase reports the
            // failure and skips this consumer entirely.
            buildStarted = true;
            List<Person> generatedPersons = new ArrayList<>(combatResult.generatedPersons());
            runGenerationPhase("Generating support forces...",
                  supportListener -> {
                      List<Person> supportPersons = CommandGenerator.generateSupportFromToe(getCampaign(), options,
                            supportListener);
                      // Spares, cargo lift and temporary crew run once the support vehicles exist, so they
                      // are stocked, lifted and crewed like everything else.
                      CommandGenerator.completeBuild(getCampaign(), options, supportListener);
                      return supportPersons;
                  },
                  supportPersons -> {
                      generatedPersons.addAll(supportPersons);
                      // The medical reserve is part of the command as built: hired before the roster flags and
                      // the starting cash so its spares are founders and count toward the
                      // running costs the cash is measured in.
                      CommandBuilder.generateMedicalReserve(getCampaign(), options, generatedPersons);
                      List<Person> hired = CommandGenerator.personsAddedSince(getCampaign(), preExistingPersonIds);
                      CommandGenerator.applyRosterFlags(getCampaign(), options, hired);
                      CommandGenerator.processStartingCash(getCampaign(), options);
                      runStartingSimulationThen(options, hired);
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
            applyPostGenerationExtras();
            return;
        }
        runGenerationPhase("Simulating the command's history...",
              simulationListener -> StartingSimulation.run(getCampaign(), options, generatedPersons,
                    simulationListener),
              simulationResult -> applyPostGenerationExtras());
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
                    // A failed phase ends the build, so the campaign gets its own origin options back.
                    restoreCampaignRandomOrigin();
                    new ImmersiveDialogNotification(campaign,
                          "Force generation failed: "
                                + ((cause == null) ? exception.getMessage() : cause.toString()), true);
                    return;
                }
                try {
                    onSuccess.accept(result);
                } catch (RuntimeException exception) {
                    restoreCampaignRandomOrigin();
                    throw exception;
                }
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
            warnings.add(MessageFormat.format(resources.getString("CommandGenerationDialog.warning.large"),
                  estimateGenerationDuration(chosenEchelon)));
        }

        if (chosenEchelon != null && chosenEchelon >= 6) {
            boolean astechPersons = options.isGenerateAstechs() && options.isAstechsAsPersonnel();
            boolean medicPersons = options.isGenerateMedics() && options.isMedicsAsPersonnel();
            if (astechPersons || medicPersons) {
                String who = resources.getString(astechPersons && medicPersons
                      ? "CommandGenerationDialog.warning.persons.both"
                      : (astechPersons ? "CommandGenerationDialog.warning.persons.astechs"
                              : "CommandGenerationDialog.warning.persons.medics"));
                warnings.add(MessageFormat.format(resources.getString("CommandGenerationDialog.warning.persons"),
                      who));
            }
        }

        return warnings;
    }

    /**
     * Returns a human-readable duration estimate for generating a force at the given ratgen echelon.
     * Numbers come from empirical observation of generation runs; the engine's processRoot is roughly
     * exponential in the echelon, and the per-leaf walker work is roughly 50ms per unit. The estimate is
     * intentionally rough - it just needs to convey "seconds" vs "minutes" vs "many minutes" so the player
     * can decide whether to proceed.
     */
    private String estimateGenerationDuration(int echelon) {
        String key = switch (echelon) {
            case 7, 8, 9, 10 -> "CommandGenerationDialog.warning.duration." + echelon;
            default -> "CommandGenerationDialog.warning.duration.short";
        };
        return resources.getString(key);
    }

    /**
     * @return the build's resource-cost warnings for the settings as the pages stand, for the Review page
     */
    private List<String> reviewWarnings() {
        CommandGenerationOptions options = reviewOptions();
        return collectPreGenerationWarnings(options, options.getForceDescriptorSnapshot().getEchelon());
    }

    /**
     * @return {@code true} if the settings differ from those the roll was made with, so the build would ask which to
     *       use
     */
    private boolean settingsChangedSinceRoll() {
        return (settingsAtLastGenerate != null) && !settingsAtLastGenerate.equals(reviewOptions());
    }

    /**
     * The settings as the pages stand, for Review. Review asks for them twice in one refresh, once for its warnings and
     * once to compare with the roll, and reading every page back is the costly part, so they are read once and kept
     * until the refresh is over.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private CommandGenerationOptions reviewOptions() {
        if (reviewOptionsCache == null) {
            reviewOptionsCache = collectOptionsFromTabs("review");
            SwingUtilities.invokeLater(() -> reviewOptionsCache = null);
        }
        return reviewOptionsCache;
    }

    /**
     * Runs the post-generation extras ({@link CommandBuilder#runPostGenerationExtras}) once the build is
     * complete, then gives the campaign its own Random Origin options back. Split out of {@link #okAction()}
     * so the EDT-side cleanup is the only thing the {@link SwingWorker#done()} callback has to do.
     */
    private void applyPostGenerationExtras() {
        try {
            CommandBuilder.runPostGenerationExtras(getCampaign(), true);
        } finally {
            // The last people the build creates (bonus-unit crews, spare personnel) are made above, so this is
            // where the build's origin options give way to the campaign's again.
            restoreCampaignRandomOrigin();
        }
    }

    /**
     * Puts the build's Random Origin options in place of the campaign's for the length of the build. Every person
     * MekHQ creates takes their origin from the campaign option, so this is how the Setup tab's choices reach them
     * without becoming the campaign's setting for later recruits.
     *
     * @param options the build's options, holding the Setup tab's Random Origin choices
     */
    private void applyBuildRandomOrigin(CommandGenerationOptions options) {
        RandomOriginOptions buildOrigin = options.getRandomOriginOptions();
        if ((buildOrigin == null) || (campaignRandomOriginOptions != null)) {
            return;
        }
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        campaignRandomOriginOptions = campaignOptions.get(CampaignOption.RANDOM_ORIGIN_OPTIONS);
        campaignOptions.set(CampaignOption.RANDOM_ORIGIN_OPTIONS, buildOrigin);
        LOGGER.info("[CompanyGen][Origin] build origin applied (randomize={}, radius={}, extraRandom={})",
              buildOrigin.isRandomizeOrigin(), buildOrigin.getOriginSearchRadius(),
              buildOrigin.isExtraRandomOrigin());
    }

    /** Gives the campaign its own Random Origin options back once the build is over; safe to call twice. */
    private void restoreCampaignRandomOrigin() {
        if (campaignRandomOriginOptions == null) {
            return;
        }
        getCampaign().getCampaignOptions().set(CampaignOption.RANDOM_ORIGIN_OPTIONS, campaignRandomOriginOptions);
        campaignRandomOriginOptions = null;
        LOGGER.info("[CompanyGen][Origin] campaign origin options restored");
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        // Alpha: no validation. The Force Generator panel doesn't expose an obviously-invalid state,
        // and the tabs persist or default cleanly. Validation can grow as user feedback identifies
        // genuinely invalid combinations.
        return ValidationState.SUCCESS;
    }
}
