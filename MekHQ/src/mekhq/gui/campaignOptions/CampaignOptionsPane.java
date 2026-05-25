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

import static java.lang.Math.round;
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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createSubTabs;
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
import javax.swing.JTabbedPane;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
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
import mekhq.gui.campaignOptions.contents.*;
import mekhq.gui.campaignOptions.optionChangeDialogs.*;

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
public class CampaignOptionsPane extends JPanel {
    private static final int SCROLL_SPEED = 16;
    private static final int HEADER_FONT_SIZE = 5;
    private static final int NAVIGATION_WIDTH = 240;

    private final JFrame frame;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final CampaignOptionsDialogMode mode;
    private final List<CampaignOptionsRoute> navigationTargets = new ArrayList<>();
      private final Map<String, Supplier<Component>> directPageFactories = new HashMap<>();
      private final Map<String, Component> directPageCache = new HashMap<>();
    private final Map<String, Integer> topLevelTabIndexes = new HashMap<>();

    private JTabbedPane legacyTabbedPane;
    private CampaignOptionsContentHost activeContentHost;
    private CampaignOptionsNavigationPanel navigationPanel;
    private boolean isSyncingNavigationSelection;
    private boolean suppressRouteRegistration;

    private GeneralTab generalTab;
    private JTabbedPane humanResourcesParentTab;
    private JTabbedPane advancementParentTab;
    private JTabbedPane equipmentAndSuppliesParentTab;
    private JTabbedPane strategicOperationsParentTab;
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
     * Initializes the campaign options pane by creating all parent tabs and adding sub-tabs for various campaign
     * settings categories. Dynamically adjusts tab fonts and layout based on UI scaling settings.
     */
    protected void initialize() {
        legacyTabbedPane = new JTabbedPane();
        legacyTabbedPane.setName("campaignOptionsLegacyTabbedPane");

            JPanel generalPage = createGeneralTab(mode);
            registerDirectPage("general", () -> generalPage);
            registerDirectPage("human-resources.personnel.general", this::createPersonnelGeneralTab);
            registerDirectPage("human-resources.personnel.awards", this::createPersonnelAwardsTab);
            registerDirectPage("human-resources.personnel.medical", this::createPersonnelMedicalTab);
            registerDirectPage("human-resources.personnel.information", this::createPersonnelInformationTab);
            registerDirectPage("human-resources.personnel.prisoners-and-civilians",
                  this::createPersonnelPrisonersAndDependentsTab);
            registerDirectPage("human-resources.biography.general", this::createBiographyGeneralTab);
            registerDirectPage("human-resources.biography.backgrounds", this::createBiographyBackgroundsTab);
            registerDirectPage("human-resources.biography.death", this::createBiographyDeathTab);
            registerDirectPage("human-resources.biography.education", this::createBiographyEducationTab);
            registerDirectPage("human-resources.biography.name-and-portraits",
                  this::createBiographyNameAndPortraitGenerationTab);
            registerDirectPage("human-resources.biography.rank", this::createBiographyRankTab);
            registerDirectPage("human-resources.relationships.marriage", this::createRelationshipMarriageTab);
            registerDirectPage("human-resources.relationships.divorce", this::createRelationshipDivorceTab);
            registerDirectPage("human-resources.relationships.procreation", this::createRelationshipProcreationTab);
                  registerDirectPage("human-resources.salaries.combat", this::createCombatSalariesTab);
                  registerDirectPage("human-resources.salaries.support", this::createSupportSalariesTab);
                  registerDirectPage("human-resources.salaries.civilian", this::createCivilianSalariesTab);
                  registerDirectPage("human-resources.turnover-and-retention.turnover",
                        this::createTurnoverAndRetentionTurnoverTab);
                  registerDirectPage("human-resources.turnover-and-retention.fatigue",
                        this::createTurnoverAndRetentionFatigueTab);
                  registerDirectPage("advancement.awards-and-randomization.randomization",
                        this::createAdvancementRandomizationTab);
                  registerDirectPage("advancement.awards-and-randomization.xp-awards",
                        this::createAdvancementXpAwardsTab);
                  registerDirectPage("advancement.awards-and-randomization.recruitment-bonuses",
                        this::createAdvancementRecruitmentBonusesTab);
                  registerDirectPage("advancement.skills.gunnery", this::createAdvancementGunnerySkillsTab);
                  registerDirectPage("advancement.skills.piloting", this::createAdvancementPilotingSkillsTab);
                  registerDirectPage("advancement.skills.support", this::createAdvancementSupportSkillsTab);
                  registerDirectPage("advancement.skills.utility", this::createAdvancementUtilitySkillsTab);
                  registerDirectPage("advancement.skills.roleplay", this::createAdvancementRoleplaySkillsTab);
                  registerDirectPage("advancement.abilities.combat", this::createAdvancementCombatAbilitiesTab);
                  registerDirectPage("advancement.abilities.maneuvering",
                        this::createAdvancementManeuveringAbilitiesTab);
                  registerDirectPage("advancement.abilities.utility", this::createAdvancementUtilityAbilitiesTab);
                  registerDirectPage("advancement.abilities.character-flaws",
                        this::createAdvancementCharacterFlawsTab);
                  registerDirectPage("advancement.abilities.character-creation-only",
                        this::createAdvancementCharacterCreationOnlyTab);
                  registerDirectPage("logistics.repairs-and-maintenance.repairs", this::createLogisticsRepairsTab);
                  registerDirectPage("logistics.repairs-and-maintenance.maintenance",
                        this::createLogisticsMaintenanceTab);
                  registerDirectPage("logistics.supplies-and-acquisition.acquisition",
                        this::createLogisticsAcquisitionTab);
                  registerDirectPage("logistics.supplies-and-acquisition.planetary-acquisition",
                        this::createLogisticsPlanetaryAcquisitionTab);
                  registerDirectPage("logistics.supplies-and-acquisition.tech-limits",
                        this::createLogisticsTechLimitsTab);
                  registerDirectPage("operations.finances.general", this::createOperationsFinancesGeneralTab);
                  registerDirectPage("operations.finances.price-multipliers",
                        this::createOperationsPriceMultipliersTab);
                  registerDirectPage("operations.markets.personnel", this::createOperationsPersonnelMarketTab);
                  registerDirectPage("operations.markets.units", this::createOperationsUnitMarketTab);
                  registerDirectPage("operations.markets.contracts", this::createOperationsContractMarketTab);
                  registerDirectPage("operations.systems.reputation", this::createOperationsReputationTab);
                  registerDirectPage("operations.systems.faction-standing", this::createOperationsFactionStandingTab);
                  registerDirectPage("operations.systems.a-time-of-war", this::createOperationsATimeOfWarTab);
                  registerDirectPage("operations.rulesets.stratcon", this::createOperationsStratConTab);
        addLazyLegacyTab("humanResourcesParentTab");
        addLazyLegacyTab("advancementParentTab");
        addLazyLegacyTab("logisticsAndMaintenanceParentTab");
        addLazyLegacyTab("strategicOperationsParentTab");
        registerRoutes();

        legacyTabbedPane.addChangeListener(evt -> selectTopLevelRoute(legacyTabbedPane.getSelectedIndex()));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
              createNavigationPanel(),
              createContentHost(generalPage));
        splitPane.setName("campaignOptionsSplitPane");
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(NAVIGATION_WIDTH);
        add(splitPane, BorderLayout.CENTER);
        navigationPanel.selectRoute(navigationTargets.get(0));
    }

    /**
     * Adds a new tab to the legacy tab pane. This keeps the old content available while the route-based shell is built
     * out incrementally.
     *
     * @param resourceName the resource string key to locate the tab title
     * @param component    the component to add as tab content
     */
    private void addLegacyTab(String resourceName, Component component) {
        legacyTabbedPane.addTab(getTopLevelTabTitle(resourceName), component);
        topLevelTabIndexes.put(resourceName, legacyTabbedPane.getTabCount() - 1);
    }

    private CampaignOptionsNavigationPanel createNavigationPanel() {
        navigationPanel = new CampaignOptionsNavigationPanel(navigationTargets, this::selectedNavigationTarget);
        return navigationPanel;
    }

      private CampaignOptionsContentHost createContentHost(Component initialContent) {
            activeContentHost = new CampaignOptionsContentHost(initialContent);
        return activeContentHost;
    }

    private void addLazyLegacyTab(String resourceName) {
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setName("pnl" + resourceName + "Placeholder");
        addLegacyTab(resourceName, placeholder);
    }
    private void registerRoutes() {
        registerStaticRoute("general", "generalPanel");

        registerStaticRoute("human-resources", "humanResourcesParentTab");
        registerStaticRoute("human-resources.personnel", "humanResourcesParentTab", "personnelContentTabs");
        registerStaticRoute("human-resources.personnel.general", "humanResourcesParentTab", "personnelContentTabs",
              "personnelGeneralTab");
        registerStaticRoute("human-resources.personnel.awards", "humanResourcesParentTab", "personnelContentTabs",
              "awardsTab");
        registerStaticRoute("human-resources.personnel.medical", "humanResourcesParentTab", "personnelContentTabs",
              "medicalTab");
        registerStaticRoute("human-resources.personnel.information", "humanResourcesParentTab", "personnelContentTabs",
              "personnelInformationTab");
        registerStaticRoute("human-resources.personnel.prisoners-and-civilians",
              "humanResourcesParentTab", "personnelContentTabs", "prisonersAndDependentsTab");
        registerStaticRoute("human-resources.biography", "humanResourcesParentTab", "biographyContentTabs");
        registerStaticRoute("human-resources.biography.general", "humanResourcesParentTab", "biographyContentTabs",
              "biographyGeneralTab");
        registerStaticRoute("human-resources.biography.backgrounds", "humanResourcesParentTab", "biographyContentTabs",
              "backgroundsTab");
        registerStaticRoute("human-resources.biography.death", "humanResourcesParentTab", "biographyContentTabs",
              "deathTab");
        registerStaticRoute("human-resources.biography.education", "humanResourcesParentTab", "biographyContentTabs",
              "educationTab");
        registerStaticRoute("human-resources.biography.name-and-portraits", "humanResourcesParentTab",
              "biographyContentTabs", "nameAndPortraitGenerationTab");
        registerStaticRoute("human-resources.biography.rank", "humanResourcesParentTab", "biographyContentTabs",
              "rankTab");
        registerStaticRoute("human-resources.relationships", "humanResourcesParentTab", "relationshipsContentTabs");
        registerStaticRoute("human-resources.relationships.marriage", "humanResourcesParentTab",
              "relationshipsContentTabs", "marriageTab");
        registerStaticRoute("human-resources.relationships.divorce", "humanResourcesParentTab",
              "relationshipsContentTabs", "divorceTab");
        registerStaticRoute("human-resources.relationships.procreation", "humanResourcesParentTab",
              "relationshipsContentTabs", "procreationTab");
        registerStaticRoute("human-resources.salaries", "humanResourcesParentTab", "salariesContentTabs");
        registerStaticRoute("human-resources.salaries.combat", "humanResourcesParentTab", "salariesContentTabs",
              "0combatSalariesTab");
        registerStaticRoute("human-resources.salaries.support", "humanResourcesParentTab", "salariesContentTabs",
              "1supportSalariesTab");
        registerStaticRoute("human-resources.salaries.civilian", "humanResourcesParentTab", "salariesContentTabs",
              "2civilianSalariesTab");
        registerStaticRoute("human-resources.turnover-and-retention", "humanResourcesParentTab",
              "turnoverAndRetentionContentTabs");
        registerStaticRoute("human-resources.turnover-and-retention.turnover", "humanResourcesParentTab",
              "turnoverAndRetentionContentTabs", "turnoverTab");
        registerStaticRoute("human-resources.turnover-and-retention.fatigue", "humanResourcesParentTab",
              "turnoverAndRetentionContentTabs", "fatigueTab");

        registerStaticRoute("advancement", "advancementParentTab");
        registerStaticRoute("advancement.awards-and-randomization", "advancementParentTab",
              "awardsAndRandomizationContentTabs");
        registerStaticRoute("advancement.awards-and-randomization.randomization", "advancementParentTab",
              "awardsAndRandomizationContentTabs", "0randomizationTab");
        registerStaticRoute("advancement.awards-and-randomization.xp-awards", "advancementParentTab",
              "awardsAndRandomizationContentTabs", "1xpAwardsTab");
        registerStaticRoute("advancement.awards-and-randomization.recruitment-bonuses", "advancementParentTab",
              "awardsAndRandomizationContentTabs", "2recruitmentBonusesTab");
        registerStaticRoute("advancement.skills", "advancementParentTab", "skillsContentTabs");
        registerStaticRoute("advancement.skills.gunnery", "advancementParentTab", "skillsContentTabs",
              "0gunnerySkillsTab");
        registerStaticRoute("advancement.skills.piloting", "advancementParentTab", "skillsContentTabs",
              "1pilotingSkillsTab");
        registerStaticRoute("advancement.skills.support", "advancementParentTab", "skillsContentTabs",
              "2supportSkillsTab");
        registerStaticRoute("advancement.skills.utility", "advancementParentTab", "skillsContentTabs",
              "3utilitySkillsTab");
        registerStaticRoute("advancement.skills.roleplay", "advancementParentTab", "skillsContentTabs",
              "4roleplaySkillsTab");
        registerStaticRoute("advancement.abilities", "advancementParentTab", "abilityContentTabs");
        registerStaticRoute("advancement.abilities.combat", "advancementParentTab", "abilityContentTabs",
              "0combatAbilitiesTab");
        registerStaticRoute("advancement.abilities.maneuvering", "advancementParentTab", "abilityContentTabs",
              "1maneuveringAbilitiesTab");
        registerStaticRoute("advancement.abilities.utility", "advancementParentTab", "abilityContentTabs",
              "2utilityAbilitiesTab");
        registerStaticRoute("advancement.abilities.character-flaws", "advancementParentTab", "abilityContentTabs",
              "3characterFlawsTab");
        registerStaticRoute("advancement.abilities.character-creation-only", "advancementParentTab",
              "abilityContentTabs", "4characterCreationOnlyTab");

        registerStaticRoute("logistics", "logisticsAndMaintenanceParentTab");
        registerStaticRoute("logistics.repairs-and-maintenance", "logisticsAndMaintenanceParentTab",
              "repairsAndMaintenanceContentTabs");
        registerStaticRoute("logistics.repairs-and-maintenance.repairs", "logisticsAndMaintenanceParentTab",
              "repairsAndMaintenanceContentTabs", "repairTab");
        registerStaticRoute("logistics.repairs-and-maintenance.maintenance", "logisticsAndMaintenanceParentTab",
              "repairsAndMaintenanceContentTabs", "maintenanceTab");
        registerStaticRoute("logistics.supplies-and-acquisition", "logisticsAndMaintenanceParentTab",
              "suppliesAndAcquisitionContentTabs");
        registerStaticRoute("logistics.supplies-and-acquisition.acquisition", "logisticsAndMaintenanceParentTab",
              "suppliesAndAcquisitionContentTabs", "acquisitionTab");
        registerStaticRoute("logistics.supplies-and-acquisition.planetary-acquisition",
              "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs", "planetaryAcquisitionTab");
        registerStaticRoute("logistics.supplies-and-acquisition.tech-limits", "logisticsAndMaintenanceParentTab",
              "suppliesAndAcquisitionContentTabs", "techLimitsTab");

        registerStaticRoute("operations", "strategicOperationsParentTab");
        registerStaticRoute("operations.finances", "strategicOperationsParentTab", "financesContentTabs");
        registerStaticRoute("operations.finances.general", "strategicOperationsParentTab", "financesContentTabs",
              "financesGeneralTab");
        registerStaticRoute("operations.finances.price-multipliers", "strategicOperationsParentTab",
              "financesContentTabs", "priceMultipliersTab");
        registerStaticRoute("operations.markets", "strategicOperationsParentTab", "marketsContentTabs");
        registerStaticRoute("operations.markets.personnel", "strategicOperationsParentTab", "marketsContentTabs",
              "personnelMarketTab");
        registerStaticRoute("operations.markets.units", "strategicOperationsParentTab", "marketsContentTabs",
              "unitMarketTab");
        registerStaticRoute("operations.markets.contracts", "strategicOperationsParentTab", "marketsContentTabs",
              "contractMarketTab");
        registerStaticRoute("operations.systems", "strategicOperationsParentTab", "systemsContentTabs");
        registerStaticRoute("operations.systems.reputation", "strategicOperationsParentTab", "systemsContentTabs",
              "reputationTab");
        registerStaticRoute("operations.systems.faction-standing", "strategicOperationsParentTab",
              "systemsContentTabs", "factionStandingTab");
        registerStaticRoute("operations.systems.a-time-of-war", "strategicOperationsParentTab", "systemsContentTabs",
              "atowTab");
        registerStaticRoute("operations.rulesets", "strategicOperationsParentTab", "rulesetsContentTabs");
        registerStaticRoute("operations.rulesets.stratcon", "strategicOperationsParentTab", "rulesetsContentTabs",
              "stratConGeneralTab");

    }

    private String getTopLevelTabTitle(String resourceName) {
        double uiScale = 1;
        try {
            uiScale = Double.parseDouble(System.getProperty("flatlaf.uiScale"));
        } catch (Exception ignored) {
        }

        return String.format("<html><font size=%s><b>%s</b></font></html>",
              round(HEADER_FONT_SIZE * uiScale),
              getTextAt(getCampaignOptionsResourceBundle(), resourceName + ".title"));
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

    private void selectTopLevelRoute(int tabIndex) {
        if (isSyncingNavigationSelection) {
            return;
        }

        for (CampaignOptionsRoute route : navigationTargets) {
            if (isTopLevelRouteFor(route, tabIndex)) {
                          CampaignOptionsRoute effectiveRoute = getDefaultDirectRoute(route);
                          if (effectiveRoute != route) {
                                    navigationPanel.selectRoute(effectiveRoute);
                                    selectRoute(effectiveRoute);
                                    resetContentScrollPosition();
                                    return;
                          }

                ensureSectionLoaded(route.getTopLevelResourceName());
                navigationPanel.selectRoute(route);
                return;
            }
        }
    }

    private void selectRoute(CampaignOptionsRoute route) {
            CampaignOptionsRoute effectiveRoute = getDefaultDirectRoute(route);
            if (effectiveRoute != route) {
                  navigationPanel.selectRoute(effectiveRoute);
                  route = effectiveRoute;
            }

            if (showDirectRoute(route)) {
                  return;
            }

        List<String> titleResourceNames = route.getTitleResourceNames();
        if (titleResourceNames.isEmpty()) {
            return;
        }

            activeContentHost.setContent(legacyTabbedPane);
        ensureSectionLoaded(route.getTopLevelResourceName());
        Component selectedComponent = selectTab(legacyTabbedPane, titleResourceNames.get(0));
        for (int index = 1; index < titleResourceNames.size(); index++) {
            if (!(selectedComponent instanceof JTabbedPane tabbedPane)) {
                return;
            }

            selectedComponent = selectTab(tabbedPane, titleResourceNames.get(index));
        }
    }

      private boolean showDirectRoute(CampaignOptionsRoute route) {
            Component directPage = getDirectPage(route.getId());
            if (directPage == null) {
                  return false;
            }

            activeContentHost.setContent(directPage, getQuoteResourceName(route));
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
            return directPageCache.get(routeId);
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

    private Component selectTab(JTabbedPane tabbedPane, String titleResourceName) {
        int tabIndex = getTabIndex(tabbedPane, titleResourceName);
        if (tabIndex >= 0 && tabbedPane.getSelectedIndex() != tabIndex) {
            tabbedPane.setSelectedIndex(tabIndex);
        }

        return tabbedPane.getSelectedComponent();
    }

    private int getTabIndex(JTabbedPane tabbedPane, String titleResourceName) {
        String title = getTextAt(getCampaignOptionsResourceBundle(), titleResourceName + ".title");
        for (int index = 0; index < tabbedPane.getTabCount(); index++) {
            String tabTitle = tabbedPane.getTitleAt(index);
            if (title.equals(tabTitle) || tabTitle.contains(title)) {
                return index;
            }
        }

        return -1;
    }

    private boolean isTopLevelRouteFor(CampaignOptionsRoute route, int tabIndex) {
        Integer routeTabIndex = topLevelTabIndexes.get(route.getTopLevelResourceName());
        return route.isTopLevelRoute() && routeTabIndex != null && routeTabIndex == tabIndex;
    }

    private void ensureSectionLoaded(String topLevelResourceName) {
        switch (topLevelResourceName) {
            case "humanResourcesParentTab" -> {
                if (humanResourcesParentTab == null) {
                    humanResourcesParentTab = createSectionContent(this::createHumanResourcesParentTab);
                    setLegacyTabComponent(topLevelResourceName, humanResourcesParentTab);
                }
            }
            case "advancementParentTab" -> {
                if (advancementParentTab == null) {
                    advancementParentTab = createSectionContent(this::createAdvancementParentTab);
                    setLegacyTabComponent(topLevelResourceName, advancementParentTab);
                }
            }
            case "logisticsAndMaintenanceParentTab" -> {
                if (equipmentAndSuppliesParentTab == null) {
                    equipmentAndSuppliesParentTab = createSectionContent(this::createEquipmentAndSuppliesParentTab);
                    setLegacyTabComponent(topLevelResourceName, equipmentAndSuppliesParentTab);
                }
            }
            case "strategicOperationsParentTab" -> {
                if (strategicOperationsParentTab == null) {
                    strategicOperationsParentTab = createSectionContent(this::createStrategicOperationsParentTab);
                    setLegacyTabComponent(topLevelResourceName, strategicOperationsParentTab);
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

    private JTabbedPane createSectionContent(SectionFactory sectionFactory) {
        suppressRouteRegistration = true;
        try {
            return sectionFactory.create();
        } finally {
            suppressRouteRegistration = false;
        }
    }

    private void setLegacyTabComponent(String resourceName, Component component) {
        Integer tabIndex = topLevelTabIndexes.get(resourceName);
        if (tabIndex != null) {
            legacyTabbedPane.setComponentAt(tabIndex, component);
        }
    }

    @FunctionalInterface
    private interface SectionFactory {
        JTabbedPane create();
    }

    private void registerStaticRoute(String id, String... titleResourceNames) {
        registerRoute(id, titleResourceNames);
    }

      private void registerDirectPage(String routeId, Supplier<Component> pageFactory) {
            directPageFactories.put(routeId, pageFactory);
      }

      private void ensureDirectPagesLoaded() {
            ensureDirectPagesLoaded("");
      }

      private void ensureDirectPagesLoaded(String routeIdPrefix) {
            for (CampaignOptionsRoute route : navigationTargets) {
                  String routeId = route.getId();
                  if (routeId.startsWith(routeIdPrefix) && directPageFactories.containsKey(routeId) &&
                              shouldPreloadDirectPage(routeId)) {
                        getDirectPage(routeId);
                  }
            }
      }

      private boolean shouldPreloadDirectPage(String routeId) {
            return !routeId.startsWith("advancement.abilities.") &&
                         !routeId.startsWith("advancement.skills.");
      }

    private void registerRoute(String id, String[] titleResourceNames, TabSelection... selections) {
        if (suppressRouteRegistration) {
            return;
        }

        List<String> path = new ArrayList<>();
        for (String titleResourceName : titleResourceNames) {
            path.add(getTextAt(getCampaignOptionsResourceBundle(), titleResourceName + ".title"));
        }

        navigationTargets.add(new CampaignOptionsRoute(id, path, List.of(titleResourceNames)));
    }

    private static class TabSelection {
        private TabSelection(JTabbedPane tabbedPane, String titleResourceName) {
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
    private JPanel createGeneralTab(CampaignOptionsDialogMode mode) {
        generalTab = new GeneralTab(campaign, frame, mode);
        JPanel createdGeneralTab = generalTab.createGeneralTab();
        generalTab.loadValuesFromCampaignOptions();

        return createdGeneralTab;
    }

      private JPanel createPersonnelGeneralTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdPersonnelGeneralTab = personnelTab.createGeneralTab();
            personnelTab.loadValuesFromCampaignOptions(campaign.getVersion());

            return createdPersonnelGeneralTab;
      }

      private JPanel createPersonnelAwardsTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdPersonnelAwardsTab = personnelTab.createAwardsTab();
            personnelTab.loadValuesFromCampaignOptions(campaign.getVersion());

            return createdPersonnelAwardsTab;
      }

      private JPanel createPersonnelMedicalTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdPersonnelMedicalTab = personnelTab.createMedicalTab();
            personnelTab.loadValuesFromCampaignOptions(campaign.getVersion());

            return createdPersonnelMedicalTab;
      }

      private JPanel createPersonnelInformationTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdPersonnelInformationTab = personnelTab.createPersonnelInformationTab();
            personnelTab.loadValuesFromCampaignOptions(campaign.getVersion());

            return createdPersonnelInformationTab;
      }

      private JPanel createPersonnelPrisonersAndDependentsTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdPersonnelPrisonersAndDependentsTab = personnelTab.createPrisonersAndDependentsTab();
            personnelTab.loadValuesFromCampaignOptions(campaign.getVersion());

            return createdPersonnelPrisonersAndDependentsTab;
      }

      private JPanel createBiographyGeneralTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdBiographyGeneralTab = biographyTab.createGeneralTab();
            biographyTab.loadValuesFromCampaignOptions();

            return createdBiographyGeneralTab;
      }

      private JPanel createBiographyBackgroundsTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdBiographyBackgroundsTab = biographyTab.createBackgroundsTab();
            biographyTab.loadValuesFromCampaignOptions();

            return createdBiographyBackgroundsTab;
      }

      private JPanel createBiographyDeathTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdBiographyDeathTab = biographyTab.createDeathTab();
            biographyTab.loadValuesFromCampaignOptions();

            return createdBiographyDeathTab;
      }

      private JPanel createBiographyEducationTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdBiographyEducationTab = biographyTab.createEducationTab();
            biographyTab.loadValuesFromCampaignOptions();

            return createdBiographyEducationTab;
      }

      private JPanel createBiographyNameAndPortraitGenerationTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdBiographyNameAndPortraitGenerationTab = biographyTab.createNameAndPortraitGenerationTab();
            biographyTab.loadValuesFromCampaignOptions();

            return createdBiographyNameAndPortraitGenerationTab;
      }

      private JPanel createBiographyRankTab() {
            ensureSectionLoaded("humanResourcesParentTab");
            JPanel createdBiographyRankTab = biographyTab.createRankTab();
            biographyTab.loadValuesFromCampaignOptions();

            return createdBiographyRankTab;
      }

      private JPanel createRelationshipMarriageTab() {
            ensureRelationshipDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.relationships.marriage");
      }

      private JPanel createRelationshipDivorceTab() {
            ensureRelationshipDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.relationships.divorce");
      }

      private JPanel createRelationshipProcreationTab() {
            ensureRelationshipDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.relationships.procreation");
      }

      private void ensureRelationshipDirectPagesCreated() {
            ensureSectionLoaded("humanResourcesParentTab");
            if (directPageCache.containsKey("human-resources.relationships.marriage") &&
                          directPageCache.containsKey("human-resources.relationships.divorce") &&
                          directPageCache.containsKey("human-resources.relationships.procreation")) {
                  return;
            }

            directPageCache.put("human-resources.relationships.marriage", relationshipsTab.createMarriageTab());
            directPageCache.put("human-resources.relationships.divorce", relationshipsTab.createDivorceTab());
            directPageCache.put("human-resources.relationships.procreation", relationshipsTab.createProcreationTab());
            relationshipsTab.loadValuesFromCampaignOptions();
      }

      private JPanel createCombatSalariesTab() {
            ensureSalaryDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.salaries.combat");
      }

      private JPanel createSupportSalariesTab() {
            ensureSalaryDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.salaries.support");
      }

      private JPanel createCivilianSalariesTab() {
            ensureSalaryDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.salaries.civilian");
      }

      private void ensureSalaryDirectPagesCreated() {
            ensureSectionLoaded("humanResourcesParentTab");
            if (directPageCache.containsKey("human-resources.salaries.combat") &&
                          directPageCache.containsKey("human-resources.salaries.support") &&
                          directPageCache.containsKey("human-resources.salaries.civilian")) {
                  return;
            }

            directPageCache.put("human-resources.salaries.combat",
                    salariesTab.createSalariesTab(PersonnelRoleSubType.COMBAT));
            directPageCache.put("human-resources.salaries.support",
                    salariesTab.createSalariesTab(PersonnelRoleSubType.SUPPORT));
            directPageCache.put("human-resources.salaries.civilian",
                    salariesTab.createSalariesTab(PersonnelRoleSubType.CIVILIAN));
            salariesTab.loadValuesFromCampaignOptions();
      }

      private JPanel createTurnoverAndRetentionTurnoverTab() {
            ensureTurnoverAndRetentionDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.turnover-and-retention.turnover");
      }

      private JPanel createTurnoverAndRetentionFatigueTab() {
            ensureTurnoverAndRetentionDirectPagesCreated();
            return (JPanel) directPageCache.get("human-resources.turnover-and-retention.fatigue");
      }

      private void ensureTurnoverAndRetentionDirectPagesCreated() {
            ensureSectionLoaded("humanResourcesParentTab");
            if (directPageCache.containsKey("human-resources.turnover-and-retention.turnover") &&
                          directPageCache.containsKey("human-resources.turnover-and-retention.fatigue")) {
                  return;
            }

            directPageCache.put("human-resources.turnover-and-retention.turnover",
                  turnoverAndRetentionTab.createTurnoverTab());
            directPageCache.put("human-resources.turnover-and-retention.fatigue",
                  turnoverAndRetentionTab.createFatigueTab());
            turnoverAndRetentionTab.loadValuesFromCampaignOptions();
      }

      private JPanel createAdvancementRandomizationTab() {
            ensureAwardsAndRandomizationDirectPagesCreated();
            return (JPanel) directPageCache.get("advancement.awards-and-randomization.randomization");
      }

      private JPanel createAdvancementXpAwardsTab() {
            ensureAwardsAndRandomizationDirectPagesCreated();
            return (JPanel) directPageCache.get("advancement.awards-and-randomization.xp-awards");
      }

      private JPanel createAdvancementRecruitmentBonusesTab() {
            ensureAwardsAndRandomizationDirectPagesCreated();
            return (JPanel) directPageCache.get("advancement.awards-and-randomization.recruitment-bonuses");
      }

      private void ensureAwardsAndRandomizationDirectPagesCreated() {
            ensureSectionLoaded("advancementParentTab");
            if (directPageCache.containsKey("advancement.awards-and-randomization.randomization") &&
                          directPageCache.containsKey("advancement.awards-and-randomization.xp-awards") &&
                          directPageCache.containsKey("advancement.awards-and-randomization.recruitment-bonuses")) {
                  return;
            }

            directPageCache.put("advancement.awards-and-randomization.randomization",
                  advancementTab.skillRandomizationTab());
            directPageCache.put("advancement.awards-and-randomization.xp-awards", advancementTab.xpAwardsTab());
            directPageCache.put("advancement.awards-and-randomization.recruitment-bonuses",
                  advancementTab.recruitmentBonusesTab());
            advancementTab.loadValuesFromCampaignOptions();
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
            ensureRepairAndMaintenanceDirectPagesCreated();
            return (JPanel) directPageCache.get("logistics.repairs-and-maintenance.repairs");
      }

      private JPanel createLogisticsMaintenanceTab() {
            ensureRepairAndMaintenanceDirectPagesCreated();
            return (JPanel) directPageCache.get("logistics.repairs-and-maintenance.maintenance");
      }

      private void ensureRepairAndMaintenanceDirectPagesCreated() {
            ensureSectionLoaded("logisticsAndMaintenanceParentTab");
            if (directPageCache.containsKey("logistics.repairs-and-maintenance.repairs") &&
                          directPageCache.containsKey("logistics.repairs-and-maintenance.maintenance")) {
                  return;
            }

            directPageCache.put("logistics.repairs-and-maintenance.repairs",
                  repairAndMaintenanceTab.createRepairTab());
            directPageCache.put("logistics.repairs-and-maintenance.maintenance",
                  repairAndMaintenanceTab.createMaintenanceTab());
            repairAndMaintenanceTab.loadValuesFromCampaignOptions();
      }

      private JPanel createLogisticsAcquisitionTab() {
            ensureEquipmentAndSuppliesDirectPagesCreated();
            return (JPanel) directPageCache.get("logistics.supplies-and-acquisition.acquisition");
      }

      private JPanel createLogisticsPlanetaryAcquisitionTab() {
            ensureEquipmentAndSuppliesDirectPagesCreated();
            return (JPanel) directPageCache.get("logistics.supplies-and-acquisition.planetary-acquisition");
      }

      private JPanel createLogisticsTechLimitsTab() {
            ensureEquipmentAndSuppliesDirectPagesCreated();
            return (JPanel) directPageCache.get("logistics.supplies-and-acquisition.tech-limits");
      }

      private void ensureEquipmentAndSuppliesDirectPagesCreated() {
            ensureSectionLoaded("logisticsAndMaintenanceParentTab");
            if (directPageCache.containsKey("logistics.supplies-and-acquisition.acquisition") &&
                          directPageCache.containsKey("logistics.supplies-and-acquisition.planetary-acquisition") &&
                          directPageCache.containsKey("logistics.supplies-and-acquisition.tech-limits")) {
                  return;
            }

            directPageCache.put("logistics.supplies-and-acquisition.acquisition",
                  equipmentAndSuppliesTab.createAcquisitionTab());
            directPageCache.put("logistics.supplies-and-acquisition.planetary-acquisition",
                  equipmentAndSuppliesTab.createPlanetaryAcquisitionTab());
            directPageCache.put("logistics.supplies-and-acquisition.tech-limits",
                  equipmentAndSuppliesTab.createTechLimitsTab());
            equipmentAndSuppliesTab.loadValuesFromCampaignOptions();
      }

      private JPanel createOperationsFinancesGeneralTab() {
            ensureOperationsFinancesDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.finances.general");
      }

      private JPanel createOperationsPriceMultipliersTab() {
            ensureOperationsFinancesDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.finances.price-multipliers");
      }

      private void ensureOperationsFinancesDirectPagesCreated() {
            ensureSectionLoaded("strategicOperationsParentTab");
            if (directPageCache.containsKey("operations.finances.general") &&
                          directPageCache.containsKey("operations.finances.price-multipliers")) {
                  return;
            }

            directPageCache.put("operations.finances.general", financesTab.createFinancesGeneralOptionsTab());
            directPageCache.put("operations.finances.price-multipliers", financesTab.createPriceMultipliersTab());
            financesTab.loadValuesFromCampaignOptions();
      }

      private JPanel createOperationsPersonnelMarketTab() {
            ensureOperationsMarketsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.markets.personnel");
      }

      private JPanel createOperationsUnitMarketTab() {
            ensureOperationsMarketsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.markets.units");
      }

      private JPanel createOperationsContractMarketTab() {
            ensureOperationsMarketsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.markets.contracts");
      }

      private void ensureOperationsMarketsDirectPagesCreated() {
            ensureSectionLoaded("strategicOperationsParentTab");
            if (directPageCache.containsKey("operations.markets.personnel") &&
                          directPageCache.containsKey("operations.markets.units") &&
                          directPageCache.containsKey("operations.markets.contracts")) {
                  return;
            }

            directPageCache.put("operations.markets.personnel", marketsTab.createPersonnelMarketTab());
            directPageCache.put("operations.markets.units", marketsTab.createUnitMarketTab());
            directPageCache.put("operations.markets.contracts", marketsTab.createContractMarketTab());
            marketsTab.loadValuesFromCampaignOptions();
      }

      private JPanel createOperationsReputationTab() {
            ensureOperationsSystemsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.systems.reputation");
      }

      private JPanel createOperationsFactionStandingTab() {
            ensureOperationsSystemsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.systems.faction-standing");
      }

      private JPanel createOperationsATimeOfWarTab() {
            ensureOperationsSystemsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.systems.a-time-of-war");
      }

      private void ensureOperationsSystemsDirectPagesCreated() {
            ensureSectionLoaded("strategicOperationsParentTab");
            if (directPageCache.containsKey("operations.systems.reputation") &&
                          directPageCache.containsKey("operations.systems.faction-standing") &&
                          directPageCache.containsKey("operations.systems.a-time-of-war")) {
                  return;
            }

            directPageCache.put("operations.systems.reputation", systemsTab.createReputationTab());
            directPageCache.put("operations.systems.faction-standing", systemsTab.createFactionStandingTab());
            directPageCache.put("operations.systems.a-time-of-war", systemsTab.createATOWTab());
            systemsTab.loadValuesFromCampaignOptions();
      }

      private JPanel createOperationsStratConTab() {
            ensureOperationsRulesetsDirectPagesCreated();
            return (JPanel) directPageCache.get("operations.rulesets.stratcon");
      }

      private void ensureOperationsRulesetsDirectPagesCreated() {
            ensureSectionLoaded("strategicOperationsParentTab");
            if (directPageCache.containsKey("operations.rulesets.stratcon")) {
                  return;
            }

            directPageCache.put("operations.rulesets.stratcon", rulesetsTab.createStratConTab());
            rulesetsTab.loadValuesFromCampaignOptions();
      }

    private JTabbedPane createHumanResourcesParentTab() {
        // Parent Tab
        JTabbedPane humanResourcesParentTab = new JTabbedPane();
        registerRoute("human-resources",
              new String[] { "humanResourcesParentTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"));

        // Personnel
        personnelTab = new PersonnelTab(campaignOptions);

        JTabbedPane personnelContentTabs = createSubTabs(Map.of());
        registerRoute("human-resources.personnel",
              new String[] { "humanResourcesParentTab", "personnelContentTabs" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "personnelContentTabs"));
        registerRoute("human-resources.personnel.general",
              new String[] { "humanResourcesParentTab", "personnelContentTabs", "personnelGeneralTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "personnelContentTabs"),
              new TabSelection(personnelContentTabs, "personnelGeneralTab"));
        registerRoute("human-resources.personnel.awards",
              new String[] { "humanResourcesParentTab", "personnelContentTabs", "awardsTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "personnelContentTabs"),
              new TabSelection(personnelContentTabs, "awardsTab"));
        registerRoute("human-resources.personnel.medical",
              new String[] { "humanResourcesParentTab", "personnelContentTabs", "medicalTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "personnelContentTabs"),
              new TabSelection(personnelContentTabs, "medicalTab"));
        registerRoute("human-resources.personnel.information",
              new String[] { "humanResourcesParentTab", "personnelContentTabs", "personnelInformationTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "personnelContentTabs"),
              new TabSelection(personnelContentTabs, "personnelInformationTab"));
        registerRoute("human-resources.personnel.prisoners-and-civilians",
              new String[] { "humanResourcesParentTab", "personnelContentTabs", "prisonersAndDependentsTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "personnelContentTabs"),
              new TabSelection(personnelContentTabs, "prisonersAndDependentsTab"));
        // Biography
        biographyTab = new BiographyTab(campaign, generalTab);

        JTabbedPane biographyContentTabs = createSubTabs(Map.of());
        registerRoute("human-resources.biography",
              new String[] { "humanResourcesParentTab", "biographyContentTabs" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"));
        registerRoute("human-resources.biography.general",
              new String[] { "humanResourcesParentTab", "biographyContentTabs", "biographyGeneralTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"),
              new TabSelection(biographyContentTabs, "biographyGeneralTab"));
        registerRoute("human-resources.biography.backgrounds",
              new String[] { "humanResourcesParentTab", "biographyContentTabs", "backgroundsTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"),
              new TabSelection(biographyContentTabs, "backgroundsTab"));
        registerRoute("human-resources.biography.death",
              new String[] { "humanResourcesParentTab", "biographyContentTabs", "deathTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"),
              new TabSelection(biographyContentTabs, "deathTab"));
        registerRoute("human-resources.biography.education",
              new String[] { "humanResourcesParentTab", "biographyContentTabs", "educationTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"),
              new TabSelection(biographyContentTabs, "educationTab"));
        registerRoute("human-resources.biography.name-and-portraits",
              new String[] { "humanResourcesParentTab", "biographyContentTabs", "nameAndPortraitGenerationTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"),
              new TabSelection(biographyContentTabs, "nameAndPortraitGenerationTab"));
        registerRoute("human-resources.biography.rank",
              new String[] { "humanResourcesParentTab", "biographyContentTabs", "rankTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "biographyContentTabs"),
              new TabSelection(biographyContentTabs, "rankTab"));

        // Relationships
        relationshipsTab = new RelationshipsTab(campaignOptions);

        JTabbedPane relationshipsContentTabs = createSubTabs(Map.of());
        registerRoute("human-resources.relationships",
              new String[] { "humanResourcesParentTab", "relationshipsContentTabs" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "relationshipsContentTabs"));
        registerRoute("human-resources.relationships.marriage",
              new String[] { "humanResourcesParentTab", "relationshipsContentTabs", "marriageTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "relationshipsContentTabs"),
              new TabSelection(relationshipsContentTabs, "marriageTab"));
        registerRoute("human-resources.relationships.divorce",
              new String[] { "humanResourcesParentTab", "relationshipsContentTabs", "divorceTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "relationshipsContentTabs"),
              new TabSelection(relationshipsContentTabs, "divorceTab"));
        registerRoute("human-resources.relationships.procreation",
              new String[] { "humanResourcesParentTab", "relationshipsContentTabs", "procreationTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "relationshipsContentTabs"),
              new TabSelection(relationshipsContentTabs, "procreationTab"));

        // Personnel
        salariesTab = new SalariesTab(campaignOptions);

        JTabbedPane salariesContentTabs = createSubTabs(Map.of());
        registerRoute("human-resources.salaries",
              new String[] { "humanResourcesParentTab", "salariesContentTabs" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "salariesContentTabs"));
        registerRoute("human-resources.salaries.combat",
              new String[] { "humanResourcesParentTab", "salariesContentTabs", "0combatSalariesTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "salariesContentTabs"),
              new TabSelection(salariesContentTabs, "0combatSalariesTab"));
        registerRoute("human-resources.salaries.support",
              new String[] { "humanResourcesParentTab", "salariesContentTabs", "1supportSalariesTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "salariesContentTabs"),
              new TabSelection(salariesContentTabs, "1supportSalariesTab"));
        registerRoute("human-resources.salaries.civilian",
              new String[] { "humanResourcesParentTab", "salariesContentTabs", "2civilianSalariesTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "salariesContentTabs"),
              new TabSelection(salariesContentTabs, "2civilianSalariesTab"));

        // Turnover and Retention
        turnoverAndRetentionTab = new TurnoverAndRetentionTab(campaignOptions);

        JTabbedPane turnoverAndRetentionContentTabs = createSubTabs(Map.of());
        registerRoute("human-resources.turnover-and-retention",
              new String[] { "humanResourcesParentTab", "turnoverAndRetentionContentTabs" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "turnoverAndRetentionContentTabs"));
        registerRoute("human-resources.turnover-and-retention.turnover",
              new String[] { "humanResourcesParentTab", "turnoverAndRetentionContentTabs", "turnoverTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "turnoverAndRetentionContentTabs"),
              new TabSelection(turnoverAndRetentionContentTabs, "turnoverTab"));
        registerRoute("human-resources.turnover-and-retention.fatigue",
              new String[] { "humanResourcesParentTab", "turnoverAndRetentionContentTabs", "fatigueTab" },
              new TabSelection(legacyTabbedPane, "humanResourcesParentTab"),
              new TabSelection(humanResourcesParentTab, "turnoverAndRetentionContentTabs"),
              new TabSelection(turnoverAndRetentionContentTabs, "fatigueTab"));

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
        registerRoute("advancement",
              new String[] { "advancementParentTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"));

        // Advancement
        advancementTab = new AdvancementTab(campaign);

        JTabbedPane awardsAndRandomizationContentTabs = createSubTabs(Map.of());
        registerRoute("advancement.awards-and-randomization",
              new String[] { "advancementParentTab", "awardsAndRandomizationContentTabs" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "awardsAndRandomizationContentTabs"));
        registerRoute("advancement.awards-and-randomization.randomization",
              new String[] { "advancementParentTab", "awardsAndRandomizationContentTabs", "0randomizationTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "awardsAndRandomizationContentTabs"),
              new TabSelection(awardsAndRandomizationContentTabs, "0randomizationTab"));
        registerRoute("advancement.awards-and-randomization.xp-awards",
              new String[] { "advancementParentTab", "awardsAndRandomizationContentTabs", "1xpAwardsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "awardsAndRandomizationContentTabs"),
              new TabSelection(awardsAndRandomizationContentTabs, "1xpAwardsTab"));
        registerRoute("advancement.awards-and-randomization.recruitment-bonuses",
              new String[] { "advancementParentTab", "awardsAndRandomizationContentTabs", "2recruitmentBonusesTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "awardsAndRandomizationContentTabs"),
              new TabSelection(awardsAndRandomizationContentTabs, "2recruitmentBonusesTab"));

        // Skills
        skillsTab = new SkillsTab(campaignOptions);

        JTabbedPane skillsContentTabs = createSubTabs(Map.of());
        registerRoute("advancement.skills",
              new String[] { "advancementParentTab", "skillsContentTabs" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "skillsContentTabs"));
        registerRoute("advancement.skills.gunnery",
              new String[] { "advancementParentTab", "skillsContentTabs", "0gunnerySkillsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "skillsContentTabs"),
              new TabSelection(skillsContentTabs, "0gunnerySkillsTab"));
        registerRoute("advancement.skills.piloting",
              new String[] { "advancementParentTab", "skillsContentTabs", "1pilotingSkillsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "skillsContentTabs"),
              new TabSelection(skillsContentTabs, "1pilotingSkillsTab"));
        registerRoute("advancement.skills.support",
              new String[] { "advancementParentTab", "skillsContentTabs", "2supportSkillsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "skillsContentTabs"),
              new TabSelection(skillsContentTabs, "2supportSkillsTab"));
        registerRoute("advancement.skills.utility",
              new String[] { "advancementParentTab", "skillsContentTabs", "3utilitySkillsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "skillsContentTabs"),
              new TabSelection(skillsContentTabs, "3utilitySkillsTab"));
        registerRoute("advancement.skills.roleplay",
              new String[] { "advancementParentTab", "skillsContentTabs", "4roleplaySkillsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "skillsContentTabs"),
              new TabSelection(skillsContentTabs, "4roleplaySkillsTab"));

        // SPAs
        abilitiesTab = new AbilitiesTab();

        JTabbedPane abilityContentTabs = createSubTabs(Map.of());
        // the loading of values from the campaign is built into the AbilitiesTab class so not called here.
        registerRoute("advancement.abilities",
              new String[] { "advancementParentTab", "abilityContentTabs" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "abilityContentTabs"));
        registerRoute("advancement.abilities.combat",
              new String[] { "advancementParentTab", "abilityContentTabs", "0combatAbilitiesTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "abilityContentTabs"),
              new TabSelection(abilityContentTabs, "0combatAbilitiesTab"));
        registerRoute("advancement.abilities.maneuvering",
              new String[] { "advancementParentTab", "abilityContentTabs", "1maneuveringAbilitiesTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "abilityContentTabs"),
              new TabSelection(abilityContentTabs, "1maneuveringAbilitiesTab"));
        registerRoute("advancement.abilities.utility",
              new String[] { "advancementParentTab", "abilityContentTabs", "2utilityAbilitiesTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "abilityContentTabs"),
              new TabSelection(abilityContentTabs, "2utilityAbilitiesTab"));
        registerRoute("advancement.abilities.character-flaws",
              new String[] { "advancementParentTab", "abilityContentTabs", "3characterFlawsTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "abilityContentTabs"),
              new TabSelection(abilityContentTabs, "3characterFlawsTab"));
        registerRoute("advancement.abilities.character-creation-only",
              new String[] { "advancementParentTab", "abilityContentTabs", "4characterCreationOnlyTab" },
              new TabSelection(legacyTabbedPane, "advancementParentTab"),
              new TabSelection(advancementParentTab, "abilityContentTabs"),
              new TabSelection(abilityContentTabs, "4characterCreationOnlyTab"));

        // Add Tabs
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "awardsAndRandomizationContentTabs.title")),
              awardsAndRandomizationContentTabs);
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "skillsContentTabs.title")), skillsContentTabs);
        advancementParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
              4, getTextAt(getCampaignOptionsResourceBundle(), "abilityContentTabs.title")), abilityContentTabs);

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
        registerRoute("logistics",
              new String[] { "logisticsAndMaintenanceParentTab" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"));

        // Repair and Maintenance
        repairAndMaintenanceTab = new RepairAndMaintenanceTab(campaignOptions);

        JTabbedPane repairsAndMaintenanceContentTabs = createSubTabs(Map.of());
        registerRoute("logistics.repairs-and-maintenance",
              new String[] { "logisticsAndMaintenanceParentTab", "repairsAndMaintenanceContentTabs" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "repairsAndMaintenanceContentTabs"));
        registerRoute("logistics.repairs-and-maintenance.repairs",
              new String[] { "logisticsAndMaintenanceParentTab", "repairsAndMaintenanceContentTabs", "repairTab" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "repairsAndMaintenanceContentTabs"),
              new TabSelection(repairsAndMaintenanceContentTabs, "repairTab"));
        registerRoute("logistics.repairs-and-maintenance.maintenance",
              new String[] { "logisticsAndMaintenanceParentTab", "repairsAndMaintenanceContentTabs", "maintenanceTab" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "repairsAndMaintenanceContentTabs"),
              new TabSelection(repairsAndMaintenanceContentTabs, "maintenanceTab"));

        // Supplies and Acquisition
        equipmentAndSuppliesTab = new EquipmentAndSuppliesTab(campaignOptions);

        JTabbedPane suppliesAndAcquisitionContentTabs = createSubTabs(Map.of());
        registerRoute("logistics.supplies-and-acquisition",
              new String[] { "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "suppliesAndAcquisitionContentTabs"));
        registerRoute("logistics.supplies-and-acquisition.acquisition",
              new String[] { "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs", "acquisitionTab" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "suppliesAndAcquisitionContentTabs"),
              new TabSelection(suppliesAndAcquisitionContentTabs, "acquisitionTab"));
        registerRoute("logistics.supplies-and-acquisition.planetary-acquisition",
              new String[] { "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs",
                         "planetaryAcquisitionTab" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "suppliesAndAcquisitionContentTabs"),
              new TabSelection(suppliesAndAcquisitionContentTabs, "planetaryAcquisitionTab"));
        registerRoute("logistics.supplies-and-acquisition.tech-limits",
              new String[] { "logisticsAndMaintenanceParentTab", "suppliesAndAcquisitionContentTabs", "techLimitsTab" },
              new TabSelection(legacyTabbedPane, "logisticsAndMaintenanceParentTab"),
              new TabSelection(equipmentAndSuppliesParentTab, "suppliesAndAcquisitionContentTabs"),
              new TabSelection(suppliesAndAcquisitionContentTabs, "techLimitsTab"));

        // Add tabs
        equipmentAndSuppliesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "suppliesAndAcquisitionContentTabs.title")),
              suppliesAndAcquisitionContentTabs);
        equipmentAndSuppliesParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>",
                    4, getTextAt(getCampaignOptionsResourceBundle(), "repairsAndMaintenanceContentTabs.title")),
              repairsAndMaintenanceContentTabs);

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
        registerRoute("operations",
              new String[] { "strategicOperationsParentTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"));

        // Finances
        financesTab = new FinancesTab(campaign);

        JTabbedPane financesContentTabs = createSubTabs(Map.of());
        registerRoute("operations.finances",
              new String[] { "strategicOperationsParentTab", "financesContentTabs" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "financesContentTabs"));
        registerRoute("operations.finances.general",
              new String[] { "strategicOperationsParentTab", "financesContentTabs", "financesGeneralTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "financesContentTabs"),
              new TabSelection(financesContentTabs, "financesGeneralTab"));
        registerRoute("operations.finances.price-multipliers",
              new String[] { "strategicOperationsParentTab", "financesContentTabs", "priceMultipliersTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "financesContentTabs"),
              new TabSelection(financesContentTabs, "priceMultipliersTab"));

        // Markets
        marketsTab = new MarketsTab(campaign);

        JTabbedPane marketsContentTabs = createSubTabs(Map.of());
        registerRoute("operations.markets",
              new String[] { "strategicOperationsParentTab", "marketsContentTabs" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "marketsContentTabs"));
        registerRoute("operations.markets.personnel",
              new String[] { "strategicOperationsParentTab", "marketsContentTabs", "personnelMarketTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "marketsContentTabs"),
              new TabSelection(marketsContentTabs, "personnelMarketTab"));
        registerRoute("operations.markets.units",
              new String[] { "strategicOperationsParentTab", "marketsContentTabs", "unitMarketTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "marketsContentTabs"),
              new TabSelection(marketsContentTabs, "unitMarketTab"));
        registerRoute("operations.markets.contracts",
              new String[] { "strategicOperationsParentTab", "marketsContentTabs", "contractMarketTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "marketsContentTabs"),
              new TabSelection(marketsContentTabs, "contractMarketTab"));

        // Systems
        systemsTab = new SystemsTab(campaign);

        JTabbedPane systemsContentTabs = createSubTabs(Map.of());
        registerRoute("operations.systems",
              new String[] { "strategicOperationsParentTab", "systemsContentTabs" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "systemsContentTabs"));
        registerRoute("operations.systems.reputation",
              new String[] { "strategicOperationsParentTab", "systemsContentTabs", "reputationTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "systemsContentTabs"),
              new TabSelection(systemsContentTabs, "reputationTab"));
        registerRoute("operations.systems.faction-standing",
              new String[] { "strategicOperationsParentTab", "systemsContentTabs", "factionStandingTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "systemsContentTabs"),
              new TabSelection(systemsContentTabs, "factionStandingTab"));
        registerRoute("operations.systems.a-time-of-war",
              new String[] { "strategicOperationsParentTab", "systemsContentTabs", "atowTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "systemsContentTabs"),
              new TabSelection(systemsContentTabs, "atowTab"));

        // Rulesets
        rulesetsTab = new RulesetsTab(campaignOptions);

        JTabbedPane rulesetsContentTabs = createSubTabs(Map.of());
        registerRoute("operations.rulesets",
              new String[] { "strategicOperationsParentTab", "rulesetsContentTabs" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "rulesetsContentTabs"));
        registerRoute("operations.rulesets.stratcon",
              new String[] { "strategicOperationsParentTab", "rulesetsContentTabs", "stratConGeneralTab" },
              new TabSelection(legacyTabbedPane, "strategicOperationsParentTab"),
              new TabSelection(strategicOperationsParentTab, "rulesetsContentTabs"),
              new TabSelection(rulesetsContentTabs, "stratConGeneralTab"));

        // Enable the below section and remove the above in the event we have Legacy Options. In 50.10 all legacy
        // options (at that time) were removed, so this section got commented out.
        //        JTabbedPane rulesetsContentTabs = createSubTabs(Map.of("stratConGeneralTab",
        //              rulesetsTab.createStratConTab(),
        //              "legacyTab",
        //              rulesetsTab.createLegacyTab()));

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

        return strategicOperationsParentTab;
      }

    /**
     * Applies the currently configured campaign options to the active {@link Campaign}. This method processes all tabs
     * in the dialog, applying the options to the campaign in logical order (e.g., "General" first, followed by other
     * categories).
     *
     * @param preset       an optional {@link CampaignPreset} used to override campaign options
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

        CampaignOptionsFreebieTracker oldCampaignOptions = new CampaignOptionsFreebieTracker(campaign.getCampaignOptions());

        // Everything assumes general tab will be the first applied.
        // While this shouldn't break anything, it's not worth moving around.
        // For all other tabs, it makes sense to apply them in the order they
        // appear in the dialog; however, this shouldn't make any major difference.
        generalTab.applyCampaignOptionsToCampaign(isStartUp, isSaveAction);

        // Human Resources
        if (humanResourcesParentTab != null) {
                  ensureDirectPagesLoaded("human-resources.");
            personnelTab.applyCampaignOptionsToCampaign(campaign, options);
            biographyTab.applyCampaignOptionsToCampaign(options);
            relationshipsTab.applyCampaignOptionsToCampaign(options);
            salariesTab.applyCampaignOptionsToCampaign(options);
            turnoverAndRetentionTab.applyCampaignOptionsToCampaign(options);
        }

        // Advancement
        if (advancementParentTab != null) {
                  ensureDirectPagesLoaded("advancement.");
            advancementTab.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);
            skillsTab.applyCampaignOptionsToCampaign(options, presetSkills);
            abilitiesTab.applyCampaignOptionsToCampaign(preset);
        }

        // Logistics
        if (equipmentAndSuppliesParentTab != null) {
                  ensureDirectPagesLoaded("logistics.");
            equipmentAndSuppliesTab.applyCampaignOptionsToCampaign(options);
            repairAndMaintenanceTab.applyCampaignOptionsToCampaign(options);
        }

        // Operations
        if (strategicOperationsParentTab != null) {
                  ensureDirectPagesLoaded("operations.");
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

        CampaignOptionsFreebieTracker newCampaignOptions = new CampaignOptionsFreebieTracker(campaign.getCampaignOptions());
        triggerUpgradeFreebies(campaign, oldCampaignOptions, newCampaignOptions, isStartUp);
    }
    /**
     * Compares a previously-recorded {@link CampaignOptionsFreebieTracker} snapshot against a new snapshot and triggers
     * any one-time handlers required when critical campaign options are enabled.
     *
     * <p>This method is intended to be called immediately after applying campaign option changes (or when
     * loading/upgrading a campaign) so the campaign can react to newly-enabled systems. Reactions may include prompting
     * the player with confirmation dialogs, adjusting campaign state, or granting "freebies" to keep the save
     * consistent and fair when major rulesets are turned on mid-campaign.</p>
     *
     * <p>Only transitions from {@code false -> true} are acted upon (that is, newly enabled features). Disabling
     * options typically does not require compensation and is therefore ignored here.</p>
     *
     * <p>When {@code isStartUp} is {@code true}, interactive prompts are suppressed; the method may still perform
     * required non-interactive adjustments depending on implementation.</p>
     *
     * @param campaign   the campaign whose state may be adjusted and/or to which reports may be added
     * @param oldOptions snapshot of the option state before the change (or previously acknowledged state)
     * @param newOptions snapshot of the option state after the change (current effective state)
     * @param isStartUp  whether this invocation is happening during startup/load/upgrade rather than an in-session
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
        boolean oldIsTrackPrisoners = !oldOptions.trackPrisoners();
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
     * <p>This method adds planetary inoculation records for:</p>
     *
     * <ul>
     *   <li>The current planet (if the campaign is on a planet, not in transit)</li>
     *   <li>Each person's origin planet</li>
     * </ul>
     *
     * <p>Personnel are assumed to have prior inoculation for their home planet, while current planet inoculation
     * requires campaign location tracking.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void inoculateAllCharacters(Campaign campaign) {
        final CurrentLocation location = campaign.getCurrentLocation();
        final LocalDate currentDay = campaign.getLocalDate();

        final Map<String, Set<InjuryType>> curesBySystem = new HashMap<>();

        final Planet planet = location.isOnPlanet() ? location.getPlanet() : null;
        final String planetId = (planet != null) ? planet.getId() : null;
        final String systemId = (planet != null) ? planet.getParentSystem().getId() : null;

        for (Person person : campaign.getPersonnel()) {
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
                  curesBySystem
            );
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
     * Applies the values from a {@link CampaignPreset} to all tabs in the dialog. This propagates preset-specific
     * configuration to all associated components and sub-tabs, including campaign-related properties such as dates,
     * factions, and skills.
     *
     * @param campaignPreset the {@link CampaignPreset} containing the preset options to apply
     * @param isStartup      {@code true} if the preset is being loaded during new campaign startup
     */
    public void applyPreset(@Nullable CampaignPreset campaignPreset, boolean isStartup) {
        if (campaignPreset == null) {
            return;
        }

        ensureAllSectionsLoaded();
      ensureDirectPagesLoaded();
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
