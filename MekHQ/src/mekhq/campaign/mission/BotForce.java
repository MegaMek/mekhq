package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

public class BotForce implements Serializable, MekHqXmlSerializable {
    /**
     *
     */
    private static final long serialVersionUID = 8259058549964342518L;

    private String name;
    private ArrayList<Entity> entityList;
    private int team;
    private int start;
    private String camoCategory;
    private String camoFileName;
    private int colorIndex;
    private BehaviorSettings behaviorSettings;

    public BotForce() {
        this.entityList = new ArrayList<>();
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            MekHQ.getLogger().log(getClass(), "BotForce()", LogLevel.ERROR, //$NON-NLS-1$
                    "Error getting Princess default behaviors"); //$NON-NLS-1$
            MekHQ.getLogger().error(getClass(), "BotForce()", ex); //$NON-NLS-1$
        }
    }

    public BotForce(String name, int team, int start, ArrayList<Entity> entityList) {
        this(name, team, start, start, entityList, Player.NO_CAMO, null, -1);
    }

    public BotForce(String name, int team, int start, int home, ArrayList<Entity> entityList) {
        this(name, team, start, home, entityList, Player.NO_CAMO, null, -1);
    }

    public BotForce(String name, int team, int start, int home, ArrayList<Entity> entityList,
            String camoCategory, String camoFileName, int colorIndex) {
        final String METHOD_NAME = "BotForce(String,int,int,int,ArrayList<Entity>,String,String,int)"; //$NON-NLS-1$
        this.name = name;
        this.team = team;
        this.start = start;
        // Filter all nulls out of the parameter entityList
        this.entityList = entityList.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        this.camoCategory = camoCategory;
        this.camoFileName = camoFileName;
        this.colorIndex = colorIndex;
        try {
            behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR.getCopy();
        } catch (PrincessException ex) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Error getting Princess default behaviors"); //$NON-NLS-1$
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex);
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

    public ArrayList<Entity> getEntityList() {
        return entityList;
    }

    public void setEntityList(ArrayList<Entity> entityList) {
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

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        colorIndex = index;
    }
    
    public int getTotalBV() {
        final String METHOD_NAME = "getTotalBV";

        int bv = 0;
        
        for(Entity entity : getEntityList()) {
            if (entity == null) {
                MekHQ.getLogger().error(BotForce.class, METHOD_NAME, "Null entity when calculating the BV a bot force, we should never find a null here. Please investigate");
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

    public void writeToXml(PrintWriter pw1, int indent) {
        final String METHOD_NAME = "writeToXml";
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "name", MekHqXmlUtil.escape(name));
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "team", team);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "start", start);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoCategory", MekHqXmlUtil.escape(camoCategory));
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoFileName", MekHqXmlUtil.escape(camoFileName));
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "colorIndex", colorIndex);

        pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<entities>");
        for (Entity en : entityList) {
            if (en == null) {
                MekHQ.getLogger().error(BotForce.class, METHOD_NAME, "Null entity when saving a bot force, we should never find a null here. Please investigate");
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
        final String METHOD_NAME = "setFieldsFromXmlNode(Node)"; //$NON-NLS-1$
        
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
            } else if (wn2.getNodeName().equalsIgnoreCase("colorIndex")) {
                colorIndex = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("entities")) {
                NodeList nl2 = wn2.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn3 = nl2.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                        Entity en = null;
                        try {
                            en = MekHqXmlUtil.getEntityFromXmlString(wn3);
                            if (wn3.getAttributes().getNamedItem("deployment") != null) {
                                en.setDeployRound(Math.max(0,
                                        Integer.parseInt(wn3.getAttributes().getNamedItem("deployment").getTextContent())));
                            }
                        } catch (Exception e) {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                    "Error loading allied unit in scenario"); //$NON-NLS-1$
                            MekHQ.getLogger().error(getClass(), METHOD_NAME, e);
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
