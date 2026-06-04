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

import static java.lang.Math.max;

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
 * Utility for moving groups of students to or from an academy campus via shared
 * {@link CurrentLocation} travel nodes.
 *
 * <p>Students departing from the same system are batched into a single {@code CurrentLocation}
 * so they travel together rather than each getting their own node.</p>
 */
public final class LocationDispatch {

    private LocationDispatch() {}

    /**
     * Dispatches {@code people} to an academy campus identified by {@code academySet},
     * {@code academyName}, and {@code campusSystemId}.
     *
     * <p>Students departing from the same system share one {@link CurrentLocation}. Students
     * already at the campus system are reparented directly. When no jump path can be calculated,
     * the existing day-counter fallback is used without creating a travel node.</p>
     *
     * <p>Sets {@code eduJourneyTime} on each person.</p>
     *
     * @param people         the students to dispatch; must not be {@code null}
     * @param academySet     the academy set name; must not be {@code null}
     * @param academyName    the academy name within the set; must not be {@code null}
     * @param campusSystemId the ID of the planetary system hosting the campus; must not be {@code null}
     * @param campaign       the active campaign; must not be {@code null}
     */
    public static void dispatchToAcademy(Collection<Person> people, String academySet,
            String academyName, String campusSystemId, Campaign campaign) {
        PlanetarySystem destSystem = campaign.getSystemById(campusSystemId);

        Map<PlanetarySystem, List<Person>> bySystem = people.stream()
              .collect(Collectors.groupingBy(p -> {
                  PlanetarySystem sys = p.getCurrentSystem();
                  return sys != null ? sys : campaign.getCurrentSystem();
              }));

        for (Map.Entry<PlanetarySystem, List<Person>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<Person> group = entry.getValue();

            if (destSystem != null && fromSystem.equals(destSystem)) {
                AcademyCampusLocation campusLoc = campaign.getOrCreateCampusLocation(
                      academySet, academyName, campusSystemId);
                if (campusLoc == null) {
                    throw new IllegalStateException(
                          "Campus location must exist for system " + campusSystemId);
                }
                group.forEach(p -> {
                    p.setParent(campusLoc);
                    p.setEduJourneyTime(2);
                });
                continue;
            }

            JumpPath path = (destSystem != null)
                  ? campaign.calculateJumpPath(fromSystem, destSystem)
                  : null;

            if (path == null || path.isEmpty()) {
                int travelTime = destSystem != null
                      ? max(2, campaign.getSimplifiedTravelTime(destSystem))
                      : 14;
                group.forEach(p -> p.setEduJourneyTime(travelTime));
                continue;
            }

            double startTransit = fromSystem.equals(campaign.getCurrentSystem())
                  && campaign.getCurrentLocation() != null
                  ? campaign.getCurrentLocation().getTransitTime()
                  : 0.0;

            AcademyCampusLocation campusLoc = campaign.getOrCreateCampusLocation(
                  academySet, academyName, campusSystemId);
            if (campusLoc == null) {
                throw new IllegalStateException(
                      "Campus location must exist for system " + campusSystemId);
            }

            CurrentLocation travelLoc = new CurrentLocation(fromSystem, startTransit);
            travelLoc.setJumpPath(path);
            travelLoc.setParent(campusLoc);
            campaign.addLocation(travelLoc);

            int journeyDays = max(2,
                  (int) Math.ceil(path.getTotalTime(campaign.getLocalDate(), startTransit, false)));
            group.forEach(p -> {
                p.setParent(travelLoc);
                p.setEduJourneyTime(journeyDays);
            });
        }
    }

    /**
     * Dispatches {@code people} homeward from their academy campuses to the campaign's current
     * system, creating a shared {@link CurrentLocation} for each group departing from the same
     * academy system.
     *
     * <p>When no jump path exists, people remain parented under their campus location so that
     * {@code getEduAcademySystem()} continues to resolve correctly during the day-counter phase
     * of the return journey.</p>
     *
     * <p>Sets {@code eduJourneyTime} and resets {@code eduDaysOfTravel} to zero on each person.</p>
     *
     * @param people   the students to send home; must not be {@code null}
     * @param campaign the active campaign; must not be {@code null}
     */
    public static void dispatchHome(Collection<Person> people, Campaign campaign) {
        PlanetarySystem homeSystem = campaign.getCurrentSystem();

        Map<String, List<Person>> byAcademySystem = people.stream()
              .collect(Collectors.groupingBy(Person::getEduAcademySystem));

        for (Map.Entry<String, List<Person>> entry : byAcademySystem.entrySet()) {
            String academySystemId = entry.getKey();
            List<Person> group = entry.getValue();
            Person first = group.get(0);

            PlanetarySystem academySystem = campaign.getSystemById(academySystemId);

            AcademyCampusLocation campusLoc = campaign.getOrCreateCampusLocation(
                  first.getEduAcademySet(), first.getEduAcademyNameInSet(), academySystemId);
            if (campusLoc == null) {
                throw new IllegalStateException(
                      "Campus location must exist for system " + academySystemId);
            }

            JumpPath returnPath = (academySystem != null && homeSystem != null)
                  ? campaign.calculateJumpPath(academySystem, homeSystem)
                  : null;

            if (returnPath == null || returnPath.isEmpty()) {
                int travelTime = academySystem != null
                      ? max(2, campaign.getSimplifiedTravelTime(academySystem))
                      : 14;
                group.forEach(p -> {
                    p.setParent(campusLoc);
                    p.setEduJourneyTime(travelTime);
                    p.setEduDaysOfTravel(0);
                });
                continue;
            }

            double startTransit = academySystem.getTimeToJumpPoint(1.0);
            CurrentLocation returnLoc = new CurrentLocation(academySystem, startTransit);
            returnLoc.setJumpPath(returnPath);
            returnLoc.setParent(campusLoc);
            campaign.addLocation(returnLoc);

            int journeyDays = max(2,
                  (int) Math.ceil(returnPath.getTotalTime(campaign.getLocalDate(), startTransit, false)));
            group.forEach(p -> {
                p.setParent(returnLoc);
                p.setEduJourneyTime(journeyDays);
                p.setEduDaysOfTravel(0);
            });
        }
    }
}
