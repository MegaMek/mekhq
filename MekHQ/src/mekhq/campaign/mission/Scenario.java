/*
 * Scenario.java
 *
 * Copyright (C) 2011-2016 - The MegaMek Team. All Rights Reserved.
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mekhq.campaign.mission.enums.ScenarioStatus;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.atb.IAtBScenario;
import mekhq.campaign.unit.Unit;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Scenario implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -2193761569359938090L;

    public static final int S_DEFAULT_ID = -1;

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

    //Stores combinations of units and the transports they are assigned to
    private Map<UUID, List<UUID>> playerTransportLinkages;
    //endregion Variable Declarations

    public Scenario() {
        this(null);
    }

    public Scenario(String n) {
        this.name = n;
        this.desc = "";
        this.report = "";
        setStatus(ScenarioStatus.CURRENT);
        this.date = null;
        this.subForceIds = new ArrayList<>();
        this.unitIds = new ArrayList<>();
        this.loots = new ArrayList<>();
        this.scenarioObjectives = new ArrayList<>();
        this.playerTransportLinkages = new HashMap<>();
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

    public void setDate(LocalDate d) {
        this.date = d;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean hasObjectives() {
        return scenarioObjectives != null &&
                scenarioObjectives.size() > 0;
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
    }

    public ForceStub getForceStub() {
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

    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<scenario id=\""
                +id
                +"\" type=\""
                +this.getClass().getName()
                +"\">");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<name>"
                +MekHqXmlUtil.escape(getName())
                +"</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<desc>"
                +MekHqXmlUtil.escape(desc)
                +"</desc>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<report>"
                +MekHqXmlUtil.escape(report)
                +"</report>");
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "status", getStatus().name());
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<id>"
                +id
                +"</id>");
        if (null != stub) {
            stub.writeToXml(pw1, indent+1);
        } else {
            // only bother writing out objectives for active scenarios
            if (hasObjectives()) {
                for (ScenarioObjective objective : this.scenarioObjectives) {
                    objective.Serialize(pw1);
                }
            }
        }
        if ((loots.size() > 0) && getStatus().isCurrent()) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<loots>");
            for (Loot l : loots) {
                l.writeToXml(pw1, indent+2);
            }
            pw1.println(MekHqXmlUtil.indentStr(indent+1)+"</loots>");
        }
        if (null != date) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "date", MekHqXmlUtil.saveFormattedDate(date));
        }
        
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "cloaked", isCloaked());
    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</scenario>");
    }

    protected void loadFieldsFromXmlNode(Node wn) throws ParseException {
        //do nothing
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
                    MekHQ.getLogger().error("Unable to load an old AtBScenario because we could not determine the battle type");
                    return null;
                }

                List<Class<IAtBScenario>> scenarioClassList = AtBScenarioFactory.getScenarios(battleType);

                if ((null == scenarioClassList) || scenarioClassList.isEmpty()) {
                    MekHQ.getLogger().error("Unable to load an old AtBScenario of battle type " + battleType);
                    return null;
                }

                retVal = (Scenario) scenarioClassList.get(0).newInstance();
            } else {
                retVal = (Scenario) Class.forName(className).newInstance();
            }

            retVal.loadFieldsFromXmlNode(wn);
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
                    retVal.stub = ForceStub.generateInstanceFromXML(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    retVal.date = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("cloaked")) {
                    retVal.cloaked = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("loots")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        if (!wn3.getNodeName().equalsIgnoreCase("loot")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().error("Unknown node type not loaded in techUnitIds nodes: " + wn3.getNodeName());
                            continue;
                        }
                        Loot loot = Loot.generateInstanceFromXML(wn3, c, version);
                        retVal.loots.add(loot);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase(ScenarioObjective.ROOT_XML_ELEMENT_NAME)) {
                    retVal.getScenarioObjectives().add(ScenarioObjective.Deserialize(wn2));
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        return retVal;
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
