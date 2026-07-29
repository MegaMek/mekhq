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

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is an array list of planets for a jump path, from which we can derive various statistics. We can also add in
 * details about the jump path here, like if the user would like to use recharge stations when available. For XML
 * serialization, this object will need to spit out a list of planet names and then reconstruct the planets from that.
 *
 * <p>A path may optionally specify a {@link #getTargetPlanet() target planet} within its destination system; when set,
 * the final in-system leg is measured to that planet instead of the system's primary world.</p>
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class JumpPath {
    private static final MMLogger LOGGER = MMLogger.create(JumpPath.class);

    private final List<PlanetarySystem> path;

    /**
     * The specific planet within the destination system this path terminates at. When {@code null} (the default), the
     * path terminates at the destination system's primary world.
     */
    @Nullable
    private Planet targetPlanet;

    public JumpPath() {
        path = new ArrayList<>();
    }

    public JumpPath(ArrayList<PlanetarySystem> p) {
        path = p;
    }

    public List<PlanetarySystem> getSystems() {
        return path;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public @Nullable PlanetarySystem getFirstSystem() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.getFirst();
        }
    }

    public @Nullable PlanetarySystem getLastSystem() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.getLast();
        }
    }

    /**
     * @return the specific planet this path terminates at, or {@code null} if it terminates at the destination system's
     *       primary world
     */
    public @Nullable Planet getTargetPlanet() {
        return targetPlanet;
    }

    /**
     * Sets the specific planet this path terminates at, overriding the default of the destination system's primary
     * world. The planet is expected to belong to the {@link #getLastSystem() last system} on the path.
     *
     * @param targetPlanet the destination planet, or {@code null} to restore the default primary-world destination
     */
    public void setTargetPlanet(final @Nullable Planet targetPlanet) {
        this.targetPlanet = targetPlanet;
    }

    public double getStartTime(double currentTransit) {
        double startTime = 0.0;
        if (null != getFirstSystem()) {
            startTime = getFirstSystem().getTimeToJumpPoint(1.0);
        }
        return startTime - currentTransit;
    }

    public double getEndTime() {
        if (null == getLastSystem()) {
            return 0.0;
        }

        // A specified target planet overrides the default of transiting to the destination system's primary world.
        return (targetPlanet != null)
                     ? targetPlanet.getTimeToJumpPoint(1.0)
                     : getLastSystem().getTimeToJumpPoint(1.0);
    }

    /**
     * Use {@link #getTotalRechargeTime(LocalDate, boolean)} instead
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public double getTotalRechargeTime(LocalDate when) {
        return getTotalRechargeTime(when, false);
    }

    /**
     * Calculates the total recharge time, in days, required to complete a journey along the path of planetary systems.
     *
     * <p>This method iterates through each system in the path, excluding the first and last systems, and sums the
     * rounded-up recharge time (in hours) for each waypoint. The total is then converted from hours to days.</p>
     *
     * @param when                the date to use for determining recharge times at each system
     * @param isUseCommandCircuit {@code true} if command circuits are being utilized during the journey; may affect
     *                            recharge efficiency or duration
     *
     * @return the total recharge time for the route, expressed in days
     */
    public double getTotalRechargeTime(LocalDate when, boolean isUseCommandCircuit) {
        int rechargeTime = 0;
        for (PlanetarySystem system : path) {
            if (system.equals(getFirstSystem())) {
                continue;
            }
            if (system.equals(getLastSystem())) {
                continue;
            }
            rechargeTime += (int) Math.ceil(system.getRechargeTime(when, isUseCommandCircuit));
        }
        return rechargeTime / 24.0;
    }

    public int getJumps() {
        return size() - 1;
    }

    /**
     * Use {@link #getTotalTime(LocalDate, double, boolean)} instead
     * <p>
     * Used in Legacy AtB tests.
     */
    @Deprecated(since = "0.50.07")
    public double getTotalTime(LocalDate when, double currentTransit) {
        return getTotalTime(when, currentTransit, false);
    }

    /**
     * Calculates the total journey time for the path of planetary systems, including recharge, start, and end times.
     *
     * <p>This method sums three parts:</p>
     * <ul>
     *     <li>Recharge time for intermediate planetary systems (in days), accounting for possible command circuit usage</li>
     *     <li>Start time, based on the current transit</li>
     *     <li>End time, representing final approach or operations at the destination</li>
     * </ul>
     *
     * @param when                the date to use for all time calculations in the journey
     * @param currentTransit      the remaining fraction of the current transit (in days or hours, depending on
     *                            context)
     * @param isUseCommandCircuit {@code true} if command circuits are used for the journey, which may affect recharge
     *                            time calculations
     *
     * @return the total time required for the journey, in days
     */
    public double getTotalTime(LocalDate when, double currentTransit, boolean isUseCommandCircuit) {
        return getTotalRechargeTime(when, isUseCommandCircuit) + getStartTime(currentTransit) + getEndTime();
    }

    public void addSystem(PlanetarySystem s) {
        path.add(s);
    }

    public void addSystems(List<PlanetarySystem> systems) {
        path.addAll(systems);
    }

    public void removeFirstSystem() {
        if (!path.isEmpty()) {
            path.removeFirst();
        }
    }

    public int size() {
        return path.size();
    }

    public PlanetarySystem get(int i) {
        if (i >= size()) {
            return null;
        } else {
            return path.get(i);
        }
    }

    public boolean contains(PlanetarySystem system) {
        return path.contains(system);
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "jumpPath");
        for (PlanetarySystem planetarySystem : path) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetName", planetarySystem.getId());
        }
        if (targetPlanet != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "targetPlanetId", targetPlanet.getId());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "jumpPath");
    }

    public static JumpPath generateInstanceFromXML(Node wn, Campaign c) {
        JumpPath retVal = null;

        try {
            retVal = new JumpPath();
            String targetPlanetId = null;
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("planetName")) {
                    PlanetarySystem p = c.getSystemByName(wn2.getTextContent());
                    if (null != p) {
                        retVal.addSystem(p);
                    } else {
                        LOGGER.error("Couldn't find planet named {}", wn2.getTextContent());
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("targetPlanetId")) {
                    targetPlanetId = wn2.getTextContent();
                }
            }

            // Resolve the target planet against the destination system after the whole path is built.
            if ((targetPlanetId != null) && (retVal.getLastSystem() != null)) {
                retVal.setTargetPlanet(retVal.getLastSystem().getPlanetById(targetPlanetId));
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        return retVal;
    }
}
