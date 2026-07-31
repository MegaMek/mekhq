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
package mekhq.gui.clientOptions;

import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsNavigationText;
import megamek.client.ui.settings.SettingsPane;
import megamek.client.ui.settings.SettingsRoute;
import megamek.client.ui.util.UIUtil;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.gui.campaignOptions.CampaignOptionsUtilities;

/**
 * MekHQ Client Options pane backed by the shared MegaMek settings navigation, page, search, and contextual-help
 * framework. Option values remain owned by {@link MHQOptionsModel} and are applied to {@link MHQOptions} only when the
 * hosting dialog confirms the edits.
 */
public class MHQOptionsPane extends JPanel {
    // Unscaled initial dialog height, floored in getPreferredSize() and scaled at use. The base dialog still clamps
    // the packed size to 80% of the screen, so this is an upper target rather than a hard height.
    private static final int START_HEIGHT = 800;

    private final JFrame frame;
    private final MHQOptions options;
    private final MHQOptionsModel model;
    private final List<SettingsRoute> routes = new ArrayList<>();
    private final Map<String, Supplier<java.awt.Component>> pageFactories = new HashMap<>();
    // Extracted per-page objects (each an MHQOptionsPage), tracked so save() can write every visited page back.
    private final List<MHQOptionsPage> pages = new ArrayList<>();

    private SettingsPane settingsPane;

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

    private void registerRoute(String id, Supplier<java.awt.Component> pageFactory, String... titleResourceNames) {
        List<String> path = new ArrayList<>();
        for (String titleResourceName : titleResourceNames) {
            path.add(getText(titleResourceName + ".title"));
        }
        pageFactories.put(id, pageFactory);
        routes.add(new SettingsRoute(id, path, List.of(titleResourceNames), false));
    }

    private void initialize() {
        SettingsNavigationText navigationText = new SettingsNavigationText(
              getText("txtCampaignOptionsFilter.text"),
              getText("txtCampaignOptionsFilter.tooltip"),
              getText("campaignOptionsFilter.noMatches"),
              getText("campaignOptionsFilter.matches"),
              getText("btnExpandAll.text"),
              getText("btnCollapseAll.text"));
        settingsPane = new SettingsPane(routes, pageFactories, navigationText,
              getText("campaignOptionsHelp.title"));
        add(settingsPane, BorderLayout.CENTER);
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
        int floorWidth = UIUtil.scaleForGUI(SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH)
                               + CampaignOptionsUtilities.campaignOptionsPanelWidth();
        int floorHeight = UIUtil.scaleForGUI(START_HEIGHT);
        return new Dimension(Math.max(preferred.width, floorWidth), Math.max(preferred.height, floorHeight));
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
