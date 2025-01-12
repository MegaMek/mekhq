package mekhq.gui.dialog.campaignOptions;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.dialog.campaignOptions.AbilitiesTab.AbilityCategory;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

import static java.lang.Math.round;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.createSubTabs;

public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private static final int SCROLL_SPEED = 16;
    private static final int HEADER_FONT_SIZE = 5;

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    private GeneralTab generalTab;
    private PersonnelTab personnelTab;
    private BiographyTab biographyTab;
    private RelationshipsTab relationshipsTab;
    private TurnoverAndRetentionTab turnoverAndRetentionTab;
    private AdvancementTab advancementTab;
    private SkillsTab skillsTab;
    private AbilitiesTab abilitiesTab;
    private RepairAndMaintenanceTab repairAndMaintenanceTab;
    private EquipmentAndSuppliesTab equipmentAndSuppliesTab;
    private FinancesTab financesTab;
    private MarketsTab marketsTab;
    private RulesetsTab rulesetsTab;
    private JTabbedPane abilityContentTabs;
    private JTabbedPane advancementParentTab;

    public CampaignOptionsPane(final JFrame frame, final Campaign campaign) {
        super(frame, resources, "campaignOptionsDialog");
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
    }

    @Override
    protected void initialize() {
        double uiScale = 1;
        try {
            uiScale = Double.parseDouble(System.getProperty("flatlaf.uiScale"));
        } catch (Exception ignored) {}

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", round(HEADER_FONT_SIZE * uiScale),
            resources.getString("generalPanel.title")), createGeneralTab());

        createTab("humanResourcesParentTab", createHumanResourcesParentTab());
        createTab("advancementParentTab", createAdvancementParentTab());
        createTab("logisticsAndMaintenanceParentTab", createEquipmentAndSuppliesParentTab());
        createTab("strategicOperationsParentTab", createStrategicOperationsParentTab());

        abilityContentTabs = new JTabbedPane();
    }

    /**
     * Creates a tab and adds it to the TabbedPane.
     *
     * @param resourceName the name of the resource used to create the tab's title
     * @param tab          the tab to be added
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
        } catch (Exception ignored) {}

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
            round(HEADER_FONT_SIZE * uiScale),
            resources.getString(resourceName + ".title")), tabScrollPane);
    }

    /**
     * Creates the General tab.
     *
     * @return a {@link JScrollPane} containing the General tab
     */
    private JScrollPane createGeneralTab() {
        generalTab = new GeneralTab(campaign, getFrame());
        JPanel createdGeneralTab = generalTab.createGeneralTab();
        generalTab.loadValuesFromCampaignOptions();

        return new JScrollPane(createdGeneralTab);
    }

    /**
     * The `createHumanResourcesParentTab` method creates and returns a `JTabbedPane` object,
     * which represents the overarching "Human Resources" tab in the user interface.
     * <p>
     * Under the "Human Resources" tab, there are several sub-tabs for different categories, including
     * Personnel, Biography, Relationships, and Turnover and Retention. Each sub-tab contains various
     * settings related to its category.
     *
     * @return JTabbedPane representing the "Human Resources" tab.
     */
    private JTabbedPane createHumanResourcesParentTab() {
        // Parent Tab
        JTabbedPane humanResourcesParentTab = new JTabbedPane();

        // Personnel
        personnelTab = new PersonnelTab(campaignOptions);

        JTabbedPane personnelContentTabs = createSubTabs(Map.of(
            "personnelGeneralTab", personnelTab.createGeneralTab(),
            "personnelInformationTab", personnelTab.createPersonnelInformationTab(),
            "awardsTab", personnelTab.createAwardsTab(),
            "prisonersAndDependentsTab", personnelTab.createPrisonersAndDependentsTab(),
            "medicalTab", personnelTab.createMedicalTab(),
            "salariesTab", personnelTab.createSalariesTab()));
        personnelTab.loadValuesFromCampaignOptions();

        // Biography
        biographyTab = new BiographyTab(campaign);

        JTabbedPane biographyContentTabs = createSubTabs(Map.of(
            "biographyGeneralTab", biographyTab.createGeneralTab(),
            "backgroundsTab", biographyTab.createBackgroundsTab(),
            "deathTab", biographyTab.createDeathTab(),
            "educationTab", biographyTab.createEducationTab(),
            "nameAndPortraitGenerationTab", biographyTab.createNameAndPortraitGenerationTab(),
            "rankTab", biographyTab.createRankTab()));
        biographyTab.loadValuesFromCampaignOptions();

        // Relationships
        relationshipsTab = new RelationshipsTab(campaignOptions);

        JTabbedPane relationshipsContentTabs = createSubTabs(Map.of(
            "marriageTab", relationshipsTab.createMarriageTab(),
            "divorceTab", relationshipsTab.createDivorceTab(),
            "procreationTab", relationshipsTab.createProcreationTab()));
        relationshipsTab.loadValuesFromCampaignOptions();

        // Turnover and Retention
        turnoverAndRetentionTab = new TurnoverAndRetentionTab(campaignOptions);

        JTabbedPane turnoverAndRetentionContentTabs = createSubTabs(Map.of(
            "turnoverTab", turnoverAndRetentionTab.createTurnoverTab(),
            "fatigueTab", turnoverAndRetentionTab.createFatigueTab()));
        turnoverAndRetentionTab.loadValuesFromCampaignOptions();

        // Add Tabs
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("personnelContentTabs.title")), personnelContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("biographyContentTabs.title")), biographyContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("relationshipsContentTabs.title")), relationshipsContentTabs);
        humanResourcesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("turnoverAndRetentionContentTabs.title")), turnoverAndRetentionContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("humanResourcesParentTab.title")), humanResourcesParentTab);

        return humanResourcesParentTab;
    }

    private JTabbedPane createAdvancementParentTab() {
        // Parent Tab
        advancementParentTab = new JTabbedPane();

        // Advancement
        advancementTab = new AdvancementTab(campaign);

        JTabbedPane awardsAndRandomizationContentTabs = createSubTabs(Map.of(
            "xpAwardsTab", advancementTab.xpAwardsTab(),
            "randomizationTab", advancementTab.skillRandomizationTab()));
        advancementTab.loadValuesFromCampaignOptions();

        // Skills
        skillsTab = new SkillsTab();

        JTabbedPane skillsContentTabs = createSubTabs(Map.of(
            "combatSkillsTab", skillsTab.createSkillsTab(true),
            "supportSkillsTab", skillsTab.createSkillsTab(false)));
        skillsTab.loadValuesFromCampaignOptions();

        // SPAs
        abilitiesTab = new AbilitiesTab();

        abilityContentTabs = createSubTabs(Map.of(
            "combatAbilitiesTab", abilitiesTab.createAbilitiesTab(AbilityCategory.COMBAT_ABILITIES),
            "maneuveringAbilitiesTab", abilitiesTab.createAbilitiesTab(AbilityCategory.MANEUVERING_ABILITIES),
            "utilityAbilitiesTab", abilitiesTab.createAbilitiesTab(AbilityCategory.UTILITY_ABILITIES)));

        // Add Tabs
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("awardsAndRandomizationContentTabs.title")), awardsAndRandomizationContentTabs);
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("skillsContentTabs.title")), skillsContentTabs);
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("abilityContentTabs.title")), abilityContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("advancementParentTab.title")), advancementParentTab);

        return advancementParentTab;
    }

    /**
     * This `createEquipmentAndSuppliesParentTab` method creates and returns a `JTabbedPane` object.
     * This represents the "Logistics and Maintenance" parent tab in the user interface.
     *
     * @return JTabbedPane representing the "Logistics and Maintenance" tab.
     */
    private JTabbedPane createEquipmentAndSuppliesParentTab() {
        // Parent Tab
        JTabbedPane equipmentAndSuppliesParentTab = new JTabbedPane();

        // Repair and Maintenance
        repairAndMaintenanceTab = new RepairAndMaintenanceTab(campaignOptions);

        JTabbedPane repairsAndMaintenanceContentTabs = createSubTabs(Map.of(
            "repairTab", repairAndMaintenanceTab.createRepairTab(),
            "maintenanceTab", repairAndMaintenanceTab.createMaintenanceTab()));
        repairAndMaintenanceTab.loadValuesFromCampaignOptions();

        // Supplies and Acquisition
        equipmentAndSuppliesTab = new EquipmentAndSuppliesTab(campaignOptions);

        JTabbedPane suppliesAndAcquisitionContentTabs = createSubTabs(Map.of(
            "acquisitionTab", equipmentAndSuppliesTab.createAcquisitionTab(),
            "planetaryAcquisitionTab", equipmentAndSuppliesTab.createPlanetaryAcquisitionTab(),
            "techLimitsTab", equipmentAndSuppliesTab.createTechLimitsTab()));
        equipmentAndSuppliesTab.loadValuesFromCampaignOptions();

        // Add tabs
        equipmentAndSuppliesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("suppliesAndAcquisitionContentTabs.title")), suppliesAndAcquisitionContentTabs);
        equipmentAndSuppliesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("repairsAndMaintenanceContentTabs.title")), repairsAndMaintenanceContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("logisticsAndMaintenanceParentTab.title")), equipmentAndSuppliesParentTab);

        return equipmentAndSuppliesParentTab;
    }

    private JTabbedPane createStrategicOperationsParentTab() {
        // Parent Tab
        JTabbedPane strategicOperationsParentTab = new JTabbedPane();

        // Finances
        financesTab = new FinancesTab(campaign);

        JTabbedPane financesContentTabs = createSubTabs(Map.of(
            "financesGeneralTab", financesTab.createFinancesGeneralOptionsTab(),
            "priceMultipliersTab", financesTab.createPriceMultipliersTab()));
        financesTab.loadValuesFromCampaignOptions();

        // Markets
        marketsTab = new MarketsTab(campaignOptions);

        JTabbedPane marketsContentTabs = createSubTabs(Map.of(
            "personnelMarketTab", marketsTab.createPersonnelMarketTab(),
            "unitMarketTab", marketsTab.createUnitMarketTab(),
            "contractMarketTab", marketsTab.createContractMarketTab()));
        marketsTab.loadValuesFromCampaignOptions();

        // Rulesets
        rulesetsTab = new RulesetsTab(campaignOptions);

        JTabbedPane rulesetsContentTabs = createSubTabs(Map.of(
            "stratConGeneralTab", rulesetsTab.createStratConTab(),
            "legacyTab", rulesetsTab.createLegacyTab()));
        rulesetsTab.loadValuesFromCampaignOptions();

        // Add tabs
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("financesContentTabs.title")), financesContentTabs);
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("marketsContentTabs.title")), marketsContentTabs);
        strategicOperationsParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("rulesetsContentTabs.title")), rulesetsContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("strategicOperationsParentTab.title")), strategicOperationsParentTab);

        return strategicOperationsParentTab;
    }

    public void applyCampaignOptionsToCampaign() {
        // Everything assumes general tab will be the first applied.
        // While this shouldn't break anything, it's not worth moving around.
        // For all other tabs, it makes sense to apply them in the order they
        // appear in the dialog; however, this shouldn't make any major difference.
        generalTab.applyCampaignOptionsToCampaign();

        // Human Resources
        personnelTab.applyCampaignOptionsToCampaign();
        biographyTab.applyCampaignOptionsToCampaign();
        relationshipsTab.applyCampaignOptionsToCampaign();
        turnoverAndRetentionTab.applyCampaignOptionsToCampaign();

        // Advancement
        advancementTab.applyCampaignOptionsToCampaign();
        skillsTab.applyCampaignOptionsToCampaign();
        abilitiesTab.applyCampaignOptionsToCampaign();

        // Logistics
        equipmentAndSuppliesTab.applyCampaignOptionsToCampaign();
        repairAndMaintenanceTab.applyCampaignOptionsToCampaign();

        // Operations
        financesTab.applyCampaignOptionsToCampaign();
        marketsTab.loadValuesFromCampaignOptions();
        rulesetsTab.applyCampaignOptionsToCampaign();
    }

    public void applyPreset(@Nullable CampaignPreset campaignPreset) {
        if (campaignPreset == null) {
            return;
        }

        // TODO override these with current campaign settings in the event we're not calling this via start up
        CampaignOptions presetCampaignOptions = campaignPreset.getCampaignOptions();
        LocalDate presetDate = campaignPreset.getDate();
        Faction presetFaction = campaignPreset.getFaction();

        generalTab.loadValuesFromCampaignOptions(presetCampaignOptions, presetDate, presetFaction);

        // Human Resources
        personnelTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        biographyTab.loadValuesFromCampaignOptions(presetCampaignOptions,
            presetCampaignOptions.getRandomOriginOptions());
        relationshipsTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        turnoverAndRetentionTab.loadValuesFromCampaignOptions(presetCampaignOptions);

        // Advancement
        advancementTab.loadValuesFromCampaignOptions(presetCampaignOptions,
            campaignPreset.getRandomSkillPreferences());
        skillsTab.loadValuesFromCampaignOptions(campaignPreset.getSkills());
        // The ability tab is a special case, so handled differently to other tabs
        rebuildAbilityContentTabsContents(campaignPreset);

        // Logistics
        equipmentAndSuppliesTab.loadValuesFromCampaignOptions(presetCampaignOptions);
        repairAndMaintenanceTab.loadValuesFromCampaignOptions(presetCampaignOptions);

        // Operations
        financesTab.loadValuesFromCampaignOptions();
        marketsTab.loadValuesFromCampaignOptions();
        rulesetsTab.loadValuesFromCampaignOptions();
    }

    private void rebuildAbilityContentTabsContents(CampaignPreset campaignPreset) {
        // Due to the complexity of the ability content tabs and how much can differ between
        // presets and saves, we rebuild the entire batch of content tabs.
        abilitiesTab.setAllAbilities(campaignPreset.getSpecialAbilities());
        abilityContentTabs = createSubTabs(Map.of(
                "combatAbilitiesTab", abilitiesTab.createAbilitiesTab(AbilityCategory.COMBAT_ABILITIES),
                "maneuveringAbilitiesTab", abilitiesTab.createAbilitiesTab(AbilityCategory.MANEUVERING_ABILITIES),
                "utilityAbilitiesTab", abilitiesTab.createAbilitiesTab(AbilityCategory.UTILITY_ABILITIES)
        ));

        for (int i = 0; i < advancementParentTab.getTabCount(); i++) {
            if (advancementParentTab.getTitleAt(i).contains(resources.getString("abilityContentTabs.title"))) {
                advancementParentTab.remove(i);
                break;
            }
        }

        advancementParentTab.addTab(
                String.format("<html><font size=%s><b>%s</b></font></html>", 4,
                        resources.getString("abilityContentTabs.title")),
                abilityContentTabs
        );

        advancementParentTab.revalidate();
        advancementParentTab.repaint();
    }
}
