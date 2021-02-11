/*
 * Copyright (c) 2014 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.Vector;

import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;

public class StaticChecks {

    public static boolean areAllForcesUndeployed(Vector<Force> forces) {
        for (Force force : forces) {
            if (force.isDeployed()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllCombatForces(Vector<Force> forces) {
        for (Force force : forces) {
            if (!force.isCombatForce()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsAvailable(Vector<Unit> units) {
        for (Unit unit : units) {
            if (!unit.isAvailable()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllForcesDeployed(Vector<Force> forces) {
        for (Force force : forces) {
            if (!force.isDeployed()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAnyForcesDeployed(Vector<Force> forces) {
        for (Force force : forces) {
            if (force.isDeployed()) {
                return true;
            }
        }
        return false;
    }

    public static boolean areAllUnitsDeployed(Vector<Unit> units) {
        for (Unit unit : units) {
            if (!unit.isDeployed()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAnyUnitsDeployed(Vector<Unit> units) {
        for (Unit unit : units) {
            if (unit.isDeployed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Used to test a selection of Units provided by the player and determine whether or not they have a
     * Transport ship assignment
     * @param units Vector of units that the player has selected
     * @return false if any unit in the passed-in Vector has not been assigned to a Transport ship
     */
    public static boolean areAllUnitsTransported(Vector<Unit> units) {
        for (Unit unit : units) {
            if (!unit.hasTransportShipAssignment()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Used to test a selection of Units provided by the player and a larger Transport to determine
     * whether or not the Transport can carry all of the selected units
     * @param units Vector of units that the player has selected
     * @param ship A single Transport-Bay-equipped Unit whose capacity we want to test the selection against
     * @return a String  indicating why the Transport cannot carry all of the selected units, or a blank result if it can
     */
    public static String canTransportShipCarry(Vector<Unit> units, Unit ship) {
        StringJoiner reason = new StringJoiner("");
        boolean loadOK = true;
        int numberASF = 0;
        int numberBA = 0;
        int numberHVee = 0;
        int numberInfantry = 0;
        int numberLVee = 0;
        int numberMech = 0;
        int numberProto = 0;
        int numberSC = 0;
        int numberSHVee = 0;
        //First test all units in the selection and find out how many of each we have
        for (Unit unit : units) {
            if (unit.getEntity().isLargeCraft()) {
                //No. Try your selection again.
                return "    Selection of Units includes a large spacecraft. \n";
            } else if (unit.getEntity().getUnitType() == UnitType.SMALL_CRAFT) {
                numberSC++;
            } else if (unit.getEntity().getUnitType() == UnitType.AERO
                        || unit.getEntity().getUnitType() == UnitType.CONV_FIGHTER) {
                // Includes conventional fighters
                numberASF++;
            } else if (unit.getEntity().getUnitType() == UnitType.BATTLE_ARMOR) {
                numberBA++;
            } else if (unit.getEntity().getUnitType() == UnitType.INFANTRY) {
                //Make sure we account for space consumed by different platoon types
                numberInfantry += (int) Math.ceil(unit.getEntity().getWeight());
            } else if (unit.getEntity().getUnitType() == UnitType.MEK) {
                // Includes LAMs and Quadvees
                numberMech++;
            } else if (unit.getEntity().getUnitType() == UnitType.PROTOMEK) {
                numberProto++;
            } else if (unit.getEntity().getUnitType() == UnitType.TANK
                        || unit.getEntity().getUnitType() == UnitType.VTOL
                        || unit.getEntity().getUnitType() == UnitType.NAVAL) {
                // Tanks, VTOLs and wet naval vessels
                double weight = unit.getEntity().getWeight();
                if (unit.getEntity().isSuperHeavy()) {
                    numberSHVee++;
                } else if (weight >= 51) {
                    numberHVee++;
                } else {
                    numberLVee++;
                }
            }
        }
        // Now test the designated ship and let us know if it can carry everyone
        if (numberSC > ship.getCurrentSmallCraftCapacity()) {
            reason.add("    Selection of Units includes too many small craft. \n");
            loadOK = false;
        }
        // Fighters can fit into any unused SC bays
        if (numberASF > (ship.getCurrentASFCapacity() + (ship.getCurrentSmallCraftCapacity() - numberSC))) {
            reason.add("    Selection of Units includes too many fighters. \n");
            loadOK = false;
        }
        if (numberBA > ship.getCurrentBattleArmorCapacity()) {
            reason.add("    Selection of Units includes too many Battle Armor units. \n");
            loadOK = false;
        }
        if (numberInfantry > ship.getCurrentInfantryCapacity()) {
            reason.add("    Selection of Units includes too many Infantry units. \n");
            loadOK = false;
        }
        if (numberMech > ship.getCurrentMechCapacity()) {
            reason.add("    Selection of Units includes too many Mechs. \n");
            loadOK = false;
        }
        if (numberProto > ship.getCurrentProtomechCapacity()) {
            reason.add("    Selection of Units includes too many ProtoMechs. \n");
            loadOK = false;
        }
        if (numberSHVee > ship.getCurrentSuperHeavyVehicleCapacity()) {
            reason.add("    Selection of Units includes too many SuperHeavy Vehicles. \n");
            loadOK = false;
        }
        // Heavy vehicles can fit into unused SuperHeavy bays
        if (numberHVee > (ship.getCurrentHeavyVehicleCapacity() + (ship.getCurrentSuperHeavyVehicleCapacity() - numberSHVee))) {
            reason.add("    Selection of Units includes too many Heavy Vehicles. \n");
            loadOK = false;
        }
        // Light vehicles can fit into any unused vehicle bays
        if (numberLVee >
            (ship.getCurrentLightVehicleCapacity() +
                    (ship.getCurrentSuperHeavyVehicleCapacity() - numberSHVee) +
                    (ship.getCurrentHeavyVehicleCapacity() - numberHVee))) {
            reason.add("    Selection of Units includes too many Light Vehicles. \n");
            loadOK = false;
        }
        if (!loadOK) {
            return reason.toString();
        }
        // Everything's ok to load. Return a blank string.
        return null;
    }

    //region C3
    public static boolean doAllUnitsHaveC3i(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasC3i()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsNotC3iNetworked(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (e.hasC3i() && e.calculateFreeC3Nodes() < 5) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsC3iNetworked(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasC3i() && !e.hasC3()) {
                return false;
            }
            if (e.hasC3i() && e.calculateFreeC3Nodes() == 5) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsOnSameC3iNetwork(Vector<Unit> units) {
        String network = null;
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (null == e.getC3NetId()) {
                return false;
            }
            if (null == network) {
                network = e.getC3NetId();
            } else if (!e.getC3NetId().equals(network)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a selection of units to see if all of them have Naval C3 equipment
     * @param units A vector of units to test for Naval C3 equipment
     * @return false if any unit in the selection does not have a functioning NC3
     */
    public static boolean doAllUnitsHaveNC3(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasNavalC3()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a selection of units to see if all of them have no Naval C3 network assigned
     * @param units A vector of units to test for Naval C3 network assignment
     * @return false if any unit in the selection does not have a functioning NC3 or is already on an NC3 network
     */
    public static boolean areAllUnitsNotNC3Networked(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (e.hasNavalC3() && e.calculateFreeC3Nodes() < 5) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a selection of units to see if all of them are on a Naval C3 network
     * @param units A vector of units to test for Naval C3 network assignment
     * @return false if any unit in the selection does not have a functioning NC3 or is not on an NC3 network with any other units
     */
    public static boolean areAllUnitsNC3Networked(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasNavalC3()) {
                return false;
            }
            if (e.hasNavalC3() && e.calculateFreeC3Nodes() == 5) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a selection of units to see if all of them are on the same Naval C3 network by ID
     * @param units A vector of units to test for Naval C3 network assignment
     * @return false if any unit in the selection does not have a functioning NC3, or is not on an NC3 network,
     *     or if any of the units is on a different NC3 network from the others.
     */
    public static boolean areAllUnitsOnSameNC3Network(Vector<Unit> units) {
        String network = null;
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            //Naval C3 recycles a lot of C3i code. C3i units will cause a false positive
            //without this check
            if (!e.hasNavalC3()) {
                return false;
            }
            if (null == e.getC3NetId()) {
                return false;
            }
            if (null == network) {
                network = e.getC3NetId();
            } else if (!e.getC3NetId().equals(network)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsC3Slaves(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasC3S()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsIndependentC3Masters(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasC3M()) {
                return false;
            }
            /* Units with multiple masters need to be set to company command before other masters
             * can connect to them.
             */
//            if (e.hasC3MM()) {
//                return false;
//            }
            if (e.C3MasterIs(unit.getEntity())) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllUnitsCompanyLevelMasters(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasC3M()) {
                return false;
            }
            if (e.hasC3MM()) {
                return false;
            }
            if (!e.C3MasterIs(unit.getEntity())) {
                return false;
            }
        }
        return true;

    }

    public static boolean doAllUnitsHaveC3Master(Vector<Unit> units) {
        for (Unit unit : units) {
            Entity e = unit.getEntity();
            if (null == e) {
                return false;
            }
            if (!e.hasC3()) {
                return false;
            }
            if (null == e.getC3Master() || e.C3MasterIs(unit.getEntity())) {
                return false;
            }
        }
        return true;

    }
    //endregion C3

    /**
     * Used to test a selection of Units provided by the player and determine whether they all share a designated unitType.
     * @param units Vector of units that the player has selected
     * @return false if any unit in the passed-in Vector does not have the specified unit type
     */
    public static boolean areAllUnitsSameType(Vector<Unit> units, int unitType) {
        if (units.isEmpty()) {
            return false;
        }
        // For our purposes, a selection of tanks isn't the same unless they're all the same weight class
        // Set this based on the first unit in the selection
        double firstUnitWeight = units.get(0).getEntity() != null ? units.get(0).getEntity().getWeight() : 0;
        boolean light = firstUnitWeight > 0 && firstUnitWeight <= 50;
        boolean heavy = firstUnitWeight > 50 && firstUnitWeight <= 100;
        boolean superheavy = firstUnitWeight > 100 && firstUnitWeight <= 200;
        for (Unit unit : units) {
            if (unit.getEntity() == null) {
                //No bueno.
                continue;
            }
            if (unitType == UnitType.TANK || unitType == UnitType.VTOL || unitType == UnitType.NAVAL) {
                if (light && unit.getEntity().getWeight() > 50) {
                    return false;
                }
                if (heavy && (unit.getEntity().getWeight() <= 50 || unit.getEntity().getWeight() > 100)) {
                    return false;
                }
                if (superheavy && (unit.getEntity().getWeight() <= 100 || unit.getEntity().getWeight() > 200)) {
                    return false;
                }

            }
            if (unit.getEntity().getUnitType() != unitType) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllInfantry(Person[] people) {
        for (Person person : people) {
            if (Person.T_INFANTRY != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllBattleArmor(Person[] people) {
        for (Person person : people) {
            if (Person.T_BA != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllVeeGunners(Person[] people) {
        for (Person person : people) {
            if (Person.T_VEE_GUNNER != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllVesselGunners(Person[] people) {
        for (Person person : people) {
            if (Person.T_SPACE_GUNNER != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllVesselCrew(Person[] people) {
        for (Person person : people) {
            if (Person.T_SPACE_CREW != person.getPrimaryRole()
                    && Person.T_VEHICLE_CREW != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllVesselPilots(Person[] people) {
        for (Person person : people) {
            if (Person.T_SPACE_PILOT != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllVesselNavigators(Person[] people) {
        for (Person person : people) {
            if (Person.T_NAVIGATOR != person.getPrimaryRole()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllActive(Person[] people) {
        for (Person person : people) {
            if (!person.getStatus().isActive()) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllClanEligible(Person[] people) {
        for (Person p : people) {
            if (!p.isClanner()) {
                return false;
            }
        }
        return areAllEligible(people);
    }

    public static boolean areAllEligible(Person[] people) {
        return areAllEligible(people, false);
    }

    public static boolean areAllEligible(Person[] people, boolean ignorePrisonerStatus) {
        int profession = people[0].getProfession();
        for (Person person : people) {
            if (!(person.getPrisonerStatus().isFree() || ignorePrisonerStatus)
                    || (person.getProfession() != profession)) {
                return false;
            }
        }
        int system = people[0].getRankSystem();
        for (Person person : people) {
            if (person.getRankSystem() != system) {
                return false;
            }
        }
        return true;
    }
    /**
     * Checks if there is at least one award in the selected group of people
     * @param people the selected group of people
     * @return true if at least one has one award
     */
    public static boolean doAnyHaveAnAward(Person[] people) {
        for (Person person : people) {
            if (person.getAwardController().hasAwards()) {
                return true;
            }
        }

        return false;
    }

    public static boolean areAnyFree(Person[] people) {
        for (Person person : people) {
            if (person.getPrisonerStatus().isFree()) {
                return true;
            }
        }

        return false;
    }

    public static boolean areAllPrisoners(Person[] people) {
        for (Person person : people) {
            if (!person.getPrisonerStatus().isPrisoner()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param people an array of people
     * @return true if they are either all dependents or all not dependents, otherwise false
     */
    public static boolean areEitherAllDependentsOrNot(Person[] people) {
        if (people.length > 0) {
            boolean isDependent = people[0].isDependent();
            for (Person person : people) {
                if (isDependent != person.isDependent()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param people an array of people
     * @return true if all of the people are female, otherwise false
     */
    public static boolean areAllFemale(Person[] people) {
        for (Person person : people) {
            if (person.getGender().isMale()) {
               return false;
            }
        }
        return true;
    }

    /**
     * @param people an array of people
     * @return true if they are either all trying to conceive or all not, otherwise false
     */
    public static boolean areEitherAllTryingToConceiveOrNot(Person[] people) {
        if (people.length > 0) {
            boolean tryingToConceive = people[0].isTryingToConceive();
            for (Person person : people) {
                if (tryingToConceive != person.isTryingToConceive()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param people an array of people
     * @return true if they are either all trying to marry or all not, otherwise false
     */
    public static boolean areEitherAllTryingToMarryOrNot(Person[] people) {
        if (people.length > 0) {
            boolean tryingToMarry = people[0].isTryingToMarry();
            for (Person person : people) {
                if (tryingToMarry != person.isTryingToMarry()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param people an array of people
     * @return true if they are either all founders or all not, otherwise false
     */
    public static boolean areEitherAllFoundersOrNot(Person[] people) {
        if (people.length > 0) {
            boolean founder = people[0].isFounder();
            for (Person person : people) {
                if (founder != person.isFounder()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean areAnyWillingToDefect(Person[] people) {
        for (Person person : people) {
            if (person.getPrisonerStatus().isWillingToDefect()) {
                return true;
            }
        }

        return false;
    }

    public static boolean areAllWoB(Person[] people) {
        for (Person p : people) {
            if (p.getRankSystem() != Ranks.RS_WOB)
                return false;
        }
        return true;
    }

    public static boolean areAllWoBOrComstar(Person[] people) {
        for (Person p : people) {
            if (p.getRankSystem() != Ranks.RS_WOB
                    && p.getRankSystem() != Ranks.RS_COM)
                return false;
        }
        return true;
    }

    public static boolean areAllSameSite(Unit[] units) {
        int site = units[0].getSite();
        for (Unit unit : units) {
            if (unit.getSite() != site) {
                return false;
            }
        }
        return true;
    }

    public static boolean allHaveSameUnit(Person[] people) {
        if ((people == null) || (people.length == 0)) {
            return false;
        }

        Unit unit = people[0].getUnit();
        for (Person person : people) {
            if (!Objects.equals(unit, person.getUnit())) {
                return false;
            }
        }
        return true;
    }
}
