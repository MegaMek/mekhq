/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.storyarc;

import java.io.PrintWriter;
import java.text.ParseException;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * A Story Trigger can be added to a StoryPoint or a StoryOutcome and when the StoryPoint is completed the StoryTrigger
 * will be executed and will do some things. This is a way to have StoryPoints affect things other than just the next
 * story point
 */
public abstract class StoryTrigger {
    private static final MMLogger logger = MMLogger.create(StoryTrigger.class);

    /** The story arc that this trigger is a part of **/
    private StoryArc arc;

    public StoryTrigger() {
        // nothing here at the moment
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

    // region I/O
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
            logger.error(ex);
        }

        return retVal;
    }
    // endregion I/O

}
