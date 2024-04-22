/*
 * StoryTrigger.java
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

import java.io.PrintWriter;
import java.text.ParseException;

/**
 * A Story Trigger can be added to a StoryPoint or a StoryOutcome and when the StoryPoint is completed the StoryTrigger
 * will be executed and will do some things. This is a way to have StoryPoints affect things other than just the
 * next story point
 */
public abstract class StoryTrigger {

    /** The story arc that this trigger is a part of **/
    private StoryArc arc;

    public StoryTrigger() {
        //nothing here at the moment
    }

    public void setStoryArc(StoryArc a) {
        this.arc = a;
    }

    protected StoryArc getStoryArc() {
        return arc;
    }

    protected Campaign getCampaign() {
        return getStoryArc().getCampaign();
    }

    /**
     * Execute whatever the trigger does
     */
    protected abstract void execute();

    //region I/O
    public abstract void writeToXml(PrintWriter pw1, int indent);

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "storyTrigger", "type", this.getClass());
    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, indent, "storyTrigger");
    }

    protected abstract void loadFieldsFromXmlNode(Node wn, Campaign c, Version v) throws ParseException;

    public static StoryTrigger generateInstanceFromXML(Node wn, Campaign c, Version v) {
        StoryTrigger retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        try {
            // Instantiate the correct child class, and call its parsing
            // function.
            retVal = (StoryTrigger) Class.forName(className).getDeclaredConstructor().newInstance();

            retVal.loadFieldsFromXmlNode(wn, c, v);

        } catch (Exception ex) {
            LogManager.getLogger().error(ex);
        }

        return retVal;
    }
    //endregion I/O

}
