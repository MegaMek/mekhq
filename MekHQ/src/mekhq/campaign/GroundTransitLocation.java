/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import static mekhq.campaign.enums.DailyReportType.GENERAL;

import java.io.PrintWriter;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.events.TransitStatusChangedEvent;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A moving location that tracks on-planet ground transport, such as an overland convoy travelling
 * between two sites on the same planet.
 *
 * <p>Unlike {@link CurrentLocation}, a {@code GroundTransitLocation} never leaves the planet — it is
 * always considered on-planet ({@link #isOnPlanet()} stays {@code true} for the whole journey) and
 * has no jump points or JumpShip recharge. It only carries a remaining ground {@link #transitTime}
 * (in days) that counts down day-by-day until it arrives.</p>
 */
public class GroundTransitLocation extends AbstractMobileLocation {
    private static final MMLogger logger = MMLogger.create(GroundTransitLocation.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GroundTransitLocation";

    // total length of the journey in days, used to report travel progress
    private double totalTransitTime;

    public GroundTransitLocation() {
        this(null, 0d);
    }

    public GroundTransitLocation(PlanetarySystem system, double transitTime) {
        super(system, transitTime);
        this.totalTransitTime = transitTime;
    }

    @Override
    public boolean isInTransit() {
        return transitTime > 0;
    }

    @Override
    public double getPercentageTransit() {
        return totalTransitTime <= 0 ? 1.0 : 1 - transitTime / totalTransitTime;
    }

    /**
     * Advances the overland journey by up to one day. When the remaining transit time reaches zero
     * the convoy has arrived: a {@link TransitCompleteEvent} fires and each child place is notified
     * of arrival.
     */
    @Override
    public void newDay(Campaign campaign, boolean isSilentProcessing) {
        if (transitTime <= 0) {
            return;
        }

        double daysTravelled = Math.min(1.0, transitTime);
        transitTime -= daysTravelled;

        if (!isSilentProcessing) {
            campaign.addReport(GENERAL, MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE,
                  "getReport.newDay.timeTraveling", (Math.round(100.0 * 24.0 * daysTravelled) / 100.0)));
        }

        if (transitTime <= 0) {
            transitTime = 0;
            if (!isSilentProcessing) {
                campaign.addReport(GENERAL,
                      MHQInternationalization.getTextAt(RESOURCE_BUNDLE, "getReport.newDay.destination"));
            }
            MekHQ.triggerEvent(new TransitCompleteEvent(this));
            notifyChildrenArrived(campaign, isSilentProcessing);
        } else {
            MekHQ.triggerEvent(new TransitStatusChangedEvent(this));
        }
    }

    @Override
    public void writeToXML(final PrintWriter printWriter, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, "groundTransitLocation");
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "currentSystemId", currentSystem.getId());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "transitTime", transitTime);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "totalTransitTime", totalTransitTime);
        for (ILocation child : getChildLocations()) {
            if (child instanceof Person person) {
                MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "personId", person.getId().toString());
            } else if (child instanceof Unit unit) {
                MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "unitId", unit.getId().toString());
            } else if (child instanceof Part part) {
                MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "partId", String.valueOf(part.getId()));
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, "groundTransitLocation");
    }

    public static GroundTransitLocation generateInstanceFromXML(Node workingNode, Campaign campaign) {
        GroundTransitLocation returnValue = null;

        try {
            returnValue = new GroundTransitLocation();
            NodeList nodeList = workingNode.getChildNodes();

            for (int x = 0; x < nodeList.getLength(); x++) {
                Node workingNode2 = nodeList.item(x);
                if (workingNode2.getNodeName().equalsIgnoreCase("currentSystemId")) {
                    PlanetarySystem planetarySystem = campaign.getSystemById(workingNode2.getTextContent());
                    if (null == planetarySystem) {
                        // Whoops, we can't find your planet man, back to Earth
                        logger.error("Couldn't find planet named {}", workingNode2.getTextContent());
                        planetarySystem = campaign.getSystemByName("Terra");
                        if (null == planetarySystem) {
                            // If that doesn't work then give the first planet we have
                            planetarySystem = campaign.getSystems().getFirst();
                        }
                    }
                    returnValue.currentSystem = planetarySystem;
                } else if (workingNode2.getNodeName().equalsIgnoreCase("transitTime")) {
                    returnValue.transitTime = Double.parseDouble(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("totalTransitTime")) {
                    returnValue.totalTransitTime = Double.parseDouble(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("personId")) {
                    returnValue.pendingPersonIds.add(UUID.fromString(workingNode2.getTextContent().trim()));
                } else if (workingNode2.getNodeName().equalsIgnoreCase("unitId")) {
                    returnValue.pendingUnitIds.add(UUID.fromString(workingNode2.getTextContent().trim()));
                } else if (workingNode2.getNodeName().equalsIgnoreCase("partId")) {
                    returnValue.pendingPartIds.add(Integer.parseInt(workingNode2.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return returnValue;
    }
}
