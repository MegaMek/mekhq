/*
 * Copyright (c) 2014-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.unit.Unit;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.stream.Stream;

public class StaticChecks {

    public static boolean areAllForcesUndeployed(final Campaign campaign, final List<Force> forces) {
        return forces.stream().noneMatch(Force::isDeployed)
                && forces.stream().flatMap(force -> force.getAllUnits(true).stream())
                        .map(campaign::getUnit).noneMatch(unit -> (unit != null) && unit.isDeployed());
    }

    public static boolean areAllCombatForces(Vector<Force> forces) {
        return forces.stream().allMatch(Force::isCombatForce);
    }

    public static boolean areAllUnitsAvailable(Vector<Unit> units) {
        return units.stream().allMatch(Unit::isAvailable);
    }

    public static boolean areAllForcesDeployed(Vector<Force> forces) {
        return forces.stream().allMatch(Force::isDeployed);
    }

    public static boolean areAnyForcesDeployed(Vector<Force> forces) {
        return forces.stream().anyMatch(Force::isDeployed);
    }

    public static boolean areAllUnitsDeployed(Vector<Unit> units) {
        return units.stream().allMatch(Unit::isDeployed);
    }

    public static boolean areAnyUnitsDeployed(Vector<Unit> units) {
        return units.stream().anyMatch(Unit::isDeployed);
    }

    /**
     * Used to test a selection of Units provided by the player and determine whether or not they have a
     * Transport ship assignment
     * @param units Vector of units that the player has selected
     * @return false if any unit in the passed-in Vector has not been assigned to a Transport ship
     */
    public static boolean areAllUnitsTransported(Vector<Unit> units) {
        return units.stream().allMatch(Unit::hasTransportShipAssignment);
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
        int numberDropShips = 0;
        // First test all units in the selection and find out how many of each we have
        for (Unit unit : units) {
            if (unit.getEntity().getUnitType() == UnitType.DROPSHIP) {
                numberDropShips++;
            } else if (unit.getEntity().isLargeCraft()) {
                // No. Try your selection again.
                return "    Selection of Units includes a large spacecraft. \n";
            } else if (unit.getEntity().getUnitType() == UnitType.SMALL_CRAFT) {
                numberSC++;
            } else if (unit.getEntity().getUnitType() == UnitType.AEROSPACEFIGHTER
                        || unit.getEntity().getUnitType() == UnitType.CONV_FIGHTER) {
                // Includes conventional fighters
                numberASF++;
            } else if (unit.getEntity().getUnitType() == UnitType.BATTLE_ARMOR) {
                numberBA++;
            } else if (unit.getEntity().getUnitType() == UnitType.INFANTRY) {
                // Make sure we account for space consumed by different platoon types
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

        if (numberDropShips > ship.getCurrentDocks()) {
            reason.add("    Selection of Units includes too many DropShips. \n");
            loadOK = false;
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

        return loadOK ? null : reason.toString();
    }

    //region C3
    public static boolean doAllUnitsHaveC3i(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3i());
    }

    public static boolean areAllUnitsNotC3iNetworked(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3i()
                && (u.getEntity().calculateFreeC3Nodes() < 5));
    }

    public static boolean areAllUnitsC3iNetworked(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3i()
                && (u.getEntity().calculateFreeC3Nodes() != 5));
    }

    public static boolean areAllUnitsOnSameC3iNetwork(Vector<Unit> units) {
        if (units.isEmpty() || (units.get(0).getEntity() == null)) {
            return false;
        }
        final String network = units.get(0).getEntity().getC3NetId();
        if (network == null) {
            return false;
        }
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3i()
                && network.equalsIgnoreCase(u.getEntity().getC3NetId()));
    }

    /**
     * Tests a selection of units to see if all of them have Naval C3 equipment
     * @param units A vector of units to test for Naval C3 equipment
     * @return false if any unit in the selection does not have a functioning NC3
     */
    public static boolean doAllUnitsHaveNC3(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && (u.getEntity().hasNavalC3()));
    }

    /**
     * Tests a selection of units to see if all of them have no Naval C3 network assigned
     * @param units A vector of units to test for Naval C3 network assignment
     * @return false if any unit in the selection does not have a functioning NC3 or is already on an NC3 network
     */
    public static boolean areAllUnitsNotNC3Networked(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasNavalC3()
                && (u.getEntity().calculateFreeC3Nodes() == 5));
    }

    /**
     * Tests a selection of units to see if all of them are on a Naval C3 network
     * @param units A vector of units to test for Naval C3 network assignment
     * @return false if any unit in the selection does not have a functioning NC3 or is not on an NC3 network with any other units
     */
    public static boolean areAllUnitsNC3Networked(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasNavalC3()
                && (u.getEntity().calculateFreeC3Nodes() != 5));
    }

    /**
     * Tests a selection of units to see if all of them are on the same Naval C3 network by ID
     * @param units A vector of units to test for Naval C3 network assignment
     * @return false if any unit in the selection does not have a functioning NC3, or is not on an NC3 network,
     *     or if any of the units is on a different NC3 network from the others.
     */
    public static boolean areAllUnitsOnSameNC3Network(Vector<Unit> units) {
        if (units.isEmpty() || (units.get(0).getEntity() == null)) {
            return false;
        }
        final String network = units.get(0).getEntity().getC3NetId();
        if (network == null) {
            return false;
        }
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasNavalC3()
                && network.equals(u.getEntity().getC3NetId()));
    }

    public static boolean areAllUnitsC3Slaves(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3S());
    }

    public static boolean areAllUnitsIndependentC3Masters(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3M()
                && !u.getEntity().C3MasterIs(u.getEntity()));
    }

    public static boolean areAllUnitsCompanyLevelMasters(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3M()
                && !u.getEntity().hasC3MM() && u.getEntity().C3MasterIs(u.getEntity()));
    }

    public static boolean doAllUnitsHaveC3Master(Vector<Unit> units) {
        return units.stream().allMatch(u -> (u.getEntity() != null) && u.getEntity().hasC3()
                && (u.getEntity().getC3Master() != null) && !u.getEntity().C3MasterIs(u.getEntity()));
    }
    //endregion C3

    /**
     * Used to test a selection of Units provided by the player and determine whether they all share a designated unitType.
     * @param units Vector of units that the player has selected
     * @return false if any unit in the passed-in Vector does not have the specified unit type
     */
    public static boolean areAllUnitsSameType(Vector<Unit> units, int unitType) {
        if (units.isEmpty() || (units.get(0).getEntity() == null)) {
            return false;
        }
        final boolean isTank = (unitType == UnitType.TANK) || (unitType == UnitType.VTOL)
                || (unitType == UnitType.NAVAL);
        final int weightClass = units.get(0).getEntity().getWeightClass();
        return units.stream().allMatch(u -> (u.getEntity() == null)
                || ((u.getEntity() != null) && (u.getEntity().getUnitType() == unitType)
                && (!isTank || (u.getEntity().getWeightClass() == weightClass))));
    }

    public static boolean areAllActive(Person... people) {
        return Stream.of(people).allMatch(p -> p.getStatus().isActive());
    }

    public static boolean areAllClanEligible(Person... people) {
        return Stream.of(people).allMatch(Person::isClanPersonnel) && areAllEligible(people);
    }

    public static boolean areAllEligible(Person... people) {
        return areAllEligible(false, people);
    }

    public static boolean areAllEligible(final boolean ignorePrisonerStatus, final Person... people) {
        final Profession profession = Profession.getProfessionFromPersonnelRole(people[0].getPrimaryRole());
        return Stream.of(people).allMatch(p -> (p.getPrisonerStatus().isFree() || ignorePrisonerStatus)
                && (profession == Profession.getProfessionFromPersonnelRole(p.getPrimaryRole()))
                && people[0].getRankSystem().equals(p.getRankSystem()));
    }

    /**
     * Checks if there is at least one award in the selected group of people
     * @param people the selected group of people
     * @return true if at least one has one award
     */
    public static boolean doAnyHaveAnAward(Person... people) {
        return Stream.of(people).anyMatch(p -> p.getAwardController().hasAwards());
    }

    public static boolean areAnyFree(Person... people) {
        return Stream.of(people).anyMatch(p -> p.getPrisonerStatus().isFree());
    }

    public static boolean areAllPrisoners(Person... people) {
        return Stream.of(people).allMatch(p -> p.getPrisonerStatus().isCurrentPrisoner());
    }

    public static boolean areAnyWillingToDefect(Person... people) {
        return Stream.of(people).anyMatch(p -> p.getPrisonerStatus().isPrisonerDefector());
    }

    public static boolean areAllSameSite(Unit... units) {
        return Stream.of(units).allMatch(u -> u.getSite() == units[0].getSite());
    }

    public static boolean allHaveSameUnit(Person... people) {
        return Stream.of(people).allMatch(p -> Objects.equals(people[0].getUnit(), p.getUnit()));
    }
}
