/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.market.contractMarket.ContractAutomation.performAutomatedActivation;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This keeps track of a location, which includes both the planet and the current position in-system. It may seem a
 * little like overkill to have a separate object here, but when we reach a point where we want to let a force be in
 * different locations, this will make it easier to keep track of everything
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class CurrentLocation extends AbstractLocation {
    private static final MMLogger logger = MMLogger.create(CurrentLocation.class);

    // keep track of jump path
    private JumpPath jumpPath;
    private double rechargeTime;
    // I would like to keep track of distance, but I ain't too good with physics
    private double transitTime;
    // JumpShip at nadir or zenith
    private boolean jumpZenith;

    // Populated during XML load; drained by CampaignXmlParser to reconnect persons after load.
    private transient List<UUID> pendingPersonIds = new ArrayList<>();

    /** Returns and clears the person UUIDs read from XML, for use during post-load reconnection. */
    public List<UUID> drainPendingPersonIds() {
        List<UUID> ids = new ArrayList<>(pendingPersonIds);
        pendingPersonIds.clear();
        return ids;
    }

    public CurrentLocation() {
        this(null, 0d);
    }

    public CurrentLocation(PlanetarySystem system, double time) {
        super(system);
        this.transitTime = time;
        this.rechargeTime = 0d;
        this.jumpZenith = true;
    }

    @Override
    public void setTransitTime(double time) {
        transitTime = time;
    }

    @Override
    public boolean isOnPlanet() {
        return transitTime <= 0;
    }

    @Override
    public boolean isAtJumpPoint() {
        return transitTime >= currentSystem.getTimeToJumpPoint(1.0);
    }

    @Override
    public double getPercentageTransit() {
        return 1 - transitTime / currentSystem.getTimeToJumpPoint(1.0);
    }

    @Override
    public boolean isInTransit() {
        return !isOnPlanet() && !isAtJumpPoint();
    }

    @Override
    public double getTransitTime() {
        return transitTime;
    }

    /**
     * @return a <code>boolean</code> indicating whether the JumpShip is at the zenith or not (nadir if false).
     */
    @Override
    public boolean isJumpZenith() {
        return jumpZenith;
    }

    @Override
    protected double getRechargeTime() {
        return rechargeTime;
    }

    @Override
    protected void setRechargeTime(double t) {
        rechargeTime = t;
    }

    /**
     * pick the best jump point (nadir of zenith). Chooses the one with a recharge station or randomly selects if both
     * have a recharge station or neither does.
     *
     * @param now - a <code>LocalDate</code> object for the present time
     *
     * @return a <code> boolean indicating whether the zenith position was chosen or not.
     */
    private boolean pickJumpPoint(LocalDate now) {
        if (currentSystem.isZenithCharge(now) && !currentSystem.isNadirCharge(now)) {
            return true;
        }
        if (!currentSystem.isZenithCharge(now) && currentSystem.isNadirCharge(now)) {
            return false;
        }
        // otherwise, both recharge stations or none so choose randomly
        return randomInt(2) == 1;
    }

    @Override
    public JumpPath getJumpPath() {
        return jumpPath;
    }

    @Override
    public void setJumpPath(JumpPath path) {
        jumpPath = path;
    }

    /**
     * Gets a value indicating whether the JumpShip is currently recharging.
     *
     * @param campaign The campaign object which owns the JumpShip.
     *
     * @return True if the JumpShip has to spend time recharging, otherwise false.
     */
    @Override
    public boolean isRecharging(Campaign campaign) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
              campaign.isGM(), campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
              campaign.getFactionStandings(), campaign.getFutureAtBContracts());

        return currentSystem.getRechargeTime(campaign.getLocalDate(), isUseCommandCircuit) > 0;
    }

    /**
     * Marks the JumpShip at the current location to be fully charged.
     *
     * @param campaign The campaign object which owns the JumpShip.
     */
    @Override
    public void setRecharged(Campaign campaign) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
              campaign.isGM(), campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
              campaign.getFactionStandings(), campaign.getFutureAtBContracts());

        rechargeTime = currentSystem.getRechargeTime(campaign.getLocalDate(), isUseCommandCircuit);
    }

    /**
     * Check for a jump path and if found, do whatever needs to be done to move forward
     */
    @Override
    public void newDay(Campaign campaign, boolean suppressReports) {
        final boolean wasTraveling = !isOnPlanet();
        LocalDate today = campaign.getLocalDate();

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
              campaign.isGM(), campaignOptions.isUseFactionStandingCommandCircuitSafe(),
              campaign.getFactionStandings(), campaign.getFutureAtBContracts());

        // recharge even if there is no jump path
        // because JumpShips don't go anywhere
        double hours = 24.0;
        double neededRechargeTime = currentSystem.getRechargeTime(today, isUseCommandCircuit);
        double usedRechargeTime = Math.min(hours, neededRechargeTime - rechargeTime);
        if (usedRechargeTime > 0) {
            if (!suppressReports) {
                campaign.addReport(GENERAL, "JumpShips spent " +
                                                  (Math.round(100.0 * usedRechargeTime) / 100.0) +
                                                  " hours recharging drives");
            }
            rechargeTime += usedRechargeTime;
            if (rechargeTime >= neededRechargeTime && !suppressReports) {
                campaign.addReport(GENERAL, "JumpShip drives fully charged");
            }
        }
        if ((null == jumpPath) || jumpPath.isEmpty()) {
            return;
        }
        // if we are not at the final jump point, then check to see if we are transiting
        // or if we can jump
        if (jumpPath.size() > 1) {
            // first check to see if we are transiting
            double usedTransitTime = Math.min(hours, 24.0 * (currentSystem.getTimeToJumpPoint(1.0) - transitTime));
            if (usedTransitTime > 0) {
                transitTime += usedTransitTime / 24.0;
                if (!suppressReports) {
                    campaign.addReport(GENERAL, "DropShips spent " +
                                                      (Math.round(100.0 * usedTransitTime) / 100.0) +
                                                      " hours in transit to jump point");
                    if (isAtJumpPoint()) {
                        campaign.addReport(GENERAL, "Jump point reached");
                    }
                }
            }
            if (isAtJumpPoint() && (rechargeTime >= neededRechargeTime)) {
                // jump
                if (campaignOptions.isUseAbilities()) {
                    checkForTransitDisorientationSyndrome(campaign, campaignOptions);
                }
                if (!suppressReports) {
                    campaign.addReport(GENERAL, "Jumping to " + jumpPath.get(1).getPrintableName(today));
                }
                currentSystem = jumpPath.get(1);
                jumpZenith = pickJumpPoint(today);
                jumpPath.removeFirstSystem();
                MekHQ.triggerEvent(new LocationChangedEvent(this, true));
                // reduce remaining hours by usedRechargeTime or usedTransitTime, whichever is
                // greater
                hours -= Math.max(usedRechargeTime, usedTransitTime);
                transitTime = currentSystem.getTimeToJumpPoint(1.0);
                rechargeTime = 0;
                // if there are hours remaining, then begin recharging jump drive
                usedRechargeTime = Math.min(hours, neededRechargeTime - rechargeTime);
                if (usedRechargeTime > 0) {
                    if (!suppressReports) {
                        campaign.addReport(GENERAL, "JumpShips spent " +
                                                          (Math.round(100.0 * usedRechargeTime) / 100.0) +
                                                          " hours recharging drives");
                    }
                    rechargeTime += usedRechargeTime;
                    if (rechargeTime >= neededRechargeTime && !suppressReports) {
                        campaign.addReport(GENERAL, "JumpShip drives fully charged");
                    }
                }
            }
        }
        // if we are now at the final jump point, then lets begin in-system transit
        if (jumpPath.size() == 1) {
            double usedTransitTime = Math.min(hours, 24.0 * transitTime);
            if (!suppressReports) {
                campaign.addReport(GENERAL, "DropShips spent " +
                                                  (Math.round(100.0 * usedTransitTime) / 100.0) +
                                                  " hours transiting into system");
            }
            transitTime -= usedTransitTime / 24.0;
            if (transitTime <= 0) {
                if (!suppressReports) {
                    campaign.addReport(GENERAL,
                          jumpPath.getLastSystem().getPrintableName(campaign.getLocalDate()) + " reached.");
                }
                // we are here!
                transitTime = 0;
                jumpPath = null;
                MekHQ.triggerEvent(new TransitCompleteEvent(this));
            }

            if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
                checkForDiseaseOrBioweaponOutbreaks(campaign, today);
            }
        }

        // If we were previously traveling and now aren't, we should check to see if we have arrived at a contract
        // system earlier than necessary. And, if appropriate, trigger inoculation prompts and activate mothballed
        // units
        if (wasTraveling && isOnPlanet()) {
            // This should be before inoculations so that we can correctly read the TO&E
            if (!campaign.getAutomatedMothballUnits().isEmpty()) {
                performAutomatedActivation(campaign);
            }

            if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
                if (!suppressReports) {
                    Inoculations.triggerInoculationPrompt(campaign, false);
                } else {
                    Inoculations.autoInoculateAll(campaign, this);
                }
            }

            testForEarlyArrival(campaign);
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "location");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "currentSystemId", currentSystem.getId());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transitTime", transitTime);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rechargeTime", rechargeTime);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "jumpZenith", jumpZenith);
        if (jumpPath != null) {
            jumpPath.writeToXML(pw, indent);
        }
        for (LocationNode child : locationNode.getChildren()) {
            if (child.getLocatable() instanceof mekhq.campaign.personnel.Person p) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personId", p.getId().toString());
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "location");
    }

    public static CurrentLocation generateInstanceFromXML(Node wn, Campaign c) {
        CurrentLocation retVal = null;

        try {
            retVal = new CurrentLocation();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("currentPlanetId") ||
                          wn2.getNodeName().equalsIgnoreCase("currentPlanetName") ||
                          wn2.getNodeName().equalsIgnoreCase("currentSystemId")) {
                    PlanetarySystem p = Systems.getInstance().getSystemById(wn2.getTextContent());
                    if (null == p) {
                        // Whoops, we can't find your planet man, back to Earth
                        logger.error("Couldn't find planet named {}", wn2.getTextContent());
                        p = c.getSystemByName("Terra");
                        if (null == p) {
                            // If that doesn't work then give the first planet we have
                            p = c.getSystems().getFirst();
                        }
                    }
                    retVal.currentSystem = p;
                } else if (wn2.getNodeName().equalsIgnoreCase("transitTime")) {
                    retVal.transitTime = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rechargeTime")) {
                    retVal.rechargeTime = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("jumpZenith")) {
                    retVal.jumpZenith = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("jumpPath")) {
                    retVal.jumpPath = JumpPath.generateInstanceFromXML(wn2, c);
                } else if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    retVal.pendingPersonIds.add(UUID.fromString(wn2.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }
}
