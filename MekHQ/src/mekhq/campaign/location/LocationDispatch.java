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
package mekhq.campaign.location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Utility for moving groups of people to a destination {@link ILocation} via a shared
 * {@link CurrentLocation} travel node.
 *
 * <p>People departing from the same system are batched into a single {@code CurrentLocation}
 * so that they travel together. People already in the destination system are reparented
 * directly without creating a travel node.</p>
 */
public final class LocationDispatch {

    private LocationDispatch() {}

    /**
     * Dispatches {@code people} to {@code destination}, grouping by departure system so that
     * everyone leaving from the same system shares one {@link CurrentLocation} for the journey.
     *
     * <p>Mirrors the travel logic in {@code EducationController.enrollPerson()}.</p>
     *
     * @param people      the people to dispatch; must not be {@code null}
     * @param destination the target {@link ILocation}; must not be {@code null}
     * @param campaign    the active campaign; must not be {@code null}
     */
    public static void dispatchToLocation(Collection<Person> people,
          ILocation destination,
          Campaign campaign) {

        PlanetarySystem destSystem = destination.getCurrentSystem();

        Map<PlanetarySystem, List<Person>> bySystem = people.stream()
              .collect(Collectors.groupingBy(p -> {
                  PlanetarySystem sys = p.getCurrentSystem();
                  return sys != null ? sys : campaign.getCurrentSystem();
              }));

        for (Map.Entry<PlanetarySystem, List<Person>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<Person> group = entry.getValue();

            if (destSystem == null || fromSystem.equals(destSystem)) {
                group.forEach(p -> p.setParent(destination));
                continue;
            }

            JumpPath path = campaign.calculateJumpPath(fromSystem, destSystem);
            if (path == null || path.isEmpty()) {
                group.forEach(p -> p.setParent(destination));
                continue;
            }

            double startTransit = campaign.getLocation() != null
                  ? campaign.getLocation().getTransitTime()
                  : fromSystem.getTimeToJumpPoint(1.0);

            CurrentLocation travelLoc = new CurrentLocation(fromSystem, startTransit);
            travelLoc.setJumpPath(path);
            travelLoc.setParent(destination);
            group.forEach(p -> p.setParent(travelLoc));
            campaign.addLocation(travelLoc);
        }
    }

}
