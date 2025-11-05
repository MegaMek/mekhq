/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import static java.lang.Math.round;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_GENERAL;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.UTILITY;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.ABRIDGED;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP_ABRIDGED;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createSubTabs;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_CREATION_ONLY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_FLAW;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.COMBAT_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.MANEUVERING_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.UTILITY_ABILITY;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.campaignOptions.contents.*;
import mekhq.gui.campaignOptions.optionChangeDialogs.MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.dialog.factionStanding.FactionStandingCampaignOptionsChangedConfirmationDialog;
import mekhq.gui.dialog.factionStanding.VeterancyAwardsCampaignOptionsChangedConfirmationDialog;

/**
 * The {@code CampaignOptionsPane} class represents a tabbed pane used for displaying and managing various campaign
 * options in MekHQ. It organizes these options into tabs and sub-tabs, enabling users to configure different aspects of
 * a campaign. This component serves as the central UI for campaign settings management.
 *
 * <p>
 * The pane is initialized with a {@link Campaign} instance, which provides the campaign's data and allows options to be
 * applied directly to the active campaign. The dialog supports multiple modes, such as {@code NORMAL},
 * {@code ABRIDGED}, and {@code STARTUP}, to determine the level of detail and features shown.
 * </p>
 *
 * <strong>Key Features:</strong>
 * <ul>
 *   <li>Organizes options into logical groups, such as General, Human Resources,
 *       Advancement, Logistics, and Operations.</li>
 *   <li>Supports loading and applying campaign presets for streamlined configuration.</li>
 *   <li>Dynamically handles UI scaling and scrolling speed based on environment properties.</li>
 *   <li>Allows scalability for future addition of new campaign settings.</li>
 * </ul>
 */
public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    private static final int SCROLL_SPEED = 16;
    private static final int HEADER_FONT_SIZE = 5;

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final CampaignOptionsDialogMode mode;

    private GeneralTab generalTab;
    private PersonnelTab personnelTab;
    private BiographyTab biographyTab;
    private RelationshipsTab relationshipsTab;
    private SalariesTab salariesTab;
    private TurnoverAndRetentionTab turnoverAndRetentionTab;
    private AdvancementTab advancementTab;
    private SkillsTab skillsTab;
    private AbilitiesTab abilitiesTab;
    private RepairAndMaintenanceTab repairAndMaintenanceTab;
    private EquipmentAndSuppliesTab equipmentAndSuppliesTab;
    private FinancesTab financesTab;
    private MarketsTab marketsTab;
    private SystemsTab systemsTab;
    private RulesetsTab rulesetsTab;
    private CampaignGUI campaignGui;

    /**
     * Constructs a {@code CampaignOptionsPane} for managing campaign settings. This initializes the tabbed pane and
     * populates it with categories and sub-tabs based on the provided {@link Campaign} instance and dialog mode.
     *
     * @param frame    the parent {@link JFrame} for this pane
     * @param campaign the {@link Campaign} object representing the current campaign
     * @param mode     the {@link CampaignOptionsDialogMode} for configuring the pane's behavior
     */
    public CampaignOptionsPane(final JFrame frame, final Campaign campaign, CampaignOptionsDialogMode mode) {
        super(frame, ResourceBundle.getBundle(getCampaignOptionsResourceBundle()), "campaignOptionsDialog");
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.mode = mode;
        if (campaign.getApp() != null) {
            campaignGui = campaign.getApp().getCampaigngui();
        }
        initialize();
    }

    /**
     * Initializes the campaign options pane by creating all parent tabs and adding sub-tabs for various campaign
     * settings categories. Dynamically adjusts tab fonts and layout based on UI scaling settings.
     */
    @Override
    protected void initialize() {
        double uiScale = 1;
        try {
            uiScale = Double.parseDouble(System.getProperty("flatlaf.uiScale"));
        } catch (Exception ignored) {
        }

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    round(HEADER_FONT_SIZE * uiScale), getTextAt(getCampaignOptionsResourceBundle(), "generalPanel.title")),
              createGeneralTab(mode));

        JTabbedPane humanResourcesParentTab = createHumanResourcesParentTab();
        createTab("humanResourcesParentTab", humanResourcesParentTab);

        JTabbedPane advancementParentTab = createAdvancementParentTab();
        createTab("advancementParentTab", advancementParentTab);

        JTabbedPane equipmentAndSuppliesParentTab = createEquipmentAndSuppliesParentTab();
        createTab("logisticsAndMaintenanceParentTab", equipmentAndSuppliesParentTab);

        JTabbedPane strategicOperationsParentTab = createStrategicOperationsParentTab();
        createTab("strategicOperationsParentTab", strategicOperationsParentTab);
    }

    /**
     * Adds a new tab to the pane. Wrapper method for adding a resource-labeled tab containing a {@link JScrollPane} to
     * the campaign options pane. Dynamically adjusts font size for consistent scaling across all UI elements.
     *
     * @param resourceName the resource string key to locate the tab title
     * @param tab          the {@link JTabbedPane} to add as content for the tab
     */
    private void createTab(String resourceName, JTabbedPane tab) {
        JScrollPane tabScrollPane = new JScrollPane(tab);

        // Increase scroll speed
        tabScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
        tabScrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_SPEED);

        // Dynamically adjust font size based on the GUI scale
        double uiScale = 1;
        try {
            uiScale = Double.parseDouble(System.getProperty("flatlaf.uiScale"));
        } catch (Exception ignored) {
        }

        if (mode != ABRIDGED && mode != STARTUP_ABRIDGED) {
            addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                  round(HEADER_FONT_SIZE * uiScale),
                  getTextAt(getCampaignOptionsResourceBundle(), resourceName + ".title")), tabScrollPane);
        }
    }

    /**
     * Creates the panel for general campaign options. Loads settings for general preferences and initializes it with
     * current campaign options.
     *
     * @param mode the state in which the dialog was triggered.
     *
     * @return a {@link JScrollPane} containing the general tab panel
     */
    private JScrollPane createGeneralTab(CampaignOptionsDialogMode mode) {
        generalTab = new GeneralTab(campaign, getFrame(), mode);
        JPanel createdGeneralTab = generalTab.createGeneralTab();
        generalTab.loadValuesFromCampaignOptions();

        return new JScrollPane(createdGeneralTab);
    }

    /**
     * Creates the "Human Resources" parent tab. This tab organizes related sub-tabs concerning personnel management,
     * relationships, turnover, and biography options.
     *
     * @return a {@link JTabbedPane} containing sub-tabs for the human resources category
     */
    private JTabbedPane createHumanResourcesParentTab() {
        // Parent Tab
        JTabbedPane humanResourcesParentTab = new JTabbedPane();

        // Personnel
        personnelTab = new PersonnelTab(campaignOptions);

        JTabbedPane personnelContentTabs = createSubTabs(Map.of("personnelGeneralTab",
              personnelTab.createGeneralTab(),
              "personnelInformationTab",
              personnelTab.createPersonnelInformationTab(),
              "awardsTab",
              personnelTab.createAwardsTab(),
              "prisonersAndDependentsTab",
              personnelTab.createPrisonersAndDependentsTab(),
              "medicalTab", personnelTab.createMedicalTab()));
        personnelTab.loadValuesFromCampaignOptions(campaign.getVersion());

        // Biography
        biographyTab = new BiographyTab(campaign, generalTab);

        JTabbedPane biographyContentTabs = createSubTabs(Map.of("biographyGeneralTab",
              biographyTab.createGeneralTab(),
              "backgroundsTab",
              biographyTab.createBackgroundsTab(),
              "deathTab",
              biographyTab.createDeathTab(),
              "educationTab",
              biographyTab.createEducationTab(),
              "nameAndPortraitGenerationTab",
              biographyTab.createNameAndPortraitGenerationTab(),
              "rankTab",
              biographyTab.createRankTab()));
        biographyTab.loadValuesFromCampaignOptions();

        // Relationships
        relationshipsTab = new RelationshipsTab(campaignOptions);

        JTabbedPane relationshipsContentTabs = createSubTabs(Map.of("marriageTab",
              relationshipsTab.createMarriageTab(),
              "divorceTab",
              relationshipsTab.createDivorceTab(),
              "procreationTab",
              relationshipsTab.createProcreationTab()));
        relationshipsTab.loadValuesFromCampaignOptions();

        // Personnel
        salariesTab = new SalariesTab(campaignOptions);

        JTabbedPane salariesContentTabs = createSubTabs(Map.of("0combatSalariesTab",
              salariesTab.createSalariesTab(PersonnelRoleSubType.COMBAT),
              "1supportSalariesTab",
              salariesTab.createSalariesTab(PersonnelRoleSubType.SUPPORT),
              "2civilianSalariesTab",
              salariesTab.createSalariesTab(PersonnelRoleSubType.CIVILIAN)));
        salariesTab.loadValuesFromCampaignOptions();

        // Turnover and Retention
        turnoverAndRetentionTab = new TurnoverAndRetentionTab(campaignOptions);

        JTabbedPane turnoverAndRetentionContentTabs = createSubTabs(Map.of("turnoverTab",
              turnoverAndRetentionTab.createTurnoverTab(),
              "fatigueTab",
              turnoverAndRetentionTab.createFatigueTab()));
        turnoverAndRetentionTab.loadValuesFromCampaignOptions();

        // Add Tabs
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "personnelContentTabs.title")), personnelContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "biographyContentTabs.title")), biographyContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "relationshipsContentTabs.title")),
              relationshipsContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "turnoverAndRetentionContentTabs.title")),
              turnoverAndRetentionContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4,
              getTextAt(getCampaignOptionsResourceBundle(), "salariesContentTabs.title")), salariesContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "humanResourcesParentTab.title")),
              humanResourcesParentTab);

        return humanResourcesParentTab;
    }

    /**
     * Creates the "Advancement" parent tab. This tab organizes related sub-tabs for awards, skill randomization,
     * general skill management, and special pilot abilities (SPAs).
     *
     * @return a {@link JTabbedPane} containing sub-tabs for the advancement category
     */
    private JTabbedPane createAdvancementParentTab() {
        // Parent Tab
        JTabbedPane advancementParentTab = new JTabbedPane();

        // Advancement
        advancementTab = new AdvancementTab(campaign);

        JTabbedPane awardsAndRandomizationContentTabs = createSubTabs(Map.of("1xpAwardsTab",
              advancementTab.xpAwardsTab(),
              "0randomizationTab",
              advancementTab.skillRandomizationTab(),
              "2recruitmentBonusesTab",
              advancementTab.recruitmentBonusesTab()));
        advancementTab.loadValuesFromCampaignOptions();

        // Skills
        skillsTab = new SkillsTab(campaignOptions);

        JTabbedPane skillsContentTabs = createSubTabs(Map.of("0gunnerySkillsTab",
              skillsTab.createSkillsTab(COMBAT_GUNNERY),
              "1pilotingSkillsTab",
              skillsTab.createSkillsTab(COMBAT_PILOTING),
              "2supportSkillsTab",
              skillsTab.createSkillsTab(SUPPORT),
              "3utilitySkillsTab",
              skillsTab.createSkillsTab(UTILITY),
              "4roleplaySkillsTab",
              skillsTab.createSkillsTab(ROLEPLAY_GENERAL)));
        skillsTab.loadValuesFromCampaignOptions();

        // SPAs
        abilitiesTab = new AbilitiesTab();

        JTabbedPane abilityContentTabs = createSubTabs(Map.of("0combatAbilitiesTab",
              abilitiesTab.createAbilitiesTab(COMBAT_ABILITY),
              "1maneuveringAbilitiesTab",
              abilitiesTab.createAbilitiesTab(MANEUVERING_ABILITY),
              "2utilityAbilitiesTab",
              abilitiesTab.createAbilitiesTab(UTILITY_ABILITY),
              "3characterFlawsTab",
              abilitiesTab.createAbilitiesTab(CHARACTER_FLAW),
              "4characterCreationOnlyTab",
              abilitiesTab.createAbilitiesTab(CHARACTER_CREATION_ONLY)));
        // the loading of values from the campaign is built into the AbilitiesTab class so not called here.

        // Add Tabs
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "awardsAndRandomizationContentTabs.title")),
              awardsAndRandomizationContentTabs);
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "skillsContentTabs.title")), skillsContentTabs);
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "abilityContentTabs.title")), abilityContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "advancementParentTab.title")), advancementParentTab);

        return advancementParentTab;
    }

    /**
     * Creates the "Logistics and Maintenance" parent tab. This tab organizes related sub-tabs for equipment
     * acquisition, repair, maintenance, and supply management options.
     *
     * @return a {@link JTabbedPane} containing sub-tabs for the logistics and maintenance category
     */
    private JTabbedPane createEquipmentAndSuppliesParentTab() {
        // Parent Tab
        JTabbedPane equipmentAndSuppliesParentTab = new JTabbedPane();

        // Repair and Maintenance
        repairAndMaintenanceTab = new RepairAndMaintenanceTab(campaignOptions);

        JTabbedPane repairsAndMaintenanceContentTabs = createSubTabs(Map.of("repairTab",
              repairAndMaintenanceTab.createRepairTab(),
              "maintenanceTab",
              repairAndMaintenanceTab.createMaintenanceTab()));
        repairAndMaintenanceTab.loadValuesFromCampaignOptions();

        // Supplies and Acquisition
        equipmentAndSuppliesTab = new EquipmentAndSuppliesTab(campaignOptions);

        JTabbedPane suppliesAndAcquisitionContentTabs = createSubTabs(Map.of("acquisitionTab",
              equipmentAndSuppliesTab.createAcquisitionTab(),
              "planetaryAcquisitionTab",
              equipmentAndSuppliesTab.createPlanetaryAcquisitionTab(),
              "techLimitsTab",
              equipmentAndSuppliesTab.createTechLimitsTab()));
        equipmentAndSuppliesTab.loadValuesFromCampaignOptions();

        // Add tabs
        equipmentAndSuppliesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "suppliesAndAcquisitionContentTabs.title")),
              suppliesAndAcquisitionContentTabs);
        equipmentAndSuppliesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "repairsAndMaintenanceContentTabs.title")),
              repairsAndMaintenanceContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "logisticsAndMaintenanceParentTab.title")),
              equipmentAndSuppliesParentTab);

        return equipmentAndSuppliesParentTab;
    }

    /**
     * Creates the "Strategic Operations" parent tab. This tab organizes related sub-tabs for finances, market
     * management (personnel, units, and contracts), and ruleset configuration.
     *
     * @return a {@link JTabbedPane} containing sub-tabs for the strategic operations category
     */
    private JTabbedPane createStrategicOperationsParentTab() {
        // Parent Tab
        JTabbedPane strategicOperationsParentTab = new JTabbedPane();

        // Finances
        financesTab = new FinancesTab(campaign);

        JTabbedPane financesContentTabs = createSubTabs(Map.of("financesGeneralTab",
              financesTab.createFinancesGeneralOptionsTab(),
              "priceMultipliersTab",
              financesTab.createPriceMultipliersTab()));
        financesTab.loadValuesFromCampaignOptions();

        // Markets
        marketsTab = new MarketsTab(campaign);

        JTabbedPane marketsContentTabs = createSubTabs(Map.of("personnelMarketTab",
              marketsTab.createPersonnelMarketTab(),
              "unitMarketTab",
              marketsTab.createUnitMarketTab(),
              "contractMarketTab",
              marketsTab.createContractMarketTab()));
        marketsTab.loadValuesFromCampaignOptions();

        // Systems
        systemsTab = new SystemsTab(campaign);

        JTabbedPane systemsContentTabs = createSubTabs(Map.of(
              "reputationTab", systemsTab.createReputationTab(),
              "factionStandingTab", systemsTab.createFactionStandingTab(),
              "atowTab", systemsTab.createATOWTab()));
        systemsTab.loadValuesFromCampaignOptions();

        // Rulesets
        rulesetsTab = new RulesetsTab(campaignOptions);

        JTabbedPane rulesetsContentTabs = createSubTabs(Map.of("stratConGeneralTab",
              rulesetsTab.createStratConTab(),
              "legacyTab",
              rulesetsTab.createLegacyTab()));
        rulesetsTab.loadValuesFromCampaignOptions();

        // Add tabs
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "financesContentTabs.title")), financesContentTabs);
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "marketsContentTabs.title")), marketsContentTabs);
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4,
              getTextAt(getCampaignOptionsResourceBundle(), "systemsContentTabs.title")), systemsContentTabs);
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "rulesetsContentTabs.title")), rulesetsContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "strategicOperationsParentTab.title")),
              strategicOperationsParentTab);

        return strategicOperationsParentTab;
    }

    /**
     * Applies the currently configured campaign options to the active {@link Campaign}. This method processes all tabs
     * in the dialog, applying the options to the campaign in logical order (e.g., "General" first, followed by other
     * categories).
     *
     * @param preset       an optional {@link CampaignPreset} used to override campaign options
     * @param isStartUp    specifies whether this is run as part of a startup initialization
     * @param isSaveAction determines if this action is saving options to a preset
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignPreset preset, boolean isStartUp,
          boolean isSaveAction) {
        CampaignOptions options = this.campaignOptions;
        RandomSkillPreferences presetRandomSkillPreferences = null;
        Map<String, SkillType> presetSkills = null;

        if (preset != null) {
            options = preset.getCampaignOptions();
            presetRandomSkillPreferences = preset.getRandomSkillPreferences();
            presetSkills = preset.getSkills();
        }

        // Store old values for use if we want to trigger certain dialogs
        boolean oldAwardVeterancySPAs = options.isAwardVeterancySPAs();
        boolean oldIsTrackFactionStanding = options.isTrackFactionStanding();
        boolean oldIsUseMASHTheatres = options.isUseMASHTheatres();

        // Everything assumes general tab will be the first applied.
        // While this shouldn't break anything, it's not worth moving around.
        // For all other tabs, it makes sense to apply them in the order they
        // appear in the dialog; however, this shouldn't make any major difference.
        generalTab.applyCampaignOptionsToCampaign(isStartUp, isSaveAction);

        // Human Resources
        personnelTab.applyCampaignOptionsToCampaign(campaign, options);
        biographyTab.applyCampaignOptionsToCampaign(options);
        relationshipsTab.applyCampaignOptionsToCampaign(options);
        salariesTab.applyCampaignOptionsToCampaign(options);
        turnoverAndRetentionTab.applyCampaignOptionsToCampaign(options);

        // Advancement
        advancementTab.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);
        skillsTab.applyCampaignOptionsToCampaign(options, presetSkills);
        abilitiesTab.applyCampaignOptionsToCampaign(preset);

        // Logistics
        equipmentAndSuppliesTab.applyCampaignOptionsToCampaign(options);
        repairAndMaintenanceTab.applyCampaignOptionsToCampaign(options);

        // Operations
        financesTab.applyCampaignOptionsToCampaign(options);
        marketsTab.applyCampaignOptionsToCampaign(options);
        rulesetsTab.applyCampaignOptionsToCampaign(options);
        systemsTab.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);

        // Tidy up
        if (preset == null) {
            recalculateCombatTeams(campaign);
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign, options));

            options.updateGameOptionsFromCampaignOptions(campaign.getGameOptions());
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
        }

        boolean newIsTrackFactionStandings = options.isTrackFactionStanding();
        if (!isStartUp && newIsTrackFactionStandings != oldIsTrackFactionStanding) { // Has tracking changed?
            FactionStandingCampaignOptionsChangedConfirmationDialog dialog = new FactionStandingCampaignOptionsChangedConfirmationDialog(
                  null,
                  campaign.getCampaignFactionIcon(),
                  campaign.getFaction(),
                  campaign.getLocalDate(),
                  campaign.getFactionStandings(),
                  campaign.getMissions(),
                  newIsTrackFactionStandings,
                  campaignOptions.getRegardMultiplier());

            List<String> reports = dialog.getReports();
            for (String report : reports) {
                if (report != null && !report.isBlank()) {
                    campaign.addReport(report);
                }
            }
        }

        boolean newIsAwardVeterancySPAs = options.isAwardVeterancySPAs();
        if (!isStartUp && newIsAwardVeterancySPAs && !oldAwardVeterancySPAs) { // Has tracking changed?
            new VeterancyAwardsCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseMASHTheatres = options.isUseMASHTheatres();
        if (!isStartUp && newIsUseMASHTheatres && !oldIsUseMASHTheatres) { // Has tracking changed?
            new MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog(campaign);
        }

        campaign.resetRandomDeath();
        if (campaignGui != null) {
            campaignGui.refreshMarketButtonLabels();
        }
    }

    /**
     * Use {@link #applyPreset(CampaignPreset, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void applyPreset(@Nullable CampaignPreset campaignPreset) {
        applyPreset(campaignPreset, false);
    }

    /**
     * Applies the values from a {@link CampaignPreset} to all tabs in the dialog. This propagates preset-specific
     * configuration to all associated components and sub-tabs, including campaign-related properties such as dates,
     * factions, and skills.
     *
     * @param campaignPreset the {@link CampaignPreset} containing the preset options to apply
     * @param isStartUp      {@code true} if the preset is being loaded during new campaign startup
     */
    public void applyPreset(@Nullable CampaignPreset campaignPreset, boolean isStartUp) {
        if (campaignPreset == null) {
            return;
        }

        CampaignOptions presetCampaignOptions = campaignPreset.getCampaignOptions();

        LocalDate presetDate = campaign.getLocalDate();
        Faction presetFaction = campaign.getFaction();
        if (isStartUp) {
            presetDate = campaignPreset.getDate();
            presetFaction = campaignPreset.getFaction();
        }

        generalTab.loadValuesFromCampaignOptions(presetDate, presetFaction);

        // Human Resources
        personnelTab.loadValuesFromCampaignOptions(presetCampaignOptions, campaign.getVersion());
        biographyTab.loadValuesFromCampaignOptions(presetCampaignOptions,
              presetCampaignOptions.getRandomOriginOptions(),
              campaignPreset.getRankSystem());
        relationshipsTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        turnoverAndRetentionTab.loadValuesFromCampaignOptions(presetCampaignOptions);

        // Advancement
        advancementTab.loadValuesFromCampaignOptions(presetCampaignOptions, campaignPreset.getRandomSkillPreferences());
        skillsTab.loadValuesFromCampaignOptions(presetCampaignOptions, campaignPreset.getSkills());
        // The ability tab is a special case, so handled differently to other tabs
        abilitiesTab.buildAllAbilityInfo(campaignPreset.getSpecialAbilities());

        // Logistics
        equipmentAndSuppliesTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        repairAndMaintenanceTab.loadValuesFromCampaignOptions(presetCampaignOptions);

        // Operations
        financesTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        marketsTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        rulesetsTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        systemsTab.loadValuesFromCampaignOptions(presetCampaignOptions, campaignPreset.getRandomSkillPreferences());
    }
}
