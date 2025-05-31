/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.personnel.BodyLocation.INTERNAL;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_TRANSIT_DISORIENTATION_SYNDROME;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.TRANSIT_DISORIENTATION_SYNDROME;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.LocationChangedEvent;
import mekhq.campaign.event.TransitCompleteEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This keeps track of a location, which includes both the planet and the current position in-system. It may seem a
 * little like overkill to have a separate object here, but when we reach a point where we want to let a force be in
 * different locations, this will make it easier to keep track of everything
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class CurrentLocation {
    private static final MMLogger logger = MMLogger.create(CurrentLocation.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CurrentLocation";

    private PlanetarySystem currentSystem;
    // keep track of jump path
    private JumpPath jumpPath;
    private double rechargeTime;
    // I would like to keep track of distance, but I ain't too good with fyziks
    private double transitTime;
    // JumpShip at nadir or zenith
    private boolean jumpZenith;

    public CurrentLocation() {
        this(null, 0d);
    }

    public CurrentLocation(PlanetarySystem system, double time) {
        this.currentSystem = system;
        this.transitTime = time;
        this.rechargeTime = 0d;
        this.jumpZenith = true;
    }

    public void setTransitTime(double time) {
        transitTime = time;
    }

    public boolean isOnPlanet() {
        return transitTime <= 0;
    }

    public boolean isAtJumpPoint() {
        return transitTime >= currentSystem.getTimeToJumpPoint(1.0);
    }

    public double getPercentageTransit() {
        return 1 - transitTime / currentSystem.getTimeToJumpPoint(1.0);
    }

    public boolean isInTransit() {
        return !isOnPlanet() && !isAtJumpPoint();
    }

    public PlanetarySystem getCurrentSystem() {
        return currentSystem;
    }

    /**
     * @return the current planet location. This is currently the primary planet of the system, but in the future this
     *       will not be the case.
     */
    public Planet getPlanet() {
        return getCurrentSystem().getPrimaryPlanet();
    }

    public double getTransitTime() {
        return transitTime;
    }

    /**
     * @return a <code>boolean</code> indicating whether the JumpShip is at the zenith or not (nadir if false).
     */
    public boolean isJumpZenith() {
        return jumpZenith;
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

    public String getReport(LocalDate date, Money jumpCost) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
              // First Line
              .append("In ").append(currentSystem.getPrintableName(date)).append(' ');

        if (isOnPlanet()) {
            sb.append("on planet ").append(getPlanet().getPrintableName(date));
        } else if (isAtJumpPoint()) {
            sb.append("at jump point");
            if (!Double.isInfinite(currentSystem.getRechargeTime(date))) {
                sb.append(" (Jumpship ")
                      .append(String.format(Locale.ROOT,
                            "%.0f",
                            (100.0 * rechargeTime) / currentSystem.getRechargeTime(date)))
                      .append("% charged)");
            }
        } else {
            if ((null != jumpPath) && (currentSystem == jumpPath.getLastSystem())) {
                sb.append(String.format(Locale.ROOT, "%.2f", getTransitTime())).append(" days from planet");
            } else {
                double timeToJP = currentSystem.getTimeToJumpPoint(1.0) - getTransitTime();
                sb.append(String.format(Locale.ROOT, "%.2f", timeToJP)).append(" days from jump point");
            }

        }

        sb.append("<br/>");

        // Second Line

        if ((null != jumpPath) && !jumpPath.isEmpty()) {
            sb.append("Traveling to ").append(jumpPath.getLastSystem().getPrintableName(date)).append(": ");
            if (jumpPath.getJumps() > 0) {
                sb.append(jumpPath.getJumps())
                      .append(jumpPath.getJumps() == 1 ? " jump remaining" : " jumps remaining");
            } else {
                sb.append("In destination system");
            }
        } else {
            sb.append("Not traveling");
        }

        sb.append("<br/>");

        // Third Line
        sb.append("Estimated Jump Cost: ").append(jumpCost.toAmountString()).append(" C-Bills<br><br>");

        sb.append("</html>");
        return sb.toString();
    }

    public JumpPath getJumpPath() {
        return jumpPath;
    }

    public void setJumpPath(JumpPath path) {
        jumpPath = path;
    }

    /**
     * Gets a value indicating whether or not the JumpShip is currently recharging.
     *
     * @param campaign The campaign object which owns the JumpShip.
     *
     * @return True if the JumpShip has to spend time recharging, otherwise false.
     */
    public boolean isRecharging(Campaign campaign) {
        return currentSystem.getRechargeTime(campaign.getLocalDate()) > 0;
    }

    /**
     * Marks the JumpShip at the current location to be fully charged.
     *
     * @param campaign The campaign object which owns the JumpShip.
     */
    public void setRecharged(Campaign campaign) {
        rechargeTime = currentSystem.getRechargeTime(campaign.getLocalDate());
    }

    /**
     * Check for a jump path and if found, do whatever needs to be done to move forward
     */
    public void newDay(Campaign campaign) {
        final boolean wasTraveling = !isOnPlanet();

        // recharge even if there is no jump path
        // because JumpShips don't go anywhere
        double hours = 24.0;
        double neededRechargeTime = currentSystem.getRechargeTime(campaign.getLocalDate());
        double usedRechargeTime = Math.min(hours, neededRechargeTime - rechargeTime);
        if (usedRechargeTime > 0) {
            campaign.addReport("JumpShips spent " +
                                     (Math.round(100.0 * usedRechargeTime) / 100.0) +
                                     " hours recharging drives");
            rechargeTime += usedRechargeTime;
            if (rechargeTime >= neededRechargeTime) {
                campaign.addReport("JumpShip drives fully charged");
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
                campaign.addReport("DropShips spent " +
                                         (Math.round(100.0 * usedTransitTime) / 100.0) +
                                         " hours in transit to jump point");
                if (isAtJumpPoint()) {
                    campaign.addReport("Jump point reached");
                }
            }
            if (isAtJumpPoint() && (rechargeTime >= neededRechargeTime)) {
                // jump
                final CampaignOptions campaignOptions = campaign.getCampaignOptions();
                if (campaignOptions.isPayForTransport()) {
                    if (!campaign.getFinances()
                               .debit(TransactionType.TRANSPORTATION,
                                     campaign.getLocalDate(),
                                     campaign.calculateCostPerJump(true, campaignOptions.isEquipmentContractBase()),
                                     "Jump from " +
                                           currentSystem.getName(campaign.getLocalDate()) +
                                           " to " +
                                           jumpPath.get(1).getName(campaign.getLocalDate()))) {
                        campaign.addReport("<font color='" +
                                                 ReportingUtilities.getNegativeColor() +
                                                 "'><b>You cannot afford to make the jump!</b></font>");
                        return;
                    }

                    if (campaignOptions.isUseAbilities()) {
                        for (Person person : campaign.getPersonnelFilteringOutDeparted()) {
                            if (person.getOptions().booleanOption(FLAW_TRANSIT_DISORIENTATION_SYNDROME)) {
                                Injury injury = TRANSIT_DISORIENTATION_SYNDROME.newInjury(campaign,
                                      person,
                                      INTERNAL,
                                      1);
                                person.addInjury(injury);

                                if (campaignOptions.isUseFatigue()) {
                                    person.changeFatigue(campaignOptions.getFatigueRate());
                                }
                            }
                        }
                    }
                }
                campaign.addReport("Jumping to " + jumpPath.get(1).getPrintableName(campaign.getLocalDate()));
                currentSystem = jumpPath.get(1);
                jumpZenith = pickJumpPoint(campaign.getLocalDate());
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
                    campaign.addReport("JumpShips spent " +
                                             (Math.round(100.0 * usedRechargeTime) / 100.0) +
                                             " hours recharging drives");
                    rechargeTime += usedRechargeTime;
                    if (rechargeTime >= neededRechargeTime) {
                        campaign.addReport("JumpShip drives fully charged");
                    }
                }
            }
        }
        // if we are now at the final jump point, then lets begin in-system transit
        if (jumpPath.size() == 1) {
            double usedTransitTime = Math.min(hours, 24.0 * transitTime);
            campaign.addReport("DropShips spent " +
                                     (Math.round(100.0 * usedTransitTime) / 100.0) +
                                     " hours transiting into system");
            transitTime -= usedTransitTime / 24.0;
            if (transitTime <= 0) {
                campaign.addReport(jumpPath.getLastSystem().getPrintableName(campaign.getLocalDate()) + " reached.");
                // we are here!
                transitTime = 0;
                jumpPath = null;
                MekHQ.triggerEvent(new TransitCompleteEvent(this));
            }
        }

        // If we were previously traveling and now aren't, we should check to see if we have arrived at a contract
        // system earlier than necessary.
        if (wasTraveling && isOnPlanet()) {
            testForEarlyArrival(campaign);
        }
    }

    /**
     * Tests for whether the campaign arrived at a contract location before it's due to start.
     *
     * <p>This method checks if the campaign has arrived early by comparing the current system with the system of
     * each future contract. If the campaign has arrived early, it calculates the number of days until the contract's
     * start date and generates both in-character and out-of-character messages. These messages are then displayed using
     * an {@link ImmersiveDialogSimple} dialog.</p>
     *
     * <p>The first matching contract in the system ends the loop after handling early arrival notifications.</p>
     *
     * @param campaign The {@link Campaign} instance containing details of the current campaign, including the current
     *                 system, future contracts, local date, and related resources needed for messaging.
     */
    private void testForEarlyArrival(Campaign campaign) {
        List<Contract> futureContracts = campaign.getFutureContracts();

        for (Contract contract : futureContracts) {
            if (Objects.equals(currentSystem, contract.getSystem())) {
                int daysTillStart = campaign.getLocalDate().until(contract.getStartDate()).getDays();

                String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                      "contract.arrivedEarly.ic." + randomInt(10),
                      campaign.getCommanderAddress(false),
                      daysTillStart);

                String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                      "contract.arrivedEarly.ooc",
                      campaign.getCommanderAddress(false));

                new ImmersiveDialogSimple(campaign,
                      campaign.getSeniorAdminPerson(TRANSPORT),
                      null,
                      inCharacterMessage,
                      null,
                      outOfCharacterMessage,
                      null,
                      false);
                break;
            }
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
                        logger.error("Couldn't find planet named " + wn2.getTextContent());
                        p = c.getSystemByName("Terra");
                        if (null == p) {
                            // If that doesn't work then give the first planet we have
                            p = c.getSystems().get(0);
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
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }
}
