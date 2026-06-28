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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
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
 * {@code CampaignOptionsPane} is the central panel of the Campaign Options dialog. It presents every campaign setting
 * through a searchable navigation tree on the left (a {@link CampaignOptionsNavigationPanel}) paired with a scrollable
 * content host on the right (a {@link CampaignOptionsContentHost}) inside a {@link javax.swing.JSplitPane}.
 *
 * <p>The pane registers a flat set of {@link CampaignOptionsRoute}s - each describing a navigable destination and its
 * hierarchical path - and maps each one to a page factory. Pages are built lazily the first time they are shown (or
 * when the navigation search index is warmed) and then cached. The per-area builders
 * ({@link mekhq.gui.campaignOptions.contents.GeneralPage GeneralPage},
 * {@link mekhq.gui.campaignOptions.contents.PersonnelPages PersonnelPages}, and the other per-area {@code *Pages}
 * classes) are likewise only instantiated the first time their section is needed.</p>
 *
 * <p>The pane is constructed with a {@link Campaign} and a {@link CampaignOptionsDialogMode} ({@code NORMAL},
 * {@code STARTUP}, {@code STARTUP_ABRIDGED}, or {@code CAMPAIGN_UPGRADE}), and also bridges the UI back to the domain:
 * it applies the edited settings to the campaign, loads {@link CampaignPreset}s, and fires the one-time
 * confirmation/compensation handlers needed when a major ruleset is switched on.</p>
 *
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Builds the navigation tree and content host and keeps them in sync as the user navigates.</li>
 *   <li>Lazily creates and caches the option pages, and the section builders that produce them.</li>
 *   <li>Feeds the navigation search index so the filter can match page and section titles.</li>
 *   <li>Applies the configured options to the active {@link Campaign} and supports saving and loading presets.</li>
 * </ul>
 */
public class CampaignOptionsPane extends JPanel {
    private static final int CONTENT_MARGIN = UIUtil.scaleForGUI(4);

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

    private GeneralPage generalPage;
    private PersonnelPages personnelPages;
    private BiographyPages biographyPages;
    private RelationshipsPages relationshipsPages;
    private SalariesPages salariesPages;
    private TurnoverAndRetentionPages turnoverAndRetentionPages;
    private AwardsAndRandomizationPages awardsAndRandomizationPages;
    private SkillsPages skillsPages;
    private AbilitiesPages abilitiesPages;
    private RepairAndMaintenancePages repairAndMaintenancePages;
    private EquipmentAndSuppliesPages equipmentAndSuppliesPages;
    private FinancesPages financesPages;
    private MarketsPages marketsPages;
    private SystemsPages systemsPages;
    private RulesetsPages rulesetsPages;
    private CampaignGUI campaignGui;

    /**
     * Constructs a {@code CampaignOptionsPane} for managing campaign settings. This builds the navigation tree and
     * content host from the provided {@link Campaign} instance and dialog mode.
     *
     * @param frame    the parent {@link JFrame} for this pane
     * @param campaign the {@link Campaign} object representing the current campaign
     * @param mode     the {@link CampaignOptionsDialogMode} for configuring the
     *                 pane's behavior
     */
    public CampaignOptionsPane(final JFrame frame, @Nonnull final Campaign campaign, CampaignOptionsDialogMode mode) {
        super(new BorderLayout());
        setName("campaignOptionsDialog");
        this.frame = frame;
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.mode = mode;
        this.campaignGui = campaign.getGUI();
        initialize();
    }

    /**
     * Builds the pane: creates the eagerly-loaded General page, registers every navigation route, and assembles the
     * navigation tree and content host into the split pane.
     */
    protected void initialize() {
        JPanel generalPage = createGeneralPage(mode);
        registerRoutes(generalPage);
        CampaignOptionsRoute initialRoute = navigationTargets.get(0);

        // Bottom margin is 0: the footer's button panel already adds top padding, so a bottom margin here would
        // stack with it and make the gap above the footer buttons look larger than the gap below them.
        setBorder(BorderFactory.createEmptyBorder(CONTENT_MARGIN, CONTENT_MARGIN, 0, CONTENT_MARGIN));
        CampaignOptionsContentHost contentHost = createContentHost(generalPage, initialRoute);

        // Abridged startup (preset "Apply") shows only the General page, so skip the navigation tree and its search
        // entirely and let the content fill the dialog.
        if (mode == STARTUP_ABRIDGED) {
            add(contentHost, BorderLayout.CENTER);
            return;
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createNavigationPanel(),
                contentHost);
        splitPane.setName("campaignOptionsSplitPane");
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(UIUtil.scaleForGUI(CampaignOptionsNavigationPanel.NAVIGATION_WIDTH));
        add(splitPane, BorderLayout.CENTER);
        navigationPanel.selectRoute(navigationTargets.get(0));
        registerSearchShortcut();
    }

    /**
     * Registers a window-level Ctrl/Cmd+F shortcut that moves focus to the navigation search field, regardless of
     * which control inside the dialog currently has focus.
     */
    private void registerSearchShortcut() {
        KeyStroke findKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(findKeyStroke, "focusCampaignOptionsSearch");
        getActionMap().put("focusCampaignOptionsSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigationPanel.focusSearchField();
            }
        });
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

        // Abridged startup (preset "Apply") shows only the General landing page, so users who just want a preset
        // aren't faced with the full options tree. The preset's other options are still applied in full via
        // ensureAllSectionsLoaded() when the dialog is accepted.
        if (mode == STARTUP_ABRIDGED) {
            return;
        }

        registerParentRoute("human-resources", "humanResourcesCategory");
        registerParentRoute("human-resources.personnel", "humanResourcesCategory", "personnelCategory");
        registerDirectRoute("human-resources.personnel.general", this::createPersonnelGeneralPage,
                "humanResourcesCategory", "personnelCategory", "personnelGeneralPage");
        registerDirectRoute("human-resources.personnel.awards", this::createPersonnelAwardsPage,
                "humanResourcesCategory", "personnelCategory", "awardsPage");
        registerDirectRoute("human-resources.personnel.medical", this::createPersonnelMedicalPage,
                "humanResourcesCategory", "personnelCategory", "medicalPage");
        registerDirectRoute("human-resources.personnel.information", this::createPersonnelInformationPage,
                "humanResourcesCategory", "personnelCategory", "personnelInformationPage");
        registerDirectRoute("human-resources.personnel.prisoners-and-civilians",
                this::createPersonnelPrisonersAndDependentsPage,
                "humanResourcesCategory", "personnelCategory", "prisonersAndDependentsPage");
        registerParentRoute("human-resources.biography", "humanResourcesCategory", "biographyCategory");
        registerDirectRoute("human-resources.biography.general", this::createBiographyGeneralPage,
                "humanResourcesCategory", "biographyCategory", "biographyGeneralPage");
        registerDirectRoute("human-resources.biography.backgrounds", this::createBiographyBackgroundsPage,
                "humanResourcesCategory", "biographyCategory", "backgroundsPage");
        registerDirectRoute("human-resources.biography.death", this::createBiographyDeathPage,
                "humanResourcesCategory", "biographyCategory", "deathPage");
        registerDirectRoute("human-resources.biography.education", this::createBiographyEducationPage,
                "humanResourcesCategory", "biographyCategory", "educationPage");
        registerDirectRoute("human-resources.biography.name-and-portraits",
                this::createBiographyNameAndPortraitGenerationPage,
                "humanResourcesCategory", "biographyCategory", "nameAndPortraitGenerationPage");
        registerDirectRoute("human-resources.biography.rank", this::createBiographyRankPage,
                "humanResourcesCategory", "biographyCategory", "rankPage");
        registerParentRoute("human-resources.relationships", "humanResourcesCategory", "relationshipsCategory");
        registerDirectRoute("human-resources.relationships.marriage", this::createRelationshipMarriagePage,
                "humanResourcesCategory", "relationshipsCategory", "marriagePage");
        registerDirectRoute("human-resources.relationships.divorce", this::createRelationshipDivorcePage,
                "humanResourcesCategory", "relationshipsCategory", "divorcePage");
        registerDirectRoute("human-resources.relationships.procreation", this::createRelationshipProcreationPage,
                "humanResourcesCategory", "relationshipsCategory", "procreationPage");
        registerParentRoute("human-resources.salaries", "humanResourcesCategory", "salariesCategory");
        registerDirectRoute("human-resources.salaries.combat", this::createCombatSalariesPage,
                "humanResourcesCategory", "salariesCategory", "0combatSalariesPage");
        registerDirectRoute("human-resources.salaries.support", this::createSupportSalariesPage,
                "humanResourcesCategory", "salariesCategory", "1supportSalariesPage");
        registerDirectRoute("human-resources.salaries.civilian", this::createCivilianSalariesPage,
                "humanResourcesCategory", "salariesCategory", "2civilianSalariesPage");
        registerParentRoute("human-resources.turnover-and-retention", "humanResourcesCategory",
                "turnoverAndRetentionCategory");
        registerDirectRoute("human-resources.turnover-and-retention.turnover",
                this::createTurnoverAndRetentionTurnoverPage,
                "humanResourcesCategory", "turnoverAndRetentionCategory", "turnoverPage");
        registerDirectRoute("human-resources.turnover-and-retention.fatigue",
                this::createTurnoverAndRetentionFatiguePage,
                "humanResourcesCategory", "turnoverAndRetentionCategory", "fatiguePage");

        registerParentRoute("advancement", "advancementCategory");
        registerParentRoute("advancement.awards-and-randomization", "advancementCategory",
                "awardsAndRandomizationCategory");
        registerDirectRoute("advancement.awards-and-randomization.randomization",
                this::createAdvancementRandomizationPage,
                "advancementCategory", "awardsAndRandomizationCategory", "0randomizationPage");
        registerDirectRoute("advancement.awards-and-randomization.xp-awards", this::createAdvancementXpAwardsPage,
                "advancementCategory", "awardsAndRandomizationCategory", "1xpAwardsPage");
        registerDirectRoute("advancement.awards-and-randomization.recruitment-bonuses",
                this::createAdvancementRecruitmentBonusesPage,
                CampaignOptionsRouteOptions.withoutHelpPanel(),
                "advancementCategory", "awardsAndRandomizationCategory", "2recruitmentBonusesPage");
        registerParentRoute("advancement.skills", "advancementCategory", "skillsCategory");
        registerDirectRoute("advancement.skills.gunnery", this::createAdvancementGunnerySkillsPage,
                "advancementCategory", "skillsCategory", "0gunnerySkillsPage");
        registerDirectRoute("advancement.skills.piloting", this::createAdvancementPilotingSkillsPage,
                "advancementCategory", "skillsCategory", "1pilotingSkillsPage");
        registerDirectRoute("advancement.skills.support", this::createAdvancementSupportSkillsPage,
                "advancementCategory", "skillsCategory", "2supportSkillsPage");
        registerDirectRoute("advancement.skills.utility", this::createAdvancementUtilitySkillsPage,
                "advancementCategory", "skillsCategory", "3utilitySkillsPage");
        registerDirectRoute("advancement.skills.roleplay", this::createAdvancementRoleplaySkillsPage,
                "advancementCategory", "skillsCategory", "4roleplaySkillsPage");
        registerParentRoute("advancement.abilities", "advancementCategory", "abilityCategory");
        registerDirectRoute("advancement.abilities.combat", this::createAdvancementCombatAbilitiesPage,
                "advancementCategory", "abilityCategory", "0combatAbilitiesPage");
        registerDirectRoute("advancement.abilities.maneuvering", this::createAdvancementManeuveringAbilitiesPage,
                "advancementCategory", "abilityCategory", "1maneuveringAbilitiesPage");
        registerDirectRoute("advancement.abilities.utility", this::createAdvancementUtilityAbilitiesPage,
                "advancementCategory", "abilityCategory", "2utilityAbilitiesPage");
        registerDirectRoute("advancement.abilities.character-flaws", this::createAdvancementCharacterFlawsPage,
                "advancementCategory", "abilityCategory", "3characterFlawsPage");
        registerDirectRoute("advancement.abilities.character-creation-only",
                this::createAdvancementCharacterCreationOnlyPage,
                "advancementCategory", "abilityCategory", "4characterCreationOnlyPage");

        registerParentRoute("logistics", "logisticsAndMaintenanceCategory");
        registerParentRoute("logistics.repairs-and-maintenance", "logisticsAndMaintenanceCategory",
                "repairsAndMaintenanceCategory");
        registerDirectRoute("logistics.repairs-and-maintenance.repairs", this::createLogisticsRepairsPage,
                "logisticsAndMaintenanceCategory", "repairsAndMaintenanceCategory", "repairPage");
        registerDirectRoute("logistics.repairs-and-maintenance.maintenance", this::createLogisticsMaintenancePage,
                "logisticsAndMaintenanceCategory", "repairsAndMaintenanceCategory", "maintenancePage");
        registerParentRoute("logistics.supplies-and-acquisition", "logisticsAndMaintenanceCategory",
                "suppliesAndAcquisitionCategory");
        registerDirectRoute("logistics.supplies-and-acquisition.acquisition", this::createLogisticsAcquisitionPage,
                "logisticsAndMaintenanceCategory", "suppliesAndAcquisitionCategory", "acquisitionPage");
        registerDirectRoute("logistics.supplies-and-acquisition.planetary-acquisition",
                this::createLogisticsPlanetaryAcquisitionPage,
                "logisticsAndMaintenanceCategory", "suppliesAndAcquisitionCategory",
                "planetaryAcquisitionPage");
        registerDirectRoute("logistics.supplies-and-acquisition.tech-limits", this::createLogisticsTechLimitsPage,
                "logisticsAndMaintenanceCategory", "suppliesAndAcquisitionCategory", "techLimitsPage");

        registerParentRoute("operations", "strategicOperationsCategory");
        registerParentRoute("operations.finances", "strategicOperationsCategory", "financesCategory");
        registerDirectRoute("operations.finances.general", this::createOperationsFinancesGeneralPage,
                "strategicOperationsCategory", "financesCategory", "financesGeneralPage");
        registerDirectRoute("operations.finances.price-multipliers", this::createOperationsPriceMultipliersPage,
                "strategicOperationsCategory", "financesCategory", "priceMultipliersPage");
        registerParentRoute("operations.markets", "strategicOperationsCategory", "marketsCategory");
        registerDirectRoute("operations.markets.personnel", this::createOperationsPersonnelMarketPage,
                "strategicOperationsCategory", "marketsCategory", "personnelMarketPage");
        registerDirectRoute("operations.markets.units", this::createOperationsUnitMarketPage,
                "strategicOperationsCategory", "marketsCategory", "unitMarketPage");
        registerDirectRoute("operations.markets.contracts", this::createOperationsContractMarketPage,
                "strategicOperationsCategory", "marketsCategory", "contractMarketPage");
        registerParentRoute("operations.systems", "strategicOperationsCategory", "systemsCategory");
        registerDirectRoute("operations.systems.reputation", this::createOperationsReputationPage,
                "strategicOperationsCategory", "systemsCategory", "reputationPage");
        registerDirectRoute("operations.systems.faction-standing", this::createOperationsFactionStandingPage,
                "strategicOperationsCategory", "systemsCategory", "factionStandingPage");
        registerDirectRoute("operations.systems.a-time-of-war", this::createOperationsATimeOfWarPage,
                "strategicOperationsCategory", "systemsCategory", "atowPage");
        registerParentRoute("operations.rulesets", "strategicOperationsCategory", "rulesetsCategory");
        registerDirectRoute("operations.rulesets.stratcon", this::createOperationsStratConPage,
                "strategicOperationsCategory", "rulesetsCategory", "stratConGeneralPage");

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
        expandSectionsForActiveFilter(directPage);
        return true;
    }

    /**
     * When a page is opened while a navigation search is active, expands the section(s) whose title or summary match
     * the search so the result the user clicked is revealed, instead of the page opening fully collapsed. Pages with no
     * matching section (e.g. the search matched only the page title) are left in their default state.
     *
     * @param directPage the page component just shown
     */
    private void expandSectionsForActiveFilter(Component directPage) {
        if (navigationPanel == null) {
            return;
        }

        String activeFilter = navigationPanel.getActiveFilter();
        if (activeFilter.isBlank()) {
            return;
        }

        CampaignOptionsPagePanel pagePanel = CampaignOptionsContentHost.findPagePanel(directPage);
        if (pagePanel == null) {
            return;
        }

        String[] tokens = activeFilter.split("\\s+");
        pagePanel.expandSectionsMatching(sectionText -> sectionMatchesAllTokens(sectionText, tokens));
    }

    private static boolean sectionMatchesAllTokens(String rawSectionText, String[] normalizedTokens) {
        String normalizedSectionText = CampaignOptionsRoute.normalizeSearchText(rawSectionText);
        for (String token : normalizedTokens) {
            if (!token.isBlank() && !normalizedSectionText.contains(token)) {
                return false;
            }
        }
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

    private void ensureCategoryLoaded(String topLevelResourceName) {
        switch (topLevelResourceName) {
            case "humanResourcesCategory" -> {
                if (personnelPages == null) {
                    initializeHumanResourcesSection();
                }
            }
            case "advancementCategory" -> {
                if (awardsAndRandomizationPages == null) {
                    initializeAdvancementSection();
                }
            }
            case "logisticsAndMaintenanceCategory" -> {
                if (equipmentAndSuppliesPages == null) {
                    initializeLogisticsSection();
                }
            }
            case "strategicOperationsCategory" -> {
                if (financesPages == null) {
                    initializeOperationsSection();
                }
            }
            default -> {
                // General is built eagerly.
            }
        }
    }

    private void ensureAllSectionsLoaded() {
        ensureCategoryLoaded("humanResourcesCategory");
        ensureCategoryLoaded("advancementCategory");
        ensureCategoryLoaded("logisticsAndMaintenanceCategory");
        ensureCategoryLoaded("strategicOperationsCategory");
    }

    private void initializeHumanResourcesSection() {
        personnelPages = new PersonnelPages(campaignOptions);
        biographyPages = new BiographyPages(campaign, generalPage);
        relationshipsPages = new RelationshipsPages(campaignOptions);
        salariesPages = new SalariesPages(campaignOptions);
        turnoverAndRetentionPages = new TurnoverAndRetentionPages(campaignOptions);
    }

    private void initializeAdvancementSection() {
        awardsAndRandomizationPages = new AwardsAndRandomizationPages(campaign);
        skillsPages = new SkillsPages(campaignOptions);
        abilitiesPages = new AbilitiesPages();
    }

    private void initializeLogisticsSection() {
        repairAndMaintenancePages = new RepairAndMaintenancePages(campaignOptions);
        equipmentAndSuppliesPages = new EquipmentAndSuppliesPages(campaignOptions);
    }

    private void initializeOperationsSection() {
        financesPages = new FinancesPages(campaign);
        marketsPages = new MarketsPages(campaign);
        systemsPages = new SystemsPages(campaign);
        rulesetsPages = new RulesetsPages(campaignOptions);
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
     * @return a {@link JScrollPane} containing the general page panel
     */
    private JPanel createGeneralPage(CampaignOptionsDialogMode mode) {
        generalPage = new GeneralPage(campaign, frame, mode);
        JPanel createdGeneralPage = generalPage.createGeneralPage();
        generalPage.loadValuesFromCampaignOptions();

        return createdGeneralPage;
    }

    private JPanel createPersonnelGeneralPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return personnelPages.createGeneralPage();
    }

    private JPanel createPersonnelAwardsPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return personnelPages.createAwardsPage();
    }

    private JPanel createPersonnelMedicalPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return personnelPages.createMedicalPage();
    }

    private JPanel createPersonnelInformationPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return personnelPages.createPersonnelInformationPage();
    }

    private JPanel createPersonnelPrisonersAndDependentsPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return personnelPages.createPrisonersAndDependentsPage();
    }

    private JPanel createBiographyGeneralPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return biographyPages.createGeneralPage();
    }

    private JPanel createBiographyBackgroundsPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return biographyPages.createBackgroundsPage();
    }

    private JPanel createBiographyDeathPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return biographyPages.createDeathPage();
    }

    private JPanel createBiographyEducationPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return biographyPages.createEducationPage();
    }

    private JPanel createBiographyNameAndPortraitGenerationPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return biographyPages.createNameAndPortraitGenerationPage();
    }

    private JPanel createBiographyRankPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return biographyPages.createRankPage();
    }

    private JPanel createRelationshipMarriagePage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return relationshipsPages.createMarriagePage();
    }

    private JPanel createRelationshipDivorcePage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return relationshipsPages.createDivorcePage();
    }

    private JPanel createRelationshipProcreationPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return relationshipsPages.createProcreationPage();
    }

    private JPanel createCombatSalariesPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return salariesPages.createSalariesPage(PersonnelRoleSubType.COMBAT);
    }

    private JPanel createSupportSalariesPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return salariesPages.createSalariesPage(PersonnelRoleSubType.SUPPORT);
    }

    private JPanel createCivilianSalariesPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return salariesPages.createSalariesPage(PersonnelRoleSubType.CIVILIAN);
    }

    private JPanel createTurnoverAndRetentionTurnoverPage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return turnoverAndRetentionPages.createTurnoverPage();
    }

    private JPanel createTurnoverAndRetentionFatiguePage() {
        ensureCategoryLoaded("humanResourcesCategory");
        return turnoverAndRetentionPages.createFatiguePage();
    }

    private JPanel createAdvancementRandomizationPage() {
        ensureCategoryLoaded("advancementCategory");
        return awardsAndRandomizationPages.skillRandomizationPage();
    }

    private JPanel createAdvancementXpAwardsPage() {
        ensureCategoryLoaded("advancementCategory");
        return awardsAndRandomizationPages.xpAwardsPage();
    }

    private JPanel createAdvancementRecruitmentBonusesPage() {
        ensureCategoryLoaded("advancementCategory");
        return awardsAndRandomizationPages.recruitmentBonusesPage();
    }

    private JPanel createAdvancementGunnerySkillsPage() {
        ensureCategoryLoaded("advancementCategory");
        return skillsPages.createSkillsPage(COMBAT_GUNNERY);
    }

    private JPanel createAdvancementPilotingSkillsPage() {
        ensureCategoryLoaded("advancementCategory");
        return skillsPages.createSkillsPage(COMBAT_PILOTING);
    }

    private JPanel createAdvancementSupportSkillsPage() {
        ensureCategoryLoaded("advancementCategory");
        return skillsPages.createSkillsPage(SUPPORT);
    }

    private JPanel createAdvancementUtilitySkillsPage() {
        ensureCategoryLoaded("advancementCategory");
        return skillsPages.createSkillsPage(UTILITY);
    }

    private JPanel createAdvancementRoleplaySkillsPage() {
        ensureCategoryLoaded("advancementCategory");
        return skillsPages.createSkillsPage(ROLEPLAY_GENERAL);
    }

    private JPanel createAdvancementCombatAbilitiesPage() {
        ensureCategoryLoaded("advancementCategory");
        return abilitiesPages.createAbilitiesPage(COMBAT_ABILITY);
    }

    private JPanel createAdvancementManeuveringAbilitiesPage() {
        ensureCategoryLoaded("advancementCategory");
        return abilitiesPages.createAbilitiesPage(MANEUVERING_ABILITY);
    }

    private JPanel createAdvancementUtilityAbilitiesPage() {
        ensureCategoryLoaded("advancementCategory");
        return abilitiesPages.createAbilitiesPage(UTILITY_ABILITY);
    }

    private JPanel createAdvancementCharacterFlawsPage() {
        ensureCategoryLoaded("advancementCategory");
        return abilitiesPages.createAbilitiesPage(CHARACTER_FLAW);
    }

    private JPanel createAdvancementCharacterCreationOnlyPage() {
        ensureCategoryLoaded("advancementCategory");
        return abilitiesPages.createAbilitiesPage(CHARACTER_CREATION_ONLY);
    }

    private JPanel createLogisticsRepairsPage() {
        ensureCategoryLoaded("logisticsAndMaintenanceCategory");
        return repairAndMaintenancePages.createRepairPage();
    }

    private JPanel createLogisticsMaintenancePage() {
        ensureCategoryLoaded("logisticsAndMaintenanceCategory");
        return repairAndMaintenancePages.createMaintenancePage();
    }

    private JPanel createLogisticsAcquisitionPage() {
        ensureCategoryLoaded("logisticsAndMaintenanceCategory");
        return equipmentAndSuppliesPages.createAcquisitionPage();
    }

    private JPanel createLogisticsPlanetaryAcquisitionPage() {
        ensureCategoryLoaded("logisticsAndMaintenanceCategory");
        return equipmentAndSuppliesPages.createPlanetaryAcquisitionPage();
    }

    private JPanel createLogisticsTechLimitsPage() {
        ensureCategoryLoaded("logisticsAndMaintenanceCategory");
        return equipmentAndSuppliesPages.createTechLimitsPage();
    }

    private JPanel createOperationsFinancesGeneralPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return financesPages.createFinancesGeneralOptionsPage();
    }

    private JPanel createOperationsPriceMultipliersPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return financesPages.createPriceMultipliersPage();
    }

    private JPanel createOperationsPersonnelMarketPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return marketsPages.createPersonnelMarketPage();
    }

    private JPanel createOperationsUnitMarketPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return marketsPages.createUnitMarketPage();
    }

    private JPanel createOperationsContractMarketPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return marketsPages.createContractMarketPage();
    }

    private JPanel createOperationsReputationPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return systemsPages.createReputationPage();
    }

    private JPanel createOperationsFactionStandingPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return systemsPages.createFactionStandingPage();
    }

    private JPanel createOperationsATimeOfWarPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return systemsPages.createATOWPage();
    }

    private JPanel createOperationsStratConPage() {
        ensureCategoryLoaded("strategicOperationsCategory");
        return rulesetsPages.createStratConPage();
    }

    /**
     * Applies the currently configured campaign options to the active
     * {@link Campaign}. This method processes all pages
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

        // Options get applied in the order they are defined in the UI
        generalPage.applyCampaignOptionsToCampaign(isStartUp, isSaveAction);

        // Human Resources
        if (personnelPages != null) {
            personnelPages.applyCampaignOptionsToCampaign(campaign, options);
            biographyPages.applyCampaignOptionsToCampaign(options);
            relationshipsPages.applyCampaignOptionsToCampaign(options);
            salariesPages.applyCampaignOptionsToCampaign(options);
            turnoverAndRetentionPages.applyCampaignOptionsToCampaign(options);
        }

        // Advancement
        if (awardsAndRandomizationPages != null) {
            awardsAndRandomizationPages.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);
            skillsPages.applyCampaignOptionsToCampaign(options, presetSkills);
            abilitiesPages.applyCampaignOptionsToCampaign(preset);
        }

        // Logistics
        if (equipmentAndSuppliesPages != null) {
            equipmentAndSuppliesPages.applyCampaignOptionsToCampaign(options);
            repairAndMaintenancePages.applyCampaignOptionsToCampaign(options);
        }

        // Operations
        if (financesPages != null) {
            financesPages.applyCampaignOptionsToCampaign(options);
            marketsPages.applyCampaignOptionsToCampaign(options);
            rulesetsPages.applyCampaignOptionsToCampaign(options);
            systemsPages.applyCampaignOptionsToCampaign(options, presetRandomSkillPreferences);
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
    public static void triggerUpgradeFreebies(@Nonnull Campaign campaign, CampaignOptionsFreebieTracker oldOptions,
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
     * Applies the values from a {@link CampaignPreset} to all pages in the dialog.
     * This propagates preset-specific
     * configuration to all associated components and sub-pages, including
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

        generalPage.loadValuesFromCampaignOptions(presetDate, presetFaction);

        // Human Resources
        personnelPages.loadValuesFromCampaignOptions(presetCampaignOptions, campaign.getVersion());
        biographyPages.loadValuesFromCampaignOptions(presetCampaignOptions,
                presetCampaignOptions.getRandomOriginOptions(),
                campaignPreset.getRankSystem());
        relationshipsPages.loadValuesFromCampaignOptions(presetCampaignOptions);
        turnoverAndRetentionPages.loadValuesFromCampaignOptions(presetCampaignOptions);

        // Advancement
        awardsAndRandomizationPages.loadValuesFromCampaignOptions(presetCampaignOptions,
                campaignPreset.getRandomSkillPreferences());
        skillsPages.loadValuesFromCampaignOptions(presetCampaignOptions, campaignPreset.getSkills());
        // The ability page is a special case, so handled differently to other pages
        abilitiesPages.buildAllAbilityInfo(campaignPreset.getSpecialAbilities());

        // Logistics
        equipmentAndSuppliesPages.loadValuesFromCampaignOptions(presetCampaignOptions);
        repairAndMaintenancePages.loadValuesFromCampaignOptions(presetCampaignOptions);

        // Operations
        financesPages.loadValuesFromCampaignOptions(presetCampaignOptions);
        marketsPages.loadValuesFromCampaignOptions(presetCampaignOptions);
        rulesetsPages.loadValuesFromCampaignOptions(presetCampaignOptions);
        systemsPages.loadValuesFromCampaignOptions(presetCampaignOptions, campaignPreset.getRandomSkillPreferences());
    }
}
