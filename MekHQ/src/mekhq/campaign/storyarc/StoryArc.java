/*
 * StoryArc.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.storyarc;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import mekhq.*;
import mekhq.campaign.event.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.enums.StoryLoadingType;
import mekhq.campaign.storyarc.storypoint.*;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Story Arc class manages a given story arc campaign
 */
public class StoryArc implements MekHqXmlSerializable {

    private String title;
    private String details;
    private String description;

    private Campaign campaign;

    /** What type of story arc **/
    private StoryLoadingType storyLoadingType;

    /** A UUID for the initial StoryPoint in this track  - can be null **/
    private UUID startingPointId;

    /** A hash of all possible StoryPoints in this StoryArc, referenced by UUID **/
    private Map<UUID, StoryPoint> storyPoints;

    /** A hash of possible personalities that the player might interact with in this story arc **/
    private Map<UUID, Personality> personalities;

    /**
     * a hash of custom string variables that the creator might specify with a string key
     */
    private Map<String, String> customStringVariables;

    /** directory path to the initial campaign data for this StoryArc - can be null **/
    private String initCampaignPath;

    /** directory path to this story arc **/
    private String directoryPath;

    /** a hash map of replacements for tokens in the narrative strings */
    private static Map<String, String> replacementTokens;

    public StoryArc() {
        storyLoadingType = StoryLoadingType.BOTH;
        storyPoints =  new LinkedHashMap<>();
        personalities = new LinkedHashMap<>();
        customStringVariables = new LinkedHashMap<>();
    }

    public void setCampaign(Campaign c) { this.campaign = c; }

    public Campaign getCampaign() { return campaign; }

    private void setTitle(String t) { this.title = t; }

    public String getTitle() { return this.title; }

    public String getDetails() { return details; }

    private void setDetails(String d) { this.details = d; }

    public String getDescription() { return this.description; }

    private void setDescription(String d) { this.description = d; }

    private void setStoryLoadingType(StoryLoadingType type) { this.storyLoadingType = type; }

    public StoryLoadingType getStoryLoadingType() { return storyLoadingType; }

    private void setStartingPointId(UUID u) { this.startingPointId = u; }

    private UUID getStartingPointId() { return startingPointId; }

    public void setInitCampaignPath(String s) { this.initCampaignPath =s; }

    public File getInitCampaignFile() {
        if(null == initCampaignPath) {
            return null;
        }
        return new File(initCampaignPath);
    }

    public void setDirectoryPath(String p) { this.directoryPath = p; }

    public String getDirectoryPath() { return directoryPath; }

    public StoryPoint getStoryPoint(UUID id) {
        if (id == null) {
            return null;
        }
        return storyPoints.get(id);
    }

    public Personality getPersonality(UUID id) {
        if (id == null) {
            return null;
        }
        Personality p = personalities.get(id);
        p.updatePersonalityFromCampaign(campaign);
        return p;
    }

    public void addCustomStringVariable(String key, String value) {
        customStringVariables.put(key, value);
    }

    public String getCustomStringVariable(String key) {
        return customStringVariables.get(key);
    }

    public void begin() {
        MekHQ.registerHandler(this);
        getStoryPoint(getStartingPointId()).start();
    }

    public void initializeDataDirectories() {
        MHQStaticDirectoryManager.initializeUserStoryPortraits(getDirectoryPath() + "/data/images/portraits");
        MHQStaticDirectoryManager.initializeUserStorySplash(getDirectoryPath() + "/data/images/storysplash");

    }

    private ScenarioStoryPoint findStoryPointByScenarioId(int scenarioId) {
        for (Map.Entry<UUID, StoryPoint> entry : storyPoints.entrySet()) {
            if (entry.getValue() instanceof ScenarioStoryPoint) {
                ScenarioStoryPoint storyPoint = (ScenarioStoryPoint) entry.getValue();
                if (null != storyPoint.getScenario() && storyPoint.getScenario().getId() == scenarioId) {
                    return storyPoint;
                }
            }
        }
        return null;
    }

    public List<String> getCurrentObjectives() {
        ArrayList<String> currentObjectives = new ArrayList<>();
        for (Map.Entry<UUID, StoryPoint> entry : storyPoints.entrySet()) {
            if (entry.getValue().isActive()) {
                String objective = entry.getValue().getObjective();
                if (!objective.isEmpty()) {
                    currentObjectives.add(objective);
                }
            }
        }
        return currentObjectives;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    //region EventHandlers
    @Subscribe
    public void handleScenarioResolved(ScenarioResolvedEvent ev) {
        // search through ScenarioStoryPoints for a match and if so complete it
        ScenarioStoryPoint storyPoint = findStoryPointByScenarioId(ev.getScenario().getId());
        if(null != storyPoint && storyPoint.isActive()) {
            storyPoint.complete();
        }
    }

    @Subscribe
    public void handleTransitComplete(TransitCompleteEvent ev) {
        //search through StoryPoints for a matching TravelStoryPoint
        TravelStoryPoint storyPoint;
        for (Map.Entry<UUID, StoryPoint> entry : storyPoints.entrySet()) {
            if (entry.getValue() instanceof TravelStoryPoint) {
                 storyPoint = (TravelStoryPoint) entry.getValue();
                 if(ev.getLocation().getCurrentSystem().getId().equals(storyPoint.getDestinationId()) &&
                         storyPoint.isActive()) {
                     storyPoint.complete();
                     break;
                }

            }
        }
    }

    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        //search through StoryPoints for a matching DateReachedStoryPoint
        DateReachedStoryPoint storyPoint;
        for (Map.Entry<UUID, StoryPoint> entry : storyPoints.entrySet()) {
            if (entry.getValue() instanceof DateReachedStoryPoint) {
                storyPoint = (DateReachedStoryPoint) entry.getValue();
                if (null != storyPoint.getDate() && ev.getCampaign().getLocalDate().equals(storyPoint.getDate())) {
                    storyPoint.start();
                    break;
                }

            }
        }
    }

    @Subscribe
    public void handlePersonChanged(PersonStatusChangedEvent ev) {
        Person p = ev.getPerson();
        if (null != p) {
            PersonStatusStoryPoint storyPoint;
            for (Map.Entry<UUID, StoryPoint> entry : storyPoints.entrySet()) {
                if (entry.getValue() instanceof PersonStatusStoryPoint) {
                    storyPoint = (PersonStatusStoryPoint) entry.getValue();
                    // is this the right person?
                    if (p.getId().equals(storyPoint.getPersonId())) {
                        // is their current status a trigger for this story point?
                        if (storyPoint.getStatusConditions().contains(p.getStatus())) {
                            storyPoint.start();
                        }
                        // either way we break to avoid further unnecessary processing
                        break;
                    }
                }
            }
        }
    }
    //endregion EventHandlers

    //region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "storyArc");
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "title", title);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "details", details);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "description", description);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "storyLoadingType", storyLoadingType.name());
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "startingPointId", startingPointId);
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "directoryPath", directoryPath);
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "storyPoints");
        for (Map.Entry<UUID, StoryPoint> entry : storyPoints.entrySet()) {
            entry.getValue().writeToXml(pw1, indent);
        }
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "storyPoints");
        if(!personalities.isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "personalities");
            for (Map.Entry<UUID, Personality> entry : personalities.entrySet()) {
                entry.getValue().writeToXml(pw1, indent);
            }
            MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "personalities");
        }
        if(!customStringVariables.isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "customStringVariables");
            for (Map.Entry<String, String> entry : customStringVariables.entrySet()) {
                MekHqXmlUtil.writeSimpleXMLOpenTag(pw1, indent++, "customStringVariable");
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "key", entry.getKey());
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "value", entry.getValue());
                MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "customStringVariable");
            }
            MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "customStringVariables");
        }
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw1, --indent, "storyArc");
    }

    protected void parseStoryPoints(NodeList nl, Campaign c, Version version) {
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE ||
                        !wn.getNodeName().equals("storyPoint")) {
                    continue;
                }
                StoryPoint storyPoint = StoryPoint.generateInstanceFromXML(wn, c, version);
                if(null != storyPoint) {
                    storyPoint.setStoryArc(this);
                    storyPoints.put(storyPoint.getId(), storyPoint);
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
        }
    }

    protected void parsePersonalities(NodeList nl, Campaign c) {
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE ||
                        !wn.getNodeName().equals("personality")) {
                    continue;
                }
                Personality personality = Personality.generateInstanceFromXML(wn, c);
                if(null != personality) {
                    personalities.put(personality.getId(), personality);
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
        }
    }

    protected void parseCustomStringVariables(NodeList nl, Campaign c) {
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE ||
                        !wn.getNodeName().equals("customStringVariable")) {
                    continue;
                }
                parseCustomStringVariable(wn.getChildNodes(), c);
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
        }
    }

    protected void parseCustomStringVariable(NodeList nl, Campaign c) {
        String key = null;
        String value = null;
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeName().equals("key")) {
                    key = wn.getTextContent().trim();
                } else if (wn.getNodeName().equals("value")) {
                    value = wn.getTextContent().trim();
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
        }
        if(null != key && null != value) {
            addCustomStringVariable(key, value);
        }
    }

    public static @Nullable StoryArc parseFromXML(final NodeList nl, Campaign c, Version version) {
        final StoryArc storyArc = new StoryArc();
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                switch (wn.getNodeName()) {
                    case "title":
                        storyArc.setTitle(wn.getTextContent().trim());
                        break;
                    case "details":
                        storyArc.setDetails(wn.getTextContent().trim());
                        break;
                    case "description":
                        storyArc.setDescription(wn.getTextContent().trim());
                        break;
                    case "storyLoadingType":
                        storyArc.setStoryLoadingType(StoryLoadingType.valueOf(wn.getTextContent().trim()));
                        break;
                    case "startingPointId":
                        storyArc.setStartingPointId(UUID.fromString(wn.getTextContent().trim()));
                        break;
                    case "directoryPath":
                        storyArc.setDirectoryPath(wn.getTextContent().trim());
                        break;
                    case "storyPoints":
                        storyArc.parseStoryPoints(wn.getChildNodes(), c, version);
                        break;
                    case "personalities":
                        storyArc.parsePersonalities(wn.getChildNodes(), c);
                        break;
                    case "customStringVariables":
                        storyArc.parseCustomStringVariables(wn.getChildNodes(), c);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
            return null;
        }
        return storyArc;
    }

    public static @Nullable StoryArc parseFromFile(final @Nullable File file, Campaign c) {
        final Document xmlDoc;
        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MekHqXmlUtil.newSafeDocumentBuilder().parse(is);
        } catch (Exception e) {
            LogManager.getLogger().error(e);
            return null;
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();

        NodeList nl = element.getChildNodes();

        final Version version = new Version(element.getAttribute("version"));

        return parseFromXML(nl, c, version);
    }

    //endregion File I/O

    private static void updateReplacementTokens(Campaign c) {
        if(null == replacementTokens) {
            replacementTokens = new LinkedHashMap<>();
        }
        Person commander = c.getSeniorCommander();
        if(null == commander) {
            //shouldn't happen unless there are no personnel, but just in case
            replacementTokens.put("@commanderRank", "rank(?)");
            replacementTokens.put("@commander", "commander(?)");

        } else {
            replacementTokens.put("@commanderRank", commander.getRankName());
            replacementTokens.put("@commander", commander.getFullTitle());
        }
    }

    /**
     * This will replace tokens in narrative text
     * @param text
     * @return
     */
    public static String replaceTokens(String text, Campaign c) {

        updateReplacementTokens(c);

        Pattern pattern;
        Matcher matcher;
        for (Map.Entry<String, String> replacement : replacementTokens.entrySet()) {
            pattern = Pattern.compile(replacement.getKey());
            matcher = pattern.matcher(text);
            text = matcher.replaceAll(replacement.getValue());
        }

        return text;
    }

}
