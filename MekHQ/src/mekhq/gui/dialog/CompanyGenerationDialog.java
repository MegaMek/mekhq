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
package mekhq.gui.dialog;

import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.PersonUtility.reRollAdvantages;
import static mekhq.campaign.personnel.PersonUtility.reRollLoyalty;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView;
import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationPersonTracker;
import mekhq.campaign.universe.factionStanding.FactionStandingJudgmentType;
import mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator;
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
import mekhq.gui.panels.CompanyGenerationOptionsPanel;

/**
 * This is currently just a temporary dialog over the CompanyGenerationOptionsPanel. Wave 5 will be when this gets
 * redone to be far nicer and more customizable.
 *
 * @author Justin "Windchild" Bowen
 */
public class CompanyGenerationDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private Campaign campaign;
    private CompanyGenerationOptions companyGenerationOptions;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;
    /**
     * Embedded MegaMek Force Generator panel. Visible only when the user selects the
     * {@code RULESET_BASED} generation method, and the source of every {@code ForceDescriptor} input
     * fed into {@code CompanyGenerator.generate()} on that path. Constructed lazily inside
     * {@link #createCenterPane()} so we don't pay the cost when the user never reaches this dialog.
     */
    private ForceGeneratorOptionsView ratgenOptionsView;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CompanyGenerationDialog", "CompanyGenerationDialog.title");
        setCampaign(campaign);
        setCompanyGenerationOptions(null);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
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

    public CompanyGenerationOptionsPanel getCompanyGenerationOptionsPanel() {
        return companyGenerationOptionsPanel;
    }

    public void setCompanyGenerationOptionsPanel(final CompanyGenerationOptionsPanel companyGenerationOptionsPanel) {
        this.companyGenerationOptionsPanel = companyGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setCompanyGenerationOptionsPanel(new CompanyGenerationOptionsPanel(getFrame(), getCampaign(),
              getCompanyGenerationOptions()));

        // Build the embedded MegaMek Force Generator panel. We hide all three of its own buttons
        // (Generate / Export MUL / Clear) because the dialog's OK button drives generation. The
        // user-supplied on-generate Consumer is a no-op for the same reason. The year field is locked
        // to the campaign year — CompanyGenerator.Stage 0 anchors it regardless, but locking the field
        // keeps the displayed value honest.
        ratgenOptionsView = new ForceGeneratorOptionsView(fd -> {}, getCampaign().getGameOptions());
        ratgenOptionsView.setGenerateButtonVisible(false);
        ratgenOptionsView.setExportMULButtonVisible(false);
        ratgenOptionsView.setClearButtonVisible(false);
        ratgenOptionsView.setYearFieldEditable(false);
        ratgenOptionsView.setCurrentYear(getCampaign().getGameYear());
        ratgenOptionsView.setBorder(BorderFactory.createTitledBorder("Force Generator (Ruleset)"));

        // Method-picker strip lives at the top of the dialog so the user can always switch methods,
        // even when the legacy options panel is hidden. The picker mirrors its selection back into
        // the legacy panel's internal combo so all of the panel's own listeners and createOptionsFromPanel()
        // see the same method — this lets us hide the panel entirely on the ratgen path without losing
        // the AtB/Windchild paths.
        JPanel methodStrip = new JPanel(new FlowLayout(FlowLayout.LEFT));
        methodStrip.setBorder(BorderFactory.createTitledBorder("Generation Method"));
        methodStrip.add(new JLabel("Company Generation Method:"));
        boolean rulesetGenAvailable = Boolean.getBoolean("mekhq.companyGenerator.ruleset");
        CompanyGenerationMethod[] availableMethods = rulesetGenAvailable
              ? CompanyGenerationMethod.values()
              : Arrays.stream(CompanyGenerationMethod.values())
                    .filter(m -> !m.isRulesetBased())
                    .toArray(CompanyGenerationMethod[]::new);
        MMComboBox<CompanyGenerationMethod> dialogMethodCombo = new MMComboBox<>("dialogMethodCombo",
              availableMethods);
        Object initialPanelSelection = getCompanyGenerationOptionsPanel()
              .getComboCompanyGenerationMethod().getSelectedItem();
        if (initialPanelSelection != null) {
            dialogMethodCombo.setSelectedItem(initialPanelSelection);
        }
        methodStrip.add(dialogMethodCombo);

        // Vertical stack: method strip, then legacy panel, then ratgen panel. Only one of the two
        // options panels is visible at a time.
        JPanel host = new JPanel();
        host.setLayout(new BoxLayout(host, BoxLayout.Y_AXIS));
        host.add(methodStrip);
        host.add(getCompanyGenerationOptionsPanel());
        host.add(ratgenOptionsView);

        Runnable applyVisibility = () -> {
            Object sel = dialogMethodCombo.getSelectedItem();
            boolean ruleset = (sel instanceof CompanyGenerationMethod m) && m.isRulesetBased();
            getCompanyGenerationOptionsPanel().setVisible(!ruleset);
            ratgenOptionsView.setVisible(ruleset);
            host.revalidate();
            host.repaint();
        };

        dialogMethodCombo.addActionListener(evt -> {
            // Mirror selection into the legacy panel so its createOptionsFromPanel() reports the
            // same method, then refresh which sub-panel is visible.
            getCompanyGenerationOptionsPanel().getComboCompanyGenerationMethod()
                  .setSelectedItem(dialogMethodCombo.getSelectedItem());
            applyVisibility.run();
        });

        applyVisibility.run();
        return new FastJScrollPane(host);
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(2, 3));

        setOkButton(new MMButton("btnGenerate", resources, "Generate.text",
              "CompanyGenerationDialog.btnGenerate.toolTipText", this::confirmationActionListener));
        panel.add(getOkButton());

        panel.add(new MMButton("btnApply", resources, "Apply.text",
              "CompanyGenerationDialog.btnApply.toolTipText", this::confirmationActionListener));

        panel.add(new MMButton("btnCancel", resources, "Cancel.text",
              "Cancel.toolTipText", this::cancelActionPerformed));

        panel.add(new MMButton("btnRestore", resources, "RestoreDefaults.text",
              "CompanyGenerationDialog.btnRestore.toolTipText",
              evt -> getCompanyGenerationOptionsPanel().setOptions()));

        panel.add(new MMButton("btnImport", resources, "Import.text",
              "CompanyGenerationDialog.btnImport.toolTipText",
              evt -> getCompanyGenerationOptionsPanel().importOptionsFromXML()));

        panel.add(new MMButton("btnExport", resources, "Export.text",
              "CompanyGenerationDialog.btnExport.toolTipText",
              evt -> getCompanyGenerationOptionsPanel().exportOptionsToXML()));

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
    //endregion Initialization

    @Override
    protected void okAction() {
        final CompanyGenerationOptions options = getCompanyGenerationOptionsPanel().createOptionsFromPanel();

        // Dev-gated branch: route through the ratgen pipeline instead of the legacy strategy.
        // Phase 1 only generates units + Formations + crews; parts / finance / contract polish stay
        // on the legacy path until those helpers are extracted into the polish package.
        if (options.getMethod().isRulesetBased()) {
            // Pull the user's selections from the embedded Force Generator panel into the snapshot.
            // The panel's year field is locked to the campaign year, and CompanyGenerator.generate's
            // Stage 0 will re-anchor year from the campaign regardless, so the snapshot ends up with
            // the canonical campaign year either way.
            if (ratgenOptionsView != null) {
                ForceDescriptor fd = ratgenOptionsView.buildForceDescriptor();
                options.getForceDescriptorSnapshot().populateFromForceDescriptor(fd);
            }
            mekhq.campaign.universe.companyGeneration.ratgen.CompanyGenerator.generate(getCampaign(), options);
            MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(),
                  getCompanyGenerationOptionsPanel().getCampaign().getFormations()));
            return;
        }

        final AbstractCompanyGenerator generator = options.getMethod().getGenerator(getCampaign(), options);

        final List<CompanyGenerationPersonTracker> trackers = generator.generatePersonnel(getCampaign());
        generator.generateUnitGenerationParameters(trackers);
        generator.generateEntities(getCampaign(), trackers);
        final List<Unit> units = generator.applyPhaseOneToCampaign(getCampaign(), trackers);

        final List<Entity> mothballedEntities = generator.generateMothballedEntities(getCampaign(), trackers);
        final List<Part> parts = generator.generateSpareParts(units);
        final List<Armor> armour = generator.generateArmour(units);
        final List<AmmoStorage> ammunition = generator.generateAmmunition(getCampaign(), units);
        units.addAll(generator.applyPhaseTwoToCampaign(getCampaign(), mothballedEntities, parts, armour, ammunition));

        generator.applyPhaseThreeToCampaign(getCampaign(), trackers, units, parts, armour, ammunition, null);

        MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(),
              getCompanyGenerationOptionsPanel().getCampaign().getFormations()));

        if (campaign.getCampaignOptions().isEnableAutoAwards()) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(campaign, false);
        }

        ReputationController reputationController = new ReputationController();
        reputationController.initializeReputation(campaign);
        campaign.setReputation(reputationController);

        processBonusUnitsBasedOnCampaignOptions(trackers, options);
    }

    private void processBonusUnitsBasedOnCampaignOptions(List<CompanyGenerationPersonTracker> trackers,
          CompanyGenerationOptions options) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.isUseAlternativeAdvancedMedical()) {
            int combatants = 0;
            for (CompanyGenerationPersonTracker tracker : trackers) {
                if (tracker.getPersonType().isCombat()) {
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
        return getCompanyGenerationOptionsPanel().validateOptions(display);
    }
}
