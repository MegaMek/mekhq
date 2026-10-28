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

import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import mekhq.campaign.unit.crewAssignment.SeatType;

/**
 * The submenus of the right-click assignment menus, one per kind of seat as the player knows it, in the order they
 * appear. Each has its text key in the Personnel tab's "Assign to Unit" menu and in the Hangar tab's "Assign Person"
 * menu.
 *
 * @author Illiani
 * @since 0.51.01
 */
enum CrewSeatMenu {
    PILOT("asPilotMenu.text", "pilotMenu.text"),
    DRIVER("asDriverMenu.text", "driverMenu.text"),
    GUNNER("asGunnerMenu.text", "gunnerMenu.text"),
    CREWMEMBER("asCrewmemberMenu.text", "crewmemberMenu.text"),
    TECH_OFFICER("asTechOfficerMenu.text", "techOfficerMenu.text"),
    CONSOLE_COMMANDER("asConsoleCommanderMenu.text", "consoleCommanderMenu.text"),
    SOLDIER("asSoldierMenu.text", "soldierMenu.text"),
    NAVIGATOR("asNavigatorMenu.text", "navigatorMenu.text");

    private final String personToUnitKey;
    private final String unitToPersonKey;

    CrewSeatMenu(String personToUnitKey, String unitToPersonKey) {
        this.personToUnitKey = personToUnitKey;
        this.unitToPersonKey = unitToPersonKey;
    }

    /** @return the text key of this submenu in the Personnel tab's menu, such as "As Pilot" */
    String getPersonToUnitKey() {
        return personToUnitKey;
    }

    /** @return the text key of this submenu in the Hangar tab's menu, such as "Pilot" */
    String getUnitToPersonKey() {
        return unitToPersonKey;
    }

    /**
     * @param seat   the seat
     * @param entity the unit's entity
     *
     * @return the submenu the seat belongs in; a vehicle's driver is a driver, but a VTOL's, fighter's or vessel's is
     *       its pilot
     */
    static CrewSeatMenu of(SeatType seat, Entity entity) {
        return switch (seat) {
            case PILOT -> PILOT;
            case DRIVER -> ((entity instanceof Tank) && !(entity instanceof VTOL)) ? DRIVER : PILOT;
            case GUNNER -> GUNNER;
            case CREW -> CREWMEMBER;
            case TECH_OFFICER -> TECH_OFFICER;
            case CONSOLE_COMMANDER -> CONSOLE_COMMANDER;
            case SOLDIER -> SOLDIER;
            case NAVIGATOR -> NAVIGATOR;
        };
    }
}
