/*
 * Copyright (C) 2009-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.FileDialogs;
import mekhq.gui.SpecialAbilityPanel;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CampaignOptionsDialog extends AbstractMHQButtonDialog {

    //region Constructors
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog", new EncodeControl()),
                "CampaignOptionsDialog", "CampaignOptionsDialog.title");
        initialize();
        setOptions(campaign.getCampaignOptions(), campaign.getRandomSkillPreferences());
    }
    //endregion Constructors

    //region Initialization
    //region Center Pane
    @Override
    protected Container createCenterPane() {
        //region Variable Declaration and Initialisation
        GridBagConstraints gridBagConstraints;
        int gridy = 0;
        int gridx = 0;

        //endregion Variable Declaration and Initialisation


        //endregion Against the Bot Tab

        return getOptionsPane();
    }
    //endregion Center Pane

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 0));

        panel.add(new MMButton("btnOkay", resources, "btnOkay.text", null,
                this::okButtonActionPerformed));

        panel.add(new MMButton("btnSavePreset", resources, "btnSavePreset.text",
                "btnSavePreset.toolTipText", evt -> btnSaveActionPerformed()));

        panel.add(new MMButton("btnLoadPreset", resources, "btnLoadPreset.text",
                "btnLoadPreset.toolTipText", evt -> {
            final CampaignPresetSelectionDialog presetSelectionDialog = new CampaignPresetSelectionDialog(getFrame());
            if (presetSelectionDialog.showDialog().isConfirmed()) {
                applyPreset(presetSelectionDialog.getSelectedPreset());
            }
        }));

        panel.add(new MMButton("btnCancel", resources, "btnCancel.text", null,
                this::cancelActionPerformed));

        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getOptionsPane()));
    }

    public void applyPreset(final @Nullable CampaignPreset preset) {
        if (preset == null) {
            return;
        }

        if (isStartup()) {
            if (preset.getDate() != null) {
                setDate(preset.getDate());
            }

            if (preset.getFaction() != null) {
                comboFaction.setSelectedItem(new FactionDisplay(preset.getFaction(), date));
            }

            if (preset.getRankSystem() != null) {
                if (preset.getRankSystem().getType().isCampaign()) {
                    rankSystemsPane.getRankSystemModel().addElement(preset.getRankSystem());
                }
                rankSystemsPane.getComboRankSystems().setSelectedItem(preset.getRankSystem());
            }
        }

        // Handle CampaignOptions and RandomSkillPreferences
        if (preset.getCampaignOptions() != null) {
            setOptions(preset.getCampaignOptions(), preset.getRandomSkillPreferences());
        }

        // Handle SPAs
        if (!preset.getSpecialAbilities().isEmpty()) {
            tempSPA = preset.getSpecialAbilities();
            recreateSPAPanel(!getUnusedSPA().isEmpty());
        }

        if (!preset.getSkills().isEmpty()) {
            // Overwriting XP Table
            tableXP.setModel(new DefaultTableModel(getSkillCostsArray(preset.getSkills()), TABLE_XP_COLUMN_NAMES));
            ((DefaultTableModel) tableXP.getModel()).fireTableDataChanged();

            // Overwriting Skill List
            for (final String skillName : SkillType.getSkillList()) {
                final JSpinner spnTarget = hashSkillTargets.get(skillName);
                if (spnTarget == null) {
                    continue;
                }
                final SkillType skillType = preset.getSkills().get(skillName);

                spnTarget.setValue(skillType.getTarget());
                hashGreenSkill.get(skillName).setValue(skillType.getGreenLevel());
                hashRegSkill.get(skillName).setValue(skillType.getRegularLevel());
                hashVetSkill.get(skillName).setValue(skillType.getVeteranLevel());
                hashEliteSkill.get(skillName).setValue(skillType.getEliteLevel());
            }
        }
    }

    public void setOptions(@Nullable CampaignOptions options,
                           @Nullable RandomSkillPreferences randomSkillPreferences) {
        // Use the provided options and preferences when possible, but flip if they are null to be safe
        if (options != null) {
            this.options = options;
        } else {
            options = this.options;
        }

        if (randomSkillPreferences != null) {
            this.rSkillPrefs = randomSkillPreferences;
        } else {
            randomSkillPreferences = this.rSkillPrefs;
        }

        //region General Tab
        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod());
        manualUnitRatingModifier.setValue(options.getManualUnitRatingModifier());
        //endregion General Tab

        //region Repair and Maintenance Tab
        useEraModsCheckBox.setSelected(options.useEraMods());
        assignedTechFirstCheckBox.setSelected(options.useAssignedTechFirst());
        resetToFirstTechCheckBox.setSelected(options.useResetToFirstTech());
        useQuirksBox.setSelected(options.useQuirks());
        useAeroSystemHitsBox.setSelected(options.useAeroSystemHits());
        if (useDamageMargin.isSelected() != options.isDestroyByMargin()) {
            useDamageMargin.doClick();
        }
        spnDamageMargin.setValue(options.getDestroyMargin());
        spnDestroyPartTarget.setValue(options.getDestroyPartTarget());

        if (checkMaintenance.isSelected() != options.checkMaintenance()) {
            checkMaintenance.doClick();
        }
        spnMaintenanceDays.setValue(options.getMaintenanceCycleDays());
        spnMaintenanceBonus.setValue(options.getMaintenanceBonus());
        useQualityMaintenance.setSelected(options.useQualityMaintenance());
        reverseQualityNames.setSelected(options.reverseQualityNames());
        useUnofficialMaintenance.setSelected(options.useUnofficialMaintenance());
        logMaintenance.setSelected(options.logMaintenance());
        //endregion Repair and Maintenance Tab

        //region Supplies and Acquisitions Tab
        spnAcquireWaitingPeriod.setValue(options.getWaitingPeriod());
        choiceAcquireSkill.setSelectedItem(options.getAcquisitionSkill());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());
        spnAcquireClanPenalty.setValue(options.getClanAcquisitionPenalty());
        spnAcquireIsPenalty.setValue(options.getIsAcquisitionPenalty());
        txtMaxAcquisitions = new JCheckBox(Integer.toString(options.getMaxAcquisitions()));

        spnNDiceTransitTime.setValue(options.getNDiceTransitTime());
        spnConstantTransitTime.setValue(options.getConstantTransitTime());
        choiceTransitTimeUnits.setSelectedItem(CampaignOptions.getTransitUnitName(options.getUnitTransitTime()));
        spnAcquireMinimum.setValue(options.getAcquireMinimumTime());
        choiceAcquireMinimumUnit.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMinimumTimeUnit()));
        spnAcquireMosBonus.setValue(options.getAcquireMosBonus());
        choiceAcquireMosUnits.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMosUnit()));

        usePlanetaryAcquisitions.setSelected(options.usesPlanetaryAcquisition());
        spnMaxJumpPlanetaryAcquisitions.setValue(options.getMaxJumpsPlanetaryAcquisition());
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(options.getPlanetAcquisitionFactionLimit());
        disallowPlanetaryAcquisitionClanCrossover.setSelected(options.disallowPlanetAcquisitionClanCrossover());
        disallowClanPartsFromIS.setSelected(options.disallowClanPartsFromIS());
        spnPenaltyClanPartsFromIS.setValue(options.getPenaltyClanPartsFroIS());
        usePlanetaryAcquisitionsVerbose.setSelected(options.usePlanetAcquisitionVerboseReporting());
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            spnPlanetAcquireTechBonus[i].setValue(options.getPlanetTechAcquisitionBonus(i));
            spnPlanetAcquireIndustryBonus[i].setValue(options.getPlanetIndustryAcquisitionBonus(i));
            spnPlanetAcquireOutputBonus[i].setValue(options.getPlanetOutputAcquisitionBonus(i));
        }
        //endregion Supplies and Acquisitions Tab

        //region Tech Limits Tab
        if (limitByYearBox.isSelected() != options.limitByYear()) {
            limitByYearBox.doClick();
        }
        disallowExtinctStuffBox.setSelected(options.disallowExtinctStuff());
        allowClanPurchasesBox.setSelected(options.allowClanPurchases());
        allowISPurchasesBox.setSelected(options.allowISPurchases());
        allowCanonOnlyBox.setSelected(options.allowCanonOnly());
        allowCanonRefitOnlyBox.setSelected(options.allowCanonRefitOnly());
        choiceTechLevel.setSelectedIndex(options.getTechLevel());
        variableTechLevelBox.setSelected(options.useVariableTechLevel() && options.limitByYear());
        factionIntroDateBox.setSelected(options.useFactionIntroDate());
        useAmmoByTypeBox.setSelected(options.useAmmoByType());
        //endregion Tech Limits Tab

        //region Personnel Tab
        // General Personnel
        chkUseTactics.setSelected(options.useTactics());
        chkUseInitiativeBonus.setSelected(options.useInitiativeBonus());
        chkUseToughness.setSelected(options.useToughness());
        chkUseArtillery.setSelected(options.useArtillery());
        chkUseAbilities.setSelected(options.useAbilities());
        if (chkUseEdge.isSelected() != options.useEdge()) {
            chkUseEdge.doClick();
        }
        chkUseSupportEdge.setSelected(options.useSupportEdge());
        chkUseImplants.setSelected(options.useImplants());
        chkUseAlternativeQualityAveraging.setSelected(options.useAlternativeQualityAveraging());
        chkUseTransfers.setSelected(options.useTransfers());
        chkUseExtendedTOEForceName.setSelected(options.isUseExtendedTOEForceName());
        chkPersonnelLogSkillGain.setSelected(options.isPersonnelLogSkillGain());
        chkPersonnelLogAbilityGain.setSelected(options.isPersonnelLogAbilityGain());
        chkPersonnelLogEdgeGain.setSelected(options.isPersonnelLogEdgeGain());

        // Expanded Personnel Information
        if (chkUseTimeInService.isSelected() != options.getUseTimeInService()) {
            chkUseTimeInService.doClick();
        }
        comboTimeInServiceDisplayFormat.setSelectedItem(options.getTimeInServiceDisplayFormat());
        if (chkUseTimeInRank.isSelected() != options.getUseTimeInRank()) {
            chkUseTimeInRank.doClick();
        }
        comboTimeInRankDisplayFormat.setSelectedItem(options.getTimeInRankDisplayFormat());
        chkUseRetirementDateTracking.setSelected(options.useRetirementDateTracking());
        chkTrackTotalEarnings.setSelected(options.isTrackTotalEarnings());
        chkTrackTotalXPEarnings.setSelected(options.isTrackTotalXPEarnings());
        chkShowOriginFaction.setSelected(options.showOriginFaction());

        // Medical
        chkUseAdvancedMedical.setSelected(options.useAdvancedMedical());
        spnHealWaitingPeriod.setValue(options.getHealingWaitingPeriod());
        spnNaturalHealWaitingPeriod.setValue(options.getNaturalHealingWaitingPeriod());
        spnMinimumHitsForVehicles.setValue(options.getMinimumHitsForVehicles());
        chkUseRandomHitsForVehicles.setSelected(options.useRandomHitsForVehicles());
        chkUseTougherHealing.setSelected(options.useTougherHealing());

        // Prisoners
        comboPrisonerCaptureStyle.setSelectedItem(options.getPrisonerCaptureStyle());
        comboPrisonerStatus.setSelectedItem(options.getDefaultPrisonerStatus());
        chkPrisonerBabyStatus.setSelected(options.getPrisonerBabyStatus());
        chkAtBPrisonerDefection.setSelected(options.useAtBPrisonerDefection());
        chkAtBPrisonerRansom.setSelected(options.useAtBPrisonerRansom());

        // Personnel Randomization
        chkUseDylansRandomXP.setSelected(options.useDylansRandomXP());
        if (chkRandomizeOrigin.isSelected() != options.randomizeOrigin()) {
            chkRandomizeOrigin.doClick();
        }
        chkRandomizeDependentsOrigin.setSelected(options.getRandomizeDependentOrigin());
        spnOriginSearchRadius.setValue(options.getOriginSearchRadius());
        chkExtraRandomOrigin.setSelected(options.extraRandomOrigin());
        spnOriginDistanceScale.setValue(options.getOriginDistanceScale());

        // Family
        comboDisplayFamilyLevel.setSelectedItem(options.getDisplayFamilyLevel());

        // Salary
        spnCommissionedSalary.setValue(options.getSalaryCommissionMultiplier());
        spnEnlistedSalary.setValue(options.getSalaryEnlistedMultiplier());
        spnAntiMekSalary.setValue(options.getSalaryAntiMekMultiplier());
        spnSpecialistInfantrySalary.setValue(options.getSalarySpecialistInfantryMultiplier());
        for (int i = 0; i < spnSalaryExperienceMultipliers.length; i++) {
            spnSalaryExperienceMultipliers[i].setValue(options.getSalaryXPMultiplier(i));
        }
        for (int i = 0; i < spnBaseSalary.length; i++) {
            spnBaseSalary[i].setValue(options.getRoleBaseSalaries()[i].getAmount().doubleValue());
        }

        // Marriage
        chkUseManualMarriages.setSelected(options.isUseManualMarriages());
        chkUseClannerMarriages.setSelected(options.isUseClannerMarriages());
        chkUsePrisonerMarriages.setSelected(options.isUsePrisonerMarriages());
        spnMinimumMarriageAge.setValue(options.getMinimumMarriageAge());
        spnCheckMutualAncestorsDepth.setValue(options.getCheckMutualAncestorsDepth());
        chkLogMarriageNameChanges.setSelected(options.isLogMarriageNameChanges());
        for (final Map.Entry<MergingSurnameStyle, JSpinner> entry : spnMarriageSurnameWeights.entrySet()) {
            entry.getValue().setValue(options.getMarriageSurnameWeights().get(entry.getKey()) / 10.0);
        }
        comboRandomMarriageMethod.setSelectedItem(options.getRandomMarriageMethod());
        if (chkUseRandomSameSexMarriages.isSelected() != options.isUseRandomSameSexMarriages()) {
            if (chkUseRandomSameSexMarriages.isEnabled()) {
                chkUseRandomSameSexMarriages.doClick();
            } else {
                chkUseRandomSameSexMarriages.setSelected(options.isUseRandomSameSexMarriages());
            }
        }
        chkUseRandomClannerMarriages.setSelected(options.isUseRandomClannerMarriages());
        chkUseRandomPrisonerMarriages.setSelected(options.isUseRandomPrisonerMarriages());
        spnRandomMarriageAgeRange.setValue(options.getRandomMarriageAgeRange());
        spnPercentageRandomMarriageOppositeSexChance.setValue(options.getPercentageRandomMarriageOppositeSexChance() * 100.0);
        spnPercentageRandomMarriageSameSexChance.setValue(options.getPercentageRandomMarriageSameSexChance() * 100.0);

        // Divorce
        chkUseManualDivorce.setSelected(options.isUseManualDivorce());
        chkUseClannerDivorce.setSelected(options.isUseClannerDivorce());
        chkUsePrisonerDivorce.setSelected(options.isUsePrisonerDivorce());
        for (final Map.Entry<SplittingSurnameStyle, JSpinner> entry : spnDivorceSurnameWeights.entrySet()) {
            entry.getValue().setValue(options.getDivorceSurnameWeights().get(entry.getKey()) / 10.0);
        }
        comboRandomDivorceMethod.setSelectedItem(options.getRandomDivorceMethod());
        if (chkUseRandomOppositeSexDivorce.isSelected() != options.isUseRandomOppositeSexDivorce()) {
            if (chkUseRandomOppositeSexDivorce.isEnabled()) {
                chkUseRandomOppositeSexDivorce.doClick();
            } else {
                chkUseRandomOppositeSexDivorce.setSelected(options.isUseRandomOppositeSexDivorce());
            }
        }

        if (chkUseRandomSameSexDivorce.isSelected() != options.isUseRandomSameSexDivorce()) {
            if (chkUseRandomSameSexDivorce.isEnabled()) {
                chkUseRandomSameSexDivorce.doClick();
            } else {
                chkUseRandomSameSexDivorce.setSelected(options.isUseRandomSameSexDivorce());
            }
        }
        chkUseRandomClannerDivorce.setSelected(options.isUseRandomClannerDivorce());
        chkUseRandomPrisonerDivorce.setSelected(options.isUseRandomPrisonerDivorce());
        spnPercentageRandomDivorceOppositeSexChance.setValue(options.getPercentageRandomDivorceOppositeSexChance() * 100.0);
        spnPercentageRandomDivorceSameSexChance.setValue(options.getPercentageRandomDivorceSameSexChance() * 100.0);

        // Procreation
        chkUseManualProcreation.setSelected(options.isUseManualProcreation());
        chkUseClannerProcreation.setSelected(options.isUseClannerProcreation());
        chkUsePrisonerProcreation.setSelected(options.isUsePrisonerProcreation());
        spnMultiplePregnancyOccurrences.setValue(options.getMultiplePregnancyOccurrences());
        comboBabySurnameStyle.setSelectedItem(options.getBabySurnameStyle());
        chkAssignNonPrisonerBabiesFounderTag.setSelected(options.isAssignNonPrisonerBabiesFounderTag());
        chkAssignChildrenOfFoundersFounderTag.setSelected(options.isAssignChildrenOfFoundersFounderTag());
        chkDetermineFatherAtBirth.setSelected(options.isDetermineFatherAtBirth());
        chkDisplayTrueDueDate.setSelected(options.isDisplayTrueDueDate());
        chkLogProcreation.setSelected(options.isLogProcreation());
        comboRandomProcreationMethod.setSelectedItem(options.getRandomProcreationMethod());
        if (chkUseRelationshiplessRandomProcreation.isSelected() != options.isUseRelationshiplessRandomProcreation()) {
            if (chkUseRelationshiplessRandomProcreation.isEnabled()) {
                chkUseRelationshiplessRandomProcreation.doClick();
            } else {
                chkUseRelationshiplessRandomProcreation.setSelected(options.isUseRelationshiplessRandomProcreation());
            }
        }
        chkUseRandomClannerProcreation.setSelected(options.isUseRandomClannerProcreation());
        chkUseRandomPrisonerProcreation.setSelected(options.isUseRandomPrisonerProcreation());
        spnPercentageRandomProcreationRelationshipChance.setValue(options.getPercentageRandomProcreationRelationshipChance() * 100.0);
        spnPercentageRandomProcreationRelationshiplessChance.setValue(options.getPercentageRandomProcreationRelationshiplessChance() * 100.0);

        // Death
        chkKeepMarriedNameUponSpouseDeath.setSelected(options.getKeepMarriedNameUponSpouseDeath());
        //endregion Personnel Tab

        //region Finances Tab
        payForPartsBox.setSelected(options.payForParts());
        payForRepairsBox.setSelected(options.payForRepairs());
        payForUnitsBox.setSelected(options.payForUnits());
        payForSalariesBox.setSelected(options.payForSalaries());
        payForOverheadBox.setSelected(options.payForOverhead());
        payForMaintainBox.setSelected(options.payForMaintain());
        payForTransportBox.setSelected(options.payForTransport());
        sellUnitsBox.setSelected(options.canSellUnits());
        sellPartsBox.setSelected(options.canSellParts());
        payForRecruitmentBox.setSelected(options.payForRecruitment());
        useLoanLimitsBox.setSelected(options.useLoanLimits());
        usePercentageMaintBox.setSelected(options.usePercentageMaint());
        useInfantryDontCountBox.setSelected(options.useInfantryDontCount());
        usePeacetimeCostBox.setSelected(options.usePeacetimeCost());
        useExtendedPartsModifierBox.setSelected(options.useExtendedPartsModifier());
        showPeacetimeCostBox.setSelected(options.showPeacetimeCost());
        comboFinancialYearDuration.setSelectedItem(options.getFinancialYearDuration());
        newFinancialYearFinancesToCSVExportBox.setSelected(options.getNewFinancialYearFinancesToCSVExport());

        // Price Multipliers
        spnCommonPartPriceMultiplier.setValue(options.getCommonPartPriceMultiplier());
        spnInnerSphereUnitPriceMultiplier.setValue(options.getInnerSphereUnitPriceMultiplier());
        spnInnerSpherePartPriceMultiplier.setValue(options.getInnerSpherePartPriceMultiplier());
        spnClanUnitPriceMultiplier.setValue(options.getClanUnitPriceMultiplier());
        spnClanPartPriceMultiplier.setValue(options.getClanPartPriceMultiplier());
        spnMixedTechUnitPriceMultiplier.setValue(options.getMixedTechUnitPriceMultiplier());
        for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
            spnUsedPartPriceMultipliers[i].setValue(options.getUsedPartPriceMultipliers()[i]);
        }
        spnDamagedPartsValueMultiplier.setValue(options.getDamagedPartsValueMultiplier());
        spnUnrepairablePartsValueMultiplier.setValue(options.getUnrepairablePartsValueMultiplier());
        spnCancelledOrderRefundMultiplier.setValue(options.getCancelledOrderRefundMultiplier());
        //endregion Finances Tab

        //region Mercenary Tab
        if (options.useEquipmentContractBase()) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }
        spnEquipPercent.setValue(options.getEquipmentContractPercent());
        spnDropshipPercent.setValue(options.getDropshipContractPercent());
        spnJumpshipPercent.setValue(options.getJumpshipContractPercent());
        spnWarshipPercent.setValue(options.getWarshipContractPercent());
        chkEquipContractSaleValue.setSelected(options.useEquipmentContractSaleValue());
        chkBLCSaleValue.setSelected(options.useBLCSaleValue());
        chkOverageRepaymentInFinalPayment.setSelected(options.getOverageRepaymentInFinalPayment());
        //endregion Mercenary Tab

        //region Experience Tab
        spnScenarioXP.setValue(options.getScenarioXP());
        spnKillXP.setValue(options.getKillXPAward());
        spnKills.setValue(options.getKillsForXP());
        spnTaskXP.setValue(options.getTaskXP());
        spnNTasksXP.setValue(options.getNTasksXP());
        spnSuccessXP.setValue(options.getSuccessXP());
        spnMistakeXP.setValue(options.getMistakeXP());
        spnIdleXP.setValue(options.getIdleXP());
        spnMonthsIdleXP.setValue(options.getMonthsIdleXP());
        spnTargetIdleXP.setValue(options.getTargetIdleXP());
        spnContractNegotiationXP.setValue(options.getContractNegotiationXP());
        spnAdminWeeklyXP.setValue(options.getAdminXP());
        spnAdminWeeklyXPPeriod.setValue(options.getAdminXPPeriod());
        spnEdgeCost.setValue(options.getEdgeCost());
        //endregion Experience Tab

        //region Skills Tab
        //endregion Skills Tab

        //region Special Abilities Tab
        //endregion Special Abilities Tab

        //region Skill Randomization Tab
        chkExtraRandom.setSelected(randomSkillPreferences.randomizeSkill());
        final int[] phenotypeProbabilities = options.getPhenotypeProbabilities();
        for (int i = 0; i < phenotypeSpinners.length; i++) {
            phenotypeSpinners[i].setValue(phenotypeProbabilities[i]);
        }
        spnProbAntiMek.setValue(rSkillPrefs.getAntiMekProb());
        spnOverallRecruitBonus.setValue(rSkillPrefs.getOverallRecruitBonus());
        for (int i = 0; i < spnTypeRecruitBonus.length; i++) {
            spnTypeRecruitBonus[i].setValue(rSkillPrefs.getRecruitBonuses()[i]);
        }
        spnArtyProb.setValue(rSkillPrefs.getArtilleryProb());
        spnArtyBonus.setValue(rSkillPrefs.getArtilleryBonus());
        spnSecondProb.setValue(rSkillPrefs.getSecondSkillProb());
        spnSecondBonus.setValue(rSkillPrefs.getSecondSkillBonus());
        spnTacticsGreen.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_GREEN));
        spnTacticsReg.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_REGULAR));
        spnTacticsVet.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_VETERAN));
        spnTacticsElite.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_ELITE));
        spnAbilGreen.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_GREEN));
        spnAbilReg.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_REGULAR));
        spnAbilVet.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_VETERAN));
        spnAbilElite.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_ELITE));
        spnCombatSA.setValue(rSkillPrefs.getCombatSmallArmsBonus());
        spnSupportSA.setValue(rSkillPrefs.getSupportSmallArmsBonus());
        //endregion Skill Randomization Tab

        //region Rank System Tab
        //endregion Rank System Tab

        //region Name and Portrait Generation Tab
        if (chkUseOriginFactionForNames.isSelected() != options.useOriginFactionForNames()) {
            chkUseOriginFactionForNames.doClick();
        }

        boolean allSelected = true;
        boolean noneSelected = true;
        final boolean[] usePortraitForRole = options.usePortraitForRoles();
        for (int i = 0; i < chkUsePortrait.length; i++) {
            chkUsePortrait[i].setSelected(usePortraitForRole[i]);
            if (usePortraitForRole[i]) {
                noneSelected = false;
            } else {
                allSelected = false;
            }
        }
        if (allSelected != allPortraitsBox.isSelected()) {
            allPortraitsBox.doClick();
        }

        if (noneSelected != noPortraitsBox.isSelected()) {
            noPortraitsBox.doClick();
        }

        chkAssignPortraitOnRoleChange.setSelected(options.getAssignPortraitOnRoleChange());
        //endregion Name and Portrait Generation Tab

        //region Markets Tab
        comboPersonnelMarketType.setSelectedItem(options.getPersonnelMarketType());
        chkPersonnelMarketReportRefresh.setSelected(options.getPersonnelMarketReportRefresh());
        spnPersonnelMarketRandomEliteRemoval.setValue(options.getPersonnelMarketRandomEliteRemoval());
        spnPersonnelMarketRandomVeteranRemoval.setValue(options.getPersonnelMarketRandomVeteranRemoval());
        spnPersonnelMarketRandomRegularRemoval.setValue(options.getPersonnelMarketRandomRegularRemoval());
        spnPersonnelMarketRandomGreenRemoval.setValue(options.getPersonnelMarketRandomGreenRemoval());
        spnPersonnelMarketRandomUltraGreenRemoval.setValue(options.getPersonnelMarketRandomUltraGreenRemoval());
        spnPersonnelMarketDylansWeight.setValue(options.getPersonnelMarketDylansWeight());

        // Unit Market
        comboUnitMarketMethod.setSelectedItem(options.getUnitMarketMethod());
        chkUnitMarketRegionalMechVariations.setSelected(options.useUnitMarketRegionalMechVariations());
        chkInstantUnitMarketDelivery.setSelected(options.getInstantUnitMarketDelivery());
        chkUnitMarketReportRefresh.setSelected(options.getUnitMarketReportRefresh());

        // Contract Market
        comboContractMarketMethod.setSelectedItem(options.getContractMarketMethod());
        chkContractMarketReportRefresh.setSelected(options.getContractMarketReportRefresh());
        //endregion Markets Tab

        //region RATs Tab
        btnUseRATGenerator.setSelected(!options.isUseStaticRATs());
        if (options.isUseStaticRATs() != btnUseStaticRATs.isSelected()) {
            btnUseStaticRATs.doClick();
        }
        for (final String rat : options.getRATs()) {
            final List<Integer> eras = RATManager.getAllRATCollections().get(rat);
            if (eras != null) {
                final StringBuilder displayName = new StringBuilder(rat);
                if (!eras.isEmpty()) {
                    displayName.append(" (").append(eras.get(0));
                    if (eras.size() > 1) {
                        displayName.append("-").append(eras.get(eras.size() - 1));
                    }
                    displayName.append(")");
                }

                if (availableRATModel.contains(displayName.toString())) {
                    chosenRATModel.addElement(displayName.toString());
                    availableRATModel.removeElement(displayName.toString());
                }
            }
        }
        chkIgnoreRATEra.setSelected(options.isIgnoreRATEra());
        //endregion RATs Tab

        //region Against the Bot Tab
        if (chkUseAtB.isSelected() != options.getUseAtB()) {
            chkUseAtB.doClick();
        }
        chkUseStratCon.setSelected(options.getUseStratCon());
        cbSkillLevel.setSelectedIndex(options.getSkillLevel());

        chkUseShareSystem.setSelected(options.getUseShareSystem());
        chkSharesExcludeLargeCraft.setSelected(options.getSharesExcludeLargeCraft());
        chkSharesForAll.setSelected(options.getSharesForAll());
        chkAeroRecruitsHaveUnits.setSelected(options.getAeroRecruitsHaveUnits());
        chkRetirementRolls.setSelected(options.doRetirementRolls());
        chkCustomRetirementMods.setSelected(options.getCustomRetirementMods());
        chkFoundersNeverRetire.setSelected(options.getFoundersNeverRetire());
        chkAddDependents.setSelected(options.canAtBAddDependents());
        chkDependentsNeverLeave.setSelected(options.getDependentsNeverLeave());
        chkTrackUnitFatigue.setSelected(options.getTrackUnitFatigue());
        chkUseLeadership.setSelected(options.getUseLeadership());
        chkTrackOriginalUnit.setSelected(options.getTrackOriginalUnit());
        chkUseAero.setSelected(options.getUseAero());
        chkUseVehicles.setSelected(options.getUseVehicles());
        chkClanVehicles.setSelected(options.getClanVehicles());

        spnSearchRadius.setValue(options.getSearchRadius());
        chkVariableContractLength.setSelected(options.getVariableContractLength());
        chkMercSizeLimited.setSelected(options.isMercSizeLimited());
        chkRestrictPartsByMission.setSelected(options.getRestrictPartsByMission());
        chkLimitLanceWeight.setSelected(options.getLimitLanceWeight());
        chkLimitLanceNumUnits.setSelected(options.getLimitLanceNumUnits());
        chkUseStrategy.setSelected(options.getUseStrategy());
        spnBaseStrategyDeployment.setValue(options.getBaseStrategyDeployment());
        spnAdditionalStrategyDeployment.setValue(options.getAdditionalStrategyDeployment());
        chkAdjustPaymentForStrategy.setSelected(options.getAdjustPaymentForStrategy());
        spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.FIGHTING));
        spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.DEFENCE));
        spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.SCOUTING));
        spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.TRAINING));
        btnIntensityUpdate.doClick();
        chkGenerateChases.setSelected(options.generateChases());

        chkDoubleVehicles.setSelected(options.getDoubleVehicles());
        spnOpforLanceTypeMechs.setValue(options.getOpforLanceTypeMechs());
        spnOpforLanceTypeMixed.setValue(options.getOpforLanceTypeMixed());
        spnOpforLanceTypeVehicles.setValue(options.getOpforLanceTypeVehicles());
        chkOpforUsesVTOLs.setSelected(options.getOpforUsesVTOLs());
        chkOpforUsesAero.setSelected(options.getAllowOpforAeros());
        spnOpforAeroChance.setValue(options.getOpforAeroChance());
        chkOpforUsesLocalForces.setSelected(options.getAllowOpforLocalUnits());
        spnOpforLocalForceChance.setValue(options.getOpforLocalUnitChance());
        chkAdjustPlayerVehicles.setSelected(options.getAdjustPlayerVehicles());
        spnFixedMapChance.setValue(options.getFixedMapChance());
        chkRegionalMechVariations.setSelected(options.getRegionalMechVariations());
        chkAttachedPlayerCamouflage.setSelected(options.getAttachedPlayerCamouflage());
        chkPlayerControlsAttachedUnits.setSelected(options.getPlayerControlsAttachedUnits());
        chkUseDropShips.setSelected(options.getUseDropShips());
        chkUseWeatherConditions.setSelected(options.getUseWeatherConditions());
        chkUseLightConditions.setSelected(options.getUseLightConditions());
        chkUsePlanetaryConditions.setSelected(options.getUsePlanetaryConditions());
        //endregion Against the Bot Tab
    }

    public static String[][] getSkillCostsArray(Hashtable<String, SkillType> skillHash) {
        String[][] array = new String[SkillType.getSkillList().length][11];
        int i = 0;
        for (final String name : SkillType.getSkillList()) {
            SkillType type = skillHash.get(name);
            for (int j = 0; j < 11; j++) {
                array[i][j] = Integer.toString(type.getCost(j));
            }
            i++;
        }
        return array;
    }
    //endregion Initialization

    private void updateOptions() {
        try {
            campaign.setName(txtName.getText());
            if (isStartup()) {
                getCampaign().getForces().setName(getCampaign().getName());
            }
            campaign.setLocalDate(date);
            // Ensure that the MegaMek year GameOption matches the campaign year
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());
            campaign.setFactionCode(comboFaction.getSelectedItem().getFaction().getShortName());
            if (null != comboFactionNames.getSelectedItem()) {
                RandomNameGenerator.getInstance().setChosenFaction((String) comboFactionNames.getSelectedItem());
            }
            RandomGenderGenerator.setPercentFemale(sldGender.getValue());
            rankSystemsPane.applyToCampaign();
            campaign.setCamouflage(camouflage);
            campaign.setColour(colour);
            campaign.setUnitIcon(unitIcon);

            for (int i = 0; i < chkUsePortrait.length; i++) {
                options.setUsePortraitForRole(i, chkUsePortrait[i].isSelected());
            }

            updateSkillTypes();
            updateXPCosts();

            // Rules panel
            options.setEraMods(useEraModsCheckBox.isSelected());
            options.setAssignedTechFirst(assignedTechFirstCheckBox.isSelected());
            options.setResetToFirstTech(resetToFirstTechCheckBox.isSelected());
            options.setQuirks(useQuirksBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(useQuirksBox.isSelected());
            options.setUnitRatingMethod((UnitRatingMethod) unitRatingMethodCombo.getSelectedItem());
            options.setManualUnitRatingModifier((Integer) manualUnitRatingModifier.getValue());
            options.setUseOriginFactionForNames(chkUseOriginFactionForNames.isSelected());
            options.setDestroyByMargin(useDamageMargin.isSelected());
            options.setDestroyMargin((Integer) spnDamageMargin.getValue());
            options.setDestroyPartTarget((Integer) spnDestroyPartTarget.getValue());
            options.setUseAeroSystemHits(useAeroSystemHitsBox.isSelected());
            options.setCheckMaintenance(checkMaintenance.isSelected());
            options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
            options.setReverseQualityNames(reverseQualityNames.isSelected());
            options.setUseUnofficialMaintenance(useUnofficialMaintenance.isSelected());
            options.setLogMaintenance(logMaintenance.isSelected());
            options.setMaintenanceBonus((Integer) spnMaintenanceBonus.getValue());
            options.setMaintenanceCycleDays((Integer) spnMaintenanceDays.getValue());
            options.setPayForParts(payForPartsBox.isSelected());
            options.setPayForRepairs(payForRepairsBox.isSelected());
            options.setPayForUnits(payForUnitsBox.isSelected());
            options.setPayForSalaries(payForSalariesBox.isSelected());
            options.setPayForOverhead(payForOverheadBox.isSelected());
            options.setPayForMaintain(payForMaintainBox.isSelected());
            options.setPayForTransport(payForTransportBox.isSelected());
            options.setPayForRecruitment(payForRecruitmentBox.isSelected());
            options.setLoanLimits(useLoanLimitsBox.isSelected());
            options.setUsePercentageMaint(usePercentageMaintBox.isSelected());
            options.setUseInfantryDontCount(useInfantryDontCountBox.isSelected());
            options.setSellUnits(sellUnitsBox.isSelected());
            options.setSellParts(sellPartsBox.isSelected());
            options.setUsePeacetimeCost(usePeacetimeCostBox.isSelected());
            options.setUseExtendedPartsModifier(useExtendedPartsModifierBox.isSelected());
            options.setShowPeacetimeCost(showPeacetimeCostBox.isSelected());
            options.setNewFinancialYearFinancesToCSVExport(newFinancialYearFinancesToCSVExportBox.isSelected());
            options.setFinancialYearDuration((FinancialYearDuration) comboFinancialYearDuration.getSelectedItem());
            options.setAssignPortraitOnRoleChange(chkAssignPortraitOnRoleChange.isSelected());

            options.setEquipmentContractBase(btnContractEquipment.isSelected());
            options.setEquipmentContractPercent((Double) spnEquipPercent.getValue());
            options.setDropshipContractPercent((Double) spnDropshipPercent.getValue());
            options.setJumpshipContractPercent((Double) spnJumpshipPercent.getValue());
            options.setWarshipContractPercent((Double) spnWarshipPercent.getValue());
            options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
            options.setBLCSaleValue(chkBLCSaleValue.isSelected());
            options.setOverageRepaymentInFinalPayment(chkOverageRepaymentInFinalPayment.isSelected());

            options.setWaitingPeriod((Integer) spnAcquireWaitingPeriod.getValue());
            options.setAcquisitionSkill((String) choiceAcquireSkill.getSelectedItem());
            options.setAcquisitionSupportStaffOnly(chkSupportStaffOnly.isSelected());
            options.setClanAcquisitionPenalty((Integer) spnAcquireClanPenalty.getValue());
            options.setIsAcquisitionPenalty((Integer) spnAcquireIsPenalty.getValue());
            options.setMaxAcquisitions(Integer.parseInt(txtMaxAcquisitions.getText()));

            options.setNDiceTransitTime((Integer) spnNDiceTransitTime.getValue());
            options.setConstantTransitTime((Integer) spnConstantTransitTime.getValue());
            options.setUnitTransitTime(choiceTransitTimeUnits.getSelectedIndex());
            options.setAcquireMosBonus((Integer) spnAcquireMosBonus.getValue());
            options.setAcquireMinimumTime((Integer) spnAcquireMinimum.getValue());
            options.setAcquireMinimumTimeUnit(choiceAcquireMinimumUnit.getSelectedIndex());
            options.setAcquireMosUnit(choiceAcquireMosUnits.getSelectedIndex());
            options.setPlanetaryAcquisition(usePlanetaryAcquisitions.isSelected());
            options.setDisallowClanPartsFromIS(disallowClanPartsFromIS.isSelected());
            options.setPlanetAcquisitionVerboseReporting(usePlanetaryAcquisitionsVerbose.isSelected());
            options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetaryAcquisitionClanCrossover.isSelected());
            options.setMaxJumpsPlanetaryAcquisition((int) spnMaxJumpPlanetaryAcquisitions.getValue());
            options.setPenaltyClanPartsFroIS((int) spnPenaltyClanPartsFromIS.getValue());
            options.setPlanetAcquisitionFactionLimit((PlanetaryAcquisitionFactionLimit) comboPlanetaryAcquisitionsFactionLimits.getSelectedItem());

            for (int i = ITechnology.RATING_A; i <= ITechnology.RATING_F; i++) {
                options.setPlanetTechAcquisitionBonus((int) spnPlanetAcquireTechBonus[i].getValue(), i);
                options.setPlanetIndustryAcquisitionBonus((int) spnPlanetAcquireIndustryBonus[i].getValue(), i);
                options.setPlanetOutputAcquisitionBonus((int) spnPlanetAcquireOutputBonus[i].getValue(), i);

            }

            options.setScenarioXP((Integer) spnScenarioXP.getValue());
            options.setKillsForXP((Integer) spnKills.getValue());
            options.setKillXPAward((Integer) spnKillXP.getValue());

            options.setTaskXP((Integer) spnTaskXP.getValue());
            options.setNTasksXP((Integer) spnNTasksXP.getValue());
            options.setSuccessXP((Integer) spnSuccessXP.getValue());
            options.setMistakeXP((Integer) spnMistakeXP.getValue());
            options.setIdleXP((Integer) spnIdleXP.getValue());
            options.setMonthsIdleXP((Integer) spnMonthsIdleXP.getValue());
            options.setContractNegotiationXP((Integer) spnContractNegotiationXP.getValue());
            options.setAdminXP((Integer) spnAdminWeeklyXP.getValue());
            options.setAdminXPPeriod((Integer) spnAdminWeeklyXPPeriod.getValue());
            options.setEdgeCost((Integer) spnEdgeCost.getValue());
            options.setTargetIdleXP((Integer) spnTargetIdleXP.getValue());

            options.setLimitByYear(limitByYearBox.isSelected());
            options.setDisallowExtinctStuff(disallowExtinctStuffBox.isSelected());
            options.setAllowClanPurchases(allowClanPurchasesBox.isSelected());
            options.setAllowISPurchases(allowISPurchasesBox.isSelected());
            options.setAllowCanonOnly(allowCanonOnlyBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_CANON_ONLY).setValue(allowCanonOnlyBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_ERA_BASED).setValue(variableTechLevelBox.isSelected());
            options.setVariableTechLevel(variableTechLevelBox.isSelected() && options.limitByYear());
            options.setFactionIntroDate(factionIntroDateBox.isSelected());
            campaign.updateTechFactionCode();
            options.setAllowCanonRefitOnly(allowCanonRefitOnlyBox.isSelected());
            options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
            options.setTechLevel(choiceTechLevel.getSelectedIndex());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_TECHLEVEL).setValue((String) choiceTechLevel.getSelectedItem());

            rSkillPrefs.setOverallRecruitBonus((Integer) spnOverallRecruitBonus.getValue());
            for (int i = 0; i < spnTypeRecruitBonus.length; i++) {
                rSkillPrefs.setRecruitBonus(i, (Integer) spnTypeRecruitBonus[i].getValue());
            }
            rSkillPrefs.setRandomizeSkill(chkExtraRandom.isSelected());
            rSkillPrefs.setAntiMekProb((Integer) spnProbAntiMek.getValue());
            rSkillPrefs.setArtilleryProb((Integer) spnArtyProb.getValue());
            rSkillPrefs.setArtilleryBonus((Integer) spnArtyBonus.getValue());
            rSkillPrefs.setSecondSkillProb((Integer) spnSecondProb.getValue());
            rSkillPrefs.setSecondSkillBonus((Integer) spnSecondBonus.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_GREEN, (Integer) spnTacticsGreen.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_REGULAR, (Integer) spnTacticsReg.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_VETERAN, (Integer) spnTacticsVet.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_ELITE, (Integer) spnTacticsElite.getValue());
            rSkillPrefs.setCombatSmallArmsBonus((Integer) spnCombatSA.getValue());
            rSkillPrefs.setSupportSmallArmsBonus((Integer) spnSupportSA.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_GREEN, (Integer) spnAbilGreen.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_REGULAR, (Integer) spnAbilReg.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_VETERAN, (Integer) spnAbilVet.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_ELITE, (Integer) spnAbilElite.getValue());
            campaign.setRandomSkillPreferences(rSkillPrefs);

            for (int i = 0; i < phenotypeSpinners.length; i++) {
                options.setPhenotypeProbability(i, (Integer) phenotypeSpinners[i].getValue());
            }

            //region Personnel Tab
            // General Personnel
            options.setUseTactics(chkUseTactics.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_COMMAND_INIT).setValue(chkUseTactics.isSelected());
            options.setUseInitiativeBonus(chkUseInitiativeBonus.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).setValue(chkUseInitiativeBonus.isSelected());
            options.setUseToughness(chkUseToughness.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_TOUGHNESS).setValue(chkUseToughness.isSelected());
            options.setUseArtillery(chkUseArtillery.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_ARTILLERY_SKILL).setValue(chkUseArtillery.isSelected());
            options.setUseAbilities(chkUseAbilities.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_PILOT_ADVANTAGES).setValue(chkUseAbilities.isSelected());
            options.setUseEdge(chkUseEdge.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.EDGE).setValue(chkUseEdge.isSelected());
            options.setUseSupportEdge(chkUseEdge.isSelected() && chkUseSupportEdge.isSelected());
            options.setUseImplants(chkUseImplants.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_MANEI_DOMINI).setValue(chkUseImplants.isSelected());
            options.setAlternativeQualityAveraging(chkUseAlternativeQualityAveraging.isSelected());
            options.setUseTransfers(chkUseTransfers.isSelected());
            options.setUseExtendedTOEForceName(chkUseExtendedTOEForceName.isSelected());
            options.setPersonnelLogSkillGain(chkPersonnelLogSkillGain.isSelected());
            options.setPersonnelLogAbilityGain(chkPersonnelLogAbilityGain.isSelected());
            options.setPersonnelLogEdgeGain(chkPersonnelLogEdgeGain.isSelected());

            // Expanded Personnel Information
            options.setUseTimeInService(chkUseTimeInService.isSelected());
            options.setTimeInServiceDisplayFormat((TimeInDisplayFormat) comboTimeInServiceDisplayFormat.getSelectedItem());
            options.setUseTimeInRank(chkUseTimeInRank.isSelected());
            options.setTimeInRankDisplayFormat((TimeInDisplayFormat) comboTimeInRankDisplayFormat.getSelectedItem());
            options.setUseRetirementDateTracking(chkUseRetirementDateTracking.isSelected());
            options.setTrackTotalEarnings(chkTrackTotalEarnings.isSelected());
            options.setTrackTotalXPEarnings(chkTrackTotalXPEarnings.isSelected());
            options.setShowOriginFaction(chkShowOriginFaction.isSelected());

            // Medical
            options.setUseAdvancedMedical(chkUseAdvancedMedical.isSelected());
            // we need to reset healing time options through the campaign because we may need to
            // loop through personnel to make adjustments
            campaign.setHealingTimeOptions((Integer) spnHealWaitingPeriod.getValue(),
                    (Integer) spnNaturalHealWaitingPeriod.getValue());
            options.setMinimumHitsForVehicles((Integer) spnMinimumHitsForVehicles.getValue());
            options.setUseRandomHitsForVehicles(chkUseRandomHitsForVehicles.isSelected());
            options.setTougherHealing(chkUseTougherHealing.isSelected());

            // Prisoners
            options.setPrisonerCaptureStyle((PrisonerCaptureStyle) comboPrisonerCaptureStyle.getSelectedItem());
            options.setDefaultPrisonerStatus((PrisonerStatus) comboPrisonerStatus.getSelectedItem());
            options.setPrisonerBabyStatus(chkPrisonerBabyStatus.isSelected());
            options.setUseAtBPrisonerDefection(chkAtBPrisonerDefection.isSelected());
            options.setUseAtBPrisonerRansom(chkAtBPrisonerRansom.isSelected());

            // Personnel Randomization
            options.setUseDylansRandomXP(chkUseDylansRandomXP.isSelected());
            options.setRandomizeOrigin(chkRandomizeOrigin.isSelected());
            options.setRandomizeDependentOrigin(chkRandomizeDependentsOrigin.isSelected());
            options.setOriginSearchRadius((Integer) spnOriginSearchRadius.getValue());
            options.setExtraRandomOrigin(chkExtraRandomOrigin.isSelected());
            options.setOriginDistanceScale((Double) spnOriginDistanceScale.getValue());

            // Family
            options.setDisplayFamilyLevel((FamilialRelationshipDisplayLevel) comboDisplayFamilyLevel.getSelectedItem());

            // Salary
            options.setSalaryCommissionMultiplier((Double) spnCommissionedSalary.getValue());
            options.setSalaryEnlistedMultiplier((Double) spnEnlistedSalary.getValue());
            options.setSalaryAntiMekMultiplier((Double) spnAntiMekSalary.getValue());
            options.setSalarySpecialistInfantryMultiplier((Double) spnSpecialistInfantrySalary.getValue());
            for (int i = 0; i < spnSalaryExperienceMultipliers.length; i++) {
                options.setSalaryXPMultiplier(i, (Double) spnSalaryExperienceMultipliers[i].getValue());
            }
            for (final PersonnelRole personnelRole : PersonnelRole.values()) {
                options.setRoleBaseSalary(personnelRole, (double) spnBaseSalary[personnelRole.ordinal()].getValue());
            }

            // Marriage
            options.setUseManualMarriages(chkUseManualMarriages.isSelected());
            options.setUseClannerMarriages(chkUseClannerMarriages.isSelected());
            options.setUsePrisonerMarriages(chkUsePrisonerMarriages.isSelected());
            options.setMinimumMarriageAge((Integer) spnMinimumMarriageAge.getValue());
            options.setCheckMutualAncestorsDepth((Integer) spnCheckMutualAncestorsDepth.getValue());
            options.setLogMarriageNameChanges(chkLogMarriageNameChanges.isSelected());
            for (final Map.Entry<MergingSurnameStyle, JSpinner> entry : spnMarriageSurnameWeights.entrySet()) {
                options.getMarriageSurnameWeights().put(entry.getKey(), (int) Math.round((Double) entry.getValue().getValue() * 10.0));
            }
            options.setRandomMarriageMethod(comboRandomMarriageMethod.getSelectedItem());
            options.setUseRandomSameSexMarriages(chkUseRandomSameSexMarriages.isSelected());
            options.setUseRandomClannerMarriages(chkUseRandomClannerMarriages.isSelected());
            options.setUseRandomPrisonerMarriages(chkUseRandomPrisonerMarriages.isSelected());
            options.setRandomMarriageAgeRange((Integer) spnRandomMarriageAgeRange.getValue());
            options.setPercentageRandomMarriageOppositeSexChance((Double) spnPercentageRandomMarriageOppositeSexChance.getValue() / 100.0);
            options.setPercentageRandomMarriageSameSexChance((Double) spnPercentageRandomMarriageSameSexChance.getValue() / 100.0);

            // Divorce
            options.setUseManualDivorce(chkUseManualDivorce.isSelected());
            options.setUseClannerDivorce(chkUseClannerDivorce.isSelected());
            options.setUsePrisonerDivorce(chkUsePrisonerDivorce.isSelected());
            for (final Map.Entry<SplittingSurnameStyle, JSpinner> entry : spnDivorceSurnameWeights.entrySet()) {
                options.getDivorceSurnameWeights().put(entry.getKey(), (int) Math.round((Double) entry.getValue().getValue() * 10.0));
            }
            options.setRandomDivorceMethod(comboRandomDivorceMethod.getSelectedItem());
            options.setUseRandomOppositeSexDivorce(chkUseRandomOppositeSexDivorce.isSelected());
            options.setUseRandomSameSexDivorce(chkUseRandomSameSexDivorce.isSelected());
            options.setUseRandomClannerDivorce(chkUseRandomClannerDivorce.isSelected());
            options.setUseRandomPrisonerDivorce(chkUseRandomPrisonerDivorce.isSelected());
            options.setPercentageRandomDivorceOppositeSexChance((Double) spnPercentageRandomDivorceOppositeSexChance.getValue() / 100.0);
            options.setPercentageRandomDivorceSameSexChance((Double) spnPercentageRandomDivorceSameSexChance.getValue() / 100.0);

            // Procreation
            options.setUseManualProcreation(chkUseManualProcreation.isSelected());
            options.setUseClannerProcreation(chkUseClannerProcreation.isSelected());
            options.setUsePrisonerProcreation(chkUsePrisonerProcreation.isSelected());
            options.setMultiplePregnancyOccurrences((Integer) spnMultiplePregnancyOccurrences.getValue());
            options.setBabySurnameStyle(comboBabySurnameStyle.getSelectedItem());
            options.setAssignNonPrisonerBabiesFounderTag(chkAssignNonPrisonerBabiesFounderTag.isSelected());
            options.setAssignChildrenOfFoundersFounderTag(chkAssignChildrenOfFoundersFounderTag.isSelected());
            options.setDetermineFatherAtBirth(chkDetermineFatherAtBirth.isSelected());
            options.setDisplayTrueDueDate(chkDisplayTrueDueDate.isSelected());
            options.setLogProcreation(chkLogProcreation.isSelected());
            options.setRandomProcreationMethod(comboRandomProcreationMethod.getSelectedItem());
            options.setUseRelationshiplessRandomProcreation(chkUseRelationshiplessRandomProcreation.isSelected());
            options.setUseRandomClannerProcreation(chkUseRandomClannerProcreation.isSelected());
            options.setUseRandomPrisonerProcreation(chkUseRandomPrisonerProcreation.isSelected());
            options.setPercentageRandomProcreationRelationshipChance((Double) spnPercentageRandomProcreationRelationshipChance.getValue() / 100.0);
            options.setPercentageRandomProcreationRelationshiplessChance((Double) spnPercentageRandomProcreationRelationshiplessChance.getValue() / 100.0);

            // Death
            options.setKeepMarriedNameUponSpouseDeath(chkKeepMarriedNameUponSpouseDeath.isSelected());
            //endregion Personnel Tab

            //region Finances Tab

            // Price Multipliers
            options.setCommonPartPriceMultiplier((Double) spnCommonPartPriceMultiplier.getValue());
            options.setInnerSphereUnitPriceMultiplier((Double) spnInnerSphereUnitPriceMultiplier.getValue());
            options.setInnerSpherePartPriceMultiplier((Double) spnInnerSpherePartPriceMultiplier.getValue());
            options.setClanUnitPriceMultiplier((Double) spnClanUnitPriceMultiplier.getValue());
            options.setClanPartPriceMultiplier((Double) spnClanPartPriceMultiplier.getValue());
            options.setMixedTechUnitPriceMultiplier((Double) spnMixedTechUnitPriceMultiplier.getValue());
            for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
                options.getUsedPartPriceMultipliers()[i] = (Double) spnUsedPartPriceMultipliers[i].getValue();
            }
            options.setDamagedPartsValueMultiplier((Double) spnDamagedPartsValueMultiplier.getValue());
            options.setUnrepairablePartsValueMultiplier((Double) spnUnrepairablePartsValueMultiplier.getValue());
            options.setCancelledOrderRefundMultiplier((Double) spnCancelledOrderRefundMultiplier.getValue());
            //endregion Finances Tab

            //start SPA
            SpecialAbility.replaceSpecialAbilities(getCurrentSPA());
            //end SPA

            //region Markets Tab
            // Personnel Market
            options.setPersonnelMarketType((String) comboPersonnelMarketType.getSelectedItem());
            options.setPersonnelMarketReportRefresh(chkPersonnelMarketReportRefresh.isSelected());
            options.setPersonnelMarketRandomEliteRemoval((Integer) spnPersonnelMarketRandomEliteRemoval.getValue());
            options.setPersonnelMarketRandomVeteranRemoval((Integer) spnPersonnelMarketRandomVeteranRemoval.getValue());
            options.setPersonnelMarketRandomRegularRemoval((Integer) spnPersonnelMarketRandomRegularRemoval.getValue());
            options.setPersonnelMarketRandomGreenRemoval((Integer) spnPersonnelMarketRandomGreenRemoval.getValue());
            options.setPersonnelMarketRandomUltraGreenRemoval((Integer) spnPersonnelMarketRandomUltraGreenRemoval.getValue());
            options.setPersonnelMarketDylansWeight((Double) spnPersonnelMarketDylansWeight.getValue());

            // Unit Market
            options.setUnitMarketMethod((UnitMarketMethod) comboUnitMarketMethod.getSelectedItem());
            options.setUnitMarketRegionalMechVariations(chkUnitMarketRegionalMechVariations.isSelected());
            options.setInstantUnitMarketDelivery(chkInstantUnitMarketDelivery.isSelected());
            options.setUnitMarketReportRefresh(chkUnitMarketReportRefresh.isSelected());

            // Contract Market
            options.setContractMarketMethod((ContractMarketMethod) comboContractMarketMethod.getSelectedItem());
            options.setContractMarketReportRefresh(chkContractMarketReportRefresh.isSelected());
            //endregion Markets Tab

            //region RATs Tab
            options.setUseStaticRATs(btnUseStaticRATs.isSelected());
            //Strip dates used in display name
            String[] ratList = new String[chosenRATModel.size()];
            for (int i = 0; i < chosenRATModel.size(); i++) {
                ratList[i] = chosenRATModel.elementAt(i).replaceFirst(" \\(.*?\\)", "");
            }
            options.setRATs(ratList);
            options.setIgnoreRATEra(chkIgnoreRATEra.isSelected());
            //endregion RATs Tab

            // Start Against the Bot
            options.setUseAtB(chkUseAtB.isSelected());
            options.setUseStratCon(chkUseStratCon.isSelected());
            options.setSkillLevel(cbSkillLevel.getSelectedIndex());
            options.setUseShareSystem(chkUseShareSystem.isSelected());
            options.setSharesExcludeLargeCraft(chkSharesExcludeLargeCraft.isSelected());
            options.setSharesForAll(chkSharesForAll.isSelected());
            options.setTrackOriginalUnit(chkTrackOriginalUnit.isSelected());
            options.setRetirementRolls(chkRetirementRolls.isSelected());
            options.setCustomRetirementMods(chkCustomRetirementMods.isSelected());
            options.setFoundersNeverRetire(chkFoundersNeverRetire.isSelected());
            options.setAtBAddDependents(chkAddDependents.isSelected());
            options.setDependentsNeverLeave(chkDependentsNeverLeave.isSelected());
            options.setTrackUnitFatigue(chkTrackUnitFatigue.isSelected());
            options.setLimitLanceWeight(chkLimitLanceWeight.isSelected());
            options.setLimitLanceNumUnits(chkLimitLanceNumUnits.isSelected());
            options.setUseLeadership(chkUseLeadership.isSelected());
            options.setUseStrategy(chkUseStrategy.isSelected());
            options.setBaseStrategyDeployment((Integer) spnBaseStrategyDeployment.getValue());
            options.setAdditionalStrategyDeployment((Integer) spnAdditionalStrategyDeployment.getValue());
            options.setAdjustPaymentForStrategy(chkAdjustPaymentForStrategy.isSelected());

            options.setUseAero(chkUseAero.isSelected());
            options.setUseVehicles(chkUseVehicles.isSelected());
            options.setClanVehicles(chkClanVehicles.isSelected());
            options.setDoubleVehicles(chkDoubleVehicles.isSelected());
            options.setAdjustPlayerVehicles(chkAdjustPlayerVehicles.isSelected());
            options.setOpforLanceTypeMechs((Integer) spnOpforLanceTypeMechs.getValue());
            options.setOpforLanceTypeMixed((Integer) spnOpforLanceTypeMixed.getValue());
            options.setOpforLanceTypeVehicles((Integer) spnOpforLanceTypeVehicles.getValue());
            options.setOpforUsesVTOLs(chkOpforUsesVTOLs.isSelected());
            options.setAllowOpforAeros(chkOpforUsesAero.isSelected());
            options.setAllowOpforLocalUnits(chkOpforUsesLocalForces.isSelected());
            options.setOpforAeroChance((Integer) spnOpforAeroChance.getValue());
            options.setOpforLocalUnitChance((Integer) spnOpforLocalForceChance.getValue());
            options.setFixedMapChance((Integer) spnFixedMapChance.getValue());
            options.setUseDropShips(chkUseDropShips.isSelected());

            options.setSearchRadius((Integer) spnSearchRadius.getValue());
            for (int i = 0; i < spnAtBBattleChance.length; i++) {
                options.setAtBBattleChance(i, (Integer) spnAtBBattleChance[i].getValue());
            }
            options.setGenerateChases(chkGenerateChases.isSelected());
            options.setVariableContractLength(chkVariableContractLength.isSelected());
            options.setMercSizeLimited(chkMercSizeLimited.isSelected());
            options.setRestrictPartsByMission(chkRestrictPartsByMission.isSelected());
            options.setRegionalMechVariations(chkRegionalMechVariations.isSelected());
            options.setAttachedPlayerCamouflage(chkAttachedPlayerCamouflage.isSelected());
            options.setPlayerControlsAttachedUnits(chkPlayerControlsAttachedUnits.isSelected());
            options.setUseWeatherConditions(chkUseWeatherConditions.isSelected());
            options.setUseLightConditions(chkUseLightConditions.isSelected());
            options.setUsePlanetaryConditions(chkUsePlanetaryConditions.isSelected());
            options.setAeroRecruitsHaveUnits(chkAeroRecruitsHaveUnits.isSelected());
            // End Against the Bot

            campaign.setCampaignOptions(options);

            MekHQ.triggerEvent(new OptionsChangedEvent(campaign, options));
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            JOptionPane.showMessageDialog(getFrame(),
                    "Campaign Options update failure, please check the logs for the exception reason.",
                    "Error Updating Campaign Options", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void okButtonActionPerformed(final ActionEvent evt) {
        if (!txtName.getText().isBlank()) {
            updateOptions();
            setResult(DialogResult.CONFIRMED);
            setVisible(false);
        }
    }

    private void btnSaveActionPerformed() {
        if (txtName.getText().isBlank()) {
            return;
        }
        updateOptions();
        setResult(DialogResult.CONFIRMED);

        final CreateCampaignPresetDialog createCampaignPresetDialog
                = new CreateCampaignPresetDialog(getFrame(), getCampaign(), null);
        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            setVisible(false);
            return;
        }
        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            setVisible(false);
            return;
        }
        preset.writeToFile(getFrame(),
                FileDialogs.saveCampaignPreset(getFrame(), preset).orElse(null));
        setVisible(false);
    }

    private void updateXPCosts() {
        for (int i = 0; i < SkillType.skillList.length; i++) {
            for (int j = 0; j < 11; j++) {
                try {
                    int cost = Integer.parseInt((String) tableXP.getValueAt(i, j));
                    SkillType.setCost(SkillType.skillList[i], cost, j);
                } catch (NumberFormatException e) {
                    LogManager.getLogger().error("unreadable value in skill cost table for " + SkillType.skillList[i]);
                }
            }
        }
    }

    private void updateSkillTypes() {
        for (final String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (null != hashSkillTargets.get(skillName)) {
                type.setTarget((Integer) hashSkillTargets.get(skillName).getValue());
            }
            if (null != hashGreenSkill.get(skillName)) {
                type.setGreenLevel((Integer) hashGreenSkill.get(skillName).getValue());
            }
            if (null != hashRegSkill.get(skillName)) {
                type.setRegularLevel((Integer) hashRegSkill.get(skillName).getValue());
            }
            if (null != hashVetSkill.get(skillName)) {
                type.setVeteranLevel((Integer) hashVetSkill.get(skillName).getValue());
            }
            if (null != hashEliteSkill.get(skillName)) {
                type.setEliteLevel((Integer) hashEliteSkill.get(skillName).getValue());
            }
        }
    }

    private void btnDateActionPerformed(ActionEvent evt) {
        // show the date chooser
        DateChooser dc = new DateChooser(getFrame(), date);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            setDate(dc.getDate());
        }
    }

    private void setDate(final @Nullable LocalDate date) {
        if (date == null) {
            return;
        }

        this.date = date;
        btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(date));

        final FactionDisplay factionDisplay = comboFaction.getSelectedItem();
        comboFaction.removeAllItems();
        ((DefaultComboBoxModel<FactionDisplay>) comboFaction.getModel()).addAll(FactionDisplay
                .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), date));
        comboFaction.setSelectedItem(factionDisplay);
    }

    private void btnCamoActionPerformed(ActionEvent evt) {
        CamoChooserDialog ccd = new CamoChooserDialog(getFrame(), camouflage);
        if (ccd.showDialog().isConfirmed()) {
            camouflage = ccd.getSelectedItem();
            btnCamo.setIcon(camouflage.getImageIcon());
        }
    }

    private Vector<String> getUnusedSPA() {
        Vector<String> unused = new Vector<>();
        PersonnelOptions poptions = new PersonnelOptions();
        for (final Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (final Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();
                if (getCurrentSPA().get(option.getName()) == null) {
                    unused.add(option.getName());
                }
            }
        }

        for (final String key : SpecialAbility.getAllDefaultSpecialAbilities().keySet()) {
            if ((getCurrentSPA().get(key) == null) && !unused.contains(key)) {
                unused.add(key);
            }
        }

        return unused;
    }

    public Hashtable<String, SpecialAbility> getCurrentSPA() {
        return tempSPA;
    }

    private void btnAddSPA() {
        SelectUnusedAbilityDialog suad = new SelectUnusedAbilityDialog(getFrame(), getUnusedSPA(), getCurrentSPA());
        suad.setVisible(true);

        recreateSPAPanel(!getUnusedSPA().isEmpty());
    }

    public void btnRemoveSPA(String name) {
        getCurrentSPA().remove(name);

        //we also need to cycle through the existing SPAs and remove this one from
        //any prereqs
        for (final String key: getCurrentSPA().keySet()) {
            SpecialAbility otherAbil = getCurrentSPA().get(key);
            Vector<String> prereq = otherAbil.getPrereqAbilities();
            Vector<String> invalid = otherAbil.getInvalidAbilities();
            Vector<String> remove = otherAbil.getRemovedAbilities();
            if (prereq.remove(name)) {
                otherAbil.setPrereqAbilities(prereq);
            }
            if (invalid.remove(name)) {
                otherAbil.setInvalidAbilities(invalid);
            }
            if (remove.remove(name)) {
                otherAbil.setRemovedAbilities(remove);
            }
        }

        recreateSPAPanel(true);
    }

    public void recreateSPAPanel(boolean enableAddSPA) {
        panSpecialAbilities.removeAll();

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panSpecialAbilities.add(btnAddSPA, gridBagConstraints);
        btnAddSPA.setEnabled(enableAddSPA);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1.0;

        NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        getCurrentSPA().values().stream().sorted((o1, o2) ->
                naturalOrderComparator.compare(o1.getDisplayName(), o2.getDisplayName())
        ).forEach(spa -> {
            panSpecialAbilities.add(new SpecialAbilityPanel(spa, this), gridBagConstraints);
            gridBagConstraints.gridy++;
        });
        panSpecialAbilities.revalidate();
        panSpecialAbilities.repaint();
    }

    private void enableAtBComponents(JPanel panel, boolean enabled) {
        for (final Component c : panel.getComponents()) {
            if (c.equals(chkUseAtB)) {
                continue;
            }

            if (c instanceof JPanel) {
                enableAtBComponents((JPanel) c, enabled);
            } else {
                c.setEnabled(enabled);
            }
        }
    }

    private double determineAtBBattleIntensity() {
        double intensity = 0.0;
        int x;

        x = (Integer) spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].getValue();
        intensity += ((-3.0 / 2.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].getValue();
        intensity += ((-4.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].getValue();
        intensity += ((-2.0 / 3.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].getValue();
        intensity += ((-9.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        intensity = intensity / 4.0;

        if (intensity > 100.0) {
            intensity = 100.0;
        }

        return Math.round(intensity * 10.0) / 10.0;
    }

    private class AtBBattleIntensityChangeListener implements ChangeListener  {
        @Override
        public void stateChanged(ChangeEvent e) {
            double intensity = (Double) spnAtBBattleIntensity.getValue();

            if (intensity >= AtBContract.MINIMUM_INTENSITY) {
                int value = (int) Math.min(Math.round(400.0 * intensity / (4.0 * intensity + 6.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(200.0 * intensity / (2.0 * intensity + 8.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(600.0 * intensity / (6.0 * intensity + 4.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(100.0 * intensity / (intensity + 9.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(value);
            } else {
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(0);
            }
        }
    }

    /*
     * Taken from:
     *  http://tips4java.wordpress.com/2008/11/18/row-number-table/
     *	Use a JTable as a renderer for row numbers of a given main table.
     *  This table must be added to the row header of the scrollpane that
     *  contains the main table.
     */
    public static class RowNamesTable extends JTable implements ChangeListener, PropertyChangeListener {
        private static final long serialVersionUID = 3151119498072423302L;
        private JTable main;

        public RowNamesTable(JTable table) {
            main = table;
            main.addPropertyChangeListener(this);

            setFocusable(false);
            setAutoCreateColumnsFromModel(false);
            setModel(main.getModel());
            setSelectionModel(main.getSelectionModel());

            TableColumn column = new TableColumn();
            column.setHeaderValue(" ");
            addColumn(column);
            column.setCellRenderer(new RowNumberRenderer());

            getColumnModel().getColumn(0).setPreferredWidth(120);
            setPreferredScrollableViewportSize(getPreferredSize());
        }

        @Override
        public void addNotify() {
            super.addNotify();
            Component c = getParent();
            //  Keep scrolling of the row table in sync with the main table.
            if (c instanceof JViewport) {
                JViewport viewport = (JViewport) c;
                viewport.addChangeListener(this);
            }
        }

        /*
         *  Delegate method to main table
         */
        @Override
        public int getRowCount() {
            return main.getRowCount();
        }

        @Override
        public int getRowHeight(int row) {
            return main.getRowHeight(row);
        }

        /*
         *  This table does not use any data from the main TableModel,
         *  so just return a value based on the row parameter.
         */
        @Override
        public Object getValueAt(int row, int column) {
            return SkillType.skillList[row];
        }

        /*
         *  Don't edit data in the main TableModel by mistake
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        //
        //  Implement the ChangeListener
        //
        @Override
        public void stateChanged(ChangeEvent e) {
            //  Keep the scrolling of the row table in sync with main table
            JViewport viewport = (JViewport) e.getSource();
            JScrollPane scrollPane = (JScrollPane) viewport.getParent();
            scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
        }

        //
        //  Implement the PropertyChangeListener
        //
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            //  Keep the row table in sync with the main table

            if ("selectionModel".equals(e.getPropertyName())) {
                setSelectionModel(main.getSelectionModel());
            }

            if ("model".equals(e.getPropertyName())) {
                setModel(main.getModel());
            }
        }

        /*
         *  Borrow the renderer from JDK1.4.2 table header
         */
        private static class RowNumberRenderer extends DefaultTableCellRenderer {
            private static final long serialVersionUID = -5430873664301394767L;

            public RowNumberRenderer() {
                setHorizontalAlignment(JLabel.LEFT);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (table != null) {
                    JTableHeader header = table.getTableHeader();

                    if (header != null) {
                        setForeground(header.getForeground());
                        setBackground(header.getBackground());
                        setFont(header.getFont());
                    }
                }

                if (isSelected) {
                    setFont(getFont().deriveFont(Font.BOLD));
                }

                setText((value == null) ? "" : value.toString());
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));

                return this;
            }
        }
    }
}
