package mekhq.gui.dialog.campaignOptions;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.dialog.campaignOptions.AbilitiesTab.AbilityCategory;

import javax.swing.*;
import java.util.Map;
import java.util.ResourceBundle;

import static java.lang.Math.round;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.createSubTabs;

public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    private static final MMLogger logger = MMLogger.create(CampaignOptionsPane.class);
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
        generalTab = new GeneralTab(campaign, getFrame(), "generalTab");
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
        personnelTab = new PersonnelTab(campaignOptions, getFrame(), "personnelTab");

        JTabbedPane personnelContentTabs = createSubTabs(Map.of(
            "personnelGeneralTab", personnelTab.createGeneralTab(),
            "personnelInformationTab", personnelTab.createPersonnelInformationTab(),
            "awardsTab", personnelTab.createAwardsTab(),
            "prisonersAndDependentsTab", personnelTab.createPrisonersAndDependentsTab(),
            "medicalTab", personnelTab.createMedicalTab(),
            "salariesTab", personnelTab.createSalariesTab()));
        personnelTab.loadValuesFromCampaignOptions();

        // Biography
        biographyTab = new BiographyTab(campaign, getFrame(), "biographyTab");

        JTabbedPane biographyContentTabs = createSubTabs(Map.of(
            "biographyGeneralTab", biographyTab.createGeneralTab(),
            "backgroundsTab", biographyTab.createBackgroundsTab(),
            "deathTab", biographyTab.createDeathTab(),
            "educationTab", biographyTab.createEducationTab(),
            "nameAndPortraitGenerationTab", biographyTab.createNameAndPortraitGenerationTab(),
            "rankTab", biographyTab.createRankTab()));
        biographyTab.loadValuesFromCampaignOptions();

        // Relationships
        relationshipsTab = new RelationshipsTab(campaignOptions, getFrame(), "relationshipsTab");

        JTabbedPane relationshipsContentTabs = createSubTabs(Map.of(
            "marriageTab", relationshipsTab.createMarriageTab(),
            "divorceTab", relationshipsTab.createDivorceTab(),
            "procreationTab", relationshipsTab.createProcreationTab()));
        relationshipsTab.loadValuesFromCampaignOptions();

        // Turnover and Retention
        turnoverAndRetentionTab = new TurnoverAndRetentionTab(campaignOptions,
                getFrame(), "turnoverAndRetentionTab");

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
        JTabbedPane advancementParentTab = new JTabbedPane();

        // Advancement
        advancementTab = new AdvancementTab(campaign, getFrame(), "advancementTab");

        JTabbedPane awardsAndRandomizationContentTabs = createSubTabs(Map.of(
            "xpAwardsTab", advancementTab.xpAwardsTab(),
            "randomizationTab", advancementTab.skillRandomizationTab()));
        advancementTab.loadValuesFromCampaignOptions();

        // Skills
        skillsTab = new SkillsTab(campaignOptions, getFrame(), "skillsTab");

        JTabbedPane skillsContentTabs = createSubTabs(Map.of(
            "combatSkillsTab", skillsTab.createSkillsTab(true),
            "supportSkillsTab", skillsTab.createSkillsTab(false)));
        skillsTab.loadValuesFromCampaignOptions();

        // SPAs
        abilitiesTab = new AbilitiesTab(campaignOptions, getFrame(), "abilitiesTab");

        JTabbedPane abilityContentTabs = createSubTabs(Map.of(
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
        repairAndMaintenanceTab = new RepairAndMaintenanceTab(campaignOptions,
                getFrame(), "repairAndMaintenanceTab");

        JTabbedPane repairsAndMaintenanceContentTabs = createSubTabs(Map.of(
            "repairTab", repairAndMaintenanceTab.createRepairTab(),
            "maintenanceTab", repairAndMaintenanceTab.createMaintenanceTab()));
        repairAndMaintenanceTab.loadValuesFromCampaignOptions();

        // Supplies and Acquisition
        equipmentAndSuppliesTab = new EquipmentAndSuppliesTab(campaignOptions,
            getFrame(), "suppliesAndAcquisitionTab");

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
        financesTab = new FinancesTab(campaign, getFrame(), "financesTab");

        JTabbedPane financesContentTabs = createSubTabs(Map.of(
            "financesGeneralTab", financesTab.createFinancesGeneralOptionsTab(),
            "priceMultipliersTab", financesTab.createPriceMultipliersTab()));
        financesTab.loadValuesFromCampaignOptions();

        // Markets
        marketsTab = new MarketsTab(campaignOptions, getFrame(), "marketsTab");

        JTabbedPane marketsContentTabs = createSubTabs(Map.of(
            "personnelMarketTab", marketsTab.createPersonnelMarketTab(),
            "unitMarketTab", marketsTab.createUnitMarketTab(),
            "contractMarketTab", marketsTab.createContractMarketTab()));
        marketsTab.loadValuesFromCampaignOptions();

        // Rulesets
        rulesetsTab = new RulesetsTab(campaignOptions, getFrame(), "rulesetsTab");

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
        abilitiesTab.applyCampaignOptionsToCampaign();
        advancementTab.applyCampaignOptionsToCampaign(campaign);
        biographyTab.applyCampaignOptionsToCampaign();
        equipmentAndSuppliesTab.applyCampaignOptionsToCampaign();
        financesTab.applyCampaignOptionsToCampaign();
        personnelTab.applyCampaignOptionsToCampaign();
        relationshipsTab.applyCampaignOptionsToCampaign();
        repairAndMaintenanceTab.applyCampaignOptionsToCampaign();
        rulesetsTab.applyCampaignOptionsToCampaign();
        skillsTab.applyCampaignOptionsToCampaign();
    }

    public void updateOptions() {
        // TODO this is where we update campaign values based on the dialog values
    }
}
