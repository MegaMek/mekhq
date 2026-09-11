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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;

/**
 * Seats the most skilled pilots in the leading lances (Assign Most Skilled to Primary Lances).
 *
 * <p>The roll gives every unit a pilot in no particular order of skill. This walks the TO&amp;E in order, the
 * command lance first and the first company next, and for each kind of single-pilot unit hands the seats out to
 * that kind's pilots best first, so the top of the TO&amp;E gets the best pilots. Pilots only swap between units
 * of the same kind, so a MekWarrior never ends up in a fighter. Crews of more than one stay where they are.</p>
 */
public final class PilotSkillSorter {

    private static final MMLogger LOGGER = MMLogger.create(PilotSkillSorter.class);
    private static final String LOG_TAG = "[CompanyGen][PilotSort]";
    /** How many seats a kind's log line names before it counts the rest. */
    private static final int SEATS_NAMED_IN_LOG = 12;

    private PilotSkillSorter() {
    }

    /**
     * Reseats the single-pilot units' pilots so the leading lances hold the most skilled.
     *
     * @param campaign the campaign whose TO&amp;E is read and whose units are reseated
     * @param options  the generation options; nothing happens unless Assign Most Skilled to Primary Lances is on
     *
     * @return how many pilots changed unit
     */
    public static int apply(Campaign campaign, CommandGenerationOptions options) {
        if (!options.isAssignMostSkilledToPrimaryLances()) {
            LOGGER.info("{} off; pilots stay where the roll put them", LOG_TAG);
            return 0;
        }
        Formation root = campaign.getPlayerForce().getFormations();
        if (root == null) {
            LOGGER.info("{} the campaign has no formations; nothing to sort", LOG_TAG);
            return 0;
        }

        Map<Integer, List<Unit>> soloUnitsByKind = new LinkedHashMap<>();
        for (UUID unitId : root.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (isSingleSeatWithPilot(unit)) {
                soloUnitsByKind.computeIfAbsent(unit.getEntity().getUnitType(), kind -> new ArrayList<>()).add(unit);
            }
        }

        Comparator<Person> mostSkilledFirst = OfficerSelector.mostSkilledFirst(campaign);
        int moved = 0;
        for (Map.Entry<Integer, List<Unit>> kind : soloUnitsByKind.entrySet()) {
            int movedOfKind = reseat(kind.getValue(), mostSkilledFirst);
            moved += movedOfKind;
            LOGGER.info("{} {}: {} pilot(s) moved; seats in TO&E order now {}", LOG_TAG,
                  UnitType.getTypeName(kind.getKey()), movedOfKind, seating(campaign, kind.getValue()));
        }
        LOGGER.info("{} {} pilot(s) moved so the leading lances hold the most skilled, across {} kind(s) of unit",
              LOG_TAG, moved, soloUnitsByKind.size());
        return moved;
    }

    /**
     * Hands the units' seats to their pilots best first.
     *
     * @return how many pilots ended up in a different unit
     */
    private static int reseat(List<Unit> units, Comparator<Person> mostSkilledFirst) {
        List<Person> pilots = new ArrayList<>();
        for (Unit unit : units) {
            pilots.add(unit.getCommander());
        }
        List<Person> sorted = new ArrayList<>(pilots);
        sorted.sort(mostSkilledFirst);

        int moved = 0;
        for (int seat = 0; seat < units.size(); seat++) {
            if (sorted.get(seat) != pilots.get(seat)) {
                moved++;
            }
        }
        if (moved == 0) {
            return 0;
        }
        for (int seat = 0; seat < units.size(); seat++) {
            units.get(seat).remove(pilots.get(seat), false);
        }
        for (int seat = 0; seat < units.size(); seat++) {
            units.get(seat).addPilotOrSoldier(sorted.get(seat));
        }
        return moved;
    }

    /** The pilots now in the units, in TO&E order, with what the sort saw in each; long lists are cut short. */
    private static String seating(Campaign campaign, List<Unit> units) {
        List<String> seats = new ArrayList<>();
        int named = Math.min(units.size(), SEATS_NAMED_IN_LOG);
        for (int seat = 0; seat < named; seat++) {
            seats.add(OfficerSelector.describe(campaign, units.get(seat).getCommander()));
        }
        String tail = (units.size() > named) ? " and " + (units.size() - named) + " more" : "";
        return String.join(", ", seats) + tail;
    }

    private static boolean isSingleSeatWithPilot(Unit unit) {
        if (unit == null) {
            return false;
        }
        Entity entity = unit.getEntity();
        boolean hasEntity = entity != null;
        boolean isSingleSeat = hasEntity && unit.usesSoloPilot();
        boolean hasPilot = isSingleSeat && (unit.getCommander() != null);
        return hasPilot;
    }
}
