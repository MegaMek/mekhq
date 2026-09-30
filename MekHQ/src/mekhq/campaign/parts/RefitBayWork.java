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
package mekhq.campaign.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import megamek.common.bays.Bay;
import megamek.common.bays.BayType;
import megamek.common.units.Entity;

/**
 * The refit work on cargo and transport bays that the bays' own parts do not cover (CO p. 206), and the refit class
 * that work falls in (CO p. 211). Cubicles and doors that are added or removed carry their own time as parts: 7 days
 * per cubicle, 10 hours per door. What remains is:
 * <ul>
 *     <li>a month for each cargo or infantry bay that is added, removed or resized on a large craft (2 hours on
 *     other units), since these bays have no cubicles to carry the work; and</li>
 *     <li>door time for each door taken out of one bay and fitted to another, since the door part itself does not
 *     change.</li>
 * </ul>
 * Crew quarters are left out; a change in crew size is handled with life support. Bay work that only removes is Class
 * A; bay work that adds is Class B if the refit also takes bay parts out, as when a BattleMek bay becomes fighter bays,
 * and Class C if it takes nothing out.
 *
 * @param time                       the extra refit time, in minutes
 * @param refitClass                 the refit class of the bay work, {@link Refit#NO_CHANGE} if there is none
 * @param baysWithoutCubiclesChanged the cargo and infantry bays added, removed or resized
 * @param doorsMoved                 the doors taken out of one bay and fitted to another
 */
record RefitBayWork(int time, int refitClass, int baysWithoutCubiclesChanged, int doorsMoved) {
    /**
     * @param oldEntity the unit as it is
     * @param newEntity the design it is being refitted to
     *
     * @return the bay work that turns the old unit's bays into the new design's
     */
    static RefitBayWork between(Entity oldEntity, Entity newEntity) {
        List<Bay> oldBays = baysOtherThanQuarters(oldEntity);
        List<Bay> newBays = baysOtherThanQuarters(newEntity);

        // A bay of the same type, size and doors is unchanged
        removeMatchingBays(oldBays, newBays, true);

        // A bay of the same type and size only gains or loses doors
        DoorChanges doorChanges = removeMatchingBays(oldBays, newBays, false);
        int doorsTakenOut = doorChanges.takenOut();
        int oldBaysWithoutCubicles = 0;
        for (Bay oldBay : oldBays) {
            doorsTakenOut += oldBay.getDoors();
            if (hasNoCubicles(oldBay)) {
                oldBaysWithoutCubicles++;
            }
        }
        int doorsFitted = doorChanges.fitted();
        int newBaysWithoutCubicles = 0;
        for (Bay newBay : newBays) {
            doorsFitted += newBay.getDoors();
            if (hasNoCubicles(newBay)) {
                newBaysWithoutCubicles++;
            }
        }
        int doorsMoved = Math.min(doorsTakenOut, doorsFitted);
        int baysWithoutCubiclesChanged = Math.max(oldBaysWithoutCubicles, newBaysWithoutCubicles);

        boolean isLargeCraft = newEntity.isLargeCraft();
        int bayDuration = isLargeCraft ? Refit.WORK_MONTH : Refit.WORK_HOUR * 2;
        int doorDuration = isLargeCraft ? Refit.WORK_HOUR * 10 : Refit.WORK_HOUR * 2;
        int time = (baysWithoutCubiclesChanged * bayDuration) + (doorsMoved * doorDuration);

        BayComponents oldComponents = BayComponents.of(oldEntity);
        BayComponents newComponents = BayComponents.of(newEntity);
        boolean isMovingDoors = doorsMoved > 0;
        boolean isFittingCubiclesOrDoors = newComponents.hasMoreThan(oldComponents);
        boolean isFittingBaysWithoutCubicles = newBaysWithoutCubicles > 0;
        boolean isAddingBayParts = isFittingCubiclesOrDoors || isFittingBaysWithoutCubicles || isMovingDoors;
        boolean isTakingOutCubiclesOrDoors = oldComponents.hasMoreThan(newComponents);
        boolean isTakingOutBaysWithoutCubicles = oldBaysWithoutCubicles > 0;
        boolean isRemovingBayParts = isTakingOutCubiclesOrDoors || isTakingOutBaysWithoutCubicles || isMovingDoors;

        int refitClass = Refit.NO_CHANGE;
        if (isAddingBayParts) {
            refitClass = isRemovingBayParts ? Refit.CLASS_B : Refit.CLASS_C;
        } else if (isRemovingBayParts) {
            refitClass = Refit.CLASS_A;
        }
        return new RefitBayWork(time, refitClass, baysWithoutCubiclesChanged, doorsMoved);
    }

    /**
     * @return the unit's cargo and transport bays, leaving out crew quarters
     */
    private static List<Bay> baysOtherThanQuarters(Entity entity) {
        List<Bay> bays = new ArrayList<>();
        for (Bay bay : entity.getTransportBays()) {
            if (!bay.isQuarters()) {
                bays.add(bay);
            }
        }
        return bays;
    }

    /**
     * Removes each old bay that has a new bay of the same type and size from both lists, pairing each bay at most once.
     *
     * @param oldBays        the old unit's bays still to be matched
     * @param newBays        the new design's bays still to be matched
     * @param mustMatchDoors {@code true} to pair only bays that also have the same number of doors
     *
     * @return the doors taken out of, and fitted to, the paired bays
     */
    private static DoorChanges removeMatchingBays(List<Bay> oldBays, List<Bay> newBays, boolean mustMatchDoors) {
        int doorsTakenOut = 0;
        int doorsFitted = 0;
        for (Iterator<Bay> oldBayIterator = oldBays.iterator(); oldBayIterator.hasNext(); ) {
            Bay oldBay = oldBayIterator.next();
            for (Iterator<Bay> newBayIterator = newBays.iterator(); newBayIterator.hasNext(); ) {
                Bay newBay = newBayIterator.next();
                boolean isSameBayType = BayType.getTypeForBay(oldBay) == BayType.getTypeForBay(newBay);
                boolean isSameSize = oldBay.getCapacity() == newBay.getCapacity();
                boolean isDoorCountAcceptable = !mustMatchDoors || (oldBay.getDoors() == newBay.getDoors());
                if (isSameBayType && isSameSize && isDoorCountAcceptable) {
                    doorsTakenOut += Math.max(0, oldBay.getDoors() - newBay.getDoors());
                    doorsFitted += Math.max(0, newBay.getDoors() - oldBay.getDoors());
                    oldBayIterator.remove();
                    newBayIterator.remove();
                    break;
                }
            }
        }
        return new DoorChanges(doorsTakenOut, doorsFitted);
    }

    /**
     * @return {@code true} if the bay holds no cubicles, so its refit work is not carried by cubicle parts
     */
    private static boolean hasNoCubicles(Bay bay) {
        return BayType.getTypeForBay(bay).getCategory() != BayType.CATEGORY_NON_INFANTRY;
    }

    /**
     * Doors taken out of, and fitted to, bays that a refit keeps.
     *
     * @param takenOut the doors taken out
     * @param fitted   the doors fitted
     */
    private record DoorChanges(int takenOut, int fitted) {}

    /**
     * The cubicles, by bay type, and the doors in a unit's cargo and transport bays, not counting crew quarters.
     *
     * @param cubicles the cubicles of each bay type
     * @param doors    the bay doors
     */
    private record BayComponents(Map<BayType, Integer> cubicles, int doors) {
        static BayComponents of(Entity entity) {
            Map<BayType, Integer> cubicles = new HashMap<>();
            int doors = 0;
            for (Bay bay : baysOtherThanQuarters(entity)) {
                doors += bay.getDoors();
                if (!hasNoCubicles(bay)) {
                    cubicles.merge(BayType.getTypeForBay(bay), (int) bay.getCapacity(), Integer::sum);
                }
            }
            return new BayComponents(cubicles, doors);
        }

        /**
         * @return {@code true} if this has more doors, or more cubicles of any bay type, than the other
         */
        boolean hasMoreThan(BayComponents other) {
            for (Map.Entry<BayType, Integer> cubiclesOfType : cubicles.entrySet()) {
                int otherCubicles = other.cubicles.getOrDefault(cubiclesOfType.getKey(), 0);
                if (cubiclesOfType.getValue() > otherCubicles) {
                    return true;
                }
            }
            return doors > other.doors;
        }
    }
}
