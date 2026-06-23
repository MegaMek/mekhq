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
package mekhq.gui.menus;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import mekhq.campaign.Campaign;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.dialog.BaseSettingsDialog;

/**
 * Context menu that lets the player send selected {@link ILocation} items (persons, units, or
 * spare parts) to a known destination {@link ILocation}.
 *
 * <p>The menu lists the campaign's Main Force and all registered {@link PlayerBase} instances.
 * The destination that all selected items currently share (if any) is excluded. Bases are
 * grouped into nested sub-menus based on their {@code displayType} field, with "/" acting as
 * a folder separator.</p>
 *
 * <p>A "New Base..." item at the bottom opens {@link BaseSettingsDialog}.</p>
 *
 * <p>The caller provides a {@code dispatcher} callback that is invoked with the chosen
 * destination — this class contains no dispatch logic itself and can therefore be reused for
 * any type of {@link ILocation} item.</p>
 */
public class SendToLocationMenu extends JScrollableMenu {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.SendToLocationMenu";

    /**
     * @param campaign   the active campaign
     * @param frame      parent frame for any dialogs
     * @param items      the {@link ILocation} objects to dispatch (persons, units, or parts)
     * @param dispatcher callback invoked with the chosen destination when the user clicks an entry
     */
    public SendToLocationMenu(Campaign campaign, JFrame frame,
          List<? extends ILocation> items,
          Consumer<ILocation> dispatcher) {
        super("SendToLocationMenu");
        initialize(campaign, frame, items, dispatcher);
    }

    private void initialize(Campaign campaign, JFrame frame,
          List<? extends ILocation> items,
          Consumer<ILocation> dispatcher) {
        if (items.isEmpty()) {
            return;
        }

        setText(getTextAt(RESOURCE_BUNDLE, "menu.sendTo.text"));

        // Determine the ILocation parent all selected items share (if any), to exclude it.
        // ILocation subtypes like Personnel extend LinkedHashMap, so Objects.equals() would
        // compare map contents rather than identity — two empty Personnel objects would falsely
        // match, hiding every base from the menu. Use an identity-keyed set instead.
        Set<ILocation> currentLocations = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ILocation item : items) {
            currentLocations.add(item.isParented() ? item.getParentLocation() : null);
        }

        ILocation sharedCurrent = currentLocations.size() == 1
              ? currentLocations.iterator().next()
              : null;

        // Main Force entry (= the campaign itself).
        // Items at main force are parented to mainForcePersonnel, hangar, or warehouse — not
        // to the campaign object directly — so check all three sub-resources.
        boolean alreadyAtMainForce = sharedCurrent == campaign
              || sharedCurrent == campaign.getMainForcePersonnel()
              || sharedCurrent == campaign.getHangar()
              || sharedCurrent == campaign.getWarehouse();
        if (!alreadyAtMainForce) {
            JMenuItem mainForce = new JMenuItem(getTextAt(RESOURCE_BUNDLE, "label.mainForce.text"));
            mainForce.addActionListener(e -> dispatcher.accept(campaign));
            add(mainForce);
        }

        // One entry per player base, nested by display type
        Map<String, JMenu> pathToMenu = new HashMap<>();
        for (PlayerBase base : campaign.getPlayerBases()) {
            // Use == (identity) — equals() on Personnel compares map contents, not object identity
            if (sharedCurrent == base
                  || sharedCurrent == base.getBasePersonnel()
                  || sharedCurrent == base.getBaseWarehouse()
                  || sharedCurrent == base.getBaseHangar()) {
                continue;
            }
            addBaseItem(this, base, dispatcher, pathToMenu);
        }

        addSeparator();

        JMenuItem newBase = new JMenuItem(getTextAt(RESOURCE_BUNDLE, "menuItem.newBase.text"));
        newBase.addActionListener(e -> {
            PlanetarySystem contextSystem = items.get(0).getCurrentSystem();
            Planet contextPlanet = items.get(0).getPlanet();
            if (contextSystem == null) {
                contextSystem = campaign.getCurrentSystem();
                contextPlanet = campaign.getPlanet();
            }
            BaseSettingsDialog dialog = new BaseSettingsDialog(frame, campaign,
                  contextSystem, contextPlanet);
            dialog.setVisible(true);
            dialog.getResult().ifPresent(dispatcher::accept);
        });
        add(newBase);
    }

    private static void addBaseItem(JMenu root, PlayerBase base,
          Consumer<ILocation> dispatcher,
          Map<String, JMenu> pathToMenu) {
        String displayType = base.getDisplayType();
        JMenu parent;

        if (displayType == null || displayType.isBlank()) {
            parent = root;
        } else {
            String[] parts = displayType.split("/", -1);
            StringBuilder pathSoFar = new StringBuilder();
            parent = root;
            for (String part : parts) {
                if (part.isBlank()) {
                    continue;
                }
                pathSoFar.append('/').append(part);
                String key = pathSoFar.toString();
                JMenu submenu = pathToMenu.get(key);
                if (submenu == null) {
                    submenu = new JMenu(part);
                    parent.add(submenu);
                    pathToMenu.put(key, submenu);
                }
                parent = submenu;
            }
        }

        JMenuItem item = new JMenuItem(base.getDisplayName());
        item.addActionListener(e -> dispatcher.accept(base));
        parent.add(item);
    }
}
