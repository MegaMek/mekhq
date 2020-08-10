/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
package mekhq.gui.enums;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public enum PersonnelFilter {
    //region Personnel Filters
    //region Standard Personnel Filters
    ALL("PersonnelFilter.ALL", true),
    ACTIVE("PersonnelFilter.ACTIVE", true),
    COMBAT("PersonnelFilter.COMBAT", true),
    SUPPORT("PersonnelFilter.SUPPORT", true),
    MECHWARRIOR("PersonnelFilter.MECHWARRIOR", true),
    VEHICLE_CREW("PersonnelFilter.VEHICLE_CREW", true),
    AEROSPACE_PILOT("PersonnelFilter.AEROSPACE_PILOT", true),
    CONVENTIONAL_AIRCRAFT_PILOT("PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT", true),
    PROTOMECH_PILOT("PersonnelFilter.PROTOMECH_PILOT", true),
    BATTLE_ARMOUR("PersonnelFilter.BATTLE_ARMOUR", true),
    SOLDIER("PersonnelFilter.SOLDIER", true),
    VESSEL_CREW("PersonnelFilter.VESSEL_CREW", true),
    TECH("PersonnelFilter.TECH", true),
    DOCTOR("PersonnelFilter.MEDICAL", true),
    ADMINISTRATOR("PersonnelFilter.ADMINISTRATOR", true),
    //endregion Standard Personnel Filters

    //region Expanded Personnel Tab Filters
    DEPENDENT("PersonnelFilter.DEPENDENT", false),
    FOUNDER("PersonnelFilter.FOUNDER", false),
    PRISONER("PersonnelFilter.PRISONER", false),
    INACTIVE("PersonnelFilter.INACTIVE", false),
    RETIRED("PersonnelFilter.RETIRED", false),
    MIA("PersonnelFilter.MIA", false),
    KIA("PersonnelFilter.KIA", false),
    DEAD("PersonnelFilter.DEAD", false);
    //endregion Expanded Personnel Tab Filters
    //endregion Personnel Filters

    private final String name;
    private final boolean standard;

    PersonnelFilter(String name, boolean standard) {
        this.name = ResourceBundle.getBundle("mekhq.resources.GUIEnums", new EncodeControl())
                .getString(name);
        this.standard = standard;
    }

    public boolean isStandard() {
        return standard;
    }

    public static List<PersonnelFilter> getStandardPersonnelFilters() {
        List<PersonnelFilter> standardFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isStandard()) {
                standardFilters.add(filter);
            }
        }
        return standardFilters;
    }

    public boolean getFilteredInformation(Person person) {
        switch (this) {
            case ALL:
                return true;
            case ACTIVE:
                return person.getStatus().isActive() && !person.getPrisonerStatus().isPrisoner();
            case COMBAT:
                return !person.getPrisonerStatus().isPrisoner() && person.hasCombatRole();
            case SUPPORT:
                return !person.getPrisonerStatus().isPrisoner() && person.hasSupportRole(true);
            case MECHWARRIOR:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRole(Person.T_MECHWARRIOR);
            case VEHICLE_CREW:
                return !person.getPrisonerStatus().isPrisoner()
                        && (person.hasRoleWithin(Person.T_GVEE_DRIVER, Person.T_VEE_GUNNER) || person.hasRole(Person.T_VEHICLE_CREW));
            case AEROSPACE_PILOT:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRole(Person.T_AERO_PILOT);
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRole(Person.T_CONV_PILOT);
            case PROTOMECH_PILOT:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRole(Person.T_PROTO_PILOT);
            case BATTLE_ARMOUR:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRole(Person.T_BA);
            case SOLDIER:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRole(Person.T_INFANTRY);
            case VESSEL_CREW:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRoleWithin(Person.T_SPACE_PILOT, Person.T_NAVIGATOR);
            case TECH:
                return !person.getPrisonerStatus().isPrisoner() && person.isTech();
            case DOCTOR:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRoleWithin(Person.T_DOCTOR, Person.T_MEDIC);
            case ADMINISTRATOR:
                return !person.getPrisonerStatus().isPrisoner() && person.hasRoleWithin(Person.T_ADMIN_COM, Person.T_ADMIN_HR);
            case DEPENDENT:
                return person.isDependent();
            case FOUNDER:
                return person.isFounder();
            case PRISONER:
                return person.getPrisonerStatus().isPrisoner();
            case INACTIVE:
                return !person.getStatus().isActive();
            case RETIRED:
                return person.getStatus().isRetired();
            case MIA:
                return person.getStatus().isMIA();
            case KIA:
                return person.getStatus().isKIA();
            case DEAD:
                return person.getStatus().isDead();
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
