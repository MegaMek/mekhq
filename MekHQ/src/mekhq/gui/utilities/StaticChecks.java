package mekhq.gui.utilities;

import java.util.UUID;
import java.util.Vector;

import megamek.common.Entity;
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
