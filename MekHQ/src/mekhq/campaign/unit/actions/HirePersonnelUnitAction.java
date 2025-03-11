/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.unit.actions;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.unit.Unit;

import java.util.Set;

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
            if (unit.getEntity() instanceof LandAirMek) {
                p = campaign.newPerson(PersonnelRole.LAM_PILOT);
            } else if (unit.getEntity() instanceof Mek) {
                p = campaign.newPerson(PersonnelRole.MEKWARRIOR);
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
            } else if (unit.getEntity() instanceof ProtoMek) {
                p = campaign.newPerson(PersonnelRole.PROTOMEK_PILOT);
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
            } else if (unit.getEntity() instanceof Mek) {
                p = campaign.newPerson(PersonnelRole.MEKWARRIOR);
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
                p = campaign.newPerson(PersonnelRole.MEKWARRIOR);
            }
            if (!campaign.recruitPerson(p, isGM)) {
                return;
            }
            unit.setTechOfficer(p);
        }

        // Ensure we generate at least one person with the artillery skill if using that skill and
        // the unit has an artillery weapon
        if (campaign.getCampaignOptions().isUseArtillery() && (unit.getEntity() != null)
                && unit.getEntity().getWeaponList().stream()
                        .anyMatch(weapon -> (weapon.getType() instanceof WeaponType)
                                && (((WeaponType) weapon.getType()).getDamage() == WeaponType.DAMAGE_ARTILLERY))) {
            final Set<Person> gunners = unit.getGunners();
            if (!gunners.isEmpty() && gunners.stream().noneMatch(person -> person.getSkills().hasSkill(SkillType.S_ARTILLERY))) {
                new DefaultSkillGenerator(campaign.getRandomSkillPreferences()).generateArtillerySkill(ObjectUtility.getRandomItem(gunners));
            }
        }

        unit.resetPilotAndEntity();
        unit.runDiagnostic(false);
    }
}
