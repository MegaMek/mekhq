/*
 * StoryPoint.java
 *
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved
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
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.PrintWriter;
import java.text.ParseException;


import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * <p>The StoryPoint abstract class is the basic building block of a StoryArc. StoryPoints can do different things when
 * they are started. When they are completed they may start other story points as determined by the specific class and
 * user input. StoryPoints are started in one of the following ways: (1) by being selected as the next story point by
 * a prior StoryPoint; (2) by meeting the trigger conditions that {@link StoryArc StoryArc} listens for.
 * <p>When a StoryPoint is started, it runs the {@link StoryPoint#start() start} method. This method will often be
 * overridden by extending classes, but overriding methods should include `super.start()` at the beginning.
 * <p>When a StoryPoint is done, it runs the {@link StoryPoint#complete() complete} method. This method will also
 * often be overwritten by extending classes, but such methods should include `super.complete()` at the end to ensure
 * StoryTriggers and StoryOutcomes are processed.
 * <p>A StoryPoint should always return a String with the {@link StoryPoint#getResult() getResult} method. This abstract
 * method must be supplied in all concrete classes. It can be used to indicate different possible outcomes from the
 * StoryPoint.
 * <p>A StoryPoint can contain a hash of {@link StoryOutcome StoryOutcome} objects. The key for the hash is a particular
 * result from the {@link StoryPoint#getResult() getResult} method. When a story point is completed, a StoryOutcome
 * matching the result will be looked for. If one is found, its `nextStoryPointId` and StoryTriggers will replace
 * the default ones set in this class. This feature is what allows for branching.
 * <p>A StoryPoint can also contain a list of {@link StoryTrigger StoryTrigger} objects. StoryTriggers can be used to make
 * various changes to the campaign. These StoryTriggers will be processed upon the completion of the StoryPoint. Note
 * that if a StoryOutcome is found that matches the {@link StoryPoint#getResult() getResult} method, the default
 * StoryTriggers specified will be replaced by those from the StoryOutcome.
 */
public abstract class StoryPoint {

    /** The story arc that this story point is a part of **/
    private StoryArc storyArc;

    /** The UUID id of this story point */
    private UUID id;

    /** a name for this story point **/
    private String name;

    /** A boolean that tracks whether the story point is currently active **/
    private boolean active;

    /**
     * The id of the next story point to start when this one is completed. It can be null if a new story point should not be
     * triggered. It can also be overwritten by a StoryOutcome
     * **/
    private UUID nextStoryPointId;

    /** A map of all possible StoryOutcomes **/
    protected Map<String, StoryOutcome> storyOutcomes;

    /** A list of StoryTriggers to execute on completion, can be overwritten by StoryOutcome */
    List<StoryTrigger> storyTriggers;

    public StoryPoint() {
        active = false;
        storyOutcomes =  new LinkedHashMap<>();
        storyTriggers = new ArrayList<>();
    }

    public void setStoryArc(StoryArc a) {
        this.storyArc = a;
        //also apply it to any triggers
        for (StoryTrigger storyTrigger : storyTriggers) {
            storyTrigger.setStoryArc(a);
        }
        //also might need to apply it to triggers in storyOutcomes
        for (Entry<String, StoryOutcome> entry : storyOutcomes.entrySet()) {
            entry.getValue().setStoryArc(a);
        }
    }

    protected StoryArc getStoryArc() {
        return storyArc;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    protected UUID getId() {
        return id;
    }

    public Boolean isActive() {
        return active;
    }

    public abstract String getTitle();

    public String getName() {
        return name;
    }

    /**
     * Do whatever needs to be done to start this story point. Specific story point types may need to override this
     */
    public void start() {
        active = true;
    }

    /**
     * Complete the story point, by processing outcomes, triggers, and proceeding to the next story point. Specific
     * story point types may need to override this.
     */
    public void complete() {
        active = false;
        processOutcomes();
        processTriggers();
        proceedToNextStoryPoint();
        // refresh the GUI in case things have changed or been added
        getCampaign().getApp().getCampaigngui().refreshAllTabs();

    }

    /**
     * Identify if a {@link StoryOutcome StoryOutcome} has a key matching the result and replace default nextStoryPointId
     * and StoryTriggers if found.
     */
    private void processOutcomes() {
        StoryOutcome chosenOutcome = storyOutcomes.get(getResult());
        if (null != chosenOutcome) {
            nextStoryPointId = chosenOutcome.getNextStoryPointId();
            storyTriggers = chosenOutcome.getStoryTriggers();
        }
    }

    /**
     * Iterate through current list of {@link StoryTrigger StoryTrigger} objects and execute each of them in turn.
     */
    private void processTriggers() {
        for (StoryTrigger storyTrigger : storyTriggers) {
            storyTrigger.execute();
        }
    }

    /**
     * Returns a string specifying the result from this StoryPoint. This can be used to identify different possible
     * results, when multiple results are possible. If different results are not possible, an empty string can be
     * returned.
     * @return A String specifying the result
     */
    protected abstract String getResult();

    /**
     * Returns a string to be used in the "Objectives" panel so players know what they should be doing next.
     * @return a <code>String</code> indicating what to show in the objective screen.
     */
    protected String getObjective() {
        return "";
    }

    /**
     * Gets the next story point and if it is not null, starts it
     */
    protected void proceedToNextStoryPoint() {
        // get the next story point
        StoryPoint nextStoryPoint = getNextStoryPoint();
        if (null != nextStoryPoint) {
            nextStoryPoint.start();
        }
    }

    /**
     * determine the next story point in the story arc based on the point. This could have been changed depending
     * on StoryOutcome
     **/
    protected StoryPoint getNextStoryPoint() {
        return storyArc.getStoryPoint(nextStoryPointId);
    }

    public Campaign getCampaign() {
        return getStoryArc().getCampaign();
    }

    //region I/O
    public abstract void writeToXml(PrintWriter pw1, int indent);

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "storyPoint", "name", name,"type", this.getClass());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "id", id);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "active", active);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "nextStoryPointId", nextStoryPointId);
        if (!storyOutcomes.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "storyOutcomes");
            for (Entry<String, StoryOutcome> entry : storyOutcomes.entrySet()) {
                entry.getValue().writeToXml(pw1, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "storyOutcomes");
        }
        if (!storyTriggers.isEmpty()) {
            for (StoryTrigger trigger : storyTriggers) {
                trigger.writeToXml(pw1, indent);
            }
        }
    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, indent, "storyPoint");
    }

    protected abstract void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException;

    public static StoryPoint generateInstanceFromXML(Node wn, Campaign c, Version version) {
        StoryPoint retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        try {
            // Instantiate the correct child class, and call its parsing
            // function.
            retVal = (StoryPoint) Class.forName(className).getDeclaredConstructor().newInstance();

            retVal.name = wn.getAttributes().getNamedItem("name").getTextContent().trim();

            retVal.loadFieldsFromXmlNode(wn, c, version);

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("nextStoryPointId")) {
                    retVal.nextStoryPointId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("active")) {
                    retVal.active = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("storyTrigger")) {
                    StoryTrigger trigger = StoryTrigger.generateInstanceFromXML(wn2, c, version);
                    retVal.storyTriggers.add(trigger);
                } else if (wn2.getNodeName().equalsIgnoreCase("storyOutcomes")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("storyOutcome")) {
                            // Error condition of sorts!
                            // Error, what should we do here?
                            LogManager.getLogger().error("Unknown node type not loaded in storyOutcomes nodes: " + wn3.getNodeName());

                            continue;
                        }
                        StoryOutcome s = StoryOutcome.generateInstanceFromXML(wn3, c, version);

                        if (null != s) {
                            retVal.storyOutcomes.put(s.getResult(), s);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error(ex);
        }

        return retVal;
    }

}
