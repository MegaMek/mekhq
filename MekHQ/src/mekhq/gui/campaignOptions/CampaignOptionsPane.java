/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.force.CombatTeam.recalculateCombatTeams;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllSystemSpecificDiseasesWithCures;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_GENERAL;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.UTILITY;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP_ABRIDGED;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_CREATION_ONLY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_FLAW;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.COMBAT_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.MANEUVERING_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.UTILITY_ABILITY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsFreebieTracker;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.gui.CampaignGUI;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.contents.*;
import mekhq.gui.campaignOptions.optionChangeDialogs.*;

/**
 * The {@code CampaignOptionsPane} class represents a tabbed pane used for
 * displaying and managing various campaign
 * options in MekHQ. It organizes these options into tabs and sub-tabs, enabling
 * users to configure different aspects of
 * a campaign. This component serves as the central UI for campaign settings
 * management.
 *
 * <p>
 * The pane is initialized with a {@link Campaign} instance, which provides the
 * campaign's data and allows options to be
 * applied directly to the active campaign. The dialog supports multiple modes,
 * such as {@code NORMAL},
 * {@code ABRIDGED}, and {@code STARTUP}, to determine the level of detail and
 * features shown.
 * </p>
 *
 * <strong>Key Features:</strong>
 * <ul>
 * <li>Organizes options into logical groups, such as General, Human Resources,
 * Advancement, Logistics, and Operations.</li>
 * <li>Supports loading and applying campaign presets for streamlined
 * configuration.</li>
 * <li>Dynamically handles UI scaling and scrolling speed based on environment
 * properties.</li>
 * <li>Allows scalability for future addition of new campaign settings.</li>
 * </ul>
 */
public class CampaignOptionsPane extends JPanel {
    private static final int SCROLL_SPEED = 16;
    private static final int NAVIGATION_WIDTH = 240;

    private final JFrame frame;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final CampaignOptionsDialogMode mode;
    private final List<CampaignOptionsRoute> navigationTargets = new ArrayList<>();
    private final Map<String, Supplier<Component>> directPageFactories = new HashMap<>();
    private final Map<String, Component> directPageCache = new HashMap<>();
    private boolean searchIndexInitialized = false;

    private CampaignOptionsContentHost activeContentHost;
    private CampaignOptionsNavigationPanel navigationPanel;
    private boolean isSyncingNavigationSelection;

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
     * Constructs a {@code CampaignOptionsPane} for managing campaign settings. This
     * initializes the tabbed pane and
     * populates it with categories and sub-tabs based on the provided
     * {@link Campaign} instance and dialog mode.
     *
     * @param frame    the parent {@link JFrame} for this pane
     * @param campaign the {@link Campaign} object representing the current campaign
     * @param mode     the {@link CampaignOptionsDialogMode} for configuring the
     *                 pane's behavior
     */
    public CampaignOptionsPane(final JFrame frame, final Campaign campaign, CampaignOptionsDialogMode mode) {
        super(new BorderLayout());
        setName("campaignOptionsDialog");
        this.frame = frame;
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.mode = mode;
        if (campaign.getApp() != null) {
            campaignGui = campaign.getApp().getCampaigngui();
        }
        initialize();
    }

    /**
     * Initializes the campaign options pane by creating all parent tabs and adding
     * sub-tabs for various campaign
     * settings categories. Dynamically adjusts tab fonts and layout based on UI
     * scaling settings.
     */
    protected void initialize() {
        JPanel generalPage = createGeneralTab(mode);
        registerRoutes(generalPage);
        CampaignOptionsRoute initialRoute = navigationTargets.get(0);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createNavigationPanel(),
                createContentHost(generalPage, initialRoute));
        splitPane.setName("campaignOptionsSplitPane");
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(NAVIGATION_WIDTH);
        add(splitPane, BorderLayout.CENTER);
        navigationPanel.selectRoute(navigationTargets.get(0));
    }

    private CampaignOptionsNavigationPanel createNavigationPanel() {
        navigationPanel = new CampaignOptionsNavigationPanel(navigationTargets, this::selectedNavigationTarget);
        navigationPanel.setSearchIndexInitializer(this::ensureSearchIndexBuilt);
        return navigationPanel;
    }

    private CampaignOptionsContentHost createContentHost(Component initialContent, CampaignOptionsRoute initialRoute) {
        activeContentHost = new CampaignOptionsContentHost(initialContent,
                getQuoteResourceName(initialRoute),
                initialRoute.shouldShowHelpPanel());
        return activeContentHost;
    }

    private void registerRoutes(JPanel generalPage) {
        registerDirectRoute("general", () -> generalPage, "generalPanel");

        registerParentRoute("human-resources", "humanResourcesParentTab");
        registerParentRoute("human-resources.personnel", "humanResourcesParentTab", "personnelContentTabs");
        registerDirectRoute("human-resources.personnel.general", this::createPersonnelGeneralTab,
                "humanResourcesParentTab", "personnelContentTabs", "personnelGeneralTab");
        registerDirectRoute("human-resources.personnel.awards", this::createPersonnelAwardsTab,
                "humanResourcesParentTab", "personnelContentTabs", "awardsTab");
        registerDirectRoute("human-resources.personnel.medical", this::createPersonnelMedicalTab,
                "humanResourcesParentTab", "personnelContentTabs", "medicalTab");
        registerDirectRoute("human-resources.personnel.information", this::createPersonnelInformationTab,
                "humanResourcesParentTab", "personnelContentTabs", "personnelInformationTab");
        registerDirectRoute("human-resources.personnel.prisoners-and-civilians",
                this::createPersonnelPrisonersAndDependentsTab,
                "humanResourcesParentTab", "personnelContentTabs", "prisonersAndDependentsTab");
        registerParentRoute("human-resources.biography", "humanResourcesParentTab", "biographyContentTabs");
        registerDirectRoute("human-resources.biography.general", this::createBiographyGeneralTab,
                "humanResourcesParentTab", "biographyContentTabs", "biographyGeneralTab");
        registerDirectRoute("human-resources.biography.backgrounds", this::createBiographyBackgroundsTab,
                "humanResourcesParentTab", "biographyContentTabs", "backgroundsTab");
        registerDirectRoute("human-resources.biography.death", this::createBiographyDeathTab,
                "humanResourcesParentTab", "biographyContentTabs", "deathTab");
        registerDirectRoute("human-resources.biography.education", this::createBiographyEducationTab,
                "humanResourcesParentTab", "biographyContentTabs", "educationTab");
        registerDirectRoute("human-resources.biography.name-and-portraits",
                this::createBiographyNameAndPortraitGenerationTab,
                "humanResourcesParentTab", "biographyContentTabs", "nameAndPortraitGenerationTab");
        registerDirectRoute("human-resources.biography.rank", this::createBiographyRankTab,
                "humanResourcesParentTab", "biographyContentTabs", "rankTab");
        registerParentRoute("human-resources.relationships", "humanResourcesParentTab", "relationshipsContentTabs");
        registerDirectRoute("human-resources.relationships.marriage", this::createRelationshipMarriageTab,
                "humanResourcesParentTab", "relationshipsContentTabs", "marriageTab");
        registerDirectRoute("human-resources.relationships.divorce", this::createRelationshipDivorceTab,
                "humanResourcesParentTab", "relationshipsContentTabs", "divorceTab");
        registerDirectRoute("human-resources.relationships.procreation", this::createRelationshipProcreationTab,
                "humanResourcesParentTab", "relationshipsContentTabs", "procreationTab");
        registerParentRoute("human-resources.salaries", "humanResourcesParentTab", "salariesContentTabs");
        registerDirectRoute("human-resources.salaries.combat", this::createCombatSalariesTab,
                "humanResourcesParentTab", "salariesContentTabs", "0combatSalariesTab");
        registerDirectRoute("human-resources.salaries.support", this::createSupportSalariesTab,
                "humanResourcesParentTab", "salariesContentTabs", "1supportSalariesTab");
        registerDirectRoute("human-resources.salaries.civilian", this::createCivilianSalariesTab,
                "humanResourcesParentTab", "salariesContentTabs", "2civilianSalariesTab");
        registerParentRoute("human-resources.turnover-and-retention", "humanResourcesParentTab",
                "turnoverAndRetentionContentTabs");
        registerDirectRoute("human-resources.turnover-and-retention.turnover",
                this::createTurnoverAndRetentionTurnoverTab,
                "humanResourcesParentTab", "turnoverAndRetentionContentTabs", "turnoverTab");
        registerDirectRoute("human-resources.turnover-and-retention.fatigue",
                this::createTurnoverAndRetentionFatigueTab,
                "humanResourcesParentTab", "turnoverAndRetentionContentTabs", "fatigueTab");

        registerParentRoute("advancement", "advancementParentTab");
        registerParentRoute("advancement.awards-and-randomization", "advancementParentTab",
                "awardsAndRandomizationContentTabs");
        registerDirectRoute("advancement.awards-and-randomization.randomization",
                this::createAdvancementRandomizationTab,
                "advancementParentTab", "awardsAndRandomizationContentTabs", "0randomizationTab");
        registerDirectRoute("advancement.awards-and-randomization.xp-awards", this::createAdvancementXpAwardsTab,
                "advancementParentTab", "awardsAndRandomizationContentTabs", "1xpAwardsTab");
        registerDirectRoute("advancement.awards-and-randomization.recruitment-bonuses",
                this::createAdvancementRecruitmentBonusesTab,
                CampaignOptionsRouteOptions.withoutHelpPanel(),
                "advancementParentTab", "awardsAndRandomizationContentTabs", "2recruitmentBonusesTab");
        registerParentRoute("advancement.skills", "advancementParentTab", "skillsContentTabs");
        registerDirectRoute("advancement.skills.gunnery", this::createAdvancementGunnerySkillsTab,
                "advancementParentTab", "skillsContentTabs", "0gunnerySkillsTab");
        registerDirectRoute("advancement.skills.piloting", this::createAdvancementPilotingSkillsTab,
                "advancementParentTab", "skillsContentTabs", "1pilotingSkillsTab");
        registerDirectRoute("advancement.skills.support", this::createAdvancementSupportSkillsTab,
                "advancementParentTab", "skillsContentTabs", "2supportSkillsTab");
        registerDirectRoute("advancement.skills.utility", this::createAdvancementUtilitySkillsTab,
                "advancementParentTab", "skillsContentTabs", "3utilitySkillsTab");
        registerDirectRoute("advancement.skills.roleplay", this::createAdvancementRoleplaySkillsTab,
                "advancementParentTab", "skillsContentTabs", "4roleplaySkillsTab");
        registerParentRoute("advancement.abilities", "advancementParentTab", "abilityContentTabs");
        registerDirectRoute("advancement.abilities.combat", this::createAdvancementCombatAbilitiesTab,
                "advancementParentTab", "abilityContentTabs", "0combatAbilitiesTab");
        registerDirectRoute("advancement.abilities.maneuvering", this::createAdvancementManeuveringAbilitiesTab,
                "advancementParentTab", "abilityContentTabs", "1maneuveringAbilitiesTab");
        registerDirectRoute("advancement.abilities.utility", this::createAdvancementUtilityAbilitiesTab,
                "advancementParentTab", "abilityContentTabs", "2utilityAbilitiesTab");
        registerDirectRoute("advancement.abilities.character-flaws", this::createAdvancementCharacterFlawsTab,
                "advancementParentTab", "abilityContentTabs", "3characterFlawsTab");
        registerDirectRoute("advancement.abilities.character-creation-only",
                this::createAdvancementCharacterCreationOnlyTab,
                "advancementParentTab", "abilityContentTabs", "4characterCreationOnlyTab");

        registerParentRoute("logistics", "logisticsAndMaintenanceParentTab");
        registerParentRoute("logistics.repairs-and-maintenance", "logisticsAndMaintenanceParentTab",
                "repairsAndMaintenanceContentTabs");
        registerDirectRoute("logistics.repairs-and-maintenance.repairs", this::createLogisticsRepairsTab,
                "logisticsAndMaintenanceParentTab", "repairsAndMaintenanceContentTabs", "repairTab");
        registerDirectRoute("logistics.repairs-and-maintenance.maintenance", this::createLogisticsMaintenanceTab,
                "logisticsAndMaintenanceParentTab", "repairsAndMaintenanceContentTabs", "maintenanceTab");
        registerParentRoute("logistics.supplies-and-acquisition", "logisticsAndMaintenanceParentTab",
                "suppliesAndAcquisitionContentTabs");
        registerDirectRoute("logistics.supplies-and-acquisition.acquisition", this::createLogisticsAcquisitionTab,
                "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs", "acquisitionTab");
        registerDirectRoute("logistics.supplies-and-acquisition.planetary-acquisition",
                this::createLogisticsPlanetaryAcquisitionTab,
                "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs",
                "planetaryAcquisitionTab");
        registerDirectRoute("logistics.supplies-and-acquisition.tech-limits", this::createLogisticsTechLimitsTab,
                "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs", "techLimitsTab");

        registerParentRoute("operations", "strategicOperationsParentTab");
        registerParentRoute("operations.finances", "strategicOperationsParentTab", "financesContentTabs");
        registerDirectRoute("operations.finances.general", this::createOperationsFinancesGeneralTab,
                "strategicOperationsParentTab", "financesContentTabs", "financesGeneralTab");
        registerDirectRoute("operations.finances.price-multipliers", this::createOperationsPriceMultipliersTab,
                "strategicOperationsParentTab", "financesContentTabs", "priceMultipliersTab");
        registerParentRoute("operations.markets", "strategicOperationsParentTab", "marketsContentTabs");
        registerDirectRoute("operations.markets.personnel", this::createOperationsPersonnelMarketTab,
                "strategicOperationsParentTab", "marketsContentTabs", "personnelMarketTab");
        registerDirectRoute("operations.markets.units", this::createOperationsUnitMarketTab,
                "strategicOperationsParentTab", "marketsContentTabs", "unitMarketTab");
        registerDirectRoute("operations.markets.contracts", this::createOperationsContractMarketTab,
                "strategicOperationsParentTab", "marketsContentTabs", "contractMarketTab");
        registerParentRoute("operations.systems", "strategicOperationsParentTab", "systemsContentTabs");
        registerDirectRoute("operations.systems.reputation", this::createOperationsReputationTab,
                "strategicOperationsParentTab", "systemsContentTabs", "reputationTab");
        registerDirectRoute("operations.systems.faction-standing", this::createOperationsFactionStandingTab,
                "strategicOperationsParentTab", "systemsContentTabs", "factionStandingTab");
        registerDirectRoute("operations.systems.a-time-of-war", this::createOperationsATimeOfWarTab,
                "strategicOperationsParentTab", "systemsContentTabs", "atowTab");
        registerParentRoute("operations.rulesets", "strategicOperationsParentTab", "rulesetsContentTabs");
        registerDirectRoute("operations.rulesets.stratcon", this::createOperationsStratConTab,
                "strategicOperationsParentTab", "rulesetsContentTabs", "stratConGeneralTab");

    }

    private void selectedNavigationTarget(CampaignOptionsRoute route) {
        isSyncingNavigationSelection = true;
        try {
            selectRoute(route);
            resetContentScrollPosition();
        } finally {
            isSyncingNavigationSelection = false;
        }
    }

    private void resetContentScrollPosition() {
        if (activeContentHost != null) {
            activeContentHost.resetScrollPosition();
        }
    }

    private void selectRoute(CampaignOptionsRoute route) {
        // Group/parent routes have no page of their own, so resolve them to their first
        // child's page. We intentionally
        // do NOT move the tree highlight onto that child here: re-selecting it would
        // trap keyboard navigation, because
        // pressing Up onto a group row would immediately bounce the selection back down
        // to the group's first child.
        // Leaving the highlight where the user put it lets Up/Down move one row at a
        // time in both directions.
        CampaignOptionsRoute effectiveRoute = getDefaultDirectRoute(route);
        showDirectRoute(effectiveRoute);
    }

    private boolean showDirectRoute(CampaignOptionsRoute route) {
        Component directPage = getDirectPage(route.getId());
        if (directPage == null) {
            return false;
        }

        activeContentHost.setContent(directPage, getQuoteResourceName(route), route.shouldShowHelpPanel());
        return true;
    }

    private String getQuoteResourceName(CampaignOptionsRoute route) {
        List<String> titleResourceNames = route.getTitleResourceNames();
        return titleResourceNames.get(titleResourceNames.size() - 1);
    }

    private Component getDirectPage(String routeId) {
        Component directPage = directPageCache.get(routeId);
        if (directPage != null) {
            return directPage;
        }

        Supplier<Component> directPageFactory = directPageFactories.get(routeId);
        if (directPageFactory == null) {
            return null;
        }

        directPage = directPageFactory.get();
        directPageCache.putIfAbsent(routeId, directPage);
        Component cachedPage = directPageCache.get(routeId);
        harvestSectionSearchText(routeId, cachedPage);
        return cachedPage;
    }

    /**
     * Copies the resolved section titles and summaries of a freshly built page into its matching route so the
     * navigation filter can match section headings. This is a no-op when the page has no sections or has already been
     * harvested.
     *
     * @param routeId the id of the route that owns the page
     * @param page    the built page content
     */
    private void harvestSectionSearchText(String routeId, Component page) {
        if (!(page instanceof CampaignOptionsPagePanel pagePanel)) {
            return;
        }

        String sectionSearchText = pagePanel.getSectionSearchText();
        if (sectionSearchText.isBlank()) {
            return;
        }

        for (CampaignOptionsRoute navigationTarget : navigationTargets) {
            if (navigationTarget.getId().equals(routeId)) {
                navigationTarget.setSectionSearchText(sectionSearchText);
                return;
            }
        }
    }

    /**
     * Builds every direct page once, on demand, so section titles and summaries become searchable across all pages
     * rather than only the pages the user has already visited. Pages are built progressively, one per Swing event, to
     * keep the dialog responsive, and the built pages are cached so later navigation is instant. Runs at most once.
     */
    void ensureSearchIndexBuilt() {
        if (searchIndexInitialized) {
            return;
        }
        searchIndexInitialized = true;
        buildSearchIndexStep(new ArrayList<>(directPageFactories.keySet()), 0);
    }

    private void buildSearchIndexStep(List<String> routeIds, int index) {
        if (index >= routeIds.size()) {
            if (navigationPanel != null) {
                navigationPanel.refreshFilter();
            }
            return;
        }

        getDirectPage(routeIds.get(index));
        SwingUtilities.invokeLater(() -> buildSearchIndexStep(routeIds, index + 1));
    }

    private CampaignOptionsRoute getDefaultDirectRoute(CampaignOptionsRoute route) {
        if (directPageFactories.containsKey(route.getId())) {
            return route;
        }

        String routePrefix = route.getId() + ".";
        for (CampaignOptionsRoute navigationTarget : navigationTargets) {
            if (navigationTarget.getId().startsWith(routePrefix)
                    && directPageFactories.containsKey(navigationTarget.getId())) {
                return navigationTarget;
            }
        }

        return route;
    }

    private void ensureSectionLoaded(String topLevelResourceName) {
        switch (topLevelResourceName) {
            case "humanResourcesParentTab" -> {
                if (personnelTab == null) {
                    initializeHumanResourcesSection();
                }
            }
            case "advancementParentTab" -> {
                if (advancementTab == null) {
                    initializeAdvancementSection();
                }
            }
            case "logisticsAndMaintenanceParentTab" -> {
                if (equipmentAndSuppliesTab == null) {
                    initializeLogisticsSection();
                }
            }
            case "strategicOperationsParentTab" -> {
                if (financesTab == null) {
                    initializeOperationsSection();
                }
            }
            default -> {
                // General is built eagerly.
            }
        }
    }

    private void ensureAllSectionsLoaded() {
        ensureSectionLoaded("humanResourcesParentTab");
        ensureSectionLoaded("advancementParentTab");
        ensureSectionLoaded("logisticsAndMaintenanceParentTab");
        ensureSectionLoaded("strategicOperationsParentTab");
    }

    private void initializeHumanResourcesSection() {
        personnelTab = new PersonnelTab(campaignOptions);
        biographyTab = new BiographyTab(campaign, generalTab);
        relationshipsTab = new RelationshipsTab(campaignOptions);
        salariesTab = new SalariesTab(campaignOptions);
        turnoverAndRetentionTab = new TurnoverAndRetentionTab(campaignOptions);
    }

    private void initializeAdvancementSection() {
        advancementTab = new AdvancementTab(campaign);
        skillsTab = new SkillsTab(campaignOptions);
        abilitiesTab = new AbilitiesTab();
    }

    private void initializeLogisticsSection() {
        repairAndMaintenanceTab = new RepairAndMaintenanceTab(campaignOptions);
        equipmentAndSuppliesTab = new EquipmentAndSuppliesTab(campaignOptions);
    }

    private void initializeOperationsSection() {
        financesTab = new FinancesTab(campaign);
        marketsTab = new MarketsTab(campaign);
        systemsTab = new SystemsTab(campaign);
        rulesetsTab = new RulesetsTab(campaignOptions);
    }

    private void registerParentRoute(String id, String... titleResourceNames) {
        registerRoute(CampaignOptionsRouteDescriptor.parent(id, titleResourceNames));
    }

    private void registerDirectRoute(String id, Supplier<Component> pageFactory, String... titleResourceNames) {
        registerDirectRoute(id, pageFactory, CampaignOptionsRouteOptions.defaults(), titleResourceNames);
    }

    private void registerDirectRoute(String id, Supplier<Component> pageFactory,
            CampaignOptionsRouteOptions routeOptions, String... titleResourceNames) {
        registerRoute(CampaignOptionsRouteDescriptor.direct(id, pageFactory, routeOptions, titleResourceNames));
    }

    private void registerRoute(CampaignOptionsRouteDescriptor descriptor) {
        List<String> path = new ArrayList<>();
        for (String titleResourceName : descriptor.getTitleResourceNames()) {
            path.add(getTextAt(getCampaignOptionsResourceBundle(), titleResourceName + ".title"));
        }

        if (descriptor.getPageFactory() != null) {
            directPageFactories.put(descriptor.getId(), descriptor.getPageFactory());
        }

        navigationTargets
                .add(new CampaignOptionsRoute(descriptor.getId(), path, descriptor.getTitleResourceNames(),
                        descriptor.shouldShowHelpPanel()));
    }

    private static class CampaignOptionsRouteOptions {
        private static final CampaignOptionsRouteOptions DEFAULT = new CampaignOptionsRouteOptions(true);
        private static final CampaignOptionsRouteOptions WITHOUT_HELP_PANEL = new CampaignOptionsRouteOptions(false);

        private final boolean showHelpPanel;

        private CampaignOptionsRouteOptions(boolean showHelpPanel) {
            this.showHelpPanel = showHelpPanel;
        }

        private static CampaignOptionsRouteOptions defaults() {
            return DEFAULT;
        }

        private static CampaignOptionsRouteOptions withoutHelpPanel() {
            return WITHOUT_HELP_PANEL;
        }

        private boolean shouldShowHelpPanel() {
            return showHelpPanel;
        }
    }

    private static class CampaignOptionsRouteDescriptor {
        private final String id;
        private final List<String> titleResourceNames;
        private final Supplier<Component> pageFactory;
        private final CampaignOptionsRouteOptions routeOptions;

        private CampaignOptionsRouteDescriptor(String id, @Nullable Supplier<Component> pageFactory,
                CampaignOptionsRouteOptions routeOptions, String... titleResourceNames) {
            this.id = id;
            this.pageFactory = pageFactory;
            this.routeOptions = routeOptions;
            this.titleResourceNames = List.of(titleResourceNames);
        }

        private static CampaignOptionsRouteDescriptor parent(String id, String... titleResourceNames) {
            return new CampaignOptionsRouteDescriptor(id, null, CampaignOptionsRouteOptions.defaults(),
                    titleResourceNames);
        }

        private static CampaignOptionsRouteDescriptor direct(String id, Supplier<Component> pageFactory,
                CampaignOptionsRouteOptions routeOptions, String... titleResourceNames) {
            return new CampaignOptionsRouteDescriptor(id, pageFactory, routeOptions, titleResourceNames);
        }

        private String getId() {
            return id;
        }

        private List<String> getTitleResourceNames() {
            return titleResourceNames;
        }

        private Supplier<Component> getPageFactory() {
            return pageFactory;
        }

        private boolean shouldShowHelpPanel() {
            return routeOptions.shouldShowHelpPanel();
        }
    }

    /**
     * Creates the panel for general campaign options. Loads settings for general
     * preferences and initializes it with
     * current campaign options.
     *
     * @param mode the state in which the dialog was triggered.
     *
     * @return a {@link JScrollPane} containing the general tab panel
     */
    private JPanel createGeneralTab(CampaignOptionsDialogMode mode) {
        generalTab = new GeneralTab(campaign, frame, mode);
        JPanel createdGeneralTab = generalTab.createGeneralTab();
        generalTab.loadValuesFromCampaignOptions();

        return createdGeneralTab;
    }

    private JPanel createPersonnelGeneralTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return personnelTab.createGeneralTab();
    }

    private JPanel createPersonnelAwardsTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return personnelTab.createAwardsTab();
    }

    private JPanel createPersonnelMedicalTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return personnelTab.createMedicalTab();
    }

    private JPanel createPersonnelInformationTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return personnelTab.createPersonnelInformationTab();
    }

    private JPanel createPersonnelPrisonersAndDependentsTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return personnelTab.createPrisonersAndDependentsTab();
    }

    private JPanel createBiographyGeneralTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return biographyTab.createGeneralTab();
    }

    private JPanel createBiographyBackgroundsTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return biographyTab.createBackgroundsTab();
    }

    private JPanel createBiographyDeathTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return biographyTab.createDeathTab();
    }

    private JPanel createBiographyEducationTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return biographyTab.createEducationTab();
    }

    private JPanel createBiographyNameAndPortraitGenerationTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return biographyTab.createNameAndPortraitGenerationTab();
    }

    private JPanel createBiographyRankTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return biographyTab.createRankTab();
    }

    private JPanel createRelationshipMarriageTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return relationshipsTab.createMarriageTab();
    }

    private JPanel createRelationshipDivorceTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return relationshipsTab.createDivorceTab();
    }

    private JPanel createRelationshipProcreationTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return relationshipsTab.createProcreationTab();
    }

    private JPanel createCombatSalariesTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return salariesTab.createSalariesTab(PersonnelRoleSubType.COMBAT);
    }

    private JPanel createSupportSalariesTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return salariesTab.createSalariesTab(PersonnelRoleSubType.SUPPORT);
    }

    private JPanel createCivilianSalariesTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return salariesTab.createSalariesTab(PersonnelRoleSubType.CIVILIAN);
    }

    private JPanel createTurnoverAndRetentionTurnoverTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return turnoverAndRetentionTab.createTurnoverTab();
    }

    private JPanel createTurnoverAndRetentionFatigueTab() {
        ensureSectionLoaded("humanResourcesParentTab");
        return turnoverAndRetentionTab.createFatigueTab();
    }

    private JPanel createAdvancementRandomizationTab() {
        ensureSectionLoaded("advancementParentTab");
        return advancementTab.skillRandomizationTab();
    }

    private JPanel createAdvancementXpAwardsTab() {
        ensureSectionLoaded("advancementParentTab");
        return advancementTab.xpAwardsTab();
    }

    private JPanel createAdvancementRecruitmentBonusesTab() {
        ensureSectionLoaded("advancementParentTab");
        return advancementTab.recruitmentBonusesTab();
    }

    private JPanel createAdvancementGunnerySkillsTab() {
        ensureSectionLoaded("advancementParentTab");
        return skillsTab.createSkillsTab(COMBAT_GUNNERY);
    }

    private JPanel createAdvancementPilotingSkillsTab() {
        ensureSectionLoaded("advancementParentTab");
        return skillsTab.createSkillsTab(COMBAT_PILOTING);
    }

    private JPanel createAdvancementSupportSkillsTab() {
        ensureSectionLoaded("advancementParentTab");
        return skillsTab.createSkillsTab(SUPPORT);
    }

    private JPanel createAdvancementUtilitySkillsTab() {
        ensureSectionLoaded("advancementParentTab");
        return skillsTab.createSkillsTab(UTILITY);
    }

    private JPanel createAdvancementRoleplaySkillsTab() {
        ensureSectionLoaded("advancementParentTab");
        return skillsTab.createSkillsTab(ROLEPLAY_GENERAL);
    }

    private JPanel createAdvancementCombatAbilitiesTab() {
        ensureSectionLoaded("advancementParentTab");
        return abilitiesTab.createAbilitiesTab(COMBAT_ABILITY);
    }

    private JPanel createAdvancementManeuveringAbilitiesTab() {
        ensureSectionLoaded("advancementParentTab");
        return abilitiesTab.createAbilitiesTab(MANEUVERING_ABILITY);
    }

    private JPanel createAdvancementUtilityAbilitiesTab() {
        ensureSectionLoaded("advancementParentTab");
        return abilitiesTab.createAbilitiesTab(UTILITY_ABILITY);
    }

    private JPanel createAdvancementCharacterFlawsTab() {
        ensureSectionLoaded("advancementParentTab");
        return abilitiesTab.createAbilitiesTab(CHARACTER_FLAW);
    }

    private JPanel createAdvancementCharacterCreationOnlyTab() {
        ensureSectionLoaded("advancementParentTab");
        return abilitiesTab.createAbilitiesTab(CHARACTER_CREATION_ONLY);
    }

    private JPanel createLogisticsRepairsTab() {
        ensureSectionLoaded("logisticsAndMaintenanceParentTab");
        return repairAndMaintenanceTab.createRepairTab();
    }

    private JPanel createLogisticsMaintenanceTab() {
        ensureSectionLoaded("logisticsAndMaintenanceParentTab");
        return repairAndMaintenanceTab.createMaintenanceTab();
    }

    private JPanel createLogisticsAcquisitionTab() {
        ensureSectionLoaded("logisticsAndMaintenanceParentTab");
        return equipmentAndSuppliesTab.createAcquisitionTab();
    }

    private JPanel createLogisticsPlanetaryAcquisitionTab() {
        ensureSectionLoaded("logisticsAndMaintenanceParentTab");
        return equipmentAndSuppliesTab.createPlanetaryAcquisitionTab();
    }

    private JPanel createLogisticsTechLimitsTab() {
        ensureSectionLoaded("logisticsAndMaintenanceParentTab");
        return equipmentAndSuppliesTab.createTechLimitsTab();
    }

    private JPanel createOperationsFinancesGeneralTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return financesTab.createFinancesGeneralOptionsTab();
    }

    private JPanel createOperationsPriceMultipliersTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return financesTab.createPriceMultipliersTab();
    }

    private JPanel createOperationsPersonnelMarketTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return marketsTab.createPersonnelMarketTab();
    }

    private JPanel createOperationsUnitMarketTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return marketsTab.createUnitMarketTab();
    }

    private JPanel createOperationsContractMarketTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return marketsTab.createContractMarketTab();
    }

    private JPanel createOperationsReputationTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return systemsTab.createReputationTab();
    }

    private JPanel createOperationsFactionStandingTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return systemsTab.createFactionStandingTab();
    }

    private JPanel createOperationsATimeOfWarTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return systemsTab.createATOWTab();
    }

    private JPanel createOperationsStratConTab() {
        ensureSectionLoaded("strategicOperationsParentTab");
        return rulesetsTab.createStratConTab();
    }

    /**
     * Applies the currently configured campaign options to the active
     * {@link Campaign}. This method processes all tabs
     * in the dialog, applying the options to the campaign in logical order (e.g.,
     * "General" first, followed by other
     * categories).
     *
     * @param preset       an optional {@link CampaignPreset} used to override
     *                     campaign options
     * @param mode         the mode in which the application process was triggered
     * @param isSaveAction determines if this action is saving options to a preset
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignPreset preset, CampaignOptionsDialogMode mode,
            boolean isSaveAction) {
        boolean isStartUp = mode == STARTUP || mode == STARTUP_ABRIDGED;

        if (preset != null || isSaveAction) {
            ensureAllSectionsLoaded();
        }

        CampaignOptions options = this.campaignOptions;
        RandomSkillPreferences presetRandomSkillPreferences = null;
        Map<String, SkillType> presetSkills = null;

        if (preset != null) {
            options = preset.getCampaignOptions();
            presetRandomSkillPreferences = preset.getRandomSkillPreferences();
            presetSkills = preset.getSkills();
        }

        CampaignOptionsFreebieTracker oldCampaignOptions = new CampaignOptionsFreebieTracker(
                campaign.getCampaignOptions());

        // Everything assumes general tab will be the first applied.
        // While this shouldn't break anything, it's not worth moving around.
        // For all other tabs, it makes sense to apply them in the order they
        // appear in the dialog; however, this shouldn't make any major difference.
        generalTab.applyCampaignOptionsToCampaign(isStartUp, isSaveAction);

        // Human Resources
        if (personnelTab != null) {
            personnelTab.applyCampaignOptionsToCampaign(campaign, options);
            biographyTab.applyCampaignOptionsToCampaign(options);
            relationshipsTab.applyCampaignOptionsToCampaign(options);
            salariesTab.applyCampaignOptionsToCampaign(options);
            turnoverAndRetentionTab.applyCampaignOptionsToCampaign(options);
        }

        // Advancement
        if (advancementTab != null) {
            advancementTab.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);
            skillsTab.applyCampaignOptionsToCampaign(options, presetSkills);
            abilitiesTab.applyCampaignOptionsToCampaign(preset);
        }

        // Logistics
        if (equipmentAndSuppliesTab != null) {
            equipmentAndSuppliesTab.applyCampaignOptionsToCampaign(options);
            repairAndMaintenanceTab.applyCampaignOptionsToCampaign(options);
        }

        // Operations
        if (financesTab != null) {
            financesTab.applyCampaignOptionsToCampaign(options);
            marketsTab.applyCampaignOptionsToCampaign(options);
            rulesetsTab.applyCampaignOptionsToCampaign(options);
            systemsTab.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);
        }

        // Tidy up
        if (preset == null) {
            recalculateCombatTeams(campaign);
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign, options));

            options.updateGameOptionsFromCampaignOptions(campaign.getGameOptions());
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
        }

        campaign.resetRandomDeath();
        if (campaignGui != null) {
            campaignGui.refreshMarketButtonLabels();
        }

        CampaignOptionsFreebieTracker newCampaignOptions = new CampaignOptionsFreebieTracker(
                campaign.getCampaignOptions());
        triggerUpgradeFreebies(campaign, oldCampaignOptions, newCampaignOptions, isStartUp);
    }

    /**
     * Compares a previously-recorded {@link CampaignOptionsFreebieTracker} snapshot
     * against a new snapshot and triggers
     * any one-time handlers required when critical campaign options are enabled.
     *
     * <p>
     * This method is intended to be called immediately after applying campaign
     * option changes (or when
     * loading/upgrading a campaign) so the campaign can react to newly-enabled
     * systems. Reactions may include prompting
     * the player with confirmation dialogs, adjusting campaign state, or granting
     * "freebies" to keep the save
     * consistent and fair when major rulesets are turned on mid-campaign.
     * </p>
     *
     * <p>
     * Only transitions from {@code false -> true} are acted upon (that is, newly
     * enabled features). Disabling
     * options typically does not require compensation and is therefore ignored
     * here.
     * </p>
     *
     * <p>
     * When {@code isStartUp} is {@code true}, interactive prompts are suppressed;
     * the method may still perform
     * required non-interactive adjustments depending on implementation.
     * </p>
     *
     * @param campaign   the campaign whose state may be adjusted and/or to which
     *                   reports may be added
     * @param oldOptions snapshot of the option state before the change (or
     *                   previously acknowledged state)
     * @param newOptions snapshot of the option state after the change (current
     *                   effective state)
     * @param isStartUp  whether this invocation is happening during
     *                   startup/load/upgrade rather than an in-session
     *                   options change
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static void triggerUpgradeFreebies(Campaign campaign, CampaignOptionsFreebieTracker oldOptions,
            CampaignOptionsFreebieTracker newOptions,
            boolean isStartUp) {
        // Store old values for use if we want to trigger certain dialogs
        boolean oldAwardVeterancySPAs = oldOptions.awardVeterancySPAs();
        boolean oldIsTrackFactionStanding = oldOptions.trackFactionStanding();
        boolean oldIsTrackPrisoners = oldOptions.trackPrisoners();
        boolean oldIsUseMASHTheatres = oldOptions.useMASHTheatres();
        boolean oldIsUseFatigue = oldOptions.useFatigue();
        boolean oldIsUseAdvancedSalvage = oldOptions.useAdvancedSalvage();
        boolean oldIsUseStratCon = oldOptions.useStratCon();
        boolean oldIsUseMapless = oldOptions.useMapless();
        boolean oldIsUseAdvancedScouting = oldOptions.useAdvancedScouting() && oldIsUseStratCon;
        boolean oldIsUseAltAdvancedMedical = oldOptions.useAltAdvancedMedical();
        boolean oldIsUseDiseases = oldIsUseAltAdvancedMedical && oldOptions.useDiseases();
        boolean oldUseNormalizedContractPayModel = oldOptions.useNormalizedContractPayModel();
        boolean oldIsDiminishReturnsContractPay = oldOptions.useDiminishingContractPay();

        boolean newIsTrackFactionStandings = newOptions.trackFactionStanding();
        if (!isStartUp && newIsTrackFactionStandings && !oldIsTrackFactionStanding) { // Has tracking changed?
            FactionStandingCampaignOptionsChangedConfirmationDialog dialog = new FactionStandingCampaignOptionsChangedConfirmationDialog(
                    null,
                    campaign.getCampaignFactionIcon(),
                    campaign.getFaction(),
                    campaign.getLocalDate(),
                    campaign.getFactionStandings(),
                    campaign.getMissions(),
                    newIsTrackFactionStandings,
                    campaign.getCampaignOptions().getRegardMultiplier());

            List<String> reports = dialog.getReports();
            for (String report : reports) {
                if (report != null && !report.isBlank()) {
                    campaign.addReport(POLITICS, report);
                }
            }
        }

        boolean newIsAwardVeterancySPAs = newOptions.awardVeterancySPAs();
        if (!isStartUp && newIsAwardVeterancySPAs && !oldAwardVeterancySPAs) { // Has tracking changed?
            new VeterancyAwardsCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseMASHTheatres = newOptions.useMASHTheatres();
        if (!isStartUp && newIsUseMASHTheatres && !oldIsUseMASHTheatres) { // Has tracking changed?
            new MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsTrackPrisoners = newOptions.trackPrisoners();
        if (!isStartUp && newIsTrackPrisoners && !oldIsTrackPrisoners) { // Has tracking changed?
            new PrisonerTrackingCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseFatigue = newOptions.useFatigue();
        if (!isStartUp && newIsUseFatigue && !oldIsUseFatigue) { // Has tracking changed?
            new FatigueTrackingCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseAdvancedSalvage = newOptions.useAdvancedSalvage();
        if (!isStartUp && newIsUseAdvancedSalvage && !oldIsUseAdvancedSalvage) { // Has tracking changed?
            new SalvageCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseStratCon = newOptions.useStratCon();
        if (!isStartUp && newIsUseStratCon && !oldIsUseStratCon) { // Has tracking changed?
            new StratConConvoyCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseMapless = newOptions.useMapless();
        if (!isStartUp && newIsUseMapless && !oldIsUseMapless) { // Has tracking changed?
            new StratConMaplessCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseAdvancedScouting = newOptions.useAdvancedScouting() && newIsUseStratCon;
        if (!isStartUp && newIsUseAdvancedScouting && !oldIsUseAdvancedScouting) { // Has tracking changed?
            new AdvancedScoutingCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseAltAdvancedMedical = newOptions.useAltAdvancedMedical();
        if (!isStartUp && newIsUseAltAdvancedMedical && !oldIsUseAltAdvancedMedical) { // Has tracking changed?
            new AltAdvancedMedicalCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsUseDiseases = newIsUseAltAdvancedMedical && newOptions.useDiseases();
        if (!isStartUp && newIsUseDiseases && !oldIsUseDiseases) { // Has tracking changed?
            inoculateAllCharacters(campaign);
        }

        boolean newUseNormalizedContractPayModel = newOptions.useNormalizedContractPayModel();
        if (!isStartUp && newUseNormalizedContractPayModel && !oldUseNormalizedContractPayModel) {
            new NormalizedContractPayCampaignOptionsChangedConfirmationDialog(campaign);
        }

        boolean newIsDiminishReturnsContractPay = newOptions.useDiminishingContractPay();
        if (!isStartUp && newIsDiminishReturnsContractPay && !oldIsDiminishReturnsContractPay) {
            new DiminishingReturnsCampaignOptionsChangedConfirmationDialog(campaign);
        }
    }

    /**
     * Inoculates all campaign personnel for their current planet and origin planet.
     *
     * <p>
     * This method adds planetary inoculation records for:
     * </p>
     *
     * <ul>
     * <li>The current planet (if the campaign is on a planet, not in transit)</li>
     * <li>Each person's origin planet</li>
     * </ul>
     *
     * <p>
     * Personnel are assumed to have prior inoculation for their home planet, while
     * current planet inoculation
     * requires campaign location tracking.
     * </p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void inoculateAllCharacters(Campaign campaign) {
        final AbstractLocation location = campaign.getCurrentLocation();
        final LocalDate currentDay = campaign.getLocalDate();

        final Map<String, Set<InjuryType>> curesBySystem = new HashMap<>();

        final Planet planet = location.isOnPlanet() ? location.getPlanet() : null;
        final String planetId = (planet != null) ? planet.getId() : null;
        final String systemId = (planet != null) ? planet.getParentSystem().getId() : null;

        for (Person person : campaign.getAllPersonnel()) {
            // Inoculate for current location, if applicable
            if (planet != null) {
                inoculate(person, planet, planetId, systemId, currentDay, curesBySystem);
            }

            // Inoculate for origin planet
            final Planet origin = person.getOriginPlanet();
            if (origin == null) {
                continue;
            }

            inoculate(
                    person,
                    origin,
                    origin.getId(),
                    origin.getParentSystem().getId(),
                    currentDay,
                    curesBySystem);
        }
    }

    private static void inoculate(Person person, Planet planet, String planetId, String systemId, LocalDate today,
            Map<String, Set<InjuryType>> curesBySystem) {
        if (!person.hasPlanetaryInoculation(planetId)) {
            person.addPlanetaryInoculation(planetId);
            MedicalLogger.inoculation(person, today, planet.getName(today));
        }

        final Set<InjuryType> activeCures = curesBySystem.computeIfAbsent(systemId,
                id -> getAllSystemSpecificDiseasesWithCures(id, today, true));

        for (InjuryType injuryType : activeCures) {
            if (!person.hasCanonDiseaseInoculation(injuryType.getKey())) {
                person.addCanonDiseaseInoculation(injuryType.getKey());
                MedicalLogger.specificInoculation(person, today, injuryType.getSimpleName());
            }
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
     * Applies the values from a {@link CampaignPreset} to all tabs in the dialog.
     * This propagates preset-specific
     * configuration to all associated components and sub-tabs, including
     * campaign-related properties such as dates,
     * factions, and skills.
     *
     * @param campaignPreset the {@link CampaignPreset} containing the preset
     *                       options to apply
     * @param isStartup      {@code true} if the preset is being loaded during new
     *                       campaign startup
     */
    public void applyPreset(@Nullable CampaignPreset campaignPreset, boolean isStartup) {
        if (campaignPreset == null) {
            return;
        }

        ensureAllSectionsLoaded();
        CampaignOptions presetCampaignOptions = campaignPreset.getCampaignOptions();

        LocalDate presetDate = campaign.getLocalDate();
        Faction presetFaction = campaign.getFaction();
        if (isStartup) {
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
        advancementTab.loadValuesFromCampaignOptions(presetCampaignOptions,
                campaignPreset.getRandomSkillPreferences());
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
