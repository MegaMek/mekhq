/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved
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
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.icons.Camouflage;
import mekhq.Utilities;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.io.migration.CamouflageMigrator;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class BotForce implements IPlayerSettings {
    private transient final UnitNameTracker nameTracker = new UnitNameTracker();
    private String name;
    private List<Entity> fixedEntityList;
    private List<Entity> generatedEntityList;
    private List<UUID> traitors;
    private int team;
    // deployment settings
    private int startingPos = Board.START_ANY;
    private int startOffset = 0;
    private int startWidth = 3;
    private int startingAnyNWx = Entity.STARTING_ANY_NONE;
    private int startingAnyNWy = Entity.STARTING_ANY_NONE;
    private int startingAnySEx = Entity.STARTING_ANY_NONE;
    private int startingAnySEy = Entity.STARTING_ANY_NONE;
    private int deployRound;
    private Camouflage camouflage = new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name());
    private PlayerColour colour = PlayerColour.BLUE;
    private BehaviorSettings behaviorSettings;
    private String templateName;
    private BotForceRandomizer bfRandomizer;

    public BotForce() {
        fixedEntityList = new ArrayList<>();
        generatedEntityList = new ArrayList<>();
        traitors = new ArrayList<>();
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            LogManager.getLogger().error("Error getting Princess default behaviors", ex);
        }
        bfRandomizer = null;
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
        this.startingPos = start;
        setFixedEntityList(entityList);
        setCamouflage(camouflage);
        setColour(colour);
        generatedEntityList = new ArrayList<>();
        traitors = new ArrayList<>();
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            LogManager.getLogger().error("Error getting Princess default behaviors", ex);
        }
        behaviorSettings.setRetreatEdge(CardinalEdge.NEAREST);
        behaviorSettings.setDestinationEdge(CardinalEdge.NONE);
    }

    @Override
    public BotForce clone() {
        final BotForce copy = new BotForce();
        copy.setName(this.getName());
        copy.setTeam(this.getTeam());
        copy.setStartingPos(this.getStartingPos());
        copy.setStartOffset(this.getStartOffset());
        copy.setStartWidth(this.getStartWidth());
        copy.setStartingAnyNWx(this.getStartingAnyNWx());
        copy.setStartingAnyNWy(this.getStartingAnyNWy());
        copy.setStartingAnySEx(this.getStartingAnySEx());
        copy.setStartingAnySEy(this.getStartingAnySEy());
        copy.setDeployRound(this.getDeployRound());
        copy.setCamouflage(this.getCamouflage().clone());
        copy.setColour(this.getColour());
        copy.setTemplateName(this.getTemplateName());
        if (this.getBotForceRandomizer() != null) {
            copy.setBotForceRandomizer(this.getBotForceRandomizer().clone());
        }
        // UUID is immutable so this should work
        copy.traitors = traitors.stream().collect(Collectors.toList());
        copy.setBehaviorSettings(new BehaviorSettings());
        copy.getBehaviorSettings().setAutoFlee(this.getBehaviorSettings().shouldAutoFlee());
        copy.getBehaviorSettings().setForcedWithdrawal(this.getBehaviorSettings().isForcedWithdrawal());
        copy.getBehaviorSettings().setDestinationEdge(this.getBehaviorSettings().getDestinationEdge());
        copy.getBehaviorSettings().setRetreatEdge(this.getBehaviorSettings().getRetreatEdge());
        copy.getBehaviorSettings().setBraveryIndex(this.getBehaviorSettings().getBraveryIndex());
        copy.getBehaviorSettings().setFallShameIndex(this.getBehaviorSettings().getFallShameIndex());
        copy.getBehaviorSettings().setHyperAggressionIndex(this.getBehaviorSettings().getHyperAggressionIndex());
        copy.getBehaviorSettings().setSelfPreservationIndex(this.getBehaviorSettings().getSelfPreservationIndex());
        copy.getBehaviorSettings().setHerdMentalityIndex(this.getBehaviorSettings().getHerdMentalityIndex());
        // this bit of trickery seems to work to make a proper copy of the entity list
        copy.fixedEntityList = this.getFixedEntityListDirect().stream().collect(Collectors.toList());

        return copy;
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

    public List<Entity> getFixedEntityList() {
        return Collections.unmodifiableList(getFixedEntityListDirect());
    }

    public List<Entity> getFixedEntityListDirect() {
        return fixedEntityList;
    }

    public void addEntity(Entity entity) {
        fixedEntityList.add(entity);
    }

    public boolean removeEntity(int index) {
        Entity e = null;
        if ((index >= 0) && (index < fixedEntityList.size())) {
            e = fixedEntityList.remove(index);
            nameTracker.remove(e, updated -> {
                updated.generateShortName();
                updated.generateDisplayName();
            });
        }

        return e != null;
    }

    public void setFixedEntityList(List<Entity> entityList) {
        nameTracker.clear();

        List<Entity> entities = new ArrayList<>();
        for (Entity e : entityList) {
            if (e != null) {
                nameTracker.add(e);
                entities.add(e);
            }
        }

        this.fixedEntityList = entities;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    @Override
    public int getStartingPos() {
        return startingPos;
    }

    @Override
    public void setStartingPos(int start) {
        this.startingPos = start;
    }

    @Override
    public int getStartOffset() { return startOffset; }

    @Override
    public void setStartOffset(int startOffset) { this.startOffset = startOffset; }

    @Override
    public int getStartWidth() { return startWidth; }

    @Override
    public void setStartWidth(int startWidth) { this.startWidth = startWidth; }

    @Override
    public int getStartingAnyNWx() { return startingAnyNWx; }

    @Override
    public void setStartingAnyNWx(int startingAnyNWx) { this.startingAnyNWx = startingAnyNWx; }

    @Override
    public int getStartingAnyNWy() { return startingAnyNWy; }

    @Override
    public void setStartingAnyNWy(int startingAnyNWy) { this.startingAnyNWy = startingAnyNWy; }

    @Override
    public int getStartingAnySEx() { return startingAnySEx; }

    @Override
    public void setStartingAnySEx(int startingAnySEx) { this.startingAnySEx = startingAnySEx; }

    @Override
    public int getStartingAnySEy() { return startingAnySEy; }

    @Override
    public void setStartingAnySEy(int startingAnySEy) { this.startingAnySEy = startingAnySEy; }

    public int getDeployRound() { return deployRound; }

    public void setDeployRound(int round) { this.deployRound = round; }

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

    public List<Entity> getFullEntityList(Campaign c) {
        List<Entity> fullEntities = new ArrayList<>();
        fullEntities.addAll(fixedEntityList);
        fullEntities.addAll(generatedEntityList);
        fullEntities.addAll(getTraitorEntities(c));
        return fullEntities;
    }

    public int getTotalBV(Campaign c) {
        int bv = 0;

        for (Entity entity : getFullEntityList(c)) {
            if (entity == null) {
                LogManager.getLogger().error("Null entity when calculating the BV a bot force, we should never find a null here. Please investigate");
            } else {
                bv += entity.calculateBattleValue(true, false);
            }
        }
        return bv;
    }

    public int getFixedBV() {
        int bv = 0;

        for (Entity entity : getFixedEntityList()) {
            if (entity == null) {
                LogManager.getLogger().error("Null entity when calculating the BV a bot force, we should never find a null here. Please investigate");
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

    public void setBotForceRandomizer(BotForceRandomizer randomizer) { this.bfRandomizer = randomizer; }

    public BotForceRandomizer getBotForceRandomizer() { return bfRandomizer; }

    public void generateRandomForces(List<Unit> playerUnits, Campaign c) {
        if (null == bfRandomizer) {
            return;
        }
        // reset the generated units
        generatedEntityList = new ArrayList<>();
        //get existing units
        List<Entity> existingEntityList = new ArrayList<>();
        existingEntityList.addAll(fixedEntityList);
        existingEntityList.addAll(getTraitorEntities(c));
        generatedEntityList = bfRandomizer.generateForce(playerUnits, existingEntityList, c);
    }

    public List<UUID> getTraitorPersons() {
        return traitors;
    }

    /**
     * Turn traitor UUIDs into an entity list by checking for associated units
     * @return a List of Entities associated with the traitor personnel UUIDs
     */
    public List<Entity> getTraitorEntities(Campaign campaign) {
        List<Entity> traitorEntities = new ArrayList<>();
        for (UUID traitor : traitors) {
            Person p = campaign.getPerson(traitor);
            if ((null != p) && (null != p.getUnit()) && (null != p.getUnit().getEntity())) {
                traitorEntities.add(p.getUnit().getEntity());
            }
        }
        return traitorEntities;
    }

    /**
     * Turn traitor UUIDs into a Unit list by checking for associated units
     * @return a List of Units associated with the traitor personnel UUIDs
     */
    public List<Unit> getTraitorUnits(Campaign campaign) {
        List<Unit> traitorUnits = new ArrayList<>();
        for (UUID traitor : traitors) {
            Person p = campaign.getPerson(traitor);
            if ((null != p) && (null != p.getUnit())) {
                traitorUnits.add(p.getUnit());
            }
        }
        return traitorUnits;
    }

    /**
     * Checks to see if a given unit has a crew member among the traitor personnel IDs. This is used
     * primarily to determine if a unit can be deployed to a scenario.
     * @param unit
     * @return a boolean indicating whether this unit is a traitor
     */
    public boolean isTraitor(Unit unit) {
        for (Person p : unit.getActiveCrew()) {
            if (traitors.contains(p.getId())) {
                return true;
            }
        }
        return false;
    }

    public BotForceStub generateStub(Campaign c) {
        List<String> stubs = Utilities.generateEntityStub(getFullEntityList(c));
        return new BotForceStub("<html>" +
                getName() + " <i>" +
                ((getTeam() == 1) ? "Allied" : "Enemy") + "</i>" +
                " Start: " + IStartingPositions.START_LOCATION_NAMES[getStartingPos()] +
                " Fixed BV: " + getTotalBV(c) +
                ((null == getBotForceRandomizer()) ? "" : "<br>Random: " + getBotForceRandomizer().getDescription(c)) +
                "</html>", stubs);
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "botForce");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "team", team);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingPos", startingPos);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startOffset", startOffset);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startWidth", startWidth);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingAnyNWx", startingAnyNWx);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingAnyNWy", startingAnyNWy);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingAnySEx", startingAnySEx);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingAnySEy", startingAnySEy);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "deployRound", deployRound);
        getCamouflage().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "colour", getColour().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "templateName", templateName);
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "entities");
        try {
            EntityListFile.writeEntityList(pw, (ArrayList<Entity>) getFixedEntityListDirect());
        } catch (IOException ex) {
            LogManager.getLogger().error("Error loading entities for BotForce " + this.getName(), ex);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "entities");

        for (UUID traitor : traitors) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "traitor", traitor);
        }

        if (null != bfRandomizer) {
            bfRandomizer.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "behaviorSettings");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forcedWithdrawal", behaviorSettings.isForcedWithdrawal());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoFlee", behaviorSettings.shouldAutoFlee());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "selfPreservationIndex", behaviorSettings.getSelfPreservationIndex());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fallShameIndex", behaviorSettings.getFallShameIndex());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hyperAggressionIndex", behaviorSettings.getHyperAggressionIndex());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destinationEdge", behaviorSettings.getDestinationEdge().ordinal());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "retreatEdge", behaviorSettings.getRetreatEdge().ordinal());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "herdMentalityIndex", behaviorSettings.getHerdMentalityIndex());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "braveryIndex", behaviorSettings.getBraveryIndex());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "behaviorSettings");
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "botForce");
    }

    public void setFieldsFromXmlNode(final Node wn, final Version version, final Campaign campaign) {
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    name = MHQXMLUtility.unEscape(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("team")) {
                    team = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("start") // Legacy - 0.49.19 removal
                        || wn2.getNodeName().equalsIgnoreCase("startingPos")) {
                    startingPos = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startOffset")) {
                    startOffset = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startWidth")) {
                    startWidth = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startingAnyNWx")) {
                    startingAnyNWx = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startingAnyNWy")) {
                    startingAnyNWy = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startingAnySEx")) {
                    startingAnySEx = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startingAnySEy")) {
                    startingAnySEy = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("deployRound")) {
                    deployRound = Integer.parseInt(wn2.getTextContent());
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
                } else if (wn2.getNodeName().equalsIgnoreCase("traitor")) {
                    traitors.add(UUID.fromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("botForceRandomizer")) {
                    BotForceRandomizer bfRandomizer = BotForceRandomizer.generateInstanceFromXML(wn2, campaign, version);
                    setBotForceRandomizer(bfRandomizer);
                } else if (wn2.getNodeName().equalsIgnoreCase("entities")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                            Entity en = null;
                            try {
                                en = MHQXMLUtility.parseSingleEntityMul((Element) wn3, campaign);
                            } catch (Exception ex) {
                                LogManager.getLogger().error("Error loading allied unit in scenario", ex);
                            }

                            if (en != null) {
                                fixedEntityList.add(en);
                            }
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("behaviorSettings")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("forcedWithdrawal")) {
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
                LogManager.getLogger().error("", e);
            }
        }

        if (version.isLowerThan("0.49.3")) {
            CamouflageMigrator.migrateCamouflage(version, getCamouflage());
        }
    }
}
