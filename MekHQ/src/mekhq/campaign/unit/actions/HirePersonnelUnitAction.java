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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.unit.actions;

import java.util.Set;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * Hires a full complement of personnel for a unit.
 */
public record HirePersonnelUnitAction(boolean isGM) implements IUnitAction {
    /**
     * Initializes a new instance of the HirePersonnelUnitAction class.
     *
     * @param isGM A boolean value indicating whether GM mode should be used to complete the action.
     */
    public HirePersonnelUnitAction {
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        while (unit.canTakeMoreDrivers()) {
            Person person = null;
            if (unit.getEntity() instanceof LandAirMek) {
                person = campaign.newPerson(PersonnelRole.LAM_PILOT);
            } else if (unit.getEntity() instanceof Mek) {
                person = campaign.newPerson(PersonnelRole.MEKWARRIOR);
            } else if (unit.getEntity() instanceof SmallCraft
                             || unit.getEntity() instanceof Jumpship) {
                person = campaign.newPerson(PersonnelRole.VESSEL_PILOT);
            } else if (unit.getEntity() instanceof ConvFighter) {
                person = campaign.newPerson(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
            } else if (unit.getEntity() instanceof Aero) {
                person = campaign.newPerson(PersonnelRole.AEROSPACE_PILOT);
            } else if (unit.getEntity() instanceof Tank) {
                person = switch (unit.getEntity().getMovementMode()) {
                    case VTOL -> campaign.newPerson(PersonnelRole.VTOL_PILOT);
                    case NAVAL, HYDROFOIL, SUBMARINE -> campaign.newPerson(PersonnelRole.NAVAL_VEHICLE_DRIVER);
                    default -> campaign.newPerson(PersonnelRole.GROUND_VEHICLE_DRIVER);
                };
            } else if (unit.getEntity() instanceof ProtoMek) {
                person = campaign.newPerson(PersonnelRole.PROTOMEK_PILOT);
            } else if (unit.getEntity() instanceof BattleArmor) {
                person = campaign.newPerson(PersonnelRole.BATTLE_ARMOUR);
            } else if (unit.getEntity() instanceof Infantry) {
                person = campaign.newPerson(PersonnelRole.SOLDIER);
            }
            if (person == null) {
                break;
            }

            if (!campaign.recruitPerson(person, isGM, true)) {
                return;
            }

            if (unit.usesSoloPilot() || unit.usesSoldiers()) {
                unit.addPilotOrSoldier(person);
            } else {
                unit.addDriver(person);
            }
        }

        while (unit.canTakeMoreGunners()) {
            Person person = null;
            if (unit.getEntity() instanceof Tank) {
                person = campaign.newPerson(PersonnelRole.VEHICLE_GUNNER);
            } else if (unit.getEntity() instanceof SmallCraft
                             || unit.getEntity() instanceof Jumpship) {
                person = campaign.newPerson(PersonnelRole.VESSEL_GUNNER);
            } else if (unit.getEntity() instanceof Mek) {
                person = campaign.newPerson(PersonnelRole.MEKWARRIOR);
            } else if (unit.getEntity() instanceof ConvFighter) {
                person = campaign.newPerson(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
            }

            if (person == null) {
                break;
            }
            if (!campaign.recruitPerson(person, isGM, true)) {
                return;
            }
            unit.addGunner(person);
        }

        while (unit.canTakeMoreVesselCrew()) {
            Person person = campaign.newPerson(unit.getEntity().isLargeCraft()
                                                     ? PersonnelRole.VESSEL_CREW : PersonnelRole.VEHICLE_CREW);
            if (person == null) {
                break;
            }
            if (!campaign.recruitPerson(person, isGM, true)) {
                return;
            }
            unit.addVesselCrew(person);
        }

        if (unit.canTakeNavigator()) {
            Person person = campaign.newPerson(PersonnelRole.VESSEL_NAVIGATOR);
            if (!campaign.recruitPerson(person, isGM, true)) {
                return;
            }
            unit.setNavigator(person);
        }

        if (unit.canTakeTechOfficer()) {
            Person person;
            //For vehicle command console we will default to gunner
            if (unit.getEntity() instanceof Tank) {
                person = campaign.newPerson(PersonnelRole.VEHICLE_GUNNER);
            } else {
                person = campaign.newPerson(PersonnelRole.MEKWARRIOR);
            }
            if (!campaign.recruitPerson(person, isGM, true)) {
                return;
            }
            unit.setTechOfficer(person);
        }

        // Ensure we generate at least one person with the artillery skill if using that skill and
        // the unit has an artillery weapon
        if (campaign.getCampaignOptions().isUseArtillery() && (unit.getEntity() != null)
                  && unit.getEntity().getWeaponList().stream()
                           .anyMatch(weapon -> (weapon.getType() != null)
                                                     &&
                                                     (weapon.getType().getDamage() == WeaponType.DAMAGE_ARTILLERY))) {
            final Set<Person> gunners = unit.getGunners();
            if (!gunners.isEmpty() &&
                      gunners.stream().noneMatch(person -> person.getSkills().hasSkill(SkillType.S_ARTILLERY))) {
                new DefaultSkillGenerator(campaign.getRandomSkillPreferences()).generateArtillerySkill(ObjectUtility.getRandomItem(
                      gunners));
            }
        }

        unit.resetPilotAndEntity();
        unit.runDiagnostic(false);
    }
}
