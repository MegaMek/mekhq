/*
 * HirePersonnelUnitAction.java
 *
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit.actions;

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * Hires a full complement of personnel for a unit.
 */
public class HirePersonnelUnitAction implements IUnitAction {
    private final boolean isGM;

    /**
     * Initializes a new instance of the HirePersonnelUnitAction class.
     * @param isGM A boolean value indicating whether or not GM mode should be used
     *             to complete the action.
     */
    public HirePersonnelUnitAction(boolean isGM) {
        this.isGM = isGM;
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        while (unit.canTakeMoreDrivers()) {
            Person p = null;
            if (unit.getEntity() instanceof LandAirMech) {
                p = campaign.newPerson(PersonnelRole.LAM_PILOT);
            } else if (unit.getEntity() instanceof Mech) {
                p = campaign.newPerson(PersonnelRole.MECHWARRIOR);
            } else if (unit.getEntity() instanceof SmallCraft
                    || unit.getEntity() instanceof Jumpship) {
                p = campaign.newPerson(PersonnelRole.VESSEL_PILOT);
            } else if (unit.getEntity() instanceof ConvFighter) {
                p = campaign.newPerson(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
            } else if (unit.getEntity() instanceof Aero) {
                p = campaign.newPerson(PersonnelRole.AEROSPACE_PILOT);
            } else if (unit.getEntity() instanceof Tank) {
                switch (unit.getEntity().getMovementMode()) {
                    case VTOL:
                        p = campaign.newPerson(PersonnelRole.VTOL_PILOT);
                        break;
                    case NAVAL:
                    case HYDROFOIL:
                    case SUBMARINE:
                        p = campaign.newPerson(PersonnelRole.NAVAL_VEHICLE_DRIVER);
                        break;
                    default:
                        p = campaign.newPerson(PersonnelRole.GROUND_VEHICLE_DRIVER);
                }
            } else if (unit.getEntity() instanceof Protomech) {
                p = campaign.newPerson(PersonnelRole.PROTOMECH_PILOT);
            } else if (unit.getEntity() instanceof BattleArmor) {
                p = campaign.newPerson(PersonnelRole.BATTLE_ARMOUR);
            } else if (unit.getEntity() instanceof Infantry) {
                p = campaign.newPerson(PersonnelRole.SOLDIER);
            }
            if (p == null) {
                break;
            }

            if (!campaign.recruitPerson(p, isGM)) {
                return;
            }

            if (unit.usesSoloPilot() || unit.usesSoldiers()) {
                unit.addPilotOrSoldier(p);
            } else {
                unit.addDriver(p);
            }
        }

        while (unit.canTakeMoreGunners()) {
            Person p = null;
            if (unit.getEntity() instanceof Tank) {
                p = campaign.newPerson(PersonnelRole.VEHICLE_GUNNER);
            } else if (unit.getEntity() instanceof SmallCraft
                    || unit.getEntity() instanceof Jumpship) {
                p = campaign.newPerson(PersonnelRole.VESSEL_GUNNER);
            } else if (unit.getEntity() instanceof Mech) {
                p = campaign.newPerson(PersonnelRole.MECHWARRIOR);
            }
            if (p == null) {
                break;
            }
            if (!campaign.recruitPerson(p, isGM)) {
                return;
            }
            unit.addGunner(p);
        }

        while (unit.canTakeMoreVesselCrew()) {
            Person p = campaign.newPerson(unit.getEntity().isSupportVehicle()
                    ? PersonnelRole.VEHICLE_CREW : PersonnelRole.VESSEL_CREW);
            if (p == null) {
                break;
            }
            if (!campaign.recruitPerson(p, isGM)) {
                return;
            }
            unit.addVesselCrew(p);
        }

        if (unit.canTakeNavigator()) {
            Person p = campaign.newPerson(PersonnelRole.VESSEL_NAVIGATOR);
            if (!campaign.recruitPerson(p, isGM)) {
                return;
            }
            unit.setNavigator(p);
        }

        if (unit.canTakeTechOfficer()) {
            Person p;
            //For vehicle command console we will default to gunner
            if (unit.getEntity() instanceof Tank) {
                p = campaign.newPerson(PersonnelRole.VEHICLE_GUNNER);
            } else {
                p = campaign.newPerson(PersonnelRole.MECHWARRIOR);
            }
            if (!campaign.recruitPerson(p, isGM)) {
                return;
            }
            unit.setTechOfficer(p);
        }

        unit.resetPilotAndEntity();
        unit.runDiagnostic(false);
    }

}
