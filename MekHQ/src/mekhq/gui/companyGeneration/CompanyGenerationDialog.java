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

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.ui.FastJScrollPane;
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
import mekhq.campaign.universe.companyGeneration.CompanyGenerationPersonTracker;
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
        final JPanel panel = new JPanel(new GridLayout(2, 2));

        setOkButton(new MMButton("btnGenerate", resources, "Generate.text",
              "CompanyGenerationDialog.btnGenerate.toolTipText", this::confirmationActionListener));
        panel.add(getOkButton());

        panel.add(new MMButton("btnApply", resources, "Apply.text",
              "CompanyGenerationDialog.btnApply.toolTipText", this::confirmationActionListener));

        panel.add(new MMButton("btnCancel", resources, "Cancel.text",
              "Cancel.toolTipText", this::cancelActionPerformed));

        panel.add(new MMButton("btnRestore", resources, "RestoreDefaults.text",
              "CompanyGenerationDialog.btnRestore.toolTipText", this::restoreDefaultsActionListener));

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

        // Run the ratgen pipeline. Stage 0 anchors year/faction from the campaign, the walker
        // materializes Units + Persons under the ToE, and Stage 8 (when wired) applies parts /
        // finance / contract polish per the snapshot above and the campaign's auto-logistics.
        CompanyGenerator.generate(getCampaign(), options);
        MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(), getCampaign().getFormations()));

        if (campaign.getCampaignOptions().isEnableAutoAwards()) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(campaign, false);
        }

        ReputationController reputationController = new ReputationController();
        reputationController.initializeReputation(campaign);
        campaign.setReputation(reputationController);

        // processBonusUnitsBasedOnCampaignOptions takes the legacy CompanyGenerationPersonTracker
        // list. The ratgen pipeline doesn't produce trackers; passing an empty list means the
        // alternative-advanced-medical spare-personnel loop won't fire. The other branches of
        // processBonusUnitsBasedOnCampaignOptions only consult the campaign, so they still work.
        // TODO: have CompanyGenerator.generate return the generated Persons so we can supply a
        // real tracker list and restore the alt-medical spare-pilot top-up.
        processBonusUnitsBasedOnCampaignOptions(Collections.emptyList(), options);
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
        // Alpha: no validation. The Force Generator panel doesn't expose an obviously-invalid state,
        // and the tabs persist or default cleanly. Validation can grow as user feedback identifies
        // genuinely invalid combinations.
        return ValidationState.SUCCESS;
    }
}
