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
 */
package mekhq.campaign;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.logging.MMLogger;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;

/**
 * This is an array list of planets for a jump path, from which we can derive
 * various statistics. We can also add in details about the jump path here, like
 * if
 * the user would like to use recharge stations when available. For XML
 * serialization,
 * this object will need to spit out a list of planet names and then reconstruct
 * the planets from that.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class JumpPath {
    private static final MMLogger logger = MMLogger.create(JumpPath.class);

    private List<PlanetarySystem> path;

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

    public PlanetarySystem getFirstSystem() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.get(0);
        }
    }

    public PlanetarySystem getLastSystem() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.get(path.size() - 1);
        }
    }

    public double getStartTime(double currentTransit) {
        double startTime = 0.0;
        if (null != getFirstSystem()) {
            startTime = getFirstSystem().getTimeToJumpPoint(1.0);
        }
        return startTime - currentTransit;
    }

    public double getEndTime() {
        double endTime = 0.0;
        if (null != getLastSystem()) {
            endTime = getLastSystem().getTimeToJumpPoint(1.0);
        }
        return endTime;
    }

    public double getTotalRechargeTime(LocalDate when) {
        int rechargeTime = 0;
        for (PlanetarySystem system : path) {
            if (system.equals(getFirstSystem())) {
                continue;
            }
            if (system.equals(getLastSystem())) {
                continue;
            }
            rechargeTime += (int) Math.ceil(system.getRechargeTime(when));
        }
        return rechargeTime / 24.0;
    }

    public int getJumps() {
        return size() - 1;
    }

    public double getTotalTime(LocalDate when, double currentTransit) {
        return getTotalRechargeTime(when) + getStartTime(currentTransit) + getEndTime();
    }

    public void addSystem(PlanetarySystem s) {
        path.add(s);
    }

    public void addSystems(List<PlanetarySystem> systems) {
        path.addAll(systems);
    }

    public void removeFirstSystem() {
        if (!path.isEmpty()) {
            path.remove(0);
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
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "jumpPath");
    }

    public static JumpPath generateInstanceFromXML(Node wn, Campaign c) {
        JumpPath retVal = null;

        try {
            retVal = new JumpPath();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("planetName")) {
                    PlanetarySystem p = c.getSystemByName(wn2.getTextContent());
                    if (null != p) {
                        retVal.addSystem(p);
                    } else {
                        logger.error("Couldn't find planet named " + wn2.getTextContent());
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }
}
