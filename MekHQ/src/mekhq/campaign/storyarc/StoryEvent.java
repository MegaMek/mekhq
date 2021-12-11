/*
 * StoryEvent.java
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

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;


import java.util.UUID;

/**
 * The StoryEvent abstract class is the basic building block of a StoryArc. StoryEvents can do
 * different things when they are started. When they are completed they may start other events as
 * determined by the specific class and user input. StoryEvents are started in one of the following ways:
 *  - By being selected as the next event by a prior StoryEvent
 *  - By meeting the trigger conditions that are checked in various places in Campaign such as a specific date
 **/
public abstract class StoryEvent implements Serializable, MekHqXmlSerializable {

    /** The story arc that this event is a part of **/
    private StoryArc arc;

    /** The UUID id of this story event */
    private UUID id;

    /** A boolean that tracks whether the event is currently active **/
    private boolean active;

    /** A basic linear next event id that may be used by the story event to determine next event **/
    private UUID nextEventId;

    protected static final String NL = System.lineSeparator();

    public StoryEvent() {
        active = false;
    }

    public void setStoryArc(StoryArc a) { this.arc = a; }

    protected StoryArc getStoryArc() { return arc; }

    public void setId(UUID id) { this.id = id; }

    protected UUID getId() { return id; }

    public Boolean isActive() { return active; }

    /**
     * Do whatever needs to be done to start this event. Specific event types may need to override this
     */
    public void startEvent() {
        active = true;
    }

    /**
     * Complete the event. Specific event types may need to override this.
     */
    public void completeEvent() {
        active = false;
        proceedToNextStoryEvent();
    }

    /**
     * Gets the next story event and if it is not null, starts it
     */
    protected void proceedToNextStoryEvent() {
        //get the next story event
        UUID nextStoryEventId = getNextStoryEvent();
        StoryEvent nextStoryEvent = arc.getStoryEvent(nextStoryEventId);
        if(null != nextStoryEvent) {
            nextStoryEvent.startEvent();
        }
    }

    /**
     * determine the next story event in the story arc based on the event.
     * This may get overriden by more complex processes in specific event types, but
     * the default will be to go to the next event it
     **/
    protected UUID getNextStoryEvent() {
        return nextEventId;
    }

    //region I/O
    @Override
    public abstract void writeToXml(PrintWriter pw1, int indent);

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        String level = MekHqXmlUtil.indentStr(indent),
                level1 = MekHqXmlUtil.indentStr(indent + 1);

        StringBuilder builder = new StringBuilder(256);
        builder.append(level)
                .append("<storyEvent id=\"")
                .append(id)
                .append("\" type=\"")
                .append(this.getClass().getName())
                .append("\">")
                .append(NL)
                .append(level1)
                .append("<active>")
                .append(active)
                .append("</active>")
                .append(NL)
                .append(level1)
                .append("<nextEventId>")
                .append(nextEventId)
                .append("</nextEventId>")
                .append(NL);

        pw1.print(builder.toString());
    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</storyEvent>");
    }

    protected abstract void loadFieldsFromXmlNode(Node wn, Campaign c) throws ParseException;

    public static StoryEvent generateInstanceFromXML(Node wn, Campaign c) {
        StoryEvent retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        try {
            // Instantiate the correct child class, and call its parsing
            // function.
            retVal = (StoryEvent) Class.forName(className).getDeclaredConstructor().newInstance();

            retVal.id = UUID.fromString(wn.getAttributes().getNamedItem("id").getTextContent().trim());

            retVal.loadFieldsFromXmlNode(wn, c);

            // Okay, now load specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("nextEventId")) {
                    retVal.nextEventId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("active")) {
                    retVal.active = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }

}
