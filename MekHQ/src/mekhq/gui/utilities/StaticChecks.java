package mekhq.gui.utilities;

import java.util.StringJoiner;
import java.util.UUID;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Ranks;
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
            if (!unit.hasTransportShipId()) {
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
     * @returns a String  indicating why the Transport cannot carry all of the selected units, or a blank result if it can
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
            } else if (unit.getEntity() instanceof SmallCraft) {
                numberSC++;
            } else if (unit.getEntity() instanceof Aero) {
                // Includes conventional fighters
                numberASF++;
            } else if (unit.getEntity() instanceof BattleArmor) {
                numberBA++;
            } else if (unit.getEntity() instanceof Infantry) {
                numberInfantry++;
            } else if (unit.getEntity() instanceof Mech) {
                // Includes LAMs and Quadvees
                numberMech++;
            } else if (unit.getEntity() instanceof Protomech) {
                numberProto++;
            } else if (unit.getEntity() instanceof Tank) {
                // Tanks and VTOLs
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
        if (numberSC > ship.getSmallCraftCapacity()) {
            reason.add("    Selection of Units includes too many small craft. \n");
            loadOK = false;
        }
        // Fighters can fit into any unused SC bays
        if (numberASF > (ship.getASFCapacity() + (ship.getSmallCraftCapacity() - numberSC))) {
            reason.add("    Selection of Units includes too many fighters. \n");
            loadOK = false;
        }
        if (numberBA > ship.getBattleArmorCapacity()) {
            reason.add("    Selection of Units includes too many Battle Armor units. \n");
            loadOK = false;
        }
        if (numberInfantry > ship.getInfantryCapacity()) {
            reason.add("    Selection of Units includes too many Infantry units. \n");
            loadOK = false;
        }
        if (numberMech > ship.getMechCapacity()) {
            reason.add("    Selection of Units includes too many Mechs. \n");
            loadOK = false;
        }
        if (numberProto > ship.getProtomechCapacity()) {
            reason.add("    Selection of Units includes too many ProtoMechs. \n");
            loadOK = false;
        }
        if (numberSHVee > ship.getSuperHeavyVehicleCapacity()) {
            reason.add("    Selection of Units includes too many SuperHeavy Vehicles. \n");
            loadOK = false;
        }
        // Heavy vehicles can fit into unused SuperHeavy bays
        if (numberHVee > (ship.getHeavyVehicleCapacity() + (ship.getSuperHeavyVehicleCapacity() - numberSHVee))) {
            reason.add("    Selection of Units includes too many Heavy Vehicles. \n");
            loadOK = false;
        }
        // Light vehicles can fit into any unused vehicle bays
        if (numberLVee > 
            (ship.getLightVehicleCapacity() + 
                    (ship.getSuperHeavyVehicleCapacity() - numberSHVee) + 
                    (ship.getHeavyVehicleCapacity() - numberHVee))) {
            reason.add("    Selection of Units includes too many Light Vehicles. \n");
            loadOK = false;
        }
        if (!loadOK) {
            return reason.toString();
        }        
        // Everything's ok to load. Return a blank string.
        return null;
    }

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
            if (Person.T_SPACE_CREW != person.getPrimaryRole()) {
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
            if (!person.isActive()) {
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
        int profession = people[0].getProfession();
        for (Person person : people) {
            if (person.isPrisoner() || person.isBondsman()
                    || person.getProfession() != profession) {
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
    
    public static boolean areAllPrisoners(Person[] people) {
        for(Person person : people) {
            if(!person.isPrisoner()) {
                return false;
            }
        }
        
        return true;
    }

    public static boolean isWillingToDefect(Person person) {
        if (!(person.isBondsman() || person.isWillingToDefect())) {
            return false;
        }

        return true;
    }

    public static boolean areAnyWillingToDefect(Person[] people) {
        for (Person person : people) {
            if (isWillingToDefect((person))) {
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
        UUID unitId = people[0].getUnitId();
        for (Person person : people) {
            if ((unitId == null && person.getUnitId() == null)
                    || (person.getUnitId() != null && person.getUnitId()
                            .equals(unitId))) {
                continue;
            }
            return false;
        }
        return true;
    }
}
