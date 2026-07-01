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
package mekhq.gui;

import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;

import megamek.common.ui.EnhancedTabbedPane;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.enums.MHQTabType;

/**
 * Top-level campaign tab that hosts the interstellar map and locations subtabs.
 */
public class NavigationTab extends CampaignGuiTab {
    private EnhancedTabbedPane innerTabs;
    private MapTab mapTab;
    private LocationsTab locationsTab;

    public NavigationTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

    @Override
    public void initTab() {
        setLayout(new BorderLayout());

        innerTabs = new EnhancedTabbedPane(true, true);
        mapTab = new MapTab(getCampaignGui(), getText("NavigationTab.MapTab.title"));
        locationsTab = new LocationsTab(getCampaignGui(), getText("NavigationTab.LocationsTab.title"));

        innerTabs.addTab(mapTab.getTabName(), mapTab);
        innerTabs.addTab(locationsTab.getTabName(), locationsTab);

        add(innerTabs, BorderLayout.CENTER);
    }

    public MapTab getMapTab() {
        return mapTab;
    }

    public void showSystem(PlanetarySystem planetarySystem) {
        mapTab.switchSystemsMap(planetarySystem);
        int index = innerTabs.indexOfComponent(mapTab);
        if (index >= 0) {
            innerTabs.setSelectedIndex(index);
        }
    }

    public void showPlanet(Planet planet) {
        // Stay on the interstellar map if their origin planet is the primary planet...
        if (planet.getParentSystem().getPrimaryPlanet().equals(planet)) {
            showSystem(planet.getParentSystem());
        } else {
            // ...otherwise, dive on in to the system view!
            mapTab.switchPlanetaryMap(planet);
            int index = innerTabs.indexOfComponent(mapTab);
            if (index >= 0) {
                innerTabs.setSelectedIndex(index);
            }
        }
    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.NAVIGATION;
    }

    @Override
    public void refreshAll() {
        mapTab.refreshAll();
        locationsTab.refreshAll();
    }

}
