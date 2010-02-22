/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package mekhq.campaign;

import common.EquipmentFactory;
import components.ActuatorSet;
import components.Armor;
import components.ArtemisIVFCS;
import components.AvailableCode;
import components.Engine;
import components.Gyro;
import components.HeatSink;
import components.InternalStructure;
import components.JumpJet;
import components.Mech;
import components.PhysicalEnhancement;
import components.SimplePlaceable;
import components.Supercharger;
import components.TargetingComputer;
import components.abPlaceable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import megamek.common.EquipmentType;
import mekhq.campaign.parts.Part;
import states.ifCockpit;
import states.ifHeatSinkFactory;
import states.ifJumpJetFactory;
import states.stCockpitStandard;
import states.stHeatSinkCLDHS;
import states.stHeatSinkCLLaser;
import states.stHeatSinkISCompact;
import states.stHeatSinkISDHS;
import states.stHeatSinkSingle;
import states.stJumpJetImproved;
import states.stJumpJetStandard;
import states.stJumpJetUMU;

/**
 * Class linking with SSW Lib and providing helper methods
 * Main method : getAbPlaceableByName
 *
 * @author natit
 */
public class SSWLibHelper {

    private static ifHeatSinkFactory getHeatSinkFactory (String name) {
        ifHeatSinkFactory singleHeatSink = new stHeatSinkSingle();
        ifHeatSinkFactory isDoubleHeatSink = new stHeatSinkISDHS();
        ifHeatSinkFactory clDoubleHeatSink = new stHeatSinkCLDHS();
        ifHeatSinkFactory isCompactHeatSink = new stHeatSinkISCompact();
        ifHeatSinkFactory clLaserHeatSink = new stHeatSinkCLLaser();
        
        if (name.equals("(IS) Double Heat Sink"))
            return isDoubleHeatSink;
        else if (name.equals("(CL) Double Heat Sink"))
            return clDoubleHeatSink;
        else if (name.contains("Compact") && name.contains("Heat Sink"))
            return isCompactHeatSink;
        else if (name.contains("Laser") && name.contains("Heat Sink"))
            return clLaserHeatSink;
        else
            return singleHeatSink;
    }

    private static ifJumpJetFactory getJumpJetFactory (String name) {
        ifJumpJetFactory jumpJetStandard = new stJumpJetStandard();
        ifJumpJetFactory jumpJetImproved = new stJumpJetImproved();
        ifJumpJetFactory jumpJetUMU = new stJumpJetUMU();

        if (name.contains("Improved Jump Jet"))
            return jumpJetImproved;
        else if (name.contains("UMU") && name.contains("Jump Jet"))
            return jumpJetUMU;
        else
            return jumpJetStandard;
    }

    /**
     * Gets an abPlaceable object from a name
     *
     * @param equipmentFactory
     * @param mech
     * @param name Candidates for names can be obtained by Part.getPotentialSSWNames
     * @return
     */
    public static abPlaceable getAbPlaceableByName (EquipmentFactory equipmentFactory, Mech mech, String name) {

        /*
         ********************* Equipment ********************
         */
        if (name.contains("Heat Sink")) {
            ifHeatSinkFactory heatSinkFactory = getHeatSinkFactory(name);
            HeatSink heatSink = heatSinkFactory.GetHeatSink();
            return heatSink;

        } else if (name.contains("Artemis IV FCS")) {
            return new ArtemisIVFCS(null);

        } else if (name.contains("Jump Jet")) {
            ifJumpJetFactory jumpJetFactory = getJumpJetFactory(name);
            JumpJet jumpJet = jumpJetFactory.GetJumpJet();
            return jumpJet;

        } else if (name.contains("MASC")) {
            PhysicalEnhancement physicalEnhancement = new PhysicalEnhancement(mech);
            
            if (name.contains("(CL)"))
                physicalEnhancement.SetCLMASC();
            else
                physicalEnhancement.SetISMASC();

            return physicalEnhancement;

        } else if (name.equals("(IS) Targeting Computer")) {
            return new TargetingComputer(null, false);

        } else if (name.equals("(CL) Targeting Computer")) {
            return new TargetingComputer(null, true);

        } else if (name.contains("Supercharger")) {
            return new Supercharger(null);

        /*
         ********************* Armor ********************
         */
        } else if (name.contains("Armor")) {
            Armor armor = new Armor(mech);

            if (name.contains("(IS)")) {
                if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_STANDARD]))
                    armor.SetStandard();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_LIGHT_FERRO]))
                    armor.SetISLF();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_HEAVY_FERRO]))
                    armor.SetISHF();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_FERRO_FIBROUS]))
                    armor.SetISFF();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_STEALTH]))
                    armor.SetISST();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_HARDENED]))
                    armor.SetHardened();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_REFLECTIVE]))
                    armor.SetISLR();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_REACTIVE]))
                    armor.SetISRE();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_INDUSTRIAL]))
                    armor.SetIndustrial();
                else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_COMMERCIAL]))
                    armor.SetCommercial();

            } else if (name.contains("(CL)")) {
                 if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_STANDARD]))
                    armor.SetStandard();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_FERRO_LAMELLOR]))
                    armor.SetCLFL();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_FERRO_FIBROUS]))
                    armor.SetCLFF();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_HARDENED]))
                    armor.SetHardened();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_REFLECTIVE]))
                    armor.SetCLLR();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_REACTIVE]))
                    armor.SetCLRE();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_INDUSTRIAL]))
                    armor.SetIndustrial();
                 else if (name.contains(EquipmentType.armorNames[EquipmentType.T_ARMOR_COMMERCIAL]))
                    armor.SetCommercial();
            }

            return armor;

        /*
         ********************* Body Part ********************
         */
        } else if (name.contains("Mech") && (name.contains("Arm") || name.contains("Torso") || name.contains("Leg") || name.contains("Head")) && !name.contains("Actuator")) {
            InternalStructure internalStructure = new InternalStructure(mech);

            // Quad or Biped doesn't matter for availability

            if (name.contains("(IS)")) {
                if (name.contains("Endosteel") || name.contains("Endo-Steel") || name.contains("Endo Steel") || name.contains("EndoSteel"))
                    internalStructure.SetISESBP();
                else if (name.contains("Composite"))
                    internalStructure.SetISCOBP();
                else if (name.contains("Reinforced"))
                    internalStructure.SetREBP();
                else
                    internalStructure.SetMSBP();
                
            } else if (name.contains("(CL)")) {
                if (name.contains("Endosteel") || name.contains("Endo-Steel") || name.contains("Endo Steel") || name.contains("EndoSteel"))
                    internalStructure.SetCLESBP();
                else if (name.contains("Reinforced"))
                    internalStructure.SetREBP();
                else
                    internalStructure.SetMSBP();
            }

            return internalStructure;
            
        /*
         ********************* Gyro ********************
         */
        } else if (name.contains("Gyro")) {
            Gyro gyro = new Gyro(mech);

            // Tech type doesn't matter for availability

            if (name.contains("Compact"))
                gyro.SetISCompact();
            else if (name.contains("Heavy Duty"))
                gyro.SetISHeavy();
            else if (name.contains("XL"))
                gyro.SetISXL();
            else
                gyro.SetStandard();

            return gyro;

        /*
         ********************* Actuator ********************
         */
        } else if (name.contains("Actuator")) {
            ActuatorSet actuatorSet = new ActuatorSet(null, mech);

            // The body part of the actuator doesn't matter for availability
            // Tech type doesn't matter for availability

            return actuatorSet.LeftFoot;

        /*
         ********************* Engine ********************
         */
        } else if (name.contains("Engine")) {
            Engine engine = new Engine(mech);

            if (name.contains("(IS)")) {
                if (name.contains("XXL"))
                    engine.SetISXXLEngine();
                else if (name.contains("XL"))
                    engine.SetISXLEngine();
                else if (name.contains("Light"))
                    engine.SetISLFEngine();
                else if (name.contains("Compact"))
                    engine.SetISCFEngine();
                else if (name.contains("Fusion"))
                    engine.SetFUEngine();
                else if (name.contains("Fuel Cell"))
                    engine.SetFCEngine();
                else if (name.contains("FISSION") || name.contains("Fission"))
                    engine.SetFIEngine();
                else if (name.contains("ICE"))
                    engine.SetICEngine();

            } else if (name.contains("(CL)")) {

                if (name.contains("XXL"))
                    engine.SetCLXXLEngine();
                else if (name.contains("XL"))
                    engine.SetCLXLEngine();
                else if (name.contains("Fusion"))
                    engine.SetFUEngine();
                else if (name.contains("Fuel Cell"))
                    engine.SetFCEngine();
                else if (name.contains("FISSION") || name.contains("Fission"))
                    engine.SetFIEngine();
                else if (name.contains("ICE"))
                    engine.SetICEngine();
            }

            return engine;

        /*
         ********************* Sensors ********************
         */
        } else if (name.contains("Sensors")) {
            ifCockpit cockpit = new stCockpitStandard();

            // Tech type doesn't matter for availability

            SimplePlaceable sensors = cockpit.GetSensors();
            return sensors;

        /*
         ********************* Life Support ********************
         */
        } else if (name.contains("Life Support")) {
            ifCockpit cockpit = new stCockpitStandard();

            // Tech type doesn't matter for availability

            SimplePlaceable lifeSupport = cockpit.GetLifeSupport();
            return lifeSupport;

        } else {
            return equipmentFactory.GetByName(name, mech);
        }
    }

    public static int getModifierFromAvailability (char availability) {
        int modifier = 999;

        switch (availability) {
            case ('A') :
                modifier = -4;
                break;
            case ('B') :
                modifier = -3;
                break;
            case ('C') :
                modifier = -2;
                break;
            case ('D') :
                modifier = -1;
                break;
            case ('E') :
                modifier = 0;
                break;
            case ('F') :
                modifier = 2;
                break;
            case ('X') :
                // Strat ops states 5 for extinct equipment but it seems to me that extinct equipment shouldn't be buyable
                modifier = 10;
                break;
        }
        
        return modifier;
    }

    public AvailableCodeHelper availableCodeHelperFactory (AvailableCode availableCode) {
        return new AvailableCodeHelper(availableCode);
    }

    private static char decreaseAvailability (char availability) {
        char finalAvailability = availability;

        switch (availability) {
            case 'A' :
                finalAvailability = 'B';
                break;
            case 'B' :
                finalAvailability = 'C';
                break;
            case 'C' :
                finalAvailability = 'D';
                break;
            case 'D' :
                finalAvailability = 'E';
                break;
            case 'E' :
                finalAvailability = 'F';
                break;
            case 'F' :
                finalAvailability = 'F';
                break;
            case 'X' :
                finalAvailability = 'X';
                break;
        }

        return finalAvailability;
    }

    public class AvailableCodeHelper {

        private AvailableCode availableCode;

        private char techRating = 'D';
        private char availabilityStarLeague = 'D';
        private char availabilitySuccessionWar = 'D';
        private char availabilityClanInvasion = 'D';

        private int introDate = -1;
        private int introFaction = -1;
        private int extinctionDate = -1;
        private int reintroDate = -1;
        private int reintroFaction = -1;

        public char getTechRating() {
            return techRating;
        }

        public AvailableCodeHelper (AvailableCode ac) {
            this.availableCode = ac;
            setup();
        }

        private void setup () {

            String introF = null;
            String reintroF = null;
            
            switch( availableCode.GetTechBase() ) {
                case AvailableCode.TECH_INNER_SPHERE:
                    techRating = availableCode.GetISTechRating();
                    availabilityStarLeague = availableCode.GetISSLCode();
                    availabilitySuccessionWar = availableCode.GetISSWCode();
                    availabilityClanInvasion = availableCode.GetISCICode();
                    introDate = availableCode.GetISIntroDate();
                    introF = availableCode.GetISIntroFaction();
                    introFaction = getFactionFromCode(introF);

                    if( availableCode.WentExtinctIS() ) {
                        extinctionDate = availableCode.GetISExtinctDate();

                        if (availableCode.WasReIntrodIS()) {
                            reintroDate = availableCode.GetISReIntroDate();
                            reintroF = availableCode.GetISReIntroFaction();
                            reintroFaction = getFactionFromCode(reintroF);
                        }
                    }
                    break;

                case AvailableCode.TECH_CLAN :
                    techRating = availableCode.GetCLTechRating();
                    availabilityStarLeague = availableCode.GetCLSLCode();
                    availabilitySuccessionWar = availableCode.GetCLSWCode();
                    availabilityClanInvasion = availableCode.GetCLCICode();
                    introDate = availableCode.GetCLIntroDate();
                    introF = availableCode.GetCLIntroFaction();
                    introFaction = getFactionFromCode(introF);

                    if( availableCode.WentExtinctCL() ) {
                        extinctionDate = availableCode.GetCLExtinctDate();
                            
                        if( availableCode.WasReIntrodCL() ) {
                            reintroDate = availableCode.GetCLReIntroDate();
                            reintroF = availableCode.GetCLReIntroFaction();
                            reintroFaction = getFactionFromCode(reintroF);
                        }
                    }
                    break;

                case AvailableCode.TECH_BOTH :
                    techRating = availableCode.GetBestTechRating();
                    availabilityStarLeague = availableCode.GetBestSLCode();
                    availabilitySuccessionWar = availableCode.GetBestSWCode();
                    availabilityClanInvasion = availableCode.GetBestCICode();
                    
                    break;
            }
            
        }

        private int getFactionFromCode (String code) {
           int faction = -1;

           if (code.equals("CC"))
               faction = Faction.F_CAPCON;
           else if (code.equals("DC"))
               faction = Faction.F_DRAC;
           else if (code.equals("FS"))
               faction = Faction.F_FEDSUN;
           else if (code.equals("FW"))
               faction = Faction.F_FWL;
           else if (code.equals("LA"))
               faction = Faction.F_LYRAN;
           else if (code.equals("CS"))
               faction = Faction.F_COMSTAR;
           else if (code.equals("WB"))
               faction = Faction.F_WOB;
           else if (code.equals("CL"))
               faction = Faction.F_CLAN;
           else if (code.equals("FR"))
               faction = Faction.F_FRR;
           else if (code.equals("TH"))
               faction = Faction.F_TERRAN;
           else
               faction = Faction.F_PERIPHERY;

           return faction;
        }

        public char getAvailability (GregorianCalendar calendar) {
            char availability;

            int year = calendar.get(Calendar.YEAR);

            int era = Era.getEra(year);
            if (era==Era.E_AOW || era==Era.E_RW || era==Era.E_SL)
                availability = availabilityStarLeague;
            else if (era==Era.E_1SW || era==Era.E_2SW || era==Era.E_3SW || era==Era.E_4SW)
                availability = availabilitySuccessionWar;
            else
                availability = availabilityClanInvasion;

            if (year < introDate)
                availability = 'X';

            if (extinctionDate > 0 && year > extinctionDate) {
                if (reintroDate < 0 || year < reintroDate)
                    availability = 'X';
            }

            // Tech manual page 280 says to decrease availability (increase letter) if unit produced close to the end of an era
            // Parts whcih have started to be produced recently should have an availability decrease
            // TODO Custom rule based on tech manual
            // "Custom" rule : increase availability letter by 2 if produced less than 3 years ago
            //                 increase availability letter by 1 if produced less than 6 years ago
            int yearsSinceIntroduction = 999;
            if (year >= introDate && (reintroDate < 0 || year < reintroDate)) {
                // Between intro and reintro (if any)
               yearsSinceIntroduction = year - introDate;
            } else if (reintroDate > 0 && year >= reintroDate) {
                // After reintro date
                yearsSinceIntroduction = year - reintroDate;
            }

            if (yearsSinceIntroduction < 3) {
                availability = decreaseAvailability(availability);
                availability = decreaseAvailability(availability);
            } else if (yearsSinceIntroduction < 5) {
                availability = decreaseAvailability(availability);
            }

            return availability;
        }
    }

    public static AvailableCodeHelper getPartAvailableCodeHelper(Part part, Campaign campaign) {
        int partTechBase = (part.isClanTechBase()?AvailableCode.TECH_CLAN:AvailableCode.TECH_INNER_SPHERE);
        AvailableCode availableCode = new AvailableCode(partTechBase);
        availableCode.SetISCodes('D', 'D', 'D', 'D');
        availableCode.SetCLCodes('D', 'D', 'D', 'D');

        abPlaceable placeable = null;
        for (String sswName : part.getPotentialSSWNames(campaign.getFaction())) {
            placeable = SSWLibHelper.getAbPlaceableByName(Campaign.getSswEquipmentFactory(), Campaign.getSswMech(), sswName);
            if (placeable != null)
                break;
        }

        if (placeable != null)
            availableCode = placeable.GetAvailability();

        AvailableCodeHelper availableCodeHelper = (new SSWLibHelper()).availableCodeHelperFactory(availableCode);

        return availableCodeHelper;
    }

    /**
     * This method genereates an availability modifier based on the tech base / tech rating of the part used relative to the faction / location of the player
     *
     * @param part
     * @param availableCodeHelper
     * @param campaign
     * @return
     */
    public static int getFactionAndTechMod (Part part, AvailableCodeHelper availableCodeHelper, Campaign campaign) {

        int currentFaction = campaign.getFaction();
        int currentYear = campaign.getCalendar().get(Calendar.YEAR);

        char techRating = 'D';
        if (availableCodeHelper != null)
            techRating = availableCodeHelper.getTechRating();

        // Change to reflect current location
        int currentLocation = currentFaction;

        int factionMod = 0;
        if (part.isClanTechBase() && !Faction.isClanFaction(currentFaction)) {
            // Availability of clan tech for IS
            if (currentYear<3050)
                // Impossible to buy before clan invasion
                factionMod = 12;
            else if (currentYear<=3052)
                // Between begining of clan invasiuon and tukayyid, very very hard to buy
                factionMod = 5;
            else if (currentYear<=3060)
                // Between tukayyid and great refusal, very hard to buy
                factionMod = 4;
            else
                // After great refusal, hard to buy
                factionMod = 3;
        }
        if (!part.isClanTechBase()) {
            // Availability of high tech rating equipment in low tech areas (periphery)
            switch (techRating) {
                case 'E' :
                    if (Faction.isPeripheryFaction(currentLocation))
                        factionMod += 1;
                    break;
                case 'F' :
                    if (Faction.isPeripheryFaction(currentLocation))
                        factionMod += 2;
                    break;
            }
        }

        return factionMod;
    }

    public static boolean compareAvailability (char a, char b) {
        return (a > b);
    }
}
