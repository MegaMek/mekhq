/*
 * Copyright (C) 2018, 2020 - The MegaMek Team. All Rights Reserved
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.icons.Camouflage;
import megamek.common.logging.LogLevel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

public class BotForce implements Serializable, MekHqXmlSerializable {
    private static final long serialVersionUID = 8259058549964342518L;

    private String name;
    private List<Entity> entityList;
    private int team;
    private int start;
    private String camoCategory;
    private String camoFileName;
    private PlayerColour colour;
    private BehaviorSettings behaviorSettings;

    public BotForce() {
        this.entityList = new ArrayList<>();
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            MekHQ.getLogger().error("Error getting Princess default behaviors", ex);
        }
    }

    public BotForce(String name, int team, int start, List<Entity> entityList) {
        this(name, team, start, start, entityList, Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name(), PlayerColour.BLUE);
    }

    public BotForce(String name, int team, int start, int home, List<Entity> entityList) {
        this(name, team, start, home, entityList, Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name(), PlayerColour.BLUE);
    }

    public BotForce(String name, int team, int start, int home, List<Entity> entityList,
                    String camoCategory, String camoFileName, PlayerColour colour) {
        this.name = name;
        this.team = team;
        this.start = start;
        // Filter all nulls out of the parameter entityList
        this.entityList = entityList.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        this.camoCategory = camoCategory;
        this.camoFileName = camoFileName;
        this.colour = colour;
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            MekHQ.getLogger().error("Error getting Princess default behaviors", ex);
        }
        behaviorSettings.setRetreatEdge(CardinalEdge.NEAREST_OR_NONE);
        behaviorSettings.setDestinationEdge(CardinalEdge.NEAREST_OR_NONE);
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
            return CardinalEdge.NEAREST_OR_NONE;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Entity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<Entity> entityList) {
        // Filter all nulls out of the parameter entityList
        this.entityList = entityList.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
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

    public Camouflage getCamouflage() {
        return new Camouflage(getCamoCategory(), getCamoFileName());
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoCategory(String camoCategory) {
        this.camoCategory = camoCategory;
    }

    public String getCamoFileName() {
        return camoFileName;
    }

    public void setCamoFileName(String camoFileName) {
        this.camoFileName = camoFileName;
    }

    public PlayerColour getColour() {
        return colour;
    }

    public void setColour(PlayerColour colour) {
        Objects.requireNonNull(colour, "Colour cannot be set to null");
        this.colour = colour;
        if (getCamouflage().isColourCamouflage()) {
            setCamoFileName(colour.name());
        }
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
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "name", name);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "team", team);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "start", start);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoCategory", camoCategory);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoFileName", camoFileName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "colour", getColour().name());

        pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<entities>");
        for (Entity en : entityList) {
            if (en == null) {
                MekHQ.getLogger().error("Null entity when saving a bot force, we should never find a null here. Please investigate");
            } else {
                pw1.println(AtBScenario.writeEntityWithCrewToXmlString(en, indent + 2, entityList));
            }
        }
        pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</entities>");

        pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<behaviorSettings>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "verbosity", behaviorSettings.getVerbosity().toString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "forcedWithdrawal", behaviorSettings.isForcedWithdrawal());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "autoFlee", behaviorSettings.shouldAutoFlee());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "selfPreservationIndex", behaviorSettings.getSelfPreservationIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "fallShameIndex", behaviorSettings.getFallShameIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "hyperAggressionIndex", behaviorSettings.getHyperAggressionIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "destinationEdge", behaviorSettings.getDestinationEdge().ordinal());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "retreatEdge", behaviorSettings.getRetreatEdge().ordinal());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "herdMentalityIndex", behaviorSettings.getHerdMentalityIndex());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2, "braveryIndex", behaviorSettings.getBraveryIndex());
        pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</behaviorSettings>");
    }

    public void setFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("name")) {
                name = MekHqXmlUtil.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("team")) {
                team = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("start")) {
                start = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("camoCategory")) {
                camoCategory = MekHqXmlUtil.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("camoFileName")) {
                camoFileName = MekHqXmlUtil.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("colour")) {
                setColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("colorIndex")) { // Legacy - 0.47.15 removal
                setColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                if (Camouflage.NO_CAMOUFLAGE.equals(getCamoCategory())) {
                    setCamoCategory(Camouflage.COLOUR_CAMOUFLAGE);
                    setCamoFileName(getColour().name());
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("entities")) {
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn3 = nl2.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                        Entity en = null;
                        try {
                            en = MekHqXmlUtil.getEntityFromXmlString(wn3);
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
        }
    }
}
