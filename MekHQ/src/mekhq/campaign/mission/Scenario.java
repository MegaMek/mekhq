/*
 * Scenario.java
 *
 * Copyright (C) 2011-2016 - The MegaMek Team. All Rights Reserved.
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.mission;

import megamek.Version;
import megamek.client.ui.swing.lobby.LobbyUtility;
import megamek.common.Entity;
import megamek.common.IStartingPositions;
import megamek.common.MapSettings;
import megamek.common.PlanetaryConditions;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.atb.IAtBScenario;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Scenario {
    //region Variable Declarations
    public static final int S_DEFAULT_ID = -1;

    /** terrain types **/
    public static final int TER_LOW_ATMO = -2;
    public static final int TER_SPACE = -1;
    public static final int TER_HILLS = 0;
    public static final int TER_BADLANDS = 1;
    public static final int TER_WETLANDS = 2;
    public static final int TER_LIGHTURBAN = 3;
    public static final int TER_FLATLANDS = 4;
    public static final int TER_WOODED = 5;
    public static final int TER_HEAVYURBAN = 6;
    public static final int TER_COASTAL = 7;
    public static final int TER_MOUNTAINS = 8;
    public static final String[] terrainTypes = {"Hills", "Badlands", "Wetlands",
            "Light Urban", "Flatlands", "Wooded", "Heavy Urban", "Coastal",
            "Mountains"
    };


    public static final int[] terrainChart = {
            TER_HILLS, TER_BADLANDS, TER_WETLANDS, TER_LIGHTURBAN,
            TER_HILLS, TER_FLATLANDS, TER_WOODED, TER_HEAVYURBAN,
            TER_COASTAL, TER_WOODED, TER_MOUNTAINS
    };

    private String name;
    private String desc;
    private String report;
    private ScenarioStatus status;
    private LocalDate date;
    private List<Integer> subForceIds;
    private List<UUID> unitIds;
    private int id = S_DEFAULT_ID;
    private int missionId;
    private ForceStub stub;
    private boolean cloaked;

    //allow multiple loot objects for meeting different mission objectives
    private List<Loot> loots;

    private List<ScenarioObjective> scenarioObjectives;

    /** Scenario Deployment Limits **/
    ScenarioDeploymentLimit deploymentLimit;

    /** Lists of enemy forces **/
    protected List<BotForce> botForces;
    protected List<BotForceStub> botForcesStubs;

    // stores external id of bot forces
    private Map<String, Entity> externalIDLookup;

    /** map generation variables **/
    private int terrainType;
    private int mapSizeX;
    private int mapSizeY;
    private String map;
    private boolean usingFixedMap;

    /** planetary conditions parameters **/
    protected int light;
    protected int weather;
    protected int wind;
    protected int fog;
    protected int atmosphere;
    private int temperature;
    protected float gravity;
    private boolean emi;
    private boolean blowingSand;
    private boolean shiftWindDirection;
    private boolean shiftWindStrength;
    private int maxWindStrength;
    private int minWindStrength;

    /** player starting position **/
    private int start;

    //Stores combinations of units and the transports they are assigned to
    private Map<UUID, List<UUID>> playerTransportLinkages;
    //endregion Variable Declarations

    public Scenario() {
        this(null);
    }

    public Scenario(String n) {
        this.name = n;
        desc = "";
        report = "";
        setStatus(ScenarioStatus.CURRENT);
        date = null;
        subForceIds = new ArrayList<>();
        unitIds = new ArrayList<>();
        loots = new ArrayList<>();
        scenarioObjectives = new ArrayList<>();
        playerTransportLinkages = new HashMap<>();
        botForces = new ArrayList<>();
        botForcesStubs = new ArrayList<>();
        externalIDLookup = new HashMap<>();

        light = PlanetaryConditions.L_DAY;
        weather = PlanetaryConditions.WE_NONE;
        wind = PlanetaryConditions.WI_NONE;
        fog = PlanetaryConditions.FOG_NONE;
        atmosphere = PlanetaryConditions.ATMO_STANDARD;
        temperature = 25;
        gravity = (float) 1.0;
        emi = false;
        blowingSand = false;
        shiftWindDirection = false;
        shiftWindStrength = false;
        maxWindStrength = PlanetaryConditions.WI_TORNADO_F4;
        minWindStrength = PlanetaryConditions.WI_NONE;

    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getDescription() {
        return desc;
    }

    public void setDesc(String d) {
        this.desc = d;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String r) {
        this.report = r;
    }

    public ScenarioStatus getStatus() {
        return status;
    }

    public void setStatus(final ScenarioStatus status) {
        this.status = status;
    }

    public @Nullable LocalDate getDate() {
        return date;
    }

    public void setDate(final @Nullable LocalDate date) {
        this.date = date;
    }

    public boolean hasObjectives() {
        return (scenarioObjectives != null) && !scenarioObjectives.isEmpty();
    }

    public List<ScenarioObjective> getScenarioObjectives() {
        return scenarioObjectives;
    }

    public void setScenarioObjectives(List<ScenarioObjective> scenarioObjectives) {
        this.scenarioObjectives = scenarioObjectives;
    }

    /**
     * This indicates that the scenario should not be displayed in the briefing tab.
     */
    public boolean isCloaked() {
        return cloaked;
    }

    public void setCloaked(boolean cloaked) {
        this.cloaked = cloaked;
    }

    public int getTerrainType() {
        return terrainType;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setTerrainType(int terrainType) {
        this.terrainType = terrainType;
    }

    public int getMapSizeX() {
        return mapSizeX;
    }

    public void setMapSizeX(int mapSizeX) {
        this.mapSizeX = mapSizeX;
    }

    public int getMapSizeY() {
        return mapSizeY;
    }

    public void setMapSizeY(int mapSizeY) {
        this.mapSizeY = mapSizeY;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getMapForDisplay() {
        if (!isUsingFixedMap()) {
            return getMap();
        } else {
            MapSettings ms = MapSettings.getInstance();
            return LobbyUtility.cleanBoardName(getMap(), ms);
        }
    }

    public boolean isUsingFixedMap() {
        return usingFixedMap;
    }

    public void setUsingFixedMap(boolean usingFixedMap) {
        this.usingFixedMap = usingFixedMap;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getWeather() {
        return weather;
    }

    public void setWeather(int weather) {
        this.weather = weather;
    }

    public int getWind() {
        return wind;
    }

    public void setWind(int wind) {
        this.wind = wind;
    }

    public int getFog() {
        return fog;
    }

    public void setFog(int fog) {
        this.fog = fog;
    }

    public int getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(int atmosphere) {
        this.atmosphere = atmosphere;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public boolean usesEMI() {
        return emi;
    }

    public void setEMI(boolean emi) {
        this.emi = emi;
    }

    public boolean usesBlowingSand() {
        return blowingSand;
    }

    public void setBlowingSand(boolean blow) {
        this.blowingSand = blow;
    }

    public boolean canWindShiftDirection() { return shiftWindDirection; }

    public void setShiftWindDirection(boolean b) { this.shiftWindDirection = b; }

    public boolean canWindShiftStrength() { return shiftWindStrength; }

    public void setShiftWindStrength(boolean b) { this.shiftWindStrength = b; }

    public int getMaxWindStrength() { return maxWindStrength; }

    public void setMaxWindStrength(int strength) { this.maxWindStrength = strength; }

    public int getMinWindStrength() { return minWindStrength; }

    public void setMinWindStrength(int strength) { this.minWindStrength = strength; }

    public ScenarioDeploymentLimit getDeploymentLimit() {
        return deploymentLimit;
    }

    public Map<UUID, List<UUID>> getPlayerTransportLinkages() {
        return playerTransportLinkages;
    }

    /**
     * Adds a transport-cargo pair to the internal transport relationship store.
     * @param transportId the UUID of the transport object
     * @param cargoId     the UUID of the cargo being transported
     */
    public void addPlayerTransportRelationship(UUID transportId, UUID cargoId) {
        playerTransportLinkages.get(transportId).add(cargoId);
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public int getMissionId() {
        return missionId;
    }

    public void setMissionId(int i) {
        this.missionId = i;
    }

    public List<Integer> getForceIDs() {
        return subForceIds;
    }

    public Force getForces(Campaign campaign) {
        Force force = new Force("Assigned Forces");
        for (int subid : subForceIds) {
            Force sub = campaign.getForce(subid);
            if (null != sub) {
                force.addSubForce(sub, false);
            }
        }
        for (UUID uid : unitIds) {
            force.addUnit(uid);
        }
        return force;
    }

    /**
     * Gets the IDs of units deployed to this scenario individually.
     */
    public List<UUID> getIndividualUnitIDs() {
        return unitIds;
    }

    public void addForces(int fid) {
        subForceIds.add(fid);
    }

    public void addUnit(UUID uid) {
        unitIds.add(uid);
    }

    public boolean containsPlayerUnit(UUID uid) {
        return unitIds.contains(uid);
    }

    public void removeUnit(UUID uid) {
        int idx = -1;
        for (int i = 0; i < unitIds.size(); i++) {
            if (uid.equals(unitIds.get(i))) {
                idx = i;
                break;
            }
        }
        if (idx > -1) {
            unitIds.remove(idx);
        }
    }

    public void removeForce(int fid) {
        List<Integer> toRemove = new ArrayList<>();
        for (Integer subForceId : subForceIds) {
            if (fid == subForceId) {
                toRemove.add(subForceId);
            }
        }
        subForceIds.removeAll(toRemove);
    }

    public void clearAllForcesAndPersonnel(Campaign campaign) {
        for (int fid : subForceIds) {
            Force f = campaign.getForce(fid);
            if (null != f) {
                f.clearScenarioIds(campaign);
                MekHQ.triggerEvent(new DeploymentChangedEvent(f, this));
            }
        }
        for (UUID uid : unitIds) {
            Unit u = campaign.getUnit(uid);
            if (null != u) {
                u.undeploy();
                MekHQ.triggerEvent(new DeploymentChangedEvent(u, this));
            }
        }
        subForceIds = new ArrayList<>();
        unitIds = new ArrayList<>();
    }

    /**
     * Converts this scenario to a stub
     */
    public void convertToStub(final Campaign campaign, final ScenarioStatus status) {
        setStatus(status);
        clearAllForcesAndPersonnel(campaign);
        generateStub(campaign);
    }

    public void generateStub(Campaign c) {
        stub = new ForceStub(getForces(c), c);
        for (BotForce bf : botForces) {
            botForcesStubs.add(generateBotStub(bf, c));
        }
        botForces.clear();
    }

    public ForceStub getForceStub() {
        return stub;
    }

    public BotForceStub generateBotStub(BotForce bf, Campaign c) {
        List<String> stubs = generateEntityStub(bf.getFullEntityList(c));
        return new BotForceStub("<html>" +
                bf.getName() + " <i>" +
                ((bf.getTeam() == 1) ? "Allied" : "Enemy") + "</i>" +
                " Start: " + IStartingPositions.START_LOCATION_NAMES[bf.getStart()] +
                " Fixed BV: " + bf.getTotalBV(c) +
                ((null == bf.getBotForceRandomizer()) ? "" : "<br>Random: " + bf.getBotForceRandomizer().getDescription()) +
                "</html>", stubs);
    }

    public List<String> generateEntityStub(List<Entity> entities) {
        List<String> stub = new ArrayList<>();
        for (Entity en : entities) {
            if (null == en) {
                stub.add("<html><font color='red'>No random assignment table found for faction</font></html>");
            } else {
                stub.add("<html>" + en.getCrew().getName() + " (" +
                        en.getCrew().getGunnery() + "/" +
                        en.getCrew().getPiloting() + "), " +
                        "<i>" + en.getShortName() + "</i>" +
                        "</html>");
            }
        }
        return stub;
    }

    public boolean isAssigned(Unit unit, Campaign campaign) {
        for (UUID uid : getForces(campaign).getAllUnits(true)) {
            if (uid.equals(unit.getId())) {
                return true;
            }
        }
        return false;
    }

    public List<BotForce> getBotForces() {
        return botForces;
    }

    public void addBotForce(BotForce botForce, Campaign c) {
        botForces.add(botForce);

        // put all bot units into the external ID lookup.
        for (Entity entity : botForce.getFullEntityList(c)) {
            getExternalIDLookup().put(entity.getExternalIdAsString(), entity);
        }
    }

    public BotForce getBotForce(int i) {
        return botForces.get(i);
    }

    public void removeBotForce(int i) {
        botForces.remove(i);
    }

    public int getNumBots() {
        return getStatus().isCurrent() ? botForces.size() : botForcesStubs.size();
    }

    public List<BotForceStub> getBotForcesStubs() {
        return botForcesStubs;
    }

    /**
     * Get a List of all traitor Units in this scenario. This function just combines the results from
     * BotForce#getTraitorUnits across all BotForces.
     * @param c - A Campaign pointer
     * @return a List of traitor Units
     */
    public List<Unit> getTraitorUnits(Campaign c) {
        List<Unit> traitorUnits = new ArrayList<>();
        for (BotForce bf : botForces) {
            traitorUnits.addAll(bf.getTraitorUnits(c));
        }
        return traitorUnits;
    }

    /**
     * Tests whether a given entity is a traitor in this Scenario by checking external id values. This should
     * also be usable against entities that are ejected pilots from the original traitor entity.
     * @param en a MegaMek Entity
     * @param c a Campaign pointer
     * @return a boolean indicating whether this entity is a traitor in this Scenario.
     */
    public boolean isTraitor(Entity en, Campaign c) {
        if ("-1".equals(en.getExternalIdAsString())) {
            return false;
        }
        UUID id = UUID.fromString(en.getExternalIdAsString());
        for (Unit u : getTraitorUnits(c)) {
            if (u.getId().equals(id)) {
                return true;
            }
        }
        // also make sure that the crew's external id does not match a traitor in
        // case of ejected pilots
        if ((null != en.getCrew()) && !"-1".equals(en.getCrew().getExternalIdAsString()) &&
                isTraitor(UUID.fromString(en.getCrew().getExternalIdAsString()))) {
            return true;
        }
        return false;
    }

    /**
     * Given a Person's id, is that person a traitor in this Scenario
     * @param personId - a UUID giving a person's id in the campaign
     * @return a boolean indicating if this person is a traitor in the Scenario
     */
    public boolean isTraitor(UUID personId) {
        for (BotForce bf : botForces) {
            if (bf.getTraitorPersons().contains(personId)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Entity> getExternalIDLookup() {
        return externalIDLookup;
    }

    public void setExternalIDLookup(HashMap<String, Entity> externalIDLookup) {
        this.externalIDLookup = externalIDLookup;
    }

    /**
     * Determines whether a unit is eligible to deploy to the scenario. If a ScenarioDeploymentLimit is present
     * the unit type will be checked to make sure it is valid. The function also checks to see if the unit is
     * a traitor unit which will disallow deployment.
     * @param unit - The Unit to be deployed
     * @param campaign - a pointer to the Campaign
     * @return true if the unit is eligible, otherwise false
     */
    public boolean canDeploy(Unit unit, Campaign campaign) {
        // first check to see if this unit is a traitor unit
        for (BotForce bf : botForces) {
            if (bf.isTraitor(unit)) {
                return false;
            }
        }
        // now check deployment limits
        if ((null != deploymentLimit) && (null != unit.getEntity())) {
            return deploymentLimit.isAllowedType(unit.getEntity().getUnitType());
        }
        return true;
    }

    /**
     * Determines whether a list of units is eligible to deploy to the scenario.
     *
     * @param units - a Vector made up of Units to be deployed
     * @param campaign - a pointer to the Campaign
     * @return true if all units in the list are eligible, otherwise false
     */
    public boolean canDeployUnits(Vector<Unit> units, Campaign campaign) {
        int additionalQuantity = 0;
        for (Unit unit : units) {
            if (!canDeploy(unit, campaign)) {
                return false;
            }
            if (null != deploymentLimit) {
                additionalQuantity += deploymentLimit.getUnitQuantity(unit);
            }
        }
        if (null != deploymentLimit) {
            if ((deploymentLimit.getCurrentQuantity(this, campaign) + additionalQuantity) >
                    deploymentLimit.getQuantityCap(campaign)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a list of forces is eligible to deploy to the scenario.
     *
     * @param forces    list of forces
     * @param c         the campaign that the forces are part of
     * @return true if all units in all forces in the list are eligible, otherwise false
     */
    public boolean canDeployForces(Vector<Force> forces, Campaign c) {
        int additionalQuantity = 0;
        for (Force force : forces) {
            Vector<UUID> units = force.getAllUnits(true);
            for (UUID id : units) {
                if (!canDeploy(c.getUnit(id), c)) {
                    return false;
                }
            }
            if (null != deploymentLimit) {
                additionalQuantity += deploymentLimit.getForceQuantity(force, c);
            }
        }
        if (null != deploymentLimit) {
            if ((deploymentLimit.getCurrentQuantity(this, c) + additionalQuantity) >
                    deploymentLimit.getQuantityCap(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean includesRequiredPersonnel(Campaign c) {
        if (null == deploymentLimit) {
            return true;
        } else {
            return deploymentLimit.checkRequiredPersonnel(this, c);
        }

    }

    public boolean includesRequiredUnits(Campaign c) {
        if (null == deploymentLimit) {
            return true;
        } else {
            return deploymentLimit.checkRequiredUnits(this, c);
        }

    }

    public boolean canStartScenario(Campaign c) {
        if (!getStatus().isCurrent()) {
            return false;
        }
        if (getForces(c).getAllUnits(true).isEmpty()) {
            return false;
        }
        if (!includesRequiredPersonnel(c)) {
            return false;
        }
        if (!includesRequiredUnits(c)) {
            return false;
        }
        return true;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    protected void writeToXMLBegin(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scenario", "id", id, "type", getClass());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", getName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "desc", desc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "report", report);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "start", start);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "status", getStatus().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id);
        if (null != stub) {
            stub.writeToXML(pw, indent);
        } else {
            // only bother writing out objectives for active scenarios
            if (hasObjectives()) {
                for (ScenarioObjective objective : this.scenarioObjectives) {
                    objective.Serialize(pw);
                }
            }
        }

        if (null != deploymentLimit) {
            deploymentLimit.writeToXML(pw, indent);
        }

        if (!botForces.isEmpty() && getStatus().isCurrent()) {
            for (BotForce botForce : botForces) {
                botForce.writeToXML(pw, indent);
            }
        }

        if (!botForcesStubs.isEmpty()) {
            for (BotForceStub botStub : botForcesStubs) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "botForceStub", "name", botStub.getName());
                for (String entity : botStub.getEntityList()) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "entityStub", entity);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "botForceStub");
            }
        }

        if (!loots.isEmpty() && getStatus().isCurrent()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "loots");
            for (Loot l : loots) {
                l.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "loots");
        }

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "date", date);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cloaked", isCloaked());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "terrainType", terrainType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usingFixedMap", isUsingFixedMap());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mapSize", mapSizeX, mapSizeY);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "map", map);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "light", light);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "weather", weather);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "wind", wind);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fog", fog);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "temperature", temperature);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "atmosphere", atmosphere);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gravity", gravity);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "emi", emi);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "blowingSand", blowingSand);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shiftWindDirection", shiftWindDirection);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shiftWindStrength", shiftWindStrength);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maxWindStrength", maxWindStrength);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minWindStrength", minWindStrength);
    }

    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, "scenario");
    }

    protected void loadFieldsFromXmlNode(final Node wn, final Version version, final Campaign campaign)
            throws ParseException {
        // Do nothing
    }

    public static Scenario generateInstanceFromXML(Node wn, Campaign c, Version version) {
        Scenario retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        try {
            // Instantiate the correct child class, and call its parsing function.
            if (className.equals(AtBScenario.class.getName())) {
                //Backwards compatibility when AtBScenarios were all part of the same class
                //Find the battle type and then load it through the AtBScenarioFactory

                NodeList nl = wn.getChildNodes();
                int battleType = -1;

                for (int x = 0; x < nl.getLength(); x++) {
                    Node wn2 = nl.item(x);

                    if (wn2.getNodeName().equalsIgnoreCase("battleType")) {
                        battleType = Integer.parseInt(wn2.getTextContent());
                        break;
                    }
                }

                if (battleType == -1) {
                    LogManager.getLogger().error("Unable to load an old AtBScenario because we could not determine the battle type");
                    return null;
                }

                List<Class<IAtBScenario>> scenarioClassList = AtBScenarioFactory.getScenarios(battleType);

                if ((null == scenarioClassList) || scenarioClassList.isEmpty()) {
                    LogManager.getLogger().error("Unable to load an old AtBScenario of battle type " + battleType);
                    return null;
                }

                retVal = (Scenario) scenarioClassList.get(0).newInstance();
            } else {
                retVal = (Scenario) Class.forName(className).newInstance();
            }

            retVal.loadFieldsFromXmlNode(wn, version, c);
            retVal.scenarioObjectives = new ArrayList<>();

            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.setName(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.setStatus(ScenarioStatus.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDesc(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("report")) {
                    retVal.setReport(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceStub")) {
                    retVal.stub = ForceStub.generateInstanceFromXML(wn2, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    retVal.date = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("cloaked")) {
                    retVal.cloaked = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("loots")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("loot")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            LogManager.getLogger().error("Unknown node type not loaded in techUnitIds nodes: " + wn3.getNodeName());
                            continue;
                        }
                        Loot loot = Loot.generateInstanceFromXML(wn3, c, version);
                        retVal.loots.add(loot);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase(ScenarioObjective.ROOT_XML_ELEMENT_NAME)) {
                    retVal.getScenarioObjectives().add(ScenarioObjective.Deserialize(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("botForceStub")) {
                    String name = MHQXMLUtility.unEscape(wn2.getAttributes().getNamedItem("name").getTextContent());
                    List<String> stub = getEntityStub(wn2);
                    retVal.botForcesStubs.add(new BotForceStub(name, stub));
                }  else if (wn2.getNodeName().equalsIgnoreCase("botForce")) {
                    BotForce bf = new BotForce();
                    try {
                        bf.setFieldsFromXmlNode(wn2, version, c);
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error loading bot force in scenario", e);
                        bf = null;
                    }

                    if (bf != null) {
                        retVal.addBotForce(bf, c);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioDeploymentLimit")) {
                    retVal.deploymentLimit =  ScenarioDeploymentLimit.generateInstanceFromXML(wn2, c, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("usingFixedMap")) {
                    retVal.setUsingFixedMap(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("terrainType")) {
                    retVal.terrainType = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mapSize")) {
                    String[] xy = wn2.getTextContent().split(",");
                    retVal.mapSizeX = Integer.parseInt(xy[0]);
                    retVal.mapSizeY = Integer.parseInt(xy[1]);
                } else if (wn2.getNodeName().equalsIgnoreCase("map")) {
                    retVal.map = wn2.getTextContent().trim();
                }  else if (wn2.getNodeName().equalsIgnoreCase("start")) {
                    retVal.start = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("light")) {
                    retVal.light = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("weather")) {
                    retVal.weather = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("wind")) {
                    retVal.wind = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("fog")) {
                    retVal.fog = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("atmosphere")) {
                    retVal.atmosphere = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("temperature")) {
                    retVal.temperature = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("gravity")) {
                    retVal.gravity = Float.parseFloat(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("emi")) {
                    retVal.emi = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("blowingSand")) {
                    retVal.blowingSand = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("shiftWindDirection")) {
                    retVal.shiftWindDirection = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("shiftWindStrength")) {
                    retVal.shiftWindStrength = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("maxWindStrength")) {
                    retVal.maxWindStrength = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("minWindStrength")) {
                    retVal.minWindStrength = Integer.parseInt(wn2.getTextContent());
                }

            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        return retVal;
    }

    protected static List<String> getEntityStub(Node wn) {
        List<String> stub = new ArrayList<>();
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("entityStub")) {
                stub.add(MHQXMLUtility.unEscape(wn2.getTextContent()));
            }
        }
        return stub;
    }

    public List<Loot> getLoot() {
        return loots;
    }

    public void addLoot(Loot l) {
        loots.add(l);
    }

    public void resetLoot() {
        loots = new ArrayList<>();
    }

    public boolean isFriendlyUnit(Entity entity, Campaign campaign) {
        return getForces(campaign).getUnits().stream().
                anyMatch(unitID -> unitID.equals(UUID.fromString(entity.getExternalIdAsString())));
    }
}
