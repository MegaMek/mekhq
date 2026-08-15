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
package mekhq.campaign.log;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;

import megamek.common.annotations.Nullable;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitAcquisitionType;

/**
 * This class is responsible for controlling the logging of Unit Log Entries.
 *
 * <p>It mirrors the personnel-oriented loggers (such as {@link ServiceLogger} and {@link AssignmentLogger}), but
 * records events against a {@link Unit} rather than a {@code Person}. A unit maintains several distinct logs:</p>
 * <ul>
 *     <li><b>Unit log</b> - ownership events: salvage, purchase, and refits.</li>
 *     <li><b>Kill log</b> - kills scored by the unit through its pilot.</li>
 *     <li><b>Crew log</b> - crew members assigned to the unit.</li>
 *     <li><b>Deployment log</b> - deployments to scenarios.</li>
 *     <li><b>Repair log</b> - repairs conducted on the unit.</li>
 * </ul>
 */
public class UnitLogger {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LogEntries";

    /**
     * Records that the unit was salvaged from a former owner. Added to the unit log.
     *
     * @param unit        the unit being logged
     * @param date        the date the unit was salvaged
     * @param formerOwner the name of the force or faction the unit was salvaged from
     */
    public static void salvagedFrom(Unit unit, LocalDate date, String formerOwner) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitSalvagedFrom.text", formerOwner);
        unit.addUnitLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records how the unit entered the campaign. Added to the unit log.
     *
     * <p>This is called for every unit added to the campaign, so that each unit's history opens with the form and
     * date of its acquisition.</p>
     *
     * @param unit            the unit being logged
     * @param date            the date the unit was acquired
     * @param acquisitionType the means by which the unit was acquired
     */
    public static void acquired(Unit unit, LocalDate date, UnitAcquisitionType acquisitionType) {
        unit.addUnitLogEntry(new UnitLogEntry(date, getTextAt(RESOURCE_BUNDLE, acquisitionType.getResourceKey())));
    }

    /**
     * Records that the unit was refit from one model to another. Added to the unit log.
     *
     * @param unit     the unit being logged
     * @param date     the date the refit completed
     * @param oldModel the model the unit was refit from
     * @param newModel the model the unit was refit into
     */
    public static void refit(Unit unit, LocalDate date, String oldModel, String newModel) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitRefit.text", oldModel, newModel);
        unit.addUnitLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records a kill scored by the unit. Added to the kill log.
     *
     * <p>Exactly one entry is recorded per kill, regardless of how many crew members the unit has. Where the unit has
     * a commander at the time of the kill they are named, but a unit crewed only by temporary crew still records the
     * kill.</p>
     *
     * @param unit          the unit being logged
     * @param date          the date of the kill
     * @param killed        the name of what was killed
     * @param commanderName the name of the unit's commander, or {@code null} if the unit has no named commander
     */
    public static void scoredKill(Unit unit, LocalDate date, String killed, @Nullable String commanderName) {
        String message = (commanderName == null) ?
                               getFormattedTextAt(RESOURCE_BUNDLE, "unitScoredKill.text", killed) :
                               getFormattedTextAt(RESOURCE_BUNDLE,
                                     "unitScoredKillCommandedBy.text",
                                     killed,
                                     commanderName);
        unit.addKillLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records that a crew member was assigned to the unit. Added to the crew log.
     *
     * @param unit       the unit being logged
     * @param date       the date the crew member was assigned
     * @param personName the name of the crew member
     */
    public static void assignedCrew(Unit unit, LocalDate date, String personName) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitAssignedCrew.text", personName);
        unit.addCrewLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records that a crew member left the unit. Added to the crew log.
     *
     * @param unit       the unit being logged
     * @param date       the date the crew member was removed
     * @param personName the name of the crew member
     */
    public static void removedCrew(Unit unit, LocalDate date, String personName) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitRemovedCrew.text", personName);
        unit.addCrewLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records that a technician took over maintenance of the unit. Added to the crew log.
     *
     * <p>Technicians are not crew, so they are recorded under their own message rather than as an assignment to a
     * crew position.</p>
     *
     * @param unit       the unit being logged
     * @param date       the date the technician was assigned
     * @param personName the name of the technician
     */
    public static void assignedTech(Unit unit, LocalDate date, String personName) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitAssignedTech.text", personName);
        unit.addCrewLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records that a technician stopped maintaining the unit. Added to the crew log.
     *
     * @param unit       the unit being logged
     * @param date       the date the technician was removed
     * @param personName the name of the technician
     */
    public static void removedTech(Unit unit, LocalDate date, String personName) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitRemovedTech.text", personName);
        unit.addCrewLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records that the unit was deployed to a scenario. Added to the deployment log.
     *
     * @param unit         the unit being logged
     * @param date         the date the unit was deployed
     * @param scenarioName the name of the scenario the unit was deployed to
     */
    public static void deployed(Unit unit, LocalDate date, String scenarioName) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitDeployed.text", scenarioName);
        unit.addDeploymentLogEntry(new UnitLogEntry(date, message));
    }

    /**
     * Records a repair conducted on the unit. Added to the repair log.
     *
     * @param unit     the unit being logged
     * @param date     the date the repair was conducted
     * @param partName the name of the part that was repaired
     * @param techName the name of the tech who conducted the repair
     */
    public static void repaired(Unit unit, LocalDate date, String partName, String techName) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "unitRepaired.text", partName, techName);
        unit.addRepairLogEntry(new UnitLogEntry(date, message));
    }
}
