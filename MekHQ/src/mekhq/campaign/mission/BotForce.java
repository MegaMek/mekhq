/*
 * Copyright (C) 2018-2021 - The MegaMek Team. All Rights Reserved
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.icons.Camouflage;
import megamek.common.logging.LogLevel;
import megamek.Version;
import mekhq.campaign.io.Migration.CamouflageMigrator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.UnitNameTracker;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

public class BotForce implements Serializable, MekHqXmlSerializable {
    private static final long serialVersionUID = 8259058549964342518L;

    private transient final UnitNameTracker nameTracker = new UnitNameTracker();
    private String name;
    private List<Entity> entityList;
    private int team;
    private int start;
    private Camouflage camouflage = new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name());
    private PlayerColour colour = PlayerColour.BLUE;
    private BehaviorSettings behaviorSettings;
    private String templateName;

    public BotForce() {
        entityList = new ArrayList<>();
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            MekHQ.getLogger().error("Error getting Princess default behaviors", ex);
        }
    }

    public BotForce(String name, int team, int start, List<Entity> entityList) {
        this(name, team, start, start, entityList,
                new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name()),
                PlayerColour.BLUE);
    }

    public BotForce(String name, int team, int start, int home, List<Entity> entityList) {
        this(name, team, start, home, entityList,
                new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name()),
                PlayerColour.BLUE);
    }

    public BotForce(String name, int team, int start, int home, List<Entity> entityList,
                    Camouflage camouflage, PlayerColour colour) {
        this.name = name;
        this.team = team;
        this.start = start;
        setEntityList(entityList);
        setCamouflage(camouflage);
        setColour(colour);
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            MekHQ.getLogger().error("Error getting Princess default behaviors", ex);
        }
        behaviorSettings.setRetreatEdge(CardinalEdge.NEAREST);
        behaviorSettings.setDestinationEdge(CardinalEdge.NONE);
    }

    /* Convert from MM's Board to Princess's HomeEdge */
    public CardinalEdge findCardinalEdge(int start) {
        switch (start) {
            case Board.START_N:
                return CardinalEdge.NORTH;
            case Board.START_S:
                return CardinalEdge.SOUTH;
            case Board.START_E:
                return CardinalEdge.EAST;
            case Board.START_W:
                return CardinalEdge.WEST;
            case Board.START_NW:
                return (Compute.randomInt(2) == 0) ? CardinalEdge.NORTH : CardinalEdge.WEST;
            case Board.START_NE:
                return (Compute.randomInt(2) == 0) ? CardinalEdge.NORTH : CardinalEdge.EAST;
            case Board.START_SW:
                return (Compute.randomInt(2) == 0) ? CardinalEdge.SOUTH : CardinalEdge.WEST;
            case Board.START_SE:
                return (Compute.randomInt(2) == 0) ? CardinalEdge.SOUTH : CardinalEdge.EAST;
            case Board.START_ANY:
                return CardinalEdge.getCardinalEdge(Compute.randomInt(4));
            default:
                return CardinalEdge.NONE;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Entity> getEntityList() {
        return Collections.unmodifiableList(entityList);
    }

    public void addEntity(Entity entity) {
        entityList.add(entity);
    }

    public boolean removeEntity(int index) {
        Entity e = null;
        if ((index >= 0) && (index < entityList.size())) {
            e = entityList.remove(index);
            nameTracker.remove(e, updated -> {
                updated.generateShortName();
                updated.generateDisplayName();
            });
        }

        return e != null;
    }

    public void setEntityList(List<Entity> entityList) {
        nameTracker.clear();

        List<Entity> entities = new ArrayList<>();
        for (Entity e : entityList) {
            if (e != null) {
                nameTracker.add(e);
                entities.add(e);
            }
        }

        this.entityList = entities;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public void setCamouflage(Camouflage camouflage) {
        this.camouflage = Objects.requireNonNull(camouflage);
    }
    public PlayerColour getColour() {
        return colour;
    }

    public void setColour(PlayerColour colour) {
        this.colour = Objects.requireNonNull(colour, "Colour cannot be set to null");
    }

    public int getTotalBV() {
        int bv = 0;

        for (Entity entity : getEntityList()) {
            if (entity == null) {
                MekHQ.getLogger().error("Null entity when calculating the BV a bot force, we should never find a null here. Please investigate");
            } else {
                bv += entity.calculateBattleValue(true, false);
            }
        }

        return bv;
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public void setBehaviorSettings(BehaviorSettings behaviorSettings) {
        this.behaviorSettings = behaviorSettings;
    }

    public void setDestinationEdge(int i) {
        behaviorSettings.setDestinationEdge(findCardinalEdge(i));
    }

    public void setRetreatEdge(int i) {
        behaviorSettings.setRetreatEdge(findCardinalEdge(i));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent++, "name", name);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "team", team);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "start", start);
        getCamouflage().writeToXML(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "colour", getColour().name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "templateName", templateName);
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "entities");
        for (Entity en : entityList) {
            if (en == null) {
                MekHQ.getLogger().error("Null entity when saving a bot force, we should never find a null here. Please investigate");
            } else {
                pw1.println(AtBScenario.writeEntityWithCrewToXmlString(en, indent, entityList));
            }
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "entities");
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "behaviorSettings");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "verbosity", behaviorSettings.getVerbosity().toString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "forcedWithdrawal", behaviorSettings.isForcedWithdrawal());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "autoFlee", behaviorSettings.shouldAutoFlee());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "selfPreservationIndex", behaviorSettings.getSelfPreservationIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "fallShameIndex", behaviorSettings.getFallShameIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "hyperAggressionIndex", behaviorSettings.getHyperAggressionIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "destinationEdge", behaviorSettings.getDestinationEdge().ordinal());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "retreatEdge", behaviorSettings.getRetreatEdge().ordinal());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "herdMentalityIndex", behaviorSettings.getHerdMentalityIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "braveryIndex", behaviorSettings.getBraveryIndex());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "behaviorSettings");
    }

    public void setFieldsFromXmlNode(final Node wn, final Version version) {
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    name = MekHqXmlUtil.unEscape(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("team")) {
                    team = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("start")) {
                    start = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase(Camouflage.XML_TAG)) {
                    setCamouflage(Camouflage.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("camoCategory")) { // Legacy - 0.49.3 removal
                    getCamouflage().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("camoFileName")) { // Legacy - 0.49.3 removal
                    getCamouflage().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("colour")) {
                    setColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("colorIndex")) { // Legacy - 0.47.15 removal
                    setColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                    if (Camouflage.NO_CAMOUFLAGE.equals(getCamouflage().getCategory())) {
                        getCamouflage().setCategory(Camouflage.COLOUR_CAMOUFLAGE);
                        getCamouflage().setFilename(getColour().name());
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("templateName")) {
                    setTemplateName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("entities")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                            Entity en = null;
                            try {
                                en = MekHqXmlUtil.parseSingleEntityMul((Element) wn3);
                            } catch (Exception e) {
                                MekHQ.getLogger().error("Error loading allied unit in scenario", e);
                            }

                            if (en != null) {
                                entityList.add(en);
                            }
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("behaviorSettings")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("verbosity")) {
                            behaviorSettings.setVerbosity(LogLevel.getLogLevel(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("forcedWithdrawal")) {
                            behaviorSettings.setForcedWithdrawal(Boolean.parseBoolean(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("autoFlee")) {
                            behaviorSettings.setAutoFlee(Boolean.parseBoolean(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("selfPreservationIndex")) {
                            behaviorSettings.setSelfPreservationIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("fallShameIndex")) {
                            behaviorSettings.setFallShameIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("hyperAggressionIndex")) {
                            behaviorSettings.setHyperAggressionIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("destinationEdge")) {
                            behaviorSettings.setDestinationEdge(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("retreatEdge")) {
                            behaviorSettings.setRetreatEdge(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("herdMentalityIndex")) {
                            behaviorSettings.setHerdMentalityIndex(Integer.parseInt(wn3.getTextContent()));
                        } else if (wn3.getNodeName().equalsIgnoreCase("braveryIndex")) {
                            behaviorSettings.setBraveryIndex(Integer.parseInt(wn3.getTextContent()));
                        }
                    }
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }

        if (version.isLowerThan("0.49.3")) {
            CamouflageMigrator.migrateCamouflage(version, getCamouflage());
        }
    }
}
