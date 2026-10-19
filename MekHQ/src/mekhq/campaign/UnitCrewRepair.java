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

package mekhq.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Repairs, as a campaign loads, unit crew lists that older versions left holding people who had left the unit.
 *
 * <p>A crew member could retire, or move to another unit, and still be listed on their old unit's crew. Their own
 * record was right, so they appeared unassigned or on their new unit everywhere else, but the stale entry could still
 * be picked for a role on the old unit, such as its engineer. Each unit drops the entries for people whose own record
 * does not place them on it. The people themselves stay in the campaign, with their assignment unchanged.</p>
 */
public final class UnitCrewRepair {
    private static final MMLogger LOGGER = MMLogger.create(UnitCrewRepair.class);

    private UnitCrewRepair() {}

    /**
     * Repairs the crew list of every unit in the campaign, in the main force's hangar and each base's. Must run after
     * every person's unit has been restored from the save, as that is what each entry is checked against.
     *
     * @param campaign the campaign being loaded
     */
    public static void repair(Campaign campaign) {
        List<Unit> units = new ArrayList<>(campaign.getPlayerForce().getHangar().getUnits());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            units.addAll(base.getBaseHangar().getUnits());
        }
        int repairedUnitCount = 0;
        for (Unit unit : units) {
            if (repair(unit)) {
                repairedUnitCount++;
            }
        }
        if (repairedUnitCount > 0) {
            LOGGER.info("[CrewRepair] Stale crew entries dropped from {} units", repairedUnitCount);
        }
    }

    /**
     * Repairs one unit's crew list.
     *
     * @param unit the unit
     *
     * @return {@code true} if any crew entry was dropped
     */
    static boolean repair(Unit unit) {
        Set<Person> departedCrew = unit.removeDepartedCrew();
        if (departedCrew.isEmpty()) {
            return false;
        }

        unit.resetPilotAndEntity();
        LOGGER.info("[CrewRepair] {}: dropped stale crew entries for people no longer aboard: {}", unit.getName(),
              departedCrew.stream().map(Person::getFullName).collect(Collectors.joining(", ")));
        return true;
    }
}
