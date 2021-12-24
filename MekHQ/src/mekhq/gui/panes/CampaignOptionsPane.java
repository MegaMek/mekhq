/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.icons.Camouflage;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.market.PersonnelMarketDylan;
import mekhq.campaign.market.PersonnelMarketRandom;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.baseComponents.JDisableablePanel;
import mekhq.gui.dialog.CampaignOptionsDialog;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    //region Variable Declarations
    //region General Variables (ones not relating to a specific tab)
    private final Campaign campaign;
    private final boolean startup;
    private CampaignOptions options;
    private RandomSkillPreferences rSkillPrefs;
    private LocalDate date;
    private Camouflage camouflage;
    private PlayerColour colour;
    private StandardForceIcon unitIcon;
    private Hashtable<String, JSpinner> hashSkillTargets;
    private Hashtable<String, JSpinner> hashGreenSkill;
    private Hashtable<String, JSpinner> hashRegSkill;
    private Hashtable<String, JSpinner> hashVetSkill;
    private Hashtable<String, JSpinner> hashEliteSkill;
    //endregion General Variables (ones not relating to a specific tab)

    //region General Tab
    private JTextField txtName;
    private MMComboBox<FactionDisplay> comboFaction;
    private JComboBox<UnitRatingMethod> unitRatingMethodCombo;
    private JSpinner manualUnitRatingModifier;
    private JButton btnDate;
    private JButton btnCamo;
    private JButton btnIcon;
    //endregion General Tab

    //region Repair and Maintenance Tab
    private JPanel panRepair;
    //repair
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
    private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useDamageMargin;
    private JSpinner spnDamageMargin;
    private JSpinner spnDestroyPartTarget;
    //maintenance
    private JCheckBox checkMaintenance;
    private JSpinner spnMaintenanceDays;
    private JSpinner spnMaintenanceBonus;
    private JCheckBox useQualityMaintenance;
    private JCheckBox reverseQualityNames;
    private JCheckBox useUnofficialMaintenance;
    private JCheckBox logMaintenance;
    //endregion Repair and Maintenance Tab

    //region Supplies and Acquisitions Tab
    private JPanel panSupplies;
    //acquisition
    private JSpinner spnAcquireWaitingPeriod;
    private JComboBox<String> choiceAcquireSkill;
    private JCheckBox chkSupportStaffOnly;
    private JSpinner spnAcquireClanPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JTextField txtMaxAcquisitions;
    //Delivery
    private JSpinner spnNDiceTransitTime;
    private JSpinner spnConstantTransitTime;
    private JComboBox<String> choiceTransitTimeUnits;
    private JSpinner spnAcquireMinimum;
    private JComboBox<String> choiceAcquireMinimumUnit;
    private JSpinner spnAcquireMosBonus;
    private JComboBox<String> choiceAcquireMosUnits;
    //planetary acquisitions
    private JCheckBox usePlanetaryAcquisitions;
    private JSpinner spnMaxJumpPlanetaryAcquisitions;
    private JComboBox<PlanetaryAcquisitionFactionLimit> comboPlanetaryAcquisitionsFactionLimits;
    private JCheckBox disallowPlanetaryAcquisitionClanCrossover;
    private JCheckBox usePlanetaryAcquisitionsVerbose;
    private JSpinner[] spnPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    private JCheckBox disallowClanPartsFromIS;
    private JSpinner spnPenaltyClanPartsFromIS;
    //endregion Supplies and Acquisitions Tab

    //region Tech Limits Tab
    private JPanel panTech;
    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private JComboBox<String> choiceTechLevel;
    private JCheckBox variableTechLevelBox;
    private JCheckBox factionIntroDateBox;
    private JCheckBox useAmmoByTypeBox;
    //endregion Tech Limits Tab

    //region Personnel Tab
    // General Personnel
    private JCheckBox chkUseTactics;
    private JCheckBox chkUseInitiativeBonus;
    private JCheckBox chkUseToughness;
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

    // Expanded Personnel
    private JCheckBox chkUseTimeInService;
    private JComboBox<TimeInDisplayFormat> comboTimeInServiceDisplayFormat;
    private JCheckBox chkUseTimeInRank;
    private JComboBox<TimeInDisplayFormat> comboTimeInRankDisplayFormat;
    private JCheckBox chkUseRetirementDateTracking;
    private JCheckBox chkTrackTotalEarnings;
    private JCheckBox chkTrackTotalXPEarnings;
    private JCheckBox chkShowOriginFaction;

    // Medical
    private JCheckBox chkUseAdvancedMedical;
    private JSpinner spnHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JSpinner spnMinimumHitsForVehicles;
    private JCheckBox chkUseRandomHitsForVehicles;
    private JCheckBox chkUseTougherHealing;

    // Prisoners
    private JComboBox<PrisonerCaptureStyle> comboPrisonerCaptureStyle;
    private JComboBox<PrisonerStatus> comboPrisonerStatus;
    private JCheckBox chkPrisonerBabyStatus;
    private JCheckBox chkAtBPrisonerDefection;
    private JCheckBox chkAtBPrisonerRansom;

    // Personnel Randomization
    private JCheckBox chkUseDylansRandomXP;
    private JCheckBox chkRandomizeOrigin;
    private JCheckBox chkRandomizeDependentsOrigin;
    private JSpinner spnOriginSearchRadius;
    private JCheckBox chkExtraRandomOrigin;
    private JSpinner spnOriginDistanceScale;

    // Family
    private JComboBox<FamilialRelationshipDisplayLevel> comboDisplayFamilyLevel;

    // Salary
    private JSpinner spnCommissionedSalary;
    private JSpinner spnEnlistedSalary;
    private JSpinner spnAntiMekSalary;
    private JSpinner spnSpecialistInfantrySalary;
    private JSpinner[] spnSalaryExperienceMultipliers;
    private JSpinner[] spnBaseSalary;

    // Marriage
    private JCheckBox chkUseManualMarriages;
    private JCheckBox chkUseClannerMarriages;
    private JCheckBox chkUsePrisonerMarriages;
    private JSpinner spnMinimumMarriageAge;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JCheckBox chkLogMarriageNameChanges;
    private Map<MergingSurnameStyle, JSpinner> spnMarriageSurnameWeights;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomSameSexMarriages;
    private JCheckBox chkUseRandomClannerMarriages;
    private JCheckBox chkUseRandomPrisonerMarriages;
    private JSpinner spnRandomMarriageAgeRange;
    private JSpinner spnPercentageRandomMarriageOppositeSexChance;
    private JLabel lblPercentageRandomMarriageSameSexChance;
    private JSpinner spnPercentageRandomMarriageSameSexChance;

    // Divorce
    private JCheckBox chkUseManualDivorce;
    private JCheckBox chkUseClannerDivorce;
    private JCheckBox chkUsePrisonerDivorce;
    private Map<SplittingSurnameStyle, JSpinner> spnDivorceSurnameWeights;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JCheckBox chkUseRandomOppositeSexDivorce;
    private JCheckBox chkUseRandomSameSexDivorce;
    private JCheckBox chkUseRandomClannerDivorce;
    private JCheckBox chkUseRandomPrisonerDivorce;
    private JLabel lblPercentageRandomDivorceOppositeSexChance;
    private JSpinner spnPercentageRandomDivorceOppositeSexChance;
    private JLabel lblPercentageRandomDivorceSameSexChance;
    private JSpinner spnPercentageRandomDivorceSameSexChance;

    // Procreation
    private JCheckBox chkUseManualProcreation;
    private JCheckBox chkUseClannerProcreation;
    private JCheckBox chkUsePrisonerProcreation;
    private JSpinner spnMultiplePregnancyOccurrences;
    private MMComboBox<BabySurnameStyle> comboBabySurnameStyle;
    private JCheckBox chkAssignNonPrisonerBabiesFounderTag;
    private JCheckBox chkAssignChildrenOfFoundersFounderTag;
    private JCheckBox chkDetermineFatherAtBirth;
    private JCheckBox chkDisplayTrueDueDate;
    private JCheckBox chkLogProcreation;
    private MMComboBox<RandomProcreationMethod> comboRandomProcreationMethod;
    private JCheckBox chkUseRelationshiplessRandomProcreation;
    private JCheckBox chkUseRandomClannerProcreation;
    private JCheckBox chkUseRandomPrisonerProcreation;
    private JSpinner spnPercentageRandomProcreationRelationshipChance;
    private JLabel lblPercentageRandomProcreationRelationshiplessChance;
    private JSpinner spnPercentageRandomProcreationRelationshiplessChance;

    // Death
    private JCheckBox chkKeepMarriedNameUponSpouseDeath;
    //endregion Personnel Tab

    //region Finances Tab
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
    private JCheckBox usePercentageMaintBox;
    private JCheckBox useInfantryDontCountBox;
    private JCheckBox usePeacetimeCostBox;
    private JCheckBox useExtendedPartsModifierBox;
    private JCheckBox showPeacetimeCostBox;
    private JCheckBox newFinancialYearFinancesToCSVExportBox;
    private JComboBox<FinancialYearDuration> comboFinancialYearDuration;

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
    //endregion Finances Tab

    //region Mercenary Tab
    private JPanel panMercenary;
    private JRadioButton btnContractEquipment;
    private JSpinner spnEquipPercent;
    private JSpinner spnDropshipPercent;
    private JSpinner spnJumpshipPercent;
    private JSpinner spnWarshipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JRadioButton btnContractPersonnel;
    private JCheckBox chkBLCSaleValue;
    private JCheckBox chkOverageRepaymentInFinalPayment;
    //endregion Mercenary Tab

    //region Experience Tab
    private JPanel panXP;
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
    private JSpinner spnEdgeCost;
    private JTextArea txtInstructionsXP;
    private JScrollPane scrXP;
    private JTable tableXP;
    private static final String[] TABLE_XP_COLUMN_NAMES = {"+0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10"};
    //endregion Experience Tab

    //region Skills Tab
    private JPanel panSkill;
    //endregion Skills Tab

    //region Special Abilities Tab
    private JPanel panSpecialAbilities;
    private Hashtable<String, SpecialAbility> tempSPA;
    private JButton btnAddSPA;
    //endregion Special Abilities Tab

    //region Skill Randomization Tab
    private JPanel panRandomSkill;
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
    private JSpinner spnAbilGreen;
    private JSpinner spnAbilReg;
    private JSpinner spnAbilVet;
    private JSpinner spnAbilElite;
    //endregion Skill Randomization Tab

    //region Rank System Tab
    private RankSystemsPane rankSystemsPane;
    //endregion Rank System Tab

    //region Name and Portrait Generation Tab
    private JPanel panNameGen;
    private JCheckBox chkUseOriginFactionForNames;
    private JComboBox<String> comboFactionNames;
    private JSlider sldGender;
    private JPanel panRandomPortrait;
    private JCheckBox[] chkUsePortrait;
    private JCheckBox allPortraitsBox;
    private JCheckBox noPortraitsBox;
    private JCheckBox chkAssignPortraitOnRoleChange;
    //endregion Name and Portrait Generation Tab

    //region Markets Tab
    // Personnel Market
    private JComboBox<String> comboPersonnelMarketType;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JSpinner spnPersonnelMarketRandomEliteRemoval;
    private JSpinner spnPersonnelMarketRandomVeteranRemoval;
    private JSpinner spnPersonnelMarketRandomRegularRemoval;
    private JSpinner spnPersonnelMarketRandomGreenRemoval;
    private JSpinner spnPersonnelMarketRandomUltraGreenRemoval;
    private JSpinner spnPersonnelMarketDylansWeight;

    // Unit Market
    private JComboBox<UnitMarketMethod> comboUnitMarketMethod;
    private JCheckBox chkUnitMarketRegionalMechVariations;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkUnitMarketReportRefresh;

    // Contract Market
    private JComboBox<ContractMarketMethod> comboContractMarketMethod;
    private JCheckBox chkContractMarketReportRefresh;
    //endregion Markets Tab

    //region RATs Tab
    private JRadioButton btnUseRATGenerator;
    private JRadioButton btnUseStaticRATs;
    private DefaultListModel<String> chosenRATModel;
    private DefaultListModel<String> availableRATModel;
    private JCheckBox chkIgnoreRATEra;
    //endregion RATs Tab

    //region Against the Bot Tab
    private JPanel panAtB;
    private JCheckBox chkUseAtB;
    private JCheckBox chkUseStratCon;
    private JComboBox<String> cbSkillLevel;

    //unit administration
    private JCheckBox chkUseShareSystem;
    private JCheckBox chkSharesExcludeLargeCraft;
    private JCheckBox chkSharesForAll;
    private JCheckBox chkAeroRecruitsHaveUnits;
    private JCheckBox chkRetirementRolls;
    private JCheckBox chkCustomRetirementMods;
    private JCheckBox chkFoundersNeverRetire;
    private JCheckBox chkAddDependents;
    private JCheckBox chkDependentsNeverLeave;
    private JCheckBox chkTrackUnitFatigue;
    private JCheckBox chkUseLeadership;
    private JCheckBox chkTrackOriginalUnit;
    private JCheckBox chkUseAero;
    private JCheckBox chkUseVehicles;
    private JCheckBox chkClanVehicles;

    //contract operations
    private JSpinner spnSearchRadius;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkRestrictPartsByMission;
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

    //scenarios
    private JCheckBox chkDoubleVehicles;
    private JSpinner spnOpforLanceTypeMechs;
    private JSpinner spnOpforLanceTypeMixed;
    private JSpinner spnOpforLanceTypeVehicles;
    private JCheckBox chkOpforUsesVTOLs;
    private JCheckBox chkOpforUsesAero;
    private JSpinner spnOpforAeroChance;
    private JCheckBox chkOpforUsesLocalForces;
    private JSpinner spnOpforLocalForceChance;
    private JSpinner spnFixedMapChance;
    private JCheckBox chkAdjustPlayerVehicles;
    private JCheckBox chkRegionalMechVariations;
    private JCheckBox chkAttachedPlayerCamouflage;
    private JCheckBox chkPlayerControlsAttachedUnits;
    private JCheckBox chkUseDropShips;
    private JCheckBox chkUseWeatherConditions;
    private JCheckBox chkUseLightConditions;
    private JCheckBox chkUsePlanetaryConditions;
    //endregion Against the Bot Tab
    //endregion Variable Declarations

    //region Constructors
    public CampaignOptionsPane(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog", new EncodeControl()),
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
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public boolean isStartup() {
        return startup;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        addTab(resources.getString("generalPanel.title"), createGeneralTab());
        addTab(resources.getString("repairAndMaintenancePanel.title"), createRepairAndMaintenanceTab());
        addTab(resources.getString("suppliesAndAcquisitionsPanel.title"), createSuppliesAndAcquisitionsTab());
        addTab(resources.getString("techLimitsPanel.title"), createTechLimitsTab());
        addTab(resources.getString("personnelPanel.title"), createPersonnelTab());
        addTab(resources.getString("financesPanel.title"), createFinancesTab());
        addTab(resources.getString("mercenaryPanel.title"), createMercenaryTab());
        addTab(resources.getString("experiencePanel.title"), createExperienceTab());
        addTab(resources.getString("skillsPanel.title"), createSkillsTab());
        addTab(resources.getString("specialAbilitiesPanel.title"), createSpecialAbilitiesTab());
        addTab(resources.getString("skillRandomizationPanel.title"), createSkillRandomizationTab());
        addTab(resources.getString("rankSystemsPanel.title"), createRankSystemsTab());
        addTab(resources.getString("nameAndPortraitGenerationPanel.title"), createNameAndPortraitGenerationTab());
        addTab(resources.getString("marketsPanel.title"), createMarketsTab());
        addTab(resources.getString("ratPanel.title"), createRATTab());
        addTab(resources.getString("againstTheBotPanel.title"), createAgainstTheBotTab());

        setPreferences();
    }

    //region Legacy Initialization
    private JPanel createGeneralTab() {
        GridBagConstraints gridBagConstraints;
        int gridy = 0;
        int gridx = 0;

        JPanel panGeneral = new JPanel(new GridBagLayout());
        panGeneral.setName("panGeneral");

        JLabel lblName = new JLabel(resources.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraints = new GridBagConstraints();
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

        unitRatingMethodCombo = new JComboBox<>(UnitRatingMethod.values());
        unitRatingMethodCombo.setName("unitRatingMethodCombo");
        gridBagConstraints.gridx = 1;
        unitRatingPanel.add(unitRatingMethodCombo, gridBagConstraints);

        JLabel manualUnitRatingModifierLabel = new JLabel(resources.getString("manualUnitRatingModifierLabel.text"));
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

        btnDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(date));
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

        return panGeneral;
    }

    private JPanel createRepairAndMaintenanceTab() {
        GridBagConstraints gridBagConstraints;

        panRepair = new JPanel();
        panRepair.setName("panRepair");
        panRepair.setLayout(new GridBagLayout());

        JPanel panSubRepair = new JPanel(new GridBagLayout());
        JPanel panSubMaintenance = new JPanel(new GridBagLayout());

        panSubRepair.setBorder(BorderFactory.createTitledBorder("Repair"));
        panSubMaintenance.setBorder(BorderFactory.createTitledBorder("Maintenance"));

        gridBagConstraints = new GridBagConstraints();
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
        ((JSpinner.DefaultEditor) spnDamageMargin.getEditor()).getTextField().setEditable(false);
        JPanel pnlDamageMargin = new JPanel();
        pnlDamageMargin.add(new JLabel("Margin:"));
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
        ((JSpinner.DefaultEditor) spnDestroyPartTarget.getEditor()).getTextField().setEditable(false);

        JPanel pnlDestroyPartTarget = new JPanel();
        pnlDestroyPartTarget.add(new JLabel("Equipment hit in combat survives on a roll of"));
        pnlDestroyPartTarget.add(spnDestroyPartTarget);
        pnlDestroyPartTarget.add(new JLabel("or better"));

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
                spnMaintenanceBonus.setEnabled(true);
                logMaintenance.setEnabled(true);
            } else {
                spnMaintenanceDays.setEnabled(false);
                useQualityMaintenance.setEnabled(false);
                useUnofficialMaintenance.setEnabled(false);
                reverseQualityNames.setEnabled(false);
                spnMaintenanceBonus.setEnabled(false);
                logMaintenance.setEnabled(false);
            }
        });

        spnMaintenanceDays = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
        ((JSpinner.DefaultEditor) spnMaintenanceDays.getEditor()).getTextField().setEditable(false);
        JPanel pnlMaintenanceDays = new JPanel();
        pnlMaintenanceDays.add(spnMaintenanceDays);
        pnlMaintenanceDays.add(new JLabel("Maintenance cycle length in days"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceDays, gridBagConstraints);

        spnMaintenanceBonus = new JSpinner(new SpinnerNumberModel(0, -13, 13, 1));
        ((JSpinner.DefaultEditor) spnMaintenanceBonus.getEditor()).getTextField().setEditable(false);
        spnMaintenanceBonus.setToolTipText(resources.getString("spnMaintenanceBonus.toolTipText"));

        JPanel pnlMaintenanceBonus = new JPanel();
        pnlMaintenanceBonus.add(spnMaintenanceBonus);
        pnlMaintenanceBonus.add(new JLabel("Maintenance modifier"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceBonus, gridBagConstraints);

        useQualityMaintenance = new JCheckBox(resources.getString("useQualityMaintenance.text"));
        useQualityMaintenance.setToolTipText(resources.getString("useQualityMaintenance.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useQualityMaintenance, gridBagConstraints);

        reverseQualityNames = new JCheckBox(resources.getString("reverseQualityNames.text"));
        reverseQualityNames.setToolTipText(resources.getString("reverseQualityNames.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(reverseQualityNames, gridBagConstraints);

        useUnofficialMaintenance = new JCheckBox(resources.getString("useUnofficialMaintenance.text"));
        useUnofficialMaintenance.setToolTipText(resources.getString("useUnofficialMaintenance.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useUnofficialMaintenance, gridBagConstraints);

        logMaintenance = new JCheckBox(resources.getString("logMaintenance.text"));
        logMaintenance.setToolTipText(resources.getString("logMaintenance.toolTipText"));
        logMaintenance.setName("logMaintenance");
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panSubMaintenance.add(logMaintenance, gridBagConstraints);

        return panRepair;
    }

    private JPanel createSuppliesAndAcquisitionsTab() {
        GridBagConstraints gridBagConstraints;

        panSupplies = new JPanel();
        panSupplies.setName("panSupplies");
        panSupplies.setLayout(new GridBagLayout());

        JPanel panSubAcquire = new JPanel(new GridBagLayout());
        JPanel panSubDelivery = new JPanel(new GridBagLayout());
        JPanel panSubPlanetAcquire = new JPanel(new GridBagLayout());

        panSubAcquire.setBorder(BorderFactory.createTitledBorder("Acquisition"));
        panSubDelivery.setBorder(BorderFactory.createTitledBorder("Delivery"));
        panSubPlanetAcquire.setBorder(BorderFactory.createTitledBorder("Planetary Acquisition"));

        gridBagConstraints = new GridBagConstraints();
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
        ((JSpinner.DefaultEditor) spnAcquireWaitingPeriod.getEditor()).getTextField().setEditable(false);

        JPanel pnlWaitingPeriod = new JPanel();
        pnlWaitingPeriod.add(spnAcquireWaitingPeriod);
        pnlWaitingPeriod.add(new JLabel("Waiting period (in days) between acquisition rolls"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlWaitingPeriod, gridBagConstraints);

        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();
        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);
        choiceAcquireSkill = new JComboBox<>(acquireSkillModel);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panSubAcquire.add(new JLabel("Acquisition Skill:"), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panSubAcquire.add(choiceAcquireSkill, gridBagConstraints);

        chkSupportStaffOnly = new JCheckBox("Only support personnel can make acquisition checks");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(chkSupportStaffOnly, gridBagConstraints);

        spnAcquireClanPenalty = new JSpinner(new SpinnerNumberModel(0, 0, 13, 1));
        ((JSpinner.DefaultEditor) spnAcquireClanPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlClanPenalty = new JPanel();
        pnlClanPenalty.add(spnAcquireClanPenalty);
        pnlClanPenalty.add(new JLabel("Penalty for Clan equipment"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlClanPenalty, gridBagConstraints);

        spnAcquireIsPenalty = new JSpinner(new SpinnerNumberModel(0, 0, 13, 1));
        ((JSpinner.DefaultEditor) spnAcquireIsPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlIsPenalty = new JPanel();
        pnlIsPenalty.add(spnAcquireIsPenalty);
        pnlIsPenalty.add(new JLabel("Penalty for Inner Sphere equipment"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlIsPenalty, gridBagConstraints);

        txtMaxAcquisitions = new JTextField(4);
        txtMaxAcquisitions.setHorizontalAlignment(JTextField.RIGHT);
        txtMaxAcquisitions.setName("txtName");

        JPanel pnlMaxAcquisitions = new JPanel();
        pnlMaxAcquisitions.add(txtMaxAcquisitions);
        pnlMaxAcquisitions.add(new JLabel("Maximum Acquisitions Per Day (0 for unlimited)"));

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
        ((JSpinner.DefaultEditor) spnNDiceTransitTime.getEditor()).getTextField().setEditable(false);

        spnConstantTransitTime = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnConstantTransitTime.getEditor()).getTextField().setEditable(false);

        spnAcquireMosBonus = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireMosBonus.getEditor()).getTextField().setEditable(false);

        spnAcquireMinimum = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireMinimum.getEditor()).getTextField().setEditable(false);

        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        choiceTransitTimeUnits = new JComboBox<>(transitUnitModel);

        DefaultComboBoxModel<String> transitMosUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMosUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        choiceAcquireMosUnits = new JComboBox<>(transitMosUnitModel);

        DefaultComboBoxModel<String> transitMinUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMinUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        choiceAcquireMinimumUnit = new JComboBox<>(transitMinUnitModel);

        JPanel pnlTransitTime = new JPanel();
        pnlTransitTime.add(new JLabel("Delivery Time:"));
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
        pnlMinTransit.add(new JLabel("Minimum Transit Time:"));
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
        pnlMosBonus.add(new JLabel("Reduce delivery time by"));
        pnlMosBonus.add(spnAcquireMosBonus);
        pnlMosBonus.add(choiceAcquireMosUnits);
        pnlMosBonus.add(new JLabel("per MoS"));

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

        comboPlanetaryAcquisitionsFactionLimits = new JComboBox<>(PlanetaryAcquisitionFactionLimit.values());
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

        disallowPlanetaryAcquisitionClanCrossover = new JCheckBox(resources.getString("disallowPlanetaryAcquisitionClanCrossover.text"));
        disallowPlanetaryAcquisitionClanCrossover.setToolTipText(resources.getString("disallowPlanetaryAcquisitionClanCrossover.toolTipText"));
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

        usePlanetaryAcquisitionsVerbose = new JCheckBox(resources.getString("usePlanetaryAcquisitionsVerbose.text"));
        usePlanetaryAcquisitionsVerbose.setToolTipText(resources.getString("usePlanetaryAcquisitionsVerbose.toolTipText"));
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
        panSocioIndustrialBonus.setBorder(BorderFactory.createTitledBorder("Planet socio-industrial modifiers "));

        JPanel panTechBonus = new JPanel(new GridBagLayout());
        JPanel panIndustryBonus = new JPanel(new GridBagLayout());
        JPanel panOutputBonus = new JPanel(new GridBagLayout());

        spnPlanetAcquireTechBonus = new JSpinner[6];
        spnPlanetAcquireIndustryBonus = new JSpinner[6];
        spnPlanetAcquireOutputBonus = new JSpinner[6];

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        panTechBonus.add(new JLabel("<html><b>Tech<b></html>"), gridBagConstraints);
        panIndustryBonus.add(new JLabel("<html><b>Industry<b></html>"), gridBagConstraints);
        panOutputBonus.add(new JLabel("<html><b>Output<b></html>"), gridBagConstraints);
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
            ((JSpinner.DefaultEditor) spnPlanetAcquireTechBonus[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) spnPlanetAcquireIndustryBonus[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) spnPlanetAcquireOutputBonus[i].getEditor()).getTextField().setEditable(false);

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

        return panSupplies;
    }

    private JPanel createTechLimitsTab() {
        GridBagConstraints gridBagConstraints;
        int gridy = 0;

        panTech = new JPanel();
        panTech.setName("panTech");
        panTech.setLayout(new GridBagLayout());

        limitByYearBox = new JCheckBox(resources.getString("limitByYearBox.text"));
        limitByYearBox.setToolTipText(resources.getString("limitByYearBox.toolTipText"));
        limitByYearBox.setName("limitByYearBox");
        limitByYearBox.addActionListener(e -> variableTechLevelBox.setEnabled(limitByYearBox.isSelected()));
        gridBagConstraints = new GridBagConstraints();
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
        choiceTechLevel.setModel(techLevelComboBoxModel);
        //choiceTechLevel.setToolTipText(resources.getString("choiceTechLevel.toolTipText"));
        choiceTechLevel.setName("choiceTechLevel");
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

        return panTech;
    }

    private JPanel createFinancesTab() {
        GridBagConstraints gridBagConstraints;
        int gridy = 0;

        JPanel panFinances = new JPanel();
        panFinances.setName("panFinances");
        panFinances.setLayout(new GridBagLayout());

        payForPartsBox = new JCheckBox(resources.getString("payForPartsBox.text"));
        payForPartsBox.setToolTipText(resources.getString("payForPartsBox.toolTipText"));
        payForPartsBox.setName("payForPartsBox");
        gridBagConstraints = new GridBagConstraints();
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
        usePercentageMaintBox = new JCheckBox(resources.getString("usePercentageMaintBox.text"));
        usePercentageMaintBox.setToolTipText(resources.getString("usePercentageMaintBox.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(usePercentageMaintBox, gridBagConstraints);

        // Unofficial infantry don't count for contract pay
        useInfantryDontCountBox = new JCheckBox(resources.getString("infantryDontCount.text"));
        useInfantryDontCountBox.setToolTipText(resources.getString("infantryDontCount.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panFinances.add(useInfantryDontCountBox, gridBagConstraints);

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

        DefaultComboBoxModel<FinancialYearDuration> financialYearDurationModel = new DefaultComboBoxModel<>(FinancialYearDuration.values());
        comboFinancialYearDuration = new JComboBox<>(financialYearDurationModel);
        comboFinancialYearDuration.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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

        newFinancialYearFinancesToCSVExportBox = new JCheckBox(resources.getString("newFinancialYearFinancesToCSVExportBox.text"));
        newFinancialYearFinancesToCSVExportBox.setToolTipText(resources.getString("newFinancialYearFinancesToCSVExportBox.toolTipText"));
        newFinancialYearFinancesToCSVExportBox.setName("newFinancialYearFinancesToCSVExportBox");
        gridBagConstraints.gridy = gridy++;
        panFinances.add(newFinancialYearFinancesToCSVExportBox, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 20;
        panFinances.add(createPriceModifiersPanel(), gridBagConstraints);

        return panFinances;
    }

    private JPanel createMercenaryTab() {
        GridBagConstraints gridBagConstraints;

        panMercenary = new JPanel();
        panMercenary.setName("panMercenary");
        panMercenary.setLayout(new GridBagLayout());

        btnContractEquipment = new JRadioButton(resources.getString("panMercenary.IntOpsPayment.title"));
        btnContractEquipment.setToolTipText(resources.getString("panMercenary.IntOpsPayment.tooltip"));
        gridBagConstraints = new GridBagConstraints();
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
        panMercenary.add(new JLabel("Combat Percent:"), gridBagConstraints);

        spnEquipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1));
        spnEquipPercent.setEditor(new JSpinner.NumberEditor(spnEquipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnEquipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnEquipPercent, gridBagConstraints);

        chkEquipContractSaleValue = new JCheckBox("Base on equipment sale value");
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
        panMercenary.add(new JLabel("DropShip Percent:"), gridBagConstraints);

        spnDropshipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT, 0.1));
        spnDropshipPercent.setEditor(new JSpinner.NumberEditor(spnDropshipPercent, "0.0"));
        ((JSpinner.NumberEditor) spnDropshipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnDropshipPercent, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("JumpShip Percent:"), gridBagConstraints);

        spnJumpshipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT, 0.1));
        spnJumpshipPercent.setEditor(new JSpinner.NumberEditor(spnJumpshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnJumpshipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnJumpshipPercent, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("WarShip Percent:"), gridBagConstraints);

        spnWarshipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_WARSHIP_EQUIPMENT_PERCENT, 0.1));
        spnWarshipPercent.setEditor(new JSpinner.NumberEditor(spnWarshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnWarshipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnWarshipPercent, gridBagConstraints);

        btnContractPersonnel = new JRadioButton(resources.getString("panMercenary.FMMRPayment.title"));
        btnContractPersonnel.setToolTipText(resources.getString("panMercenary.FMMRPayment.tooltip"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractPersonnel, gridBagConstraints);

        chkBLCSaleValue = new JCheckBox("Base battle loss compensation on equipment sale value");
        gridBagConstraints.gridy = 6;
        panMercenary.add(chkBLCSaleValue, gridBagConstraints);

        chkOverageRepaymentInFinalPayment = new JCheckBox(resources.getString("chkOverageRepaymentInFinalPayment.text"));
        chkOverageRepaymentInFinalPayment.setToolTipText(resources.getString("chkOverageRepaymentInFinalPayment.toolTipText"));
        gridBagConstraints.gridy = 7;
        panMercenary.add(chkOverageRepaymentInFinalPayment, gridBagConstraints);

        ButtonGroup groupContract = new ButtonGroup();
        groupContract.add(btnContractEquipment);
        groupContract.add(btnContractPersonnel);

        return panMercenary;
    }

    private JPanel createExperienceTab() {
        GridBagConstraints gridBagConstraints;

        panXP = new JPanel();
        panXP.setName("panXP");
        panXP.setLayout(new GridBagLayout());

        JLabel lblScenarioXP = new JLabel(resources.getString("lblScenarioXP.text"));
        spnScenarioXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnScenarioXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnScenarioXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblScenarioXP, gridBagConstraints);

        JLabel lblKillXP = new JLabel(resources.getString("lblKillXP.text"));
        spnKillXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnKillXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnKillXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblKillXP, gridBagConstraints);

        JLabel lblKills = new JLabel(resources.getString("lblKills.text"));
        spnKills = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnKills.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnKills, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblKills, gridBagConstraints);

        JLabel lblTaskXP = new JLabel(resources.getString("lblKillXP.text"));
        spnTaskXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnTaskXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnTaskXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblTaskXP, gridBagConstraints);

        JLabel lblTasks = new JLabel(resources.getString("lblTasks.text"));
        spnNTasksXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnNTasksXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnNTasksXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblTasks, gridBagConstraints);

        JLabel lblSuccessXp = new JLabel(resources.getString("lblSuccessXP.text"));
        spnSuccessXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnSuccessXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnSuccessXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblSuccessXp, gridBagConstraints);

        JLabel lblMistakeXP = new JLabel(resources.getString("lblMistakeXP.text"));
        spnMistakeXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnMistakeXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMistakeXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(lblMistakeXP, gridBagConstraints);

        spnIdleXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP for every"), gridBagConstraints);

        spnMonthsIdleXP = new JSpinner(new SpinnerNumberModel(0, 0, 36, 1));
        ((JSpinner.DefaultEditor) spnMonthsIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnMonthsIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("active month(s) on a 2d6 roll of greater than or equal to"), gridBagConstraints);

        spnTargetIdleXP = new JSpinner(new SpinnerNumberModel(2, 2, 13, 1));
        ((JSpinner.DefaultEditor) spnTargetIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnTargetIdleXP, gridBagConstraints);

        spnContractNegotiationXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnContractNegotiationXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnContractNegotiationXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP awarded to the selected negotiator for a new contract"), gridBagConstraints);

        spnAdminWeeklyXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnAdminWeeklyXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP awarded to each administrator every Monday for the work of the previous"), gridBagConstraints);

        spnAdminWeeklyXPPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        ((JSpinner.DefaultEditor) spnAdminWeeklyXPPeriod.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXPPeriod, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("week(s)"), gridBagConstraints);

        spnEdgeCost = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnEdgeCost.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnEdgeCost, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP Cost for 1 Edge Point"), gridBagConstraints);

        txtInstructionsXP = new JTextArea(resources.getString("txtInstructionsXP.text"));
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
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(txtInstructionsXP, gridBagConstraints);

        tableXP = new JTable(getSkillCostsArray(SkillType.getSkillHash()), TABLE_XP_COLUMN_NAMES);
        tableXP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableXP.setRowSelectionAllowed(false);
        tableXP.setColumnSelectionAllowed(false);
        tableXP.setCellSelectionEnabled(true);
        scrXP = new JScrollPane(tableXP);
        scrXP.setMinimumSize(new Dimension(550, 140));
        scrXP.setPreferredSize(new Dimension(550, 140));
        JTable rowTable = new RowNamesTable(tableXP);
        scrXP.setRowHeaderView(rowTable);
        scrXP.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(scrXP, gridBagConstraints);

        return panXP;
    }

    private JScrollPane createSkillsTab() {
        GridBagConstraints gridBagConstraints;
        int gridy = 0;
        int gridx = 0;

        panSkill = new JPanel();
        panSkill.setName("panSkill");
        panSkill.setLayout(new GridBagLayout());

        JPanel skPanel;

        gridBagConstraints = new GridBagConstraints();
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
            ((JSpinner.DefaultEditor) spnTarget.getEditor()).getTextField().setEditable(false);
            hashSkillTargets.put(skillName, spnTarget);
            skPanel.add(spnTarget, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillGreen.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnGreen = new JSpinner(new SpinnerNumberModel(type.getGreenLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnGreen.getEditor()).getTextField().setEditable(false);
            hashGreenSkill.put(skillName, spnGreen);
            skPanel.add(spnGreen, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillRegular.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnReg = new JSpinner(new SpinnerNumberModel(type.getRegularLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnReg.getEditor()).getTextField().setEditable(false);
            hashRegSkill.put(skillName, spnReg);
            skPanel.add(spnReg, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillVeteran.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnVet = new JSpinner(new SpinnerNumberModel(type.getVeteranLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnVet.getEditor()).getTextField().setEditable(false);
            hashVetSkill.put(skillName, spnVet);
            skPanel.add(spnVet, c);
            c.gridx++;
            lblSkill = new JLabel(resources.getString("lblSkillElite.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnElite = new JSpinner(new SpinnerNumberModel(type.getEliteLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnElite.getEditor()).getTextField().setEditable(false);
            hashEliteSkill.put(skillName, spnElite);
            skPanel.add(spnElite, c);
            c.gridx++;

            skPanel.setBorder(BorderFactory.createTitledBorder(skillName));
            panSkill.add(skPanel, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        JScrollPane scrSkill = new JScrollPane(panSkill);
        scrSkill.setPreferredSize(new Dimension(500, 400));

        return scrSkill;
    }

    private JScrollPane createSpecialAbilitiesTab() {
        panSpecialAbilities = new JPanel(new GridBagLayout());

        Set<String> spaNames = SpecialAbility.getAllSpecialAbilities().keySet();
        //We need to create a temporary hash of special abilities that we can modify without
        //changing the underlying one in case the user cancels the changes
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

        panRandomSkill = new JPanel();
        panRandomSkill.setName("panRandomSkill");
        panRandomSkill.setLayout(new GridBagLayout());

        JPanel panRollTable = new JPanel(new GridLayout(6, 3, 5, 0));
        panRollTable.add(new JLabel("<html><b>Value</b></html>"));
        panRollTable.add(new JLabel("<html><b>Level</b></html>"));
        panRollTable.add(new JLabel("<html><b># Abils</b></html>"));
        panRollTable.add(new JLabel("less than 2"));
        JLabel lblUltraGreen = new JLabel("Ultra-Green/None");
        lblUltraGreen.setToolTipText(resources.getString("lblUltraGreen.toolTipText"));
        panRollTable.add(lblUltraGreen);
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("2-5"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_GREEN]));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("6-9"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_REGULAR]));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("10-11"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_VETERAN]));
        panRollTable.add(new JLabel("1"));
        panRollTable.add(new JLabel("12 or more"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ELITE]));
        panRollTable.add(new JLabel("2"));
        panRollTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2d6 + Bonus"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JLabel lblOverallRecruitBonus = new JLabel(resources.getString("lblOverallRecruitBonus.text"));
        chkExtraRandom = new JCheckBox(resources.getString("chkExtraRandom.text"));
        chkExtraRandom.setToolTipText(resources.getString("chkExtraRandom.toolTipText"));
        JLabel lblProbAntiMek = new JLabel(resources.getString("lblProbAntiMek.text"));
        spnProbAntiMek = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbAntiMek.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
        ((JSpinner.DefaultEditor) spnOverallRecruitBonus.getEditor()).getTextField().setEditable(false);
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
            ((JSpinner.DefaultEditor) spin.getEditor()).getTextField().setEditable(false);
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
        phenotypesPanel.setBorder(BorderFactory.createTitledBorder("Trueborn Phenotype Probabilities"));

        for (int i = 0; i < phenotypes.size(); i++) {
            JSpinner phenotypeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            phenotypeSpinners[i] = phenotypeSpinner;
            JPanel phenotypePanel = new JPanel();
            phenotypePanel.add(phenotypeSpinner);
            phenotypePanel.add(new JLabel(phenotypes.get(i).getName()));
            phenotypePanel.setToolTipText(phenotypes.get(i).getToolTip());
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
                BorderFactory.createTitledBorder("Artillery Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        spnArtyProb = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((JSpinner.DefaultEditor) spnArtyProb.getEditor()).getTextField().setEditable(false);
        spnArtyProb.setToolTipText(resources.getString("spnArtyProb.toolTipText"));
        panArtillery.add(spnArtyProb);
        panArtillery.add(new JLabel("Probability"));
        spnArtyBonus = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnArtyBonus.getEditor()).getTextField().setEditable(false);
        panArtillery.add(spnArtyBonus);
        panArtillery.add(new JLabel("Bonus"));
        JPanel panSecondary = new JPanel();
        spnSecondProb = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((JSpinner.DefaultEditor) spnSecondProb.getEditor()).getTextField().setEditable(false);
        spnSecondProb.setToolTipText(resources.getString("spnSecondProb.toolTipText"));
        panSecondary.add(spnSecondProb);
        panSecondary.add(new JLabel("Probability"));
        spnSecondBonus = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSecondBonus.getEditor()).getTextField().setEditable(false);
        panSecondary.add(spnSecondBonus);
        panSecondary.add(new JLabel("Bonus"));
        panSecondary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Skills"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panTactics = new JPanel();
        spnTacticsGreen = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsGreen.getEditor()).getTextField().setEditable(false);
        spnTacticsGreen.setToolTipText(resources.getString("spnTacticsGreen.toolTipText"));
        spnTacticsReg = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsReg.getEditor()).getTextField().setEditable(false);
        spnTacticsReg.setToolTipText(resources.getString("spnTacticsReg.toolTipText"));
        spnTacticsVet = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsVet.getEditor()).getTextField().setEditable(false);
        spnTacticsVet.setToolTipText(resources.getString("spnTacticsVet.toolTipText"));
        spnTacticsElite = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsElite.getEditor()).getTextField().setEditable(false);
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
                BorderFactory.createTitledBorder("Tactics Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panSmallArms = new JPanel();
        spnCombatSA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnCombatSA.getEditor()).getTextField().setEditable(false);
        spnCombatSA.setToolTipText(resources.getString("spnCombatSA.toolTipText"));
        spnSupportSA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSupportSA.getEditor()).getTextField().setEditable(false);
        spnSupportSA.setToolTipText(resources.getString("spnSupportSA.toolTipText"));
        panSmallArms.add(spnCombatSA);
        panSmallArms.add(new JLabel("Combat Personnel"));
        panSmallArms.add(spnSupportSA);
        panSmallArms.add(new JLabel("Support Personnel"));
        panSmallArms.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Small Arms Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panAbilities = new JPanel();
        spnAbilGreen = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilGreen.getEditor()).getTextField().setEditable(false);
        spnAbilGreen.setToolTipText(resources.getString("spnAbilGreen.toolTipText"));
        spnAbilReg = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilReg.getEditor()).getTextField().setEditable(false);
        spnAbilReg.setToolTipText(resources.getString("spnAbilReg.toolTipText"));
        spnAbilVet = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilVet.getEditor()).getTextField().setEditable(false);
        spnAbilVet.setToolTipText(resources.getString("spnAbilVet.toolTipText"));
        spnAbilElite = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilElite.getEditor()).getTextField().setEditable(false);
        spnAbilElite.setToolTipText(resources.getString("spnAbilElite.toolTipText"));
        panAbilities.add(spnAbilGreen);
        panAbilities.add(new JLabel("Green"));
        panAbilities.add(spnAbilReg);
        panAbilities.add(new JLabel("Reg"));
        panAbilities.add(spnAbilVet);
        panAbilities.add(new JLabel("Vet"));
        panAbilities.add(spnAbilElite);
        panAbilities.add(new JLabel("Elite"));
        panAbilities.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Special Abilities"),
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

    private JPanel createNameAndPortraitGenerationTab() {
        GridBagConstraints gridBagConstraints;
        int gridy = 0;

        panNameGen = new JPanel();
        panNameGen.setName("panNameGen");
        panNameGen.setLayout(new GridBagLayout());

        chkUseOriginFactionForNames = new JCheckBox(resources.getString("chkUseOriginFactionForNames.text"));
        chkUseOriginFactionForNames.setToolTipText(resources.getString("chkUseOriginFactionForNames.toolTipText"));
        chkUseOriginFactionForNames.setName("chkUseOriginFactionForNames");
        chkUseOriginFactionForNames.addActionListener(
                evt -> comboFactionNames.setEnabled(!chkUseOriginFactionForNames.isSelected()));
        gridBagConstraints = new GridBagConstraints();
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
        comboFactionNames = new JComboBox<>(factionNamesModel);
        comboFactionNames.setMinimumSize(new Dimension(400, 30));
        comboFactionNames.setName("comboFactionNames");
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

        panRandomPortrait = new JPanel();
        panRandomPortrait.setName("panRandomPortrait");
        panRandomPortrait.setLayout(new BorderLayout());

        // The math below is used to determine how to split the personnel role options for portraits,
        // which it does into 4 columns with rows equal to the number of roles plus two, with the
        // additional two being the all role and no role options.
        JPanel panUsePortrait = new JPanel(new GridLayout((int) Math.ceil((personnelRoles.length + 2) / 4.0), 4));
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

        chkAssignPortraitOnRoleChange = new JCheckBox(resources.getString("chkAssignPortraitOnRoleChange.text"));
        chkAssignPortraitOnRoleChange.setToolTipText(resources.getString("chkAssignPortraitOnRoleChange.toolTipText"));
        chkAssignPortraitOnRoleChange.setName("chkAssignPortraitOnRoleChange");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(chkAssignPortraitOnRoleChange, gridBagConstraints);

        return panNameGen;
    }

    private JPanel createAgainstTheBotTab() {
        GridBagConstraints gridBagConstraints;

        panAtB = new JPanel();

        cbSkillLevel = new JComboBox<>();
        chkUseShareSystem = new JCheckBox();
        chkRetirementRolls = new JCheckBox();
        chkTrackUnitFatigue = new JCheckBox();
        chkCustomRetirementMods = new JCheckBox();
        chkFoundersNeverRetire = new JCheckBox();
        chkTrackOriginalUnit = new JCheckBox();
        chkLimitLanceWeight = new JCheckBox();
        chkLimitLanceNumUnits = new JCheckBox();
        chkUseLeadership = new JCheckBox();
        chkUseStrategy = new JCheckBox();
        spnBaseStrategyDeployment = new JSpinner();
        spnAdditionalStrategyDeployment = new JSpinner();

        chkUseAero = new JCheckBox();
        chkUseVehicles = new JCheckBox();
        chkClanVehicles = new JCheckBox();
        chkDoubleVehicles = new JCheckBox();
        chkAdjustPlayerVehicles = new JCheckBox();
        spnOpforLanceTypeMechs = new JSpinner();
        spnOpforLanceTypeMixed = new JSpinner();
        spnOpforLanceTypeVehicles = new JSpinner();
        chkOpforUsesVTOLs = new JCheckBox();
        chkOpforUsesAero = new JCheckBox();
        chkOpforUsesLocalForces = new JCheckBox();
        spnOpforAeroChance = new JSpinner();
        spnOpforLocalForceChance = new JSpinner();
        spnFixedMapChance = new JSpinner();

        spnSearchRadius = new JSpinner();
        chkVariableContractLength = new JCheckBox();
        chkMercSizeLimited = new JCheckBox();
        chkRestrictPartsByMission = new JCheckBox();
        chkUseLightConditions = new JCheckBox();
        chkUsePlanetaryConditions = new JCheckBox();
        chkAeroRecruitsHaveUnits = new JCheckBox();

        panAtB.setName("panAtB");
        panAtB.setLayout(new GridBagLayout());

        JPanel panSubAtBAdmin = new JPanel(new GridBagLayout());
        JPanel panSubAtBContract = new JPanel(new GridBagLayout());
        JPanel panSubAtBScenario = new JPanel(new GridBagLayout());
        panSubAtBAdmin.setBorder(BorderFactory.createTitledBorder("Unit Administration"));
        panSubAtBContract.setBorder(BorderFactory.createTitledBorder("Contract Operations"));
        panSubAtBScenario.setBorder(BorderFactory.createTitledBorder("Scenarios"));

        chkUseAtB = new JCheckBox(resources.getString("chkUseAtB.text"));
        chkUseAtB.setToolTipText(resources.getString("chkUseAtB.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(10, 10, 10, 10);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panAtB.add(chkUseAtB, gridBagConstraints);
        chkUseAtB.addActionListener(ev -> enableAtBComponents(panAtB, chkUseAtB.isSelected()));

        JLabel lblSkillLevel = new JLabel(resources.getString("lblSkillLevel.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        panAtB.add(lblSkillLevel, gridBagConstraints);

        // TODO : Switch me to use a modified RandomSkillsGenerator.levelNames
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ULTRA_GREEN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_GREEN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_REGULAR]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_VETERAN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ELITE]);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        panAtB.add(cbSkillLevel, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panAtB.add(panSubAtBAdmin, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
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
        chkUseShareSystem = new JCheckBox(resources.getString("chkUseShareSystem.text"));
        chkUseShareSystem.setToolTipText(resources.getString("chkUseShareSystem.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBAdmin.add(chkUseShareSystem, gridBagConstraints);

        chkSharesExcludeLargeCraft = new JCheckBox(resources.getString("chkSharesExcludeLargeCraft.text"));
        chkSharesExcludeLargeCraft.setToolTipText(resources.getString("chkSharesExcludeLargeCraft.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesExcludeLargeCraft, gridBagConstraints);

        chkSharesForAll = new JCheckBox(resources.getString("chkSharesForAll.text"));
        chkSharesForAll.setToolTipText(resources.getString("chkSharesForAll.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesForAll, gridBagConstraints);

        chkAeroRecruitsHaveUnits = new JCheckBox(resources.getString("chkAeroRecruitsHaveUnits.text"));
        chkAeroRecruitsHaveUnits.setToolTipText(resources.getString("chkAeroRecruitsHaveUnits.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAeroRecruitsHaveUnits, gridBagConstraints);

        chkRetirementRolls = new JCheckBox(resources.getString("chkRetirementRolls.text"));
        chkRetirementRolls.setToolTipText(resources.getString("chkRetirementRolls.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkRetirementRolls, gridBagConstraints);

        chkCustomRetirementMods = new JCheckBox(resources.getString("chkCustomRetirementMods.text"));
        chkCustomRetirementMods.setToolTipText(resources.getString("chkCustomRetirementMods.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkCustomRetirementMods, gridBagConstraints);

        chkFoundersNeverRetire = new JCheckBox(resources.getString("chkFoundersNeverRetire.text"));
        chkFoundersNeverRetire.setToolTipText(resources.getString("chkFoundersNeverRetire.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkFoundersNeverRetire, gridBagConstraints);

        chkAddDependents = new JCheckBox(resources.getString("chkAddDependents.text"));
        chkAddDependents.setToolTipText(resources.getString("chkAddDependents.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAddDependents, gridBagConstraints);

        chkDependentsNeverLeave = new JCheckBox(resources.getString("chkDependentsNeverLeave.text"));
        chkDependentsNeverLeave.setToolTipText(resources.getString("chkDependentsNeverLeave.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkDependentsNeverLeave, gridBagConstraints);

        chkTrackUnitFatigue = new JCheckBox(resources.getString("chkTrackUnitFatigue.text"));
        chkTrackUnitFatigue.setToolTipText(resources.getString("chkTrackUnitFatigue.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackUnitFatigue, gridBagConstraints);

        chkUseLeadership = new JCheckBox(resources.getString("chkUseLeadership.text"));
        chkUseLeadership.setToolTipText(resources.getString("chkUseLeadership.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseLeadership, gridBagConstraints);

        chkTrackOriginalUnit = new JCheckBox(resources.getString("chkTrackOriginalUnit.text"));
        chkTrackOriginalUnit.setToolTipText(resources.getString("chkTrackOriginalUnit.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackOriginalUnit, gridBagConstraints);

        chkUseAero = new JCheckBox(resources.getString("chkUseAero.text"));
        chkUseAero.setToolTipText(resources.getString("chkUseAero.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseAero, gridBagConstraints);

        chkUseVehicles = new JCheckBox(resources.getString("chkUseVehicles.text"));
        chkUseVehicles.setToolTipText(resources.getString("chkUseVehicles.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseVehicles, gridBagConstraints);

        chkClanVehicles = new JCheckBox(resources.getString("chkClanVehicles.text"));
        chkClanVehicles.setToolTipText(resources.getString("chkClanVehicles.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkClanVehicles, gridBagConstraints);

        JLabel lblSearchRadius = new JLabel(resources.getString("lblSearchRadius.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBContract.add(lblSearchRadius, gridBagConstraints);

        spnSearchRadius.setModel(new SpinnerNumberModel(300, 100, 2500, 100));
        spnSearchRadius.setToolTipText(resources.getString("spnSearchRadius.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        panSubAtBContract.add(spnSearchRadius, gridBagConstraints);

        chkVariableContractLength = new JCheckBox(resources.getString("chkVariableContractLength.text"));
        chkVariableContractLength.setToolTipText(resources.getString("chkVariableContractLength.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkVariableContractLength, gridBagConstraints);

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

        chkLimitLanceWeight = new JCheckBox(resources.getString("chkLimitLanceWeight.text"));
        chkLimitLanceWeight.setToolTipText(resources.getString("chkLimitLanceWeight.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceWeight, gridBagConstraints);

        chkLimitLanceNumUnits = new JCheckBox(resources.getString("chkLimitLanceNumUnits.text"));
        chkLimitLanceNumUnits.setToolTipText(resources.getString("chkLimitLanceNumUnits.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceNumUnits, gridBagConstraints);

        JLabel lblLanceStructure = new JLabel(resources.getString("lblLanceStructure.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblLanceStructure, gridBagConstraints);

        chkUseStrategy = new JCheckBox(resources.getString("chkUseStrategy.text"));
        chkUseStrategy.setToolTipText(resources.getString("chkUseStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkUseStrategy, gridBagConstraints);

        JLabel lblBaseStrategyDeployment = new JLabel(resources.getString("lblBaseStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblBaseStrategyDeployment, gridBagConstraints);

        spnBaseStrategyDeployment.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnBaseStrategyDeployment.setToolTipText(resources.getString("spnBaseStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        panSubAtBContract.add(spnBaseStrategyDeployment, gridBagConstraints);

        JLabel lblAdditionalStrategyDeployment = new JLabel(resources.getString("lblAdditionalStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblAdditionalStrategyDeployment, gridBagConstraints);

        spnAdditionalStrategyDeployment.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnAdditionalStrategyDeployment.setToolTipText(resources.getString("spnAdditionalStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        panSubAtBContract.add(spnAdditionalStrategyDeployment, gridBagConstraints);

        chkAdjustPaymentForStrategy = new JCheckBox(resources.getString("chkAdjustPaymentForStrategy.text"));
        chkAdjustPaymentForStrategy.setName("chkAdjustPaymentForStrategy");
        chkAdjustPaymentForStrategy.setToolTipText(resources.getString("chkAdjustPaymentForStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkAdjustPaymentForStrategy, gridBagConstraints);

        JLabel lblIntensity = new JLabel(resources.getString("lblIntensity.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        panSubAtBContract.add(lblIntensity, gridBagConstraints);

        // Note that spnAtBBattleIntensity is located here visibly, however must be initialized
        // following the chance of battle by role

        JLabel lblBattleFrequency = new JLabel(resources.getString("lblBattleFrequency.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(lblBattleFrequency, gridBagConstraints);

        spnAtBBattleChance = new JSpinner[AtBLanceRole.values().length - 1];

        JLabel lblFightChance = new JLabel(AtBLanceRole.FIGHTING.toString() + ":");
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblFightChance, gridBagConstraints);

        JSpinner atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblDefendChance = new JLabel(AtBLanceRole.DEFENCE.toString() + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        panSubAtBContract.add(lblDefendChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblScoutChance = new JLabel(AtBLanceRole.SCOUTING.toString() + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        panSubAtBContract.add(lblScoutChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblTrainingChance = new JLabel(AtBLanceRole.TRAINING.toString() + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
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
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(btnIntensityUpdate, gridBagConstraints);

        // Note that this must be after the chance by role because it requires the chance by role
        // for the initial value to be calculated
        spnAtBBattleIntensity = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
        spnAtBBattleIntensity.setToolTipText(resources.getString("spnIntensity.toolTipText"));
        spnAtBBattleIntensity.addChangeListener(atBBattleIntensityChangeListener);
        spnAtBBattleIntensity.setMinimumSize(new Dimension(60, 25));
        spnAtBBattleIntensity.setPreferredSize(new Dimension(60, 25));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(spnAtBBattleIntensity, gridBagConstraints);

        chkGenerateChases = new JCheckBox(resources.getString("chkGenerateChases.text"));
        chkGenerateChases.setName("chkGenerateChases");
        chkGenerateChases.setToolTipText(resources.getString("chkGenerateChases.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        panSubAtBContract.add(chkGenerateChases, gridBagConstraints);

        int yTablePosition = 0;
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

        JLabel lblOpforLanceType = new JLabel(resources.getString("lblOpforLanceType.text"));
        lblOpforLanceType.setToolTipText(resources.getString("lblOpforLanceType.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(lblOpforLanceType, gridBagConstraints);

        spnOpforLanceTypeMechs.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpforLanceTypeMechs.setToolTipText(resources.getString("lblOpforLanceType.toolTipText"));
        spnOpforLanceTypeMixed.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpforLanceTypeMixed.setToolTipText(resources.getString("lblOpforLanceType.toolTipText"));
        spnOpforLanceTypeVehicles.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpforLanceTypeVehicles.setToolTipText(resources.getString("lblOpforLanceType.toolTipText"));
        JPanel panOpforLanceType = new JPanel();
        panOpforLanceType.add(new JLabel(resources.getString("lblOpforLanceTypeMek.text")));
        panOpforLanceType.add(spnOpforLanceTypeMechs);
        panOpforLanceType.add(new JLabel(resources.getString("lblOpforLanceTypeMixed.text")));
        panOpforLanceType.add(spnOpforLanceTypeMixed);
        panOpforLanceType.add(new JLabel(resources.getString("lblOpforLanceTypeVehicle.text")));
        panOpforLanceType.add(spnOpforLanceTypeVehicles);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforLanceType, gridBagConstraints);

        chkOpforUsesVTOLs = new JCheckBox(resources.getString("chkOpforUsesVTOLs.text"));
        chkOpforUsesVTOLs.setToolTipText(resources.getString("chkOpforUsesVTOLs.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkOpforUsesVTOLs, gridBagConstraints);

        JPanel panOpforAero = new JPanel();
        chkOpforUsesAero = new JCheckBox(resources.getString("chkOpforUsesAero.text"));
        chkOpforUsesAero.setToolTipText(resources.getString("chkOpforUsesAero.toolTipText"));
        JLabel lblOpforAeroChance = new JLabel(resources.getString("lblOpforAeroLikelihood.text"));
        lblOpforAeroChance.setToolTipText(resources.getString("lblOpforAeroLikelihood.toolTipText"));
        spnOpforAeroChance.setModel(new SpinnerNumberModel(0, 0, 6, 1));
        panOpforAero.add(chkOpforUsesAero);
        panOpforAero.add(spnOpforAeroChance);
        panOpforAero.add(lblOpforAeroChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforAero, gridBagConstraints);

        JPanel panOpforLocal = new JPanel();
        chkOpforUsesLocalForces = new JCheckBox(resources.getString("chkOpforUsesLocalForces.text"));
        chkOpforUsesLocalForces.setToolTipText(resources.getString("chkOpforUsesLocalForces.toolTipText"));
        JLabel lblOpforLocalForceChance = new JLabel(resources.getString("lblOpforLocalForceLikelihood.text"));
        lblOpforLocalForceChance.setToolTipText(resources.getString("lblOpforLocalForceLikelihood.toolTipText"));
        spnOpforLocalForceChance.setModel(new SpinnerNumberModel(0, 0, 6, 1));
        panOpforLocal.add(chkOpforUsesLocalForces);
        panOpforLocal.add(spnOpforLocalForceChance);
        panOpforLocal.add(lblOpforLocalForceChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforLocal, gridBagConstraints);

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

        chkRegionalMechVariations = new JCheckBox(resources.getString("chkRegionalMechVariations.text"));
        chkRegionalMechVariations.setToolTipText(resources.getString("chkRegionalMechVariations.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkRegionalMechVariations, gridBagConstraints);

        chkAttachedPlayerCamouflage = new JCheckBox(resources.getString("chkAttachedPlayerCamouflage.text"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkAttachedPlayerCamouflage, gridBagConstraints);

        chkPlayerControlsAttachedUnits = new JCheckBox(resources.getString("chkPlayerControlsAttachedUnits.text"));
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
        spnFixedMapChance.setModel(new SpinnerNumberModel(0, 0, 100, 10));
        panFixedMapChance.add(lblFixedMapChance);
        panFixedMapChance.add(spnFixedMapChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panFixedMapChance, gridBagConstraints);

        JScrollPane scrAtB = new JScrollPane(panAtB);
        scrAtB.setPreferredSize(new Dimension(500, 410));

        enableAtBComponents(panAtB, chkUseAtB.isSelected());

        return scrAtB;
    }
    //endregion Legacy Initialization

    //region Modern Initialization
    //region Personnel Tab
    private JScrollPane createPersonnelTab() {
        final JPanel personnelPanel = new JPanel(new GridBagLayout());
        personnelPanel.setName("personnelPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        personnelPanel.add(createGeneralPersonnelPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createExpandedPersonnelInformationPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createMedicalPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createPrisonerPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createPersonnelRandomizationPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createFamilyPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        personnelPanel.add(createSalaryPanel(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        personnelPanel.add(createMarriagePanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createDivorcePanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createProcreationPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createDeathPanel(), gbc);

        final JScrollPane scrollPersonnel = new JScrollPane(personnelPanel);
        scrollPersonnel.setPreferredSize(new Dimension(500, 400));

        return scrollPersonnel;
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

        chkUseAlternativeQualityAveraging = new JCheckBox(resources.getString("chkUseAlternativeQualityAveraging.text"));
        chkUseAlternativeQualityAveraging.setToolTipText(resources.getString("chkUseAlternativeQualityAveraging.toolTipText"));
        chkUseAlternativeQualityAveraging.setName("chkUseAlternativeQualityAveraging");

        chkUseTransfers = new JCheckBox(resources.getString("chkUseTransfers.text"));
        chkUseTransfers.setToolTipText(resources.getString("chkUseTransfers.toolTipText"));
        chkUseTransfers.setName("chkUseTransfers");

        chkUseExtendedTOEForceName = new JCheckBox(resources.getString("chkUseExtendedTOEForceName.text"));
        chkUseExtendedTOEForceName.setToolTipText(resources.getString("chkUseExtendedTOEForceName.toolTipText"));
        chkUseExtendedTOEForceName.setName("chkUseExtendedTOEForceName ");

        chkPersonnelLogSkillGain = new JCheckBox(resources.getString("chkPersonnelLogSkillGain.text"));
        chkPersonnelLogSkillGain.setToolTipText(resources.getString("chkPersonnelLogSkillGain.toolTipText"));
        chkPersonnelLogSkillGain.setName("chkPersonnelLogSkillGain");

        chkPersonnelLogAbilityGain = new JCheckBox(resources.getString("chkPersonnelLogAbilityGain.text"));
        chkPersonnelLogAbilityGain.setToolTipText(resources.getString("chkPersonnelLogAbilityGain.toolTipText"));
        chkPersonnelLogAbilityGain.setName("chkPersonnelLogAbilityGain");

        chkPersonnelLogEdgeGain = new JCheckBox(resources.getString("chkPersonnelLogEdgeGain.text"));
        chkPersonnelLogEdgeGain.setToolTipText(resources.getString("chkPersonnelLogEdgeGain.toolTipText"));
        chkPersonnelLogEdgeGain.setName("chkPersonnelLogEdgeGain");

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
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging)
                        .addComponent(chkUseTransfers)
                        .addComponent(chkUseExtendedTOEForceName)
                        .addComponent(chkPersonnelLogSkillGain)
                        .addComponent(chkPersonnelLogAbilityGain)
                        .addComponent(chkPersonnelLogEdgeGain)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging)
                        .addComponent(chkUseTransfers)
                        .addComponent(chkUseExtendedTOEForceName)
                        .addComponent(chkPersonnelLogSkillGain)
                        .addComponent(chkPersonnelLogAbilityGain)
                        .addComponent(chkPersonnelLogEdgeGain)
        );

        return panel;
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
        lblTimeInServiceDisplayFormat.setToolTipText(resources.getString("lblTimeInServiceDisplayFormat.toolTipText"));
        lblTimeInServiceDisplayFormat.setName("lblTimeInServiceDisplayFormat");

        comboTimeInServiceDisplayFormat = new JComboBox<>(TimeInDisplayFormat.values());
        comboTimeInServiceDisplayFormat.setToolTipText(resources.getString("lblTimeInServiceDisplayFormat.toolTipText"));
        comboTimeInServiceDisplayFormat.setName("comboTimeInServiceDisplayFormat");

        chkUseTimeInRank = new JCheckBox(resources.getString("chkUseTimeInRank.text"));
        chkUseTimeInRank.setToolTipText(resources.getString("chkUseTimeInRank.toolTipText"));
        chkUseTimeInRank.setName("chkUseTimeInRank");
        chkUseTimeInRank.addActionListener(evt -> {
            lblTimeInRankDisplayFormat.setEnabled(chkUseTimeInRank.isSelected());
            comboTimeInRankDisplayFormat.setEnabled(chkUseTimeInRank.isSelected());
        });

        lblTimeInRankDisplayFormat.setText(resources.getString("lblTimeInRankDisplayFormat.text"));
        lblTimeInRankDisplayFormat.setToolTipText(resources.getString("lblTimeInRankDisplayFormat.toolTipText"));
        lblTimeInRankDisplayFormat.setName("lblTimeInRankDisplayFormat");

        comboTimeInRankDisplayFormat = new JComboBox<>(TimeInDisplayFormat.values());
        comboTimeInRankDisplayFormat.setToolTipText(resources.getString("lblTimeInRankDisplayFormat.toolTipText"));
        comboTimeInRankDisplayFormat.setName("comboTimeInRankDisplayFormat");

        chkUseRetirementDateTracking = new JCheckBox(resources.getString("chkUseRetirementDateTracking.text"));
        chkUseRetirementDateTracking.setToolTipText(resources.getString("chkUseRetirementDateTracking.toolTipText"));
        chkUseRetirementDateTracking.setName("chkUseRetirementDateTracking");

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
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("expandedPersonnelInformationPanel.title")));
        panel.setName("expandedPersonnelInformationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTimeInService)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblTimeInServiceDisplayFormat)
                                .addComponent(comboTimeInServiceDisplayFormat, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseTimeInRank)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblTimeInRankDisplayFormat)
                                .addComponent(comboTimeInRankDisplayFormat, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRetirementDateTracking)
                        .addComponent(chkTrackTotalEarnings)
                        .addComponent(chkTrackTotalXPEarnings)
                        .addComponent(chkShowOriginFaction)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseTimeInService)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTimeInServiceDisplayFormat)
                                .addComponent(comboTimeInServiceDisplayFormat))
                        .addComponent(chkUseTimeInRank)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTimeInRankDisplayFormat)
                                .addComponent(comboTimeInRankDisplayFormat))
                        .addComponent(chkUseRetirementDateTracking)
                        .addComponent(chkTrackTotalEarnings)
                        .addComponent(chkTrackTotalXPEarnings)
                        .addComponent(chkShowOriginFaction)
        );

        return panel;
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

        final JLabel lblNaturalHealWaitingPeriod = new JLabel(resources.getString("lblNaturalHealWaitingPeriod.text"));
        lblNaturalHealWaitingPeriod.setToolTipText(resources.getString("lblNaturalHealWaitingPeriod.toolTipText"));
        lblNaturalHealWaitingPeriod.setName("lblNaturalHealWaitingPeriod");

        spnNaturalHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        spnNaturalHealWaitingPeriod.setToolTipText(resources.getString("lblNaturalHealWaitingPeriod.toolTipText"));
        spnNaturalHealWaitingPeriod.setName("spnNaturalHealWaitingPeriod");

        final JLabel lblMinimumHitsForVehicles = new JLabel(resources.getString("lblMinimumHitsForVehicles.text"));
        lblMinimumHitsForVehicles.setToolTipText(resources.getString("lblMinimumHitsForVehicles.toolTipText"));
        lblMinimumHitsForVehicles.setName("lblMinimumHitsForVehicles");

        spnMinimumHitsForVehicles = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        spnMinimumHitsForVehicles.setToolTipText(resources.getString("lblMinimumHitsForVehicles.toolTipText"));
        spnMinimumHitsForVehicles.setName("spnMinimumHitsForVehicles");
        ((JSpinner.DefaultEditor) spnMinimumHitsForVehicles.getEditor()).getTextField().setEditable(false);

        chkUseRandomHitsForVehicles = new JCheckBox(resources.getString("chkUseRandomHitsForVehicles.text"));
        chkUseRandomHitsForVehicles.setToolTipText(resources.getString("chkUseRandomHitsForVehicles.toolTipText"));
        chkUseRandomHitsForVehicles.setName("chkUseRandomHitsForVehicles");

        chkUseTougherHealing = new JCheckBox(resources.getString("chkUseTougherHealing.text"));
        chkUseTougherHealing.setToolTipText(resources.getString("chkUseTougherHealing.toolTipText"));
        chkUseTougherHealing.setName("chkUseTougherHealing");

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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblHealWaitingPeriod)
                                .addComponent(spnHealWaitingPeriod, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblNaturalHealWaitingPeriod)
                                .addComponent(spnNaturalHealWaitingPeriod, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMinimumHitsForVehicles)
                                .addComponent(spnMinimumHitsForVehicles, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRandomHitsForVehicles)
                        .addComponent(chkUseTougherHealing)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
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
        );

        return panel;
    }

    private JPanel createPrisonerPanel() {
        // Create Panel Components
        final JLabel lblPrisonerCaptureStyle = new JLabel(resources.getString("lblPrisonerCaptureStyle.text"));
        lblPrisonerCaptureStyle.setToolTipText(resources.getString("lblPrisonerCaptureStyle.toolTipText"));
        lblPrisonerCaptureStyle.setName("lblPrisonerCaptureStyle");

        comboPrisonerCaptureStyle = new JComboBox<>(PrisonerCaptureStyle.values());
        comboPrisonerCaptureStyle.setName("comboPrisonerCaptureStyle");
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrisonerCaptureStyle) {
                    list.setToolTipText(((PrisonerCaptureStyle) value).getToolTip());
                }
                return this;
            }
        });

        final JLabel lblPrisonerStatus = new JLabel(resources.getString("lblPrisonerStatus.text"));
        lblPrisonerStatus.setToolTipText(resources.getString("lblPrisonerStatus.toolTipText"));
        lblPrisonerStatus.setName("lblPrisonerStatus");

        final DefaultComboBoxModel<PrisonerStatus> prisonerStatusModel = new DefaultComboBoxModel<>(PrisonerStatus.values());
        prisonerStatusModel.removeElement(PrisonerStatus.FREE); // we don't want this as a standard use case for prisoners
        comboPrisonerStatus = new JComboBox<>(prisonerStatusModel);
        comboPrisonerStatus.setToolTipText(resources.getString("lblPrisonerStatus.toolTipText"));
        comboPrisonerStatus.setName("comboPrisonerStatus");

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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPrisonerCaptureStyle)
                                .addComponent(comboPrisonerCaptureStyle, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPrisonerStatus)
                                .addComponent(comboPrisonerStatus, GroupLayout.Alignment.LEADING))
                        .addComponent(chkPrisonerBabyStatus)
                        .addComponent(chkAtBPrisonerDefection)
                        .addComponent(chkAtBPrisonerRansom)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPrisonerCaptureStyle)
                                .addComponent(comboPrisonerCaptureStyle))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPrisonerStatus)
                                .addComponent(comboPrisonerStatus))
                        .addComponent(chkPrisonerBabyStatus)
                        .addComponent(chkAtBPrisonerDefection)
                        .addComponent(chkAtBPrisonerRansom)
        );

        return panel;
    }

    private JPanel createPersonnelRandomizationPanel() {
        // Create Panel Components
        chkUseDylansRandomXP = new JCheckBox(resources.getString("chkUseDylansRandomXP.text"));
        chkUseDylansRandomXP.setToolTipText(resources.getString("chkUseDylansRandomXP.toolTipText"));
        chkUseDylansRandomXP.setName("chkUseDylansRandomXP");

        final JPanel randomOriginPanel = createRandomOriginPanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelRandomizationPanel.title")));
        panel.setName("personnelRandomizationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseDylansRandomXP)
                        .addComponent(randomOriginPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseDylansRandomXP)
                        .addComponent(randomOriginPanel)
        );

        return panel;
    }

    private JPanel createRandomOriginPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblOriginSearchRadius = new JLabel();
        final JLabel lblOriginDistanceScale = new JLabel();

        // Create Panel Components
        chkRandomizeOrigin = new JCheckBox(resources.getString("chkRandomizeOrigin.text"));
        chkRandomizeOrigin.setToolTipText(resources.getString("chkRandomizeOrigin.toolTipText"));
        chkRandomizeOrigin.setName("chkRandomizeOrigin");
        chkRandomizeOrigin.addActionListener(evt -> {
            final boolean selected = chkRandomizeOrigin.isSelected();
            chkRandomizeDependentsOrigin.setEnabled(selected);
            lblOriginSearchRadius.setEnabled(selected);
            spnOriginSearchRadius.setEnabled(selected);
            chkExtraRandomOrigin.setEnabled(selected);
            lblOriginDistanceScale.setEnabled(selected);
            spnOriginDistanceScale.setEnabled(selected);
        });

        chkRandomizeDependentsOrigin = new JCheckBox(resources.getString("chkRandomizeDependentsOrigin.text"));
        chkRandomizeDependentsOrigin.setToolTipText(resources.getString("chkRandomizeDependentsOrigin.toolTipText"));
        chkRandomizeDependentsOrigin.setName("chkRandomizeDependentsOrigin");

        lblOriginSearchRadius.setText(resources.getString("lblOriginSearchRadius.text"));
        lblOriginSearchRadius.setToolTipText(resources.getString("lblOriginSearchRadius.toolTipText"));
        lblOriginSearchRadius.setName("lblOriginSearchRadius");

        spnOriginSearchRadius = new JSpinner(new SpinnerNumberModel(50, 10, 250, 10));
        spnOriginSearchRadius.setToolTipText(resources.getString("lblOriginSearchRadius.toolTipText"));
        spnOriginSearchRadius.setName("spnOriginSearchRadius");

        chkExtraRandomOrigin = new JCheckBox(resources.getString("chkExtraRandomOrigin.text"));
        chkExtraRandomOrigin.setToolTipText(resources.getString("chkExtraRandomOrigin.toolTipText"));
        chkExtraRandomOrigin.setName("chkExtraRandomOrigin");

        lblOriginDistanceScale.setText(resources.getString("lblOriginDistanceScale.text"));
        lblOriginDistanceScale.setToolTipText(resources.getString("lblOriginDistanceScale.toolTipText"));
        lblOriginDistanceScale.setName("lblOriginDistanceScale");

        spnOriginDistanceScale = new JSpinner(new SpinnerNumberModel(0.6, 0.1, 2.0, 0.1));
        spnOriginDistanceScale.setToolTipText(resources.getString("lblOriginDistanceScale.toolTipText"));
        spnOriginDistanceScale.setName("spnOriginDistanceScale");

        // Programmatically Assign Accessibility Labels
        lblOriginSearchRadius.setLabelFor(spnOriginSearchRadius);
        lblOriginDistanceScale.setLabelFor(spnOriginDistanceScale);

        // Disable Panel by Default
        chkRandomizeOrigin.setSelected(true);
        chkRandomizeOrigin.doClick();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomOriginPanel.title")));
        panel.setName("randomOriginPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkRandomizeOrigin)
                        .addComponent(chkRandomizeDependentsOrigin)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(spnOriginSearchRadius, GroupLayout.Alignment.LEADING))
                        .addComponent(chkExtraRandomOrigin)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(spnOriginDistanceScale, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkRandomizeOrigin)
                        .addComponent(chkRandomizeDependentsOrigin)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(spnOriginSearchRadius))
                        .addComponent(chkExtraRandomOrigin)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(spnOriginDistanceScale))
        );

        return panel;
    }

    private JPanel createFamilyPanel() {
        // Create Panel Components
        final JLabel lblDisplayFamilyLevel = new JLabel(resources.getString("lblDisplayFamilyLevel.text"));
        lblDisplayFamilyLevel.setToolTipText(resources.getString("lblDisplayFamilyLevel.toolTipText"));
        lblDisplayFamilyLevel.setName("lblDisplayFamilyLevel");

        comboDisplayFamilyLevel = new JComboBox<>(FamilialRelationshipDisplayLevel.values());
        comboDisplayFamilyLevel.setToolTipText(resources.getString("lblDisplayFamilyLevel.toolTipText"));
        comboDisplayFamilyLevel.setName("comboDisplayFamilyLevel");

        // Programmatically Assign Accessibility Labels
        lblDisplayFamilyLevel.setLabelFor(comboDisplayFamilyLevel);

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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblDisplayFamilyLevel)
                                .addComponent(comboDisplayFamilyLevel, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblDisplayFamilyLevel)
                                .addComponent(comboDisplayFamilyLevel))
        );

        return panel;
    }

    private JPanel createSalaryPanel() {
        // Create Panel Components
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
                        .addComponent(salaryMultiplierPanel)
                        .addComponent(salaryExperienceModifierPanel)
                        .addComponent(baseSalaryPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(salaryMultiplierPanel)
                        .addComponent(salaryExperienceModifierPanel)
                        .addComponent(baseSalaryPanel)
        );

        return panel;
    }

    private JPanel createSalaryMultiplierPanel() {
        // Create Panel Components
        final JLabel lblCommissionedSalary = new JLabel(resources.getString("lblCommissionedSalary.text"));
        lblCommissionedSalary.setToolTipText(resources.getString("lblCommissionedSalary.toolTipText"));
        lblCommissionedSalary.setName("lblCommissionedSalary");

        spnCommissionedSalary = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05));
        spnCommissionedSalary.setToolTipText(resources.getString("lblCommissionedSalary.toolTipText"));
        spnCommissionedSalary.setName("spnCommissionedSalary");

        final JLabel lblEnlistedSalary = new JLabel(resources.getString("lblEnlistedSalary.text"));
        lblEnlistedSalary.setToolTipText(resources.getString("lblEnlistedSalary.toolTipText"));
        lblEnlistedSalary.setName("lblEnlistedSalary");

        spnEnlistedSalary = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05));
        spnEnlistedSalary.setToolTipText(resources.getString("lblEnlistedSalary.toolTipText"));
        spnEnlistedSalary.setName("spnEnlistedSalary");

        final JLabel lblAntiMekSalary = new JLabel(resources.getString("lblAntiMekSalary.text"));
        lblAntiMekSalary.setToolTipText(resources.getString("lblAntiMekSalary.toolTipText"));
        lblAntiMekSalary.setName("lblAntiMekSalary");

        spnAntiMekSalary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.05));
        spnAntiMekSalary.setToolTipText(resources.getString("lblAntiMekSalary.toolTipText"));
        spnAntiMekSalary.setName("spnAntiMekSalary");

        final JLabel lblSpecialistInfantrySalary = new JLabel(resources.getString("lblSpecialistInfantrySalary.text"));
        lblSpecialistInfantrySalary.setToolTipText(resources.getString("lblSpecialistInfantrySalary.toolTipText"));
        lblSpecialistInfantrySalary.setName("lblSpecialistInfantrySalary");

        spnSpecialistInfantrySalary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.05));
        spnSpecialistInfantrySalary.setToolTipText(resources.getString("lblSpecialistInfantrySalary.toolTipText"));
        spnSpecialistInfantrySalary.setName("spnSpecialistInfantrySalary");

        // Programmatically Assign Accessibility Labels
        lblCommissionedSalary.setLabelFor(spnCommissionedSalary);
        lblEnlistedSalary.setLabelFor(spnEnlistedSalary);
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCommissionedSalary)
                                .addComponent(spnCommissionedSalary)
                                .addComponent(lblEnlistedSalary)
                                .addComponent(spnEnlistedSalary)
                                .addComponent(lblAntiMekSalary)
                                .addComponent(spnAntiMekSalary)
                                .addComponent(lblSpecialistInfantrySalary)
                                .addComponent(spnSpecialistInfantrySalary, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCommissionedSalary)
                                .addComponent(spnCommissionedSalary)
                                .addComponent(lblEnlistedSalary)
                                .addComponent(spnEnlistedSalary)
                                .addComponent(lblAntiMekSalary)
                                .addComponent(spnAntiMekSalary)
                                .addComponent(lblSpecialistInfantrySalary)
                                .addComponent(spnSpecialistInfantrySalary))
        );

        return panel;
    }

    private JPanel createSalaryExperienceMultiplierPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 10));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("salaryExperienceMultiplierPanel.title")));
        panel.setToolTipText(resources.getString("salaryExperienceMultiplierPanel.toolTipText"));
        panel.setName("salaryExperienceMultiplierPanel");

        spnSalaryExperienceMultipliers = new JSpinner[5];
        for (int i = 0; i < 5; i++) {
            final String skillLevel = SkillType.getExperienceLevelName(i);
            final String toolTipText = String.format(resources.getString("lblSalaryExperienceMultiplier.toolTipText"), skillLevel);

            final JLabel label = new JLabel(skillLevel);
            label.setToolTipText(toolTipText);
            label.setName("lbl" + skillLevel);
            panel.add(label);

            spnSalaryExperienceMultipliers[i] = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05));
            spnSalaryExperienceMultipliers[i].setToolTipText(toolTipText);
            spnSalaryExperienceMultipliers[i].setName("spn" + skillLevel);
            panel.add(spnSalaryExperienceMultipliers[i]);

            label.setLabelFor(spnSalaryExperienceMultipliers[i]);
        }

        return panel;
    }

    private JPanel createBaseSalaryPanel() {
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        final JPanel panel = new JPanel(new GridLayout((int) Math.ceil((double) (personnelRoles.length - 1) / 3.0), 6));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("baseSalaryPanel.title")));
        panel.setPreferredSize(new Dimension(200, 200));

        spnBaseSalary = new JSpinner[personnelRoles.length];
        for (final PersonnelRole personnelRole : personnelRoles) {
            // Create Reused Values
            final String toolTipText = String.format(resources.getString("lblBaseSalary.toolTipText"), personnelRole.toString());

            // Create Panel Components
            final JLabel label = new JLabel(personnelRole.toString());
            label.setToolTipText(toolTipText);
            label.setName("lbl" + personnelRole.toString());
            panel.add(label);

            final JSpinner salarySpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 10.0));
            salarySpinner.setToolTipText(toolTipText);
            salarySpinner.setName("spn" + personnelRole.toString());
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

        chkUseClannerMarriages = new JCheckBox(resources.getString("chkUseClannerMarriages.text"));
        chkUseClannerMarriages.setToolTipText(resources.getString("chkUseClannerMarriages.toolTipText"));
        chkUseClannerMarriages.setName("chkUseClannerMarriages");
        chkUseClannerMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClannerMarriages.setEnabled(!method.isNone() && chkUseClannerMarriages.isSelected());
        });

        chkUsePrisonerMarriages = new JCheckBox(resources.getString("chkUsePrisonerMarriages.text"));
        chkUsePrisonerMarriages.setToolTipText(resources.getString("chkUsePrisonerMarriages.toolTipText"));
        chkUsePrisonerMarriages.setName("chkUsePrisonerMarriages");
        chkUsePrisonerMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerMarriages.setEnabled(!method.isNone() && chkUsePrisonerMarriages.isSelected());
        });

        final JLabel lblMinimumMarriageAge = new JLabel(resources.getString("lblMinimumMarriageAge.text"));
        lblMinimumMarriageAge.setToolTipText(resources.getString("lblMinimumMarriageAge.toolTipText"));
        lblMinimumMarriageAge.setName("lblMinimumMarriageAge");

        spnMinimumMarriageAge = new JSpinner(new SpinnerNumberModel(16, 14, null, 1));
        spnMinimumMarriageAge.setToolTipText(resources.getString("lblMinimumMarriageAge.toolTipText"));
        spnMinimumMarriageAge.setName("spnMinimumMarriageAge");

        final JLabel lblCheckMutualAncestorsDepth = new JLabel(resources.getString("lblCheckMutualAncestorsDepth.text"));
        lblCheckMutualAncestorsDepth.setToolTipText(resources.getString("lblCheckMutualAncestorsDepth.toolTipText"));
        lblCheckMutualAncestorsDepth.setName("lblCheckMutualAncestorsDepth");

        spnCheckMutualAncestorsDepth = new JSpinner(new SpinnerNumberModel(4, 0, 20, 1));
        spnCheckMutualAncestorsDepth.setToolTipText(resources.getString("lblCheckMutualAncestorsDepth.toolTipText"));
        spnCheckMutualAncestorsDepth.setName("spnCheckMutualAncestorsDepth");

        chkLogMarriageNameChanges = new JCheckBox(resources.getString("chkLogMarriageNameChanges.text"));
        chkLogMarriageNameChanges.setToolTipText(resources.getString("chkLogMarriageNameChanges.toolTipText"));
        chkLogMarriageNameChanges.setName("chkLogMarriageNameChanges");

        final JPanel marriageSurnameWeightsPanel = createMarriageSurnameWeightsPanel();

        final JPanel randomMarriagePanel = createRandomMarriagePanel();

        // Programmatically Assign Accessibility Labels
        lblMinimumMarriageAge.setLabelFor(spnMinimumMarriageAge);
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
                        .addComponent(chkUseClannerMarriages)
                        .addComponent(chkUsePrisonerMarriages)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMinimumMarriageAge)
                                .addComponent(spnMinimumMarriageAge, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCheckMutualAncestorsDepth)
                                .addComponent(spnCheckMutualAncestorsDepth, GroupLayout.Alignment.LEADING))
                        .addComponent(chkLogMarriageNameChanges)
                        .addComponent(marriageSurnameWeightsPanel)
                        .addComponent(randomMarriagePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseManualMarriages)
                        .addComponent(chkUseClannerMarriages)
                        .addComponent(chkUsePrisonerMarriages)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMinimumMarriageAge)
                                .addComponent(spnMinimumMarriageAge))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCheckMutualAncestorsDepth)
                                .addComponent(spnCheckMutualAncestorsDepth))
                        .addComponent(chkLogMarriageNameChanges)
                        .addComponent(marriageSurnameWeightsPanel)
                        .addComponent(randomMarriagePanel)
        );

        return panel;
    }

    private JPanel createMarriageSurnameWeightsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("marriageSurnameWeightsPanel.title")));
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

        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod", RandomMarriageMethod.values());
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
            final boolean sameSexEnabled = enabled && chkUseRandomSameSexMarriages.isSelected();
            final boolean percentageEnabled = method.isPercentage();
            chkUseRandomSameSexMarriages.setEnabled(enabled);
            chkUseRandomClannerMarriages.setEnabled(enabled && chkUseClannerMarriages.isSelected());
            chkUseRandomPrisonerMarriages.setEnabled(enabled && chkUsePrisonerMarriages.isSelected());
            lblRandomMarriageAgeRange.setEnabled(enabled);
            spnRandomMarriageAgeRange.setEnabled(enabled);
            percentageRandomMarriagePanel.setEnabled(percentageEnabled);
            lblPercentageRandomMarriageSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
            spnPercentageRandomMarriageSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
        });

        chkUseRandomSameSexMarriages = new JCheckBox(resources.getString("chkUseRandomSameSexMarriages.text"));
        chkUseRandomSameSexMarriages.setToolTipText(resources.getString("chkUseRandomSameSexMarriages.toolTipText"));
        chkUseRandomSameSexMarriages.setName("chkUseRandomSameSexMarriages");
        chkUseRandomSameSexMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean sameSexEnabled = chkUseRandomSameSexMarriages.isEnabled()
                    && chkUseRandomSameSexMarriages.isSelected();
            final boolean percentageEnabled = sameSexEnabled && method.isPercentage();
            lblPercentageRandomMarriageSameSexChance.setEnabled(percentageEnabled);
            spnPercentageRandomMarriageSameSexChance.setEnabled(percentageEnabled);
        });

        chkUseRandomClannerMarriages = new JCheckBox(resources.getString("chkUseRandomClannerMarriages.text"));
        chkUseRandomClannerMarriages.setToolTipText(resources.getString("chkUseRandomClannerMarriages.toolTipText"));
        chkUseRandomClannerMarriages.setName("chkUseRandomClannerMarriages");

        chkUseRandomPrisonerMarriages = new JCheckBox(resources.getString("chkUseRandomPrisonerMarriages.text"));
        chkUseRandomPrisonerMarriages.setToolTipText(resources.getString("chkUseRandomPrisonerMarriages.toolTipText"));
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomMarriageMethod)
                                .addComponent(comboRandomMarriageMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRandomSameSexMarriages)
                        .addComponent(chkUseRandomClannerMarriages)
                        .addComponent(chkUseRandomPrisonerMarriages)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomMarriageAgeRange)
                                .addComponent(spnRandomMarriageAgeRange, GroupLayout.Alignment.LEADING))
                        .addComponent(percentageRandomMarriagePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageMethod)
                                .addComponent(comboRandomMarriageMethod))
                        .addComponent(chkUseRandomSameSexMarriages)
                        .addComponent(chkUseRandomClannerMarriages)
                        .addComponent(chkUseRandomPrisonerMarriages)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageAgeRange)
                                .addComponent(spnRandomMarriageAgeRange))
                        .addComponent(percentageRandomMarriagePanel)
        );

        return panel;
    }

    private void createPercentageRandomMarriagePanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblPercentageRandomMarriageOppositeSexChance = new JLabel(resources.getString("lblPercentageRandomMarriageOppositeSexChance.text"));
        lblPercentageRandomMarriageOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageOppositeSexChance.toolTipText"));
        lblPercentageRandomMarriageOppositeSexChance.setName("lblPercentageRandomMarriageOppositeSexChance");

        spnPercentageRandomMarriageOppositeSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomMarriageOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageOppositeSexChance.toolTipText"));
        spnPercentageRandomMarriageOppositeSexChance.setName("spnPercentageRandomMarriageOppositeSexChance");

        lblPercentageRandomMarriageSameSexChance = new JLabel(resources.getString("lblPercentageRandomMarriageSameSexChance.text"));
        lblPercentageRandomMarriageSameSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageSameSexChance.toolTipText"));
        lblPercentageRandomMarriageSameSexChance.setName("lblPercentageRandomMarriageSameSexChance");

        spnPercentageRandomMarriageSameSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomMarriageSameSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageSameSexChance.toolTipText"));
        spnPercentageRandomMarriageSameSexChance.setName("spnPercentageRandomMarriageSameSexChance");

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomMarriageOppositeSexChance.setLabelFor(spnPercentageRandomMarriageOppositeSexChance);
        lblPercentageRandomMarriageSameSexChance.setLabelFor(spnPercentageRandomMarriageSameSexChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomMarriagePanel.title")));
        panel.setToolTipText(RandomMarriageMethod.PERCENTAGE.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomMarriageOppositeSexChance)
                                .addComponent(spnPercentageRandomMarriageOppositeSexChance, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomMarriageSameSexChance)
                                .addComponent(spnPercentageRandomMarriageSameSexChance, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomMarriageOppositeSexChance)
                                .addComponent(spnPercentageRandomMarriageOppositeSexChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomMarriageSameSexChance)
                                .addComponent(spnPercentageRandomMarriageSameSexChance))
        );
    }

    private JPanel createDivorcePanel() {
        // Create Panel Components
        chkUseManualDivorce = new JCheckBox(resources.getString("chkUseManualDivorce.text"));
        chkUseManualDivorce.setToolTipText(resources.getString("chkUseManualDivorce.toolTipText"));
        chkUseManualDivorce.setName("chkUseManualDivorce");

        chkUseClannerDivorce = new JCheckBox(resources.getString("chkUseClannerDivorce.text"));
        chkUseClannerDivorce.setToolTipText(resources.getString("chkUseClannerDivorce.toolTipText"));
        chkUseClannerDivorce.setName("chkUseClannerDivorce");
        chkUseClannerDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClannerDivorce.setEnabled(!method.isNone() && chkUseClannerDivorce.isSelected());
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
                        .addComponent(chkUseClannerDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(divorceSurnameWeightsPanel)
                        .addComponent(randomDivorcePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseManualDivorce)
                        .addComponent(chkUseClannerDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(divorceSurnameWeightsPanel)
                        .addComponent(randomDivorcePanel)
        );

        return panel;
    }

    private JPanel createDivorceSurnameWeightsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("divorceSurnameWeightsPanel.title")));
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
            final boolean oppositeSexEnabled = enabled && chkUseRandomOppositeSexDivorce.isSelected();
            final boolean sameSexEnabled = enabled && chkUseRandomSameSexDivorce.isSelected();
            final boolean percentageEnabled = method.isPercentage();
            chkUseRandomOppositeSexDivorce.setEnabled(enabled);
            chkUseRandomSameSexDivorce.setEnabled(enabled);
            chkUseRandomClannerDivorce.setEnabled(enabled && chkUseClannerDivorce.isSelected());
            chkUseRandomPrisonerDivorce.setEnabled(enabled && chkUsePrisonerDivorce.isSelected());
            percentageRandomDivorcePanel.setEnabled(percentageEnabled);
            lblPercentageRandomDivorceOppositeSexChance.setEnabled(oppositeSexEnabled && percentageEnabled);
            spnPercentageRandomDivorceOppositeSexChance.setEnabled(oppositeSexEnabled && percentageEnabled);
            lblPercentageRandomDivorceSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
            spnPercentageRandomDivorceSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
        });

        chkUseRandomOppositeSexDivorce = new JCheckBox(resources.getString("chkUseRandomOppositeSexDivorce.text"));
        chkUseRandomOppositeSexDivorce.setToolTipText(resources.getString("chkUseRandomOppositeSexDivorce.toolTipText"));
        chkUseRandomOppositeSexDivorce.setName("chkUseRandomOppositeSexDivorce");
        chkUseRandomOppositeSexDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean selected = chkUseRandomOppositeSexDivorce.isEnabled()
                    && chkUseRandomOppositeSexDivorce.isSelected();
            final boolean percentageEnabled = selected && method.isPercentage();
            lblPercentageRandomDivorceOppositeSexChance.setEnabled(percentageEnabled);
            spnPercentageRandomDivorceOppositeSexChance.setEnabled(percentageEnabled);
        });

        chkUseRandomSameSexDivorce = new JCheckBox(resources.getString("chkUseRandomSameSexDivorce.text"));
        chkUseRandomSameSexDivorce.setToolTipText(resources.getString("chkUseRandomSameSexDivorce.toolTipText"));
        chkUseRandomSameSexDivorce.setName("chkUseRandomSameSexDivorce");
        chkUseRandomSameSexDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean selected = chkUseRandomSameSexDivorce.isEnabled()
                    && chkUseRandomSameSexDivorce.isSelected();
            final boolean percentageEnabled = selected && method.isPercentage();
            lblPercentageRandomDivorceSameSexChance.setEnabled(percentageEnabled);
            spnPercentageRandomDivorceSameSexChance.setEnabled(percentageEnabled);
        });

        chkUseRandomClannerDivorce = new JCheckBox(resources.getString("chkUseRandomClannerDivorce.text"));
        chkUseRandomClannerDivorce.setToolTipText(resources.getString("chkUseRandomClannerDivorce.toolTipText"));
        chkUseRandomClannerDivorce.setName("chkUseRandomClannerDivorce");

        chkUseRandomPrisonerDivorce = new JCheckBox(resources.getString("chkUseRandomPrisonerDivorce.text"));
        chkUseRandomPrisonerDivorce.setToolTipText(resources.getString("chkUseRandomPrisonerDivorce.toolTipText"));
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomDivorceMethod)
                                .addComponent(comboRandomDivorceMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRandomOppositeSexDivorce)
                        .addComponent(chkUseRandomSameSexDivorce)
                        .addComponent(chkUseRandomClannerDivorce)
                        .addComponent(chkUseRandomPrisonerDivorce)
                        .addComponent(percentageRandomDivorcePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomDivorceMethod)
                                .addComponent(comboRandomDivorceMethod))
                        .addComponent(chkUseRandomOppositeSexDivorce)
                        .addComponent(chkUseRandomSameSexDivorce)
                        .addComponent(chkUseRandomClannerDivorce)
                        .addComponent(chkUseRandomPrisonerDivorce)
                        .addComponent(percentageRandomDivorcePanel)
        );

        return panel;
    }

    private void createPercentageRandomDivorcePanel(final JPanel panel) {
        // Create Panel Components
        lblPercentageRandomDivorceOppositeSexChance = new JLabel(resources.getString("lblPercentageRandomDivorceOppositeSexChance.text"));
        lblPercentageRandomDivorceOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceOppositeSexChance.toolTipText"));
        lblPercentageRandomDivorceOppositeSexChance.setName("lblPercentageRandomDivorceOppositeSexChance");

        spnPercentageRandomDivorceOppositeSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.00001));
        spnPercentageRandomDivorceOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceOppositeSexChance.toolTipText"));
        spnPercentageRandomDivorceOppositeSexChance.setName("spnPercentageRandomDivorceOppositeSexChance");
        spnPercentageRandomDivorceOppositeSexChance.setEditor(new JSpinner.NumberEditor(spnPercentageRandomDivorceOppositeSexChance, "0.00000"));

        lblPercentageRandomDivorceSameSexChance = new JLabel(resources.getString("lblPercentageRandomDivorceSameSexChance.text"));
        lblPercentageRandomDivorceSameSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceSameSexChance.toolTipText"));
        lblPercentageRandomDivorceSameSexChance.setName("lblPercentageRandomDivorceSameSexChance");

        spnPercentageRandomDivorceSameSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.00001));
        spnPercentageRandomDivorceSameSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceSameSexChance.toolTipText"));
        spnPercentageRandomDivorceSameSexChance.setName("spnPercentageRandomDivorceSameSexChance");
        spnPercentageRandomDivorceSameSexChance.setEditor(new JSpinner.NumberEditor(spnPercentageRandomDivorceSameSexChance, "0.00000"));

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomDivorceOppositeSexChance.setLabelFor(spnPercentageRandomDivorceOppositeSexChance);
        lblPercentageRandomDivorceSameSexChance.setLabelFor(spnPercentageRandomDivorceSameSexChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomDivorcePanel.title")));
        panel.setToolTipText(RandomDivorceMethod.PERCENTAGE.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomDivorceOppositeSexChance)
                                .addComponent(spnPercentageRandomDivorceOppositeSexChance, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomDivorceSameSexChance)
                                .addComponent(spnPercentageRandomDivorceSameSexChance, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomDivorceOppositeSexChance)
                                .addComponent(spnPercentageRandomDivorceOppositeSexChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomDivorceSameSexChance)
                                .addComponent(spnPercentageRandomDivorceSameSexChance))
        );
    }

    private JPanel createProcreationPanel() {
        // Create Panel Components
        chkUseManualProcreation = new JCheckBox(resources.getString("chkUseManualProcreation.text"));
        chkUseManualProcreation.setToolTipText(resources.getString("chkUseManualProcreation.toolTipText"));
        chkUseManualProcreation.setName("chkUseManualProcreation");

        chkUseClannerProcreation = new JCheckBox(resources.getString("chkUseClannerProcreation.text"));
        chkUseClannerProcreation.setToolTipText(resources.getString("chkUseClannerProcreation.toolTipText"));
        chkUseClannerProcreation.setName("chkUseClannerProcreation");
        chkUseClannerProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClannerProcreation.setEnabled(!method.isNone() && chkUseClannerProcreation.isSelected());
        });

        chkUsePrisonerProcreation = new JCheckBox(resources.getString("chkUsePrisonerProcreation.text"));
        chkUsePrisonerProcreation.setToolTipText(resources.getString("chkUsePrisonerProcreation.toolTipText"));
        chkUsePrisonerProcreation.setName("chkUsePrisonerProcreation");
        chkUsePrisonerProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerProcreation.setEnabled(!method.isNone() && chkUsePrisonerProcreation.isSelected());
        });

        final JLabel lblMultiplePregnancyOccurrences = new JLabel(resources.getString("lblMultiplePregnancyOccurrences.text"));
        lblMultiplePregnancyOccurrences.setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        lblMultiplePregnancyOccurrences.setName("lblMultiplePregnancyOccurrences");

        spnMultiplePregnancyOccurrences = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        spnMultiplePregnancyOccurrences.setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        spnMultiplePregnancyOccurrences.setName("spnMultiplePregnancyOccurrences");

        final JLabel lblMultiplePregnancyOccurrencesEnd = new JLabel(resources.getString("lblMultiplePregnancyOccurrencesEnd.text"));
        lblMultiplePregnancyOccurrencesEnd.setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
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

        chkAssignNonPrisonerBabiesFounderTag = new JCheckBox(resources.getString("chkAssignNonPrisonerBabiesFounderTag.text"));
        chkAssignNonPrisonerBabiesFounderTag.setToolTipText(resources.getString("chkAssignNonPrisonerBabiesFounderTag.toolTipText"));
        chkAssignNonPrisonerBabiesFounderTag.setName("chkAssignNonPrisonerBabiesFounderTag");

        chkAssignChildrenOfFoundersFounderTag = new JCheckBox(resources.getString("chkAssignChildrenOfFoundersFounderTag.text"));
        chkAssignChildrenOfFoundersFounderTag.setToolTipText(resources.getString("chkAssignChildrenOfFoundersFounderTag.toolTipText"));
        chkAssignChildrenOfFoundersFounderTag.setName("chkAssignChildrenOfFoundersFounderTag");

        chkDetermineFatherAtBirth = new JCheckBox(resources.getString("chkDetermineFatherAtBirth.text"));
        chkDetermineFatherAtBirth.setToolTipText(resources.getString("chkDetermineFatherAtBirth.toolTipText"));
        chkDetermineFatherAtBirth.setName("chkDetermineFatherAtBirth");

        chkDisplayTrueDueDate = new JCheckBox(resources.getString("chkDisplayTrueDueDate.text"));
        chkDisplayTrueDueDate.setToolTipText(resources.getString("chkDisplayTrueDueDate.toolTipText"));
        chkDisplayTrueDueDate.setName("chkDisplayTrueDueDate");

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
                        .addComponent(chkUseClannerProcreation)
                        .addComponent(chkUsePrisonerProcreation)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMultiplePregnancyOccurrences)
                                .addComponent(spnMultiplePregnancyOccurrences)
                                .addComponent(lblMultiplePregnancyOccurrencesEnd, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblBabySurnameStyle)
                                .addComponent(comboBabySurnameStyle, GroupLayout.Alignment.LEADING))
                        .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                        .addComponent(chkAssignChildrenOfFoundersFounderTag)
                        .addComponent(chkDetermineFatherAtBirth)
                        .addComponent(chkDisplayTrueDueDate)
                        .addComponent(chkLogProcreation)
                        .addComponent(randomProcreationPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseManualProcreation)
                        .addComponent(chkUseClannerProcreation)
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
                        .addComponent(chkLogProcreation)
                        .addComponent(randomProcreationPanel)
        );

        return panel;
    }

    private JPanel createRandomProcreationPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel percentageRandomProcreationPanel = new JDisableablePanel("percentageRandomProcreationPanel");

        // Create Panel Components
        final JLabel lblRandomProcreationMethod = new JLabel(resources.getString("lblRandomProcreationMethod.text"));
        lblRandomProcreationMethod.setToolTipText(resources.getString("lblRandomProcreationMethod.toolTipText"));
        lblRandomProcreationMethod.setName("lblRandomProcreationMethod");

        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod", RandomProcreationMethod.values());
        comboRandomProcreationMethod.setToolTipText(resources.getString("lblRandomProcreationMethod.toolTipText"));
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
            final boolean percentageEnabled = method.isPercentage();
            final boolean relationshiplessEnabled = enabled && chkUseRelationshiplessRandomProcreation.isSelected();
            chkUseRelationshiplessRandomProcreation.setEnabled(enabled);
            chkUseRandomClannerProcreation.setEnabled(enabled && chkUseClannerProcreation.isSelected());
            chkUseRandomPrisonerProcreation.setEnabled(enabled && chkUsePrisonerProcreation.isSelected());
            percentageRandomProcreationPanel.setEnabled(percentageEnabled);
            lblPercentageRandomProcreationRelationshiplessChance.setEnabled(relationshiplessEnabled && percentageEnabled);
            spnPercentageRandomProcreationRelationshiplessChance.setEnabled(relationshiplessEnabled && percentageEnabled);
        });

        chkUseRelationshiplessRandomProcreation = new JCheckBox(resources.getString("chkUseRelationshiplessRandomProcreation.text"));
        chkUseRelationshiplessRandomProcreation.setToolTipText(resources.getString("chkUseRelationshiplessRandomProcreation.toolTipText"));
        chkUseRelationshiplessRandomProcreation.setName("chkUseRelationshiplessRandomProcreation");
        chkUseRelationshiplessRandomProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean sameSexEnabled = chkUseRelationshiplessRandomProcreation.isEnabled()
                    && chkUseRelationshiplessRandomProcreation.isSelected();
            final boolean percentageEnabled = sameSexEnabled && method.isPercentage();
            lblPercentageRandomProcreationRelationshiplessChance.setEnabled(percentageEnabled);
            spnPercentageRandomProcreationRelationshiplessChance.setEnabled(percentageEnabled);
        });

        chkUseRandomClannerProcreation = new JCheckBox(resources.getString("chkUseRandomClannerProcreation.text"));
        chkUseRandomClannerProcreation.setToolTipText(resources.getString("chkUseRandomClannerProcreation.toolTipText"));
        chkUseRandomClannerProcreation.setName("chkUseRandomClannerProcreation");

        chkUseRandomPrisonerProcreation = new JCheckBox(resources.getString("chkUseRandomPrisonerProcreation.text"));
        chkUseRandomPrisonerProcreation.setToolTipText(resources.getString("chkUseRandomPrisonerProcreation.toolTipText"));
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomProcreationMethod)
                                .addComponent(comboRandomProcreationMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRelationshiplessRandomProcreation)
                        .addComponent(chkUseRandomClannerProcreation)
                        .addComponent(chkUseRandomPrisonerProcreation)
                        .addComponent(percentageRandomProcreationPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomProcreationMethod)
                                .addComponent(comboRandomProcreationMethod))
                        .addComponent(chkUseRelationshiplessRandomProcreation)
                        .addComponent(chkUseRandomClannerProcreation)
                        .addComponent(chkUseRandomPrisonerProcreation)
                        .addComponent(percentageRandomProcreationPanel)
        );

        return panel;
    }

    private void createPercentageRandomProcreationPanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblPercentageRandomProcreationRelationshipChance = new JLabel(resources.getString("lblPercentageRandomProcreationRelationshipChance.text"));
        lblPercentageRandomProcreationRelationshipChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshipChance.toolTipText"));
        lblPercentageRandomProcreationRelationshipChance.setName("lblPercentageRandomProcreationRelationshipChance");

        spnPercentageRandomProcreationRelationshipChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomProcreationRelationshipChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshipChance.toolTipText"));
        spnPercentageRandomProcreationRelationshipChance.setName("spnChanceProcreation");

        lblPercentageRandomProcreationRelationshiplessChance = new JLabel(resources.getString("lblPercentageRandomProcreationRelationshiplessChance.text"));
        lblPercentageRandomProcreationRelationshiplessChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshiplessChance.toolTipText"));
        lblPercentageRandomProcreationRelationshiplessChance.setName("lblPercentageRandomProcreationRelationshiplessChance");

        spnPercentageRandomProcreationRelationshiplessChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomProcreationRelationshiplessChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshiplessChance.toolTipText"));
        spnPercentageRandomProcreationRelationshiplessChance.setName("spnPercentageRandomProcreationRelationshiplessChance");

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomProcreationRelationshipChance.setLabelFor(spnPercentageRandomProcreationRelationshipChance);
        lblPercentageRandomProcreationRelationshiplessChance.setLabelFor(spnPercentageRandomProcreationRelationshiplessChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomProcreationPanel.title")));
        panel.setToolTipText(RandomProcreationMethod.PERCENTAGE.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomProcreationRelationshipChance)
                                .addComponent(spnPercentageRandomProcreationRelationshipChance, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomProcreationRelationshiplessChance)
                                .addComponent(spnPercentageRandomProcreationRelationshiplessChance, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomProcreationRelationshipChance)
                                .addComponent(spnPercentageRandomProcreationRelationshipChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomProcreationRelationshiplessChance)
                                .addComponent(spnPercentageRandomProcreationRelationshiplessChance))
        );
    }

    private JPanel createDeathPanel() {
        // Create Panel Components
        chkKeepMarriedNameUponSpouseDeath = new JCheckBox(resources.getString("chkKeepMarriedNameUponSpouseDeath.text"));
        chkKeepMarriedNameUponSpouseDeath.setToolTipText(resources.getString("chkKeepMarriedNameUponSpouseDeath.toolTipText"));
        chkKeepMarriedNameUponSpouseDeath.setName("chkKeepMarriedNameUponSpouseDeath");

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
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkKeepMarriedNameUponSpouseDeath)
        );

        return panel;
    }
    //endregion Personnel Tab

    //region Finances Tab
    private JPanel createPriceModifiersPanel() {
        // Create Panel Components
        final JLabel lblCommonPartPriceMultiplier = new JLabel(resources.getString("lblCommonPartPriceMultiplier.text"));
        lblCommonPartPriceMultiplier.setToolTipText(resources.getString("lblCommonPartPriceMultiplier.toolTipText"));
        lblCommonPartPriceMultiplier.setName("lblCommonPartPriceMultiplier");

        spnCommonPartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnCommonPartPriceMultiplier.setToolTipText(resources.getString("lblCommonPartPriceMultiplier.toolTipText"));
        spnCommonPartPriceMultiplier.setName("spnCommonPartPriceMultiplier");

        final JLabel lblInnerSphereUnitPriceMultiplier = new JLabel(resources.getString("lblInnerSphereUnitPriceMultiplier.text"));
        lblInnerSphereUnitPriceMultiplier.setToolTipText(resources.getString("lblInnerSphereUnitPriceMultiplier.toolTipText"));
        lblInnerSphereUnitPriceMultiplier.setName("lblInnerSphereUnitPriceMultiplier");

        spnInnerSphereUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnInnerSphereUnitPriceMultiplier.setToolTipText(resources.getString("lblInnerSphereUnitPriceMultiplier.toolTipText"));
        spnInnerSphereUnitPriceMultiplier.setName("spnInnerSphereUnitPriceMultiplier");

        final JLabel lblInnerSpherePartPriceMultiplier = new JLabel(resources.getString("lblInnerSpherePartPriceMultiplier.text"));
        lblInnerSpherePartPriceMultiplier.setToolTipText(resources.getString("lblInnerSpherePartPriceMultiplier.toolTipText"));
        lblInnerSpherePartPriceMultiplier.setName("lblInnerSpherePartPriceMultiplier");

        spnInnerSpherePartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnInnerSpherePartPriceMultiplier.setToolTipText(resources.getString("lblInnerSpherePartPriceMultiplier.toolTipText"));
        spnInnerSpherePartPriceMultiplier.setName("spnInnerSpherePartPriceMultiplier");

        final JLabel lblClanUnitPriceMultiplier = new JLabel(resources.getString("lblClanUnitPriceMultiplier.text"));
        lblClanUnitPriceMultiplier.setToolTipText(resources.getString("lblClanUnitPriceMultiplier.toolTipText"));
        lblClanUnitPriceMultiplier.setName("lblClanUnitPriceMultiplier");

        spnClanUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnClanUnitPriceMultiplier.setToolTipText(resources.getString("lblClanUnitPriceMultiplier.toolTipText"));
        spnClanUnitPriceMultiplier.setName("spnClanUnitPriceMultiplier");

        final JLabel lblClanPartPriceMultiplier = new JLabel(resources.getString("lblClanPartPriceMultiplier.text"));
        lblClanPartPriceMultiplier.setToolTipText(resources.getString("lblClanPartPriceMultiplier.toolTipText"));
        lblClanPartPriceMultiplier.setName("lblClanPartPriceMultiplier");

        spnClanPartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnClanPartPriceMultiplier.setToolTipText(resources.getString("lblClanPartPriceMultiplier.toolTipText"));
        spnClanPartPriceMultiplier.setName("spnClanPartPriceMultiplier");

        final JLabel lblMixedTechUnitPriceMultiplier = new JLabel(resources.getString("lblMixedTechUnitPriceMultiplier.text"));
        lblMixedTechUnitPriceMultiplier.setToolTipText(resources.getString("lblMixedTechUnitPriceMultiplier.toolTipText"));
        lblMixedTechUnitPriceMultiplier.setName("lblMixedTechUnitPriceMultiplier");

        spnMixedTechUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnMixedTechUnitPriceMultiplier.setToolTipText(resources.getString("lblMixedTechUnitPriceMultiplier.toolTipText"));
        spnMixedTechUnitPriceMultiplier.setName("spnMixedTechUnitPriceMultiplier");

        final JPanel usedPartsValueMultipliersPanel = createUsedPartsValueMultipliersPanel();

        final JLabel lblDamagedPartsValueMultiplier = new JLabel(resources.getString("lblDamagedPartsValueMultiplier.text"));
        lblDamagedPartsValueMultiplier.setToolTipText(resources.getString("lblDamagedPartsValueMultiplier.toolTipText"));
        lblDamagedPartsValueMultiplier.setName("lblDamagedPartsValueMultiplier");

        spnDamagedPartsValueMultiplier = new JSpinner(new SpinnerNumberModel(0.33, 0.00, 1.00, 0.05));
        spnDamagedPartsValueMultiplier.setToolTipText(resources.getString("lblDamagedPartsValueMultiplier.toolTipText"));
        spnDamagedPartsValueMultiplier.setName("spnDamagedPartsValueMultiplier");
        spnDamagedPartsValueMultiplier.setEditor(new JSpinner.NumberEditor(spnDamagedPartsValueMultiplier, "0.00"));

        final JLabel lblUnrepairablePartsValueMultiplier = new JLabel(resources.getString("lblUnrepairablePartsValueMultiplier.text"));
        lblUnrepairablePartsValueMultiplier.setToolTipText(resources.getString("lblUnrepairablePartsValueMultiplier.toolTipText"));
        lblUnrepairablePartsValueMultiplier.setName("lblUnrepairablePartsValueMultiplier");

        spnUnrepairablePartsValueMultiplier = new JSpinner(new SpinnerNumberModel(0.10, 0.00, 1.00, 0.05));
        spnUnrepairablePartsValueMultiplier.setToolTipText(resources.getString("lblUnrepairablePartsValueMultiplier.toolTipText"));
        spnUnrepairablePartsValueMultiplier.setName("spnUnrepairablePartsValueMultiplier");
        spnUnrepairablePartsValueMultiplier.setEditor(new JSpinner.NumberEditor(spnUnrepairablePartsValueMultiplier, "0.00"));

        final JLabel lblCancelledOrderRefundMultiplier = new JLabel(resources.getString("lblCancelledOrderRefundMultiplier.text"));
        lblCancelledOrderRefundMultiplier.setToolTipText(resources.getString("lblCancelledOrderRefundMultiplier.toolTipText"));
        lblCancelledOrderRefundMultiplier.setName("lblCancelledOrderRefundMultiplier");

        spnCancelledOrderRefundMultiplier = new JSpinner(new SpinnerNumberModel(0.50, 0.00, 1.00, 0.05));
        spnCancelledOrderRefundMultiplier.setToolTipText(resources.getString("lblCancelledOrderRefundMultiplier.toolTipText"));
        spnCancelledOrderRefundMultiplier.setName("spnCancelledOrderRefundMultiplier");
        spnCancelledOrderRefundMultiplier.setEditor(new JSpinner.NumberEditor(spnCancelledOrderRefundMultiplier, "0.00"));

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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCommonPartPriceMultiplier)
                                .addComponent(spnCommonPartPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblInnerSphereUnitPriceMultiplier)
                                .addComponent(spnInnerSphereUnitPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblInnerSpherePartPriceMultiplier)
                                .addComponent(spnInnerSpherePartPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblClanUnitPriceMultiplier)
                                .addComponent(spnClanUnitPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblClanPartPriceMultiplier)
                                .addComponent(spnClanPartPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMixedTechUnitPriceMultiplier)
                                .addComponent(spnMixedTechUnitPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addComponent(usedPartsValueMultipliersPanel)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblDamagedPartsValueMultiplier)
                                .addComponent(spnDamagedPartsValueMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblUnrepairablePartsValueMultiplier)
                                .addComponent(spnUnrepairablePartsValueMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCancelledOrderRefundMultiplier)
                                .addComponent(spnCancelledOrderRefundMultiplier, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
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
                                .addComponent(spnCancelledOrderRefundMultiplier))
        );

        return panel;
    }

    private JPanel createUsedPartsValueMultipliersPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("usedPartsValueMultipliersPanel.title")));
        panel.setName("usedPartsValueMultipliersPanel");

        spnUsedPartPriceMultipliers = new JSpinner[Part.QUALITY_F + 1];
        for (int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            final String qualityLevel = Part.getQualityName(i, false);

            final JLabel label = new JLabel(qualityLevel);
            label.setToolTipText(resources.getString("lblUsedPartPriceMultiplier.toolTipText"));
            label.setName("lbl" + qualityLevel);
            panel.add(label);

            spnUsedPartPriceMultipliers[i] = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[i].setToolTipText(resources.getString("lblUsedPartPriceMultiplier.toolTipText"));
            spnUsedPartPriceMultipliers[i].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[i].setEditor(new JSpinner.NumberEditor(spnUsedPartPriceMultipliers[i], "0.00"));
            panel.add(spnUsedPartPriceMultipliers[i]);

            label.setLabelFor(spnUsedPartPriceMultipliers[i]);
        }

        return panel;
    }
    //endregion Finances Tab

    //region Rank Systems Tab
    private JScrollPane createRankSystemsTab() {
        rankSystemsPane = new RankSystemsPane(getFrame(), getCampaign());
        return rankSystemsPane;
    }
    //endregion Rank Systems Tab

    //region Markets Tab
    private JScrollPane createMarketsTab() {
        final JPanel marketsPanel = new JPanel(new GridBagLayout());
        marketsPanel.setName("marketsPanel");

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
        final JLabel lblPersonnelMarketRandomEliteRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomVeteranRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomRegularRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomGreenRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomUltraGreenRemoval = new JLabel();
        final JLabel lblPersonnelMarketDylansWeight = new JLabel();

        // Create Panel Components
        final JLabel lblPersonnelMarketType = new JLabel(resources.getString("lblPersonnelMarket.text"));
        lblPersonnelMarketType.setToolTipText(resources.getString("lblPersonnelMarketType.toolTipText"));
        lblPersonnelMarketType.setName("lblPersonnelMarketType");

        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance().getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        comboPersonnelMarketType = new JComboBox<>(personnelMarketTypeModel);
        comboPersonnelMarketType.setToolTipText(resources.getString("lblPersonnelMarketType.toolTipText"));
        comboPersonnelMarketType.setName("comboPersonnelMarketType");
        comboPersonnelMarketType.addActionListener(evt -> {
            final boolean isDylan = new PersonnelMarketDylan().getModuleName().equals(comboPersonnelMarketType.getSelectedItem());
            final boolean enabled = isDylan || new PersonnelMarketRandom().getModuleName().equals(comboPersonnelMarketType.getSelectedItem());
            lblPersonnelMarketRandomEliteRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomEliteRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomVeteranRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomVeteranRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomRegularRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomRegularRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomGreenRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomGreenRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomUltraGreenRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomUltraGreenRemoval.setEnabled(enabled);
            lblPersonnelMarketDylansWeight.setEnabled(isDylan);
            spnPersonnelMarketDylansWeight.setEnabled(isDylan);
        });

        chkPersonnelMarketReportRefresh = new JCheckBox(resources.getString("chkPersonnelMarketReportRefresh.text"));
        chkPersonnelMarketReportRefresh.setToolTipText(resources.getString("chkPersonnelMarketReportRefresh.toolTipText"));
        chkPersonnelMarketReportRefresh.setName("chkPersonnelMarketReportRefresh");

        lblPersonnelMarketRandomEliteRemoval.setText(resources.getString("lblPersonnelMarketRandomEliteRemoval.text"));
        lblPersonnelMarketRandomEliteRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomEliteRemoval.toolTipText"));
        lblPersonnelMarketRandomEliteRemoval.setName("lblPersonnelMarketRandomEliteRemoval");

        spnPersonnelMarketRandomEliteRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomEliteRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomEliteRemoval.toolTipText"));
        spnPersonnelMarketRandomEliteRemoval.setName("spnPersonnelMarketRandomEliteRemoval");

        lblPersonnelMarketRandomVeteranRemoval.setText(resources.getString("lblPersonnelMarketRandomVeteranRemoval.text"));
        lblPersonnelMarketRandomVeteranRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomVeteranRemoval.toolTipText"));
        lblPersonnelMarketRandomVeteranRemoval.setName("lblPersonnelMarketRandomVeteranRemoval");

        spnPersonnelMarketRandomVeteranRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomVeteranRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomVeteranRemoval.toolTipText"));
        spnPersonnelMarketRandomVeteranRemoval.setName("spnPersonnelMarketRandomVeteranRemoval");

        lblPersonnelMarketRandomRegularRemoval.setText(resources.getString("lblPersonnelMarketRandomRegularRemoval.text"));
        lblPersonnelMarketRandomRegularRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomRegularRemoval.toolTipText"));
        lblPersonnelMarketRandomRegularRemoval.setName("lblPersonnelMarketRandomRegularRemoval");

        spnPersonnelMarketRandomRegularRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomRegularRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomRegularRemoval.toolTipText"));
        spnPersonnelMarketRandomRegularRemoval.setName("spnPersonnelMarketRandomRegularRemoval");

        lblPersonnelMarketRandomGreenRemoval.setText(resources.getString("lblPersonnelMarketRandomGreenRemoval.text"));
        lblPersonnelMarketRandomGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomGreenRemoval.toolTipText"));
        lblPersonnelMarketRandomGreenRemoval.setName("lblPersonnelMarketRandomGreenRemoval");

        spnPersonnelMarketRandomGreenRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomGreenRemoval.toolTipText"));
        spnPersonnelMarketRandomGreenRemoval.setName("spnPersonnelMarketRandomGreenRemoval");

        lblPersonnelMarketRandomUltraGreenRemoval.setText(resources.getString("lblPersonnelMarketRandomUltraGreenRemoval.text"));
        lblPersonnelMarketRandomUltraGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomUltraGreenRemoval.toolTipText"));
        lblPersonnelMarketRandomUltraGreenRemoval.setName("lblPersonnelMarketRandomUltraGreenRemoval");

        spnPersonnelMarketRandomUltraGreenRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomUltraGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomUltraGreenRemoval.toolTipText"));
        spnPersonnelMarketRandomUltraGreenRemoval.setName("spnPersonnelMarketRandomUltraGreenRemoval");

        lblPersonnelMarketDylansWeight.setText(resources.getString("lblPersonnelMarketDylansWeight.text"));
        lblPersonnelMarketDylansWeight.setToolTipText(resources.getString("lblPersonnelMarketDylansWeight.toolTipText"));
        lblPersonnelMarketDylansWeight.setName("lblPersonnelMarketDylansWeight");

        spnPersonnelMarketDylansWeight = new JSpinner(new SpinnerNumberModel(0.3, 0, 1, 0.1));
        spnPersonnelMarketDylansWeight.setToolTipText(resources.getString("lblPersonnelMarketDylansWeight.toolTipText"));
        spnPersonnelMarketDylansWeight.setName("spnPersonnelMarketDylansWeight");

        // Programmatically Assign Accessibility Labels
        lblPersonnelMarketType.setLabelFor(comboPersonnelMarketType);
        lblPersonnelMarketRandomEliteRemoval.setLabelFor(spnPersonnelMarketRandomEliteRemoval);
        lblPersonnelMarketRandomVeteranRemoval.setLabelFor(spnPersonnelMarketRandomVeteranRemoval);
        lblPersonnelMarketRandomRegularRemoval.setLabelFor(spnPersonnelMarketRandomRegularRemoval);
        lblPersonnelMarketRandomGreenRemoval.setLabelFor(spnPersonnelMarketRandomGreenRemoval);
        lblPersonnelMarketRandomUltraGreenRemoval.setLabelFor(spnPersonnelMarketRandomUltraGreenRemoval);
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketType)
                                .addComponent(comboPersonnelMarketType, GroupLayout.Alignment.LEADING))
                        .addComponent(chkPersonnelMarketReportRefresh)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomEliteRemoval)
                                .addComponent(spnPersonnelMarketRandomEliteRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomVeteranRemoval)
                                .addComponent(spnPersonnelMarketRandomVeteranRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomRegularRemoval)
                                .addComponent(spnPersonnelMarketRandomRegularRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomGreenRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomUltraGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomUltraGreenRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketDylansWeight)
                                .addComponent(spnPersonnelMarketDylansWeight, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketType)
                                .addComponent(comboPersonnelMarketType))
                        .addComponent(chkPersonnelMarketReportRefresh)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomEliteRemoval)
                                .addComponent(spnPersonnelMarketRandomEliteRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomVeteranRemoval)
                                .addComponent(spnPersonnelMarketRandomVeteranRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomRegularRemoval)
                                .addComponent(spnPersonnelMarketRandomRegularRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomGreenRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomUltraGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomUltraGreenRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketDylansWeight)
                                .addComponent(spnPersonnelMarketDylansWeight))
        );

        return panel;
    }

    private JPanel createUnitMarketPanel() {
        // Create Panel Components
        final JLabel lblUnitMarketMethod = new JLabel(resources.getString("lblUnitMarketMethod.text"));
        lblUnitMarketMethod.setToolTipText(resources.getString("lblUnitMarketMethod.toolTipText"));
        lblUnitMarketMethod.setName("lblUnitMarketMethod");

        comboUnitMarketMethod = new JComboBox<>(UnitMarketMethod.values());
        comboUnitMarketMethod.setToolTipText(resources.getString("lblUnitMarketMethod.toolTipText"));
        comboUnitMarketMethod.setName("comboUnitMarketMethod");
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
            final boolean enabled = !((UnitMarketMethod) Objects.requireNonNull(comboUnitMarketMethod.getSelectedItem())).isNone();
            chkUnitMarketRegionalMechVariations.setEnabled(enabled);
            chkInstantUnitMarketDelivery.setEnabled(enabled);
            chkUnitMarketReportRefresh.setEnabled(enabled);
        });

        chkUnitMarketRegionalMechVariations = new JCheckBox(resources.getString("chkUnitMarketRegionalMechVariations.text"));
        chkUnitMarketRegionalMechVariations.setToolTipText(resources.getString("chkUnitMarketRegionalMechVariations.toolTipText"));
        chkUnitMarketRegionalMechVariations.setName("chkUnitMarketRegionalMechVariations");

        chkInstantUnitMarketDelivery = new JCheckBox(resources.getString("chkInstantUnitMarketDelivery.text"));
        chkInstantUnitMarketDelivery.setToolTipText(resources.getString("chkInstantUnitMarketDelivery.toolTipText"));
        chkInstantUnitMarketDelivery.setName("chkInstantUnitMarketDelivery");

        chkUnitMarketReportRefresh = new JCheckBox(resources.getString("chkUnitMarketReportRefresh.text"));
        chkUnitMarketReportRefresh.setToolTipText(resources.getString("chkUnitMarketReportRefresh.toolTipText"));
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblUnitMarketMethod)
                                .addComponent(comboUnitMarketMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUnitMarketRegionalMechVariations)
                        .addComponent(chkInstantUnitMarketDelivery)
                        .addComponent(chkUnitMarketReportRefresh)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnitMarketMethod)
                                .addComponent(comboUnitMarketMethod))
                        .addComponent(chkUnitMarketRegionalMechVariations)
                        .addComponent(chkInstantUnitMarketDelivery)
                        .addComponent(chkUnitMarketReportRefresh)
        );

        return panel;
    }

    private JPanel createContractMarketPanel() {
        // Create Panel Components
        final JLabel lblContractMarketMethod = new JLabel(resources.getString("lblContractMarketMethod.text"));
        lblContractMarketMethod.setToolTipText(resources.getString("lblContractMarketMethod.toolTipText"));
        lblContractMarketMethod.setName("lblContractMarketMethod");
        lblContractMarketMethod.setVisible(false); // TODO : AbstractContractMarket : Remove

        comboContractMarketMethod = new JComboBox<>(ContractMarketMethod.values());
        comboContractMarketMethod.setToolTipText(resources.getString("lblContractMarketMethod.toolTipText"));
        comboContractMarketMethod.setName("comboContractMarketMethod");
        comboContractMarketMethod.setVisible(false); // TODO : AbstractContractMarket : Remove
        /*
        comboContractMarketMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContractMarketMethod) {
                    list.setToolTipText(((ContractMarketMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboContractMarketMethod.addActionListener(evt -> {
            final boolean enabled = !((ContractMarketMethod) Objects.requireNonNull(comboContractMarketMethod.getSelectedItem())).isNone();
            chkContractMarketReportRefresh.setEnabled(enabled);
        });
         */

        chkContractMarketReportRefresh = new JCheckBox(resources.getString("chkContractMarketReportRefresh.text"));
        chkContractMarketReportRefresh.setToolTipText(resources.getString("chkContractMarketReportRefresh.toolTipText"));
        chkContractMarketReportRefresh.setName("chkContractMarketReportRefresh");

        // Programmatically Assign Accessibility Labels
        lblContractMarketMethod.setLabelFor(comboContractMarketMethod);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("contractMarketPanel.title")));
        panel.setName("contractMarketPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblContractMarketMethod)
                                .addComponent(comboContractMarketMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkContractMarketReportRefresh)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblContractMarketMethod)
                                .addComponent(comboContractMarketMethod))
                        .addComponent(chkContractMarketReportRefresh)
        );

        return panel;
    }
    //endregion Markets Tab

    //region RATs Tab
    private JScrollPane createRATTab() {
        // Initialize Components Used in ActionListeners
        final JDisableablePanel traditionalRATPanel = new JDisableablePanel("traditionalRATPanel");

        // Initialize Parsing Variables
        final ButtonGroup group = new ButtonGroup();

        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("ratPanel");

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
                        displayName.append("-").append(eras.get(eras.size() - 1));
                    }
                    displayName.append(")");
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
            availableRATs.setSelectedIndex(Math.min(selectedIndex, availableRATModel.size() - 1));
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
                "btnMoveRATUp.toolTipText", evt ->{
            final int selectedIndex = chosenRATs.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }
            final String element = chosenRATModel.getElementAt(selectedIndex);
            chosenRATModel.setElementAt(chosenRATModel.getElementAt(selectedIndex - 1), selectedIndex);
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
            chosenRATModel.setElementAt(chosenRATModel.getElementAt(selectedIndex + 1), selectedIndex);
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
        availableRATs.addListSelectionListener(evt -> btnAddRAT.setEnabled(availableRATs.getSelectedIndex() >= 0));

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
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(txtRATInstructions)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblAvailableRATs)
                                        .addComponent(availableRATs))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(btnAddRAT)
                                        .addComponent(btnRemoveRAT)
                                        .addComponent(btnMoveRATUp)
                                        .addComponent(btnMoveRATDown))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblChosenRATs)
                                        .addComponent(chosenRATs)))
                        .addComponent(chkIgnoreRATEra)
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(txtRATInstructions)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAvailableRATs)
                                        .addComponent(lblChosenRATs))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(availableRATs)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnAddRAT)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnRemoveRAT)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnMoveRATUp)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnMoveRATDown))
                                        .addComponent(chosenRATs))
                                .addComponent(chkIgnoreRATEra))
        );
    }
    //endregion RATs Tab

    //region Against the Bot Tab
    //endregion Against the Bot Tab
    //endregion Modern Initialization
    //endregion Initialization
}
