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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import mekhq.campaign.Campaign;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.dialog.BaseSettingsDialog;

/**
 * Context menu that lets the player send selected personnel to a known {@link ILocation}.
 *
 * <p>The menu lists the campaign's Main Force and all registered {@link PlayerBase} instances.
 * The destination that all selected people currently share (if any) is excluded. Bases are
 * grouped into nested sub-menus based on their {@code displayType} field, with "/" acting as
 * a folder separator.</p>
 *
 * <p>A "New Base..." item at the bottom opens {@link BaseSettingsDialog}.</p>
 */
public class SendToLocationMenu extends JScrollableMenu {

    public SendToLocationMenu(Campaign campaign, JFrame frame, Person... people) {
        super("SendToLocationMenu");
        initialize(campaign, frame, people);
    }

    private void initialize(Campaign campaign, JFrame frame, Person... people) {
        if (people.length == 0) {
            return;
        }

        setText("Send To...");

        List<Person> peopleList = Arrays.asList(people);

        // Determine the ILocation parent all selected people share (if any), to exclude it
        Set<ILocation> currentLocations = Arrays.stream(people)
              .map(p -> {
                  var node = p.getLocationNode();
                  if (node == null || node.getParent() == null) {
                      return null;
                  }
                  return node.getParent().getLocatable();
              })
              .collect(Collectors.toSet());

        ILocation sharedCurrent = currentLocations.size() == 1
              ? currentLocations.iterator().next()
              : null;

        // Main Force entry (= the campaign itself)
        if (!Objects.equals(sharedCurrent, campaign)) {
            JMenuItem mainForce = new JMenuItem("Main Force");
            mainForce.addActionListener(e ->
                  LocationDispatch.dispatchToLocation(peopleList, campaign, campaign));
            add(mainForce);
        }

        // One entry per player base, nested by display type
        Map<String, JMenu> pathToMenu = new HashMap<>();
        for (PlayerBase base : campaign.getPlayerBases()) {
            if (Objects.equals(sharedCurrent, base)) {
                continue;
            }
            addBaseItem(this, base, peopleList, campaign, pathToMenu);
        }

        addSeparator();

        JMenuItem newBase = new JMenuItem("New Base...");
        newBase.addActionListener(e -> {
            PlanetarySystem contextSystem = people.length > 0 ? people[0].getCurrentSystem() : null;
            Planet contextPlanet = people.length > 0 ? people[0].getPlanet() : null;
            if (contextSystem == null) {
                contextSystem = campaign.getCurrentSystem();
                contextPlanet = campaign.getPlanet();
            }
            BaseSettingsDialog dlg = new BaseSettingsDialog(frame, campaign,
                  contextSystem, contextPlanet);
            dlg.setVisible(true);
            dlg.getResult().ifPresent(base ->
                  LocationDispatch.dispatchToLocation(peopleList, base, campaign));
        });
        add(newBase);
    }

    private static void addBaseItem(JMenu root, PlayerBase base, List<Person> people,
          Campaign campaign, Map<String, JMenu> pathToMenu) {
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
                JMenu sub = pathToMenu.get(key);
                if (sub == null) {
                    sub = new JMenu(part);
                    parent.add(sub);
                    pathToMenu.put(key, sub);
                }
                parent = sub;
            }
        }

        JMenuItem item = new JMenuItem(base.getDisplayName());
        item.addActionListener(e ->
              LocationDispatch.dispatchToLocation(people, base, campaign));
        parent.add(item);
    }
}
