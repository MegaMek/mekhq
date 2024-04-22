/*
 * SkillType.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.personnel;

import megamek.Version;
import megamek.common.*;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map;

/**
 * Skill type will hold static information for each skill type like base target number,
 * whether to count up, and XP costs for advancement.
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class SkillType {
    // combat skills
    public static final String S_PILOT_MECH  = "Piloting/Mech";
    public static final String S_PILOT_AERO  = "Piloting/Aerospace";
    public static final String S_PILOT_JET   = "Piloting/Aircraft";
    public static final String S_PILOT_GVEE  = "Piloting/Ground Vehicle";
    public static final String S_PILOT_VTOL  = "Piloting/VTOL";
    public static final String S_PILOT_NVEE  = "Piloting/Naval";
    public static final String S_PILOT_SPACE = "Piloting/Spacecraft";
    public static final String S_GUN_MECH    = "Gunnery/Mech";
    public static final String S_GUN_AERO    = "Gunnery/Aerospace";
    public static final String S_GUN_JET     = "Gunnery/Aircraft";
    public static final String S_GUN_VEE     = "Gunnery/Vehicle";
    public static final String S_GUN_SPACE   = "Gunnery/Spacecraft";
    public static final String S_GUN_BA      = "Gunnery/Battlesuit";
    public static final String S_GUN_PROTO   = "Gunnery/ProtoMech";
    public static final String S_ARTILLERY   = "Artillery";
    public static final String S_SMALL_ARMS  = "Small Arms";
    public static final String S_ANTI_MECH   = "Anti-Mech";
    public static final String S_TACTICS     = "Tactics";
    // non-combat skills
    public static final String S_TECH_MECH     = "Tech/Mech";
    public static final String S_TECH_MECHANIC = "Tech/Mechanic";
    public static final String S_TECH_AERO     = "Tech/Aero";
    public static final String S_TECH_BA       = "Tech/BA";
    public static final String S_TECH_VESSEL   = "Tech/Vessel";
    public static final String S_ASTECH        = "Astech";
    public static final String S_DOCTOR        = "Doctor";
    public static final String S_MEDTECH       = "Medtech";
    public static final String S_NAV           = "Hyperspace Navigation";
    public static final String S_ADMIN         = "Administration";
    public static final String S_NEG           = "Negotiation";
    public static final String S_LEADER        = "Leadership";
    public static final String S_SCROUNGE      = "Scrounge";
    public static final String S_STRATEGY      = "Strategy";

    public static final int NUM_LEVELS = 11;

    public static final String[] skillList = {S_PILOT_MECH,S_GUN_MECH,S_PILOT_AERO,S_GUN_AERO,
                                              S_PILOT_GVEE,S_PILOT_VTOL,S_PILOT_NVEE,S_GUN_VEE,
                                              S_PILOT_JET,S_GUN_JET,S_PILOT_SPACE,S_GUN_SPACE,S_ARTILLERY,
                                              S_GUN_BA,S_GUN_PROTO,S_SMALL_ARMS,S_ANTI_MECH,
                                              S_TECH_MECH,S_TECH_MECHANIC,S_TECH_AERO,S_TECH_BA,S_TECH_VESSEL,S_ASTECH,
                                              S_DOCTOR,S_MEDTECH,S_NAV,
                                              S_ADMIN,
                                              S_TACTICS,S_STRATEGY,
                                              S_NEG,S_LEADER,S_SCROUNGE};

    public static Map<String, SkillType> lookupHash;

    public static final int SKILL_NONE = 0;

    public static final int EXP_NONE = -1;
    public static final int EXP_ULTRA_GREEN = 0;
    public static final int EXP_GREEN = 1;
    public static final int EXP_REGULAR = 2;
    public static final int EXP_VETERAN = 3;
    public static final int EXP_ELITE = 4;

    public static String getExperienceLevelName(int level) {
        switch (level) {
            case EXP_ULTRA_GREEN:
                return "Ultra-Green";
            case EXP_GREEN:
                return "Green";
            case EXP_REGULAR:
                return "Regular";
            case EXP_VETERAN:
                return "Veteran";
            case EXP_ELITE:
                return "Elite";
            case -1:
                return "Unknown";
            default:
                return "Impossible";
        }
    }

    private String name;
    private int target;
    private boolean countUp;
    private int greenLvl;
    private int regLvl;
    private int vetLvl;
    private int eliteLvl;
    private Integer[] costs;

    public static void setSkillTypes(Map<String, SkillType> skills) {
        // we are going to cycle through all skills in case ones have been added since this hash
        // was created
        for (String name : skillList) {
            if (null != skills.get(name)) {
                lookupHash.put(name, skills.get(name));
            }
        }
    }

    public static Map<String, SkillType> getSkillHash() {
        return lookupHash;
    }

    public static void setSkillHash(final Map<String, SkillType> hash) {
        lookupHash = hash;
    }

    public static String[] getSkillList() {
        return skillList;
    }

    /** Creates new SkillType */
    public SkillType() {
        greenLvl = 1;
        regLvl = 3;
        vetLvl = 4;
        eliteLvl = 5;
        costs = new Integer[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    }

    public String getName() {
        return name;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int t) {
        target = t;
    }

    public boolean countUp() {
        return countUp;
    }

    public int getLevelFromExperience(int expLvl) {
        switch (expLvl) {
            case EXP_REGULAR:
                return regLvl;
            case EXP_VETERAN:
                return vetLvl;
            case EXP_ELITE:
                return eliteLvl;
            case EXP_GREEN:
                return greenLvl;
            default:
                // for ultra-green we take the midpoint between green and 0, rounding down.
                // If the user has set green as zero, then this will be the same
                return (int) Math.floor(greenLvl / 2.0);
        }
    }

    public int getGreenLevel() {
        return greenLvl;
    }

    public void setGreenLevel(int l) {
        greenLvl = l;
    }

    public int getRegularLevel() {
        return regLvl;
    }

    public void setRegularLevel(int l) {
        regLvl = l;
    }

    public int getVeteranLevel() {
        return vetLvl;
    }

    public void setVeteranLevel(int l) {
        vetLvl = l;
    }

    public int getEliteLevel() {
        return eliteLvl;
    }

    public void setEliteLevel(int l) {
        eliteLvl = l;
    }

    public int getCost(int lvl) {
        if ((lvl > 10) || (lvl < 0)) {
            return -1;
        } else {
            return costs[lvl];
        }
    }

    /** get the cost to acquire this skill at the given level from scratch **/
    public int getTotalCost(int lvl) {
        int totalCost = 0;
        for (int i = 0; i<=lvl; i++) {
            totalCost = totalCost + costs[i];
        }
        return totalCost;
    }

    /** @return the maximum level of that skill (the last one not set to cost = -1, or 10) */
    public int getMaxLevel() {
        for (int lvl = 0; lvl < costs.length; ++ lvl) {
            if (costs[lvl] < 0) {
                return lvl - 1;
            }
        }

        return costs.length - 1;
    }

    public static void setCost(String name, int cost, int lvl) {
        SkillType type = lookupHash.get(name);
        if ((name != null) && (lvl < 11)) {
            type.costs[lvl] = cost;
        }
    }

    public boolean isPiloting() {
        return name.equals(S_PILOT_MECH) || name.equals(S_PILOT_AERO)
                    || name.equals(S_PILOT_GVEE) || name.equals(S_PILOT_VTOL)
                    || name.equals(S_PILOT_NVEE) || name.equals(S_PILOT_JET)
                    || name.equals(S_PILOT_SPACE);
    }

    public boolean isGunnery() {
        return name.equals(S_GUN_MECH) || name.equals(S_GUN_AERO)
                    || name.equals(S_GUN_VEE) || name.equals(S_GUN_BA)
                    || name.equals(S_SMALL_ARMS) || name.equals(S_GUN_JET)
                    || name.equals(S_GUN_SPACE) || name.equals(S_GUN_PROTO)
                    || name.equals(S_ARTILLERY);
    }

    public int getExperienceLevel(int lvl) {
        if (lvl >= eliteLvl) {
            return EXP_ELITE;
        } else if (lvl >= vetLvl) {
            return EXP_VETERAN;
        } else if (lvl >= regLvl) {
            return EXP_REGULAR;
        } else if (lvl >= greenLvl) {
            return EXP_GREEN;
        } else {
            return EXP_ULTRA_GREEN;
        }
    }

    public static void initializeTypes() {
        lookupHash = new Hashtable<>();
        lookupHash.put(S_PILOT_MECH, createPilotingMech());
        lookupHash.put(S_GUN_MECH, createGunneryMech());
        lookupHash.put(S_PILOT_AERO, createPilotingAero());
        lookupHash.put(S_GUN_AERO, createGunneryAero());
        lookupHash.put(S_PILOT_JET, createPilotingJet());
        lookupHash.put(S_GUN_JET, createGunneryJet());
        lookupHash.put(S_PILOT_SPACE, createPilotingSpace());
        lookupHash.put(S_GUN_SPACE, createGunnerySpace());
        lookupHash.put(S_PILOT_GVEE, createPilotingGroundVee());
        lookupHash.put(S_PILOT_NVEE, createPilotingNavalVee());
        lookupHash.put(S_PILOT_VTOL, createPilotingVTOL());
        lookupHash.put(S_GUN_VEE, createGunneryVehicle());
        lookupHash.put(S_ARTILLERY, createArtillery());
        lookupHash.put(S_GUN_BA, createGunneryBA());
        lookupHash.put(S_GUN_PROTO, createGunneryProto());
        lookupHash.put(S_SMALL_ARMS, createSmallArms());
        lookupHash.put(S_ANTI_MECH, createAntiMech());
        lookupHash.put(S_TECH_MECH, createTechMech());
        lookupHash.put(S_TECH_MECHANIC, createTechMechanic());
        lookupHash.put(S_TECH_AERO, createTechAero());
        lookupHash.put(S_TECH_BA, createTechBA());
        lookupHash.put(S_TECH_VESSEL, createTechVessel());
        lookupHash.put(S_ASTECH, createAstech());
        lookupHash.put(S_DOCTOR, createDoctor());
        lookupHash.put(S_MEDTECH, createMedtech());
        lookupHash.put(S_NAV, createNav());
        lookupHash.put(S_TACTICS, createTactics());
        lookupHash.put(S_STRATEGY, createStrategy());
        lookupHash.put(S_ADMIN, createAdmin());
        lookupHash.put(S_LEADER, createLeadership());
        lookupHash.put(S_NEG, createNegotiation());
        lookupHash.put(S_SCROUNGE, createScrounge());
    }

    public static SkillType getType(String t) {
        return lookupHash.get(t);
    }

    public static String getDrivingSkillFor(Entity en) {
        if (en instanceof Tank) {
            switch (en.getMovementMode()) {
                case VTOL:
                    return S_PILOT_VTOL;
                case NAVAL:
                case HYDROFOIL:
                case SUBMARINE:
                    return S_PILOT_NVEE;
                default:
                    return S_PILOT_GVEE;
            }
        } else if ((en instanceof SmallCraft) || (en instanceof Jumpship)) {
            return S_PILOT_SPACE;
        } else if (en instanceof ConvFighter) {
            return S_PILOT_JET;
        } else if (en instanceof Aero) {
            return S_PILOT_AERO;
        } else if (en instanceof Infantry) {
            return S_ANTI_MECH;
        } else if (en instanceof Protomech) {
            return S_GUN_PROTO;
        } else {
            return S_PILOT_MECH;
        }
    }

    public static String getGunnerySkillFor(Entity en) {
        if (en instanceof Tank) {
            return S_GUN_VEE;
        } else if ((en instanceof SmallCraft) || (en instanceof Jumpship)) {
            return S_GUN_SPACE;
        } else if (en instanceof ConvFighter) {
            return S_GUN_JET;
        } else if (en instanceof Aero) {
            return S_GUN_AERO;
        } else if (en instanceof Infantry) {
            if (en instanceof BattleArmor) {
                return S_GUN_BA;
            } else {
                return S_SMALL_ARMS;
            }
        } else if (en instanceof Protomech) {
            return S_GUN_PROTO;
        } else {
            return S_GUN_MECH;
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skillType");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "target", target);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "countUp", countUp);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "greenLvl", greenLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "regLvl", regLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vetLvl", vetLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eliteLvl", eliteLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "costs", StringUtils.join(costs, ','));
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "skillType");
    }

    public static void generateInstanceFromXML(Node wn, Version version) {
        try {
            SkillType retVal = new SkillType();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("target")) {
                    retVal.target = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("greenLvl")) {
                    retVal.greenLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("regLvl")) {
                    retVal.regLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("vetLvl")) {
                    retVal.vetLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("eliteLvl")) {
                    retVal.eliteLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("countUp")) {
                    retVal.countUp = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("costs")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.costs[i] = Integer.parseInt(values[i]);
                    }
                }
            }

            if ("Gunnery/Protomech".equals(retVal.getName())) { // Renamed in 0.49.12
                retVal.name = "Gunnery/ProtoMech";
            }

            lookupHash.put(retVal.name, retVal);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    public static void generateSeparateInstanceFromXML(final Node wn,
                                                       final Map<String, SkillType> hash) {
        try {
            SkillType retVal = new SkillType();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("target")) {
                    retVal.target = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("greenLvl")) {
                    retVal.greenLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("regLvl")) {
                    retVal.regLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("vetLvl")) {
                    retVal.vetLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("eliteLvl")) {
                    retVal.eliteLvl = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("countUp")) {
                    retVal.countUp = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("costs")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.costs[i] = Integer.parseInt(values[i]);
                    }
                }
            }

            if ("Gunnery/Protomech".equals(retVal.getName())) { // Renamed in 0.49.12
                retVal.name = "Gunnery/ProtoMech";
            }

            hash.put(retVal.name, retVal);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    public static SkillType createPilotingMech() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_MECH;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createGunneryMech() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_MECH;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createPilotingAero() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_AERO;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createGunneryAero() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_AERO;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createPilotingJet() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_JET;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createGunneryJet() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_JET;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createPilotingSpace() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_SPACE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createGunnerySpace() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_SPACE;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createPilotingGroundVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_GVEE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createPilotingNavalVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_NVEE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createPilotingVTOL() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_VTOL;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createGunneryVehicle() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_VEE;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createArtillery() {
        SkillType skill = new SkillType();
        skill.name = S_ARTILLERY;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createGunneryBA() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_BA;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createGunneryProto() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_PROTO;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};

        return skill;
    }

    public static SkillType createSmallArms() {
        SkillType skill = new SkillType();
        skill.name = S_SMALL_ARMS;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createAntiMech() {
        SkillType skill = new SkillType();
        skill.name = S_ANTI_MECH;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,-1,-1};

        return skill;
    }

    public static SkillType createTechMech() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECH;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createTechMechanic() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECHANIC;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createTechAero() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_AERO;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createTechBA() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_BA;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createTechVessel() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_VESSEL;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createAstech() {
        SkillType skill = new SkillType();
        skill.name = S_ASTECH;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createDoctor() {
        SkillType skill = new SkillType();
        skill.name = S_DOCTOR;
        skill.target = 11;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,0,8,8,8,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createMedtech() {
        SkillType skill = new SkillType();
        skill.name = S_MEDTECH;
        skill.target = 11;
        skill.countUp = false;
        skill.costs = new Integer[]{16,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createNav() {
        SkillType skill = new SkillType();
        skill.name = S_NAV;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }

    public static SkillType createTactics() {
        SkillType skill = new SkillType();
        skill.name = S_TACTICS;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};

        return skill;
    }

    public static SkillType createStrategy() {
        SkillType skill = new SkillType();
        skill.name = S_STRATEGY;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};

        return skill;
    }

    public static SkillType createAdmin() {
        SkillType skill = new SkillType();
        skill.name = S_ADMIN;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,0,4,4,4,-1,-1,-1,-1,-1};

        return skill;
    }

    public static SkillType createLeadership() {
        SkillType skill = new SkillType();
        skill.name = S_LEADER;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};

        return skill;
    }

    public static SkillType createNegotiation() {
        SkillType skill = new SkillType();
        skill.name = S_NEG;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};

        return skill;
    }

    public static SkillType createScrounge() {
        SkillType skill = new SkillType();
        skill.name = S_SCROUNGE;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};

        return skill;
    }
}
