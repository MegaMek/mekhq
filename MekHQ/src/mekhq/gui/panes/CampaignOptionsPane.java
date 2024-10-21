/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panes;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.JDisableablePanel;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.enums.ValidationState;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.codeUtilities.StringUtility;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.market.PersonnelMarketDylan;
import mekhq.campaign.market.PersonnelMarketRandom;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.Skills;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.SpecialAbilityPanel;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.SelectUnusedAbilityDialog;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;
import mekhq.gui.panels.RandomOriginOptionsPanel;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.LayoutStyle.ComponentPlacement;
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
import java.util.Map.Entry;
import java.util.stream.IntStream;

import static megamek.client.ui.WrapLayout.wordWrap;

/**
 * @author Justin 'Windchild' Bowen
 */
public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    private static final MMLogger logger = MMLogger.create(CampaignOptionsPane.class);

    // region Variable Declarations
    // region General Variables (ones not relating to a specific tab)
    private final Campaign campaign;
    private final boolean startup;
    private CampaignOptions options;
    private RandomSkillPreferences rSkillPrefs;
    private LocalDate date;
    private Camouflage camouflage;
    private final PlayerColour colour;
    private StandardForceIcon unitIcon;
    private final Hashtable<String, JSpinner> hashSkillTargets;
    private final Hashtable<String, JSpinner> hashGreenSkill;
    private final Hashtable<String, JSpinner> hashRegSkill;
    private final Hashtable<String, JSpinner> hashVetSkill;
    private final Hashtable<String, JSpinner> hashEliteSkill;
    // endregion General Variables (ones not relating to a specific tab)

    // region General Tab
    private JTextField txtName;
    private MMComboBox<FactionDisplay> comboFaction;
    private MMComboBox<UnitRatingMethod> unitRatingMethodCombo;
    private JSpinner manualUnitRatingModifier;
    private JButton btnDate;
    private JButton btnCamo;
    private JButton btnIcon;
    // endregion General Tab

    // region Repair and Maintenance Tab
    // Repair
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
    private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useDamageMargin;
    private JSpinner spnDamageMargin;
    private JSpinner spnDestroyPartTarget;

    // Maintenance
    private JCheckBox checkMaintenance;
    private JSpinner spnMaintenanceDays;
    private JSpinner spnMaintenanceBonus;
    private JCheckBox useQualityMaintenance;
    private JCheckBox reverseQualityNames;
    private JCheckBox chkUseRandomUnitQualities;
    private JCheckBox chkUsePlanetaryModifiers;
    private JCheckBox useUnofficialMaintenance;
    private JCheckBox logMaintenance;
    private JSpinner spnDefaultMaintenanceTime;
    // endregion Repair and Maintenance Tab

    // region Supplies and Acquisitions Tab
    // Acquisition
    private JSpinner spnAcquireWaitingPeriod;
    private MMComboBox<String> choiceAcquireSkill;
    private JCheckBox chkSupportStaffOnly;
    private JSpinner spnAcquireClanPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JSpinner spnMaxAcquisitions;

    // Delivery
    private JSpinner spnNDiceTransitTime;
    private JSpinner spnConstantTransitTime;
    private MMComboBox<String> choiceTransitTimeUnits;
    private JSpinner spnAcquireMinimum;
    private MMComboBox<String> choiceAcquireMinimumUnit;
    private JSpinner spnAcquireMosBonus;
    private MMComboBox<String> choiceAcquireMosUnits;

    // Planetary Acquisitions
    private JCheckBox usePlanetaryAcquisitions;
    private JSpinner spnMaxJumpPlanetaryAcquisitions;
    private MMComboBox<PlanetaryAcquisitionFactionLimit> comboPlanetaryAcquisitionsFactionLimits;
    private JCheckBox disallowPlanetaryAcquisitionClanCrossover;
    private JCheckBox usePlanetaryAcquisitionsVerbose;
    private JSpinner[] spnPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    private JCheckBox disallowClanPartsFromIS;
    private JSpinner spnPenaltyClanPartsFromIS;
    // endregion Supplies and Acquisitions Tab

    // region Tech Limits Tab
    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private MMComboBox<String> choiceTechLevel;
    private JCheckBox variableTechLevelBox;
    private JCheckBox factionIntroDateBox;
    private JCheckBox useAmmoByTypeBox;
    // endregion Tech Limits Tab

    // region Personnel Tab
    // General Personnel
    private JCheckBox chkUseTactics;
    private JCheckBox chkUseInitiativeBonus;
    private JCheckBox chkUseToughness;
    private JCheckBox chkUseRandomToughness;
    private JCheckBox chkUseArtillery;
    private JCheckBox chkUseAbilities;
    private JCheckBox chkUseEdge;
    private JCheckBox chkUseSupportEdge;
    private JCheckBox chkUseImplants;
    private JCheckBox chkUseAlternativeQualityAveraging;
    private JCheckBox chkUseTransfers;
    private JCheckBox chkUseExtendedTOEForceName;
    private JCheckBox chkPersonnelLogSkillGain;
    private JCheckBox chkPersonnelLogAbilityGain;
    private JCheckBox chkPersonnelLogEdgeGain;
    private JCheckBox chkDisplayPersonnelLog;
    private JCheckBox chkDisplayScenarioLog;
    private JCheckBox chkDisplayKillRecord;

    // Expanded Personnel
    private JCheckBox chkUseTimeInService;
    private MMComboBox<TimeInDisplayFormat> comboTimeInServiceDisplayFormat;
    private JCheckBox chkUseTimeInRank;
    private MMComboBox<TimeInDisplayFormat> comboTimeInRankDisplayFormat;
    private JCheckBox chkTrackTotalEarnings;
    private JCheckBox chkTrackTotalXPEarnings;
    private JCheckBox chkShowOriginFaction;

    // Admin
    private final JPanel administratorsPanel = new JPanel();
    private JCheckBox chkAdminsHaveNegotiation;
    private JCheckBox chkAdminsHaveScrounge;
    private JCheckBox chkAdminExperienceLevelIncludeNegotiation;
    private JCheckBox chkAdminExperienceLevelIncludeScrounge;

    // Medical
    private JCheckBox chkUseAdvancedMedical;
    private JSpinner spnHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JSpinner spnMinimumHitsForVehicles;
    private JCheckBox chkUseRandomHitsForVehicles;
    private JCheckBox chkUseTougherHealing;
    private JSpinner spnMaximumPatients;

    // Prisoners
    private MMComboBox<PrisonerCaptureStyle> comboPrisonerCaptureStyle;
    private MMComboBox<PrisonerStatus> comboPrisonerStatus;
    private JCheckBox chkPrisonerBabyStatus;
    private JCheckBox chkAtBPrisonerDefection;
    private JCheckBox chkAtBPrisonerRansom;

    // Dependent
    private JCheckBox chkUseRandomDependentAddition;
    private JCheckBox chkUseRandomDependentRemoval;

    // Personnel Removal
    private JPanel personnelRemovalSubPanel = new JPanel();
    private JCheckBox chkUsePersonnelRemoval;
    private JCheckBox chkUseRemovalExemptCemetery;
    private JCheckBox chkUseRemovalExemptRetirees;

    // Salary
    private JCheckBox chkDisableSecondaryRoleSalary;
    private JSpinner spnAntiMekSalary;
    private JSpinner spnSpecialistInfantrySalary;
    private Map<SkillLevel, JSpinner> spnSalaryExperienceMultipliers;
    private JSpinner[] spnBaseSalary;
    // endregion Personnel Tab

    // region Turnover and Retention Tab
    // Header Options
    private JCheckBox chkUseRandomRetirement;

    // Settings
    private final JPanel turnoverAndRetentionSettingsPanel = new JPanel();
    private JLabel lblTurnoverFixedTargetNumber;
    private JSpinner spnTurnoverFixedTargetNumber;
    private MMComboBox<TurnoverFrequency> comboTurnoverFrequency;
    private JCheckBox chkUseContractCompletionRandomRetirement;
    private JCheckBox chkUseRandomFounderTurnover;
    private JCheckBox chkUseFounderRetirement;
    private JCheckBox chkAeroRecruitsHaveUnits;
    private JCheckBox chkTrackOriginalUnit;
    private JCheckBox chkUseSubContractSoldiers;
    private JSpinner spnServiceContractDuration;
    private JSpinner spnServiceContractModifier;
    private JCheckBox chkPayBonusDefault;
    private JLabel lblPayBonusDefaultThreshold;
    private JSpinner spnPayBonusDefaultThreshold;

    // Modifiers
    private final JPanel turnoverAndRetentionModifiersPanel = new JPanel();
    private JCheckBox chkUseCustomRetirementModifiers;
    private JCheckBox chkUseFatigueModifiers;
    private JCheckBox chkUseSkillModifiers;
    private JCheckBox chkUseAgeModifiers;
    private JCheckBox chkUseUnitRatingModifiers;
    private JCheckBox chkUseFactionModifiers;
    private JCheckBox chkUseMissionStatusModifiers;
    private JCheckBox chkUseHostileTerritoryModifiers;
    private JCheckBox chkUseFamilyModifiers;
    private JCheckBox chkUseLoyaltyModifiers;

    private final JPanel loyaltySubPanel = new JPanel();
    private JCheckBox chkUseHideLoyalty;

    // Payout
    private final JPanel turnoverAndRetentionPayoutPanel = new JPanel();
    private JSpinner spnPayoutRateOfficer;
    private JSpinner spnPayoutRateEnlisted;
    private JSpinner spnPayoutRetirementMultiplier;
    private JCheckBox chkUsePayoutServiceBonus;

    private final JPanel payoutServiceBonusSubPanel = new JPanel();
    private JLabel lblPayoutServiceBonusRate;
    private JSpinner spnPayoutServiceBonusRate;

    // Unit Cohesion
    private final JPanel turnoverAndRetentionUnitCohesionPanel = new JPanel();
    private JCheckBox chkUseAdministrativeStrain;
    private JCheckBox chkUseManagementSkill;

    private final JPanel administrativeStrainSubPanel = new JPanel();
    private JLabel lblAdministrativeCapacity;
    private JSpinner spnAdministrativeCapacity;
    private JLabel lblMultiCrewStrainDivider;
    private JSpinner spnMultiCrewStrainDivider;

    private final JPanel managementSkillSubPanel = new JPanel();
    private JCheckBox chkUseCommanderLeadershipOnly;
    private JLabel lblManagementSkillPenalty;
    private JSpinner spnManagementSkillPenalty;

    // Fatigue
    private final JPanel turnoverAndRetentionFatiguePanel = new JPanel();
    private JCheckBox chkUseFatigue;

    private final JPanel fatigueSubPanel = new JPanel();
    private JSpinner spnFatigueRate;
    private JCheckBox chkUseInjuryFatigue;
    private JSpinner spnFieldKitchenCapacity;
    private JCheckBox chkFieldKitchenIgnoreNonCombatants;
    private JSpinner spnFatigueLeaveThreshold;
    // endregion Turnover and Retention Tab

    // region Life Paths Tab
    // Personnel Randomization
    private JCheckBox chkUseDylansRandomXP;
    private JSpinner spnNonBinaryDiceSize;

    // Random Histories
    private RandomOriginOptionsPanel randomOriginOptionsPanel;
    private JCheckBox chkUseRandomPersonalities;
    private JCheckBox chkUseRandomPersonalityReputation;
    private JCheckBox chkUseIntelligenceXpMultiplier;
    private JCheckBox chkUseSimulatedRelationships;

    // Marriage
    private JCheckBox chkUseManualMarriages;
    private JCheckBox chkUseClanPersonnelMarriages;
    private JCheckBox chkUsePrisonerMarriages;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JSpinner spnNoInterestInMarriageDiceSize;
    private JCheckBox chkLogMarriageNameChanges;
    private Map<MergingSurnameStyle, JSpinner> spnMarriageSurnameWeights;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomClanPersonnelMarriages;
    private JCheckBox chkUseRandomPrisonerMarriages;
    private JSpinner spnRandomMarriageAgeRange;
    private JSpinner spnRandomMarriageDiceSize;
    private JSpinner spnRandomSameSexMarriageDiceSize;
    private JSpinner spnRandomNewDependentMarriage;

    // Divorce
    private JCheckBox chkUseManualDivorce;
    private JCheckBox chkUseClanPersonnelDivorce;
    private JCheckBox chkUsePrisonerDivorce;
    private Map<SplittingSurnameStyle, JSpinner> spnDivorceSurnameWeights;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JCheckBox chkUseRandomOppositeSexDivorce;
    private JCheckBox chkUseRandomSameSexDivorce;
    private JCheckBox chkUseRandomClanPersonnelDivorce;
    private JCheckBox chkUseRandomPrisonerDivorce;
    private JLabel lblRandomDivorceDiceSize;
    private JSpinner spnRandomDivorceDiceSize;

    // Procreation
    private JCheckBox chkUseManualProcreation;
    private JCheckBox chkUseClanPersonnelProcreation;
    private JCheckBox chkUsePrisonerProcreation;
    private JSpinner spnMultiplePregnancyOccurrences;
    private MMComboBox<BabySurnameStyle> comboBabySurnameStyle;
    private JCheckBox chkAssignNonPrisonerBabiesFounderTag;
    private JCheckBox chkAssignChildrenOfFoundersFounderTag;
    private JCheckBox chkDetermineFatherAtBirth;
    private JCheckBox chkDisplayTrueDueDate;
    private JSpinner spnNoInterestInChildrenDiceSize;
    private JCheckBox chkUseMaternityLeave;
    private JCheckBox chkLogProcreation;
    private MMComboBox<RandomProcreationMethod> comboRandomProcreationMethod;
    private JCheckBox chkUseRelationshiplessRandomProcreation;
    private JCheckBox chkUseRandomClanPersonnelProcreation;
    private JCheckBox chkUseRandomPrisonerProcreation;
    private JSpinner spnRandomProcreationRelationshipDiceSize;
    private JLabel lblRandomProcreationRelationshiplessDiceSize;
    private JSpinner spnRandomProcreationRelationshiplessDiceSize;

    // Awards
    private MMComboBox<AwardBonus> comboAwardBonusStyle;
    private JSpinner spnAwardTierSize;
    private JCheckBox chkEnableAutoAwards;
    private JCheckBox chkIssuePosthumousAwards;
    private JCheckBox chkIssueBestAwardOnly;
    private JCheckBox chkIgnoreStandardSet;
    private JCheckBox chkEnableContractAwards;
    private JCheckBox chkEnableFactionHunterAwards;
    private JCheckBox chkEnableInjuryAwards;
    private JCheckBox chkEnableIndividualKillAwards;
    private JCheckBox chkEnableFormationKillAwards;
    private JCheckBox chkEnableRankAwards;
    private JCheckBox chkEnableScenarioAwards;
    private JCheckBox chkEnableSkillAwards;
    private JCheckBox chkEnableTheatreOfWarAwards;
    private JCheckBox chkEnableTimeAwards;
    private JCheckBox chkEnableTrainingAwards;
    private JCheckBox chkEnableMiscAwards;
    private JLabel lblAwardSetFilterList;
    private JTextArea txtAwardSetFilterList;

    // Death
    private JCheckBox chkKeepMarriedNameUponSpouseDeath;
    private MMComboBox<RandomDeathMethod> comboRandomDeathMethod;
    private Map<AgeGroup, JCheckBox> chkEnabledRandomDeathAgeGroups;
    private JCheckBox chkUseRandomClanPersonnelDeath;
    private JCheckBox chkUseRandomPrisonerDeath;
    private JCheckBox chkUseRandomDeathSuicideCause;
    private JSpinner spnPercentageRandomDeathChance;
    private JSpinner[] spnExponentialRandomDeathMaleValues;
    private JSpinner[] spnExponentialRandomDeathFemaleValues;
    private Map<TenYearAgeRange, JSpinner> spnAgeRangeRandomDeathMaleValues;
    private Map<TenYearAgeRange, JSpinner> spnAgeRangeRandomDeathFemaleValues;

    // Family
    private MMComboBox<FamilialRelationshipDisplayLevel> comboFamilyDisplayLevel;

    // Anniversaries
    private final JPanel anniversaryPanel = new JPanel();
    private JCheckBox chkAnnounceBirthdays;
    private JCheckBox chkAnnounceRecruitmentAnniversaries;
    private JCheckBox chkAnnounceOfficersOnly;
    private JCheckBox chkAnnounceChildBirthdays;

    // Education
    private JCheckBox chkUseEducationModule;
    private JLabel lblCurriculumXpRate;
    private JSpinner spnCurriculumXpRate;
    private JLabel lblMaximumJumpCount;
    private JSpinner spnMaximumJumpCount;
    private JCheckBox chkUseReeducationCamps;
    private JCheckBox chkEnableLocalAcademies;
    private JCheckBox chkEnablePrestigiousAcademies;
    private JCheckBox chkEnableUnitEducation;
    private JCheckBox chkShowIneligibleAcademies;
    private JCheckBox chkEnableOverrideRequirements;
    private JLabel lblEntranceExamBaseTargetNumber;
    private JSpinner spnEntranceExamBaseTargetNumber;
    private JCheckBox chkEnableBonuses;
    private JLabel lblFacultyXpMultiplier;
    private JSpinner spnFacultyXpMultiplier;
    private JLabel lblAdultDropoutChance;
    private JSpinner spnAdultDropoutChance;
    private JLabel lblChildrenDropoutChance;
    private JSpinner spnChildrenDropoutChance;
    private JCheckBox chkAllAges;
    private JLabel lblMilitaryAcademyAccidents;
    private JSpinner spnMilitaryAcademyAccidents;
    // endregion Life Paths Tab

    // region Finances Tab
    private JCheckBox payForPartsBox;
    private JCheckBox payForRepairsBox;
    private JCheckBox payForUnitsBox;
    private JCheckBox payForSalariesBox;
    private JCheckBox payForOverheadBox;
    private JCheckBox payForMaintainBox;
    private JCheckBox payForTransportBox;
    private JCheckBox sellUnitsBox;
    private JCheckBox sellPartsBox;
    private JCheckBox payForRecruitmentBox;
    private JCheckBox useLoanLimitsBox;
    private JCheckBox usePercentageMaintenanceBox;
    private JCheckBox useInfantryDoseNotCountBox;
    private JCheckBox usePeacetimeCostBox;
    private JCheckBox useExtendedPartsModifierBox;
    private JCheckBox showPeacetimeCostBox;
    private JCheckBox newFinancialYearFinancesToCSVExportBox;
    private MMComboBox<FinancialYearDuration> comboFinancialYearDuration;

    // Price Multipliers
    private JSpinner spnCommonPartPriceMultiplier;
    private JSpinner spnInnerSphereUnitPriceMultiplier;
    private JSpinner spnInnerSpherePartPriceMultiplier;
    private JSpinner spnClanUnitPriceMultiplier;
    private JSpinner spnClanPartPriceMultiplier;
    private JSpinner spnMixedTechUnitPriceMultiplier;
    private JSpinner[] spnUsedPartPriceMultipliers;
    private JSpinner spnDamagedPartsValueMultiplier;
    private JSpinner spnUnrepairablePartsValueMultiplier;
    private JSpinner spnCancelledOrderRefundMultiplier;

    // Taxes
    private JCheckBox chkUseTaxes;
    private JPanel taxesSubPanel = new JPanel();
    private JSpinner spnTaxesPercentage;

    private JCheckBox chkUseShareSystem;
    private JPanel sharesSubPanel;
    private JCheckBox chkSharesForAll;
    // endregion Finances Tab

    // region Mercenary Tab
    private JRadioButton btnContractEquipment;
    private JSpinner spnEquipPercent;
    private JSpinner spnDropShipPercent;
    private JSpinner spnJumpShipPercent;
    private JSpinner spnWarShipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JRadioButton btnContractPersonnel;
    private JCheckBox chkBLCSaleValue;
    private JCheckBox chkOverageRepaymentInFinalPayment;
    // endregion Mercenary Tab

    // region Experience Tab
    private JSpinner spnXpCostMultiplier;
    private JSpinner spnScenarioXP;
    private JSpinner spnKillXP;
    private JSpinner spnKills;
    private JSpinner spnTaskXP;
    private JSpinner spnNTasksXP;
    private JSpinner spnSuccessXP;
    private JSpinner spnMistakeXP;
    private JSpinner spnIdleXP;
    private JSpinner spnMonthsIdleXP;
    private JSpinner spnTargetIdleXP;
    private JSpinner spnContractNegotiationXP;
    private JSpinner spnAdminWeeklyXP;
    private JSpinner spnAdminWeeklyXPPeriod;
    private JSpinner spnMissionXpFail;
    private JSpinner spnMissionXpSuccess;
    private JSpinner spnMissionXpOutstandingSuccess;

    private JSpinner spnEdgeCost;
    private JTable tableXP;
    private static final String[] TABLE_XP_COLUMN_NAMES = { "+0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8",
            "+9",
            "+10" };
    // endregion Experience Tab

    // region Skills Tab
    // endregion Skills Tab

    // region Special Abilities Tab
    private AbstractMHQScrollablePanel panSpecialAbilities;
    private Map<String, SpecialAbility> tempSPA;
    private JButton btnAddSPA;
    // endregion Special Abilities Tab

    // region Skill Randomization Tab
    private JCheckBox chkExtraRandom;
    private JSpinner[] phenotypeSpinners;
    private JSpinner spnProbAntiMek;
    private JSpinner spnOverallRecruitBonus;
    private JSpinner[] spnTypeRecruitBonus;
    private JSpinner spnArtyProb;
    private JSpinner spnArtyBonus;
    private JSpinner spnTacticsGreen;
    private JSpinner spnTacticsReg;
    private JSpinner spnTacticsVet;
    private JSpinner spnTacticsElite;
    private JSpinner spnCombatSA;
    private JSpinner spnSupportSA;
    private JSpinner spnSecondProb;
    private JSpinner spnSecondBonus;
    private JSpinner spnAbilityGreen;
    private JSpinner spnAbilityReg;
    private JSpinner spnAbilityVet;
    private JSpinner spnAbilityElite;
    // endregion Skill Randomization Tab

    // region Rank System Tab
    private RankSystemsPane rankSystemsPane;
    // endregion Rank System Tab

    // region Name and Portrait Generation Tab
    private JCheckBox chkUseOriginFactionForNames;
    private MMComboBox<String> comboFactionNames;
    private JSlider sldGender;
    private JCheckBox[] chkUsePortrait;
    private JCheckBox allPortraitsBox;
    private JCheckBox noPortraitsBox;
    private JCheckBox chkAssignPortraitOnRoleChange;
    // endregion Name and Portrait Generation Tab

    // region Markets Tab
    // Personnel Market
    private MMComboBox<String> comboPersonnelMarketType;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private Map<SkillLevel, JSpinner> spnPersonnelMarketRandomRemovalTargets;
    private JSpinner spnPersonnelMarketDylansWeight;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;

    // Unit Market
    private MMComboBox<UnitMarketMethod> comboUnitMarketMethod;
    private JCheckBox chkUnitMarketRegionalMekVariations;
    private JLabel lblUnitMarketSpecialUnitChance;
    private JSpinner spnUnitMarketSpecialUnitChance;
    private JLabel lblUnitMarketRarityModifier;
    private JSpinner spnUnitMarketRarityModifier;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkUnitMarketReportRefresh;

    // Contract Market
    private JPanel contractMarketPanel;
    private MMComboBox<ContractMarketMethod> comboContractMarketMethod;
    private JSpinner spnContractSearchRadius;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkContractMarketReportRefresh;
    private JSpinner spnContractMaxSalvagePercentage;
    private JSpinner spnDropShipBonusPercentage;
    // endregion Markets Tab

    // region RATs Tab
    private JRadioButton btnUseRATGenerator;
    private JRadioButton btnUseStaticRATs;
    private DefaultListModel<String> chosenRATModel;
    private DefaultListModel<String> availableRATModel;
    private JCheckBox chkIgnoreRATEra;
    // endregion RATs Tab

    // region Against the Bot Tab
    private AbstractMHQScrollablePanel panAtB;
    private JCheckBox chkUseAtB;
    private JCheckBox chkUseStratCon;
    private MMComboBox<SkillLevel> comboSkillLevel;

    // unit administration
    private JCheckBox chkUseAero;
    private JCheckBox chkUseVehicles;
    private JCheckBox chkClanVehicles;
    private JCheckBox chkAutoConfigMunitions;

    // contract operations
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkRestrictPartsByMission;
    private JSpinner spnBonusPartExchangeValue;
    private JSpinner spnBonusPartMaxExchangeCount;
    private JCheckBox chkLimitLanceWeight;
    private JCheckBox chkLimitLanceNumUnits;
    private JCheckBox chkUseStrategy;
    private JSpinner spnBaseStrategyDeployment;
    private JSpinner spnAdditionalStrategyDeployment;
    private JCheckBox chkAdjustPaymentForStrategy;
    private JSpinner spnAtBBattleIntensity;
    private JSpinner[] spnAtBBattleChance;
    private JButton btnIntensityUpdate;
    private JCheckBox chkGenerateChases;

    // scenarios
    private JCheckBox chkUseGenericBattleValue;
    private JCheckBox chkUseVerboseBidding;
    private JCheckBox chkDoubleVehicles;
    private JSpinner spnOpForLanceTypeMeks;
    private JSpinner spnOpForLanceTypeMixed;
    private JSpinner spnOpForLanceTypeVehicles;
    private JCheckBox chkOpForUsesVTOLs;
    private JCheckBox chkOpForUsesAero;
    private JSpinner spnOpForAeroChance;
    private JCheckBox chkOpForUsesLocalForces;
    private JSpinner spnOpForLocalForceChance;
    private JSpinner spnSPAUpgradeIntensity;
    private JSpinner spnFixedMapChance;
    private JCheckBox chkAdjustPlayerVehicles;
    private JCheckBox chkRegionalMekVariations;
    private JCheckBox chkAttachedPlayerCamouflage;
    private JCheckBox chkPlayerControlsAttachedUnits;
    private JCheckBox chkUseDropShips;
    private JCheckBox chkUseWeatherConditions;
    private JCheckBox chkUseLightConditions;
    private JCheckBox chkUsePlanetaryConditions;
    private JSpinner spnScenarioModMax;
    private JSpinner spnScenarioModChance;
    private JSpinner spnScenarioModBV;
    // endregion Against the Bot Tab
    // endregion Variable Declarations

    // region Constructors
    public CampaignOptionsPane(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog",
                MekHQ.getMHQOptions().getLocale()),
                "CampaignOptionsPane");
        this.campaign = campaign;
        this.startup = startup;
        this.date = campaign.getLocalDate();
        this.camouflage = campaign.getCamouflage();
        this.colour = campaign.getColour();
        this.unitIcon = campaign.getUnitIcon();
        hashSkillTargets = new Hashtable<>();
        hashGreenSkill = new Hashtable<>();
        hashRegSkill = new Hashtable<>();
        hashVetSkill = new Hashtable<>();
        hashEliteSkill = new Hashtable<>();

        initialize();
        setOptions(campaign.getCampaignOptions(), campaign.getRandomSkillPreferences());
        btnCamo.setIcon(camouflage.getImageIcon());
        btnIcon.setIcon(unitIcon.getImageIcon(75));
    }
    // endregion Constructors

    // region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public boolean isStartup() {
        return startup;
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected void initialize() {
        addTab(resources.getString("generalPanel.title"), createGeneralTab());
        addTab(resources.getString("repairAndMaintenancePanel.title"), createRepairAndMaintenanceTab());
        addTab(resources.getString("suppliesAndAcquisitionsPanel.title"), createSuppliesAndAcquisitionsTab());
        addTab(resources.getString("techLimitsPanel.title"), createTechLimitsTab());
        addTab(resources.getString("personnelPanel.title"), createPersonnelTab());
        addTab(resources.getString("lifePathsPanel.title"), createLifePathsPanel());
        addTab(resources.getString("turnoverAndRetentionPanel.title"), createTurnoverAndRetentionTab());
        addTab(resources.getString("financesPanel.title"),
                createFinancesTab(campaign.getCampaignOptions().isReverseQualityNames()));
        addTab(resources.getString("mercenaryPanel.title"), createMercenaryTab());
        addTab(resources.getString("experiencePanel.title"), createExperienceTab());
        addTab(resources.getString("skillsPanel.title"), createSkillsTab());
        addTab(resources.getString("specialAbilitiesPanel.title"), createSpecialAbilitiesTab());
        addTab(resources.getString("skillRandomizationPanel.title"), createSkillRandomizationTab());
        addTab(resources.getString("rankSystemsPanel.title"), createRankSystemsTab());
        addTab(resources.getString("nameAndPortraitGenerationPanel.title"),
                createNameAndPortraitGenerationTab());
        addTab(resources.getString("marketsPanel.title"), createMarketsTab());
        addTab(resources.getString("ratPanel.title"), createRATTab());
        addTab(resources.getString("againstTheBotPanel.title"), createAgainstTheBotTab());

        setPreferences();
    }

    // region Legacy Initialization
    // Define what is legacy and not and remove in Milestone after 0.49.19

    private JScrollPane createGeneralTab() {
        int gridy = 0;
        int gridx = 0;

        AbstractMHQScrollablePanel panGeneral = new DefaultMHQScrollablePanel(getFrame(), "generalPanel",
                new GridBagLayout());

        JLabel lblName = new JLabel(resources.getString("lblName.text"));
        lblName.setName("lblName");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panGeneral.add(lblName, gridBagConstraints);

        txtName = new JTextField(campaign.getName());
        txtName.setName("txtName");
        txtName.setMinimumSize(new Dimension(500, 30));
        txtName.setPreferredSize(new Dimension(500, 30));
        gridBagConstraints.gridx = gridx--;
        panGeneral.add(txtName, gridBagConstraints);

        JButton lblNameGenerator = new JButton(resources.getString("lblNameGenerator.text"));
        lblNameGenerator.setName("lblNameGenerator");
        lblNameGenerator.addActionListener(e -> txtName
                .setText(BackgroundsController
                        .randomMercenaryCompanyNameGenerator(campaign.getFlaggedCommander())));
        gridBagConstraints = new GridBagConstraints();
        panGeneral.add(lblNameGenerator, gridBagConstraints);

        JLabel lblFaction = new JLabel(resources.getString("lblFaction.text"));
        lblFaction.setName("lblFaction");
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        panGeneral.add(lblFaction, gridBagConstraints);

        final DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(FactionDisplay
                .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), date));
        comboFaction = new MMComboBox<>("comboFaction", factionModel);
        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), date));
        comboFaction.setMinimumSize(new Dimension(400, 30));
        comboFaction.setPreferredSize(new Dimension(400, 30));
        gridBagConstraints.gridx = gridx--;
        panGeneral.add(comboFaction, gridBagConstraints);

        JPanel unitRatingPanel = new JPanel(new GridBagLayout());

        JLabel unitRatingMethodLabel = new JLabel(resources.getString("unitRatingMethodLabel.text"));
        unitRatingMethodLabel.setName("unitRatingMethodLabel");
        gridBagConstraints.gridx = 0;
        unitRatingPanel.add(unitRatingMethodLabel, gridBagConstraints);

        unitRatingMethodCombo = new MMComboBox<>("unitRatingMethodCombo", UnitRatingMethod.values());
        gridBagConstraints.gridx = 1;
        unitRatingPanel.add(unitRatingMethodCombo, gridBagConstraints);

        JLabel manualUnitRatingModifierLabel = new JLabel(
                resources.getString("manualUnitRatingModifierLabel.text"));
        gridBagConstraints.gridx = 2;
        unitRatingPanel.add(manualUnitRatingModifierLabel, gridBagConstraints);

        manualUnitRatingModifier = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
        gridBagConstraints.gridx = 3;
        unitRatingPanel.add(manualUnitRatingModifier, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panGeneral.add(unitRatingPanel, gridBagConstraints);

        JLabel lblDate = new JLabel(resources.getString("lblDate.text"));
        lblDate.setName("lblDate");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panGeneral.add(lblDate, gridBagConstraints);

        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.setName("btnDate");
        btnDate.setMinimumSize(new Dimension(400, 30));
        btnDate.setPreferredSize(new Dimension(400, 30));
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints.gridx = gridx--;
        panGeneral.add(btnDate, gridBagConstraints);

        JLabel lblCamo = new JLabel(resources.getString("lblCamo.text"));
        lblCamo.setName("lblCamo");
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        panGeneral.add(lblCamo, gridBagConstraints);

        btnCamo = new JButton();
        btnCamo.setName("btnCamo");
        btnCamo.setMinimumSize(new Dimension(84, 72));
        btnCamo.setPreferredSize(new Dimension(84, 72));
        btnCamo.setMaximumSize(new Dimension(84, 72));
        btnCamo.addActionListener(this::btnCamoActionPerformed);
        gridBagConstraints.gridx = gridx--;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panGeneral.add(btnCamo, gridBagConstraints);

        JLabel lblIcon = new JLabel(resources.getString("lblIcon.text"));
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panGeneral.add(lblIcon, gridBagConstraints);

        btnIcon = new JButton();
        btnIcon.setMinimumSize(new Dimension(84, 72));
        btnIcon.setPreferredSize(new Dimension(84, 72));
        btnIcon.setMaximumSize(new Dimension(84, 72));
        btnIcon.addActionListener(evt -> {
            final UnitIconDialog unitIconDialog = new UnitIconDialog(getFrame(), unitIcon);
            if (unitIconDialog.showDialog().isConfirmed() && (unitIconDialog.getSelectedItem() != null)) {
                unitIcon = unitIconDialog.getSelectedItem();
                btnIcon.setIcon(unitIcon.getImageIcon(75));
            }
        });
        gridBagConstraints.gridx = gridx--;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panGeneral.add(btnIcon, gridBagConstraints);

        return new JScrollPane(panGeneral);
    }

    private JScrollPane createRepairAndMaintenanceTab() {
        final AbstractMHQScrollablePanel panRepair = new DefaultMHQScrollablePanel(getFrame(),
                "repairAndMaintenancePanel", new GridBagLayout());

        JPanel panSubRepair = new JPanel(new GridBagLayout());
        JPanel panSubMaintenance = new JPanel(new GridBagLayout());

        panSubRepair.setBorder(BorderFactory.createTitledBorder(resources.getString("lblSubRepair.text")));
        panSubMaintenance.setBorder(
                BorderFactory.createTitledBorder(resources.getString("lblSubMaintenance.text")));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panRepair.add(panSubRepair, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panRepair.add(panSubMaintenance, gridBagConstraints);

        useEraModsCheckBox = new JCheckBox(resources.getString("useEraModsCheckBox.text"));
        useEraModsCheckBox.setToolTipText(resources.getString("useEraModsCheckBox.toolTipText"));
        useEraModsCheckBox.setName("useEraModsCheckBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(useEraModsCheckBox, gridBagConstraints);

        assignedTechFirstCheckBox = new JCheckBox(resources.getString("assignedTechFirstCheckBox.text"));
        assignedTechFirstCheckBox.setToolTipText(resources.getString("assignedTechFirstCheckBox.toolTipText"));
        assignedTechFirstCheckBox.setName("assignedTechFirstCheckBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(assignedTechFirstCheckBox, gridBagConstraints);

        resetToFirstTechCheckBox = new JCheckBox(resources.getString("resetToFirstTechCheckBox.text"));
        resetToFirstTechCheckBox.setToolTipText(resources.getString("resetToFirstTechCheckBox.toolTipText"));
        resetToFirstTechCheckBox.setName("resetToFirstTechCheckBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(resetToFirstTechCheckBox, gridBagConstraints);

        useQuirksBox = new JCheckBox(resources.getString("useQuirksBox.text"));
        useQuirksBox.setToolTipText(resources.getString("useQuirksBox.toolTipText"));
        useQuirksBox.setName("useQuirksBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(useQuirksBox, gridBagConstraints);

        useAeroSystemHitsBox = new JCheckBox(resources.getString("useAeroSystemHits.text"));
        useAeroSystemHitsBox.setToolTipText(resources.getString("useAeroSystemHits.toolTipText"));
        useAeroSystemHitsBox.setName("useAeroSystemHits");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(useAeroSystemHitsBox, gridBagConstraints);

        useDamageMargin = new JCheckBox(resources.getString("useDamageMargin.text"));
        useDamageMargin.setToolTipText(resources.getString("useDamageMargin.toolTipText"));
        useDamageMargin.addActionListener(evt -> spnDamageMargin.setEnabled(useDamageMargin.isSelected()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(useDamageMargin, gridBagConstraints);

        spnDamageMargin = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        ((DefaultEditor) spnDamageMargin.getEditor()).getTextField().setEditable(false);
        JPanel pnlDamageMargin = new JPanel();
        pnlDamageMargin.add(new JLabel(resources.getString("lblDamageMargin.text")));
        pnlDamageMargin.add(spnDamageMargin);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDamageMargin, gridBagConstraints);

        spnDestroyPartTarget = new JSpinner(new SpinnerNumberModel(2, 2, 13, 1));
        ((DefaultEditor) spnDestroyPartTarget.getEditor()).getTextField().setEditable(false);

        JPanel pnlDestroyPartTarget = new JPanel();
        pnlDestroyPartTarget.add(new JLabel(resources.getString("lblDestroyPartTarget.text")));
        pnlDestroyPartTarget.add(spnDestroyPartTarget);
        pnlDestroyPartTarget.add(new JLabel(resources.getString("lblDestroyPartTargetSuffix.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDestroyPartTarget, gridBagConstraints);

        checkMaintenance = new JCheckBox(resources.getString("checkMaintenance.text"));
        checkMaintenance.setToolTipText(resources.getString("checkMaintenance.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(checkMaintenance, gridBagConstraints);

        checkMaintenance.addActionListener(evt -> {
            if (checkMaintenance.isSelected()) {
                spnMaintenanceDays.setEnabled(true);
                useQualityMaintenance.setEnabled(true);
                useUnofficialMaintenance.setEnabled(true);
                reverseQualityNames.setEnabled(true);
                chkUseRandomUnitQualities.setEnabled(true);
                chkUsePlanetaryModifiers.setEnabled(true);
                spnMaintenanceBonus.setEnabled(true);
                logMaintenance.setEnabled(true);
                spnDefaultMaintenanceTime.setEnabled(true);
            } else {
                spnMaintenanceDays.setEnabled(false);
                useQualityMaintenance.setEnabled(false);
                useUnofficialMaintenance.setEnabled(false);
                reverseQualityNames.setEnabled(false);
                chkUseRandomUnitQualities.setEnabled(false);
                chkUsePlanetaryModifiers.setEnabled(false);
                spnMaintenanceBonus.setEnabled(false);
                logMaintenance.setEnabled(false);
                spnDefaultMaintenanceTime.setEnabled(false);
            }
        });

        spnMaintenanceDays = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
        ((DefaultEditor) spnMaintenanceDays.getEditor()).getTextField().setEditable(false);
        JPanel pnlMaintenanceDays = new JPanel();
        pnlMaintenanceDays.add(spnMaintenanceDays);
        pnlMaintenanceDays.add(new JLabel(resources.getString("lblMaintenanceDays.text")));
        pnlMaintenanceDays.setToolTipText(wordWrap(resources.getString("lblMaintenanceDays.toolTipText")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceDays, gridBagConstraints);

        spnMaintenanceBonus = new JSpinner(new SpinnerNumberModel(0, -13, 13, 1));
        ((DefaultEditor) spnMaintenanceBonus.getEditor()).getTextField().setEditable(false);
        spnMaintenanceBonus.setToolTipText(resources.getString("spnMaintenanceBonus.toolTipText"));

        JPanel pnlMaintenanceBonus = new JPanel();
        pnlMaintenanceBonus.add(spnMaintenanceBonus);
        pnlMaintenanceBonus.add(new JLabel(resources.getString("lblMaintenanceBonus.text")));

        spnDefaultMaintenanceTime = new JSpinner(new SpinnerNumberModel(4, 1, 4, 1));
        ((DefaultEditor) spnDefaultMaintenanceTime.getEditor()).getTextField().setEditable(false);
        spnDefaultMaintenanceTime.setToolTipText(resources.getString("lblDefaultMaintenanceTime.toolTipText"));

        JPanel pnlDefaultMaintenanceTime = new JPanel();
        pnlDefaultMaintenanceTime.add(spnDefaultMaintenanceTime);
        pnlDefaultMaintenanceTime.add(new JLabel(resources.getString("lblDefaultMaintenanceTime.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlDefaultMaintenanceTime, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceBonus, gridBagConstraints);

        useQualityMaintenance = new JCheckBox(resources.getString("useQualityMaintenance.text"));
        useQualityMaintenance.setToolTipText(resources.getString("useQualityMaintenance.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useQualityMaintenance, gridBagConstraints);

        reverseQualityNames = new JCheckBox(resources.getString("reverseQualityNames.text"));
        reverseQualityNames.setToolTipText(resources.getString("reverseQualityNames.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(reverseQualityNames, gridBagConstraints);

        reverseQualityNames.addActionListener(evt -> recreateFinancesPanel(reverseQualityNames.isSelected()));

        chkUseRandomUnitQualities = new JCheckBox(resources.getString("chkUseRandomUnitQualities.text"));
        chkUseRandomUnitQualities.setToolTipText(resources.getString("chkUseRandomUnitQualities.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(chkUseRandomUnitQualities, gridBagConstraints);

        chkUsePlanetaryModifiers = new JCheckBox(resources.getString("chkUsePlanetaryModifiers.text"));
        chkUsePlanetaryModifiers.setToolTipText(resources.getString("chkUsePlanetaryModifiers.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(chkUsePlanetaryModifiers, gridBagConstraints);

        useUnofficialMaintenance = new JCheckBox(resources.getString("useUnofficialMaintenance.text"));
        useUnofficialMaintenance.setToolTipText(resources.getString("useUnofficialMaintenance.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useUnofficialMaintenance, gridBagConstraints);

        logMaintenance = new JCheckBox(resources.getString("logMaintenance.text"));
        logMaintenance.setToolTipText(resources.getString("logMaintenance.toolTipText"));
        logMaintenance.setName("logMaintenance");
        gridBagConstraints.gridy = 9;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panSubMaintenance.add(logMaintenance, gridBagConstraints);

        return new JScrollPane(panRepair);
    }

    private JScrollPane createSuppliesAndAcquisitionsTab() {
        final AbstractMHQScrollablePanel panSupplies = new DefaultMHQScrollablePanel(getFrame(),
                "suppliesAndAcquisitionsPanel", new GridBagLayout());

        JPanel panSubAcquire = new JPanel(new GridBagLayout());
        JPanel panSubDelivery = new JPanel(new GridBagLayout());
        JPanel panSubPlanetAcquire = new JPanel(new GridBagLayout());

        panSubAcquire.setBorder(BorderFactory.createTitledBorder(resources.getString("lblSubAcquire.text")));
        panSubDelivery.setBorder(BorderFactory.createTitledBorder(resources.getString("lblSubDelivery.text")));
        panSubPlanetAcquire
                .setBorder(BorderFactory
                        .createTitledBorder(resources.getString("lblSubPlanetAcquire.text")));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panSupplies.add(panSubAcquire, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panSupplies.add(panSubDelivery, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panSupplies.add(panSubPlanetAcquire, gridBagConstraints);

        spnAcquireWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        ((DefaultEditor) spnAcquireWaitingPeriod.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panSubAcquire.add(new JLabel(resources.getString("lblAcquireSkill.text")), gridBagConstraints);

        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();
        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);
        choiceAcquireSkill = new MMComboBox<>("choiceAcquireSkill", acquireSkillModel);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panSubAcquire.add(choiceAcquireSkill, gridBagConstraints);

        chkSupportStaffOnly = new JCheckBox(resources.getString("lblSupportStaffOnly.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(chkSupportStaffOnly, gridBagConstraints);

        spnAcquireClanPenalty = new JSpinner(new SpinnerNumberModel(0, 0, 13, 1));
        ((DefaultEditor) spnAcquireClanPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlClanPenalty = new JPanel();
        pnlClanPenalty.add(spnAcquireClanPenalty);
        pnlClanPenalty.add(new JLabel(resources.getString("lblAcquireClanPenalty.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlClanPenalty, gridBagConstraints);

        spnAcquireIsPenalty = new JSpinner(new SpinnerNumberModel(0, 0, 13, 1));
        ((DefaultEditor) spnAcquireIsPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlIsPenalty = new JPanel();
        pnlIsPenalty.add(spnAcquireIsPenalty);
        pnlIsPenalty.add(new JLabel(resources.getString("lblAcquireIsPenalty.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlIsPenalty, gridBagConstraints);

        JPanel pnlWaitingPeriod = new JPanel();
        pnlWaitingPeriod.add(spnAcquireWaitingPeriod);
        pnlWaitingPeriod.add(new JLabel(resources.getString("lblWaitingPeriod.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        panSubAcquire.add(pnlWaitingPeriod, gridBagConstraints);
        spnMaxAcquisitions = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnMaxAcquisitions.setName("spnMaxAcquisitions");
        spnMaxAcquisitions.setToolTipText(resources.getString("spnMaxAcquisitions.toolTipText"));

        JPanel pnlMaxAcquisitions = new JPanel();
        pnlMaxAcquisitions.add(spnMaxAcquisitions);
        pnlMaxAcquisitions.add(new JLabel(resources.getString("lblMaxAcquisitions.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlMaxAcquisitions, gridBagConstraints);

        spnNDiceTransitTime = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((DefaultEditor) spnNDiceTransitTime.getEditor()).getTextField().setEditable(false);

        spnConstantTransitTime = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((DefaultEditor) spnConstantTransitTime.getEditor()).getTextField().setEditable(false);

        spnAcquireMosBonus = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((DefaultEditor) spnAcquireMosBonus.getEditor()).getTextField().setEditable(false);

        spnAcquireMinimum = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((DefaultEditor) spnAcquireMinimum.getEditor()).getTextField().setEditable(false);

        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }

        choiceTransitTimeUnits = new MMComboBox<>("choiceTransitTimeUnits", transitUnitModel);

        DefaultComboBoxModel<String> transitMosUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMosUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }

        choiceAcquireMosUnits = new MMComboBox<>("choiceAcquireMosUnits", transitMosUnitModel);

        DefaultComboBoxModel<String> transitMinUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMinUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }

        choiceAcquireMinimumUnit = new MMComboBox<>("choiceAcquireMinimumUnit", transitMinUnitModel);

        JPanel pnlTransitTime = new JPanel();
        pnlTransitTime.add(new JLabel(resources.getString("lblTransitTime.text")));
        pnlTransitTime.add(spnNDiceTransitTime);
        pnlTransitTime.add(new JLabel("d6 + "));
        pnlTransitTime.add(spnConstantTransitTime);
        pnlTransitTime.add(choiceTransitTimeUnits);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlTransitTime, gridBagConstraints);

        JPanel pnlMinTransit = new JPanel();
        pnlMinTransit.add(new JLabel(resources.getString("lblMinTransit.text")));
        pnlMinTransit.add(spnAcquireMinimum);
        pnlMinTransit.add(choiceAcquireMinimumUnit);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlMinTransit, gridBagConstraints);

        JPanel pnlMosBonus = new JPanel();
        pnlMosBonus.add(new JLabel(resources.getString("lblMosBonus.text")));
        pnlMosBonus.add(spnAcquireMosBonus);
        pnlMosBonus.add(choiceAcquireMosUnits);
        pnlMosBonus.add(new JLabel(resources.getString("lblMosBonusSuffix.text")));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlMosBonus, gridBagConstraints);

        usePlanetaryAcquisitions = new JCheckBox(resources.getString("usePlanetaryAcquisitions.text"));
        usePlanetaryAcquisitions.setToolTipText(resources.getString("usePlanetaryAcquisitions.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(usePlanetaryAcquisitions, gridBagConstraints);

        spnMaxJumpPlanetaryAcquisitions = new JSpinner(new SpinnerNumberModel(2, 0, 5, 1));
        JPanel panMaxJump = new JPanel();
        panMaxJump.add(spnMaxJumpPlanetaryAcquisitions);
        panMaxJump.add(new JLabel(resources.getString("lblMaxJumpPlanetaryAcquisitions.text")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panMaxJump, gridBagConstraints);

        comboPlanetaryAcquisitionsFactionLimits = new MMComboBox<>("comboPlanetaryAcquisitionsFactionLimits",
                PlanetaryAcquisitionFactionLimit.values());
        JPanel panFactionLimit = new JPanel();
        panFactionLimit.add(new JLabel(resources.getString("lblPlanetaryAcquisitionsFactionLimits.text")));
        panFactionLimit.add(comboPlanetaryAcquisitionsFactionLimits);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panFactionLimit, gridBagConstraints);

        disallowPlanetaryAcquisitionClanCrossover = new JCheckBox(
                resources.getString("disallowPlanetaryAcquisitionClanCrossover.text"));
        disallowPlanetaryAcquisitionClanCrossover
                .setToolTipText(resources
                        .getString("disallowPlanetaryAcquisitionClanCrossover.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(disallowPlanetaryAcquisitionClanCrossover, gridBagConstraints);

        disallowClanPartsFromIS = new JCheckBox(resources.getString("disallowClanPartsFromIS.text"));
        disallowClanPartsFromIS.setToolTipText(resources.getString("disallowClanPartsFromIS.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(disallowClanPartsFromIS, gridBagConstraints);

        spnPenaltyClanPartsFromIS = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        JPanel panPenaltyClanPartsFromIS = new JPanel();
        panPenaltyClanPartsFromIS.add(spnPenaltyClanPartsFromIS);
        JLabel lblPenaltyClanPartsFromIS = new JLabel(resources.getString("spnPenaltyClanPartsFromIS.text"));
        lblPenaltyClanPartsFromIS.setToolTipText(resources.getString("spnPenaltyClanPartsFromIS.toolTipText"));
        panPenaltyClanPartsFromIS.add(lblPenaltyClanPartsFromIS);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panPenaltyClanPartsFromIS, gridBagConstraints);

        usePlanetaryAcquisitionsVerbose = new JCheckBox(
                resources.getString("usePlanetaryAcquisitionsVerbose.text"));
        usePlanetaryAcquisitionsVerbose
                .setToolTipText(resources.getString("usePlanetaryAcquisitionsVerbose.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(usePlanetaryAcquisitionsVerbose, gridBagConstraints);

        JPanel panSocioIndustrialBonus = new JPanel();
        panSocioIndustrialBonus.setLayout(new BoxLayout(panSocioIndustrialBonus, BoxLayout.LINE_AXIS));
        panSocioIndustrialBonus
                .setBorder(BorderFactory.createTitledBorder(
                        resources.getString("lblSocioIndustrialBonusPanel.text")));

        JPanel panTechBonus = new JPanel(new GridBagLayout());
        JPanel panIndustryBonus = new JPanel(new GridBagLayout());
        JPanel panOutputBonus = new JPanel(new GridBagLayout());

        spnPlanetAcquireTechBonus = new JSpinner[6];
        spnPlanetAcquireIndustryBonus = new JSpinner[6];
        spnPlanetAcquireOutputBonus = new JSpinner[6];

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        panTechBonus.add(new JLabel(resources.getString("lblTechBonus.text")), gridBagConstraints);
        panIndustryBonus.add(new JLabel(resources.getString("lblIndustryBonus.text")), gridBagConstraints);
        panOutputBonus.add(new JLabel(resources.getString("lblOutputBonus.text")), gridBagConstraints);
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.insets = new Insets(0, 20, 0, 0);
            panTechBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            panIndustryBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            panOutputBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            spnPlanetAcquireTechBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            spnPlanetAcquireIndustryBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            spnPlanetAcquireOutputBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            ((DefaultEditor) spnPlanetAcquireTechBonus[i].getEditor()).getTextField().setEditable(false);
            ((DefaultEditor) spnPlanetAcquireIndustryBonus[i].getEditor()).getTextField()
                    .setEditable(false);
            ((DefaultEditor) spnPlanetAcquireOutputBonus[i].getEditor()).getTextField().setEditable(false);

            panTechBonus.add(spnPlanetAcquireTechBonus[i], gridBagConstraints);
            panOutputBonus.add(spnPlanetAcquireOutputBonus[i], gridBagConstraints);
            panIndustryBonus.add(spnPlanetAcquireIndustryBonus[i], gridBagConstraints);
        }

        panSocioIndustrialBonus.add(panTechBonus);
        panSocioIndustrialBonus.add(panIndustryBonus);
        panSocioIndustrialBonus.add(panOutputBonus);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panSocioIndustrialBonus, gridBagConstraints);

        return new JScrollPane(panSupplies);
    }

    private JScrollPane createTechLimitsTab() {
        int gridy = 0;

        final AbstractMHQScrollablePanel panTech = new DefaultMHQScrollablePanel(getFrame(), "techLimitsPanel",
                new GridBagLayout());

        limitByYearBox = new JCheckBox(resources.getString("limitByYearBox.text"));
        limitByYearBox.setToolTipText(resources.getString("limitByYearBox.toolTipText"));
        limitByYearBox.setName("limitByYearBox");
        limitByYearBox.addActionListener(e -> variableTechLevelBox.setEnabled(limitByYearBox.isSelected()));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(limitByYearBox, gridBagConstraints);

        disallowExtinctStuffBox = new JCheckBox(resources.getString("disallowExtinctStuffBox.text"));
        disallowExtinctStuffBox.setToolTipText(resources.getString("disallowExtinctStuffBox.toolTipText"));
        disallowExtinctStuffBox.setName("disallowExtinctStuffBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(disallowExtinctStuffBox, gridBagConstraints);

        allowClanPurchasesBox = new JCheckBox(resources.getString("allowClanPurchasesBox.text"));
        allowClanPurchasesBox.setToolTipText(resources.getString("allowClanPurchasesBox.toolTipText"));
        allowClanPurchasesBox.setName("allowClanPurchasesBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(allowClanPurchasesBox, gridBagConstraints);

        allowISPurchasesBox = new JCheckBox(resources.getString("allowISPurchasesBox.text"));
        allowISPurchasesBox.setToolTipText(resources.getString("allowISPurchasesBox.toolTipText"));
        allowISPurchasesBox.setName("allowISPurchasesBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(allowISPurchasesBox, gridBagConstraints);

        allowCanonOnlyBox = new JCheckBox(resources.getString("allowCanonOnlyBox.text"));
        allowCanonOnlyBox.setToolTipText(resources.getString("allowCanonOnlyBox.toolTipText"));
        allowCanonOnlyBox.setName("allowCanonOnlyBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonOnlyBox, gridBagConstraints);

        allowCanonRefitOnlyBox = new JCheckBox(resources.getString("allowCanonRefitOnlyBox.text"));
        allowCanonRefitOnlyBox.setToolTipText(resources.getString("allowCanonRefitOnlyBox.toolTipText"));
        allowCanonRefitOnlyBox.setName("allowCanonRefitOnlyBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonRefitOnlyBox, gridBagConstraints);

        JLabel lblTechLevel = new JLabel(resources.getString("lblTechLevel.text"));
        lblTechLevel.setName("lblTechLevel");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panTech.add(lblTechLevel, gridBagConstraints);

        DefaultComboBoxModel<String> techLevelComboBoxModel = new DefaultComboBoxModel<>();
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_INTRO));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_STANDARD));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_ADVANCED));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_EXPERIMENTAL));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_UNOFFICIAL));
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", techLevelComboBoxModel);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(choiceTechLevel, gridBagConstraints);

        variableTechLevelBox = new JCheckBox(resources.getString("variableTechLevelBox.text"));
        variableTechLevelBox.setToolTipText(resources.getString("variableTechLevelBox.toolTipText"));
        variableTechLevelBox.setName("variableTechLevelBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(variableTechLevelBox, gridBagConstraints);

        factionIntroDateBox = new JCheckBox(resources.getString("factionIntroDateBox.text"));
        factionIntroDateBox.setToolTipText(resources.getString("factionIntroDateBox.toolTipText"));
        factionIntroDateBox.setName("factionIntroDateBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(factionIntroDateBox, gridBagConstraints);

        useAmmoByTypeBox = new JCheckBox(resources.getString("useAmmoByTypeBox.text"));
        useAmmoByTypeBox.setToolTipText(resources.getString("useAmmoByTypeBox.toolTipText"));
        useAmmoByTypeBox.setName("useAmmoByTypeBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panTech.add(useAmmoByTypeBox, gridBagConstraints);

        return new JScrollPane(panTech);
    }

    private JScrollPane createFinancesTab(boolean reverseQualities) {
        int gridy = 0;

        AbstractMHQScrollablePanel panFinances = new DefaultMHQScrollablePanel(getFrame(),
                "financesPanel", new GridBagLayout());

        payForPartsBox = new JCheckBox(resources.getString("payForPartsBox.text"));
        payForPartsBox.setToolTipText(resources.getString("payForPartsBox.toolTipText"));
        payForPartsBox.setName("payForPartsBox");

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForPartsBox, gridBagConstraints);

        payForRepairsBox = new JCheckBox(resources.getString("payForRepairsBox.text"));
        payForRepairsBox.setToolTipText(resources.getString("payForRepairsBox.toolTipText"));
        payForRepairsBox.setName("payForRepairsBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForRepairsBox, gridBagConstraints);

        payForUnitsBox = new JCheckBox(resources.getString("payForUnitsBox.text"));
        payForUnitsBox.setToolTipText(resources.getString("payForUnitsBox.toolTipText"));
        payForUnitsBox.setName("payForUnitsBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForUnitsBox, gridBagConstraints);

        payForSalariesBox = new JCheckBox(resources.getString("payForSalariesBox.text"));
        payForSalariesBox.setToolTipText(resources.getString("payForSalariesBox.toolTipText"));
        payForSalariesBox.setName("payForSalariesBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForSalariesBox, gridBagConstraints);

        payForOverheadBox = new JCheckBox(resources.getString("payForOverheadBox.text"));
        payForOverheadBox.setToolTipText(resources.getString("payForOverheadBox.toolTipText"));
        payForOverheadBox.setName("payForOverheadBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForOverheadBox, gridBagConstraints);

        payForMaintainBox = new JCheckBox(resources.getString("payForMaintainBox.text"));
        payForMaintainBox.setToolTipText(resources.getString("payForMaintainBox.toolTipText"));
        payForMaintainBox.setName("payForMaintainBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForMaintainBox, gridBagConstraints);

        payForTransportBox = new JCheckBox(resources.getString("payForTransportBox.text"));
        payForTransportBox.setToolTipText(resources.getString("payForTransportBox.toolTipText"));
        payForTransportBox.setName("payForTransportBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForTransportBox, gridBagConstraints);

        sellUnitsBox = new JCheckBox(resources.getString("sellUnitsBox.text"));
        sellUnitsBox.setToolTipText(resources.getString("sellUnitsBox.toolTipText"));
        sellUnitsBox.setName("sellUnitsBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(sellUnitsBox, gridBagConstraints);

        sellPartsBox = new JCheckBox(resources.getString("sellPartsBox.text"));
        sellPartsBox.setToolTipText(resources.getString("sellPartsBox.toolTipText"));
        sellPartsBox.setName("sellPartsBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(sellPartsBox, gridBagConstraints);

        payForRecruitmentBox = new JCheckBox(resources.getString("payForRecruitmentBox.text"));
        payForRecruitmentBox.setToolTipText(resources.getString("payForRecruitmentBox.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(payForRecruitmentBox, gridBagConstraints);

        useLoanLimitsBox = new JCheckBox(resources.getString("useLoanLimitsBox.text"));
        useLoanLimitsBox.setToolTipText(resources.getString("useLoanLimitsBox.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(useLoanLimitsBox, gridBagConstraints);

        // Unofficial maintenance costs
        usePercentageMaintenanceBox = new JCheckBox(resources.getString("usePercentageMaintenanceBox.text"));
        usePercentageMaintenanceBox
                .setToolTipText(resources.getString("usePercentageMaintenanceBox.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(usePercentageMaintenanceBox, gridBagConstraints);

        // Unofficial infantry doesn't count for contract pay
        useInfantryDoseNotCountBox = new JCheckBox(resources.getString("infantryDontCount.text"));
        useInfantryDoseNotCountBox.setToolTipText(resources.getString("infantryDontCount.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(useInfantryDoseNotCountBox, gridBagConstraints);

        // Campaign Operations Peacetime operating costs
        usePeacetimeCostBox = new JCheckBox(resources.getString("usePeacetimeCostBox.text"));
        usePeacetimeCostBox.setToolTipText(resources.getString("usePeacetimeCostBox.toolTipText"));
        usePeacetimeCostBox.setName("usePeacetimeCostBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(usePeacetimeCostBox, gridBagConstraints);

        useExtendedPartsModifierBox = new JCheckBox(resources.getString("useExtendedPartsModifierBox.text"));
        useExtendedPartsModifierBox.setName("useExtendedPartsModifierBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(useExtendedPartsModifierBox, gridBagConstraints);

        showPeacetimeCostBox = new JCheckBox(resources.getString("showPeacetimeCostBox.text"));
        showPeacetimeCostBox.setToolTipText(resources.getString("showPeacetimeCostBox.toolTipText"));
        showPeacetimeCostBox.setName("showPeacetimeCostBox");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(showPeacetimeCostBox, gridBagConstraints);

        DefaultComboBoxModel<FinancialYearDuration> financialYearDurationModel = new DefaultComboBoxModel<>(
                FinancialYearDuration.values());
        comboFinancialYearDuration = new MMComboBox<>("comboFinancialYearDuration", financialYearDurationModel);
        comboFinancialYearDuration.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FinancialYearDuration) {
                    list.setToolTipText(((FinancialYearDuration) value).getToolTipText());
                }
                return this;
            }
        });
        JPanel pnlFinancialYearDuration = new JPanel();
        pnlFinancialYearDuration.add(new JLabel(resources.getString("financialYearDuration.text")));
        pnlFinancialYearDuration.setToolTipText(resources.getString("financialYearDuration.toolTipText"));
        pnlFinancialYearDuration.add(comboFinancialYearDuration);
        gridBagConstraints.gridy = gridy++;
        panFinances.add(pnlFinancialYearDuration, gridBagConstraints);

        newFinancialYearFinancesToCSVExportBox = new JCheckBox(
                resources.getString("newFinancialYearFinancesToCSVExportBox.text"));
        newFinancialYearFinancesToCSVExportBox
                .setToolTipText(resources
                        .getString("newFinancialYearFinancesToCSVExportBox.toolTipText"));
        newFinancialYearFinancesToCSVExportBox.setName("newFinancialYearFinancesToCSVExportBox");
        gridBagConstraints.gridy = gridy++;
        panFinances.add(newFinancialYearFinancesToCSVExportBox, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 20;
        panFinances.add(createPriceModifiersPanel(reverseQualities), gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        panFinances.add(createTaxesPanel(), gridBagConstraints);

        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 20;
        panFinances.add(createSharesPanel(), gridBagConstraints);

        return new JScrollPane(panFinances);
    }

    private JScrollPane createMercenaryTab() {
        final AbstractMHQScrollablePanel panMercenary = new DefaultMHQScrollablePanel(getFrame(),
                "mercenaryPanel", new GridBagLayout());

        btnContractEquipment = new JRadioButton(resources.getString("panMercenary.IntOpsPayment.title"));
        btnContractEquipment.setToolTipText(resources.getString("panMercenary.IntOpsPayment.tooltip"));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractEquipment, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel(resources.getString("lblEquipPercent.text")), gridBagConstraints);

        spnEquipPercent = new JSpinner(
                new SpinnerNumberModel(0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
                        0.1));
        spnEquipPercent.setEditor(new NumberEditor(spnEquipPercent, "0.0"));
        ((DefaultEditor) spnEquipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnEquipPercent, gridBagConstraints);

        chkEquipContractSaleValue = new JCheckBox(resources.getString("chkEquipContractSaleValue.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(chkEquipContractSaleValue, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel(resources.getString("lblDropShipPercent.text")), gridBagConstraints);

        spnDropShipPercent = new JSpinner(
                new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT,
                        0.1));
        spnDropShipPercent.setEditor(new NumberEditor(spnDropShipPercent, "0.0"));
        ((NumberEditor) spnDropShipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnDropShipPercent, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel(resources.getString("lblJumpShipPercent.text")), gridBagConstraints);

        spnJumpShipPercent = new JSpinner(
                new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT,
                        0.1));
        spnJumpShipPercent.setEditor(new NumberEditor(spnJumpShipPercent, "0.0"));
        ((DefaultEditor) spnJumpShipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnJumpShipPercent, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel(resources.getString("lblWarShipPercent.text")), gridBagConstraints);

        spnWarShipPercent = new JSpinner(
                new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_WARSHIP_EQUIPMENT_PERCENT,
                        0.1));
        spnWarShipPercent.setEditor(new NumberEditor(spnWarShipPercent, "0.0"));
        ((DefaultEditor) spnWarShipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnWarShipPercent, gridBagConstraints);

        btnContractPersonnel = new JRadioButton(resources.getString("panMercenary.FMMRPayment.title"));
        btnContractPersonnel.setToolTipText(resources.getString("panMercenary.FMMRPayment.tooltip"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractPersonnel, gridBagConstraints);

        chkBLCSaleValue = new JCheckBox(resources.getString("lblBLCSaleValue.text"));
        gridBagConstraints.gridy = 6;
        panMercenary.add(chkBLCSaleValue, gridBagConstraints);

        chkOverageRepaymentInFinalPayment = new JCheckBox(
                resources.getString("chkOverageRepaymentInFinalPayment.text"));
        chkOverageRepaymentInFinalPayment
                .setToolTipText(resources.getString("chkOverageRepaymentInFinalPayment.toolTipText"));
        gridBagConstraints.gridy = 7;
        panMercenary.add(chkOverageRepaymentInFinalPayment, gridBagConstraints);

        ButtonGroup groupContract = new ButtonGroup();
        groupContract.add(btnContractEquipment);
        groupContract.add(btnContractPersonnel);

        return new JScrollPane(panMercenary);
    }

    private JScrollPane createExperienceTab() {
        final AbstractMHQScrollablePanel panXP = new DefaultMHQScrollablePanel(getFrame(),
                "experiencePanel", new GridBagLayout());

        JLabel lblXpCostMultiplier = new JLabel(resources.getString("lblXpCostMultiplier.text"));
        lblXpCostMultiplier.setToolTipText(resources.getString("lblXpCostMultiplier.tooltip"));
        spnXpCostMultiplier = new JSpinner(new SpinnerNumberModel(1, 0, 5, 0.05));
        ((DefaultEditor) spnXpCostMultiplier.getEditor()).getTextField().setEditable(false);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnXpCostMultiplier, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblXpCostMultiplier, gridBagConstraints);

        JLabel lblScenarioXP = new JLabel(resources.getString("lblScenarioXP.text"));
        spnScenarioXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnScenarioXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnScenarioXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblScenarioXP, gridBagConstraints);

        JLabel lblKillXP = new JLabel(resources.getString("lblXPForEvery.text"));
        spnKillXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnKillXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnKillXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblKillXP, gridBagConstraints);

        JLabel lblKills = new JLabel(resources.getString("lblKills.text"));
        spnKills = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnKills.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnKills, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblKills, gridBagConstraints);

        JLabel lblTaskXP = new JLabel(resources.getString("lblXPForEvery.text"));
        spnTaskXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnTaskXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnTaskXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblTaskXP, gridBagConstraints);

        JLabel lblTasks = new JLabel(resources.getString("lblTasks.text"));
        spnNTasksXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnNTasksXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnNTasksXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblTasks, gridBagConstraints);

        JLabel lblSuccessXp = new JLabel(resources.getString("lblSuccessXP.text"));
        spnSuccessXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnSuccessXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnSuccessXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblSuccessXp, gridBagConstraints);

        JLabel lblMistakeXP = new JLabel(resources.getString("lblMistakeXP.text"));
        spnMistakeXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnMistakeXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMistakeXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblMistakeXP, gridBagConstraints);

        spnIdleXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("lblXPForEvery.text")), gridBagConstraints);

        spnMonthsIdleXP = new JSpinner(new SpinnerNumberModel(0, 0, 36, 1));
        ((DefaultEditor) spnMonthsIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMonthsIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("lblTargetIdleXP.text")), gridBagConstraints);

        spnTargetIdleXP = new JSpinner(new SpinnerNumberModel(2, 2, 13, 1));
        ((DefaultEditor) spnTargetIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnTargetIdleXP, gridBagConstraints);

        spnContractNegotiationXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnContractNegotiationXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnContractNegotiationXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("lblContractNegotiationXP.text")), gridBagConstraints);

        spnAdminWeeklyXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnAdminWeeklyXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("lblAdminWeeklyXP.text")), gridBagConstraints);

        spnAdminWeeklyXPPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        ((DefaultEditor) spnAdminWeeklyXPPeriod.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXPPeriod, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("lblAdminWeeklyXPPeriod.text")), gridBagConstraints);

        spnMissionXpFail = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        ((DefaultEditor) spnMissionXpFail.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMissionXpFail, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("spnMissionXpFail.text")), gridBagConstraints);

        spnMissionXpSuccess = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        ((DefaultEditor) spnMissionXpSuccess.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMissionXpSuccess, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("spnMissionXpSuccess.text")), gridBagConstraints);

        spnMissionXpOutstandingSuccess = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        ((DefaultEditor) spnMissionXpOutstandingSuccess.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMissionXpOutstandingSuccess, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("spnMissionXpOutstandingSuccess.text")), gridBagConstraints);

        spnEdgeCost = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((DefaultEditor) spnEdgeCost.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnEdgeCost, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel(resources.getString("lblEdgeCost.text")), gridBagConstraints);

        final JTextArea txtInstructionsXP = new JTextArea(resources.getString("txtInstructionsXP.text"));
        txtInstructionsXP.setName("txtInstructions");
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setLineWrap(true);
        txtInstructionsXP.setWrapStyleWord(true);
        txtInstructionsXP.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("txtInstructionsXP.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructionsXP.setOpaque(false);
        txtInstructionsXP.setMinimumSize(new Dimension(550, 120));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(txtInstructionsXP, gridBagConstraints);

        tableXP = new JTable(getSkillCostsArray(SkillType.getSkillHash()), TABLE_XP_COLUMN_NAMES);
        tableXP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableXP.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableXP.setRowHeight(25);
        tableXP.setRowSelectionAllowed(false);
        tableXP.setColumnSelectionAllowed(false);
        tableXP.setCellSelectionEnabled(true);
        tableXP.setShowGrid(true);
        final JScrollPane scrXP = new JScrollPane(tableXP);
        scrXP.setMinimumSize(new Dimension(500, ((tableXP.getRowCount() * 25) + 50)));
        scrXP.setPreferredSize(new Dimension(500, ((tableXP.getRowCount() * 25) + 50)));
        JTable rowTable = new RowNamesTable(tableXP);
        rowTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        rowTable.setShowGrid(true);
        rowTable.setRowHeight(25);
        scrXP.setRowHeaderView(rowTable);
        scrXP.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(scrXP, gridBagConstraints);

        return new JScrollPane(panXP);
    }

    private JScrollPane createSkillsTab() {
        final AbstractMHQScrollablePanel panSkill = new DefaultMHQScrollablePanel(getFrame(), "skillsPanel",
                new GridBagLayout());

        JPanel skPanel;

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        GridBagConstraints c;
        JSpinner spnTarget;
        JSpinner spnGreen;
        JSpinner spnReg;
        JSpinner spnVet;
        JSpinner spnElite;
        SkillType type;
        JLabel lblSkill;
        for (final String skillName : SkillType.getSkillList()) {
            type = SkillType.getType(skillName);
            skPanel = new JPanel();
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(5, 5, 5, 5);
            lblSkill = new JLabel(resources.getString("lblSkillTarget.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnTarget = new JSpinner(new SpinnerNumberModel(type.getTarget(), 0, 12, 1));
            ((DefaultEditor) spnTarget.getEditor()).getTextField().setEditable(false);
            hashSkillTargets.put(skillName, spnTarget);
            skPanel.add(spnTarget, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillGreen.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnGreen = new JSpinner(new SpinnerNumberModel(type.getGreenLevel(), 0, 10, 1));
            ((DefaultEditor) spnGreen.getEditor()).getTextField().setEditable(false);
            hashGreenSkill.put(skillName, spnGreen);
            skPanel.add(spnGreen, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillRegular.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnReg = new JSpinner(new SpinnerNumberModel(type.getRegularLevel(), 0, 10, 1));
            ((DefaultEditor) spnReg.getEditor()).getTextField().setEditable(false);
            hashRegSkill.put(skillName, spnReg);
            skPanel.add(spnReg, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillVeteran.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnVet = new JSpinner(new SpinnerNumberModel(type.getVeteranLevel(), 0, 10, 1));
            ((DefaultEditor) spnVet.getEditor()).getTextField().setEditable(false);
            hashVetSkill.put(skillName, spnVet);
            skPanel.add(spnVet, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillElite.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnElite = new JSpinner(new SpinnerNumberModel(type.getEliteLevel(), 0, 10, 1));
            ((DefaultEditor) spnElite.getEditor()).getTextField().setEditable(false);
            hashEliteSkill.put(skillName, spnElite);
            skPanel.add(spnElite, c);
            c.gridx++;

            skPanel.setBorder(BorderFactory.createTitledBorder(skillName));
            panSkill.add(skPanel, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        final JScrollPane scrSkill = new JScrollPane(panSkill);
        scrSkill.setPreferredSize(new Dimension(500, 400));
        return scrSkill;
    }

    private JScrollPane createSpecialAbilitiesTab() {
        panSpecialAbilities = new DefaultMHQScrollablePanel(getFrame(), "specialAbilitiesPanel",
                new GridBagLayout());

        Set<String> spaNames = SpecialAbility.getSpecialAbilities().keySet();
        // We need to create a temporary hash of special abilities that we can modify
        // without
        // changing the underlying one in case the user cancels the changes
        tempSPA = new Hashtable<>();
        for (final String name : spaNames) {
            getCurrentSPA().put(name, SpecialAbility.getAbility(name).clone());
        }

        btnAddSPA = new JButton("Add Another Special Ability");
        btnAddSPA.addActionListener(evt -> btnAddSPA());

        recreateSPAPanel(!getUnusedSPA().isEmpty());

        JScrollPane scrSPA = new JScrollPane(panSpecialAbilities);
        scrSPA.setPreferredSize(new Dimension(500, 400));
        return scrSPA;
    }

    private JScrollPane createSkillRandomizationTab() {
        GridBagConstraints gridBagConstraints;

        final AbstractMHQScrollablePanel panRandomSkill = new DefaultMHQScrollablePanel(getFrame(),
                "skillRandomizationPanel", new GridBagLayout());

        JPanel panRollTable = new JPanel(new GridLayout(6, 3, 5, 0));
        panRollTable.add(new JLabel("<html><b>Value</b></html>"));
        panRollTable.add(new JLabel("<html><b>Level</b></html>"));
        panRollTable.add(new JLabel("<html><b># Random SPAs</b></html>"));
        panRollTable.add(new JLabel("less than 2"));
        JLabel lblUltraGreen = new JLabel(SkillLevel.ULTRA_GREEN.toString());
        lblUltraGreen.setToolTipText(resources.getString("lblUltraGreen.toolTipText"));
        panRollTable.add(lblUltraGreen);
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("2-5"));
        panRollTable.add(new JLabel(SkillLevel.GREEN.toString()));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("6-9"));
        panRollTable.add(new JLabel(SkillLevel.REGULAR.toString()));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("10-11"));
        panRollTable.add(new JLabel(SkillLevel.VETERAN.toString()));
        panRollTable.add(new JLabel("1"));
        panRollTable.add(new JLabel("12 or more"));
        panRollTable.add(new JLabel(SkillLevel.ELITE.toString()));
        panRollTable.add(new JLabel("2"));
        panRollTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2d6 + Bonus"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JLabel lblOverallRecruitBonus = new JLabel(resources.getString("lblOverallRecruitBonus.text"));
        chkExtraRandom = new JCheckBox(resources.getString("chkExtraRandom.text"));
        chkExtraRandom.setToolTipText(resources.getString("chkExtraRandom.toolTipText"));
        JLabel lblProbAntiMek = new JLabel(resources.getString("lblProbAntiMek.text"));
        spnProbAntiMek = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((DefaultEditor) spnProbAntiMek.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
        ((DefaultEditor) spnOverallRecruitBonus.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus.setToolTipText(resources.getString("spnOverallRecruitBonus.toolTipText"));

        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        spnTypeRecruitBonus = new JSpinner[personnelRoles.length];
        int nRow = (int) Math.ceil(personnelRoles.length / 4.0);
        JPanel panTypeRecruitBonus = new JPanel(new GridLayout(nRow, 4));
        JSpinner spin;
        JPanel panRecruit;
        for (final PersonnelRole role : personnelRoles) {
            panRecruit = new JPanel(new GridBagLayout());

            spin = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            ((DefaultEditor) spin.getEditor()).getTextField().setEditable(false);
            spnTypeRecruitBonus[role.ordinal()] = spin;
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(2, 5, 0, 0);
            panRecruit.add(spin, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            panRecruit.add(new JLabel(role.toString()), gridBagConstraints);

            panTypeRecruitBonus.add(panRecruit);
        }

        panTypeRecruitBonus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("panTypeRecruitBonus.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(chkExtraRandom, gridBagConstraints);

        // Phenotype Percentage Generation
        List<Phenotype> phenotypes = Phenotype.getExternalPhenotypes();
        phenotypeSpinners = new JSpinner[phenotypes.size()];

        JPanel phenotypesPanel = new JPanel(new GridLayout((int) Math.ceil(phenotypes.size() / 2.0), 2));
        phenotypesPanel.setBorder(
                BorderFactory.createTitledBorder(resources.getString("lblPhenotypesPanel.text")));

        for (int i = 0; i < phenotypes.size(); i++) {
            JSpinner phenotypeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            phenotypeSpinners[i] = phenotypeSpinner;
            JPanel phenotypePanel = new JPanel();
            phenotypePanel.add(phenotypeSpinner);
            phenotypePanel.add(new JLabel(phenotypes.get(i).getName()));
            phenotypePanel.setToolTipText(phenotypes.get(i).getToolTipText());
            phenotypesPanel.add(phenotypePanel);
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(phenotypesPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(panRollTable, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(spnProbAntiMek, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(lblProbAntiMek, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(spnOverallRecruitBonus, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(lblOverallRecruitBonus, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(panTypeRecruitBonus, gridBagConstraints);

        JPanel panArtillery = new JPanel();
        panArtillery.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("lblArtillerySkill.text")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        spnArtyProb = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((DefaultEditor) spnArtyProb.getEditor()).getTextField().setEditable(false);
        spnArtyProb.setToolTipText(resources.getString("spnArtyProb.toolTipText"));
        panArtillery.add(spnArtyProb);
        panArtillery.add(new JLabel("Probability"));
        spnArtyBonus = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnArtyBonus.getEditor()).getTextField().setEditable(false);
        panArtillery.add(spnArtyBonus);
        panArtillery.add(new JLabel("Bonus"));
        JPanel panSecondary = new JPanel();
        spnSecondProb = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((DefaultEditor) spnSecondProb.getEditor()).getTextField().setEditable(false);
        spnSecondProb.setToolTipText(resources.getString("spnSecondProb.toolTipText"));
        panSecondary.add(spnSecondProb);
        panSecondary.add(new JLabel("Probability"));
        spnSecondBonus = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnSecondBonus.getEditor()).getTextField().setEditable(false);
        panSecondary.add(spnSecondBonus);
        panSecondary.add(new JLabel("Bonus"));
        panSecondary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("lblSecondarySkills.text")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panTactics = new JPanel();
        spnTacticsGreen = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnTacticsGreen.getEditor()).getTextField().setEditable(false);
        spnTacticsGreen.setToolTipText(resources.getString("spnTacticsGreen.toolTipText"));
        spnTacticsReg = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnTacticsReg.getEditor()).getTextField().setEditable(false);
        spnTacticsReg.setToolTipText(resources.getString("spnTacticsReg.toolTipText"));
        spnTacticsVet = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnTacticsVet.getEditor()).getTextField().setEditable(false);
        spnTacticsVet.setToolTipText(resources.getString("spnTacticsVet.toolTipText"));
        spnTacticsElite = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnTacticsElite.getEditor()).getTextField().setEditable(false);
        spnTacticsElite.setToolTipText(resources.getString("spnTacticsElite.toolTipText"));
        panTactics.add(spnTacticsGreen);
        panTactics.add(new JLabel("Green"));
        panTactics.add(spnTacticsReg);
        panTactics.add(new JLabel("Reg"));
        panTactics.add(spnTacticsVet);
        panTactics.add(new JLabel("Vet"));
        panTactics.add(spnTacticsElite);
        panTactics.add(new JLabel("Elite"));
        panTactics.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("lblTacticsSkill.text")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panSmallArms = new JPanel();
        spnCombatSA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnCombatSA.getEditor()).getTextField().setEditable(false);
        spnCombatSA.setToolTipText(resources.getString("spnCombatSA.toolTipText"));
        spnSupportSA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnSupportSA.getEditor()).getTextField().setEditable(false);
        spnSupportSA.setToolTipText(resources.getString("spnSupportSA.toolTipText"));
        panSmallArms.add(spnCombatSA);
        panSmallArms.add(new JLabel(resources.getString("lblCombatPersonnel.text")));
        panSmallArms.add(spnSupportSA);
        panSmallArms.add(new JLabel(resources.getString("lblSupportPersonnel.text")));
        panSmallArms.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("lblSmallArmsSkill.text")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panAbilities = new JPanel();
        spnAbilityGreen = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnAbilityGreen.getEditor()).getTextField().setEditable(false);
        spnAbilityGreen.setToolTipText(resources.getString("spnAbilityGreen.toolTipText"));
        spnAbilityReg = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnAbilityReg.getEditor()).getTextField().setEditable(false);
        spnAbilityReg.setToolTipText(resources.getString("spnAbilityReg.toolTipText"));
        spnAbilityVet = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnAbilityVet.getEditor()).getTextField().setEditable(false);
        spnAbilityVet.setToolTipText(resources.getString("spnAbilityVet.toolTipText"));
        spnAbilityElite = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((DefaultEditor) spnAbilityElite.getEditor()).getTextField().setEditable(false);
        spnAbilityElite.setToolTipText(resources.getString("spnAbilityElite.toolTipText"));
        panAbilities.add(spnAbilityGreen);
        panAbilities.add(new JLabel("Green"));
        panAbilities.add(spnAbilityReg);
        panAbilities.add(new JLabel("Reg"));
        panAbilities.add(spnAbilityVet);
        panAbilities.add(new JLabel("Vet"));
        panAbilities.add(spnAbilityElite);
        panAbilities.add(new JLabel("Elite"));
        panAbilities.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("lblSpecialAbilities.text")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JPanel panOtherBonuses = new JPanel(new GridLayout(3, 2));
        panOtherBonuses.add(panArtillery);
        panOtherBonuses.add(panSecondary);
        panOtherBonuses.add(panTactics);
        panOtherBonuses.add(panAbilities);
        panOtherBonuses.add(panSmallArms);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(panOtherBonuses, gridBagConstraints);

        JScrollPane scrRandomSkill = new JScrollPane(panRandomSkill);
        scrRandomSkill.setPreferredSize(new Dimension(500, 400));
        return scrRandomSkill;
    }

    private JScrollPane createNameAndPortraitGenerationTab() {
        int gridy = 0;

        final AbstractMHQScrollablePanel panNameGen = new DefaultMHQScrollablePanel(getFrame(),
                "nameAndPortraitGenerationPanel", new GridBagLayout());

        chkUseOriginFactionForNames = new JCheckBox(resources.getString("chkUseOriginFactionForNames.text"));
        chkUseOriginFactionForNames
                .setToolTipText(resources.getString("chkUseOriginFactionForNames.toolTipText"));
        chkUseOriginFactionForNames.setName("chkUseOriginFactionForNames");
        chkUseOriginFactionForNames.addActionListener(
                evt -> comboFactionNames.setEnabled(!chkUseOriginFactionForNames.isSelected()));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panNameGen.add(chkUseOriginFactionForNames, gridBagConstraints);

        JLabel lblFactionNames = new JLabel(resources.getString("lblFactionNames.text"));
        lblFactionNames.setName("lblFactionNames");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(lblFactionNames, gridBagConstraints);

        DefaultComboBoxModel<String> factionNamesModel = new DefaultComboBoxModel<>();
        for (final String faction : RandomNameGenerator.getInstance().getFactions()) {
            factionNamesModel.addElement(faction);
        }
        factionNamesModel.setSelectedItem(RandomNameGenerator.getInstance().getChosenFaction());
        comboFactionNames = new MMComboBox<>("comboFactionNames", factionNamesModel);
        comboFactionNames.setMinimumSize(new Dimension(400, 30));
        comboFactionNames.setPreferredSize(new Dimension(400, 30));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(comboFactionNames, gridBagConstraints);

        JLabel lblGender = new JLabel(resources.getString("lblGender.text"));
        lblGender.setName("lblGender");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(lblGender, gridBagConstraints);

        sldGender = new JSlider(SwingConstants.HORIZONTAL, 0, 100, RandomGenderGenerator.getPercentFemale());
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        panNameGen.add(sldGender, gridBagConstraints);

        final JPanel panRandomPortrait = new JPanel();
        panRandomPortrait.setName("panRandomPortrait");
        panRandomPortrait.setLayout(new BorderLayout());

        // The math below is used to determine how to split the personnel role options
        // for portraits,
        // which it does into 4 columns with rows equal to the number of roles plus two,
        // with the
        // additional two being the all role and no role options.
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        JPanel panUsePortrait = new JPanel(
                new GridLayout((int) Math.ceil((personnelRoles.length + 2) / 4.0), 4));
        chkUsePortrait = new JCheckBox[personnelRoles.length];
        allPortraitsBox = new JCheckBox(resources.getString("panUsePortrait.all.text"));
        noPortraitsBox = new JCheckBox(resources.getString("panUsePortrait.no.text"));
        allPortraitsBox.addActionListener(evt -> {
            final boolean selected = allPortraitsBox.isSelected();
            for (final JCheckBox box : chkUsePortrait) {
                if (selected) {
                    box.setSelected(true);
                }
                box.setEnabled(!selected);
            }
            if (selected) {
                noPortraitsBox.setSelected(false);
            }
        });
        noPortraitsBox.addActionListener(evt -> {
            final boolean selected = noPortraitsBox.isSelected();
            for (final JCheckBox box : chkUsePortrait) {
                if (selected) {
                    box.setSelected(false);
                }
                box.setEnabled(!selected);
            }
            if (selected) {
                allPortraitsBox.setSelected(false);
            }
        });
        panUsePortrait.add(allPortraitsBox);
        panUsePortrait.add(noPortraitsBox);

        JCheckBox box;
        for (final PersonnelRole role : PersonnelRole.values()) {
            box = new JCheckBox(role.toString());
            panUsePortrait.add(box);
            chkUsePortrait[role.ordinal()] = box;
        }

        panRandomPortrait.add(panUsePortrait, BorderLayout.CENTER);
        JTextArea txtPortraitInst = new JTextArea(resources.getString("txtPortraitInst.text"));
        txtPortraitInst.setPreferredSize(new Dimension(728, 60));
        txtPortraitInst.setEditable(false);
        txtPortraitInst.setLineWrap(true);
        txtPortraitInst.setWrapStyleWord(true);
        txtPortraitInst.setOpaque(false);
        panRandomPortrait.add(txtPortraitInst, BorderLayout.PAGE_START);

        panRandomPortrait.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("panRandomPortrait.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panNameGen.add(panRandomPortrait, gridBagConstraints);

        chkAssignPortraitOnRoleChange = new JCheckBox(
                resources.getString("chkAssignPortraitOnRoleChange.text"));
        chkAssignPortraitOnRoleChange
                .setToolTipText(resources.getString("chkAssignPortraitOnRoleChange.toolTipText"));
        chkAssignPortraitOnRoleChange.setName("chkAssignPortraitOnRoleChange");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(chkAssignPortraitOnRoleChange, gridBagConstraints);

        return new JScrollPane(panNameGen);
    }

    private JScrollPane createAgainstTheBotTab() {
        panAtB = new DefaultMHQScrollablePanel(getFrame(), "atbPanel", new GridBagLayout());

        JPanel panSubAtBAdmin = new JPanel(new GridBagLayout());
        JPanel panSubAtBContract = new JPanel(new GridBagLayout());
        JPanel panSubAtBScenario = new JPanel(new GridBagLayout());
        panSubAtBAdmin.setBorder(BorderFactory.createTitledBorder(resources.getString("lblSubAtbAdmin.text")));
        panSubAtBContract.setBorder(
                BorderFactory.createTitledBorder(resources.getString("lblSubAtBContract.text")));
        panSubAtBScenario.setBorder(
                BorderFactory.createTitledBorder(resources.getString("lblSubAtBScenario.text")));

        chkUseAtB = new JCheckBox(resources.getString("chkUseAtB.text"));
        chkUseAtB.setToolTipText(resources.getString("chkUseAtB.toolTipText"));
        chkUseAtB.setSelected(true);
        chkUseAtB.addActionListener(evt -> {
            final boolean enabled = chkUseAtB.isSelected();
            enableAtBComponents(panAtB, enabled);

            // TODO : AbstractContractMarket : Delink more from AtB
//            if (contractMarketPanel.isEnabled() != enabled) {
//                comboContractMarketMethod.setSelectedItem(enabled
//                        ? ContractMarketMethod.ATB_MONTHLY
//                        : ContractMarketMethod.NONE);
//                contractMarketPanel.setEnabled(enabled);
//                comboContractMarketMethod.setEnabled(true);
//            }
        });
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(10, 10, 10, 10);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panAtB.add(chkUseAtB, gridBagConstraints);

        JLabel lblSkillLevel = new JLabel(resources.getString("lblSkillLevel.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        panAtB.add(lblSkillLevel, gridBagConstraints);

        final DefaultComboBoxModel<SkillLevel> skillLevelModel = new DefaultComboBoxModel<>(
                Skills.SKILL_LEVELS);
        skillLevelModel.removeElement(SkillLevel.NONE); // we don't want this as a standard use case for the
                                                        // skill level
        comboSkillLevel = new MMComboBox<>("comboSkillLevel", skillLevelModel);
        comboSkillLevel.setToolTipText(resources.getString("lblSkillLevel.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        panAtB.add(comboSkillLevel, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panAtB.add(panSubAtBAdmin, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        panAtB.add(panSubAtBScenario, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        panAtB.add(panSubAtBContract, gridBagConstraints);

        chkUseStratCon = new JCheckBox(resources.getString("chkUseStratCon.text"));
        chkUseStratCon.setToolTipText(resources.getString("chkUseStratCon.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        panAtB.add(chkUseStratCon, gridBagConstraints);

        // AtB options: "Unit Administration" frame controls
        chkUseAero = new JCheckBox(resources.getString("chkUseAero.text"));
        chkUseAero.setToolTipText(resources.getString("chkUseAero.toolTipText"));
        gridBagConstraints.gridy = 0;
        panSubAtBAdmin.add(chkUseAero, gridBagConstraints);

        chkUseVehicles = new JCheckBox(resources.getString("chkUseVehicles.text"));
        chkUseVehicles.setToolTipText(resources.getString("chkUseVehicles.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseVehicles, gridBagConstraints);

        chkClanVehicles = new JCheckBox(resources.getString("chkClanVehicles.text"));
        chkClanVehicles.setToolTipText(resources.getString("chkClanVehicles.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkClanVehicles, gridBagConstraints);

        chkAutoConfigMunitions = new JCheckBox(resources.getString("chkAutoConfigMunitions.text"));
        chkAutoConfigMunitions.setToolTipText(resources.getString("chkAutoConfigMunitions.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAutoConfigMunitions, gridBagConstraints);

        chkMercSizeLimited = new JCheckBox(resources.getString("chkMercSizeLimited.text"));
        chkMercSizeLimited.setToolTipText(resources.getString("chkMercSizeLimited.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkMercSizeLimited, gridBagConstraints);

        chkRestrictPartsByMission = new JCheckBox(resources.getString("chkRestrictPartsByMission.text"));
        chkRestrictPartsByMission.setToolTipText(resources.getString("chkRestrictPartsByMission.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkRestrictPartsByMission, gridBagConstraints);

        JLabel lblBonusPartExchangeValue = new JLabel(resources.getString("lblBonusPartExchangeValue.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblBonusPartExchangeValue, gridBagConstraints);

        spnBonusPartExchangeValue = new JSpinner(new SpinnerNumberModel(500000, 0, 1000000, 1));
        spnBonusPartExchangeValue.setToolTipText(resources.getString("lblBonusPartExchangeValue.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        panSubAtBContract.add(spnBonusPartExchangeValue, gridBagConstraints);

        JLabel lblBonusPartMaxExchangeCount = new JLabel(resources.getString("lblBonusPartMaxExchangeCount.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblBonusPartMaxExchangeCount, gridBagConstraints);

        spnBonusPartMaxExchangeCount = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        spnBonusPartMaxExchangeCount
                .setToolTipText(resources.getString("lblBonusPartMaxExchangeCount.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        panSubAtBContract.add(spnBonusPartMaxExchangeCount, gridBagConstraints);

        chkLimitLanceWeight = new JCheckBox(resources.getString("chkLimitLanceWeight.text"));
        chkLimitLanceWeight.setToolTipText(resources.getString("chkLimitLanceWeight.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceWeight, gridBagConstraints);

        chkLimitLanceNumUnits = new JCheckBox(resources.getString("chkLimitLanceNumUnits.text"));
        chkLimitLanceNumUnits.setToolTipText(resources.getString("chkLimitLanceNumUnits.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceNumUnits, gridBagConstraints);

        JLabel lblLanceStructure = new JLabel(resources.getString("lblLanceStructure.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblLanceStructure, gridBagConstraints);

        chkUseStrategy = new JCheckBox(resources.getString("chkUseStrategy.text"));
        chkUseStrategy.setToolTipText(resources.getString("chkUseStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkUseStrategy, gridBagConstraints);

        JLabel lblBaseStrategyDeployment = new JLabel(resources.getString("lblBaseStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblBaseStrategyDeployment, gridBagConstraints);

        spnBaseStrategyDeployment = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        spnBaseStrategyDeployment.setToolTipText(resources.getString("spnBaseStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        panSubAtBContract.add(spnBaseStrategyDeployment, gridBagConstraints);

        JLabel lblAdditionalStrategyDeployment = new JLabel(
                resources.getString("lblAdditionalStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblAdditionalStrategyDeployment, gridBagConstraints);

        spnAdditionalStrategyDeployment = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        spnAdditionalStrategyDeployment
                .setToolTipText(resources.getString("spnAdditionalStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        panSubAtBContract.add(spnAdditionalStrategyDeployment, gridBagConstraints);

        chkAdjustPaymentForStrategy = new JCheckBox(resources.getString("chkAdjustPaymentForStrategy.text"));
        chkAdjustPaymentForStrategy.setName("chkAdjustPaymentForStrategy");
        chkAdjustPaymentForStrategy
                .setToolTipText(resources.getString("chkAdjustPaymentForStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkAdjustPaymentForStrategy, gridBagConstraints);

        JLabel lblIntensity = new JLabel(resources.getString("lblIntensity.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        panSubAtBContract.add(lblIntensity, gridBagConstraints);

        // Note that spnAtBBattleIntensity is located here visibly, however must be
        // initialized
        // following the chance of battle by role

        JLabel lblBattleFrequency = new JLabel(resources.getString("lblBattleFrequency.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(lblBattleFrequency, gridBagConstraints);

        spnAtBBattleChance = new JSpinner[AtBLanceRole.values().length - 1];

        JLabel lblFightChance = new JLabel(AtBLanceRole.FIGHTING + ":");
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblFightChance, gridBagConstraints);

        JSpinner atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblDefendChance = new JLabel(AtBLanceRole.DEFENCE + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        panSubAtBContract.add(lblDefendChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblScoutChance = new JLabel(AtBLanceRole.SCOUTING + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        panSubAtBContract.add(lblScoutChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblTrainingChance = new JLabel(AtBLanceRole.TRAINING + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        panSubAtBContract.add(lblTrainingChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        btnIntensityUpdate = new JButton(resources.getString("btnIntensityUpdate.text"));
        AtBBattleIntensityChangeListener atBBattleIntensityChangeListener = new AtBBattleIntensityChangeListener();
        btnIntensityUpdate.addChangeListener(evt -> {
            spnAtBBattleIntensity.removeChangeListener(atBBattleIntensityChangeListener);
            spnAtBBattleIntensity.setValue(determineAtBBattleIntensity());
            spnAtBBattleIntensity.addChangeListener(atBBattleIntensityChangeListener);
        });
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(btnIntensityUpdate, gridBagConstraints);

        // Note that this must be after the chance by role because it requires the
        // chance by role
        // for the initial value to be calculated
        spnAtBBattleIntensity = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
        spnAtBBattleIntensity.setToolTipText(resources.getString("spnIntensity.toolTipText"));
        spnAtBBattleIntensity.addChangeListener(atBBattleIntensityChangeListener);
        spnAtBBattleIntensity.setMinimumSize(new Dimension(60, 25));
        spnAtBBattleIntensity.setPreferredSize(new Dimension(60, 25));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(spnAtBBattleIntensity, gridBagConstraints);

        chkGenerateChases = new JCheckBox(resources.getString("chkGenerateChases.text"));
        chkGenerateChases.setName("chkGenerateChases");
        chkGenerateChases.setToolTipText(resources.getString("chkGenerateChases.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        panSubAtBContract.add(chkGenerateChases, gridBagConstraints);

        int yTablePosition = 0;
        chkUseGenericBattleValue = new JCheckBox(resources.getString("chkUseGenericBattleValue.text"));
        chkUseGenericBattleValue.setToolTipText(wordWrap(resources.getString("chkUseGenericBattleValue.toolTipText")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseGenericBattleValue, gridBagConstraints);

        chkUseVerboseBidding = new JCheckBox(resources.getString("chkUseVerboseBidding.text"));
        chkUseVerboseBidding.setToolTipText(wordWrap(resources.getString("chkUseVerboseBidding.toolTipText")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseVerboseBidding, gridBagConstraints);

        chkDoubleVehicles = new JCheckBox(resources.getString("chkDoubleVehicles.text"));
        chkDoubleVehicles.setToolTipText(resources.getString("chkDoubleVehicles.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkDoubleVehicles, gridBagConstraints);

        JLabel lblOpForLanceType = new JLabel(resources.getString("lblOpForLanceType.text"));
        lblOpForLanceType.setToolTipText(resources.getString("lblOpForLanceType.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(lblOpForLanceType, gridBagConstraints);

        spnOpForLanceTypeMeks = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpForLanceTypeMeks.setToolTipText(resources.getString("lblOpForLanceType.toolTipText"));
        spnOpForLanceTypeMixed = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpForLanceTypeMixed.setToolTipText(resources.getString("lblOpForLanceType.toolTipText"));
        spnOpForLanceTypeVehicles = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpForLanceTypeVehicles.setToolTipText(resources.getString("lblOpForLanceType.toolTipText"));
        JPanel panOpForLanceType = new JPanel();
        panOpForLanceType.add(new JLabel(resources.getString("lblOpForLanceTypeMek.text")));
        panOpForLanceType.add(spnOpForLanceTypeMeks);
        panOpForLanceType.add(new JLabel(resources.getString("lblOpForLanceTypeMixed.text")));
        panOpForLanceType.add(spnOpForLanceTypeMixed);
        panOpForLanceType.add(new JLabel(resources.getString("lblOpForLanceTypeVehicle.text")));
        panOpForLanceType.add(spnOpForLanceTypeVehicles);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpForLanceType, gridBagConstraints);

        chkOpForUsesVTOLs = new JCheckBox(resources.getString("chkOpForUsesVTOLs.text"));
        chkOpForUsesVTOLs.setToolTipText(resources.getString("chkOpForUsesVTOLs.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkOpForUsesVTOLs, gridBagConstraints);

        JPanel panOpForAero = new JPanel();
        chkOpForUsesAero = new JCheckBox(resources.getString("chkOpForUsesAero.text"));
        chkOpForUsesAero.setToolTipText(resources.getString("chkOpForUsesAero.toolTipText"));
        JLabel lblOpForAeroChance = new JLabel(resources.getString("lblOpForAeroLikelihood.text"));
        lblOpForAeroChance.setToolTipText(resources.getString("lblOpForAeroLikelihood.toolTipText"));
        spnOpForAeroChance = new JSpinner(new SpinnerNumberModel(0, 0, 6, 1));
        panOpForAero.add(chkOpForUsesAero);
        panOpForAero.add(spnOpForAeroChance);
        panOpForAero.add(lblOpForAeroChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpForAero, gridBagConstraints);

        JPanel panOpForLocal = new JPanel();
        chkOpForUsesLocalForces = new JCheckBox(resources.getString("chkOpForUsesLocalForces.text"));
        chkOpForUsesLocalForces.setToolTipText(resources.getString("chkOpForUsesLocalForces.toolTipText"));
        JLabel lblOpForLocalForceChance = new JLabel(resources.getString("lblOpForLocalForceLikelihood.text"));
        lblOpForLocalForceChance
                .setToolTipText(resources.getString("lblOpForLocalForceLikelihood.toolTipText"));
        spnOpForLocalForceChance = new JSpinner(new SpinnerNumberModel(0, 0, 6, 1));
        panOpForLocal.add(chkOpForUsesLocalForces);
        panOpForLocal.add(spnOpForLocalForceChance);
        panOpForLocal.add(lblOpForLocalForceChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpForLocal, gridBagConstraints);

        chkAdjustPlayerVehicles = new JCheckBox(resources.getString("chkAdjustPlayerVehicles.text"));
        chkAdjustPlayerVehicles.setToolTipText(resources.getString("chkAdjustPlayerVehicles.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkAdjustPlayerVehicles, gridBagConstraints);

        chkRegionalMekVariations = new JCheckBox(resources.getString("chkRegionalMekVariations.text"));
        chkRegionalMekVariations.setToolTipText(resources.getString("chkRegionalMekVariations.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkRegionalMekVariations, gridBagConstraints);

        chkAttachedPlayerCamouflage = new JCheckBox(resources.getString("chkAttachedPlayerCamouflage.text"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkAttachedPlayerCamouflage, gridBagConstraints);

        chkPlayerControlsAttachedUnits = new JCheckBox(
                resources.getString("chkPlayerControlsAttachedUnits.text"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkPlayerControlsAttachedUnits, gridBagConstraints);

        chkUseDropShips = new JCheckBox(resources.getString("chkUseDropShips.text"));
        chkUseDropShips.setToolTipText(resources.getString("chkUseDropShips.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkUseDropShips, gridBagConstraints);

        chkUseWeatherConditions = new JCheckBox(resources.getString("chkUseWeatherConditions.text"));
        chkUseWeatherConditions.setToolTipText(resources.getString("chkUseWeatherConditions.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkUseWeatherConditions, gridBagConstraints);

        chkUseLightConditions = new JCheckBox(resources.getString("chkUseLightConditions.text"));
        chkUseLightConditions.setToolTipText(resources.getString("chkUseLightConditions.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseLightConditions, gridBagConstraints);

        chkUsePlanetaryConditions = new JCheckBox(resources.getString("chkUsePlanetaryConditions.text"));
        chkUsePlanetaryConditions.setToolTipText(resources.getString("chkUsePlanetaryConditions.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUsePlanetaryConditions, gridBagConstraints);

        JPanel panFixedMapChance = new JPanel();
        JLabel lblFixedMapChance = new JLabel(resources.getString("lblFixedMapChance.text"));
        lblFixedMapChance.setToolTipText(resources.getString("lblFixedMapChance.toolTipText"));
        spnFixedMapChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        panFixedMapChance.add(lblFixedMapChance);
        panFixedMapChance.add(spnFixedMapChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panFixedMapChance, gridBagConstraints);

        JPanel panSPAUpgradeIntensity = new JPanel();
        JLabel lblSPAUpgradeIntensity = new JLabel(resources.getString("lblSPAUpgradeIntensity.text"));
        lblSPAUpgradeIntensity.setToolTipText(resources.getString("lblSPAUpgradeIntensity.toolTipText"));
        spnSPAUpgradeIntensity = new JSpinner(new SpinnerNumberModel(0, -1, 3, 1));
        panSPAUpgradeIntensity.add(lblSPAUpgradeIntensity);
        panSPAUpgradeIntensity.add(spnSPAUpgradeIntensity);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panSPAUpgradeIntensity, gridBagConstraints);

        JPanel panScenarioMod = new JPanel();
        JLabel lblScenarioMod = new JLabel(resources.getString("lblScenarioMod.text"));
        panScenarioMod.add(lblScenarioMod);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panScenarioMod, gridBagConstraints);

        JLabel lblScenarioModMax = new JLabel(resources.getString("lblScenarioModMax.text"));
        lblScenarioModMax.setToolTipText(resources.getString("lblScenarioModMax.toolTipText"));
        spnScenarioModMax = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
        panScenarioMod.add(lblScenarioModMax);
        panScenarioMod.add(spnScenarioModMax);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panScenarioMod, gridBagConstraints);

        JLabel lblScenarioModChance = new JLabel(resources.getString("lblScenarioModChance.text"));
        lblScenarioModChance.setToolTipText(resources.getString("lblScenarioModChance.toolTipText"));
        spnScenarioModChance = new JSpinner(new SpinnerNumberModel(25, 5, 100, 5));
        panScenarioMod.add(lblScenarioModChance);
        panScenarioMod.add(spnScenarioModChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panScenarioMod, gridBagConstraints);

        JLabel lblScenarioModBV = new JLabel(resources.getString("lblScenarioModBV.text"));
        lblScenarioModBV.setToolTipText(resources.getString("lblScenarioModBV.toolTipText"));
        spnScenarioModBV = new JSpinner(new SpinnerNumberModel(50, 5, 100, 5));
        panScenarioMod.add(lblScenarioModBV);
        panScenarioMod.add(spnScenarioModBV);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panScenarioMod, gridBagConstraints);

        JScrollPane scrAtB = new JScrollPane(panAtB);
        scrAtB.setPreferredSize(new Dimension(500, 410));
        return scrAtB;
    }
    // endregion Legacy Initialization

    // region Modern Initialization
    // region Personnel Tab
    private JScrollPane createPersonnelTab() {
        final AbstractMHQScrollablePanel personnelPanel = new DefaultMHQScrollablePanel(getFrame(),
                "personnelPanel", new GridBagLayout());
        personnelPanel.setTracksViewportWidth(false);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        personnelPanel.add(createGeneralPersonnelPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createExpandedPersonnelInformationPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createLogPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createAdministratorsPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createAwardsPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createMedicalPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createPrisonerPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createDependentPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        personnelPanel.add(createPersonnelRemovalPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        personnelPanel.add(createSalaryPanel(), gbc);

        final JScrollPane scrollPersonnel = new JScrollPane(personnelPanel);
        scrollPersonnel.setPreferredSize(new Dimension(500, 400));

        return scrollPersonnel;
    }

    // region Turnover and Retention Tab
    private JScrollPane createTurnoverAndRetentionTab() {
        final AbstractMHQScrollablePanel turnoverAndRetentionPanel = new DefaultMHQScrollablePanel(getFrame(),
                "turnoverAndRetentionPanel", new GridBagLayout());
        turnoverAndRetentionPanel.setTracksViewportWidth(false);

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        turnoverAndRetentionPanel.add(createTurnoverAndRetentionHeaderPanel(), gbc);

        gbc.gridy++;
        turnoverAndRetentionPanel.add(createTurnoverAndRetentionSettingsPanel(), gbc);

        gbc.gridx++;
        turnoverAndRetentionPanel.add(createTurnoverAndRetentionModifiersPanel(), gbc);

        gbc.gridx++;
        turnoverAndRetentionPanel.add(createTurnoverAndRetentionPayoutPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        turnoverAndRetentionPanel.add(createTurnoverAndRetentionFatiguePanel(), gbc);

        gbc.gridx++;
        turnoverAndRetentionPanel.add(createTurnoverAndRetentionUnitCohesionPanel(), gbc);

        final JScrollPane scrollPersonnel = new JScrollPane(turnoverAndRetentionPanel);
        scrollPersonnel.setPreferredSize(new Dimension(500, 400));

        return scrollPersonnel;
    }

    // region Life Paths Tab
    private JScrollPane createLifePathsPanel() {
        final AbstractMHQScrollablePanel lifePathsPanel = new DefaultMHQScrollablePanel(getFrame(),
                "lifePathsPanel", new GridBagLayout());
        lifePathsPanel.setTracksViewportWidth(false);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lifePathsPanel.add(createPersonnelRandomizationPanel(), gbc);

        gbc.gridx++;
        lifePathsPanel.add(createRandomHistoriesPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        lifePathsPanel.add(createFamilyPanel(), gbc);

        gbc.gridx++;
        lifePathsPanel.add(createAnniversaryPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        lifePathsPanel.add(createMarriagePanel(), gbc);

        gbc.gridx++;
        lifePathsPanel.add(createDivorcePanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        lifePathsPanel.add(createEducationPanel(), gbc);

        gbc.gridx++;
        lifePathsPanel.add(createProcreationPanel(), gbc);

        //

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        lifePathsPanel.add(createDeathPanel(), gbc);

        final JScrollPane scrollLifePaths = new JScrollPane(lifePathsPanel);
        scrollLifePaths.setPreferredSize(new Dimension(400, 400));

        return scrollLifePaths;
    }

    private JPanel createGeneralPersonnelPanel() {
        // Create Panel Components
        chkUseTactics = new JCheckBox(resources.getString("chkUseTactics.text"));
        chkUseTactics.setToolTipText(resources.getString("chkUseTactics.toolTipText"));
        chkUseTactics.setName("chkUseTactics");

        chkUseInitiativeBonus = new JCheckBox(resources.getString("chkUseInitiativeBonus.text"));
        chkUseInitiativeBonus.setToolTipText(resources.getString("chkUseInitiativeBonus.toolTipText"));
        chkUseInitiativeBonus.setName("chkUseInitiativeBonus");

        chkUseToughness = new JCheckBox(resources.getString("chkUseToughness.text"));
        chkUseToughness.setToolTipText(resources.getString("chkUseToughness.toolTipText"));
        chkUseToughness.setName("chkUseToughness");

        chkUseRandomToughness = new JCheckBox(resources.getString("chkUseRandomToughness.text"));
        chkUseRandomToughness.setToolTipText(resources.getString("chkUseRandomToughness.toolTipText"));
        chkUseRandomToughness.setName("chkUseRandomToughness");

        chkUseArtillery = new JCheckBox(resources.getString("chkUseArtillery.text"));
        chkUseArtillery.setToolTipText(resources.getString("chkUseArtillery.toolTipText"));
        chkUseArtillery.setName("chkUseArtillery");

        chkUseAbilities = new JCheckBox(resources.getString("chkUseAbilities.text"));
        chkUseAbilities.setToolTipText(resources.getString("chkUseAbilities.toolTipText"));
        chkUseAbilities.setName("chkUseAbilities");

        chkUseEdge = new JCheckBox(resources.getString("chkUseEdge.text"));
        chkUseEdge.setToolTipText(resources.getString("chkUseEdge.toolTipText"));
        chkUseEdge.setName("chkUseEdge");
        chkUseEdge.addActionListener(evt -> chkUseSupportEdge.setEnabled(chkUseEdge.isSelected()));

        chkUseSupportEdge = new JCheckBox(resources.getString("chkUseSupportEdge.text"));
        chkUseSupportEdge.setToolTipText(resources.getString("chkUseSupportEdge.toolTipText"));
        chkUseSupportEdge.setName("chkUseSupportEdge");

        chkUseImplants = new JCheckBox(resources.getString("chkUseImplants.text"));
        chkUseImplants.setToolTipText(resources.getString("chkUseImplants.toolTipText"));
        chkUseImplants.setName("chkUseImplants");

        chkUseAlternativeQualityAveraging = new JCheckBox(
                resources.getString("chkUseAlternativeQualityAveraging.text"));
        chkUseAlternativeQualityAveraging
                .setToolTipText(resources.getString("chkUseAlternativeQualityAveraging.toolTipText"));
        chkUseAlternativeQualityAveraging.setName("chkUseAlternativeQualityAveraging");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setName("generalPersonnelPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseRandomToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseRandomToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging));

        return panel;
    }

    private JPanel createLogPanel() {
        chkUseTransfers = new JCheckBox(resources.getString("chkUseTransfers.text"));
        chkUseTransfers.setToolTipText(resources.getString("chkUseTransfers.toolTipText"));
        chkUseTransfers.setName("chkUseTransfers");

        chkUseExtendedTOEForceName = new JCheckBox(resources.getString("chkUseExtendedTOEForceName.text"));
        chkUseExtendedTOEForceName
                .setToolTipText(resources.getString("chkUseExtendedTOEForceName.toolTipText"));
        chkUseExtendedTOEForceName.setName("chkUseExtendedTOEForceName");

        chkPersonnelLogSkillGain = new JCheckBox(resources.getString("chkPersonnelLogSkillGain.text"));
        chkPersonnelLogSkillGain.setToolTipText(resources.getString("chkPersonnelLogSkillGain.toolTipText"));
        chkPersonnelLogSkillGain.setName("chkPersonnelLogSkillGain");

        chkPersonnelLogAbilityGain = new JCheckBox(resources.getString("chkPersonnelLogAbilityGain.text"));
        chkPersonnelLogAbilityGain
                .setToolTipText(resources.getString("chkPersonnelLogAbilityGain.toolTipText"));
        chkPersonnelLogAbilityGain.setName("chkPersonnelLogAbilityGain");

        chkPersonnelLogEdgeGain = new JCheckBox(resources.getString("chkPersonnelLogEdgeGain.text"));
        chkPersonnelLogEdgeGain.setToolTipText(resources.getString("chkPersonnelLogEdgeGain.toolTipText"));
        chkPersonnelLogEdgeGain.setName("chkPersonnelLogEdgeGain");

        chkDisplayPersonnelLog = new JCheckBox(resources.getString("chkDisplayPersonnelLog.text"));
        chkDisplayPersonnelLog.setToolTipText(resources.getString("chkDisplayPersonnelLog.toolTipText"));
        chkDisplayPersonnelLog.setName("chkDisplayPersonnelLog");

        chkDisplayScenarioLog = new JCheckBox(resources.getString("chkDisplayScenarioLog.text"));
        chkDisplayScenarioLog.setToolTipText(resources.getString("chkDisplayScenarioLog.toolTipText"));
        chkDisplayScenarioLog.setName("chkDisplayScenarioLog");

        chkDisplayKillRecord = new JCheckBox(resources.getString("chkDisplayKillRecord.text"));
        chkDisplayKillRecord.setToolTipText(resources.getString("chkDisplayKillRecord.toolTipText"));
        chkDisplayKillRecord.setName("chkDisplayKillRecord");

        final JPanel logPanel = new JPanel();
        logPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("logPanel.title")));
        logPanel.setName("logPanel");

        final GroupLayout layout = new GroupLayout(logPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        logPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTransfers)
                        .addComponent(chkUseExtendedTOEForceName)
                        .addComponent(chkPersonnelLogSkillGain)
                        .addComponent(chkPersonnelLogAbilityGain)
                        .addComponent(chkPersonnelLogEdgeGain)
                        .addComponent(chkDisplayPersonnelLog)
                        .addComponent(chkDisplayScenarioLog)
                        .addComponent(chkDisplayKillRecord));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseTransfers)
                        .addComponent(chkUseExtendedTOEForceName)
                        .addComponent(chkPersonnelLogSkillGain)
                        .addComponent(chkPersonnelLogAbilityGain)
                        .addComponent(chkPersonnelLogEdgeGain)
                        .addComponent(chkDisplayPersonnelLog)
                        .addComponent(chkDisplayScenarioLog)
                        .addComponent(chkDisplayKillRecord));

        return logPanel;
    }

    private void createFatigueSubPanel() {
        JLabel lblFatigueWarning = new JLabel(resources.getString("lblFatigueWarning.text"));
        lblFatigueWarning.setName("lblFatigueWarning");
        lblFatigueWarning.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        JLabel lblFatigueRate = new JLabel(resources.getString("lblFatigueRate.text"));
        lblFatigueRate.setToolTipText(resources.getString("lblFatigueRate.toolTipText"));
        lblFatigueRate.setName("lblFatigueRate");
        lblFatigueRate.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        spnFatigueRate = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        spnFatigueRate.setToolTipText(resources.getString("lblFatigueRate.toolTipText"));
        spnFatigueRate.setName("spnFatigueRate");
        spnFatigueRate.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        chkUseInjuryFatigue = new JCheckBox(resources.getString("chkUseInjuryFatigue.text"));
        chkUseInjuryFatigue.setToolTipText(resources.getString("chkUseInjuryFatigue.toolTipText"));
        chkUseInjuryFatigue.setName("chkUseInjuryFatigue");
        chkUseInjuryFatigue.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        JLabel lblFieldKitchenCapacity = new JLabel(resources.getString("lblFieldKitchenCapacity.text"));
        lblFieldKitchenCapacity.setToolTipText(resources.getString("lblFieldKitchenCapacity.toolTipText"));
        lblFieldKitchenCapacity.setName("lblFieldKitchenCapacity");
        lblFieldKitchenCapacity.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        spnFieldKitchenCapacity = new JSpinner(new SpinnerNumberModel(150, 0, 450, 1));
        spnFieldKitchenCapacity.setToolTipText(resources.getString("lblFieldKitchenCapacity.toolTipText"));
        spnFieldKitchenCapacity.setName("spnFieldKitchenCapacity");
        spnFieldKitchenCapacity.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        chkFieldKitchenIgnoreNonCombatants = new JCheckBox(
                resources.getString("chkFieldKitchenIgnoreNonCombatants.text"));
        chkFieldKitchenIgnoreNonCombatants
                .setToolTipText(resources.getString("chkFieldKitchenIgnoreNonCombatants.toolTipText"));
        chkFieldKitchenIgnoreNonCombatants.setName("chkFieldKitchenIgnoreNonCombatants");
        chkFieldKitchenIgnoreNonCombatants.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        JLabel lblFatigueLeaveThreshold = new JLabel(resources.getString("lblFatigueLeaveThreshold.text"));
        lblFatigueLeaveThreshold.setToolTipText(resources.getString("lblFatigueLeaveThreshold.toolTipText"));
        lblFatigueLeaveThreshold.setName("lblFatigueLeaveThreshold");
        lblFatigueLeaveThreshold.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        spnFatigueLeaveThreshold = new JSpinner(new SpinnerNumberModel(13, 0, 17, 1));
        spnFatigueLeaveThreshold.setToolTipText(resources.getString("lblFatigueLeaveThreshold.toolTipText"));
        spnFatigueLeaveThreshold.setName("spnFatigueLeaveThreshold");
        spnFatigueLeaveThreshold.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        fatigueSubPanel.setBorder(BorderFactory.createTitledBorder(""));
        fatigueSubPanel.setName("fatigueSubPanel");
        fatigueSubPanel.setEnabled(campaign.getCampaignOptions().isUseFatigue());

        final GroupLayout layout = new GroupLayout(fatigueSubPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        fatigueSubPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(lblFatigueWarning)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblFatigueRate)
                                .addComponent(spnFatigueRate, Alignment.LEADING))
                        .addComponent(chkUseInjuryFatigue)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblFieldKitchenCapacity)
                                .addComponent(spnFieldKitchenCapacity,
                                        Alignment.LEADING))
                        .addComponent(chkFieldKitchenIgnoreNonCombatants)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblFatigueLeaveThreshold)
                                .addComponent(spnFatigueLeaveThreshold,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblFatigueWarning)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFatigueRate)
                                .addComponent(spnFatigueRate))
                        .addComponent(chkUseInjuryFatigue)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFieldKitchenCapacity)
                                .addComponent(spnFieldKitchenCapacity))
                        .addComponent(chkFieldKitchenIgnoreNonCombatants)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFatigueLeaveThreshold)
                                .addComponent(spnFatigueLeaveThreshold)));
    }

    private JPanel createExpandedPersonnelInformationPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblTimeInServiceDisplayFormat = new JLabel();
        final JLabel lblTimeInRankDisplayFormat = new JLabel();

        // Create Panel Components
        chkUseTimeInService = new JCheckBox(resources.getString("chkUseTimeInService.text"));
        chkUseTimeInService.setToolTipText(resources.getString("chkUseTimeInService.toolTipText"));
        chkUseTimeInService.setName("chkUseTimeInService");
        chkUseTimeInService.addActionListener(evt -> {
            lblTimeInServiceDisplayFormat.setEnabled(chkUseTimeInService.isSelected());
            comboTimeInServiceDisplayFormat.setEnabled(chkUseTimeInService.isSelected());
        });

        lblTimeInServiceDisplayFormat.setText(resources.getString("lblTimeInServiceDisplayFormat.text"));
        lblTimeInServiceDisplayFormat
                .setToolTipText(resources.getString("lblTimeInServiceDisplayFormat.toolTipText"));
        lblTimeInServiceDisplayFormat.setName("lblTimeInServiceDisplayFormat");

        comboTimeInServiceDisplayFormat = new MMComboBox<>("comboTimeInServiceDisplayFormat",
                TimeInDisplayFormat.values());
        comboTimeInServiceDisplayFormat
                .setToolTipText(resources.getString("lblTimeInServiceDisplayFormat.toolTipText"));

        chkUseTimeInRank = new JCheckBox(resources.getString("chkUseTimeInRank.text"));
        chkUseTimeInRank.setToolTipText(resources.getString("chkUseTimeInRank.toolTipText"));
        chkUseTimeInRank.setName("chkUseTimeInRank");
        chkUseTimeInRank.addActionListener(evt -> {
            lblTimeInRankDisplayFormat.setEnabled(chkUseTimeInRank.isSelected());
            comboTimeInRankDisplayFormat.setEnabled(chkUseTimeInRank.isSelected());
        });

        lblTimeInRankDisplayFormat.setText(resources.getString("lblTimeInRankDisplayFormat.text"));
        lblTimeInRankDisplayFormat
                .setToolTipText(resources.getString("lblTimeInRankDisplayFormat.toolTipText"));
        lblTimeInRankDisplayFormat.setName("lblTimeInRankDisplayFormat");

        comboTimeInRankDisplayFormat = new MMComboBox<>("comboTimeInRankDisplayFormat",
                TimeInDisplayFormat.values());
        comboTimeInRankDisplayFormat
                .setToolTipText(resources.getString("lblTimeInRankDisplayFormat.toolTipText"));

        chkTrackTotalEarnings = new JCheckBox(resources.getString("chkTrackTotalEarnings.text"));
        chkTrackTotalEarnings.setToolTipText(resources.getString("chkTrackTotalEarnings.toolTipText"));
        chkTrackTotalEarnings.setName("chkTrackTotalEarnings");

        chkTrackTotalXPEarnings = new JCheckBox(resources.getString("chkTrackTotalXPEarnings.text"));
        chkTrackTotalXPEarnings.setToolTipText(resources.getString("chkTrackTotalXPEarnings.toolTipText"));
        chkTrackTotalXPEarnings.setName("chkTrackTotalXPEarnings");

        chkShowOriginFaction = new JCheckBox(resources.getString("chkShowOriginFaction.text"));
        chkShowOriginFaction.setToolTipText(resources.getString("chkShowOriginFaction.toolTipText"));
        chkShowOriginFaction.setName("chkShowOriginFaction");

        // Programmatically Assign Accessibility Labels
        lblTimeInServiceDisplayFormat.setLabelFor(comboTimeInServiceDisplayFormat);
        lblTimeInRankDisplayFormat.setLabelFor(comboTimeInRankDisplayFormat);

        // Disable Panel Portions by Default
        chkUseTimeInService.setSelected(true);
        chkUseTimeInService.doClick();
        chkUseTimeInRank.setSelected(true);
        chkUseTimeInRank.doClick();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("expandedPersonnelInformationPanel.title")));
        panel.setName("expandedPersonnelInformationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTimeInService)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblTimeInServiceDisplayFormat)
                                .addComponent(comboTimeInServiceDisplayFormat,
                                        Alignment.LEADING))
                        .addComponent(chkUseTimeInRank)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblTimeInRankDisplayFormat)
                                .addComponent(comboTimeInRankDisplayFormat,
                                        Alignment.LEADING))
                        .addComponent(chkTrackTotalEarnings)
                        .addComponent(chkTrackTotalXPEarnings)
                        .addComponent(chkShowOriginFaction));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseTimeInService)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTimeInServiceDisplayFormat)
                                .addComponent(comboTimeInServiceDisplayFormat))
                        .addComponent(chkUseTimeInRank)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTimeInRankDisplayFormat)
                                .addComponent(comboTimeInRankDisplayFormat))
                        .addComponent(chkTrackTotalEarnings)
                        .addComponent(chkTrackTotalXPEarnings)
                        .addComponent(chkShowOriginFaction));

        return panel;
    }

    private JPanel createAwardsPanel() {
        // Create Panel Components
        final JPanel autoAwardsPanel = createAutoAwardsPanel();

        final JLabel lblAwardTierSize = new JLabel(resources.getString("lblAwardTierSize.text"));
        lblAwardTierSize.setToolTipText(resources.getString("lblAwardTierSize.toolTipText"));
        lblAwardTierSize.setName("lblAwardTierSize");
        spnAwardTierSize = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        spnAwardTierSize.setMaximumSize(new Dimension(60, 25));

        chkIssuePosthumousAwards = new JCheckBox(resources.getString("chkIssuePosthumousAwards.text"));
        chkIssuePosthumousAwards.setToolTipText(resources.getString("chkIssuePosthumousAwards.toolTipText"));
        chkIssuePosthumousAwards.setName("chkIssuePosthumousAwards");

        chkIssueBestAwardOnly = new JCheckBox(resources.getString("chkIssueBestAwardOnly.text"));
        chkIssueBestAwardOnly.setToolTipText(resources.getString("chkIssueBestAwardOnly.toolTipText"));
        chkIssueBestAwardOnly.setName("chkIssueBestAwardOnly");

        chkIgnoreStandardSet = new JCheckBox(resources.getString("chkIgnoreStandardSet.text"));
        chkIgnoreStandardSet.setToolTipText(resources.getString("chkIgnoreStandardSet.toolTipText"));
        chkIgnoreStandardSet.setName("chkIgnoreStandardSet");

        final JLabel lblAwardBonusStyle = new JLabel(resources.getString("lblAwardBonusStyle.text"));
        lblAwardBonusStyle.setToolTipText(resources.getString("lblAwardBonusStyle.toolTipText"));
        lblAwardBonusStyle.setName("lblAwardBonusStyle");

        comboAwardBonusStyle = new MMComboBox<>("comboAwardBonusStyle", AwardBonus.values());
        comboAwardBonusStyle.setToolTipText(resources.getString("lblAwardBonusStyle.toolTipText"));
        comboAwardBonusStyle.setMaximumSize(new Dimension(80, 25));
        comboAwardBonusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AwardBonus) {
                    list.setToolTipText(((AwardBonus) value).getToolTipText());
                }
                return this;
            }
        });

        lblAwardSetFilterList = new JLabel(resources.getString("lblAwardSetFilterList.text"));
        lblAwardSetFilterList.setToolTipText(
                wordWrap(resources.getString("lblAwardSetFilterList.toolTipText"), 100));
        lblAwardSetFilterList.setName("lblAwardSetFilterList");

        txtAwardSetFilterList = new JTextArea(5, 20);
        txtAwardSetFilterList.setLineWrap(true);
        txtAwardSetFilterList.setWrapStyleWord(true);
        txtAwardSetFilterList.setToolTipText(
                wordWrap(resources.getString("lblAwardSetFilterList.toolTipText"), 100));
        txtAwardSetFilterList.setName("txtAwardSetFilterList");
        txtAwardSetFilterList.setText("");

        JScrollPane scrollAwardSetFilterList = new JScrollPane(txtAwardSetFilterList);
        scrollAwardSetFilterList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollAwardSetFilterList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        chkEnableAutoAwards = new JCheckBox(resources.getString("chkEnableAutoAwards.text"));
        chkEnableAutoAwards.setToolTipText(resources.getString("chkEnableAutoAwards.toolTipText"));
        chkEnableAutoAwards.setName("chkEnableAutoAwards");
        chkEnableAutoAwards.addActionListener(evt -> {
            final boolean isEnabled = chkEnableAutoAwards.isSelected();

            chkIssuePosthumousAwards.setEnabled(isEnabled);
            chkIssueBestAwardOnly.setEnabled(isEnabled);
            chkIgnoreStandardSet.setEnabled(isEnabled);
            autoAwardsPanel.setEnabled(isEnabled);
            chkEnableContractAwards.setEnabled(isEnabled);
            chkEnableFactionHunterAwards.setEnabled(isEnabled);
            chkEnableInjuryAwards.setEnabled(isEnabled);
            chkEnableIndividualKillAwards.setEnabled(isEnabled);
            chkEnableFormationKillAwards.setEnabled(isEnabled);
            chkEnableRankAwards.setEnabled(isEnabled);
            chkEnableScenarioAwards.setEnabled(isEnabled);
            chkEnableSkillAwards.setEnabled(isEnabled);
            chkEnableTheatreOfWarAwards.setEnabled(isEnabled);
            chkEnableTimeAwards.setEnabled(isEnabled);
            chkEnableTrainingAwards.setEnabled(isEnabled);
            chkEnableMiscAwards.setEnabled(isEnabled);
            lblAwardSetFilterList.setEnabled(isEnabled);
            txtAwardSetFilterList.setEnabled(isEnabled);
        });

        // this prevents a really annoying bug where disabled options don't stay
        // disabled when
        // reloading Campaign Options
        if (!campaign.getCampaignOptions().isEnableAutoAwards()) {
            chkIssuePosthumousAwards.setEnabled(false);
            chkIssueBestAwardOnly.setEnabled(false);
            chkIgnoreStandardSet.setEnabled(false);
            autoAwardsPanel.setEnabled(false);
            chkEnableContractAwards.setEnabled(false);
            chkEnableFactionHunterAwards.setEnabled(false);
            chkEnableInjuryAwards.setEnabled(false);
            chkEnableIndividualKillAwards.setEnabled(false);
            chkEnableFormationKillAwards.setEnabled(false);
            chkEnableRankAwards.setEnabled(false);
            chkEnableScenarioAwards.setEnabled(false);
            chkEnableSkillAwards.setEnabled(false);
            chkEnableTheatreOfWarAwards.setEnabled(false);
            chkEnableTimeAwards.setEnabled(false);
            chkEnableTrainingAwards.setEnabled(false);
            chkEnableMiscAwards.setEnabled(false);
            lblAwardSetFilterList.setEnabled(false);
            txtAwardSetFilterList.setEnabled(false);
        }

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("awardsPanel.title")));
        panel.setName("awardsPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblAwardBonusStyle)
                                .addComponent(comboAwardBonusStyle))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblAwardTierSize)
                                .addComponent(spnAwardTierSize))
                        .addComponent(chkEnableAutoAwards)
                        .addComponent(chkIssuePosthumousAwards)
                        .addComponent(chkIssueBestAwardOnly)
                        .addComponent(chkIgnoreStandardSet)
                        .addComponent(autoAwardsPanel)
                        .addComponent(lblAwardSetFilterList)
                        .addComponent(scrollAwardSetFilterList));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblAwardBonusStyle)
                                .addComponent(comboAwardBonusStyle))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblAwardTierSize)
                                .addComponent(spnAwardTierSize))
                        .addComponent(chkEnableAutoAwards)
                        .addComponent(chkIssuePosthumousAwards)
                        .addComponent(chkIssueBestAwardOnly)
                        .addComponent(chkIgnoreStandardSet)
                        .addComponent(autoAwardsPanel)
                        .addComponent(lblAwardSetFilterList)
                        .addComponent(scrollAwardSetFilterList));

        return panel;
    }

    private JPanel createAutoAwardsPanel() {
        // Create Panel Components
        chkEnableContractAwards = new JCheckBox(resources.getString("chkEnableContractAwards.text"));
        chkEnableContractAwards.setToolTipText(resources.getString("chkEnableContractAwards.toolTipText"));
        chkEnableContractAwards.setName("chkEnableContractAwards");

        chkEnableFactionHunterAwards = new JCheckBox(resources.getString("chkEnableFactionHunterAwards.text"));
        chkEnableFactionHunterAwards
                .setToolTipText(resources.getString("chkEnableFactionHunterAwards.toolTipText"));
        chkEnableFactionHunterAwards.setName("chkEnableFactionHunterAwards");

        chkEnableInjuryAwards = new JCheckBox(resources.getString("chkEnableInjuryAwards.text"));
        chkEnableInjuryAwards.setToolTipText(resources.getString("chkEnableInjuryAwards.toolTipText"));
        chkEnableInjuryAwards.setName("chkEnableInjuryAwards");

        chkEnableIndividualKillAwards = new JCheckBox(
                resources.getString("chkEnableIndividualKillAwards.text"));
        chkEnableIndividualKillAwards
                .setToolTipText(resources.getString("chkEnableIndividualKillAwards.toolTipText"));
        chkEnableIndividualKillAwards.setName("chkEnableIndividualKillAwards");

        chkEnableFormationKillAwards = new JCheckBox(resources.getString("chkEnableFormationKillAwards.text"));
        chkEnableFormationKillAwards
                .setToolTipText(resources.getString("chkEnableFormationKillAwards.toolTipText"));
        chkEnableFormationKillAwards.setName("chkEnableFormationKillAwards");

        chkEnableRankAwards = new JCheckBox(resources.getString("chkEnableRankAwards.text"));
        chkEnableRankAwards.setToolTipText(resources.getString("chkEnableRankAwards.toolTipText"));
        chkEnableRankAwards.setName("chkEnableRankAwards");

        chkEnableScenarioAwards = new JCheckBox(resources.getString("chkEnableScenarioAwards.text"));
        chkEnableScenarioAwards.setToolTipText(resources.getString("chkEnableScenarioAwards.toolTipText"));
        chkEnableScenarioAwards.setName("chkEnableScenarioAwards");

        chkEnableSkillAwards = new JCheckBox(resources.getString("chkEnableSkillAwards.text"));
        chkEnableSkillAwards.setToolTipText(resources.getString("chkEnableSkillAwards.toolTipText"));
        chkEnableSkillAwards.setName("chkEnableSkillAwards");

        chkEnableTheatreOfWarAwards = new JCheckBox(resources.getString("chkEnableTheatreOfWarAwards.text"));
        chkEnableTheatreOfWarAwards
                .setToolTipText(resources.getString("chkEnableTheatreOfWarAwards.toolTipText"));
        chkEnableTheatreOfWarAwards.setName("chkEnableTheatreOfWarAwards");

        chkEnableTimeAwards = new JCheckBox(resources.getString("chkEnableTimeAwards.text"));
        chkEnableTimeAwards.setToolTipText(resources.getString("chkEnableTimeAwards.toolTipText"));
        chkEnableTimeAwards.setName("chkEnableTimeAwards");

        chkEnableTrainingAwards = new JCheckBox(resources.getString("chkEnableTrainingAwards.text"));
        chkEnableTrainingAwards.setToolTipText(resources.getString("chkEnableTrainingAwards.toolTipText"));
        chkEnableTrainingAwards.setName("chkEnableTrainingAwards");

        chkEnableMiscAwards = new JCheckBox(resources.getString("chkEnableMiscAwards.text"));
        chkEnableMiscAwards.setToolTipText(resources.getString("chkEnableMiscAwards.toolTipText"));
        chkEnableMiscAwards.setName("chkEnableMiscAwards");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("autoAwardsPanel.title")));
        panel.setName("autoAwardsPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.BASELINE)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(chkEnableContractAwards)
                                .addComponent(chkEnableFactionHunterAwards)
                                .addComponent(chkEnableInjuryAwards)
                                .addComponent(chkEnableIndividualKillAwards))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(chkEnableRankAwards)
                                .addComponent(chkEnableScenarioAwards)
                                .addComponent(chkEnableSkillAwards)
                                .addComponent(chkEnableFormationKillAwards))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(chkEnableTheatreOfWarAwards)
                                .addComponent(chkEnableTimeAwards)
                                .addComponent(chkEnableTrainingAwards)
                                .addComponent(chkEnableMiscAwards)));

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(chkEnableContractAwards)
                                .addComponent(chkEnableFactionHunterAwards)
                                .addComponent(chkEnableInjuryAwards)
                                .addComponent(chkEnableIndividualKillAwards))
                        .addGroup(layout.createParallelGroup()
                                .addComponent(chkEnableFormationKillAwards)
                                .addComponent(chkEnableRankAwards)
                                .addComponent(chkEnableScenarioAwards)
                                .addComponent(chkEnableSkillAwards))
                        .addGroup(layout.createParallelGroup()
                                .addComponent(chkEnableTheatreOfWarAwards)
                                .addComponent(chkEnableTimeAwards)
                                .addComponent(chkEnableTrainingAwards)
                                .addComponent(chkEnableMiscAwards)));

        return panel;
    }

    private Component createAdministratorsPanel() {
        chkAdminsHaveNegotiation = new JCheckBox(resources.getString("chkAdminsHaveNegotiation.text"));
        chkAdminsHaveNegotiation.setToolTipText(resources.getString("chkAdminsHaveNegotiation.toolTipText"));
        chkAdminsHaveNegotiation.setName("chkAdminsHaveNegotiation");

        chkAdminExperienceLevelIncludeNegotiation = new JCheckBox(
                resources.getString("chkAdminExperienceLevelIncludeNegotiation.text"));
        chkAdminExperienceLevelIncludeNegotiation
                .setToolTipText(resources
                        .getString("chkAdminExperienceLevelIncludeNegotiation.toolTipText"));
        chkAdminExperienceLevelIncludeNegotiation.setName("chkAdminExperienceLevelIncludeNegotiation");

        chkAdminsHaveScrounge = new JCheckBox(resources.getString("chkAdminsHaveScrounge.text"));
        chkAdminsHaveScrounge.setToolTipText(resources.getString("chkAdminsHaveScrounge.toolTipText"));
        chkAdminsHaveScrounge.setName("chkAdminsHaveScrounge");

        chkAdminExperienceLevelIncludeScrounge = new JCheckBox(
                resources.getString("chkAdminExperienceLevelIncludeScrounge.text"));
        chkAdminExperienceLevelIncludeScrounge
                .setToolTipText(resources
                        .getString("chkAdminExperienceLevelIncludeScrounge.toolTipText"));
        chkAdminExperienceLevelIncludeScrounge.setName("chkAdminExperienceLevelIncludeScrounge");

        administratorsPanel
                .setBorder(BorderFactory
                        .createTitledBorder(resources.getString("administratorsPanel.title")));
        administratorsPanel.setName("administratorsPanel");

        final GroupLayout layout = new GroupLayout(administratorsPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        administratorsPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkAdminsHaveNegotiation)
                        .addComponent(chkAdminExperienceLevelIncludeNegotiation)
                        .addGap(15)
                        .addComponent(chkAdminsHaveScrounge)
                        .addComponent(chkAdminExperienceLevelIncludeScrounge));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkAdminsHaveNegotiation)
                        .addComponent(chkAdminExperienceLevelIncludeNegotiation)
                        .addComponent(chkAdminsHaveScrounge)
                        .addComponent(chkAdminExperienceLevelIncludeScrounge));
        return administratorsPanel;
    }

    private JPanel createMedicalPanel() {
        // Create Panel Components
        chkUseAdvancedMedical = new JCheckBox(resources.getString("chkUseAdvancedMedical.text"));
        chkUseAdvancedMedical.setToolTipText(resources.getString("chkUseAdvancedMedical.toolTipText"));
        chkUseAdvancedMedical.setName("chkUseAdvancedMedical");

        final JLabel lblHealWaitingPeriod = new JLabel(resources.getString("lblHealWaitingPeriod.text"));
        lblHealWaitingPeriod.setToolTipText(resources.getString("lblHealWaitingPeriod.toolTipText"));
        lblHealWaitingPeriod.setName("lblHealWaitingPeriod");

        spnHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
        spnHealWaitingPeriod.setToolTipText(resources.getString("lblHealWaitingPeriod.toolTipText"));
        spnHealWaitingPeriod.setName("spnHealWaitingPeriod");

        final JLabel lblNaturalHealWaitingPeriod = new JLabel(
                resources.getString("lblNaturalHealWaitingPeriod.text"));
        lblNaturalHealWaitingPeriod
                .setToolTipText(resources.getString("lblNaturalHealWaitingPeriod.toolTipText"));
        lblNaturalHealWaitingPeriod.setName("lblNaturalHealWaitingPeriod");

        spnNaturalHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        spnNaturalHealWaitingPeriod
                .setToolTipText(resources.getString("lblNaturalHealWaitingPeriod.toolTipText"));
        spnNaturalHealWaitingPeriod.setName("spnNaturalHealWaitingPeriod");

        final JLabel lblMinimumHitsForVehicles = new JLabel(
                resources.getString("lblMinimumHitsForVehicles.text"));
        lblMinimumHitsForVehicles.setToolTipText(resources.getString("lblMinimumHitsForVehicles.toolTipText"));
        lblMinimumHitsForVehicles.setName("lblMinimumHitsForVehicles");

        spnMinimumHitsForVehicles = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        spnMinimumHitsForVehicles.setToolTipText(resources.getString("lblMinimumHitsForVehicles.toolTipText"));
        spnMinimumHitsForVehicles.setName("spnMinimumHitsForVehicles");
        ((DefaultEditor) spnMinimumHitsForVehicles.getEditor()).getTextField().setEditable(false);

        chkUseRandomHitsForVehicles = new JCheckBox(resources.getString("chkUseRandomHitsForVehicles.text"));
        chkUseRandomHitsForVehicles
                .setToolTipText(resources.getString("chkUseRandomHitsForVehicles.toolTipText"));
        chkUseRandomHitsForVehicles.setName("chkUseRandomHitsForVehicles");

        chkUseTougherHealing = new JCheckBox(resources.getString("chkUseTougherHealing.text"));
        chkUseTougherHealing.setToolTipText(resources.getString("chkUseTougherHealing.toolTipText"));
        chkUseTougherHealing.setName("chkUseTougherHealing");

        final JLabel lblMaximumPatients = new JLabel(resources.getString("lblMaximumPatients.text"));
        lblMaximumPatients.setToolTipText(resources.getString("lblMaximumPatients.toolTipText"));
        lblMaximumPatients.setName("lblMaximumPatients");

        spnMaximumPatients = new JSpinner(new SpinnerNumberModel(25, 1, 100, 1));
        spnMaximumPatients.setToolTipText(resources.getString("lblMaximumPatients.toolTipText"));
        spnMaximumPatients.setName("spnMaximumPatients");

        // Programmatically Assign Accessibility Labels
        lblHealWaitingPeriod.setLabelFor(spnHealWaitingPeriod);
        lblNaturalHealWaitingPeriod.setLabelFor(spnNaturalHealWaitingPeriod);
        lblMinimumHitsForVehicles.setLabelFor(spnMinimumHitsForVehicles);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("medicalPanel.title")));
        panel.setName("medicalPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseAdvancedMedical)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblHealWaitingPeriod)
                                .addComponent(spnHealWaitingPeriod, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblNaturalHealWaitingPeriod)
                                .addComponent(spnNaturalHealWaitingPeriod,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMinimumHitsForVehicles)
                                .addComponent(spnMinimumHitsForVehicles,
                                        Alignment.LEADING))
                        .addComponent(chkUseRandomHitsForVehicles)
                        .addComponent(chkUseTougherHealing)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMaximumPatients)
                                .addComponent(spnMaximumPatients, Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseAdvancedMedical)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblHealWaitingPeriod)
                                .addComponent(spnHealWaitingPeriod))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNaturalHealWaitingPeriod)
                                .addComponent(spnNaturalHealWaitingPeriod))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMinimumHitsForVehicles)
                                .addComponent(spnMinimumHitsForVehicles))
                        .addComponent(chkUseRandomHitsForVehicles)
                        .addComponent(chkUseTougherHealing)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMaximumPatients)
                                .addComponent(spnMaximumPatients)));

        return panel;
    }

    private JPanel createPrisonerPanel() {
        // Create Panel Components
        final JLabel lblPrisonerCaptureStyle = new JLabel(resources.getString("lblPrisonerCaptureStyle.text"));
        lblPrisonerCaptureStyle.setToolTipText(resources.getString("lblPrisonerCaptureStyle.toolTipText"));
        lblPrisonerCaptureStyle.setName("lblPrisonerCaptureStyle");

        comboPrisonerCaptureStyle = new MMComboBox<>("comboPrisonerCaptureStyle",
                PrisonerCaptureStyle.values());
        comboPrisonerCaptureStyle.setToolTipText(resources.getString("lblPrisonerCaptureStyle.toolTipText"));
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrisonerCaptureStyle) {
                    list.setToolTipText(((PrisonerCaptureStyle) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblPrisonerStatus = new JLabel(resources.getString("lblPrisonerStatus.text"));
        lblPrisonerStatus.setToolTipText(resources.getString("lblPrisonerStatus.toolTipText"));
        lblPrisonerStatus.setName("lblPrisonerStatus");

        final DefaultComboBoxModel<PrisonerStatus> prisonerStatusModel = new DefaultComboBoxModel<>(
                PrisonerStatus.values());
        prisonerStatusModel.removeElement(PrisonerStatus.FREE); // we don't want this as a standard use case for
                                                                // prisoners
        comboPrisonerStatus = new MMComboBox<>("comboPrisonerStatus", prisonerStatusModel);
        comboPrisonerStatus.setToolTipText(resources.getString("lblPrisonerStatus.toolTipText"));

        chkPrisonerBabyStatus = new JCheckBox(resources.getString("chkPrisonerBabyStatus.text"));
        chkPrisonerBabyStatus.setToolTipText(resources.getString("chkPrisonerBabyStatus.toolTipText"));
        chkPrisonerBabyStatus.setName("chkPrisonerBabyStatus");

        chkAtBPrisonerDefection = new JCheckBox(resources.getString("chkAtBPrisonerDefection.text"));
        chkAtBPrisonerDefection.setToolTipText(resources.getString("chkAtBPrisonerDefection.toolTipText"));
        chkAtBPrisonerDefection.setName("chkAtBPrisonerDefection");

        chkAtBPrisonerRansom = new JCheckBox(resources.getString("chkAtBPrisonerRansom.text"));
        chkAtBPrisonerRansom.setToolTipText(resources.getString("chkAtBPrisonerRansom.toolTipText"));
        chkAtBPrisonerRansom.setName("chkAtBPrisonerRansom");

        // Programmatically Assign Accessibility Labels
        lblPrisonerCaptureStyle.setLabelFor(comboPrisonerCaptureStyle);
        lblPrisonerStatus.setLabelFor(comboPrisonerStatus);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("prisonerPanel.title")));
        panel.setName("prisonerPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPrisonerCaptureStyle)
                                .addComponent(comboPrisonerCaptureStyle,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPrisonerStatus)
                                .addComponent(comboPrisonerStatus, Alignment.LEADING))
                        .addComponent(chkPrisonerBabyStatus)
                        .addComponent(chkAtBPrisonerDefection)
                        .addComponent(chkAtBPrisonerRansom));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPrisonerCaptureStyle)
                                .addComponent(comboPrisonerCaptureStyle))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPrisonerStatus)
                                .addComponent(comboPrisonerStatus))
                        .addComponent(chkPrisonerBabyStatus)
                        .addComponent(chkAtBPrisonerDefection)
                        .addComponent(chkAtBPrisonerRansom));

        return panel;
    }

    private JPanel createTurnoverAndRetentionHeaderPanel() {
        chkUseRandomRetirement = new JCheckBox(resources.getString("chkUseRandomRetirement.text"));
        chkUseRandomRetirement.setToolTipText(resources.getString("chkUseRandomRetirement.toolTipText"));
        chkUseRandomRetirement.setName("chkUseRandomRetirement");
        chkUseRandomRetirement.addActionListener(evt -> {
            boolean isEnabled = chkUseRandomRetirement.isSelected();

            // General Handlers
            for (Component component : turnoverAndRetentionSettingsPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            for (Component component : turnoverAndRetentionModifiersPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            for (Component component : turnoverAndRetentionPayoutPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            for (Component component : turnoverAndRetentionUnitCohesionPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            // Border Handlers
            turnoverAndRetentionSettingsPanel.setEnabled(isEnabled);
            turnoverAndRetentionModifiersPanel.setEnabled(isEnabled);
            turnoverAndRetentionPayoutPanel.setEnabled(isEnabled);
            turnoverAndRetentionUnitCohesionPanel.setEnabled(isEnabled);

            // Special Case Handlers
            // These are elements whose status is influenced by other campaign options.
            lblTurnoverFixedTargetNumber.setEnabled(isEnabled);
            spnTurnoverFixedTargetNumber.setEnabled(isEnabled);

            boolean isUseLoyaltyModifiers = chkUseLoyaltyModifiers.isSelected();
            loyaltySubPanel.setEnabled((isEnabled) && (isUseLoyaltyModifiers));
            chkUseHideLoyalty.setEnabled((isEnabled) && (isUseLoyaltyModifiers));

            boolean isUseServiceBonus = chkUsePayoutServiceBonus.isSelected();
            payoutServiceBonusSubPanel.setEnabled((isEnabled) && (isUseServiceBonus));
            lblPayoutServiceBonusRate.setEnabled((isEnabled) && (isUseServiceBonus));
            spnPayoutServiceBonusRate.setEnabled((isEnabled) && (isUseServiceBonus));

            boolean isUseAdministrativeStrain = chkUseAdministrativeStrain.isSelected();
            administrativeStrainSubPanel.setEnabled((isEnabled) && (isUseAdministrativeStrain));
            lblAdministrativeCapacity.setEnabled((isEnabled) && (isUseAdministrativeStrain));
            spnAdministrativeCapacity.setEnabled((isEnabled) && (isUseAdministrativeStrain));
            lblMultiCrewStrainDivider.setEnabled((isEnabled) && (isUseAdministrativeStrain));
            spnMultiCrewStrainDivider.setEnabled((isEnabled) && (isUseAdministrativeStrain));

            boolean isUseManagementSkill = chkUseManagementSkill.isSelected();
            managementSkillSubPanel.setEnabled((isEnabled) && (isUseManagementSkill));
            chkUseCommanderLeadershipOnly.setEnabled((isEnabled) && (isUseManagementSkill));
            lblManagementSkillPenalty.setEnabled((isEnabled) && (isUseManagementSkill));
            spnManagementSkillPenalty.setEnabled((isEnabled) && (isUseManagementSkill));
        });

        final JPanel turnoverAndRetentionHeaderPanel = new JPanel();
        turnoverAndRetentionHeaderPanel.setName("turnoverAndRetentionHeaderPanel");

        final GroupLayout layout = new GroupLayout(turnoverAndRetentionHeaderPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        turnoverAndRetentionHeaderPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseRandomRetirement));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseRandomRetirement));

        return turnoverAndRetentionHeaderPanel;
    }

    private JPanel createTurnoverAndRetentionSettingsPanel() {
        boolean isUseTurnover = campaign.getCampaignOptions().isUseRandomRetirement();

        lblTurnoverFixedTargetNumber = new JLabel(resources.getString("lblTurnoverFixedTargetNumber.text"));
        lblTurnoverFixedTargetNumber
                .setToolTipText(resources.getString("lblTurnoverFixedTargetNumber.toolTipText"));
        lblTurnoverFixedTargetNumber.setName("lblTurnoverFixedTargetNumber");
        lblTurnoverFixedTargetNumber.setEnabled(isUseTurnover);

        spnTurnoverFixedTargetNumber = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
        spnTurnoverFixedTargetNumber
                .setToolTipText(resources.getString("lblTurnoverFixedTargetNumber.toolTipText"));
        spnTurnoverFixedTargetNumber.setName("spnTurnoverFixedTargetNumber");
        spnTurnoverFixedTargetNumber.setEnabled(isUseTurnover);

        lblTurnoverFixedTargetNumber = new JLabel(resources.getString("lblTurnoverFixedTargetNumber.text"));
        lblTurnoverFixedTargetNumber
                .setToolTipText(resources.getString("lblTurnoverFixedTargetNumber.toolTipText"));
        lblTurnoverFixedTargetNumber.setName("lblTurnoverFixedTargetNumber");
        lblTurnoverFixedTargetNumber.setEnabled(isUseTurnover);

        JLabel lblTurnoverFrequency = new JLabel(resources.getString("lblTurnoverFrequency.text"));
        lblTurnoverFrequency.setToolTipText(resources.getString("lblTurnoverFrequency.toolTipText"));
        lblTurnoverFrequency.setName("lblTurnoverFrequency");
        lblTurnoverFrequency.setEnabled(isUseTurnover);

        comboTurnoverFrequency = new MMComboBox<>("comboTurnoverFrequency", TurnoverFrequency.values());
        comboTurnoverFrequency.setToolTipText(resources.getString("lblTurnoverFrequency.toolTipText"));
        comboTurnoverFrequency.setEnabled(isUseTurnover);
        comboTurnoverFrequency.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TurnoverFrequency) {
                    list.setToolTipText(((TurnoverFrequency) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseContractCompletionRandomRetirement = new JCheckBox(
                resources.getString("chkUseContractCompletionRandomRetirement.text"));
        chkUseContractCompletionRandomRetirement
                .setToolTipText(resources
                        .getString("chkUseContractCompletionRandomRetirement.toolTipText"));
        chkUseContractCompletionRandomRetirement.setName("chkUseContractCompletionRandomRetirement");
        chkUseContractCompletionRandomRetirement.setEnabled(isUseTurnover);

        chkUseRandomFounderTurnover = new JCheckBox(resources.getString("chkUseRandomFounderTurnover.text"));
        chkUseRandomFounderTurnover
                .setToolTipText(resources.getString("chkUseRandomFounderTurnover.toolTipText"));
        chkUseRandomFounderTurnover.setName("chkUseRandomFounderTurnover");
        chkUseRandomFounderTurnover.setEnabled(isUseTurnover);

        chkUseFounderRetirement = new JCheckBox(resources.getString("chkUseFounderRetirement.text"));
        chkUseFounderRetirement.setToolTipText(resources.getString("chkUseFounderRetirement.toolTipText"));
        chkUseFounderRetirement.setName("chkUseFounderRetirement");
        chkUseFounderRetirement.setEnabled(isUseTurnover);

        chkTrackOriginalUnit = new JCheckBox(resources.getString("chkTrackOriginalUnit.text"));
        chkTrackOriginalUnit.setToolTipText(resources.getString("chkTrackOriginalUnit.toolTipText"));
        chkTrackOriginalUnit.setName("chkTrackOriginalUnit");
        chkTrackOriginalUnit.setEnabled(isUseTurnover);

        chkAeroRecruitsHaveUnits = new JCheckBox(resources.getString("chkAeroRecruitsHaveUnits.text"));
        chkAeroRecruitsHaveUnits.setToolTipText(resources.getString("chkAeroRecruitsHaveUnits.toolTipText"));
        chkAeroRecruitsHaveUnits.setName("chkAeroRecruitsHaveUnits");
        chkAeroRecruitsHaveUnits.setEnabled(isUseTurnover);

        chkUseSubContractSoldiers = new JCheckBox(resources.getString("chkUseSubContractSoldiers.text"));
        chkUseSubContractSoldiers.setToolTipText(resources.getString("chkUseSubContractSoldiers.toolTipText"));
        chkUseSubContractSoldiers.setName("chkUseSubContractSoldiers");
        chkUseSubContractSoldiers.setEnabled(isUseTurnover);

        JLabel lblServiceContractDuration = new JLabel(resources.getString("lblServiceContractDuration.text"));
        lblServiceContractDuration.setToolTipText(resources.getString("lblServiceContractDuration.toolTipText"));
        lblServiceContractDuration.setName("lblServiceContractDuration");
        lblServiceContractDuration.setEnabled(isUseTurnover);

        spnServiceContractDuration = new JSpinner(new SpinnerNumberModel(36, 0, 120, 1));
        spnServiceContractDuration
                .setToolTipText(resources.getString("lblServiceContractDuration.toolTipText"));
        spnServiceContractDuration.setName("spnServiceContractDuration");
        spnServiceContractDuration.setEnabled(isUseTurnover);

        JLabel lblServiceContractModifier = new JLabel(resources.getString("lblServiceContractModifier.text"));
        lblServiceContractModifier.setToolTipText(resources.getString("lblServiceContractModifier.toolTipText"));
        lblServiceContractModifier.setName("lblServiceContractModifier");
        lblServiceContractModifier.setEnabled(isUseTurnover);

        spnServiceContractModifier = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
        spnServiceContractModifier
                .setToolTipText(resources.getString("lblServiceContractModifier.toolTipText"));
        spnServiceContractModifier.setName("spnServiceContractModifier");
        spnServiceContractModifier.setEnabled(isUseTurnover);

        chkPayBonusDefault = new JCheckBox(resources.getString("chkPayBonusDefault.text"));
        chkPayBonusDefault.setToolTipText(resources.getString("chkPayBonusDefault.toolTipText"));
        chkPayBonusDefault.setName("chkPayBonusDefault");
        chkPayBonusDefault.setEnabled(isUseTurnover);
        chkPayBonusDefault.addActionListener(evt -> {
            boolean isEnabled = chkPayBonusDefault.isSelected();

            lblPayBonusDefaultThreshold.setEnabled(isEnabled);
            spnPayBonusDefaultThreshold.setEnabled(isEnabled);
        });

        lblPayBonusDefaultThreshold = new JLabel(resources.getString("lblPayBonusDefaultThreshold.text"));
        lblPayBonusDefaultThreshold
                .setToolTipText(resources.getString("lblPayBonusDefaultThreshold.toolTipText"));
        lblPayBonusDefaultThreshold.setName("lblPayBonusDefaultThreshold");
        lblPayBonusDefaultThreshold
                .setEnabled((isUseTurnover) && (campaign.getCampaignOptions().isPayBonusDefault()));

        spnPayBonusDefaultThreshold = new JSpinner(new SpinnerNumberModel(3, 0, 12, 1));
        spnPayBonusDefaultThreshold
                .setToolTipText(resources.getString("lblPayBonusDefaultThreshold.toolTipText"));
        spnPayBonusDefaultThreshold.setName("spnPayBonusDefaultThreshold");
        spnPayBonusDefaultThreshold
                .setEnabled((isUseTurnover) && (campaign.getCampaignOptions().isPayBonusDefault()));

        turnoverAndRetentionSettingsPanel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("turnoverAndRetentionSettingsPanel.title")));
        turnoverAndRetentionSettingsPanel.setName("turnoverAndRetentionSettingsPanel");
        turnoverAndRetentionSettingsPanel.setEnabled(isUseTurnover);

        final GroupLayout layout = new GroupLayout(turnoverAndRetentionSettingsPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        turnoverAndRetentionSettingsPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblTurnoverFixedTargetNumber)
                                .addComponent(spnTurnoverFixedTargetNumber,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblTurnoverFrequency)
                                .addComponent(comboTurnoverFrequency,
                                        Alignment.LEADING))
                        .addComponent(chkUseContractCompletionRandomRetirement)
                        .addComponent(chkUseRandomFounderTurnover)
                        .addComponent(chkUseFounderRetirement)
                        .addComponent(chkTrackOriginalUnit)
                        .addComponent(chkAeroRecruitsHaveUnits)
                        .addComponent(chkUseSubContractSoldiers)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblServiceContractDuration)
                                .addComponent(spnServiceContractDuration,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblServiceContractModifier)
                                .addComponent(spnServiceContractModifier,
                                        Alignment.LEADING))
                        .addComponent(chkPayBonusDefault)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPayBonusDefaultThreshold)
                                .addComponent(spnPayBonusDefaultThreshold,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTurnoverFixedTargetNumber)
                                .addComponent(spnTurnoverFixedTargetNumber))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTurnoverFrequency)
                                .addComponent(comboTurnoverFrequency))
                        .addComponent(chkUseContractCompletionRandomRetirement)
                        .addComponent(chkUseRandomFounderTurnover)
                        .addComponent(chkUseFounderRetirement)
                        .addComponent(chkTrackOriginalUnit)
                        .addComponent(chkAeroRecruitsHaveUnits)
                        .addComponent(chkUseSubContractSoldiers)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblServiceContractDuration)
                                .addComponent(spnServiceContractDuration))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblServiceContractModifier)
                                .addComponent(spnServiceContractModifier))
                        .addComponent(chkPayBonusDefault)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPayBonusDefaultThreshold)
                                .addComponent(spnPayBonusDefaultThreshold)));

        return turnoverAndRetentionSettingsPanel;
    }

    private JPanel createTurnoverAndRetentionModifiersPanel() {
        boolean isUseTurnover = campaign.getCampaignOptions().isUseRandomRetirement();

        chkUseCustomRetirementModifiers = new JCheckBox(
                resources.getString("chkUseCustomRetirementModifiers.text"));
        chkUseCustomRetirementModifiers
                .setToolTipText(resources.getString("chkUseCustomRetirementModifiers.toolTipText"));
        chkUseCustomRetirementModifiers.setName("chkUseCustomRetirementModifiers");
        chkUseCustomRetirementModifiers.setEnabled(campaign.getCampaignOptions().isUseRandomRetirement());

        chkUseFatigueModifiers = new JCheckBox(resources.getString("chkUseFatigueModifiers.text"));
        chkUseFatigueModifiers.setToolTipText(resources.getString("chkUseFatigueModifiers.toolTipText"));
        chkUseFatigueModifiers.setName("chkUseFatigueModifiers");
        chkUseFatigueModifiers.setEnabled((isUseTurnover) && (campaign.getCampaignOptions().isUseFatigue()));

        chkUseSkillModifiers = new JCheckBox(resources.getString("chkUseSkillModifiers.text"));
        chkUseSkillModifiers.setToolTipText(resources.getString("chkUseSkillModifiers.toolTipText"));
        chkUseSkillModifiers.setName("chkUseSkillModifiers");
        chkUseSkillModifiers.setEnabled(isUseTurnover);

        chkUseAgeModifiers = new JCheckBox(resources.getString("chkUseAgeModifiers.text"));
        chkUseAgeModifiers.setToolTipText(resources.getString("chkUseAgeModifiers.toolTipText"));
        chkUseAgeModifiers.setName("chkUseAgeModifiers");
        chkUseAgeModifiers.setEnabled(isUseTurnover);

        chkUseUnitRatingModifiers = new JCheckBox(resources.getString("chkUseUnitRatingModifiers.text"));
        chkUseUnitRatingModifiers.setToolTipText(resources.getString("chkUseUnitRatingModifiers.toolTipText"));
        chkUseUnitRatingModifiers.setName("chkUseUnitRatingModifiers");
        chkUseUnitRatingModifiers.setEnabled(isUseTurnover);

        chkUseFactionModifiers = new JCheckBox(resources.getString("chkUseFactionModifiers.text"));
        chkUseFactionModifiers.setToolTipText(resources.getString("chkUseFactionModifiers.toolTipText"));
        chkUseFactionModifiers.setName("chkUseFactionModifiers");
        chkUseFactionModifiers.setEnabled(isUseTurnover);

        chkUseMissionStatusModifiers = new JCheckBox(resources.getString("chkUseMissionStatusModifiers.text"));
        chkUseMissionStatusModifiers
                .setToolTipText(resources.getString("chkUseMissionStatusModifiers.toolTipText"));
        chkUseMissionStatusModifiers.setName("chkUseMissionStatusModifiers");
        chkUseMissionStatusModifiers.setEnabled(isUseTurnover);

        chkUseHostileTerritoryModifiers = new JCheckBox(
                resources.getString("chkUseHostileTerritoryModifiers.text"));
        chkUseHostileTerritoryModifiers
                .setToolTipText(resources.getString("chkUseHostileTerritoryModifiers.toolTipText"));
        chkUseHostileTerritoryModifiers.setName("chkUseHostileTerritoryModifiers");
        chkUseHostileTerritoryModifiers.setEnabled(isUseTurnover);

        chkUseFamilyModifiers = new JCheckBox(resources.getString("chkUseFamilyModifiers.text"));
        chkUseFamilyModifiers.setToolTipText(resources.getString("chkUseFamilyModifiers.toolTipText"));
        chkUseFamilyModifiers.setName("chkUseFamilyModifiers");
        chkUseFamilyModifiers.setEnabled(isUseTurnover);

        chkUseLoyaltyModifiers = new JCheckBox(resources.getString("chkUseLoyaltyModifiers.text"));
        chkUseLoyaltyModifiers.setToolTipText(resources.getString("chkUseLoyaltyModifiers.toolTipText"));
        chkUseLoyaltyModifiers.setName("chkUseLoyaltyModifiers");
        chkUseLoyaltyModifiers.setEnabled(isUseTurnover);
        chkUseLoyaltyModifiers.addActionListener(evt -> {
            final boolean isEnabled = chkUseLoyaltyModifiers.isSelected();

            for (Component component : loyaltySubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            loyaltySubPanel.setEnabled(isEnabled);
        });

        createLoyaltySubPanel(isUseTurnover);

        turnoverAndRetentionModifiersPanel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("turnoverAndRetentionModifiersPanel.title")));
        turnoverAndRetentionModifiersPanel.setName("turnoverAndRetentionModifiersPanel");
        turnoverAndRetentionModifiersPanel.setEnabled(isUseTurnover);

        final GroupLayout layout = new GroupLayout(turnoverAndRetentionModifiersPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        turnoverAndRetentionModifiersPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseCustomRetirementModifiers)
                        .addComponent(chkUseFatigueModifiers)
                        .addComponent(chkUseSkillModifiers)
                        .addComponent(chkUseAgeModifiers)
                        .addComponent(chkUseUnitRatingModifiers)
                        .addComponent(chkUseFactionModifiers)
                        .addComponent(chkUseHostileTerritoryModifiers)
                        .addComponent(chkUseMissionStatusModifiers)
                        .addComponent(chkUseFamilyModifiers)
                        .addGap(15)
                        .addComponent(chkUseLoyaltyModifiers)
                        .addComponent(loyaltySubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseCustomRetirementModifiers)
                        .addComponent(chkUseFatigueModifiers)
                        .addComponent(chkUseSkillModifiers)
                        .addComponent(chkUseAgeModifiers)
                        .addComponent(chkUseUnitRatingModifiers)
                        .addComponent(chkUseFactionModifiers)
                        .addComponent(chkUseHostileTerritoryModifiers)
                        .addComponent(chkUseMissionStatusModifiers)
                        .addComponent(chkUseFamilyModifiers)
                        .addComponent(chkUseLoyaltyModifiers)
                        .addComponent(loyaltySubPanel));

        return turnoverAndRetentionModifiersPanel;
    }

    private void createLoyaltySubPanel(boolean isUseTurnover) {
        chkUseHideLoyalty = new JCheckBox(resources.getString("chkUseHideLoyalty.text"));
        chkUseHideLoyalty.setToolTipText(resources.getString("chkUseHideLoyalty.toolTipText"));
        chkUseHideLoyalty.setName("chkUseHideLoyalty");
        chkUseHideLoyalty.setEnabled(isUseTurnover && campaign.getCampaignOptions().isUseLoyaltyModifiers());

        loyaltySubPanel.setBorder(BorderFactory.createTitledBorder(""));
        loyaltySubPanel.setName("loyaltySubPanel");
        loyaltySubPanel.setEnabled(isUseTurnover && campaign.getCampaignOptions().isUseLoyaltyModifiers());

        final GroupLayout layout = new GroupLayout(loyaltySubPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        loyaltySubPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseHideLoyalty));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseHideLoyalty));
    }

    private JPanel createTurnoverAndRetentionPayoutPanel() {
        boolean isUseTurnover = campaign.getCampaignOptions().isUseRandomRetirement();

        JLabel lblPayoutRateOfficer = new JLabel(resources.getString("lblPayoutRateOfficer.text"));
        lblPayoutRateOfficer.setToolTipText(resources.getString("lblPayoutRateOfficer.toolTipText"));
        lblPayoutRateOfficer.setName("lblPayoutRateOfficer");
        lblPayoutRateOfficer.setEnabled(isUseTurnover);

        spnPayoutRateOfficer = new JSpinner(new SpinnerNumberModel(3, 0, 12, 1));
        spnPayoutRateOfficer.setToolTipText(resources.getString("lblPayoutRateOfficer.toolTipText"));
        spnPayoutRateOfficer.setName("spnPayoutRateOfficer");
        spnPayoutRateOfficer.setEnabled(isUseTurnover);

        JLabel lblPayoutRateEnlisted = new JLabel(resources.getString("lblPayoutRateEnlisted.text"));
        lblPayoutRateEnlisted.setToolTipText(resources.getString("lblPayoutRateEnlisted.toolTipText"));
        lblPayoutRateEnlisted.setName("lblPayoutRateEnlisted");
        lblPayoutRateEnlisted.setEnabled(isUseTurnover);

        spnPayoutRateEnlisted = new JSpinner(new SpinnerNumberModel(3, 0, 12, 1));
        spnPayoutRateEnlisted.setToolTipText(resources.getString("lblPayoutRateEnlisted.toolTipText"));
        spnPayoutRateEnlisted.setName("lblPayoutRateEnlisted");
        spnPayoutRateEnlisted.setEnabled(isUseTurnover);

        JLabel lblPayoutRetirementMultiplier = new JLabel(resources.getString("lblPayoutRetirementMultiplier.text"));
        lblPayoutRetirementMultiplier.setToolTipText(resources.getString("lblPayoutRetirementMultiplier.toolTipText"));
        lblPayoutRetirementMultiplier.setName("lblPayoutRetirementMultiplier");
        lblPayoutRetirementMultiplier.setEnabled(isUseTurnover);

        spnPayoutRetirementMultiplier = new JSpinner(new SpinnerNumberModel(24, 1, 120, 1));
        spnPayoutRetirementMultiplier.setToolTipText(resources.getString("lblPayoutRateEnlisted.toolTipText"));
        spnPayoutRetirementMultiplier.setName("spnPayoutRetirementMultiplier");
        spnPayoutRetirementMultiplier.setEnabled(isUseTurnover);

        chkUsePayoutServiceBonus = new JCheckBox(resources.getString("chkUsePayoutServiceBonus.text"));
        chkUsePayoutServiceBonus.setToolTipText(resources.getString("chkUsePayoutServiceBonus.toolTipText"));
        chkUsePayoutServiceBonus.setName("chkUsePayoutServiceBonus");
        chkUsePayoutServiceBonus.setEnabled(isUseTurnover);
        chkUsePayoutServiceBonus.addActionListener(evt -> {
            final boolean isEnabled = chkUsePayoutServiceBonus.isSelected();

            for (Component component : payoutServiceBonusSubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            payoutServiceBonusSubPanel.setEnabled(isEnabled);
        });

        createPayoutServiceBonusSubPanel(isUseTurnover);

        turnoverAndRetentionPayoutPanel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("turnoverAndRetentionPayoutPanel.title")));
        turnoverAndRetentionPayoutPanel.setName("turnoverAndRetentionPayoutPanel");
        turnoverAndRetentionPayoutPanel.setEnabled(isUseTurnover);

        final GroupLayout layout = new GroupLayout(turnoverAndRetentionPayoutPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        turnoverAndRetentionPayoutPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPayoutRateOfficer)
                                .addComponent(spnPayoutRateOfficer, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPayoutRateEnlisted)
                                .addComponent(spnPayoutRateEnlisted, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPayoutRetirementMultiplier)
                                .addComponent(spnPayoutRetirementMultiplier,
                                        Alignment.LEADING))
                        .addGap(15)
                        .addComponent(chkUsePayoutServiceBonus)
                        .addComponent(payoutServiceBonusSubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPayoutRateOfficer)
                                .addComponent(spnPayoutRateOfficer))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPayoutRateEnlisted)
                                .addComponent(spnPayoutRateEnlisted))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPayoutRetirementMultiplier)
                                .addComponent(spnPayoutRetirementMultiplier))
                        .addComponent(chkUsePayoutServiceBonus)
                        .addComponent(payoutServiceBonusSubPanel));

        return turnoverAndRetentionPayoutPanel;
    }

    private void createPayoutServiceBonusSubPanel(boolean isUseTurnover) {
        boolean isUseServiceBonus = campaign.getCampaignOptions().isUsePayoutServiceBonus();

        lblPayoutServiceBonusRate = new JLabel(resources.getString("lblPayoutServiceBonusRate.text"));
        lblPayoutServiceBonusRate.setToolTipText(resources.getString("lblPayoutServiceBonusRate.toolTipText"));
        lblPayoutServiceBonusRate.setName("lblPayoutServiceBonusRate");
        lblPayoutServiceBonusRate.setEnabled((isUseTurnover) && (isUseServiceBonus));

        spnPayoutServiceBonusRate = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        spnPayoutServiceBonusRate.setToolTipText(resources.getString("lblPayoutServiceBonusRate.toolTipText"));
        spnPayoutServiceBonusRate.setName("spnPayoutServiceBonusRate");
        spnPayoutServiceBonusRate.setEnabled((isUseTurnover) && (isUseServiceBonus));

        payoutServiceBonusSubPanel.setBorder(BorderFactory.createTitledBorder(""));
        payoutServiceBonusSubPanel.setName("payoutServiceBonusSubPanel");
        payoutServiceBonusSubPanel.setEnabled((isUseTurnover) && (isUseServiceBonus));

        final GroupLayout layout = new GroupLayout(payoutServiceBonusSubPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        payoutServiceBonusSubPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPayoutServiceBonusRate)
                                .addComponent(spnPayoutServiceBonusRate,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPayoutServiceBonusRate)
                                .addComponent(spnPayoutServiceBonusRate)));
    }

    private JPanel createTurnoverAndRetentionUnitCohesionPanel() {
        boolean isUseTurnover = campaign.getCampaignOptions().isUseRandomRetirement();

        chkUseAdministrativeStrain = new JCheckBox(resources.getString("chkUseAdministrativeStrain.text"));
        chkUseAdministrativeStrain
                .setToolTipText(resources.getString("chkUseAdministrativeStrain.toolTipText"));
        chkUseAdministrativeStrain.setName("chkUseAdministrativeStrain");
        chkUseAdministrativeStrain.setEnabled(isUseTurnover);
        chkUseAdministrativeStrain.addActionListener(evt -> {
            final boolean isEnabled = chkUseAdministrativeStrain.isSelected();

            for (Component component : administrativeStrainSubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            administrativeStrainSubPanel.setEnabled(isEnabled);
        });

        createAdministrativeStrainSubPanel(isUseTurnover);

        chkUseManagementSkill = new JCheckBox(resources.getString("chkUseManagementSkill.text"));
        chkUseManagementSkill.setToolTipText(resources.getString("chkUseManagementSkill.toolTipText"));
        chkUseManagementSkill.setName("chkUseManagementSkill");
        chkUseManagementSkill.setEnabled(isUseTurnover);
        chkUseManagementSkill.addActionListener(evt -> {
            final boolean isEnabled = chkUseManagementSkill.isSelected();

            for (Component component : managementSkillSubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            managementSkillSubPanel.setEnabled(isEnabled);
        });

        createManagementSkillSubPanel(isUseTurnover);

        turnoverAndRetentionUnitCohesionPanel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("turnoverAndRetentionUnitCohesionPanel.title")));
        turnoverAndRetentionUnitCohesionPanel.setName("turnoverAndRetentionUnitCohesionPanel");
        turnoverAndRetentionUnitCohesionPanel.setEnabled(isUseTurnover);

        final GroupLayout layout = new GroupLayout(turnoverAndRetentionUnitCohesionPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        turnoverAndRetentionUnitCohesionPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseAdministrativeStrain)
                        .addComponent(administrativeStrainSubPanel)
                        .addGap(15)
                        .addComponent(chkUseManagementSkill)
                        .addComponent(managementSkillSubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseAdministrativeStrain)
                        .addComponent(administrativeStrainSubPanel)
                        .addComponent(chkUseManagementSkill)
                        .addComponent(managementSkillSubPanel));

        return turnoverAndRetentionUnitCohesionPanel;
    }

    private void createAdministrativeStrainSubPanel(boolean isUseTurnover) {
        boolean isUseAdministrativeStrain = campaign.getCampaignOptions().isUseAdministrativeStrain();

        lblAdministrativeCapacity = new JLabel(resources.getString("lblAdministrativeCapacity.text"));
        lblAdministrativeCapacity.setToolTipText(resources.getString("lblAdministrativeCapacity.toolTipText"));
        lblAdministrativeCapacity.setName("lblAdministrativeCapacity");
        lblAdministrativeCapacity.setEnabled((isUseTurnover) && (isUseAdministrativeStrain));

        spnAdministrativeCapacity = new JSpinner(new SpinnerNumberModel(10, 1, 30, 1));
        spnAdministrativeCapacity.setToolTipText(resources.getString("lblAdministrativeCapacity.toolTipText"));
        spnAdministrativeCapacity.setName("spnAdministrativeCapacity");
        spnAdministrativeCapacity.setEnabled((isUseTurnover) && (isUseAdministrativeStrain));

        lblMultiCrewStrainDivider = new JLabel(resources.getString("lblMultiCrewStrainDivider.text"));
        lblMultiCrewStrainDivider.setToolTipText(resources.getString("lblMultiCrewStrainDivider.toolTipText"));
        lblMultiCrewStrainDivider.setName("lblMultiCrewStrainDivider");
        lblMultiCrewStrainDivider.setEnabled((isUseTurnover) && (isUseAdministrativeStrain));

        spnMultiCrewStrainDivider = new JSpinner(new SpinnerNumberModel(5, 1, 25, 1));
        spnMultiCrewStrainDivider.setToolTipText(resources.getString("lblMultiCrewStrainDivider.toolTipText"));
        spnMultiCrewStrainDivider.setName("spnMultiCrewStrainDivider");
        spnMultiCrewStrainDivider.setEnabled((isUseTurnover) && (isUseAdministrativeStrain));

        administrativeStrainSubPanel.setBorder(BorderFactory.createTitledBorder(""));
        administrativeStrainSubPanel.setName("administrativeStrainSubPanel");
        administrativeStrainSubPanel.setEnabled((isUseTurnover) && (isUseAdministrativeStrain));

        final GroupLayout layout = new GroupLayout(administrativeStrainSubPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        administrativeStrainSubPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblAdministrativeCapacity)
                                .addComponent(spnAdministrativeCapacity,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMultiCrewStrainDivider)
                                .addComponent(spnMultiCrewStrainDivider,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblAdministrativeCapacity)
                                .addComponent(spnAdministrativeCapacity))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMultiCrewStrainDivider)
                                .addComponent(spnMultiCrewStrainDivider)));
    }

    private void createManagementSkillSubPanel(boolean isUseTurnover) {
        boolean isUseManagementSkill = campaign.getCampaignOptions().isUseManagementSkill();

        chkUseCommanderLeadershipOnly = new JCheckBox(
                resources.getString("chkUseCommanderLeadershipOnly.text"));
        chkUseCommanderLeadershipOnly
                .setToolTipText(resources.getString("chkUseCommanderLeadershipOnly.toolTipText"));
        chkUseCommanderLeadershipOnly.setName("chkUseCommanderLeadershipOnly");
        chkUseCommanderLeadershipOnly.setEnabled((isUseTurnover) && (isUseManagementSkill));

        lblManagementSkillPenalty = new JLabel(resources.getString("lblManagementSkillPenalty.text"));
        lblManagementSkillPenalty.setToolTipText(resources.getString("lblManagementSkillPenalty.toolTipText"));
        lblManagementSkillPenalty.setName("lblManagementSkillPenalty");
        lblManagementSkillPenalty.setEnabled((isUseTurnover) && (isUseManagementSkill));

        spnManagementSkillPenalty = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        spnManagementSkillPenalty.setToolTipText(resources.getString("lblManagementSkillPenalty.toolTipText"));
        spnManagementSkillPenalty.setName("spnManagementSkillPenalty");
        spnManagementSkillPenalty.setEnabled((isUseTurnover) && (isUseManagementSkill));

        managementSkillSubPanel.setBorder(BorderFactory.createTitledBorder(""));
        managementSkillSubPanel.setName("managementSkillSubPanel");
        managementSkillSubPanel.setEnabled((isUseTurnover) && (isUseManagementSkill));

        final GroupLayout layout = new GroupLayout(managementSkillSubPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        managementSkillSubPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseCommanderLeadershipOnly)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblManagementSkillPenalty)
                                .addComponent(spnManagementSkillPenalty,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseCommanderLeadershipOnly)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblManagementSkillPenalty)
                                .addComponent(spnManagementSkillPenalty)));
    }

    private JPanel createPersonnelRandomizationPanel() {
        // Create Panel Components
        chkUseDylansRandomXP = new JCheckBox(resources.getString("chkUseDylansRandomXP.text"));
        chkUseDylansRandomXP.setToolTipText(resources.getString("chkUseDylansRandomXP.toolTipText"));
        chkUseDylansRandomXP.setName("chkUseDylansRandomXP");

        JLabel lblNonBinaryDiceSize = new JLabel(resources.getString("lblNonBinaryDiceSize.text"));
        lblNonBinaryDiceSize.setToolTipText(resources.getString("lblNonBinaryDiceSize.toolTipText"));
        lblNonBinaryDiceSize.setName("lblNonBinaryDiceSize");

        spnNonBinaryDiceSize = new JSpinner(new SpinnerNumberModel(60, 0, 100000, 1));
        spnNonBinaryDiceSize.setToolTipText(wordWrap(resources.getString("lblNonBinaryDiceSize.toolTipText")));
        spnNonBinaryDiceSize.setName("spnNonBinaryDiceSize");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("personnelRandomizationPanel.title")));
        panel.setName("personnelRandomizationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseDylansRandomXP)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblNonBinaryDiceSize)
                                .addComponent(spnNonBinaryDiceSize, Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseDylansRandomXP)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNonBinaryDiceSize)
                                .addComponent(spnNonBinaryDiceSize))
        );

        return panel;
    }

    private JPanel createRandomHistoriesPanel() {
        randomOriginOptionsPanel = new RandomOriginOptionsPanel(getFrame(), campaign, comboFaction);

        chkUseRandomPersonalities = new JCheckBox(resources.getString("chkUseRandomPersonalities.text"));
        chkUseRandomPersonalities.setToolTipText(resources.getString("chkUseRandomPersonalities.toolTipText"));
        chkUseRandomPersonalities.setName("chkUseRandomPersonalities");

        chkUseRandomPersonalityReputation = new JCheckBox(
                resources.getString("chkUseRandomPersonalityReputation.text"));
        chkUseRandomPersonalityReputation
                .setToolTipText(resources.getString("chkUseRandomPersonalityReputation.toolTipText"));
        chkUseRandomPersonalityReputation.setName("chkUseRandomPersonalityReputation");

        chkUseIntelligenceXpMultiplier = new JCheckBox(
                resources.getString("chkUseIntelligenceXpMultiplier.text"));
        chkUseIntelligenceXpMultiplier
                .setToolTipText(resources.getString("chkUseIntelligenceXpMultiplier.toolTipText"));
        chkUseIntelligenceXpMultiplier.setName("chkUseIntelligenceXpMultiplier");

        chkUseSimulatedRelationships = new JCheckBox(resources.getString("chkUseSimulatedRelationships.text"));
        chkUseSimulatedRelationships.setToolTipText(resources.getString("chkUseSimulatedRelationships.toolTipText"));
        chkUseSimulatedRelationships.setName("chkUseSimulatedRelationships");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomHistoriesPanel.title")));
        panel.setName("randomHistoriesPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(randomOriginOptionsPanel)
                        .addComponent(chkUseRandomPersonalities)
                        .addComponent(chkUseRandomPersonalityReputation)
                        .addComponent(chkUseIntelligenceXpMultiplier)
                        .addComponent(chkUseSimulatedRelationships)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(randomOriginOptionsPanel)
                        .addComponent(chkUseRandomPersonalities)
                        .addComponent(chkUseRandomPersonalityReputation)
                        .addComponent(chkUseIntelligenceXpMultiplier)
                        .addComponent(chkUseSimulatedRelationships)
        );

        return panel;
    }

    private JPanel createFamilyPanel() {
        // Create Panel Components
        final JLabel lblFamilyDisplayLevel = new JLabel(resources.getString("lblFamilyDisplayLevel.text"));
        lblFamilyDisplayLevel.setToolTipText(resources.getString("lblFamilyDisplayLevel.toolTipText"));
        lblFamilyDisplayLevel.setName("lblFamilyDisplayLevel");

        comboFamilyDisplayLevel = new MMComboBox<>("comboFamilyDisplayLevel",
                FamilialRelationshipDisplayLevel.values());
        comboFamilyDisplayLevel.setToolTipText(resources.getString("lblFamilyDisplayLevel.toolTipText"));
        comboFamilyDisplayLevel.setName("comboFamilyDisplayLevel");

        // Programmatically Assign Accessibility Labels
        lblFamilyDisplayLevel.setLabelFor(comboFamilyDisplayLevel);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("familyPanel.title")));
        panel.setName("familyPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblFamilyDisplayLevel)
                                .addComponent(comboFamilyDisplayLevel,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFamilyDisplayLevel)
                                .addComponent(comboFamilyDisplayLevel)));

        return panel;
    }

    private JPanel createAnniversaryPanel() {
        chkAnnounceBirthdays = new JCheckBox(resources.getString("chkAnnounceBirthdays.text"));
        chkAnnounceBirthdays.setToolTipText(resources.getString("chkAnnounceBirthdays.toolTipText"));
        chkAnnounceBirthdays.setName("chkAnnounceBirthdays");
        chkAnnounceBirthdays.addActionListener(evt -> {
            final boolean isEnabled = chkAnnounceBirthdays.isSelected();

            chkAnnounceChildBirthdays.setEnabled(isEnabled);
        });

        chkAnnounceRecruitmentAnniversaries = new JCheckBox(resources.getString("chkAnnounceRecruitmentAnniversaries.text"));
        chkAnnounceRecruitmentAnniversaries.setToolTipText(resources.getString("chkAnnounceRecruitmentAnniversaries.toolTipText"));
        chkAnnounceRecruitmentAnniversaries.setName("chkAnnounceRecruitmentAnniversaries");

        chkAnnounceOfficersOnly = new JCheckBox(resources.getString("chkAnnounceOfficersOnly.text"));
        chkAnnounceOfficersOnly.setToolTipText(resources.getString("chkAnnounceOfficersOnly.toolTipText"));
        chkAnnounceOfficersOnly.setName("chkAnnounceOfficersOnly");
        chkAnnounceOfficersOnly.setEnabled(campaign.getCampaignOptions().isAnnounceBirthdays());

        chkAnnounceChildBirthdays = new JCheckBox(resources.getString("chkAnnounceChildBirthdays.text"));
        chkAnnounceChildBirthdays.setToolTipText(resources.getString("chkAnnounceChildBirthdays.toolTipText"));
        chkAnnounceChildBirthdays.setName("chkAnnounceChildBirthdays");
        chkAnnounceChildBirthdays.setEnabled(campaign.getCampaignOptions().isAnnounceBirthdays());

        anniversaryPanel.setBorder(
                BorderFactory.createTitledBorder(resources.getString("anniversaryPanel.title")));
        anniversaryPanel.setName("anniversaryPanel");

        final GroupLayout layout = new GroupLayout(anniversaryPanel);
        anniversaryPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkAnnounceBirthdays)
                        .addComponent(chkAnnounceRecruitmentAnniversaries)
                        .addComponent(chkAnnounceOfficersOnly)
                        .addComponent(chkAnnounceChildBirthdays));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkAnnounceBirthdays)
                        .addComponent(chkAnnounceRecruitmentAnniversaries)
                        .addComponent(chkAnnounceOfficersOnly)
                        .addComponent(chkAnnounceChildBirthdays));

        return anniversaryPanel;
    }

    private JPanel createDependentPanel() {
        // Create Panel Components
        chkUseRandomDependentAddition = new JCheckBox(resources.getString("chkUseRandomDependentAddition.text"));
        chkUseRandomDependentAddition.setToolTipText(resources.getString("chkUseRandomDependentAddition.toolTipText"));
        chkUseRandomDependentAddition.setName("chkUseRandomDependentAddition");

        chkUseRandomDependentRemoval = new JCheckBox(resources.getString("chkUseRandomDependentRemoval.text"));
        chkUseRandomDependentRemoval.setToolTipText(resources.getString("chkUseRandomDependentRemoval.toolTipText"));
        chkUseRandomDependentRemoval.setName("chkUseRandomDependentRemoval");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("dependentPanel.title")));
        panel.setToolTipText(resources.getString("dependentPanel.toolTipText"));
        panel.setName("dependentPanel");

        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseRandomDependentAddition)
                        .addComponent(chkUseRandomDependentRemoval)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseRandomDependentAddition)
                        .addComponent(chkUseRandomDependentRemoval)
        );

        return panel;
    }

    private JPanel createPersonnelRemovalPanel() {
        // Create Panel Components
        chkUsePersonnelRemoval = new JCheckBox(resources.getString("chkUsePersonnelRemoval.text"));
        chkUsePersonnelRemoval.setToolTipText(resources.getString("chkUsePersonnelRemoval.toolTipText"));
        chkUsePersonnelRemoval.setName("chkUsePersonnelRemoval");
        chkUsePersonnelRemoval.addActionListener(evt -> {
            final boolean isEnabled = chkUsePersonnelRemoval.isSelected();

            for (Component component : personnelRemovalSubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            personnelRemovalSubPanel.setEnabled(isEnabled);
        });

        personnelRemovalSubPanel = createPersonnelRemovalSubPanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelRemovalPanel.title")));
        panel.setToolTipText(resources.getString("personnelRemovalPanel.toolTipText"));
        panel.setName("personnelRemovalPanel");

        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUsePersonnelRemoval)
                        .addComponent(personnelRemovalSubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUsePersonnelRemoval)
                        .addComponent(personnelRemovalSubPanel));

        return panel;
    }

    private JPanel createPersonnelRemovalSubPanel() {
        boolean isEnabled = campaign.getCampaignOptions().isUsePersonnelRemoval();

        // Create Panel Components
        chkUseRemovalExemptCemetery = new JCheckBox(resources.getString("chkUseRemovalExemptCemetery.text"));
        chkUseRemovalExemptCemetery
                .setToolTipText(resources.getString("chkUseRemovalExemptCemetery.toolTipText"));
        chkUseRemovalExemptCemetery.setName("chkUseRemovalExemptCemetery");
        chkUseRemovalExemptCemetery.setEnabled(isEnabled);

        chkUseRemovalExemptRetirees = new JCheckBox(resources.getString("chkUseRemovalExemptRetirees.text"));
        chkUseRemovalExemptRetirees
                .setToolTipText(resources.getString("chkUseRemovalExemptRetirees.toolTipText"));
        chkUseRemovalExemptRetirees.setName("chkUseRemovalExemptRetirees");
        chkUseRemovalExemptRetirees.setEnabled(isEnabled);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setToolTipText(resources.getString("personnelRemovalPanel.toolTipText"));
        panel.setName("personnelRemovalPanel");

        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseRemovalExemptCemetery)
                        .addComponent(chkUseRemovalExemptRetirees));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseRemovalExemptCemetery)
                        .addComponent(chkUseRemovalExemptRetirees));

        return panel;
    }

    private JPanel createTurnoverAndRetentionFatiguePanel() {
        chkUseFatigue = new JCheckBox(resources.getString("chkUseFatigue.text"));
        chkUseFatigue.setToolTipText(resources.getString("chkUseFatigue.toolTipText"));
        chkUseFatigue.setName("chkUseFatigue");
        chkUseFatigue.addActionListener(evt -> {
            final boolean isEnabled = chkUseFatigue.isSelected();

            for (Component component : fatigueSubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }

            fatigueSubPanel.setEnabled(isEnabled);

            chkUseFatigueModifiers.setEnabled(isEnabled);
        });

        createFatigueSubPanel();

        turnoverAndRetentionFatiguePanel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("turnoverAndRetentionFatiguePanel.title")));
        turnoverAndRetentionFatiguePanel.setName("turnoverAndRetentionFatiguePanel");

        final GroupLayout layout = new GroupLayout(turnoverAndRetentionFatiguePanel);
        turnoverAndRetentionFatiguePanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseFatigue)
                        .addComponent(fatigueSubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseFatigue)
                        .addComponent(fatigueSubPanel));

        return turnoverAndRetentionFatiguePanel;
    }

    private JPanel createSalaryPanel() {
        // Create Panel Components
        chkDisableSecondaryRoleSalary = new JCheckBox(
                resources.getString("chkDisableSecondaryRoleSalary.text"));
        chkDisableSecondaryRoleSalary
                .setToolTipText(resources.getString("chkDisableSecondaryRoleSalary.toolTipText"));
        chkDisableSecondaryRoleSalary.setName("chkDisableSecondaryRoleSalary");

        final JPanel salaryMultiplierPanel = createSalaryMultiplierPanel();

        final JPanel salaryExperienceModifierPanel = createSalaryExperienceMultiplierPanel();

        final JPanel baseSalaryPanel = createBaseSalaryPanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("salaryPanel.title")));
        panel.setName("salaryPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkDisableSecondaryRoleSalary)
                        .addComponent(salaryMultiplierPanel)
                        .addComponent(salaryExperienceModifierPanel)
                        .addComponent(baseSalaryPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkDisableSecondaryRoleSalary)
                        .addComponent(salaryMultiplierPanel)
                        .addComponent(salaryExperienceModifierPanel)
                        .addComponent(baseSalaryPanel));

        return panel;
    }

    private JPanel createSalaryMultiplierPanel() {
        // Create Panel Components
        final JLabel lblAntiMekSalary = new JLabel(resources.getString("lblAntiMekSalary.text"));
        lblAntiMekSalary.setToolTipText(resources.getString("lblAntiMekSalary.toolTipText"));
        lblAntiMekSalary.setName("lblAntiMekSalary");

        spnAntiMekSalary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.05));
        spnAntiMekSalary.setToolTipText(resources.getString("lblAntiMekSalary.toolTipText"));
        spnAntiMekSalary.setName("spnAntiMekSalary");

        final JLabel lblSpecialistInfantrySalary = new JLabel(
                resources.getString("lblSpecialistInfantrySalary.text"));
        lblSpecialistInfantrySalary
                .setToolTipText(resources.getString("lblSpecialistInfantrySalary.toolTipText"));
        lblSpecialistInfantrySalary.setName("lblSpecialistInfantrySalary");

        spnSpecialistInfantrySalary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.05));
        spnSpecialistInfantrySalary
                .setToolTipText(resources.getString("lblSpecialistInfantrySalary.toolTipText"));
        spnSpecialistInfantrySalary.setName("spnSpecialistInfantrySalary");

        // Programmatically Assign Accessibility Labels\
        lblAntiMekSalary.setLabelFor(spnAntiMekSalary);
        lblSpecialistInfantrySalary.setLabelFor(spnSpecialistInfantrySalary);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("salaryMultiplierPanel.title")));
        panel.setToolTipText(resources.getString("salaryMultiplierPanel.toolTipText"));
        panel.setName("salaryMultiplierPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblAntiMekSalary)
                                .addComponent(spnAntiMekSalary)
                                .addComponent(lblSpecialistInfantrySalary)
                                .addComponent(spnSpecialistInfantrySalary,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblAntiMekSalary)
                                .addComponent(spnAntiMekSalary)
                                .addComponent(lblSpecialistInfantrySalary)
                                .addComponent(spnSpecialistInfantrySalary)));

        return panel;
    }

    private JPanel createSalaryExperienceMultiplierPanel() {
        final JPanel panel = new JPanel(new GridLayout(2, 8));
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("salaryExperienceMultiplierPanel.title")));
        panel.setToolTipText(resources.getString("salaryExperienceMultiplierPanel.toolTipText"));
        panel.setName("salaryExperienceMultiplierPanel");

        spnSalaryExperienceMultipliers = new HashMap<>();
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final String toolTipText = String.format(
                    resources.getString("lblSalaryExperienceMultiplier.toolTipText"),
                    skillLevel);

            final JLabel label = new JLabel(skillLevel.toString());
            label.setToolTipText(toolTipText);
            label.setName("lbl" + skillLevel);
            panel.add(label);

            spnSalaryExperienceMultipliers.put(skillLevel,
                    new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05)));
            spnSalaryExperienceMultipliers.get(skillLevel).setToolTipText(toolTipText);
            spnSalaryExperienceMultipliers.get(skillLevel).setName("spn" + skillLevel);
            panel.add(spnSalaryExperienceMultipliers.get(skillLevel));

            label.setLabelFor(spnSalaryExperienceMultipliers.get(skillLevel));
        }

        return panel;
    }

    private JPanel createBaseSalaryPanel() {
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        final JPanel panel = new JPanel(
                new GridLayout((int) Math.ceil((double) (personnelRoles.length - 1) / 3.0), 6));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("baseSalaryPanel.title")));
        panel.setPreferredSize(new Dimension(200, 200));

        spnBaseSalary = new JSpinner[personnelRoles.length];
        for (final PersonnelRole personnelRole : personnelRoles) {
            // Create Reused Values
            final String toolTipText = String.format(resources.getString("lblBaseSalary.toolTipText"),
                    personnelRole.toString());

            // Create Panel Components
            final JLabel label = new JLabel(personnelRole.toString());
            label.setToolTipText(toolTipText);
            label.setName("lbl" + personnelRole.name());
            panel.add(label);

            final JSpinner salarySpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 10.0));
            salarySpinner.setToolTipText(toolTipText);
            salarySpinner.setName("spn" + personnelRole.name());
            panel.add(salarySpinner);

            // Programmatically Assign Accessibility Labels
            label.setLabelFor(salarySpinner);

            // Component Tracking Assignment
            spnBaseSalary[personnelRole.ordinal()] = salarySpinner;
        }

        return panel;
    }

    private JPanel createMarriagePanel() {
        // Create Panel Components
        chkUseManualMarriages = new JCheckBox(resources.getString("chkUseManualMarriages.text"));
        chkUseManualMarriages.setToolTipText(resources.getString("chkUseManualMarriages.toolTipText"));
        chkUseManualMarriages.setName("chkUseManualMarriages");

        chkUseClanPersonnelMarriages = new JCheckBox(resources.getString("chkUseClanPersonnelMarriages.text"));
        chkUseClanPersonnelMarriages
                .setToolTipText(resources.getString("chkUseClanPersonnelMarriages.toolTipText"));
        chkUseClanPersonnelMarriages.setName("chkUseClanPersonnelMarriages");
        chkUseClanPersonnelMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClanPersonnelMarriages
                    .setEnabled(!method.isNone() && chkUseClanPersonnelMarriages.isSelected());
        });

        chkUsePrisonerMarriages = new JCheckBox(resources.getString("chkUsePrisonerMarriages.text"));
        chkUsePrisonerMarriages.setToolTipText(resources.getString("chkUsePrisonerMarriages.toolTipText"));
        chkUsePrisonerMarriages.setName("chkUsePrisonerMarriages");
        chkUsePrisonerMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerMarriages
                    .setEnabled(!method.isNone() && chkUsePrisonerMarriages.isSelected());
        });

        final JLabel lblNoInterestInMarriageDiceSize = new JLabel(resources.getString("lblNoInterestInMarriageDiceSize.text"));
        lblNoInterestInMarriageDiceSize.setToolTipText(wordWrap(resources.getString("lblNoInterestInMarriageDiceSize.toolTipText")));
        lblNoInterestInMarriageDiceSize.setName("lblNoInterestInMarriageDiceSize");

        spnNoInterestInMarriageDiceSize = new JSpinner(new SpinnerNumberModel(10, 1, 100000, 1));
        spnNoInterestInMarriageDiceSize.setToolTipText(wordWrap(resources.getString("lblNoInterestInMarriageDiceSize.toolTipText")));
        spnNoInterestInMarriageDiceSize.setName("spnNoInterestInMarriageDiceSize");

        final JLabel lblCheckMutualAncestorsDepth = new JLabel(
                resources.getString("lblCheckMutualAncestorsDepth.text"));
        lblCheckMutualAncestorsDepth
                .setToolTipText(resources.getString("lblCheckMutualAncestorsDepth.toolTipText"));
        lblCheckMutualAncestorsDepth.setName("lblCheckMutualAncestorsDepth");

        spnCheckMutualAncestorsDepth = new JSpinner(new SpinnerNumberModel(4, 0, 20, 1));
        spnCheckMutualAncestorsDepth
                .setToolTipText(resources.getString("lblCheckMutualAncestorsDepth.toolTipText"));
        spnCheckMutualAncestorsDepth.setName("spnCheckMutualAncestorsDepth");

        chkLogMarriageNameChanges = new JCheckBox(resources.getString("chkLogMarriageNameChanges.text"));
        chkLogMarriageNameChanges.setToolTipText(resources.getString("chkLogMarriageNameChanges.toolTipText"));
        chkLogMarriageNameChanges.setName("chkLogMarriageNameChanges");

        final JPanel marriageSurnameWeightsPanel = createMarriageSurnameWeightsPanel();

        final JPanel randomMarriagePanel = createRandomMarriagePanel();

        // Programmatically Assign Accessibility Labels
        lblCheckMutualAncestorsDepth.setLabelFor(spnCheckMutualAncestorsDepth);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("marriagePanel.title")));
        panel.setName("marriagePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseManualMarriages)
                        .addComponent(chkUseClanPersonnelMarriages)
                        .addComponent(chkUsePrisonerMarriages)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblNoInterestInMarriageDiceSize)
                                .addComponent(spnNoInterestInMarriageDiceSize, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblCheckMutualAncestorsDepth)
                                .addComponent(spnCheckMutualAncestorsDepth,
                                        Alignment.LEADING))
                        .addComponent(chkLogMarriageNameChanges)
                        .addComponent(marriageSurnameWeightsPanel)
                        .addComponent(randomMarriagePanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseManualMarriages)
                        .addComponent(chkUseClanPersonnelMarriages)
                        .addComponent(chkUsePrisonerMarriages)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNoInterestInMarriageDiceSize)
                                .addComponent(spnNoInterestInMarriageDiceSize))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCheckMutualAncestorsDepth)
                                .addComponent(spnCheckMutualAncestorsDepth))
                        .addComponent(chkLogMarriageNameChanges)
                        .addComponent(marriageSurnameWeightsPanel)
                        .addComponent(randomMarriagePanel));

        return panel;
    }

    private JPanel createMarriageSurnameWeightsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 6));
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("marriageSurnameWeightsPanel.title")));
        panel.setToolTipText(resources.getString("marriageSurnameWeightsPanel.toolTipText"));
        panel.setName("marriageSurnameWeightsPanel");

        spnMarriageSurnameWeights = new HashMap<>();
        for (final MergingSurnameStyle style : MergingSurnameStyle.values()) {
            if (style.isWeighted()) {
                continue;
            }
            final JLabel label = new JLabel(style.toString());
            label.setToolTipText(style.getToolTipText());
            label.setName("lbl" + style);
            panel.add(label);

            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
            spinner.setToolTipText(style.getToolTipText());
            spinner.setName("spn" + style);
            spnMarriageSurnameWeights.put(style, spinner);
            panel.add(spinner);

            label.setLabelFor(spinner);
        }

        return panel;
    }

    private JPanel createRandomMarriagePanel() {
        // Initialize Components Used in ActionListeners
        final JLabel lblRandomMarriageAgeRange = new JLabel();
        final JPanel percentageRandomMarriagePanel = new JDisableablePanel("percentageRandomMarriagePanel");

        // Create Panel Components
        final JLabel lblRandomMarriageMethod = new JLabel(resources.getString("lblRandomMarriageMethod.text"));
        lblRandomMarriageMethod.setToolTipText(resources.getString("lblRandomMarriageMethod.toolTipText"));
        lblRandomMarriageMethod.setName("lblRandomMarriageMethod");

        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
                RandomMarriageMethod.values());
        comboRandomMarriageMethod.setToolTipText(resources.getString("lblRandomMarriageMethod.toolTipText"));
        comboRandomMarriageMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomMarriageMethod) {
                    list.setToolTipText(((RandomMarriageMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomMarriageMethod.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            final boolean percentageEnabled = method.isDiceRoll();
            chkUseRandomClanPersonnelMarriages.setEnabled(enabled && chkUseClanPersonnelMarriages.isSelected());
            chkUseRandomPrisonerMarriages.setEnabled(enabled && chkUsePrisonerMarriages.isSelected());
            lblRandomMarriageAgeRange.setEnabled(enabled);
            spnRandomMarriageAgeRange.setEnabled(enabled);
            percentageRandomMarriagePanel.setEnabled(percentageEnabled);
        });

        chkUseRandomClanPersonnelMarriages = new JCheckBox(
                resources.getString("chkUseRandomClanPersonnelMarriages.text"));
        chkUseRandomClanPersonnelMarriages
                .setToolTipText(resources.getString("chkUseRandomClanPersonnelMarriages.toolTipText"));
        chkUseRandomClanPersonnelMarriages.setName("chkUseRandomClanPersonnelMarriages");

        chkUseRandomPrisonerMarriages = new JCheckBox(
                resources.getString("chkUseRandomPrisonerMarriages.text"));
        chkUseRandomPrisonerMarriages
                .setToolTipText(resources.getString("chkUseRandomPrisonerMarriages.toolTipText"));
        chkUseRandomPrisonerMarriages.setName("chkUseRandomPrisonerMarriages");

        lblRandomMarriageAgeRange.setText(resources.getString("lblRandomMarriageAgeRange.text"));
        lblRandomMarriageAgeRange.setToolTipText(resources.getString("lblRandomMarriageAgeRange.toolTipText"));
        lblRandomMarriageAgeRange.setName("lblRandomMarriageAgeRange");

        spnRandomMarriageAgeRange = new JSpinner(new SpinnerNumberModel(10, 0, null, 1.0));
        spnRandomMarriageAgeRange.setToolTipText(resources.getString("lblRandomMarriageAgeRange.toolTipText"));
        spnRandomMarriageAgeRange.setName("spnRandomMarriageAgeRange");

        createPercentageRandomMarriagePanel(percentageRandomMarriagePanel);

        // Programmatically Assign Accessibility Labels
        lblRandomMarriageMethod.setLabelFor(comboRandomMarriageMethod);
        lblRandomMarriageAgeRange.setLabelFor(spnRandomMarriageAgeRange);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomMarriagePanel.title")));
        panel.setName("randomMarriagePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomMarriageMethod)
                                .addComponent(comboRandomMarriageMethod, Alignment.LEADING))
                        .addComponent(chkUseRandomClanPersonnelMarriages)
                        .addComponent(chkUseRandomPrisonerMarriages)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomMarriageAgeRange)
                                .addComponent(spnRandomMarriageAgeRange,
                                        Alignment.LEADING))
                        .addComponent(percentageRandomMarriagePanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageMethod)
                                .addComponent(comboRandomMarriageMethod))
                        .addComponent(chkUseRandomClanPersonnelMarriages)
                        .addComponent(chkUseRandomPrisonerMarriages)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageAgeRange)
                                .addComponent(spnRandomMarriageAgeRange))
                        .addComponent(percentageRandomMarriagePanel));

        return panel;
    }

    private void createPercentageRandomMarriagePanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblRandomMarriageOppositeSexDiceSize = new JLabel(resources.getString("lblRandomMarriageOppositeSexDiceSize.text"));
        lblRandomMarriageOppositeSexDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomMarriageOppositeSexDiceSize.toolTipText")));
        lblRandomMarriageOppositeSexDiceSize.setName("lblRandomMarriageOppositeSexDiceSize");

        spnRandomMarriageDiceSize = new JSpinner(new SpinnerNumberModel(5000, 0, 100000, 1));
        spnRandomMarriageDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomMarriageOppositeSexDiceSize.toolTipText")));
        spnRandomMarriageDiceSize.setName("spnPercentageRandomMarriageOppositeSexChance");

        final JLabel lblRandomSameSexMarriageDiceSize = new JLabel(resources.getString("lblRandomSameSexMarriageDiceSize.text"));
        lblRandomSameSexMarriageDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomSameSexMarriageDiceSize.toolTipText")));
        lblRandomSameSexMarriageDiceSize.setName("lblRandomSameSexMarriageDiceSize");

        spnRandomSameSexMarriageDiceSize = new JSpinner(new SpinnerNumberModel(14, 0, 100000, 1));
        spnRandomSameSexMarriageDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomSameSexMarriageDiceSize.toolTipText")));
        spnRandomSameSexMarriageDiceSize.setName("spnRandomSameSexMarriageDiceSize");

        final JLabel lblRandomNewDependentMarriage = new JLabel(resources.getString("lblRandomNewDependentMarriage.text"));
        lblRandomNewDependentMarriage.setToolTipText(wordWrap(resources.getString("lblRandomNewDependentMarriage.toolTipText")));
        lblRandomNewDependentMarriage.setName("lblRandomNewDependentMarriage");

        spnRandomNewDependentMarriage = new JSpinner(new SpinnerNumberModel(20, 0, 100000, 1));
        spnRandomNewDependentMarriage.setToolTipText(wordWrap(resources.getString("lblRandomNewDependentMarriage.toolTipText")));
        spnRandomNewDependentMarriage.setName("spnRandomNewDependentMarriage");

        // Programmatically Assign Accessibility Labels
        lblRandomMarriageOppositeSexDiceSize.setLabelFor(spnRandomMarriageDiceSize);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomMarriagePanel.title")));
        panel.setToolTipText(RandomMarriageMethod.DICE_ROLL.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomMarriageOppositeSexDiceSize)
                                .addComponent(spnRandomMarriageDiceSize, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomSameSexMarriageDiceSize)
                                .addComponent(spnRandomSameSexMarriageDiceSize, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomNewDependentMarriage)
                                .addComponent(spnRandomNewDependentMarriage, Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageOppositeSexDiceSize)
                                .addComponent(spnRandomMarriageDiceSize))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomSameSexMarriageDiceSize)
                                .addComponent(spnRandomSameSexMarriageDiceSize))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomNewDependentMarriage)
                                .addComponent(spnRandomNewDependentMarriage))
        );
    }

    private JPanel createDivorcePanel() {
        // Create Panel Components
        chkUseManualDivorce = new JCheckBox(resources.getString("chkUseManualDivorce.text"));
        chkUseManualDivorce.setToolTipText(resources.getString("chkUseManualDivorce.toolTipText"));
        chkUseManualDivorce.setName("chkUseManualDivorce");

        chkUseClanPersonnelDivorce = new JCheckBox(resources.getString("chkUseClanPersonnelDivorce.text"));
        chkUseClanPersonnelDivorce
                .setToolTipText(resources.getString("chkUseClanPersonnelDivorce.toolTipText"));
        chkUseClanPersonnelDivorce.setName("chkUseClanPersonnelDivorce");
        chkUseClanPersonnelDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClanPersonnelDivorce
                    .setEnabled(!method.isNone() && chkUseClanPersonnelDivorce.isSelected());
        });

        chkUsePrisonerDivorce = new JCheckBox(resources.getString("chkUsePrisonerDivorce.text"));
        chkUsePrisonerDivorce.setToolTipText(resources.getString("chkUsePrisonerDivorce.toolTipText"));
        chkUsePrisonerDivorce.setName("chkUsePrisonerDivorce");
        chkUsePrisonerDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerDivorce.setEnabled(!method.isNone() && chkUsePrisonerDivorce.isSelected());
        });

        final JPanel divorceSurnameWeightsPanel = createDivorceSurnameWeightsPanel();

        final JPanel randomDivorcePanel = createRandomDivorcePanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("divorcePanel.title")));
        panel.setName("divorcePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseManualDivorce)
                        .addComponent(chkUseClanPersonnelDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(divorceSurnameWeightsPanel)
                        .addComponent(randomDivorcePanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseManualDivorce)
                        .addComponent(chkUseClanPersonnelDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(divorceSurnameWeightsPanel)
                        .addComponent(randomDivorcePanel));

        return panel;
    }

    private JPanel createDivorceSurnameWeightsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 4));
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("divorceSurnameWeightsPanel.title")));
        panel.setToolTipText(resources.getString("divorceSurnameWeightsPanel.toolTipText"));
        panel.setName("divorceSurnameWeightsPanel");

        spnDivorceSurnameWeights = new HashMap<>();
        for (final SplittingSurnameStyle style : SplittingSurnameStyle.values()) {
            if (style.isWeighted()) {
                continue;
            }
            final JLabel label = new JLabel(style.toString());
            label.setToolTipText(style.getToolTipText());
            label.setName("lbl" + style);
            panel.add(label);

            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
            spinner.setToolTipText(style.getToolTipText());
            spinner.setName("spn" + style);
            spnDivorceSurnameWeights.put(style, spinner);
            panel.add(spinner);

            label.setLabelFor(spinner);
        }

        return panel;
    }

    private JPanel createRandomDivorcePanel() {
        // Initialize Components Used in ActionListeners
        final JPanel percentageRandomDivorcePanel = new JDisableablePanel("percentageRandomDivorcePanel");

        // Create Panel Components
        final JLabel lblRandomDivorceMethod = new JLabel(resources.getString("lblRandomDivorceMethod.text"));
        lblRandomDivorceMethod.setToolTipText(resources.getString("lblRandomDivorceMethod.toolTipText"));
        lblRandomDivorceMethod.setName("lblRandomDivorceMethod");

        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        comboRandomDivorceMethod.setToolTipText(resources.getString("lblRandomDivorceMethod.toolTipText"));
        comboRandomDivorceMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDivorceMethod) {
                    list.setToolTipText(((RandomDivorceMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomDivorceMethod.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            final boolean percentageEnabled = method.isDiceRoll();
            chkUseRandomOppositeSexDivorce.setEnabled(enabled);
            chkUseRandomSameSexDivorce.setEnabled(enabled);
            chkUseRandomClanPersonnelDivorce.setEnabled(enabled && chkUseClanPersonnelDivorce.isSelected());
            chkUseRandomPrisonerDivorce.setEnabled(enabled && chkUsePrisonerDivorce.isSelected());
            percentageRandomDivorcePanel.setEnabled(percentageEnabled);
            lblRandomDivorceDiceSize.setEnabled(percentageEnabled);
            spnRandomDivorceDiceSize.setEnabled(percentageEnabled);
        });

        chkUseRandomOppositeSexDivorce = new JCheckBox(
                resources.getString("chkUseRandomOppositeSexDivorce.text"));
        chkUseRandomOppositeSexDivorce
                .setToolTipText(resources.getString("chkUseRandomOppositeSexDivorce.toolTipText"));
        chkUseRandomOppositeSexDivorce.setName("chkUseRandomOppositeSexDivorce");

        chkUseRandomSameSexDivorce = new JCheckBox(resources.getString("chkUseRandomSameSexDivorce.text"));
        chkUseRandomSameSexDivorce
                .setToolTipText(resources.getString("chkUseRandomSameSexDivorce.toolTipText"));
        chkUseRandomSameSexDivorce.setName("chkUseRandomSameSexDivorce");

        chkUseRandomClanPersonnelDivorce = new JCheckBox(
                resources.getString("chkUseRandomClanPersonnelDivorce.text"));
        chkUseRandomClanPersonnelDivorce
                .setToolTipText(resources.getString("chkUseRandomClanPersonnelDivorce.toolTipText"));
        chkUseRandomClanPersonnelDivorce.setName("chkUseRandomClanPersonnelDivorce");

        chkUseRandomPrisonerDivorce = new JCheckBox(resources.getString("chkUseRandomPrisonerDivorce.text"));
        chkUseRandomPrisonerDivorce
                .setToolTipText(resources.getString("chkUseRandomPrisonerDivorce.toolTipText"));
        chkUseRandomPrisonerDivorce.setName("chkUseRandomPrisonerDivorce");

        createPercentageRandomDivorcePanel(percentageRandomDivorcePanel);

        // Programmatically Assign Accessibility Labels
        lblRandomDivorceMethod.setLabelFor(comboRandomDivorceMethod);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomDivorcePanel.title")));
        panel.setName("randomDivorcePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomDivorceMethod)
                                .addComponent(comboRandomDivorceMethod,
                                        Alignment.LEADING))
                        .addComponent(chkUseRandomOppositeSexDivorce)
                        .addComponent(chkUseRandomSameSexDivorce)
                        .addComponent(chkUseRandomClanPersonnelDivorce)
                        .addComponent(chkUseRandomPrisonerDivorce)
                        .addComponent(percentageRandomDivorcePanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomDivorceMethod)
                                .addComponent(comboRandomDivorceMethod))
                        .addComponent(chkUseRandomOppositeSexDivorce)
                        .addComponent(chkUseRandomSameSexDivorce)
                        .addComponent(chkUseRandomClanPersonnelDivorce)
                        .addComponent(chkUseRandomPrisonerDivorce)
                        .addComponent(percentageRandomDivorcePanel));

        return panel;
    }

    private void createPercentageRandomDivorcePanel(final JPanel panel) {
        // Create Panel Components
        lblRandomDivorceDiceSize = new JLabel(resources.getString("lblRandomDivorceDiceSize.text"));
        lblRandomDivorceDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomDivorceDiceSize.toolTipText")));
        lblRandomDivorceDiceSize.setName("lblRandomDivorceDiceSize");

        spnRandomDivorceDiceSize = new JSpinner(new SpinnerNumberModel(900, 0, 100000, 1));
        spnRandomDivorceDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomDivorceDiceSize.toolTipText")));
        spnRandomDivorceDiceSize.setName("spnRandomDivorceDiceSize");

        // Programmatically Assign Accessibility Labels
        lblRandomDivorceDiceSize.setLabelFor(spnRandomDivorceDiceSize);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomDivorcePanel.title")));
        panel.setToolTipText(RandomDivorceMethod.DICE_ROLL.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomDivorceDiceSize)
                                .addComponent(spnRandomDivorceDiceSize, Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomDivorceDiceSize)
                                .addComponent(spnRandomDivorceDiceSize))
        );
    }

    private JPanel createProcreationPanel() {
        // Create Panel Components
        chkUseManualProcreation = new JCheckBox(resources.getString("chkUseManualProcreation.text"));
        chkUseManualProcreation.setToolTipText(resources.getString("chkUseManualProcreation.toolTipText"));
        chkUseManualProcreation.setName("chkUseManualProcreation");

        chkUseClanPersonnelProcreation = new JCheckBox(
                resources.getString("chkUseClanPersonnelProcreation.text"));
        chkUseClanPersonnelProcreation
                .setToolTipText(resources.getString("chkUseClanPersonnelProcreation.toolTipText"));
        chkUseClanPersonnelProcreation.setName("chkUseClanPersonnelProcreation");
        chkUseClanPersonnelProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClanPersonnelProcreation
                    .setEnabled(!method.isNone() && chkUseClanPersonnelProcreation.isSelected());
        });

        chkUsePrisonerProcreation = new JCheckBox(resources.getString("chkUsePrisonerProcreation.text"));
        chkUsePrisonerProcreation.setToolTipText(resources.getString("chkUsePrisonerProcreation.toolTipText"));
        chkUsePrisonerProcreation.setName("chkUsePrisonerProcreation");
        chkUsePrisonerProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerProcreation
                    .setEnabled(!method.isNone() && chkUsePrisonerProcreation.isSelected());
        });

        final JLabel lblMultiplePregnancyOccurrences = new JLabel(
                resources.getString("lblMultiplePregnancyOccurrences.text"));
        lblMultiplePregnancyOccurrences
                .setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        lblMultiplePregnancyOccurrences.setName("lblMultiplePregnancyOccurrences");

        spnMultiplePregnancyOccurrences = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        spnMultiplePregnancyOccurrences
                .setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        spnMultiplePregnancyOccurrences.setName("spnMultiplePregnancyOccurrences");

        final JLabel lblMultiplePregnancyOccurrencesEnd = new JLabel(
                resources.getString("lblMultiplePregnancyOccurrencesEnd.text"));
        lblMultiplePregnancyOccurrencesEnd
                .setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        lblMultiplePregnancyOccurrencesEnd.setName("lblMultiplePregnancyOccurrencesEnd");

        final JLabel lblBabySurnameStyle = new JLabel(resources.getString("lblBabySurnameStyle.text"));
        lblBabySurnameStyle.setToolTipText(resources.getString("lblBabySurnameStyle.toolTipText"));
        lblBabySurnameStyle.setName("lblBabySurnameStyle");

        comboBabySurnameStyle = new MMComboBox<>("comboBabySurnameStyle", BabySurnameStyle.values());
        comboBabySurnameStyle.setToolTipText(resources.getString("lblBabySurnameStyle.toolTipText"));
        comboBabySurnameStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BabySurnameStyle) {
                    list.setToolTipText(((BabySurnameStyle) value).getToolTipText());
                }
                return this;
            }
        });

        chkAssignNonPrisonerBabiesFounderTag = new JCheckBox(
                resources.getString("chkAssignNonPrisonerBabiesFounderTag.text"));
        chkAssignNonPrisonerBabiesFounderTag
                .setToolTipText(resources
                        .getString("chkAssignNonPrisonerBabiesFounderTag.toolTipText"));
        chkAssignNonPrisonerBabiesFounderTag.setName("chkAssignNonPrisonerBabiesFounderTag");

        chkAssignChildrenOfFoundersFounderTag = new JCheckBox(
                resources.getString("chkAssignChildrenOfFoundersFounderTag.text"));
        chkAssignChildrenOfFoundersFounderTag
                .setToolTipText(resources
                        .getString("chkAssignChildrenOfFoundersFounderTag.toolTipText"));
        chkAssignChildrenOfFoundersFounderTag.setName("chkAssignChildrenOfFoundersFounderTag");

        chkDetermineFatherAtBirth = new JCheckBox(resources.getString("chkDetermineFatherAtBirth.text"));
        chkDetermineFatherAtBirth.setToolTipText(resources.getString("chkDetermineFatherAtBirth.toolTipText"));
        chkDetermineFatherAtBirth.setName("chkDetermineFatherAtBirth");

        chkDisplayTrueDueDate = new JCheckBox(resources.getString("chkDisplayTrueDueDate.text"));
        chkDisplayTrueDueDate.setToolTipText(resources.getString("chkDisplayTrueDueDate.toolTipText"));
        chkDisplayTrueDueDate.setName("chkDisplayTrueDueDate");

        final JLabel lblNoInterestInChildrenDiceSize = new JLabel(resources.getString("lblNoInterestInChildrenDiceSize.text"));
        lblNoInterestInChildrenDiceSize.setToolTipText(wordWrap(resources.getString("lblNoInterestInChildrenDiceSize.toolTipText")));
        lblNoInterestInChildrenDiceSize.setName("lblNoInterestInChildrenDiceSize");

        spnNoInterestInChildrenDiceSize = new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
        spnNoInterestInChildrenDiceSize.setToolTipText(wordWrap(resources.getString("lblNoInterestInChildrenDiceSize.toolTipText")));
        spnNoInterestInChildrenDiceSize.setName("spnNoInterestInChildrenDiceSize");

        chkUseMaternityLeave = new JCheckBox(resources.getString("chkUseMaternityLeave.text"));
        chkUseMaternityLeave.setToolTipText(wordWrap(resources.getString("chkUseMaternityLeave.toolTipText")));
        chkUseMaternityLeave.setName("chkUseMaternityLeave");

        chkLogProcreation = new JCheckBox(resources.getString("chkLogProcreation.text"));
        chkLogProcreation.setToolTipText(resources.getString("chkLogProcreation.toolTipText"));
        chkLogProcreation.setName("chkLogProcreation");

        final JPanel randomProcreationPanel = createRandomProcreationPanel();

        // Programmatically Assign Accessibility Labels
        lblMultiplePregnancyOccurrences.setLabelFor(spnMultiplePregnancyOccurrences);
        lblBabySurnameStyle.setLabelFor(comboBabySurnameStyle);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("procreationPanel.title")));
        panel.setName("procreationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseManualProcreation)
                        .addComponent(chkUseClanPersonnelProcreation)
                        .addComponent(chkUsePrisonerProcreation)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMultiplePregnancyOccurrences)
                                .addComponent(spnMultiplePregnancyOccurrences)
                                .addComponent(lblMultiplePregnancyOccurrencesEnd,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblBabySurnameStyle)
                                .addComponent(comboBabySurnameStyle, Alignment.LEADING))
                        .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                        .addComponent(chkAssignChildrenOfFoundersFounderTag)
                        .addComponent(chkDetermineFatherAtBirth)
                        .addComponent(chkDisplayTrueDueDate)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblNoInterestInChildrenDiceSize)
                                .addComponent(spnNoInterestInChildrenDiceSize, Alignment.LEADING))
                        .addComponent(chkUseMaternityLeave)
                        .addComponent(chkLogProcreation)
                        .addComponent(randomProcreationPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseManualProcreation)
                        .addComponent(chkUseClanPersonnelProcreation)
                        .addComponent(chkUsePrisonerProcreation)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMultiplePregnancyOccurrences)
                                .addComponent(spnMultiplePregnancyOccurrences)
                                .addComponent(lblMultiplePregnancyOccurrencesEnd))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblBabySurnameStyle)
                                .addComponent(comboBabySurnameStyle))
                        .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                        .addComponent(chkAssignChildrenOfFoundersFounderTag)
                        .addComponent(chkDetermineFatherAtBirth)
                        .addComponent(chkDisplayTrueDueDate)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNoInterestInChildrenDiceSize)
                                .addComponent(spnNoInterestInChildrenDiceSize))
                        .addComponent(chkUseMaternityLeave)
                        .addComponent(chkLogProcreation)
                        .addComponent(randomProcreationPanel));

        return panel;
    }

    private JPanel createEducationPanel() {
        // General Settings
        lblCurriculumXpRate = new JLabel(resources.getString("lblCurriculumXpRate.text"));
        lblCurriculumXpRate.setToolTipText(resources.getString("lblCurriculumXpRate.toolTip"));
        lblCurriculumXpRate.setName("lblCurriculumXpRate");

        spnCurriculumXpRate = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        spnCurriculumXpRate.setToolTipText(resources.getString("lblCurriculumXpRate.toolTip"));
        spnCurriculumXpRate.setName("spnCurriculumXpRate");

        lblMaximumJumpCount = new JLabel(resources.getString("lblMaximumJumpCount.text"));
        lblMaximumJumpCount.setToolTipText(resources.getString("lblMaximumJumpCount.toolTip"));
        lblMaximumJumpCount.setName("lblMaximumJumpCount");

        spnMaximumJumpCount = new JSpinner(new SpinnerNumberModel(5, 1, 200, 1));
        spnMaximumJumpCount.setToolTipText(resources.getString("lblMaximumJumpCount.toolTip"));
        spnMaximumJumpCount.setName("spnMaximumJumpCount");

        chkUseReeducationCamps = new JCheckBox(resources.getString("chkUseReeducationCamps.text"));
        chkUseReeducationCamps.setToolTipText(resources.getString("chkUseReeducationCamps.toolTip"));
        chkUseReeducationCamps.setName("chkUseReeducationCamps");

        // Academy Set Enable
        final JPanel enableStandardSetsPanel = createStandardSetsPanel();

        // Show Ineligible Enable
        chkShowIneligibleAcademies = new JCheckBox(resources.getString("chkShowIneligibleAcademies.text"));
        chkShowIneligibleAcademies.setToolTipText(resources.getString("chkShowIneligibleAcademies.toolTip"));
        chkShowIneligibleAcademies.setName("chkShowIneligibleAcademies");

        chkEnableOverrideRequirements = new JCheckBox(
                resources.getString("chkEnableOverrideRequirements.text"));
        chkEnableOverrideRequirements
                .setToolTipText(resources.getString("chkEnableOverrideRequirements.toolTip"));
        chkEnableOverrideRequirements.setName("chkEnableOverrideRequirements");

        // Entrance Exams
        lblEntranceExamBaseTargetNumber = new JLabel(
                resources.getString("lblEntranceExamBaseTargetNumber.text"));
        lblEntranceExamBaseTargetNumber
                .setToolTipText(wordWrap(
                        resources.getString("lblEntranceExamBaseTargetNumber.toolTip")));
        lblEntranceExamBaseTargetNumber.setName("lblEntranceExamBaseTargetNumber");

        spnEntranceExamBaseTargetNumber = new JSpinner(new SpinnerNumberModel(14, 0, 20, 1));
        spnEntranceExamBaseTargetNumber
                .setToolTipText(wordWrap(
                        resources.getString("lblEntranceExamBaseTargetNumber.toolTip")));
        spnEntranceExamBaseTargetNumber.setName("spnEntranceExamBaseTargetNumber");

        // XP & Skill Bonuses
        final JPanel xpAndSkillBonusesPanel = createXpAndSkillBonusesPanel();

        // Dropout Chances
        final JPanel dropoutChancePanel = createDropoutChancePanel();

        // Random Fatal Accidents & Events
        final JPanel accidentsAndEventsPanel = createAccidentsAndEventsPanel();

        // Global Enable
        chkUseEducationModule = new JCheckBox(resources.getString("chkUseEducationModule.text"));
        chkUseEducationModule.setToolTipText(resources.getString("chkUseEducationModule.toolTip"));
        chkUseEducationModule.setName("chkUseEducationModule");
        chkUseEducationModule.addActionListener(evt -> {
            final boolean isEnabled = chkUseEducationModule.isSelected();

            lblCurriculumXpRate.setEnabled(isEnabled);
            spnCurriculumXpRate.setEnabled(isEnabled);
            lblMaximumJumpCount.setEnabled(isEnabled);
            spnMaximumJumpCount.setEnabled(isEnabled);
            chkUseReeducationCamps.setEnabled(isEnabled);

            enableStandardSetsPanel.setEnabled(isEnabled);
            chkEnableLocalAcademies.setEnabled(isEnabled);
            chkEnablePrestigiousAcademies.setEnabled(isEnabled);
            chkEnableUnitEducation.setEnabled(isEnabled);

            chkEnableOverrideRequirements.setEnabled(isEnabled);
            chkShowIneligibleAcademies.setEnabled(isEnabled);

            lblEntranceExamBaseTargetNumber.setEnabled(isEnabled);
            spnEntranceExamBaseTargetNumber.setEnabled(isEnabled);

            xpAndSkillBonusesPanel.setEnabled(isEnabled);
            chkEnableBonuses.setEnabled(isEnabled);
            lblFacultyXpMultiplier.setEnabled(isEnabled);
            spnFacultyXpMultiplier.setEnabled(isEnabled);

            dropoutChancePanel.setEnabled(isEnabled);
            lblAdultDropoutChance.setEnabled(isEnabled);
            spnAdultDropoutChance.setEnabled(isEnabled);
            lblChildrenDropoutChance.setEnabled(isEnabled);
            spnChildrenDropoutChance.setEnabled(isEnabled);

            accidentsAndEventsPanel.setEnabled(isEnabled);
            chkAllAges.setEnabled(isEnabled);
            lblMilitaryAcademyAccidents.setEnabled(isEnabled);
            spnMilitaryAcademyAccidents.setEnabled(isEnabled);
        });

        // this prevents a really annoying bug where disabled options don't stay
        // disabled when
        // reloading Campaign Options
        lblCurriculumXpRate.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        spnCurriculumXpRate.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        lblMaximumJumpCount.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        spnMaximumJumpCount.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        chkUseReeducationCamps.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        enableStandardSetsPanel.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        chkEnableOverrideRequirements.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        chkShowIneligibleAcademies.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        lblEntranceExamBaseTargetNumber.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        lblEntranceExamBaseTargetNumber.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        xpAndSkillBonusesPanel.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        dropoutChancePanel.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        accidentsAndEventsPanel.setEnabled(campaign.getCampaignOptions().isUseEducationModule());

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("educationPanel.title")));
        panel.setName("educationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseEducationModule)
                        .addGap(10)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblCurriculumXpRate)
                                .addComponent(spnCurriculumXpRate))
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblMaximumJumpCount)
                                .addComponent(spnMaximumJumpCount))
                        .addComponent(chkUseReeducationCamps)
                        .addComponent(enableStandardSetsPanel)
                        .addComponent(chkEnableOverrideRequirements)
                        .addComponent(chkShowIneligibleAcademies)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblEntranceExamBaseTargetNumber)
                                .addComponent(spnEntranceExamBaseTargetNumber))
                        .addComponent(xpAndSkillBonusesPanel)
                        .addComponent(dropoutChancePanel)
                        .addComponent(accidentsAndEventsPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseEducationModule)
                        .addGap(10)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCurriculumXpRate)
                                .addComponent(spnCurriculumXpRate))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMaximumJumpCount)
                                .addComponent(spnMaximumJumpCount))
                        .addComponent(chkUseReeducationCamps)
                        .addComponent(enableStandardSetsPanel)
                        .addComponent(chkEnableOverrideRequirements)
                        .addComponent(chkShowIneligibleAcademies)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblEntranceExamBaseTargetNumber)
                                .addComponent(spnEntranceExamBaseTargetNumber))
                        .addComponent(xpAndSkillBonusesPanel)
                        .addComponent(dropoutChancePanel)
                        .addComponent(accidentsAndEventsPanel));
        return panel;
    }

    private JPanel createStandardSetsPanel() {
        chkEnableLocalAcademies = new JCheckBox(resources.getString("chkEnableLocalAcademies.text"));
        chkEnableLocalAcademies.setToolTipText(resources.getString("chkEnableLocalAcademies.toolTip"));
        chkEnableLocalAcademies.setName("chkEnableLocalAcademies");

        chkEnablePrestigiousAcademies = new JCheckBox(
                resources.getString("chkEnablePrestigiousAcademies.text"));
        chkEnablePrestigiousAcademies
                .setToolTipText(resources.getString("chkEnablePrestigiousAcademies.toolTip"));
        chkEnablePrestigiousAcademies.setName("chkEnablePrestigiousAcademies");

        chkEnableUnitEducation = new JCheckBox(resources.getString("chkEnableUnitEducation.text"));
        chkEnableUnitEducation.setToolTipText(resources.getString("chkEnableUnitEducation.toolTip"));
        chkEnableUnitEducation.setName("chkEnableUnitEducation");

        // these prevent a really annoying bug where disabled options don't stay
        // disabled when
        // reloading Campaign Options
        chkEnableLocalAcademies.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        chkEnablePrestigiousAcademies.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        chkEnableUnitEducation.setEnabled(campaign.getCampaignOptions().isUseEducationModule());

        // creating the layout
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("standardSets.title")));
        panel.setName("standardSetsPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkEnableLocalAcademies)
                        .addComponent(chkEnablePrestigiousAcademies)
                        .addComponent(chkEnableUnitEducation));

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkEnableLocalAcademies)
                        .addComponent(chkEnablePrestigiousAcademies)
                        .addComponent(chkEnableUnitEducation));

        return panel;
    }

    private JPanel createXpAndSkillBonusesPanel() {
        chkEnableBonuses = new JCheckBox(resources.getString("chkEnableBonuses.text"));
        chkEnableBonuses.setToolTipText(resources.getString("chkEnableBonuses.toolTip"));
        chkEnableBonuses.setName("chkEnableBonuses");

        lblFacultyXpMultiplier = new JLabel(resources.getString("lblFacultyXPMultiplier.text"));
        lblFacultyXpMultiplier.setToolTipText(resources.getString("lblFacultyXPMultiplier.toolTip"));
        lblFacultyXpMultiplier.setName("lblFacultyXpMultiplier");

        spnFacultyXpMultiplier = new JSpinner(new SpinnerNumberModel(1.00, 0.00, 10.00, 0.01));
        spnFacultyXpMultiplier.setToolTipText(resources.getString("lblFacultyXPMultiplier.toolTip"));
        spnFacultyXpMultiplier.setName("spnFacultyXpMultiplier");

        chkEnableBonuses.setEnabled(campaign.getCampaignOptions().isUseEducationModule());

        // creating the layout
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("xpAndSkillBonuses.title")));
        panel.setName("xpAndSkillBonusesPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkEnableBonuses)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblFacultyXpMultiplier)
                                .addComponent(spnFacultyXpMultiplier,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkEnableBonuses)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFacultyXpMultiplier)
                                .addComponent(spnFacultyXpMultiplier)));

        return panel;
    }

    private JPanel createDropoutChancePanel() {
        lblAdultDropoutChance = new JLabel(resources.getString("lblAdultDropoutChance.text"));
        lblAdultDropoutChance.setToolTipText(resources.getString("lblAdultDropoutChance.toolTip"));
        lblAdultDropoutChance.setName("lblAdultDropoutChance");
        spnAdultDropoutChance = new JSpinner(new SpinnerNumberModel(1000, 0, 100000, 1));
        spnAdultDropoutChance.setToolTipText(resources.getString("lblAdultDropoutChance.toolTip"));
        spnAdultDropoutChance.setName("spnAdultDropoutChance");

        lblChildrenDropoutChance = new JLabel(resources.getString("lblChildrenDropoutChance.text"));
        lblChildrenDropoutChance.setToolTipText(resources.getString("lblChildrenDropoutChance.toolTip"));
        lblChildrenDropoutChance.setName("lblChildrenDropoutChance");
        spnChildrenDropoutChance = new JSpinner(new SpinnerNumberModel(10000, 0, 100000, 1));
        spnChildrenDropoutChance.setToolTipText(resources.getString("lblChildrenDropoutChance.toolTip"));
        spnChildrenDropoutChance.setName("spnChildrenDropoutChance");

        // These prevent a really annoying bug where disabled options don't stay
        // disabled when
        // reloading Campaign Options
        lblAdultDropoutChance.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        spnAdultDropoutChance.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        lblChildrenDropoutChance.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        spnChildrenDropoutChance.setEnabled(campaign.getCampaignOptions().isUseEducationModule());

        // creating the layout
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("dropoutChance.title")));
        panel.setName("dropoutChancePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblAdultDropoutChance)
                                .addComponent(spnAdultDropoutChance, Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(lblChildrenDropoutChance)
                                        .addComponent(spnChildrenDropoutChance,
                                                Alignment.LEADING))));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblAdultDropoutChance)
                                .addComponent(spnAdultDropoutChance)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblChildrenDropoutChance)
                                        .addComponent(spnChildrenDropoutChance))));

        return panel;
    }

    private JPanel createAccidentsAndEventsPanel() {
        chkAllAges = new JCheckBox(resources.getString("chkAllAges.text"));
        chkAllAges.setToolTipText(resources.getString("chkAllAges.toolTip"));
        chkAllAges.setName("chkAllAges");

        lblMilitaryAcademyAccidents = new JLabel(resources.getString("lblMilitaryAcademyAccidents.text"));
        lblMilitaryAcademyAccidents.setToolTipText(resources.getString("lblMilitaryAcademyAccidents.toolTip"));
        lblMilitaryAcademyAccidents.setName("lblMilitaryAcademyAccidents");
        spnMilitaryAcademyAccidents = new JSpinner(new SpinnerNumberModel(10000, 0, 100000, 1));
        spnMilitaryAcademyAccidents.setToolTipText(resources.getString("lblMilitaryAcademyAccidents.toolTip"));
        spnMilitaryAcademyAccidents.setName("spnMilitaryAcademyAccidents");

        // These prevent a really annoying bug where disabled options don't stay
        // disabled when
        // reloading Campaign Options
        chkAllAges.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        lblMilitaryAcademyAccidents.setEnabled(campaign.getCampaignOptions().isUseEducationModule());
        spnMilitaryAcademyAccidents.setEnabled(campaign.getCampaignOptions().isUseEducationModule());

        // creating the layout
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("accidentsAndEvents.title")));
        panel.setName("accidentsAndEventsPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkAllAges)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMilitaryAcademyAccidents)
                                .addComponent(spnMilitaryAcademyAccidents,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkAllAges)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMilitaryAcademyAccidents)
                                .addComponent(spnMilitaryAcademyAccidents)));

        return panel;
    }

    private JPanel createRandomProcreationPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel percentageRandomProcreationPanel = new JDisableablePanel(
                "percentageRandomProcreationPanel");

        // Create Panel Components
        final JLabel lblRandomProcreationMethod = new JLabel(
                resources.getString("lblRandomProcreationMethod.text"));
        lblRandomProcreationMethod
                .setToolTipText(resources.getString("lblRandomProcreationMethod.toolTipText"));
        lblRandomProcreationMethod.setName("lblRandomProcreationMethod");

        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod",
                RandomProcreationMethod.values());
        comboRandomProcreationMethod
                .setToolTipText(resources.getString("lblRandomProcreationMethod.toolTipText"));
        comboRandomProcreationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomProcreationMethod) {
                    list.setToolTipText(((RandomProcreationMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomProcreationMethod.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            final boolean percentageEnabled = method.isDiceRoll();
            final boolean relationshiplessEnabled = enabled && chkUseRelationshiplessRandomProcreation.isSelected();
            chkUseRelationshiplessRandomProcreation.setEnabled(enabled);
            chkUseRandomClanPersonnelProcreation
                    .setEnabled(enabled && chkUseClanPersonnelProcreation.isSelected());
            chkUseRandomPrisonerProcreation.setEnabled(enabled && chkUsePrisonerProcreation.isSelected());
            percentageRandomProcreationPanel.setEnabled(percentageEnabled);
            lblRandomProcreationRelationshiplessDiceSize.setEnabled(relationshiplessEnabled && percentageEnabled);
            spnRandomProcreationRelationshiplessDiceSize.setEnabled(relationshiplessEnabled && percentageEnabled);
        });

        chkUseRelationshiplessRandomProcreation = new JCheckBox(
                resources.getString("chkUseRelationshiplessRandomProcreation.text"));
        chkUseRelationshiplessRandomProcreation
                .setToolTipText(resources
                        .getString("chkUseRelationshiplessRandomProcreation.toolTipText"));
        chkUseRelationshiplessRandomProcreation.setName("chkUseRelationshiplessRandomProcreation");
        chkUseRelationshiplessRandomProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean sameSexEnabled = chkUseRelationshiplessRandomProcreation.isEnabled()
                    && chkUseRelationshiplessRandomProcreation.isSelected();
            final boolean percentageEnabled = sameSexEnabled && method.isDiceRoll();
            lblRandomProcreationRelationshiplessDiceSize.setEnabled(percentageEnabled);
            spnRandomProcreationRelationshiplessDiceSize.setEnabled(percentageEnabled);
        });

        chkUseRandomClanPersonnelProcreation = new JCheckBox(
                resources.getString("chkUseRandomClanPersonnelProcreation.text"));
        chkUseRandomClanPersonnelProcreation
                .setToolTipText(resources
                        .getString("chkUseRandomClanPersonnelProcreation.toolTipText"));
        chkUseRandomClanPersonnelProcreation.setName("chkUseRandomClanPersonnelProcreation");

        chkUseRandomPrisonerProcreation = new JCheckBox(
                resources.getString("chkUseRandomPrisonerProcreation.text"));
        chkUseRandomPrisonerProcreation
                .setToolTipText(resources.getString("chkUseRandomPrisonerProcreation.toolTipText"));
        chkUseRandomPrisonerProcreation.setName("chkUseRandomPrisonerProcreation");

        createPercentageRandomProcreationPanel(percentageRandomProcreationPanel);

        // Programmatically Assign Accessibility Labels
        lblRandomProcreationMethod.setLabelFor(comboRandomProcreationMethod);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomProcreationPanel.title")));
        panel.setName("randomProcreationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomProcreationMethod)
                                .addComponent(comboRandomProcreationMethod,
                                        Alignment.LEADING))
                        .addComponent(chkUseRelationshiplessRandomProcreation)
                        .addComponent(chkUseRandomClanPersonnelProcreation)
                        .addComponent(chkUseRandomPrisonerProcreation)
                        .addComponent(percentageRandomProcreationPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomProcreationMethod)
                                .addComponent(comboRandomProcreationMethod))
                        .addComponent(chkUseRelationshiplessRandomProcreation)
                        .addComponent(chkUseRandomClanPersonnelProcreation)
                        .addComponent(chkUseRandomPrisonerProcreation)
                        .addComponent(percentageRandomProcreationPanel));

        return panel;
    }

    private void createPercentageRandomProcreationPanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblRandomProcreationRelationshipDiceSize = new JLabel(resources.getString("lblRandomProcreationRelationshipDiceSize.text"));
        lblRandomProcreationRelationshipDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomProcreationRelationshipDiceSize.toolTipText")));
        lblRandomProcreationRelationshipDiceSize.setName("lblRandomProcreationRelationshipDiceSize");

        spnRandomProcreationRelationshipDiceSize = new JSpinner(new SpinnerNumberModel(621, 0, 100000, 1));
        spnRandomProcreationRelationshipDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomProcreationRelationshipDiceSize.toolTipText")));
        spnRandomProcreationRelationshipDiceSize.setName("spnRandomProcreationRelationshipDiceSize");

        lblRandomProcreationRelationshiplessDiceSize = new JLabel(resources.getString("lblRandomProcreationRelationshiplessDiceSize.text"));
        lblRandomProcreationRelationshiplessDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomProcreationRelationshiplessDiceSize.toolTipText")));
        lblRandomProcreationRelationshiplessDiceSize.setName("lblRandomProcreationRelationshiplessDiceSize");

        spnRandomProcreationRelationshiplessDiceSize = new JSpinner(new SpinnerNumberModel(1861, 0, 100000, 1));
        spnRandomProcreationRelationshiplessDiceSize.setToolTipText(wordWrap(resources.getString("lblRandomProcreationRelationshipDiceSize.toolTipText")));
        spnRandomProcreationRelationshiplessDiceSize.setName("spnRandomProcreationRelationshiplessDiceSize");

        // Programmatically Assign Accessibility Labels
        lblRandomProcreationRelationshipDiceSize.setLabelFor(spnRandomProcreationRelationshipDiceSize);
        lblRandomProcreationRelationshiplessDiceSize.setLabelFor(spnRandomProcreationRelationshipDiceSize);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomProcreationPanel.title")));
        panel.setToolTipText(RandomProcreationMethod.DICE_ROLL.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomProcreationRelationshipDiceSize)
                                .addComponent(spnRandomProcreationRelationshipDiceSize, Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomProcreationRelationshiplessDiceSize)
                                .addComponent(spnRandomProcreationRelationshiplessDiceSize, Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomProcreationRelationshipDiceSize)
                                .addComponent(spnRandomProcreationRelationshipDiceSize))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomProcreationRelationshiplessDiceSize)
                                .addComponent(spnRandomProcreationRelationshiplessDiceSize))
        );
    }

    private JPanel createDeathPanel() {
        // Create Panel Components
        chkKeepMarriedNameUponSpouseDeath = new JCheckBox(
                resources.getString("chkKeepMarriedNameUponSpouseDeath.text"));
        chkKeepMarriedNameUponSpouseDeath
                .setToolTipText(resources.getString("chkKeepMarriedNameUponSpouseDeath.toolTipText"));
        chkKeepMarriedNameUponSpouseDeath.setName("chkKeepMarriedNameUponSpouseDeath");

        final JPanel randomDeathPanel = createRandomDeathPanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("deathPanel.title")));
        panel.setName("deathPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkKeepMarriedNameUponSpouseDeath)
                        .addComponent(randomDeathPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkKeepMarriedNameUponSpouseDeath)
                        .addComponent(randomDeathPanel));

        return panel;
    }

    private JPanel createRandomDeathPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel enabledRandomDeathAgeGroupsPanel = new JDisableablePanel(
                "enabledRandomDeathAgeGroupsPanel");

        final JPanel percentageRandomDeathPanel = new JDisableablePanel("percentageRandomDeathPanel");

        final JPanel exponentialRandomDeathPanel = new JDisableablePanel("exponentialRandomDeathPanel");

        final JPanel ageRangeRandomDeathPanel = new JDisableablePanel("ageRangeRandomDeathPanel");

        // Create Panel Components
        final JLabel lblRandomDeathMethod = new JLabel(resources.getString("lblRandomDeathMethod.text"));
        lblRandomDeathMethod.setToolTipText(resources.getString("lblRandomDeathMethod.toolTipText"));
        lblRandomDeathMethod.setName("lblRandomDeathMethod");

        comboRandomDeathMethod = new MMComboBox<>("comboRandomDeathMethod", RandomDeathMethod.values());
        comboRandomDeathMethod.setToolTipText(resources.getString("lblRandomDeathMethod.toolTipText"));
        comboRandomDeathMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDeathMethod) {
                    list.setToolTipText(((RandomDeathMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomDeathMethod.addActionListener(evt -> {
            final RandomDeathMethod method = comboRandomDeathMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            enabledRandomDeathAgeGroupsPanel.setEnabled(enabled);
            chkUseRandomClanPersonnelDeath.setEnabled(enabled);
            chkUseRandomPrisonerDeath.setEnabled(enabled);
            chkUseRandomDeathSuicideCause.setEnabled(enabled);
            percentageRandomDeathPanel.setEnabled(method.isPercentage());
            exponentialRandomDeathPanel.setEnabled(method.isExponential());
            ageRangeRandomDeathPanel.setEnabled(method.isAgeRange());
        });

        createEnabledRandomDeathAgeGroupsPanel(enabledRandomDeathAgeGroupsPanel);

        chkUseRandomClanPersonnelDeath = new JCheckBox(
                resources.getString("chkUseRandomClanPersonnelDeath.text"));
        chkUseRandomClanPersonnelDeath
                .setToolTipText(resources.getString("chkUseRandomClanPersonnelDeath.toolTipText"));
        chkUseRandomClanPersonnelDeath.setName("chkUseRandomClanPersonnelDeath");

        chkUseRandomPrisonerDeath = new JCheckBox(resources.getString("chkUseRandomPrisonerDeath.text"));
        chkUseRandomPrisonerDeath.setToolTipText(resources.getString("chkUseRandomPrisonerDeath.toolTipText"));
        chkUseRandomPrisonerDeath.setName("chkUseRandomPrisonerDeath");

        chkUseRandomDeathSuicideCause = new JCheckBox(
                resources.getString("chkUseRandomDeathSuicideCause.text"));
        chkUseRandomDeathSuicideCause
                .setToolTipText(resources.getString("chkUseRandomDeathSuicideCause.toolTipText"));
        chkUseRandomDeathSuicideCause.setName("chkUseRandomDeathSuicideCause");

        createPercentageRandomDeathPanel(percentageRandomDeathPanel);

        createExponentialRandomDeathPanel(exponentialRandomDeathPanel);

        createAgeRangeRandomDeathPanel(ageRangeRandomDeathPanel);

        // Programmatically Assign Accessibility Labels
        lblRandomDeathMethod.setLabelFor(comboRandomDeathMethod);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomDeathPanel.title")));
        panel.setName("randomDeathPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomDeathMethod)
                                .addComponent(comboRandomDeathMethod,
                                        Alignment.LEADING))
                        .addComponent(enabledRandomDeathAgeGroupsPanel)
                        .addComponent(chkUseRandomClanPersonnelDeath)
                        .addComponent(chkUseRandomPrisonerDeath)
                        .addComponent(chkUseRandomDeathSuicideCause)
                        .addComponent(percentageRandomDeathPanel)
                        .addComponent(exponentialRandomDeathPanel)
                        .addComponent(ageRangeRandomDeathPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomDeathMethod)
                                .addComponent(comboRandomDeathMethod))
                        .addComponent(enabledRandomDeathAgeGroupsPanel)
                        .addComponent(chkUseRandomClanPersonnelDeath)
                        .addComponent(chkUseRandomPrisonerDeath)
                        .addComponent(chkUseRandomDeathSuicideCause)
                        .addComponent(percentageRandomDeathPanel)
                        .addComponent(exponentialRandomDeathPanel)
                        .addComponent(ageRangeRandomDeathPanel));

        return panel;
    }

    private void createEnabledRandomDeathAgeGroupsPanel(final JPanel panel) {
        // Initialize Variables Required
        final AgeGroup[] ageGroups = AgeGroup.values();

        chkEnabledRandomDeathAgeGroups = new HashMap<>();

        // Create the Panel
        panel.setBorder(
                BorderFactory.createTitledBorder(
                        resources.getString("enabledRandomDeathAgeGroupsPanel.title")));
        panel.setToolTipText(resources.getString("enabledRandomDeathAgeGroupsPanel.toolTipText"));
        panel.setLayout(new GridLayout(1, ageGroups.length));

        // Create the primary panel
        for (final AgeGroup ageGroup : ageGroups) {
            // Create Panel Components
            final JCheckBox checkBox = new JCheckBox(ageGroup.toString());
            checkBox.setToolTipText(ageGroup.getToolTipText());
            checkBox.setName("chk" + ageGroup);
            panel.add(checkBox);
            chkEnabledRandomDeathAgeGroups.put(ageGroup, checkBox);
        }
    }

    private void createPercentageRandomDeathPanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblPercentageRandomDeathChance = new JLabel(
                resources.getString("lblPercentageRandomDeathChance.text"));
        lblPercentageRandomDeathChance
                .setToolTipText(resources.getString("lblPercentageRandomDeathChance.toolTipText"));
        lblPercentageRandomDeathChance.setName("lblPercentageRandomDeathChance");

        spnPercentageRandomDeathChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.000001));
        spnPercentageRandomDeathChance
                .setToolTipText(resources.getString("lblPercentageRandomDeathChance.toolTipText"));
        spnPercentageRandomDeathChance.setName("spnPercentageRandomDeathChance");
        spnPercentageRandomDeathChance.setEditor(new NumberEditor(spnPercentageRandomDeathChance, "0.000000"));

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomDeathChance.setLabelFor(spnPercentageRandomDeathChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomDeathPanel.title")));
        panel.setToolTipText(RandomProcreationMethod.DICE_ROLL.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPercentageRandomDeathChance)
                                .addComponent(spnPercentageRandomDeathChance,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomDeathChance)
                                .addComponent(spnPercentageRandomDeathChance)));
    }

    private void createExponentialRandomDeathPanel(final JPanel panel) {
        // Create Panel Components
        final JPanel exponentialRandomDeathMalePanel = createExponentialRandomDeathMalePanel();

        final JPanel exponentialRandomDeathFemalePanel = createExponentialRandomDeathFemalePanel();

        // Layout the Panel
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("exponentialRandomDeathPanel.title")));
        panel.setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(exponentialRandomDeathMalePanel)
                                .addComponent(exponentialRandomDeathFemalePanel,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(exponentialRandomDeathMalePanel)
                                .addComponent(exponentialRandomDeathFemalePanel)));
    }

    private JPanel createExponentialRandomDeathMalePanel() {
        // Create Panel Components
        spnExponentialRandomDeathMaleValues = new JSpinner[3];

        spnExponentialRandomDeathMaleValues[0] = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.0001));
        spnExponentialRandomDeathMaleValues[0].setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        spnExponentialRandomDeathMaleValues[0].setName("spnExponentialRandomDeathMaleC");
        ((NumberEditor) spnExponentialRandomDeathMaleValues[0].getEditor()).getFormat()
                .setMaximumFractionDigits(4);

        final JLabel lblPowerOfTen = new JLabel(resources.getString("PowerOfTen.text"));
        lblPowerOfTen.setName("lblPowerOfTen");

        spnExponentialRandomDeathMaleValues[1] = new JSpinner(new SpinnerNumberModel(0.0, -100.0, 100.0, 1.0));
        spnExponentialRandomDeathMaleValues[1].setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        spnExponentialRandomDeathMaleValues[1].setName("spnExponentialRandomDeathMaleN");

        final JLabel lblExponential = new JLabel(resources.getString("Exponential.text"));
        lblExponential.setName("lblExponential");

        spnExponentialRandomDeathMaleValues[2] = new JSpinner(
                new SpinnerNumberModel(0.0, -100.0, 100.0, 0.0001));
        spnExponentialRandomDeathMaleValues[2].setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        spnExponentialRandomDeathMaleValues[2].setName("spnExponentialRandomDeathMaleK");
        ((NumberEditor) spnExponentialRandomDeathMaleValues[2].getEditor()).getFormat()
                .setMaximumFractionDigits(4);

        final JLabel lblExponentialRandomDeathAge = new JLabel(
                resources.getString("lblExponentialRandomDeathAge.text"));
        lblExponential.setName("lblExponentialRandomDeathAge");

        // Layout the Panel
        final JPanel panel = new JDisableablePanel("exponentialRandomDeathMalePanel");
        panel.setBorder(BorderFactory.createTitledBorder(Gender.MALE.toString()));
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(spnExponentialRandomDeathMaleValues[0])
                                .addComponent(lblPowerOfTen)
                                .addComponent(spnExponentialRandomDeathMaleValues[1])
                                .addComponent(lblExponential)
                                .addComponent(spnExponentialRandomDeathMaleValues[2])
                                .addComponent(lblExponentialRandomDeathAge,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(spnExponentialRandomDeathMaleValues[0])
                                .addComponent(lblPowerOfTen)
                                .addComponent(spnExponentialRandomDeathMaleValues[1])
                                .addComponent(lblExponential)
                                .addComponent(spnExponentialRandomDeathMaleValues[2])
                                .addComponent(lblExponentialRandomDeathAge)));

        return panel;
    }

    private JPanel createExponentialRandomDeathFemalePanel() {
        // Create Panel Components
        spnExponentialRandomDeathFemaleValues = new JSpinner[3];

        spnExponentialRandomDeathFemaleValues[0] = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.0001));
        spnExponentialRandomDeathFemaleValues[0].setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        spnExponentialRandomDeathFemaleValues[0].setName("spnExponentialRandomDeathFemaleC");
        ((NumberEditor) spnExponentialRandomDeathFemaleValues[0].getEditor()).getFormat()
                .setMaximumFractionDigits(4);

        final JLabel lblPowerOfTen = new JLabel(resources.getString("PowerOfTen.text"));
        lblPowerOfTen.setName("lblPowerOfTen");

        spnExponentialRandomDeathFemaleValues[1] = new JSpinner(
                new SpinnerNumberModel(0.0, -100.0, 100.0, 1.0));
        spnExponentialRandomDeathFemaleValues[1].setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        spnExponentialRandomDeathFemaleValues[1].setName("spnExponentialRandomDeathFemaleN");

        final JLabel lblExponential = new JLabel(resources.getString("Exponential.text"));
        lblExponential.setName("lblExponential");

        spnExponentialRandomDeathFemaleValues[2] = new JSpinner(
                new SpinnerNumberModel(0.0, -100.0, 100.0, 0.0001));
        spnExponentialRandomDeathFemaleValues[2].setToolTipText(RandomDeathMethod.EXPONENTIAL.getToolTipText());
        spnExponentialRandomDeathFemaleValues[2].setName("spnExponentialRandomDeathFemaleK");
        ((NumberEditor) spnExponentialRandomDeathFemaleValues[2].getEditor()).getFormat()
                .setMaximumFractionDigits(4);

        final JLabel lblExponentialRandomDeathAge = new JLabel(
                resources.getString("lblExponentialRandomDeathAge.text"));
        lblExponential.setName("lblExponentialRandomDeathAge");

        // Layout the Panel
        final JPanel panel = new JDisableablePanel("exponentialRandomDeathFemalePanel");
        panel.setBorder(BorderFactory.createTitledBorder(Gender.FEMALE.toString()));
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(spnExponentialRandomDeathFemaleValues[0])
                                .addComponent(lblPowerOfTen)
                                .addComponent(spnExponentialRandomDeathFemaleValues[1])
                                .addComponent(lblExponential)
                                .addComponent(spnExponentialRandomDeathFemaleValues[2])
                                .addComponent(lblExponentialRandomDeathAge,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(spnExponentialRandomDeathFemaleValues[0])
                                .addComponent(lblPowerOfTen)
                                .addComponent(spnExponentialRandomDeathFemaleValues[1])
                                .addComponent(lblExponential)
                                .addComponent(spnExponentialRandomDeathFemaleValues[2])
                                .addComponent(lblExponentialRandomDeathAge)));

        return panel;
    }

    private void createAgeRangeRandomDeathPanel(final JPanel panel) {
        // Initialize Variables Required
        final TenYearAgeRange[] ageRanges = TenYearAgeRange.values();

        spnAgeRangeRandomDeathMaleValues = new HashMap<>();

        spnAgeRangeRandomDeathFemaleValues = new HashMap<>();

        // Create the Panel
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("ageRangeRandomDeathPanel.title")));
        panel.setToolTipText(RandomDeathMethod.AGE_RANGE.getToolTipText());
        panel.setLayout(new GridLayout(ageRanges.length + 1, 3));

        // Create Title Row
        final JLabel lblAgeRange = new JLabel(resources.getString("lblAgeRange.text"));
        lblAgeRange.setName("lblAgeRange");
        panel.add(lblAgeRange);

        final JLabel lblMale = new JLabel(Gender.MALE.toString());
        lblMale.setName("lblMale");
        panel.add(lblMale);

        final JLabel lblFemale = new JLabel(Gender.FEMALE.toString());
        lblFemale.setName("lblFemale");
        panel.add(lblFemale);

        // Create the primary panel
        for (final TenYearAgeRange ageRange : ageRanges) {
            // Create Panel Components
            final JLabel label = new JLabel(ageRange.toString());
            label.setName("lbl" + ageRange);
            panel.add(label);

            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000.0, 10.0));
            spinner.setToolTipText(RandomDeathMethod.AGE_RANGE.getToolTipText());
            spinner.setName("spnAgeRangeRandomDeathMale" + ageRange);
            ((NumberEditor) spinner.getEditor()).getFormat().applyPattern("###,###.0");
            ((NumberEditor) spinner.getEditor()).getFormat().setMaximumFractionDigits(1);
            panel.add(spinner);
            spnAgeRangeRandomDeathMaleValues.put(ageRange, spinner);

            spinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000.0, 10.0));
            spinner.setToolTipText(RandomDeathMethod.AGE_RANGE.getToolTipText());
            spinner.setName("spnAgeRangeRandomDeathFemale" + ageRange);
            ((NumberEditor) spinner.getEditor()).getFormat().applyPattern("###,###.0");
            ((NumberEditor) spinner.getEditor()).getFormat().setMaximumFractionDigits(1);
            panel.add(spinner);
            spnAgeRangeRandomDeathFemaleValues.put(ageRange, spinner);
        }
    }
    // endregion Personnel Tab

    // region Finances Tab
    private JPanel createPriceModifiersPanel(boolean reverseQualities) {
        // Create Panel Components
        final JLabel lblCommonPartPriceMultiplier = new JLabel(
                resources.getString("lblCommonPartPriceMultiplier.text"));
        lblCommonPartPriceMultiplier
                .setToolTipText(resources.getString("lblCommonPartPriceMultiplier.toolTipText"));
        lblCommonPartPriceMultiplier.setName("lblCommonPartPriceMultiplier");

        spnCommonPartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnCommonPartPriceMultiplier
                .setToolTipText(resources.getString("lblCommonPartPriceMultiplier.toolTipText"));
        spnCommonPartPriceMultiplier.setName("spnCommonPartPriceMultiplier");

        final JLabel lblInnerSphereUnitPriceMultiplier = new JLabel(
                resources.getString("lblInnerSphereUnitPriceMultiplier.text"));
        lblInnerSphereUnitPriceMultiplier
                .setToolTipText(resources.getString("lblInnerSphereUnitPriceMultiplier.toolTipText"));
        lblInnerSphereUnitPriceMultiplier.setName("lblInnerSphereUnitPriceMultiplier");

        spnInnerSphereUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnInnerSphereUnitPriceMultiplier
                .setToolTipText(resources.getString("lblInnerSphereUnitPriceMultiplier.toolTipText"));
        spnInnerSphereUnitPriceMultiplier.setName("spnInnerSphereUnitPriceMultiplier");

        final JLabel lblInnerSpherePartPriceMultiplier = new JLabel(
                resources.getString("lblInnerSpherePartPriceMultiplier.text"));
        lblInnerSpherePartPriceMultiplier
                .setToolTipText(resources.getString("lblInnerSpherePartPriceMultiplier.toolTipText"));
        lblInnerSpherePartPriceMultiplier.setName("lblInnerSpherePartPriceMultiplier");

        spnInnerSpherePartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnInnerSpherePartPriceMultiplier
                .setToolTipText(resources.getString("lblInnerSpherePartPriceMultiplier.toolTipText"));
        spnInnerSpherePartPriceMultiplier.setName("spnInnerSpherePartPriceMultiplier");

        final JLabel lblClanUnitPriceMultiplier = new JLabel(
                resources.getString("lblClanUnitPriceMultiplier.text"));
        lblClanUnitPriceMultiplier
                .setToolTipText(resources.getString("lblClanUnitPriceMultiplier.toolTipText"));
        lblClanUnitPriceMultiplier.setName("lblClanUnitPriceMultiplier");

        spnClanUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnClanUnitPriceMultiplier
                .setToolTipText(resources.getString("lblClanUnitPriceMultiplier.toolTipText"));
        spnClanUnitPriceMultiplier.setName("spnClanUnitPriceMultiplier");

        final JLabel lblClanPartPriceMultiplier = new JLabel(
                resources.getString("lblClanPartPriceMultiplier.text"));
        lblClanPartPriceMultiplier
                .setToolTipText(resources.getString("lblClanPartPriceMultiplier.toolTipText"));
        lblClanPartPriceMultiplier.setName("lblClanPartPriceMultiplier");

        spnClanPartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnClanPartPriceMultiplier
                .setToolTipText(resources.getString("lblClanPartPriceMultiplier.toolTipText"));
        spnClanPartPriceMultiplier.setName("spnClanPartPriceMultiplier");

        final JLabel lblMixedTechUnitPriceMultiplier = new JLabel(
                resources.getString("lblMixedTechUnitPriceMultiplier.text"));
        lblMixedTechUnitPriceMultiplier
                .setToolTipText(resources.getString("lblMixedTechUnitPriceMultiplier.toolTipText"));
        lblMixedTechUnitPriceMultiplier.setName("lblMixedTechUnitPriceMultiplier");

        spnMixedTechUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnMixedTechUnitPriceMultiplier
                .setToolTipText(resources.getString("lblMixedTechUnitPriceMultiplier.toolTipText"));
        spnMixedTechUnitPriceMultiplier.setName("spnMixedTechUnitPriceMultiplier");

        final JPanel usedPartsValueMultipliersPanel = createUsedPartsValueMultipliersPanel(reverseQualities);

        final JLabel lblDamagedPartsValueMultiplier = new JLabel(
                resources.getString("lblDamagedPartsValueMultiplier.text"));
        lblDamagedPartsValueMultiplier
                .setToolTipText(resources.getString("lblDamagedPartsValueMultiplier.toolTipText"));
        lblDamagedPartsValueMultiplier.setName("lblDamagedPartsValueMultiplier");

        spnDamagedPartsValueMultiplier = new JSpinner(new SpinnerNumberModel(0.33, 0.00, 1.00, 0.05));
        spnDamagedPartsValueMultiplier
                .setToolTipText(resources.getString("lblDamagedPartsValueMultiplier.toolTipText"));
        spnDamagedPartsValueMultiplier.setName("spnDamagedPartsValueMultiplier");
        spnDamagedPartsValueMultiplier.setEditor(new NumberEditor(spnDamagedPartsValueMultiplier, "0.00"));

        final JLabel lblUnrepairablePartsValueMultiplier = new JLabel(
                resources.getString("lblUnrepairablePartsValueMultiplier.text"));
        lblUnrepairablePartsValueMultiplier
                .setToolTipText(resources.getString("lblUnrepairablePartsValueMultiplier.toolTipText"));
        lblUnrepairablePartsValueMultiplier.setName("lblUnrepairablePartsValueMultiplier");

        spnUnrepairablePartsValueMultiplier = new JSpinner(new SpinnerNumberModel(0.10, 0.00, 1.00, 0.05));
        spnUnrepairablePartsValueMultiplier
                .setToolTipText(resources.getString("lblUnrepairablePartsValueMultiplier.toolTipText"));
        spnUnrepairablePartsValueMultiplier.setName("spnUnrepairablePartsValueMultiplier");
        spnUnrepairablePartsValueMultiplier
                .setEditor(new NumberEditor(spnUnrepairablePartsValueMultiplier, "0.00"));

        final JLabel lblCancelledOrderRefundMultiplier = new JLabel(
                resources.getString("lblCancelledOrderRefundMultiplier.text"));
        lblCancelledOrderRefundMultiplier
                .setToolTipText(resources.getString("lblCancelledOrderRefundMultiplier.toolTipText"));
        lblCancelledOrderRefundMultiplier.setName("lblCancelledOrderRefundMultiplier");

        spnCancelledOrderRefundMultiplier = new JSpinner(new SpinnerNumberModel(0.50, 0.00, 1.00, 0.05));
        spnCancelledOrderRefundMultiplier
                .setToolTipText(resources.getString("lblCancelledOrderRefundMultiplier.toolTipText"));
        spnCancelledOrderRefundMultiplier.setName("spnCancelledOrderRefundMultiplier");
        spnCancelledOrderRefundMultiplier
                .setEditor(new NumberEditor(spnCancelledOrderRefundMultiplier, "0.00"));

        // Programmatically Assign Accessibility Labels
        lblCommonPartPriceMultiplier.setLabelFor(spnCommonPartPriceMultiplier);
        lblInnerSphereUnitPriceMultiplier.setLabelFor(spnInnerSphereUnitPriceMultiplier);
        lblInnerSpherePartPriceMultiplier.setLabelFor(spnInnerSpherePartPriceMultiplier);
        lblClanUnitPriceMultiplier.setLabelFor(spnClanUnitPriceMultiplier);
        lblClanPartPriceMultiplier.setLabelFor(spnClanPartPriceMultiplier);
        lblMixedTechUnitPriceMultiplier.setLabelFor(spnMixedTechUnitPriceMultiplier);
        lblDamagedPartsValueMultiplier.setLabelFor(spnDamagedPartsValueMultiplier);
        lblUnrepairablePartsValueMultiplier.setLabelFor(spnUnrepairablePartsValueMultiplier);
        lblCancelledOrderRefundMultiplier.setLabelFor(spnCancelledOrderRefundMultiplier);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("priceMultipliersPanel.title")));
        panel.setName("priceMultipliersPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblCommonPartPriceMultiplier)
                                .addComponent(spnCommonPartPriceMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblInnerSphereUnitPriceMultiplier)
                                .addComponent(spnInnerSphereUnitPriceMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblInnerSpherePartPriceMultiplier)
                                .addComponent(spnInnerSpherePartPriceMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblClanUnitPriceMultiplier)
                                .addComponent(spnClanUnitPriceMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblClanPartPriceMultiplier)
                                .addComponent(spnClanPartPriceMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMixedTechUnitPriceMultiplier)
                                .addComponent(spnMixedTechUnitPriceMultiplier,
                                        Alignment.LEADING))
                        .addComponent(usedPartsValueMultipliersPanel)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblDamagedPartsValueMultiplier)
                                .addComponent(spnDamagedPartsValueMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblUnrepairablePartsValueMultiplier)
                                .addComponent(spnUnrepairablePartsValueMultiplier,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblCancelledOrderRefundMultiplier)
                                .addComponent(spnCancelledOrderRefundMultiplier,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCommonPartPriceMultiplier)
                                .addComponent(spnCommonPartPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblInnerSphereUnitPriceMultiplier)
                                .addComponent(spnInnerSphereUnitPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblInnerSpherePartPriceMultiplier)
                                .addComponent(spnInnerSpherePartPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblClanUnitPriceMultiplier)
                                .addComponent(spnClanUnitPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblClanPartPriceMultiplier)
                                .addComponent(spnClanPartPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMixedTechUnitPriceMultiplier)
                                .addComponent(spnMixedTechUnitPriceMultiplier))
                        .addComponent(usedPartsValueMultipliersPanel)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblDamagedPartsValueMultiplier)
                                .addComponent(spnDamagedPartsValueMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnrepairablePartsValueMultiplier)
                                .addComponent(spnUnrepairablePartsValueMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCancelledOrderRefundMultiplier)
                                .addComponent(spnCancelledOrderRefundMultiplier)));

        return panel;
    }

    private JPanel createUsedPartsValueMultipliersPanel(boolean reverseQualities) {
        final JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.setBorder(BorderFactory
                .createTitledBorder(resources.getString("usedPartsValueMultipliersPanel.title")));
        panel.setName("usedPartsValueMultipliersPanel");

        spnUsedPartPriceMultipliers = new JSpinner[PartQuality.QUALITY_F.toNumeric() + 1];

        for (PartQuality quality : PartQuality.allQualities()) {
            final String qualityLevel = quality.toName(reverseQualities);

            final JLabel label = new JLabel(qualityLevel);
            label.setToolTipText(resources.getString("lblUsedPartPriceMultiplier.toolTipText"));
            label.setName("lbl" + qualityLevel);
            panel.add(label);

            spnUsedPartPriceMultipliers[quality.toNumeric()] = new JSpinner(
                        new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[quality.toNumeric()]
                    .setToolTipText(resources.getString("lblUsedPartPriceMultiplier.toolTipText"));
            spnUsedPartPriceMultipliers[quality.toNumeric()].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[quality.toNumeric()]
                    .setEditor(new NumberEditor(spnUsedPartPriceMultipliers[quality.toNumeric()], "0.00"));
            panel.add(spnUsedPartPriceMultipliers[quality.toNumeric()]);

            label.setLabelFor(spnUsedPartPriceMultipliers[quality.toNumeric()]);
        }

        return panel;
    }

    private JPanel createTaxesPanel() {
        taxesSubPanel = createTaxesSubPanel();

        chkUseTaxes = new JCheckBox(resources.getString("chkUseTaxes.text"));
        chkUseTaxes.setToolTipText(resources.getString("chkUseTaxes.toolTipText"));
        chkUseTaxes.setName("chkUseTaxes");
        chkUseTaxes.addActionListener(evt -> {
            final boolean isEnabled = chkUseTaxes.isSelected();

            for (Component component : taxesSubPanel.getComponents()) {
                component.setEnabled(isEnabled);
            }
        });

        final JPanel taxesPanel = new JPanel();
        taxesPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("taxesPanel.title")));
        taxesPanel.setName("taxesPanel");

        final GroupLayout layout = new GroupLayout(taxesPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        taxesPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTaxes)
                        .addComponent(taxesSubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseTaxes)
                        .addComponent(taxesSubPanel));

        return taxesPanel;
    }

    private JPanel createTaxesSubPanel() {
        boolean isEnabled = campaign.getCampaignOptions().isUseTaxes();

        JLabel lblTaxesPercentage = new JLabel();
        lblTaxesPercentage.setText(resources.getString("lblTaxesPercentage.text"));
        lblTaxesPercentage.setToolTipText(resources.getString("lblTaxesPercentage.toolTipText"));
        lblTaxesPercentage.setName("lblTaxesPercentage");
        lblTaxesPercentage.setEnabled(isEnabled);

        spnTaxesPercentage = new JSpinner(new SpinnerNumberModel(30, 1, 100, 1));
        spnTaxesPercentage.setToolTipText(resources.getString("lblTaxesPercentage.toolTipText"));
        spnTaxesPercentage.setName("spnTaxesPercentage");
        spnTaxesPercentage.setEnabled(isEnabled);

        taxesSubPanel.setBorder(BorderFactory.createTitledBorder(""));
        taxesSubPanel.setName("taxesSubPanel");

        final GroupLayout layout = new GroupLayout(taxesSubPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        taxesSubPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblTaxesPercentage)
                                .addComponent(spnTaxesPercentage, Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTaxesPercentage)
                                .addComponent(spnTaxesPercentage)));

        return taxesSubPanel;
    }

    private JPanel createSharesPanel() {
        chkUseShareSystem = new JCheckBox(resources.getString("chkUseShareSystem.text"));
        chkUseShareSystem.setToolTipText(resources.getString("chkUseShareSystem.toolTipText"));
        chkUseShareSystem.setName("chkUseShareSystem");

        sharesSubPanel = createSharesSubPanel();

        JPanel sharesPanel = new JDisableablePanel("sharesPanel");
        sharesPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("sharesPanel.title")));

        final GroupLayout layout = new GroupLayout(sharesPanel);
        sharesPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseShareSystem)
                        .addComponent(sharesSubPanel));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chkUseShareSystem)
                        .addComponent(sharesSubPanel));

        return sharesPanel;
    }

    private JPanel createSharesSubPanel() {
        chkSharesForAll = new JCheckBox(resources.getString("chkSharesForAll.text"));
        chkSharesForAll.setToolTipText(resources.getString("chkSharesForAll.toolTipText"));
        chkSharesForAll.setName("chkSharesForAll");

        sharesSubPanel = new JDisableablePanel("sharesSubPanel");

        final GroupLayout layout = new GroupLayout(sharesSubPanel);
        sharesSubPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout
                .createSequentialGroup()
                .addComponent(chkSharesForAll));

        layout.setHorizontalGroup(layout
                .createParallelGroup(Alignment.LEADING)
                .addComponent(chkSharesForAll));

        return sharesSubPanel;
    }
    // endregion Finances Tab

    // region Rank Systems Tab
    private JScrollPane createRankSystemsTab() {
        rankSystemsPane = new RankSystemsPane(getFrame(), getCampaign());
        return rankSystemsPane;
    }
    // endregion Rank Systems Tab

    // region Markets Tab
    private JScrollPane createMarketsTab() {
        final AbstractMHQScrollablePanel marketsPanel = new DefaultMHQScrollablePanel(getFrame(),
                "marketsPanel",
                new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        marketsPanel.add(createPersonnelMarketPanel(), gbc);

        gbc.gridx++;
        marketsPanel.add(createContractMarketPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        marketsPanel.add(createUnitMarketPanel(), gbc);

        JScrollPane scrollMarkets = new JScrollPane(marketsPanel);
        scrollMarkets.setPreferredSize(new Dimension(500, 400));

        return scrollMarkets;
    }

    private JPanel createPersonnelMarketPanel() {
        // Initialize Labels Used in ActionListeners
        final JPanel personnelMarketRandomRemovalTargetsPanel = new JDisableablePanel(
                "personnelMarketRandomRemovalTargetsPanel");
        final JLabel lblPersonnelMarketDylansWeight = new JLabel();

        // Create Panel Components
        final JLabel lblPersonnelMarketType = new JLabel(resources.getString("lblPersonnelMarket.text"));
        lblPersonnelMarketType.setToolTipText(resources.getString("lblPersonnelMarketType.toolTipText"));
        lblPersonnelMarketType.setName("lblPersonnelMarketType");

        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance()
                .getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType", personnelMarketTypeModel);
        comboPersonnelMarketType.setToolTipText(resources.getString("lblPersonnelMarketType.toolTipText"));
        comboPersonnelMarketType.addActionListener(evt -> {
            final boolean isDylan = new PersonnelMarketDylan().getModuleName()
                    .equals(comboPersonnelMarketType.getSelectedItem());
            final boolean enabled = isDylan
                    || new PersonnelMarketRandom().getModuleName()
                            .equals(comboPersonnelMarketType.getSelectedItem());
            personnelMarketRandomRemovalTargetsPanel.setEnabled(enabled);
            lblPersonnelMarketDylansWeight.setEnabled(isDylan);
            spnPersonnelMarketDylansWeight.setEnabled(isDylan);
        });

        chkPersonnelMarketReportRefresh = new JCheckBox(
                resources.getString("chkPersonnelMarketReportRefresh.text"));
        chkPersonnelMarketReportRefresh
                .setToolTipText(resources.getString("chkPersonnelMarketReportRefresh.toolTipText"));
        chkPersonnelMarketReportRefresh.setName("chkPersonnelMarketReportRefresh");

        createPersonnelMarketRandomRemovalTargetsPanel(personnelMarketRandomRemovalTargetsPanel);

        lblPersonnelMarketDylansWeight.setText(resources.getString("lblPersonnelMarketDylansWeight.text"));
        lblPersonnelMarketDylansWeight
                .setToolTipText(resources.getString("lblPersonnelMarketDylansWeight.toolTipText"));
        lblPersonnelMarketDylansWeight.setName("lblPersonnelMarketDylansWeight");

        spnPersonnelMarketDylansWeight = new JSpinner(new SpinnerNumberModel(0.3, 0, 1, 0.1));
        spnPersonnelMarketDylansWeight
                .setToolTipText(resources.getString("lblPersonnelMarketDylansWeight.toolTipText"));
        spnPersonnelMarketDylansWeight.setName("spnPersonnelMarketDylansWeight");

        chkUsePersonnelHireHiringHallOnly = new JCheckBox(
                resources.getString("chkUsePersonnelHireHiringHallOnly.text"));
        chkUsePersonnelHireHiringHallOnly
                .setToolTipText(resources.getString("chkUsePersonnelHireHiringHallOnly.toolTipText"));
        chkUsePersonnelHireHiringHallOnly.setName("chkUsePersonnelHireHiringHallOnly");

        // Programmatically Assign Accessibility Labels
        lblPersonnelMarketType.setLabelFor(comboPersonnelMarketType);
        lblPersonnelMarketDylansWeight.setLabelFor(spnPersonnelMarketDylansWeight);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelMarketPanel.title")));
        panel.setName("personnelMarketPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketType)
                                .addComponent(comboPersonnelMarketType,
                                        Alignment.LEADING))
                        .addComponent(chkPersonnelMarketReportRefresh)
                        .addComponent(personnelMarketRandomRemovalTargetsPanel)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketDylansWeight)
                                .addComponent(spnPersonnelMarketDylansWeight,
                                        Alignment.LEADING))
                        .addComponent(chkUsePersonnelHireHiringHallOnly));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketType)
                                .addComponent(comboPersonnelMarketType))
                        .addComponent(chkPersonnelMarketReportRefresh)
                        .addComponent(personnelMarketRandomRemovalTargetsPanel)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketDylansWeight)
                                .addComponent(spnPersonnelMarketDylansWeight))
                        .addComponent(chkUsePersonnelHireHiringHallOnly));

        return panel;
    }

    private void createPersonnelMarketRandomRemovalTargetsPanel(final JPanel panel) {
        spnPersonnelMarketRandomRemovalTargets = new HashMap<>();

        // Create the Panel
        panel.setBorder(BorderFactory
                .createTitledBorder(
                        resources.getString("personnelMarketRandomRemovalTargetsPanel.title")));
        panel.setToolTipText(resources.getString("personnelMarketRandomRemovalTargetsPanel.toolTipText"));
        panel.setLayout(new GridLayout(Skills.SKILL_LEVELS.length, 2));

        // Fill out the Panel
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel label = new JLabel(skillLevel.toString());
            label.setToolTipText(
                    resources.getString("personnelMarketRandomRemovalTargetsPanel.toolTipText"));
            label.setName("lbl" + skillLevel);
            panel.add(label);

            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
            spinner.setToolTipText(
                    resources.getString("personnelMarketRandomRemovalTargetsPanel.toolTipText"));
            spinner.setName("spn" + skillLevel);
            spnPersonnelMarketRandomRemovalTargets.put(skillLevel, spinner);
            panel.add(spinner);

            label.setLabelFor(spinner);
        }
    }

    private JPanel createUnitMarketPanel() {
        // Create Panel Components
        final JLabel lblUnitMarketMethod = new JLabel(resources.getString("lblUnitMarketMethod.text"));
        lblUnitMarketMethod.setToolTipText(resources.getString("lblUnitMarketMethod.toolTipText"));
        lblUnitMarketMethod.setName("lblUnitMarketMethod");

        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());
        comboUnitMarketMethod.setToolTipText(resources.getString("lblUnitMarketMethod.toolTipText"));
        comboUnitMarketMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UnitMarketMethod) {
                    list.setToolTipText(((UnitMarketMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboUnitMarketMethod.addActionListener(evt -> {
            final UnitMarketMethod method = comboUnitMarketMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            chkUnitMarketRegionalMekVariations.setEnabled(enabled);
            lblUnitMarketSpecialUnitChance.setEnabled(enabled);
            spnUnitMarketSpecialUnitChance.setEnabled(enabled);
            lblUnitMarketRarityModifier.setEnabled(enabled);
            spnUnitMarketRarityModifier.setEnabled(enabled);
            chkInstantUnitMarketDelivery.setEnabled(enabled);
            chkUnitMarketReportRefresh.setEnabled(enabled);
        });

        chkUnitMarketRegionalMekVariations = new JCheckBox(
                resources.getString("chkUnitMarketRegionalMekVariations.text"));
        chkUnitMarketRegionalMekVariations
                .setToolTipText(resources.getString("chkUnitMarketRegionalMekVariations.toolTipText"));
        chkUnitMarketRegionalMekVariations.setName("chkUnitMarketRegionalMekVariations");

        lblUnitMarketSpecialUnitChance = new JLabel();
        lblUnitMarketSpecialUnitChance.setText(resources.getString("lblUnitMarketSpecialUnitChance.text"));
        lblUnitMarketSpecialUnitChance
                .setToolTipText(resources.getString("lblUnitMarketSpecialUnitChance.toolTipText"));
        lblUnitMarketSpecialUnitChance.setName("lblUnitMarketSpecialUnitChance");

        spnUnitMarketSpecialUnitChance = new JSpinner(new SpinnerNumberModel(30, 0, 100, 1));
        spnUnitMarketSpecialUnitChance
                .setToolTipText(resources.getString("lblUnitMarketSpecialUnitChance.toolTipText"));
        spnUnitMarketSpecialUnitChance.setName("spnUnitMarketSpecialUnitChance");

        lblUnitMarketRarityModifier = new JLabel(resources.getString("lblUnitMarketRarityModifier.text"));
        lblUnitMarketRarityModifier
                .setToolTipText(resources.getString("lblUnitMarketRarityModifier.toolTipText"));
        lblUnitMarketRarityModifier.setName("lblUnitMarketRarityModifier");

        spnUnitMarketRarityModifier = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        spnUnitMarketRarityModifier
                .setToolTipText(resources.getString("lblUnitMarketRarityModifier.toolTipText"));
        spnUnitMarketRarityModifier.setName("spnUnitMarketRarityModifier");

        chkInstantUnitMarketDelivery = new JCheckBox(resources.getString("chkInstantUnitMarketDelivery.text"));
        chkInstantUnitMarketDelivery
                .setToolTipText(resources.getString("chkInstantUnitMarketDelivery.toolTipText"));
        chkInstantUnitMarketDelivery.setName("chkInstantUnitMarketDelivery");

        chkUnitMarketReportRefresh = new JCheckBox(resources.getString("chkUnitMarketReportRefresh.text"));
        chkUnitMarketReportRefresh
                .setToolTipText(resources.getString("chkUnitMarketReportRefresh.toolTipText"));
        chkUnitMarketReportRefresh.setName("chkUnitMarketReportRefresh");

        // Programmatically Assign Accessibility Labels
        lblUnitMarketMethod.setLabelFor(comboUnitMarketMethod);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("unitMarketPanel.title")));
        panel.setName("unitMarketPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblUnitMarketMethod)
                                .addComponent(comboUnitMarketMethod, Alignment.LEADING))
                        .addComponent(chkUnitMarketRegionalMekVariations)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblUnitMarketSpecialUnitChance)
                                .addComponent(spnUnitMarketSpecialUnitChance,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblUnitMarketRarityModifier)
                                .addComponent(spnUnitMarketRarityModifier,
                                        Alignment.LEADING))
                        .addComponent(chkInstantUnitMarketDelivery)
                        .addComponent(chkUnitMarketReportRefresh));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnitMarketMethod)
                                .addComponent(comboUnitMarketMethod))
                        .addComponent(chkUnitMarketRegionalMekVariations)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnitMarketSpecialUnitChance)
                                .addComponent(spnUnitMarketSpecialUnitChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnitMarketRarityModifier)
                                .addComponent(spnUnitMarketRarityModifier))
                        .addComponent(chkInstantUnitMarketDelivery)
                        .addComponent(chkUnitMarketReportRefresh));

        return panel;
    }

    private JPanel createContractMarketPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblContractSearchRadius = new JLabel();
        final JLabel lblCoontractMaxSalvagePercentage = new JLabel();

        // Create Panel Components
        final JLabel lblContractMarketMethod = new JLabel(resources.getString("lblContractMarketMethod.text"));
        lblContractMarketMethod.setToolTipText(resources.getString("lblContractMarketMethod.toolTipText"));
        lblContractMarketMethod.setName("lblContractMarketMethod");

        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod",
                ContractMarketMethod.values());
        comboContractMarketMethod.setToolTipText(resources.getString("lblContractMarketMethod.toolTipText"));
        comboContractMarketMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContractMarketMethod) {
                    list.setToolTipText(wordWrap(((ContractMarketMethod) value).getToolTipText()));
                }
                return this;
            }
        });
        comboContractMarketMethod.addActionListener(evt -> {
            final ContractMarketMethod method = comboContractMarketMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            lblContractSearchRadius.setEnabled(enabled);
            spnContractSearchRadius.setEnabled(enabled);
            chkVariableContractLength.setEnabled(enabled);
            chkContractMarketReportRefresh.setEnabled(enabled);
            spnContractMaxSalvagePercentage.setEnabled(enabled);
        });
        comboContractMarketMethod.setEnabled(true);

        lblContractSearchRadius.setText(resources.getString("lblContractSearchRadius.text"));
        lblContractSearchRadius.setToolTipText(resources.getString("lblContractSearchRadius.toolTipText"));
        lblContractSearchRadius.setName("lblContractSearchRadius");

        spnContractSearchRadius = new JSpinner(new SpinnerNumberModel(300, 100, 2500, 100));
        spnContractSearchRadius.setToolTipText(resources.getString("lblContractSearchRadius.toolTipText"));
        spnContractSearchRadius.setName("spnContractSearchRadius");

        chkVariableContractLength = new JCheckBox(resources.getString("chkVariableContractLength.text"));
        chkVariableContractLength.setToolTipText(resources.getString("chkVariableContractLength.toolTipText"));
        chkVariableContractLength.setName("chkVariableContractLength");

        chkContractMarketReportRefresh = new JCheckBox(
                resources.getString("chkContractMarketReportRefresh.text"));
        chkContractMarketReportRefresh
                .setToolTipText(resources.getString("chkContractMarketReportRefresh.toolTipText"));
        chkContractMarketReportRefresh.setName("chkContractMarketReportRefresh");

        lblCoontractMaxSalvagePercentage.setText(resources.getString("lblContractMaxSalvagePercentage.text"));
        lblCoontractMaxSalvagePercentage
                .setToolTipText(resources.getString("lblContractMaxSalvagePercentage.toolTipText"));
        lblCoontractMaxSalvagePercentage.setName("lblContractSearchRadius");

        spnContractMaxSalvagePercentage = new JSpinner(new SpinnerNumberModel(100, 0, 100, 10));
        spnContractMaxSalvagePercentage
                .setToolTipText(resources.getString("lblContractMaxSalvagePercentage.toolTipText"));
        spnContractMaxSalvagePercentage.setName("spnContractMaxSalvagePercentage");

        JLabel lblDropShipBonusPercentage = new JLabel(resources.getString("lblDropShipBonusPercentage.text"));
        lblDropShipBonusPercentage
                .setToolTipText(wordWrap(
                        resources.getString("lblDropShipBonusPercentage.toolTipText")));
        lblDropShipBonusPercentage.setName("lblDropShipBonusPercentage");

        spnDropShipBonusPercentage = new JSpinner(new SpinnerNumberModel(0, 0, 20, 5));
        spnDropShipBonusPercentage
                .setToolTipText(resources.getString("lblDropShipBonusPercentage.toolTipText"));
        spnDropShipBonusPercentage.setName("spnDropShipBonusPercentage");

        // Programmatically Assign Accessibility Labels
        lblContractMarketMethod.setLabelFor(comboContractMarketMethod);
        lblContractSearchRadius.setLabelFor(spnContractSearchRadius);
        lblCoontractMaxSalvagePercentage.setLabelFor(spnContractMaxSalvagePercentage);

        // Layout the UI
        contractMarketPanel = new JDisableablePanel("contractMarketPanel");
        contractMarketPanel
                .setBorder(BorderFactory
                        .createTitledBorder(resources.getString("contractMarketPanel.title")));

        final GroupLayout layout = new GroupLayout(contractMarketPanel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        contractMarketPanel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblContractMarketMethod)
                                .addComponent(comboContractMarketMethod,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblContractSearchRadius)
                                .addComponent(spnContractSearchRadius,
                                        Alignment.LEADING))
                        .addComponent(chkVariableContractLength)
                        .addComponent(chkContractMarketReportRefresh)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblCoontractMaxSalvagePercentage)
                                .addComponent(spnContractMaxSalvagePercentage,
                                        Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblDropShipBonusPercentage)
                                .addComponent(spnDropShipBonusPercentage,
                                        Alignment.LEADING)));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblContractMarketMethod)
                                .addComponent(comboContractMarketMethod))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblContractSearchRadius)
                                .addComponent(spnContractSearchRadius))
                        .addComponent(chkVariableContractLength)
                        .addComponent(chkContractMarketReportRefresh)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCoontractMaxSalvagePercentage)
                                .addComponent(spnContractMaxSalvagePercentage))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblDropShipBonusPercentage)
                                .addComponent(spnDropShipBonusPercentage)));

        return contractMarketPanel;
    }
    // endregion Markets Tab

    // region RATs Tab
    private JScrollPane createRATTab() {
        // Initialize Components Used in ActionListeners
        final JDisableablePanel traditionalRATPanel = new JDisableablePanel("traditionalRATPanel");

        // Initialize Parsing Variables
        final ButtonGroup group = new ButtonGroup();

        // Create the Panel
        final AbstractMHQScrollablePanel panel = new DefaultMHQScrollablePanel(getFrame(), "ratPanel",
                new GridBagLayout());

        // Create Panel Components
        btnUseRATGenerator = new JRadioButton(resources.getString("btnUseRATGenerator.text"));
        btnUseRATGenerator.setToolTipText(resources.getString("btnUseRATGenerator.tooltip"));
        btnUseRATGenerator.setName("btnUseRATGenerator");
        group.add(btnUseRATGenerator);
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        panel.add(btnUseRATGenerator, gbc);

        btnUseStaticRATs = new JRadioButton(resources.getString("btnUseStaticRATs.text"));
        btnUseStaticRATs.setToolTipText(resources.getString("btnUseStaticRATs.tooltip"));
        btnUseStaticRATs.setName("btnUseStaticRATs");
        btnUseStaticRATs.addItemListener(ev -> traditionalRATPanel.setEnabled(btnUseStaticRATs.isSelected()));
        group.add(btnUseStaticRATs);
        gbc.gridy++;
        panel.add(btnUseStaticRATs, gbc);

        createTraditionalRATPanel(traditionalRATPanel);
        gbc.gridy++;
        panel.add(traditionalRATPanel, gbc);

        // Disable Panel Portions by Default
        btnUseStaticRATs.setSelected(true);
        btnUseStaticRATs.doClick();

        return new JScrollPane(panel);
    }

    private void createTraditionalRATPanel(final JDisableablePanel panel) {
        // Initialize Components Used in ActionListeners
        final JList<String> chosenRATs = new JList<>();

        // Create Panel Components
        final JTextArea txtRATInstructions = new JTextArea(resources.getString("txtRATInstructions.text"));
        txtRATInstructions.setEditable(false);
        txtRATInstructions.setLineWrap(true);
        txtRATInstructions.setWrapStyleWord(true);

        final JLabel lblAvailableRATs = new JLabel(resources.getString("lblAvailableRATs.text"));

        availableRATModel = new DefaultListModel<>();
        for (final String rat : RATManager.getAllRATCollections().keySet()) {
            final List<Integer> eras = RATManager.getAllRATCollections().get(rat);
            if (eras != null) {
                final StringBuilder displayName = new StringBuilder(rat);
                if (!eras.isEmpty()) {
                    displayName.append(" (").append(eras.get(0));
                    if (eras.size() > 1) {
                        displayName.append('-').append(eras.get(eras.size() - 1));
                    }
                    displayName.append(')');
                }
                availableRATModel.addElement(displayName.toString());
            }
        }
        final JList<String> availableRATs = new JList<>(availableRATModel);
        availableRATs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JButton btnAddRAT = new MMButton("btnAddRAT", resources, "btnAddRAT.text",
                "btnAddRAT.toolTipText", evt -> {
                    final int selectedIndex = availableRATs.getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    chosenRATModel.addElement(availableRATs.getSelectedValue());
                    availableRATModel.removeElementAt(availableRATs.getSelectedIndex());
                    availableRATs.setSelectedIndex(
                            Math.min(selectedIndex, availableRATModel.size() - 1));
                });

        final JButton btnRemoveRAT = new MMButton("btnRemoveRAT", resources, "btnRemoveRAT.text",
                "btnRemoveRAT.toolTipText", evt -> {
                    final int selectedIndex = chosenRATs.getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    availableRATModel.addElement(chosenRATs.getSelectedValue());
                    chosenRATModel.removeElementAt(chosenRATs.getSelectedIndex());
                    chosenRATs.setSelectedIndex(Math.min(selectedIndex, chosenRATModel.size() - 1));
                });

        final JButton btnMoveRATUp = new MMButton("btnMoveRATUp", resources, "btnMoveRATUp.text",
                "btnMoveRATUp.toolTipText", evt -> {
                    final int selectedIndex = chosenRATs.getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    final String element = chosenRATModel.getElementAt(selectedIndex);
                    chosenRATModel.setElementAt(chosenRATModel.getElementAt(selectedIndex - 1),
                            selectedIndex);
                    chosenRATModel.setElementAt(element, selectedIndex - 1);
                    chosenRATs.setSelectedIndex(selectedIndex - 1);
                });

        final JButton btnMoveRATDown = new MMButton("btnMoveRATDown", resources, "btnMoveRATDown.text",
                "btnMoveRATDown.toolTipText", evt -> {
                    final int selectedIndex = chosenRATs.getSelectedIndex();
                    if (selectedIndex < 0) {
                        return;
                    }
                    final String element = chosenRATModel.getElementAt(selectedIndex);
                    chosenRATModel.setElementAt(chosenRATModel.getElementAt(selectedIndex + 1),
                            selectedIndex);
                    chosenRATModel.setElementAt(element, selectedIndex + 1);
                    chosenRATs.setSelectedIndex(selectedIndex + 1);
                });

        final JLabel lblChosenRATs = new JLabel(resources.getString("lblChosenRATs.text"));

        chosenRATModel = new DefaultListModel<>();
        chosenRATs.setModel(chosenRATModel);
        chosenRATs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chosenRATs.addListSelectionListener(evt -> {
            btnRemoveRAT.setEnabled(chosenRATs.getSelectedIndex() >= 0);
            btnMoveRATUp.setEnabled(chosenRATs.getSelectedIndex() > 0);
            btnMoveRATDown.setEnabled(chosenRATModel.size() > chosenRATs.getSelectedIndex() + 1);
        });

        chkIgnoreRATEra = new JCheckBox(resources.getString("chkIgnoreRATEra.text"));
        chkIgnoreRATEra.setToolTipText(resources.getString("chkIgnoreRATEra.toolTipText"));
        chkIgnoreRATEra.setName("chkIgnoreRATEra");

        // Add Previously Impossible Listeners
        availableRATs.addListSelectionListener(
                evt -> btnAddRAT.setEnabled(availableRATs.getSelectedIndex() >= 0));

        // Programmatically Assign Accessibility Labels
        lblAvailableRATs.setLabelFor(availableRATs);
        lblChosenRATs.setLabelFor(chosenRATs);

        // Layout the UI
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("traditionalRATPanel.title")));
        panel.setLayout(new BorderLayout());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(txtRATInstructions)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(lblAvailableRATs)
                                        .addComponent(availableRATs))
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(btnAddRAT)
                                        .addComponent(btnRemoveRAT)
                                        .addComponent(btnMoveRATUp)
                                        .addComponent(btnMoveRATDown))
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(lblChosenRATs)
                                        .addComponent(chosenRATs)))
                        .addComponent(chkIgnoreRATEra));

        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(txtRATInstructions)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(lblAvailableRATs)
                                        .addComponent(lblChosenRATs))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(availableRATs)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnAddRAT)
                                                .addPreferredGap(
                                                        ComponentPlacement.RELATED)
                                                .addComponent(btnRemoveRAT)
                                                .addPreferredGap(
                                                        ComponentPlacement.RELATED)
                                                .addComponent(btnMoveRATUp)
                                                .addPreferredGap(
                                                        ComponentPlacement.RELATED)
                                                .addComponent(btnMoveRATDown))
                                        .addComponent(chosenRATs))
                                .addComponent(chkIgnoreRATEra)));
    }
    // endregion RATs Tab

    // region Against the Bot Tab
    // endregion Against the Bot Tab
    // endregion Modern Initialization
    // endregion Initialization

    // region Options
    public void setOptions(@Nullable CampaignOptions options,
            @Nullable RandomSkillPreferences randomSkillPreferences) {
        // Use the provided options and preferences when possible, but flip if they are
        // null to be safe
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

        // region General Tab
        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod());
        manualUnitRatingModifier.setValue(options.getManualUnitRatingModifier());
        // endregion General Tab

        // region Repair and Maintenance Tab
        useEraModsCheckBox.setSelected(options.isUseEraMods());
        assignedTechFirstCheckBox.setSelected(options.isAssignedTechFirst());
        resetToFirstTechCheckBox.setSelected(options.isResetToFirstTech());
        useQuirksBox.setSelected(options.isUseQuirks());
        useAeroSystemHitsBox.setSelected(options.isUseAeroSystemHits());
        if (useDamageMargin.isSelected() != options.isDestroyByMargin()) {
            useDamageMargin.doClick();
        }
        spnDamageMargin.setValue(options.getDestroyMargin());
        spnDestroyPartTarget.setValue(options.getDestroyPartTarget());

        if (checkMaintenance.isSelected() != options.isCheckMaintenance()) {
            checkMaintenance.doClick();
        }
        spnMaintenanceDays.setValue(options.getMaintenanceCycleDays());
        spnMaintenanceBonus.setValue(options.getMaintenanceBonus());
        spnDefaultMaintenanceTime.setValue(options.getDefaultMaintenanceTime());
        useQualityMaintenance.setSelected(options.isUseQualityMaintenance());
        reverseQualityNames.setSelected(options.isReverseQualityNames());
        chkUseRandomUnitQualities.setSelected(options.isUseRandomUnitQualities());
        chkUsePlanetaryModifiers.setSelected(options.isUsePlanetaryModifiers());
        useUnofficialMaintenance.setSelected(options.isUseUnofficialMaintenance());
        logMaintenance.setSelected(options.isLogMaintenance());
        // endregion Repair and Maintenance Tab

        // region Supplies and Acquisitions Tab
        spnAcquireWaitingPeriod.setValue(options.getWaitingPeriod());
        choiceAcquireSkill.setSelectedItem(options.getAcquisitionSkill());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());
        spnAcquireClanPenalty.setValue(options.getClanAcquisitionPenalty());
        spnAcquireIsPenalty.setValue(options.getIsAcquisitionPenalty());
        spnMaxAcquisitions.setValue(options.getMaxAcquisitions());

        spnNDiceTransitTime.setValue(options.getNDiceTransitTime());
        spnConstantTransitTime.setValue(options.getConstantTransitTime());
        choiceTransitTimeUnits
                .setSelectedItem(CampaignOptions.getTransitUnitName(options.getUnitTransitTime()));
        spnAcquireMinimum.setValue(options.getAcquireMinimumTime());
        choiceAcquireMinimumUnit
                .setSelectedItem(CampaignOptions
                        .getTransitUnitName(options.getAcquireMinimumTimeUnit()));
        spnAcquireMosBonus.setValue(options.getAcquireMosBonus());
        choiceAcquireMosUnits.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMosUnit()));

        usePlanetaryAcquisitions.setSelected(options.isUsePlanetaryAcquisition());
        spnMaxJumpPlanetaryAcquisitions.setValue(options.getMaxJumpsPlanetaryAcquisition());
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(options.getPlanetAcquisitionFactionLimit());
        disallowPlanetaryAcquisitionClanCrossover.setSelected(options.isPlanetAcquisitionNoClanCrossover());
        disallowClanPartsFromIS.setSelected(options.isNoClanPartsFromIS());
        spnPenaltyClanPartsFromIS.setValue(options.getPenaltyClanPartsFromIS());
        usePlanetaryAcquisitionsVerbose.setSelected(options.isPlanetAcquisitionVerbose());
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            spnPlanetAcquireTechBonus[i].setValue(options.getPlanetTechAcquisitionBonus(i));
            spnPlanetAcquireIndustryBonus[i].setValue(options.getPlanetIndustryAcquisitionBonus(i));
            spnPlanetAcquireOutputBonus[i].setValue(options.getPlanetOutputAcquisitionBonus(i));
        }
        // endregion Supplies and Acquisitions Tab

        // region Tech Limits Tab
        if (limitByYearBox.isSelected() != options.isLimitByYear()) {
            limitByYearBox.doClick();
        }
        disallowExtinctStuffBox.setSelected(options.isDisallowExtinctStuff());
        allowClanPurchasesBox.setSelected(options.isAllowClanPurchases());
        allowISPurchasesBox.setSelected(options.isAllowISPurchases());
        allowCanonOnlyBox.setSelected(options.isAllowCanonOnly());
        allowCanonRefitOnlyBox.setSelected(options.isAllowCanonRefitOnly());
        choiceTechLevel.setSelectedIndex(options.getTechLevel());
        variableTechLevelBox.setSelected(options.isVariableTechLevel() && options.isLimitByYear());
        factionIntroDateBox.setSelected(options.isFactionIntroDate());
        useAmmoByTypeBox.setSelected(options.isUseAmmoByType());
        // endregion Tech Limits Tab

        // region Personnel Tab
        // General Personnel
        chkUseTactics.setSelected(options.isUseTactics());
        chkUseInitiativeBonus.setSelected(options.isUseInitiativeBonus());
        chkUseToughness.setSelected(options.isUseToughness());
        chkUseRandomToughness.setSelected(options.isUseRandomToughness());
        chkUseArtillery.setSelected(options.isUseArtillery());
        chkUseAbilities.setSelected(options.isUseAbilities());
        if (chkUseEdge.isSelected() != options.isUseEdge()) {
            chkUseEdge.doClick();
        }
        chkUseSupportEdge.setSelected(options.isUseSupportEdge());
        chkUseImplants.setSelected(options.isUseImplants());
        chkUseAlternativeQualityAveraging.setSelected(options.isAlternativeQualityAveraging());
        chkUseTransfers.setSelected(options.isUseTransfers());
        chkUseExtendedTOEForceName.setSelected(options.isUseExtendedTOEForceName());
        chkPersonnelLogSkillGain.setSelected(options.isPersonnelLogSkillGain());
        chkPersonnelLogAbilityGain.setSelected(options.isPersonnelLogAbilityGain());
        chkPersonnelLogEdgeGain.setSelected(options.isPersonnelLogEdgeGain());
        chkDisplayPersonnelLog.setSelected(options.isDisplayPersonnelLog());
        chkDisplayScenarioLog.setSelected(options.isDisplayScenarioLog());
        chkDisplayKillRecord.setSelected(options.isDisplayKillRecord());

        // Expanded Personnel Information
        if (chkUseTimeInService.isSelected() != options.isUseTimeInService()) {
            chkUseTimeInService.doClick();
        }
        comboTimeInServiceDisplayFormat.setSelectedItem(options.getTimeInServiceDisplayFormat());
        if (chkUseTimeInRank.isSelected() != options.isUseTimeInRank()) {
            chkUseTimeInRank.doClick();
        }
        comboTimeInRankDisplayFormat.setSelectedItem(options.getTimeInRankDisplayFormat());
        chkTrackTotalEarnings.setSelected(options.isTrackTotalEarnings());
        chkTrackTotalXPEarnings.setSelected(options.isTrackTotalXPEarnings());
        chkShowOriginFaction.setSelected(options.isShowOriginFaction());

        // Administrators
        chkAdminsHaveNegotiation.setSelected(options.isAdminsHaveNegotiation());
        chkAdminExperienceLevelIncludeNegotiation
                .setSelected(options.isAdminExperienceLevelIncludeNegotiation());
        chkAdminsHaveScrounge.setSelected(options.isAdminsHaveScrounge());
        chkAdminExperienceLevelIncludeScrounge.setSelected(options.isAdminExperienceLevelIncludeScrounge());

        // Medical
        chkUseAdvancedMedical.setSelected(options.isUseAdvancedMedical());
        spnHealWaitingPeriod.setValue(options.getHealingWaitingPeriod());
        spnNaturalHealWaitingPeriod.setValue(options.getNaturalHealingWaitingPeriod());
        spnMinimumHitsForVehicles.setValue(options.getMinimumHitsForVehicles());
        chkUseRandomHitsForVehicles.setSelected(options.isUseRandomHitsForVehicles());
        chkUseTougherHealing.setSelected(options.isTougherHealing());
        spnMaximumPatients.setValue(options.getMaximumPatients());

        // Prisoners
        comboPrisonerCaptureStyle.setSelectedItem(options.getPrisonerCaptureStyle());
        comboPrisonerStatus.setSelectedItem(options.getDefaultPrisonerStatus());
        chkPrisonerBabyStatus.setSelected(options.isPrisonerBabyStatus());
        chkAtBPrisonerDefection.setSelected(options.isUseAtBPrisonerDefection());
        chkAtBPrisonerRansom.setSelected(options.isUseAtBPrisonerRansom());

        // Dependent
        chkUseRandomDependentAddition.setSelected(options.isUseRandomDependentAddition());
        chkUseRandomDependentRemoval.setSelected(options.isUseRandomDependentRemoval());

        // Personnel Removal
        chkUsePersonnelRemoval.setSelected(options.isUsePersonnelRemoval());
        chkUseRemovalExemptCemetery.setSelected(options.isUseRemovalExemptCemetery());
        chkUseRemovalExemptRetirees.setSelected(options.isUseRemovalExemptRetirees());

        // Salary
        chkDisableSecondaryRoleSalary.setSelected(options.isDisableSecondaryRoleSalary());
        spnAntiMekSalary.setValue(options.getSalaryAntiMekMultiplier());
        spnSpecialistInfantrySalary.setValue(options.getSalarySpecialistInfantryMultiplier());
        for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
            entry.getValue().setValue(options.getSalaryXPMultipliers().get(entry.getKey()));
        }

        for (int i = 0; i < spnBaseSalary.length; i++) {
            spnBaseSalary[i].setValue(options.getRoleBaseSalaries()[i].getAmount().doubleValue());
        }

        // Awards
        comboAwardBonusStyle.setSelectedItem(options.getAwardBonusStyle());
        chkEnableAutoAwards.setSelected(options.isEnableAutoAwards());
        chkIssuePosthumousAwards.setSelected(options.isIssuePosthumousAwards());
        chkIssueBestAwardOnly.setSelected(options.isIssueBestAwardOnly());
        chkIgnoreStandardSet.setSelected(options.isIgnoreStandardSet());
        spnAwardTierSize.setValue(options.getAwardTierSize());
        chkEnableContractAwards.setSelected(options.isEnableContractAwards());
        chkEnableFactionHunterAwards.setSelected(options.isEnableFactionHunterAwards());
        chkEnableInjuryAwards.setSelected(options.isEnableInjuryAwards());
        chkEnableIndividualKillAwards.setSelected(options.isEnableIndividualKillAwards());
        chkEnableFormationKillAwards.setSelected(options.isEnableFormationKillAwards());
        chkEnableRankAwards.setSelected(options.isEnableRankAwards());
        chkEnableScenarioAwards.setSelected(options.isEnableScenarioAwards());
        chkEnableSkillAwards.setSelected(options.isEnableSkillAwards());
        chkEnableTheatreOfWarAwards.setSelected(options.isEnableTheatreOfWarAwards());
        chkEnableTimeAwards.setSelected(options.isEnableTimeAwards());
        chkEnableTrainingAwards.setSelected(options.isEnableTimeAwards());
        chkEnableMiscAwards.setSelected(options.isEnableMiscAwards());
        txtAwardSetFilterList.setText(options.getAwardSetFilterList());
        // endregion Personnel Tab

        // region Turnover and Retention Tab
        // Header
        chkUseRandomRetirement.setSelected(options.isUseRandomRetirement());

        // Settings
        spnTurnoverFixedTargetNumber.setValue(options.getTurnoverFixedTargetNumber());
        comboTurnoverFrequency.setSelectedItem(options.getTurnoverFrequency());
        chkUseContractCompletionRandomRetirement.setSelected(options.isUseContractCompletionRandomRetirement());
        chkUseRandomFounderTurnover.setSelected(options.isUseRandomFounderTurnover());
        chkUseFounderRetirement.setSelected(options.isUseFounderRetirement());
        chkTrackOriginalUnit.setSelected(options.isTrackOriginalUnit());
        chkAeroRecruitsHaveUnits.setSelected(options.isAeroRecruitsHaveUnits());
        chkUseSubContractSoldiers.setSelected(options.isUseSubContractSoldiers());
        spnServiceContractDuration.setValue(options.getServiceContractDuration());
        spnServiceContractModifier.setValue(options.getServiceContractModifier());
        chkPayBonusDefault.setSelected(options.isPayBonusDefault());
        spnPayBonusDefaultThreshold.setValue(options.getPayBonusDefaultThreshold());

        // Modifiers
        chkUseCustomRetirementModifiers.setSelected(options.isUseCustomRetirementModifiers());
        chkUseFatigueModifiers.setSelected(options.isUseFatigueModifiers());
        chkUseSkillModifiers.setSelected(options.isUseSkillModifiers());
        chkUseAgeModifiers.setSelected(options.isUseAgeModifiers());
        chkUseUnitRatingModifiers.setSelected(options.isUseUnitRatingModifiers());
        chkUseFactionModifiers.setSelected(options.isUseFactionModifiers());
        chkUseHostileTerritoryModifiers.setSelected(options.isUseHostileTerritoryModifiers());
        chkUseMissionStatusModifiers.setSelected(options.isUseMissionStatusModifiers());
        chkUseFamilyModifiers.setSelected(options.isUseFamilyModifiers());
        chkUseLoyaltyModifiers.setSelected(options.isUseLoyaltyModifiers());
        chkUseHideLoyalty.setSelected(options.isUseHideLoyalty());

        // Payouts
        spnPayoutRateOfficer.setValue(options.getPayoutRateOfficer());
        spnPayoutRateEnlisted.setValue(options.getPayoutRateEnlisted());
        spnPayoutRetirementMultiplier.setValue(options.getPayoutRetirementMultiplier());
        chkUsePayoutServiceBonus.setSelected(options.isUsePayoutServiceBonus());
        spnPayoutServiceBonusRate.setValue(options.getPayoutServiceBonusRate());

        // Unit Cohesion
        chkUseAdministrativeStrain.setSelected(options.isUseAdministrativeStrain());
        chkUseManagementSkill.setSelected(options.isUseManagementSkill());

        spnAdministrativeCapacity.setValue(options.getAdministrativeCapacity());
        spnMultiCrewStrainDivider.setValue(options.getMultiCrewStrainDivider());

        chkUseCommanderLeadershipOnly.setSelected(options.isUseCommanderLeadershipOnly());
        spnManagementSkillPenalty.setValue(options.getManagementSkillPenalty());

        // Fatigue
        chkUseFatigue.setSelected(options.isUseFatigue());
        spnFatigueRate.setValue(options.getFatigueRate());
        chkUseInjuryFatigue.setSelected(options.isUseInjuryFatigue());
        spnFieldKitchenCapacity.setValue(options.getFieldKitchenCapacity());
        chkFieldKitchenIgnoreNonCombatants.setSelected(options.isUseFieldKitchenIgnoreNonCombatants());
        spnFatigueLeaveThreshold.setValue(options.getFatigueLeaveThreshold());
        // endregion Turnover and Retention Tab

        // region Life Paths Tab
        // Personnel Randomization
        chkUseDylansRandomXP.setSelected(options.isUseDylansRandomXP());
        spnNonBinaryDiceSize.setValue(options.getNonBinaryDiceSize());

        // Random Histories
        randomOriginOptionsPanel.setOptions(options.getRandomOriginOptions());
        chkUseRandomPersonalities.setSelected(options.isUseRandomPersonalities());
        chkUseRandomPersonalityReputation.setSelected(options.isUseRandomPersonalityReputation());
        chkUseIntelligenceXpMultiplier.setSelected(options.isUseIntelligenceXpMultiplier());
        chkUseSimulatedRelationships.setSelected(options.isUseSimulatedRelationships());

        // Family
        comboFamilyDisplayLevel.setSelectedItem(options.getFamilyDisplayLevel());

        // Anniversaries
        chkAnnounceBirthdays.setSelected(options.isAnnounceBirthdays());
        chkAnnounceRecruitmentAnniversaries.setSelected(options.isAnnounceRecruitmentAnniversaries());
        chkAnnounceOfficersOnly.setSelected(options.isAnnounceOfficersOnly());
        chkAnnounceChildBirthdays.setSelected(options.isAnnounceChildBirthdays());

        // Marriage
        chkUseManualMarriages.setSelected(options.isUseManualMarriages());
        chkUseClanPersonnelMarriages.setSelected(options.isUseClanPersonnelMarriages());
        chkUsePrisonerMarriages.setSelected(options.isUsePrisonerMarriages());
        spnNoInterestInMarriageDiceSize.setValue(options.getNoInterestInMarriageDiceSize());
        spnCheckMutualAncestorsDepth.setValue(options.getCheckMutualAncestorsDepth());
        chkLogMarriageNameChanges.setSelected(options.isLogMarriageNameChanges());
        for (final Entry<MergingSurnameStyle, JSpinner> entry : spnMarriageSurnameWeights.entrySet()) {
            entry.getValue().setValue(options.getMarriageSurnameWeights().get(entry.getKey()) / 10.0);
        }
        comboRandomMarriageMethod.setSelectedItem(options.getRandomMarriageMethod());
        chkUseRandomClanPersonnelMarriages.setSelected(options.isUseRandomClanPersonnelMarriages());
        chkUseRandomPrisonerMarriages.setSelected(options.isUseRandomPrisonerMarriages());
        spnRandomMarriageAgeRange.setValue(options.getRandomMarriageAgeRange());
        spnRandomMarriageDiceSize.setValue(options.getRandomMarriageDiceSize());
        spnRandomSameSexMarriageDiceSize.setValue(options.getRandomSameSexMarriageDiceSize());
        spnRandomNewDependentMarriage.setValue(options.getRandomNewDependentMarriage());

        // Divorce
        chkUseManualDivorce.setSelected(options.isUseManualDivorce());
        chkUseClanPersonnelDivorce.setSelected(options.isUseClanPersonnelDivorce());
        chkUsePrisonerDivorce.setSelected(options.isUsePrisonerDivorce());
        for (final Entry<SplittingSurnameStyle, JSpinner> entry : spnDivorceSurnameWeights.entrySet()) {
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
        chkUseRandomClanPersonnelDivorce.setSelected(options.isUseRandomClanPersonnelDivorce());
        chkUseRandomPrisonerDivorce.setSelected(options.isUseRandomPrisonerDivorce());
        spnRandomDivorceDiceSize.setValue(options.getRandomDivorceDiceSize());

        // Procreation
        chkUseManualProcreation.setSelected(options.isUseManualProcreation());
        chkUseClanPersonnelProcreation.setSelected(options.isUseClanPersonnelProcreation());
        chkUsePrisonerProcreation.setSelected(options.isUsePrisonerProcreation());
        spnMultiplePregnancyOccurrences.setValue(options.getMultiplePregnancyOccurrences());
        comboBabySurnameStyle.setSelectedItem(options.getBabySurnameStyle());
        chkAssignNonPrisonerBabiesFounderTag.setSelected(options.isAssignNonPrisonerBabiesFounderTag());
        chkAssignChildrenOfFoundersFounderTag.setSelected(options.isAssignChildrenOfFoundersFounderTag());
        chkDetermineFatherAtBirth.setSelected(options.isDetermineFatherAtBirth());
        chkDisplayTrueDueDate.setSelected(options.isDisplayTrueDueDate());
        spnNoInterestInChildrenDiceSize.setValue(options.getNoInterestInChildrenDiceSize());
        chkUseMaternityLeave.setSelected(options.isUseMaternityLeave());
        chkLogProcreation.setSelected(options.isLogProcreation());
        comboRandomProcreationMethod.setSelectedItem(options.getRandomProcreationMethod());
        if (chkUseRelationshiplessRandomProcreation.isSelected() != options
                .isUseRelationshiplessRandomProcreation()) {
            if (chkUseRelationshiplessRandomProcreation.isEnabled()) {
                chkUseRelationshiplessRandomProcreation.doClick();
            } else {
                chkUseRelationshiplessRandomProcreation
                        .setSelected(options.isUseRelationshiplessRandomProcreation());
            }
        }
        chkUseRandomClanPersonnelProcreation.setSelected(options.isUseRandomClanPersonnelProcreation());
        chkUseRandomPrisonerProcreation.setSelected(options.isUseRandomPrisonerProcreation());
        spnRandomProcreationRelationshipDiceSize.setValue(options.getRandomProcreationRelationshipDiceSize());
        spnRandomProcreationRelationshiplessDiceSize.setValue(options.getRandomProcreationRelationshiplessDiceSize());

        // Education
        chkUseEducationModule.setSelected(options.isUseEducationModule());
        spnCurriculumXpRate.setValue(options.getCurriculumXpRate());
        spnMaximumJumpCount.setValue(options.getMaximumJumpCount());
        chkUseReeducationCamps.setSelected(options.isUseReeducationCamps());
        chkEnableLocalAcademies.setSelected(options.isEnableLocalAcademies());
        chkEnablePrestigiousAcademies.setSelected(options.isEnablePrestigiousAcademies());
        chkEnableUnitEducation.setSelected(options.isEnableUnitEducation());
        chkEnableOverrideRequirements.setSelected(options.isEnableOverrideRequirements());
        chkShowIneligibleAcademies.setSelected(options.isEnableShowIneligibleAcademies());
        spnEntranceExamBaseTargetNumber.setValue(options.getEntranceExamBaseTargetNumber());
        spnFacultyXpMultiplier.setValue(options.getFacultyXpRate());
        chkEnableBonuses.setSelected(options.isEnableBonuses());
        spnAdultDropoutChance.setValue(options.getAdultDropoutChance());
        spnChildrenDropoutChance.setValue(options.getChildrenDropoutChance());
        chkAllAges.setSelected(options.isAllAges());
        spnMilitaryAcademyAccidents.setValue(options.getMilitaryAcademyAccidents());

        // Death
        chkKeepMarriedNameUponSpouseDeath.setSelected(options.isKeepMarriedNameUponSpouseDeath());
        comboRandomDeathMethod.setSelectedItem(options.getRandomDeathMethod());
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            chkEnabledRandomDeathAgeGroups.get(ageGroup)
                    .setSelected(options.getEnabledRandomDeathAgeGroups().get(ageGroup));
        }
        chkUseRandomClanPersonnelDeath.setSelected(options.isUseRandomClanPersonnelDeath());
        chkUseRandomPrisonerDeath.setSelected(options.isUseRandomPrisonerDeath());
        chkUseRandomDeathSuicideCause.setSelected(options.isUseRandomDeathSuicideCause());
        spnPercentageRandomDeathChance.setValue(options.getPercentageRandomDeathChance());
        for (int i = 0; i < spnExponentialRandomDeathMaleValues.length; i++) {
            spnExponentialRandomDeathMaleValues[i]
                    .setValue(options.getExponentialRandomDeathMaleValues()[i]);
            spnExponentialRandomDeathFemaleValues[i]
                    .setValue(options.getExponentialRandomDeathFemaleValues()[i]);
        }
        for (final TenYearAgeRange ageRange : TenYearAgeRange.values()) {
            spnAgeRangeRandomDeathMaleValues.get(ageRange)
                    .setValue(options.getAgeRangeRandomDeathMaleValues().get(ageRange));
            spnAgeRangeRandomDeathFemaleValues.get(ageRange)
                    .setValue(options.getAgeRangeRandomDeathFemaleValues().get(ageRange));
        }
        // endregion Life Paths Tab

        // region Finances Tab
        payForPartsBox.setSelected(options.isPayForParts());
        payForRepairsBox.setSelected(options.isPayForRepairs());
        payForUnitsBox.setSelected(options.isPayForUnits());
        payForSalariesBox.setSelected(options.isPayForSalaries());
        payForOverheadBox.setSelected(options.isPayForOverhead());
        payForMaintainBox.setSelected(options.isPayForMaintain());
        payForTransportBox.setSelected(options.isPayForTransport());
        sellUnitsBox.setSelected(options.isSellUnits());
        sellPartsBox.setSelected(options.isSellParts());
        payForRecruitmentBox.setSelected(options.isPayForRecruitment());
        useLoanLimitsBox.setSelected(options.isUseLoanLimits());
        usePercentageMaintenanceBox.setSelected(options.isUsePercentageMaint());
        useInfantryDoseNotCountBox.setSelected(options.isInfantryDontCount());
        usePeacetimeCostBox.setSelected(options.isUsePeacetimeCost());
        useExtendedPartsModifierBox.setSelected(options.isUseExtendedPartsModifier());
        showPeacetimeCostBox.setSelected(options.isShowPeacetimeCost());
        comboFinancialYearDuration.setSelectedItem(options.getFinancialYearDuration());
        newFinancialYearFinancesToCSVExportBox.setSelected(options.isNewFinancialYearFinancesToCSVExport());

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

        // Shares
        chkUseShareSystem.setSelected(options.isUseShareSystem());
        chkSharesForAll.setSelected(options.isSharesForAll());

        // Taxes
        chkUseTaxes.setSelected(options.isUseTaxes());
        spnTaxesPercentage.setValue(options.getTaxesPercentage());
        // endregion Finances Tab

        // region Mercenary Tab
        if (options.isEquipmentContractBase()) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }
        spnEquipPercent.setValue(options.getEquipmentContractPercent());
        spnDropShipPercent.setValue(options.getDropShipContractPercent());
        spnJumpShipPercent.setValue(options.getJumpShipContractPercent());
        spnWarShipPercent.setValue(options.getWarShipContractPercent());
        chkEquipContractSaleValue.setSelected(options.isEquipmentContractSaleValue());
        chkBLCSaleValue.setSelected(options.isBLCSaleValue());
        chkOverageRepaymentInFinalPayment.setSelected(options.isOverageRepaymentInFinalPayment());
        // endregion Mercenary Tab

        // region Experience Tab
        spnXpCostMultiplier.setValue(options.getXpCostMultiplier());
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
        spnMissionXpFail.setValue(options.getMissionXpFail());
        spnMissionXpSuccess.setValue(options.getMissionXpSuccess());
        spnMissionXpOutstandingSuccess.setValue(options.getMissionXpOutstandingSuccess());
        spnEdgeCost.setValue(options.getEdgeCost());
        // endregion Experience Tab

        // region Skills Tab
        // endregion Skills Tab

        // region Special Abilities Tab
        // endregion Special Abilities Tab

        // region Skill Randomization Tab
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
        spnAbilityGreen.setValue(rSkillPrefs.getSpecialAbilityBonus(SkillType.EXP_GREEN));
        spnAbilityReg.setValue(rSkillPrefs.getSpecialAbilityBonus(SkillType.EXP_REGULAR));
        spnAbilityVet.setValue(rSkillPrefs.getSpecialAbilityBonus(SkillType.EXP_VETERAN));
        spnAbilityElite.setValue(rSkillPrefs.getSpecialAbilityBonus(SkillType.EXP_ELITE));
        spnCombatSA.setValue(rSkillPrefs.getCombatSmallArmsBonus());
        spnSupportSA.setValue(rSkillPrefs.getSupportSmallArmsBonus());
        // endregion Skill Randomization Tab

        // region Rank System Tab
        // endregion Rank System Tab

        // region Name and Portrait Generation Tab
        if (chkUseOriginFactionForNames.isSelected() != options.isUseOriginFactionForNames()) {
            chkUseOriginFactionForNames.doClick();
        }

        boolean allSelected = true;
        boolean noneSelected = true;
        final boolean[] usePortraitForRole = options.isUsePortraitForRoles();
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

        chkAssignPortraitOnRoleChange.setSelected(options.isAssignPortraitOnRoleChange());
        // endregion Name and Portrait Generation Tab

        // region Markets Tab
        comboPersonnelMarketType.setSelectedItem(options.getPersonnelMarketName());
        chkPersonnelMarketReportRefresh.setSelected(options.isPersonnelMarketReportRefresh());
        for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
            entry.getValue().setValue(options.getPersonnelMarketRandomRemovalTargets().get(entry.getKey()));
        }
        spnPersonnelMarketDylansWeight.setValue(options.getPersonnelMarketDylansWeight());
        chkUsePersonnelHireHiringHallOnly.setSelected(options.isUsePersonnelHireHiringHallOnly());

        // Unit Market
        comboUnitMarketMethod.setSelectedItem(options.getUnitMarketMethod());
        chkUnitMarketRegionalMekVariations.setSelected(options.isUnitMarketRegionalMekVariations());
        spnUnitMarketSpecialUnitChance.setValue(options.getUnitMarketSpecialUnitChance());
        spnUnitMarketRarityModifier.setValue(options.getUnitMarketRarityModifier());
        chkInstantUnitMarketDelivery.setSelected(options.isInstantUnitMarketDelivery());
        chkUnitMarketReportRefresh.setSelected(options.isUnitMarketReportRefresh());

        // Contract Market
        comboContractMarketMethod.setSelectedItem(options.getContractMarketMethod());
        spnContractSearchRadius.setValue(options.getContractSearchRadius());
        chkVariableContractLength.setSelected(options.isVariableContractLength());
        chkContractMarketReportRefresh.setSelected(options.isContractMarketReportRefresh());
        spnContractMaxSalvagePercentage.setValue(options.getContractMaxSalvagePercentage());
        spnDropShipBonusPercentage.setValue(options.getDropShipBonusPercentage());
        // endregion Markets Tab

        // region RATs Tab
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
                        displayName.append('-').append(eras.get(eras.size() - 1));
                    }
                    displayName.append(')');
                }

                if (availableRATModel.contains(displayName.toString())) {
                    chosenRATModel.addElement(displayName.toString());
                    availableRATModel.removeElement(displayName.toString());
                }
            }
        }
        chkIgnoreRATEra.setSelected(options.isIgnoreRATEra());
        // endregion RATs Tab

        // region Against the Bot Tab
        if (chkUseAtB.isSelected() != options.isUseAtB()) {
            chkUseAtB.doClick();
        }
        chkUseStratCon.setSelected(options.isUseStratCon());
        comboSkillLevel.setSelectedItem(options.getSkillLevel());
        chkUseAero.setSelected(options.isUseAero());
        chkUseVehicles.setSelected(options.isUseVehicles());
        chkClanVehicles.setSelected(options.isClanVehicles());
        chkAutoConfigMunitions.setSelected(options.isAutoConfigMunitions());
        chkMercSizeLimited.setSelected(options.isMercSizeLimited());
        chkRestrictPartsByMission.setSelected(options.isRestrictPartsByMission());
        spnBonusPartExchangeValue.setValue(options.getBonusPartExchangeValue());
        spnBonusPartMaxExchangeCount.setValue(options.getBonusPartMaxExchangeCount());
        chkLimitLanceWeight.setSelected(options.isLimitLanceWeight());
        chkLimitLanceNumUnits.setSelected(options.isLimitLanceNumUnits());
        chkUseStrategy.setSelected(options.isUseStrategy());
        spnBaseStrategyDeployment.setValue(options.getBaseStrategyDeployment());
        spnAdditionalStrategyDeployment.setValue(options.getAdditionalStrategyDeployment());
        chkAdjustPaymentForStrategy.setSelected(options.isAdjustPaymentForStrategy());
        spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()]
                .setValue(options.getAtBBattleChance(AtBLanceRole.FIGHTING));
        spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()]
                .setValue(options.getAtBBattleChance(AtBLanceRole.DEFENCE));
        spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()]
                .setValue(options.getAtBBattleChance(AtBLanceRole.SCOUTING));
        spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()]
                .setValue(options.getAtBBattleChance(AtBLanceRole.TRAINING));
        btnIntensityUpdate.doClick();
        chkGenerateChases.setSelected(options.isGenerateChases());

        chkUseGenericBattleValue.setSelected(options.isUseGenericBattleValue());
        chkUseVerboseBidding.setSelected(options.isUseVerboseBidding());
        chkDoubleVehicles.setSelected(options.isDoubleVehicles());
        spnOpForLanceTypeMeks.setValue(options.getOpForLanceTypeMeks());
        spnOpForLanceTypeMixed.setValue(options.getOpForLanceTypeMixed());
        spnOpForLanceTypeVehicles.setValue(options.getOpForLanceTypeVehicles());
        chkOpForUsesVTOLs.setSelected(options.isOpForUsesVTOLs());
        chkOpForUsesAero.setSelected(options.isAllowOpForAeros());
        spnOpForAeroChance.setValue(options.getOpForAeroChance());
        chkOpForUsesLocalForces.setSelected(options.isAllowOpForLocalUnits());
        spnOpForLocalForceChance.setValue(options.getOpForLocalUnitChance());
        chkAdjustPlayerVehicles.setSelected(options.isAdjustPlayerVehicles());
        spnFixedMapChance.setValue(options.getFixedMapChance());
        spnSPAUpgradeIntensity.setValue(options.getSpaUpgradeIntensity());
        spnScenarioModMax.setValue(options.getScenarioModMax());
        spnScenarioModChance.setValue(options.getScenarioModChance());
        spnScenarioModBV.setValue(options.getScenarioModBV());
        chkRegionalMekVariations.setSelected(options.isRegionalMekVariations());
        chkAttachedPlayerCamouflage.setSelected(options.isAttachedPlayerCamouflage());
        chkPlayerControlsAttachedUnits.setSelected(options.isPlayerControlsAttachedUnits());
        chkUseDropShips.setSelected(options.isUseDropShips());
        chkUseWeatherConditions.setSelected(options.isUseWeatherConditions());
        chkUseLightConditions.setSelected(options.isUseLightConditions());
        chkUsePlanetaryConditions.setSelected(options.isUsePlanetaryConditions());
        // endregion Against the Bot Tab
    }

    public void updateOptions() {
        try {
            campaign.setName(txtName.getText());
            if (isStartup()) {
                getCampaign().getForces().setName(getCampaign().getName());
            }
            campaign.setLocalDate(date);

            if ((campaign.getCampaignStartDate() == null)
                    || (campaign.getCampaignStartDate().isAfter(campaign.getLocalDate()))) {
                campaign.setCampaignStartDate(date);
            }
            // Ensure that the MegaMek year GameOption matches the campaign year
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR)
                    .setValue(campaign.getGameYear());
            // Null state handled during validation
            campaign.setFaction(comboFaction.getSelectedItem().getFaction());
            RandomNameGenerator.getInstance().setChosenFaction(comboFactionNames.getSelectedItem());
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
            campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)
                    .setValue(useQuirksBox.isSelected());
            options.setUnitRatingMethod(unitRatingMethodCombo.getSelectedItem());
            options.setManualUnitRatingModifier((Integer) manualUnitRatingModifier.getValue());
            options.setUseOriginFactionForNames(chkUseOriginFactionForNames.isSelected());
            options.setDestroyByMargin(useDamageMargin.isSelected());
            options.setDestroyMargin((Integer) spnDamageMargin.getValue());
            options.setDestroyPartTarget((Integer) spnDestroyPartTarget.getValue());
            options.setUseAeroSystemHits(useAeroSystemHitsBox.isSelected());
            options.setCheckMaintenance(checkMaintenance.isSelected());
            options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
            options.setReverseQualityNames(reverseQualityNames.isSelected());
            options.setUseRandomUnitQualities(chkUseRandomUnitQualities.isSelected());
            options.setUsePlanetaryModifiers(chkUsePlanetaryModifiers.isSelected());
            options.setUseUnofficialMaintenance(useUnofficialMaintenance.isSelected());
            options.setLogMaintenance(logMaintenance.isSelected());
            options.setMaintenanceBonus((Integer) spnMaintenanceBonus.getValue());
            options.setDefaultMaintenanceTime((Integer) spnDefaultMaintenanceTime.getValue());
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
            options.setUsePercentageMaint(usePercentageMaintenanceBox.isSelected());
            options.setUseInfantryDontCount(useInfantryDoseNotCountBox.isSelected());
            options.setSellUnits(sellUnitsBox.isSelected());
            options.setSellParts(sellPartsBox.isSelected());
            options.setUsePeacetimeCost(usePeacetimeCostBox.isSelected());
            options.setUseExtendedPartsModifier(useExtendedPartsModifierBox.isSelected());
            options.setShowPeacetimeCost(showPeacetimeCostBox.isSelected());
            options.setNewFinancialYearFinancesToCSVExport(
                    newFinancialYearFinancesToCSVExportBox.isSelected());
            options.setFinancialYearDuration(comboFinancialYearDuration.getSelectedItem());
            options.setAssignPortraitOnRoleChange(chkAssignPortraitOnRoleChange.isSelected());

            options.setEquipmentContractBase(btnContractEquipment.isSelected());
            options.setEquipmentContractPercent((Double) spnEquipPercent.getValue());
            options.setDropShipContractPercent((Double) spnDropShipPercent.getValue());
            options.setJumpShipContractPercent((Double) spnJumpShipPercent.getValue());
            options.setWarShipContractPercent((Double) spnWarShipPercent.getValue());
            options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
            options.setBLCSaleValue(chkBLCSaleValue.isSelected());
            options.setOverageRepaymentInFinalPayment(chkOverageRepaymentInFinalPayment.isSelected());

            options.setWaitingPeriod((Integer) spnAcquireWaitingPeriod.getValue());
            options.setAcquisitionSkill(choiceAcquireSkill.getSelectedItem());
            options.setAcquisitionSupportStaffOnly(chkSupportStaffOnly.isSelected());
            options.setClanAcquisitionPenalty((Integer) spnAcquireClanPenalty.getValue());
            options.setIsAcquisitionPenalty((Integer) spnAcquireIsPenalty.getValue());
            options.setMaxAcquisitions((Integer) spnMaxAcquisitions.getValue());

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
            options.setDisallowPlanetAcquisitionClanCrossover(
                    disallowPlanetaryAcquisitionClanCrossover.isSelected());
            options.setMaxJumpsPlanetaryAcquisition((int) spnMaxJumpPlanetaryAcquisitions.getValue());
            options.setPenaltyClanPartsFromIS((int) spnPenaltyClanPartsFromIS.getValue());
            options.setPlanetAcquisitionFactionLimit(
                    comboPlanetaryAcquisitionsFactionLimits.getSelectedItem());

            for (int i = ITechnology.RATING_A; i <= ITechnology.RATING_F; i++) {
                options.setPlanetTechAcquisitionBonus((int) spnPlanetAcquireTechBonus[i].getValue(), i);
                options.setPlanetIndustryAcquisitionBonus(
                        (int) spnPlanetAcquireIndustryBonus[i].getValue(), i);
                options.setPlanetOutputAcquisitionBonus((int) spnPlanetAcquireOutputBonus[i].getValue(),
                        i);

            }

            options.setXpCostMultiplier((Double) spnXpCostMultiplier.getValue());
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
            options.setMissionXpFail((Integer) spnMissionXpFail.getValue());
            options.setMissionXpSuccess((Integer) spnMissionXpSuccess.getValue());
            options.setMissionXpOutstandingSuccess((Integer) spnMissionXpOutstandingSuccess.getValue());

            options.setLimitByYear(limitByYearBox.isSelected());
            options.setDisallowExtinctStuff(disallowExtinctStuffBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_SHOW_EXTINCT)
                    .setValue(!disallowExtinctStuffBox.isSelected());
            options.setAllowClanPurchases(allowClanPurchasesBox.isSelected());
            options.setAllowISPurchases(allowISPurchasesBox.isSelected());
            options.setAllowCanonOnly(allowCanonOnlyBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_CANON_ONLY)
                    .setValue(allowCanonOnlyBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_ERA_BASED)
                    .setValue(variableTechLevelBox.isSelected());
            options.setVariableTechLevel(variableTechLevelBox.isSelected() && options.isLimitByYear());
            options.setFactionIntroDate(factionIntroDateBox.isSelected());
            campaign.updateTechFactionCode();
            options.setAllowCanonRefitOnly(allowCanonRefitOnlyBox.isSelected());
            options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
            options.setTechLevel(choiceTechLevel.getSelectedIndex());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_TECHLEVEL)
                    .setValue(choiceTechLevel.getSelectedItem());

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
            rSkillPrefs.setSpecialAbilityBonus(SkillType.EXP_GREEN, (Integer) spnAbilityGreen.getValue());
            rSkillPrefs.setSpecialAbilityBonus(SkillType.EXP_REGULAR, (Integer) spnAbilityReg.getValue());
            rSkillPrefs.setSpecialAbilityBonus(SkillType.EXP_VETERAN, (Integer) spnAbilityVet.getValue());
            rSkillPrefs.setSpecialAbilityBonus(SkillType.EXP_ELITE, (Integer) spnAbilityElite.getValue());
            campaign.setRandomSkillPreferences(rSkillPrefs);

            for (int i = 0; i < phenotypeSpinners.length; i++) {
                options.setPhenotypeProbability(i, (Integer) phenotypeSpinners[i].getValue());
            }

            // region Personnel Tab
            // General Personnel
            options.setUseTactics(chkUseTactics.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_COMMAND_INIT)
                    .setValue(chkUseTactics.isSelected());
            options.setUseInitiativeBonus(chkUseInitiativeBonus.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)
                    .setValue(chkUseInitiativeBonus.isSelected());
            options.setUseToughness(chkUseToughness.isSelected());
            options.setUseRandomToughness(chkUseRandomToughness.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_TOUGHNESS)
                    .setValue(chkUseToughness.isSelected());
            options.setUseArtillery(chkUseArtillery.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_ARTILLERY_SKILL)
                    .setValue(chkUseArtillery.isSelected());
            options.setUseAbilities(chkUseAbilities.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_PILOT_ADVANTAGES)
                    .setValue(chkUseAbilities.isSelected());
            options.setUseEdge(chkUseEdge.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.EDGE).setValue(chkUseEdge.isSelected());
            options.setUseSupportEdge(chkUseEdge.isSelected() && chkUseSupportEdge.isSelected());
            options.setUseImplants(chkUseImplants.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_MANEI_DOMINI)
                    .setValue(chkUseImplants.isSelected());
            options.setAlternativeQualityAveraging(chkUseAlternativeQualityAveraging.isSelected());
            options.setUseTransfers(chkUseTransfers.isSelected());
            options.setUseExtendedTOEForceName(chkUseExtendedTOEForceName.isSelected());
            options.setPersonnelLogSkillGain(chkPersonnelLogSkillGain.isSelected());
            options.setPersonnelLogAbilityGain(chkPersonnelLogAbilityGain.isSelected());
            options.setPersonnelLogEdgeGain(chkPersonnelLogEdgeGain.isSelected());
            options.setDisplayPersonnelLog(chkDisplayPersonnelLog.isSelected());
            options.setDisplayScenarioLog(chkDisplayScenarioLog.isSelected());
            options.setDisplayKillRecord(chkDisplayKillRecord.isSelected());

            // Expanded Personnel Information
            options.setUseTimeInService(chkUseTimeInService.isSelected());
            options.setTimeInServiceDisplayFormat(comboTimeInServiceDisplayFormat.getSelectedItem());
            options.setUseTimeInRank(chkUseTimeInRank.isSelected());
            options.setTimeInRankDisplayFormat(comboTimeInRankDisplayFormat.getSelectedItem());
            options.setTrackTotalEarnings(chkTrackTotalEarnings.isSelected());
            options.setTrackTotalXPEarnings(chkTrackTotalXPEarnings.isSelected());
            options.setShowOriginFaction(chkShowOriginFaction.isSelected());

            // Administrators
            options.setAdminsHaveNegotiation(chkAdminsHaveNegotiation.isSelected());
            options.setAdminExperienceLevelIncludeNegotiation(
                    chkAdminExperienceLevelIncludeNegotiation.isSelected());
            options.setAdminsHaveScrounge(chkAdminsHaveScrounge.isSelected());
            options.setAdminExperienceLevelIncludeScrounge(
                    chkAdminExperienceLevelIncludeScrounge.isSelected());

            // autoAwards
            options.setEnableAutoAwards(chkEnableAutoAwards.isSelected());
            options.setAwardBonusStyle(comboAwardBonusStyle.getSelectedItem());
            options.setIssuePosthumousAwards(chkIssuePosthumousAwards.isSelected());
            options.setIssueBestAwardOnly(chkIssueBestAwardOnly.isSelected());
            options.setIgnoreStandardSet(chkIgnoreStandardSet.isSelected());
            options.setAwardTierSize((int) spnAwardTierSize.getValue());
            options.setEnableContractAwards(chkEnableContractAwards.isSelected());
            options.setEnableFactionHunterAwards(chkEnableFactionHunterAwards.isSelected());
            options.setEnableInjuryAwards(chkEnableInjuryAwards.isSelected());
            options.setEnableIndividualKillAwards(chkEnableIndividualKillAwards.isSelected());
            options.setEnableFormationKillAwards(chkEnableFormationKillAwards.isSelected());
            options.setEnableRankAwards(chkEnableRankAwards.isSelected());
            options.setEnableScenarioAwards(chkEnableScenarioAwards.isSelected());
            options.setEnableSkillAwards(chkEnableSkillAwards.isSelected());
            options.setEnableTheatreOfWarAwards(chkEnableTheatreOfWarAwards.isSelected());
            options.setEnableTimeAwards(chkEnableTimeAwards.isSelected());
            options.setEnableTimeAwards(chkEnableTrainingAwards.isSelected());
            options.setEnableMiscAwards(chkEnableMiscAwards.isSelected());
            options.setAwardSetFilterList(txtAwardSetFilterList.getText());

            // Medical
            options.setUseAdvancedMedical(chkUseAdvancedMedical.isSelected());
            // we need to reset healing time options through the campaign because we may
            // need to
            // loop through personnel to make adjustments
            campaign.setHealingTimeOptions((Integer) spnHealWaitingPeriod.getValue(),
                    (Integer) spnNaturalHealWaitingPeriod.getValue());
            options.setMinimumHitsForVehicles((Integer) spnMinimumHitsForVehicles.getValue());
            options.setUseRandomHitsForVehicles(chkUseRandomHitsForVehicles.isSelected());
            options.setTougherHealing(chkUseTougherHealing.isSelected());
            options.setMaximumPatients((int) spnMaximumPatients.getValue());

            // Prisoners
            options.setPrisonerCaptureStyle(comboPrisonerCaptureStyle.getSelectedItem());
            options.setDefaultPrisonerStatus(comboPrisonerStatus.getSelectedItem());
            options.setPrisonerBabyStatus(chkPrisonerBabyStatus.isSelected());
            options.setUseAtBPrisonerDefection(chkAtBPrisonerDefection.isSelected());
            options.setUseAtBPrisonerRansom(chkAtBPrisonerRansom.isSelected());

            // Dependent
            options.setUseRandomDependentAddition(chkUseRandomDependentAddition.isSelected());
            options.setUseRandomDependentRemoval(chkUseRandomDependentRemoval.isSelected());

            // Personnel Removal
            options.setUsePersonnelRemoval(chkUsePersonnelRemoval.isSelected());
            options.setUseRemovalExemptCemetery(chkUseRemovalExemptCemetery.isSelected());
            options.setUseRemovalExemptRetirees(chkUseRemovalExemptRetirees.isSelected());

            // Salary
            options.setDisableSecondaryRoleSalary(chkDisableSecondaryRoleSalary.isSelected());
            options.setSalaryAntiMekMultiplier((Double) spnAntiMekSalary.getValue());
            options.setSalarySpecialistInfantryMultiplier((Double) spnSpecialistInfantrySalary.getValue());
            for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
                options.getSalaryXPMultipliers().put(entry.getKey(),
                        (Double) entry.getValue().getValue());
            }

            for (final PersonnelRole personnelRole : PersonnelRole.values()) {
                options.setRoleBaseSalary(personnelRole,
                        (double) spnBaseSalary[personnelRole.ordinal()].getValue());
            }
            // endregion Personnel Tab

            // region Turnover and Retention
            // Header
            options.setUseRandomRetirement(chkUseRandomRetirement.isSelected());

            // Settings
            options.setTurnoverFixedTargetNumber((Integer) spnTurnoverFixedTargetNumber.getValue());
            options.setTurnoverFrequency(comboTurnoverFrequency.getSelectedItem());
            options.setUseContractCompletionRandomRetirement(
                    chkUseContractCompletionRandomRetirement.isSelected());
            options.setUseRandomFounderTurnover(chkUseRandomFounderTurnover.isSelected());
            options.setUseFounderRetirement(chkUseFounderRetirement.isSelected());
            options.setAeroRecruitsHaveUnits(chkAeroRecruitsHaveUnits.isSelected());
            options.setUseSubContractSoldiers(chkUseSubContractSoldiers.isSelected());
            options.setServiceContractDuration((Integer) spnServiceContractDuration.getValue());
            options.setServiceContractModifier((Integer) spnServiceContractModifier.getValue());
            options.setPayBonusDefault(chkPayBonusDefault.isSelected());
            options.setPayBonusDefaultThreshold((Integer) spnPayBonusDefaultThreshold.getValue());

            // Modifiers
            options.setUseCustomRetirementModifiers(chkUseCustomRetirementModifiers.isSelected());
            options.setUseFatigueModifiers(chkUseFatigueModifiers.isSelected());
            options.setUseSkillModifiers(chkUseSkillModifiers.isSelected());
            options.setUseAgeModifiers(chkUseAgeModifiers.isSelected());
            options.setUseUnitRatingModifiers(chkUseUnitRatingModifiers.isSelected());
            options.setUseFactionModifiers(chkUseFactionModifiers.isSelected());
            options.setUseHostileTerritoryModifiers(chkUseHostileTerritoryModifiers.isSelected());
            options.setUseMissionStatusModifiers(chkUseMissionStatusModifiers.isSelected());
            options.setUseFamilyModifiers(chkUseFamilyModifiers.isSelected());
            options.setUseLoyaltyModifiers(chkUseLoyaltyModifiers.isSelected());
            options.setUseHideLoyalty(chkUseHideLoyalty.isSelected());

            // Payouts
            options.setPayoutRateOfficer((Integer) spnPayoutRateOfficer.getValue());
            options.setPayoutRateEnlisted((Integer) spnPayoutRateEnlisted.getValue());
            options.setPayoutRetirementMultiplier((Integer) spnPayoutRetirementMultiplier.getValue());
            options.setUsePayoutServiceBonus(chkUsePayoutServiceBonus.isSelected());
            options.setPayoutServiceBonusRate((Integer) spnPayoutServiceBonusRate.getValue());

            // Unit Cohesion
            options.setUseAdministrativeStrain(chkUseAdministrativeStrain.isSelected());
            options.setUseManagementSkill(chkUseManagementSkill.isSelected());

            options.setAdministrativeCapacity((Integer) spnAdministrativeCapacity.getValue());
            options.setMultiCrewStrainDivider((Integer) spnMultiCrewStrainDivider.getValue());

            options.setUseCommanderLeadershipOnly(chkUseCommanderLeadershipOnly.isSelected());
            options.setManagementSkillPenalty((Integer) spnManagementSkillPenalty.getValue());

            // Fatigue
            options.setUseFatigue(chkUseFatigue.isSelected());
            options.setFatigueRate((Integer) spnFatigueRate.getValue());
            options.setUseInjuryFatigue(chkUseInjuryFatigue.isSelected());
            options.setFieldKitchenCapacity((Integer) spnFieldKitchenCapacity.getValue());
            options.setFieldKitchenIgnoreNonCombatants(chkFieldKitchenIgnoreNonCombatants.isSelected());
            options.setFatigueLeaveThreshold((Integer) spnFatigueLeaveThreshold.getValue());
            // endregion Turnover and Retention

            // region Life Paths Tab
            // Personnel Randomization
            options.setUseDylansRandomXP(chkUseDylansRandomXP.isSelected());
            options.setNonBinaryDiceSize((int) spnNonBinaryDiceSize.getValue());

            // Random Histories
            options.setRandomOriginOptions(randomOriginOptionsPanel.createOptionsFromPanel());
            options.setUseRandomPersonalities(chkUseRandomPersonalities.isSelected());
            options.setUseSimulatedRelationships(chkUseSimulatedRelationships.isSelected());
            options.setUseRandomPersonalityReputation(chkUseRandomPersonalityReputation.isSelected());
            options.setUseIntelligenceXpMultiplier(chkUseIntelligenceXpMultiplier.isSelected());

            // Family
            options.setFamilyDisplayLevel(comboFamilyDisplayLevel.getSelectedItem());

            // Anniversaries
            options.setAnnounceBirthdays(chkAnnounceBirthdays.isSelected());
            options.setAnnounceRecruitmentAnniversaries(chkAnnounceRecruitmentAnniversaries.isSelected());
            options.setAnnounceOfficersOnly(chkAnnounceOfficersOnly.isSelected());
            options.setAnnounceChildBirthdays(chkAnnounceChildBirthdays.isSelected());

            // Marriage
            options.setUseManualMarriages(chkUseManualMarriages.isSelected());
            options.setUseClanPersonnelMarriages(chkUseClanPersonnelMarriages.isSelected());
            options.setUsePrisonerMarriages(chkUsePrisonerMarriages.isSelected());
            options.setNoInterestInMarriageDiceSize((int) spnNoInterestInMarriageDiceSize.getValue());
            options.setCheckMutualAncestorsDepth((Integer) spnCheckMutualAncestorsDepth.getValue());
            options.setLogMarriageNameChanges(chkLogMarriageNameChanges.isSelected());
            for (final Entry<MergingSurnameStyle, JSpinner> entry : spnMarriageSurnameWeights.entrySet()) {
                options.getMarriageSurnameWeights().put(entry.getKey(),
                        (int) Math.round((Double) entry.getValue().getValue() * 10.0));
            }
            options.setRandomMarriageMethod(comboRandomMarriageMethod.getSelectedItem());
            options.setUseRandomClanPersonnelMarriages(chkUseRandomClanPersonnelMarriages.isSelected());
            options.setUseRandomPrisonerMarriages(chkUseRandomPrisonerMarriages.isSelected());
            options.setRandomMarriageAgeRange((Integer) spnRandomMarriageAgeRange.getValue());
            options.setRandomMarriageDiceSize((int) spnRandomMarriageDiceSize.getValue());
            options.setRandomSameSexMarriageDiceSize((int) spnRandomSameSexMarriageDiceSize.getValue());
            options.setRandomNewDependentMarriage((int) spnRandomNewDependentMarriage.getValue());

            // Divorce
            options.setUseManualDivorce(chkUseManualDivorce.isSelected());
            options.setUseClanPersonnelDivorce(chkUseClanPersonnelDivorce.isSelected());
            options.setUsePrisonerDivorce(chkUsePrisonerDivorce.isSelected());
            for (final Entry<SplittingSurnameStyle, JSpinner> entry : spnDivorceSurnameWeights.entrySet()) {
                options.getDivorceSurnameWeights().put(entry.getKey(),
                        (int) Math.round((Double) entry.getValue().getValue() * 10.0));
            }
            options.setRandomDivorceMethod(comboRandomDivorceMethod.getSelectedItem());
            options.setUseRandomOppositeSexDivorce(chkUseRandomOppositeSexDivorce.isSelected());
            options.setUseRandomSameSexDivorce(chkUseRandomSameSexDivorce.isSelected());
            options.setUseRandomClanPersonnelDivorce(chkUseRandomClanPersonnelDivorce.isSelected());
            options.setUseRandomPrisonerDivorce(chkUseRandomPrisonerDivorce.isSelected());
            options.setRandomDivorceDiceSize((int) spnRandomDivorceDiceSize.getValue());

            // Procreation
            options.setUseManualProcreation(chkUseManualProcreation.isSelected());
            options.setUseClanPersonnelProcreation(chkUseClanPersonnelProcreation.isSelected());
            options.setUsePrisonerProcreation(chkUsePrisonerProcreation.isSelected());
            options.setMultiplePregnancyOccurrences((Integer) spnMultiplePregnancyOccurrences.getValue());
            options.setBabySurnameStyle(comboBabySurnameStyle.getSelectedItem());
            options.setAssignNonPrisonerBabiesFounderTag(chkAssignNonPrisonerBabiesFounderTag.isSelected());
            options.setAssignChildrenOfFoundersFounderTag(
                    chkAssignChildrenOfFoundersFounderTag.isSelected());
            options.setDetermineFatherAtBirth(chkDetermineFatherAtBirth.isSelected());
            options.setDisplayTrueDueDate(chkDisplayTrueDueDate.isSelected());
            options.setNoInterestInChildrenDiceSize((int) spnNoInterestInChildrenDiceSize.getValue());
            options.setUseMaternityLeave(chkUseMaternityLeave.isSelected());
            options.setLogProcreation(chkLogProcreation.isSelected());
            options.setRandomProcreationMethod(comboRandomProcreationMethod.getSelectedItem());
            options.setUseRelationshiplessRandomProcreation(
                    chkUseRelationshiplessRandomProcreation.isSelected());
            options.setUseRandomClanPersonnelProcreation(chkUseRandomClanPersonnelProcreation.isSelected());
            options.setUseRandomPrisonerProcreation(chkUseRandomPrisonerProcreation.isSelected());
            options.setRandomProcreationRelationshipDiceSize((Integer) spnRandomProcreationRelationshipDiceSize.getValue());
            options.setRandomProcreationRelationshiplessDiceSize((Integer) spnRandomProcreationRelationshiplessDiceSize.getValue());

            // Education
            options.setUseEducationModule(chkUseEducationModule.isSelected());
            options.setCurriculumXpRate((Integer) spnCurriculumXpRate.getValue());
            options.setMaximumJumpCount((Integer) spnMaximumJumpCount.getValue());
            options.setUseReeducationCamps(chkUseReeducationCamps.isSelected());
            options.setEnableLocalAcademies(chkEnableLocalAcademies.isSelected());
            options.setEnablePrestigiousAcademies(chkEnablePrestigiousAcademies.isSelected());
            options.setEnableUnitEducation(chkEnableUnitEducation.isSelected());
            options.setEnableOverrideRequirements(chkEnableOverrideRequirements.isSelected());
            options.setEnableShowIneligibleAcademies(chkShowIneligibleAcademies.isSelected());
            options.setEntranceExamBaseTargetNumber((int) spnEntranceExamBaseTargetNumber.getValue());
            options.setFacultyXpRate((Double) spnFacultyXpMultiplier.getValue());
            options.setEnableBonuses(chkEnableBonuses.isSelected());
            options.setAdultDropoutChance((Integer) spnAdultDropoutChance.getValue());
            options.setChildrenDropoutChance((Integer) spnChildrenDropoutChance.getValue());
            options.setAllAges(chkAllAges.isSelected());
            options.setMilitaryAcademyAccidents((Integer) spnMilitaryAcademyAccidents.getValue());

            // Death
            options.setKeepMarriedNameUponSpouseDeath(chkKeepMarriedNameUponSpouseDeath.isSelected());
            options.setRandomDeathMethod(comboRandomDeathMethod.getSelectedItem());
            for (final AgeGroup ageGroup : AgeGroup.values()) {
                options.getEnabledRandomDeathAgeGroups().put(ageGroup,
                        chkEnabledRandomDeathAgeGroups.get(ageGroup).isSelected());
            }
            options.setUseRandomClanPersonnelDeath(chkUseRandomClanPersonnelDeath.isSelected());
            options.setUseRandomPrisonerDeath(chkUseRandomPrisonerDeath.isSelected());
            options.setUseRandomDeathSuicideCause(chkUseRandomDeathSuicideCause.isSelected());
            options.setPercentageRandomDeathChance((Double) spnPercentageRandomDeathChance.getValue());
            for (int i = 0; i < spnExponentialRandomDeathMaleValues.length; i++) {
                options.getExponentialRandomDeathMaleValues()[i] = (double) spnExponentialRandomDeathMaleValues[i]
                        .getValue();
                options.getExponentialRandomDeathFemaleValues()[i] = (double) spnExponentialRandomDeathFemaleValues[i]
                        .getValue();
            }
            for (final TenYearAgeRange ageRange : TenYearAgeRange.values()) {
                options.getAgeRangeRandomDeathMaleValues().put(ageRange,
                        (double) spnAgeRangeRandomDeathMaleValues.get(ageRange).getValue());
                options.getAgeRangeRandomDeathFemaleValues().put(ageRange,
                        (double) spnAgeRangeRandomDeathFemaleValues.get(ageRange).getValue());
            }
            // endregion Life Paths Tab

            // region Finances Tab
            // Price Multipliers
            options.setCommonPartPriceMultiplier((Double) spnCommonPartPriceMultiplier.getValue());
            options.setInnerSphereUnitPriceMultiplier(
                    (Double) spnInnerSphereUnitPriceMultiplier.getValue());
            options.setInnerSpherePartPriceMultiplier(
                    (Double) spnInnerSpherePartPriceMultiplier.getValue());
            options.setClanUnitPriceMultiplier((Double) spnClanUnitPriceMultiplier.getValue());
            options.setClanPartPriceMultiplier((Double) spnClanPartPriceMultiplier.getValue());
            options.setMixedTechUnitPriceMultiplier((Double) spnMixedTechUnitPriceMultiplier.getValue());
            for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
                options.getUsedPartPriceMultipliers()[i] = (Double) spnUsedPartPriceMultipliers[i]
                        .getValue();
            }
            options.setDamagedPartsValueMultiplier((Double) spnDamagedPartsValueMultiplier.getValue());
            options.setUnrepairablePartsValueMultiplier(
                    (Double) spnUnrepairablePartsValueMultiplier.getValue());
            options.setCancelledOrderRefundMultiplier(
                    (Double) spnCancelledOrderRefundMultiplier.getValue());

            options.setUseShareSystem(chkUseShareSystem.isSelected());
            options.setSharesForAll(chkSharesForAll.isSelected());

            // region Taxes
            options.setUseTaxes(chkUseTaxes.isSelected());
            options.setTaxesPercentage((Integer) spnTaxesPercentage.getValue());
            // endregion Taxes
            // endregion Finances Tab

            // start SPA
            SpecialAbility.replaceSpecialAbilities(getCurrentSPA());
            // end SPA

            // region Markets Tab
            // Personnel Market
            options.setPersonnelMarketName(comboPersonnelMarketType.getSelectedItem());
            if (comboPersonnelMarketType.getSelectedItem().equals("Campaign Ops")) {
                campaign.getPersonnelMarket().setPaidRecruitment(false);
            }
            options.setPersonnelMarketReportRefresh(chkPersonnelMarketReportRefresh.isSelected());
            for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets
                    .entrySet()) {
                options.getPersonnelMarketRandomRemovalTargets().put(entry.getKey(),
                        (int) entry.getValue().getValue());
            }
            options.setPersonnelMarketDylansWeight((Double) spnPersonnelMarketDylansWeight.getValue());
            options.setUsePersonnelHireHiringHallOnly(chkUsePersonnelHireHiringHallOnly.isSelected());

            // Unit Market
            options.setUnitMarketMethod(comboUnitMarketMethod.getSelectedItem());
            options.setUnitMarketRegionalMekVariations(chkUnitMarketRegionalMekVariations.isSelected());
            options.setUnitMarketSpecialUnitChance((Integer) spnUnitMarketSpecialUnitChance.getValue());
            options.setUnitMarketRarityModifier((int) spnUnitMarketRarityModifier.getValue());
            options.setInstantUnitMarketDelivery(chkInstantUnitMarketDelivery.isSelected());
            options.setUnitMarketReportRefresh(chkUnitMarketReportRefresh.isSelected());

            // Contract Market
            options.setContractMarketMethod(comboContractMarketMethod.getSelectedItem());
            options.setContractSearchRadius((Integer) spnContractSearchRadius.getValue());
            options.setVariableContractLength(chkVariableContractLength.isSelected());
            options.setContractMarketReportRefresh(chkContractMarketReportRefresh.isSelected());
            options.setContractMaxSalvagePercentage((Integer) spnContractMaxSalvagePercentage.getValue());
            options.setDropShipBonusPercentage((int) spnDropShipBonusPercentage.getValue());
            // endregion Markets Tab

            // region RATs Tab
            options.setUseStaticRATs(btnUseStaticRATs.isSelected());
            // We use a stream to strip dates used in display name
            options.setRATs(IntStream.range(0, chosenRATModel.size())
                    .mapToObj(i -> chosenRATModel.elementAt(i).replaceFirst(" \\(.*?\\)", ""))
                    .toArray(String[]::new));
            options.setIgnoreRATEra(chkIgnoreRATEra.isSelected());
            // endregion RATs Tab

            // region Against the Bot
            options.setUseAtB(chkUseAtB.isSelected());
            options.setUseStratCon(chkUseStratCon.isSelected());
            options.setSkillLevel(comboSkillLevel.getSelectedItem());
            options.setLimitLanceWeight(chkLimitLanceWeight.isSelected());
            options.setLimitLanceNumUnits(chkLimitLanceNumUnits.isSelected());
            options.setUseStrategy(chkUseStrategy.isSelected());
            options.setBaseStrategyDeployment((Integer) spnBaseStrategyDeployment.getValue());
            options.setAdditionalStrategyDeployment((Integer) spnAdditionalStrategyDeployment.getValue());
            options.setAdjustPaymentForStrategy(chkAdjustPaymentForStrategy.isSelected());

            options.setUseAero(chkUseAero.isSelected());
            options.setUseVehicles(chkUseVehicles.isSelected());
            options.setClanVehicles(chkClanVehicles.isSelected());
            options.setAutoConfigMunitions(chkAutoConfigMunitions.isSelected());
            options.setUseGenericBattleValue(chkUseGenericBattleValue.isSelected());
            options.setUseVerboseBidding(chkUseVerboseBidding.isSelected());
            options.setDoubleVehicles(chkDoubleVehicles.isSelected());
            options.setAdjustPlayerVehicles(chkAdjustPlayerVehicles.isSelected());
            options.setOpForLanceTypeMeks((Integer) spnOpForLanceTypeMeks.getValue());
            options.setOpForLanceTypeMixed((Integer) spnOpForLanceTypeMixed.getValue());
            options.setOpForLanceTypeVehicles((Integer) spnOpForLanceTypeVehicles.getValue());
            options.setOpForUsesVTOLs(chkOpForUsesVTOLs.isSelected());
            options.setAllowOpForAeros(chkOpForUsesAero.isSelected());
            options.setAllowOpForLocalUnits(chkOpForUsesLocalForces.isSelected());
            options.setOpForAeroChance((Integer) spnOpForAeroChance.getValue());
            options.setOpForLocalUnitChance((Integer) spnOpForLocalForceChance.getValue());
            options.setFixedMapChance((Integer) spnFixedMapChance.getValue());
            options.setSpaUpgradeIntensity((Integer) spnSPAUpgradeIntensity.getValue());
            options.setScenarioModMax((Integer) spnScenarioModMax.getValue());
            options.setScenarioModChance((Integer) spnScenarioModChance.getValue());
            options.setScenarioModBV((Integer) spnScenarioModBV.getValue());
            options.setUseDropShips(chkUseDropShips.isSelected());

            for (int i = 0; i < spnAtBBattleChance.length; i++) {
                options.setAtBBattleChance(i, (Integer) spnAtBBattleChance[i].getValue());
            }
            options.setGenerateChases(chkGenerateChases.isSelected());
            options.setMercSizeLimited(chkMercSizeLimited.isSelected());
            options.setRestrictPartsByMission(chkRestrictPartsByMission.isSelected());
            options.setBonusPartExchangeValue((Integer) spnBonusPartExchangeValue.getValue());
            options.setBonusPartMaxExchangeCount((Integer) spnBonusPartMaxExchangeCount.getValue());
            options.setRegionalMekVariations(chkRegionalMekVariations.isSelected());
            options.setAttachedPlayerCamouflage(chkAttachedPlayerCamouflage.isSelected());
            options.setPlayerControlsAttachedUnits(chkPlayerControlsAttachedUnits.isSelected());
            options.setUseWeatherConditions(chkUseWeatherConditions.isSelected());
            options.setUseLightConditions(chkUseLightConditions.isSelected());
            options.setUsePlanetaryConditions(chkUsePlanetaryConditions.isSelected());
            // endregion Against the Bot

            campaign.setCampaignOptions(options);

            MekHQ.triggerEvent(new OptionsChangedEvent(campaign, options));
        } catch (Exception ex) {
            logger.error(ex, "Campaign Options update failure, please check the logs for the exception reason.",
                    "Error Updating Campaign Options");
        }
    }

    /**
     * Validates the data contained in this panel, returning the current state of
     * validation.
     *
     * @param display to display dialogs containing the messages or not
     * @return ValidationState.SUCCESS if the data validates successfully,
     *         ValidationState.WARNING
     *         if a warning was issued, or ValidationState.FAILURE if validation
     *         fails
     */
    public ValidationState validateOptions(final boolean display) {
        // region Errors
        // Name Validation
        if (txtName.getText().isBlank()) {
            if (display) {
                JOptionPane.showMessageDialog(getFrame(),
                        resources.getString("CampaignOptionsPane.EmptyCampaignName.text"),
                        resources.getString("InvalidOptions.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return ValidationState.FAILURE;
        }

        // Faction Validation
        final FactionDisplay factionDisplay = comboFaction.getSelectedItem();
        if (factionDisplay == null) {
            if (display) {
                JOptionPane.showMessageDialog(getFrame(),
                        resources.getString("CampaignOptionsPane.NullFactionSelected.text"),
                        resources.getString("InvalidOptions.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return ValidationState.FAILURE;
        } else if (StringUtility.isNullOrBlank(factionDisplay.getFaction().getShortName())) {
            if (display) {
                JOptionPane.showMessageDialog(getFrame(),
                        String.format(resources.getString(
                                "CampaignOptionsPane.FactionWithoutFactionCodeSelected.text"),
                                factionDisplay),
                        resources.getString("InvalidOptions.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return ValidationState.FAILURE;
        }
        // endregion Errors

        // The options specified are correct, and thus can be saved
        return ValidationState.SUCCESS;
    }
    // endregion Options

    // region Unsorted Or Legacy Methods
    // Define what is legacy and not and remove in Milestone after 0.49.19
    public void applyPreset(final @Nullable CampaignPreset preset) {
        if (preset == null) {
            return;
        }

        if (isStartup()) {
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
            tableXP.setModel(new DefaultTableModel(getSkillCostsArray(preset.getSkills()),
                    TABLE_XP_COLUMN_NAMES));
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

    public static String[][] getSkillCostsArray(Map<String, SkillType> skillHash) {
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

    private void updateXPCosts() {
        for (int i = 0; i < SkillType.skillList.length; i++) {
            for (int j = 0; j < 11; j++) {
                try {
                    int cost = Integer.parseInt((String) tableXP.getValueAt(i, j));
                    SkillType.setCost(SkillType.skillList[i], cost, j);
                } catch (Exception ex) {
                    String message = String.format("unreadable value in skill cost table for {}",
                            SkillType.skillList[i]);
                    logger.error(ex, message);
                }
            }
        }
    }

    private void updateSkillTypes() {
        for (final String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (hashSkillTargets.get(skillName) != null) {
                type.setTarget((Integer) hashSkillTargets.get(skillName).getValue());
            }

            if (hashGreenSkill.get(skillName) != null) {
                type.setGreenLevel((Integer) hashGreenSkill.get(skillName).getValue());
            }

            if (hashRegSkill.get(skillName) != null) {
                type.setRegularLevel((Integer) hashRegSkill.get(skillName).getValue());
            }

            if (hashVetSkill.get(skillName) != null) {
                type.setVeteranLevel((Integer) hashVetSkill.get(skillName).getValue());
            }

            if (hashEliteSkill.get(skillName) != null) {
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
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));

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

        for (final String key : SpecialAbility.getDefaultSpecialAbilities().keySet()) {
            if ((getCurrentSPA().get(key) == null) && !unused.contains(key)) {
                unused.add(key);
            }
        }

        return unused;
    }

    public Map<String, SpecialAbility> getCurrentSPA() {
        return tempSPA;
    }

    private void btnAddSPA() {
        SelectUnusedAbilityDialog suad = new SelectUnusedAbilityDialog(getFrame(), getUnusedSPA(),
                getCurrentSPA());
        suad.setVisible(true);

        recreateSPAPanel(!getUnusedSPA().isEmpty());
    }

    public void btnRemoveSPA(String name) {
        getCurrentSPA().remove(name);

        // we also need to cycle through the existing SPAs and remove this one from any
        // prereqs
        for (final String key : getCurrentSPA().keySet()) {
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
        getCurrentSPA().values().stream()
                .sorted((o1, o2) -> naturalOrderComparator.compare(o1.getDisplayName(),
                        o2.getDisplayName()))
                .forEach(spa -> {
                    panSpecialAbilities.add(new SpecialAbilityPanel(spa, this), gridBagConstraints);
                    gridBagConstraints.gridy++;
                });
        panSpecialAbilities.revalidate();
        panSpecialAbilities.repaint();
    }

    /**
     * Recreates the finances panel to reverse the quality labels.
     *
     * @param reverseQualities boolean for if the qualities are reversed.
     */
    private void recreateFinancesPanel(boolean reverseQualities) {
        int financesTabIndex = indexOfTab(resources.getString("financesPanel.title"));
        removeTabAt(financesTabIndex);
        insertTab(resources.getString("financesPanel.title"), null, createFinancesTab(reverseQualities), null,
                financesTabIndex);

        // The following setters and getter calls are so that recreating the tab doesn't
        // reset all
        // options to their default values

        // General finances panel options
        payForPartsBox.setSelected(options.isPayForParts());
        payForRepairsBox.setSelected(options.isPayForRepairs());
        payForUnitsBox.setSelected(options.isPayForUnits());
        payForSalariesBox.setSelected(options.isPayForSalaries());
        payForOverheadBox.setSelected(options.isPayForOverhead());
        payForMaintainBox.setSelected(options.isPayForMaintain());
        payForTransportBox.setSelected(options.isPayForTransport());
        sellUnitsBox.setSelected(options.isSellUnits());
        sellPartsBox.setSelected(options.isSellParts());
        payForRecruitmentBox.setSelected(options.isPayForRecruitment());
        useLoanLimitsBox.setSelected(options.isUseLoanLimits());
        usePercentageMaintenanceBox.setSelected(options.isUsePercentageMaint());
        useInfantryDoseNotCountBox.setSelected(options.isInfantryDontCount());
        usePeacetimeCostBox.setSelected(options.isUsePeacetimeCost());
        useExtendedPartsModifierBox.setSelected(options.isUseExtendedPartsModifier());
        showPeacetimeCostBox.setSelected(options.isShowPeacetimeCost());
        comboFinancialYearDuration.setSelectedItem(options.getFinancialYearDuration());
        newFinancialYearFinancesToCSVExportBox.setSelected(options.isNewFinancialYearFinancesToCSVExport());

        // Taxes
        chkUseTaxes.setSelected(options.isUseTaxes());
        spnTaxesPercentage.setValue(options.getTaxesPercentage());

        // Price Multipliers Panel
        spnCommonPartPriceMultiplier.setValue(options.getCommonPartPriceMultiplier());
        spnInnerSphereUnitPriceMultiplier.setValue(options.getInnerSphereUnitPriceMultiplier());
        spnInnerSpherePartPriceMultiplier.setValue(options.getInnerSpherePartPriceMultiplier());
        spnClanUnitPriceMultiplier.setValue(options.getClanUnitPriceMultiplier());
        spnClanPartPriceMultiplier.setValue(options.getClanPartPriceMultiplier());
        spnMixedTechUnitPriceMultiplier.setValue(options.getMixedTechUnitPriceMultiplier());
        spnDamagedPartsValueMultiplier.setValue(options.getDamagedPartsValueMultiplier());
        spnUnrepairablePartsValueMultiplier.setValue(options.getUnrepairablePartsValueMultiplier());
        spnCancelledOrderRefundMultiplier.setValue(options.getCancelledOrderRefundMultiplier());

        // Used Parts Multiplier Panel
        for (int index = PartQuality.QUALITY_A.toNumeric(); index <= PartQuality.QUALITY_F.toNumeric(); index++) {
            spnUsedPartPriceMultipliers[index].setValue(options.getUsedPartPriceMultipliers()[index]);
        }

        // Shares System
        chkUseShareSystem.setSelected(options.isUseShareSystem());
        chkSharesForAll.setSelected(options.isSharesForAll());
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

        int x = (Integer) spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].getValue();
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

    private class AtBBattleIntensityChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            double intensity = (Double) spnAtBBattleIntensity.getValue();

            if (intensity >= AtBContract.MINIMUM_INTENSITY) {
                int value = (int) Math.min(
                        Math.round(400.0 * intensity / (4.0 * intensity + 6.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(200.0 * intensity / (2.0 * intensity + 8.0) + 0.05),
                        100);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(600.0 * intensity / (6.0 * intensity + 4.0) + 0.05),
                        100);
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
     * http://tips4java.wordpress.com/2008/11/18/row-number-table/
     * Use a JTable as a renderer for row numbers of a given main table.
     * This table must be added to the row header of the scrollpane that
     * contains the main table.
     */
    public static class RowNamesTable extends JTable implements ChangeListener, PropertyChangeListener {
        private final JTable main;

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
            // Keep scrolling of the row table in sync with the main table.
            if (c instanceof JViewport viewport) {
                viewport.addChangeListener(this);
            }
        }

        /*
         * Delegate method to main table
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
         * This table does not use any data from the main TableModel,
         * so return a value based on the row parameter.
         */
        @Override
        public Object getValueAt(int row, int column) {
            return SkillType.skillList[row];
        }

        /*
         * Don't edit data in the main TableModel by mistake
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        //
        // Implement the ChangeListener
        //
        @Override
        public void stateChanged(ChangeEvent e) {
            // Keep the scrolling of the row table in sync with main table
            JViewport viewport = (JViewport) e.getSource();
            JScrollPane scrollPane = (JScrollPane) viewport.getParent();
            scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
        }

        //
        // Implement the PropertyChangeListener
        //
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            // Keep the row table in sync with the main table

            if ("selectionModel".equals(e.getPropertyName())) {
                setSelectionModel(main.getSelectionModel());
            }

            if ("model".equals(e.getPropertyName())) {
                setModel(main.getModel());
            }
        }

        /*
         * Borrow the renderer from JDK1.4.2 table header
         */
        private static class RowNumberRenderer extends DefaultTableCellRenderer {
            public RowNumberRenderer() {
                setHorizontalAlignment(JLabel.LEFT);
            }

            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                    final boolean isSelected,
                    final boolean hasFocus, final int row,
                    final int column) {
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
    // endregion Unsorted Or Legacy Methods
}
