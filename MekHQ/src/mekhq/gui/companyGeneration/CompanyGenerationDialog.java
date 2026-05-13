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
package mekhq.gui.companyGeneration;

import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.PersonUtility.reRollAdvantages;
import static mekhq.campaign.personnel.PersonUtility.reRollLoyalty;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.ratgen.CompanyGenerator;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.campaignOptions.optionChangeDialogs.AdvancedScoutingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.campaignOptions.optionChangeDialogs.FatigueTrackingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.campaignOptions.optionChangeDialogs.MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.campaignOptions.optionChangeDialogs.PrisonerTrackingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.campaignOptions.optionChangeDialogs.SalvageCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.campaignOptions.optionChangeDialogs.StratConConvoyCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;

/**
 * Top-level dialog for the Company Generation pipeline. Hosts a {@link CompanyGenerationPane} with
 * four tabs (Setup, Force Generator, Spares, Other) and runs the ratgen pipeline on OK.
 *
 * <p>This dialog used to wrap a monolithic {@code CompanyGenerationOptionsPanel} that exposed
 * AtB / Windchild method pickers alongside the ratgen path. With the deletion of those legacy
 * generators, the dialog runs the ratgen pipeline unconditionally: the four tabs persist user
 * preferences into {@link CompanyGenerationOptions} and the campaign's auto-logistics percentages,
 * then {@link CompanyGenerator#generate(Campaign, CompanyGenerationOptions)} performs the actual
 * generation.</p>
 *
 * @author Justin "Windchild" Bowen (original)
 */
public class CompanyGenerationDialog extends AbstractMHQValidationButtonDialog {

    private static final MMLogger LOGGER = MMLogger.create(CompanyGenerationDialog.class);

    private Campaign campaign;
    private CompanyGenerationOptions companyGenerationOptions;
    private CompanyGenerationPane pane;

    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CompanyGenerationDialog", "CompanyGenerationDialog.title");
        setCampaign(campaign);
        setCompanyGenerationOptions(null);
        initialize();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
    }

    public @Nullable CompanyGenerationOptions getCompanyGenerationOptions() {
        return companyGenerationOptions;
    }

    public void setCompanyGenerationOptions(final @Nullable CompanyGenerationOptions companyGenerationOptions) {
        this.companyGenerationOptions = companyGenerationOptions;
    }

    public CompanyGenerationPane getPane() {
        return pane;
    }

    @Override
    protected Container createCenterPane() {
        CompanyGenerationOptions startingOptions = companyGenerationOptions != null
              ? companyGenerationOptions
              : new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        pane = new CompanyGenerationPane(getFrame(), getCampaign(), startingOptions);

        // Populate every tab from the supplied options on first show.
        pane.getSetupTab().loadValuesFromOptions(startingOptions);
        pane.getForceGeneratorTab().loadValuesFromOptions(startingOptions);
        pane.getSparesTab().loadValuesFromOptions(startingOptions);
        pane.getOtherTab().loadValuesFromOptions(startingOptions);

        return new FastJScrollPane(pane);
    }

    @Override
    protected JPanel createButtonPanel() {
        // Standard dialog button arrangement: destructive / reset actions on the far left,
        // primary actions on the right with the default Generate button at the far right so
        // Enter triggers it. Replaces the previous 2x2 grid where Cancel sat diagonally
        // opposite Generate — that layout was awkward to scan.
        final JPanel panel = new JPanel(new BorderLayout());

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftButtons.add(new MMButton("btnRestore", resources, "RestoreDefaults.text",
              "CompanyGenerationDialog.btnRestore.toolTipText", this::restoreDefaultsActionListener));
        panel.add(leftButtons, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.add(new MMButton("btnCancel", resources, "Cancel.text",
              "Cancel.toolTipText", this::cancelActionPerformed));
        rightButtons.add(new MMButton("btnApply", resources, "Apply.text",
              "CompanyGenerationDialog.btnApply.toolTipText", this::confirmationActionListener));
        setOkButton(new MMButton("btnGenerate", resources, "Generate.text",
              "CompanyGenerationDialog.btnGenerate.toolTipText", this::confirmationActionListener));
        rightButtons.add(getOkButton());
        panel.add(rightButtons, BorderLayout.EAST);

        return panel;
    }

    private void confirmationActionListener(final ActionEvent evt) {
        okButtonActionPerformed(evt);

        Faction campaignFaction = campaign.getFaction();
        String campaignFactionCode = campaignFaction.getShortName();
        if (campaignFactionCode.equals(MERCENARY_FACTION_CODE)) {
            final boolean IS_STARTUP = true;
            final boolean IS_NEW_ORGANIZATION = true;
            campaign.checkForNewMercenaryOrganizationStartUp(IS_STARTUP, IS_NEW_ORGANIZATION);
            return;
        }

        PersonnelRole role = campaignFaction.isClan() ? PersonnelRole.MEKWARRIOR : PersonnelRole.MILITARY_LIAISON;
        Person speaker = campaign.newPerson(role, campaignFactionCode, Gender.RANDOMIZE);
        new FactionJudgmentDialog(campaign, speaker, campaign.getCommander(), "HELLO", campaignFaction,
              FactionStandingJudgmentType.WELCOME, ImmersiveDialogWidth.MEDIUM, null, null);
    }

    private void restoreDefaultsActionListener(final ActionEvent evt) {
        CompanyGenerationOptions defaults = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        pane.getSetupTab().loadValuesFromOptions(defaults);
        pane.getForceGeneratorTab().loadValuesFromOptions(defaults);
        pane.getSparesTab().loadValuesFromOptions(defaults);
        pane.getOtherTab().loadValuesFromOptions(defaults);
    }

    @Override
    protected void okAction() {
        // Build a CompanyGenerationOptions snapshot from the four tabs. The Setup / Force Generator /
        // Other tabs all round-trip through this object; the Spares tab writes to CampaignOptions
        // directly (see SparesTab.writeValuesToOptions for the rationale).
        CompanyGenerationOptions options = companyGenerationOptions != null
              ? companyGenerationOptions
              : new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        pane.getSetupTab().writeValuesToOptions(options);
        pane.getForceGeneratorTab().writeValuesToOptions(options);
        pane.getSparesTab().writeValuesToOptions(options);
        pane.getOtherTab().writeValuesToOptions(options);

        // For large forces (Brigade and above for IS, Galaxy and above for Clan, Level V+ for
        // ComStar), warn the user that generation will take a noticeable amount of time. The
        // warning lets them cancel out and pick a smaller force without committing to a multi-minute
        // wait, especially when they were testing and didn't realize they'd picked the SLDF Army.
        //
        // Plain text rather than HTML for the message: an HTML-bearing JOptionPane goes through
        // BasicHTML / BasicTextUI / DefaultCaret on dismissal. When this modal disposes and the
        // next modal (the GenerationProgressDialog) opens immediately afterward, the pending caret
        // repaint event fires against a torn-down view and throws
        // "Cannot invoke java.util.Vector.add(Object) because this.viewBuffer is null" (a
        // long-standing Swing bug). Plain text routes through BasicLabelUI instead and avoids the
        // FlowView code path entirely.
        Integer chosenEchelon = options.getForceDescriptorSnapshot().getEchelon();
        if (chosenEchelon != null && chosenEchelon >= 7) {
            String estimate = estimateGenerationDuration(chosenEchelon);
            int choice = JOptionPane.showConfirmDialog(getFrame(),
                  "You've picked a large force.\n\nEstimated generation time: " + estimate + ".\n\nContinue?",
                  "Long Generation",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                LOGGER.info("[CompanyGen][Worker] user cancelled at long-generation warning (echelon={})", chosenEchelon);
                return;
            }
        }

        // Run the ratgen pipeline on a background thread with a modal progress dialog up front so
        // the user gets feedback during long generations (Star League Defense Force Armies take
        // minutes; without a dialog the app appears frozen). The worker's done() handler runs on
        // the EDT after generation completes and fires the post-gen extras.
        GenerationProgressDialog progressDialog = new GenerationProgressDialog(getFrame());
        long okStartedNanos = System.nanoTime();
        LOGGER.info("[CompanyGen][Worker] okAction prepared SwingWorker (thread={})", Thread.currentThread().getName());

        SwingWorker<CompanyGenerator.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected CompanyGenerator.Result doInBackground() {
                long workerStartNanos = System.nanoTime();
                LOGGER.info("[CompanyGen][Worker] SwingWorker.doInBackground START (thread={})",
                      Thread.currentThread().getName());
                CompanyGenerator.Result result;
                try {
                    result = CompanyGenerator.generate(getCampaign(), options, progressDialog.asListener());
                } catch (Throwable t) {
                    LOGGER.error(t, "[CompanyGen][Worker] SwingWorker.doInBackground threw");
                    throw t;
                }
                long elapsedMs = (System.nanoTime() - workerStartNanos) / 1_000_000;
                LOGGER.info("[CompanyGen][Worker] SwingWorker.doInBackground DONE in {}ms ({} persons)",
                      elapsedMs, result.generatedPersons().size());
                return result;
            }

            @Override
            protected void done() {
                LOGGER.info("[CompanyGen][Worker] SwingWorker.done START (thread={})",
                      Thread.currentThread().getName());
                progressDialog.finish();
                CompanyGenerator.Result result;
                try {
                    // Surface any uncaught exception from the background thread.
                    result = get();
                } catch (Exception ex) {
                    LOGGER.error(ex, "Force generation failed");
                    new ImmersiveDialogNotification(campaign,
                          "Force generation failed: " + ex.getMessage(),
                          true);
                    return;
                }
                LOGGER.info("[CompanyGen][Worker] SwingWorker.done -> applyPostGenerationExtras");
                applyPostGenerationExtras(options, result.generatedPersons());
                LOGGER.info("[CompanyGen][Worker] SwingWorker.done complete");
            }
        };

        worker.execute();
        LOGGER.info("[CompanyGen][Worker] worker.execute() returned (thread={}); about to setVisible(true) on progressDialog",
              Thread.currentThread().getName());
        // Modal dialog blocks the EDT until SwingWorker.done() calls finish().
        progressDialog.setVisible(true);
        long modalElapsedMs = (System.nanoTime() - okStartedNanos) / 1_000_000;
        LOGGER.info("[CompanyGen][Worker] progressDialog.setVisible(true) returned after {}ms (modal closed, thread={})",
              modalElapsedMs, Thread.currentThread().getName());
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
     * units) after {@link CompanyGenerator#generate} completes. Split out of {@link #okAction()} so
     * the EDT-side cleanup is the only thing the {@link SwingWorker#done()} callback has to do.
     */
    private void applyPostGenerationExtras(CompanyGenerationOptions options, List<Person> generatedPersons) {
        long startNanos = System.nanoTime();
        LOGGER.info("[CompanyGen][PostGen] START (thread={}, generatedPersons={})",
              Thread.currentThread().getName(), generatedPersons.size());
        LOGGER.info("[CompanyGen][PostGen] firing OrganizationChangedEvent");
        MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(), getCampaign().getFormations()));

        if (campaign.getCampaignOptions().isEnableAutoAwards()) {
            LOGGER.info("[CompanyGen][PostGen] running AutoAwardsController");
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(campaign, false);
        }

        LOGGER.info("[CompanyGen][PostGen] initializing ReputationController");
        ReputationController reputationController = new ReputationController();
        reputationController.initializeReputation(campaign);
        campaign.setReputation(reputationController);

        LOGGER.info("[CompanyGen][PostGen] running processBonusUnitsBasedOnCampaignOptions");
        processBonusUnitsBasedOnCampaignOptions(generatedPersons, options);
        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        LOGGER.info("[CompanyGen][PostGen] DONE in {}ms", totalMs);
    }

    private void processBonusUnitsBasedOnCampaignOptions(List<Person> generatedPersons,
          CompanyGenerationOptions options) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.isUseAlternativeAdvancedMedical()) {
            int combatants = 0;
            for (Person person : generatedPersons) {
                if (person.isCombat()) {
                    combatants++;
                }
            }

            if (combatants > 0) {
                new ImmersiveDialogNotification(campaign,
                      resources.getString("CompanyGenerationDialog.campaignOptions.altAdvancedMedical"),
                      true);
                for (int i = 0; i < combatants; i++) {
                    generateSparePersonnel(options);
                }
            }
        }

        if (campaignOptions.isUseFatigue()) {
            new ImmersiveDialogNotification(campaign,
                  resources.getString("CompanyGenerationDialog.campaignOptions.fatigue"),
                  true);
            FatigueTrackingCampaignOptionsChangedConfirmationDialog.processFreeUnit(campaign);
        }

        if (campaignOptions.isUseMASHTheatres()) {
            new ImmersiveDialogNotification(campaign,
                  resources.getString("CompanyGenerationDialog.campaignOptions.mash"),
                  true);
            MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog.processFreeUnit(campaign);
        }

        if (!campaignOptions.getPrisonerCaptureStyle().isNone()) {
            new ImmersiveDialogNotification(campaign,
                  resources.getString("CompanyGenerationDialog.campaignOptions.security"),
                  true);
            PrisonerTrackingCampaignOptionsChangedConfirmationDialog.processFreeUnit(campaign);
        }

        if (campaignOptions.isUseCamOpsSalvage()) {
            new ImmersiveDialogNotification(campaign,
                  resources.getString("CompanyGenerationDialog.campaignOptions.salvage"),
                  true);
            SalvageCampaignOptionsChangedConfirmationDialog.processFreeUnits(campaign);
        }

        if (campaignOptions.isUseStratCon()) {
            new ImmersiveDialogNotification(campaign,
                  resources.getString("CompanyGenerationDialog.campaignOptions.stratCon"),
                  true);
            StratConConvoyCampaignOptionsChangedConfirmationDialog.processFreeUnits(campaign);
        }

        if (campaignOptions.isUseAdvancedScouting() && campaignOptions.isUseStratCon()) {
            AdvancedScoutingCampaignOptionsChangedConfirmationDialog.processFreeSkills(campaign, true);
        }
    }

    private void generateSparePersonnel(CompanyGenerationOptions options) {
        Person person = campaign.newPerson(PersonnelRole.MEKWARRIOR);

        RandomSkillPreferences randomSkillPreferences = campaign.getRandomSkillPreferences();
        boolean useExtraRandomness = randomSkillPreferences.randomizeSkill();

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        overrideSkills(campaignOptions.isAdminsHaveNegotiation(),
              campaignOptions.isDoctorsUseAdministration(),
              campaignOptions.isTechsUseAdministration(),
              campaignOptions.isUseArtillery(),
              useExtraRandomness,
              person,
              PersonnelRole.MEKWARRIOR,
              SkillLevel.GREEN);

        SkillLevel actualSkillLevel = person.getSkillLevel(campaign, false);
        reRollLoyalty(person, actualSkillLevel);
        reRollAdvantages(campaign, person, actualSkillLevel);

        if (options.isAutomaticallyAssignRanks()) {
            final Faction faction = options.isUseSpecifiedFactionToAssignRanks()
                                          ? options.getSpecifiedFaction()
                                          : campaign.getFaction();
            person.setRank((faction.isComStarOrWoB() || faction.isClan())
                                 ? 4
                                 : 12);
        }

        campaign.recruitPerson(person, true, true);
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        // Alpha: no validation. The Force Generator panel doesn't expose an obviously-invalid state,
        // and the tabs persist or default cleanly. Validation can grow as user feedback identifies
        // genuinely invalid combinations.
        return ValidationState.SUCCESS;
    }
}
