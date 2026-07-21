/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.MHQOptionsPage.RESOURCE_BUNDLE;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * Pilot pane that reuses the Campaign Options tree-navigation framework (the navigation panel, content host, and page
 * shell) for the MekHQ Client Options. Its purpose is to validate that the framework is generic across a second
 * consumer - one backed by {@link MHQOptions} and the {@code GUI.properties} bundle rather than the campaign - before
 * the framework is extracted into MegaMek.
 *
 * <p>This lives in the {@code campaignOptions} package for now so it can use the framework's package-private classes;
 * when the framework is extracted and made public, this pane will move to its own home.</p>
 */
public class MHQOptionsPane extends JPanel {
    private static final int CONTENT_MARGIN = UIUtil.scaleForGUI(4);
    // Unscaled initial dialog height, floored in getPreferredSize() and scaled at use. The base dialog still clamps
    // the packed size to 80% of the screen, so this is an upper target rather than a hard height.
    private static final int START_HEIGHT = 800;

    private final JFrame frame;
    private final MHQOptions options;
    private final MHQOptionsModel model;
    private final List<CampaignOptionsRoute> routes = new ArrayList<>();
    private final Map<String, Supplier<Component>> pageFactories = new HashMap<>();
    private final Map<String, Component> pageCache = new HashMap<>();
    // Extracted per-page objects (each an MHQOptionsPage), tracked so save() can write every visited page back.
    private final List<MHQOptionsPage> pages = new ArrayList<>();

    private CampaignOptionsContentHost contentHost;
    private CampaignOptionsNavigationPanel navigationPanel;
    private boolean searchIndexInitialized;

    public MHQOptionsPane(JFrame frame) {
        super(new BorderLayout());
        setName("mhqOptionsPane");
        this.frame = frame;
        options = MekHQ.getMHQOptions();
        model = new MHQOptionsModel(options);
        registerRoutes();
        initialize();
    }

    private void registerRoutes() {
        registerRoute("display", new MHQDisplayPage(model), "displayPage");
        registerRoute("colours", new MHQColoursPage(model), "coloursPage");
        registerRoute("fonts", new MHQFontsPage(model), "fontsPage");
        registerRoute("saveOptions", new MHQSaveOptionsPage(model), "saveOptionsPage");
        registerRoute("newDay", new MHQNewDayPage(model), "newDayPage");
        registerRoute("reminders", new MHQRemindersPage(model), "remindersPage");
        registerRoute("advanced", new MHQAdvancedPage(model, frame), "advancedPage");
    }

    /** Registers a route backed by an extracted {@link MHQOptionsPage}, tracking it so its values are saved. */
    private void registerRoute(String id, MHQOptionsPage page, String... titleResourceNames) {
        pages.add(page);
        registerRoute(id, page::createPage, titleResourceNames);
    }

    private void registerRoute(String id, Supplier<Component> pageFactory, String... titleResourceNames) {
        List<String> path = new ArrayList<>();
        for (String titleResourceName : titleResourceNames) {
            path.add(getTextAt(RESOURCE_BUNDLE, titleResourceName + ".title"));
        }
        pageFactories.put(id, pageFactory);
        routes.add(new CampaignOptionsRoute(id, path, List.of(titleResourceNames)));
    }

    private void initialize() {
        CampaignOptionsRoute initialRoute = routes.get(0);
        Component initialContent = getPage(initialRoute.getId());
        contentHost = new CampaignOptionsContentHost(initialContent, null, false, RESOURCE_BUNDLE);
        navigationPanel = new CampaignOptionsNavigationPanel(routes, this::selectRoute, RESOURCE_BUNDLE);
        // Lazily index every page's section titles/summaries on the first search, so the navigation filter matches
        // section headings and not only page titles (matching Campaign Options).
        navigationPanel.setSearchIndexInitializer(this::ensureSearchIndexBuilt);

        // Match the Campaign Options pane: a small content margin (0 at the bottom, since the footer adds its own top
        // padding). getPreferredSize() floors the width so the dialog opens at a Campaign-Options-comparable size
        // rather than packing tightly around the narrower MekHQ pages.
        setBorder(BorderFactory.createEmptyBorder(CONTENT_MARGIN, CONTENT_MARGIN, 0, CONTENT_MARGIN));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigationPanel, contentHost);
        splitPane.setName("mhqOptionsSplitPane");
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(UIUtil.scaleForGUI(CampaignOptionsNavigationPanel.NAVIGATION_WIDTH));
        add(splitPane, BorderLayout.CENTER);

        navigationPanel.selectRoute(initialRoute);
    }

    /**
     * Floors the pane's initial size so the dialog opens at a comfortable size rather than packing tightly around the
     * simpler MekHQ pages (whose sections start collapsed). Width is floored to the navigation column plus the shared
     * page-width cap (narrower pages centre within it, as in Campaign Options); height is floored to a fixed start
     * height. The base dialog still clamps the result to 80% of the screen, the dialog stays freely resizable, and
     * naturally larger content still wins via {@code Math.max}.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        int floorWidth = UIUtil.scaleForGUI(CampaignOptionsNavigationPanel.NAVIGATION_WIDTH)
              + CampaignOptionsUtilities.campaignOptionsPanelWidth();
        int floorHeight = UIUtil.scaleForGUI(START_HEIGHT);
        return new Dimension(Math.max(preferred.width, floorWidth), Math.max(preferred.height, floorHeight));
    }

    private void selectRoute(CampaignOptionsRoute route) {
        Component page = getPage(route.getId());
        if (page != null) {
            contentHost.setContent(page, null, false);
            expandSectionsForActiveFilter(page);
        }
    }

    /**
     * When a page is opened while a navigation search is active, expands the section(s) whose title or summary match
     * the search so the result the user selected is visible. If the filter matched the page rather than a particular
     * section, every section is expanded so the page's content is still revealed.
     *
     * @param page the page component just shown
     */
    private void expandSectionsForActiveFilter(Component page) {
        if (navigationPanel == null) {
            return;
        }

        String activeFilter = navigationPanel.getActiveFilter();
        if (activeFilter.isBlank()) {
            return;
        }

        CampaignOptionsPagePanel pagePanel = CampaignOptionsContentHost.findPagePanel(page);
        if (pagePanel == null) {
            return;
        }

        String[] tokens = activeFilter.split("\\s+");
        boolean expandedMatchingSection = pagePanel.expandSectionsMatching(
              sectionText -> sectionMatchesAllTokens(sectionText, tokens));
        if (!expandedMatchingSection) {
            pagePanel.expandAllSections();
        }
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

    private Component getPage(String routeId) {
        Component page = pageCache.computeIfAbsent(routeId, id -> pageFactories.get(id).get());
        harvestSectionSearchText(routeId, page);
        return page;
    }

    /**
     * Copies the resolved section titles and summaries of a freshly built page into its matching route, so the
     * navigation filter can match section headings and not only the page title. A no-op when the page has no
     * sections; re-running it simply re-sets the same text.
     */
    private void harvestSectionSearchText(String routeId, Component page) {
        if (!(page instanceof CampaignOptionsPagePanel pagePanel)) {
            return;
        }
        String sectionSearchText = pagePanel.getSectionSearchText();
        if (sectionSearchText.isBlank()) {
            return;
        }
        for (CampaignOptionsRoute route : routes) {
            if (route.getId().equals(routeId)) {
                route.setSectionSearchText(sectionSearchText);
                return;
            }
        }
    }

    /**
     * Builds every page once, on demand, so section titles and summaries become searchable across all pages rather
     * than only the pages the user has already opened. Pages are built progressively, one per Swing event, to keep
     * the dialog responsive, and are cached so later navigation is instant. Runs at most once.
     */
    void ensureSearchIndexBuilt() {
        if (searchIndexInitialized) {
            return;
        }
        searchIndexInitialized = true;
        buildSearchIndexStep(new ArrayList<>(pageFactories.keySet()), 0);
    }

    private void buildSearchIndexStep(List<String> routeIds, int index) {
        if (index >= routeIds.size()) {
            if (navigationPanel != null) {
                navigationPanel.refreshFilter();
            }
            return;
        }
        getPage(routeIds.get(index));
        SwingUtilities.invokeLater(() -> buildSearchIndexStep(routeIds, index + 1));
    }

    /**
     * Writes the edited options back to {@link MHQOptions}. Called by the hosting dialog when the user confirms. Each
     * visited page copies its controls into the shared {@link MHQOptionsModel}; a page the user never opened was never
     * built, so its {@link MHQOptionsPage#writeToModel()} is a no-op and the model keeps the values it was built with.
     * The fully-populated model is then applied to {@link MHQOptions} (and the GUI-scale and user-directory stores) in
     * one step.
     */
    public void save() {
        for (MHQOptionsPage page : pages) {
            page.writeToModel();
        }
        model.applyTo(options);
    }
}
