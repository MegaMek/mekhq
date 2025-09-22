/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.storyArc.storypoint;

import java.io.PrintWriter;
import java.text.ParseException;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.storyArc.StoryPoint;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This StoryPoint begins transit to some destination on the map. It completes when the campaign arrives at this
 * destination.
 */
public class TravelStoryPoint extends StoryPoint {
    private static final MMLogger LOGGER = MMLogger.create(TravelStoryPoint.class);

    // region Variable Declarations
    /** The id of the planetary system that is the destination */
    private String destinationId;

    /**
     * Should travel automatically begin to this system when the story point starts? For the time being, this will be
     * yes. Once we implement a graphical display of story objectives then we can give creators the option of letting
     * players arrange travel themselves
     */
    private boolean autoStart;
    // endregion Variable Declarations

    // region Constructors
    public TravelStoryPoint() {
        super();
        autoStart = true;
    }
    // endregion Constructors

    // region getter/setters
    @Override
    public String getTitle() {
        PlanetarySystem system = getDestination();

        if (null != system) {
            return system.getName(getStoryArc().getCampaign().getLocalDate());
        }
        return "Unknown planetary system";
    }

    public String getDestinationId() {
        return destinationId;
    }

    @Override
    protected String getResult() {
        return null;
    }
    // endregion getter/setters

    @Override
    public String getObjective() {
        return "Travel to " + getTitle();
    }

    public PlanetarySystem getDestination() {
        return Systems.getInstance().getSystemById(destinationId);
    }

    @Override
    public void start() {
        super.start();
        if (null == getDestination()) {
            // if we don't have a valid destination, then complete the story point
            complete();
        } else if (autoStart) {
            CurrentLocation location = getStoryArc().getCampaign().getLocation();
            JumpPath path = getStoryArc().getCampaign().calculateJumpPath(location.getCurrentSystem(),
                  getDestination());
            getStoryArc().getCampaign().getLocation().setJumpPath(path);
        }
    }

    // region File I/O
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "destinationId", destinationId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "autoStart", autoStart);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("destinationId")) {
                    destinationId = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("autoStart")) {
                    autoStart = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }
    // endregion File I/O
}
